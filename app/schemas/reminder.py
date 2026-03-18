from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field, field_validator


def _normalize_hhmm(v: str) -> str:
    # "HH:MM" 00:00 ~ 23:59
    if v is None:
        raise ValueError("remind_time must be 'HH:MM'")
    s = str(v).strip()
    try:
        hh, mm = s.split(":")
        h = int(hh)
        m = int(mm)
    except Exception:
        raise ValueError("remind_time must be 'HH:MM'")

    if not (0 <= h <= 23 and 0 <= m <= 59):
        raise ValueError("remind_time must be between 00:00 and 23:59")

    return f"{h:02d}:{m:02d}"


class ReminderCreateRequest(BaseModel):
    product_id: int
    remind_time: str = Field(..., description="HH:MM (24h)")
    timezone: str = Field(default="Asia/Seoul")
    label: Optional[str] = None
    is_active: bool = True

    @field_validator("remind_time")
    @classmethod
    def validate_remind_time(cls, v: str) -> str:
        return _normalize_hhmm(v)

    @field_validator("timezone")
    @classmethod
    def validate_timezone(cls, v: str) -> str:
        s = (v or "").strip()
        if not s:
            raise ValueError("timezone cannot be empty")
        return s


class ReminderUpdateRequest(BaseModel):
    remind_time: Optional[str] = Field(default=None, description="HH:MM (24h)")
    timezone: Optional[str] = None
    label: Optional[str] = None
    is_active: Optional[bool] = None

    @field_validator("remind_time")
    @classmethod
    def validate_remind_time(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return None
        return _normalize_hhmm(v)

    @field_validator("timezone")
    @classmethod
    def validate_timezone(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return None
        s = v.strip()
        if not s:
            raise ValueError("timezone cannot be empty")
        return s


class ReminderOut(BaseModel):
    id: int
    user_id: int
    product_id: int
    remind_time: str
    timezone: str
    label: Optional[str] = None
    is_active: bool