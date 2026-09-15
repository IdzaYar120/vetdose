from app.api.v1.admin_crud import build_admin_crud_router
from app.models import Substance
from app.schemas.substance import SubstanceCreate, SubstanceRead, SubstanceUpdate

admin_router = build_admin_crud_router(
    model=Substance,
    create_schema=SubstanceCreate,
    update_schema=SubstanceUpdate,
    read_schema=SubstanceRead,
    prefix="/substances",
    tags=["substances"],
    not_found_message="Діючу речовину не знайдено.",
)
