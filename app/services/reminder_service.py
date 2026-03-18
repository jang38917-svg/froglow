from __future__ import annotations

from typing import List, Optional

from sqlalchemy.orm import Session

from app.models.reminder import Reminder


def list_reminders(db: Session, user_id: int) -> List[Reminder]:
    return (
        db.query(Reminder)
        .filter(Reminder.user_id == user_id)
        .order_by(Reminder.id.desc())
        .all()
    )


def create_reminder(
    db: Session,
    user_id: int,
    product_id: int,
    remind_time: str,
    timezone: str,
    label: Optional[str],
    is_active: bool,
) -> Reminder:
    reminder = Reminder(
        user_id=user_id,
        product_id=product_id,
        remind_time=remind_time,
        timezone=timezone,
        label=label,
        is_active=is_active,
    )
    db.add(reminder)
    db.commit()
    db.refresh(reminder)
    return reminder


def update_reminder(
    db: Session,
    user_id: int,
    reminder_id: int,
    remind_time: Optional[str] = None,
    timezone: Optional[str] = None,
    label: Optional[str] = None,
    is_active: Optional[bool] = None,
) -> Reminder | None:
    reminder = (
        db.query(Reminder)
        .filter(Reminder.id == reminder_id, Reminder.user_id == user_id)
        .first()
    )
    if not reminder:
        return None

    if remind_time is not None:
        reminder.remind_time = remind_time
    if timezone is not None:
        reminder.timezone = timezone
    if label is not None:
        reminder.label = label
    if is_active is not None:
        reminder.is_active = is_active

    db.commit()
    db.refresh(reminder)
    return reminder


def delete_reminder(db: Session, user_id: int, reminder_id: int) -> bool:
    reminder = (
        db.query(Reminder)
        .filter(Reminder.id == reminder_id, Reminder.user_id == user_id)
        .first()
    )
    if not reminder:
        return False
    db.delete(reminder)
    db.commit()
    return True