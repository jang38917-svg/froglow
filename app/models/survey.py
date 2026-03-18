"""
피부 타입(건성/지성/복합성) 설문을 위한 DB 테이블 정의 파일

구성
1) SurveyQuestion : 질문
2) SurveyOption   : 선택지 (+ 점수: 건성/지성/복합성)
3) SurveyResponse : 사용자가 설문을 1번 제출한 "묶음"
4) SurveyAnswer   : 제출 묶음 안에서, 어떤 질문에 어떤 선택지를 골랐는지 기록

"""

from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, func
from sqlalchemy.orm import relationship

from app.db.base import Base


class SurveyQuestion(Base):
    """
    설문 질문 테이블
    """
    __tablename__ = "survey_questions"

    id = Column(Integer, primary_key=True, index=True)

    # 질문 텍스트 (예: "세안 후 아무것도 바르지 않았을 때 피부 느낌은?")
    text = Column(String, nullable=False)

    # 질문 노출 순서 (1, 2, 3...)
    order = Column(Integer, nullable=False, default=1)

    # 한 질문은 여러 선택지를 가짐
    # cascade 옵션: 질문이 삭제되면 그에 속한 선택지도 함께 삭제
    options = relationship(
        "SurveyOption",
        back_populates="question",
        cascade="all, delete-orphan",
    )


class SurveyOption(Base):
    """
    설문 선택지 테이블
    - 사용자가 선택지를 고르면 해당 점수를 누적해서 결과 타입을 판단합니다.
    """
    __tablename__ = "survey_options"

    id = Column(Integer, primary_key=True, index=True)

    # 이 선택지가 어느 질문에 속하는지 (FK)
    question_id = Column(
        Integer,
        ForeignKey("survey_questions.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    # 선택지 텍스트 (예: "얼굴 전체가 심하게 당기고 건조하다")
    text = Column(String, nullable=False)

    # 점수(가중치)
    # - 해당 선택지를 선택하면 아래 점수
    dry_score = Column(Integer, nullable=False, default=0)
    oily_score = Column(Integer, nullable=False, default=0)
    comb_score = Column(Integer, nullable=False, default=0)

    # 관계 설정 (선택지 → 질문)
    question = relationship("SurveyQuestion", back_populates="options")


class SurveyResponse(Base):
    """
    사용자가 설문을 '한 번' 제출한 기록(응답 묶음)
    - 로그인 연동이 되어 있으므로 user_id는 반드시 존재 (nullable=False)
    """
    __tablename__ = "survey_responses"

    id = Column(Integer, primary_key=True, index=True)

    # 로그인 연동 전제:
    # user_id는 반드시 존재해야 함.
    # 아래 ForeignKey의 "users.id"에서 users는 '유저 테이블 이름'입니다.
    # 만약 너희 유저 테이블명이 다르면 (예: "user", "members", "app_users") 여기만 바꿔줘!
    user_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    # 제출 시간
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    #  설문 결과 저장용 컬럼 (추가)
    # - 추천 로직에서 최신 설문 결과를 빠르게 조회하기 위함
    skin_type = Column(String, nullable=True)  # "건성" / "지성" / "복합성"
    dry_total = Column(Integer, nullable=False, default=0)
    oily_total = Column(Integer, nullable=False, default=0)
    comb_total = Column(Integer, nullable=False, default=0)

    # 한 번의 제출은 여러 답변(Answer)을 가짐
    answers = relationship(
        "SurveyAnswer",
        back_populates="response",
        cascade="all, delete-orphan",
    )


class SurveyAnswer(Base):
    """
    한 번의 제출(SurveyResponse) 안에서,
    '어떤 질문(question_id)에 어떤 선택지(option_id)를 골랐는지' 기록

    - response_id: 어떤 설문 제출(묶음)인지
    - question_id: 어떤 질문인지
    - option_id  : 어떤 선택지를 골랐는지
    """
    __tablename__ = "survey_answers"

    id = Column(Integer, primary_key=True, index=True)

    response_id = Column(
        Integer,
        ForeignKey("survey_responses.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    question_id = Column(
        Integer,
        ForeignKey("survey_questions.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    option_id = Column(
        Integer,
        ForeignKey("survey_options.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    # 관계 설정 (답변 → 제출)
    response = relationship("SurveyResponse", back_populates="answers")
