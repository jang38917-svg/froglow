from __future__ import annotations

import os
from datetime import date, datetime, time, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.auth import get_current_user
from app.db.session import SessionLocal
from app.models.diagnosis_history import DiagnosisHistory
from app.models.user import User
from app.schemas.diagnosis_calendar import (
    DiagnosisCalendarDailyResponse,
    DiagnosisCalendarItem,
    DiagnosisCalendarDateItem,
    DiagnosisCalendarDatesResponse,
    DiagnosisImages,
    DiagnosisImageItem,
    DiagnosisResult,
)

router = APIRouter(
    prefix="/calendar",
    tags=["diagnosis-calendar"],
)

UPLOAD_DIR = "data/uploads"
KST = timezone(timedelta(hours=9))


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def _to_kst(dt: datetime) -> datetime:
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(KST)


def _build_annotated_path(folder_id: str, direction: str) -> str | None:
    abs_path = os.path.join(UPLOAD_DIR, folder_id, f"{direction}_annotated.jpg")
    if os.path.exists(abs_path):
        return f"/uploads/{folder_id}/{direction}_annotated.jpg"
    return None


def _build_original_path(folder_id: str, direction: str) -> str | None:
    abs_path = os.path.join(UPLOAD_DIR, folder_id, f"{direction}.jpg")
    if os.path.exists(abs_path):
        return f"/uploads/{folder_id}/{direction}.jpg"
    return None


@router.get("/daily", response_model=DiagnosisCalendarDailyResponse)
def get_daily_diagnosis_histories(
    date_value: date = Query(..., alias="date"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    start_kst = datetime.combine(date_value, time.min).replace(tzinfo=KST)
    end_kst = start_kst + timedelta(days=1)

    start_utc = start_kst.astimezone(timezone.utc).replace(tzinfo=None)
    end_utc = end_kst.astimezone(timezone.utc).replace(tzinfo=None)

    rows = (
        db.query(DiagnosisHistory)
        .filter(DiagnosisHistory.user_id == current_user.id)
        .filter(DiagnosisHistory.created_at >= start_utc)
        .filter(DiagnosisHistory.created_at < end_utc)
        .order_by(DiagnosisHistory.created_at.desc())
        .all()
    )

    items = []
    for row in rows:
        items.append(
            DiagnosisCalendarItem(
                id=row.id,
                created_at=_to_kst(row.created_at),
                primary_concern=row.primary_concern,
                folder_id=row.folder_id,
                images=DiagnosisImages(
                    front=DiagnosisImageItem(
                        original=_build_original_path(row.folder_id, "front"),
                        annotated=_build_annotated_path(row.folder_id, "front"),
                    ),
                    left=DiagnosisImageItem(
                        original=_build_original_path(row.folder_id, "left"),
                        annotated=_build_annotated_path(row.folder_id, "left"),
                    ),
                    right=DiagnosisImageItem(
                        original=_build_original_path(row.folder_id, "right"),
                        annotated=_build_annotated_path(row.folder_id, "right"),
                    ),
                ),
                diagnosis=DiagnosisResult(
                    acne=row.acne_count,
                    redness=row.redness_count,
                    wrinkle=row.wrinkle_count,
                    spot=row.spot_count,
                ),
            )
        )

    return DiagnosisCalendarDailyResponse(
        date=date_value,
        items=items,
    )


@router.get("/dates", response_model=DiagnosisCalendarDatesResponse)
def get_calendar_dates(
    year: int = Query(..., ge=2000, le=2100),
    month: int = Query(..., ge=1, le=12),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    start_kst = datetime(year, month, 1, 0, 0, 0, tzinfo=KST)

    if month == 12:
        end_kst = datetime(year + 1, 1, 1, 0, 0, 0, tzinfo=KST)
    else:
        end_kst = datetime(year, month + 1, 1, 0, 0, 0, tzinfo=KST)

    start_utc = start_kst.astimezone(timezone.utc).replace(tzinfo=None)
    end_utc = end_kst.astimezone(timezone.utc).replace(tzinfo=None)

    rows = (
        db.query(DiagnosisHistory.created_at)
        .filter(DiagnosisHistory.user_id == current_user.id)
        .filter(DiagnosisHistory.created_at >= start_utc)
        .filter(DiagnosisHistory.created_at < end_utc)
        .all()
    )

    count_by_date: dict[date, int] = {}

    for (created_at,) in rows:
        local_date = _to_kst(created_at).date()
        count_by_date[local_date] = count_by_date.get(local_date, 0) + 1

    result_dates = [
        DiagnosisCalendarDateItem(date=target_date, count=count)
        for target_date, count in sorted(count_by_date.items())
    ]

    return DiagnosisCalendarDatesResponse(
        year=year,
        month=month,
        dates=result_dates,
    )