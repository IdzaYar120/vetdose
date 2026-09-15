import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from app.models.enums import FoodProduct, Route


class WithdrawalPeriodCreate(BaseModel):
    product_id: uuid.UUID
    species_id: uuid.UUID
    route: Route
    food_product: FoodProduct
    days: int = Field(ge=0)
    hours: int | None = Field(default=None, ge=0, le=23)
    source: str = Field(min_length=1)


class WithdrawalPeriodUpdate(BaseModel):
    product_id: uuid.UUID | None = None
    species_id: uuid.UUID | None = None
    route: Route | None = None
    food_product: FoodProduct | None = None
    days: int | None = Field(default=None, ge=0)
    hours: int | None = Field(default=None, ge=0, le=23)
    source: str | None = Field(default=None, min_length=1)


class WithdrawalPeriodRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    product_id: uuid.UUID
    species_id: uuid.UUID
    route: Route
    food_product: FoodProduct
    days: int
    hours: int | None
    source: str
    created_at: datetime
    updated_at: datetime
    is_deleted: bool
