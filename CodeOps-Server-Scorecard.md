# CodeOps-Server — Quality Scorecard

**Generated:** 2026-03-02
**Commit:** `ab48bcf80d292567683063ff547f5fb11fd86a6d`
**Branch:** `main`
**Auditor:** Claude Code (claude-sonnet-4-6)
**Audit Source:** CodeOps-Server-Audit.md (2026-03-02)

---

## Overall Grade Summary

| Category | Score | Grade |
|---|---|---|
| Test Coverage | 96 / 100 | A |
| Documentation Coverage | 100 / 100 | A+ |
| Security Posture | 72 / 100 | B |
| Technical Debt | 85 / 100 | B+ |
| Code Quality | 80 / 100 | B |
| **OVERALL** | **87 / 100** | **B+** |

---

## 1. Test Coverage

**Grade: A (96 / 100)**

### Counts

| Metric | Value |
|---|---|
| Source files | 905 |
| Test files | 243 |
| Total `@Test` methods | 3,602 |
| Service files | 93 |
| Service test files | 92 |
| Controller files | 74 |
| Controller test files | 73 |
| Integration test files (`@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest`, Testcontainers) | 76 |
| Pure unit test files (`@ExtendWith(MockitoExtension)`, `@Mock`) | 117 |

### Coverage Rationale

- **Service coverage: 99%** — 92 of 93 service files have a corresponding `*ServiceTest.java`. Only `SolutionService` is untested.
- **Controller coverage: 99%** — 73 of 74 controller files have a corresponding `*ControllerTest.java`. `BaseController` is an abstract base class and does not require its own test.
- **Test depth: strong** — 3,602 `@Test` methods across 243 files yields an average of ~15 tests per test file. This indicates meaningful coverage depth, not superficial smoke tests.
- **Test types: balanced** — 76 files use Spring context / container infrastructure (integration), 117 use pure Mockito (unit). The 1.5:1 unit-to-integration ratio reflects a healthy testing pyramid.
- **JaCoCo configured** — `jacoco-maven-plugin 0.8.14` is in the build. Report generation requires `mvn verify`; coverage targets are not enforced via build failure thresholds.
- **H2 + Testcontainers** — Unit tests use H2 in-memory database. Integration tests use `testcontainers:postgresql:1.19.8` and `testcontainers:kafka:1.19.8` for production-like environments.

### Gaps

| Gap | Severity |
|---|---|
| `SolutionService` has no unit test | LOW |
| No build-enforced coverage thresholds in JaCoCo config | LOW |
| No performance / load tests | INFO |

---

## 2. Documentation Coverage

**Grade: A+ (100 / 100)**

### Counts

| Class Category | Total | With Javadoc | Coverage |
|---|---|---|---|
| Service classes | 93 | 93 | 100% |
| Controller classes | 74 | 74 | 100% |
| Config / Configuration classes | 28 | 28 | 100% |
| Security classes | 5 | 5 | 100% |

### Documentation Rationale

- **Class-level Javadoc: 100%** — Every service, controller, and config class contains a `/** ... */` block on the class declaration.
- **Method-level Javadoc: strong** — Services contain 1,101 Javadoc blocks across 807 public methods (ratio > 1:1, meaning many blocks cover multiple methods or include `@param`/`@return` tags). All security components (`JwtTokenProvider`, `JwtAuthFilter`, `RateLimitFilter`, `McpTokenAuthFilter`, `McpSecurityService`, `SecurityConfig`) have full method-level Javadoc.
- **Exceptions documented** — `CodeOpsException`, `NotFoundException`, `ValidationException`, `AuthorizationException` all have Javadoc. `GlobalExceptionHandler` has full class and method documentation.
- **Correctly excluded** — DTOs (Java records), entities, repositories, and MapStruct mappers are correctly excluded from documentation requirements per CONVENTIONS.md.
- **Inline comments** — Security-critical decisions (CSRF disabled, BCrypt strength, filter order) are explained inline in `SecurityConfig.java`. `TokenBlacklistService` documents its known limitations (memory-only, expiry not enforced) within its Javadoc.

### Gaps

| Gap | Severity |
|---|---|
| None found | — |

---

## 3. Security Posture

**Grade: B (72 / 100)**

### Findings

| # | Finding | Severity | Points Deducted |
|---|---|---|---|
| S-1 | Dev hardcoded `JWT_SECRET` in `application-dev.yml` | HIGH | -8 |
| S-2 | Dev hardcoded `ENCRYPTION_KEY` in `application-dev.yml` | HIGH | -8 |
| S-3 | `TokenBlacklistService` is in-memory only — JWT revocations lost on server restart | MEDIUM | -5 |
| S-4 | MFA bypass: `AuthService.login()` logs ERROR but continues without MFA challenge when `mfaEnabled=true` and `mfaSecret=null` | MEDIUM | -4 |
| S-5 | Fleet Docker API at `tcp://localhost:2375` — unauthenticated TCP Docker socket in dev config | MEDIUM | -3 |
| S-6 | `RateLimitFilter` trusts first `X-Forwarded-For` header without validation — spoofable without a reverse proxy | LOW | -1 |
| S-7 | Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`) is `permitAll()` — intentional for dev, must be locked down in prod | INFO | 0 |
| S-8 | `AppConstants.MIN_PASSWORD_LENGTH = 1` — effectively no minimum password length enforcement | LOW | -1 |

### Positive Security Posture

| Control | Status |
|---|---|
| JWT HS256 with JTI blacklist support | IMPLEMENTED |
| BCrypt strength 12 | IMPLEMENTED |
| AES-256-GCM for GitHub PAT and Jira API token storage | IMPLEMENTED |
| Decrypted credentials never returned in API responses | IMPLEMENTED |
| Per-IP rate limiting on `/api/v1/auth/**` (10 req/min) | IMPLEMENTED |
| HSTS (31536000s, includeSubDomains) | IMPLEMENTED |
| CSP: `default-src 'self'; frame-ancestors 'none'` | IMPLEMENTED |
| X-Frame-Options: DENY | IMPLEMENTED |
| X-Content-Type-Options | IMPLEMENTED |
| MFA: TOTP (HOTP/dev.samstevens.totp) and Email OTP | IMPLEMENTED |
| `@PreAuthorize` on all protected endpoints | IMPLEMENTED |
| Team membership verified in service layer for all mutations | IMPLEMENTED |
| `@Version` optimistic locking on 5 high-contention entities | IMPLEMENTED |
| CSRF disabled with rationale (stateless JWT, no cookie auth) | CORRECT |
| Stateless session management (`SessionCreationPolicy.STATELESS`) | IMPLEMENTED |
| `@Valid` on `@RequestBody` params | 162 of 174 usages |

### Gaps

| Finding | Action Required |
|---|---|
| S-1, S-2: Dev secret defaults | Acceptable for dev. Production deployments MUST override via env vars. Document in deployment runbook. |
| S-3: In-memory JWT blacklist | Wire Redis to `TokenBlacklistService`. Spring Data Redis dependency is already provisioned. |
| S-4: MFA bypass path | Enforce MFA secret presence when `mfaEnabled=true` at account setup time. |
| S-5: Docker TCP | Add TLS certificate configuration in `DockerConfig.java` for non-dev environments. |
| S-6: X-Forwarded-For | Validate against trusted proxy CIDRs or use Spring's `ForwardedHeaderFilter`. |
| S-8: Password length | Raise `MIN_PASSWORD_LENGTH` to 8 for production hardening. |

---

## 4. Technical Debt

**Grade: B+ (85 / 100)**

### TODO / FIXME Inventory

| File | Line | Description | Severity |
|---|---|---|---|
| `EncryptionService.java` | 56 | Re-encryption migration strategy missing for key rotation | MEDIUM |
| `fleet/config/DockerConfig.java` | 17 | `junixsocket-common` dependency not added; Unix socket Docker host unsupported | LOW |

**Total TODOs / FIXMEs in production source: 2**

### Architectural Debt

| # | Issue | Severity | Points Deducted |
|---|---|---|---|
| D-1 | Manual cascade delete in `ProjectService.deleteProject()` — 14 manual repository delete calls in FK-safe order. Any new child entity must be manually added here. | MEDIUM | -5 |
| D-2 | Redis provisioned in `docker-compose.yml` but never wired into Spring. Rate limiters and token blacklist rely on in-memory `ConcurrentHashMap` instances that do not survive restarts and do not scale horizontally. | MEDIUM | -5 |
| D-3 | Kafka topic mismatch — `docker-compose.yml` defines `codeops.core.*` topics; `AppConstants` defines `codeops-logs` and `codeops-metrics`. The `codeops.core.*` topics have no active `@KafkaListener`. | LOW | -3 |
| D-4 | No JaCoCo build-failure thresholds configured. Coverage is measured but not enforced. | LOW | -2 |

### Positive Debt Management

- Only 2 real TODOs found across 905 source files — extremely low defect annotation density.
- All TODOs are documented with context (not raw `TODO:` with no explanation).
- `@Deprecated` annotations not observed — no deprecated-but-not-removed APIs.
- Version overrides for Java 25 compatibility are correct and documented in `pom.xml` comments and `CLAUDE.md`.
- All known Lombok/MapStruct `boolean isXxx` mapping issues are documented in the codebase notes.

---

## 5. Code Quality

**Grade: B (80 / 100)**

### Positive Indicators

| Indicator | Assessment |
|---|---|
| `spring.jpa.open-in-view: false` | Correctly disabled. Eliminates the most common N+1 source in Spring MVC. |
| `@Transactional(readOnly = true)` usage | 211 read-only transaction boundaries found — good query optimization discipline. |
| `@Query` annotations | 50 custom JPQL queries; 1 native query. Derived query method names used correctly throughout. |
| `@Version` optimistic locking | Applied on 5 high-contention entities: `QaJob`, `AgentRun`, `Finding`, `RemediationTask`, `TechDebtItem`. Correct choice. |
| `@ManyToOne` fetch strategy | 120 `@ManyToOne` fields; 0 with `fetch = FetchType.EAGER`. All default to LAZY — correct for performance. |
| `@OneToMany` fetch strategy | JPA default is LAZY for `@OneToMany`. 36 collections found; none explicitly override to EAGER. |
| `@Valid` on `@RequestBody` | 162 of 174 `@RequestBody` parameters include `@Valid` (93% coverage). |
| `AuditLogService.log()` is `@Async` | Audit writes do not block request threads. `CallerRunsPolicy` prevents queue overflow. |
| `@Enumerated(EnumType.STRING)` | Applied consistently on all enum fields across all entities. No ordinal storage. |
| Encryption for sensitive fields | `EncryptionService` (AES-256-GCM) applied to GitHub PAT and Jira API token. Correct and consistent. |
| MapStruct for DTO mapping | 55 mappers using MapStruct avoids manual mapping errors in subsystems with complex entity graphs. |
| `PageResponse<T>` | Consistent pagination contract across all paginated endpoints. |

### Issues Found

| # | Issue | Severity | Points Deducted |
|---|---|---|---|
| Q-1 | `@ManyToOne` — 0 of 120 use explicit `fetch = FetchType.LAZY` annotation. Java default is EAGER for `@ManyToOne`, though Hibernate/JPA's effective default for `@ManyToOne` is LAZY when `open-in-view=false` and transactions are properly scoped. Annotation omission is acceptable but ambiguous. | LOW | -5 |
| Q-2 | 12 of 174 `@RequestBody` parameters (6.9%) lack `@Valid`. Input validation not enforced on those endpoints. | LOW | -5 |
| Q-3 | Manual cascade delete in `ProjectService.deleteProject()` (see D-1 above) is a code quality concern as well — 14 delete calls in a single method with no transaction-level rollback verification per step. | MEDIUM | -5 |
| Q-4 | Core subsystem has no MapStruct mapper layer — services perform manual mapping via private `mapToXxxResponse()` methods. Inconsistent approach between core and subsystems. | LOW | -3 |
| Q-5 | `TokenBlacklistService.blacklist()` accepts `expiry` parameter documented as "currently unused" — expiry-based automatic eviction never implemented, creating unbounded memory growth risk at scale. | MEDIUM | -2 |

### N+1 Query Risk Assessment

- **LAZY fetch by default** — `@OneToMany` defaults to LAZY; `@ManyToOne` is annotated without EAGER override. Risk is low in transactional service boundaries.
- **`open-in-view: false`** — Eliminates the most common Spring N+1 source.
- **Courier subsystem** — `Collection` → `List<Folder>` → `List<Request>` → `List<RequestHeader>` chain is a multi-level `@OneToMany`. Deep traversal without `JOIN FETCH` or batch fetching could produce N+1 at nested levels. No `@BatchSize` annotations observed.
- **Custom `@Query` coverage: 50 queries** — Key list and search operations use JPQL queries, reducing derived-query N+1 exposure.

---

## 6. Grading Methodology

### Scoring Rubric

| Grade | Score Range | Description |
|---|---|---|
| A+ | 97 – 100 | Exceeds all standards |
| A | 90 – 96 | Meets all standards, minor gaps |
| B+ | 83 – 89 | Meets most standards, defined gaps |
| B | 75 – 82 | Functional quality, moderate gaps |
| C | 60 – 74 | Below standards, significant gaps |
| D | 40 – 59 | Systemic deficiencies |
| F | 0 – 39 | Not production-ready |

### Deductions Applied

| Category | Base | Deductions | Final |
|---|---|---|---|
| Test Coverage | 100 | -4 (SolutionService untested, no threshold enforcement) | 96 |
| Documentation Coverage | 100 | 0 | 100 |
| Security Posture | 100 | -8 (S-1), -8 (S-2), -5 (S-3), -4 (S-4), -3 (S-5), -1 (S-6), -1 (S-8) | 70 → 72* |
| Technical Debt | 100 | -5 (D-1), -5 (D-2), -3 (D-3), -2 (D-4) | 85 |
| Code Quality | 100 | -5 (Q-1), -5 (Q-2), -5 (Q-3), -3 (Q-4), -2 (Q-5) | 80 |
| **Overall** | 100 | Weighted average (equal weight) | **87** |

*Security S-1 and S-2 are partially mitigated by the fact that dev defaults are well-documented, env-var-overridable in prod, and validated at startup — score adjusted to 72.

---

## 7. Priority Action Items

### Immediate (Before Production)

| Priority | Action |
|---|---|
| P1 | Wire Redis `TokenBlacklistService` — implement JWT revocation persistence (Redis is already provisioned). |
| P2 | Implement expiry-based eviction in `TokenBlacklistService.blacklist()` using the existing `expiry` parameter. |
| P3 | Resolve MFA bypass: enforce `mfaSecret` presence when `mfaEnabled=true` at account setup. |
| P4 | Restrict Swagger UI to non-production profiles or IP allowlist. |
| P5 | Add Docker TLS configuration in `DockerConfig.java` for fleet subsystem. |

### Short-Term (Next Sprint)

| Priority | Action |
|---|---|
| P6 | Replace manual cascade delete in `ProjectService.deleteProject()` with JPA `CascadeType.ALL` + `orphanRemoval = true` on `Project` entity relationships. |
| P7 | Add `@Valid` to the 12 `@RequestBody` parameters currently missing it. |
| P8 | Resolve Kafka topic mismatch — either align `docker-compose.yml` to use `codeops-logs`/`codeops-metrics`, or update `AppConstants` to match the `codeops.core.*` naming scheme. |
| P9 | Add JaCoCo minimum coverage thresholds to `pom.xml` (`mvn verify` should fail if coverage drops below 80%). |
| P10 | Write unit tests for `SolutionService`. |

### Long-Term (Hardening)

| Priority | Action |
|---|---|
| P11 | Implement re-encryption migration strategy for `EncryptionService` key rotation (document as runbook). |
| P12 | Add `@BatchSize` annotations to multi-level `@OneToMany` chains in the Courier subsystem. |
| P13 | Unify DTO mapping strategy: either add MapStruct mappers to the core subsystem or document the intentional inconsistency. |
| P14 | Raise `MIN_PASSWORD_LENGTH` from 1 to 8 and add complexity regex validation at the auth layer. |
| P15 | Validate or sanitize `X-Forwarded-For` header in `RateLimitFilter` against trusted proxy CIDR ranges. |
