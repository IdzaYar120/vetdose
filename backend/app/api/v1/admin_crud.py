import uuid
from collections.abc import Sequence
from typing import Any

from fastapi import APIRouter, Depends, status
from pydantic import BaseModel

from app.crud import CRUDRepository, ModelT
from app.db import DbSession
from app.errors import APIError
from app.security import require_admin_api_key

Action = str  # one of "list", "get", "create", "update", "delete"
ALL_ACTIONS: frozenset[Action] = frozenset({"list", "get", "create", "update", "delete"})


def build_admin_crud_router(
    *,
    model: type[ModelT],
    create_schema: type[BaseModel],
    update_schema: type[BaseModel],
    read_schema: type[BaseModel],
    prefix: str,
    tags: Sequence[str],
    actions: frozenset[Action] = ALL_ACTIONS,
    not_found_message: str = "Запис не знайдено.",
) -> APIRouter:
    """Builds a standard admin CRUD router (all routes require X-API-Key).

    Every entity in this API is soft-deleted and has the same shape (id,
    created_at, updated_at, is_deleted, plus its own columns), so the five
    routes below are identical for species/substance/product/dose_rule/
    contraindication/withdrawal_period. ``actions`` lets a caller drop routes
    that are already covered by a public read endpoint (e.g. products).
    """

    router = APIRouter(
        prefix=prefix, tags=list(tags), dependencies=[Depends(require_admin_api_key)]
    )
    repo: CRUDRepository[ModelT] = CRUDRepository(model)

    if "list" in actions:

        @router.get("", response_model=list[read_schema])  # type: ignore[valid-type]
        def list_items(db: DbSession) -> list[ModelT]:
            return repo.list(db)

    if "get" in actions:

        @router.get("/{item_id}", response_model=read_schema)
        def get_item(item_id: uuid.UUID, db: DbSession) -> ModelT:
            obj = repo.get(db, item_id)
            if obj is None:
                raise APIError("NOT_FOUND", not_found_message, status.HTTP_404_NOT_FOUND)
            return obj

    if "create" in actions:

        @router.post("", response_model=read_schema, status_code=status.HTTP_201_CREATED)
        def create_item(payload: create_schema, db: DbSession) -> ModelT:  # type: ignore[valid-type]
            obj = model(**payload.model_dump())  # type: ignore[attr-defined]
            return repo.create(db, obj)

    if "update" in actions:

        @router.patch("/{item_id}", response_model=read_schema)
        def update_item(
            item_id: uuid.UUID,
            payload: update_schema,  # type: ignore[valid-type]
            db: DbSession,
        ) -> ModelT:
            obj = repo.get(db, item_id)
            if obj is None:
                raise APIError("NOT_FOUND", not_found_message, status.HTTP_404_NOT_FOUND)
            changes: dict[str, Any] = payload.model_dump(exclude_unset=True)  # type: ignore[attr-defined]
            return repo.update(db, obj, changes)

    if "delete" in actions:

        @router.delete("/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
        def delete_item(item_id: uuid.UUID, db: DbSession) -> None:
            obj = repo.get(db, item_id)
            if obj is None:
                raise APIError("NOT_FOUND", not_found_message, status.HTTP_404_NOT_FOUND)
            repo.soft_delete(db, obj)

    return router
