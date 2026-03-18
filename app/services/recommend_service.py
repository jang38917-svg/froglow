from __future__ import annotations

import re
from sqlalchemy.orm import Session
from sqlalchemy import func

from app.models.product import Product
from app.models.product_skin_type import ProductSkinType
from app.utils.grade import score_to_grade

_GRADE_SCORE = {"A": 5, "B": 4, "C": 3, "D": 2, "E": 1}
_BANNED_NAME_KEYWORDS = ["연고", "사마귀", "비립종", "물사마귀", "처방"]

# --- 보조 함수 (기존 로직 유지) ---

def _split_tags(value: str | None) -> list[str]:
    if not value:
        return []
    raw = str(value).replace("|", ",").replace("/", ",")
    return [t.strip().lower() for t in raw.split(",") if t.strip()]

def _severity_weight(score_0_to_10: float) -> int:
    if score_0_to_10 >= 8: return 3
    if score_0_to_10 >= 6: return 2
    if score_0_to_10 >= 4: return 1
    return 0

def _formulation_bonus(skin_type: str, formulation_tags: list[str]) -> int:
    if not formulation_tags: return 0
    preferred = {
        "DRY": {
            "best": {"cream", "balm", "oil", "ointment", "rich", "butter", "moisture_cream"},
            "ok": {"lotion", "essence", "ampoule", "serum", "emulsion", "toner", "skin"},
            "avoid": {"gel", "water", "matte", "sebum", "oil-control"},
        },
        "OILY": {
            "best": {"gel", "water", "light", "fluid", "lotion", "essence"},
            "ok": {"serum", "ampoule", "emulsion", "toner", "skin"},
            "avoid": {"oil", "balm", "ointment", "rich", "butter", "moisture_cream"},
        },
        "COMB": {
            "best": {"lotion", "gel", "essence", "emulsion"},
            "ok": {"cream", "serum", "ampoule", "toner", "skin", "moisture_cream"},
            "avoid": {"ointment", "butter"},
        },
    }
    rules = preferred.get(skin_type, {"best": set(), "ok": set(), "avoid": set()})
    bonus = 0
    for t in formulation_tags:
        if t in rules["best"]: bonus = max(bonus, 2)
        elif t in rules["ok"]: bonus = max(bonus, 1)
        elif t in rules["avoid"]: bonus = min(bonus, -1)
    return bonus

def _grade_quality_bonus(product: Product) -> int:
    grades = [product.acne_grade, product.spot_grade, product.redness_grade, product.wrinkle_grade]
    if any((g or "").strip().upper() == "A" for g in grades): return 1
    if any((g or "").strip().upper() == "E" for g in grades): return -1
    return 0

def _compute_recommend_score_1_to_10(skin_type, acne_score, spots_score, redness_score, wrinkles_score, product):
    score = 5
    concern_tags = _split_tags(product.concerns)
    formulation_tags = _split_tags(product.formulation)
    
    score += _severity_weight(acne_score) if "acne" in concern_tags else 0
    score += _severity_weight(spots_score) if "spot" in concern_tags else 0
    score += _severity_weight(redness_score) if "redness" in concern_tags else 0
    score += _severity_weight(wrinkles_score) if "wrinkle" in concern_tags else 0
    
    score += _formulation_bonus(skin_type, formulation_tags)
    score += _grade_quality_bonus(product)
    return int(max(1, min(10, score)))

def _invert_required_product_grade(user_grade: str) -> str:
    mapping = {"A": "E", "B": "D", "C": "C", "D": "B", "E": "A"}
    return mapping.get((user_grade or "").strip().upper(), "C")

def _acceptable_by_required_grades(product: Product, required: dict) -> bool:
    def s(g: str | None) -> int: return _GRADE_SCORE.get((g or "").strip().upper(), 3)
    return (s(product.acne_grade) >= s(required["acne_grade"]) and
            s(product.spot_grade) >= s(required["spot_grade"]) and
            s(product.redness_grade) >= s(required["redness_grade"]) and
            s(product.wrinkle_grade) >= s(required["wrinkle_grade"]))

def _normalize_name_for_dedupe(name: str | None) -> str:
    if not name: return ""
    return re.sub(r"[\s\W_]+", "", str(name).lower()).strip()

def _is_banned_name(name: str | None) -> bool:
    n = _normalize_name_for_dedupe(name)
    return any(_normalize_name_for_dedupe(kw) in n for kw in _BANNED_NAME_KEYWORDS)

def _dedupe_scored_items(scored, limit):
    out, seen_ids, seen_names = [], set(), set()
    for computed, p in scored:
        pid = int(p.product_id)
        name_key = _normalize_name_for_dedupe(p.product_name)
        if pid in seen_ids or (name_key and name_key in seen_names): continue
        seen_ids.add(pid)
        if name_key: seen_names.add(name_key)
        out.append((computed, p))
        if len(out) >= limit: break
    return out

def _relax_required_grades(required: dict) -> dict:
    order = {"A": "B", "B": "C", "C": "D", "D": "E", "E": "E"}
    return {k: order.get(v, "E") for k, v in required.items()}

# --- 메인 추천 함수 ---

def recommend_products(
    db: Session,
    skin_type: str,
    acne_score: float,
    spots_score: float,
    redness_score: float,
    wrinkles_score: float,
    limit: int = 20,
):
    # 1. 등급 및 요구 등급 계산
    user_grades = {
        "acne_grade": score_to_grade(acne_score),
        "spot_grade": score_to_grade(spots_score),
        "redness_grade": score_to_grade(redness_score),
        "wrinkle_grade": score_to_grade(wrinkles_score),
    }
    required_grades = {k: _invert_required_product_grade(v) for k, v in user_grades.items()}

    # 2. DB 쿼리 및 기본 필터링
    q = db.query(Product).join(ProductSkinType, Product.product_id == ProductSkinType.product_id)\
          .filter(ProductSkinType.skin_type == skin_type, Product.is_active.is_(True))
    
    products = q.all()

    # 3. 점수 계산 및 정렬
    scored_all = []
    for p in products:
        if _is_banned_name(p.product_name): continue
        score = _compute_recommend_score_1_to_10(skin_type, acne_score, spots_score, redness_score, wrinkles_score, p)
        scored_all.append((score, p))

    def _parse_price(p_val):
        try: return int(str(p_val).replace(",", ""))
        except: return 9999999

    scored_all.sort(key=lambda x: (x[0], -_parse_price(x[1].price), x[1].product_id), reverse=True)
    scored_unique = _dedupe_scored_items(scored_all, limit=1000)

    # ✅ [핵심 기능] 제형 하나당 딱 '최고의 1개'만 추출하는 헬퍼 함수
    def get_best_single_product(form_name: str, current_required):
        # 1차 시도: 요구 등급 충족하는 1등
        for comp, p in scored_unique:
            if p.formulation == form_name and _acceptable_by_required_grades(p, current_required):
                return (comp, p)
        # 2차 시도: 등급 대폭 완화해서 해당 제형 중 1등 찾기
        for comp, p in scored_unique:
            if p.formulation == form_name:
                return (comp, p)
        return None

    # 4. 루틴 단계 구성 (아침 4단계, 저녁 4단계)
    # 각 슬롯에 들어갈 제형 정의
    morning_structure = [
        {"step": "TONER", "name": "토너", "forms": ["TONER", "SKIN", "PAD"]},
        {"step": "ESSENCE_GROUP", "name": "앰플/에센스/세럼 (원하는 만큼 선택)", "forms": ["AMPOULE", "ESSENCE", "SERUM"]},
        {"step": "CREAM_GROUP", "name": "에멀전/크림", "forms": ["LOTION", "EMULSION", "MOISTURE_CREAM", "CREAM"]},
        {"step": "SUNCARE", "name": "선크림", "forms": ["SUN_CREAM"]}
    ]

    evening_structure = [
        {"step": "TONER", "name": "토너", "forms": ["TONER", "SKIN", "PAD"]},
        {"step": "ESSENCE_GROUP", "name": "앰플/에센스/세럼 (원하는 만큼 선택)", "forms": ["AMPOULE", "ESSENCE", "SERUM"]},
        {"step": "CREAM_GROUP", "name": "에멀전/크림", "forms": ["LOTION", "EMULSION", "MOISTURE_CREAM", "CREAM"]},
        {"step": "MASK", "name": "마스크팩 (선택)", "forms": ["MASK_PACK"]}
    ]

    def _build_step_data(structure):
        routine_result = []
        for stage in structure:
            items = []
            for f in stage["forms"]:
                res = get_best_single_product(f, required_grades)
                if res:
                    comp, p = res
                    items.append({
                        "product_id": p.product_id,
                        "product_name": p.product_name,
                        "product_image_url": p.product_image_url,
                        "price": p.price,
                        "formulation": p.formulation,
                        "recommend_score": comp,
                        "acne_grade": p.acne_grade,
                        "spot_grade": p.spot_grade,
                        "redness_grade": p.redness_grade,
                        "wrinkle_grade": p.wrinkle_grade,
                    })
            if items:
                routine_result.append({"step": stage["step"], "step_name": stage["name"], "items": items})
        return routine_result

    return {
        "user_grades": user_grades,
        "morning_routine": _build_step_data(morning_structure),
        "evening_routine": _build_step_data(evening_structure)
    }