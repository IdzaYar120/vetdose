from fastapi import APIRouter

from app.api.v1.admin_crud import build_admin_crud_router
from app.crud import CRUDRepository
from app.db import DbSession
from app.models import Species
from app.schemas.species import SpeciesCreate, SpeciesRead, SpeciesUpdate

public_router = APIRouter(prefix="/species", tags=["species"])
_repo: CRUDRepository[Species] = CRUDRepository(Species)


@public_router.get("", response_model=list[SpeciesRead])
def list_species(db: DbSession) -> list[Species]:
    return _repo.list(db)


admin_router = build_admin_crud_router(
    model=Species,
    create_schema=SpeciesCreate,
    update_schema=SpeciesUpdate,
    read_schema=SpeciesRead,
    prefix="/species",
    tags=["species"],
    actions=frozenset({"get", "create", "update", "delete"}),
    not_found_message="Вид тварини не знайдено.",
)
