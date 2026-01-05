from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import select, or_

from app.db.session import get_db
from app.models.user import User

from app.schemas.user import UserCreate, UserOut
from app.core.security import hash_password, verify_password

from app.schemas.auth import LoginRequest, LoginResponse

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/signup", response_model=UserOut, status_code=status.HTTP_201_CREATED)
def signup(payload: UserCreate, db: Session = Depends(get_db)):
    """
    회원가입
    - payload.id: 사용자가 입력한 로그인 아이디(별명)
    - DB에는 User.login_id 로 저장
    """

    # ✅ 중복 검사: login_id 또는 email
    exists = db.execute(
        select(User).where(
            or_(
                User.login_id == payload.id,
                User.email == payload.email,
            )
        )
    ).scalar_one_or_none()

    if exists:
        if exists.login_id == payload.id:
            raise HTTPException(status_code=409, detail="이미 사용 중인 아이디입니다.")
        raise HTTPException(status_code=409, detail="이미 사용 중인 이메일입니다.")

    hashed = hash_password(payload.password)

    user = User(
        name=payload.name,
        login_id=payload.id,          # ✅ 여기 중요
        email=payload.email,
        gender=payload.gender,
        birth_date=payload.birth_date,
        hashed_password=hashed,
    )

    db.add(user)
    db.commit()
    db.refresh(user)

    # ✅ UserOut의 id는 login_id를 내려줘야 함
    # 그런데 from_attributes=True라서 이름이 달라 매핑이 안 됨
    # → 그래서 여기서는 dict로 직접 매핑해서 반환
    return UserOut(
        id=user.login_id,
        name=user.name,
        email=user.email,
        gender=user.gender,
        birth_date=user.birth_date,
    )


@router.post("/login", response_model=LoginResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    """
    로그인
    - payload.id(별명) 또는 payload.email 로 로그인
    """

    # ✅ id / email 둘 중 하나는 필수
    if not payload.id and not payload.email:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="id 또는 email 중 하나는 필수입니다.",
        )

    if payload.id:
        stmt = select(User).where(User.login_id == payload.id)
    else:
        stmt = select(User).where(User.email == payload.email)

    user = db.execute(stmt).scalar_one_or_none()

    if not user or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="로그인 정보가 올바르지 않습니다.")

    return LoginResponse(
        message="로그인 성공",
        id=user.login_id,
        email=user.email,
        name=user.name,
    )
