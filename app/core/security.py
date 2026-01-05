#해싱 검사

from passlib.context import CryptContext

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(password: str) -> str:
    #"""평문 비밀번호 -> bcrypt 해시"""
    return pwd_context.hash(password)

def verify_password(password: str, hashed: str) -> bool:
    #"""로그인 시 사용: 평문 vs 해시 검증"""
    return pwd_context.verify(password, hashed)