from fastapi import FastAPI, HTTPException, UploadFile, File, Form
import json
import traceback

from skin_analysis import analyze_skin_from_bytes

app = FastAPI(title="Skin Score AI Service")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analyze")
async def analyze(
    file: UploadFile = File(...),
    detections: str = Form(...)
):
    try:
        image_bytes = await file.read()
        parsed_detections = json.loads(detections)

        result = analyze_skin_from_bytes(
            image_bytes=image_bytes,
            detections=parsed_detections
        )
        return result

    except Exception as e:
        print("=== ANALYZE ERROR ===")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))