# CodeOps-Server — Quality Scorecard

**Audit Date:** 2026-02-28T21:02:36Z
**Branch:** main
**Commit:** 30465ca90c67b0413d9bbfa330d1e90adc2fc91e
**Auditor:** Claude Code (Automated)

> This scorecard is NOT loaded into coding sessions. It's for project health tracking only.

---

## Security (10 checks, max 20)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| SEC-01 | BCrypt/Argon2 password encoding | 16 references (BCrypt strength 12) | 2/2 |
| SEC-02 | JWT signature validation | 12 references (HS256, jjwt 0.12.6) | 2/2 |
| SEC-03 | SQL injection prevention | 1 potential string concat (needs review) | 1/2 |
| SEC-04 | CSRF protection | Disabled (stateless JWT API — intentional) | 2/2 |
| SEC-05 | Rate limiting configured | 5 references (RateLimitFilter, 10/60s on auth) | 2/2 |
| SEC-06 | Sensitive data logging prevented | 23 potential hits (password/secret/token in log statements — needs review) | 0/2 |
| SEC-07 | Input validation on endpoints | 162 @Valid/@Validated references | 2/2 |
| SEC-08 | Authorization checks | 262 @PreAuthorize/hasRole references | 2/2 |
| SEC-09 | Secrets externalized | 0 hardcoded secrets (all via env vars in prod) | 2/2 |
| SEC-10 | HTTPS enforced in prod | HSTS header configured, no require-ssl | 1/2 |

**Security Score: 16 / 20 (80%)**

---

## Data Integrity (8 checks, max 16)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| DI-01 | Audit fields on entities | 4/107 entity files have createdAt/updatedAt (BaseEntity pattern — 22 entities inherit, but scan counts files not inheritance) | 1/2 |
| DI-02 | Optimistic locking (@Version) | 5 entities (AgentRun, Finding, QaJob, RemediationTask, TechDebtItem) | 2/2 |
| DI-03 | Cascade delete protection | 35 CascadeType references (Fleet uses CascadeType.ALL on ServiceProfile children; core uses manual FK-safe delete) | 2/2 |
| DI-04 | Unique constraints | 41 references (@Column unique, @UniqueConstraint) | 2/2 |
| DI-05 | FK constraints (JPA relationships) | 164 @ManyToOne/@OneToMany references | 2/2 |
| DI-06 | Nullable fields documented | 578 nullable=false/@NotNull references | 2/2 |
| DI-07 | Soft delete pattern | 14 references (isArchived on Project, isActive on User/connections) | 1/2 |
| DI-08 | Transaction boundaries | 428 @Transactional references | 2/2 |

**Data Integrity Score: 14 / 16 (87.5%)**

---

## API Quality (8 checks, max 16)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| API-01 | Consistent error response format | GlobalExceptionHandler @ControllerAdvice with 14 mappings | 2/2 |
| API-02 | Pagination on list endpoints | Pageable/Page used throughout | 2/2 |
| API-03 | Validation on request bodies | @Valid @RequestBody on mutation endpoints | 2/2 |
| API-04 | Proper HTTP status codes | ResponseEntity/@ResponseStatus used | 2/2 |
| API-05 | API versioning | /api/v1/ prefix on all endpoints | 2/2 |
| API-06 | Request/response logging | LoggingInterceptor on /api/**, MDC with correlationId | 2/2 |
| API-07 | HATEOAS/hypermedia | Not implemented | 0/2 |
| API-08 | OpenAPI/Swagger annotations | @Tag annotations on Courier controllers, limited elsewhere | 1/2 |

**API Quality Score: 13 / 16 (81.3%)**

---

## Code Quality (11 checks, max 22)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| CQ-01 | Constructor injection | Field: 2, Constructor: 169 (98.8% constructor) | 2/2 |
| CQ-02 | Lombok usage consistent | @Data/@Getter/@Setter/@Builder used on all entities | 2/2 |
| CQ-03 | No System.out/printStackTrace | 4 occurrences found | 1/2 |
| CQ-04 | Logging framework used | @Slf4j on all services/controllers | 2/2 |
| CQ-05 | Constants extracted | AppConstants with 300+ constants, @Value for config | 2/2 |
| CQ-06 | DTOs separate from entities | 100+ entities, 200+ DTOs (clear separation) | 2/2 |
| CQ-07 | Service layer exists | 90+ service classes | 2/2 |
| CQ-08 | Repository layer exists | 90+ repository interfaces | 2/2 |
| CQ-09 | Doc comments on classes = 100% | **FAIL (269/317 = 84.9%) — BLOCKING** | 0/2 |
| CQ-10 | Doc comments on methods = 100% | **FAIL (373/985 = 37.9%) — BLOCKING** | 0/2 |
| CQ-11 | No TODO/FIXME/placeholder/stub | **FAIL (4 found) — BLOCKING** | 0/2 |

**CQ-09, CQ-10, and CQ-11 are BLOCKING. Code Quality category scores 0.**

**Code Quality Score: 0 / 22 (0%) — BLOCKED**

### BLOCKING Issues Detail

**CQ-09 Undocumented Classes (48 files):**
Files without Javadoc `/**` on class declaration. 48 files across core config, controllers, services, and module classes need class-level Javadoc.

**CQ-10 Undocumented Methods (612 methods):**
Public methods without Javadoc `/**`. 612 of 985 public methods across services, controllers, and utility classes lack method-level documentation.

**CQ-11 TODO/FIXME/Placeholder Patterns (4 found):**
1. `EncryptionService.java:56` — TODO: Key rotation
2. `DockerConfig.java:17` — TODO
3. `RetentionExecutor.java:82` — "not yet implemented"
4. `S3StorageService.java:157` — "not yet implemented"

---

## Test Quality (12 checks, max 24)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| TST-01 | Unit test files | 225 | 2/2 |
| TST-02 | Integration test files | 16 | 2/2 |
| TST-03 | Real database in ITs | Testcontainers PostgreSQLContainer | 2/2 |
| TST-04 | Source-to-test ratio | 225 unit tests / ~90 source files = 2.5x | 2/2 |
| TST-05a | Unit test coverage = 100% | **Not measured (JaCoCo report not generated)** | 0/2 |
| TST-05b | Integration test coverage = 100% | **Not measured** | 0/2 |
| TST-05c | Combined coverage = 100% | **Not measured** | 0/2 |
| TST-06 | Test config exists | application-test.yml present | 2/2 |
| TST-07 | Security tests | @WithMockUser, authorization header tests present | 2/2 |
| TST-08 | Auth flow e2e | register/login tests in ITs | 2/2 |
| TST-09 | DB state verification in ITs | Repository/findBy usage in ITs | 2/2 |
| TST-10 | Total @Test methods | 3585 (unit + integration) | 2/2 |

**TST-05a/b/c are BLOCKING (coverage not measured). Test Quality category scores 0.**

**Test Quality Score: 0 / 24 (0%) — BLOCKED**

---

## Infrastructure (6 checks, max 12)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| INF-01 | Non-root Dockerfile | YES (appuser/appgroup) | 2/2 |
| INF-02 | DB ports localhost only | localhost:5432 in docker-compose | 2/2 |
| INF-03 | Env vars for prod secrets | 9 env vars in application-prod.yml | 2/2 |
| INF-04 | Health check endpoint | /api/v1/health (public) | 2/2 |
| INF-05 | Structured logging | LogstashEncoder in prod profile | 2/2 |
| INF-06 | CI/CD config | **None detected** | 0/2 |

**Infrastructure Score: 10 / 12 (83.3%)**

---

## Security Vulnerabilities — Snyk (5 checks, max 10)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| SNYK-01 | Zero critical dependency vulns | **FAIL (5 critical) — BLOCKING** | 0/2 |
| SNYK-02 | Zero high dependency vulns | **FAIL (28 high) — BLOCKING** | 0/2 |
| SNYK-03 | Medium/low dependency vulns | 30 found (21 medium, 9 low) | 1/2 |
| SNYK-04 | Zero code (SAST) errors | PASS (0 errors) | 2/2 |
| SNYK-05 | Zero code (SAST) warnings | PASS (0 warnings) | 2/2 |

**SNYK-01 and SNYK-02 are BLOCKING. Snyk category scores 0.**

**Snyk Score: 0 / 10 (0%) — BLOCKED**

### Snyk BLOCKING Issues

5 CRITICAL vulnerabilities:
1. `tomcat-embed-core@10.1.24` — Uncaught Exception (fix: upgrade to 9.0.96+)
2. `tomcat-embed-core@10.1.24` — TOCTOU Race Condition x2 (fix: upgrade to 9.0.98+)
3. `spring-security-web@6.3.0` — Missing Authorization (fix: upgrade to 5.7.13+)
4. `spring-security-crypto@6.3.0` — Authentication Bypass by Primary Weakness (fix: upgrade to 6.3.8+)

28 HIGH vulnerabilities across: netty (4), kafka-clients (5), tomcat-embed-core (10), commons-lang3 (1), lz4-java (2), spring-beans (1), spring-core (1), spring-webmvc (2), spring-security-config (1), spring-security-web (1).

**Root fix:** Upgrade Spring Boot from 3.3.0 to 3.4.x+ (resolves most transitives).

---

## Scorecard Summary

| Category | Score | Max | % |
|----------|-------|-----|---|
| Security | 16 | 20 | 80% |
| Data Integrity | 14 | 16 | 87.5% |
| API Quality | 13 | 16 | 81.3% |
| Code Quality | **0** | 22 | **0% (BLOCKED)** |
| Test Quality | **0** | 24 | **0% (BLOCKED)** |
| Infrastructure | 10 | 12 | 83.3% |
| Snyk Vulnerabilities | **0** | 10 | **0% (BLOCKED)** |
| **OVERALL** | **53** | **120** | **44.2%** |

**Grade: D (40-54%)**

---

## BLOCKING Issues Summary

The following issues must be resolved before the codebase can achieve a passing grade:

1. **CQ-09 (Doc Coverage — Classes):** 48 classes missing Javadoc. Must reach 100%.
2. **CQ-10 (Doc Coverage — Methods):** 612 public methods missing Javadoc. Must reach 100%.
3. **CQ-11 (TODO/FIXME/Stubs):** 4 incomplete implementations found. Must reach 0.
4. **TST-05 (Test Coverage):** JaCoCo reports not generated. Must measure and achieve 100%.
5. **SNYK-01 (Critical Vulns):** 5 critical dependency vulnerabilities. Must reach 0.
6. **SNYK-02 (High Vulns):** 28 high dependency vulnerabilities. Must reach 0.

### Priority Resolution Order

1. **Upgrade Spring Boot to 3.4.x+** — Resolves most Snyk critical/high vulnerabilities (SNYK-01, SNYK-02)
2. **Fix 4 TODO/stub patterns** — Complete or remove incomplete implementations (CQ-11)
3. **Add class-level Javadoc** to 48 undocumented classes (CQ-09)
4. **Add method-level Javadoc** to 612 undocumented methods (CQ-10)
5. **Configure JaCoCo** and achieve 100% test coverage (TST-05)
6. **Add CI/CD pipeline** (INF-06, non-blocking but important)
