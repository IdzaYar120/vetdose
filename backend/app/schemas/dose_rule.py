import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.models.enums import DoseUnit, MaxTotalDoseUnit, Route
from app.schemas.common import DecimalStr


class DoseRuleCreate(BaseModel):
    substance_id: uuid.UUID
    species_id: uuid.UUID
    route: Route
    indication: str | None = Field(default=None, max_length=255)
    dose_min: DecimalStr = Field(gt=0)
    dose_max: DecimalStr = Field(gt=0)
    dose_unit: DoseUnit
    max_total_dose: DecimalStr | None = Field(default=None, gt=0)
    max_total_dose_unit: MaxTotalDoseUnit | None = None
    frequency: str | None = Field(default=None, max_length=150)
    duration: str | None = Field(default=None, max_length=150)
    notes: str | None = None
    source: str = Field(min_length=1)
    is_verified: bool = False

    @model_validator(mode="after")
    def check_dose_range(self) -> "DoseRuleCreate":
        if self.dose_min > self.dose_max:
            raise ValueError("dose_min не може перевищувати dose_max")
        if (self.max_total_dose is None) != (self.max_total_dose_unit is None):
            raise ValueError("max_total_dose і max_total_dose_unit мають бути задані разом")
        return self


class DoseRuleUpdate(BaseModel):
    substance_id: uuid.UUID | None = None
    species_id: uuid.UUID | None = None
    route: Route | None = None
    indication: str | None = Field(default=None, max_length=255)
    dose_min: DecimalStr | None = Field(default=None, gt=0)
    dose_max: DecimalStr | None = Field(default=None, gt=0)
    dose_unit: DoseUnit | None = None
    max_total_dose: DecimalStr | None = Field(default=None, gt=0)
    max_total_dose_unit: MaxTotalDoseUnit | None = None
    frequency: str | None = Field(default=None, max_length=150)
    duration: str | None = Field(default=None, max_length=150)
    notes: str | None = None
    source: str | None = Field(default=None, min_length=1)
    is_verified: bool | None = None


class DoseRuleRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    substance_id: uuid.UUID
    species_id: uuid.UUID
    route: Route
    indication: str | None
    dose_min: DecimalStr
    dose_max: DecimalStr
    dose_unit: DoseUnit
    max_total_dose: DecimalStr | None
    max_total_dose_unit: MaxTotalDoseUnit | None
    frequency: str | None
    duration: str | None
    notes: str | None
    source: str
    is_verified: bool
    created_at: datetime
    updated_at: datetime
    is_deleted: bool
