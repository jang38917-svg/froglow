from __future__ import annotations

from datetime import datetime
from typing import List

from sqlalchemy.orm import Session

from app.models.product import Product
from app.models.routine import Routine, RoutineItem


def get_routine(db: Session, user_id: int, routine_type: str):
    return db.query(Routine).filter(
        Routine.user_id == user_id,
        Routine.routine_type == routine_type
    ).first()


def _get_step_name(routine_type: str, step_order: int) -> str:
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


def build_routine_preview(db: Session, routine_type: str, product_ids: list[int]):
    if not product_ids:
        return []

    products = (
        db.query(Product.product_id, Product.product_name)
        .filter(Product.product_id.in_(product_ids))
        .all()
    )
    product_name_map = {p.product_id: p.product_name for p in products}

    items = []
    for idx, product_id in enumerate(product_ids, start=1):
        items.append(
            {
                "product_id": product_id,
                "step_order": idx,
                "reason": None,
                "product_name": product_name_map.get(product_id),
                "step_name": _get_step_name(routine_type, idx),
            }
        )

    return items


def build_saved_routine_response(db: Session, routine: Routine):
    if not routine:
        return None

    rows = (
        db.query(RoutineItem, Product.product_name)
        .outerjoin(Product, Product.product_id == RoutineItem.product_id)
        .filter(RoutineItem.routine_id == routine.id)
        .order_by(RoutineItem.step_order.asc())
        .all()
    )

    return {
        "id": routine.id,
        "user_id": routine.user_id,
        "routine_type": routine.routine_type,
        "items": [
            {
                "product_id": item.product_id,
                "step_order": item.step_order,
                "reason": item.reason if item.reason else None,
                "product_name": product_name,
                "step_name": _get_step_name(routine.routine_type, item.step_order),
            }
            for item, product_name in rows
        ],
    }


def save_routine(db: Session, user_id: int, routine_type: str, product_ids: list):
    existing_routines = (
        db.query(Routine)
        .filter(Routine.user_id == user_id, Routine.routine_type == routine_type)
        .all()
    )

    for routine in existing_routines:
        db.query(RoutineItem).filter(RoutineItem.routine_id == routine.id).delete()
        db.delete(routine)

    db.commit()

    new_routine = Routine(
        user_id=user_id,
        routine_type=routine_type,
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow(),
    )
    db.add(new_routine)
    db.flush()

    current_order = 1

    if not product_ids:
        db.commit()
        db.refresh(new_routine)
        return new_routine

    if isinstance(product_ids[0], int):
        for product_id in product_ids:
            new_item = RoutineItem(
                routine_id=new_routine.id,
                product_id=product_id,
                step_order=current_order,
                reason="",
            )
            db.add(new_item)
            current_order += 1

        db.commit()
        db.refresh(new_routine)
        return new_routine

    for step in product_ids:
        if not isinstance(step, dict):
            continue

        reason_text = step.get("description", "")
        for item in step.get("items", []):
            product_id = item.get("product_id")
            if product_id is None:
                continue

            new_item = RoutineItem(
                routine_id=new_routine.id,
                product_id=product_id,
                step_order=current_order,
                reason=reason_text,
            )
            db.add(new_item)
            current_order += 1

    db.commit()
    db.refresh(new_routine)
    return new_routine