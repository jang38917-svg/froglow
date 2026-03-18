import os
from google import genai
from fastapi import HTTPException
from dotenv import load_dotenv

load_dotenv()

class ChatbotService:
    def __init__(self):
        # 💡 코드를 직접 수정하지 않아도 .env 파일만 바꾸면 작동하도록 설정합니다.
        api_key = os.getenv("GEMINI_API_KEY")
        
        if not api_key:
            raise ValueError(".env 파일에 GEMINI_API_KEY가 없습니다!")
            
        self.client = genai.Client(api_key=api_key)

    async def get_skin_advice(self, user_message: str):
        try:
            response = await self.client.aio.models.generate_content(
                model="gemini-flash-latest", 
                contents=user_message,
                config={
                    "system_instruction": "당신은 전문 피부 관리 상담사입니다. 객관적인 정보만 제공하세요."
                }
            )
            return response.text
        except Exception as e:
            print(f"!!! Gemini API Error !!!: {str(e)}")
            raise HTTPException(status_code=500, detail=f"AI 오류: {str(e)}")

chatbot_service = ChatbotService()