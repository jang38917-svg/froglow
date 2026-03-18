# app/db/backfill_product_grades.py
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.product import Product
from app.core.grading import infer_grades


def _all_grades_are_default_c(p: Product) -> bool:
    return (
        (p.acne_grade == "C")
        and (p.redness_grade == "C")
        and (p.spot_grade == "C")
        and (p.wrinkle_grade == "C")
    )


def main(batch_size: int = 1000):
    db: Session = SessionLocal()
    try:
        q = db.query(Product).filter(Product.is_active.is_(True))
        total = q.count()

        offset = 0
        changed_total = 0

        while True:
            items = (
                q.order_by(Product.product_id.asc())
                .offset(offset)
                .limit(batch_size)
                .all()
            )
            if not items:
                break

            changed_in_batch = 0

            for p in items:
                # ✅ 안전장치:
                # 이미 퍼져있는 등급(A/B/D/E 등)이 하나라도 있으면 절대 건드리지 않음
                # "전부 C"인 경우만 재계산(=현재 C 몰림 문제 대상만)
                if not _all_grades_are_default_c(p):
                    continue

                grades = infer_grades(p.product_name or "")

                p.acne_grade = grades.get("acne") or "C"
                p.redness_grade = grades.get("redness") or "C"
                p.wrinkle_grade = grades.get("wrinkle") or "C"
                p.spot_grade = grades.get("spot") or "C"

                changed_in_batch += 1

            db.commit()
            changed_total += changed_in_batch

            offset += batch_size
            print(
                "updated",
                min(offset, total),
                "/",
                total,
                "| changed_in_batch",
                changed_in_batch,
                "| changed_total",
                changed_total,
            )

        print("done", "| changed_total", changed_total)
    finally:
        db.close()


if __name__ == "__main__":
    main()