from datetime import date
from pydantic import BaseModel, Field
from typing import Literal

Gender = Literal["M", "F", "O"]  # M=남, F=여, O=기타/선택


class UserCreate(BaseModel):
    # ✅ 로그인 아이디(별명)
    login_id: str = Field(min_length=4, max_length=30, pattern=r"^[a-zA-Z0-9_]+$")

    #이메일
    email: str = Field(min_length=5, max_length=255)

    password: str = Field(min_length=4, max_length=72)

    # 선택 정보
    name: str | None = Field(default=None, max_length=50)
    gender: Gender | None = None
    birth_date: date | None = None


class UserOut(BaseModel):
    pk: int
    name: str | None
    login_id: str | None
    email: str
    gender: Gender | None
    birth_date: date | None

    class Config:
        from_attributes = True
