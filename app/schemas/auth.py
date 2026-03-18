from pydantic import BaseModel, Field, EmailStr
from datetime import date


class LoginRequest(BaseModel):
    login_id: str = Field(min_length=4, max_length=30)
    password: str = Field(min_length=4, max_length=72)


class LoginResponse(BaseModel):
    message: str

    # JWT 토큰
    access_token: str
    token_type: str = "bearer"

    # 설문 필요 여부
    needs_survey: bool
    login_id: str
    email: str
    name: str | None


#  "회원가입" 

class RegisterRequest(BaseModel):
    login_id: str = Field(min_length=3, max_length=30)
    email: EmailStr
    password: str = Field(min_length=4, max_length=72)

    name: str | None = Field(default=None, max_length=50)
    gender: str | None = Field(default=None, max_length=1)  # "M" / "F" / "O"
    birth_date: date | None = None


class RegisterResponse(BaseModel):
    """
    회원가입 응답 스키마
    - 가입이 성공했는지 + 생성된 유저 기본 정보 반환
    """
    message: str
    id: int
    login_id: str
    email: str
    name: str | None
