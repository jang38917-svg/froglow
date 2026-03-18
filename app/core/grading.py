# app/core/grading.py
from __future__ import annotations
from typing import Dict

# 🌟 1티어 (핵심 성분 / 확실한 타겟) - 매칭당 +20점
KEYWORDS_T1 = {
    "acne": ["바하", "아하", "파하", "살리실", "티트리", "bha", "aha", "pha", "에이씨", "ac", "블레미쉬", "아크네"],
    "redness": ["병풀", "시카", "판테놀", "마데카", "센텔라", "cica", "어성초", "알로에", "징크", "아줄렌"],
    "wrinkle": ["레티놀", "레티날", "펩타이드", "콜라겐", "엘라스틴", "아데노신", "egf", "바쿠치올"],
    "spot": ["비타민c", "나이아신", "나이아신아마이드", "알부틴", "트라넥삼", "글루타치온", "멜라"],
}

# 🌟 2티어 (효능 및 마케팅 단어) - 매칭당 +10점
KEYWORDS_T2 = {
    "acne": ["트러블", "여드름", "피지", "모공", "각질", "블랙헤드", "화이트헤드", "클리어", "피부고민"],
    "redness": ["홍조", "진정", "민감", "저자극", "리페어", "장벽", "calming", "soothing", "수딩", "카밍", "릴리프", "레드"],
    "wrinkle": ["주름", "탄력", "리프팅", "안티에이징", "anti-aging", "링클", "에이징", "영양", "볼륨", "탄탄"],
    "spot": ["잡티", "기미", "미백", "브라이트닝", "톤업", "화이트닝", "광채", "다크스팟", "색소", "침착", "흑자", "비타민"],
}

# 🚨 감점 키워드 (상극 속성) - 매칭당 -10점
KEYWORDS_PENALTY = {
    "acne": ["오일", "오일리", "고영양", "밤", "balm", "쉐어버터", "꾸덕"],
    "redness": ["스크럽", "필링", "아하", "바하", "aha", "bha", "각질제거"],
    "wrinkle": [],
    "spot": [],
}

# 제품 타입/카테고리 보정 키워드
SUNCARE_HINTS = ["선크림", "선스틱", "선젤", "선로션", "sunscreen", "spf", "pa+"]
MIST_HINTS = ["미스트", "mist"]
TONER_HINTS = ["토너", "스킨", "toner"]

# 등급 비교용 (A가 가장 좋음)
GRADE_SCORE = {"A": 5, "B": 4, "C": 3, "D": 2, "E": 1}

def _calculate_score(text: str, concern: str, price: int | None = None) -> int:
    score = 0
    t = (text or "").lower()

    # 1. 1티어 키워드 검사 (+20점)
    for w in KEYWORDS_T1.get(concern, []):
        if w.lower() in t:
            score += 20
    
    # 2. 2티어 키워드 검사 (+10점)
    for w in KEYWORDS_T2.get(concern, []):
        if w.lower() in t:
            score += 10
            
    # 3. 감점 키워드 검사 (-10점)
    for w in KEYWORDS_PENALTY.get(concern, []):
        if w.lower() in t:
            score -= 10

    # 4. 가격 가산점 (+3 ~ +5점)
    if price is not None:
        if price <= 25000:
            score += 5
        elif price <= 35000:
            score += 3

    return score

def _score_to_grade(score: int) -> str:
    """합산 점수에 따른 최종 등급 컷"""
    if score >= 35:
        return "A"  # 예: 1티어(20) + 2티어(10) + 가성비(5)
    if score >= 20:
        return "B"  # 예: 1티어(20) OR 2티어(10)*2
    if score >= 10:
        return "C"  # 예: 2티어(10)
    if score >= 3:
        return "D"  # 예: 성분은 없지만 가격 가산점만 받은 경우
    return "E"      # 예: 아무것도 없거나 감점당한 경우

def _max_grade(g1: str, g2: str) -> str:
    return g1 if GRADE_SCORE[g1] >= GRADE_SCORE[g2] else g2

def _min_grade(g1: str, g2: str) -> str:
    return g1 if GRADE_SCORE[g1] <= GRADE_SCORE[g2] else g2

def infer_grades(product_name: str, category_text: str = "", price: int | None = None) -> Dict[str, str]:
    """
    입력: 상품명(필수), 카테고리/설명 텍스트(선택), 가격(선택)
    출력: {"acne":"A~E", "redness":"A~E", "wrinkle":"A~E", "spot":"A~E"}
    """
    text = f"{product_name or ''} {category_text or ''}".strip().lower()

    grades = {}
    concerns = ["acne", "redness", "wrinkle", "spot"]
    
    # 각 고민별 점수 계산 및 등급 부여
    for concern in concerns:
        score = _calculate_score(text, concern, price)
        grades[concern] = _score_to_grade(score)

    # 제형/카테고리 기반 보정 (하한선 유지)
    if any(h in text for h in SUNCARE_HINTS):
        grades["spot"] = _max_grade(grades["spot"], "C")
        grades["wrinkle"] = _max_grade(grades["wrinkle"], "C")

    if any(h in text for h in MIST_HINTS):
        grades["redness"] = _max_grade(grades["redness"], "C")
        
    # 제형/카테고리 기반 보정 (상한선 제한 - 🆕 새로 추가됨!)
    # 토너/미스트는 제형 특성상 깊은 주름/미백 개선에 한계가 있어 최대 B등급까지만 허용
    if any(h in text for h in TONER_HINTS + MIST_HINTS):
        grades["wrinkle"] = _min_grade(grades["wrinkle"], "B")
        grades["spot"] = _min_grade(grades["spot"], "B")

    return grades