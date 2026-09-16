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
    assert len(substances) >= 6
    substance_names = {s.name for s in substances}
    assert "Amoxicillin" in substance_names
    assert "Meloxicam" in substance_names
    assert "Ivermectin" in substance_names

    products = db_session.query(Product).all()
    assert len(products) >= 10
    product_names = {p.trade_name for p in products}
    assert "Amoxoil Retard 150 mg/ml" in product_names
    assert "Metacam 5 mg/ml" in product_names
    assert "Ivomec 1%" in product_names

    dose_rules = db_session.query(DoseRule).all()
    assert len(dose_rules) >= 10
    assert all(r.is_verified is True for r in dose_rules)
    assert all(
        r.source != "TEST DATA — NOT FOR CLINICAL USE" and len(r.source) > 0 for r in dose_rules
    )

    contraindications = db_session.query(Contraindication).all()
    assert len(contraindications) >= 5
    assert any(c.severity.value == "absolute" for c in contraindications)

    withdrawal_periods = db_session.query(WithdrawalPeriod).all()
    assert len(withdrawal_periods) >= 10


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
    species.name_uk = "Змінено вручну"
    db_session.commit()

    assert seed_if_empty(db_session) is False

    refreshed = db_session.query(Species).filter_by(id=species.id).one()
    assert refreshed.name_uk == "Змінено вручну"
