import uuid
from typing import TYPE_CHECKING

from sqlalchemy import Enum, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import EntityMixin
from app.models.enums import Severity
from app.models.types import GUID

if TYPE_CHECKING:
    from app.models.species import Species
    from app.models.substance import Substance


class Contraindication(EntityMixin):
    __tablename__ = "contraindication"

    substance_id: Mapped[uuid.UUID] = mapped_column(
        GUID(), ForeignKey("substance.id"), nullable=False
    )
    species_id: Mapped[uuid.UUID | None] = mapped_column(
        GUID(), ForeignKey("species.id"), nullable=True
    )
    condition: Mapped[str | None] = mapped_column(String(255), nullable=True)
    severity: Mapped[Severity] = mapped_column(
        Enum(Severity, native_enum=False, values_callable=lambda x: [e.value for e in x]),
        nullable=False,
    )
    message_uk: Mapped[str] = mapped_column(Text, nullable=False)
    source: Mapped[str] = mapped_column(Text, nullable=False)

    substance: Mapped["Substance"] = relationship(back_populates="contraindications")
    species: Mapped["Species | None"] = relationship(back_populates="contraindications")
