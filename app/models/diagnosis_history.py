from __future__ import annotations
from datetime import datetime
from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column
from app.db.base import Base

class DiagnosisHistory(Base):
    """
    피부 진단 결과 및 분석 시 사용된 이미지 경로 히스토리
    """
    __tablename__ = "diagnosis_histories"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    
    # users 테이블 PK 연결 (기존 user 모델에 맞춰 id 또는 user_id로 수정 가능)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    # AI 분석 점수 (YOLO에서 나온 카운트)
    acne_count: Mapped[int] = mapped_column(Integer, default=0)
    redness_count: Mapped[int] = mapped_column(Integer, default=0)
    wrinkle_count: Mapped[int] = mapped_column(Integer, default=0)
    spot_count: Mapped[int] = mapped_column(Integer, default=0)

    # 종합 결과
    primary_concern: Mapped[str] = mapped_column(String(50), nullable=False)
    
    # 이미지 저장 정보
    folder_id: Mapped[str] = mapped_column(String(20), nullable=False) # uuid 조각
    front_image_path: Mapped[str] = mapped_column(String(255), nullable=True)
    left_image_path: Mapped[str] = mapped_column(String(255), nullable=True)
    right_image_path: Mapped[str] = mapped_column(String(255), nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)

    # (옵션) 유저 모델과 관계 설정이 필요하면 추가
    # user = relationship("User", back_populates="diagnosis_histories")