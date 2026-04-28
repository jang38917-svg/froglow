from __future__ import annotations

import os
from datetime import date, datetime, time, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.auth import get_current_user
from app.db.session import SessionLocal
from app.models.diagnosis_history import DiagnosisHistory
from app.models.product import Product
from app.models.routine import Routine, RoutineCompletion, RoutineItem
from app.models.user import User
from app.schemas.diagnosis_calendar import (
    DiagnosisCalendarDailyResponse,
    DiagnosisCalendarItem,
    DiagnosisCalendarDateItem,
    DiagnosisCalendarDatesResponse,
    DiagnosisImages,
    DiagnosisImageItem,
    DiagnosisResult,
    DiagnosisRoutineCompletion,
    DiagnosisRoutineGroup,
    DiagnosisRoutineItem,
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


def _normalize_text(value: str | None) -> str:
    return (value or "").strip().lower()


def _contains_any(text: str, keywords: list[str]) -> bool:
    return any(keyword in text for keyword in keywords)


def _detect_step_name_from_product(product_name: str | None, formulation: str | None) -> str | None:
    name = _normalize_text(product_name)
    form = _normalize_text(formulation)
    text = f"{form} {name}".strip()

    if not text:
        return None

    if _contains_any(text, ["선크림", "썬크림", "선블록", "선블럭", "sunscreen", "sun cream", "sunblock", "uv"]):
        return "선크림"

    if _contains_any(text, ["마스크팩", "마스크 팩", "슬리핑팩", "수면팩", "mask pack", "sleeping pack"]):
        return "마스크팩"

    if _contains_any(text, ["패치", "patch"]):
        return "패치"

    if _contains_any(text, ["크림", "cream", "gel cream", "gelcream"]):
        return "크림"

    if _contains_any(text, ["에멀전", "emulsion", "로션", "lotion"]):
        return "에멀전"

    if _contains_any(text, ["앰플", "ampoule"]):
        return "앰플"

    if _contains_any(text, ["에센스", "essence"]):
        return "에센스"

    if _contains_any(text, ["세럼", "serum"]):
        return "세럼"

    if _contains_any(text, ["토너패드", "toner pad", "pad"]):
        return "토너"

    if _contains_any(text, ["스킨 소프너", "skin softener"]):
        return "토너"

    if _contains_any(text, ["토너", "스킨토너", "toner"]):
        return "토너"

    if _contains_any(text, ["스킨", "skin"]):
        return "토너"

    return None


def _get_default_step_name(routine_type: str, step_order: int) -> str:
    rt = (routine_type or "").upper()

    if rt == "AM":
        step_map = {
            1: "토너",
            2: "세럼",
            3: "크림",
            4: "선크림",
        }
        return step_map.get(step_order, f"{step_order}단계")

    if rt == "PM":
        step_map = {
            1: "토너",
            2: "세럼",
            3: "크림",
            4: "나이트케어",
        }
        return step_map.get(step_order, f"{step_order}단계")

    return f"{step_order}단계"


def _get_step_name(
    routine_type: str,
    step_order: int,
    product_name: str | None = None,
    formulation: str | None = None,
) -> str:
    detected = _detect_step_name_from_product(product_name, formulation)
    if detected:
        return detected

    return _get_default_step_name(routine_type, step_order)


def _get_step_priority(routine_type: str, step_name: str | None) -> int:
    rt = (routine_type or "").upper()
    name = (step_name or "").strip()

    if name == "토너":
        return 10
    if name == "앰플":
        return 20
    if name == "에센스":
        return 21
    if name == "세럼":
        return 22
    if name == "에멀전":
        return 30
    if name == "크림":
        return 31
    if name == "패치":
        return 35

    if rt == "AM":
        if name == "선크림":
            return 40
        if name == "마스크팩":
            return 50
    elif rt == "PM":
        if name == "마스크팩":
            return 40
        if name == "선크림":
            return 50

    return 999


def _build_routine_items(db: Session, user_id: int, routine_type: str) -> list[DiagnosisRoutineItem]:
    routine = (
        db.query(Routine)
        .filter(Routine.user_id == user_id, Routine.routine_type == routine_type)
        .first()
    )

    if not routine:
        return []

    rows = (
        db.query(
            RoutineItem.product_id,
            RoutineItem.step_order,
            Product.product_name,
            Product.formulation,
            Product.product_image_url,
        )
        .outerjoin(Product, Product.product_id == RoutineItem.product_id)
        .filter(RoutineItem.routine_id == routine.id)
        .order_by(RoutineItem.step_order.asc())
        .all()
    )

    raw_items = []
    for product_id, step_order, product_name, formulation, product_image_url in rows:
        step_name = _get_step_name(
            routine_type,
            step_order,
            product_name=product_name,
            formulation=formulation,
        )

        raw_items.append(
            {
                "product_id": product_id,
                "step_order": step_order,
                "_original_order": step_order,
                "product_name": product_name,
                "step_name": step_name,
                "product_image_url": product_image_url,
            }
        )

    sorted_items = sorted(
        raw_items,
        key=lambda x: (
            _get_step_priority(routine_type, x.get("step_name")),
            x.get("_original_order", 9999),
        ),
    )

    result = []
    for idx, item in enumerate(sorted_items, start=1):
        result.append(
            DiagnosisRoutineItem(
                product_id=item["product_id"],
                product_name=item.get("product_name"),
                step_order=idx,
                step_name=item.get("step_name"),
                product_image_url=item.get("product_image_url"),
            )
        )

    return result


def _build_routine_group_from_snapshot(snapshot: dict | None) -> DiagnosisRoutineGroup | None:
    if not snapshot:
        return None

    result = {
        "AM": [],
        "PM": [],
    }

    for routine_type in ["AM", "PM"]:
        items = snapshot.get(routine_type, []) or []

        for item in items:
            product_id = item.get("product_id")
            if product_id is None:
                continue

            result[routine_type].append(
                DiagnosisRoutineItem(
                    product_id=product_id,
                    product_name=item.get("product_name"),
                    step_order=item.get("step_order", len(result[routine_type]) + 1),
                    step_name=item.get("step_name"),
                    product_image_url=item.get("product_image_url"),
                )
            )

    return DiagnosisRoutineGroup(
        AM=result["AM"],
        PM=result["PM"],
    )


def _build_current_routine_group(db: Session, user_id: int) -> DiagnosisRoutineGroup:
    return DiagnosisRoutineGroup(
        AM=_build_routine_items(db, user_id, "AM"),
        PM=_build_routine_items(db, user_id, "PM"),
    )


def _build_routine_completion(
    db: Session,
    user_id: int,
    date_value: date,
) -> DiagnosisRoutineCompletion:
    rows = (
        db.query(RoutineCompletion)
        .filter(RoutineCompletion.user_id == user_id)
        .filter(RoutineCompletion.date == date_value)
        .all()
    )

    status = {
        "AM": False,
        "PM": False,
    }

    for row in rows:
        routine_type = (row.routine_type or "").strip().upper()
        if routine_type in status:
            status[routine_type] = bool(row.is_completed)

    return DiagnosisRoutineCompletion(
        AM=status["AM"],
        PM=status["PM"],
    )


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

    routine_group = None

    if rows:
        routine_group = _build_routine_group_from_snapshot(rows[0].routine_snapshot)

    if routine_group is None:
        routine_group = _build_current_routine_group(db, current_user.id)

    routine_completion = _build_routine_completion(
        db=db,
        user_id=current_user.id,
        date_value=date_value,
    )

    return DiagnosisCalendarDailyResponse(
        date=date_value,
        items=items,
        routine=routine_group,
        routine_completion=routine_completion,
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