# app/core/security.py
"""
보안 관련 유틸리티 모음

1) 비밀번호 해싱 / 검증 (bcrypt)
2) JWT 액세스 토큰 생성 / 검증
"""

import os
from datetime import datetime, timedelta

from passlib.context import CryptContext
from jose import jwt, JWTError

# 비밀번호 해싱 / 검증 

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    """
    평문 비밀번호 → bcrypt 해시
    (회원가입 시 사용)
    """
    return pwd_context.hash(password)


def verify_password(password: str, hashed: str) -> bool:
    """
    로그인 시 사용
    - 입력한 평문 비밀번호
    - DB에 저장된 해시 비밀번호
    비교
    """
    return pwd_context.verify(password, hashed)

# JWT 설정 (.env에서 관리 권장)
#  실서비스에서는 반드시 .env
JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY", "change-this-secret-key")
JWT_ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")
JWT_EXPIRE_MINUTES = int(os.getenv("JWT_EXPIRE_MINUTES", "60"))


# 
#  JWT 토큰 생성
# 

def create_access_token(subject: str) -> str:
    """
    JWT 액세스 토큰 생성

    subject:
    - 보통 user.id를 문자열로 넣음
    - 토큰 안에서 "이 사용자가 누구인지" 식별하는 값
    """
    expire = datetime.utcnow() + timedelta(minutes=JWT_EXPIRE_MINUTES)

    payload = {
        "sub": subject,   # 토큰 주체 (user_id)
        "exp": expire,    # 만료 시간
    }

    token = jwt.encode(payload, JWT_SECRET_KEY, algorithm=JWT_ALGORITHM)
    return token

# 4️⃣ JWT 토큰 검증
def decode_access_token(token: str) -> dict:
    """
    JWT 토큰 검증 및 디코딩

    - 유효하지 않으면 JWTError 발생
    - 정상적이면 payload(dict) 반환
    """
    return jwt.decode(token, JWT_SECRET_KEY, algorithms=[JWT_ALGORITHM])
