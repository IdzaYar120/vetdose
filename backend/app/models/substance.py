from typing import TYPE_CHECKING

from sqlalchemy import String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import EntityMixin

if TYPE_CHECKING:
    from app.models.contraindication import Contraindication
    from app.models.dose_rule import DoseRule
    from app.models.product import Product


class Substance(EntityMixin):
    __tablename__ = "substance"

    name: Mapped[str] = mapped_column(String(150), nullable=False)
    name_uk: Mapped[str] = mapped_column(String(150), nullable=False)
    pharmacological_group: Mapped[str | None] = mapped_column(String(150), nullable=True)
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)

    products: Mapped[list["Product"]] = relationship(back_populates="substance")
    dose_rules: Mapped[list["DoseRule"]] = relationship(back_populates="substance")
    contraindications: Mapped[list["Contraindication"]] = relationship(back_populates="substance")
