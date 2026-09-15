"""Pure dose-calculation engine.

No database or framework dependency on purpose: the same logic is reimplemented
independently in Android (``DoseCalculator.kt``) and the web app
(``doseCalculator.ts``). All three read ``shared/calculation_test_cases.json``
and must produce identical results, so any behavioural change here must be
mirrored (and re-verified) in the other two implementations.

All monetary-precision-sensitive arithmetic uses ``Decimal`` — never ``float``.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from decimal import ROUND_DOWN, ROUND_HALF_UP, ROUND_UP, Decimal
from enum import StrEnum


class DoseUnit(StrEnum):
    MG_PER_KG = "mg_per_kg"
    MCG_PER_KG = "mcg_per_kg"
    IU_PER_KG = "iu_per_kg"
    ML_PER_KG = "ml_per_kg"
    ML_PER_10KG = "ml_per_10kg"
    MG_PER_ANIMAL = "mg_per_animal"
    ML_PER_ANIMAL = "ml_per_animal"


class ConcentrationUnit(StrEnum):
    MG_PER_ML = "mg_per_ml"
    MCG_PER_ML = "mcg_per_ml"
    IU_PER_ML = "iu_per_ml"
    MG_PER_TABLET = "mg_per_tablet"
    MG_PER_G = "mg_per_g"


class MaxTotalDoseUnit(StrEnum):
    MG = "mg"
    MCG = "mcg"
    IU = "iu"
    ML = "ml"


class Severity(StrEnum):
    ABSOLUTE = "absolute"
    CAUTION = "caution"


MASS_DOSE_UNITS = frozenset({DoseUnit.MG_PER_KG, DoseUnit.MCG_PER_KG, DoseUnit.MG_PER_ANIMAL})
IU_DOSE_UNITS = frozenset({DoseUnit.IU_PER_KG})
VOLUME_DOSE_UNITS = frozenset({DoseUnit.ML_PER_KG, DoseUnit.ML_PER_10KG, DoseUnit.ML_PER_ANIMAL})

_PRIMARY_BASIS: dict[DoseUnit, str] = {
    DoseUnit.MG_PER_KG: "mg",
    DoseUnit.MG_PER_ANIMAL: "mg",
    DoseUnit.MCG_PER_KG: "mcg",
    DoseUnit.IU_PER_KG: "iu",
    DoseUnit.ML_PER_KG: "ml",
    DoseUnit.ML_PER_10KG: "ml",
    DoseUnit.ML_PER_ANIMAL: "ml",
}

_CONCENTRATION_BASIS: dict[ConcentrationUnit, str] = {
    ConcentrationUnit.MG_PER_ML: "mg",
    ConcentrationUnit.MCG_PER_ML: "mcg",
    ConcentrationUnit.IU_PER_ML: "iu",
    ConcentrationUnit.MG_PER_TABLET: "mg",
    ConcentrationUnit.MG_PER_G: "mg",
}

DOSE_RATE_LABELS: dict[DoseUnit, str] = {
    DoseUnit.MG_PER_KG: "мг/кг",
    DoseUnit.MCG_PER_KG: "мкг/кг",
    DoseUnit.IU_PER_KG: "МО/кг",
    DoseUnit.ML_PER_KG: "мл/кг",
    DoseUnit.ML_PER_10KG: "мл/10кг",
    DoseUnit.MG_PER_ANIMAL: "мг/тварину",
    DoseUnit.ML_PER_ANIMAL: "мл/тварину",
}

BASIS_LABELS: dict[str, str] = {"mg": "мг", "mcg": "мкг", "iu": "МО", "ml": "мл"}

CONCENTRATION_LABELS: dict[ConcentrationUnit, str] = {
    ConcentrationUnit.MG_PER_ML: "мг/мл",
    ConcentrationUnit.MCG_PER_ML: "мкг/мл",
    ConcentrationUnit.IU_PER_ML: "МО/мл",
    ConcentrationUnit.MG_PER_TABLET: "мг/таблетку",
    ConcentrationUnit.MG_PER_G: "мг/г",
}

ADMINISTRATION_UNIT_LABELS: dict[str, str] = {"ml": "мл", "tablets": "таблеток", "g": "г"}

MIN_WEIGHT_KG = Decimal(0)
MAX_WEIGHT_KG = Decimal(1500)


class CalculatorError(Exception):
    """Raised for invalid input or an unsatisfiable unit combination.

    ``code`` is the machine-readable error code from the VetDose protocol
    (e.g. ``INCOMPATIBLE_UNITS``); it is what shared test cases assert on.
    """

    def __init__(self, code: str, message: str) -> None:
        self.code = code
        self.message = message
        super().__init__(message)


@dataclass(frozen=True)
class SpeciesInput:
    is_food_producing: bool
    typical_min_weight_kg: Decimal
    typical_max_weight_kg: Decimal


@dataclass(frozen=True)
class DoseRuleInput:
    dose_min: Decimal
    dose_max: Decimal
    dose_unit: DoseUnit
    is_verified: bool = False
    max_total_dose: Decimal | None = None
    max_total_dose_unit: MaxTotalDoseUnit | None = None


@dataclass(frozen=True)
class ProductInput:
    concentration_value: Decimal | None = None
    concentration_unit: ConcentrationUnit | None = None
    tablet_divisible_by: int | None = None


@dataclass(frozen=True)
class ContraindicationInput:
    severity: Severity
    message_uk: str


@dataclass(frozen=True)
class CalculationInput:
    weight_kg: Decimal
    species: SpeciesInput
    dose_rule: DoseRuleInput
    product: ProductInput
    contraindications: tuple[ContraindicationInput, ...] = ()


@dataclass(frozen=True)
class CalculationWarning:
    code: str
    message_uk: str


@dataclass(frozen=True)
class CalculationResult:
    dose_min: Decimal | None
    dose_max: Decimal | None
    dose_amount_unit: str | None
    administration_min: Decimal
    administration_max: Decimal
    administration_unit: str
    max_dose_capped: bool
    warnings: tuple[CalculationWarning, ...] = field(default_factory=tuple)
    explanation: tuple[str, ...] = field(default_factory=tuple)


def format_number(value: Decimal) -> str:
    """Format a Decimal the way it is shown to a vet: trimmed, comma decimal."""
    quantized = value.quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP)
    text = format(quantized, "f")
    if "." in text:
        text = text.rstrip("0").rstrip(".")
    return text.replace(".", ",")


def _fmt_range(min_val: Decimal, max_val: Decimal, unit_label: str) -> str:
    if min_val == max_val:
        return f"{format_number(min_val)} {unit_label}"
    return f"{format_number(min_val)}–{format_number(max_val)} {unit_label}"


def _quantize(value: Decimal, places: int) -> Decimal:
    exponent = Decimal(1).scaleb(-places)
    return value.quantize(exponent, rounding=ROUND_HALF_UP)


def round_volume_for_display(value: Decimal) -> Decimal:
    if value < 1:
        return value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    if value <= 10:
        return value.quantize(Decimal("0.1"), rounding=ROUND_HALF_UP)
    step = Decimal("0.5")
    units = (value / step).quantize(Decimal(1), rounding=ROUND_HALF_UP)
    return units * step


def _floor_to_step(value: Decimal, step: Decimal) -> Decimal:
    units = (value / step).to_integral_value(rounding=ROUND_DOWN)
    return units * step


def _ceil_to_step(value: Decimal, step: Decimal) -> Decimal:
    units = (value / step).to_integral_value(rounding=ROUND_UP)
    return units * step


def _convertible(from_basis: str, to_basis: str) -> bool:
    if from_basis == to_basis:
        return True
    return {from_basis, to_basis} <= {"mg", "mcg"}


def _conversion_factor(from_basis: str, to_basis: str) -> Decimal:
    if from_basis == to_basis:
        return Decimal(1)
    if from_basis == "mg" and to_basis == "mcg":
        return Decimal(1000)
    if from_basis == "mcg" and to_basis == "mg":
        return Decimal("0.001")
    raise CalculatorError(
        "INCOMPATIBLE_UNITS", f"Неможливо конвертувати одиниці «{from_basis}» у «{to_basis}»."
    )


def calculate_dose(data: CalculationInput) -> CalculationResult:
    weight_kg = data.weight_kg
    species = data.species
    dose_rule = data.dose_rule
    product = data.product

    if not (MIN_WEIGHT_KG < weight_kg <= MAX_WEIGHT_KG):
        raise CalculatorError(
            "INVALID_WEIGHT",
            f"Вага має бути більшою за 0 і не більшою за {MAX_WEIGHT_KG} кг "
            f"(отримано {format_number(weight_kg)} кг).",
        )

    if dose_rule.dose_min > dose_rule.dose_max:
        raise CalculatorError(
            "INVALID_DOSE_RANGE",
            "Мінімальна доза правила не може перевищувати максимальну.",
        )

    dose_unit = dose_rule.dose_unit
    primary_basis = _PRIMARY_BASIS[dose_unit]

    explanation: list[str] = []

    # Step 1: raw primary quantity (substance mass/IU dose, or volume for
    # volume-based dose units) before any capping.
    if dose_unit == DoseUnit.MG_PER_ANIMAL or dose_unit == DoseUnit.ML_PER_ANIMAL:
        dose_min_raw = dose_rule.dose_min
        dose_max_raw = dose_rule.dose_max
        explanation.append(
            "Фіксована доза на тварину: "
            f"{_fmt_range(dose_min_raw, dose_max_raw, DOSE_RATE_LABELS[dose_unit])}"
        )
    elif dose_unit == DoseUnit.ML_PER_10KG:
        dose_min_raw = weight_kg * dose_rule.dose_min / Decimal(10)
        dose_max_raw = weight_kg * dose_rule.dose_max / Decimal(10)
        explanation.append(
            f"{format_number(weight_kg)} кг ÷ 10 × "
            f"{_fmt_range(dose_rule.dose_min, dose_rule.dose_max, DOSE_RATE_LABELS[dose_unit])}"
            f" = {_fmt_range(dose_min_raw, dose_max_raw, BASIS_LABELS[primary_basis])}"
        )
    else:
        dose_min_raw = weight_kg * dose_rule.dose_min
        dose_max_raw = weight_kg * dose_rule.dose_max
        explanation.append(
            f"{format_number(weight_kg)} кг × "
            f"{_fmt_range(dose_rule.dose_min, dose_rule.dose_max, DOSE_RATE_LABELS[dose_unit])}"
            f" = {_fmt_range(dose_min_raw, dose_max_raw, BASIS_LABELS[primary_basis])}"
        )

    # Step 2: cap at max_total_dose, if any. Applies uniformly to the primary
    # quantity, whatever its basis (mass, IU or volume).
    max_dose_capped = False
    if dose_rule.max_total_dose is not None:
        if dose_rule.max_total_dose_unit is None:
            raise CalculatorError(
                "MISSING_MAX_DOSE_UNIT",
                "Вказано максимальну дозу без одиниці вимірювання.",
            )
        max_basis = dose_rule.max_total_dose_unit.value
        if not _convertible(primary_basis, max_basis):
            raise CalculatorError(
                "INCOMPATIBLE_UNITS",
                f"Одиниця максимальної дози «{max_basis}» несумісна з одиницею дози "
                f"«{primary_basis}».",
            )
        factor = _conversion_factor(max_basis, primary_basis)
        max_in_primary_basis = dose_rule.max_total_dose * factor
        if dose_max_raw > max_in_primary_basis:
            dose_max_raw = max_in_primary_basis
            max_dose_capped = True
        if dose_min_raw > max_in_primary_basis:
            dose_min_raw = max_in_primary_basis
            max_dose_capped = True
        if max_dose_capped:
            explanation.append(
                f"Доза обмежена максимумом {format_number(max_in_primary_basis)} "
                f"{BASIS_LABELS[primary_basis]}"
            )

    # Step 3: administration amount (what the vet actually draws up / gives).
    tablet_range_invalid = False
    if primary_basis in ("mg", "mcg", "iu"):
        if product.concentration_value is None or product.concentration_unit is None:
            raise CalculatorError(
                "MISSING_CONCENTRATION",
                "Для розрахунку об'єму або кількості таблеток потрібна концентрація препарату.",
            )
        if product.concentration_value <= 0:
            raise CalculatorError(
                "INVALID_CONCENTRATION", "Концентрація препарату має бути більшою за нуль."
            )
        concentration_unit = product.concentration_unit
        concentration_basis = _CONCENTRATION_BASIS[concentration_unit]
        if not _convertible(primary_basis, concentration_basis):
            raise CalculatorError(
                "INCOMPATIBLE_UNITS",
                f"Одиниця дози «{primary_basis}» несумісна з концентрацією препарату "
                f"«{concentration_basis}».",
            )
        factor = _conversion_factor(primary_basis, concentration_basis)
        dose_min_conc_basis = dose_min_raw * factor
        dose_max_conc_basis = dose_max_raw * factor

        if concentration_unit == ConcentrationUnit.MG_PER_TABLET:
            if product.tablet_divisible_by is None:
                raise CalculatorError("MISSING_TABLET_DIVISOR", "Не вказано крок ділення таблетки.")
            step = Decimal(1) / Decimal(product.tablet_divisible_by)
            raw_min = dose_min_conc_basis / product.concentration_value
            raw_max = dose_max_conc_basis / product.concentration_value
            administration_min = _ceil_to_step(raw_min, step)
            administration_max = _floor_to_step(raw_max, step)
            administration_unit = "tablets"
            explanation.append(
                f"{_fmt_range(dose_min_raw, dose_max_raw, BASIS_LABELS[primary_basis])} ÷ "
                f"{format_number(product.concentration_value)} "
                f"{CONCENTRATION_LABELS[concentration_unit]} = "
                f"{_fmt_range(administration_min, administration_max, 'таблеток')} "
                f"(округлено до кроку {format_number(step)})"
            )
            if administration_min > administration_max:
                tablet_range_invalid = True
        else:
            raw_min = dose_min_conc_basis / product.concentration_value
            raw_max = dose_max_conc_basis / product.concentration_value
            administration_unit = "g" if concentration_unit == ConcentrationUnit.MG_PER_G else "ml"
            if administration_unit == "ml":
                administration_min = round_volume_for_display(raw_min)
                administration_max = round_volume_for_display(raw_max)
            else:
                administration_min = _quantize(raw_min, 2)
                administration_max = _quantize(raw_max, 2)
            explanation.append(
                f"{_fmt_range(dose_min_raw, dose_max_raw, BASIS_LABELS[primary_basis])} ÷ "
                f"{format_number(product.concentration_value)} "
                f"{CONCENTRATION_LABELS[concentration_unit]} = "
                f"{_fmt_range(raw_min, raw_max, ADMINISTRATION_UNIT_LABELS[administration_unit])}"
            )

        dose_min_out: Decimal | None = _quantize(dose_min_raw, 4)
        dose_max_out: Decimal | None = _quantize(dose_max_raw, 4)
        dose_amount_unit_out: str | None = primary_basis
    else:
        administration_unit = "ml"
        administration_min = round_volume_for_display(dose_min_raw)
        administration_max = round_volume_for_display(dose_max_raw)
        dose_min_out = None
        dose_max_out = None
        dose_amount_unit_out = None
        if product.concentration_value is not None and product.concentration_unit in (
            ConcentrationUnit.MG_PER_ML,
            ConcentrationUnit.MCG_PER_ML,
        ):
            if product.concentration_value <= 0:
                raise CalculatorError(
                    "INVALID_CONCENTRATION", "Концентрація препарату має бути більшою за нуль."
                )
            aux_min = dose_min_raw * product.concentration_value
            aux_max = dose_max_raw * product.concentration_value
            dose_min_out = _quantize(aux_min, 4)
            dose_max_out = _quantize(aux_max, 4)
            dose_amount_unit_out = _CONCENTRATION_BASIS[product.concentration_unit]
            explanation.append(
                f"{_fmt_range(dose_min_raw, dose_max_raw, 'мл')} × "
                f"{format_number(product.concentration_value)} "
                f"{CONCENTRATION_LABELS[product.concentration_unit]} = "
                f"{_fmt_range(dose_min_out, dose_max_out, BASIS_LABELS[dose_amount_unit_out])}"
            )

    # Step 4: warnings, in a fixed, deterministic order (mirrored in the other
    # two implementations so shared test cases compare warning_codes as lists).
    warnings: list[CalculationWarning] = []
    if not (species.typical_min_weight_kg <= weight_kg <= species.typical_max_weight_kg):
        warnings.append(
            CalculationWarning(
                "WEIGHT_OUT_OF_TYPICAL_RANGE",
                f"Вага {format_number(weight_kg)} кг виходить за типовий діапазон виду "
                f"({format_number(species.typical_min_weight_kg)}–"
                f"{format_number(species.typical_max_weight_kg)} кг). Підтвердіть значення.",
            )
        )
    for contraindication in data.contraindications:
        code = (
            "ABSOLUTE_CONTRAINDICATION"
            if contraindication.severity == Severity.ABSOLUTE
            else "CAUTION"
        )
        warnings.append(CalculationWarning(code, contraindication.message_uk))
    if not dose_rule.is_verified:
        warnings.append(
            CalculationWarning(
                "RULE_NOT_VERIFIED",
                "Це правило дозування ще не підтверджене лікарем. Застосовуйте з обережністю.",
            )
        )
    if species.is_food_producing:
        warnings.append(
            CalculationWarning(
                "FOOD_PRODUCING_ANIMAL",
                "Продуктивна тварина: перед використанням продукції перевірте терміни виведення.",
            )
        )
    if max_dose_capped:
        warnings.append(
            CalculationWarning(
                "MAX_DOSE_CAPPED",
                "Розрахована доза перевищувала максимально допустиму і була обмежена цим "
                "значенням.",
            )
        )
    if tablet_range_invalid:
        warnings.append(
            CalculationWarning(
                "TABLET_CANNOT_MATCH_RANGE",
                "Після округлення до кроку ділення таблетки мінімальна кількість перевищує "
                "максимальну. Перевірте дозу вручну.",
            )
        )

    return CalculationResult(
        dose_min=dose_min_out,
        dose_max=dose_max_out,
        dose_amount_unit=dose_amount_unit_out,
        administration_min=administration_min,
        administration_max=administration_max,
        administration_unit=administration_unit,
        max_dose_capped=max_dose_capped,
        warnings=tuple(warnings),
        explanation=tuple(explanation),
    )
