import uuid

from pydantic import BaseModel

from app.schemas.common import DecimalStr


class CalculateRequest(BaseModel):
    # No numeric range constraint here on purpose: weight validity (>0, <=1500kg)
    # is the calculator's job (INVALID_WEIGHT), so the same rule applies
    # identically whether the request comes through this API or a client
    # calculating offline.
    weight_kg: DecimalStr
    species_id: uuid.UUID
    dose_rule_id: uuid.UUID
    product_id: uuid.UUID


class CalculationWarningOut(BaseModel):
    code: str
    message_uk: str


class CalculateResponse(BaseModel):
    dose_min: DecimalStr | None
    dose_max: DecimalStr | None
    dose_amount_unit: str | None
    administration_min: DecimalStr
    administration_max: DecimalStr
    administration_unit: str
    max_dose_capped: bool
    warnings: list[CalculationWarningOut]
    explanation: list[str]
