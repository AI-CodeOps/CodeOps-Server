# CodeOps Server

**AI-powered software maintenance platform — Cloud Service**

## Overview

CodeOps Server is the cloud backend for the CodeOps desktop application. It provides authentication, team collaboration, data persistence, file storage, and notification services for the AI-powered codebase health management platform.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Database | PostgreSQL 16 |
| File Storage | AWS S3 (local filesystem fallback for dev) |
| Email | AWS SES (logging fallback for dev) |
| Auth | Self-managed JWT (HS256) |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Container | Docker |

## Getting Started

### Prerequisites

- **Java 21** (JDK) — `java -version`
- **Maven 3.9+** — `mvn -version`
- **Docker & Docker Compose** — `docker compose version`

### Start Infrastructure

```bash
# Start all Docker containers (Postgres, Redis, Zookeeper, Kafka)
chmod +x scripts/start-infra.sh scripts/stop-infra.sh scripts/reset-infra.sh
./scripts/start-infra.sh
```

### Start the Server

```bash
mvn clean compile -DskipTests
mvn spring-boot:run
```

The server starts on **http://localhost:8090**.

### Verify

```bash
# Health check
curl http://localhost:8090/api/v1/health

# Swagger UI
open http://localhost:8090/swagger-ui/index.html
```

### Stop / Reset

```bash
./scripts/stop-infra.sh      # Stop containers (data preserved)
./scripts/reset-infra.sh     # Stop containers AND delete all data
```

### Port Reference

| Service | Port | Credentials |
|---------|------|-------------|
| CodeOps Server | 8090 | — |
| CodeOps Vault | 8097 | — |
| PostgreSQL (Server) | 5432 | codeops / codeops / codeops |
| PostgreSQL (Vault) | 5433 | codeops_vault / codeops_vault / codeops_vault |
| Redis | 6379 | — |
| Zookeeper | 2181 | — |
| Kafka | 9092 | — |

## Environment Variables

### Required (Production)

| Variable | Description |
|----------|-------------|
| DATABASE_URL | PostgreSQL JDBC URL |
| DATABASE_USERNAME | Database user |
| DATABASE_PASSWORD | Database password |
| JWT_SECRET | JWT signing secret (min 32 chars) |
| ENCRYPTION_KEY | AES-256 encryption key for credentials (min 32 chars) |

### Optional (Production)

| Variable | Description | Default |
|----------|-------------|---------|
| S3_BUCKET | S3 bucket name | codeops-dev |
| AWS_REGION | AWS region | us-east-1 |
| SES_FROM_EMAIL | Sender email for notifications | noreply@codeops.dev |

### Dev Defaults
In dev profile, S3 and SES are disabled. Files are stored locally at `~/.codeops/storage/`. Emails are logged to console.

## API Overview

Base URL: `/api/v1`

### Authentication
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /auth/register | Public | Register new user |
| POST | /auth/login | Public | Login, returns JWT |
| POST | /auth/refresh | Public | Refresh JWT token |
| POST | /auth/change-password | Bearer | Change password |

### Users & Teams
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /users/me | Bearer | Current user profile |
| POST | /teams | Bearer | Create team |
| GET | /teams | Bearer | List user's teams |
| POST | /teams/{id}/invitations | Bearer | Invite member |

### Projects
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /projects/{teamId} | Bearer | Create project |
| GET | /projects/team/{teamId} | Bearer | List projects |
| PUT | /projects/{id} | Bearer | Update project |

### QA Jobs & Findings
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /jobs | Bearer | Create QA job |
| POST | /jobs/{id}/agents/batch | Bearer | Create agent runs |
| POST | /findings | Bearer | Create finding |
| GET | /findings/job/{id} | Bearer | List findings (paginated) |

### Personas & Directives
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /personas | Bearer | Create persona |
| POST | /directives | Bearer | Create directive |
| POST | /directives/assign | Bearer | Assign to project |

### Integrations
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /integrations/github/{teamId} | Bearer | Add GitHub connection |
| POST | /integrations/jira/{teamId} | Bearer | Add Jira connection |

### Analysis & Monitoring
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /tech-debt | Bearer | Create tech debt item |
| POST | /dependencies/scans | Bearer | Create dependency scan |
| POST | /health-monitor/snapshots | Bearer | Create health snapshot |
| GET | /metrics/project/{id} | Bearer | Project metrics |
| GET | /metrics/team/{id} | Bearer | Team metrics |

### Admin
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /admin/users | ADMIN | List all users |
| GET | /admin/usage | ADMIN | System usage stats |
| GET | /admin/audit-log/team/{id} | ADMIN | Team audit log |

See full API docs at `/swagger-ui/index.html` when running.

## Auth Flow

1. Register: POST /auth/register -> returns JWT + refresh token
2. Login: POST /auth/login -> returns JWT + refresh token
3. Use JWT: Include `Authorization: Bearer <token>` header on all requests
4. Refresh: POST /auth/refresh with refresh token when JWT expires (24h)
5. Refresh tokens expire after 30 days

### Role Hierarchy
- **OWNER** — Full team control, delete team, transfer ownership
- **ADMIN** — User management, all CRUD operations
- **MEMBER** — Run jobs, manage findings, create personas/directives
- **VIEWER** — Read-only access

## Database

PostgreSQL with Hibernate auto-DDL (dev) / validate (prod).

- **25 tables** auto-generated from JPA entities
- UUID primary keys
- Instant timestamps (UTC)
- JSON stored as TEXT columns
- Encrypted credentials (AES-256-GCM) for GitHub PATs and Jira API tokens

## Docker

### Build

```bash
mvn clean package -DskipTests
docker build -t codeops-server .
```

### Run

```bash
docker run -p 8090:8090 \
  -e DATABASE_URL=jdbc:postgresql://host:5432/codeops \
  -e DATABASE_USERNAME=codeops \
  -e DATABASE_PASSWORD=<password> \
  -e JWT_SECRET=<secret> \
  -e ENCRYPTION_KEY=<key> \
  codeops-server
```

## Project Structure

```
src/main/java/com/codeops/
├── CodeOpsApplication.java
├── config/          — AppConstants, CORS, JWT, S3, SES, Async
├── security/        — JwtTokenProvider, JwtAuthFilter, SecurityConfig, SecurityUtils
├── entity/          — 25 JPA entities + enums/
├── repository/      — 25 Spring Data JPA repositories
├── dto/
│   ├── request/     — ~38 request DTOs (Java records)
│   └── response/    — ~30 response DTOs (Java records)
├── service/         — 24 service classes
├── controller/      — 18 REST controllers
└── notification/    — EmailService, TeamsWebhookService, NotificationDispatcher
```
