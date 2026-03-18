from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
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


# ✅ 임시: 인증 붙이기 전까지 user_id를 1로 처리
def get_current_user_id() -> int:
    return 1


@router.get("", response_model=list[ReminderOut])
def get_reminders(db: Session = Depends(get_db)):
    user_id = get_current_user_id()
    reminders = list_reminders(db, user_id)
    return [
        ReminderOut(
            id=r.id,
            user_id=r.user_id,
            product_id=r.product_id,
            remind_time=r.remind_time,
            timezone=r.timezone,
            label=r.label,
            is_active=r.is_active,
        )
        for r in reminders
    ]


@router.post("", response_model=ReminderOut)
def post_reminder(payload: ReminderCreateRequest, db: Session = Depends(get_db)):
    user_id = get_current_user_id()
    r = create_reminder(
        db=db,
        user_id=user_id,
        product_id=payload.product_id,
        remind_time=payload.remind_time,
        timezone=payload.timezone,
        label=payload.label,
        is_active=payload.is_active,
    )
    return ReminderOut(
        id=r.id,
        user_id=r.user_id,
        product_id=r.product_id,
        remind_time=r.remind_time,
        timezone=r.timezone,
        label=r.label,
        is_active=r.is_active,
    )


@router.patch("/{reminder_id}", response_model=ReminderOut)
def patch_reminder(reminder_id: int, payload: ReminderUpdateRequest, db: Session = Depends(get_db)):
    user_id = get_current_user_id()
    r = update_reminder(
        db=db,
        user_id=user_id,
        reminder_id=reminder_id,
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
        product_id=r.product_id,
        remind_time=r.remind_time,
        timezone=r.timezone,
        label=r.label,
        is_active=r.is_active,
    )


@router.delete("/{reminder_id}")
def delete_reminder_api(reminder_id: int, db: Session = Depends(get_db)):
    user_id = get_current_user_id()
    ok = delete_reminder(db, user_id, reminder_id)
    if not ok:
        raise HTTPException(status_code=404, detail="Reminder not found")
    return {"ok": True}