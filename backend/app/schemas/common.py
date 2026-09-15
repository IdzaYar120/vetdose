from decimal import Decimal
from typing import Annotated

from pydantic import BaseModel, PlainSerializer

# Decimal fields are always serialized as JSON strings, never numbers: a JSON
# number would round-trip through float on many clients (including
# JavaScript's JSON.parse), which is exactly the precision loss this project
# forbids for dose math. Clients parse these strings back into Decimal /
# BigDecimal / decimal.js, never float.
DecimalStr = Annotated[
    Decimal, PlainSerializer(lambda v: str(v), return_type=str, when_used="json")
]


class ErrorDetail(BaseModel):
    code: str
    message: str


class ErrorResponse(BaseModel):
    error: ErrorDetail
