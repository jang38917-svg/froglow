"""add routine key to reminders

Revision ID: 58ba19f012be
Revises: c5377eaf72ef
Create Date: 2026-04-15 19:00:00.000000

"""
from alembic import op
import sqlalchemy as sa


revision = "58ba19f012be"
down_revision = "c5377eaf72ef"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("reminders", sa.Column("routine_key", sa.String(length=10), nullable=True))
    op.create_index(op.f("ix_reminders_routine_key"), "reminders", ["routine_key"], unique=False)

    op.execute(
        """
        UPDATE reminders
        SET routine_key = 'AM'
        WHERE reminder_type = 'ROUTINE' AND routine_key IS NULL
        """
    )

    op.execute(
        """
        UPDATE reminders
        SET label = '아침 루틴 체크'
        WHERE reminder_type = 'ROUTINE'
          AND routine_key = 'AM'
          AND (label IS NULL OR label = '루틴 체크')
        """
    )


def downgrade():
    op.drop_index(op.f("ix_reminders_routine_key"), table_name="reminders")
    op.drop_column("reminders", "routine_key")