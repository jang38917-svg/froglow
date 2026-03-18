import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker


#DB 세션/엔진 설정 파일

#- FastAPI, Alembic, 테스트 코드 모두
#  동일한 DATABASE_URL을 사용하도록 통일

#  DATABASE_URL 하나만 사용 (가장 안전하고 깔끔한 방식)
DATABASE_URL = os.getenv("DATABASE_URL")

if not DATABASE_URL:
    raise RuntimeError(
        "DATABASE_URL is not set. "
        "Check your .env file."
    )

# SQLAlchemy 엔진 생성
engine = create_engine(
    DATABASE_URL,
    pool_pre_ping=True,
)

# DB 세션 팩토리
SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine,
)

def get_db():
   # FastAPI Dependency
   # 요청 1회당 DB 세션 1개를 생성하고
   # 요청 종료 시 자동으로 닫아줌
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
