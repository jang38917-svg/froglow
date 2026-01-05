from sqlalchemy import Column, Integer, String, DateTime, Date
from sqlalchemy.sql import func
from app.db.base import Base

class User(Base):
    __tablename__ = "users"

    # DB 내부 PK (자동 증가)
    id = Column(Integer, primary_key=True, index=True)

    # 이메일 (중복 불가)
    email = Column(String, unique=True, index=True, nullable=False)

    # 실명
    name = Column(String(50), nullable=True)

    # 로그인 아이디(별명) - username 대신 login_id로 명확하게
    login_id = Column(String(30), unique=True, index=True, nullable=True)

    # 성별 (M/F/O)
    gender = Column(String(1), nullable=True)

    # 생년월일
    birth_date = Column(Date, nullable=True)

    # 비밀번호 해시
    hashed_password = Column(String, nullable=False)

    created_at = Column(
        DateTime(timezone=True),
        server_default=func.now()
    )
