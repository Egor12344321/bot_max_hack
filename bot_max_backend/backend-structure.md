# Структура backend

Исходники: `src/main/java/com/runiversityadmisson/bot`.
Сначала выбираем слой, затем предметную область.

## Domain — модели, доступ к данным, расчёты

`domain/applicant/model` и `domain/applicant/ports` используют одинаковые группы:

| Пакет | Модели | Репозитории |
| --- | --- | --- |
| `profile` | User, Language, CitizenshipOption | UserRepository, LanguageRepository, CitizenshipOptionRepository |
| `direction` | Direction, InterestCategory | DirectionRepository, InterestCategoryRepository |
| `exam` | EgeScore, Subject | EgeScoreRepository, SubjectRepository |
| `university` | University, Program, ProgramSubject | ProgramRepository |
| `benefit` | Achievement, PrivilegeCategory, QuotaType | AchievementRepository |
| `olympiad` | Olympiad, OlympiadProfile, OlympiadDiploma, OlympiadDegree, OlympiadBenefit, OlympiadBenefitRule | OlympiadRepository, OlympiadProfileRepository, OlympiadBenefitRuleRepository |

`domain/applicant/service` — UserService и AdmissionBenefitCalculator.

## Application — сценарии приложения

- `direction` — каталог и выбор направлений.
- `olympiad` — справочник олимпиад и дипломы пользователя.
- `benefit` — достижения, льготы и персональный расчёт.
- `onboarding` — REST-сценарии заполнения анкеты.
- `onboarding/bot` — диалог онбординга в MAX, обработка обновлений и сообщения.
- `session` — состояние бот-сессии и чтение сохранённой анкеты.
- `exchange` — обмен launchParams на авторизацию.
- `catalog` — загрузка справочников.

### DTO

`application/dto` сгруппирован по назначению:
`auth`, `profile`, `session`, `direction`, `exam`, `olympiad`, `benefit`, `common`.

Запросы и ответы одного сценария находятся рядом. Суффиксы `Request`, `Input`, `Response`
показывают назначение класса. `DirectionSelection` используется и для запроса, и для ответа.
DTO результатов расчёта льгот, включая OlympiadBenefitResponse, лежат в `benefit`.

## Presentation — HTTP и авторизация

`presentation/controller`:

- `auth` — ExchangeController;
- `catalog` — CatalogController;
- `session` — SessionController, SessionOnboardingController;
- `direction` — DirectionsController;
- `benefit` — AdmissionBenefitsController;
- `webhook` — MaxWebhookController;
- `profile`, `university`, `planning` — контроллеры-заготовки профиля, вузов и планирования.

Общие обработчики ошибок — `presentation/exception`, фильтры — `presentation/filter`,
настройка безопасности и JWT — `presentation/security`.

## Infrastructure — внешние системы

- `config` — RedisConfig.
- `external/max` — клиент MAX и DTO его протокола.

## Тесты и ресурсы

- Unit-тесты в `src/test/java` повторяют пакеты проверяемых классов.
- Миграции: `src/main/resources/db/migration`.
- Справочники: `src/main/resources/catalog`.

Новые классы добавляем в существующий предметный пакет соответствующего слоя.
Если появляется новая область, создаём для неё согласованные пакеты в нужных слоях.
