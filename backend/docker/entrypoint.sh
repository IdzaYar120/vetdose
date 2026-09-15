#!/bin/sh
set -e

echo "Waiting for the database..."
attempt=0
until uv run python -c "
from sqlalchemy import create_engine
from app.config import get_settings
create_engine(get_settings().database_url).connect().close()
" 2>/dev/null; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 30 ]; then
        echo "Database did not become available in time." >&2
        exit 1
    fi
    sleep 1
done

echo "Running migrations..."
uv run alembic upgrade head

echo "Seeding fictitious test data (skipped if the database already has data)..."
uv run python -m app.seed.seed_data

echo "Starting: $*"
exec "$@"
