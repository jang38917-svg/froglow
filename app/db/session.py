import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# .env 파일에서 DB 접속 정보 읽어오기
# 없으면 기본값 사용
POSTGRES_USER = os.getenv("POSTGRES_USER", "skincare")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "skincare_pw")
POSTGRES_DB = os.getenv("POSTGRES_DB", "skincare_db")

# PostgreSQL 접속 주소
# localhost:5432 는 docker-compose에서 포트 포워딩한 값
DATABASE_URL = (
    f"postgresql://{POSTGRES_USER}:{POSTGRES_PASSWORD}"
    f"@localhost:5432/{POSTGRES_DB}"
)

# DB 엔진 생성 (실제 DB와 연결하는 객체)
engine = create_engine(DATABASE_URL)

# DB 세션 생성기
# API 요청 하나당 세션 하나 쓰는 구조
SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine
)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()