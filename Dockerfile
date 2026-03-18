FROM python:3.11-slim

WORKDIR /app

# 1. 필수 시스템 패키지 설치 (패키지명 수정 완료)
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    libgl1 \
    libglib2.0-0 \
 && rm -rf /var/lib/apt/lists/*

# 2. pip 자체를 최신으로 업데이트
RUN pip install --no-cache-dir --upgrade pip setuptools wheel

# 3. 요구사항 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 4. 소스 코드 복사
COPY . .

EXPOSE 8000

# 5. 실행 명령
CMD ["uvicorn", "app.main:app", "--host=0.0.0.0", "--port=8000", "--reload"]