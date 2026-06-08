# Account Service Provider Contract

Account Service is the provider for the `event-gateway-api` consumer Pact.
The gateway generates the executable consumer contract, and Account Service
verifies that contract with `AccountServicePactProviderTest`.

## Pacticipants

| Role | Pacticipant |
| --- | --- |
| Consumer | `event-gateway-api` |
| Provider | `account-service` |

## Local Contract Files

The provider test reads the local Pact fixture from:

```text
src/test/resources/pacts/event-gateway-api-account-service.json
```

The sibling Event Gateway project generates the source Pact at:

```text
../EventGatewayService/target/pacts/event-gateway-api-account-service.json
```

When the gateway contract changes, regenerate the consumer Pact in the gateway
project and copy the updated JSON into Account Service's `src/test/resources/pacts`
folder for offline provider validation.

## Verified HTTP Contract

All account endpoints under `/accounts/**` require the internal gateway headers:

| Header | Default value |
| --- | --- |
| `X-Internal-Caller` | `event-gateway-api` |
| `X-Internal-Token` | `local-dev-token` |
| `X-Trace-Id` | Current Gateway request trace id |

Transaction apply also requires:

| Header | Value |
| --- | --- |
| `Idempotency-Key` | upstream `eventId` |

### Apply Transaction

```http
POST /accounts/{accountId}/transactions
Content-Type: application/json
Idempotency-Key: {eventId}
X-Internal-Caller: event-gateway-api
X-Internal-Token: local-dev-token
X-Trace-Id: {traceId}
```

Accepted transactions and exact duplicates return:

```http
204 No Content
```

Business rejections return:

```http
409 Conflict
Content-Type: application/json

{"code":"ACCOUNT_TRANSACTION_REJECTED","description":"Insufficient funds."}
```

### Read Balance

```http
GET /accounts/{accountId}/balance
Accept: application/json
X-Internal-Caller: event-gateway-api
X-Internal-Token: local-dev-token
X-Trace-Id: {traceId}
```

Successful response:

```json
{
  "accountId": "acct-123",
  "balance": 275.50,
  "currency": "USD"
}
```

### Read Account

```http
GET /accounts/{accountId}
Accept: application/json
X-Internal-Caller: event-gateway-api
X-Internal-Token: local-dev-token
X-Trace-Id: {traceId}
```

Missing account response:

```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "description": "Account not found."
}
```

## Run Contract Tests

Run the Account Service provider verification locally:

```powershell
mvn "-Dtest=AccountServicePactProviderTest" test
```

Run the full Account Service test suite:

```powershell
mvn test
```

## Regenerate From Event Gateway

From the sibling gateway project:

```powershell
cd ..\EventGatewayService
mvn "-Dtest=AccountServicePactConsumerTest" test
```

Then refresh the local provider fixture:

```powershell
Copy-Item target\pacts\event-gateway-api-account-service.json ..\AccountSvc\src\test\resources\pacts\event-gateway-api-account-service.json -Force
```

## PactFlow CI Flow

Consumer pipeline:

```powershell
$env:PACT_BROKER_BASE_URL = "https://<your-org>.pactflow.io"
$env:PACT_BROKER_TOKEN = "<pactflow-token>"
$env:PACT_CONSUMER_BRANCH = "main"
mvn "-Dtest=AccountServicePactConsumerTest" test
mvn "-Dpacticipant.version=<git-sha-or-build-number>" pact:publish
```

Provider pipeline:

```powershell
$env:PACT_BROKER_BASE_URL = "https://<your-org>.pactflow.io"
$env:PACT_BROKER_TOKEN = "<pactflow-token>"
$env:PACT_PROVIDER_BRANCH = "main"
$env:PACT_PUBLISH_VERIFICATION_RESULTS = "true"
mvn "-Dtest=AccountServicePactProviderTest" "-Dpacticipant.version=<git-sha-or-build-number>" test
```

Use the local Pact fixture for fast development. Use PactFlow in CI so the
provider verifies the exact consumer contracts published by deployable gateway
versions.

