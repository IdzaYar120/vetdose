"""Production-ready clinical veterinary data for the VetDose database.

Substances, products, dosing rules, contraindications and withdrawal periods
are sourced from verified veterinary formularies (Plumb's Veterinary Drug Handbook,
BSAVA Small Animal Formulary, EMA SPC, and State Veterinary Registries).
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

PLUMBS_SOURCE = "Plumb's Veterinary Drug Handbook (9th Ed.)"
BSAVA_SOURCE = "BSAVA Small Animal Formulary (9th Ed.)"
EMA_SOURCE = "EMA SPC (Summary of Product Characteristics)"
REGISTRY_SOURCE = "Державний реєстр ветеринарних препаратів України"


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
            name="Amoxicillin",
            name_uk="Амоксицилін",
            pharmacological_group="Антибіотики (пеніциліни широкого спектра)",
            notes="Бета-лактамний антибіотик широкого спектра дії.",
        ),
        "meloxicam": Substance(
            name="Meloxicam",
            name_uk="Мелоксикам",
            pharmacological_group="НПЗЗ (селективний інгібітор ЦОГ-2)",
            notes="Нестероїдний протизапальний засіб з знеболювальною та антипіретичною дією.",
        ),
        "ivermectin": Substance(
            name="Ivermectin",
            name_uk="Івермектин",
            pharmacological_group="Протипаразитарні засоби (макроциклічні лактони)",
            notes="Ендодектоцид проти круглих гельмінтів та ектопаразитів.",
        ),
        "enrofloxacin": Substance(
            name="Enrofloxacin",
            name_uk="Енрофлоксацин",
            pharmacological_group="Антибіотики (фторхінолони)",
            notes="Бактерицидний фторхінолон широкого спектра дії.",
        ),
        "oxytocin": Substance(
            name="Oxytocin",
            name_uk="Окситоцин",
            pharmacological_group="Гормони (стимулятори пологової діяльності)",
            notes="Пептидний гормон задньої частки гіпофіза.",
        ),
        "ketoprofen": Substance(
            name="Ketoprofen",
            name_uk="Кетопрофен",
            pharmacological_group="НПЗЗ (похідні пропіонової кислоти)",
            notes="Нестероїдний протизапальний, анальгетичний та антипіретичний засіб.",
        ),
        "pimobendan": Substance(
            name="Pimobendan",
            name_uk="Пімобендан",
            pharmacological_group="Кардіотонічні засоби (позитивний інотроп)",
            notes="Інодилятатор для лікування серцевої недостатності у собак.",
        ),
    }
    for substance_row in substances.values():
        session.add(substance_row)

    products = {
        "amox_inj": Product(
            trade_name="Amoxoil Retard 150 mg/ml",
            manufacturer="Laboratorios Syva",
            substance=substances["amoxicillin"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("150"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "amox_tab_250": Product(
            trade_name="Amoxival 250 mg",
            manufacturer="Ceva Sante Animale",
            substance=substances["amoxicillin"],
            form=ProductForm.TABLET,
            concentration_value=Decimal("250"),
            concentration_unit=ConcentrationUnit.MG_PER_TABLET,
            tablet_divisible_by=2,
        ),
        "amox_tab_50": Product(
            trade_name="Amoxibactin 50 mg",
            manufacturer="Dechra / Le Vet",
            substance=substances["amoxicillin"],
            form=ProductForm.TABLET,
            concentration_value=Decimal("50"),
            concentration_unit=ConcentrationUnit.MG_PER_TABLET,
            tablet_divisible_by=4,
        ),
        "meloxicam_inj": Product(
            trade_name="Metacam 5 mg/ml",
            manufacturer="Boehringer Ingelheim",
            substance=substances["meloxicam"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("5"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "meloxicam_oral": Product(
            trade_name="Metacam 1.5 mg/ml",
            manufacturer="Boehringer Ingelheim",
            substance=substances["meloxicam"],
            form=ProductForm.ORAL_SOLUTION,
            concentration_value=Decimal("1.5"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "meloxicam_tab": Product(
            trade_name="Meloxidyl 2.5 mg",
            manufacturer="Ceva Sante Animale",
            substance=substances["meloxicam"],
            form=ProductForm.TABLET,
            concentration_value=Decimal("2.5"),
            concentration_unit=ConcentrationUnit.MG_PER_TABLET,
            tablet_divisible_by=2,
        ),
        "ivermectin_inj": Product(
            trade_name="Ivomec 1%",
            manufacturer="Boehringer Ingelheim",
            substance=substances["ivermectin"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("10"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "ivermectin_pour_on": Product(
            trade_name="Baymec Pour-on 0.5%",
            manufacturer="Elanco / Bayer",
            substance=substances["ivermectin"],
            form=ProductForm.OTHER,
            concentration_value=Decimal("5"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "enro_inj": Product(
            trade_name="Baytril 5%",
            manufacturer="Elanco / Bayer",
            substance=substances["enrofloxacin"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("50"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "enro_tab": Product(
            trade_name="Enroxil 15 mg",
            manufacturer="KRKA",
            substance=substances["enrofloxacin"],
            form=ProductForm.TABLET,
            concentration_value=Decimal("15"),
            concentration_unit=ConcentrationUnit.MG_PER_TABLET,
            tablet_divisible_by=2,
        ),
        "oxytocin_inj": Product(
            trade_name="Oxytocin 10 IU/ml",
            manufacturer="Bioveta",
            substance=substances["oxytocin"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("10"),
            concentration_unit=ConcentrationUnit.IU_PER_ML,
        ),
        "ketofen_inj": Product(
            trade_name="Ketofen 10%",
            manufacturer="Ceva Sante Animale",
            substance=substances["ketoprofen"],
            form=ProductForm.INJECTION_SOLUTION,
            concentration_value=Decimal("100"),
            concentration_unit=ConcentrationUnit.MG_PER_ML,
        ),
        "vetmedin_tab": Product(
            trade_name="Vetmedin 2.5 mg",
            manufacturer="Boehringer Ingelheim",
            substance=substances["pimobendan"],
            form=ProductForm.TABLET,
            concentration_value=Decimal("2.5"),
            concentration_unit=ConcentrationUnit.MG_PER_TABLET,
            tablet_divisible_by=2,
        ),
    }
    for product_row in products.values():
        session.add(product_row)

    dose_rules = [
        DoseRule(
            substance=substances["amoxicillin"],
            species=species["dog"],
            route=Route.PO,
            indication="Бактеріальні інфекції травного, дихального та сечостатевого тракту",
            dose_min=Decimal("10"),
            dose_max=Decimal("20"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="кожні 12 год",
            duration="5–7 днів",
            source=PLUMBS_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["amoxicillin"],
            species=species["cat"],
            route=Route.IM,
            indication="Системні бактеріальні інфекції, інфекції м'яких тканин",
            dose_min=Decimal("10"),
            dose_max=Decimal("15"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="кожні 24 год",
            duration="3–5 днів",
            source=BSAVA_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["amoxicillin"],
            species=species["cattle"],
            route=Route.IM,
            indication="Респіраторні інфекції ВРХ (комплекс пневмоній), інфекції м'яких тканин",
            dose_min=Decimal("7"),
            dose_max=Decimal("15"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="кожні 24 год",
            duration="3–5 днів",
            source=EMA_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["amoxicillin"],
            species=species["pig"],
            route=Route.IM,
            indication="Респіраторні інфекції свиней, синдром ММА",
            dose_min=Decimal("15"),
            dose_max=Decimal("15"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="кожні 24 год",
            duration="3 дні",
            source=EMA_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["meloxicam"],
            species=species["dog"],
            route=Route.PO,
            indication="Запалення та біль при гострих і хронічних захворюваннях опорно-рухового апарату",
            dose_min=Decimal("0.1"),
            dose_max=Decimal("0.2"),
            dose_unit=DoseUnit.MG_PER_KG,
            max_total_dose=Decimal("20"),
            max_total_dose_unit=MaxTotalDoseUnit.MG,
            frequency="1 раз на добу",
            duration="Початкова доза 0.2 мг/кг у 1-й день, далі підтримуюча 0.1 мг/кг",
            source=PLUMBS_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["meloxicam"],
            species=species["cat"],
            route=Route.SC,
            indication="Післяопераційна анальгезія та зменшення запалення",
            dose_min=Decimal("0.05"),
            dose_max=Decimal("0.1"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="Одноразово",
            source=BSAVA_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["meloxicam"],
            species=species["cattle"],
            route=Route.SC,
            indication="Гострі респіраторні інфекції у поєднанні з антибіотикотерапією, гострий мастит",
            dose_min=Decimal("0.5"),
            dose_max=Decimal("0.5"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="Одноразово",
            source=EMA_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["meloxicam"],
            species=species["pig"],
            route=Route.IM,
            indication="Захворювання опорно-рухового апарату, синдром мастит-метрит-агалактія (ММА)",
            dose_min=Decimal("0.4"),
            dose_max=Decimal("0.4"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="Одноразово",
            source=EMA_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["ivermectin"],
            species=species["cattle"],
            route=Route.SC,
            indication="Шлунково-кишкові та легеневі нематодози, вошивість, короста",
            dose_min=Decimal("200"),
            dose_max=Decimal("200"),
            dose_unit=DoseUnit.MCG_PER_KG,
            frequency="Одноразово (1 мл на 50 кг маси тіла)",
            source=PLUMBS_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["ivermectin"],
            species=species["horse"],
            route=Route.PO,
            indication="Стронгілятози, оксиуроз, параскаридоз, гастрофільоз",
            dose_min=Decimal("200"),
            dose_max=Decimal("200"),
            dose_unit=DoseUnit.MCG_PER_KG,
            frequency="Одноразово",
            source=PLUMBS_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["ivermectin"],
            species=species["sheep"],
            route=Route.TOPICAL,
            indication="Псороптоз (чесотка), гельмінтози",
            dose_min=Decimal("500"),
            dose_max=Decimal("500"),
            dose_unit=DoseUnit.MCG_PER_KG,
            frequency="Одноразово",
            source=REGISTRY_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["enrofloxacin"],
            species=species["dog"],
            route=Route.PO,
            indication="Бактеріальні інфекції сечовидільної системи, шлунково-кишкового тракту, ран",
            dose_min=Decimal("5"),
            dose_max=Decimal("5"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="1 раз на добу",
            duration="5–10 днів",
            source=PLUMBS_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["enrofloxacin"],
            species=species["cat"],
            route=Route.PO,
            indication="Інфекції дихальних та сечовидільних шляхів",
            dose_min=Decimal("5"),
            dose_max=Decimal("5"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="1 раз на добу (не перевищувати 5 мг/кг/добу)",
            duration="До 5 днів",
            source=BSAVA_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["enrofloxacin"],
            species=species["cattle"],
            route=Route.SC,
            indication="Респіраторні інфекції ВРХ, спричинені Pasteurella multocida, Mannheimia haemolytica",
            dose_min=Decimal("2.5"),
            dose_max=Decimal("5"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="1 раз на добу",
            duration="3–5 днів",
            source=EMA_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["oxytocin"],
            species=species["cattle"],
            route=Route.IM,
            indication="Слабкість родової діяльності, затримання посліду, атонія матки, мастит (для авідділення молока)",
            dose_min=Decimal("0.5"),
            dose_max=Decimal("1"),
            dose_unit=DoseUnit.IU_PER_KG,
            frequency="Одноразово (20–40 МО на тварину)",
            source=PLUMBS_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["ketoprofen"],
            species=species["cattle"],
            route=Route.IM,
            indication="Запальні та больові синдроми при захворюваннях опорно-рухового апарату, парезах, маститах",
            dose_min=Decimal("3"),
            dose_max=Decimal("3"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="1 раз на добу",
            duration="1–3 дні",
            source=EMA_SOURCE,
            is_verified=True,
        ),
        DoseRule(
            substance=substances["pimobendan"],
            species=species["dog"],
            route=Route.PO,
            indication="Серцева недостатність при ДКМП або ендокардіозі мітрального клапана",
            dose_min=Decimal("0.25"),
            dose_max=Decimal("0.5"),
            dose_unit=DoseUnit.MG_PER_KG,
            frequency="Кожні 12 год",
            duration="Довічно / за призначенням кардіолога",
            source=PLUMBS_SOURCE,
            is_verified=True,
        ),
    ]
    for dose_rule_row in dose_rules:
        session.add(dose_rule_row)

    contraindications = [
        Contraindication(
            substance=substances["meloxicam"],
            species=None,
            condition="Вагітність, лактація, виразки ШКТ",
            severity=Severity.ABSOLUTE,
            message_uk="Протипоказано вагітним та лактуючим тваринам, а також тваринам із виразковими ураженнями ШКТ або геморагічними розладами.",
            source=PLUMBS_SOURCE,
        ),
        Contraindication(
            substance=substances["amoxicillin"],
            species=species["rabbit"],
            condition="Пероральне застосування кролям та гризунам",
            severity=Severity.ABSOLUTE,
            message_uk="Протипоказано пероральне призначення кролям, гвінейським свинкам та хом'якам через ризик летальної ентеротоксемії та фатального дисбактеріозу.",
            source=PLUMBS_SOURCE,
        ),
        Contraindication(
            substance=substances["amoxicillin"],
            species=None,
            condition="Ниркова недостатність або гіперчутливість до пеніцилінів",
            severity=Severity.CAUTION,
            message_uk="Застосовувати з обережністю при нирковій недостатності. Потрібне корегування дози або збільшення інтервалу між введеннями.",
            source=BSAVA_SOURCE,
        ),
        Contraindication(
            substance=substances["ivermectin"],
            species=species["dog"],
            condition="Мутація гена MDR1 (ABCB1-1delta) у породах собак",
            severity=Severity.ABSOLUTE,
            message_uk="Протипоказано породам собак із дефектом гена MDR1 (коллі, шелті, австралійська вівчарка) через проникнення крізь ГЕБ і важку летальну нейротоксичність.",
            source=PLUMBS_SOURCE,
        ),
        Contraindication(
            substance=substances["enrofloxacin"],
            species=species["cat"],
            condition="Дозування понад 5 мг/кг/добу у котів",
            severity=Severity.ABSOLUTE,
            message_uk="Строго протипоказано перевищувати дозу 5 мг/кг/добу у котів через ризик розвитку гострої незворотної ретинопатії та сліпоти.",
            source=BSAVA_SOURCE,
        ),
        Contraindication(
            substance=substances["enrofloxacin"],
            species=species["dog"],
            condition="Молоді собаки в період активного росту",
            severity=Severity.ABSOLUTE,
            message_uk="Протипоказано молодим собакам до 12–18 місяців у період активного росту кістяка через ризик пошкодження суглобових хрящів (артропатія).",
            source=PLUMBS_SOURCE,
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
            days=28,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["amox_inj"],
            species=species["cattle"],
            route=Route.IM,
            food_product=FoodProduct.MILK,
            days=4,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["amox_inj"],
            species=species["pig"],
            route=Route.IM,
            food_product=FoodProduct.MEAT,
            days=16,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["meloxicam_inj"],
            species=species["cattle"],
            route=Route.SC,
            food_product=FoodProduct.MEAT,
            days=15,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["meloxicam_inj"],
            species=species["cattle"],
            route=Route.SC,
            food_product=FoodProduct.MILK,
            days=5,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["meloxicam_inj"],
            species=species["pig"],
            route=Route.IM,
            food_product=FoodProduct.MEAT,
            days=5,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["ivermectin_inj"],
            species=species["cattle"],
            route=Route.SC,
            food_product=FoodProduct.MEAT,
            days=49,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["ivermectin_inj"],
            species=species["sheep"],
            route=Route.SC,
            food_product=FoodProduct.MEAT,
            days=28,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["ivermectin_inj"],
            species=species["pig"],
            route=Route.SC,
            food_product=FoodProduct.MEAT,
            days=28,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["enro_inj"],
            species=species["cattle"],
            route=Route.SC,
            food_product=FoodProduct.MEAT,
            days=14,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["enro_inj"],
            species=species["cattle"],
            route=Route.SC,
            food_product=FoodProduct.MILK,
            days=4,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["oxytocin_inj"],
            species=species["cattle"],
            route=Route.IM,
            food_product=FoodProduct.MEAT,
            days=0,
            source=REGISTRY_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["oxytocin_inj"],
            species=species["cattle"],
            route=Route.IM,
            food_product=FoodProduct.MILK,
            days=0,
            source=REGISTRY_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["ketofen_inj"],
            species=species["cattle"],
            route=Route.IM,
            food_product=FoodProduct.MEAT,
            days=4,
            source=EMA_SOURCE,
        ),
        WithdrawalPeriod(
            product=products["ketofen_inj"],
            species=species["cattle"],
            route=Route.IM,
            food_product=FoodProduct.MILK,
            days=0,
            source=EMA_SOURCE,
        ),
    ]
    for withdrawal_period_row in withdrawal_periods:
        session.add(withdrawal_period_row)

    session.commit()


def seed_if_empty(session: Session) -> bool:
    """Seeds on a fresh database OR if the existing database contains legacy TEST_ data."""
    first_substance = session.query(Substance).first()
    if first_substance is not None and not first_substance.name.startswith("TEST_"):
        return False
    seed(session)
    return True


def main() -> None:
    import sys

    from app.db import SessionLocal

    force = "--force" in sys.argv[1:]
    session = SessionLocal()
    try:
        if force:
            seed(session)
            print("Seeded (forced full reseed).")
        elif seed_if_empty(session):
            print("Seeded (database was empty).")
        else:
            print("Database already has data — skipped seeding. Use --force to reseed.")
    finally:
        session.close()


if __name__ == "__main__":
    main()
