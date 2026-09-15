import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from app.models.enums import ConcentrationUnit, ProductForm
from app.schemas.common import DecimalStr
from app.schemas.contraindication import ContraindicationRead
from app.schemas.dose_rule import DoseRuleRead
from app.schemas.withdrawal_period import WithdrawalPeriodRead


class ProductCreate(BaseModel):
    trade_name: str = Field(min_length=1, max_length=150)
    manufacturer: str | None = Field(default=None, max_length=150)
    substance_id: uuid.UUID
    form: ProductForm
    concentration_value: DecimalStr = Field(gt=0)
    concentration_unit: ConcentrationUnit
    tablet_divisible_by: int | None = Field(default=None, ge=1, le=4)


class ProductUpdate(BaseModel):
    trade_name: str | None = Field(default=None, min_length=1, max_length=150)
    manufacturer: str | None = Field(default=None, max_length=150)
    substance_id: uuid.UUID | None = None
    form: ProductForm | None = None
    concentration_value: DecimalStr | None = Field(default=None, gt=0)
    concentration_unit: ConcentrationUnit | None = None
    tablet_divisible_by: int | None = Field(default=None, ge=1, le=4)


class ProductRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    trade_name: str
    manufacturer: str | None
    substance_id: uuid.UUID
    form: ProductForm
    concentration_value: DecimalStr
    concentration_unit: ConcentrationUnit
    tablet_divisible_by: int | None
    created_at: datetime
    updated_at: datetime
    is_deleted: bool


class ProductListItem(BaseModel):
    id: uuid.UUID
    trade_name: str
    manufacturer: str | None
    form: ProductForm
    concentration_value: DecimalStr
    concentration_unit: ConcentrationUnit
    substance_id: uuid.UUID
    substance_name_uk: str


class ProductDetail(ProductRead):
    dose_rules: list[DoseRuleRead]
    contraindications: list[ContraindicationRead]
    withdrawal_periods: list[WithdrawalPeriodRead]
