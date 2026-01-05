# .env 파일 자동 로드
from dotenv import load_dotenv
load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

#추가: auth 라우터 연결
from app.api.auth import router as auth_router

# DB 관련 import
from app.db.base import Base
from app.db.session import engine

from app.models.user import User

Base.metadata.create_all(bind=engine)

app = FastAPI(title="SkinCare API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

#추가: 라우터 등록
app.include_router(auth_router)

@app.get("/")
def root():
    return {"message": "SkinCare API is running"}

@app.get("/health")
def health():
    return {"status": "ok"}
