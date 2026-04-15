from __future__ import annotations

from typing import List, Optional

from sqlalchemy.orm import Session

from app.models.reminder import Reminder


DEFAULT_ROUTINE_TIMEZONE = "Asia/Seoul"
DEFAULT_ROUTINE_TYPE = "ROUTINE"
DEFAULT_ROUTINE_FREQUENCY = "DAILY"

DEFAULT_AM_ROUTINE_KEY = "AM"
DEFAULT_PM_ROUTINE_KEY = "PM"

DEFAULT_AM_ROUTINE_LABEL = "아침 루틴 체크"
DEFAULT_PM_ROUTINE_LABEL = "저녁 루틴 체크"

DEFAULT_AM_ROUTINE_TIME = "09:00"
DEFAULT_PM_ROUTINE_TIME = "00:00"


def _get_routine_reminder(db: Session, user_id: int, routine_key: str) -> Reminder | None:
    return (
        db.query(Reminder)
        .filter(
            Reminder.user_id == user_id,
            Reminder.reminder_type == DEFAULT_ROUTINE_TYPE,
            Reminder.routine_key == routine_key,
        )
        .order_by(Reminder.id.asc())
        .first()
    )


def _get_default_routine_label(routine_key: str) -> str:
    if routine_key == DEFAULT_PM_ROUTINE_KEY:
        return DEFAULT_PM_ROUTINE_LABEL
    return DEFAULT_AM_ROUTINE_LABEL


def _get_default_routine_time(routine_key: str) -> str:
    if routine_key == DEFAULT_PM_ROUTINE_KEY:
        return DEFAULT_PM_ROUTINE_TIME
    return DEFAULT_AM_ROUTINE_TIME


def ensure_default_routine_reminders(db: Session, user_id: int) -> None:
    am = _get_routine_reminder(db, user_id, DEFAULT_AM_ROUTINE_KEY)
    pm = _get_routine_reminder(db, user_id, DEFAULT_PM_ROUTINE_KEY)

    created = False

    if not am:
        db.add(
            Reminder(
                user_id=user_id,
                reminder_type=DEFAULT_ROUTINE_TYPE,
                routine_key=DEFAULT_AM_ROUTINE_KEY,
                frequency=DEFAULT_ROUTINE_FREQUENCY,
                interval_hours=None,
                remind_time=DEFAULT_AM_ROUTINE_TIME,
                timezone=DEFAULT_ROUTINE_TIMEZONE,
                label=DEFAULT_AM_ROUTINE_LABEL,
                is_active=True,
            )
        )
        created = True

    if not pm:
        db.add(
            Reminder(
                user_id=user_id,
                reminder_type=DEFAULT_ROUTINE_TYPE,
                routine_key=DEFAULT_PM_ROUTINE_KEY,
                frequency=DEFAULT_ROUTINE_FREQUENCY,
                interval_hours=None,
                remind_time=DEFAULT_PM_ROUTINE_TIME,
                timezone=DEFAULT_ROUTINE_TIMEZONE,
                label=DEFAULT_PM_ROUTINE_LABEL,
                is_active=True,
            )
        )
        created = True

    if created:
        db.commit()


def list_reminders(db: Session, user_id: int) -> List[Reminder]:
    ensure_default_routine_reminders(db, user_id)

    return (
        db.query(Reminder)
        .filter(Reminder.user_id == user_id)
        .order_by(Reminder.id.desc())
        .all()
    )


def create_reminder(
    db: Session,
    user_id: int,
    reminder_type: str,
    routine_key: Optional[str],
    frequency: str,
    interval_hours: Optional[int],
    remind_time: str,
    timezone: str,
    label: Optional[str],
    is_active: bool,
) -> Reminder:
    if reminder_type == "ROUTINE":
        if routine_key not in {"AM", "PM"}:
            raise ValueError("routine_key is required for ROUTINE reminder")

        existing = _get_routine_reminder(db, user_id, routine_key)
        if existing:
            existing.frequency = frequency
            existing.interval_hours = interval_hours if frequency == "EVERY_N_HOURS" else None
            existing.remind_time = remind_time
            existing.timezone = timezone
            existing.label = label or _get_default_routine_label(routine_key)
            existing.is_active = is_active
            db.commit()
            db.refresh(existing)
            return existing

        reminder = Reminder(
            user_id=user_id,
            reminder_type="ROUTINE",
            routine_key=routine_key,
            frequency=frequency,
            interval_hours=interval_hours if frequency == "EVERY_N_HOURS" else None,
            remind_time=remind_time,
            timezone=timezone,
            label=label or _get_default_routine_label(routine_key),
            is_active=is_active,
        )
        db.add(reminder)
        db.commit()
        db.refresh(reminder)
        return reminder

    reminder = Reminder(
        user_id=user_id,
        reminder_type="PRODUCT",
        routine_key=None,
        frequency=frequency,
        interval_hours=interval_hours if frequency == "EVERY_N_HOURS" else None,
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
    reminder_type: Optional[str] = None,
    routine_key: Optional[str] = None,
    frequency: Optional[str] = None,
    interval_hours: Optional[int] = None,
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

    next_type = reminder.reminder_type if reminder_type is None else reminder_type
    next_frequency = reminder.frequency if frequency is None else frequency
    next_routine_key = reminder.routine_key if routine_key is None else routine_key

    if reminder_type is not None:
        reminder.reminder_type = reminder_type

    if routine_key is not None:
        reminder.routine_key = routine_key

    if next_type == "PRODUCT":
        reminder.routine_key = None

    if frequency is not None:
        reminder.frequency = frequency

    if next_frequency == "EVERY_N_HOURS":
        if interval_hours is not None:
            reminder.interval_hours = interval_hours
    else:
        reminder.interval_hours = None

    if remind_time is not None:
        reminder.remind_time = remind_time
    if timezone is not None:
        reminder.timezone = timezone
    if label is not None:
        reminder.label = label
    if is_active is not None:
        reminder.is_active = is_active

    if next_type == "ROUTINE" and not reminder.label:
        reminder.label = _get_default_routine_label(next_routine_key or "AM")

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

    ensure_default_routine_reminders(db, user_id)
    return True