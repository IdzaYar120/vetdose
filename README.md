# VetDose

Калькулятор доз для ветеринарного лікаря змішаної практики: вид тварини + вага
+ препарат → доза в одиницях діючої речовини та в мл/таблетках, з видовими
протипоказаннями та термінами виведення для продуктивних тварин.

Монорепозиторій: `backend` (FastAPI), `android` (Kotlin/Compose),
`web` (React PWA), `shared` (спільні тест-кейси калькулятора доз).

## Архітектура

```
                    ┌─────────────┐
                    │  PostgreSQL │
                    └──────┬──────┘
                           │
                    ┌──────┴──────┐
                    │   backend   │  FastAPI, /api/v1
                    │  (джерело   │  X-API-Key для запису
                    │   правди)   │
                    └──┬───────┬──┘
             GET /sync │       │ GET /sync
        (лише читання) │       │ (лише читання)
              ┌────────┴──┐ ┌──┴────────┐
              │  android   │ │    web    │
              │ (Room —    │ │ (Dexie —  │
              │  офлайн)   │ │  офлайн)  │
              └────────────┘ └───────────┘
```

Backend — єдине джерело правди й місце запису (адмінський CRUD за
`X-API-Key`). Android і web — незалежні офлайн-перші клієнти: обидва тримають
повну копію довідників локально (Room / Dexie) і синхронізують її одним
ендпоінтом, `GET /api/v1/sync`; калькулятор і пошук завжди працюють проти
локальних даних, мережа потрібна лише для синхронізації. Алгоритм розрахунку
дози реалізовано тричі незалежно — `backend/app/services/calculator.py`
(Python/`Decimal`), `android/.../DoseCalculator.kt` (Kotlin/`BigDecimal`),
`web/src/domain/calculator/doseCalculator.ts` (TypeScript/`decimal.js`) — і
всі три перевіряються проти одного набору кейсів,
[`shared/calculation_test_cases.json`](shared/calculation_test_cases.json),
щоб розрахунок залишався ідентичним на всіх платформах.

## ⚠️ Дані

Уся клінічна інформація в базі на цьому етапі — **фіктивні тестові дані**
(`TEST_`-префікс, `source = "TEST DATA — NOT FOR CLINICAL USE"`,
`is_verified = false`). Жодне число тут не є реальною рекомендованою дозою.

## Статус

- **Етап 1 (готово):** ядро backend — моделі, міграція, seed, калькулятор доз,
  спільні тест-кейси, тести.
- **Етап 2 (готово):** REST API (`/api/v1`), X-API-Key для запису, єдиний
  формат помилок, Dockerfile, docker-compose з PostgreSQL.
- **Етап 3 (готово):** Android-каркас (Gradle, Compose, тема, навігація,
  Hilt), `DoseCalculator.kt` у domain-шарі, JUnit-тести на спільних кейсах.
- **Етап 4 (готово):** Android — Room (сутності, DAO), Retrofit, репозиторії,
  `SyncWorker` (WorkManager, кожні 12 год + вручну), налаштування сервера
  через DataStore.
- **Етап 5 (готово):** Android — усі екрани (розрахунок, пошук препарату,
  результат, історія, налаштування) з ViewModel, обрані препарати,
  Compose UI-тести для екрана результату й попереджень.
- **Етап 6 (готово):** Web — каркас (Vite + React + TypeScript), третій
  незалежний порт калькулятора (`doseCalculator.ts`, decimal.js), офлайн-дані
  через Dexie (IndexedDB), синхронізація з `/api/v1/sync`, базова конфігурація
  PWA (`vite-plugin-pwa`).
- **Етап 7 (готово):** Web — усі екрани (розрахунок, пошук препарату,
  результат, історія, налаштування), історія й обрані препарати (Dexie),
  iOS-особливості PWA (банер «На екран «Домівка»», відновлення після
  очищення сховища), Testing Library + Playwright (Chromium + WebKit),
  Dockerfile + nginx, інтеграція з docker-compose.
- **Етап 8 (готово):** фіналізація — цей README (архітектура, як додати
  реальні клінічні дані через API, тестування PWA на iPhone, відомі
  обмеження), повний прогін тестів усіх трьох клієнтів разом.

## Запуск через Docker (найшвидший шлях)

```bash
docker compose up --build
```

Піднімає PostgreSQL, backend і web (nginx, статична збірка). Backend сам
застосовує міграції Alembic і наповнює базу фіктивними тестовими даними
**лише якщо вона порожня** (перезапуск контейнера нічого не стирає).
Перевірити:

```bash
curl http://localhost:8000/api/v1/health
curl http://localhost:8000/api/v1/species
```

Документація API (Swagger UI) — http://localhost:8000/docs. Веб-застосунок —
http://localhost:8080.

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

## Додавання реальних клінічних даних

Уся інформація в базі за замовчуванням — фіктивні тестові дані (див. вище).
Реальні дозування вводить лікар через адмінське API (`X-API-Key`), одним
записом за раз. Значення нижче — приклади формату запиту, **не реальні
дози**: замініть їх дійсними, перевіреними лікарем числами й джерелом.

```bash
API=http://localhost:8000/api/v1
KEY=your-admin-api-key   # ADMIN_API_KEY з .env / docker-compose

# 1. Діюча речовина (переглянути наявні: GET $API/substances з X-API-Key;
#    якщо потрібної немає — створити нову).
curl -X POST $API/substances -H "X-API-Key: $KEY" -H "Content-Type: application/json" \
  -d '{"name": "Amoxicillin", "name_uk": "Амоксицилін"}'
# -> {"id": "<substance_id>", ...}

# 2. Торгова форма препарату (концентрація/форма визначають подальший
#    розрахунок об'єму чи кількості таблеток).
curl -X POST $API/products -H "X-API-Key: $KEY" -H "Content-Type: application/json" \
  -d '{"trade_name": "Amoxi-Vet 150", "substance_id": "<substance_id>",
       "form": "injection_solution", "concentration_value": "150",
       "concentration_unit": "mg_per_ml"}'

# 3. Правило дозування: вид тварини (species_id — GET $API/species),
#    substance_id, шлях введення, діапазон дози, джерело.
#    is_verified: false, доки лікар не підтвердив (крок 4) — застосунки
#    показують непідтверджені правила з попередженням.
curl -X POST $API/dose-rules -H "X-API-Key: $KEY" -H "Content-Type: application/json" \
  -d '{"substance_id": "<substance_id>", "species_id": "<species_id>",
       "route": "im", "dose_min": "10", "dose_max": "20",
       "dose_unit": "mg_per_kg",
       "source": "<реальне джерело — довідник, інструкція, тощо>",
       "is_verified": false}'
# -> {"id": "<dose_rule_id>", ...}

# 4. Лікар підтверджує правило після перевірки.
curl -X POST $API/dose-rules/<dose_rule_id>/verify -H "X-API-Key: $KEY"

# За потреби: протипоказання (POST $API/contraindications, severity
# "absolute"|"caution") і терміни виведення для продуктивних тварин
# (POST $API/withdrawal-periods).
```

На Windows у Git Bash/MSYS кирилиця в інлайновому `-d '...'` іноді псується
до відправки (сервер відповідає `"There was an error parsing the body"`) —
у такому разі покладіть JSON у файл і скористайтесь `--data-binary @file.json`
замість `-d`.

`dose_unit` / `concentration_unit` / `route` / `severity` / `food_product` —
рядкові enum-значення з `backend/app/models/enums.py`; `PATCH` на той самий
шлях редагує запис частково, `DELETE` — м'яко видаляє (`is_deleted=true`,
зникає з `/sync` для клієнтів). Повний список полів і обмежень — `/docs`
(Swagger UI) або `backend/app/schemas/*.py`.

Після цього Android і web підхоплять зміни при наступній синхронізації
(«Синхронізувати зараз» на екрані Налаштувань, або автоматично раз на 12 год
в Android).

## Android

Стек: Kotlin, Jetpack Compose (Material 3), Hilt, Navigation Compose.
`domain/calculator/DoseCalculator.kt` — незалежний від Android-фреймворку
Kotlin-порт `backend/app/services/calculator.py` (той самий алгоритм,
округлення, коди попереджень і текст пояснень українською).

```bash
cd android
./gradlew testDebugUnitTest   # 63 тести: 37 спільних кейсів калькулятора + Room/sync + Compose UI
./gradlew assembleDebug       # зібрати debug APK
./gradlew lintDebug           # Android Lint
```

Потрібні: JDK 17+ і Android SDK (`compileSdk`/`targetSdk` 36, `minSdk` 26) —
задайте шлях до SDK у `android/local.properties` (`sdk.dir=...`, у
`.gitignore`) або через змінну оточення `ANDROID_HOME`.

### Дані й синхронізація (Етап 4)

- **Room** — єдине джерело правди для UI (`data/local/`): 6 сутностей
  (species, substance, product, dose_rule, contraindication,
  withdrawal_period), 1:1 з таблицями backend. Видалені на сервері записи
  фізично видаляються локально, а не позначаються — застосунок ніколи не
  показує м'яко видалені рядки, тож тримати їх локально нема сенсу.
- **Retrofit** — лише один ендпоінт, `GET /api/v1/sync` (`data/remote/`):
  калькулятор і пошук працюють повністю офлайн проти Room, мережа потрібна
  лише для синхронізації.
- **Репозиторії** (`data/repository/`) — по одному на сутність (читання з
  Room як `Flow`, мапінг у domain-моделі) + `SyncRepository` (застосовує
  `/sync` в одній Room-транзакції: upsert активних рядків, видалення тих, де
  `is_deleted=true`).
- **SyncWorker** (`data/sync/`) — Hilt-інтегрований `CoroutineWorker`:
  періодично раз на 12 год (лише за наявності мережі) через `SyncScheduler`,
  плюс одноразовий запуск для кнопки «Синхронізувати зараз».
- **Налаштування** (`data/settings/SettingsRepository.kt`, DataStore) —
  адреса сервера (за замовчуванням `http://10.0.2.2:8000/` — аліас хоста в
  емуляторі) і час останньої синхронізації (курсор `since` для наступного
  інкрементного `/sync`); `BaseUrlInterceptor` підставляє її в кожен запит,
  тож зміна адреси не потребує перестворення Retrofit.
- **Мережа**: `network_security_config.xml` дозволяє HTTP лише для
  `10.0.2.2`/`localhost` і лише в debug-збірці (`src/debug/res/xml/`);
  release-збірка приймає тільки HTTPS.
- Тести Room/sync запускаються через Robolectric (реальний in-memory SQLite,
  без емулятора); пінінг `@Config(sdk = [34])` обходить баг сумісності
  Robolectric 4.17 з JDK 21 на щойно доданих у Robolectric API-36 шейдах.

### Екрани (Етап 5)

- **Розрахунок** (стартовий екран) — чіпи видів з емодзі-іконками, поле ваги
  (приймає кому й крапку), вибір препарату (перехід на пошук), кнопка
  «Розрахувати». Порожня база → екран «Потрібна перша синхронізація» з
  переходом у Налаштування. Вага поза типовим діапазоном виду → діалог
  підтвердження перед розрахунком.
- **Пошук препарату** — пошук за назвою препарату або діючою речовиною
  (`String.contains(ignoreCase = true)`, коректно для кирилиці — не SQL
  `LIKE`), перемикач фільтра за видом, секція «Обрані» над «Усі препарати»,
  зірочка для додавання/прибирання з обраних (`favorite_product`, Room,
  локально).
- **Результат** — великий шрифт для об'єму/дози (точний масштаб від
  `roundVolumeForDisplay`, не обрізаний до «голого» числа), діапазон дози,
  шлях введення, кратність, кольорові картки попереджень (`WarningCard`),
  терміни виведення з розрахованою датою (сьогодні + днів), розгортуване
  покрокове пояснення, джерело правила. Абсолютне протипоказання — червона
  картка, що приховує дозу до явного підтвердження лікарем.
- **Історія** — останні розрахунки (`calculation_history`, Room, локально;
  зберігаються лише вхідні дані розрахунку, не результат — калькулятор
  дешевий і чистий, тож результат завжди перераховується заново при показі).
  Дотик по запису — повторний перехід на Результат.
- **Налаштування** — адреса сервера (з підставленням у кожен запит через
  `BaseUrlInterceptor`), кнопка «Синхронізувати зараз» зі станом виконання
  через `WorkManager.getWorkInfosForUniqueWorkFlow`, час останньої
  синхронізації, дисклеймер «рішення приймає лікар».
- Compose UI-тести (`ui/components/WarningCardTest.kt`,
  `ui/screens/result/ResultContentTest.kt`) запускаються через Robolectric
  без емулятора; знадобився debug-only `AndroidManifest.xml`, що додає
  LAUNCHER-інтент-фільтр тестовій `ComponentActivity` (в пінованій версії
  `androidx.compose.ui:ui-test-manifest` його бракує, і Robolectric інакше
  відмовляється резолвити навіть explicit-component Intent).

**Чому AGP 8.13.2, а не 9.x:** на момент розробки Hilt Gradle-плагін уже
вимагає AGP ≥9.0, але найновіші версії AndroidX (Compose BOM, core-ktx,
lifecycle, navigation-compose) вже вимагають AGP ≥9.1/`compileSdk` 37 — а
класичний плагін `org.jetbrains.kotlin.android`, потрібний для KSP
(анотації Hilt, а з Етапу 4 — Room), поки що несумісний із новим внутрішнім
Kotlin-режимом AGP 9. Тому версії в `gradle/libs.versions.toml` свідомо
пришпилені на кілька релізів раніше найновіших — це найширша зона
сумісності станом на зараз, а не недогляд. Gradle-обгортка — 9.5.1 (не
найновіша 9.7.x: AGP 8.x несумісний із внутрішніми API, прибраними в
Gradle 9.6).

## Web

Стек: Vite, React, TypeScript, `decimal.js` (розрахунки), Dexie (IndexedDB),
`vite-plugin-pwa`. `domain/calculator/doseCalculator.ts` — третій незалежний
порт `backend/app/services/calculator.py` (той самий алгоритм, округлення,
коди попереджень і текст пояснень українською).

```bash
cd web
npm install
npm run dev          # http://localhost:5173
npm run test          # Vitest: компонентні тести + shared/calculation_test_cases.json
npm run test:e2e      # Playwright (Chromium + WebKit) — потребує запущений backend
npm run typecheck
npm run lint
npm run build         # також генерує service worker PWA
```

Бекенд за замовчуванням дозволяє CORS з `http://localhost:5173` (Vite dev) і
`http://localhost:8080` (контейнер web, нижче).

### Дані й синхронізація (Етап 6)

- **Dexie (IndexedDB)** — офлайн-джерело правди для UI (`data/local/`): одна
  таблиця на кожну з 6 сутностей backend плюс `settings` (адреса сервера,
  курсор `since`) — вебеквівалент Android DataStore, і (з Етапу 7)
  `calculationHistory`/`favoriteProducts` — локальні, ніколи не
  синхронізуються, як і в Android. Видалені на сервері записи фізично
  видаляються локально, як і в Android.
- **`data/remote/`** — типи DTO (snake_case, 1:1 з Pydantic-схемами) і
  єдиний виклик API, `GET /api/v1/sync`; пошук і розрахунок дози працюють
  повністю офлайн проти Dexie.
- **`data/sync.ts`** — `runSync()` застосовує відповідь `/sync` в одній
  Dexie-транзакції (upsert активних рядків, видалення позначених
  `is_deleted`), потім просуває курсор `since`.
- **`data/repositories.ts`** — по одній групі функцій на сутність
  (вебеквівалент Android `data/repository/*.kt`), реактивність — через
  `dexie-react-hooks`'s `useLiveQuery` замість власної обгортки над `Flow`.

### Екрани та iOS-особливості (Етап 7)

- **Розрахунок** (стартовий екран) — чіпи видів з емодзі, поле ваги (приймає
  кому й крапку), вибір препарату. Пошук препарату рендериться як
  full-screen оверлей усередині самого екрана, **не** окремий маршрут —
  навігація на `/search/...` і назад знищила б стан вибору виду/ваги
  (React Router, на відміну від Android NavHost, не зберігає стан екрана
  лише тому, що зверху був відкритий інший).
- **Результат** — розділений на обгортку із завантаженням даних і чисті
  `ResultContent`/`AbsoluteContraindicationGate` (як в Android), щоб їх можна
  було тестувати із синтетичними props без поліфілу IndexedDB. Абсолютне
  протипоказання приховує дозу до підтвердження лікарем.
- **Історія** / **Налаштування** — як і в Android Етапу 5.
- **iOS PWA**: `ui/IosInstallBanner.tsx` — інструкція «Поділитися → На екран
  «Домівка»» (у iOS Safari немає нативного `beforeinstallprompt`),
  визначення iPadOS (звітує як `MacIntel` із тач-підтримкою, не `iPad`).
  `data/useNeedsSync.ts` розрізняє «ще не синхронізовано» від «синхронізовано
  раніше, але таблиці порожні зараз» (iOS Safari може витіснити IndexedDB під
  тиском сховища) — і показує відповідне повідомлення.

Перевірено проти реального backend у справжньому браузері (Playwright,
Chromium + WebKit — той самий рушій, що й у iOS Safari): повний цикл
розрахунку, оверлей пошуку зберігає вибір після повернення, ворота
абсолютного протипоказання, а також (Етап 6) що дані переживають
перезавантаження сторінки й повторна синхронізація не дублює записи.

### PWA / Docker

`Dockerfile` — двоетапна збірка (Node → статичний `dist/`, обслуговується
nginx, `docker/nginx.conf`); `docker compose up --build` з кореня репо також
піднімає його на `http://localhost:8080` поруч із backend і PostgreSQL —
nginx повертає `index.html` для будь-якого незнайденого шляху, тож маршрути
react-router (напр. `/result/...`) працюють і при прямому переході, не лише
при клієнтській навігації.

## Тестування PWA на iPhone

Веб-застосунок — це PWA (`vite-plugin-pwa`), і на iOS встановлюється лише
через Safari (Chrome/Firefox на iOS теж використовують WebKit, але не мають
доступу до «Додати на екран «Домівка»»). Адреса має бути **HTTPS** (окрім
`localhost`) — iOS не реєструє service worker на звичайному HTTP; для
перевірки з телефону в локальній мережі знадобиться reverse-proxy з
сертифікатом (напр. `Caddy` чи `ngrok`) перед контейнером `web` на :8080, або
розгортання на реальному HTTPS-домені.

1. Відкрити адресу застосунку в Safari на iPhone.
2. Якщо ще не встановлено — вгорі має з'явитись банер (жовтогарячий,
   `IosInstallBanner.tsx`) з інструкцією: «Поділитися» (значок ⬆️ внизу
   екрана) → «На екран «Домівка»». Це не нативний Chrome-банер (`beforeinstallprompt`
   на iOS Safari не існує), тому текст показує застосунок сам.
3. Після встановлення — відкрити з екрана «Домівка» (не з Safari): застосунок
   запускається в standalone-режимі (без адресного рядка браузера), банер
   install зникає.
4. Перейти в Налаштування → «Синхронізувати зараз», перевірити, що дані
   з'явились (кількість видів/препаратів на екрані Розрахунку).
5. Перевірити офлайн-режим: увімкнути режим польоту, зробити розрахунок —
   калькулятор і пошук мають працювати без мережі (усе, крім самої
   синхронізації, офлайн за задумом).
6. Перевірка відновлення після очищення сховища (iOS Safari може витіснити
   IndexedDB під тиском пам'яті, зазвичай після ~7 днів без використання):
   Налаштування Safari → Additional Data → видалити дані сайту для адреси
   застосунку, відкрити застосунок знову — має з'явитись повідомлення
   «Дані було очищено» (не «Потрібна перша синхронізація» — застосунок
   пам'ятає, що синхронізація вже була, через збережений час останньої
   синхронізації) з кнопкою переходу в Налаштування.

## Спільні тест-кейси калькулятора

[`shared/calculation_test_cases.json`](shared/calculation_test_cases.json) —
35+ кейсів (вхід → очікуваний результат), які проганяються проти
`backend/app/services/calculator.py` (pytest), `DoseCalculator.kt` (JUnit) і
`web/src/domain/calculator/doseCalculator.ts` (Vitest), щоб гарантувати
однаковий розрахунок на сервері, в Android-програмі та у вебзастосунку.

## Відомі обмеження

- **Синхронізація лише в один бік** (сервер → клієнт, `GET /sync`). Ні
  Android, ні web не можуть створювати чи редагувати довідникові дані з
  самого застосунку — лише адмінське API. Це свідоме рішення (лікар
  редагує дані централізовано, застосунки — лише розрахунок), не недогляд,
  але варто знати, якщо очікувалась можливість редагування з телефону.
- **Немає користувацької автентифікації.** Синхронізація (`GET /sync`) і
  розрахунок (`POST /calculate`) — публічні ендпоінти без ключа: будь-хто з
  адресою сервера бачить весь довідник. `X-API-Key` захищає лише запис.
  Прийнятно для застосунку однієї клініки за NAT/VPN; для публічного
  розгортання знадобиться окремий шар автентифікації користувачів.
- **`tablet_divisible_by` дозволяє 1–4**, але формула ділення таблетки дає
  точний, скінченний крок лише для 1, 2 і 4 (степені двійки); значення 3
  математично коректне, проте дає крок з довгим періодичним дробом
  (~28 значущих цифр) — однаково поводиться в усіх трьох реалізаціях
  калькулятора (навмисно, не розбіжність), але клінічно ділити таблетку на
  треті непрактично, тож це значення краще не використовувати для реальних
  препаратів.
- **Немає CI/CD.** Тести (backend/Android/web) запускаються вручну; немає
  GitHub Actions чи подібного пайплайна, немає налаштованого підписання
  Android release-збірки для Google Play.
- **iOS — лише PWA**, нативного застосунку немає. PWA на iOS вимагає HTTPS
  (окрім `localhost`) для service worker — реального публічного HTTPS-хосту
  в цьому репозиторії не налаштовано, лише інструкції (див. вище).
- **Синхронізація на web — лише вручну** (кнопка «Синхронізувати зараз»);
  на відміну від Android (`SyncWorker`, WorkManager, раз на 12 год), у
  браузера немає надійного фонового періодичного запуску без Periodic
  Background Sync API, який підтримує далеко не кожен браузер.
- **Пошук препаратів** (Android і web) завантажує всю локальну таблицю
  `product`/`products` у пам'ять і фільтрує на клієнті (для коректного
  порівняння кирилиці без урахування регістру — SQL/IndexedDB `LIKE`-пошук
  цього не вміє). Прийнятно для довідника ветклініки (десятки-сотні
  препаратів), не розраховано на тисячі записів без пагінації.
- **Резервне копіювання PostgreSQL** не автоматизоване — том Docker
  (`vetdose_pg_data`) переживає перезапуск контейнера, але бекапи/відновлення
  для продакшену не налаштовані й не документовані.
