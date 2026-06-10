# Froglow YOLO AI Server

Froglow 졸업작품에서 사용하는 피부 문제 탐지 AI 서버입니다.
사용자가 촬영한 얼굴 이미지를 입력받아 YOLOv8 모델을 기반으로 여드름, 잡티, 주름 등의 피부 문제를 탐지합니다.

이 서버는 메인 백엔드 서버와 분리된 독립 FastAPI 서버로 동작하며, 백엔드 서버에서 이미지 분석 요청을 보내면 탐지 결과를 JSON 형태로 반환합니다.

---

## 1. 프로젝트 개요

Froglow는 사용자의 피부 상태를 분석하고, 분석 결과를 바탕으로 피부 루틴과 제품 추천을 제공하는 모바일 피부 관리 서비스입니다.

본 AI 서버는 Froglow 서비스의 피부 분석 기능 중 피부 문제 탐지 역할을 담당합니다.

### 담당 기능

* 얼굴 이미지 입력
* YOLOv8 기반 피부 문제 탐지
* 피부 문제별 bounding box 탐지
* 탐지 결과 JSON 반환
* 분석된 이미지 저장 또는 반환
* 메인 백엔드 서버와 API 통신

---

## 2. 주요 기능

### 2.1 피부 이미지 분석

사용자가 촬영한 얼굴 이미지를 입력받아 YOLOv8 모델로 피부 문제를 탐지합니다.

탐지 대상 클래스는 다음과 같습니다.

| 클래스 번호 | 클래스명    | 설명  |
| ------ | ------- | --- |
| 0      | acne    | 여드름 |
| 1      | spot    | 잡티  |
| 2      | wrinkle | 주름  |

> 현재 모델은 redness 클래스를 제외한 3개 클래스를 기준으로 학습되었습니다.

---

### 2.2 탐지 결과 반환

이미지 분석 결과는 JSON 형태로 반환됩니다.

반환 데이터에는 다음 정보가 포함될 수 있습니다.

* 탐지된 피부 문제 클래스
* confidence 값
* bounding box 좌표
* 클래스별 탐지 개수
* 분석 이미지 경로
* annotated image 경로

---

### 2.3 메인 백엔드 서버 연동

메인 백엔드 서버는 사용자의 진단 요청을 받은 후, 저장된 이미지 경로 또는 이미지 파일을 이 AI 서버로 전달합니다.

AI 서버는 이미지를 분석한 뒤 탐지 결과를 반환하고, 메인 백엔드 서버는 이 결과를 피부 점수화 서버와 함께 사용하여 최종 피부 진단 결과를 생성합니다.

---

## 3. 기술 스택

| 구분               | 기술             |
| ---------------- | -------------- |
| Language         | Python         |
| Web Framework    | FastAPI        |
| AI Model         | YOLOv8         |
| AI Library       | Ultralytics    |
| Image Processing | OpenCV, Pillow |
| Server           | Uvicorn        |
| Container        | Docker         |

---

## 4. 프로젝트 구조

```txt
froglow-yolo-ai-server/
├── app/
│   ├── main.py
│   ├── yolo_analyzer.py
│   └── ...
├── weights/
│   └── best.pt
├── requirements.txt
├── Dockerfile
├── .dockerignore
├── .gitignore
└── README.md
```

> 실제 파일 구조는 개발 환경에 따라 일부 다를 수 있습니다.

---

## 5. 실행 전 준비 사항

### 5.1 Python 설치

Python 3.10 이상 버전을 권장합니다.

```bash
python --version
```

---

### 5.2 가상환경 생성

Windows PowerShell 기준:

```bash
python -m venv .venv
.venv\Scripts\activate
```

macOS / Linux 기준:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

---

### 5.3 패키지 설치

```bash
pip install -r requirements.txt
```

---

## 6. 모델 파일 준비

학습된 YOLO 모델 파일을 아래 경로에 넣어야 합니다.

```txt
weights/best.pt
```

예시:

```txt
froglow-yolo-ai-server/
└── weights/
    └── best.pt
```

### 주의사항

`best.pt` 파일은 용량 문제로 GitHub에 포함하지 않는 것을 권장합니다.

따라서 GitHub에서 프로젝트를 clone한 뒤에는 직접 학습된 모델 파일을 `weights/` 폴더에 넣어야 합니다.

---

## 7. 서버 실행 방법

### 7.1 로컬 실행

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

서버가 정상적으로 실행되면 아래 주소에서 API 문서를 확인할 수 있습니다.

```txt
http://127.0.0.1:8001/docs
```

---

### 7.2 Docker 실행

Docker 이미지 빌드:

```bash
docker build -t froglow-yolo-ai-server .
```

Docker 컨테이너 실행:

```bash
docker run -p 8001:8001 froglow-yolo-ai-server
```

실행 후 접속 주소:

```txt
http://127.0.0.1:8001/docs
```

---

## 8. API 사용 예시

### 이미지 분석 요청

```bash
curl -X POST "http://127.0.0.1:8001/analyze" \
  -F "image=@test.jpg"
```

### 응답 예시

```json
{
  "success": true,
  "detections": [
    {
      "class_id": 0,
      "class_name": "acne",
      "confidence": 0.87,
      "bbox": [120, 85, 180, 145]
    },
    {
      "class_id": 1,
      "class_name": "spot",
      "confidence": 0.76,
      "bbox": [240, 130, 285, 175]
    }
  ],
  "counts": {
    "acne": 1,
    "spot": 1,
    "wrinkle": 0
  }
}
```

> 실제 응답 구조는 서버 코드에 따라 달라질 수 있습니다.

---

## 9. 메인 서버와의 연동 방식

Froglow 메인 백엔드 서버는 피부 진단 과정에서 이 YOLO AI 서버에 이미지 분석 요청을 보냅니다.

기본 연동 주소 예시는 다음과 같습니다.

```txt
http://host.docker.internal:8001/analyze
```

Docker 환경에서 메인 백엔드 서버와 AI 서버를 함께 실행하는 경우, 각 컨테이너의 네트워크 설정에 따라 주소가 달라질 수 있습니다.

---

## 10. GitHub 업로드 시 제외할 파일

다음 파일 및 폴더는 GitHub에 올리지 않는 것을 권장합니다.

```gitignore
.venv/
venv/
__pycache__/
.env
data/
uploads/
runs/
outputs/
*.pt
*.pth
*.onnx
*.engine
```

특히 아래 파일은 GitHub에 직접 업로드하지 않는 것을 권장합니다.

```txt
weights/best.pt
```

---

## 11. 실행 흐름

```txt
1. 사용자가 앱에서 얼굴 사진 촬영
2. 메인 백엔드 서버가 이미지 저장
3. 메인 백엔드 서버가 YOLO AI 서버로 분석 요청
4. YOLO AI 서버가 피부 문제 탐지
5. 탐지 결과를 JSON 형태로 반환
6. 메인 백엔드 서버가 점수화 서버와 함께 최종 진단 결과 생성
```

---

## 12. 프로젝트 목적

이 서버는 졸업작품 Froglow의 AI 기반 피부 분석 기능을 구현하기 위해 제작되었습니다.

사용자가 직접 피부 상태를 확인하고, 분석 결과를 기반으로 루틴 및 제품 추천을 받을 수 있도록 하는 것이 목적입니다.

---

## 13. 참고 사항

* 본 서버는 졸업작품 및 학습 목적의 프로젝트입니다.
* 분석 결과는 의료 진단이 아닌 피부 관리 참고용입니다.
* 실제 서비스 적용 시에는 더 많은 데이터와 검증 과정이 필요합니다.
