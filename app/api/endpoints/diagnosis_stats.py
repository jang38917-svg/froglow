from __future__ import annotations

from datetime import date, datetime, time, timedelta, timezone
from typing import Literal, Optional

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.api.auth import get_current_user
from app.db.session import SessionLocal
from app.models.diagnosis_history import DiagnosisHistory
from app.models.user import User

router = APIRouter(
    prefix="/stats",
    tags=["diagnosis-stats"],
)

KST = timezone(timedelta(hours=9))


class DiagnosisAverageItem(BaseModel):
    acne: Optional[float] = None
    redness: Optional[float] = None
    wrinkle: Optional[float] = None
    spot: Optional[float] = None


class DiagnosisStatsPoint(BaseModel):
    # weekly 응답에서 사용
    week: Optional[int] = None

    # monthly 응답에서 사용
    month: Optional[int] = None

    label: str
    start_date: date
    end_date: date

    acne: Optional[float] = None
    redness: Optional[float] = None
    wrinkle: Optional[float] = None
    spot: Optional[float] = None


class DiagnosisTrendStatsResponse(BaseModel):
    period: Literal["weekly", "monthly"]
    year: int
    month: Optional[int] = None
    start_date: date
    end_date: date
    average: DiagnosisAverageItem
    items: list[DiagnosisStatsPoint]


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def _kst_date_range_to_utc_naive(start_date: date, end_date: date) -> tuple[datetime, datetime]:
    """
    KST 기준 날짜 범위를 DB 조회용 UTC naive datetime 범위로 변환한다.

    DB의 created_at이 timezone 없는 UTC datetime으로 저장되어 있다는 전제에 맞춘다.
    """
    start_kst = datetime.combine(start_date, time.min).replace(tzinfo=KST)
    end_kst = datetime.combine(end_date + timedelta(days=1), time.min).replace(tzinfo=KST)

    start_utc = start_kst.astimezone(timezone.utc).replace(tzinfo=None)
    end_utc = end_kst.astimezone(timezone.utc).replace(tzinfo=None)

    return start_utc, end_utc


def _utc_naive_to_kst_date(value: datetime) -> date:
    """
    DB의 UTC naive datetime을 KST 날짜로 변환한다.
    """
    if value.tzinfo is None:
        value = value.replace(tzinfo=timezone.utc)

    return value.astimezone(KST).date()


def _get_month_range(year: int, month: int) -> tuple[date, date]:
    start_date = date(year, month, 1)

    if month == 12:
        next_month = date(year + 1, 1, 1)
    else:
        next_month = date(year, month + 1, 1)

    end_date = next_month - timedelta(days=1)
    return start_date, end_date


def _get_year_range(year: int) -> tuple[date, date]:
    return date(year, 1, 1), date(year, 12, 31)


def _round_average(values: list[float]) -> Optional[float]:
    if not values:
        return None

    return round(sum(values) / len(values), 2)


def _build_average(rows: list[DiagnosisHistory]) -> DiagnosisAverageItem:
    if not rows:
        return DiagnosisAverageItem(
            acne=None,
            redness=None,
            wrinkle=None,
            spot=None,
        )

    return DiagnosisAverageItem(
        acne=_round_average([row.acne_count for row in rows if row.acne_count is not None]),
        redness=_round_average([row.redness_count for row in rows if row.redness_count is not None]),
        wrinkle=_round_average([row.wrinkle_count for row in rows if row.wrinkle_count is not None]),
        spot=_round_average([row.spot_count for row in rows if row.spot_count is not None]),
    )


def _build_point_average(rows: list[DiagnosisHistory]) -> dict:
    return {
        "acne": _round_average([row.acne_count for row in rows if row.acne_count is not None]),
        "redness": _round_average([row.redness_count for row in rows if row.redness_count is not None]),
        "wrinkle": _round_average([row.wrinkle_count for row in rows if row.wrinkle_count is not None]),
        "spot": _round_average([row.spot_count for row in rows if row.spot_count is not None]),
    }


def _get_diagnosis_rows(
    db: Session,
    current_user: User,
    start_date: date,
    end_date: date,
) -> list[DiagnosisHistory]:
    start_utc, end_utc = _kst_date_range_to_utc_naive(start_date, end_date)

    return (
        db.query(DiagnosisHistory)
        .filter(DiagnosisHistory.user_id == current_user.id)
        .filter(DiagnosisHistory.created_at >= start_utc)
        .filter(DiagnosisHistory.created_at < end_utc)
        .order_by(DiagnosisHistory.created_at.asc())
        .all()
    )


def _build_weekly_trend_response(
    db: Session,
    current_user: User,
    year: int,
    month: int,
) -> DiagnosisTrendStatsResponse:
    """
    선택한 월을 1~7일, 8~14일, 15~21일, 22~28일, 29일~말일 기준으로 나누고
    각 주차별 평균 점수를 반환한다.
    """
    start_date, end_date = _get_month_range(year, month)
    rows = _get_diagnosis_rows(
        db=db,
        current_user=current_user,
        start_date=start_date,
        end_date=end_date,
    )

    items: list[DiagnosisStatsPoint] = []

    week = 1
    current_start = start_date

    while current_start <= end_date:
        current_end = min(current_start + timedelta(days=6), end_date)

        bucket_rows = []
        for row in rows:
            row_date = _utc_naive_to_kst_date(row.created_at)
            if current_start <= row_date <= current_end:
                bucket_rows.append(row)

        averages = _build_point_average(bucket_rows)

        items.append(
            DiagnosisStatsPoint(
                week=week,
                label=f"{week}주차",
                start_date=current_start,
                end_date=current_end,
                acne=averages["acne"],
                redness=averages["redness"],
                wrinkle=averages["wrinkle"],
                spot=averages["spot"],
            )
        )

        week += 1
        current_start = current_end + timedelta(days=1)

    return DiagnosisTrendStatsResponse(
        period="weekly",
        year=year,
        month=month,
        start_date=start_date,
        end_date=end_date,
        average=_build_average(rows),
        items=items,
    )


def _build_monthly_trend_response(
    db: Session,
    current_user: User,
    year: int,
) -> DiagnosisTrendStatsResponse:
    """
    선택한 연도의 1월~12월 월별 평균 점수를 반환한다.
    """
    start_date, end_date = _get_year_range(year)
    rows = _get_diagnosis_rows(
        db=db,
        current_user=current_user,
        start_date=start_date,
        end_date=end_date,
    )

    items: list[DiagnosisStatsPoint] = []

    for month in range(1, 13):
        month_start, month_end = _get_month_range(year, month)

        bucket_rows = []
        for row in rows:
            row_date = _utc_naive_to_kst_date(row.created_at)
            if month_start <= row_date <= month_end:
                bucket_rows.append(row)

        averages = _build_point_average(bucket_rows)

        items.append(
            DiagnosisStatsPoint(
                month=month,
                label=f"{month}월",
                start_date=month_start,
                end_date=month_end,
                acne=averages["acne"],
                redness=averages["redness"],
                wrinkle=averages["wrinkle"],
                spot=averages["spot"],
            )
        )

    return DiagnosisTrendStatsResponse(
        period="monthly",
        year=year,
        month=None,
        start_date=start_date,
        end_date=end_date,
        average=_build_average(rows),
        items=items,
    )


@router.get("/weekly", response_model=DiagnosisTrendStatsResponse)
def get_weekly_stats(
    year: int = Query(..., ge=2000, le=2100),
    month: int = Query(..., ge=1, le=12),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return _build_weekly_trend_response(
        db=db,
        current_user=current_user,
        year=year,
        month=month,
    )


@router.get("/monthly", response_model=DiagnosisTrendStatsResponse)
def get_monthly_stats(
    year: int = Query(..., ge=2000, le=2100),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return _build_monthly_trend_response(
        db=db,
        current_user=current_user,
        year=year,
    )