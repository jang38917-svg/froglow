from pydantic import BaseModel, EmailStr, Field

class LoginRequest(BaseModel):
    # ✅ 로그인 아이디(별명)
    id: str | None = Field(default=None, min_length=4, max_length=30)

    # 또는 이메일 로그인도 허용
    email: EmailStr | None = None

    password: str = Field(min_length=8, max_length=128)

class LoginResponse(BaseModel):
    message: str
    id: str
    email: EmailStr
    name: str | None
