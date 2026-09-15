import uuid
from typing import TYPE_CHECKING

from sqlalchemy import Enum, ForeignKey, Integer, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import EntityMixin
from app.models.enums import FoodProduct, Route
from app.models.types import GUID

if TYPE_CHECKING:
    from app.models.product import Product
    from app.models.species import Species


class WithdrawalPeriod(EntityMixin):
    __tablename__ = "withdrawal_period"

    product_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("product.id"), nullable=False)
    species_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("species.id"), nullable=False)
    route: Mapped[Route] = mapped_column(
        Enum(Route, native_enum=False, values_callable=lambda x: [e.value for e in x]),
        nullable=False,
    )
    food_product: Mapped[FoodProduct] = mapped_column(
        Enum(FoodProduct, native_enum=False, values_callable=lambda x: [e.value for e in x]),
        nullable=False,
    )
    days: Mapped[int] = mapped_column(Integer, nullable=False)
    hours: Mapped[int | None] = mapped_column(Integer, nullable=True)
    source: Mapped[str] = mapped_column(Text, nullable=False)

    product: Mapped["Product"] = relationship(back_populates="withdrawal_periods")
    species: Mapped["Species"] = relationship(back_populates="withdrawal_periods")
