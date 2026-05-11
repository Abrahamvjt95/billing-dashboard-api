# Billing Dashboard — API

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![CI](https://github.com/Abrahamvjt95/billing-dashboard-api/actions/workflows/ci.yml/badge.svg)

REST API for a SaaS billing management system. Manages clients, invoices with line items, and payment tracking with automatic status transitions.

**Live API →** `https://billing-dashboard-api-production.up.railway.app`  
**Swagger UI →** `https://billing-dashboard-api-production.up.railway.app/swagger-ui.html`  
**Demo account →** `demo@billflow.com` / `demo1234`  
**Frontend →** [billing-dashboard-frontend-zeta.vercel.app](https://billing-dashboard-frontend-zeta.vercel.app)  
**Frontend repo →** [billing-dashboard-frontend](https://github.com/Abrahamvjt95/billing-dashboard-frontend)

---

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security + JWT (access + refresh token) |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | PostgreSQL 17 |
| Migrations | Flyway |
| Documentation | SpringDoc OpenAPI 3 |
| Tests | JUnit 5 + Mockito + Testcontainers |
| Build | Maven |
| Deploy | Railway |

## Architecture

```
┌─────────────────────────────────────────┐
│              Angular Frontend           │
└────────────────────┬────────────────────┘
                     │ HTTPS / JWT
┌────────────────────▼────────────────────┐
│           Spring Boot API               │
│  Controllers → Services → Repositories  │
└────────────────────┬────────────────────┘
                     │ JPA / Flyway
┌────────────────────▼────────────────────┐
│              PostgreSQL                 │
│  users · clients · invoices · payments  │
└─────────────────────────────────────────┘
```

## Key features

- **JWT auth** — stateless, access token (15 min) + refresh token (7 days) with rotate-on-use
- **Invoice lifecycle** — DRAFT → SENT → PAID (auto) / OVERDUE
- **Partial payments** — multiple payments per invoice, auto-marks PAID when balance reaches zero
- **Row-level ownership** — users only access their own clients and invoices
- **RFC 9457 error responses** — consistent `ProblemDetail` format for all errors
- **OpenAPI docs** — interactive Swagger UI with JWT authorization support

## API endpoints

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh

GET    /api/v1/clients
POST   /api/v1/clients
GET    /api/v1/clients/{id}
PUT    /api/v1/clients/{id}
DELETE /api/v1/clients/{id}

GET    /api/v1/invoices?status=SENT
POST   /api/v1/invoices
GET    /api/v1/invoices/{id}
PUT    /api/v1/invoices/{id}
DELETE /api/v1/invoices/{id}

GET    /api/v1/invoices/{id}/payments
POST   /api/v1/invoices/{id}/payments

GET    /api/v1/dashboard/stats
```

## Running locally

**Prerequisites:** Java 21, Maven, PostgreSQL 17

```bash
# 1. Clone
git clone https://github.com/Abrahamvjt95/billing-dashboard-api.git
cd billing-dashboard-api

# 2. Create database
psql -U postgres -c "CREATE USER billing_user WITH PASSWORD 'billing_pass';"
psql -U postgres -c "CREATE DATABASE billing_dashboard OWNER billing_user;"

# 3. Run (Flyway runs migrations automatically)
mvn spring-boot:run
```

Or with Docker Compose (no PostgreSQL needed locally):

```bash
docker compose up
```

The API will be available at `http://localhost:8080`.  
Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Environment variables

| Variable | Description | Default |
|---|---|---|
| `PGHOST` | PostgreSQL host | `localhost` |
| `PGPORT` | PostgreSQL port | `5432` |
| `PGDATABASE` | Database name | `billing_dashboard` |
| `PGUSER` | Database user | `billing_user` |
| `PGPASSWORD` | Database password | `billing_pass` |
| `JWT_SECRET` | Base64-encoded 256-bit secret | dev default (change in prod) |
| `PORT` | Server port | `8080` |

## Running tests

```bash
# Unit tests (no DB needed)
mvn test -Dtest="*ServiceTest"

# Integration tests (requires local PostgreSQL)
mvn test -Dtest="*LocalIntegrationTest"

# All tests with Docker (Testcontainers)
mvn test
```

## Project structure

```
src/
├── main/java/com/abrahamjaimes/billing/
│   ├── config/          # Security, OpenAPI, Application beans
│   ├── controller/      # REST controllers
│   ├── dto/             # Request / Response records
│   ├── entity/          # JPA entities + enums
│   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   ├── repository/      # Spring Data JPA interfaces
│   ├── security/        # JWT filter, service, properties
│   └── service/         # Business logic
└── main/resources/
    ├── application.properties
    └── db/migration/    # Flyway SQL migrations (V1–V5)
```
