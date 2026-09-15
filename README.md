# VetDose

Калькулятор доз для ветеринарного лікаря змішаної практики: вид тварини + вага
+ препарат → доза в одиницях діючої речовини та в мл/таблетках, з видовими
протипоказаннями та термінами виведення для продуктивних тварин.

Монорепозиторій: `backend` (FastAPI), `android` (Kotlin/Compose, пізніше),
`web` (React PWA, пізніше), `shared` (спільні тест-кейси калькулятора доз).

## ⚠️ Дані

Уся клінічна інформація в базі на цьому етапі — **фіктивні тестові дані**
(`TEST_`-префікс, `source = "TEST DATA — NOT FOR CLINICAL USE"`,
`is_verified = false`). Жодне число тут не є реальною рекомендованою дозою.

## Статус

- **Етап 1 (готово):** ядро backend — моделі, міграція, seed, калькулятор доз,
  спільні тест-кейси, тести.
- **Етап 2 (готово):** REST API (`/api/v1`), X-API-Key для запису, єдиний
  формат помилок, Dockerfile, docker-compose з PostgreSQL.
- Етапи 3–8 (Android, Web PWA, фіналізація) — у розробці.

## Запуск через Docker (найшвидший шлях)

```bash
docker compose up --build
```

Піднімає PostgreSQL і backend; контейнер backend сам застосовує міграції
Alembic і наповнює базу фіктивними тестовими даними **лише якщо вона порожня**
(перезапуск контейнера нічого не стирає). Перевірити:

```bash
curl http://localhost:8000/api/v1/health
curl http://localhost:8000/api/v1/species
```

Документація API (Swagger UI) — http://localhost:8000/docs.

За потреби задайте власний ключ адміністратора перед підняттям:
`ADMIN_API_KEY=your-key docker compose up --build`.

## Backend (без Docker)

Стек: Python 3.12, FastAPI, SQLAlchemy 2.x, Alembic, PostgreSQL (SQLite —
лише для тестів), `uv` для керування залежностями, ruff + mypy, pytest.

```bash
cd backend
uv sync                       # встановити залежності
uv run pytest                 # тести + покриття
uv run ruff check . && uv run ruff format --check .
uv run mypy app tests

# міграції (потребують PostgreSQL; DATABASE_URL з .env або змінної оточення)
uv run alembic upgrade head
uv run python -m app.seed.seed_data              # наповнити, лише якщо порожньо
uv run python -m app.seed.seed_data --force       # примусово перенаповнити (dev)

uv run uvicorn app.main:app --reload              # http://localhost:8000/docs
```

Скопіюйте `backend/.env.example` у `backend/.env` і за потреби змініть
`DATABASE_URL` / `ADMIN_API_KEY`.

## API

Префікс `/api/v1`, документація — `/docs` (OpenAPI/Swagger).

- Публічні (без ключа): `GET /species`, `GET /products` (пошук,
  `?search=&species_id=`), `GET /products/{id}` (з правилами,
  протипоказаннями й термінами виведення), `POST /calculate`,
  `GET /sync?since=` (інкрементна синхронізація, включно з видаленими
  записами), `GET /health`.
- Адмінські (заголовок `X-API-Key`): CRUD для `species`, `substances`,
  `products`, `dose-rules`, `contraindications`, `withdrawal-periods`;
  `POST /dose-rules/{id}/verify`.
- Помилки — завжди `{"error": {"code": "...", "message": "..."}}`.

## Спільні тест-кейси калькулятора

[`shared/calculation_test_cases.json`](shared/calculation_test_cases.json) —
35+ кейсів (вхід → очікуваний результат), які проганяються проти
`backend/app/services/calculator.py` (pytest), а пізніше — і проти Kotlin- та
TypeScript-реалізацій того самого калькулятора, щоб гарантувати однаковий
розрахунок на сервері, в Android-програмі та у вебзастосунку.
