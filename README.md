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
- Етапи 2–8 (API, Docker, Android, Web PWA) — у розробці.

## Backend

Стек: Python 3.12, FastAPI, SQLAlchemy 2.x, Alembic, PostgreSQL (SQLite —
лише для тестів), `uv` для керування залежностями, ruff + mypy, pytest.

```bash
cd backend
uv sync                       # встановити залежності
uv run pytest                 # тести + покриття
uv run ruff check . && uv run ruff format --check .
uv run mypy app

# міграції (потребують PostgreSQL; DATABASE_URL з .env або змінної оточення)
uv run alembic upgrade head
uv run python -m app.seed.seed_data   # наповнити фіктивними тестовими даними
```

Скопіюйте `backend/.env.example` у `backend/.env` і за потреби змініть
`DATABASE_URL` / `ADMIN_API_KEY`.

## Спільні тест-кейси калькулятора

[`shared/calculation_test_cases.json`](shared/calculation_test_cases.json) —
35+ кейсів (вхід → очікуваний результат), які проганяються проти
`backend/app/services/calculator.py` (pytest), а пізніше — і проти Kotlin- та
TypeScript-реалізацій того самого калькулятора, щоб гарантувати однаковий
розрахунок на сервері, в Android-програмі та у вебзастосунку.
