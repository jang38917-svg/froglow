"""merge heads

Revision ID: 257794540afe
Revises: b2cefa02701e, d4b382e4981c, ddc8d0708d44
Create Date: 2026-03-03 22:32:55.219231

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '257794540afe'
down_revision: Union[str, Sequence[str], None] = ('b2cefa02701e', 'd4b382e4981c', 'ddc8d0708d44')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    pass


def downgrade() -> None:
    """Downgrade schema."""
    pass
