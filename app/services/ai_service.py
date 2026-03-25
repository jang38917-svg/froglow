import json
import requests
from fastapi import APIRouter, UploadFile, File

router = APIRouter()

YOLO_URL = "http://host.docker.internal:8001/analyze"
SCORE_URL = "http://host.docker.internal:8002/analyze"


@router.post("/analyze")
async def analyze(file: UploadFile = File(...)):
    image_bytes = await file.read()

    # -------------------------
    # 1. YOLO 서버 호출
    # -------------------------
    files = {
        "file": (file.filename, image_bytes, file.content_type)
    }

    yolo_res = requests.post(YOLO_URL, files=files)

    if yolo_res.status_code != 200:
        return {"error": "YOLO server failed", "detail": yolo_res.text}

    yolo_data = yolo_res.json()
    detections = yolo_data.get("detections", [])

    # -------------------------
    # 2. score 서버 호출
    # -------------------------
    data = {
        "detections": json.dumps(detections)
    }

    score_res = requests.post(
        SCORE_URL,
        files=files,
        data=data
    )

    if score_res.status_code != 200:
        return {"error": "Score server failed", "detail": score_res.text}

    score_data = score_res.json()

    return {
        "detections": detections,
        "score": score_data
    }