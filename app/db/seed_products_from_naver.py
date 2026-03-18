import os
import re
import html
import argparse
import time
from typing import Iterable, Optional

import requests
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.product import Product
from app.models.product_skin_type import ProductSkinType
from app.core.grading import infer_grades  

from dotenv import load_dotenv
load_dotenv()

NAVER_URL = "https://openapi.naver.com/v1/search/shop.json"

def _headers() -> dict:
    cid = os.getenv("NAVER_CLIENT_ID", "").strip()
    sec = os.getenv("NAVER_CLIENT_SECRET", "").strip()
    if not cid or not sec:
        raise RuntimeError("NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 환경변수가 비어있음")
    return {"X-Naver-Client-Id": cid, "X-Naver-Client-Secret": sec}

_TAG_RE = re.compile(r"<[^>]+>")

def clean_title(title: str) -> str:
    no_tags = _TAG_RE.sub("", title or "")
    return html.unescape(no_tags).strip()

def extract_product_id(item: dict) -> Optional[int]:
    for k in ("productId", "mallProductId"):
        v = item.get(k)
        if v is None: continue
        try: return int(str(v).strip())
        except ValueError: continue
    return None

def fetch_page(query: str, display: int, start: int, sort: str = "sim") -> list[dict]:
    try:
        r = requests.get(
            NAVER_URL,
            headers=_headers(),
            params={"query": query, "display": display, "start": start, "sort": sort},
            timeout=30,
        )
        r.raise_for_status()
        return r.json().get("items", [])
    except Exception as e:
        print(f"API 호출 에러 ({query}): {e}")
        return []

# ✅ [중요] 추천 서비스의 target_forms와 100% 일치하도록 키워드 보강
FORMULATION_KEYWORDS = [
    ("SUN_CREAM", ["선크림", "선블록", "선젤", "자외선차단", "sunscreen", "sunblock"]),
    ("MASK_PACK", ["마스크팩", "시트마스크", "팩", "mask pack", "sheet mask"]),
    ("MOISTURE_CREAM", ["수분크림", "보습크림", "영양크림", "재생크림", "시카크림", "moisture cream", "cream"]),
    ("LOTION", ["로션", "에멀젼", "에멀전", "lotion", "emulsion"]),
    ("AMPOULE", ["앰플", "ampoule"]),
    ("SERUM", ["세럼", "serum"]),
    ("ESSENCE", ["에센스", "essence"]),
    ("TONER", ["토너", "스킨", "패드", "toner", "skin", "pad"]),
]

CONCERN_KEYWORDS = [
    ("redness", ["홍조", "열감", "민감", "예민", "진정", "시카", "병풀", "cica"]),
    ("acne", ["여드름", "트러블", "아크네", "피지", "모공", "각질"]),
    ("spot", ["잡티", "미백", "기미", "브라이트닝", "화이트닝"]),
    ("wrinkle", ["주름", "탄력", "리프팅", "에이징", "안티에이징", "레티놀"]),
]

def infer_formulation(query: str, title: str, category: Optional[str]) -> Optional[str]:
    text = f"{query} {title} {category or ''}".lower()
    for form, kws in FORMULATION_KEYWORDS:
        if any(k.lower() in text for k in kws):
            return form
    return "ETC"

def infer_concerns(query: str, title: str, category: Optional[str]) -> Optional[str]:
    text = f"{query} {title} {category or ''}".lower()
    hits = []
    for tag, kws in CONCERN_KEYWORDS:
        if any(k.lower() in text for k in kws):
            hits.append(tag)
    return ",".join(list(set(hits))) if hits else None

def build_category_name(item: dict) -> Optional[str]:
    cat = " > ".join(filter(None, [item.get(f"category{i}") for i in range(1, 5)]))
    return cat or None

def _all_grades_are_default_c(p: Product) -> bool:
    return all(getattr(p, f"{attr}_grade") == "C" for attr in ["acne", "redness", "spot", "wrinkle"])

def upsert_product(db: Session, **kwargs) -> bool:
    pid = kwargs['product_id']
    name = kwargs['product_name']
    price = kwargs.get('price')
    formulation = kwargs.get('formulation')
    
    exists = db.query(Product).filter((Product.product_id == pid) | (Product.product_name == name)).first()

    # 등급 추론 (기존 로직 유지)
    inferred = infer_grades(product_name=name, category_text=kwargs.get('category_text') or "", price=price)

    if exists:
        changed = False
        if price is not None and exists.price != str(price):
            exists.price = str(price)
            changed = True
        if formulation and not exists.formulation:
            exists.formulation = formulation
            changed = True
        if _all_grades_are_default_c(exists) or changed:
            exists.acne_grade = inferred["acne"] or "C"
            exists.redness_grade = inferred["redness"] or "C"
            exists.wrinkle_grade = inferred["wrinkle"] or "C"
            exists.spot_grade = inferred["spot"] or "C"
            changed = True
        if changed: db.add(exists)
        return False

    p = Product(
        product_id=pid,
        product_name=name,
        product_image_url=kwargs.get('product_image_url'),
        acne_grade=inferred["acne"] or "C",
        redness_grade=inferred["redness"] or "C",
        wrinkle_grade=inferred["wrinkle"] or "C",
        spot_grade=inferred["spot"] or "C",
        formulation=formulation,
        concerns=kwargs.get('concerns'),
        price=str(price) if price else None,
    )
    db.add(p)
    return True

def attach_skin_types(db: Session, product_id: int, skin_types: Iterable[str]) -> int:
    inserted = 0
    for st in skin_types:
        st = st.strip().upper()
        if not db.query(ProductSkinType).filter_by(product_id=product_id, skin_type=st).first():
            db.add(ProductSkinType(product_id=product_id, skin_type=st))
            inserted += 1
    return inserted

def seed_from_naver(query: str, pages: int = 2, display: int = 50, attach_all_skin_types: bool = True):
    db = SessionLocal()
    try:
        total_inserted = 0
        skin_types = ("DRY", "OILY", "COMB", "SENSITIVE") if attach_all_skin_types else ()
        
        for page_idx in range(pages):
            items = fetch_page(query=query, display=display, start=1 + page_idx * display)
            for item in items:
                pid = extract_product_id(item)
                if not pid: continue
                
                name = clean_title(item.get("title", ""))
                price = int(item.get("lprice")) if item.get("lprice", "").isdigit() else None
                cat = build_category_name(item)
                
                inserted = upsert_product(
                    db, product_id=pid, product_name=name,
                    product_image_url=item.get("image"),
                    formulation=infer_formulation(query, name, cat),
                    concerns=infer_concerns(query, name, cat),
                    category_text=cat, price=price
                )
                if inserted:
                    total_inserted += 1
                    attach_skin_types(db, pid, skin_types)
            db.commit()
        print(f"✅ {query}: {total_inserted}개 신규 저장 완료")
    finally:
        db.close()

def main():
    # ✅ 루틴 모든 단계를 채우기 위한 기본 검색어 세트
    ROUTINE_QUERIES = [
        "진정 토너", "수분 앰플", "보습 크림", "선크림", "마스크팩"
    ]
    
    print("🚀 루틴 전단계 데이터 수집 시작...")
    for q in ROUTINE_QUERIES:
        seed_from_naver(query=q, pages=3, display=100, attach_all_skin_types=True)
        time.sleep(0.5) # API 속도 제한 방지
    print("🏁 모든 데이터 수집 완료!")

if __name__ == "__main__":
    main()