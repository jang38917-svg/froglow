from fastapi import APIRouter
from app.services.chatbot_service import chatbot_service

router = APIRouter()

@router.post("/ask")
async def ask_question(message: str):
    answer = await chatbot_service.get_skin_advice(message)
    return {"reply": answer}