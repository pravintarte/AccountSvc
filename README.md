# Account Service

Spring Boot Account Service skeleton aligned with the sibling Event Gateway API.

## Baseline Stack

- Java 21
- Spring Boot 4.0.6
- Spring Cloud 2025.1.1 with Netflix Eureka client
- Zipkin tracing through Spring Boot Actuator and Micrometer tracing
- Docker deployment
- Lombok
- Apache Commons Collections
- Hikari connection pooling through Spring Boot datasource auto-configuration
- H2 in-memory database
- Cucumber acceptance tests through the JUnit Platform

## API

- `POST /accounts/{accountId}/transactions` applies a CREDIT or DEBIT transaction sent by Event Gateway.
- `GET /accounts/{accountId}/balance` returns the computed balance.
- `GET /accounts/{accountId}` returns account details and recent transactions.
- `GET /health` and `GET /healthcheck` return the public service health response.
- `GET /actuator/health` returns runtime health details.
- `GET /h2-console` opens the development H2 console.

## Event Gateway Contract

Transaction apply requests accept the same payload used by Event Gateway's `AccountServiceClient`:

```json
{
  "eventId": "9b63f0d4-0f49-4f85-9447-fd45a0c5b3c2",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": {
    "source": "event-gateway"
  }
}
```

The endpoint also accepts `Idempotency-Key`; exact duplicate `eventId` calls are acknowledged without re-applying the transaction.

## Service Discovery

This service registers with Eureka using `spring.application.name=account-service`, matching Event Gateway's default `ACCOUNT_SERVICE_SERVICE_ID`.

Useful overrides:

```powershell
$env:EUREKA_DEFAULT_ZONE = "http://localhost:8761/eureka/"
$env:EUREKA_CLIENT_ENABLED = "true"
$env:EUREKA_REGISTER_WITH_EUREKA = "true"
$env:EUREKA_FETCH_REGISTRY = "true"
```

## Structured Logging and Tracing

Console logs use Spring Boot structured Logstash JSON format. Each log line includes stable `serviceName`, `traceId`, and `spanId` fields, matching Event Gateway's log contract.

Zipkin endpoint override:

```powershell
$env:ZIPKIN_ENDPOINT = "http://localhost:9411/api/v2/spans"
```

## Database Connection Pooling

Account Service is the downstream service for Event Gateway, so circuit
breakers belong on callers that make remote requests to this service. The local
ledger operations in this service are protected at the datasource boundary with
Hikari connection pooling.

Default pool settings:

```powershell
$env:ACCOUNT_SERVICE_DB_POOL_MAX_SIZE = "10"
$env:ACCOUNT_SERVICE_DB_POOL_MIN_IDLE = "2"
$env:ACCOUNT_SERVICE_DB_CONNECTION_TIMEOUT = "2000"
$env:ACCOUNT_SERVICE_DB_VALIDATION_TIMEOUT = "1000"
$env:ACCOUNT_SERVICE_DB_IDLE_TIMEOUT = "600000"
$env:ACCOUNT_SERVICE_DB_MAX_LIFETIME = "1800000"
$env:ACCOUNT_SERVICE_DB_LEAK_DETECTION_THRESHOLD = "0"
```

Use database pool metrics from Actuator `GET /actuator/metrics` to tune these
values for real workload and database capacity.

## Acceptance Tests

Cucumber acceptance tests run through Maven and publish HTML, JSON, and JUnit XML reports under `target/cucumber-reports`.

```powershell
mvn -Dtest=RunCucumberAcceptanceTest test
mvn clean test
```

## Docker

```powershell
mvn clean package
docker compose up --build
```
