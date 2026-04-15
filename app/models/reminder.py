from __future__ import annotations

from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class Reminder(Base):
    """
    립밤/핸드크림/미스트/아침 루틴/저녁 루틴 알림
    """
    __tablename__ = "reminders"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)

    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    # PRODUCT / ROUTINE
    reminder_type: Mapped[str] = mapped_column(String(20), nullable=False, default="PRODUCT", index=True)

    # ROUTINE일 때만 사용: AM / PM
    routine_key: Mapped[str | None] = mapped_column(String(10), nullable=True, index=True)

    # DAILY / EVERY_N_HOURS
    frequency: Mapped[str] = mapped_column(String(30), nullable=False, default="DAILY")

    interval_hours: Mapped[int | None] = mapped_column(Integer, nullable=True)

    remind_time: Mapped[str] = mapped_column(String(5), nullable=False)

    timezone: Mapped[str] = mapped_column(String(64), nullable=False, default="Asia/Seoul")

    # "립밤", "핸드크림", "미스트", "아침 루틴 체크", "저녁 루틴 체크"
    label: Mapped[str | None] = mapped_column(String(100), nullable=True)

    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )