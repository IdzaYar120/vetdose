from datetime import datetime

from pydantic import BaseModel

from app.schemas.contraindication import ContraindicationRead
from app.schemas.dose_rule import DoseRuleRead
from app.schemas.product import ProductRead
from app.schemas.species import SpeciesRead
from app.schemas.substance import SubstanceRead
from app.schemas.withdrawal_period import WithdrawalPeriodRead


class SyncResponse(BaseModel):
    server_time: datetime
    species: list[SpeciesRead]
    substances: list[SubstanceRead]
    products: list[ProductRead]
    dose_rules: list[DoseRuleRead]
    contraindications: list[ContraindicationRead]
    withdrawal_periods: list[WithdrawalPeriodRead]
