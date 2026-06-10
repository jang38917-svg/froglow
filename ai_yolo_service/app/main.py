import json
from fastapi import FastAPI, HTTPException, UploadFile, File, Form

from app.services.detector import run_inference_from_upload
from app.core.config import DEFAULT_CONF, DEFAULT_IOU

app = FastAPI(title="YOLO Skin Detection Service")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/ready")
def ready():
    return {"ready": True}


@app.post("/analyze")
async def analyze(
    file: UploadFile = File(...),
    conf: float = Form(DEFAULT_CONF),
    iou: float = Form(DEFAULT_IOU),
):
    try:
        result = await run_inference_from_upload(
            upload_file=file,
            conf=conf,
            iou=iou,
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Inference failed: {str(e)}")