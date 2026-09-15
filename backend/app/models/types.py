import uuid
from typing import Any

from sqlalchemy import CHAR
from sqlalchemy.dialects.postgresql import UUID as PostgresUUID
from sqlalchemy.engine.interfaces import Dialect
from sqlalchemy.types import TypeDecorator, TypeEngine


class GUID(TypeDecorator[uuid.UUID]):
    """Platform-independent UUID type.

    Uses PostgreSQL's native UUID type when available, otherwise stores
    as a stringified CHAR(36) — needed so the test suite can run against
    SQLite while production runs on PostgreSQL.
    """

    impl = CHAR
    cache_ok = True

    def load_dialect_impl(self, dialect: Dialect) -> TypeEngine[Any]:
        if dialect.name == "postgresql":
            return dialect.type_descriptor(PostgresUUID(as_uuid=True))
        return dialect.type_descriptor(CHAR(36))

    def process_bind_param(self, value: uuid.UUID | str | None, dialect: Dialect) -> str | None:
        if value is None:
            return None
        if dialect.name == "postgresql":
            return str(value)
        if not isinstance(value, uuid.UUID):
            value = uuid.UUID(value)
        return str(value)

    def process_result_value(
        self, value: str | uuid.UUID | None, dialect: Dialect
    ) -> uuid.UUID | None:
        if value is None:
            return None
        if isinstance(value, uuid.UUID):
            return value
        return uuid.UUID(value)
