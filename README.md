# Менеджер баллов

Бот и мини-приложение в MAX для планирования поступления в вуз. Пользователь указывает баллы ЕГЭ, выбирает направления, добавляет олимпиады, индивидуальные достижения и льготы, а затем сравнивает программы и составляет план поступления.

**Домен приложения:** [runiversityadmission.ru](https://runiversityadmission.ru). Для авторизации открывайте мини-приложение через бота MAX.

## Запуск через Docker

После настройки корневого `.env`, домена и HTTPS-сертификатов (см. раздел «Развёртывание на сервере») выполните из корня репозитория:

```sh
docker compose up -d --pull always
```

Команда скачает актуальные готовые образы и запустит весь проект: backend, frontend, PostgreSQL, Redis и nginx. Образы приложения должны быть заранее опубликованы в Docker Hub аккаунте, указанном в `DOCKER_USERNAME`.

## Возможности

- Сбор исходных данных через бота и редактирование баллов ЕГЭ в мини-приложении.
- Выбор направлений и расстановка их приоритетов.
- Рекомендации по программам с учётом профиля абитуриента.
- Сравнение конкурсного балла с прошлогодним проходным и просмотр расчёта.
- План «5 × 5»: до пяти вузов и пяти различных направлений в каждом.
- Ручное составление плана, автоматический подбор и дополнение существующего плана.

Баллы и условия поступления рассчитывает backend. Прошлогодний проходной балл используется для сравнения и не гарантирует поступление. Автоподбор создаёт черновик: итоговый план сохраняется отдельной кнопкой.

## Стек и структура

| Каталог              | Назначение                                                             |
| -------------------- | ---------------------------------------------------------------------- |
| `bot_max_backend/`   | Бот MAX и REST API: Java 21, Spring Boot, Spring Security, JPA, Flyway |
| `bot_max_frontend/`  | Мини-приложение: React, TypeScript, Vite, Redux Toolkit, MAX UI        |
| `nginx/`             | HTTPS и проксирование фронтенда, API и webhook                         |
| `.github/workflows/` | Автоматизация развёртывания frontend и backend                         |
| `docker-compose.yml` | Серверный запуск приложения, PostgreSQL, Redis и nginx                 |

PostgreSQL хранит данные приложения, Redis используется для сессий. Мини-приложение передаёт данные запуска MAX в API и получает JWT для дальнейших запросов.

## Локальная разработка

Нужны Node.js 22 с npm, JDK 21 и Docker с Compose. Maven Wrapper находится в каталоге backend.

### Backend

Из корня проекта запустите PostgreSQL и Redis:

```sh
docker compose -f bot_max_backend/compose.yaml up -d
```

Настройки этой конфигурации совпадают с локальными значениями в `application.yaml`: PostgreSQL на порту `5432`, Redis на `6379`.

Перед запуском backend задайте в окружении `MAX_BOT_TOKEN`, `MAX_WEBHOOK_SECRET` и `JWT_SECRET`. Затем:

```sh
cd bot_max_backend
./mvnw spring-boot:run
```

В PowerShell используйте `./mvnw.cmd spring-boot:run`. Backend по умолчанию слушает порт `8080`, API доступен с префиксом `/v1`. Миграции Flyway применяются при запуске.

### Frontend

В отдельном терминале:

```sh
cd bot_max_frontend
npm ci
```

Скопируйте `.env.example` в `.env` в этом же каталоге и настройте переменные:

| Переменная      | Назначение                                                                                    |
| --------------- | --------------------------------------------------------------------------------------------- |
| `VITE_API_URL`  | Адрес API с префиксом версии: локально `http://localhost:8080/v1`, за общим nginx — `/api/v1` |
| `VITE_API_MODE` | `real` для API; `mock` для поддерживаемых демонстрационных запросов                           |
| `VITE_MAX_MODE` | `real` для запуска в MAX; `mock` для имитации данных запуска при разработке                   |

```sh
npm run dev
```

Vite запускается на `http://localhost:5173`. Для полного сценария с реальной авторизацией мини-приложение нужно открывать из бота MAX после заполнения исходных данных. Адрес приложения должен быть доступен с устройства пользователя; `localhost` на телефоне не указывает на компьютер разработчика.

При раздельных адресах frontend и API потребуется настроить CORS или прокси. Текущая серверная конфигурация nginx обслуживает их на одном домене. Mock-режим не заменяет backend целиком: рекомендации, направления и план обращаются к реальному API.

Переменные `VITE_*` встраиваются в сборку. После их изменения требуется перезапуск dev-сервера или новая сборка образа.

## Развёртывание на сервере

Корневой `docker-compose.yml` использует готовые образы `${DOCKER_USERNAME}/abiturient-backend:latest` и `${DOCKER_USERNAME}/abiturient-frontend:latest`.

Создайте `.env` в корне проекта:

```dotenv
DOCKER_USERNAME=your-dockerhub-account
POSTGRES_DB=score_manager
POSTGRES_USER=score_manager
POSTGRES_PASSWORD=replace-with-a-strong-password
MAX_BOT_TOKEN=your-max-bot-token
MAX_WEBHOOK_SECRET=replace-with-a-random-secret
JWT_SECRET=replace-with-a-random-secret-at-least-32-characters
JWT_TTL=3600
```

Файл `.env` исключён из Git. Для своего домена измените `server_name` и пути к сертификатам в `nginx/conf.d/default.conf`. Текущая конфигурация рассчитана на `runiversityadmission.ru` и сертификаты в `/etc/letsencrypt/live/runiversityadmission.ru/` на сервере.

Перед запуском образы должны быть опубликованы в указанном Docker Hub аккаунте. Их можно собрать из корня репозитория, заменив `your-dockerhub-account`:

```sh
docker build -t your-dockerhub-account/abiturient-backend:latest ./bot_max_backend
docker build -t your-dockerhub-account/abiturient-frontend:latest ./bot_max_frontend
docker push your-dockerhub-account/abiturient-backend:latest
docker push your-dockerhub-account/abiturient-frontend:latest
```

Dockerfile frontend по умолчанию использует реальный режим и `/api/v1`. Корневой Compose скачивает образы, а не собирает исходники:

```sh
docker compose pull
docker compose up -d
docker compose logs -f backend frontend nginx
```

Nginx направляет `/api/` в backend, `/webhook/` — в обработчик webhook, остальные запросы — во frontend. В настройках бота MAX необходимо указать HTTPS-адрес мини-приложения и настроить webhook на `https://<домен>/webhook/max` с соответствующим секретом.

Идентификатор web-app задаётся настройкой `MAX_WEB_APP` backend; её значение по умолчанию находится в `application.yaml`. Если используется другой бот, передайте нужное значение через `environment` сервиса `backend` в Compose.

Остановка без удаления томов базы данных:

```sh
docker compose down
```

## Проверки

Frontend, из `bot_max_frontend/`:

```sh
npm test
npm run lint
npm run build
```

Backend, из `bot_max_backend/`:

```sh
./mvnw test
```

В PowerShell: `./mvnw.cmd test`. Для тестов с Testcontainers нужен работающий Docker.

## Документация API

- [API планирования поступления](bot_max_backend/admission-planning.openapi.yaml)
- [Общий контракт API](bot_max_backend/max-abiturient-api-0.0.2%20%281%29.yaml)
- [API направлений](bot_max_backend/directions-api.md)

Основные маршруты мини-приложения: `/onboarding/interests`, `/onboarding/olympiads`, `/onboarding/achievements`, `/onboarding/privileges`, `/profile`, `/profile/ege`, `/universities` и `/universities?view=plan`.
