from pydantic import BaseModel
from fastapi import APIRouter

from app.services.chatbot_service import chatbot_service


router = APIRouter(prefix="/chatbot", tags=["chatbot"])


class ChatRequest(BaseModel):
    message: str


@router.post("/ask")
async def ask_question(body: ChatRequest):
    answer = await chatbot_service.get_skin_advice(body.message)
    return {"reply": answer}