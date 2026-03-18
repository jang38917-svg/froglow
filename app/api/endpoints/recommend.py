# app/api/endpoints/recommend.py
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.db.session import SessionLocal

# 🌟 개발자님이 짜두신 전설의 서비스 로직 임포트!
from app.services.recommend_service import recommend_products

router = APIRouter()

# DB 세션 의존성 (기존에 쓰시던 get_db가 있다면 그걸 임포트해 쓰셔도 됩니다)
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# 프론트엔드(또는 AI 모델 결과)에서 넘어올 데이터 형식
class RecommendRequest(BaseModel):
    skin_type: str        # 예: "DRY", "OILY", "COMB"
    acne_score: float     # 예: 8.5 (10점 만점 기준)
    spots_score: float
    redness_score: float
    wrinkles_score: float
    limit: int = 20       # 기본값 20개

@router.post("/routine")
def get_skin_care_routine(req: RecommendRequest, db: Session = Depends(get_db)):
    result = recommend_products(
        db=db,
        skin_type=req.skin_type,
        acne_score=req.acne_score,
        spots_score=req.spots_score,
        redness_score=req.redness_score,
        wrinkles_score=req.wrinkles_score,
        limit=req.limit
    )
    return result