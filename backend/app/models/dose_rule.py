import uuid
from decimal import Decimal
from typing import TYPE_CHECKING

from sqlalchemy import Boolean, Enum, ForeignKey, Numeric, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import EntityMixin
from app.models.enums import DoseUnit, MaxTotalDoseUnit, Route
from app.models.types import GUID

if TYPE_CHECKING:
    from app.models.species import Species
    from app.models.substance import Substance


class DoseRule(EntityMixin):
    __tablename__ = "dose_rule"

    substance_id: Mapped[uuid.UUID] = mapped_column(
        GUID(), ForeignKey("substance.id"), nullable=False
    )
    species_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("species.id"), nullable=False)
    route: Mapped[Route] = mapped_column(
        Enum(Route, native_enum=False, values_callable=lambda x: [e.value for e in x]),
        nullable=False,
    )
    indication: Mapped[str | None] = mapped_column(String(255), nullable=True)
    dose_min: Mapped[Decimal] = mapped_column(Numeric(12, 4), nullable=False)
    dose_max: Mapped[Decimal] = mapped_column(Numeric(12, 4), nullable=False)
    dose_unit: Mapped[DoseUnit] = mapped_column(
        Enum(DoseUnit, native_enum=False, values_callable=lambda x: [e.value for e in x]),
        nullable=False,
    )
    max_total_dose: Mapped[Decimal | None] = mapped_column(Numeric(12, 4), nullable=True)
    max_total_dose_unit: Mapped[MaxTotalDoseUnit | None] = mapped_column(
        Enum(
            MaxTotalDoseUnit,
            native_enum=False,
            values_callable=lambda x: [e.value for e in x],
        ),
        nullable=True,
    )
    frequency: Mapped[str | None] = mapped_column(String(150), nullable=True)
    duration: Mapped[str | None] = mapped_column(String(150), nullable=True)
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    source: Mapped[str] = mapped_column(Text, nullable=False)
    is_verified: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)

    substance: Mapped["Substance"] = relationship(back_populates="dose_rules")
    species: Mapped["Species"] = relationship(back_populates="dose_rules")
