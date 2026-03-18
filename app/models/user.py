from sqlalchemy import Column, Integer, String, DateTime, Date, func
from app.db.base import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)

    email = Column(String, unique=True, index=True, nullable=False)

    name = Column(String(50), nullable=True)

    # 로그인에 쓰는 값이면 nullable=False 권장
    login_id = Column(String(30), unique=True, index=True, nullable=False)

    gender = Column(String(1), nullable=True)

    birth_date = Column(Date, nullable=True)

    hashed_password = Column(String, nullable=False)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    #  설문 완료 여부 판단용 (NULL이면 미완료)
    survey_completed_at = Column(DateTime(timezone=True), nullable=True)
