from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field, field_validator


ALLOWED_REMINDER_TYPES = {"PRODUCT", "ROUTINE"}
ALLOWED_FREQUENCIES = {"DAILY", "EVERY_N_HOURS"}
ALLOWED_ROUTINE_KEYS = {"AM", "PM"}


def _normalize_hhmm(v: str) -> str:
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
    reminder_type: str = Field(default="PRODUCT", description="PRODUCT or ROUTINE")
    routine_key: Optional[str] = Field(default=None, description="AM or PM for ROUTINE")
    frequency: str = Field(default="DAILY", description="DAILY or EVERY_N_HOURS")
    interval_hours: Optional[int] = None
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

    @field_validator("reminder_type")
    @classmethod
    def validate_reminder_type(cls, v: str) -> str:
        s = (v or "").strip().upper()
        if s not in ALLOWED_REMINDER_TYPES:
            raise ValueError("reminder_type must be PRODUCT or ROUTINE")
        return s

    @field_validator("routine_key")
    @classmethod
    def validate_routine_key(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return None
        s = v.strip().upper()
        if s not in ALLOWED_ROUTINE_KEYS:
            raise ValueError("routine_key must be AM or PM")
        return s

    @field_validator("frequency")
    @classmethod
    def validate_frequency(cls, v: str) -> str:
        s = (v or "").strip().upper()
        if s not in ALLOWED_FREQUENCIES:
            raise ValueError("frequency must be DAILY or EVERY_N_HOURS")
        return s

    @field_validator("interval_hours")
    @classmethod
    def validate_interval_hours(cls, v: Optional[int]) -> Optional[int]:
        if v is None:
            return None
        if v < 1 or v > 24:
            raise ValueError("interval_hours must be between 1 and 24")
        return v


class ReminderUpdateRequest(BaseModel):
    reminder_type: Optional[str] = None
    routine_key: Optional[str] = None
    frequency: Optional[str] = None
    interval_hours: Optional[int] = None
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

    @field_validator("reminder_type")
    @classmethod
    def validate_reminder_type(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return None
        s = v.strip().upper()
        if s not in ALLOWED_REMINDER_TYPES:
            raise ValueError("reminder_type must be PRODUCT or ROUTINE")
        return s

    @field_validator("routine_key")
    @classmethod
    def validate_routine_key(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return None
        s = v.strip().upper()
        if s not in ALLOWED_ROUTINE_KEYS:
            raise ValueError("routine_key must be AM or PM")
        return s

    @field_validator("frequency")
    @classmethod
    def validate_frequency(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return None
        s = v.strip().upper()
        if s not in ALLOWED_FREQUENCIES:
            raise ValueError("frequency must be DAILY or EVERY_N_HOURS")
        return s

    @field_validator("interval_hours")
    @classmethod
    def validate_interval_hours(cls, v: Optional[int]) -> Optional[int]:
        if v is None:
            return None
        if v < 1 or v > 24:
            raise ValueError("interval_hours must be between 1 and 24")
        return v


class ReminderOut(BaseModel):
    id: int
    user_id: int
    reminder_type: str
    routine_key: Optional[str] = None
    frequency: str
    interval_hours: Optional[int] = None
    remind_time: str
    timezone: str
    label: Optional[str] = None
    is_active: bool