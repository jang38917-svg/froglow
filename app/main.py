from dotenv import load_dotenv
load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.api.auth import router as auth_router
from app.api.survey import router as survey_router
from app.api.products import router as products_router
from app.api.endpoints.recommend import router as recommend_router
from app.api.routines import router as routines_router
from app.api.reminders import router as reminders_router
from app.api.endpoints.diagnosis import router as diagnosis_router
from app.api.endpoints.diagnosis_calendar import router as diagnosis_calendar_router
from app.api.endpoints.diagnosis_stats import router as diagnosis_stats_router

from app.db.session import SessionLocal, engine
from app.db.base import Base
from app.db.seed_survey import seed_survey_questions

from app.models.user import User
from app.models.product import Product
from app.models.survey import (
    SurveyQuestion,
    SurveyOption,
    SurveyResponse,
    SurveyAnswer,
)
from app.models.reminder import Reminder
from app.api.endpoints import chatbot

app = FastAPI(title="SkinCare API")

app.mount("/uploads", StaticFiles(directory="data/uploads"), name="uploads")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(survey_router)
app.include_router(products_router)
app.include_router(recommend_router, prefix="/api/recommend", tags=["Recommendations"])
app.include_router(routines_router)
app.include_router(reminders_router)
app.include_router(chatbot.router, prefix="/api/v1/chatbot", tags=["chatbot"])
app.include_router(diagnosis_router, prefix="/api/v1/diagnosis", tags=["Diagnosis"])
app.include_router(diagnosis_calendar_router, prefix="/api/v1/diagnosis", tags=["Diagnosis Calendar"])
app.include_router(diagnosis_stats_router, prefix="/api/v1/diagnosis", tags=["Diagnosis Stats"])

@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)

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