# .env 파일 자동 로드
from dotenv import load_dotenv
load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# DB 관련 import
from app.db.base import Base
from app.db.session import engine

# ⚠️ 중요
# 이 import가 있어야 SQLAlchemy가
# "users 테이블이 존재한다"는 걸 인식함
from app.models.user import User

# 서버 시작 시 테이블 자동 생성
# (없으면 생성, 있으면 무시)
Base.metadata.create_all(bind=engine)

# FastAPI 앱 생성
app = FastAPI(title="SkinCare API")

# 안드로이드 앱에서 접근 가능하도록 CORS 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 개발 중엔 전체 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 서버 살아있는지 확인용
@app.get("/")
def root():
    return {"message": "SkinCare API is running"}

@app.get("/health")
def health():
    return {"status": "ok"}
