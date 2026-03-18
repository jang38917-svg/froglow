"""
피부 타입(건성/지성/복합성) 설문 API

1) GET  /survey/questions
   - 질문 목록 + 선택지 목록 반환 (order 순서대로)

2) GET  /survey/preset-options
   - 설문 전에 먼저 보여줄 선택지:
     ["건성", "지성", "복합성", "모르겠습니다"]

3) POST /survey/submit
   - preset_skin_type != "모르겠습니다" 이면:
       -> 설문 스킵하고 해당 타입을 결과로 저장
   - preset_skin_type == "모르겠습니다"(또는 None)이면:
       -> 기존 설문 로직 그대로 진행 (answers 기반 계산)
"""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from datetime import datetime

from app.db.session import get_db

# User 모델 추가 (설문 완료 시간 DB 반영용)
from app.models.user import User

# 설문 모델
from app.models.survey import (
    SurveyQuestion,
    SurveyOption,
    SurveyResponse,
    SurveyAnswer,
)

# 설문 스키마
from app.schemas.survey import SurveyQuestionOut, SurveySubmitIn, SurveyResultOut

# 로그인 연동
from app.api.auth import get_current_user


router = APIRouter(prefix="/survey", tags=["Survey"])

# (추가) 설문 전 사전 선택 옵션
PRESET_SKIN_TYPES = ["건성", "지성", "복합성", "모르겠습니다"]

# 🚀 AI 분석기 연동을 위한 한글 -> 영문 매핑 사전 (네 변수명 COMB 유지!)
SKIN_TYPE_MAP = {
    "건성": "DRY",
    "지성": "OILY",
    "복합성": "COMB"
}


@router.get("/preset-options", response_model=list[str])
def get_preset_options():
    """
    설문 전에 먼저 보여줄 1차 선택지 반환
    """
    return PRESET_SKIN_TYPES


@router.get("/questions", response_model=list[SurveyQuestionOut])
def get_questions(db: Session = Depends(get_db)):
    """
    질문/선택지 목록 반환
    - order 기준으로 정렬
    """
    questions = db.query(SurveyQuestion).order_by(SurveyQuestion.order.asc()).all()
    return questions


def determine_skin_type(dry: int, oily: int, comb: int) -> str:
    """
    결과 판별 로직
    """
    if comb >= dry and comb >= oily:
        return "복합성"

    if dry >= 8 and oily <= 3:
        return "건성"

    if oily >= 8 and dry <= 3:
        return "지성"

    return "복합성"


@router.post("/submit", response_model=SurveyResultOut)
def submit_survey(
    payload: SurveySubmitIn,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    """
    설문 제출

    (추가) preset_skin_type 분기
    - preset_skin_type 이 "건성/지성/복합성" 이면: 설문 스킵 + 저장
    - preset_skin_type 이 "모르겠습니다" (또는 None)이면: 기존 설문 로직 진행
    """
    user_id = current_user.id

    # (추가) preset_skin_type 유효성 체크
    if payload.preset_skin_type is not None and payload.preset_skin_type not in PRESET_SKIN_TYPES:
        raise HTTPException(status_code=400, detail="Invalid preset_skin_type")

    
    # (추가) 사전 선택 분기: 모르겠습니다가 아니면 설문 스킵
    if payload.preset_skin_type is not None and payload.preset_skin_type != "모르겠습니다":
        # 응답 묶음 생성 (기존 테이블 그대로 사용)
        response = SurveyResponse(user_id=user_id)
        db.add(response)
        db.flush()

        # 사전 선택값을 최종 타입으로 저장 (기존 필드 사용)
        response.skin_type = payload.preset_skin_type
        response.dry_total = 0
        response.oily_total = 0
        response.comb_total = 0
        db.add(response)

        # 설문 완료 처리 (기존 처리 그대로)
        db_user = db.get(User, user_id)
        db_user.survey_completed_at = datetime.utcnow()
        
        # 🚀 유저 테이블에 영문으로 변환해서 피부 타입 저장 (기본값도 COMB!)
        db_user.skin_type = SKIN_TYPE_MAP.get(payload.preset_skin_type, "COMB")
        
        db.add(db_user)
        db.commit()

        return SurveyResultOut(
            skin_type=response.skin_type,
            scores={"dry": 0, "oily": 0, "combination": 0},
            response_id=response.id,
        )

    # (기존) answers 기반 설문 로직 그대로
    #  preset_skin_type이 None이거나 "모르겠습니다"면 answers는 반드시 1개 이상
    if payload.answers is None or len(payload.answers) < 1:
        raise HTTPException(status_code=400, detail="answers is required when preset_skin_type is '모르겠습니다' or null")

    # 응답 묶음 생성
    response = SurveyResponse(user_id=user_id)
    db.add(response)
    db.flush()

    dry_total = 0
    oily_total = 0
    comb_total = 0

    for ans in payload.answers:
        option = (
            db.query(SurveyOption)
            .filter(SurveyOption.id == ans.option_id)
            .first()
        )

        if option is None:
            raise HTTPException(
                status_code=400,
                detail=f"Invalid option_id: {ans.option_id}",
            )

        if option.question_id != ans.question_id:
            raise HTTPException(
                status_code=400,
                detail=f"option_id {ans.option_id} does not belong to question_id {ans.question_id}",
            )

        dry_total += option.dry_score
        oily_total += option.oily_score
        comb_total += option.comb_score

        db.add(
            SurveyAnswer(
                response_id=response.id,
                question_id=ans.question_id,
                option_id=ans.option_id,
            )
        )

    # 결과 판별
    skin_type = determine_skin_type(dry_total, oily_total, comb_total)

    scores = {
        "dry": dry_total,
        "oily": oily_total,
        "combination": comb_total,
    }

    # 설문 결과를 DB에도 저장
    response.skin_type = skin_type
    response.dry_total = dry_total
    response.oily_total = oily_total
    response.comb_total = comb_total
    db.add(response)

    # 설문 완료 처리
    db_user = db.get(User, user_id)
    db_user.survey_completed_at = datetime.utcnow()
    
    # 🚀 유저 테이블에 영문으로 변환해서 피부 타입 저장 (기본값도 COMB!)
    db_user.skin_type = SKIN_TYPE_MAP.get(skin_type, "COMB")
    
    db.add(db_user)
    db.commit()

    return SurveyResultOut(
        skin_type=skin_type,
        scores=scores,
        response_id=response.id,
    )