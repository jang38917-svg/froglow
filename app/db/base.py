# SQLAlchemy에서 모든 테이블이 상속받을 Base 클래스 생성
# 이 Base를 기준으로 테이블이 만들어짐
from sqlalchemy.orm import declarative_base

# 모든 모델(User, Routine 등)이 이 Base를 상속함
Base = declarative_base()
