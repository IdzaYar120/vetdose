from fastapi import Header

from app.config import get_settings
from app.errors import APIError


def require_admin_api_key(x_api_key: str | None = Header(default=None)) -> None:
    settings = get_settings()
    if not x_api_key or x_api_key != settings.admin_api_key:
        raise APIError("UNAUTHORIZED", "Недійсний або відсутній X-API-Key.", status_code=401)
