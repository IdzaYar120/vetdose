"""Fictitious development/test data for the VetDose database.

Every substance, product and dosing rule here is made up for development and
testing purposes only. See the ``TEST_`` name prefix and the mandatory
``source`` field on every rule: none of these numbers come from a real
formulary and none of them may be used for an actual clinical decision.
"""

from decimal import Decimal
from typing import Any

from sqlalchemy.orm import Session

from app.models import Contraindication, DoseRule, Product, Species, Substance, WithdrawalPeriod
from app.models.enums import (
    ConcentrationUnit,
    DoseUnit,
    FoodProduct,
    MaxTotalDoseUnit,
    ProductForm,
    Route,
    Severity,
)

TEST_SOURCE = "TEST DATA — NOT FOR CLINICAL USE"


def _species_defs() -> list[dict[str, Any]]:
    return [
        dict(code="cat", name_uk="Кіт/кішка", is_food_producing=False, min=2, max=8),
        dict(code="dog", name_uk="Собака", is_food_producing=False, min=2, max=90),
        dict(
            code="cattle", name_uk="Велика рогата худоба", is_food_producing=True, min=30, max=900
        ),
        dict(code="pig", name_uk="Свиня", is_food_producing=True, min=1, max=350),
        dict(code="horse", name_uk="Кінь", is_food_producing=False, min=100, max=700),
        dict(code="sheep", name_uk="Вівця", is_food_producing=True, min=20, max=120),
        dict(code="goat", name_uk="Коза", is_food_producing=True, min=15, max=90),
        dict(code="poultry", name_uk="Птиця", is_food_producing=True, min=Decimal("0.5"), max=6),
        dict(code="rabbit", name_uk="Кріль", is_food_producing=True, min=1, max=6),
    ]


def seed(session: Session) -> None:
    session.query(WithdrawalPeriod).delete()
    session.query(Contraindication).delete()
    session.query(DoseRule).delete()
    session.query(Product).delete()
    session.query(Substance).delete()
    session.query(Species).delete()
    session.flush()

    species = {}
    for s in _species_defs():
        row = Species(
            code=s["code"],
            name_uk=s["name_uk"],
            is_food_producing=s["is_food_producing"],
            typical_min_weight_kg=Decimal(s["min"]),
            typical_max_weight_kg=Decimal(s["max"]),
        )
        session.add(row)
        species[s["code"]] = row

    substances = {
        "amoxicillin": Substance(
            name="TEST_Substance_Amoxicillin_Analog",
            name_uk="TEST_Речовина_Амоксицилін_аналог",
            pharmacological_group="TEST: антибіотики (пеніциліни)",
            notes="Фіктивна речовина для розробки та тестів.",
        ),
        "meloxicam": Substance(
            name="TEST_Substance_Meloxicam_Analog",
            name_uk="TEST_Речовина_Мелоксикам_аналог",
            pharmacological_group="TEST: НПЗЗ",
            notes="Фіктивна речовина для розробки та тестів.",
        ),
        "ivermectin": Substance(
            name="TEST_Substance_Ivermectin_Analog",
            name_uk="TEST_Речовина_Івермектин_аналог",
            pharmacological_group="TEST: протипаразитарні",
            notes="Фіктивна речовина для розробки та тестів.",
        ),
        "oxytocin": Substance(
            name="TEST_Substance_Oxytocin_Analog",
            name_uk="TEST_Речовина_Окситоцин_аналог",
            pharmacological_group="TEST: гормони",
            notes="Фіктивна речовина для розробки та тестів.",
        ),
        "insulin": Substance(
            name="TEST_Substance_Insulin_Analog",
            name_uk="TEST_Речовина_Інсулін_аналог",
            pharmacological_group="TEST: гормони (інсулін)",
            notes="Фіктивна речовина для розробки та тестів.",
        ),
        "vitamin_b": Substance(
            name="TEST_Substance_Vitamin_B_Complex",
            name_uk="TEST_Речовина_Вітамін_B_комплекс",
            pharmacological_group="TEST: вітаміни",
            notes="Фіктивна речовина для розробки та тестів.",
        ),
    }
    for substance_row in substances.values():
        session.add(substance_row)

    products = {
        "amox_inj": Product(
            trade_name="TEST_Antibiotic_Inj_A",
            manufacturer="TEST Labs",
            substance=substances["amoxicillin"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("150"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "amox_tab": Product(
            trade_name="TEST_Antibiotic_Tab_A",
            manufacturer="TEST Labs",
            substance=substances["amoxicillin"],
            form=ProductForm.TABLET,
            concentration_value=Decimal("250"),
            concentration_unit=ConcentrationUnit.MG_PER_TABLET,
            tablet_divisible_by=2,
        ),
        "amox_susp": Product(
            trade_name="TEST_Antibiotic_Susp_A",
            manufacturer="TEST Labs",
            substance=substances["amoxicillin"],
            form=ProductForm.SUSPENSION,
            concentration_value=Decimal("100"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "meloxicam_oral": Product(
            trade_name="TEST_NSAID_Oral_A",
            manufacturer="TEST Labs",
            substance=substances["meloxicam"],
            form=ProductForm.ORAL_SOLUTION,
            concentration_value=Decimal("1.5"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "meloxicam_tab": Product(
            trade_name="TEST_NSAID_Tab_A",
            manufacturer="TEST Labs",
            substance=substances["meloxicam"],
            form=ProductForm.TABLET,
            concentration_value=Decimal("5"),
            concentration_unit=ConcentrationUnit.MG_PER_TABLET,
            tablet_divisible_by=4,
        ),
        "ivermectin_inj": Product(
            trade_name="TEST_Antiparasitic_Inj_A",
            manufacturer="TEST Labs",
            substance=substances["ivermectin"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("10"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "ivermectin_pour_on": Product(
            trade_name="TEST_Antiparasitic_PourOn_A",
            manufacturer="TEST Labs",
            substance=substances["ivermectin"],
            form=ProductForm.OTHER,
            concentration_value=Decimal("5"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "oxytocin_inj": Product(
            trade_name="TEST_Oxytocin_Inj_A",
            manufacturer="TEST Labs",
            substance=substances["oxytocin"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("10"),
            concentration_unit=ConcentrationUnit.IU_PER_ML,
        ),
        "insulin_inj": Product(
            trade_name="TEST_Insulin_Inj_A",
            manufacturer="TEST Labs",
            substance=substances["insulin"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("100"),
            concentration_unit=ConcentrationUnit.IU_PER_ML,
        ),
        "vitamin_b_powder": Product(
            trade_name="TEST_VitaminB_Powder_A",
            manufacturer="TEST Labs",
            substance=substances["vitamin_b"],
            form=ProductForm.POWDER,
            concentration_value=Decimal("50"),
            concentration_unit=ConcentrationUnit.MG_PER_G,
        ),
    }
    for product_row in products.values():
        session.add(product_row)

    dose_rules = [
        DoseRule(
            substance=substances["amoxicillin"],
            species=species["dog"],
            route=Route.PO,
            indication="TEST: неускладнена бактеріальна інфекція",
            dose_min=Decimal("10"),
            dose_max=Decimal("20"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="кожні 12 год",
            duration="5–7 днів",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["amoxicillin"],
            species=species["cat"],
            route=Route.IM,
            dose_min=Decimal("10"),
            dose_max=Decimal("15"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="кожні 24 год",
            duration="5 днів",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["amoxicillin"],
            species=species["cattle"],
            route=Route.IM,
            dose_min=Decimal("5"),
            dose_max=Decimal("10"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="кожні 24 год",
            duration="3–5 днів",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["amoxicillin"],
            species=species["rabbit"],
            route=Route.PO,
            dose_min=Decimal("10"),
            dose_max=Decimal("20"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="кожні 12 год",
            duration="5 днів",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["meloxicam"],
            species=species["dog"],
            route=Route.PO,
            indication="TEST: біль і запалення",
            dose_min=Decimal("0.1"),
            dose_max=Decimal("0.2"),
            dose_unit=DoseUnit.MG_PER_KG,
            max_total_dose=Decimal("12"),
            max_total_dose_unit=MaxTotalDoseUnit.MG,
            frequency="1 раз на добу",
            duration="до 5 днів",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["meloxicam"],
            species=species["cat"],
            route=Route.SC,
            dose_min=Decimal("0.05"),
            dose_max=Decimal("0.1"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="одноразово",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["ivermectin"],
            species=species["horse"],
            route=Route.PO,
            dose_min=Decimal("200"),
            dose_max=Decimal("200"),
            dose_unit=DoseUnit.MCG_PER_KG,
            frequency="одноразово",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["ivermectin"],
            species=species["sheep"],
            route=Route.TOPICAL,
            dose_min=Decimal("0.5"),
            dose_max=Decimal("0.5"),
            dose_unit=DoseUnit.ML_PER_KG,
            frequency="одноразово",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["oxytocin"],
            species=species["cattle"],
            route=Route.IM,
            dose_min=Decimal("0.5"),
            dose_max=Decimal("1"),
            dose_unit=DoseUnit.IU_PER_KG,
            frequency="одноразово",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["insulin"],
            species=species["dog"],
            route=Route.SC,
            dose_min=Decimal("0.25"),
            dose_max=Decimal("0.5"),
            dose_unit=DoseUnit.IU_PER_KG,
            frequency="кожні 12 год",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["insulin"],
            species=species["cat"],
            route=Route.SC,
            dose_min=Decimal("0.25"),
            dose_max=Decimal("0.5"),
            dose_unit=DoseUnit.IU_PER_KG,
            frequency="кожні 12 год",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["vitamin_b"],
            species=species["pig"],
            route=Route.PO,
            dose_min=Decimal("20"),
            dose_max=Decimal("40"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="1 раз на добу з кормом",
            source=TEST_SOURCE,
            is_verified=False,
        ),
        DoseRule(
            substance=substances["vitamin_b"],
            species=species["poultry"],
            route=Route.PO,
            dose_min=Decimal("10"),
            dose_max=Decimal("20"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="1 раз на добу з кормом",
            source=TEST_SOURCE,
            is_verified=False,
        ),
    ]
    for dose_rule_row in dose_rules:
        session.add(dose_rule_row)

    contraindications = [
        Contraindication(
            substance=substances["meloxicam"],
            species=None,
            condition="вагітність",
            severity=Severity.ABSOLUTE,
            message_uk="TEST: протипоказано тільним/вагітним тваринам (фіктивні дані).",
            source=TEST_SOURCE,
        ),
        Contraindication(
            substance=substances["amoxicillin"],
            species=None,
            condition="ниркова недостатність",
            severity=Severity.CAUTION,
            message_uk=(
                "TEST: застосовувати з обережністю при нирковій недостатності (фіктивні дані)."
            ),
            source=TEST_SOURCE,
        ),
        Contraindication(
            substance=substances["ivermectin"],
            species=species["dog"],
            condition="чутливість до макроциклічних лактонів (напр. MDR1)",
            severity=Severity.ABSOLUTE,
            message_uk="TEST: протипоказано собакам із чутливістю MDR1 (фіктивні дані).",
            source=TEST_SOURCE,
        ),
    ]
    for contraindication_row in contraindications:
        session.add(contraindication_row)

    withdrawal_periods = [
        WithdrawalPeriod(
            product=products["amox_inj"],
            species=species["cattle"],
            route=Route.IM,
            food_product=FoodProduct.MEAT,
            days=10,
            source=TEST_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["amox_inj"],
            species=species["cattle"],
            route=Route.IM,
            food_product=FoodProduct.MILK,
            days=3,
            source=TEST_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["amox_susp"],
            species=species["pig"],
            route=Route.PO,
            food_product=FoodProduct.MEAT,
            days=7,
            source=TEST_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["ivermectin_pour_on"],
            species=species["sheep"],
            route=Route.TOPICAL,
            food_product=FoodProduct.MEAT,
            days=14,
            source=TEST_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["vitamin_b_powder"],
            species=species["poultry"],
            route=Route.PO,
            food_product=FoodProduct.EGGS,
            days=2,
            source=TEST_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["vitamin_b_powder"],
            species=species["poultry"],
            route=Route.PO,
            food_product=FoodProduct.MEAT,
            days=1,
            source=TEST_SOURCE,
        ),
    ]
    for withdrawal_period_row in withdrawal_periods:
        session.add(withdrawal_period_row)

    session.commit()


def main() -> None:
    from app.db import SessionLocal

    session = SessionLocal()
    try:
        seed(session)
    finally:
        session.close()


if __name__ == "__main__":
    main()
