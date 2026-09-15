import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.common import DecimalStr


class SpeciesCreate(BaseModel):
    code: str = Field(min_length=1, max_length=32)
    name_uk: str = Field(min_length=1, max_length=100)
    is_food_producing: bool
    typical_min_weight_kg: DecimalStr = Field(gt=0)
    typical_max_weight_kg: DecimalStr = Field(gt=0)


class SpeciesUpdate(BaseModel):
    code: str | None = Field(default=None, min_length=1, max_length=32)
    name_uk: str | None = Field(default=None, min_length=1, max_length=100)
    is_food_producing: bool | None = None
    typical_min_weight_kg: DecimalStr | None = Field(default=None, gt=0)
    typical_max_weight_kg: DecimalStr | None = Field(default=None, gt=0)


class SpeciesRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    code: str
    name_uk: str
    is_food_producing: bool
    typical_min_weight_kg: DecimalStr
    typical_max_weight_kg: DecimalStr
    created_at: datetime
    updated_at: datetime
    is_deleted: bool
