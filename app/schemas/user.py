# FastAPI에서 "요청(request) 데이터 검증"을 담당

from datetime import date
from pydantic import BaseModel, EmailStr, Field
from typing import Literal

Gender = Literal["M", "F", "O"]  # M=남, F=여, O=기타/선택


class UserCreate(BaseModel):
    # 실명(본명)
    name: str = Field(min_length=1, max_length=50)

    #   로그인할 때 쓰는 아이디(별명) = id
    id: str = Field(min_length=4, max_length=30, pattern=r"^[a-zA-Z0-9_]+$")

    # 이메일
    email: EmailStr

    # 비밀번호(평문으로 받고 서버에서 해싱해서 저장)
    password: str = Field(min_length=4, max_length=128)

    # 성별
    gender: Gender

    # 생년월일 ("2000-01-31")
    birth_date: date


class UserOut(BaseModel):
   #서버가 클라이언트로 내려주는 사용자 정보
    pk: int
    name: str | None
    id: str | None
    email: EmailStr
    gender: Gender | None
    birth_date: date | None

    class Config:
        from_attributes = True
