# CodeOps-Server Architecture

## Overview

CodeOps-Server is a consolidated Spring Boot 3.3 monolith running on Java 21. It combines four formerly independent microservices into a single deployable unit:

| Module | Package | API Prefix | Purpose |
|--------|---------|------------|---------|
| **Core** | `com.codeops.*` | `/api/v1/` | Authentication, teams, projects, QA jobs, findings, personas, directives, compliance, tech debt, dependencies, health monitoring, notifications |
| **Registry** | `com.codeops.registry.*` | `/api/v1/registry/` | Service registry, solutions, port allocations, API routes, topology, environment configs, workstation profiles, infrastructure resources |
| **Logger** | `com.codeops.logger.*` | `/api/v1/logger/` | Log ingestion, log queries, log traps, alert channels/rules, metrics, dashboards, traces, retention policies, anomaly detection |
| **Courier** | `com.codeops.courier.*` | `/api/v1/courier/` | HTTP request proxy, collections, folders, requests, environments, variables, GraphQL, collection runner, import/export, code generation, sharing |

## Codebase Statistics

| Category | Core | Registry | Logger | Courier | Total |
|----------|------|----------|--------|---------|-------|
| Entities | 28 | 11 | 16 | 18 | **73** |
| Enums | 25 | 11 | 10 | 7 | **53** |
| Controllers | 17 | 10 | 11 | 13 | **51** |
| Services | 26 | 10 | 19 | 22 | **77** |
| Repositories | 26 | 11 | 16 | 18 | **71** |
| Mappers | 0 | 0 | 13 | 13 | **26** |
| **Source Files** | | | | | **642** |
| **Test Files** | | | | | **163** |
| **Test Methods** | | | | | **2540** |
| **Database Tables** | 28 | 10 | 16 | 18 | **72** |
| **API Paths** | 127 | 65 | 75 | 62 | **329** |
| **API Operations** | 151 | 77 | 104 | 79 | **411** |

## Package Structure

```
com.codeops/
├── CodeOpsApplication.java          ← @SpringBootApplication (scans all subpackages)
├── config/                          ← Shared config (AppConstants, DataSeeder, Jackson, CORS, Kafka, etc.)
├── security/                        ← JWT auth (JwtTokenProvider, JwtAuthFilter, SecurityConfig, SecurityUtils)
├── entity/                          ← Core JPA entities (BaseEntity superclass, 25 enums)
├── repository/                      ← Core Spring Data JPA repositories
├── dto/request/                     ← Core request DTOs (Java records + Jakarta Validation)
├── dto/response/                    ← Core response DTOs (Java records)
├── service/                         ← Core business logic
├── controller/                      ← Core REST controllers
├── notification/                    ← Email (SMTP), Teams webhook dispatch
├── exception/                       ← Shared exceptions (CodeOpsException, NotFoundException, etc.)
│
├── registry/                        ← Registry module
│   ├── entity/ + entity/enums/
│   ├── repository/
│   ├── dto/request/ + dto/response/
│   ├── service/
│   ├── controller/
│   └── util/                        ← SlugUtils
│
├── logger/                          ← Logger module
│   ├── entity/ + entity/enums/
│   ├── repository/
│   ├── dto/request/ + dto/response/ + dto/mapper/
│   ├── service/
│   ├── controller/
│   └── event/                       ← LogEntryEventListener
│
└── courier/                         ← Courier module
    ├── entity/ + entity/enums/
    ├── repository/
    ├── dto/request/ + dto/response/ + dto/mapper/
    ├── service/
    ├── controller/
    └── config/                      ← HttpClientConfig (java.net.http.HttpClient bean)
```

## Shared Infrastructure

All modules share these Server-owned components:

- **BaseEntity** (`com.codeops.entity.BaseEntity`) — `id` (UUID), `createdAt`, `updatedAt` — extended by all entities
- **SecurityUtils** (`com.codeops.security.SecurityUtils`) — `getCurrentUserId()` from JWT principal
- **JwtAuthFilter** / **JwtTokenProvider** — JWT HS256 authentication (24h access, 30d refresh)
- **RateLimitFilter** — Request rate limiting
- **RequestCorrelationFilter** — Correlation ID propagation
- **AppConstants** — All module constants with module-specific prefixes (`LOGGER_`, `COURIER_`)
- **Exceptions** — `CodeOpsException`, `NotFoundException`, `ValidationException`, `AuthorizationException`
- **GlobalExceptionHandler** — Centralized error responses (400/401/403/404/500)
- **DataSeeder** — Seeds development data for all 4 modules (dev profile only)
- **SecurityConfig** — Single `SecurityFilterChain` with per-module path authorization

## Authentication & Authorization

- JWT (HS256) with 24h access tokens and 30d refresh tokens
- Principal stored as UUID in `SecurityContext`
- Team-scoped roles: **OWNER**, **ADMIN**, **MEMBER**, **VIEWER**
- Core endpoints: role checked in service layer via team membership
- Registry/Logger/Courier: `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")` at controller level
- Public endpoints: `/api/v1/health`, `/api/v1/courier/health`, `/api/v1/auth/**`, Swagger UI

## Infrastructure

| Service | Port | Container | Purpose |
|---------|------|-----------|---------|
| CodeOps-Server | 8090 | — | Application (all 4 modules) |
| PostgreSQL | 5432 | codeops-db | Primary database (72 tables in `public` schema) |
| Redis | 6379 | codeops-redis | Rate limiting, caching |
| Kafka | 9092 | codeops-kafka | Log ingestion pipeline |
| Zookeeper | 2181 | codeops-zookeeper | Kafka coordination |

## Key Technical Decisions

- **Hibernate `ddl-auto: update`** for schema management (no Flyway during development)
- **`open-in-view: false`** — Lazy collections require `@Transactional(readOnly = true)` on service read methods
- **MapStruct** with `builder = @Builder(disableBuilder = true)` and `componentModel = "spring"` for Logger/Courier mappers
- **GraalVM Polyglot** for Courier pre-request/post-response JavaScript sandboxing
- **`java.net.http.HttpClient`** for Courier HTTP proxy (not RestTemplate)
- **Bean name qualifiers** for duplicate simple class names across modules (e.g., `courierHealthController`, `loggerMetricsService`, `registryDependencyController`)

## Running

```bash
# Start infrastructure
docker-compose up -d

# Start server (dev profile enables DataSeeder)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
mvn test

# API docs
open http://localhost:8090/swagger-ui/index.html
```
