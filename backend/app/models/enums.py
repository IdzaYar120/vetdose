import enum


class ProductForm(enum.StrEnum):
    INJECTION_SOLUTION = "injection_solution"
    ORAL_SOLUTION = "oral_solution"
    TABLET = "tablet"
    POWDER = "powder"
    SUSPENSION = "suspension"
    OTHER = "other"


class ConcentrationUnit(enum.StrEnum):
    MG_PER_ML = "mg_per_ml"
    MCG_PER_ML = "mcg_per_ml"
    IU_PER_ML = "iu_per_ml"
    MG_PER_TABLET = "mg_per_tablet"
    MG_PER_G = "mg_per_g"


class Route(enum.StrEnum):
    IV = "iv"
    IM = "im"
    SC = "sc"
    PO = "po"
    TOPICAL = "topical"
    OTHER = "other"


class DoseUnit(enum.StrEnum):
    MG_PER_KG = "mg_per_kg"
    MCG_PER_KG = "mcg_per_kg"
    IU_PER_KG = "iu_per_kg"
    ML_PER_KG = "ml_per_kg"
    ML_PER_10KG = "ml_per_10kg"
    MG_PER_ANIMAL = "mg_per_animal"
    ML_PER_ANIMAL = "ml_per_animal"


class MaxTotalDoseUnit(enum.StrEnum):
    MG = "mg"
    MCG = "mcg"
    IU = "iu"
    ML = "ml"


class Severity(enum.StrEnum):
    ABSOLUTE = "absolute"
    CAUTION = "caution"


class FoodProduct(enum.StrEnum):
    MEAT = "meat"
    MILK = "milk"
    EGGS = "eggs"
    HONEY = "honey"
