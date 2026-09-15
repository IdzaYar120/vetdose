import uuid
from typing import Annotated

from fastapi import APIRouter, Query, status
from sqlalchemy import exists, or_, select

from app.api.v1.admin_crud import build_admin_crud_router
from app.crud import CRUDRepository
from app.db import DbSession
from app.errors import APIError
from app.models import Contraindication, DoseRule, Product, Substance, WithdrawalPeriod
from app.schemas.product import (
    ProductCreate,
    ProductDetail,
    ProductListItem,
    ProductRead,
    ProductUpdate,
)

public_router = APIRouter(prefix="/products", tags=["products"])
_product_repo: CRUDRepository[Product] = CRUDRepository(Product)


@public_router.get("", response_model=list[ProductListItem])
def search_products(
    db: DbSession,
    search: Annotated[str | None, Query()] = None,
    species_id: Annotated[uuid.UUID | None, Query()] = None,
) -> list[ProductListItem]:
    stmt = (
        select(Product, Substance)
        .join(Substance, Product.substance_id == Substance.id)
        .where(Product.is_deleted.is_(False))
    )
    if search:
        pattern = f"%{search}%"
        stmt = stmt.where(
            or_(
                Product.trade_name.ilike(pattern),
                Substance.name_uk.ilike(pattern),
                Substance.name.ilike(pattern),
            )
        )
    if species_id is not None:
        stmt = stmt.where(
            exists(
                select(DoseRule.id).where(
                    DoseRule.substance_id == Product.substance_id,
                    DoseRule.species_id == species_id,
                    DoseRule.is_deleted.is_(False),
                )
            )
        )
    rows = db.execute(stmt).all()
    return [
        ProductListItem(
            id=product.id,
            trade_name=product.trade_name,
            manufacturer=product.manufacturer,
            form=product.form,
            concentration_value=product.concentration_value,
            concentration_unit=product.concentration_unit,
            substance_id=substance.id,
            substance_name_uk=substance.name_uk,
        )
        for product, substance in rows
    ]


@public_router.get("/{product_id}", response_model=ProductDetail)
def get_product_detail(product_id: uuid.UUID, db: DbSession) -> ProductDetail:
    product = _product_repo.get(db, product_id)
    if product is None:
        raise APIError("NOT_FOUND", "Препарат не знайдено.", status.HTTP_404_NOT_FOUND)

    dose_rules = list(
        db.scalars(
            select(DoseRule).where(
                DoseRule.substance_id == product.substance_id, DoseRule.is_deleted.is_(False)
            )
        )
    )
    contraindications = list(
        db.scalars(
            select(Contraindication).where(
                Contraindication.substance_id == product.substance_id,
                Contraindication.is_deleted.is_(False),
            )
        )
    )
    withdrawal_periods = list(
        db.scalars(
            select(WithdrawalPeriod).where(
                WithdrawalPeriod.product_id == product.id, WithdrawalPeriod.is_deleted.is_(False)
            )
        )
    )

    return ProductDetail(
        **ProductRead.model_validate(product).model_dump(),
        dose_rules=dose_rules,
        contraindications=contraindications,
        withdrawal_periods=withdrawal_periods,
    )


admin_router = build_admin_crud_router(
    model=Product,
    create_schema=ProductCreate,
    update_schema=ProductUpdate,
    read_schema=ProductRead,
    prefix="/products",
    tags=["products"],
    actions=frozenset({"create", "update", "delete"}),
    not_found_message="Препарат не знайдено.",
)
