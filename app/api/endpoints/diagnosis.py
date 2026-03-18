import os
import uuid
from fastapi import APIRouter, File, UploadFile, Depends, Form
from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.services.ai_service import skin_ai
from app.services.recommend_service import recommend_products 
from app.services.routine_service import save_routine
from app.models.diagnosis_history import DiagnosisHistory
from app.api.auth import get_current_user # 🚀 추가
from app.models.user import User # 🚀 추가

router = APIRouter()
UPLOAD_DIR = "data/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

def get_db():
    db = SessionLocal()
    try: yield db
    finally: db.close()

@router.post("/analyze")
async def analyze_skin(
    front: UploadFile = File(...),
    left: UploadFile = File(...),
    right: UploadFile = File(...),
    skin_type: str = Form("OILY"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user) # 🚀 로그인 유저 주입
):
    # 1. 사진 저장
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

    # 2. AI 분석 합산
    final_summary = {
        "acne": max(r["acne"] for r in raw_results),
        "redness": max(r["redness"] for r in raw_results),
        "wrinkle": max(r["wrinkle"] for r in raw_results),
        "spot": max(r["spot"] for r in raw_results)
    }

    # 3. 추천 엔진 호출
    recommendation_result = recommend_products(
        db=db, skin_type=skin_type,
        acne_score=float(final_summary["acne"]),
        spots_score=float(final_summary["spot"]),
        redness_score=float(final_summary["redness"]),
        wrinkles_score=float(final_summary["wrinkle"])
    )

    # 4. 고민 순위 결정
    priority, primary_concern = ["acne", "redness", "wrinkle", "spot"], "clean"
    for concern in priority:
        if final_summary[concern] > 0:
            primary_concern = concern
            break

    # 🚀 5. 실제 로그인 유저 ID로 DB 저장
    user_id = current_user.id 

    if recommendation_result.get("morning_routine"):
        save_routine(db, user_id=user_id, routine_type="AM", product_ids=recommendation_result["morning_routine"])
    if recommendation_result.get("evening_routine"):
        save_routine(db, user_id=user_id, routine_type="PM", product_ids=recommendation_result["evening_routine"])

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

    # 6. 기존 포맷 그대로 응답
    return {
        "status": "success",
        "folder_id": folder_id,
        "diagnosis": final_summary,
        "primary_concern": primary_concern,
        "images": saved_file_paths,
        "recommendation": recommendation_result 
    }