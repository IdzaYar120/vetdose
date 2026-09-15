import uuid

from fastapi import APIRouter, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.crud import ModelT
from app.db import DbSession
from app.errors import APIError
from app.models import Contraindication, DoseRule, Product, Species
from app.schemas.calculate import CalculateRequest, CalculateResponse, CalculationWarningOut
from app.services import calculator as calc

router = APIRouter(tags=["calculate"])


def _get_or_404(db: Session, model: type[ModelT], obj_id: uuid.UUID, message: str) -> ModelT:
    obj = db.get(model, obj_id)
    if obj is None or obj.is_deleted:
        raise APIError("NOT_FOUND", message, status.HTTP_404_NOT_FOUND)
    return obj


@router.post("/calculate", response_model=CalculateResponse)
def calculate(payload: CalculateRequest, db: DbSession) -> CalculateResponse:
    species = _get_or_404(db, Species, payload.species_id, "Вид тварини не знайдено.")
    dose_rule = _get_or_404(db, DoseRule, payload.dose_rule_id, "Правило дозування не знайдено.")
    product = _get_or_404(db, Product, payload.product_id, "Препарат не знайдено.")

    if dose_rule.species_id != species.id:
        raise APIError("SPECIES_MISMATCH", "Правило дозування не стосується обраного виду.", 400)
    if dose_rule.substance_id != product.substance_id:
        raise APIError(
            "SUBSTANCE_MISMATCH", "Препарат і правило дозування стосуються різних речовин.", 400
        )

    contraindications = list(
        db.scalars(
            select(Contraindication).where(
                Contraindication.substance_id == dose_rule.substance_id,
                Contraindication.is_deleted.is_(False),
                (Contraindication.species_id.is_(None))
                | (Contraindication.species_id == species.id),
            )
        )
    )

    calc_input = calc.CalculationInput(
        weight_kg=payload.weight_kg,
        species=calc.SpeciesInput(
            is_food_producing=species.is_food_producing,
            typical_min_weight_kg=species.typical_min_weight_kg,
            typical_max_weight_kg=species.typical_max_weight_kg,
        ),
        dose_rule=calc.DoseRuleInput(
            dose_min=dose_rule.dose_min,
            dose_max=dose_rule.dose_max,
            dose_unit=calc.DoseUnit(dose_rule.dose_unit.value),
            is_verified=dose_rule.is_verified,
            max_total_dose=dose_rule.max_total_dose,
            max_total_dose_unit=(
                calc.MaxTotalDoseUnit(dose_rule.max_total_dose_unit.value)
                if dose_rule.max_total_dose_unit is not None
                else None
            ),
        ),
        product=calc.ProductInput(
            concentration_value=product.concentration_value,
            concentration_unit=calc.ConcentrationUnit(product.concentration_unit.value),
            tablet_divisible_by=product.tablet_divisible_by,
        ),
        contraindications=tuple(
            calc.ContraindicationInput(
                severity=calc.Severity(c.severity.value), message_uk=c.message_uk
            )
            for c in contraindications
        ),
    )

    try:
        result = calc.calculate_dose(calc_input)
    except calc.CalculatorError as exc:
        raise APIError(exc.code, exc.message, status.HTTP_422_UNPROCESSABLE_CONTENT) from exc

    return CalculateResponse(
        dose_min=result.dose_min,
        dose_max=result.dose_max,
        dose_amount_unit=result.dose_amount_unit,
        administration_min=result.administration_min,
        administration_max=result.administration_max,
        administration_unit=result.administration_unit,
        max_dose_capped=result.max_dose_capped,
        warnings=[
            CalculationWarningOut(code=w.code, message_uk=w.message_uk) for w in result.warnings
        ],
        explanation=list(result.explanation),
    )
