from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session
from sqlalchemy import select, or_
from jose import JWTError

from app.db.session import get_db
from app.models.user import User

from app.core.security import (
    hash_password,
    verify_password,
    create_access_token,
    decode_access_token,
)

from app.schemas.auth import (
    LoginRequest,
    LoginResponse,
    RegisterRequest,
    RegisterResponse,
)

router = APIRouter(prefix="/auth", tags=["auth"])

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")


@router.post("/register", response_model=RegisterResponse, status_code=201)
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    """
    회원가입
    - email / login_id 중복 체크
    - 비밀번호는 bcrypt로 해시해서 저장
    - survey_completed_at은 None (설문 미완료 상태)
    """

    if len(payload.password.encode("utf-8")) > 72:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="비밀번호는 72이하만 가능합니다.",
        )

    email = payload.email.strip()
    login_id = payload.login_id.strip()

    # DEBUG: 서버가 실제로 받은 password 길이/내용 일부 확인 (응답/변수 변경 없음)
    print("DEBUG register password bytes =", len(payload.password.encode("utf-8")))
    print("DEBUG register password repr  =", repr(payload.password[:120]))

    # 1) 중복 체크 (email OR login_id)
    stmt = select(User).where(
        or_(
            User.email == email,
            User.login_id == login_id,
        )
    )
    existing = db.execute(stmt).scalar_one_or_none()

    if existing:
        if existing.email == email:
            raise HTTPException(status_code=400, detail="이미 사용 중인 이메일입니다.")
        if existing.login_id == login_id:
            raise HTTPException(status_code=400, detail="이미 사용 중인 로그인 아이디입니다.")
        raise HTTPException(status_code=400, detail="이미 존재하는 사용자입니다.")

    # 2) 유저 생성
    # 추가 안전장치: bcrypt ValueError를 400으로 변환 (응답/변수 변경 없음)
    try:
        hashed_pw = hash_password(payload.password)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="비밀번호는 72바이트 이하만 가능합니다.",
        )

    user = User(
        email=email,
        login_id=login_id,
        name=payload.name,
        gender=payload.gender,
        birth_date=payload.birth_date,
        hashed_password=hashed_pw,
        survey_completed_at=None,
    )

    db.add(user)
    db.commit()
    db.refresh(user)

    return RegisterResponse(
        message="회원가입 성공",
        id=user.id,
        login_id=user.login_id,
        email=user.email,
        name=user.name,
    )


@router.post("/login", response_model=LoginResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    """
    로그인
    - login_id + password 로 로그인
    - 성공 시 JWT 토큰 발급
    - needs_survey: 설문 미완료 여부
    """

    # bcrypt는 "72 bytes" 제한이 있음 (한글/이모지 포함 시 바이트가 커짐)
    # - 이 체크가 없으면 verify_password에서 500 에러(ValueError)로 터질 수 있음
    if len(payload.password.encode("utf-8")) > 72:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="비밀번호는 72바이트 이하만 가능합니다. (한글/이모지 포함 시 더 짧아질 수 있어요)",
        )

    # DEBUG: 서버가 실제로 받은 password 길이/내용 일부 확인 (응답/변수 변경 없음)
    print("DEBUG login password bytes =", len(payload.password.encode("utf-8")))
    print("DEBUG login password repr  =", repr(payload.password[:120]))

    # schemas/auth.py에서 login_id로 통일했으니 여기도 login_id로 받아야 함
    login_id = payload.login_id.strip()

    # 1) 사용자 조회
    stmt = select(User).where(User.login_id == login_id)
    user = db.execute(stmt).scalar_one_or_none()

    # 2) 비밀번호 검증
    if not user or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="아이디 또는 비밀번호가 올바르지 않습니다.",
        )

    # 3) 설문 여부 판단
    needs_survey = user.survey_completed_at is None

    # 4) 토큰 생성 (sub에 user.id 저장)
    access_token = create_access_token(subject=str(user.id))

    return LoginResponse(
        message="로그인 성공",
        access_token=access_token,
        token_type="bearer",
        needs_survey=needs_survey,
        login_id=user.login_id,
        email=user.email,
        name=user.name,
    )


def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
) -> User:
    """
    토큰으로 현재 유저 가져오기
    - 설문 제출 같은 보호 API에서 Depends(get_current_user)로 사용
    """
    try:
        payload = decode_access_token(token)
        user_id = payload.get("sub")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token (no subject)")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    user = db.get(User, int(user_id))
    if not user:
        raise HTTPException(status_code=401, detail="User not found")

    return user
