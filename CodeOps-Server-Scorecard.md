# CodeOps Server -- Quality Scorecard

**Project:** CodeOps Server
**Branch:** main
**Commit:** f27f566acf8b53d9ee7a15081fea752f7ee3484a
**Scorecard Date:** 2026-02-24T20:09:01Z
**Auditor:** Claude Code (Automated)

> This scorecard is a retrospective quality assessment. It is NOT loaded into coding sessions.
> It accompanies the audit (`CodeOps-Server-Audit.md`) and OpenAPI spec (`CodeOps-Server-OpenAPI.yaml`).

---

## Scoring Key

Each check is scored:
- **0** -- Not present
- **1** -- Partial implementation
- **2** -- Fully implemented

---

## Security (10 checks, max 20)

| ID | Check | Score | Evidence |
|----|-------|-------|----------|
| SEC-01 | Auth on all mutation endpoints | 2 | Class-level `@PreAuthorize("isAuthenticated()")` on all core controllers. `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")` on Courier/Logger/Registry controllers. All 380+ endpoints covered via class-level annotations. Method-level grep returns 0/220 (false negative -- class-level auth is not captured by method-level grep). |
| SEC-02 | SSRF protection on outbound URLs | 2 | 9 occurrences of URL validation across `AlertChannelService.validateWebhookUrl` and `TeamsWebhookService`. Blocks localhost, 127.0.0.1, ::1, 0.0.0.0, and all RFC1918 prefixes. HTTPS required for webhooks. |
| SEC-03 | Token revocation / logout | 2 | 34 references to blacklist. `TokenBlacklistService` uses in-memory `ConcurrentHashMap`. JTI checked on every JWT validation. Logout invalidates tokens immediately. |
| SEC-04 | Rate limiting present | 1 | `RateLimitFilter` applied to `/api/v1/auth/**` paths only. 10 requests per 60 seconds per IP. In-memory `ConcurrentHashMap` storage. **Gap:** Rate limiting does not cover non-auth endpoints (API abuse, data exfiltration). |
| SEC-05 | Input validation on all request DTOs | 2 | Jakarta Validation on all request DTOs. `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`, `@Valid` for nested objects. All controllers use `@Valid`. |
| SEC-06 | Encryption at rest | 2 | AES-256-GCM for GitHub PAT and Jira API token via `EncryptionService`. Key sourced from config (`@Value`). Encrypted values never returned in API responses. |
| SEC-07 | Password hashing | 2 | BCrypt with strength 12 configured in `SecurityConfig`. |
| SEC-08 | Security headers configured | 2 | CSP (`default-src 'self'; frame-ancestors 'none'`), X-Frame-Options DENY, X-Content-Type-Options nosniff, HSTS 1 year. |
| SEC-09 | CORS configuration | 2 | Configurable allowed origins (default `localhost:3000`). Credentials allowed. 1-hour preflight cache. No wildcard origins. |
| SEC-10 | Audit logging | 2 | `AuditLogService` (`@Async`) tracks entity CRUD operations. 84 audit logging references across controllers. |

**Security Score: 19 / 20 (95%)**

---

## Data Integrity (8 checks, max 16)

| ID | Check | Score | Evidence |
|----|-------|-------|----------|
| DAT-01 | All enum fields use `@Enumerated(STRING)` | 2 | 68 `@Enumerated(EnumType.STRING)` annotations. Zero `ORDINAL` usage anywhere in the codebase. |
| DAT-02 | Database indexes on FK columns | 2 | 116 `@Index` annotations across entities. Foreign key columns are indexed. |
| DAT-03 | Nullable constraints on required fields | 2 | 324 `nullable=false` column definitions across entity classes. |
| DAT-04 | Optimistic locking (`@Version`) | 1 | 5 entities with `@Version` (`QaJob`, `Finding`, `TechDebtItem`, `RemediationTask`, `AgentRun`). **Gap:** Remaining 18 entities lack optimistic locking -- concurrent updates could produce lost-update anomalies on high-contention entities. |
| DAT-05 | Cascade delete handled | 2 | `ProjectService.deleteProject()` handles FK-safe order deletion of all child records. Connections use soft delete (`isActive=false`). |
| DAT-06 | Pagination on list queries | 2 | 42 references to `Pageable`/`PageResponse`. All list endpoints support pagination. Default page size 20, max 100. |
| DAT-07 | Proper relationship mapping | 2 | JPA `@ManyToOne`/`@OneToMany` relationships used throughout. No comma-separated ID strings. |
| DAT-08 | Audit timestamps on entities | 2 | 23 entities extend `BaseEntity` (id, createdAt, updatedAt). 4 exceptions: `SystemSetting`, `AuditLog`, `MfaEmailCode`, `ProjectDirective` -- all have documented reasons for deviation. |

**Data Integrity Score: 15 / 16 (94%)**

---

## API Quality (8 checks, max 16)

| ID | Check | Score | Evidence |
|----|-------|-------|----------|
| API-01 | Consistent error responses (`GlobalExceptionHandler`) | 2 | 1 centralized handler covering 15 exception types. Consistent `ErrorResponse(status, message)` format. |
| API-02 | Audit logging on mutations | 2 | 84 audit logging references in controllers. Mutations tracked via `AuditLogService`. |
| API-03 | Pagination on list endpoints | 2 | 42 references to `Pageable`/`PageResponse`. Default page size 20, max 100. |
| API-04 | Correct HTTP status codes | 2 | 321 status code references. Proper use of 200 (OK), 201 (Created), 204 (No Content), 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 404 (Not Found), 405 (Method Not Allowed), 429 (Too Many Requests), 500 (Internal Server Error). |
| API-05 | Input validation | 2 | All request DTOs use Jakarta Validation annotations. `@Valid` on all controller method parameters. |
| API-06 | Correlation ID / request tracing | 2 | `RequestCorrelationFilter` generates/propagates `X-Correlation-ID` header. MDC integration for log correlation. |
| API-07 | Consistent error response format | 2 | `ErrorResponse` record used across all modules. Consistent shape regardless of exception type. |
| API-08 | OpenAPI / Swagger documentation | 2 | `springdoc-openapi-starter-webmvc-ui 2.5.0` configured. Swagger UI available at `/swagger-ui/index.html`. |

**API Quality Score: 16 / 16 (100%)**

---

## Code Quality (10 checks, max 20)

| ID | Check | Score | Evidence |
|----|-------|-------|----------|
| CQ-01 | No `getReferenceById` usage | 2 | 0 usages. All repository calls use `findById` with proper `orElseThrow` error handling. |
| CQ-02 | Consistent exception hierarchy | 2 | 4 custom exceptions: `CodeOpsException` (base), `AuthorizationException`, `ValidationException`, `NotFoundException`. Logger module has its own `ValidationException`. Clean hierarchy. |
| CQ-03 | No TODO/FIXME/HACK in source | 1 | 2 found: 1 in seed data markdown (not production code), 1 in `EncryptionService` comment about key rotation. **Minor:** The `EncryptionService` TODO should be tracked as a backlog item. |
| CQ-04 | Logging present in services/security | 2 | 176+ log statements across services. SLF4J/Logback with `@Slf4j` annotation. Structured logging (Logstash Logback Encoder) in prod profile. |
| CQ-05 | MapStruct configuration | 2 | All mappers use `componentModel="spring"` with builder disabled. Boolean `is*` field mappings explicitly handled with `@Mapping` annotations. |
| CQ-06 | Lazy loading on relationships | 2 | All `@ManyToOne` relationships use `FetchType.LAZY`. No eager fetching detected. |
| CQ-07 | Lombok usage | 2 | `@Data`/`@Getter`/`@Setter`/`@Builder`/`@RequiredArgsConstructor` on all entities/services. Version 1.18.42 for Java 25 compatibility. |
| CQ-08 | Transactional annotations | 2 | `@Transactional` on write operations. `@Transactional(readOnly = true)` on read operations where appropriate. |
| CQ-09 | Doc comments on classes | 1 | Class-level documentation on approximately 40% of classes. **Gap:** Majority of classes lack Javadoc. Controllers, services, and security classes should all have class-level documentation per CONVENTIONS.md. |
| CQ-10 | Package organization | 2 | Clean 4-module architecture. Each module follows `entity/enum/repository/service/controller/dto/mapper` pattern consistently. |

**Code Quality Score: 18 / 20 (90%)**

---

## Test Quality (10 checks, max 20)

| ID | Check | Score | Evidence |
|----|-------|-------|----------|
| TST-01 | Unit test files | 2 | 146 unit test files. |
| TST-02 | Integration test files | 2 | 16 integration test files with 121 `@Test` methods. |
| TST-03 | Real database in integration tests | 2 | Testcontainers PostgreSQL and Kafka (3 references). H2 for unit tests. |
| TST-04 | Source-to-test ratio | 2 | 146 unit test files covering services, controllers, and security. Strong coverage. |
| TST-05 | Code coverage (JaCoCo) | 2 | JaCoCo 0.8.14 configured for coverage reports. |
| TST-06 | Test configuration | 2 | `application-test.yml` (H2, Kafka disabled) and `application-integration.yml` (Testcontainers PostgreSQL). |
| TST-07 | Security tests | 2 | 185 references to security testing (`@WithMockUser`, `authentication()`, `SecurityContext`). |
| TST-08 | Test framework compatibility | 2 | Mockito 5.21.0 and ByteBuddy 1.18.4 for Java 25 compatibility. `--add-opens` args in Surefire. |
| TST-09 | WebMvcTest pattern | 2 | `@WebMvcTest` + `@Import(TestSecurityConfig)` + `@EnableMethodSecurity`. Proper filter chain handling with `FilterRegistrationBean` disablement. |
| TST-10 | Total test methods | 2 | 2431 unit + 121 integration = **2552 total `@Test` methods**. |

**Test Quality Score: 20 / 20 (100%)**

---

## Infrastructure (6 checks, max 12)

| ID | Check | Score | Evidence |
|----|-------|-------|----------|
| INF-01 | Non-root Dockerfile | 2 | Dockerfile uses `appuser`/`appgroup`. Application runs as non-root. |
| INF-02 | DB ports localhost only | 2 | PostgreSQL bound to `127.0.0.1:5432` in `docker-compose.yml`. Not exposed to external interfaces. |
| INF-03 | Environment variables for prod secrets | 2 | 9+ environment variables: `DATABASE_URL`, `JWT_SECRET`, `ENCRYPTION_KEY`, S3 config, MAIL config. Secrets not hardcoded. |
| INF-04 | Structured logging | 2 | Logstash Logback Encoder in prod profile. JSON format for log aggregation. |
| INF-05 | Health check endpoint | 2 | `/api/v1/health` (public) and `/api/v1/courier/health` (public). |
| INF-06 | CI/CD pipeline | 0 | **No pipeline files detected.** No GitHub Actions, Jenkins, GitLab CI, or any CI/CD configuration. |

**Infrastructure Score: 10 / 12 (83%)**

---

## Scorecard Summary

```
Category             | Score | Max |    %
---------------------+-------+-----+------
Security             |    19 |  20 |  95%
Data Integrity       |    15 |  16 |  94%
API Quality          |    16 |  16 | 100%
Code Quality         |    18 |  20 |  90%
Test Quality         |    20 |  20 | 100%
Infrastructure       |    10 |  12 |  83%
---------------------+-------+-----+------
OVERALL              |    98 | 104 |  94%

Grade: A
```

---

## Blocking Issues (Score 0)

| ID | Check | Impact | Recommendation |
|----|-------|--------|----------------|
| INF-06 | CI/CD pipeline | No automated build, test, or deployment pipeline. Manual processes are error-prone and do not scale. | Create a GitHub Actions workflow with build, test (unit + integration), JaCoCo coverage gate, and Docker image publish stages. |

---

## Improvement Recommendations

### Priority 1 -- Blocking

1. **INF-06: Add CI/CD pipeline.** No GitHub Actions, Jenkins, or any pipeline configuration exists. This is the single largest operational gap. A minimal pipeline should: compile, run all 2552 tests, enforce JaCoCo coverage thresholds, build the Docker image, and push to a container registry. Estimated effort: single pass.

### Priority 2 -- High Value

2. **SEC-04: Extend rate limiting beyond auth endpoints.** Currently only `/api/v1/auth/**` is rate-limited (10 req/60s/IP). All mutation endpoints and data-intensive GET endpoints should have rate limits to prevent API abuse and data exfiltration. Consider a tiered approach: stricter limits on auth, moderate on mutations, lighter on reads.

3. **CQ-09: Increase Javadoc coverage to 100%.** Class-level documentation is at approximately 40%. CONVENTIONS.md mandates documentation on every class and public method (excluding DTOs, entities, and generated code). A single documentation pass across all services, controllers, security classes, mappers, and configuration classes would close this gap.

4. **DAT-04: Expand optimistic locking.** Only 5 of 23 entities have `@Version`. Consider adding `@Version` to entities with high concurrent-write potential: `Project`, `Team`, `TeamMember`, `Connection`, `AlertChannel`, `Persona`, `Directive`. Low-contention entities (audit logs, system settings) can remain without.

### Priority 3 -- Incremental

5. **CQ-03: Resolve remaining TODOs.** The `EncryptionService` TODO about key rotation should be tracked and resolved. Key rotation is a production security requirement.

6. **SEC-04 (supplement): Move rate limiting storage to Redis.** The current in-memory `ConcurrentHashMap` does not survive restarts and does not work in multi-instance deployments. Redis (already in the Docker Compose stack) is the natural choice.

7. **SEC-03 (supplement): Move token blacklist to Redis.** Same reasoning as rate limiting -- the in-memory `ConcurrentHashMap` blacklist is lost on restart, meaning revoked tokens become valid again after a server restart. Redis provides persistence and multi-instance consistency.

---

## Category Deep Dive

### Security -- 19/20

The security posture is strong. Authentication is enforced at the class level on all controllers, eliminating the risk of unprotected endpoints slipping through. SSRF protection is thorough with RFC1918 blocking and HTTPS enforcement. AES-256-GCM encryption protects sensitive credentials. The only gap is rate limiting scope -- auth endpoints are protected, but the rest of the API surface is unguarded.

**In-memory concerns:** Both rate limiting (SEC-04) and token blacklisting (SEC-03) use `ConcurrentHashMap`. This works for single-instance development but will not survive restarts or scale to multiple instances. Both should migrate to Redis before production.

### Data Integrity -- 15/16

All enums use `STRING` storage (zero ORDINAL usage). 116 indexes and 324 not-null constraints demonstrate disciplined schema design. Pagination is enforced on all list endpoints. The only gap is partial optimistic locking -- 5 of 23 entities have `@Version`. The 5 that do (`QaJob`, `Finding`, `TechDebtItem`, `RemediationTask`, `AgentRun`) are the entities most likely to see concurrent writes from background jobs, which is a good prioritization.

### API Quality -- 16/16

Full marks. Centralized error handling with 15 exception types, consistent `ErrorResponse` format, correlation IDs via `X-Correlation-ID`, proper HTTP status codes, Jakarta Validation on all inputs, and Swagger/OpenAPI documentation. This is a model API layer.

### Code Quality -- 18/20

Clean architecture with a consistent 4-module pattern. Zero `getReferenceById` calls, all `LAZY` fetch types, proper `@Transactional` usage, and a well-structured exception hierarchy. The two gaps are Javadoc coverage (40%) and 2 residual TODOs. Neither is a functional issue, but both violate CONVENTIONS.md mandates.

### Test Quality -- 20/20

Full marks. 2552 test methods across 162 files. Strong security test coverage (185 references). Proper test infrastructure with Testcontainers for integration tests, H2 for unit tests, and dedicated test configuration profiles. Mockito and ByteBuddy versions are updated for Java 25 compatibility.

### Infrastructure -- 10/12

Non-root Docker, localhost-only DB ports, structured JSON logging in production, health endpoints, and environment variable-driven secrets. The single zero-score is the absence of any CI/CD pipeline -- no GitHub Actions, no Jenkins, no GitLab CI. This is the most impactful gap in the entire scorecard.

---

*Generated by Claude Code (Automated) -- 2026-02-24T20:09:01Z*
