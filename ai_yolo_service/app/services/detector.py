import os
import io
import json
import base64
import tempfile

import numpy as np
from PIL import Image
from ultralytics import YOLO
from fastapi import UploadFile

from app.core.config import MODEL_PATH, CLASS_MAP_PATH
from app.utils.transform import calculate_area, calculate_center


if not os.path.exists(MODEL_PATH):
    raise RuntimeError(f"Model not found: {MODEL_PATH}")

model = YOLO(MODEL_PATH)

if os.path.exists(CLASS_MAP_PATH):
    with open(CLASS_MAP_PATH, "r", encoding="utf-8") as f:
        CLASS_MAP = json.load(f)
else:
    CLASS_MAP = {}


def _normalize_to_temp_jpg(image_bytes: bytes) -> str:
    img = Image.open(io.BytesIO(image_bytes)).convert("RGB")

    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".jpg")
    tmp_path = tmp.name
    tmp.close()

    img.save(tmp_path, format="JPEG", quality=95)
    return tmp_path


def _encode_annotated_image(result) -> str:
    plotted = result.plot()  # numpy array (BGR)
    if plotted is None:
        return ""

    # result.plot() 결과를 PIL 이미지로 변환
    plotted_rgb = plotted[..., ::-1]  # BGR -> RGB
    img = Image.fromarray(plotted_rgb.astype(np.uint8))

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=95)
    return base64.b64encode(buf.getvalue()).decode("utf-8")


async def run_inference_from_upload(upload_file: UploadFile, conf: float, iou: float):
    image_bytes = await upload_file.read()
    temp_image_path = None

    try:
        temp_image_path = _normalize_to_temp_jpg(image_bytes)

        results = model.predict(
            source=temp_image_path,
            conf=conf,
            iou=iou,
            verbose=False
        )

        if not results:
            return {
                "filename": upload_file.filename,
                "count": 0,
                "detections": [],
                "summary": {},
                "annotated_image_base64": ""
            }

        result = results[0]
        detections = []
        class_stats = {}

        if result.boxes is None:
            return {
                "filename": upload_file.filename,
                "count": 0,
                "detections": [],
                "summary": {},
                "annotated_image_base64": _encode_annotated_image(result)
            }

        for box in result.boxes:
            xyxy = box.xyxy[0].tolist()
            conf_score = float(box.conf[0])
            cls_id = int(box.cls[0])

            label = CLASS_MAP.get(str(cls_id), str(cls_id))

            x1, y1, x2, y2 = map(float, xyxy)

            area = calculate_area(x1, y1, x2, y2)
            center = calculate_center(x1, y1, x2, y2)

            detections.append({
                "class_id": cls_id,
                "label": label,
                "confidence": round(conf_score, 4),
                "bbox": {
                    "x1": round(x1, 2),
                    "y1": round(y1, 2),
                    "x2": round(x2, 2),
                    "y2": round(y2, 2),
                },
                "area": round(area, 2),
                "center": center
            })

            if label not in class_stats:
                class_stats[label] = {"count": 0, "avg_conf": 0.0}

            class_stats[label]["count"] += 1
            class_stats[label]["avg_conf"] += conf_score

        for label in class_stats:
            count = class_stats[label]["count"]
            class_stats[label]["avg_conf"] = round(
                class_stats[label]["avg_conf"] / count, 4
            )

        annotated_image_base64 = _encode_annotated_image(result)

        return {
            "filename": upload_file.filename,
            "count": len(detections),
            "detections": detections,
            "summary": class_stats,
            "annotated_image_base64": annotated_image_base64
        }

    finally:
        if temp_image_path and os.path.exists(temp_image_path):
            os.remove(temp_image_path)