from __future__ import annotations

from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, JSON, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class DiagnosisHistory(Base):
    """
    피부 진단 결과 및 분석 시 사용된 이미지 경로 히스토리
    """
    __tablename__ = "diagnosis_histories"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)

    user_id: Mapped[int] = mapped_column(
        Integer,
        ForeignKey("users.id"),
        nullable=False,
        index=True,
    )

    acne_count: Mapped[int] = mapped_column(Integer, default=0)
    redness_count: Mapped[int] = mapped_column(Integer, default=0)
    wrinkle_count: Mapped[int] = mapped_column(Integer, default=0)
    spot_count: Mapped[int] = mapped_column(Integer, default=0)

    primary_concern: Mapped[str] = mapped_column(String(50), nullable=False)

    folder_id: Mapped[str] = mapped_column(String(20), nullable=False)
    front_image_path: Mapped[str] = mapped_column(String(255), nullable=True)
    left_image_path: Mapped[str] = mapped_column(String(255), nullable=True)
    right_image_path: Mapped[str] = mapped_column(String(255), nullable=True)

    routine_snapshot: Mapped[dict | None] = mapped_column(JSON, nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        nullable=False,
        default=datetime.utcnow,
    )