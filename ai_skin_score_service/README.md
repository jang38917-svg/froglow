# Froglow Skin Score AI Server

Froglow 졸업작품에서 사용하는 피부 점수화 AI 서버입니다.
YOLO AI 서버에서 탐지한 피부 문제 결과를 기반으로 여드름, 잡티, 주름, 홍조 등의 피부 상태 점수를 계산하고, 계산된 점수를 등급으로 변환합니다.

이 서버는 메인 백엔드 서버와 분리된 독립 FastAPI 서버로 동작하며, 피부 진단 결과를 점수와 등급 형태로 변환하여 반환합니다.

---

## 1. 프로젝트 개요

Froglow는 사용자의 피부 상태를 분석하고, 분석 결과에 따라 피부 루틴과 제품 추천을 제공하는 모바일 피부 관리 서비스입니다.

본 서버는 Froglow 서비스의 피부 분석 과정 중 피부 상태 점수화 및 등급화 기능을 담당합니다.

YOLO AI 서버가 피부 문제를 탐지하면, 이 점수화 서버는 탐지 결과를 바탕으로 각 피부 항목별 점수를 계산합니다. 이후 계산된 점수를 A~E 등급으로 변환하여 사용자가 피부 상태를 쉽게 이해할 수 있도록 합니다.

---

## 2. 주요 기능

### 2.1 피부 문제별 점수 계산

YOLO AI 서버의 탐지 결과를 기반으로 피부 문제별 점수를 계산합니다.

점수 항목은 다음과 같습니다.

| 항목      | 설명     |
| ------- | ------ |
| acne    | 여드름 점수 |
| spot    | 잡티 점수  |
| wrinkle | 주름 점수  |
| redness | 홍조 점수  |

> 현재 YOLO 모델은 acne, spot, wrinkle 중심으로 동작하며, redness는 메인 백엔드 구조와의 호환을 위해 점수 항목에 포함될 수 있습니다.

---

### 2.2 피부 문제별 등급화

계산된 점수를 기준으로 각 피부 항목에 대한 등급을 생성합니다.

등급은 A부터 E까지로 구성됩니다.

| 등급 | 의미       |
| -- | -------- |
| A  | 매우 양호    |
| B  | 양호       |
| C  | 보통       |
| D  | 주의 필요    |
| E  | 집중 관리 필요 |

점수가 낮을수록 피부 상태가 좋은 것으로 판단하며, 점수가 높을수록 관리가 더 필요한 상태로 판단합니다.

---

### 2.3 주요 피부 고민 판단

피부 항목별 점수 중 가장 높은 점수를 가진 항목을 주요 피부 고민으로 판단합니다.

예를 들어 acne 점수가 가장 높으면 주요 피부 고민은 여드름으로 판단됩니다.

---

## 3. 기술 스택

| 구분              | 기술       |
| --------------- | -------- |
| Language        | Python   |
| Web Framework   | FastAPI  |
| Data Validation | Pydantic |
| Server          | Uvicorn  |
| Container       | Docker   |

---

## 4. 프로젝트 구조

```txt
ai_skin_score_service/
├── app.py
├── skin_analysis.py
├── requirements.txt
├── Dockerfile
├── .dockerignore
├── .gitignore
├── example_request.json
├── example_response.json
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

## 6. 서버 실행 방법

### 6.1 로컬 실행

```bash
uvicorn app:app --host 0.0.0.0 --port 8002 --reload
```

서버가 정상적으로 실행되면 아래 주소에서 API 문서를 확인할 수 있습니다.

```txt
http://127.0.0.1:8002/docs
```

---

### 6.2 Docker 실행

Docker 이미지 빌드:

```bash
docker build -t froglow-skin-score-server .
```

Docker 컨테이너 실행:

```bash
docker run -p 8002:8002 froglow-skin-score-server
```

실행 후 접속 주소:

```txt
http://127.0.0.1:8002/docs
```

---

## 7. API 사용 예시

### 7.1 점수화 요청

```bash
curl -X POST "http://127.0.0.1:8002/analyze" \
  -H "Content-Type: application/json" \
  -d @example_request.json
```

---

### 7.2 요청 예시

```json
{
  "detections": [
    {
      "class_name": "acne",
      "confidence": 0.87,
      "bbox": [120, 85, 180, 145]
    },
    {
      "class_name": "spot",
      "confidence": 0.76,
      "bbox": [240, 130, 285, 175]
    },
    {
      "class_name": "wrinkle",
      "confidence": 0.69,
      "bbox": [310, 200, 360, 245]
    }
  ]
}
```

---

### 7.3 응답 예시

```json
{
  "success": true,
  "scores": {
    "acne": 7,
    "spot": 4,
    "wrinkle": 3,
    "redness": 1
  },
  "grades": {
    "acne": "D",
    "spot": "B",
    "wrinkle": "B",
    "redness": "A"
  },
  "primary_concern": "acne"
}
```

> 실제 요청 및 응답 구조는 서버 코드에 따라 일부 달라질 수 있습니다.

---

## 8. 점수 기준

피부 점수는 1점부터 10점까지의 범위를 사용합니다.

| 점수  | 의미           |
| --- | ------------ |
| 1점  | 피부 문제가 거의 없음 |
| 10점 | 피부 문제가 심한 상태 |

즉, 점수가 낮을수록 좋은 피부 상태이고, 점수가 높을수록 관리가 더 필요한 상태를 의미합니다.

---

## 9. 등급 기준

Score 서버는 계산된 점수를 기준으로 A~E 등급을 생성합니다.

| 점수 구간 | 등급 | 해석       |
| ----- | -- | -------- |
| 1~2점  | A  | 매우 양호    |
| 3~4점  | B  | 양호       |
| 5~6점  | C  | 보통       |
| 7~8점  | D  | 주의 필요    |
| 9~10점 | E  | 집중 관리 필요 |

예를 들어 acne 점수가 7점이면 acne 등급은 D로 판단됩니다.

```json
{
  "acne": 7,
  "grade": "D"
}
```

---

## 10. 주요 피부 고민 판단 방식

Score 서버는 항목별 점수 중 가장 높은 점수를 가진 항목을 기준으로 주요 피부 고민을 판단합니다.

예시:

```json
{
  "scores": {
    "acne": 7,
    "spot": 4,
    "wrinkle": 3,
    "redness": 1
  },
  "grades": {
    "acne": "D",
    "spot": "B",
    "wrinkle": "B",
    "redness": "A"
  },
  "primary_concern": "acne"
}
```

위 예시에서는 acne 점수가 가장 높기 때문에 주요 피부 고민은 여드름으로 판단됩니다.

---

## 11. 메인 서버와의 연동 방식

Froglow 메인 백엔드 서버는 YOLO AI 서버의 탐지 결과를 바탕으로 이 점수화 서버에 분석 요청을 보냅니다.

기본 연동 주소 예시는 다음과 같습니다.

```txt
http://host.docker.internal:8002/analyze
```

점수화 서버는 다음과 같은 정보를 메인 백엔드 서버에 반환합니다.

* 피부 항목별 점수
* 피부 항목별 등급
* 주요 피부 고민
* 점수화 결과 요약

메인 백엔드 서버는 이 결과를 바탕으로 다음 기능을 수행합니다.

* 피부 진단 결과 저장
* 주요 피부 고민 판단
* 피부 타입 및 고민 기반 제품 추천
* 루틴 추천 생성
* 통계 화면 데이터 제공

---

## 12. 전체 분석 흐름

```txt
1. 사용자가 앱에서 얼굴 사진 촬영
2. 메인 백엔드 서버가 이미지를 저장
3. 메인 백엔드 서버가 YOLO AI 서버로 이미지 분석 요청
4. YOLO AI 서버가 피부 문제 탐지 결과 반환
5. 메인 백엔드 서버가 탐지 결과를 점수화 서버로 전달
6. 점수화 서버가 항목별 피부 점수 계산
7. 점수화 서버가 점수를 기준으로 등급 생성
8. 점수화 서버가 주요 피부 고민 판단
9. 메인 백엔드 서버가 최종 진단 결과 저장
10. 앱에서 진단 결과, 제품 추천, 루틴 추천, 통계 화면 제공
```

---

## 13. GitHub 업로드 시 제외할 파일

다음 파일 및 폴더는 GitHub에 올리지 않는 것을 권장합니다.

```gitignore
.venv/
venv/
__pycache__/
.pytest_cache/
.env
data/
uploads/
outputs/
*.log
```

---

## 14. `.env` 파일 관리

환경변수 파일이 있는 경우 GitHub에 직접 업로드하지 않습니다.

예를 들어 `.env` 파일이 있다면 `.gitignore`에 반드시 포함해야 합니다.

```gitignore
.env
```

필요한 경우 `.env.example` 파일을 만들어 예시 값만 공유합니다.

```env
SERVER_PORT=8002
```

---

## 15. 실행 확인 방법

서버 실행 후 Swagger 문서에 접속합니다.

```txt
http://127.0.0.1:8002/docs
```

`/analyze` API를 선택한 뒤 예시 JSON 데이터를 입력하여 점수화 결과가 정상적으로 반환되는지 확인합니다.

정상 응답에는 일반적으로 다음 정보가 포함됩니다.

```json
{
  "scores": {
    "acne": 7,
    "spot": 4,
    "wrinkle": 3,
    "redness": 1
  },
  "grades": {
    "acne": "D",
    "spot": "B",
    "wrinkle": "B",
    "redness": "A"
  },
  "primary_concern": "acne"
}
```

---

## 16. 프로젝트 목적

이 서버는 Froglow 졸업작품에서 피부 분석 결과를 사용자가 이해하기 쉬운 점수와 등급 형태로 제공하기 위해 제작되었습니다.

단순히 탐지 결과를 보여주는 것이 아니라, 항목별 점수와 등급을 통해 사용자가 자신의 피부 상태를 쉽게 확인하고 관리 방향을 잡을 수 있도록 하는 것이 목적입니다.

---

## 17. 참고 사항

* 본 서버는 졸업작품 및 학습 목적의 프로젝트입니다.
* 점수 및 등급 결과는 의료 진단이 아닌 피부 관리 참고용입니다.
* 실제 서비스 적용 시에는 더 많은 데이터, 사용자 피드백, 피부 전문가 검증 과정이 필요합니다.
* 피부 상태는 촬영 환경, 조명, 얼굴 각도, 이미지 품질에 따라 결과가 달라질 수 있습니다.
