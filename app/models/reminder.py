from __future__ import annotations

from datetime import datetime

# BigInteger가 이미 임포트되어 있었네요! 그대로 사용합니다.
from sqlalchemy import BigInteger, Boolean, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class Reminder(Base):
    """
    미스트/핸드크림 등 사용자가 지정한 시간에 바르도록 하는 리마인더
    """
    __tablename__ = "reminders"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)

    # users 테이블 PK 참조
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    # [수정 완료] Integer -> BigInteger로 변경하여 11자리 이상의 큰 상품 ID도 수용 가능하게 함
    product_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("products.product_id"), nullable=False, index=True)

    # "HH:MM" 문자열 저장
    remind_time: Mapped[str] = mapped_column(String(5), nullable=False)

    # 예: "Asia/Seoul"
    timezone: Mapped[str] = mapped_column(String(64), nullable=False, default="Asia/Seoul")

    # "미스트", "핸드크림" 같은 라벨(옵션)
    label: Mapped[str] = mapped_column(String(100), nullable=True)

    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )