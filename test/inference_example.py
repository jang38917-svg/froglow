from ultralytics import YOLO
from pathlib import Path
import json

CLASS_MAP = {
    0: "acne",
    1: "redness",
    2: "wrinkle",
    3: "spot",
}

def run_inference(
    model_path: str,
    image_path: str,
    imgsz: int = 1024,
    conf: float = 0.25,
):
    model = YOLO(model_path)
    results = model.predict(source=image_path, imgsz=imgsz, conf=conf, verbose=False)

    detections = []

    for r in results:
        if r.boxes is None:
            continue

        for b in r.boxes:
            cls_id = int(b.cls[0].item())
            confidence = float(b.conf[0].item())
            x1, y1, x2, y2 = map(float, b.xyxy[0].tolist())

            detections.append({
                "class_id": cls_id,
                "class_name": CLASS_MAP.get(cls_id, f"unknown_{cls_id}"),
                "confidence": round(confidence, 4),
                "bbox": {
                    "x1": round(x1, 2),
                    "y1": round(y1, 2),
                    "x2": round(x2, 2),
                    "y2": round(y2, 2),
                }
            })

    output = {
        "image": Path(image_path).name,
        "model": Path(model_path).name,
        "imgsz": imgsz,
        "confidence_threshold": conf,
        "detections": detections,
        "count": len(detections),
    }

    return output


if __name__ == "__main__":
    model_path = "best.pt"
    image_path = "test.jpg"

    result = run_inference(
        model_path=model_path,
        image_path=image_path,
        imgsz=1024,
        conf=0.25,
    )

    print(json.dumps(result, indent=2, ensure_ascii=False))

    with open("sample_prediction.json", "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)

    print("saved: sample_prediction.json")
