from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.api.auth import get_current_user
from app.db.session import SessionLocal
from app.models.user import User
from app.schemas.routine import (
    RoutineCreateRequest,
    RoutineItemOut,
    RoutineOut,
    RoutinePreviewRequest,
    RoutinePreviewResponse,
)
from app.services.routine_service import build_routine_preview, get_routine, save_routine

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

    return RoutineOut(
        id=routine.id,
        user_id=routine.user_id,
        routine_type=routine.routine_type,
        items=[
            RoutineItemOut(
                product_id=i.product_id,
                step_order=i.step_order,
                reason=i.reason,
            )
            for i in routine.items
        ],
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

    return RoutineOut(
        id=routine.id,
        user_id=routine.user_id,
        routine_type=routine.routine_type,
        items=[
            RoutineItemOut(
                product_id=i.product_id,
                step_order=i.step_order,
                reason=i.reason,
            )
            for i in routine.items
        ],
    )