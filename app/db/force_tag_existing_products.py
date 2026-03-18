from app.db.session import SessionLocal
from app.models.product import Product
from app.db.seed_products_from_naver import (
    infer_formulation,
    infer_concerns,
)

def main():
    db = SessionLocal()
    try:
        products = db.query(Product).all()
        updated = 0

        for p in products:
            name = p.product_name or ""

            formulation = infer_formulation(name, name, None)
            concerns = infer_concerns(name, name, None)

            p.formulation = formulation
            p.concerns = concerns
            db.add(p)
            updated += 1

        db.commit()
        print(f"FORCE_UPDATED={updated}")
    finally:
        db.close()

if __name__ == "__main__":
    main()
