# AGENTS.md — «Абитуриент MAX»

Инструкции для ИИ-агентов, работающих в этом репозитории. Перед изменением кода
прочитай этот файл целиком: здесь описаны границы проекта, стек и **жёсткие
соглашения**, которые нельзя нарушать без явной просьбы.

---

## 1. Что это за проект

Бот + мини-приложение в мессенджере **MAX** для иностранных абитуриентов,
поступающих в российские вузы. Помогает пройти поступление без ошибок:
определяет трек, собирает чек-лист документов (перевод/апостиль/легализация),
считает шансы на бюджет и строит стратегию приоритетов.

- Трек хакатона: «Забота о людях» (адаптация на новом месте).
- Бот в MAX: `@t722_hakaton_max_bot`.
- Аудитория: сегмент A — граждане ЕАЭС (Беларусь, Казахстан, Киргизия, Армения),
  ядро MVP; сегмент B — иностранцы из других стран (Китай, Турция, Африка),
  подача через **ruID** с 2026 года.
- Языки интерфейса: **ru, kk, ky**. Другие не добавлять без запроса.

---

## 2. Стек

| Слой | Технологии |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security, Spring Data Redis, Flyway, springdoc-openapi 3.1.0, Lombok |
| БД | PostgreSQL 15 (prod) / `postgres:latest` (Testcontainers) |
| Кэш/сессии | Redis 7 |
| Frontend | React 19, TypeScript ~6.0, Vite 8, Redux Toolkit 2, react-router-dom 7, i18next + react-i18next, `@maxhub/max-ui` 0.5 |
| Инфра | Docker, Docker Compose, Nginx 1.25 + сертификаты Let's Encrypt, GitHub Actions |
| Тесты | JUnit 5 + Testcontainers (backend), `node --test` (frontend) |

Версии зафиксированы в `bot_max_backend/pom.xml` и
`bot_max_frontend/package.json`. Не обновляй мажорные версии и не добавляй
зависимости без необходимости — это хакатон, а не долгоживущий продукт.

---

## 3. Структура репозитория

```
bot_max_hack/
├── .github/workflows/
│   ├── deploy-backend.yml        # сборка jar → docker → push → ssh deploy
│   └── deploy-frontend.yml       # npm ci → build → docker → push → ssh deploy
├── bot_max_backend/              # Spring Boot
│   ├── src/main/java/com/runiversityadmisson/bot/
│   ├── src/main/resources/application.yaml
│   ├── src/test/java/com/runiversityadmisson/bot/
│   ├── Dockerfile                # multi-stage: maven:3.9-temurin-21 → temurin:21-jre-alpine
│   ├── compose.yaml              # локальные postgres + redis
│   ├── mvnw / mvnw.cmd           # Maven wrapper
│   ├── pom.xml
│   └── max-abiturient-api-0.0.2 (1).yaml   # OpenAPI 3.0.3 — контракт API
├── bot_max_frontend/             # React + MAX UI (мини-апп)
│   ├── src/
│   ├── tests/regressions.test.cjs
│   ├── Dockerfile, nginx.conf, compose.yaml
│   ├── vite.config.ts, tsconfig.*.json, eslint.config.js
│   └── package.json
├── nginx/conf.d/default.conf     # reverse-proxy: /api, /webhook → backend, / → frontend
├── docker-compose.yml            # прод-стек: backend, frontend, postgres, redis, nginx

└── AGENTS.md
```

---

## 4. Backend (`bot_max_backend`)

### Текущее состояние — это каркас

Вся реализация отсутствует. Есть только:

- `com.runiversityadmisson.bot.BotApplication` — точка входа.
- `src/main/resources/application.yaml` — **3 строки** (`spring.application.name: bot`).
- Тесты: `BotApplicationTests` (контекст поднимается), `TestBotApplication`,
  `TestcontainersConfiguration` (PostgreSQL + Redis контейнеры).

### Обязательные соглашения при создании кода

- **Base package:** `com.runiversityadmisson.bot`. Новые пакеты — только под ним.
- **Структура слоёв** (создавай по мере необходимости, не заводи пустые пакеты):

```
com.runiversityadmisson.bot
├── bot/           # контроллер вебхуков MAX, отправка сообщений, long polling
├── auth/          # JWT, обмен initData → токен, Spring Security config
├── session/       # сущность/репозиторий/сервис сессии абитуриента
├── domain/        # JPA-сущности: University, Direction, EgeScore, Achievement, Privilege, Deadline
├── repository/    # Spring Data JPA
├── service/       # бизнес-логика: расчёт шансов, стратегия, дедлайны
├── web/           # REST-контроллеры мини-аппы (/v1/**), DTO, маппинг
├── config/        # Security, Redis, OpenAPI, WebClient
└── scheduler/     # @Scheduled — напоминания о дедлайнах
```

- **DTO наружу** — отдельные record-классы в `web/dto`. JPA-сущности наружу
  не отдавать никогда.
- **Миграции — только Flyway**, файлы `src/main/resources/db/migration/V<N>__<описание>.sql`.
  Никаких `ddl-auto` в проде, никаких ручных правок схемы.
- **Java-стиль:** табы (как в сгенерированном `BotApplication`), LF-переводы строк,
  без `var` в полях, конструкторная инъекция, Lombok `@Getter/@Setter/@Builder`
  допустимы.
- **Логирование:** SLF4J через Lombok `@Slf4j`. Никогда не логировать
  `initData`, JWT, `MAX_BOT_TOKEN`, пароли, персональные данные абитуриента.

### Переменные окружения (уже объявлены в `docker-compose.yml`)

| Переменная | Назначение |
|---|---|
| `POSTGRES_HOST` / `POSTGRES_PORT` / `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | datasource |
| `REDIS_HOST` / `REDIS_PORT` | сессии, кэш, TTL-состояние |
| `MAX_BOT_TOKEN` | токен бота MAX |
| `MAX_WEBHOOK_SECRET` | проверка подписи вебхуков |
| `JWT_SECRET`, `JWT_TTL` | подпись и TTL токенов мини-аппы |

**Важно:** `application.yaml` их пока не читает. При добавлении конфигурации
используй Spring placeholders `${POSTGRES_HOST:localhost}` и т. п., чтобы
значения дефолтились для локальной разработки.

### Команды

```bash
cd bot_max_backend
./mvnw spring-boot:run          # запуск (поднимет postgres+redis из compose.yaml)
./mvnw test                     # тесты (нужен Docker для Testcontainers)
./mvnw clean package            # сборка jar → target/
```

На Windows используй `mvnw.cmd`. Локальные БД: `docker compose up -d` в
`bot_max_backend` (порты 5432 / 6379).

---

## 5. Frontend (`bot_max_frontend`)

Мини-апп React, работает внутри MAX. Собран вокруг **слоёв**, каждый из которых
уже отделён. Не смешивай их.

```
src/
├── main.tsx                  # entry: StrictMode → AppProviders → App
├── app/                      # каркас приложения
│   ├── App.tsx               # гейт: загрузка / ошибка инициализации / рендер роутера
│   ├── AppRouter.tsx → router.tsx   # все маршруты в одном месте
│   ├── providers.tsx         # Redux Provider + MaxUI + data-theme
│   └── initializeSession.ts  # обмен launch params → JWT → session
├── api/                      # слой доступа к данным
│   ├── client.ts             # fetch-обёртка: base URL, Bearer, разбор ошибок
│   ├── authApi.ts, sessionApi.ts, onboardingApi.ts, universitiesApi.ts,
│   │   prioritiesApi.ts, deadlinesApi.ts, strategyApi.ts, profileApi.ts
│   └── types/                # доменные типы, по файлу на домен
├── lib/max/                  # мост к SDK MAX
│   ├── types.ts              # интерфейс MaxBridge
│   ├── bridge.ts             # прод-реализация поверх window.WebApp
│   └── mockBridge.ts         # заглушка для локальной разработки
├── store/                    # Redux Toolkit
│   ├── store.ts, hooks.ts    # типизированные useAppSelector / useAppDispatch
│   └── slices/sessionSlice.ts
├── hooks/                    # useAppInit, useProfile, useUniversities, ...
├── components/               # переиспользуемые: AppLayout, PageHeader, UniversityCard,
│                             # ChanceBadge, ChanceCircle, EmptyState, ErrorState,
│                             # LoadingState, BottomNavigation, OnboardingLayout
├── pages/                    # 10 экранов, по папке на экран
├── mocks/                    # mock-данные + mockApi.ts (полная имитация бэкенда)
├── i18n/                     # i18n.ts + locales/{ru,kk,ky}.ts
├── styles/                   # reset.css, variables.css, globals.css
├── utils/                    # чистые функции (egeScore, formatDate, storage, ...)
└── assets/
```

### Жёсткие соглашения

**1. Именованные экспорты, никаких default.** ESLint-плагин `react-refresh`
это требует:

```tsx
export function InterestsPage() { ... }        // ✅
export default InterestsPage;                 // ❌
```

**2. Алиас `@/`** — настроен в `vite.config.ts` и `tsconfig.app.json`.
Импорты вида `@/pages/UniversitiesPage/UniversitiesPage`.

**3. Структура папки компонента/страницы** — ровно как сейчас:

```
InterestsPage/
├── InterestsPage.tsx
└── InterestsPage.module.css
```

Имя папки = имя компонента = имя файла. CSS — **CSS Modules**,
доступ через `styles.x`. Глобальные стили — только в `src/styles/`.

**4. UI-кит:** компоненты только из `@maxhub/max-ui`
(`Typography`, `Button`, `Container`, `Panel`, ...). Свои компоненты —
в `src/components/`, с явными пропсами.

**5. Никаких хардкод-строк в UI.** Весь текст через `useTranslation()`:

```tsx
const { t } = useTranslation();
t("onboarding.interests.title");
t("common.step", { step: 1, total: 3 });   // интерполяция — только {{doubleBraces}}
```

Ключи — `секция.экран.поле`, лежат в `src/i18n/locales/ru.ts`.
**Добавляешь ключ — синхронно добавляй в `kk.ts` и `ky.ts`** (это обязательно,
иначе UI откатится на `ru` через `fallbackLng`).

**6. Доступ к данным — только через `src/api/`.** Компоненты и хуки не вызывают
`fetch` напрямую.



### Переменные окружения

`bot_max_frontend/.env.example`:

| Переменная | Значения | Смысл |
|---|---|---|
| `VITE_API_MODE` | `mock` \| `real` | источник данных: моки или бэкенд |
| `VITE_MAX_MODE` | `mock` \| `real` | SDK MAX или заглушка |
| `VITE_API_URL` | URL | base URL бэкенда, включая `/v1` |

**Прод-сборка обязана** передавать `VITE_API_MODE=real` и `VITE_MAX_MODE=real`
через `ARG` в `Dockerfile` — иначе на сервере будет мок.

### Команды

```bash
cd bot_max_frontend
npm install
npm run dev       # vite на :5173, host=true, open=true
npm run build     # tsc -b && vite build  → dist/
npm run lint      # eslint
npm run preview
npm test          # node --test tests/*.test.cjs
```

---

## 6. Контракт API

Единственный источник правды: `bot_max_backend/max-abiturient-api-0.0.2 (1).yaml`
(OpenAPI 3.0.3, base path `/v1`).

- **Меняешь контракт → сначала правь YAML, потом код бэкенда, потом типы и
  api-слой фронта.** Не расходись со спецификацией.
- Аутентификация — `bearerAuth` (JWT, выдаётся при обмене `initData`).
- Теги: `Auth`, `Localization`, `Citizenship`, `Subjects`, `Interests`,
  `Achievements`, `Privileges`, `Universities`, `Priorities`, `Profile`,
  `Deadlines`, `Strategy`.

Ключевые пути:

| Метод | Путь | Назначение |
|---|---|---|
| POST | `/auth/bot-session` | бот создаёт/возобновляет сессию |
| POST | `/auth/exchange` | обмен `initData` MAX → JWT |
| GET | `/sessions/{sessionId}` | черновик сессии |
| GET | `/languages` | справочник языков |
| PUT | `/sessions/{sessionId}/language` | смена языка |
| GET | `/citizenship-groups` | справочник групп гражданства |
| PUT | `/sessions/{sessionId}/citizenship` | сохранить гражданство |
| GET | `/subjects` | предметы ЕГЭ |
| PUT | `/sessions/{sessionId}/ege-scores` | сохранить баллы ЕГЭ |
| GET | `/interest-categories` | категории интересов |
| PUT | `/sessions/{sessionId}/interests` | сохранить интересы |
| GET | `/achievements` | индивидуальные достижения |
| PUT | `/sessions/{sessionId}/achievements` | сохранить достижения |
| GET | `/privilege-categories` | льготы/квоты |
| PUT | `/sessions/{sessionId}/privilege` | применить льготу |
| GET | `/universities/search` | поиск вузов |
| GET | `/universities/{universityId}` | карточка вуза |
| GET/POST | `/sessions/{sessionId}/universities` | список выбранных вузов |
| PUT | `/sessions/{sessionId}/universities/{universityId}/directions/priorities` | порядок направлений |
| GET/PUT | `/sessions/{sessionId}/priorities` | стратегия приоритетов |
| GET | `/sessions/{sessionId}/profile` | сводка профиля |
| GET | `/sessions/{sessionId}/deadlines` | календарь дедлайнов |
| PUT | `/sessions/{sessionId}/deadlines/{eventId}/reminders` | напоминания |
| POST | `/sessions/{sessionId}/strategy` | финализация стратегии |
| GET | `/sessions/{sessionId}/strategy/report` | отчёт (продублирован в боте) |

Ключевые DTO: `Session`, `SessionDraft`, `EgeScoreResult`, `InterestCategory`,
`Achievement`, `PrivilegeCategory`, `PrivilegeApplyResult`, `UniversityCard`,
`UniversitySummary`, `UniversityDetail`, `RiskStatus`, `RiskFactors`,
`PriorityListItem`, `ProfileSummary`, `DeadlineEvent`, `StrategyFinalizeResponse`.
Ошибки: `BadRequest`, `Unauthorized`, `NotFound`, `Error`.

---

## 7. Тесты

**Backend** — `bot_max_backend/src/test/java/...`:
JUnit 5, `@SpringBootTest`, Testcontainers. `TestcontainersConfiguration` поднимает
PostgreSQL и Redis и прокидывает их через `@ServiceConnection`. Требуется
запущенный Docker. Интеграционные тесты с БД — с `@Import(TestcontainersConfiguration.class)`.

**Frontend** — `bot_max_frontend/tests/regressions.test.cjs`:
кастомный загрузчик, который транспилирует реальные TS-модули через `typescript`
и исполняет их в `node:vm` с подменённым `import.meta.env`
(`VITE_API_MODE=mock`, `VITE_MAX_MODE=mock`) и `localStorage`.
Vitest/Jest в проекте нет. Тесты запускаются на реальных исходниках из `src/`
по алиасу `@/` — **придерживайся этой схемы**, не добавляй тест-раннер.

Правило: нет теста — нет фичи. Для расчётов (шансы, приоритеты, дедлайны)
тесты обязательны, потому что это ядро ценности продукта.

---

## 8. CI/CD и деплой

- `deploy-backend.yml` — триггер на push в `main` с изменениями в
  `bot_max_backend/**`: `mvn clean package -DskipTests` → docker build/push
  (тег `:latest`) → `docker compose pull backend && docker compose up -d backend`
  по SSH.
- `deploy-frontend.yml` — то же для `bot_max_frontend/**`: `npm ci` → `npm run build`
  → docker build/push → `docker compose pull frontend && docker compose up -d frontend`.
- Nginx (`nginx/conf.d/default.conf`) терминирует TLS и проксирует:
  `/api/` → `backend:8080`, `/webhook/` → `backend:8080`, `/` → `frontend:80`.
- Внутренние контейнеры связаны через сеть `app-network`; наружу открыты только
  80/443 у nginx.

Секреты (`DOCKER_USERNAME`, `DOCKER_PASSWORD`, `SSH_HOST`, `SSH_USER`, `SSH_KEY`)
берутся из GitHub Secrets. **Никогда не коммить `.env`** — в `.gitignore` он уже есть.

---

## 9. Известные заглушки — не считай их готовой функциональностью

Перед тем как писать код «поверх» этого, знай о следующем:

| Место | Состояние |
|---|---|
| `bot_max_backend/src/main/java/...` | только `BotApplication`, ни одного эндпоинта |
| `bot_max_backend/src/main/resources/application.yaml` | 3 строки, datasource/redis не сконфигурированы |
| `bot_max_backend/src/main/resources/db/migration/` | отсутствует, Flyway-миграций нет |
| `bot_max_frontend/src/store/slices/uiSlice.ts` | пустой файл, редьюсер в `store.ts` не подключён |
| `nginx/conf.d/default.conf` | плейсхолдер `твой-домен.ru` |
| `docker-compose.yml` | путь `/etc/letsencrypt/live/твой-домен.ru` — заменить на реальный домен |
| оба workflow | `cd /path/to/your/project` — заменить на реальный путь на сервере |
| `bot_max_backend/Dockerfile` | `mvn clean package -DskipTests` — тесты в CI не гоняются |
| корневой `.env.example` | отсутствует, хотя `docker-compose.yml` его требует |
| `Idex.txt` | кодировка cp1251, а не UTF-8 |

---

## 10. Правила работы для агента


2. **Не расширяй scope.** Задача — хакатон-MVP. Не добавляй RabbitMQ, Kubernetes,
   отдельный сервис расчётов, ORM-фичи «на будущее». Требуется — спроси.
3. **Следуй существующим паттернам, а не собственным вкусам.** Паттерн
   `isMock`-ветвления, структура папок, именованные экспорты, CSS Modules,
   i18n — это контракт проекта, а не стилистическое предпочтение.
4. **Одна фича — полный срез.** Новый эндпоинт = YAML + backend + типы +
   api-слой + мок + UI. Оставить фронт в mock-режиме без мока — значит сдать
   неработающую фичу.
5. **Не хардкодь домены, токены, секреты, SQL в Java.** Только env-переменные
   и Flyway.
6. **Не коммитай, не пушь, не редактируй CI и деплой**, пока явно не попросят.
7. **Перед сдачей прогони:** `npm run lint && npm run build && npm test`
   во фронте, `./mvnw test` в бэкенде.
8. **Спрашивай, а не додумывай**, если неясно: язык интерфейса, трек поступления,
   формула расчёта шансов, источник данных по вузам. Эти вещи меняют продукт.
9. Пиши по-русски: комментарии, коммиты, UI-тексты, документация.
10. Комментарии в коде не добавляй, если не попросили — код должен читаться сам.
