"""update reminders text based

Revision ID: abc123
Revises: 155de55ec941
Create Date: 2026-04-15 18:00:00.000000

"""
from alembic import op
import sqlalchemy as sa


revision = "c5377eaf72ef"
down_revision = "155de55ec941"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column(
        "reminders",
        sa.Column("reminder_type", sa.String(length=20), nullable=True),
    )
    op.add_column(
        "reminders",
        sa.Column("frequency", sa.String(length=30), nullable=True),
    )
    op.add_column(
        "reminders",
        sa.Column("interval_hours", sa.Integer(), nullable=True),
    )

    op.execute(
        "UPDATE reminders SET reminder_type = 'PRODUCT' WHERE reminder_type IS NULL"
    )
    op.execute(
        "UPDATE reminders SET frequency = 'DAILY' WHERE frequency IS NULL"
    )

    op.alter_column(
        "reminders",
        "reminder_type",
        existing_type=sa.String(length=20),
        nullable=False,
    )
    op.alter_column(
        "reminders",
        "frequency",
        existing_type=sa.String(length=30),
        nullable=False,
    )

    op.drop_constraint(
        "reminders_product_id_fkey",
        "reminders",
        type_="foreignkey",
    )
    op.drop_index("ix_reminders_product_id", table_name="reminders")
    op.drop_column("reminders", "product_id")

    op.create_index(
        op.f("ix_reminders_reminder_type"),
        "reminders",
        ["reminder_type"],
        unique=False,
    )


def downgrade():
    op.add_column(
        "reminders",
        sa.Column("product_id", sa.BigInteger(), nullable=True),
    )
    op.create_index(
        "ix_reminders_product_id",
        "reminders",
        ["product_id"],
        unique=False,
    )
    op.create_foreign_key(
        "reminders_product_id_fkey",
        "reminders",
        "products",
        ["product_id"],
        ["product_id"],
    )

    op.drop_index(
        op.f("ix_reminders_reminder_type"),
        table_name="reminders",
    )
    op.drop_column("reminders", "interval_hours")
    op.drop_column("reminders", "frequency")
    op.drop_column("reminders", "reminder_type")