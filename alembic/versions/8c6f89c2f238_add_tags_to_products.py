"""add_tags_to_products

Revision ID: 8c6f89c2f238
Revises: b17bd6ce0d1e
Create Date: 2026-02-10 07:24:37.589657

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '8c6f89c2f238'
down_revision: Union[str, Sequence[str], None] = 'b17bd6ce0d1e'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    pass


def downgrade() -> None:
    """Downgrade schema."""
    pass
