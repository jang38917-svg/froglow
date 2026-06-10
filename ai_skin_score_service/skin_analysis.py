import os
from io import BytesIO

os.environ["CUDA_VISIBLE_DEVICES"] = "-1"

import tensorflow as tf
from huggingface_hub import login, snapshot_download
from PIL import Image
import numpy as np
from dotenv import load_dotenv

load_dotenv()

HF_TOKEN = os.getenv("HF_TOKEN")
if not HF_TOKEN:
    raise RuntimeError("HF_TOKEN 환경변수가 설정되어야 합니다.")

login(token=HF_TOKEN)

_loaded_model = None

FEATURE_NAME_KO = {
    "acne": "여드름",
    "spots": "잡티",
    "redness": "홍조",
    "wrinkles": "주름"
}

GRADE_LABELS = {
    "A": "매우 양호",
    "B": "양호",
    "C": "보통",
    "D": "주의",
    "E": "심함"
}


def load_derm_model():
    global _loaded_model

    if _loaded_model is not None:
        return _loaded_model

    print("🔄 derm-foundation 모델 다운로드 중...")
    model_root = snapshot_download(repo_id="google/derm-foundation")

    print("🔄 TensorFlow 모델 로드 중...")
    _loaded_model = tf.saved_model.load(model_root)

    print("✅ derm-foundation 모델 로드 완료!")
    return _loaded_model


def extract_derm_embedding_from_bytes(image_bytes: bytes):
    model = load_derm_model()

    try:
        img = Image.open(BytesIO(image_bytes))
        img.load()
    except Exception as e:
        raise RuntimeError(
            f"업로드된 이미지 파일을 열 수 없습니다. "
            f"실제 JPEG/PNG 파일인지 확인하세요. "
            f"원본 에러: {e}"
        )

    img = img.convert("RGB")
    img = img.resize((224, 224), Image.Resampling.LANCZOS)

    buf = BytesIO()
    img.save(buf, format="PNG")
    image_bytes_png = buf.getvalue()

    input_tensor = tf.train.Example(
        features=tf.train.Features(
            feature={
                "image/encoded": tf.train.Feature(
                    bytes_list=tf.train.BytesList(value=[image_bytes_png])
                )
            }
        )
    ).SerializeToString()

    infer = model.signatures["serving_default"]
    output = infer(inputs=tf.constant([input_tensor]))

    if "embedding" in output:
        embedding = output["embedding"].numpy().flatten()
    else:
        key = list(output.keys())[0]
        embedding = output[key].numpy().flatten()

    return embedding


def score_to_grade(score: float) -> str:
    score = max(0.0, min(10.0, float(score)))

    if score < 2.0:
        return "A"
    elif score < 4.0:
        return "B"
    elif score < 6.0:
        return "C"
    elif score < 8.0:
        return "D"
    else:
        return "E"


def get_grade_description(feature_key: str, grade: str) -> str:
    name = FEATURE_NAME_KO.get(feature_key, feature_key)

    desc_map = {
        "A": f"{name} 상태가 매우 양호합니다.",
        "B": f"{name} 상태가 양호한 편입니다.",
        "C": f"{name} 상태가 보통 수준입니다.",
        "D": f"{name} 상태에 주의가 필요합니다.",
        "E": f"{name} 상태가 심한 편입니다."
    }

    return desc_map[grade]


def build_feature_result(feature_key: str, score: float) -> dict:
    score = max(0.0, min(10.0, round(float(score), 1)))
    grade = score_to_grade(score)

    return {
        "score": score,
        "grade": grade,
        "label": GRADE_LABELS[grade],
        "description": get_grade_description(feature_key, grade)
    }


def analyze_skin_from_bytes(image_bytes: bytes, detections: list, yolo_weight: float = 0.7, derm_weight: float = 0.3) -> dict:
    embedding = extract_derm_embedding_from_bytes(image_bytes)

    emb_mean = float(np.mean(embedding))
    emb_std = float(np.std(embedding))
    emb_abs_mean = float(np.mean(np.abs(embedding)))
    emb_max = float(np.max(embedding))
    emb_min = float(np.min(embedding))

    def clamp(x):
        return max(0.0, min(10.0, round(float(x), 1)))

    derm_scores = {
        "acne": clamp(5.0 + (emb_std * 8.0) + (emb_mean * 2.0)),
        "spots": clamp(5.0 + (emb_abs_mean * 6.0) + (emb_min * 1.5)),
        "redness": clamp(5.0 + (emb_std * 6.0) + (emb_max * 1.5)),
        "wrinkles": clamp(5.0 + (emb_abs_mean * 5.0) + (emb_mean * 1.0)),
    }

    yolo_to_feature = {
        "acne": "acne",
        "redness": "redness",
        "spot": "spots",
        "spots": "spots",
        "wrinkle": "wrinkles",
        "wrinkles": "wrinkles"
    }

    grouped = {
        "acne": [],
        "spots": [],
        "redness": [],
        "wrinkles": []
    }

    for d in detections:
        cls = d.get("label") or d.get("class_name")
        conf = float(d.get("confidence", 0.0))

        if not cls:
            continue

        key = yolo_to_feature.get(str(cls).lower())
        if key:
            grouped[key].append(conf)

    yolo_scores = {}
    for key, confs in grouped.items():
        if not confs:
            score = 0.0
        else:
            count = len(confs)
            avg = float(np.mean(confs))
            score = (count * 1.5) + (avg * 5.0)

        yolo_scores[key] = clamp(score)

    raw_scores = {}
    for key in ["acne", "spots", "redness", "wrinkles"]:
        final = (yolo_scores[key] * yolo_weight) + (derm_scores[key] * derm_weight)
        raw_scores[key] = clamp(final)

    analysis = {
        key: build_feature_result(key, raw_scores[key])
        for key in ["acne", "spots", "redness", "wrinkles"]
    }

    return {
        "analysis": analysis,
        "raw_scores": raw_scores
    }