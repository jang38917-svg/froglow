from sqlalchemy import Column, Integer, String, DateTime, Date, func
from app.db.base import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    name = Column(String(50), nullable=True)
    login_id = Column(String(30), unique=True, index=True, nullable=False)
    gender = Column(String(1), nullable=True)
    birth_date = Column(Date, nullable=True)
    hashed_password = Column(String, nullable=False)
    
    # 🚀 유저의 피부 타입을 저장할 컬럼
    skin_type = Column(String(20), nullable=True) 

    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    survey_completed_at = Column(DateTime(timezone=True), nullable=True)