# AGENTS.md

Руководство для AI-агентов и разработчиков, работающих над веб-приложением для изучения китайского языка.

## Обзор проекта

Веб-приложение для изучения китайских иероглифов с системой интервального повторения (FSRS-5). Два режима тестирования: распознавание (иероглиф → пиньинь + английский перевод) и воспроизведение (английское слово → рисование иероглифа).

**Стек:**
- Backend: Spring Boot 3.x, Java 21, PostgreSQL, Flyway
- Frontend: React 18, TypeScript, Vite, TanStack Query, Zustand, Tailwind CSS, shadcn/ui
- Ключевые библиотеки: HanziWriter.js (рисование), pinyin-pro (нормализация пиньиня)
- Без Redis — JWT stateless, rate limiting через Bucket4j in-memory

## Принципы

- **MVP first.** Не делаем то, что не входит в MVP.
- **Архитектура под рост, без overengineering.** Backend stateless, индексы продуманы, мультиязычность заложена в схему — но без преждевременных абстракций.
- **Английский — единственный язык переводов в MVP.** Схема БД готова к `ru`, но контент только `en`.
- **Никакого Redis.** Если нужен кеш — обсуждается отдельно.
- **Тесты на критическую логику обязательны:** FSRS-алгоритм, grading-сервис, выдача очереди.

---

## Структура задач

Задачи разбиты на этапы по неделям. Каждая задача имеет ID, описание, критерии готовности (Definition of Done) и зависимости.

---

## Этап 1: Фундамент (неделя 1)

### TASK-001: Скелет Spring Boot проекта
- Создать проект на Spring Boot 3.x + Java 21
- Подключить: Spring Web, Spring Data JPA, Spring Security, Validation, Flyway, PostgreSQL Driver, Lombok, Bucket4j
- Настроить структуру пакетов: `auth`, `user`, `hanzi`, `deck`, `study/session`, `study/srs`, `study/grading`, `stats`, `admin`, `common`
- Настроить `application.yml` для dev/prod профилей
- **DoD:** проект собирается, запускается, есть healthcheck эндпоинт `/actuator/health`

### TASK-002: Docker Compose для локальной разработки
- `docker-compose.yml` с сервисами: `postgres`, `backend`, `frontend`
- `.env.example` со всеми переменными окружения
- README с инструкцией запуска
- **DoD:** `docker compose up` поднимает весь стек, backend подключается к БД

### TASK-003: Миграции Flyway — базовая схема
Создать миграции для таблиц:
- `users(id, email UNIQUE, password_hash, role, settings_json JSONB, created_at, updated_at)`
- `refresh_tokens(token_hash UNIQUE, user_id FK, expires_at, revoked, created_at)`
- `hanzi(id, character UNIQUE, pinyin, stroke_count, hsk_level, frequency_rank, status, created_at, updated_at)`
- `hanzi_translation(id, hanzi_id FK, language, meanings TEXT[], is_primary)`
- `hanzi_example(id, hanzi_id FK, language, sentence, pinyin, translation)`
- `deck(id, name, slug UNIQUE, is_system, description)`
- `deck_hanzi(deck_id FK, hanzi_id FK, position, PK(deck_id, hanzi_id))`
- `user_deck(user_id FK, deck_id FK, subscribed_at, PK(user_id, deck_id))`
- `user_card(id, user_id FK, hanzi_id FK, mode, state, stability, difficulty, last_review, due_date, reps, lapses, elapsed_days, scheduled_days, algorithm_version, UNIQUE(user_id, hanzi_id, mode))`
- `review_log(id, user_card_id FK, rating, state_before, elapsed_days, scheduled_days, stability_before, difficulty_before, reviewed_at, response_time_ms, hint_count, stroke_mistakes)`

Индексы:
- `user_card(user_id, due_date)`
- `hanzi(status, hsk_level)`
- GIN + pg_trgm на `hanzi.character`, `hanzi.pinyin`
- `review_log(user_card_id, reviewed_at)`
- `review_log(reviewed_at)`

**DoD:** все миграции применяются на чистой БД без ошибок, индексы видны в `\di`

### TASK-004: JPA сущности
- Создать `@Entity` классы для всех таблиц из TASK-003
- Использовать `@Enumerated(EnumType.STRING)` для статусов и режимов
- `meanings TEXT[]` маппить через `@Type` на `List<String>` (Hibernate 6 умеет нативно)
- Repository-интерфейсы на Spring Data JPA
- **DoD:** интеграционный тест на CRUD каждой сущности проходит

### TASK-005: Аутентификация — регистрация и логин
- `POST /api/auth/register` — { email, password } → создать юзера, BCrypt(cost=12)
- `POST /api/auth/login` — вернуть `{ accessToken, refreshToken }`
- `POST /api/auth/refresh` — обмен refresh на новую пару
- JWT с RS256, ключи из env (генерируются один раз)
- Refresh-токен хранится в БД как SHA-256 хеш
- Spring Security фильтр для access-токена
- Rate limiting через Bucket4j: 5 попыток / 15 мин на IP для `/auth/*`
- Валидация: email формат, пароль минимум 8 символов
- **DoD:** интеграционные тесты на happy path и основные ошибки (400, 401, 429)

### TASK-006: Импорт стартовой базы иероглифов
- CLI-команда (Spring Boot Application Runner с профилем `import`) или отдельный maintenance endpoint под admin
- Источник: CC-CEDICT (скачать дамп, парсить)
- Дополнить HSK-уровнями из открытого списка HSK 2.0
- Импортировать HSK 1 (~150 иероглифов) со статусом `DRAFT`
- Сохранить английские meanings из CC-CEDICT
- **DoD:** в БД лежит ~150 иероглифов уровня HSK 1 с переводами

### TASK-007: Минимальная админка для редактуры (можно через SQL)
- Вариант A (минимум): SQL-скрипты в `docs/editing/` с примерами запросов на правку и публикацию
- Вариант B (если есть время): эндпоинты под `ROLE_ADMIN`:
  - `GET /api/admin/hanzi?status=DRAFT&page=`
  - `PUT /api/admin/hanzi/{id}` — правка пиньиня, переводов
  - `POST /api/admin/hanzi/{id}/publish` — DRAFT/REVIEWED → PUBLISHED
- Промоут одного юзера в `ROLE_ADMIN` через миграцию
- Отредактировать HSK 1 вручную, перевести в `PUBLISHED`
- **DoD:** пользователи видят только `PUBLISHED` иероглифы, HSK 1 полностью отредактирован

---

## Этап 2: FSRS (неделя 2)

### TASK-008: Интерфейс SRS-алгоритма
```java
public interface SrsAlgorithm {
    SrsScheduleResult schedule(SrsCardState state, Rating rating, Instant now);
}
```
- Записи: `SrsCardState`, `SrsScheduleResult`, enum `Rating` (AGAIN=1, HARD=2, GOOD=3, EASY=4)
- enum `CardState` (NEW, LEARNING, REVIEW, RELEARNING)
- **DoD:** интерфейс и DTO готовы, есть Javadoc

### TASK-009: Реализация FSRS-5 на Java
- Класс `FsrsAlgorithm implements SrsAlgorithm`
- Реализация по [официальной спецификации FSRS-5](https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm)
- Дефолтные веса w0..w18 из спецификации
- `requestRetention` берётся из настроек пользователя (по умолчанию 0.9)
- Формулы:
  - Initial stability/difficulty для NEW
  - Update stability/difficulty при REVIEW (success / failure)
  - `nextInterval = stability * (factor based on requestRetention)`
- **DoD:** unit-тесты по эталонным test vectors из `ts-fsrs` проходят с точностью до 4 знаков

### TASK-010: Test vectors для FSRS
- Скопировать или сгенерировать эталонные кейсы из `ts-fsrs` (https://github.com/open-spaced-repetition/ts-fsrs)
- Минимум 20 сценариев: новая карточка с разными rating, последовательности повторений, lapse-сценарии
- Параметризованные JUnit 5 тесты
- **DoD:** все 20+ кейсов проходят

### TASK-011: SrsService — обвязка над алгоритмом
- `SrsService.review(userCardId, rating, metadata)`:
  1. Загрузить `UserCard`
  2. Конвертировать в `SrsCardState`
  3. Вызвать `algorithm.schedule(...)`
  4. Сохранить новое состояние в `UserCard`
  5. Записать `ReviewLog` со всеми метаданными (rating, state_before, stability_before, response_time_ms, hint_count, stroke_mistakes)
- Транзакционность: всё в одной транзакции
- **DoD:** интеграционный тест: ревью меняет состояние карточки и пишет лог

---

## Этап 3: Учебные эндпоинты (неделя 3)

### TASK-012: Сервис формирования сессии
- `StudySessionService.getTodayQueue(userId)`:
  1. Найти все `UserCard` с `due_date <= now()` для подписанных колод пользователя
  2. Добавить новые карточки (NEW) до дневного лимита из настроек
  3. Перемешать с правилом: не показывать оба режима (RECOGNITION/PRODUCTION) одного иероглифа подряд
- Учитывать максимум повторений в день (`maxReviewsPerDay`)
- **DoD:** unit-тест проверяет правильность очереди для разных входных данных

### TASK-013: Endpoint GET /api/study/session
- Возвращает массив карточек: `{ userCardId, hanziId, character, pinyin, mode, meanings (только если RECOGNITION с показанным ответом — нет, не возвращаем), strokeData (для PRODUCTION) }`
- Для PRODUCTION: возвращать meanings (для показа на экране) и НЕ возвращать сам иероглиф (его пользователь должен нарисовать)
- Для RECOGNITION: возвращать иероглиф и пиньинь, НЕ возвращать meanings
- Защита: только для авторизованных
- **DoD:** интеграционный тест возвращает корректную структуру

### TASK-014: Grading-сервис — проверка ответов в Recognition
- `GradingService.gradeRecognition(hanziId, userPinyin, userMeaning, withTones, allowTypos)`:
  - Нормализация пиньиня:
    - Принимать оба формата: `nǐ` и `ni3`
    - Если `withTones=false` — игнорировать тоны
    - Использовать pinyin-pro для конвертации
  - Нормализация значения:
    - lowercase, trim
    - удалить артикли в начале: `to `, `a `, `an `, `the `
    - удалить пунктуацию по краям
  - Сравнение значения с массивом meanings:
    - Точное совпадение → правильно
    - Левенштейн ≤ 1 → "near match" (вернуть флаг для подтверждения у пользователя)
- Возвращает `GradingResult { correct, nearMatch, suggestedRating }`
- **DoD:** unit-тесты на 30+ кейсов: точные, опечатки, разные форматы тонов, пустой ввод

### TASK-015: Endpoint POST /api/study/review
- Тело: `{ userCardId, mode, rating, responseTimeMs, hintCount?, strokeMistakes? }`
- Валидация: карточка принадлежит пользователю, mode совпадает
- Вызов `SrsService.review(...)`
- Возвращает обновлённое состояние и `nextDue`
- **DoD:** интеграционный тест happy path + 401, 403, 404

### TASK-016: Endpoint GET /api/stats/dashboard
- Возвращает: `{ dueTodayCount, newAvailableCount, learnedTotal, currentStreak, accuracy7d }`
- `currentStreak` — количество дней подряд с хотя бы одним ревью
- `accuracy7d` — % ответов с rating >= 3 за 7 дней
- **DoD:** интеграционный тест с подготовленными данными

---

## Этап 4: Frontend каркас (неделя 4)

### TASK-017: Скелет React-приложения
- Vite + React 18 + TypeScript
- React Router (роуты: /login, /register, /, /study, /decks, /hanzi/:id, /search, /stats, /settings)
- TanStack Query настроен, базовый axios/fetch клиент с инжектом токена
- Zustand store для текущего юзера и токенов
- Tailwind CSS + shadcn/ui (init, базовые компоненты: Button, Input, Card, Dialog)
- Логика обновления access-токена через refresh при 401
- **DoD:** приложение запускается, роутинг работает, заглушки страниц

### TASK-018: Страницы /login и /register
- Формы с валидацией (react-hook-form + zod)
- После успеха — сохранение токенов в Zustand + localStorage, редирект на /
- Обработка ошибок (показ toast)
- **DoD:** регистрация и вход работают end-to-end

### TASK-019: Дашборд /
- Карточки: Due today, New available, Learned total, Streak, Accuracy 7d
- Кнопка "Start studying" — ведёт на /study
- Skeleton loader пока грузится
- **DoD:** дашборд отображает реальные данные с бэкенда

### TASK-020: Экран сессии — Recognition
- Загрузить очередь через `GET /api/study/session`
- Показывать одну карточку:
  - Большой иероглиф по центру
  - Поля ввода: pinyin, meaning
  - Кнопка "Check" (или Enter)
- После проверки:
  - Показать правильный ответ, разницу с введённым
  - 4 кнопки оценки: Again / Hard / Good / Easy с горячими клавишами 1-4
  - Кнопка по умолчанию подсвечена в зависимости от результата grading
- При near-match — диалог "Did you mean X?" перед проставлением rating
- Прогресс-бар сверху (X из Y карточек)
- При завершении сессии — экран статистики
- **DoD:** можно пройти полную сессию из 5+ карточек в режиме Recognition

---

## Этап 5: Рисование (неделя 5)

### TASK-021: Проверка покрытия hanzi-writer-data
- Скрипт, который проверяет, что для всех иероглифов в БД (HSK 1-6) есть данные в `hanzi-writer-data`
- Список отсутствующих — добавить флаг `has_stroke_data BOOLEAN` в таблицу `hanzi`
- **DoD:** отчёт по покрытию, флаг проставлен

### TASK-022: Компонент HanziDrawingPad
- Props: `character`, `helpLevel ('STRICT'|'NORMAL'|'EASY')`, `onComplete(result)`, `onSkip()`
- Использует `hanzi-writer` npm-пакет
- `result`: `{ rating: Rating, hintCount: number, strokeMistakes: number, durationMs: number }`
- Маппинг ошибок и подсказок на rating:
  - Все черты с первого раза, 0 подсказок → `EASY`
  - Все черты, ≤2 подсказки или ≤2 ошибки → `GOOD`
  - Доделал, но было больше → `HARD`
  - Skip / явный выход → `AGAIN`
- Адаптивный canvas: minSize 280px, maxSize 400px
- На мобильных: `touch-action: none` на canvas
- Кнопки: Skip, Show stroke order (показать анимацию правильного написания)
- **DoD:** компонент работает в Storybook или на тестовой странице, корректно валидирует черты

### TASK-023: Production-режим в сессии — рисование
- Для PRODUCTION-карточки:
  - Вверху — английское слово (meaning)
  - По центру — `<HanziDrawingPad>` с целевым иероглифом
- После завершения рисования или skip — автоматически проставляется rating, переход на следующую карточку
- В настройках — переключатель helpLevel (Strict / Normal / Easy)
- **DoD:** можно пройти PRODUCTION-карточки в сессии через рисование

### TASK-024: Fallback Production через выбор вариантов
- В настройках пользователя — флаг `productionMode: 'DRAWING' | 'CHOICE'`
- Если CHOICE — показывать 6 кнопок (1 правильный + 5 дистракторов)
- Дистракторы: иероглифы из той же колоды/HSK-уровня, исключая правильный
- Backend-эндпоинт `GET /api/study/distractors?hanziId=&count=5`
- **DoD:** работают оба режима, переключаются в настройках

### TASK-025: Тестирование на реальных мобильных устройствах
- Проверить рисование на iPhone, Android (Chrome, Safari)
- Скролл не мешает рисованию
- Распознавание черт работает корректно с пальца
- Размер canvas комфортный
- Документировать найденные проблемы в issues
- **DoD:** работает на iOS Safari и Android Chrome

---

## Этап 6: Полировка и Production-режим (неделя 6)

### TASK-026: Поиск иероглифов
- Backend: `GET /api/hanzi/search?q=&hsk=&page=&size=`
- Поиск по character, pinyin, meanings (через pg_trgm для нечёткого)
- Сортировка по релевантности и frequency_rank
- Frontend: страница /search с дебаунсом ввода, фильтрами по HSK, пагинацией
- **DoD:** поиск находит "ni" → 你, "hello" → 你好

### TASK-027: Страница иероглифа /hanzi/:id
- Большой иероглиф с анимацией порядка черт (HanziWriter в режиме просмотра)
- Pinyin, meanings, stroke count, HSK level
- Примеры из `hanzi_example`
- Кнопка "Add to study" если ещё не в очереди пользователя
- Состояние карточек пользователя по этому иероглифу (если изучается)
- **DoD:** страница полностью отображает информацию

### TASK-028: Колоды
- `GET /api/decks` — список системных колод
- `POST /api/decks/{id}/subscribe` — подписаться (создаёт записи `user_deck` и `UserCard` со статусом NEW для всех иероглифов колоды)
- Frontend: страница /decks со списком, кнопкой Subscribe
- **DoD:** пользователь может подписаться на колоду, иероглифы появляются в очереди

### TASK-029: Страница /settings
- Дневной лимит новых карточек
- Дневной лимит повторений
- requestRetention (FSRS) — слайдер 0.8–0.97 или пресеты "Casual / Normal / Intensive"
- productionMode (DRAWING / CHOICE)
- drawingHelpLevel (STRICT / NORMAL / EASY)
- withTones для проверки пиньиня (true/false)
- **DoD:** настройки сохраняются и применяются

### TASK-030: Базовая статистика /stats
- График ревью по дням за 30 дней (recharts или chart.js)
- Распределение карточек по состояниям (NEW/LEARNING/REVIEW)
- Точность по дням
- **DoD:** графики отображают реальные данные

### TASK-031: Расширение базы до HSK 1-3
- Импорт HSK 2 и HSK 3 в `DRAFT`
- Редактура → `PUBLISHED`
- Ориентир: ~600 иероглифов всего
- **DoD:** в проде доступны HSK 1-3 как PUBLISHED

---

## Этап 7: Деплой (неделя 7)

### TASK-032: Production-сборка
- Multi-stage Dockerfile для backend (jlink для уменьшения образа)
- Production build для frontend, отдача через nginx
- `docker-compose.prod.yml` с правильными env-переменными
- **DoD:** оба образа собираются, prod compose поднимается

### TASK-033: Деплой на VPS
- Caddy как reverse proxy с автоматическим Let's Encrypt
- Backup PostgreSQL: cron + `pg_dump` + загрузка в S3-совместимое хранилище
- Метрики через Spring Boot Actuator + Prometheus endpoint (даже если не подключаем UI — пусть будет)
- **DoD:** приложение доступно по HTTPS на собственном домене

### TASK-034: CI/CD
- GitHub Actions:
  - На PR: lint + tests (backend + frontend)
  - На push в main: сборка образов, push в registry, деплой на VPS через ssh
- **DoD:** push в main автоматически деплоит

### TASK-035: Базовый мониторинг
- Логи backend в JSON (Logback)
- Healthcheck в Caddy/мониторинг через Uptime Kuma или аналог
- **DoD:** падение backend замечается в течение 5 минут

---

## Что вне MVP

Эти задачи НЕ делаем сейчас. Если хочется — открыть как issue для будущих этапов.

- Дообучение FSRS-весов под конкретного пользователя
- Аудио иероглифов и режим аудирования
- Пользовательские колоды и импорт CSV
- PWA / оффлайн-режим
- Русский язык переводов (схема готова, но контент не делаем)
- Социальные фичи: общие колоды, ачивки, лидерборды
- Email-уведомления (включая сброс пароля — в MVP без него, либо с минимальным провайдером типа Resend если просто)
- Мобильное приложение (React Native)
- OAuth (Google/GitHub)
- Многопользовательский режим админки

---

## Соглашения по коду

### Backend
- Java 21, records для DTO, sealed classes где уместно
- Lombok для entity, не для DTO
- Все DTO — record
- Имена пакетов в нижнем регистре, существительные в единственном числе
- Не использовать `@Autowired` на полях — только конструктор-инжекция
- Транзакции — на сервисном слое, `@Transactional` явно
- SQL миграции версионируются: `V001__init.sql`, `V002__add_index.sql`
- Никогда не редактировать применённую миграцию — только новая

### Frontend
- TypeScript strict mode
- Никаких `any` без явного комментария почему
- Компоненты — функциональные, hooks
- Каждый API-запрос через TanStack Query (никаких useEffect + fetch)
- Стили через Tailwind, кастомный CSS — только если Tailwind не справляется
- Компоненты разбивать когда > 200 строк
- Типы API-ответов генерировать из OpenAPI (если успеем) или вручную поддерживать в `src/api/types.ts`

### Тесты
- Backend: JUnit 5 + AssertJ + Testcontainers для интеграционных
- Frontend: Vitest + Testing Library для компонентов, Playwright для E2E (опционально в MVP)
- Покрытие критичной логики (FSRS, grading, выдача очереди) ≥ 90%

### Git
- Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`
- Ветки: `feat/TASK-001-skeleton`, `fix/TASK-014-pinyin-tones`
- Pull request обязателен, squash-merge в main
- В описании PR — список затронутых TASK-ID

---

## Принципы для AI-агентов

При работе над задачами из этого файла:

1. **Один TASK — один PR.** Не смешивать несвязанные задачи.
2. **Перед началом — прочитать DoD.** Реализация считается готовой только когда DoD выполнен.
3. **Не выходить за рамки задачи.** Если по ходу работы нашёлся баг или улучшение в чужом коде — открыть отдельный issue, не править в этом PR.
4. **Тесты — часть задачи, не отдельная.** PR без тестов на новую логику не мержится.
5. **Миграции БД — всегда новые файлы.** Никогда не редактировать применённые.
6. **Если задача неясна — задать вопрос, не додумывать.** Особенно по бизнес-логике (FSRS, grading).
7. **Документировать неочевидные решения.** Комментарий в коде или запись в `docs/decisions/`.
8. **Перед стартом проверить TASK-зависимости.** Не браться за TASK-020, если TASK-013 не готов.

---

## Правила качества кода (обязательны для LLM-агентов)

Эти правила — жёсткие. Любая генерация кода, не соответствующая им, считается дефектом и должна быть переписана до коммита. Они применяются и к backend (Java), и к frontend (TS/React) с поправкой на язык.

### 1. Production-style
- Никаких учебных упрощений, заглушек или TODO-вместо-реализации, если они ухудшают архитектуру.
- Код пишется так, как если бы он шёл сразу в прод.

### 2. Читаемость
- Метод — **не больше 25 строк** тела (без сигнатуры и закрывающей скобки). Если не помещается — выделять private-методы.
- Длина строки — **не больше 120 символов**.
- Методы располагаются **в порядке их вызова сверху вниз** (caller выше callee).
- Запрещена глубоко вложенная логика (>3 уровней) — выносить в early-return или отдельные методы.
- Сложные условия — в именованные private-методы (`isExpired()`, `hasReachedDailyLimit()`).
- Имена переменных, методов и классов — самодокументирующиеся. Однобуквенные имена — только в общепринятых коротких циклах (`for (int i = 0; ...)`) и стандартных лямбдах (`.map(e -> ...)` с очевидным контекстом).

### 3. Clean code
- **Минимум дублирования.** Дважды повторённая логика выносится в helper.
- **Явные DTO** — никаких `Map<String, Object>` через границу API.
- **Enum вместо магических строк** — статусы, режимы, роли.
- **`final` для неизменяемых зависимостей** (поля сервисов, конфигов).
- **Конструкторная инъекция** — никаких `@Autowired` на полях.
- **Логирование через slf4j** — `LoggerFactory.getLogger(...)`, без `System.out`.

### 4. Комментарии и JavaDoc
- JavaDoc/комментарии только там, где помогают понять контракт, поток данных или нетривиальную оптимизацию.
- Не дублировать в комментарии то, что и так видно из имени метода.

### 5. Обработка ошибок
- Явные классы исключений (`InvalidCredentialsException`, `EmailAlreadyUsedException`), а не голые `RuntimeException` без типа.
- Централизованная обработка через `@RestControllerAdvice` с маппингом на HTTP-коды.
- Никогда не глотать `Exception` без логирования и осознанного решения.

### 6. Конфигурация
- `@ConfigurationProperties`-records, а не россыпь `@Value`.
- Все ttl/тайминги — `Duration`, не `long`.
- Чувствительные параметры — только из env, с разумным default только для dev.

### 7. Готовность к нагрузке
- Минимизировать лишние аллокации в hot-path (валидация, выдача очереди, grading).
- Не копировать большие массивы/коллекции без необходимости (`List.copyOf` только если нужна неизменяемость).
- Не делать блокирующие операции (sync I/O, heavy CPU) в фильтрах/контроллерах без явной причины.
- Reflection в hot-path — запрещено без замера.
- Многопоточность: shared state — `ConcurrentHashMap`/`AtomicX` или иммутабельность. Любой `static` mutable field — повод для review.

### 8. Тесты — обязательны
Для каждого реализованного кейса должны быть тесты, моделирующие **реальные пользовательские обращения**, а не только happy path. PR без негативных кейсов и edge-case'ов не мержится.

Виды тестов и когда их писать:
- **Unit-тесты** — изолированная логика (FSRS-алгоритм, нормализация пиньиня, grading).
- **Integration-тесты** — REST API через `MockMvc` или `TestRestTemplate` против реальной БД (Testcontainers).
- **Concurrency-тесты** — для hot-path и shared state (rate limiter, кеш сессий).
- **Тесты на timeout/retry/failover** — если поведение является частью контракта.

Покрывать обязательно:
- happy path;
- 400/401/403/404/409/422/429 для каждого endpoint, где они достижимы;
- граничные значения (пустой ввод, максимальная длина, off-by-one);
- состояние гонки в shared structures.

### 9. Что входит в "генерацию кода"
При выполнении любой задачи агент обязан выдать **одним PR**:
- production-код;
- тесты, покрывающие новые кейсы согласно п.8;
- при необходимости — test fixtures, builders, mock-серверы;
- обновлённую документацию (если меняется контракт / env-переменные / схема).

### 10. Self-check перед коммитом
Перед коммитом агент обязан мысленно пройтись по всем 9 пунктам выше и переписать всё, что не соответствует. Этот self-check важнее скорости.

---

## Контакты и материалы

- FSRS-5 спецификация: https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm
- ts-fsrs (референс реализации): https://github.com/open-spaced-repetition/ts-fsrs
- HanziWriter: https://hanziwriter.org/
- CC-CEDICT: https://www.mdbg.net/chinese/dictionary?page=cedict
- Make Me a Hanzi: https://github.com/skishore/makemeahanzi
