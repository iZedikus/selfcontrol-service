# Чеклист интеграции IS

Документ для ручной и автоматизированной приёмки REST API информационной системы (IS).
Спецификация: `REST_КОНТРАКТ.yaml` (секции `[IS / …]`).

## Окружение

| Переменная | Значение по умолчанию | Назначение |
|------------|----------------------|------------|
| `IS_BASE_URL` | `http://localhost:8080` | Базовый URL IS |
| `SIMULACRUM_BASE_URL` | `http://localhost:8081` | Базовый URL Simulacrum (исходящие вызовы IS) |
| `IS_CREDITOR_SYSTEM_ID` | `00000000-0000-0000-0000-000000000001` | Идентификатор IS в Simulacrum |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/selfcontrol` | PostgreSQL |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ для ProfileSync / TriggerEvent |

Порт IS: **8080** (`server.port=8080` в `application.properties`).

Swagger UI: `http://localhost:8080/swagger-ui.html` (спецификация IS: `/openapi-is.yaml`).

## Матрица эндпоинтов IS

| Группа | Method | Path | Auth |
|--------|--------|------|------|
| Auth | POST | `/api/v1/auth/register` | — |
| Auth | POST | `/api/v1/auth/login` | — |
| Auth | POST | `/api/v1/auth/refresh` | — |
| Accounts | GET | `/api/v1/accounts` | Bearer |
| Accounts | POST | `/api/v1/accounts` | Bearer |
| Accounts | DELETE | `/api/v1/accounts/{linkedAccountId}` | Bearer |
| Consents | POST | `/api/v1/accounts/{linkedAccountId}/consent` | Bearer |
| Consents | DELETE | `/api/v1/accounts/{linkedAccountId}/consent` | Bearer |
| Scenarios | GET | `/api/v1/scenarios/templates` | — |
| Scenarios | GET | `/api/v1/scenarios` | Bearer |
| Scenarios | POST | `/api/v1/scenarios` | Bearer |
| Scenarios | PUT | `/api/v1/scenarios/{userScenarioId}` | Bearer |
| Scenarios | DELETE | `/api/v1/scenarios/{userScenarioId}` | Bearer |
| Executions | GET | `/api/v1/scenarios/{userScenarioId}/executions` | Bearer |
| Executions | GET | `/api/v1/executions/{executionId}` | Bearer |
| Notifications | GET | `/api/v1/notifications` | Bearer |
| Notifications | PATCH | `/api/v1/notifications/{notificationId}/read` | Bearer |
| Admin | GET | `/api/v1/admin/users` | Bearer (Admin) |
| Admin | PATCH | `/api/v1/admin/users/{userId}/status` | Bearer (Admin) |
| Admin | POST | `/api/v1/admin/scenarios/templates` | Bearer (Admin) |

## Исходящие REST-вызовы IS → Simulacrum

| Когда | Method | URL |
|-------|--------|-----|
| Выдача ПДА | POST | `{SIMULACRUM_BASE_URL}/api/v1/consents` |
| Отзыв ПДА | DELETE | `{SIMULACRUM_BASE_URL}/api/v1/consents/{consentId}` |
| Срабатывание сценария (TriggerEvent) | POST | `{SIMULACRUM_BASE_URL}/api/v1/payments/debit` |
| Polling статуса debit | GET | `{SIMULACRUM_BASE_URL}/api/v1/payments/{transactionId}/status` |

## E2E-сценарий

Предварительно в Simulacrum должен существовать тестовый счёт (через Simulacrum Admin API или Swagger на `:8081`):

```bash
export IS_BASE_URL=http://localhost:8080
export SIMULACRUM_BASE_URL=http://localhost:8081
```

### 1. Регистрация

```bash
curl -s -X POST "$IS_BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@selfcontrol.local",
    "password": "password1",
    "firstName": "Ivan",
    "lastName": "Ivanov",
    "phoneNumber": "+79001234567"
  }' | jq .
```

Сохраните `accessToken` и `userId`.

### 2. Логин (если нужен новый токен)

```bash
curl -s -X POST "$IS_BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@selfcontrol.local","password":"password1"}' | jq .
```

```bash
export TOKEN="<accessToken>"
export AUTH="Authorization: Bearer $TOKEN"
```

### 3. Привязка счёта

`paymentToken` должен совпадать с `accountId` счёта в Simulacrum (например `ACC-TEST-001`):

```bash
curl -s -X POST "$IS_BASE_URL/api/v1/accounts" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{
    "paymentToken": "ACC-TEST-001",
    "bankBic": "044525974",
    "currency": "RUB",
    "displayName": "Мой основной счёт",
    "maskedPan": "4321"
  }' | jq .
```

Сохраните `linkedAccountId`.

### 4. Выдача акцепта (ПДА)

```bash
export LINKED_ACCOUNT_ID="<linkedAccountId>"

curl -s -X POST "$IS_BASE_URL/api/v1/accounts/$LINKED_ACCOUNT_ID/consent" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{
    "totalDebitLimit": "10000.00",
    "maxSingleDebit": "500.00",
    "currency": "RUB",
    "expiresAt": "2030-01-01T00:00:00.000Z"
  }' | jq .
```

Проверка: в Simulacrum создан consent, IS сохранил `externalConsentId`.

### 5. Каталог сценариев и активация

```bash
curl -s "$IS_BASE_URL/api/v1/scenarios/templates" | jq .

export TEMPLATE_ID="<templateId из каталога>"

curl -s -X POST "$IS_BASE_URL/api/v1/scenarios" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{
    \"templateId\": \"$TEMPLATE_ID\",
    \"linkedAccountId\": \"$LINKED_ACCOUNT_ID\",
    \"debitAmount\": \"200.00\",
    \"currency\": \"RUB\",
    \"recipientPaymentToken\": \"charity-token-xyz\",
    \"scenarioConfig\": {
      \"mccCodes\": [\"5912\", \"5813\"],
      \"matchMode\": \"ANY\"
    }
  }" | jq .
```

Сохраните `userScenarioId`. IS публикует `ProfileSyncMessage` (REGISTER) в RabbitMQ.

### 6. Mock trigger → debit

**Вариант A — через Simulacrum:** сгенерировать транзакцию с подходящим MCC на счёт `ACC-TEST-001`. ORACLE получит событие и отправит `TriggerEventMessage` в IS.

**Вариант B — прямой mock (для отладки без ORACLE):** опубликовать `TriggerEventMessage` в очередь trigger inbox IS (см. `!RABBITMQ_КОНТРАКТ.yaml`).

После обработки trigger IS вызывает:
1. `POST {SIMULACRUM_BASE_URL}/api/v1/payments/debit`
2. `GET {SIMULACRUM_BASE_URL}/api/v1/payments/{transactionId}/status` (polling до 5 попыток)

### 7. Проверка execution и уведомлений

```bash
export USER_SCENARIO_ID="<userScenarioId>"

curl -s "$IS_BASE_URL/api/v1/scenarios/$USER_SCENARIO_ID/executions?page=0&size=20" \
  -H "$AUTH" | jq .
```

```bash
curl -s "$IS_BASE_URL/api/v1/notifications?unreadOnly=false" \
  -H "$AUTH" | jq .
```

Ожидаемые типы уведомлений: `ScenarioTriggered`, `DebitCompleted` или `DebitFailed`.

### 8. Отзыв акцепта (опционально)

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE \
  "$IS_BASE_URL/api/v1/accounts/$LINKED_ACCOUNT_ID/consent" \
  -H "$AUTH"
```

Ожидается **204**. Активные сценарии получают ProfileSync TERMINATE.

## Формат ошибок

Все 4xx/5xx возвращают:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Описание",
  "timestamp": "2026-05-25T10:00:00.000Z"
}
```

Без токена на защищённых эндпоинтах: **401** / `UNAUTHORIZED`.

## Быстрая проверка health

```bash
curl -s "$IS_BASE_URL/actuator/health" | jq .
```

## Запуск тестов

```bash
./gradlew test
```

Contract-тесты контроллеров: `src/test/java/ru/stepanov/selfcontrol/api/v1/*ControllerTest.java`.
