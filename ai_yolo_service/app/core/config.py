import os

MODEL_PATH = os.getenv("YOLO_MODEL_PATH", "best.pt")
CLASS_MAP_PATH = os.getenv("CLASS_MAP_PATH", "class_map.json")

DEFAULT_CONF = float(os.getenv("YOLO_CONF", "0.25"))
DEFAULT_IOU = float(os.getenv("YOLO_IOU", "0.45"))

MAX_IMAGE_SIZE = int(os.getenv("YOLO_MAX_SIZE", "1024"))