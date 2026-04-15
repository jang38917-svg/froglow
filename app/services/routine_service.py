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


def _sort_routine_items(routine_type: str, items: list[dict]) -> list[dict]:
    sorted_items = sorted(
        items,
        key=lambda x: (
            _get_step_priority(routine_type, x.get("step_name")),
            x.get("_original_order", 9999),
        ),
    )

    result = []
    for idx, item in enumerate(sorted_items, start=1):
        new_item = {
            "product_id": item["product_id"],
            "step_order": idx,
            "reason": item.get("reason"),
            "product_name": item.get("product_name"),
            "step_name": item.get("step_name"),
        }
        result.append(new_item)

    return result


def build_routine_preview(db: Session, routine_type: str, product_ids: list[int]):
    if not product_ids:
        return []

    products = (
        db.query(Product.product_id, Product.product_name, Product.formulation)
        .filter(Product.product_id.in_(product_ids))
        .all()
    )
    product_map = {
        p.product_id: {
            "product_name": p.product_name,
            "formulation": p.formulation,
        }
        for p in products
    }

    raw_items = []
    for idx, product_id in enumerate(product_ids, start=1):
        product_info = product_map.get(product_id, {})
        product_name = product_info.get("product_name")
        formulation = product_info.get("formulation")
        step_name = _get_step_name(
            routine_type,
            idx,
            product_name=product_name,
            formulation=formulation,
        )

        raw_items.append(
            {
                "product_id": product_id,
                "step_order": idx,
                "_original_order": idx,
                "reason": None,
                "product_name": product_name,
                "step_name": step_name,
            }
        )

    return _sort_routine_items(routine_type, raw_items)


def build_saved_routine_response(db: Session, routine: Routine):
    if not routine:
        return None

    rows = (
        db.query(RoutineItem, Product.product_name, Product.formulation)
        .outerjoin(Product, Product.product_id == RoutineItem.product_id)
        .filter(RoutineItem.routine_id == routine.id)
        .order_by(RoutineItem.step_order.asc())
        .all()
    )

    raw_items = []
    for item, product_name, formulation in rows:
        step_name = _get_step_name(
            routine.routine_type,
            item.step_order,
            product_name=product_name,
            formulation=formulation,
        )

        raw_items.append(
            {
                "product_id": item.product_id,
                "step_order": item.step_order,
                "_original_order": item.step_order,
                "reason": item.reason if item.reason else None,
                "product_name": product_name,
                "step_name": step_name,
            }
        )

    sorted_items = _sort_routine_items(routine.routine_type, raw_items)

    return {
        "id": routine.id,
        "user_id": routine.user_id,
        "routine_type": routine.routine_type,
        "items": sorted_items,
    }


def _sort_product_ids_for_save(db: Session, routine_type: str, product_ids: list[int]) -> list[int]:
    if not product_ids:
        return product_ids

    products = (
        db.query(Product.product_id, Product.product_name, Product.formulation)
        .filter(Product.product_id.in_(product_ids))
        .all()
    )
    product_map = {
        p.product_id: {
            "product_name": p.product_name,
            "formulation": p.formulation,
        }
        for p in products
    }

    raw_items = []
    for idx, product_id in enumerate(product_ids, start=1):
        product_info = product_map.get(product_id, {})
        product_name = product_info.get("product_name")
        formulation = product_info.get("formulation")
        step_name = _get_step_name(
            routine_type,
            idx,
            product_name=product_name,
            formulation=formulation,
        )

        raw_items.append(
            {
                "product_id": product_id,
                "_original_order": idx,
                "step_name": step_name,
            }
        )

    sorted_items = sorted(
        raw_items,
        key=lambda x: (
            _get_step_priority(routine_type, x.get("step_name")),
            x.get("_original_order", 9999),
        ),
    )

    return [item["product_id"] for item in sorted_items]


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
        sorted_product_ids = _sort_product_ids_for_save(db, routine_type, product_ids)

        for product_id in sorted_product_ids:
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
        items = step.get("items", [])

        item_product_ids = []
        for item in items:
            product_id = item.get("product_id")
            if product_id is None:
                continue
            item_product_ids.append(product_id)

        sorted_product_ids = _sort_product_ids_for_save(db, routine_type, item_product_ids)

        for product_id in sorted_product_ids:
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