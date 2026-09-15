from app.models import Contraindication, DoseRule, Product, Species, Substance, WithdrawalPeriod
from app.seed.seed_data import seed, seed_if_empty


def test_seed_populates_expected_fixtures(db_session):
    seed(db_session)

    species = db_session.query(Species).all()
    assert len(species) == 9
    assert {s.code for s in species} == {
        "cat",
        "dog",
        "cattle",
        "pig",
        "horse",
        "sheep",
        "goat",
        "poultry",
        "rabbit",
    }

    substances = db_session.query(Substance).all()
    assert 5 <= len(substances) <= 6
    assert all(s.name.startswith("TEST_") for s in substances)

    products = db_session.query(Product).all()
    assert 8 <= len(products) <= 10
    assert all(p.trade_name.startswith("TEST_") for p in products)

    dose_rules = db_session.query(DoseRule).all()
    assert len(dose_rules) >= 5
    assert all(r.is_verified is False for r in dose_rules)
    assert all(r.source == "TEST DATA — NOT FOR CLINICAL USE" for r in dose_rules)

    contraindications = db_session.query(Contraindication).all()
    assert len(contraindications) >= 1
    assert any(c.severity.value == "absolute" for c in contraindications)

    withdrawal_periods = db_session.query(WithdrawalPeriod).all()
    assert len(withdrawal_periods) >= 2


def test_seed_is_rerunnable(db_session):
    seed(db_session)
    seed(db_session)

    assert db_session.query(Species).count() == 9


def test_seed_if_empty_seeds_fresh_db(db_session):
    assert seed_if_empty(db_session) is True
    assert db_session.query(Species).count() == 9


def test_seed_if_empty_skips_when_data_exists(db_session):
    seed(db_session)
    species = db_session.query(Species).first()
    species.name_uk = "TEST Змінено вручну"
    db_session.commit()

    assert seed_if_empty(db_session) is False

    refreshed = db_session.query(Species).filter_by(id=species.id).one()
    assert refreshed.name_uk == "TEST Змінено вручну"
