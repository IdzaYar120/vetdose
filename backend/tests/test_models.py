from decimal import Decimal

from app.models import Contraindication, DoseRule, Product, Species, Substance, WithdrawalPeriod
from app.models.enums import (
    ConcentrationUnit,
    DoseUnit,
    FoodProduct,
    ProductForm,
    Route,
    Severity,
)


def test_species_round_trip(db_session):
    species = Species(
        code="cat",
        name_uk="Кіт/кішка",
        is_food_producing=False,
        typical_min_weight_kg=Decimal("2"),
        typical_max_weight_kg=Decimal("8"),
    )
    db_session.add(species)
    db_session.commit()

    fetched = db_session.query(Species).filter_by(code="cat").one()
    assert fetched.name_uk == "Кіт/кішка"
    assert fetched.is_food_producing is False
    assert fetched.is_deleted is False
    assert fetched.id is not None
    assert fetched.created_at is not None


def test_full_graph_round_trip(db_session):
    species = Species(
        code="cattle",
        name_uk="Велика рогата худоба",
        is_food_producing=True,
        typical_min_weight_kg=Decimal("30"),
        typical_max_weight_kg=Decimal("900"),
    )
    substance = Substance(
        name="TEST_Substance_1",
        name_uk="TEST_Речовина_1",
        pharmacological_group="TEST: група",
    )
    product = Product(
        trade_name="TEST_Product_1",
        substance=substance,
        form=ProductForm.INJECTION_SOLUTION,
        concentration_value=Decimal("100"),
        concentration_unit=ConcentrationUnit.MG_PER_ML,
    )
    dose_rule = DoseRule(
        substance=substance,
        species=species,
        route=Route.IM,
        dose_min=Decimal("5"),
        dose_max=Decimal("10"),
        dose_unit=DoseUnit.MG_PER_KG,
        source="TEST DATA — NOT FOR CLINICAL USE",
        is_verified=False,
    )
    contraindication = Contraindication(
        substance=substance,
        species=species,
        severity=Severity.ABSOLUTE,
        message_uk="TEST: протипоказання",
        source="TEST DATA — NOT FOR CLINICAL USE",
    )
    withdrawal_period = WithdrawalPeriod(
        product=product,
        species=species,
        route=Route.IM,
        food_product=FoodProduct.MEAT,
        days=10,
        source="TEST DATA — NOT FOR CLINICAL USE",
    )
    db_session.add_all(
        [species, substance, product, dose_rule, contraindication, withdrawal_period]
    )
    db_session.commit()

    assert db_session.query(DoseRule).count() == 1
    assert db_session.query(Contraindication).count() == 1
    assert db_session.query(WithdrawalPeriod).count() == 1
    fetched_product = db_session.query(Product).filter_by(trade_name="TEST_Product_1").one()
    assert fetched_product.substance.name == "TEST_Substance_1"
    assert fetched_product.tablet_divisible_by is None


def test_soft_delete_flag_defaults_false(db_session):
    substance = Substance(name="TEST_Substance_2", name_uk="TEST_Речовина_2")
    db_session.add(substance)
    db_session.commit()
    assert substance.is_deleted is False

    substance.is_deleted = True
    db_session.commit()
    fetched = db_session.query(Substance).filter_by(name="TEST_Substance_2").one()
    assert fetched.is_deleted is True
