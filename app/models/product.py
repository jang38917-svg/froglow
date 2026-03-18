# app/models/product.py
from sqlalchemy import Column, BigInteger, String, Text, Boolean, CHAR, Integer
from app.db.base import Base

class Product(Base):
    __tablename__ = "products"

    product_id = Column(BigInteger, primary_key=True, index=True) # BigInteger 확인!
    product_name = Column(String(255), nullable=False)
    product_image_url = Column(Text, nullable=True)
    acne_grade = Column(CHAR(1), nullable=False)
    redness_grade = Column(CHAR(1), nullable=False)
    wrinkle_grade = Column(CHAR(1), nullable=False)
    spot_grade = Column(CHAR(1), nullable=False)
    recommend_score = Column(Integer, nullable=False, default=3)
    is_active = Column(Boolean, nullable=False, default=True)
    formulation = Column(String(50), nullable=True)
    concerns = Column(String(100), nullable=True)
    price = Column(String(50), nullable=True) # String 확인!