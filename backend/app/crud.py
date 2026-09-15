import uuid
from typing import Any, Generic, TypeVar

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.base import EntityMixin

ModelT = TypeVar("ModelT", bound=EntityMixin)


class CRUDRepository(Generic[ModelT]):
    """Thin, soft-delete-aware data access layer shared by all admin routers."""

    def __init__(self, model: type[ModelT]) -> None:
        self.model = model

    def list(self, db: Session, *, include_deleted: bool = False) -> list[ModelT]:
        stmt = select(self.model)
        if not include_deleted:
            stmt = stmt.where(self.model.is_deleted.is_(False))
        return list(db.scalars(stmt))

    def get(
        self, db: Session, obj_id: uuid.UUID, *, include_deleted: bool = False
    ) -> ModelT | None:
        obj = db.get(self.model, obj_id)
        if obj is None:
            return None
        if obj.is_deleted and not include_deleted:
            return None
        return obj

    def create(self, db: Session, obj: ModelT) -> ModelT:
        db.add(obj)
        db.commit()
        db.refresh(obj)
        return obj

    def update(self, db: Session, obj: ModelT, changes: dict[str, Any]) -> ModelT:
        for key, value in changes.items():
            setattr(obj, key, value)
        db.commit()
        db.refresh(obj)
        return obj

    def soft_delete(self, db: Session, obj: ModelT) -> ModelT:
        obj.is_deleted = True
        db.commit()
        db.refresh(obj)
        return obj
