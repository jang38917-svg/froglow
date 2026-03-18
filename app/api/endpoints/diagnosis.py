import os
import uuid
from fastapi import APIRouter, File, UploadFile, Depends, Form
from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.services.ai_service import skin_ai
from app.services.recommend_service import recommend_products 
from app.services.routine_service import save_routine
from app.models.diagnosis_history import DiagnosisHistory

router = APIRouter()
UPLOAD_DIR = "data/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

def get_db():
    db = SessionLocal()
    try: yield db
    finally: db.close()

@router.post("/analyze")
async def analyze_skin(
    front: UploadFile = File(...), left: UploadFile = File(...), right: UploadFile = File(...),
    skin_type: str = Form("OILY"), db: Session = Depends(get_db)
):
    folder_id = uuid.uuid4().hex[:10]
    save_path = os.path.join(UPLOAD_DIR, folder_id)
    os.makedirs(save_path, exist_ok=True)

    directions, files = ["front", "left", "right"], [front, left, right]
    raw_results, saved_file_paths = [], {}

    for dir_name, file in zip(directions, files):
        content = await file.read()
        file_path = os.path.join(save_path, f"{dir_name}.jpg")
        with open(file_path, "wb") as f: f.write(content)
        saved_file_paths[dir_name] = file_path
        raw_results.append(skin_ai.predict(content))

    final_summary = {
        "acne": max(r["acne"] for r in raw_results),
        "redness": max(r["redness"] for r in raw_results),
        "wrinkle": max(r["wrinkle"] for r in raw_results),
        "spot": max(r["spot"] for r in raw_results)
    }

    recommendation_result = recommend_products(
        db=db, skin_type=skin_type, acne_score=float(final_summary["acne"]),
        spots_score=float(final_summary["spot"]), redness_score=float(final_summary["redness"]),
        wrinkles_score=float(final_summary["wrinkle"])
    )

    priority, primary_concern = ["acne", "redness", "wrinkle", "spot"], "clean"
    for concern in priority:
        if final_summary[concern] > 0:
            primary_concern = concern
            break

    # DB 저장 (변수명 product_ids 유지)
    user_id = 1 
    if recommendation_result.get("morning_routine"):
        save_routine(db, user_id=user_id, routine_type="AM", product_ids=recommendation_result["morning_routine"])
    if recommendation_result.get("evening_routine"):
        save_routine(db, user_id=user_id, routine_type="PM", product_ids=recommendation_result["evening_routine"])

    new_history = DiagnosisHistory(
        user_id=user_id, acne_count=final_summary["acne"], redness_count=final_summary["redness"],
        wrinkle_count=final_summary["wrinkle"], spot_count=final_summary["spot"],
        primary_concern=primary_concern, folder_id=folder_id,
        front_image_path=saved_file_paths.get("front"), left_image_path=saved_file_paths.get("left"),
        right_image_path=saved_file_paths.get("right")
    )
    db.add(new_history)
    db.commit()

    return {
        "status": "success", "folder_id": folder_id, "diagnosis": final_summary,
        "primary_concern": primary_concern, "images": saved_file_paths,
        "recommendation": recommendation_result 
    }