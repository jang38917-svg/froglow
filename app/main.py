# app/main.py
from dotenv import load_dotenv
load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.auth import router as auth_router
from app.api.survey import router as survey_router
from app.api.products import router as products_router
from app.api.endpoints.recommend import router as recommend_router
from app.api.routines import router as routines_router
from app.api.reminders import router as reminders_router
# ✅ 진단 라우터 추가
from app.api.endpoints.diagnosis import router as diagnosis_router 

from app.db.session import SessionLocal, engine
from app.db.base import Base
from app.db.seed_survey import seed_survey_questions

# --- 모델 임포트 구역 ---
from app.models.user import User  # noqa: F401
# ✅ [여기가 핵심!] Product 모델을 임포트해야 테이블이 생성됩니다.
from app.models.product import Product  # noqa: F401
from app.models.survey import (
    SurveyQuestion,
    SurveyOption,
    SurveyResponse,
    SurveyAnswer,
)  # noqa: F401
from app.models.reminder import Reminder  # noqa: F401
from app.api.endpoints import chatbot # 추가
# -----------------------

app = FastAPI(title="SkinCare API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(auth_router)
app.include_router(survey_router)
app.include_router(products_router)
app.include_router(recommend_router, prefix="/api/recommend", tags=["Recommendations"])
app.include_router(routines_router)
app.include_router(reminders_router)
app.include_router(chatbot.router, prefix="/api/v1/chatbot", tags=["chatbot"])
# ✅ 진단 라우터 등록
app.include_router(diagnosis_router, prefix="/api/v1/diagnosis", tags=["Diagnosis"])

@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)
    
    # 2. 설문 데이터 시딩
    db = SessionLocal()
    try:
        seed_survey_questions(db)
    finally:
        db.close()

@app.get("/")
def root():
    return {"message": "SkinCare API is running"}

@app.get("/health")
def health():
    return {"status": "ok"}