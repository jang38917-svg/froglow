from pydantic import BaseModel
from typing import Optional, List, Dict


class AnalyzeRequest(BaseModel):
    image_path: str
    conf: Optional[float] = None
    iou: Optional[float] = None


class BBox(BaseModel):
    x1: float
    y1: float
    x2: float
    y2: float


class Detection(BaseModel):
    class_id: int
    label: str
    confidence: float
    bbox: BBox
    area: float
    center: Dict[str, float]


class AnalyzeResponse(BaseModel):
    image_path: str
    count: int
    detections: List[Detection]
    summary: Dict