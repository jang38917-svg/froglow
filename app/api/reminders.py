from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.api.auth import get_current_user
from app.db.session import SessionLocal
from app.models.user import User
from app.schemas.reminder import ReminderCreateRequest, ReminderOut, ReminderUpdateRequest
from app.services.reminder_service import (
    create_reminder,
    delete_reminder,
    list_reminders,
    update_reminder,
)

router = APIRouter(prefix="/reminders", tags=["reminders"])


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@router.get("", response_model=list[ReminderOut])
def get_reminders(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    reminders = list_reminders(db, current_user.id)
    return [
        ReminderOut(
            id=r.id,
            user_id=r.user_id,
            reminder_type=r.reminder_type,
            routine_key=r.routine_key,
            frequency=r.frequency,
            interval_hours=r.interval_hours,
            remind_time=r.remind_time,
            timezone=r.timezone,
            label=r.label,
            is_active=r.is_active,
        )
        for r in reminders
    ]


@router.post("", response_model=ReminderOut)
def post_reminder(
    payload: ReminderCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if payload.reminder_type == "PRODUCT" and not payload.label:
        raise HTTPException(status_code=400, detail="label is required for PRODUCT reminder")

    if payload.reminder_type == "ROUTINE" and payload.routine_key not in {"AM", "PM"}:
        raise HTTPException(status_code=400, detail="routine_key is required for ROUTINE reminder")

    try:
        r = create_reminder(
            db=db,
            user_id=current_user.id,
            reminder_type=payload.reminder_type,
            routine_key=payload.routine_key,
            frequency=payload.frequency,
            interval_hours=payload.interval_hours,
            remind_time=payload.remind_time,
            timezone=payload.timezone,
            label=payload.label,
            is_active=payload.is_active,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    return ReminderOut(
        id=r.id,
        user_id=r.user_id,
        reminder_type=r.reminder_type,
        routine_key=r.routine_key,
        frequency=r.frequency,
        interval_hours=r.interval_hours,
        remind_time=r.remind_time,
        timezone=r.timezone,
        label=r.label,
        is_active=r.is_active,
    )


@router.patch("/{reminder_id}", response_model=ReminderOut)
def patch_reminder(
    reminder_id: int,
    payload: ReminderUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    r = update_reminder(
        db=db,
        user_id=current_user.id,
        reminder_id=reminder_id,
        reminder_type=payload.reminder_type,
        routine_key=payload.routine_key,
        frequency=payload.frequency,
        interval_hours=payload.interval_hours,
        remind_time=payload.remind_time,
        timezone=payload.timezone,
        label=payload.label,
        is_active=payload.is_active,
    )
    if not r:
        raise HTTPException(status_code=404, detail="Reminder not found")

    return ReminderOut(
        id=r.id,
        user_id=r.user_id,
        reminder_type=r.reminder_type,
        routine_key=r.routine_key,
        frequency=r.frequency,
        interval_hours=r.interval_hours,
        remind_time=r.remind_time,
        timezone=r.timezone,
        label=r.label,
        is_active=r.is_active,
    )


@router.delete("/{reminder_id}")
def delete_reminder_api(
    reminder_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ok = delete_reminder(db, current_user.id, reminder_id)
    if not ok:
        raise HTTPException(status_code=404, detail="Reminder not found")
    return {"ok": True}