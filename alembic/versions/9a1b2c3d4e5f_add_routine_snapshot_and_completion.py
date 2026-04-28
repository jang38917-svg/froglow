from alembic import op
import sqlalchemy as sa


revision = "9a1b2c3d4e5f"
down_revision = "58ba19f012be"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column(
        "diagnosis_histories",
        sa.Column("routine_snapshot", sa.JSON(), nullable=True),
    )

    op.create_table(
        "routine_completions",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("date", sa.Date(), nullable=False),
        sa.Column("routine_type", sa.String(length=2), nullable=False),
        sa.Column("is_completed", sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.Column("completed_at", sa.DateTime(), nullable=True),
        sa.UniqueConstraint(
            "user_id",
            "date",
            "routine_type",
            name="uq_routine_completion_user_date_type",
        ),
    )

    op.create_index(
        "ix_routine_completions_user_id",
        "routine_completions",
        ["user_id"],
    )
    op.create_index(
        "ix_routine_completions_date",
        "routine_completions",
        ["date"],
    )
    op.create_index(
        "ix_routine_completions_routine_type",
        "routine_completions",
        ["routine_type"],
    )


def downgrade():
    op.drop_index(
        "ix_routine_completions_routine_type",
        table_name="routine_completions",
    )
    op.drop_index(
        "ix_routine_completions_date",
        table_name="routine_completions",
    )
    op.drop_index(
        "ix_routine_completions_user_id",
        table_name="routine_completions",
    )
    op.drop_table("routine_completions")
    op.drop_column("diagnosis_histories", "routine_snapshot")