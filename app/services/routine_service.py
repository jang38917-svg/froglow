from sqlalchemy.orm import Session
from app.models.routine import Routine, RoutineItem
from datetime import datetime

def get_routine(db: Session, user_id: int, routine_type: str):
    return db.query(Routine).filter(Routine.user_id == user_id, Routine.routine_type == routine_type).first()

def build_routine_preview(db: Session, routine: Routine):
    if not routine:
        return None

    items = (
        db.query(RoutineItem)
        .filter(RoutineItem.routine_id == routine.id)
        .order_by(RoutineItem.step_order)
        .all()
    )

    return {
        "id": routine.id,
        "type": routine.routine_type,
        "items": [
            {
                "product_id": item.product_id,
                "step_order": item.step_order,
                "reason": item.reason,
            }
            for item in items
        ],
        "updated_at": routine.updated_at,
    }

def save_routine(db: Session, user_id: int, routine_type: str, product_ids: list):
    # 기존 유저 루틴 조회
    existing_routines = (
        db.query(Routine)
        .filter(Routine.user_id == user_id, Routine.routine_type == routine_type)
        .all()
    )

    # 기존 루틴에 연결된 routine_items 먼저 삭제
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
    for step in product_ids:
        reason_text = step.get("description", "")
        for item in step.get("items", []):
            new_item = RoutineItem(
                routine_id=new_routine.id,
                product_id=item["product_id"],
                step_order=current_order,
                reason=reason_text,
            )
            db.add(new_item)
            current_order += 1

    db.commit()
    return new_routine