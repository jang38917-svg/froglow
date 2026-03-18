from __future__ import annotations

import re
from typing import List, Optional, Literal

from fastapi import APIRouter, Depends, Query, HTTPException
from pydantic import BaseModel, ConfigDict
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.product import Product
from app.models.product_skin_type import ProductSkinType

router = APIRouter(prefix="/products", tags=["products"])

SkinType = Literal["DRY", "OILY", "COMB"]

# ✅ JSON 파싱을 깨는 제어문자 제거용 (0x00~0x1F, 0x7F)
_CTRL_RE = re.compile(r"[\x00-\x1F\x7F]")


def _clean_text(v: Optional[str]) -> Optional[str]:
    if v is None:
        return None
    s = str(v)
    s = _CTRL_RE.sub("", s)
    return s.strip()


class ProductOut(BaseModel):
    product_id: int
    product_name: str
    product_image_url: Optional[str] = None

    acne_grade: str
    redness_grade: str
    wrinkle_grade: str
    spot_grade: str

    recommend_score: int
    is_active: bool

    formulation: Optional[str] = None
    concerns: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)


def _to_out(p: Product) -> ProductOut:
    return ProductOut(
        product_id=int(p.product_id),
        product_name=_clean_text(p.product_name) or "",
        product_image_url=_clean_text(p.product_image_url),
        acne_grade=_clean_text(p.acne_grade) or "C",
        redness_grade=_clean_text(p.redness_grade) or "C",
        wrinkle_grade=_clean_text(p.wrinkle_grade) or "C",
        spot_grade=_clean_text(p.spot_grade) or "C",
        recommend_score=int(p.recommend_score),
        is_active=bool(p.is_active),
        formulation=_clean_text(p.formulation),
        concerns=_clean_text(p.concerns),
    )


@router.get(
    "",
    response_model=List[ProductOut],
    response_model_exclude_none=False,
)
def list_products(
    skin_type: Optional[SkinType] = Query(default=None, description="DRY / OILY / COMB"),
    active_only: bool = Query(default=True, description="true면 is_active만"),
    formulations: Optional[str] = Query(
        default=None,
        description="제형 필터. 예) MOISTURE_CREAM,TONER,SERUM (comma separated)",
    ),
    concerns: Optional[str] = Query(
        default=None,
        description="피부고민 필터. 예) redness,acne,spot,wrinkle (comma separated)",
    ),
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    db: Session = Depends(get_db),
):
    q = db.query(Product)

    if active_only:
        q = q.filter(Product.is_active == True)  # noqa: E712

    if skin_type:
        q = q.join(ProductSkinType, ProductSkinType.product_id == Product.product_id)
        q = q.filter(ProductSkinType.skin_type == skin_type)

    if formulations:
        wanted_forms = [f.strip().upper() for f in formulations.split(",") if f.strip()]
        if wanted_forms:
            q = q.filter(Product.formulation.in_(wanted_forms))

    if concerns:
        wanted = [c.strip().lower() for c in concerns.split(",") if c.strip()]
        for c in wanted:
            q = q.filter(Product.concerns.ilike(f"%{c}%"))

    q = q.order_by(Product.recommend_score.desc(), Product.product_id.asc())
    q = q.offset(offset).limit(limit)

    rows = q.all()
    return [_to_out(p) for p in rows]


@router.get("/{product_id}", response_model=ProductOut, response_model_exclude_none=False)
def get_product(product_id: int, db: Session = Depends(get_db)):
    product = db.query(Product).filter(Product.product_id == product_id).one_or_none()
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    return _to_out(product)