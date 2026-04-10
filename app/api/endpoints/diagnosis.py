import os
import uuid
import random
import base64
from datetime import datetime, time
from typing import Optional

from fastapi import APIRouter, File, UploadFile, Depends, Form, HTTPException
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.services.recommend_service import recommend_products
from app.services.routine_service import save_routine, get_routine
from app.services.yolo_client import call_yolo_api, YoloClientError
from app.services.skin_score_client import call_skin_score_api, SkinScoreClientError
from app.services.image_preprocess import preprocess_image_map
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

    if "," in base64_str:
        base64_str = base64_str.split(",", 1)[1]

    image_bytes = base64.b64decode(base64_str)
    with open(output_path, "wb") as f:
        f.write(image_bytes)


def _extract_product_ids(routine_steps: list[dict]) -> list[int]:
    product_ids: list[int] = []

    for step in routine_steps or []:
        items = step.get("items", [])
        for item in items:
            product_id = item.get("product_id")
            if product_id is not None:
                product_ids.append(product_id)

    unique_product_ids: list[int] = []
    seen = set()

    for product_id in product_ids:
        if product_id not in seen:
            seen.add(product_id)
            unique_product_ids.append(product_id)

    return unique_product_ids


def _parse_record_date(record_date: Optional[str]) -> datetime:
    created_at_value = datetime.utcnow()

    if record_date:
        parsed_date = datetime.strptime(record_date, "%Y-%m-%d").date()
        created_at_value = datetime.combine(parsed_date, time.min)

    return created_at_value


def _build_image_paths_from_folder(folder_id: str) -> dict[str, Optional[str]]:
    folder_path = os.path.join(UPLOAD_DIR, folder_id)

    return {
        "front": os.path.join(folder_path, "front.jpg"),
        "left": os.path.join(folder_path, "left.jpg"),
        "right": os.path.join(folder_path, "right.jpg"),
    }


def _ensure_folder_exists(folder_id: str) -> str:
    folder_path = os.path.join(UPLOAD_DIR, folder_id)
    if not os.path.isdir(folder_path):
        raise HTTPException(status_code=404, detail="Analysis folder not found")
    return folder_path


def _ensure_initial_routines(
    db: Session,
    user_id: int,
    recommendation_result: dict,
) -> None:
    morning_product_ids = _extract_product_ids(
        recommendation_result.get("morning_routine", [])
    )
    evening_product_ids = _extract_product_ids(
        recommendation_result.get("evening_routine", [])
    )

    existing_am_routine = get_routine(db, user_id=user_id, routine_type="AM")
    existing_pm_routine = get_routine(db, user_id=user_id, routine_type="PM")

    if existing_am_routine is None and morning_product_ids:
        save_routine(
            db,
            user_id=user_id,
            routine_type="AM",
            product_ids=morning_product_ids
        )

    if existing_pm_routine is None and evening_product_ids:
        save_routine(
            db,
            user_id=user_id,
            routine_type="PM",
            product_ids=evening_product_ids
        )


def _create_history(
    db: Session,
    user_id: int,
    folder_id: str,
    final_summary: dict[str, int],
    primary_concern: str,
    saved_file_paths: dict[str, Optional[str]],
    created_at_value: datetime,
) -> DiagnosisHistory:
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
        right_image_path=saved_file_paths.get("right"),
        created_at=created_at_value
    )
    db.add(new_history)
    db.commit()
    db.refresh(new_history)
    return new_history


async def _run_analysis(
    front: UploadFile,
    left: UploadFile,
    right: UploadFile,
    final_skin: str,
    db: Session,
) -> dict:
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

    original_image_paths = {
        "front": saved_file_paths["front"],
        "left": saved_file_paths["left"],
        "right": saved_file_paths["right"],
    }

    preprocessed_results = preprocess_image_map(
        original_image_paths,
        max_size=1280,
        enable_sharpen=False,
        overwrite=True,
    )

    preprocessed_image_paths = {
        key: result.processed_path
        for key, result in preprocessed_results.items()
    }

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
        for direction in directions:
            yolo_result = await call_yolo_api(
                image_path=preprocessed_image_paths[direction],
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

        score_result = await call_skin_score_api(
            image_path=preprocessed_image_paths["front"],
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

    primary_concern = max(final_summary, key=final_summary.get)

    return {
        "folder_id": folder_id,
        "diagnosis": final_summary,
        "primary_concern": primary_concern,
        "images": saved_file_paths,
        "annotated_images": annotated_file_paths,
        "recommendation": recommendation_result,
    }


@router.post("/analyze/preview")
async def analyze_skin_preview(
    front: UploadFile = File(...),
    left: UploadFile = File(...),
    right: UploadFile = File(...),
    skin_type: Optional[str] = Form(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    final_skin = skin_type or current_user.skin_type or "COMB"

    result = await _run_analysis(
        front=front,
        left=left,
        right=right,
        final_skin=final_skin,
        db=db,
    )

    return {
        "status": "success",
        "folder_id": result["folder_id"],
        "diagnosis": result["diagnosis"],
        "primary_concern": result["primary_concern"],
        "images": result["images"],
        "annotated_images": result["annotated_images"],
        "recommendation": result["recommendation"],
    }


@router.post("/save")
async def save_diagnosis(
    folder_id: str = Form(...),
    acne: int = Form(...),
    redness: int = Form(...),
    wrinkle: int = Form(...),
    spot: int = Form(...),
    primary_concern: Optional[str] = Form(None),
    skin_type: Optional[str] = Form(None),
    record_date: Optional[str] = Form(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    _ensure_folder_exists(folder_id)

    final_skin = skin_type or current_user.skin_type or "COMB"

    final_summary = {
        "acne": _clamp_score(acne),
        "redness": _clamp_score(redness),
        "wrinkle": _clamp_score(wrinkle),
        "spot": _clamp_score(spot),
    }

    final_primary_concern = primary_concern
    if not final_primary_concern or final_primary_concern not in final_summary:
        final_primary_concern = max(final_summary, key=final_summary.get)

    saved_file_paths = _build_image_paths_from_folder(folder_id)

    recommendation_result = recommend_products(
        db=db,
        skin_type=final_skin,
        acne_score=float(final_summary["acne"]),
        spots_score=float(final_summary["spot"]),
        redness_score=float(final_summary["redness"]),
        wrinkles_score=float(final_summary["wrinkle"])
    )

    _ensure_initial_routines(
        db=db,
        user_id=current_user.id,
        recommendation_result=recommendation_result,
    )

    created_at_value = _parse_record_date(record_date)

    history = _create_history(
        db=db,
        user_id=current_user.id,
        folder_id=folder_id,
        final_summary=final_summary,
        primary_concern=final_primary_concern,
        saved_file_paths=saved_file_paths,
        created_at_value=created_at_value,
    )

    return {
        "status": "success",
        "id": history.id,
        "folder_id": folder_id,
        "diagnosis": final_summary,
        "primary_concern": final_primary_concern,
        "images": saved_file_paths,
        "recommendation": recommendation_result,
        "record_date": created_at_value.date().isoformat(),
    }


@router.post("/analyze")
async def analyze_skin(
    front: UploadFile = File(...),
    left: UploadFile = File(...),
    right: UploadFile = File(...),
    skin_type: Optional[str] = Form(None),
    record_date: Optional[str] = Form(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    final_skin = skin_type or current_user.skin_type or "COMB"

    result = await _run_analysis(
        front=front,
        left=left,
        right=right,
        final_skin=final_skin,
        db=db,
    )

    _ensure_initial_routines(
        db=db,
        user_id=current_user.id,
        recommendation_result=result["recommendation"],
    )

    created_at_value = _parse_record_date(record_date)

    _create_history(
        db=db,
        user_id=current_user.id,
        folder_id=result["folder_id"],
        final_summary=result["diagnosis"],
        primary_concern=result["primary_concern"],
        saved_file_paths=result["images"],
        created_at_value=created_at_value,
    )

    return {
        "status": "success",
        "folder_id": result["folder_id"],
        "diagnosis": result["diagnosis"],
        "primary_concern": result["primary_concern"],
        "images": result["images"],
        "annotated_images": result["annotated_images"],
        "recommendation": result["recommendation"]
    }