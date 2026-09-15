import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from app.models.enums import Severity


class ContraindicationCreate(BaseModel):
    substance_id: uuid.UUID
    species_id: uuid.UUID | None = None
    condition: str | None = Field(default=None, max_length=255)
    severity: Severity
    message_uk: str = Field(min_length=1)
    source: str = Field(min_length=1)


class ContraindicationUpdate(BaseModel):
    substance_id: uuid.UUID | None = None
    species_id: uuid.UUID | None = None
    condition: str | None = Field(default=None, max_length=255)
    severity: Severity | None = None
    message_uk: str | None = Field(default=None, min_length=1)
    source: str | None = Field(default=None, min_length=1)


class ContraindicationRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    substance_id: uuid.UUID
    species_id: uuid.UUID | None
    condition: str | None
    severity: Severity
    message_uk: str
    source: str
    created_at: datetime
    updated_at: datetime
    is_deleted: bool
