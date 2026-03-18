from __future__ import annotations
from datetime import datetime
from typing import List
from sqlalchemy import BigInteger, DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.db.base import Base

class Routine(Base):
    __tablename__ = "routines"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    routine_type: Mapped[str] = mapped_column(String(2), nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    items: Mapped[List["RoutineItem"]] = relationship(
        "RoutineItem", back_populates="routine", cascade="all, delete-orphan", order_by="RoutineItem.step_order.asc()"
    )

class RoutineItem(Base):
    __tablename__ = "routine_items"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    routine_id: Mapped[int] = mapped_column(Integer, ForeignKey("routines.id"), nullable=False, index=True)
    
    # 🚀 570억대 숫자를 위해 BigInteger 사용
    product_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("products.product_id"), nullable=False, index=True)
    
    step_order: Mapped[int] = mapped_column(Integer, nullable=False, index=True)
    reason: Mapped[str] = mapped_column(Text, nullable=True)
    routine: Mapped["Routine"] = relationship("Routine", back_populates="items")