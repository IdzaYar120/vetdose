import uuid
from decimal import Decimal
from typing import TYPE_CHECKING

from sqlalchemy import Enum, ForeignKey, Numeric, SmallInteger, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import EntityMixin
from app.models.enums import ConcentrationUnit, ProductForm
from app.models.types import GUID

if TYPE_CHECKING:
    from app.models.substance import Substance
    from app.models.withdrawal_period import WithdrawalPeriod


class Product(EntityMixin):
    __tablename__ = "product"

    trade_name: Mapped[str] = mapped_column(String(150), nullable=False)
    manufacturer: Mapped[str | None] = mapped_column(String(150), nullable=True)
    substance_id: Mapped[uuid.UUID] = mapped_column(
        GUID(), ForeignKey("substance.id"), nullable=False
    )
    form: Mapped[ProductForm] = mapped_column(
        Enum(ProductForm, native_enum=False, values_callable=lambda x: [e.value for e in x]),
        nullable=False,
    )
    concentration_value: Mapped[Decimal] = mapped_column(Numeric(12, 4), nullable=False)
    concentration_unit: Mapped[ConcentrationUnit] = mapped_column(
        Enum(
            ConcentrationUnit,
            native_enum=False,
            values_callable=lambda x: [e.value for e in x],
        ),
        nullable=False,
    )
    tablet_divisible_by: Mapped[int | None] = mapped_column(SmallInteger, nullable=True)

    substance: Mapped["Substance"] = relationship(back_populates="products")
    withdrawal_periods: Mapped[list["WithdrawalPeriod"]] = relationship(back_populates="product")
