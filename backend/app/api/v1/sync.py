from datetime import UTC, datetime
from typing import Annotated

from fastapi import APIRouter, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db import DbSession
from app.models import Contraindication, DoseRule, Product, Species, Substance, WithdrawalPeriod
from app.models.base import EntityMixin
from app.schemas.sync import SyncResponse

router = APIRouter(tags=["sync"])


def _changed(db: Session, model: type[EntityMixin], since: datetime | None) -> list[EntityMixin]:
    stmt = select(model)
    if since is not None:
        stmt = stmt.where(model.updated_at > since)
    return list(db.scalars(stmt))


@router.get("/sync", response_model=SyncResponse)
def sync(db: DbSession, since: Annotated[datetime | None, Query()] = None) -> SyncResponse:
    server_time = datetime.now(UTC)
    return SyncResponse(
        server_time=server_time,
        species=_changed(db, Species, since),
        substances=_changed(db, Substance, since),
        products=_changed(db, Product, since),
        dose_rules=_changed(db, DoseRule, since),
        contraindications=_changed(db, Contraindication, since),
        withdrawal_periods=_changed(db, WithdrawalPeriod, since),
    )
