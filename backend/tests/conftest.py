import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool

from app import models  # noqa: F401  (registers all mappers)
from app.config import get_settings
from app.db import Base, get_db
from app.main import app
from app.seed.seed_data import seed

ADMIN_API_KEY_FOR_TESTS = "test-admin-key"


@pytest.fixture()
def db_session():
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    session_factory = sessionmaker(bind=engine)
    session: Session = session_factory()
    try:
        yield session
    finally:
        session.close()
        engine.dispose()


@pytest.fixture()
def seeded_db(db_session):
    seed(db_session)
    return db_session


@pytest.fixture()
def client(db_session, monkeypatch):
    monkeypatch.setattr(get_settings(), "admin_api_key", ADMIN_API_KEY_FOR_TESTS)
    app.dependency_overrides[get_db] = lambda: db_session
    try:
        with TestClient(app) as test_client:
            yield test_client
    finally:
        app.dependency_overrides.clear()


@pytest.fixture()
def admin_headers():
    return {"X-API-Key": ADMIN_API_KEY_FOR_TESTS}
