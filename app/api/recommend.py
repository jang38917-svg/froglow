from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.services.recommend_service import recommend_products

router = APIRouter(prefix="/products", tags=["products"])

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@router.get("/recommend")
def recommend(
    skin_type: str = Query(..., description="DRY / OILY / COMB"),
    acne_score: float = Query(..., ge=0.0, le=10.0),
    spots_score: float = Query(..., ge=0.0, le=10.0),
    redness_score: float = Query(..., ge=0.0, le=10.0),
    wrinkles_score: float = Query(..., ge=0.0, le=10.0),
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    skin_type = skin_type.strip().upper()
    if skin_type not in ("DRY", "OILY", "COMB"):
        return {"detail": "skin_type은 DRY / OILY / COMB 중 하나여야 함"}

    return recommend_products(
        db,
        skin_type=skin_type,
        acne_score=acne_score,
        spots_score=spots_score,
        redness_score=redness_score,
        wrinkles_score=wrinkles_score,
        limit=limit
    )
