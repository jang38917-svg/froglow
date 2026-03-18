import json
import io
from PIL import Image
from ultralytics import YOLO
from typing import List, Dict

class SkinAI:
    def __init__(self, model_path: str = "ml_models/best.pt"):
        # 서버 시작 시 모델을 메모리에 로드
        self.model = YOLO(model_path)
        
        # 클래스 맵 로드
        with open("data/class_map.json", "r", encoding="utf-8") as f:
            self.class_map = json.load(f)

    def predict(self, image_bytes: bytes) -> Dict[str, int]:
        img = Image.open(io.BytesIO(image_bytes))
        # AI 팀원 설정값: imgsz=1024, conf=0.25
        results = self.model.predict(source=img, imgsz=1024, conf=0.25, verbose=False)
        
        counts = {"acne": 0, "redness": 0, "wrinkle": 0, "spot": 0}
        for r in results:
            for b in r.boxes:
                cls_id = str(int(b.cls[0].item()))
                class_name = self.class_map.get(cls_id)
                if class_name in counts:
                    counts[class_name] += 1
        return counts

# 싱글톤 객체 생성
skin_ai = SkinAI()