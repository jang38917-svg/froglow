from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, Field


class RoutinePreviewRequest(BaseModel):
    routine_type: str = Field(..., description="AM or PM")
    product_ids: List[int]


class RoutineItemOut(BaseModel):
    product_id: int
    step_order: int
    reason: Optional[str] = None
    product_name: Optional[str] = None
    step_name: Optional[str] = None


class RoutinePreviewResponse(BaseModel):
    routine_type: str
    items: List[RoutineItemOut]


class RoutineCreateRequest(BaseModel):
    routine_type: str = Field(..., description="AM or PM")
    product_ids: List[int]


class RoutineOut(BaseModel):
    id: int
    user_id: int
    routine_type: str
    items: List[RoutineItemOut]