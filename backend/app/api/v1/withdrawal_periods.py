from app.api.v1.admin_crud import build_admin_crud_router
from app.models import WithdrawalPeriod
from app.schemas.withdrawal_period import (
    WithdrawalPeriodCreate,
    WithdrawalPeriodRead,
    WithdrawalPeriodUpdate,
)

admin_router = build_admin_crud_router(
    model=WithdrawalPeriod,
    create_schema=WithdrawalPeriodCreate,
    update_schema=WithdrawalPeriodUpdate,
    read_schema=WithdrawalPeriodRead,
    prefix="/withdrawal-periods",
    tags=["withdrawal-periods"],
    not_found_message="Термін виведення не знайдено.",
)
