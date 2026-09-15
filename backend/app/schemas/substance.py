import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class SubstanceCreate(BaseModel):
    name: str = Field(min_length=1, max_length=150)
    name_uk: str = Field(min_length=1, max_length=150)
    pharmacological_group: str | None = Field(default=None, max_length=150)
    notes: str | None = None


class SubstanceUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=150)
    name_uk: str | None = Field(default=None, min_length=1, max_length=150)
    pharmacological_group: str | None = Field(default=None, max_length=150)
    notes: str | None = None


class SubstanceRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    name: str
    name_uk: str
    pharmacological_group: str | None
    notes: str | None
    created_at: datetime
    updated_at: datetime
    is_deleted: bool
