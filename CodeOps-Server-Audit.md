# CodeOps-Server — Codebase Audit

**Generated:** 2026-03-02T19:14:57Z
**Commit:** `ab48bcf80d292567683063ff547f5fb11fd86a6d` — FIX-001: Add 4 adversarial AgentType values — CHAOS_MONKEY, HOSTILE_USER, COMPLIANCE_AUDITOR, LOAD_SABOTEUR
**Branch:** `main`
**Auditor:** Claude Code (claude-sonnet-4-6)

---

## 1. Project Identity

| Field | Value |
|---|---|
| Artifact | `com.codeops:codeops-server:0.1.0-SNAPSHOT` |
| Framework | Spring Boot 3.3.0 |
| Java Target | 21 (compiled with Java 25, Lombok/Mockito override versions applied) |
| Port | 8090 |
| API Prefix | `/api/v1/` |
| Active Profile (default) | `dev` |
| Source Files | 905 Java files (main) |
| Test Files | 243 Java files |
| Controllers | 72 |
| Services | 98 |
| Mappers | 55 |

---

## 2. Directory Structure

```
CodeOps-Server/
├── src/main/java/com/codeops/
│   ├── CodeOpsApplication.java           ← Entry point (@SpringBootApplication, @EnableScheduling)
│   ├── config/                           ← AppConstants, AsyncConfig, CorsConfig, DataSeeder,
│   │                                        GlobalExceptionHandler, JacksonConfig, JwtProperties,
│   │                                        KafkaConsumerConfig, LoggingInterceptor, MailProperties,
│   │                                        RequestCorrelationFilter, RestTemplateConfig, S3Config, WebMvcConfig
│   ├── controller/                       ← 18 core controllers
│   ├── dto/                              ← request/ and response/ (Java records)
│   ├── entity/                           ← 22 core entities + enums/
│   ├── exception/                        ← CodeOpsException, NotFoundException, ValidationException, AuthorizationException
│   ├── notification/                     ← EmailService, TeamsWebhookService, NotificationDispatcher
│   ├── repository/                       ← 25 core repositories
│   ├── security/                         ← SecurityConfig, JwtTokenProvider, JwtAuthFilter, RateLimitFilter, SecurityUtils
│   ├── service/                          ← 24 core services
│   ├── courier/                          ← HTTP client subsystem (Postman-like)
│   ├── fleet/                            ← Docker container management subsystem
│   ├── logger/                           ← Centralized logging/metrics subsystem
│   ├── mcp/                              ← Model Context Protocol subsystem
│   ├── registry/                         ← Service registry subsystem
│   └── relay/                            ← Real-time messaging + WebSocket subsystem
├── src/main/resources/
│   ├── application.yml                   ← name + port (8090), active profile: dev
│   ├── application-dev.yml               ← DB, JWT, encryption, CORS, S3 (disabled), mail (disabled), fleet
│   ├── application-prod.yml              ← All env vars required; ddl-auto: validate; S3+mail enabled
│   └── logback-spring.xml                ← Profile-specific: dev=plain, prod=JSON/Logstash, test=WARN
├── docker-compose.yml                    ← PostgreSQL 16, Redis 7, Zookeeper 7.5, Kafka 7.5 + topic init
└── pom.xml
```

---

## 3. Build & Dependency Manifest

**Parent:** `spring-boot-starter-parent:3.3.0`

**Version Overrides (Java 25 compatibility):**
- `lombok.version`: 1.18.42
- `mockito.version`: 5.21.0
- `byte-buddy.version`: 1.18.4

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | (SB 3.3.0) | REST API, embedded Tomcat |
| spring-boot-starter-data-jpa | (SB 3.3.0) | Hibernate ORM, Spring Data |
| spring-boot-starter-security | (SB 3.3.0) | Authentication/authorization |
| spring-boot-starter-validation | (SB 3.3.0) | Jakarta Bean Validation |
| spring-boot-starter-websocket | (SB 3.3.0) | STOMP WebSocket (Relay) |
| spring-boot-starter-mail | (SB 3.3.0) | SMTP email (MFA, notifications) |
| postgresql | (SB 3.3.0) | PostgreSQL JDBC driver (runtime) |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT generation/validation (HS256) |
| software.amazon.awssdk:s3 | 2.25.0 | S3 file storage |
| dev.samstevens.totp:totp | 1.7.1 | TOTP/HOTP MFA support |
| lombok | 1.18.42 | Code generation (provided) |
| mapstruct | 1.5.5.Final | Entity-to-DTO mapping |
| jackson-datatype-jsr310 | (SB 3.3.0) | Java time module for Jackson |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI at `/swagger-ui/index.html` |
| logstash-logback-encoder | 7.4 | JSON logging for prod |
| spring-kafka | (SB 3.3.0) | Kafka producer/consumer |
| org.graalvm.polyglot:polyglot + js | 24.1.1 | GraalVM JS engine (Courier scripts) |
| jackson-dataformat-yaml | (SB 3.3.0) | YAML parsing (Courier OpenAPI import) |
| spring-boot-starter-test | (SB 3.3.0) | JUnit 5, Mockito, AssertJ (test) |
| spring-security-test | (SB 3.3.0) | Security test support (test) |
| testcontainers:postgresql | 1.19.8 | Real DB integration tests (test) |
| testcontainers:junit-jupiter | 1.19.8 | Testcontainers JUnit 5 support (test) |
| h2 | (SB 3.3.0) | In-memory DB for unit tests (test) |
| spring-kafka-test | (SB 3.3.0) | Embedded Kafka for tests (test) |
| testcontainers:kafka | 1.19.8 | Kafka Testcontainer (test) |

**Build Plugins:**
- `spring-boot-maven-plugin` — fat JAR, excludes Lombok
- `maven-compiler-plugin` — explicit `annotationProcessorPaths` for Lombok 1.18.42 + MapStruct 1.5.5.Final (required for Java 22+)
- `maven-surefire-plugin` — `--add-opens` for java.base reflection; includes `**/*Test.java` and `**/*IT.java`
- `jacoco-maven-plugin:0.8.14` — coverage report (prepare-agent + report goals)

---

## 4. Configuration & Infrastructure Summary

**`src/main/resources/application.yml`** — Sets `spring.application.name=codeops-server`, `server.port=8090`, active profile `dev`.

**`src/main/resources/application-dev.yml`** — Key values:
- DB: `jdbc:postgresql://localhost:5432/codeops`, credentials `codeops/codeops` (with `${DB_USERNAME}/${DB_PASSWORD}` fallback)
- JPA: `ddl-auto: update`, `show-sql: true`, `open-in-view: false`
- Kafka: `bootstrap-servers: localhost:9094`, group `codeops-server`, earliest offset
- JWT: `secret: ${JWT_SECRET:dev-secret-key-...}`, 24h access, 30d refresh
- Encryption: `key: dev-only-encryption-key-minimum-32ch`
- CORS: `http://localhost:3000,http://localhost:5173`
- S3: `enabled: false` — local filesystem at `${user.home}/.codeops/storage`
- Mail: `enabled: false` — emails logged to console
- Fleet Docker: `host: tcp://localhost:2375`, API version `v1.43`
- Log levels: `com.codeops=DEBUG`, Hibernate SQL=DEBUG

**`src/main/resources/application-prod.yml`** — All configuration via env vars; `ddl-auto: validate`; S3 and mail enabled; log levels INFO/WARN.

**`src/main/resources/logback-spring.xml`** — Dev: plain console with `correlationId` and `userId` MDC fields. Prod: JSON/Logstash encoder with `correlationId`, `userId`, `teamId`, `requestPath` MDC. Test: WARN level, minimal output.

**`docker-compose.yml`** — Services: `codeops-db` (PostgreSQL 16, port 5432, volume `codeops-postgres-data`), `codeops-redis` (Redis 7, port 6379, AOF persistence), `codeops-zookeeper` (7.5.0, port 2181), `codeops-kafka` (7.5.0, port 9092, `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`). A `kafka-init` service creates 10 topics at startup.

**Kafka Topics (created at startup):**
- `codeops.core.decision.created`, `.resolved`, `.escalated`
- `codeops.core.outcome.created`, `.validated`, `.invalidated`
- `codeops.core.hypothesis.created`, `.concluded`
- `codeops.integrations.sync`
- `codeops.notifications`
- All: 3 partitions, replication-factor 1

---

## 5. Startup & Runtime Behavior

`CodeOpsApplication` is annotated `@SpringBootApplication`, `@EnableConfigurationProperties({JwtProperties.class, MailProperties.class})`, `@EnableScheduling`. On startup: Hibernate validates/updates schema (`ddl-auto`), `DataSeeder` populates system personas and directives, `JwtTokenProvider.validateSecret()` enforces 32-char minimum JWT secret. Scheduling enables `MfaService.cleanupExpiredCodes()`. The filter chain executes `RequestCorrelationFilter` → `RateLimitFilter` → `McpTokenAuthFilter` → `JwtAuthFilter` → `UsernamePasswordAuthenticationFilter`.

---

## 6. Entity / Data Model Layer

All entities extend `BaseEntity` unless noted. `BaseEntity`: `@MappedSuperclass`, fields `UUID id` (`@GeneratedValue(UUID)`), `Instant createdAt`, `Instant updatedAt` (managed by `@PrePersist`/`@PreUpdate`).

### Core Entities (`com.codeops.entity`)

**User** (`@Table("users")`): `String email` (unique, 255), `String passwordHash` (255), `String displayName` (100), `String avatarUrl` (500), `Boolean isActive=true`, `Instant lastLoginAt`, `Boolean mfaEnabled=false`, `MfaMethod mfaMethod=NONE`, `String mfaSecret` (500), `String mfaRecoveryCodes` (2000).

**Team** (`@Table("teams")`): `String name` (100), `String description` (TEXT), `User owner` (ManyToOne LAZY), `String teamsWebhookUrl` (500), `String settingsJson` (TEXT).

**TeamMember** (`@Table("team_members")`): UC on `(team_id, user_id)`, indexes on `team_id`, `user_id`. `Team team` (ManyToOne LAZY), `User user` (ManyToOne LAZY), `TeamRole role`, `Instant joinedAt`.

**Project** (`@Table("projects")`): index on `team_id`. `Team team` (ManyToOne LAZY), `String name` (200), `String description` (TEXT), `GitHubConnection githubConnection` (nullable ManyToOne LAZY), `String repoUrl` (500), `String repoFullName` (200), `String defaultBranch` (default `main`), `JiraConnection jiraConnection` (nullable ManyToOne LAZY), `String jiraProjectKey` (20), `String jiraDefaultIssueType` (default `Task`), `String jiraLabels` (TEXT/JSON), `String jiraComponent` (100), `String techStack` (200), `Integer healthScore`, `Instant lastAuditAt`, `String settingsJson` (TEXT), `Boolean isArchived=false`, `User createdBy` (ManyToOne LAZY).

**QaJob** (`@Table("qa_jobs")`): indexes on `project_id`, `started_by`. `Project project` (ManyToOne LAZY), `JobMode mode`, `JobStatus status`, `String name` (200), `String branch` (100), `String configJson` (TEXT), `String summaryMd` (TEXT), `JobResult overallResult`, `Integer healthScore`, `Integer totalFindings=0`, `Integer criticalCount=0`, `Integer highCount=0`, `Integer mediumCount=0`, `Integer lowCount=0`, `String jiraTicketKey` (50), `User startedBy` (ManyToOne LAZY), `Instant startedAt`, `Instant completedAt`, `@Version Long version`.

**AgentRun** (`@Table("agent_runs")`): index on `job_id`. `QaJob job` (ManyToOne LAZY), `AgentType agentType`, `AgentStatus status`, `AgentResult result`, `String reportS3Key` (500), `Integer score`, `Integer findingsCount=0`, `Integer criticalCount=0`, `Integer highCount=0`, `Instant startedAt`, `Instant completedAt`, `@Version Long version`.

**Finding** (`@Table("findings")`): indexes on `job_id`, `status`. `QaJob job` (ManyToOne LAZY), `AgentType agentType`, `Severity severity`, `String title` (500), `String description` (TEXT), `String filePath` (500), `Integer lineNumber`, `String recommendation` (TEXT), `String evidence` (TEXT), `Effort effortEstimate`, `DebtCategory debtCategory`, `FindingStatus status=OPEN`, `User statusChangedBy` (nullable ManyToOne LAZY), `Instant statusChangedAt`, `@Version Long version`.

**RemediationTask** (`@Table("remediation_tasks")`): index on `job_id`. `QaJob job` (ManyToOne LAZY), `Integer taskNumber`, `String title` (500), `String description` (TEXT), `String promptMd` (TEXT), `String promptS3Key` (500), `List<Finding> findings` (ManyToMany via `remediation_task_findings` join table), `Priority priority`, `TaskStatus status=PENDING`, `User assignedTo` (nullable ManyToOne LAZY), `String jiraKey` (50), `@Version Long version`.

**TechDebtItem** (`@Table("tech_debt_items")`): index on `project_id`. `Project project` (ManyToOne LAZY), `DebtCategory category`, `String title` (500), `String description` (TEXT), `String filePath` (500), `Effort effortEstimate`, `BusinessImpact businessImpact`, `DebtStatus status=IDENTIFIED`, `QaJob firstDetectedJob` (nullable ManyToOne LAZY), `QaJob resolvedJob` (nullable ManyToOne LAZY), `@Version Long version`.

**ComplianceItem** (`@Table("compliance_items")`): index on `job_id`. `QaJob job` (ManyToOne LAZY), `String requirement` (TEXT), `Specification spec` (nullable ManyToOne LAZY), `ComplianceStatus status`, `String evidence` (TEXT), `AgentType agentType`, `String notes` (TEXT).

**Specification** (`@Table("specifications")`): index on `job_id`. `QaJob job` (ManyToOne LAZY), `String name` (200), `SpecType specType`, `String s3Key` (500).

**BugInvestigation** (`@Table("bug_investigations")`): `QaJob job` (ManyToOne LAZY), `String jiraKey` (50), `String jiraSummary` (TEXT), `String jiraDescription` (TEXT), `String jiraCommentsJson` (TEXT), `String jiraAttachmentsJson` (TEXT), `String jiraLinkedIssues` (TEXT), `String additionalContext` (TEXT), `String rcaMd` (TEXT), `String impactAssessmentMd` (TEXT), `String rcaS3Key` (500), `Boolean rcaPostedToJira=false`, `Boolean fixTasksCreatedInJira=false`.

**HealthSnapshot** (`@Table("health_snapshots")`): index on `project_id`. `Project project` (ManyToOne LAZY), `QaJob job` (nullable ManyToOne LAZY), `Integer healthScore`, `String findingsBySeverity` (TEXT/JSON), `Integer techDebtScore`, `Integer dependencyScore`, `BigDecimal testCoveragePercent` (5,2), `Instant capturedAt`.

**HealthSchedule** (`@Table("health_schedules")`): index on `project_id`. `Project project` (ManyToOne LAZY), `ScheduleType scheduleType`, `String cronExpression` (50), `String agentTypes` (TEXT/JSON), `Boolean isActive=true`, `Instant lastRunAt`, `Instant nextRunAt`, `User createdBy` (ManyToOne LAZY).

**DependencyScan** (`@Table("dependency_scans")`): index on `project_id`. `Project project` (ManyToOne LAZY), `QaJob job` (nullable ManyToOne LAZY), `String manifestFile` (200), `Integer totalDependencies`, `Integer outdatedCount`, `Integer vulnerableCount`, `String scanDataJson` (TEXT).

**DependencyVulnerability** (`@Table("dependency_vulnerabilities")`): index on `scan_id`. `DependencyScan scan` (ManyToOne LAZY), `String dependencyName` (200), `String currentVersion` (50), `String fixedVersion` (50), `String cveId` (30), `Severity severity`, `String description` (TEXT), `VulnerabilityStatus status=OPEN`.

**GitHubConnection** (`@Table("github_connections")`): `Team team` (ManyToOne LAZY), `String name` (100), `GitHubAuthType authType`, `String encryptedCredentials` (TEXT), `String githubUsername` (100), `Boolean isActive=true`, `User createdBy` (ManyToOne LAZY).

**JiraConnection** (`@Table("jira_connections")`): `Team team` (ManyToOne LAZY), `String name` (100), `String instanceUrl` (500), `String email` (255), `String encryptedApiToken` (TEXT), `Boolean isActive=true`, `User createdBy` (ManyToOne LAZY).

**Invitation** (`@Table("invitations")`): indexes on `team_id`, `email`. `Team team` (ManyToOne LAZY), `String email` (255), `User invitedBy` (ManyToOne LAZY), `TeamRole role`, `String token` (100, unique), `InvitationStatus status`, `Instant expiresAt`.

**Persona** (`@Table("personas")`): index on `team_id`. `String name` (100), `AgentType agentType`, `String description` (TEXT), `String contentMd` (TEXT), `Scope scope`, `Team team` (nullable ManyToOne LAZY), `User createdBy` (ManyToOne LAZY), `Boolean isDefault=false`, `Integer version=1`.

**Directive** (`@Table("directives")`): index on `team_id`. `String name` (200), `String description` (TEXT), `String contentMd` (TEXT), `DirectiveCategory category`, `DirectiveScope scope`, `Team team` (nullable ManyToOne LAZY), `Project project` (nullable ManyToOne LAZY), `User createdBy` (ManyToOne LAZY), `Integer version=1`.

**ProjectDirective** (`@Table("project_directives")`): Composite PK — `@EmbeddedId ProjectDirectiveId` (fields: `UUID projectId`, `UUID directiveId`). Does NOT extend BaseEntity. `Project project` (`@MapsId("projectId")`), `Directive directive` (`@MapsId("directiveId")`), `Boolean enabled=true`.

**NotificationPreference** (`@Table("notification_preferences")`): UC on `(user_id, event_type)`. `User user` (ManyToOne LAZY), `String eventType` (50), `Boolean inApp=true`, `Boolean email=false`.

**MfaEmailCode** (`@Table("mfa_email_codes")`): Does NOT extend BaseEntity. `UUID id` (GenerationType.UUID), `UUID userId`, `String codeHash` (255), `Instant expiresAt`, `boolean used=false`, `Instant createdAt`. BCrypt-hashed, 10-min TTL, single-use.

**AuditLog** (`@Table("audit_log")`): Does NOT extend BaseEntity. Long PK (`IDENTITY`). `User user` (nullable ManyToOne LAZY), `Team team` (nullable ManyToOne LAZY), `String action` (50), `String entityType` (30), `UUID entityId`, `String details` (TEXT), `String ipAddress` (45), `Instant createdAt`.

**SystemSetting** (`@Table("system_settings")`): Does NOT extend BaseEntity. `String settingKey` (PK, 100), `String value` (TEXT), `User updatedBy` (nullable ManyToOne LAZY), `Instant updatedAt`.

### Courier Entities (`com.codeops.courier.entity`)

**Collection**: `User owner` (ManyToOne LAZY), `String name`, `String description`, `Boolean isShared=false`, `String settings` (TEXT). Owns `Folder`, `Request`, `CollectionShare`, `Fork`, `MergeRequest`.

**Request**: `Collection collection` (ManyToOne LAZY), `Folder folder` (nullable ManyToOne LAZY), `HttpMethod method`, `String url`, `String name`, `Integer sortOrder`. Has `RequestHeader`, `RequestParam`, `RequestBody`, `RequestAuth`, `RequestScript` children.

**RequestHistory**: `User user` (ManyToOne LAZY), `Request request` (nullable ManyToOne LAZY), `HttpMethod method`, `String url`, `Integer statusCode`, `Long durationMs`, `String requestJson` (TEXT), `String responseJson` (TEXT), `Instant executedAt`.

**Environment**: `User owner`, `Team team`, `String name`, `Boolean isActive=false`, `Boolean isShared=false`. Has `EnvironmentVariable` children.

**GlobalVariable**: `User user`, `Team team`, `String key`, `String value`, `Boolean isSecret=false`.

**RunResult** / **RunIteration**: Collection run results with pass/fail counts, duration, and per-iteration details.

**CollectionShare**: `Collection collection`, `User sharedWith`, `SharePermission permission`.

**Fork** / **MergeRequest**: Support collection branching and merge workflows.

**CodeSnippetTemplate**: Built-in code generation templates per language.

### Fleet Entities (`com.codeops.fleet.entity`)

**ContainerInstance**, **DeploymentRecord**, **ContainerHealthCheck**, **ContainerLog**, **ServiceProfile**, **SolutionProfile**, **SolutionService**, **EnvironmentVariable**, **PortMapping**, **VolumeMount**, **NetworkConfig**, **WorkstationProfile**, **WorkstationSolution**, **DeploymentContainer**.

### Logger Entities (`com.codeops.logger.entity`)

**LogEntry**, **LogSource**, **LogTrap**, **TrapCondition**, **Metric**, **MetricSeries**, **TraceSpan**, **Dashboard**, **DashboardWidget**, **AlertRule**, **AlertChannel**, **AlertHistory**, **AnomalyBaseline**, **SavedQuery**, **QueryHistory**, **RetentionPolicy**.

### MCP Entities (`com.codeops.mcp.entity`)

**McpApiToken**: SHA-256 hashed token, `String tokenHash` (64), `String scopesJson` (TEXT), `Instant expiresAt`, `TokenStatus status`, `String displayPrefix` (8).

**McpSession**: `DeveloperProfile developerProfile`, `SessionStatus status`, `McpTransport transport`, `Instant startedAt`, `Instant lastActivityAt`, `Integer toolCallCount=0`.

**DeveloperProfile**: `User user`, `Team team`, `String displayName`, `String idePreferences` (TEXT), `Boolean isActive=true`.

**ProjectDocument** / **ProjectDocumentVersion**: Document versioning with `DocumentType`, `Boolean isFlagged`.

**SessionResult** / **SessionToolCall**: Tool call history with `ToolCallStatus`, request/response JSON.

**ActivityFeedEntry**: `ActivityType`, `String description`, linked to session/document/project.

### Registry Entities (`com.codeops.registry.entity`)

**ServiceRegistration**: `String slug` (unique per team), `ServiceType serviceType`, `ServiceStatus status`, `HealthStatus healthStatus`, linked ports, dependencies.

**Solution** / **SolutionMember**: Groups services, `SolutionStatus`, `SolutionCategory`.

**PortRange** / **PortAllocation**: Port management with conflict detection.

**ServiceDependency**: `DependencyType` (REQUIRED, OPTIONAL, DEV_ONLY, CIRCULAR).

**EnvironmentConfig** / **ConfigTemplate** / **ApiRouteRegistration** / **InfraResource** / **WorkstationProfile**.

### Relay Entities (`com.codeops.relay.entity`)

**Channel**: `String name`, `String slug`, `ChannelType`, `Boolean isArchived`, `UUID teamId`, `String topic`, `UUID createdBy`. Has `ChannelMember`, `Message`, `PinnedMessage` children.

**Message**: `UUID channelId`, `UUID authorId`, `MessageType`, `String content` (TEXT), `Boolean isEdited`, `UUID parentMessageId` (nullable, threads). Has `Reaction` children.

**DirectConversation** / **DirectMessage**: DM channels with `ConversationType` (DM, GROUP).

**ChannelMember**: `MemberRole` (OWNER, MODERATOR, MEMBER).

**UserPresence**: `UUID userId`, `PresenceStatus`, `Instant lastSeen`, `String statusMessage`.

**MessageThread** / **PinnedMessage** / **Reaction** / **ReadReceipt** / **FileAttachment** / **PlatformEvent**.

---

## 7. Enum Inventory

### Core Enums (`com.codeops.entity.enums`)

| Enum | Values | Used In |
|---|---|---|
| `AgentType` | SECURITY, CODE_QUALITY, BUILD_HEALTH, COMPLETENESS (Tier 1); API_CONTRACT, TEST_COVERAGE, UI_UX, DOCUMENTATION, DATABASE, PERFORMANCE, DEPENDENCY, ARCHITECTURE (Tier 2); CHAOS_MONKEY, HOSTILE_USER, COMPLIANCE_AUDITOR, LOAD_SABOTEUR (Tier 3) | AgentRun, Finding, ComplianceItem, Persona |
| `AgentStatus` | PENDING, RUNNING, COMPLETED, FAILED | AgentRun |
| `AgentResult` | PASS, WARN, FAIL | AgentRun |
| `JobMode` | AUDIT, COMPLIANCE, BUG_INVESTIGATE, REMEDIATE, TECH_DEBT, DEPENDENCY, HEALTH_MONITOR | QaJob |
| `JobStatus` | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED | QaJob |
| `JobResult` | PASS, WARN, FAIL | QaJob |
| `Severity` | CRITICAL, HIGH, MEDIUM, LOW | Finding, DependencyVulnerability |
| `FindingStatus` | OPEN, ACKNOWLEDGED, FALSE_POSITIVE, FIXED, WONT_FIX | Finding |
| `VulnerabilityStatus` | OPEN, UPDATING, SUPPRESSED, RESOLVED | DependencyVulnerability |
| `DebtCategory` | ARCHITECTURE, CODE, TEST, DEPENDENCY, DOCUMENTATION | TechDebtItem, Finding |
| `DebtStatus` | IDENTIFIED, PLANNED, IN_PROGRESS, RESOLVED | TechDebtItem |
| `ComplianceStatus` | MET, PARTIAL, MISSING, NOT_APPLICABLE | ComplianceItem |
| `TaskStatus` | PENDING, ASSIGNED, EXPORTED, JIRA_CREATED, COMPLETED | RemediationTask |
| `Priority` | P0, P1, P2, P3 | RemediationTask |
| `Effort` | S, M, L, XL | Finding, TechDebtItem |
| `BusinessImpact` | LOW, MEDIUM, HIGH, CRITICAL | TechDebtItem |
| `TeamRole` | OWNER, ADMIN, MEMBER, VIEWER | TeamMember, Invitation |
| `InvitationStatus` | PENDING, ACCEPTED, EXPIRED | Invitation |
| `Scope` | SYSTEM, TEAM, USER | Persona |
| `DirectiveScope` | TEAM, PROJECT, USER | Directive |
| `DirectiveCategory` | ARCHITECTURE, STANDARDS, CONVENTIONS, CONTEXT, OTHER | Directive |
| `SpecType` | OPENAPI, MARKDOWN, SCREENSHOT, FIGMA | Specification |
| `ScheduleType` | DAILY, WEEKLY, ON_COMMIT | HealthSchedule |
| `MfaMethod` | NONE, TOTP, EMAIL | User |
| `GitHubAuthType` | PAT, OAUTH, SSH | GitHubConnection |

### Courier Enums (`com.codeops.courier.entity.enums`)

| Enum | Values |
|---|---|
| `AuthType` | NO_AUTH, API_KEY, BEARER_TOKEN, BASIC_AUTH, OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS, OAUTH2_IMPLICIT, OAUTH2_PASSWORD, JWT_BEARER, INHERIT_FROM_PARENT |
| `BodyType` | NONE, FORM_DATA, X_WWW_FORM_URLENCODED, RAW_JSON, RAW_XML, RAW_HTML, RAW_TEXT, RAW_YAML, BINARY, GRAPHQL |
| `HttpMethod` | GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS |
| `ScriptType` | PRE_REQUEST, POST_RESPONSE |
| `RunStatus` | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED |
| `CodeLanguage` | CURL, PYTHON_REQUESTS, JAVASCRIPT_FETCH, JAVASCRIPT_AXIOS, JAVA_HTTP_CLIENT, JAVA_OKHTTP, CSHARP_HTTP_CLIENT, GO, RUBY, PHP, SWIFT, KOTLIN |
| `SharePermission` | (values in file — VIEW, EDIT) |

### Fleet Enums (`com.codeops.fleet.entity.enums`)

`ContainerStatus`, `DeploymentAction`, `HealthStatus`, `RestartPolicy`

### Logger Enums (`com.codeops.logger.entity.enums`)

`AlertChannelType`, `AlertSeverity`, `AlertStatus`, `ConditionType`, `LogLevel`, `MetricType`, `RetentionAction`, `SpanStatus`, `TrapType`, `WidgetType`

### MCP Enums (`com.codeops.mcp.entity.enums`)

`ActivityType`, `AuthorType`, `DocumentType`, `Environment`, `McpTransport`, `SessionStatus`, `TokenStatus`, `ToolCallStatus`

### Registry Enums (`com.codeops.registry.entity.enums`)

`ConfigSource`, `ConfigTemplateType`, `DependencyType`, `HealthStatus`, `InfraResourceType`, `PortType`, `ServiceStatus`, `ServiceType`, `SolutionCategory`, `SolutionMemberRole`, `SolutionStatus`

### Relay Enums (`com.codeops.relay.entity.enums`)

`ChannelType`, `ConversationType`, `FileUploadStatus`, `MemberRole`, `MessageType`, `PlatformEventType`, `PresenceStatus`, `ReactionType`

---

## 8. Repository Layer

All repositories extend `JpaRepository<Entity, ID>` unless noted.

### Core Repositories (`com.codeops.repository`)

**UserRepository**: `Optional<User> findByEmail(String)`, `boolean existsByEmail(String)`, `List<User> findByDisplayNameContainingIgnoreCase(String)`.

**TeamRepository**: Standard CRUD.

**TeamMemberRepository**: `List<TeamMember> findByTeamId(UUID)`, `List<TeamMember> findByUserId(UUID)`, `Optional<TeamMember> findByTeamIdAndUserId(UUID, UUID)`, `boolean existsByTeamIdAndUserId(UUID, UUID)`, `long countByTeamId(UUID)`, `void deleteByTeamIdAndUserId(UUID, UUID)`.

**ProjectRepository**: `List<Project> findByTeamIdAndIsArchivedFalse(UUID)`, `Page<Project> findByTeamIdAndIsArchivedFalse(UUID, Pageable)`, `Page<Project> findByTeamId(UUID, Pageable)`, `long countByTeamId(UUID)`, `@Modifying void deleteAllByProjectId(UUID)`.

**QaJobRepository**: `Page<QaJob> findByProjectId(UUID, Pageable)`, `Page<QaJob> findByStartedById(UUID, Pageable)`, `@Modifying void deleteAllByProjectId(UUID)`.

**AgentRunRepository**: `List<AgentRun> findByJobId(UUID)`, `Optional<AgentRun> findByJobIdAndAgentType(UUID, AgentType)`, `@Modifying void deleteAllByProjectId(UUID)`.

**FindingRepository**: `Page<Finding> findByJobId(UUID, Pageable)`, `Page<Finding> findByJobIdAndSeverity(UUID, Severity, Pageable)`, `Page<Finding> findByJobIdAndAgentType(UUID, AgentType, Pageable)`, `Page<Finding> findByJobIdAndStatus(UUID, FindingStatus, Pageable)`, `Map<Severity, Long> countBySeverityGrouped(UUID)`, `@Modifying void deleteAllByProjectId(UUID)`.

**TechDebtItemRepository**: `Page<TechDebtItem> findByProjectId(UUID, Pageable)`, `Page<TechDebtItem> findByProjectIdAndStatus(UUID, DebtStatus, Pageable)`, `Page<TechDebtItem> findByProjectIdAndCategory(UUID, DebtCategory, Pageable)`, `@Modifying void deleteAllByProjectId(UUID)`.

**RemediationTaskRepository**: `Page<RemediationTask> findByJobId(UUID, Pageable)`, `Page<RemediationTask> findByAssignedToId(UUID, Pageable)`, `@Modifying void deleteJoinTableByProjectId(UUID)`, `@Modifying void deleteAllByProjectId(UUID)`.

**ComplianceItemRepository**: `Page<ComplianceItem> findByJobId(UUID, Pageable)`, `Page<ComplianceItem> findByJobIdAndStatus(UUID, ComplianceStatus, Pageable)`, `@Modifying void deleteAllByProjectId(UUID)`.

**SpecificationRepository**: `Page<Specification> findByJobId(UUID, Pageable)`, `@Modifying void deleteAllByProjectId(UUID)`.

**BugInvestigationRepository**: `Optional<BugInvestigation> findByJobId(UUID)`, `Optional<BugInvestigation> findByJiraKey(String)`, `@Modifying void deleteAllByProjectId(UUID)`.

**DependencyScanRepository**: `Page<DependencyScan> findByProjectId(UUID, Pageable)`, `Optional<DependencyScan> findFirstByProjectIdOrderByCreatedAtDesc(UUID)`, `@Modifying void deleteAllByProjectId(UUID)`.

**DependencyVulnerabilityRepository**: `Page<DependencyVulnerability> findByScanId(UUID, Pageable)`, `Page<DependencyVulnerability> findByScanIdAndSeverity(UUID, Severity, Pageable)`, `Page<DependencyVulnerability> findByScanIdAndStatus(UUID, VulnerabilityStatus, Pageable)`, `@Modifying void deleteAllByProjectId(UUID)`.

**HealthSnapshotRepository**: `Page<HealthSnapshot> findByProjectId(UUID, Pageable)`, `Optional<HealthSnapshot> findFirstByProjectIdOrderByCapturedAtDesc(UUID)`, `List<HealthSnapshot> findTopNByProjectIdOrderByCapturedAtDesc(UUID, int)`, `@Modifying void deleteAllByProjectId(UUID)`.

**HealthScheduleRepository**: `List<HealthSchedule> findByProjectId(UUID)`, `List<HealthSchedule> findByIsActiveTrue()`, `@Modifying void deleteAllByProjectId(UUID)`.

**GitHubConnectionRepository**: `List<GitHubConnection> findByTeamId(UUID)`.

**JiraConnectionRepository**: `List<JiraConnection> findByTeamId(UUID)`.

**PersonaRepository**: `Page<Persona> findByTeamId(UUID, Pageable)`, `List<Persona> findByTeamIdAndAgentType(UUID, AgentType)`, `Optional<Persona> findByTeamIdAndAgentTypeAndIsDefaultTrue(UUID, AgentType)`, `List<Persona> findByCreatedById(UUID)`, `List<Persona> findByScopeAndIsDefaultTrue(Scope)`.

**DirectiveRepository**: `List<Directive> findByTeamId(UUID)`, `List<Directive> findByProjectId(UUID)`, `List<Directive> findByTeamIdAndScope(UUID, DirectiveScope)`, `@Modifying void deleteAllByProjectId(UUID)`.

**ProjectDirectiveRepository**: `List<ProjectDirective> findByProjectId(UUID)`, `Optional<ProjectDirective> findByProjectIdAndDirectiveId(UUID, UUID)`, `@Modifying void deleteAllByProjectId(UUID)`.

**InvitationRepository**: `Optional<Invitation> findByToken(String)`, `List<Invitation> findByTeamIdAndStatus(UUID, InvitationStatus)`, `List<Invitation> findByTeamIdAndEmailAndStatusForUpdate(UUID, String, InvitationStatus)`.

**AuditLogRepository**: `Page<AuditLog> findByTeamId(UUID, Pageable)`, `Page<AuditLog> findByUserId(UUID, Pageable)`.

**MfaEmailCodeRepository**: `Optional<MfaEmailCode> findFirstByUserIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(UUID, Instant)`, `@Modifying void deleteByExpiresAtBefore(Instant)`.

**NotificationPreferenceRepository**: `List<NotificationPreference> findByUserId(UUID)`, `Optional<NotificationPreference> findByUserIdAndEventType(UUID, String)`.

**SystemSettingRepository**: Standard CRUD (String key PK).

---

## 9. Service Layer — Full Method Signatures

### Core Services (`com.codeops.service`)

**AuthService** (`@Transactional`):
- `AuthResponse register(RegisterRequest request)`
- `AuthResponse login(LoginRequest request)`
- `AuthResponse refreshToken(RefreshTokenRequest request)`
- `void changePassword(ChangePasswordRequest request)`

**UserService** (`@Transactional(readOnly=true)` class-level):
- `UserResponse getUserById(UUID id)`
- `UserResponse getUserByEmail(String email)`
- `UserResponse getCurrentUser()`
- `@Transactional UserResponse updateUser(UUID userId, UpdateUserRequest request)`
- `List<UserResponse> searchUsers(String query)`
- `@Transactional void deactivateUser(UUID userId)`
- `@Transactional void activateUser(UUID userId)`

**TeamService** (`@Transactional`):
- `TeamResponse createTeam(CreateTeamRequest request)`
- `@Transactional(readOnly=true) TeamResponse getTeam(UUID teamId)`
- `@Transactional(readOnly=true) List<TeamResponse> getTeamsForUser()`
- `TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request)`
- `void deleteTeam(UUID teamId)` — owner-only
- `@Transactional(readOnly=true) List<TeamMemberResponse> getTeamMembers(UUID teamId)`
- `TeamMemberResponse updateMemberRole(UUID teamId, UUID userId, UpdateMemberRoleRequest request)`
- `void removeMember(UUID teamId, UUID userId)` — self-removal allowed; admin needed for others
- `InvitationResponse inviteMember(UUID teamId, InviteMemberRequest request)`
- `TeamResponse acceptInvitation(String token)`
- `@Transactional(readOnly=true) List<InvitationResponse> getTeamInvitations(UUID teamId)`
- `void cancelInvitation(UUID invitationId)`

**ProjectService** (`@Transactional`):
- `ProjectResponse createProject(UUID teamId, CreateProjectRequest request)`
- `@Transactional(readOnly=true) ProjectResponse getProject(UUID projectId)`
- `@Transactional(readOnly=true) List<ProjectResponse> getProjectsForTeam(UUID teamId)`
- `@Transactional(readOnly=true) PageResponse<ProjectResponse> getAllProjectsForTeam(UUID teamId, boolean includeArchived, Pageable pageable)`
- `ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request)`
- `void archiveProject(UUID projectId)`
- `void unarchiveProject(UUID projectId)`
- `void deleteProject(UUID projectId)` — owner-only; manual cascade delete of all child records
- `void updateHealthScore(UUID projectId, int score)`

**QaJobService** (`@Transactional`):
- `JobResponse createJob(CreateJobRequest request)`
- `@Transactional(readOnly=true) JobResponse getJob(UUID jobId)`
- `@Transactional(readOnly=true) PageResponse<JobSummaryResponse> getJobsForProject(UUID projectId, Pageable pageable)`
- `@Transactional(readOnly=true) PageResponse<JobSummaryResponse> getJobsByUser(UUID userId, Pageable pageable)`
- `JobResponse updateJob(UUID jobId, UpdateJobRequest request)`
- `void deleteJob(UUID jobId)`

**AgentRunService** (`@Transactional`):
- `AgentRunResponse createAgentRun(CreateAgentRunRequest request)`
- `List<AgentRunResponse> createAgentRuns(UUID jobId, List<AgentType> agentTypes)`
- `@Transactional(readOnly=true) List<AgentRunResponse> getAgentRuns(UUID jobId)`
- `@Transactional(readOnly=true) AgentRunResponse getAgentRun(UUID agentRunId)`
- `AgentRunResponse updateAgentRun(UUID agentRunId, UpdateAgentRunRequest request)`
- `@Transactional(readOnly=true) List<AgentType> getEligibleAdversarialAgents(UUID jobId)`

**FindingService** (`@Transactional`):
- `FindingResponse createFinding(CreateFindingRequest request)`
- `List<FindingResponse> createFindings(List<CreateFindingRequest> requests)`
- `@Transactional(readOnly=true) FindingResponse getFinding(UUID findingId)`
- `@Transactional(readOnly=true) PageResponse<FindingResponse> getFindingsForJob(UUID jobId, Pageable pageable)`
- `@Transactional(readOnly=true) PageResponse<FindingResponse> getFindingsByJobAndSeverity(UUID jobId, Severity severity, Pageable pageable)`
- `@Transactional(readOnly=true) PageResponse<FindingResponse> getFindingsByJobAndAgent(UUID jobId, AgentType agentType, Pageable pageable)`
- `@Transactional(readOnly=true) PageResponse<FindingResponse> getFindingsByJobAndStatus(UUID jobId, FindingStatus status, Pageable pageable)`
- `FindingResponse updateFindingStatus(UUID findingId, UpdateFindingStatusRequest request)`
- `List<FindingResponse> bulkUpdateFindingStatus(BulkUpdateFindingsRequest request)`
- `@Transactional(readOnly=true) Map<Severity, Long> countFindingsBySeverity(UUID jobId)`

**TechDebtService** (`@Transactional`):
- `TechDebtItemResponse createTechDebtItem(CreateTechDebtItemRequest request)`
- `List<TechDebtItemResponse> createTechDebtItems(List<CreateTechDebtItemRequest> requests)`
- `@Transactional(readOnly=true) TechDebtItemResponse getTechDebtItem(UUID itemId)`
- `@Transactional(readOnly=true) PageResponse<TechDebtItemResponse> getTechDebtForProject(UUID projectId, Pageable pageable)`
- `@Transactional(readOnly=true) PageResponse<TechDebtItemResponse> getTechDebtByStatus(UUID projectId, DebtStatus status, Pageable pageable)`
- `@Transactional(readOnly=true) PageResponse<TechDebtItemResponse> getTechDebtByCategory(UUID projectId, DebtCategory category, Pageable pageable)`
- `TechDebtItemResponse updateTechDebtStatus(UUID itemId, UpdateTechDebtStatusRequest request)`
- `void deleteTechDebtItem(UUID itemId)`
- `@Transactional(readOnly=true) Map<String, Object> getDebtSummary(UUID projectId)`

**ComplianceService** (`@Transactional`):
- `SpecificationResponse createSpecification(CreateSpecificationRequest request)`
- `@Transactional(readOnly=true) PageResponse<SpecificationResponse> getSpecificationsForJob(UUID jobId, Pageable pageable)`
- `ComplianceItemResponse createComplianceItem(CreateComplianceItemRequest request)`
- `List<ComplianceItemResponse> createComplianceItems(List<CreateComplianceItemRequest> requests)`
- `@Transactional(readOnly=true) PageResponse<ComplianceItemResponse> getComplianceItemsForJob(UUID jobId, Pageable pageable)`
- `@Transactional(readOnly=true) PageResponse<ComplianceItemResponse> getComplianceItemsByStatus(UUID jobId, ComplianceStatus status, Pageable pageable)`
- `@Transactional(readOnly=true) Map<String, Object> getComplianceSummary(UUID jobId)`

**DependencyService** (`@Transactional`):
- `DependencyScanResponse createScan(CreateDependencyScanRequest request)`
- `@Transactional(readOnly=true) DependencyScanResponse getScan(UUID scanId)`
- `@Transactional(readOnly=true) PageResponse<DependencyScanResponse> getScansForProject(UUID projectId, Pageable pageable)`
- `@Transactional(readOnly=true) DependencyScanResponse getLatestScan(UUID projectId)`
- `VulnerabilityResponse addVulnerability(CreateVulnerabilityRequest request)`
- `List<VulnerabilityResponse> addVulnerabilities(List<CreateVulnerabilityRequest> requests)`
- `@Transactional(readOnly=true) PageResponse<VulnerabilityResponse> getVulnerabilities(UUID scanId, Pageable pageable)`
- `@Transactional(readOnly=true) PageResponse<VulnerabilityResponse> getVulnerabilitiesBySeverity(UUID scanId, Severity severity, Pageable pageable)`
- `@Transactional(readOnly=true) PageResponse<VulnerabilityResponse> getOpenVulnerabilities(UUID scanId, Pageable pageable)`
- `VulnerabilityResponse updateVulnerabilityStatus(UUID vulnerabilityId, VulnerabilityStatus status)`

**RemediationTaskService** (`@Transactional`):
- `TaskResponse createTask(CreateTaskRequest request)`
- `List<TaskResponse> createTasks(List<CreateTaskRequest> requests)`
- `@Transactional(readOnly=true) PageResponse<TaskResponse> getTasksForJob(UUID jobId, Pageable pageable)`
- `@Transactional(readOnly=true) TaskResponse getTask(UUID taskId)`
- `@Transactional(readOnly=true) PageResponse<TaskResponse> getTasksAssignedToUser(UUID userId, Pageable pageable)`
- `TaskResponse updateTask(UUID taskId, UpdateTaskRequest request)`
- `String uploadTaskPrompt(UUID jobId, int taskNumber, String promptMd)`

**BugInvestigationService** (`@Transactional`):
- `BugInvestigationResponse createInvestigation(CreateBugInvestigationRequest request)`
- `@Transactional(readOnly=true) BugInvestigationResponse getInvestigation(UUID investigationId)`
- `@Transactional(readOnly=true) BugInvestigationResponse getInvestigationByJob(UUID jobId)`
- `@Transactional(readOnly=true) BugInvestigationResponse getInvestigationByJiraKey(String jiraKey)`
- `BugInvestigationResponse updateInvestigation(UUID investigationId, UpdateBugInvestigationRequest request)`
- `String uploadRca(UUID jobId, String rcaMd)`

**HealthMonitorService** (`@Transactional`):
- `HealthScheduleResponse createSchedule(CreateHealthScheduleRequest request)`
- `@Transactional(readOnly=true) List<HealthScheduleResponse> getSchedulesForProject(UUID projectId)`
- `@Transactional(readOnly=true) List<HealthScheduleResponse> getActiveSchedules()`
- `HealthScheduleResponse updateSchedule(UUID scheduleId, boolean isActive)`
- `void deleteSchedule(UUID scheduleId)`
- `void markScheduleRun(UUID scheduleId)`
- `HealthSnapshotResponse createSnapshot(CreateHealthSnapshotRequest request)`
- `@Transactional(readOnly=true) PageResponse<HealthSnapshotResponse> getSnapshots(UUID projectId, Pageable pageable)`
- `@Transactional(readOnly=true) HealthSnapshotResponse getLatestSnapshot(UUID projectId)`
- `@Transactional(readOnly=true) List<HealthSnapshotResponse> getHealthTrend(UUID projectId, int limit)`

**MetricsService** (`@Transactional(readOnly=true)`):
- `ProjectMetricsResponse getProjectMetrics(UUID projectId)`
- `TeamMetricsResponse getTeamMetrics(UUID teamId)`
- `List<HealthSnapshotResponse> getHealthTrend(UUID projectId, int days)`

**PersonaService** (`@Transactional`):
- `PersonaResponse createPersona(CreatePersonaRequest request)`
- `@Transactional(readOnly=true) PersonaResponse getPersona(UUID personaId)`
- `@Transactional(readOnly=true) PageResponse<PersonaResponse> getPersonasForTeam(UUID teamId, Pageable pageable)`
- `@Transactional(readOnly=true) List<PersonaResponse> getPersonasByAgentType(UUID teamId, AgentType agentType)`
- `@Transactional(readOnly=true) PersonaResponse getDefaultPersona(UUID teamId, AgentType agentType)`
- `@Transactional(readOnly=true) List<PersonaResponse> getPersonasByUser(UUID userId)`
- `@Transactional(readOnly=true) List<PersonaResponse> getSystemPersonas()`
- `PersonaResponse updatePersona(UUID personaId, UpdatePersonaRequest request)`
- `void deletePersona(UUID personaId)`
- `PersonaResponse setAsDefault(UUID personaId)`
- `PersonaResponse removeDefault(UUID personaId)`

**DirectiveService** (`@Transactional`):
- `DirectiveResponse createDirective(CreateDirectiveRequest request)`
- `@Transactional(readOnly=true) DirectiveResponse getDirective(UUID directiveId)`
- `@Transactional(readOnly=true) List<DirectiveResponse> getDirectivesForTeam(UUID teamId)`
- `@Transactional(readOnly=true) List<DirectiveResponse> getDirectivesForProject(UUID projectId)`
- `@Transactional(readOnly=true) List<DirectiveResponse> getDirectivesByCategory(UUID teamId, DirectiveScope scope)`
- `DirectiveResponse updateDirective(UUID directiveId, UpdateDirectiveRequest request)`
- `void deleteDirective(UUID directiveId)`
- `ProjectDirectiveResponse assignToProject(AssignDirectiveRequest request)`
- `void removeFromProject(UUID projectId, UUID directiveId)`
- `@Transactional(readOnly=true) List<ProjectDirectiveResponse> getProjectDirectives(UUID projectId)`
- `@Transactional(readOnly=true) List<DirectiveResponse> getEnabledDirectivesForProject(UUID projectId)`
- `ProjectDirectiveResponse toggleProjectDirective(UUID projectId, UUID directiveId, boolean enabled)`

**GitHubConnectionService** (`@Transactional`):
- `GitHubConnectionResponse createConnection(UUID teamId, CreateGitHubConnectionRequest request)` — encrypts credentials with AES-256-GCM
- `@Transactional(readOnly=true) List<GitHubConnectionResponse> getConnections(UUID teamId)`
- `@Transactional(readOnly=true) GitHubConnectionResponse getConnection(UUID connectionId)`
- `void deleteConnection(UUID connectionId)`
- `String getDecryptedCredentials(UUID connectionId)` — internal use only; never returned via API

**JiraConnectionService** (`@Transactional`):
- `JiraConnectionResponse createConnection(UUID teamId, CreateJiraConnectionRequest request)` — encrypts API token
- `@Transactional(readOnly=true) List<JiraConnectionResponse> getConnections(UUID teamId)`
- `@Transactional(readOnly=true) JiraConnectionResponse getConnection(UUID connectionId)`
- `void deleteConnection(UUID connectionId)`
- `String getDecryptedApiToken(UUID connectionId)` — internal use only
- `JiraConnectionDetails getConnectionDetails(UUID connectionId)`

**MfaService** (`@Transactional`):
- `MfaSetupResponse setupMfa(MfaSetupRequest request)` — TOTP setup; generates TOTP secret
- `MfaStatusResponse verifyAndEnableMfa(MfaVerifyRequest request)`
- `MfaRecoveryResponse setupEmailMfa(MfaEmailSetupRequest request)`
- `MfaStatusResponse verifyEmailSetupAndEnable(MfaVerifyRequest request)`
- `AuthResponse verifyMfaLogin(MfaLoginRequest request)` — completes MFA challenge
- `void sendLoginMfaCode(MfaResendRequest request)`
- `MfaStatusResponse disableMfa(MfaSetupRequest request)`
- `MfaRecoveryResponse regenerateRecoveryCodes(MfaSetupRequest request)`
- `@Transactional(readOnly=true) MfaStatusResponse getMfaStatus()`
- `void adminResetMfa(UUID targetUserId)`
- `@Transactional void cleanupExpiredCodes()` — scheduled task

**AdminService** (`@Transactional`):
- `@Transactional(readOnly=true) Page<UserResponse> getAllUsers(Pageable pageable)`
- `@Transactional(readOnly=true) UserResponse getUserById(UUID userId)`
- `UserResponse updateUserStatus(UUID userId, AdminUpdateUserRequest request)`
- `@Transactional(readOnly=true) SystemSettingResponse getSystemSetting(String key)`
- `SystemSettingResponse updateSystemSetting(UpdateSystemSettingRequest request)`
- `@Transactional(readOnly=true) List<SystemSettingResponse> getAllSettings()`
- `@Transactional(readOnly=true) Map<String, Object> getUsageStats()`

**AuditLogService**:
- `@Async @Transactional void log(UUID userId, UUID teamId, String action, String entityType, UUID entityId, String details)`
- `@Transactional(readOnly=true) Page<AuditLogResponse> getTeamAuditLog(UUID teamId, Pageable pageable)`
- `@Transactional(readOnly=true) Page<AuditLogResponse> getUserAuditLog(UUID userId, Pageable pageable)`

**NotificationService** (`@Transactional`):
- `@Transactional(readOnly=true) List<NotificationPreferenceResponse> getPreferences(UUID userId)`
- `NotificationPreferenceResponse updatePreference(UUID userId, UpdateNotificationPreferenceRequest request)`
- `List<NotificationPreferenceResponse> updatePreferences(UUID userId, List<UpdateNotificationPreferenceRequest> requests)`
- `@Transactional(readOnly=true) boolean shouldNotify(UUID userId, String eventType, String channel)`

**EncryptionService**: AES-256-GCM. `String encrypt(String plaintext)`, `String decrypt(String encryptedBase64)`. Key injected via `@Value("${codeops.encryption.key}")`.

**TokenBlacklistService**: In-memory `ConcurrentHashMap`. `void blacklist(String jti, Instant expiry)`, `boolean isBlacklisted(String jti)`.

**ReportStorageService**: `String uploadReport(UUID jobId, AgentType agentType, String markdownContent)`, `String uploadSummaryReport(UUID jobId, String markdownContent)`, `String downloadReport(String s3Key)`, `void deleteReportsForJob(UUID jobId)`, `String uploadSpecification(UUID jobId, String fileName, byte[] fileData, String contentType)`, `byte[] downloadSpecification(String s3Key)`.

**S3StorageService**: `String upload(String key, byte[] data, String contentType)`, `byte[] download(String key)`, `void delete(String key)`, `String generatePresignedUrl(String key, Duration expiry)`. When S3 disabled in dev, uses local filesystem at `~/.codeops/storage/`.

---

## 10. Controller / API Layer — Method Signatures Only

**Base Path:** All controllers under `/api/v1/`

### `AuthController` — `/api/v1/auth`
`POST /register` → `authService.register()` | `POST /login` → `authService.login()` | `POST /refresh` → `authService.refreshToken()` | `POST /logout` (auth) | `POST /change-password` (auth) | `POST /mfa/setup` (auth) → `mfaService.setupMfa()` | `POST /mfa/verify` (auth) → `mfaService.verifyAndEnableMfa()` | `POST /mfa/login` → `mfaService.verifyMfaLogin()` | `POST /mfa/disable` (auth) | `POST /mfa/recovery-codes` (auth) | `GET /mfa/status` (auth) | `POST /mfa/setup/email` (auth) | `POST /mfa/verify-setup/email` (auth) | `POST /mfa/resend`

### `UserController` — `/api/v1/users`
`GET /me` | `GET /{id}` | `PUT /{id}` | `GET /search?q=` | `PUT /{id}/deactivate` (ADMIN/OWNER) | `PUT /{id}/activate` (ADMIN/OWNER)

### `TeamController` — `/api/v1/teams`
`POST /` | `GET /` | `GET /{teamId}` | `PUT /{teamId}` | `DELETE /{teamId}` | `GET /{teamId}/members` | `PUT /{teamId}/members/{userId}/role` | `DELETE /{teamId}/members/{userId}` | `POST /{teamId}/invitations` | `GET /{teamId}/invitations` | `DELETE /{teamId}/invitations/{invitationId}` | `POST /invitations/{token}/accept`

### `ProjectController` — `/api/v1/projects`
`POST /{teamId}` | `GET /team/{teamId}?page&size&includeArchived` | `GET /{projectId}` | `PUT /{projectId}` | `PUT /{projectId}/archive` | `PUT /{projectId}/unarchive` | `DELETE /{projectId}`

### `JobController` — `/api/v1/jobs`
`POST /` | `GET /{jobId}` | `GET /project/{projectId}` | `GET /mine` | `PUT /{jobId}` | `DELETE /{jobId}` | `POST /{jobId}/agents` | `POST /{jobId}/agents/batch` | `GET /{jobId}/agents` | `PUT /agents/{agentRunId}` | `GET /{jobId}/investigation` | `POST /{jobId}/investigation` | `PUT /investigations/{investigationId}`

### `FindingController` — `/api/v1/findings`
`POST /` | `POST /batch` | `GET /{findingId}` | `GET /job/{jobId}` | `GET /job/{jobId}/severity/{severity}` | `GET /job/{jobId}/agent/{agentType}` | `GET /job/{jobId}/status/{status}` | `GET /job/{jobId}/counts` | `PUT /{findingId}/status` | `PUT /bulk-status`

### `TechDebtController` — `/api/v1/tech-debt`
`POST /` | `POST /batch` | `GET /{itemId}` | `GET /project/{projectId}` | `GET /project/{projectId}/status/{status}` | `GET /project/{projectId}/category/{category}` | `PUT /{itemId}/status` | `DELETE /{itemId}` | `GET /project/{projectId}/summary`

### `ComplianceController` — `/api/v1/compliance`
`POST /specs` | `GET /specs/job/{jobId}` | `POST /items` | `POST /items/batch` | `GET /items/job/{jobId}` | `GET /items/job/{jobId}/status/{status}` | `GET /summary/job/{jobId}`

### `DependencyController` — `/api/v1/dependencies`
`POST /scans` | `GET /scans/{scanId}` | `GET /scans/project/{projectId}` | `GET /scans/project/{projectId}/latest` | `POST /vulnerabilities` | `POST /vulnerabilities/batch` | `GET /vulnerabilities/scan/{scanId}` | `GET /vulnerabilities/scan/{scanId}/severity/{severity}` | `GET /vulnerabilities/scan/{scanId}/open` | `PUT /vulnerabilities/{vulnerabilityId}/status`

### `TaskController` — `/api/v1/tasks`
`POST /` | `POST /batch` | `GET /job/{jobId}` | `GET /{taskId}` | `GET /assigned-to-me` | `PUT /{taskId}`

### `DirectiveController` — `/api/v1/directives`
`POST /` | `GET /{directiveId}` | `GET /team/{teamId}` | `GET /project/{projectId}` | `PUT /{directiveId}` | `DELETE /{directiveId}` | `POST /assign` | `DELETE /project/{projectId}/directive/{directiveId}` | `GET /project/{projectId}/assignments` | `GET /project/{projectId}/enabled` | `PUT /project/{projectId}/directive/{directiveId}/toggle`

### `PersonaController` — `/api/v1/personas`
`POST /` | `GET /{personaId}` | `GET /team/{teamId}` | `GET /team/{teamId}/agent/{agentType}` | `GET /team/{teamId}/default/{agentType}` | `GET /mine` | `GET /system` | `PUT /{personaId}` | `DELETE /{personaId}` | `PUT /{personaId}/set-default` | `PUT /{personaId}/remove-default`

### `IntegrationController` — `/api/v1/integrations`
`POST /github/{teamId}` | `GET /github/{teamId}` | `GET /github/{teamId}/{connectionId}` | `DELETE /github/{teamId}/{connectionId}` | `POST /jira/{teamId}` | `GET /jira/{teamId}` | `GET /jira/{teamId}/{connectionId}` | `DELETE /jira/{teamId}/{connectionId}`

### `HealthMonitorController` — `/api/v1/health-monitor`
`POST /schedules` | `GET /schedules/project/{projectId}` | `PUT /schedules/{scheduleId}` | `DELETE /schedules/{scheduleId}` | `POST /snapshots` | `GET /snapshots/project/{projectId}` | `GET /snapshots/project/{projectId}/latest` | `GET /snapshots/project/{projectId}/trend`

### `MetricsController` — `/api/v1/metrics`
`GET /project/{projectId}` | `GET /team/{teamId}` | `GET /project/{projectId}/trend`

### `ReportController` — `/api/v1/reports`
`POST /job/{jobId}/agent/{agentType}` | `POST /job/{jobId}/summary` | `GET /download?s3Key=` | `POST /job/{jobId}/spec` | `GET /spec/download?s3Key=`

### `AdminController` — `/api/v1/admin` (ADMIN/OWNER role required)
`GET /users` | `GET /users/{userId}` | `PUT /users/{userId}` | `GET /settings` | `GET /settings/{key}` | `PUT /settings` | `GET /usage` | `GET /audit-log/team/{teamId}` | `GET /audit-log/user/{userId}` | `POST /users/{userId}/reset-mfa`

### Courier Controllers — `/api/v1/courier/`
`/collections` (CRUD + duplicate, fork, forks, export, tree), `/requests` (CRUD + duplicate, move, reorder, headers, params, body, auth, scripts, send), `/environments` (CRUD + activate, clone, variables), `/folders` (CRUD + subfolders, requests, move, reorder), `/history` (list, by-user, by-method, search, delete), `/proxy/send` + `/send/{requestId}`, `/runner/start`, `/runner/results` (list, by-collection, detail, cancel, delete), `/graphql` (execute, introspect, validate, format), `/import` (postman, openapi, curl), `/codegen/generate` + `/generate/all` + `/languages`, `/variables/global` (list, save, batch, delete), `/collections/{id}/shares` (CRUD + shared-with-me), `/health`

### Relay Controllers — `/api/v1/relay/`
`/channels` (CRUD + archive, unarchive, topic, join, leave, members, pins), `/channels/{id}/messages` (send, list, get, edit, delete), `/channels/{id}/messages/{parentId}/thread`, `/channels/{id}/messages/search`, `/messages/search-all`, `/channels/{id}/messages/read`, `/messages/unread-counts`, `/channels/{id}/threads/active`, `/dm/conversations` (CRUD), `/dm/conversations/{id}/messages`, `/dm/messages`, `/presence` (update, list, team, online, dnd, offline, count), `/reactions/messages/{id}/toggle`, `/files/upload` + download + delete, `/events` (list, by-type, by-entity, undelivered, retry), `/health`

### Fleet Controllers — `/api/v1/fleet/`
`/containers` (create, stop, restart, delete, get, logs, stats, exec, list, by-status, sync), `/health` (summary, check-container, check-all, history, purge), `/images` (list, pull, delete, prune), `/networks` (list, create, delete, connect, disconnect), `/service-profiles` (CRUD + auto-generate), `/solution-profiles` (CRUD + services management + start/stop), `/volumes` (list, create, delete, prune), `/workstation-profiles` (CRUD + solutions management + start/stop), `/health`

### MCP Controllers — `/api/v1/mcp/`
`/sessions` (create, complete, get, history, mine, cancel, tool-calls), `/developers/profile` + developers list + update + tokens (CRUD), `/documents` (CRUD + by-type + versions + flagged + clear-flag), `/activity` (team, project, since), `/protocol/sse` (SSE stream), `/protocol/sse/message`, `/protocol/message`

### Registry Controllers — `/api/v1/registry/`
`/teams/{teamId}/services` + `/services/{serviceId}` (CRUD + status, clone, identity, health, by-slug), `/teams/{teamId}/solutions` + `/solutions/{solutionId}` (CRUD + detail + members + health), `/dependencies` (add, delete, graph, impact, startup-order, cycles), `/ports` (auto-allocate, allocate, by-service, by-team, map, check, conflicts, ranges, seed, update-range), `/infra-resources` (CRUD + orphans + reassign/orphan), `/routes` (add, delete, by-service, by-gateway, check), `/config/generate`, `/health/summary`, `/workstations` (CRUD + default + set-default + from-solution + refresh-startup-order), `/topology` (team, solution, neighborhood, stats)

---

## 11. Security Configuration

**Filter Execution Order:** `RequestCorrelationFilter` → `RateLimitFilter` → `McpTokenAuthFilter` → `JwtAuthFilter` → `UsernamePasswordAuthenticationFilter`

**Public Endpoints:**
- `GET /api/v1/auth/**` — all auth endpoints
- `GET /api/v1/health`
- `GET /api/v1/courier/health`
- `GET /api/v1/fleet/health`
- `GET /swagger-ui/**`, `/v3/api-docs/**`, `/v3/api-docs.yaml`
- `WS /ws/relay/**`

**Session:** `STATELESS` — no server-side session state.

**CSRF:** Disabled (stateless JWT API).

**Password Encoder:** `BCryptPasswordEncoder(strength=12)`.

**Method Security:** `@EnableMethodSecurity` — `@PreAuthorize` on controllers. Admin endpoints require `hasRole('ADMIN') or hasRole('OWNER')`.

**Security Headers:** CSP `default-src 'self'; frame-ancestors 'none'`, X-Frame-Options DENY, X-Content-Type-Options, HSTS with `includeSubDomains` and `maxAge=31536000`.

**Authentication Entry Point:** 401 Unauthorized (no redirect).

---

## 12. Custom Security Components

**`JwtTokenProvider`** (`@Component`):
- `generateToken(User user, List<String> roles)` — HS256, `expirationHours` (24h), claims: `sub=userId`, `email`, `roles`, `jti`
- `generateRefreshToken(User user)` — HS256, `refreshExpirationDays` (30d), claim: `type=refresh`, `jti`
- `generateMfaChallengeToken(User user)` — HS256, 5-minute TTL, claim: `type=mfa_challenge`
- `boolean validateToken(String token)` — verifies signature, expiry, and JTI blacklist
- `boolean isRefreshToken(String token)` — checks `type=refresh` claim
- `boolean isMfaChallengeToken(String token)` — checks `type=mfa_challenge` claim
- `UUID getUserIdFromToken(String token)`, `String getEmailFromToken(String token)`, `List<String> getRolesFromToken(String token)`
- `Claims parseClaims(String token)` — signature verification via HMAC key
- `@PostConstruct validateSecret()` — enforces 32-char minimum; throws `IllegalStateException` if invalid

**`JwtAuthFilter`** (`OncePerRequestFilter`): Extracts `Bearer` token from `Authorization` header; sets principal as `UUID` in `SecurityContextHolder`; roles mapped to `ROLE_` prefix authorities. MFA challenge tokens rejected for normal API access.

**`RateLimitFilter`** (`OncePerRequestFilter`): Applies only to `/api/v1/auth/**`. Max 10 requests/minute per IP. Uses `ConcurrentHashMap` sliding window. Returns 429 with JSON body on limit exceeded. IP resolved from `X-Forwarded-For` header or `remoteAddr`.

**`McpTokenAuthFilter`** (`OncePerRequestFilter`): Applies only to `/api/v1/mcp/**`. Detects `Bearer mcp_*` tokens. Validates via `DeveloperProfileService.validateToken()`. Builds `McpSessionContext` request attribute. Populates `SecurityContextHolder` with team role. Non-MCP tokens pass through to `JwtAuthFilter`.

**`SecurityUtils`** (static utility): `UUID getCurrentUserId()`, `boolean hasRole(String role)`, `boolean isAdmin()`.

**`McpSecurityService`** (`@Service`): `isWithinSessionLimit(UUID, int)`, `isWithinRateLimit(UUID, int)` (sliding window), `isToolAllowed(String toolName, List<String> allowedScopes)`, `validateSessionOwnership(UUID, UUID)`, `getCurrentContext(HttpServletRequest)`, `buildContextFromJwt(UUID, UUID)`, `resolveTokenScopes(String rawToken)`.

**`EncryptionService`**: AES-256-GCM. Random IV per encryption. Output is `Base64(iv + ciphertext)`. Used for GitHub PAT and Jira API token storage. Credentials never returned in API responses.

**`TokenBlacklistService`**: In-memory `ConcurrentHashMap<String, Instant>` keyed by JWT JTI. Checked on every token validation. No persistence — blacklist lost on server restart.

---

## 13. Exception Handling & Error Responses

**`GlobalExceptionHandler`** (`@RestControllerAdvice`). All handlers return `ErrorResponse(int status, String message)`.

| Exception | HTTP Status | Message |
|---|---|---|
| `EntityNotFoundException` (JPA) | 404 | "Resource not found" |
| `NotFoundException` (CodeOps) | 404 | exception message |
| `IllegalArgumentException` | 400 | "Invalid request" |
| `MethodArgumentNotValidException` | 400 | comma-joined field errors |
| `ValidationException` (CodeOps) | 400 | exception message |
| `MissingServletRequestParameterException` | 400 | "Missing required parameter: {name}" |
| `MissingRequestHeaderException` | 400 | "Missing required header: {name}" |
| `MethodArgumentTypeMismatchException` | 400 | "Invalid value for parameter '{name}': {value}" |
| `HttpMessageNotReadableException` | 400 | "Malformed request body" |
| `AccessDeniedException` (Spring) | 403 | "Access denied" |
| `AuthorizationException` (CodeOps) | 403 | exception message |
| `HttpRequestMethodNotSupportedException` | 405 | "HTTP method '{}' is not supported..." |
| `NoResourceFoundException` | 404 | "Resource not found" |
| `CodeOpsException` | 500 | "An internal error occurred" |
| `Exception` (catch-all) | 500 | "An internal error occurred" |

Internal details never exposed in 500 responses. All exceptions logged at WARN (4xx) or ERROR (5xx) level.

**Custom Exception Hierarchy:**
- `CodeOpsException extends RuntimeException` — base
- `NotFoundException extends CodeOpsException` — 3 constructors: message, (entityName, UUID id), (entityName, field, value)
- `ValidationException extends CodeOpsException`
- `AuthorizationException extends CodeOpsException`

---

## 14. Mappers / DTOs

MapStruct mappers (55 total). All annotated `@Mapper(componentModel = "spring")`.

### Core Mappers
No dedicated mapper layer for core subsystem — services perform manual mapping via private `mapToXxxResponse()` methods using Java record constructors.

### Courier Mappers (`com.codeops.courier.dto.mapper`)
`CollectionMapper`, `EnvironmentMapper`, `EnvironmentVariableMapper`, `FolderMapper`, `GlobalVariableMapper`, `RequestAuthMapper`, `RequestBodyMapper`, `RequestHeaderMapper`, `RequestHistoryMapper`, `RequestMapper`, `RequestParamMapper`, `RequestScriptMapper`, `RunResultMapper`

### Relay Mappers (`com.codeops.relay.dto.mapper`)
`ChannelMapper`, `ChannelMemberMapper`, `DirectConversationMapper`, `DirectMessageMapper`, `FileAttachmentMapper`, `MessageMapper`, `MessageThreadMapper`, `PinnedMessageMapper`, `PlatformEventMapper`, `ReactionMapper`, `UserPresenceMapper`

### Fleet Mappers (`com.codeops.fleet.dto.mapper`)
Various fleet entity mappers (per `FleetMapperTest`).

### MCP Mappers (`com.codeops.mcp.dto.mapper`)
`McpMapper` (per `McpMapperTest`).

### Registry Mappers
Registry subsystem mappers (per registry controller/service tests).

**Note:** Known `boolean isXxx` → Lombok/MapStruct mapping issue. Fields `isShared`, `isEnabled`, `isSecret`, `isActive` require explicit `@Mapping(target = "isShared", source = "shared")` on mapper methods due to JavaBeans `isXxx()` naming convention stripping the `is` prefix.

---

## 15. Utility Classes & Shared Components

**`AppConstants`** (`config/AppConstants.java`): Final class; all `public static final`. Defines: team limits (`MAX_TEAM_MEMBERS=50`, `MAX_PROJECTS_PER_TEAM=100`, `MAX_PERSONAS_PER_TEAM=50`, `MAX_DIRECTIVES_PER_PROJECT=20`), file size limits, auth constants (`JWT_EXPIRY_HOURS=24`, `REFRESH_TOKEN_EXPIRY_DAYS=30`, `MIN_PASSWORD_LENGTH=1`), S3 prefixes, QA constants, pagination defaults, port ranges, Registry constants, Logger constants, Courier constants, Relay constants, Fleet constants, MCP constants.

**`RequestCorrelationFilter`** (`config/RequestCorrelationFilter.java`): Injects `correlationId` and `userId` into MDC on each request for structured logging.

**`LoggingInterceptor`** (`config/LoggingInterceptor.java`): `HandlerInterceptor` — logs HTTP method, URI, duration, status at DEBUG level.

**`AsyncConfig`** (`config/AsyncConfig.java`): `ThreadPoolTaskExecutor` with 5 core / 20 max / 100-queue, `codeops-async-` prefix, `CallerRunsPolicy` rejection. `AsyncUncaughtExceptionHandler` logs at ERROR.

**`CorsConfig`** (`config/CorsConfig.java`): Configures allowed origins from `${codeops.cors.allowed-origins}`.

**`JacksonConfig`** (`config/JacksonConfig.java`): `LenientInstantDeserializer` — accepts ISO-8601 timestamps without timezone; interprets as UTC. Registered via `Jackson2ObjectMapperBuilderCustomizer`.

**`KafkaConsumerConfig`** (`config/KafkaConsumerConfig.java`): `ConsumerFactory` with String key/value deserializers. `ConcurrentKafkaListenerContainerFactory` with `DefaultErrorHandler` (3 retries, 1s fixed backoff).

**`DataSeeder`** (`config/DataSeeder.java`): Seeds system personas and default directives on startup.

**`HealthController`** (`config/HealthController.java`): `GET /api/v1/health` — public endpoint returning 200 OK.

**`PageResponse<T>`**: Generic record for paginated responses: `List<T> content`, `int page`, `int size`, `long totalElements`, `int totalPages`, `boolean last`.

**`SlugUtils`** (`registry/util/SlugUtils.java`): Slug generation and validation for service registry.

---

## 16. Database Schema (Live)

**Database:** PostgreSQL 16 (`public` schema). 84 tables. Managed by Hibernate `ddl-auto: update` (dev) / `validate` (prod).

**All 84 Tables:**
`agent_runs`, `alert_channels`, `alert_history`, `alert_rules`, `anomaly_baselines`, `api_route_registrations`, `audit_log`, `bug_investigations`, `channel_members`, `channels`, `code_snippet_templates`, `collection_shares`, `collections`, `compliance_items`, `config_templates`, `dashboard_widgets`, `dashboards`, `dependency_scans`, `dependency_vulnerabilities`, `direct_conversations`, `direct_messages`, `directives`, `environment_configs`, `environment_variables`, `environments`, `file_attachments`, `findings`, `folders`, `forks`, `github_connections`, `global_variables`, `health_schedules`, `health_snapshots`, `infra_resources`, `invitations`, `jira_connections`, `log_entries`, `log_sources`, `log_traps`, `merge_requests`, `message_threads`, `messages`, `metric_series`, `metrics`, `mfa_email_codes`, `notification_preferences`, `personas`, `pinned_messages`, `platform_events`, `port_allocations`, `port_ranges`, `project_directives`, `projects`, `qa_jobs`, `query_history`, `reactions`, `read_receipts`, `remediation_task_findings`, `remediation_tasks`, `request_auths`, `request_bodies`, `request_headers`, `request_history`, `request_params`, `request_scripts`, `requests`, `retention_policies`, `run_iterations`, `run_results`, `saved_queries`, `service_dependencies`, `service_registrations`, `solution_members`, `solutions`, `specifications`, `system_settings`, `team_members`, `teams`, `tech_debt_items`, `trace_spans`, `trap_conditions`, `user_presences`, `users`, `workstation_profiles`

**Notable Index Coverage:** `agent_runs(job_id)`, `audit_log(user_id, team_id)`, `compliance_items(job_id)`, `dependency_scans(project_id)`, `dependency_vulnerabilities(scan_id)`, `directives(team_id)`, `findings(job_id, status)`, `health_schedules(project_id)`, `health_snapshots(project_id)`, `invitations(team_id, email)`, `notification_preferences(user_id)`, `personas(team_id)`, `projects(team_id)`, `qa_jobs(project_id, started_by)`, `remediation_tasks(job_id)`, `tech_debt_items(project_id)`, `team_members(team_id, user_id)`.

---

## 17. Message Broker Configuration

**Kafka:** Apache Kafka 7.5.0 via Confluent CP. Bootstrap: `localhost:9094` (dev), `localhost:9092` (docker internal `29092`).

**Consumer:** `KafkaConsumerConfig` — group `codeops-server`, StringDeserializer, earliest offset, 3 retries with 1s backoff.

**Active Consumer:**
- `KafkaLogConsumer` (`logger` subsystem) — `@KafkaListener` on `codeops-logs` and/or `codeops-metrics` topics (topic from `AppConstants.KAFKA_LOG_TOPIC`, `KAFKA_METRICS_TOPIC`).

**Defined Topics (docker `kafka-init`):**
`codeops.core.decision.created`, `.resolved`, `.escalated`, `codeops.core.outcome.created`, `.validated`, `.invalidated`, `codeops.core.hypothesis.created`, `.concluded`, `codeops.integrations.sync`, `codeops.notifications`

**Note:** The defined Kafka topics in `docker-compose.yml` (`codeops.core.*`) do not match `AppConstants` topic constants (`codeops-logs`, `codeops-metrics`). The logger subsystem consumes from `AppConstants`-defined topics. The `codeops.core.*` topics appear defined for future use.

---

## 18. Cache Layer

**No caching framework configured.** No `@Cacheable`, `@CacheEvict`, or `CacheManager` beans found in source. Redis is provisioned in `docker-compose.yml` (port 6379) but is not wired into the Spring application. In-memory caching is used only for:
- `TokenBlacklistService`: `ConcurrentHashMap<String, Instant>` (JTI blacklist)
- `RateLimitFilter`: `ConcurrentHashMap<String, RateWindow>` (auth rate limiting)
- `McpSecurityService`: `ConcurrentHashMap<UUID, List<Long>>` (MCP tool call rate limiting)

---

## 19. Environment Variable Inventory

| Variable | Profile | Required | Purpose |
|---|---|---|---|
| `JWT_SECRET` | dev/prod | YES | JWT HMAC signing key (min 32 chars) |
| `ENCRYPTION_KEY` | prod | YES | AES-256-GCM encryption key |
| `DATABASE_URL` | prod | YES | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | prod | YES | DB username |
| `DATABASE_PASSWORD` | prod | YES | DB password |
| `CORS_ALLOWED_ORIGINS` | prod | YES | Comma-separated allowed origins |
| `AWS_REGION` | prod | YES | AWS region for S3 |
| `S3_BUCKET` | prod | YES | S3 bucket name |
| `MAIL_FROM_EMAIL` | prod | YES | SES sender address |
| `DB_USERNAME` | dev | NO | Dev DB username (default: `codeops`) |
| `DB_PASSWORD` | dev | NO | Dev DB password (default: `codeops`) |

**Dev Hardcoded Values (not for production):**
- `JWT_SECRET` default: `dev-secret-key-minimum-32-characters-long-for-hs256`
- `ENCRYPTION_KEY` default: `dev-only-encryption-key-minimum-32ch`
- `CORS` default: `http://localhost:3000,http://localhost:5173`

---

## 20. Service Dependency Map

```
AuthController → AuthService → UserRepository, JwtTokenProvider, PasswordEncoder,
                               TeamMemberRepository, MfaEmailCodeRepository, EmailService

TeamController → TeamService → TeamRepository, TeamMemberRepository,
                               UserRepository, InvitationRepository

ProjectController → ProjectService → ProjectRepository, TeamMemberRepository,
                                     UserRepository, TeamRepository,
                                     GitHubConnectionRepository, JiraConnectionRepository,
                                     (10 other repos for cascade delete)

JobController → QaJobService, AgentRunService, BugInvestigationService
              → QaJobRepository, AgentRunRepository, BugInvestigationRepository

FindingController → FindingService → FindingRepository

JwtAuthFilter → JwtTokenProvider → JwtProperties, TokenBlacklistService
McpTokenAuthFilter → DeveloperProfileService, McpSecurityService, TeamMemberRepository
SecurityConfig → JwtAuthFilter, McpTokenAuthFilter, RateLimitFilter, RequestCorrelationFilter

EncryptionService ← GitHubConnectionService, JiraConnectionService

AuditLogService (@Async) → AuditLogRepository, UserRepository, TeamRepository

S3StorageService ← ReportStorageService ← JobController/ReportController

Kafka Consumer: KafkaLogConsumer → LogEntry/MetricSeries persistence (Logger subsystem)
```

---

## 21. Known Technical Debt & Issues

### TODO / FIXME Scan Results

| File | Line | Issue |
|---|---|---|
| `EncryptionService.java` | 56 | `TODO: Changing key derivation invalidates existing encrypted data — requires re-encryption migration` |
| `fleet/config/DockerConfig.java` | 17 | `TODO: Add junixsocket-common dependency` for Docker Unix socket support |

**Note:** All other "placeholder" hits in the scan are in Javadoc comments describing `{{variable}}` substitution syntax in `CodeGenerationService` and `VariableService` — not actual code defects.

### Architectural Observations

1. **TokenBlacklistService is in-memory only** — JWT revocations (logout) are lost on server restart. Redis is provisioned but not wired. This is a security gap in production.

2. **Manual cascade delete in `ProjectService.deleteProject()`** — 14 manual repository delete calls in FK-safe order rather than JPA cascade configuration. Fragile — any new child entity would require a code change.

3. **Kafka topic mismatch** — Topics in `docker-compose.yml` (`codeops.core.decision.created` etc.) do not match `AppConstants` Logger topic constants (`codeops-logs`, `codeops-metrics`). The `codeops.core.*` topics appear unused by any current `@KafkaListener`.

4. **Redis provisioned but unused** — Redis container runs but no `CacheManager` or Spring Data Redis is configured. Rate limiting and token blacklist use in-memory maps instead.

5. **`AppConstants.MIN_PASSWORD_LENGTH = 1`** — Minimal password requirement (length 1) with upper, lower, digit, and special char requirements applied only in `AuthService`. Production environments may need stronger defaults.

6. **`open-in-view: false`** — Correctly disabled (no N+1 via lazy loading in presentation layer), but services use LAZY fetch throughout. Careful attention needed for any lazy-loading outside of transactions.

7. **Fleet Docker host `tcp://localhost:2375`** — Unauthenticated TCP Docker socket configured in dev. Requires TLS configuration for production.

---

## 22. Security Vulnerability Scan

**Snyk:** Not installed. Manual assessment only.

| Finding | Severity | Notes |
|---|---|---|
| `JWT_SECRET` has dev default | HIGH | Default `dev-secret-key-...` in `application-dev.yml`. Prod requires env var override. Enforced by `@PostConstruct` validation (32-char min). |
| `ENCRYPTION_KEY` dev hardcoded | HIGH | `dev-only-encryption-key-minimum-32ch` in dev profile. No enforcement beyond minimum length. |
| TokenBlacklistService in-memory | MEDIUM | Logout revocations lost on restart. Redis not wired. |
| MFA safety fallback bypass | MEDIUM | `AuthService.login()` logs ERROR but bypasses MFA if `mfaEnabled=true` but `mfaSecret=null`. Intentional defensive behavior but should alert operations. |
| Docker API unauthenticated TCP | MEDIUM | `tcp://localhost:2375` in dev config. Production must use TLS. |
| Rate limit in-memory | LOW | `RateLimitFilter` and `McpSecurityService` rate buckets reset on server restart. Redis would provide persistence and multi-instance support. |
| Swagger UI publicly accessible | INFO | `/swagger-ui/**` and `/v3/api-docs/**` are in `permitAll()`. Intentional for dev; should be restricted in prod. |
| X-Forwarded-For trusted blindly | LOW | `RateLimitFilter` trusts first `X-Forwarded-For` value without validation. Can be spoofed if no reverse proxy is present. |

**Positive Security Posture:**
- HS256 JWT with JTI blacklist support
- BCrypt strength 12 for passwords
- AES-256-GCM for credential encryption (GitHub PAT, Jira API token)
- Decrypted credentials never returned in API responses
- Per-IP rate limiting on auth endpoints
- HSTS, CSP, X-Frame-Options, X-Content-Type-Options headers configured
- MFA support: TOTP and Email OTP
- `@PreAuthorize` on all protected endpoints
- Team membership verified in service layer for all mutations
- `@Version` optimistic locking on `QaJob`, `AgentRun`, `Finding`, `RemediationTask`, `TechDebtItem`
