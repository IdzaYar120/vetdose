"""Direct unit tests for the GUID TypeDecorator's dialect-specific branches.

These run without a database connection, which is exactly why the earlier
bug here slipped past the (SQLite-only) integration test suite: SQLite's
process_result_value always receives a string, but psycopg's native
postgresql.UUID(as_uuid=True) already hands back a uuid.UUID object, so
calling uuid.UUID(value) on it blew up only against real PostgreSQL
(caught by docker-compose smoke testing, see Stage 2 notes).
"""

import uuid
from dataclasses import dataclass
from typing import cast

from sqlalchemy.engine.interfaces import Dialect

from app.models.types import GUID


@dataclass
class _FakeDialect:
    name: str


def _dialect(name: str) -> Dialect:
    return cast(Dialect, _FakeDialect(name))


def test_process_result_value_passes_through_native_uuid_from_postgres():
    guid = GUID()
    value = uuid.uuid4()
    result = guid.process_result_value(value, _dialect("postgresql"))
    assert result is value


def test_process_result_value_parses_string_from_sqlite():
    guid = GUID()
    value = uuid.uuid4()
    result = guid.process_result_value(str(value), _dialect("sqlite"))
    assert result == value


def test_process_result_value_none_stays_none():
    guid = GUID()
    assert guid.process_result_value(None, _dialect("postgresql")) is None
    assert guid.process_result_value(None, _dialect("sqlite")) is None


def test_process_bind_param_sqlite_stores_as_plain_string():
    guid = GUID()
    value = uuid.uuid4()
    result = guid.process_bind_param(value, _dialect("sqlite"))
    assert result == str(value)


def test_process_bind_param_postgres_stringifies_for_native_type():
    guid = GUID()
    value = uuid.uuid4()
    result = guid.process_bind_param(value, _dialect("postgresql"))
    assert result == str(value)


def test_process_bind_param_none_stays_none():
    guid = GUID()
    assert guid.process_bind_param(None, _dialect("postgresql")) is None
