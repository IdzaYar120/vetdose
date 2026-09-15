from app.api.v1.admin_crud import build_admin_crud_router
from app.models import Contraindication
from app.schemas.contraindication import (
    ContraindicationCreate,
    ContraindicationRead,
    ContraindicationUpdate,
)

admin_router = build_admin_crud_router(
    model=Contraindication,
    create_schema=ContraindicationCreate,
    update_schema=ContraindicationUpdate,
    read_schema=ContraindicationRead,
    prefix="/contraindications",
    tags=["contraindications"],
    not_found_message="Протипоказання не знайдено.",
)
