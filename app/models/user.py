from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.sql import func
from app.db.base import Base

# users 테이블과 매핑되는 클래스
class User(Base):
    # 실제 DB 테이블 이름
    __tablename__ = "users"

    # 기본키 (자동 증가)
    id = Column(Integer, primary_key=True, index=True)

    # 로그인에 사용할 이메일 (중복 불가)
    email = Column(String, unique=True, index=True, nullable=False)

    # 비밀번호는 절대 평문 저장 ❌
    # bcrypt로 해시된 값 저장
    hashed_password = Column(String, nullable=False)

    # 계정 생성 시각 (자동)
    created_at = Column(
        DateTime(timezone=True),
        server_default=func.now()
    )
