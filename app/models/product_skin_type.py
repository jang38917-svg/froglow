# app/models/product_skin_type.py
from sqlalchemy import Column, BigInteger, String, ForeignKey
from app.db.base import Base

class ProductSkinType(Base):
    __tablename__ = "product_skin_types"

    product_id = Column(BigInteger, ForeignKey("products.product_id"), primary_key=True)
    skin_type = Column(String(10), primary_key=True)  # DRY / OILY / COMB
