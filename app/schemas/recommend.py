# app/schemas/recommend.py
from pydantic import BaseModel
from typing import Dict, List, Optional

class AIScore(BaseModel):
    acne: int
    redness: int
    wrinkle: int
    spot: int

class RoutineRequest(BaseModel):
    skin_type: str
    ai_scores: AIScore

class ProductRecommend(BaseModel):
    product_id: int
    name: str
    # 👇 [수정] String으로 바뀐 DB 가격을 에러 없이 받아주기 위해 str로 변경!
    price: Optional[str] 
    grade: str
    image_url: Optional[str]

class RoutineStep(BaseModel):
    step_name: str
    recommended_products: List[ProductRecommend]

class RoutineResponse(BaseModel):
    main_concern: str
    skin_type: str
    routine: List[RoutineStep]