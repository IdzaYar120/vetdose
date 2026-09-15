from decimal import Decimal
from typing import TYPE_CHECKING

from sqlalchemy import Boolean, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import EntityMixin

if TYPE_CHECKING:
    from app.models.contraindication import Contraindication
    from app.models.dose_rule import DoseRule
    from app.models.withdrawal_period import WithdrawalPeriod


class Species(EntityMixin):
    __tablename__ = "species"

    code: Mapped[str] = mapped_column(String(32), unique=True, nullable=False, index=True)
    name_uk: Mapped[str] = mapped_column(String(100), nullable=False)
    is_food_producing: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    typical_min_weight_kg: Mapped[Decimal] = mapped_column(Numeric(8, 3), nullable=False)
    typical_max_weight_kg: Mapped[Decimal] = mapped_column(Numeric(8, 3), nullable=False)

    dose_rules: Mapped[list["DoseRule"]] = relationship(back_populates="species")
    contraindications: Mapped[list["Contraindication"]] = relationship(back_populates="species")
    withdrawal_periods: Mapped[list["WithdrawalPeriod"]] = relationship(back_populates="species")
