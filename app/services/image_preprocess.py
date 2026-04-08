from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Dict

import cv2
import numpy as np


DEFAULT_MAX_SIZE = 1280
DEFAULT_JPEG_QUALITY = 95

DEFAULT_CLAHE_CLIP_LIMIT = 2.0
DEFAULT_CLAHE_TILE_GRID_SIZE = (8, 8)

DEFAULT_ENABLE_SHARPEN = False

SHARPEN_KERNEL = np.array(
    [
        [0, -0.5, 0],
        [-0.5, 3.0, -0.5],
        [0, -0.5, 0],
    ],
    dtype=np.float32,
)


@dataclass(frozen=True)
class PreprocessResult:
    original_path: str
    processed_path: str
    width: int
    height: int


def _read_image(image_path: str) -> np.ndarray:
    path = Path(image_path)
    if not path.exists():
        raise FileNotFoundError(f"이미지 파일이 존재하지 않습니다: {image_path}")

    data = np.fromfile(str(path), dtype=np.uint8)
    if data.size == 0:
        raise ValueError(f"이미지 파일을 읽을 수 없습니다: {image_path}")

    image = cv2.imdecode(data, cv2.IMREAD_COLOR)
    if image is None:
        raise ValueError(f"이미지 디코딩에 실패했습니다: {image_path}")

    return image


def _write_image(image_path: str, image: np.ndarray, jpeg_quality: int = DEFAULT_JPEG_QUALITY) -> None:
    suffix = Path(image_path).suffix.lower()

    if suffix not in {".jpg", ".jpeg", ".png"}:
        raise ValueError(f"지원하지 않는 이미지 확장자입니다: {suffix}")

    if suffix in {".jpg", ".jpeg"}:
        ok, encoded = cv2.imencode(
            ".jpg",
            image,
            [int(cv2.IMWRITE_JPEG_QUALITY), int(jpeg_quality)],
        )
    else:
        ok, encoded = cv2.imencode(".png", image)

    if not ok or encoded is None:
        raise ValueError(f"이미지 인코딩에 실패했습니다: {image_path}")

    encoded.tofile(image_path)


def _resize_keep_aspect(image: np.ndarray, max_size: int = DEFAULT_MAX_SIZE) -> np.ndarray:
    height, width = image.shape[:2]
    long_side = max(height, width)

    if long_side <= max_size:
        return image

    scale = max_size / float(long_side)
    new_width = int(width * scale)
    new_height = int(height * scale)

    resized = cv2.resize(
        image,
        (new_width, new_height),
        interpolation=cv2.INTER_AREA,
    )
    return resized


def _apply_clahe_to_l_channel(image_bgr: np.ndarray) -> np.ndarray:
    lab = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2LAB)
    l_channel, a_channel, b_channel = cv2.split(lab)

    clahe = cv2.createCLAHE(
        clipLimit=DEFAULT_CLAHE_CLIP_LIMIT,
        tileGridSize=DEFAULT_CLAHE_TILE_GRID_SIZE,
    )
    l_channel = clahe.apply(l_channel)

    merged = cv2.merge((l_channel, a_channel, b_channel))
    result = cv2.cvtColor(merged, cv2.COLOR_LAB2BGR)
    return result


def _apply_mild_sharpen(image_bgr: np.ndarray) -> np.ndarray:
    sharpened = cv2.filter2D(image_bgr, ddepth=-1, kernel=SHARPEN_KERNEL)
    sharpened = np.clip(sharpened, 0, 255).astype(np.uint8)
    return sharpened


def build_preprocessed_path(original_path: str) -> str:
    path = Path(original_path)
    return str(path.with_name(f"{path.stem}_preprocessed.jpg"))


def preprocess_image(
    original_path: str,
    processed_path: str | None = None,
    *,
    max_size: int = DEFAULT_MAX_SIZE,
    enable_sharpen: bool = DEFAULT_ENABLE_SHARPEN,
    overwrite: bool = True,
) -> PreprocessResult:
    original = _read_image(original_path)
    processed = _resize_keep_aspect(original, max_size=max_size)
    processed = _apply_clahe_to_l_channel(processed)

    if enable_sharpen:
        processed = _apply_mild_sharpen(processed)

    save_path = processed_path or build_preprocessed_path(original_path)

    save_file = Path(save_path)
    save_file.parent.mkdir(parents=True, exist_ok=True)

    if save_file.exists() and not overwrite:
        existing = _read_image(str(save_file))
        h, w = existing.shape[:2]
        return PreprocessResult(
            original_path=original_path,
            processed_path=str(save_file),
            width=w,
            height=h,
        )

    _write_image(str(save_file), processed)

    h, w = processed.shape[:2]
    return PreprocessResult(
        original_path=original_path,
        processed_path=str(save_file),
        width=w,
        height=h,
    )


def preprocess_image_map(
    image_paths: Dict[str, str],
    *,
    max_size: int = DEFAULT_MAX_SIZE,
    enable_sharpen: bool = DEFAULT_ENABLE_SHARPEN,
    overwrite: bool = True,
) -> Dict[str, PreprocessResult]:
    results: Dict[str, PreprocessResult] = {}

    for key, image_path in image_paths.items():
        results[key] = preprocess_image(
            original_path=image_path,
            max_size=max_size,
            enable_sharpen=enable_sharpen,
            overwrite=overwrite,
        )

    return results