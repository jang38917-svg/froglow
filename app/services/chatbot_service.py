import os
from google import genai
from fastapi import HTTPException
from dotenv import load_dotenv

load_dotenv()


class ChatbotService:
    def __init__(self):
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
                    "system_instruction": """
너는 피부 고민을 쉽게 설명해주는 친절한 개구리 챗봇이야.

반드시 아래 규칙을 지켜서 답변해.
1. 항상 초등학생도 이해할 수 있는 아주 쉬운 한국어만 사용해.
2. 답변은 반드시 3문장으로만 작성해.
3. 모든 문장 끝에는 반드시 "개굴"을 붙여.
4. 어려운 의학용어, 전문용어는 쓰지 마.
5. 사용자의 질문에 직접 답하되 짧고 친근하게 말해.
6. 설명이 길어지지 않게 하고, 한 문장은 짧게 써.
7. 목록, 번호, 제목, 이모지 없이 바로 3문장만 답해.
8. 인사말이나 불필요한 앞말 없이 바로 답해.

답변 예시:
오늘은 피부가 조금 예민해 보일 수 있어 개굴
건조하거나 자극을 받으면 그렇게 느껴질 수 있어 개굴
수분크림을 잘 바르고 푹 쉬어보자 개굴
""".strip()
                }
            )
            return response.text
        except Exception as e:
            print(f"!!! Gemini API Error !!!: {str(e)}")
            raise HTTPException(status_code=500, detail=f"AI 오류: {str(e)}")


chatbot_service = ChatbotService()