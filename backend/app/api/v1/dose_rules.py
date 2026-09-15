import uuid

from fastapi import APIRouter, Depends, status

from app.api.v1.admin_crud import build_admin_crud_router
from app.crud import CRUDRepository
from app.db import DbSession
from app.errors import APIError
from app.models import DoseRule
from app.schemas.dose_rule import DoseRuleCreate, DoseRuleRead, DoseRuleUpdate
from app.security import require_admin_api_key

admin_router = build_admin_crud_router(
    model=DoseRule,
    create_schema=DoseRuleCreate,
    update_schema=DoseRuleUpdate,
    read_schema=DoseRuleRead,
    prefix="/dose-rules",
    tags=["dose-rules"],
    not_found_message="Правило дозування не знайдено.",
)

extra_router = APIRouter(
    prefix="/dose-rules", tags=["dose-rules"], dependencies=[Depends(require_admin_api_key)]
)
_repo: CRUDRepository[DoseRule] = CRUDRepository(DoseRule)


@extra_router.post("/{item_id}/verify", response_model=DoseRuleRead)
def verify_dose_rule(item_id: uuid.UUID, db: DbSession) -> DoseRule:
    obj = _repo.get(db, item_id)
    if obj is None:
        raise APIError("NOT_FOUND", "Правило дозування не знайдено.", status.HTTP_404_NOT_FOUND)
    return _repo.update(db, obj, {"is_verified": True})
