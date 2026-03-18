# app/db/run_naver_seed.py
from app.db.seed_products_from_naver import seed_from_naver
from dotenv import load_dotenv
load_dotenv()

KEYWORDS = [
    # 기본 스킨케어
    "수분크림",
    "토너",
    "세럼",
    "앰플",
    "에센스",
    "아이크림",
    "미스트",
    "선크림",

    # 홍조
    "홍조 크림",
    "홍조 진정",
    "시카 크림",
    "병풀 크림",

    # 잡티
    "잡티 크림",
    "미백 세럼",
    "기미 크림",
    "브라이트닝 에센스",

    # 여드름
    "여드름 크림",
    "트러블 진정",
    "아크네 세럼",
    "피지 조절 세럼",

    # 주름
    "주름 크림",
    "탄력 세럼",
    "안티에이징 크림",
    "레티놀 크림",

    # 핸드케어 (추가)
    "핸드크림",
    "손크림",
    "고보습 핸드크림",
    "무향 핸드크림",
    "시카 핸드크림",

    # 마스크팩 (추가)
    "마스크팩",
    "시트마스크",
    "수분 마스크팩",
    "진정 마스크팩",
    "미백 마스크팩",
]

def main():
    total = {"seen": 0, "inserted_products": 0, "inserted_skin_links": 0}

    for q in KEYWORDS:
        r = seed_from_naver(
            query=q,
            pages=3,
            display=100,
            sort="sim",
            default_grade="C",
            attach_all_skin_types=True,
        )
        print("done:", r)

        total["seen"] += r["seen"]
        total["inserted_products"] += r["inserted_products"]
        total["inserted_skin_links"] += r["inserted_skin_links"]

    print("TOTAL:", total)

if __name__ == "__main__":
    main()