from __future__ import annotations

from datetime import date, datetime

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.api.auth import get_current_user
from app.db.session import SessionLocal
from app.models.routine import RoutineCompletion
from app.models.user import User
from app.schemas.routine import (
    RoutineCompletionRequest,
    RoutineCompletionResponse,
    RoutineCompletionStatusResponse,
    RoutineCreateRequest,
    RoutineItemOut,
    RoutineOut,
    RoutinePreviewRequest,
    RoutinePreviewResponse,
)
from app.services.routine_service import (
    build_routine_preview,
    build_saved_routine_response,
    get_routine,
    save_routine,
)

router = APIRouter(prefix="/routines", tags=["routines"])


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def _normalize_routine_type(rt: str) -> str:
    v = (rt or "").strip().upper()
    if v not in ("AM", "PM"):
        raise ValueError("routine_type must be AM or PM")
    return v


def _dedupe_preserve_order(xs: list[int]) -> list[int]:
    seen = set()
    out: list[int] = []
    for x in xs:
        if x in seen:
            continue
        seen.add(x)
        out.append(x)
    return out


@router.post("/preview", response_model=RoutinePreviewResponse)
def preview_routine(
    payload: RoutinePreviewRequest,
    db: Session = Depends(get_db),
):
    try:
        routine_type = _normalize_routine_type(payload.routine_type)
        product_ids = _dedupe_preserve_order(payload.product_ids or [])
        items = build_routine_preview(db, routine_type, product_ids)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    return RoutinePreviewResponse(
        routine_type=routine_type,
        items=[RoutineItemOut(**it) for it in items],
    )


@router.get("/completion", response_model=RoutineCompletionStatusResponse)
def read_routine_completion(
    date_value: date = Query(..., alias="date"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    rows = (
        db.query(RoutineCompletion)
        .filter(RoutineCompletion.user_id == current_user.id)
        .filter(RoutineCompletion.date == date_value)
        .all()
    )

    status = {
        "AM": False,
        "PM": False,
    }

    for row in rows:
        rt = _normalize_routine_type(row.routine_type)
        status[rt] = bool(row.is_completed)

    return RoutineCompletionStatusResponse(
        date=date_value,
        AM=status["AM"],
        PM=status["PM"],
    )


@router.post("/completion", response_model=RoutineCompletionResponse)
def save_routine_completion(
    payload: RoutineCompletionRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    try:
        routine_type = _normalize_routine_type(payload.routine_type)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    row = (
        db.query(RoutineCompletion)
        .filter(RoutineCompletion.user_id == current_user.id)
        .filter(RoutineCompletion.date == payload.date)
        .filter(RoutineCompletion.routine_type == routine_type)
        .first()
    )

    completed_at_value = datetime.utcnow() if payload.is_completed else None

    if row:
        row.is_completed = payload.is_completed
        row.completed_at = completed_at_value
    else:
        row = RoutineCompletion(
            user_id=current_user.id,
            date=payload.date,
            routine_type=routine_type,
            is_completed=payload.is_completed,
            completed_at=completed_at_value,
        )
        db.add(row)

    db.commit()
    db.refresh(row)

    return RoutineCompletionResponse(
        date=row.date,
        routine_type=row.routine_type,
        is_completed=row.is_completed,
        completed_at=row.completed_at,
    )


@router.post("", response_model=RoutineOut)
def create_or_replace_routine(
    payload: RoutineCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    user_id = current_user.id

    try:
        routine_type = _normalize_routine_type(payload.routine_type)
        product_ids = _dedupe_preserve_order(payload.product_ids or [])
        routine = save_routine(db, user_id, routine_type, product_ids)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    data = build_saved_routine_response(db, routine)

    return RoutineOut(
        id=data["id"],
        user_id=data["user_id"],
        routine_type=data["routine_type"],
        items=[RoutineItemOut(**it) for it in data["items"]],
    )


@router.get("", response_model=RoutineOut)
def read_routine(
    routine_type: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    user_id = current_user.id

    try:
        routine_type = _normalize_routine_type(routine_type)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    routine = get_routine(db, user_id, routine_type)
    if not routine:
        raise HTTPException(status_code=404, detail="Routine not found")

    data = build_saved_routine_response(db, routine)

    return RoutineOut(
        id=data["id"],
        user_id=data["user_id"],
        routine_type=data["routine_type"],
        items=[RoutineItemOut(**it) for it in data["items"]],
    )