# Структура backend

Исходники: `src/main/java/com/runiversityadmisson/bot`.
Сначала выбираем слой, затем предметную область.

## Словарь имён

Два слова использовались в двух смыслах, поэтому закреплены однозначно:

| Слово | Что означает |
| --- | --- |
| `BotQuestionnaire` | незавершённая анкета, которую бот набирает в диалоге. Живёт в Redis до часа, стирается после онбординга |
| `Session` | заявка абитуриента в БД. Только она адресуется в `/v1/sessions/{id}` |
| `Questionnaire` | заполнение заявки через REST мини-аппа |
| `StudyDirection` | федеральное направление подготовки из справочника, а не «направление» в общем смысле |
| `Program` | конкретная образовательная программа конкретного вуза |

`application/max` — код, работающий с платформой MAX: вебхуки, диалог, Redis.
Всё остальное в `application` — код над заявкой абитуриента.
`domain` и `presentation` платформой MAX не делятся.

## Domain — модели, доступ к данным, расчёты

`domain/applicant/model` и `domain/applicant/ports` используют одинаковые группы:

| Пакет | Модели | Репозитории |
| --- | --- | --- |
| `profile` | User, Language, CitizenshipOption | UserRepository, LanguageRepository, CitizenshipOptionRepository |
| `direction` | StudyDirection, InterestCategory | StudyDirectionRepository, InterestCategoryRepository |
| `exam` | EgeScore, Subject | EgeScoreRepository, SubjectRepository |
| `university` | University, Program, ProgramSubject | ProgramRepository |
| `benefit` | Achievement, PrivilegeCategory, QuotaType | AchievementRepository |
| `planning` | ApplicationPlan, ApplicationPlanItem | ApplicationPlanRepository |
| `olympiad` | Olympiad, OlympiadProfile, OlympiadDiploma, OlympiadDegree, OlympiadBenefit, OlympiadBenefitRule | OlympiadRepository, OlympiadProfileRepository, OlympiadBenefitRuleRepository |

`domain/applicant/service` — UserService, AdmissionBenefitCalculator, ProgramRecommendationPolicy (отбор программ для рекомендаций) и ApplicationPlanGenerator (автоплан 5×5).

## Application — сценарии приложения

Код над заявкой абитуриента:

- `direction` — каталог направлений и выбор пользователя.
- `olympiad` — справочник олимпиад и дипломы пользователя.
- `benefit` — достижения, льготы, персональный расчёт и справочник льготных категорий.
- `onboarding` — заполнение анкеты через REST.
- `session` — чтение сохранённой анкеты.
- `auth` — обмен launchParams на JWT.
- `planning` — рекомендации программ по выбранному направлению и план 5×5 (сохранение, автоплан).
- `profile` — экран профиля: данные пользователя и сводка по подборкам.

Код платформы MAX:

- `max` — разбор и маршрутизация обновлений от вебхука.
- `max/dialog` — состояние диалога бота: анкета, шаг, хранилище Redis.
- `max/onboarding` — сценарий диалога и отправка вопросов.

### DTO

`application/dto` сгруппирован по назначению:
`auth`, `profile`, `session`, `direction`, `exam`, `olympiad`, `benefit`, `planning`, `common`.

Запросы и ответы одного сценария находятся рядом. Суффиксы `Request`, `Input`, `Response`
показывают назначение класса. `StudyDirectionSelection` используется и для запроса, и для ответа.
DTO результатов расчёта льгот, включая OlympiadBenefitResponse, лежат в `benefit`.

## Presentation — HTTP и авторизация

`presentation/controller`:

- `auth` — AuthTokenController;
- `catalog` — CatalogController;
- `session` — SessionController (чтение заявки), SessionQuestionnaireController (заполнение);
- `direction` — StudyDirectionsController;
- `benefit` — AdmissionBenefitsController;
- `webhook` — MaxWebhookController;
- `planning` — RecommendationsController, ApplicationPlanController; остальные контроллеры планирования пока заготовки;
- `profile` — ProfileController;
- `university` — контроллер-заготовка вузов.

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
