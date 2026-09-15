"""Runs every case in shared/calculation_test_cases.json against the Python
calculator. The same file is also read by the Android (JUnit) and web
(Vitest) test suites so all three implementations are checked for agreement.
"""

import json
from decimal import Decimal
from pathlib import Path
from typing import Any

import pytest

from app.services.calculator import (
    CalculationInput,
    CalculatorError,
    ConcentrationUnit,
    ContraindicationInput,
    DoseRuleInput,
    DoseUnit,
    MaxTotalDoseUnit,
    ProductInput,
    Severity,
    SpeciesInput,
    calculate_dose,
)

SHARED_CASES_PATH = Path(__file__).resolve().parents[2] / "shared" / "calculation_test_cases.json"


def _load_cases() -> list[dict[str, Any]]:
    with SHARED_CASES_PATH.open(encoding="utf-8") as f:
        result: list[dict[str, Any]] = json.load(f)
        return result


def _decimal_or_none(value: str | None) -> Decimal | None:
    return Decimal(value) if value is not None else None


def _build_input(raw: dict[str, Any]) -> CalculationInput:
    species_raw = raw["species"]
    dose_rule_raw = raw["dose_rule"]
    product_raw = raw["product"]

    species = SpeciesInput(
        is_food_producing=species_raw["is_food_producing"],
        typical_min_weight_kg=Decimal(species_raw["typical_min_weight_kg"]),
        typical_max_weight_kg=Decimal(species_raw["typical_max_weight_kg"]),
    )
    dose_rule = DoseRuleInput(
        dose_min=Decimal(dose_rule_raw["dose_min"]),
        dose_max=Decimal(dose_rule_raw["dose_max"]),
        dose_unit=DoseUnit(dose_rule_raw["dose_unit"]),
        is_verified=dose_rule_raw["is_verified"],
        max_total_dose=_decimal_or_none(dose_rule_raw.get("max_total_dose")),
        max_total_dose_unit=(
            MaxTotalDoseUnit(dose_rule_raw["max_total_dose_unit"])
            if dose_rule_raw.get("max_total_dose_unit")
            else None
        ),
    )
    product = ProductInput(
        concentration_value=_decimal_or_none(product_raw.get("concentration_value")),
        concentration_unit=(
            ConcentrationUnit(product_raw["concentration_unit"])
            if product_raw.get("concentration_unit")
            else None
        ),
        tablet_divisible_by=product_raw.get("tablet_divisible_by"),
    )
    contraindications = tuple(
        ContraindicationInput(severity=Severity(c["severity"]), message_uk=c["message_uk"])
        for c in raw.get("contraindications", [])
    )
    return CalculationInput(
        weight_kg=Decimal(raw["weight_kg"]),
        species=species,
        dose_rule=dose_rule,
        product=product,
        contraindications=contraindications,
    )


CASES = _load_cases()


@pytest.mark.parametrize("case", CASES, ids=[c["id"] for c in CASES])
def test_shared_calculation_case(case: dict[str, Any]) -> None:
    calc_input = _build_input(case["input"])
    expected = case["expected"]

    if expected.get("error_code") is not None:
        with pytest.raises(CalculatorError) as exc_info:
            calculate_dose(calc_input)
        assert exc_info.value.code == expected["error_code"]
        return

    result = calculate_dose(calc_input)

    assert (str(result.dose_min) if result.dose_min is not None else None) == expected["dose_min"]
    assert (str(result.dose_max) if result.dose_max is not None else None) == expected["dose_max"]
    assert result.dose_amount_unit == expected["dose_amount_unit"]
    assert str(result.administration_min) == expected["administration_min"]
    assert str(result.administration_max) == expected["administration_max"]
    assert result.administration_unit == expected["administration_unit"]
    assert result.max_dose_capped == expected["max_dose_capped"]
    assert [w.code for w in result.warnings] == expected["warning_codes"]
    assert list(result.explanation) == expected["explanation"]


def test_shared_cases_file_has_enough_coverage() -> None:
    assert len(CASES) >= 25
