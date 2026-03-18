import os
import sys
from logging.config import fileConfig

from alembic import context
from dotenv import load_dotenv
from sqlalchemy import engine_from_config, pool

load_dotenv()  # 프로젝트 루트의 .env 자동 로드

# app 패키지를 찾기 위해 프로젝트 루트 경로를 sys.path에 추가
# alembic 폴더는 프로젝트 루트 바로 아래에 있으므로 ".."가 루트입니다.
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

# Alembic Config 객체
config = context.config

# 로깅 설정
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

# Base.metadata를 target_metadata로 설정해야 autogenerate가 작동합니다.
from app.db.base import Base  # noqa: E402

# 모델을 import 해야 Alembic이 테이블을 "인지"합니다.
# import만 해도 Base.metadata에 테이블 정보가 등록됩니다.
from app.models import user  # noqa: F401, E402
from app.models import survey  # noqa: F401, E402
from app.models import product  # noqa: F401, E402
from app.models import product_skin_type  # noqa: F401, E402
from app.models import routine  # noqa: F401, E402
from app.models import reminder  # noqa: F401, E402

target_metadata = Base.metadata


def get_database_url() -> str:
    """
    Alembic이 사용할 DB URL을 .env의 DATABASE_URL에서 읽어옵니다.
    Docker 환경에서는 보통 @db:5432/... 형태가 됩니다.
    """
    url = os.getenv("DATABASE_URL")
    if not url:
        raise RuntimeError(
            "DATABASE_URL is not set. Check your .env file. "
            "Example: postgresql+psycopg2://user:pass@db:5432/dbname"
        )
    return url


def run_migrations_offline() -> None:
    """Offline 모드: DB 연결 없이 SQL 스크립트 생성/출력에 사용"""
    url = get_database_url()
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
        compare_type=True,
    )

    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    """Online 모드: 실제 DB에 연결해서 마이그레이션 수행"""
    configuration = config.get_section(config.config_ini_section) or {}

    # alembic.ini에 뭐가 적혀있든 상관없이 DATABASE_URL로 덮어씁니다.
    configuration["sqlalchemy.url"] = get_database_url()

    connectable = engine_from_config(
        configuration,
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )

    with connectable.connect() as connection:
        context.configure(
            connection=connection,
            target_metadata=target_metadata,
            compare_type=True,
        )

        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()