"""
설문 API에서 사용하는 요청/응답 스키마(Pydantic 모델) 모음

- /survey/questions 응답 형태
- /survey/submit 요청/응답 형태
"""

from pydantic import BaseModel, Field
from typing import List, Dict, Optional


class SurveyOptionOut(BaseModel):
    # 질문의 선택지 응답 형태
    id: int
    text: str

    class Config:
        from_attributes = True  # SQLAlchemy ORM 객체 -> Pydantic 변환 허용


class SurveyQuestionOut(BaseModel):
    # 질문 응답 형태 (선택지 포함)
    id: int
    text: str
    order: int
    options: List[SurveyOptionOut]

    class Config:
        from_attributes = True


class SurveyAnswerIn(BaseModel):
    """
    설문 제출 시: 한 문항에 대한 답(질문ID + 선택지ID)
    """
    question_id: int
    option_id: int


class SurveySubmitIn(BaseModel):
    """
    설문 제출 요청 payload
    - user_id는 받지 않음 (서버가 로그인된 user에서 가져옴)

    (추가)
    preset_skin_type:
    - "건성" / "지성" / "복합성" / "모르겠습니다"
    - "모르겠습니다" 또는 None 인 경우에만 answers 설문 진행
    """
    preset_skin_type: Optional[str] = None

    # 기존 설문 답변 (모르겠습니다일 때만 사용됨)
    answers: List[SurveyAnswerIn] = Field(default_factory=list)


class SurveyResultOut(BaseModel):
    # 설문 제출 결과 응답
    skin_type: str
    scores: Dict[str, int]
    response_id: int
