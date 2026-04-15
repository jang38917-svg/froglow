from __future__ import annotations

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from app.api.auth import router as auth_router
from app.api.endpoints.diagnosis import router as diagnosis_router
from app.api.endpoints.diagnosis_calendar import router as diagnosis_calendar_router
from app.api.endpoints.diagnosis_stats import router as diagnosis_stats_router
from app.api.endpoints.chatbot import router as chatbot_router
from app.api.endpoints.recommend import router as recommend_router
from app.api.products import router as products_router
from app.api.reminders import router as reminders_router
from app.api.routines import router as routines_router
from app.api.survey import router as survey_router

app = FastAPI(title="SkinCare API")

app.mount("/uploads", StaticFiles(directory="data/uploads"), name="uploads")

app.include_router(auth_router)
app.include_router(survey_router)
app.include_router(products_router)
app.include_router(reminders_router)
app.include_router(routines_router)

app.include_router(diagnosis_router, prefix="/api/v1")
app.include_router(diagnosis_calendar_router, prefix="/api/v1/diagnosis")
app.include_router(diagnosis_stats_router, prefix="/api/v1/diagnosis")
app.include_router(chatbot_router, prefix="/api/v1")
app.include_router(recommend_router)