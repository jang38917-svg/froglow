from __future__ import annotations

from datetime import date, datetime
from typing import List, Optional

from pydantic import BaseModel


class DiagnosisImageItem(BaseModel):
    original: Optional[str] = None
    annotated: Optional[str] = None


class DiagnosisImages(BaseModel):
    front: DiagnosisImageItem
    left: DiagnosisImageItem
    right: DiagnosisImageItem


class DiagnosisResult(BaseModel):
    acne: int
    redness: int
    wrinkle: int
    spot: int


class DiagnosisCalendarItem(BaseModel):
    id: int
    created_at: datetime
    primary_concern: str
    folder_id: str
    images: DiagnosisImages
    diagnosis: DiagnosisResult


class DiagnosisRoutineItem(BaseModel):
    product_id: int
    product_name: Optional[str] = None
    step_order: int
    step_name: Optional[str] = None
    product_image_url: Optional[str] = None


class DiagnosisRoutineGroup(BaseModel):
    AM: List[DiagnosisRoutineItem]
    PM: List[DiagnosisRoutineItem]


class DiagnosisRoutineCompletion(BaseModel):
    AM: bool
    PM: bool


class DiagnosisCalendarDailyResponse(BaseModel):
    date: date
    items: List[DiagnosisCalendarItem]
    routine: DiagnosisRoutineGroup
    routine_completion: DiagnosisRoutineCompletion


class DiagnosisCalendarDateItem(BaseModel):
    date: date
    count: int


class DiagnosisCalendarDatesResponse(BaseModel):
    year: int
    month: int
    dates: List[DiagnosisCalendarDateItem]


class DiagnosisAverageItem(BaseModel):
    acne: float
    redness: float
    wrinkle: float
    spot: float


class DiagnosisStatsResponse(BaseModel):
    period: str
    start_date: date
    end_date: date
    average: DiagnosisAverageItem