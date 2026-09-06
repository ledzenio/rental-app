# Rental Service

Стартовая база для проекта "Сервис аренды техники с модулем отслеживания состояния оборудования и расчета износа".

## Структура

- `backend/` - backend на Spring Boot
- `frontend/` - frontend на React + TypeScript + Vite
- `docker-compose.yml` - PostgreSQL + Redis для локальной разработки

## Быстрый запуск

1. Поднять инфраструктуру:
   - `docker compose up -d`
2. Настроить переменные окружения для backend (обязательно для OAuth2 и Geocoding):
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
   - `YANDEX_GEOCODER_API_KEY`
   - `SMTP_HOST`
   - `SMTP_PORT` (обычно `587`)
   - `SMTP_USERNAME`
   - `SMTP_PASSWORD`
   - `MAIL_FROM` (опционально, по умолчанию = `SMTP_USERNAME`)
   - опционально:
     - `FRONTEND_URL` (по умолчанию `http://localhost:5173`)
     - `DEPOT_LATITUDE`, `DEPOT_LONGITUDE` (координаты базы)
     - `LOGISTICS_FREE_DISTANCE_KM` (бесплатный радиус)
     - `LOGISTICS_SURCHARGE_PER_KM` (надбавка за 1 км)
3. Запустить backend:
   - `cd backend`
   - `mvn spring-boot:run`
   - backend URL: `http://localhost:8088`
4. Запустить frontend:
   - `cd frontend`
   - `npm install`
   - `npm run dev`
5. Открыть `http://localhost:5173`

### Сделано

- Симметричная структура: отдельные `backend/` и `frontend/`
- Очистка проекта от дублирующих корневых backend-файлов
- Backend: Spring Boot 3.2 + Java 21 + PostgreSQL + Flyway
- JWT auth (`access + refresh`) с хранением refresh токенов в БД
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
  - `POST /api/v1/auth/logout`
  - `GET /api/v1/auth/me`
- OAuth2 / OpenID Connect:
  - вход через Google аккаунт (Gmail) через `Spring Security OAuth2 Client`
  - маршрут backend: `/oauth2/authorization/google`
  - после успешного входа выдаются внутренние JWT (`access + refresh`)
- Роли: `USER`, `MANAGER`, `SERVICE_SPECIALIST`
- Предсозданные сотрудники:
  - `manager@rental.local` / `password`
  - `specialist@rental.local` / `password`
- Каталог услуг:
  - `GET /api/v1/services` (поиск, фильтрация, сортировка, пагинация)
  - `GET /api/v1/services/{id}`
- Пользовательские сценарии:
  - `GET /api/v1/user/saved-services`
  - `POST /api/v1/user/saved-services/{serviceId}`
  - `DELETE /api/v1/user/saved-services/{serviceId}`
  - `POST /api/v1/user/service-requests`
  - `GET /api/v1/user/service-requests`
  - `GET /api/v1/user/service-requests/geocoding-estimate?address=...` (Yandex Geocoder + расчет логистики)
- Менеджерские сценарии:
  - `GET /api/v1/manager/users` (поиск + пагинация)
  - `GET /api/v1/manager/users/{userId}`
  - `PATCH /api/v1/manager/users/{userId}` (роли + статус блокировки)
  - `GET /api/v1/manager/service-requests` (фильтр по статусу + пагинация)
  - `PATCH /api/v1/manager/service-requests/{requestId}/status`
  - `GET /api/v1/manager/service-requests/summary`
  - `POST /api/v1/manager/services` (создание услуги)
  - `PUT /api/v1/manager/services/{serviceId}` (редактирование услуги)
  - `PATCH /api/v1/manager/services/{serviceId}/activate` (активация услуги)
  - `DELETE /api/v1/manager/services/{serviceId}` (деактивация услуги)
  - `POST /api/v1/manager/invoices` (выставление счета за услугу/штрафа)
  - `GET /api/v1/manager/service-requests/invoice-candidates` (заявки-кандидаты для выставления сервисного счета)
  - `GET /api/v1/manager/defect-reports` (список дефектных ведомостей)
  - `PATCH /api/v1/manager/defect-reports/{defectReportId}/status` (утверждение/отклонение)
  - `GET /api/v1/manager/defect-reports/summary` (сводка по статусам)
  - `GET /api/v1/manager/reports/revenue?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD`
  - `GET /api/v1/manager/reports/revenue/export?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD` (CSV)
- Role-based доступ:
  - manager endpoints защищены через `@PreAuthorize("hasRole('MANAGER')")`
  - specialist endpoints защищены через `@PreAuthorize("hasRole('SERVICE_SPECIALIST')")`
- Сервисный модуль:
  - `POST /api/v1/specialist/equipment/{equipmentId}/state` (фиксация состояния + расчет износа/амортизации)
  - `GET /api/v1/specialist/equipment/{equipmentId}/state` (история последних состояний)
  - `POST /api/v1/specialist/defect-reports` (дефектная ведомость)
  - `GET /api/v1/specialist/defect-reports` (список ведомостей, опц. фильтр по статусу)
- Финансовый модуль (базовый):
  - `GET /api/v1/user/invoices` (список счетов текущего пользователя)
  - `POST /api/v1/user/invoices/{invoiceId}/pay` (оплата счета виртуального баланса)
  - `POST /api/v1/auth/me/top-up/request-code` (запрос кода подтверждения пополнения)
  - `POST /api/v1/auth/me/top-up/confirm` (пополнение баланса по коду)
  - SMTP:
    - при выставлении счета пользователю отправляется email-уведомление
    - код подтверждения пополнения баланса отправляется на email пользователя
  - привязка счета к заявке и/или дефектной ведомости
  - при оплате штрафного счета дефектная ведомость получает статус `APPROVED`
  - бизнес-валидации:
    - штрафной счет (`PENALTY`) требует `defectReportId`
    - сервисный счет (`SERVICE`) требует `serviceRequestId`
    - дефектная ведомость может выставляться в счет только как `PENALTY`
  - при выставлении сервисного счета заявка переходит в `IN_PROGRESS`
  - после оплаты сервисного счета заявка переходит в `IN_PROGRESS` (если она была в `AWAITING_PAYMENT`)
- Workflow дефектных ведомостей с правилами переходов:
  - `NEW -> SENT_TO_MANAGER | REJECTED`
  - `SENT_TO_MANAGER -> APPROVED | REJECTED`
  - из `APPROVED/REJECTED` переходы запрещены
- Тесты:
  - добавлен unit-тест `ManagerDefectWorkflowServiceTest` на валидацию переходов
- Реализация Strategy pattern для расчета износа:
  - `WearCalculationStrategy`
  - `DefaultWearCalculationStrategy`
- Миграции:
  - `V1__init_schema.sql`
  - `V2__add_refresh_tokens.sql`
  - `V3__add_service_catalog.sql`
  - `V4__specialist_equipment_wear_and_defects.sql`
  - `V5__billing_and_defect_workflow.sql`
- Frontend (продуктовый этап для роли USER):
  - стек: `React + TypeScript + Vite + Zustand + React Router + Axios`
  - улучшенная архитектура frontend:
    - выделены `api.ts`, `authStore.ts`, `types.ts`
    - структурированные пользовательские страницы с отдельными маршрутами
  - реализованные пользовательские экраны:
    - `Auth` (вход/регистрация)
    - `Каталог услуг` (поиск, фильтр, добавление в отложенные, создание заявки)
    - `Карточка услуги` (`/services/:id`)
    - `Отложенные услуги`
    - `Мои заявки`
    - `Мои счета` (оплата)
    - `Профиль`
  - пользовательская логика оформления заявки:
    - выбор точки объекта на карте
    - расчет логистики до оформления
    - сохранение черновика адреса/комментария при добавлении в отложенные
    - оформление заявки отдельным действием
  - улучшения UX:
    - чистая навигация по роли USER
    - единый визуальный стиль и адаптивная верстка
    - статусы загрузки/ошибок/успешных действий
    - всплывающий чат-виджет в правом нижнем углу:
      - USER: диалоги по своим заявкам
      - MANAGER: диалоги со всеми пользователями по заявкам


### Отдельно: проверка OAuth2 Google

1. В Google Cloud Console создать OAuth Client ID (Web application)
2. Добавить redirect URI:
   - `http://localhost:8088/login/oauth2/code/google`
3. В приложении нажать кнопку `Войти через Google (Gmail)` на странице авторизации
4. После согласия Google должен вернуть на frontend, пользователь авторизуется в системе

### Отдельно: проверка Yandex Geocoder

1. Зайти пользователем в карточку услуги или в `Отложенные услуги`
2. Выбрать точку объекта на карте
3. Проверить, что показываются:
   - нормализованный адрес
   - расстояние до базы
   - логистическая надбавка
4. Создать заявку и убедиться, что логистический блок добавился в комментарий заявки

### Отдельно: проверка выставления и оплаты счета (реальный SMTP)

1. Под менеджером открыть `Панель менеджера -> Финансы`
2. Выбрать заявку из выпадающего списка кандидатов (чек заполняется автоматически)
3. Проверить сумму/описание и нажать `Отправить чек клиенту`
4. Под пользователем открыть `Мои счета` и убедиться, что счет отображается
5. Нажать `Получить код оплаты`
6. Проверить почту пользователя, взять код из письма
7. Ввести код в форме счета и нажать `Подтвердить оплату`
8. Проверить:
   - статус счета стал `PAID`
   - виртуальный баланс пользователя уменьшился

### Как настроить SMTP (пример для Gmail)

1. Включить 2FA в Google-аккаунте отправителя
2. Создать App Password (Google Account -> Security -> App passwords)
3. Установить переменные:
   - `SMTP_HOST=smtp.gmail.com`
   - `SMTP_PORT=587`
   - `SMTP_USERNAME=<ваш gmail>`
   - `SMTP_PASSWORD=<app password из 16 символов>`
   - `MAIL_FROM=<тот же gmail>`
4. Перезапустить backend
