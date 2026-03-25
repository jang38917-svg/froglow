import os
import uuid
import random
import base64
from typing import Optional

from fastapi import APIRouter, File, UploadFile, Depends, Form
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.services.recommend_service import recommend_products
from app.services.routine_service import save_routine
from app.services.yolo_client import call_yolo_api, YoloClientError
from app.services.skin_score_client import call_skin_score_api, SkinScoreClientError
from app.models.diagnosis_history import DiagnosisHistory
from app.api.auth import get_current_user
from app.models.user import User

router = APIRouter()
UPLOAD_DIR = "data/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def _clamp_score(value: float, min_value: int = 1, max_value: int = 10) -> int:
    return max(min_value, min(max_value, int(round(value))))


def fallback_logic() -> dict[str, int]:
    return {
        "acne": random.randint(1, 10),
        "redness": random.randint(1, 10),
        "wrinkle": random.randint(1, 10),
        "spot": random.randint(1, 10),
    }


def _save_base64_image(base64_str: str, output_path: str) -> None:
    if not base64_str:
        return

    # data:image/jpeg;base64,... 형태 대응
    if "," in base64_str:
        base64_str = base64_str.split(",", 1)[1]

    image_bytes = base64.b64decode(base64_str)
    with open(output_path, "wb") as f:
        f.write(image_bytes)


@router.post("/analyze")
async def analyze_skin(
    front: UploadFile = File(...),
    left: UploadFile = File(...),
    right: UploadFile = File(...),
    skin_type: Optional[str] = Form(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    final_skin = skin_type or current_user.skin_type or "COMB"

    folder_id = uuid.uuid4().hex[:10]
    save_path = os.path.join(UPLOAD_DIR, folder_id)
    os.makedirs(save_path, exist_ok=True)

    directions = ["front", "left", "right"]
    files = [front, left, right]
    saved_file_paths = {}
    annotated_file_paths = {}

    for dir_name, file in zip(directions, files):
        content = await file.read()
        file_path = os.path.join(save_path, f"{dir_name}.jpg")
        with open(file_path, "wb") as f:
            f.write(content)
        saved_file_paths[dir_name] = file_path

    yolo_api_url = os.getenv(
        "YOLO_API_URL",
        "http://host.docker.internal:8001/analyze"
    )
    skin_score_api_url = os.getenv(
        "SKIN_SCORE_API_URL",
        "http://host.docker.internal:8002/analyze"
    )

    front_detections = []

    try:
        # -------------------------
        # 1) front / left / right 각각 YOLO 호출
        # -------------------------
        for direction in directions:
            yolo_result = await call_yolo_api(
                image_path=saved_file_paths[direction],
                api_url=yolo_api_url,
                timeout=30.0,
            )

            detections = yolo_result.get("detections", [])
            annotated_b64 = yolo_result.get("annotated_image_base64")

            if direction == "front":
                front_detections = detections

            annotated_path = os.path.join(save_path, f"{direction}_annotated.jpg")
            if annotated_b64:
                _save_base64_image(annotated_b64, annotated_path)
                annotated_file_paths[direction] = annotated_path
            else:
                annotated_file_paths[direction] = None

        # -------------------------
        # 2) 점수 계산은 기존처럼 front 기준
        # -------------------------
        score_result = await call_skin_score_api(
            image_path=saved_file_paths["front"],
            detections=front_detections,
            api_url=skin_score_api_url,
            timeout=30.0,
        )
        raw_scores = score_result["raw_scores"]

        final_summary = {
            "acne": _clamp_score(raw_scores["acne"]),
            "redness": _clamp_score(raw_scores["redness"]),
            "wrinkle": _clamp_score(raw_scores["wrinkles"]),
            "spot": _clamp_score(raw_scores["spots"]),
        }

    except (YoloClientError, SkinScoreClientError):
        final_summary = fallback_logic()
        annotated_file_paths = {
            "front": None,
            "left": None,
            "right": None,
        }

    recommendation_result = recommend_products(
        db=db,
        skin_type=final_skin,
        acne_score=float(final_summary["acne"]),
        spots_score=float(final_summary["spot"]),
        redness_score=float(final_summary["redness"]),
        wrinkles_score=float(final_summary["wrinkle"])
    )

    priority, primary_concern = ["acne", "redness", "wrinkle", "spot"], "clean"
    for concern in priority:
        if final_summary[concern] > 0:
            primary_concern = concern
            break

    user_id = current_user.id

    if recommendation_result.get("morning_routine"):
        save_routine(
            db,
            user_id=user_id,
            routine_type="AM",
            product_ids=recommendation_result["morning_routine"]
        )

    if recommendation_result.get("evening_routine"):
        save_routine(
            db,
            user_id=user_id,
            routine_type="PM",
            product_ids=recommendation_result["evening_routine"]
        )

    new_history = DiagnosisHistory(
        user_id=user_id,
        acne_count=final_summary["acne"],
        redness_count=final_summary["redness"],
        wrinkle_count=final_summary["wrinkle"],
        spot_count=final_summary["spot"],
        primary_concern=primary_concern,
        folder_id=folder_id,
        front_image_path=saved_file_paths.get("front"),
        left_image_path=saved_file_paths.get("left"),
        right_image_path=saved_file_paths.get("right")
    )
    db.add(new_history)
    db.commit()

    return {
        "status": "success",
        "folder_id": folder_id,
        "diagnosis": final_summary,
        "primary_concern": primary_concern,
        "images": saved_file_paths,
        "annotated_images": annotated_file_paths,
        "recommendation": recommendation_result
    }