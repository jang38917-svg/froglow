# app/crawlers/naver_shopping_api.py
from __future__ import annotations
import os
import time
import re
from typing import Any, Dict, List
import requests

NAVER_SHOPPING_URL = "https://openapi.naver.com/v1/search/shop.json"

def _determine_formulation(title: str) -> str:
    """상품명 키워드를 기반으로 제형을 자동 분류합니다."""
    title = title.lower()
    rules = {
        'SUN_CREAM': ['선크림', '선블록', '선젤', '선에센스', '자외선', '차단', 'sun'],
        'MOISTURE_CREAM': ['수분크림', '보습크림', '영양크림', '재생크림', '시카크림', 'cream'],
        'AMPOULE': ['앰플', 'ampoule'],
        'SERUM': ['세럼', 'serum', '에센스', 'essence'],
        'LOTION': ['로션', '에멀젼', '에멀전', 'lotion', 'emulsion'],
        'TONER': ['토너', '스킨', 'toner', 'skin', '패드', 'pad'],
        'MASK_PACK': ['마스크팩', '시트마스크', '팩', 'mask']
    }
    
    for formulation, keywords in rules.items():
        if any(kw in title for kw in keywords):
            return formulation
    return "ETC"  # 분류되지 않은 경우

def search_products(keyword: str, start: int = 1, display: int = 20) -> List[Dict[str, Any]]:
    headers = {
        "X-Naver-Client-Id": (os.getenv("NAVER_CLIENT_ID") or "").strip(),
        "X-Naver-Client-Secret": (os.getenv("NAVER_CLIENT_SECRET") or "").strip(),
    }
    
    params = {"query": keyword, "start": start, "display": display, "sort": "sim"}
    r = requests.get(NAVER_SHOPPING_URL, headers=headers, params=params, timeout=30)
    r.raise_for_status()
    
    data = r.json()
    items = []

    for it in data.get("items", []):
        product_id = it.get("productId") or it.get("link")
        if not product_id: continue

        # 원문 제목에서 HTML 태그 제거
        raw_title = it.get("title", "").replace("<b>", "").replace("</b>", "").strip()
        
        # ✅ 핵심: 제형 자동 결정
        formulation = _determine_formulation(raw_title)

        items.append({
            "productId": str(product_id),
            "productName": raw_title,
            "imageUrl": it.get("image") or None,
            "detailUrl": it.get("link") or None,
            "categoryName": " > ".join(filter(None, [it.get("category1"), it.get("category2"), it.get("category3"), it.get("category4")])) or None,
            "price": it.get("lprice") or None,
            "formulation": formulation  # ✅ 이제 DB에 들어갈 때 제형이 분류되어 들어갑니다!
        })

    return items