from __future__ import annotations

from datetime import date, datetime, time, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.auth import get_current_user
from app.db.session import SessionLocal
from app.models.diagnosis_history import DiagnosisHistory
from app.models.user import User
from app.schemas.diagnosis_calendar import (
    DiagnosisAverageItem,
    DiagnosisStatsResponse,
)

router = APIRouter(
    prefix="/stats",
    tags=["diagnosis-stats"],
)

KST = timezone(timedelta(hours=9))


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def _build_stats_response(
    db: Session,
    current_user: User,
    start_date: date,
    end_date: date,
    period: str,
) -> DiagnosisStatsResponse:
    start_kst = datetime.combine(start_date, time.min).replace(tzinfo=KST)
    end_kst = datetime.combine(end_date + timedelta(days=1), time.min).replace(tzinfo=KST)

    start_utc = start_kst.astimezone(timezone.utc).replace(tzinfo=None)
    end_utc = end_kst.astimezone(timezone.utc).replace(tzinfo=None)

    rows = (
        db.query(DiagnosisHistory)
        .filter(DiagnosisHistory.user_id == current_user.id)
        .filter(DiagnosisHistory.created_at >= start_utc)
        .filter(DiagnosisHistory.created_at < end_utc)
        .all()
    )

    if not rows:
        return DiagnosisStatsResponse(
            period=period,
            start_date=start_date,
            end_date=end_date,
            average=DiagnosisAverageItem(
                acne=0.0,
                redness=0.0,
                wrinkle=0.0,
                spot=0.0,
            ),
        )

    acne_avg = round(sum(row.acne_count for row in rows) / len(rows), 2)
    redness_avg = round(sum(row.redness_count for row in rows) / len(rows), 2)
    wrinkle_avg = round(sum(row.wrinkle_count for row in rows) / len(rows), 2)
    spot_avg = round(sum(row.spot_count for row in rows) / len(rows), 2)

    return DiagnosisStatsResponse(
        period=period,
        start_date=start_date,
        end_date=end_date,
        average=DiagnosisAverageItem(
            acne=acne_avg,
            redness=redness_avg,
            wrinkle=wrinkle_avg,
            spot=spot_avg,
        ),
    )


def _get_week_range(target_date: date) -> tuple[date, date]:
    start_date = target_date - timedelta(days=target_date.weekday())
    end_date = start_date + timedelta(days=6)
    return start_date, end_date


def _get_month_range(year: int, month: int) -> tuple[date, date]:
    start_date = date(year, month, 1)

    if month == 12:
        next_month = date(year + 1, 1, 1)
    else:
        next_month = date(year, month + 1, 1)

    end_date = next_month - timedelta(days=1)
    return start_date, end_date


@router.get("/weekly", response_model=DiagnosisStatsResponse)
def get_weekly_stats(
    date_value: date = Query(..., alias="date"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    start_date, end_date = _get_week_range(date_value)

    return _build_stats_response(
        db=db,
        current_user=current_user,
        start_date=start_date,
        end_date=end_date,
        period="weekly",
    )


@router.get("/monthly", response_model=DiagnosisStatsResponse)
def get_monthly_stats(
    year: int = Query(..., ge=2000, le=2100),
    month: int = Query(..., ge=1, le=12),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    start_date, end_date = _get_month_range(year, month)

    return _build_stats_response(
        db=db,
        current_user=current_user,
        start_date=start_date,
        end_date=end_date,
        period="monthly",
    )