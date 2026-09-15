from fastapi import APIRouter

from app.api.v1 import (
    calculate,
    contraindications,
    dose_rules,
    health,
    products,
    species,
    substances,
    sync,
    withdrawal_periods,
)

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(health.router)
api_router.include_router(species.public_router)
api_router.include_router(species.admin_router)
api_router.include_router(substances.admin_router)
api_router.include_router(products.public_router)
api_router.include_router(products.admin_router)
api_router.include_router(dose_rules.admin_router)
api_router.include_router(dose_rules.extra_router)
api_router.include_router(contraindications.admin_router)
api_router.include_router(withdrawal_periods.admin_router)
api_router.include_router(calculate.router)
api_router.include_router(sync.router)
