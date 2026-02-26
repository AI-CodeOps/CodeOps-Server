# CodeOps-Server — Codebase Audit

**Audit Date:** 2026-02-26T02:33:34Z
**Branch:** main
**Commit:** ceebd53022b7fbf6e4160d868de4716aa116ed37 OpenAPI spec — 2026-02-25
**Auditor:** Claude Code (Automated)
**Purpose:** Zero-context reference for AI-assisted development
**Audit File:** CodeOps-Server-Audit.md
**Scorecard:** CodeOps-Server-Scorecard.md
**OpenAPI Spec:** CodeOps-Server-OpenAPI.yaml (generated separately)

> This audit is the source of truth for the CodeOps-Server codebase structure, entities, services, and configuration.
> The OpenAPI spec (CodeOps-Server-OpenAPI.yaml) is the source of truth for all endpoints, DTOs, and API contracts.
> An AI reading this audit + the OpenAPI spec should be able to generate accurate code
> changes, new features, tests, and fixes without filesystem access.

---

## 1. Project Identity

```
Project Name: CodeOps-Server
Repository URL: https://github.com/AI-CodeOps/CodeOps-Server.git
Primary Language / Framework: Java / Spring Boot 3.3.0
Java Version: 21 (runtime: OpenJDK 25)
Build Tool: Maven 3.9.x (via mvnw wrapper)
Current Branch: main
Latest Commit Hash: ceebd53022b7fbf6e4160d868de4716aa116ed37
Latest Commit Message: OpenAPI spec — 2026-02-25
Audit Timestamp: 2026-02-26T02:33:34Z
```

---

## 2. Directory Structure

960 project files across a single-module Spring Boot application with 5 logical domains organized by package:

```
CodeOps-Server/
├── pom.xml                              ← Maven build manifest
├── Dockerfile                           ← Production container image
├── docker-compose.yml                   ← Dev infrastructure (Postgres, Redis, Kafka)
├── start-codeops.sh                     ← Dev startup script
├── scripts/seed-codeops.sh              ← Database seeding script
├── src/main/java/com/codeops/
│   ├── CodeOpsApplication.java          ← Entry point
│   ├── config/                          ← App config, security, async, CORS, Jackson, Kafka
│   ├── controller/                      ← 17 Core REST controllers
│   ├── dto/request/                     ← Core request DTOs (Java records)
│   ├── dto/response/                    ← Core response DTOs (Java records)
│   ├── entity/                          ← 28 Core JPA entities
│   ├── entity/enums/                    ← 25 Core enums
│   ├── exception/                       ← Custom exceptions (CodeOpsException hierarchy)
│   ├── notification/                    ← Email (SES), Teams webhook, dispatcher
│   ├── repository/                      ← 26 Core Spring Data JPA repositories
│   ├── security/                        ← JWT auth, rate limiting, SecurityConfig
│   ├── service/                         ← 26 Core services
│   ├── courier/                         ← API client module (Postman-like)
│   │   ├── config/                      ← HttpClientConfig
│   │   ├── controller/                  ← 13 Courier controllers
│   │   ├── dto/mapper/                  ← 13 MapStruct mappers
│   │   ├── dto/request/                 ← Courier request DTOs
│   │   ├── dto/response/               ← Courier response DTOs
│   │   ├── entity/                      ← 18 Courier entities
│   │   ├── entity/enums/               ← 7 Courier enums
│   │   ├── repository/                  ← 18 Courier repositories
│   │   └── service/                     ← 22 Courier services
│   ├── logger/                          ← Log management module
│   │   ├── controller/                  ← 11 Logger controllers
│   │   ├── dto/mapper/                  ← 13 Logger MapStruct mappers
│   │   ├── dto/request/                 ← Logger request DTOs
│   │   ├── dto/response/               ← Logger response DTOs
│   │   ├── entity/                      ← 16 Logger entities
│   │   ├── entity/enums/               ← 10 Logger enums
│   │   ├── event/                       ← Spring event publishing
│   │   ├── repository/                  ← 16 Logger repositories
│   │   └── service/                     ← 18 Logger services
│   ├── registry/                        ← Service registry module
│   │   ├── controller/                  ← 10 Registry controllers
│   │   ├── dto/request/                 ← Registry request DTOs
│   │   ├── dto/response/               ← Registry response DTOs
│   │   ├── entity/                      ← 11 Registry entities
│   │   ├── entity/enums/               ← 11 Registry enums
│   │   ├── repository/                  ← 11 Registry repositories
│   │   ├── service/                     ← 10 Registry services
│   │   └── util/                        ← SlugUtils
│   └── relay/                           ← Team messaging module (WebSocket)
│       ├── config/                      ← RelayDataSeeder
│       ├── controller/                  ← 8 Relay controllers
│       ├── dto/mapper/                  ← 11 Relay MapStruct mappers
│       ├── dto/request/                 ← Relay request DTOs
│       ├── dto/response/               ← Relay response DTOs
│       ├── entity/                      ← 12 Relay entities
│       ├── entity/enums/               ← 8 Relay enums
│       ├── repository/                  ← 12 Relay repositories
│       ├── service/                     ← 8 Relay services
│       └── websocket/                   ← STOMP-over-WebSocket (7 files)
├── src/main/resources/
│   ├── application.yml                  ← Base config
│   ├── application-dev.yml              ← Dev profile
│   ├── application-prod.yml             ← Prod profile (env vars)
│   └── logback-spring.xml               ← Logging config
└── src/test/
    ├── java/com/codeops/               ← 195 test files (3,062 @Test methods)
    └── resources/
        ├── application-test.yml         ← Unit test config (H2)
        └── application-integration.yml  ← Integration test config (Testcontainers)
```

Single-module monolith with 5 logical domains (Core, Courier, Logger, Registry, Relay) sharing the same Spring context and database.

---

## 3. Build & Dependency Manifest

**Build file:** `pom.xml`

### Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.3.0 | REST API framework |
| spring-boot-starter-data-jpa | 3.3.0 | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.3.0 | Authentication/authorization |
| spring-boot-starter-validation | 3.3.0 | Jakarta Bean Validation |
| spring-boot-starter-websocket | 3.3.0 | STOMP-over-WebSocket |
| spring-boot-starter-mail | 3.3.0 | Email via SMTP/SES |
| spring-kafka | 3.3.0 | Kafka consumer/producer |
| postgresql | (managed) | PostgreSQL JDBC driver |
| h2 | (managed) | In-memory test database |
| jjwt-api/impl/jackson | 0.12.6 | JWT token creation/validation |
| mapstruct | 1.5.5.Final | DTO mapping code generation |
| lombok | 1.18.42 | Boilerplate reduction |
| aws-java-sdk-s3 | 2.25.0 | S3 file storage |
| aws-java-sdk-ses | 2.25.0 | SES email sending |
| springdoc-openapi | 2.5.0 | Swagger UI / OpenAPI |
| graalvm-polyglot-js | 24.1.1 | JavaScript script execution (Courier) |
| dev.samstevens.totp | 1.7.1 | TOTP MFA support |
| testcontainers-postgresql | 1.19.8 | Integration test database |
| mockito | 5.21.0 | Test mocking (Java 25 compatible) |
| byte-buddy | 1.18.4 | Mockito runtime (Java 25 compatible) |
| jacoco | 0.8.14 | Code coverage |

### Build Plugins

| Plugin | Configuration |
|---|---|
| spring-boot-maven-plugin | Excludes lombok from final JAR |
| maven-compiler-plugin | Java 21 source/target, explicit Lombok + MapStruct annotation processor paths |
| maven-surefire-plugin | --add-opens for Java 25 compatibility, argLine for JaCoCo |
| jacoco-maven-plugin | Prepare-agent + report-aggregate goals |

### Build Commands
```
Build: mvn clean package -DskipTests
Test: mvn test
Run: mvn spring-boot:run
Package: mvn clean package
```

---

## 4. Configuration & Infrastructure Summary

- **`application.yml`** — Base profile. Server port 8090, active profile `dev`, app name `codeops-server`.
- **`application-dev.yml`** — PostgreSQL `localhost:5432/codeops` (user/pass: `codeops`), Hibernate ddl-auto `update`, show-sql on, Kafka `localhost:9094`, S3 disabled, mail disabled, local storage at `~/.codeops/storage`, CORS origins `localhost:3000,5173`, debug logging.
- **`application-prod.yml`** — All secrets from env vars (`${DATABASE_URL}`, `${JWT_SECRET}`, `${ENCRYPTION_KEY}`, etc.), Hibernate ddl-auto `validate`, S3 enabled, mail enabled, INFO logging.
- **`application-test.yml`** — H2 in-memory database, Hibernate `create-drop`, Kafka listeners disabled, Flyway disabled, test JWT secret, WARN logging.
- **`application-integration.yml`** — PostgreSQL via Testcontainers (@DynamicPropertySource), Hibernate `create-drop`, WARN logging.
- **`logback-spring.xml`** — DEV: human-readable console with MDC (correlationId, userId). PROD: JSON via LogstashEncoder. TEST: WARN-level minimal.
- **`docker-compose.yml`** — PostgreSQL 16 (codeops-db, 127.0.0.1:5432), Redis 7 (codeops-redis, 6379), Zookeeper (2181), Kafka 7.5.0 (9092/29092), Kafka-init creates 5 topics.
- **`Dockerfile`** — eclipse-temurin:21-jre-alpine, non-root user `appuser`, exposes 8090.
- **`.env`** — Not present; `.env.example` not detected. All dev config is in application-dev.yml.

### Connection Map
```
Database: PostgreSQL, localhost, 5432, codeops
Cache: Redis, localhost, 6379
Message Broker: Kafka, localhost, 9092 (external) / 29092 (internal)
External APIs: GitHub API (via PAT/OAuth), Jira API (via API token), Microsoft Teams (webhooks)
Cloud Services: AWS S3 (file storage), AWS SES (email) — both disabled in dev
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

- **Entry point:** `com.codeops.CodeOpsApplication` — `@SpringBootApplication`, `@EnableConfigurationProperties({JwtProperties.class, MailProperties.class})`, `@EnableScheduling`
- **Initialization:**
  - `DataSeeder.java` (@PostConstruct, dev profile only) — seeds users, teams, projects, jobs, findings, connections, personas, directives, compliance, dependencies, tech debt, health data, Courier collections, Logger sources, Registry services, Relay channels
  - `RelayDataSeeder.java` — seeds Relay-specific data (channels, messages, reactions, DMs, presence, platform events)
  - Hibernate ddl-auto creates/updates all 84 tables
  - JwtTokenProvider validates secret key length (min 32 chars)
  - Kafka consumer config logs bootstrap servers at startup
- **Scheduled tasks:**
  - `RetentionExecutor` — daily at 2:00 AM, executes log retention policies (purge/archive by age and level)
  - Stale presence cleanup — timeout-based offline detection
- **Health check:** `GET /api/v1/health` — returns `{"status":"UP","service":"codeops-server","timestamp":"<ISO-8601>"}` (public, no auth)

---

## 6. Entity / Data Model Layer

### Core Module (28 entities)

#### BaseEntity.java (abstract superclass)
Table: (inherited) | PK: `id` UUID (GenerationType.UUID)
Fields: `id: UUID`, `createdAt: Instant` (@CreatedDate), `updatedAt: Instant` (@LastModifiedDate)
Extended by all core entities except AuditLog (Long PK), SystemSetting (String PK), MfaEmailCode (UUID, no BaseEntity)

#### User.java
Table: `users` | PK: inherited UUID
Fields: `email: String` (unique, 255), `passwordHash: String` (255), `displayName: String` (100), `avatarUrl: String` (500), `isActive: Boolean` (default true), `lastLoginAt: Instant`, `mfaEnabled: Boolean` (default false), `mfaMethod: MfaMethod`, `mfaSecret: String` (encrypted), `recoveryCodes: String` (encrypted JSON)
Relationships: None outbound
Audit: createdAt, updatedAt (via BaseEntity)

#### Team.java
Table: `teams` | PK: inherited UUID
Fields: `name: String` (100, unique), `description: String` (TEXT), `avatarUrl: String` (500), `teamsWebhookUrl: String` (1000)
Relationships: `@ManyToOne → User` (owner), `@OneToMany → TeamMember` (mappedBy team)

#### TeamMember.java
Table: `team_members` | PK: inherited UUID
Fields: `role: TeamRole` (enum STRING)
Relationships: `@ManyToOne → Team`, `@ManyToOne → User`
Unique: (team_id, user_id)

#### Project.java
Table: `projects` | PK: inherited UUID
Fields: `name: String` (200), `description: String` (TEXT), `repoUrl: String` (500), `defaultBranch: String` (100, default "main"), `language: String` (50), `healthScore: Integer`, `isArchived: Boolean` (default false)
Relationships: `@ManyToOne → Team`, `@ManyToOne → GitHubConnection`, `@ManyToOne → JiraConnection`
@Version: yes

#### QaJob.java
Table: `qa_jobs` | PK: inherited UUID | Indexes: project_id, started_by
Fields: `mode: JobMode`, `status: JobStatus`, `name: String` (200), `branch: String` (100), `configJson: String` (TEXT), `summaryMd: String` (TEXT), `overallResult: JobResult`, `healthScore: Integer`, `totalFindings/critical/high/medium/lowCount: Integer`, `jiraTicketKey: String` (50), `startedAt/completedAt: Instant`
Relationships: `@ManyToOne → Project`, `@ManyToOne → User` (startedBy)
@Version: yes

#### Finding.java
Table: `findings` | PK: inherited UUID | Indexes: job_id
Fields: `agentType: AgentType`, `severity: Severity`, `title: String` (500), `description/recommendation/filePath/lineRange/codeSnippet/evidenceUrl: String`, `status: FindingStatus` (default OPEN), `statusChangedAt: Instant`
Relationships: `@ManyToOne → QaJob`, `@ManyToOne → User` (statusChangedBy)
@Version: yes

#### AgentRun.java
Table: `agent_runs` | PK: inherited UUID | Index: job_id
Fields: `agentType: AgentType`, `status: AgentStatus`, `result: AgentResult`, `reportS3Key: String`, `score: Integer`, `findingsCount/criticalCount/highCount: Integer`, `startedAt/completedAt: Instant`
Relationships: `@ManyToOne → QaJob`
@Version: yes

#### AuditLog.java
Table: `audit_log` | PK: `id: Long` (IDENTITY) | Indexes: user_id, team_id
Fields: `action: String` (50), `entityType: String` (30), `entityId: UUID`, `details: String` (TEXT), `ipAddress: String` (45), `createdAt: Instant`
Relationships: `@ManyToOne → User`, `@ManyToOne → Team`
Note: Does NOT extend BaseEntity

#### Persona.java
Table: `personas` | PK: inherited UUID
Fields: `name: String` (100), `agentType: AgentType`, `scope: Scope`, `systemPrompt: String` (TEXT), `isDefault: Boolean`
Relationships: `@ManyToOne → Team`, `@ManyToOne → User`

#### Directive.java
Table: `directives` | PK: inherited UUID
Fields: `name: String` (100), `content: String` (TEXT), `scope: DirectiveScope`, `category: DirectiveCategory`, `version: Integer`
Relationships: `@ManyToOne → Team`, `@ManyToOne → User`

#### ProjectDirective.java
Table: `project_directives` | PK: `@EmbeddedId ProjectDirectiveId` (projectId + directiveId)
Fields: `isEnabled: Boolean` (default true)
Relationships: `@ManyToOne → Project`, `@ManyToOne → Directive`

#### Additional Core Entities (listed with table names):
- `BugInvestigation` (bug_investigations) — RCA linked to QaJob with Jira integration
- `ComplianceItem` (compliance_items) — Compliance requirements with status tracking
- `DependencyScan` (dependency_scans) — Vulnerability scan results per project
- `DependencyVulnerability` (dependency_vulnerabilities) — Individual CVEs
- `GitHubConnection` (github_connections) — Encrypted PAT/OAuth/SSH credentials
- `HealthSchedule` (health_schedules) — Automated check schedules (DAILY/WEEKLY/ON_COMMIT)
- `HealthSnapshot` (health_snapshots) — Point-in-time health measurements
- `Invitation` (invitations) — Team invitations with unique token and expiry
- `JiraConnection` (jira_connections) — Encrypted Jira API tokens
- `MfaEmailCode` (mfa_email_codes) — BCrypt-hashed email MFA codes (10-min TTL)
- `NotificationPreference` (notification_preferences) — Per-user event notification settings
- `RemediationTask` (remediation_tasks) — Fix tasks linked to findings (@Version)
- `Specification` (specifications) — Design/API spec documents (OpenAPI, Markdown, Screenshot, Figma)
- `SystemSetting` (system_settings) — Global key-value settings (String PK)
- `TechDebtItem` (tech_debt_items) — Technical debt tracking with impact/effort (@Version)

### Courier Module (18 entities)

- `Collection` (courier_collections) — Team-scoped API collections with auth, variables, scripts
- `CollectionShare` (courier_collection_shares) — Permission-based sharing (VIEWER/EDITOR/ADMIN)
- `CodeSnippetTemplate` (courier_code_snippet_templates) — Custom code generation templates
- `Environment` (courier_environments) — Named environment configurations
- `EnvironmentVariable` (courier_environment_variables) — Scoped to environment or collection
- `Folder` (courier_folders) — Nested folder structure with auth inheritance
- `Fork` (courier_forks) — Collection branching for parallel work
- `GlobalVariable` (courier_global_variables) — Team-wide variables
- `MergeRequest` (courier_merge_requests) — Fork-to-source merge with conflict tracking
- `Request` (courier_requests) — HTTP request with method, URL, headers, params, body, auth, scripts
- `RequestAuth` (courier_request_auths) — Auth config (Bearer, Basic, OAuth2, JWT, API Key)
- `RequestBody` (courier_request_bodies) — Body types (JSON, XML, form-data, GraphQL, binary)
- `RequestHeader` (courier_request_headers) — Key-value headers with enabled flag
- `RequestHistory` (courier_request_histories) — Executed request audit trail
- `RequestParam` (courier_request_params) — Query parameters with enabled flag
- `RequestScript` (courier_request_scripts) — Pre-request/post-response JavaScript
- `RunIteration` (courier_run_iterations) — Single iteration of collection run
- `RunResult` (courier_run_results) — Collection run execution results

### Logger Module (16 entities)

- `LogEntry` (log_entries) — Individual log records with team scoping, correlation/trace IDs
- `LogSource` (log_sources) — Registered services/applications sending logs
- `LogTrap` (log_traps) — Pattern-based alerting triggers (PATTERN/FREQUENCY/ABSENCE)
- `TrapCondition` (trap_conditions) — Individual trap evaluation rules
- `AlertRule` (alert_rules) — Connects traps to notification channels with throttling
- `AlertChannel` (alert_channels) — Notification targets (EMAIL/WEBHOOK/TEAMS/SLACK)
- `AlertHistory` (alert_histories) — Alert lifecycle (FIRED → ACKNOWLEDGED → RESOLVED)
- `Dashboard` (logger_dashboards) — Configurable widget grids
- `DashboardWidget` (logger_dashboard_widgets) — Widget definitions with query bindings
- `AnomalyBaseline` (anomaly_baselines) — Learned patterns for z-score anomaly detection
- `Metric` (logger_metrics) — Metric definitions (COUNTER/GAUGE/HISTOGRAM/TIMER)
- `MetricSeries` (metric_series) — Time-series data points
- `QueryHistory` (query_histories) — Query execution audit trail
- `RetentionPolicy` (retention_policies) — Log lifecycle management (PURGE/ARCHIVE)
- `SavedQuery` (saved_queries) — Reusable query definitions
- `TraceSpan` (trace_spans) — Distributed trace spans with parent-child hierarchy

### Registry Module (11 entities)

- `ServiceRegistration` (service_registrations) — Core entity with full relationships
- `Solution` (solutions) — Logical grouping of services into products
- `SolutionMember` (solution_members) — Join entity with role (CORE/SUPPORTING/INFRASTRUCTURE/EXTERNAL)
- `PortAllocation` (port_allocations) — Allocated ports with auto-allocation flag
- `PortRange` (port_ranges) — Configurable port ranges per team/type/environment
- `ServiceDependency` (service_dependencies) — Directed dependency edges
- `ApiRouteRegistration` (api_route_registrations) — Claimed API route prefixes
- `EnvironmentConfig` (registry_environment_configs) — Non-secret key-value configs
- `ConfigTemplate` (config_templates) — Generated configuration templates
- `InfraResource` (infra_resources) — Cloud/infrastructure resource tracking
- `WorkstationProfile` (workstation_profiles) — Local dev profiles

### Relay Module (12 entities)

- `Channel` (relay_channels) — PUBLIC/PRIVATE/PROJECT/SERVICE channels with slug
- `ChannelMember` (relay_channel_members) — Membership with role, lastReadAt, isMuted
- `Message` (relay_messages) — Channel messages with threads (parentId), mentions, soft delete
- `MessageThread` (relay_message_threads) — Thread metadata (reply count, participants)
- `DirectConversation` (relay_direct_conversations) — 1:1 or group DMs
- `DirectMessage` (relay_direct_messages) — Messages in direct conversations
- `Reaction` (relay_reactions) — Emoji reactions on messages
- `FileAttachment` (relay_file_attachments) — File metadata and storage paths
- `PinnedMessage` (relay_pinned_messages) — Pinned message references
- `ReadReceipt` (relay_read_receipts) — Last-read position tracking
- `UserPresence` (relay_user_presences) — Online status per user per team
- `PlatformEvent` (relay_platform_events) — Cross-module events with delivery tracking

---

## 7. Enum Inventory

### Core Enums (25)
| Enum | Values | Used In |
|------|--------|---------|
| AgentResult | PASS, WARN, FAIL | AgentRun |
| AgentStatus | PENDING, RUNNING, COMPLETED, FAILED | AgentRun |
| AgentType | SECURITY, CODE_QUALITY, BUILD_HEALTH, COMPLETENESS, API_CONTRACT, TEST_COVERAGE, UI_UX, DOCUMENTATION, DATABASE, PERFORMANCE, DEPENDENCY, ARCHITECTURE | AgentRun, Finding, Persona, ComplianceItem |
| BusinessImpact | LOW, MEDIUM, HIGH, CRITICAL | TechDebtItem |
| ComplianceStatus | MET, PARTIAL, MISSING, NOT_APPLICABLE | ComplianceItem |
| DebtCategory | ARCHITECTURE, CODE, TEST, DEPENDENCY, DOCUMENTATION | TechDebtItem |
| DebtStatus | IDENTIFIED, PLANNED, IN_PROGRESS, RESOLVED | TechDebtItem |
| DirectiveCategory | ARCHITECTURE, STANDARDS, CONVENTIONS, CONTEXT, OTHER | Directive |
| DirectiveScope | TEAM, PROJECT, USER | Directive |
| Effort | S, M, L, XL | TechDebtItem |
| FindingStatus | OPEN, ACKNOWLEDGED, FALSE_POSITIVE, FIXED, WONT_FIX | Finding |
| GitHubAuthType | PAT, OAUTH, SSH | GitHubConnection |
| InvitationStatus | PENDING, ACCEPTED, EXPIRED | Invitation |
| JobMode | AUDIT, COMPLIANCE, BUG_INVESTIGATE, REMEDIATE, TECH_DEBT, DEPENDENCY, HEALTH_MONITOR | QaJob |
| JobResult | PASS, WARN, FAIL | QaJob |
| JobStatus | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED | QaJob |
| MfaMethod | NONE, TOTP, EMAIL | User |
| Priority | P0, P1, P2, P3 | RemediationTask |
| ScheduleType | DAILY, WEEKLY, ON_COMMIT | HealthSchedule |
| Scope | SYSTEM, TEAM, USER | Persona |
| Severity | CRITICAL, HIGH, MEDIUM, LOW | Finding, DependencyVulnerability |
| SpecType | OPENAPI, MARKDOWN, SCREENSHOT, FIGMA | Specification |
| TaskStatus | PENDING, ASSIGNED, EXPORTED, JIRA_CREATED, COMPLETED | RemediationTask |
| TeamRole | OWNER, ADMIN, MEMBER, VIEWER | TeamMember, Invitation |
| VulnerabilityStatus | OPEN, UPDATING, SUPPRESSED, RESOLVED | DependencyVulnerability |

### Courier Enums (7)
| Enum | Values |
|------|--------|
| AuthType | NO_AUTH, API_KEY, BEARER_TOKEN, BASIC_AUTH, OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS, OAUTH2_IMPLICIT, OAUTH2_PASSWORD, JWT_BEARER, INHERIT_FROM_PARENT |
| BodyType | NONE, FORM_DATA, X_WWW_FORM_URLENCODED, RAW_JSON, RAW_XML, RAW_HTML, RAW_TEXT, RAW_YAML, BINARY, GRAPHQL |
| CodeLanguage | CURL, PYTHON_REQUESTS, JAVASCRIPT_FETCH, JAVASCRIPT_AXIOS, JAVA_HTTP_CLIENT, JAVA_OKHTTP, CSHARP_HTTP_CLIENT, CSHARP_REST_SHARP, GO, RUBY, PHP, SWIFT, KOTLIN |
| HttpMethod | GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS |
| RunStatus | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED |
| ScriptType | PRE_REQUEST, POST_RESPONSE |
| SharePermission | VIEWER, EDITOR, ADMIN |

### Logger Enums (10)
| Enum | Values |
|------|--------|
| LogLevel | TRACE, DEBUG, INFO, WARN, ERROR, FATAL |
| TrapType | PATTERN, FREQUENCY, ABSENCE |
| ConditionType | REGEX, KEYWORD, FREQUENCY_THRESHOLD, ABSENCE |
| AlertChannelType | EMAIL, WEBHOOK, TEAMS, SLACK |
| AlertSeverity | INFO, WARNING, CRITICAL |
| AlertStatus | FIRED, ACKNOWLEDGED, RESOLVED |
| MetricType | COUNTER, GAUGE, HISTOGRAM, TIMER |
| RetentionAction | PURGE, ARCHIVE |
| SpanStatus | OK, ERROR |
| WidgetType | LOG_STREAM, TIME_SERIES_CHART, COUNTER, GAUGE, TABLE, HEATMAP, PIE_CHART, BAR_CHART |

### Registry Enums (11)
| Enum | Values |
|------|--------|
| ConfigSource | AUTO_GENERATED, MANUAL, INHERITED, REGISTRY_DERIVED |
| ConfigTemplateType | DOCKER_COMPOSE, APPLICATION_YML, APPLICATION_PROPERTIES, ENV_FILE, TERRAFORM_MODULE, CLAUDE_CODE_HEADER, CONVENTIONS_MD, NGINX_CONF, GITHUB_ACTIONS, DOCKERFILE, MAKEFILE, README_SECTION |
| DependencyType | HTTP_REST, GRPC, KAFKA_TOPIC, DATABASE_SHARED, REDIS_SHARED, LIBRARY, GATEWAY_ROUTE, WEBSOCKET, FILE_SYSTEM, OTHER |
| HealthStatus | UP, DOWN, DEGRADED, UNKNOWN |
| InfraResourceType | S3_BUCKET, SQS_QUEUE, SNS_TOPIC, CLOUDWATCH_LOG_GROUP, IAM_ROLE, SECRETS_MANAGER_PATH, SSM_PARAMETER, RDS_INSTANCE, ELASTICACHE_CLUSTER, ECR_REPOSITORY, CLOUD_MAP_NAMESPACE, ROUTE53_RECORD, ACM_CERTIFICATE, ALB_TARGET_GROUP, ECS_SERVICE, LAMBDA_FUNCTION, DYNAMODB_TABLE, DOCKER_NETWORK, DOCKER_VOLUME, OTHER |
| PortType | HTTP_API, FRONTEND_DEV, DATABASE, REDIS, KAFKA, KAFKA_INTERNAL, ZOOKEEPER, GRPC, WEBSOCKET, DEBUG, ACTUATOR, CUSTOM |
| ServiceStatus | ACTIVE, INACTIVE, DEPRECATED, ARCHIVED |
| ServiceType | SPRING_BOOT_API, FLUTTER_WEB, FLUTTER_DESKTOP, FLUTTER_MOBILE, REACT_SPA, VUE_SPA, NEXT_JS, EXPRESS_API, FASTAPI, DOTNET_API, GO_API, LIBRARY, WORKER, GATEWAY, DATABASE_SERVICE, MESSAGE_BROKER, CACHE_SERVICE, MCP_SERVER, CLI_TOOL, OTHER |
| SolutionCategory | PLATFORM, APPLICATION, LIBRARY_SUITE, INFRASTRUCTURE, TOOLING, OTHER |
| SolutionMemberRole | CORE, SUPPORTING, INFRASTRUCTURE, EXTERNAL_DEPENDENCY |
| SolutionStatus | ACTIVE, IN_DEVELOPMENT, DEPRECATED, ARCHIVED |

### Relay Enums (8)
| Enum | Values |
|------|--------|
| ChannelType | PUBLIC, PRIVATE, PROJECT, SERVICE |
| MessageType | TEXT, SYSTEM, PLATFORM_EVENT, FILE |
| ReactionType | EMOJI |
| PresenceStatus | ONLINE, AWAY, DND, OFFLINE |
| PlatformEventType | AUDIT_COMPLETED, ALERT_FIRED, SESSION_COMPLETED, SECRET_ROTATED, CONTAINER_CRASHED, SERVICE_REGISTERED, DEPLOYMENT_COMPLETED, BUILD_COMPLETED, FINDING_CRITICAL, MERGE_REQUEST_CREATED |
| FileUploadStatus | UPLOADING, COMPLETE, FAILED |
| ConversationType | ONE_ON_ONE, GROUP |
| MemberRole | OWNER, ADMIN, MEMBER |

---

## 8. Repository Layer

### Core Repositories (26)

All extend `JpaRepository<Entity, UUID>` unless noted.

| Repository | Entity | Notable Custom Methods |
|---|---|---|
| UserRepository | User | `findByEmail(String)`, `findByDisplayNameContainingIgnoreCase(String)` |
| TeamRepository | Team | `findByOwnerId(UUID)` |
| TeamMemberRepository | TeamMember | `findByTeamIdAndUserId(UUID, UUID)`, `existsByTeamIdAndUserId(UUID, UUID)`, `findByUserId(UUID)`, `countByTeamId(UUID)` |
| ProjectRepository | Project | `findByTeamId(UUID, Pageable)`, `findByTeamIdAndIsArchived(UUID, boolean, Pageable)` |
| QaJobRepository | QaJob | `findByProjectId(UUID, Pageable)`, `findByStartedById(UUID, Pageable)` |
| FindingRepository | Finding | `findByJobId(UUID, Pageable)`, `findByJobIdAndSeverity/AgentType/Status`, `countByJobIdGroupBySeverity` |
| AgentRunRepository | AgentRun | `findByJobId(UUID)`, `findByJobIdAndAgentType(UUID, AgentType)` |
| AuditLogRepository | AuditLog (Long PK) | `findByUserId/TeamId(UUID, Pageable)` |
| PersonaRepository | Persona | `findByTeamId(UUID, Pageable)`, `findByTeamIdAndAgentType`, `findByScope(Scope)`, `findByUserId` |
| DirectiveRepository | Directive | `findByTeamId(UUID)` |
| ProjectDirectiveRepository | ProjectDirective | `findByProjectId(UUID)`, `findByProjectIdAndIsEnabled(UUID, true)`, `deleteByProjectIdAndDirectiveId` |
| InvitationRepository | Invitation | `findByToken(String)` (with @Lock PESSIMISTIC_WRITE) |
| GitHubConnectionRepository | GitHubConnection | `findByTeamIdAndIsActive(UUID, true)` |
| JiraConnectionRepository | JiraConnection | `findByTeamIdAndIsActive(UUID, true)` |
| BugInvestigationRepository | BugInvestigation | `findByJobId(UUID)` |
| ComplianceItemRepository | ComplianceItem | `findByJobId(UUID, Pageable)`, `findByJobIdAndStatus` |
| DependencyScanRepository | DependencyScan | `findByProjectIdOrderByCreatedAtDesc` |
| DependencyVulnerabilityRepository | DependencyVulnerability | `findByScanId(UUID, Pageable)`, `findByScanIdAndSeverity/Status` |
| HealthScheduleRepository | HealthSchedule | `findByProjectId(UUID)`, `findByIsActive(true)` |
| HealthSnapshotRepository | HealthSnapshot | `findByProjectId(UUID, Pageable)`, `findTopByProjectIdOrderByCapturedAtDesc` |
| MfaEmailCodeRepository | MfaEmailCode | `findByUserIdAndUsed(UUID, false)` |
| NotificationPreferenceRepository | NotificationPreference | `findByUserId(UUID)`, `findByUserIdAndEventType` |
| RemediationTaskRepository | RemediationTask | `findByJobId(UUID, Pageable)`, `findByAssignedToId(UUID, Pageable)` |
| SpecificationRepository | Specification | `findByJobId(UUID, Pageable)` |
| SystemSettingRepository | SystemSetting (String PK) | `findBySettingKey(String)` |
| TechDebtItemRepository | TechDebtItem | `findByProjectId(UUID)`, `findByProjectIdAndStatus/Category(UUID, enum, Pageable)` |

### Courier Repositories (18)
CollectionRepository, FolderRepository, RequestRepository, RequestAuthRepository, RequestBodyRepository, RequestHeaderRepository, RequestParamRepository, RequestScriptRepository, EnvironmentRepository, EnvironmentVariableRepository, GlobalVariableRepository, RequestHistoryRepository, RunResultRepository, RunIterationRepository, ForkRepository, MergeRequestRepository, CollectionShareRepository, CodeSnippetTemplateRepository

### Logger Repositories (16)
LogEntryRepository, LogSourceRepository, LogTrapRepository, TrapConditionRepository, AlertRuleRepository, AlertChannelRepository, AlertHistoryRepository, DashboardRepository, DashboardWidgetRepository, AnomalyBaselineRepository, MetricRepository, MetricSeriesRepository, QueryHistoryRepository, RetentionPolicyRepository, SavedQueryRepository, TraceSpanRepository

### Registry Repositories (11)
ServiceRegistrationRepository, SolutionRepository, SolutionMemberRepository, PortAllocationRepository, PortRangeRepository, ServiceDependencyRepository, ApiRouteRegistrationRepository, EnvironmentConfigRepository, ConfigTemplateRepository, InfraResourceRepository, WorkstationProfileRepository

### Relay Repositories (12)
ChannelRepository, ChannelMemberRepository, MessageRepository, MessageThreadRepository, DirectConversationRepository, DirectMessageRepository, ReactionRepository, FileAttachmentRepository, PinnedMessageRepository, ReadReceiptRepository, UserPresenceRepository, PlatformEventRepository

---

## 9. Service Layer — Full Method Signatures

### Core Services (26)

#### AuthService.java
Injects: UserRepository, PasswordEncoder, JwtTokenProvider, TokenBlacklistService, MfaService, TeamMemberRepository, AuditLogService
- `register(RegisterRequest): AuthResponse` — Creates user, returns JWT
- `login(LoginRequest): AuthResponse` — Validates credentials, checks MFA, returns JWT or MFA challenge token
- `refreshToken(RefreshTokenRequest): AuthResponse` — Validates refresh token, issues new pair
- `logout(String token): void` — Extracts JTI, blacklists token
- `changePassword(ChangePasswordRequest, UUID userId): void` — Validates old password, updates hash

#### UserService.java
Injects: UserRepository
- `getUserById(UUID): UserResponse`
- `getUserByEmail(String): UserResponse`
- `getCurrentUser(): UserResponse`
- `updateUser(UUID, UpdateUserRequest): UserResponse`
- `searchUsers(String query): List<UserResponse>` — Case-insensitive, max 20 results
- `deactivateUser(UUID): void`
- `activateUser(UUID): void`

#### TeamService.java
Injects: TeamRepository, TeamMemberRepository, UserRepository, InvitationRepository, EmailService, AuditLogService
- `createTeam(CreateTeamRequest, UUID userId): TeamResponse` — Creator becomes OWNER
- `getTeamsForUser(UUID): List<TeamResponse>`
- `getTeam(UUID): TeamResponse`
- `updateTeam(UUID, UpdateTeamRequest): TeamResponse`
- `deleteTeam(UUID): void` — Requires OWNER role
- `getMembers(UUID teamId): List<TeamMemberResponse>`
- `updateMemberRole(UUID teamId, UUID userId, UpdateRoleRequest): TeamMemberResponse`
- `removeMember(UUID teamId, UUID userId): void`
- `inviteMember(UUID teamId, InviteMemberRequest): InvitationResponse` — Generates token, sends email
- `acceptInvitation(String token): void` — Pessimistic lock on invitation
- `getInvitations(UUID teamId): List<InvitationResponse>`
- `cancelInvitation(UUID teamId, UUID invitationId): void`

#### ProjectService.java
Injects: ProjectRepository, TeamMemberRepository, GitHubConnectionRepository, JiraConnectionRepository, QaJobRepository, FindingRepository, AgentRunRepository + 8 more repos for cascading delete
- `createProject(UUID teamId, CreateProjectRequest): ProjectResponse`
- `getProjectsForTeam(UUID teamId, boolean includeArchived, Pageable): PageResponse<ProjectResponse>`
- `getProject(UUID): ProjectResponse`
- `updateProject(UUID, UpdateProjectRequest): ProjectResponse`
- `archiveProject(UUID): ProjectResponse`
- `unarchiveProject(UUID): ProjectResponse`
- `deleteProject(UUID): void` — Cascading delete across all related entities
- `updateHealthScore(UUID projectId, Integer score): void` — Called by QaJobService on job completion

#### QaJobService.java
Injects: QaJobRepository, ProjectRepository, TeamMemberRepository, AgentRunRepository, BugInvestigationRepository, ProjectService
- `createJob(CreateJobRequest): JobResponse`
- `getJob(UUID): JobResponse`
- `getJobsForProject(UUID, Pageable): PageResponse<JobResponse>`
- `getJobsForUser(UUID, Pageable): PageResponse<JobResponse>`
- `updateJob(UUID, UpdateJobRequest): JobResponse` — Auto-updates project health score on completion
- `deleteJob(UUID): void`
- `createAgentRun(UUID jobId, CreateAgentRunRequest): AgentRunResponse`
- `createAgentRunsBatch(UUID jobId, List<CreateAgentRunRequest>): List<AgentRunResponse>`
- `getAgentRuns(UUID jobId): List<AgentRunResponse>`
- `updateAgentRun(UUID agentRunId, UpdateAgentRunRequest): AgentRunResponse`

#### Additional Core Services (signatures abbreviated):
- **AdminService** — User listing/update, system settings CRUD, usage statistics, audit log queries, MFA reset
- **AuditLogService** — `@Async log(UUID userId, UUID teamId, String action, String entityType, UUID entityId, String details): void`
- **BugInvestigationService** — Investigation CRUD with S3 report upload
- **ComplianceService** — Spec/item CRUD, weighted compliance scoring
- **DependencyService** — Scan/vulnerability CRUD, severity filtering
- **DirectiveService** — Directive CRUD, project assignment/toggle, max limits
- **EncryptionService** — AES-256-GCM encrypt/decrypt with PBKDF2 key derivation
- **FindingService** — Finding CRUD, bulk status updates, severity count aggregation
- **GitHubConnectionService** — Soft-delete connections, credential encryption/decryption
- **HealthMonitorService** — Schedule CRUD, snapshot creation, trend analysis
- **JiraConnectionService** — Connection CRUD with JiraConnectionDetails record
- **MetricsService** — Project/team quality metrics aggregation, health trend
- **MfaService** — TOTP setup/verify, email MFA, recovery codes (encrypted), admin reset
- **NotificationService** — User preference management, `shouldNotify(UUID userId, String event): boolean`
- **PersonaService** — CRUD with scope management, default per agent type
- **RemediationTaskService** — Task CRUD, assignment, S3 prompt uploads
- **ReportStorageService** — Report/spec upload/download via S3StorageService
- **S3StorageService** — Transparent S3/local filesystem switching, presigned URLs
- **TechDebtService** — Item CRUD, status/category filtering, debt summary
- **TokenBlacklistService** — In-memory ConcurrentHashMap JWT revocation

### Courier Services (22)
- **CollectionService** — Collection CRUD, duplication, search, access control
- **FolderService** — Folder CRUD, tree building, reordering, circular reference prevention
- **RequestService** — Request CRUD, duplication, component management
- **RequestProxyService** — HTTP execution with variable resolution, auth, redirect tracking, history
- **EnvironmentService** — Environment CRUD, activation, cloning, variable management
- **VariableService** — {{variable}} resolution: Global → Collection → Environment → Local
- **AuthResolverService** — Auth configuration resolution to HTTP headers/params
- **ScriptEngineService** — GraalJS sandbox with Postman-compatible `pm` API
- **CollectionRunnerService** — Sequential request execution with scripts, assertions, data files
- **ForkService** — Collection forking and fork management
- **MergeService** — Merge request creation, conflict detection, fork merging
- **ShareService** — Collection sharing with permission-level checks (VIEWER/EDITOR/ADMIN)
- **HistoryService** — Request history with pagination, search, filtering, cleanup
- **ImportService** — Collection import orchestration (Postman, OpenAPI, cURL)
- **PostmanImporter** — Postman v2.1 JSON parsing with folder nesting
- **OpenApiImporter** — OpenAPI 3.x spec parsing
- **CurlImporter** — cURL command parsing with tokenization
- **ExportService** — Export to Postman v2.1, OpenAPI 3.0.3, native JSON
- **CodeGenerationService** — Code snippet generation in 12+ languages
- **GraphQLService** — GraphQL execution, introspection, query validation
- **DataFileParser** — CSV/JSON data file parsing for collection runs
- **ScriptContext** — Mutable execution context for script variables

### Logger Services (18)
- **LogIngestionService** — HTTP push + Kafka ingestion, source auto-creation
- **LogQueryService** — JPA Criteria queries, full-text search, DSL parsing
- **LogParsingService** — Raw log parsing (JSON, key-value, Spring Boot, syslog, plain text)
- **LogSourceService** — Source CRUD
- **LogTrapService** — Trap CRUD, evaluation engine invocation, historical testing
- **AlertService** — Rule CRUD, alert firing, throttling, lifecycle management
- **AlertChannelService** — Channel CRUD, async notification delivery, SSRF protection
- **DashboardService** — Dashboard CRUD, widget management, template system
- **MetricsService** — Metric registration (idempotent), time-series push, aggregation
- **AnomalyDetectionService** — Baseline management, z-score anomaly checking
- **AnomalyBaselineCalculator** — Statistical calculations (mean, stddev, z-score)
- **MetricAggregationService** — Percentile, std dev, bucketed time aggregation
- **TraceService** — Span CRUD, trace assembly, waterfall visualization
- **TraceAnalysisService** — Waterfall building, parent-child depth, error propagation
- **RetentionService** — Policy CRUD, storage usage reporting
- **RetentionExecutor** — `@Scheduled(cron="0 0 2 * * *")` daily execution
- **TrapEvaluationEngine** — Stateless pattern/keyword/frequency evaluation with regex cache
- **LogQueryDslParser** — SQL-like DSL parser with field mapping

### Registry Services (10)
- **ServiceRegistryService** — Service CRUD, slug generation, cloning, health checking
- **SolutionService** — Solution CRUD, member management, health aggregation
- **PortAllocationService** — Auto-allocation from ranges, conflict detection
- **DependencyGraphService** — Kahn's topological sort, DFS cycle detection, BFS impact analysis
- **ApiRouteService** — Route registration with prefix overlap detection
- **ConfigEngineService** — Generates Docker Compose, application.yml, Claude Code headers
- **HealthCheckService** — Health aggregation (cached/live), team/solution summaries
- **InfraResourceService** — Resource CRUD, orphan detection, reassignment
- **TopologyService** — Ecosystem visualization, neighborhood BFS (depth cap 3)
- **WorkstationProfileService** — Profile CRUD, startup order from dependency graph

### Relay Services (8)
- **ChannelService** — Channel CRUD, membership, pinned messages, auto-channel creation
- **MessageService** — Send/edit/delete, threads, search, read receipts, unread counts
- **ThreadService** — Thread metadata, reply counting, participant management
- **DirectMessageService** — Conversation CRUD, message CRUD, read tracking
- **ReactionService** — Toggle-based emoji reactions, aggregated summaries
- **FileAttachmentService** — Upload (25MB limit), download, MIME detection, local storage
- **PresenceService** — Status management, heartbeat, stale detection, team-wide queries
- **PlatformEventService** — Event publishing/delivery, channel resolution, retry logic

---

## 10. Controller / API Layer — Method Signatures Only

### Core Controllers (17 + 1 Health)

| Controller | Base Path | Injects | Endpoint Count |
|---|---|---|---|
| AuthController | /api/v1/auth | AuthService, AuditLogService, MfaService | 14 |
| AdminController | /api/v1/admin | AdminService, AuditLogService | 10 |
| UserController | /api/v1/users | UserService, AuditLogService | 6 |
| TeamController | /api/v1/teams | TeamService, AuditLogService | 12 |
| ProjectController | /api/v1/projects | ProjectService, AuditLogService | 7 |
| JobController | /api/v1/jobs | QaJobService, AuditLogService | 13 |
| FindingController | /api/v1/findings | FindingService, AuditLogService | 10 |
| ComplianceController | /api/v1/compliance | ComplianceService | 7 |
| DependencyController | /api/v1/dependencies | DependencyService | 10 |
| DirectiveController | /api/v1/directives | DirectiveService, AuditLogService | 11 |
| HealthMonitorController | /api/v1/health-monitor | HealthMonitorService | 8 |
| IntegrationController | /api/v1/integrations | GitHubConnectionService, JiraConnectionService, AuditLogService | 8 |
| MetricsController | /api/v1/metrics | MetricsService | 3 |
| PersonaController | /api/v1/personas | PersonaService, AuditLogService | 11 |
| ReportController | /api/v1/reports | ReportStorageService | 5 |
| TaskController | /api/v1/tasks | RemediationTaskService, AuditLogService | 6 |
| TechDebtController | /api/v1/tech-debt | TechDebtService, AuditLogService | 9 |
| HealthController | /api/v1/health | (none) | 1 |

### Courier Controllers (13)
| Controller | Base Path |
|---|---|
| CollectionController | /api/v1/courier/collections |
| FolderController | /api/v1/courier/folders |
| RequestController | /api/v1/courier/requests |
| ProxyController | /api/v1/courier/proxy |
| EnvironmentController | /api/v1/courier/environments |
| VariableController | /api/v1/courier/variables |
| HistoryController | /api/v1/courier/history |
| RunnerController | /api/v1/courier/runner |
| ShareController | /api/v1/courier/shares |
| ImportController | /api/v1/courier/import |
| CodeGenerationController | /api/v1/courier/codegen |
| GraphQLController | /api/v1/courier/graphql |
| HealthController (Courier) | /api/v1/courier/health |

### Logger Controllers (11)
| Controller | Base Path |
|---|---|
| LogSourceController | /api/v1/logger/sources |
| LogIngestionController | /api/v1/logger/logs |
| LogQueryController | /api/v1/logger/query |
| LogTrapController | /api/v1/logger/traps |
| AlertController | /api/v1/logger/alerts |
| DashboardController | /api/v1/logger/dashboards |
| MetricsController | /api/v1/logger/metrics |
| AnomalyController | /api/v1/logger/anomalies |
| RetentionController | /api/v1/logger/retention |
| TraceController | /api/v1/logger/traces |
| BaseController | (abstract, shared utilities) |

### Registry Controllers (10)
| Controller | Base Path |
|---|---|
| RegistryController | /api/v1/registry/teams/{teamId}/services |
| SolutionController | /api/v1/registry/teams/{teamId}/solutions |
| PortController | /api/v1/registry |
| DependencyController | /api/v1/registry |
| ConfigController | /api/v1/registry |
| InfraController | /api/v1/registry |
| RouteController | /api/v1/registry |
| TopologyController | /api/v1/registry |
| WorkstationController | /api/v1/registry |
| HealthManagementController | /api/v1/registry |

### Relay Controllers (8)
| Controller | Base Path |
|---|---|
| ChannelController | /api/v1/relay/channels |
| MessageController | /api/v1/relay/messages |
| DirectMessageController | /api/v1/relay/dm |
| ReactionController | /api/v1/relay/reactions |
| FileController | /api/v1/relay/files |
| PresenceController | /api/v1/relay/presence |
| PlatformEventController | /api/v1/relay/events |
| RelayHealthController | /api/v1/relay/health |

---

## 11. Security Configuration

```
Authentication: JWT (HS256) — internal token provider
Token issuer/validator: JwtTokenProvider (internal)
Password encoder: BCrypt (strength 12)

Public endpoints (no auth required):
  - /api/v1/auth/**
  - /api/v1/health
  - /api/v1/courier/health
  - /swagger-ui/**
  - /v3/api-docs/**
  - /ws/relay/**

Protected endpoints:
  - /api/** → authenticated (via @PreAuthorize on each controller)
  - Admin endpoints → hasRole('ADMIN') or hasRole('OWNER')

CORS: Origins from codeops.cors.allowed-origins (localhost:3000,5173 in dev)
      Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
      Headers: Authorization, Content-Type, X-Requested-With

CSRF: Disabled (stateless JWT API)

Rate limiting: RateLimitFilter on /api/v1/auth/** — 10 requests per 60s per IP
               Client IP from X-Forwarded-For or remote addr

Security headers: CSP, HSTS (31536000s), X-Frame-Options DENY, X-Content-Type-Options
```

---

## 12. Custom Security Components

```
=== JwtAuthFilter.java ===
Extends: OncePerRequestFilter
Purpose: Extracts and validates JWT from Authorization header
Extracts token from: Authorization: Bearer {token} header
Validates via: JwtTokenProvider.validateToken()
Rejects MFA challenge tokens for normal API access
Sets SecurityContext: YES — UsernamePasswordAuthenticationToken with userId as principal, ROLE_* authorities

=== JwtTokenProvider.java ===
Generates: Access tokens (24h), refresh tokens (30d), MFA challenge tokens (5min)
Algorithm: HS256 with minimum 32-char secret
Claims: sub (userId), email, roles, jti (for blacklisting), type (refresh/mfa_challenge)
Validates against: TokenBlacklistService for revocation

=== RateLimitFilter.java ===
Extends: OncePerRequestFilter
Purpose: Per-IP sliding window rate limiting on auth endpoints
Config: 10 requests per 60s window
Storage: ConcurrentHashMap (in-memory)
Response on exceed: 429 JSON

=== RequestCorrelationFilter.java ===
Extends: OncePerRequestFilter (HIGHEST_PRECEDENCE)
Purpose: Generates/propagates X-Correlation-ID, enriches MDC
MDC keys: correlationId, requestPath, requestMethod

=== SecurityUtils.java ===
Static utility: getCurrentUserId(), hasRole(), isAdmin()
```

---

## 13. Exception Handling & Error Responses

```
=== GlobalExceptionHandler.java ===
@ControllerAdvice: YES

Exception Mappings:
  - EntityNotFoundException → 404
  - NotFoundException → 404
  - IllegalArgumentException → 400
  - ValidationException → 400
  - MethodArgumentNotValidException → 400 (field-level errors)
  - MissingServletRequestParameterException → 400
  - HttpMessageNotReadableException → 400 (malformed JSON/enum)
  - MethodArgumentTypeMismatchException → 400
  - NoResourceFoundException → 404
  - AccessDeniedException → 403
  - AuthorizationException → 403
  - HttpRequestMethodNotSupportedException → 405
  - CodeOpsException → 500
  - Exception (catch-all) → 500

Standard error response format:
{
  "timestamp": "2026-02-26T01:35:27Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/..."
}

For validation errors, includes "fieldErrors" map with field-specific messages.
```

---

## 14. Mappers / DTOs

### Core Module
No MapStruct mappers — manual mapping in service classes via private `mapToResponse()` methods.

### Courier Module (13 MapStruct mappers)
All use `componentModel = "spring"`, `builder = @Builder(disableBuilder = true)`:
- RequestMapper, RequestBodyMapper, RequestParamMapper, RequestHeaderMapper, RequestScriptMapper, RequestAuthMapper
- CollectionMapper, FolderMapper, EnvironmentMapper, EnvironmentVariableMapper, GlobalVariableMapper
- RunResultMapper, RequestHistoryMapper

Custom `@Mapping` annotations for boolean `is*` fields → source `shared`/`enabled` (Lombok naming convention).

### Logger Module (13 MapStruct mappers)
LogEntryMapper, LogSourceMapper, LogTrapMapper, TrapConditionMapper, AlertRuleMapper, AlertChannelMapper, AlertHistoryMapper, DashboardMapper, DashboardWidgetMapper, MetricMapper, AnomalyBaselineMapper, QueryHistoryMapper, SavedQueryMapper, RetentionPolicyMapper, TraceSpanMapper

### Registry Module
No MapStruct mappers — manual mapping in service classes.

### Relay Module (11 MapStruct mappers)
ChannelMapper, ChannelMemberMapper, MessageMapper, MessageThreadMapper, DirectConversationMapper, DirectMessageMapper, ReactionMapper, FileAttachmentMapper, PinnedMessageMapper, UserPresenceMapper, PlatformEventMapper

Custom mappings for `isEdited`/`isDeleted`/`isArchived` → source `edited`/`deleted`/`archived`.

---

## 15. Utility Classes & Shared Components

```
=== AppConstants.java ===
Centralized constants for all modules:
  Team limits: 50 members, 100 projects
  File limits: 25MB reports, 100KB personas, 200KB directives
  Auth: JWT 24h, refresh 30d, invitation 7d, min password 1 char
  S3 prefixes: reports/, specs/, personas/, releases/
  Port ranges per type (HTTP: 8080-8099, etc.)
  Registry: max 500 services, 100 solutions, 50 ports, 100 dependencies
  Courier: 30s timeout, 10 max redirects, 10MB response, 100 req/min rate limit
  Relay: 10000 char message, 25MB file, 50 max reactions, 25 group DM, 5min heartbeat
  Logger: Kafka topics, 30-day retention, query limits

=== SecurityUtils.java ===
Methods: getCurrentUserId(), hasRole(String), isAdmin()
Used by: All services for authorization checks

=== SlugUtils.java (Registry)
Methods: generateSlug(String name), validateSlug(String), makeUnique(String, Set<String>)
Algorithm: lowercase, replace non-alphanumeric with hyphens, 3-63 chars

=== PageResponse<T> (record)
Fields: content, page, size, totalElements, totalPages, last
Used by: All paginated endpoints across all modules
```

---

## 16. Database Schema (Live)

84 tables in the `public` schema, managed by Hibernate ddl-auto:update.

### Table Inventory by Module

**Core (28 tables):** users, teams, team_members, projects, qa_jobs, findings, agent_runs, audit_log, personas, directives, project_directives, invitations, github_connections, jira_connections, bug_investigations, compliance_items, dependency_scans, dependency_vulnerabilities, health_schedules, health_snapshots, mfa_email_codes, notification_preferences, remediation_tasks, remediation_task_findings (join), specifications, system_settings, tech_debt_items

**Courier (18 tables):** courier_collections, courier_collection_shares, courier_code_snippet_templates, courier_environments, courier_environment_variables, courier_folders, courier_forks, courier_global_variables, courier_merge_requests, courier_requests, courier_request_auths, courier_request_bodies, courier_request_headers, courier_request_histories, courier_request_params, courier_request_scripts, courier_run_iterations, courier_run_results

**Logger (16 tables):** log_entries, log_sources, log_traps, trap_conditions, alert_rules, alert_channels, alert_histories, logger_dashboards, logger_dashboard_widgets, anomaly_baselines, logger_metrics, metric_series, query_histories, retention_policies, saved_queries, trace_spans

**Registry (11 tables):** service_registrations, solutions, solution_members, port_allocations, port_ranges, service_dependencies, api_route_registrations, registry_environment_configs, config_templates, infra_resources, workstation_profiles

**Relay (12 tables):** relay_channels, relay_channel_members, relay_messages, relay_message_threads, relay_direct_conversations, relay_direct_messages, relay_reactions, relay_file_attachments, relay_pinned_messages, relay_read_receipts, relay_user_presences, relay_platform_events

Schema drift: None detected — Hibernate ddl-auto manages all tables.

---

## 17. Message Broker Configuration

```
Broker: Apache Kafka 7.5.0 (Confluent)
Connection: localhost:9092 (external), localhost:29092 (internal)
Consumer Group: codeops-server
Auto-offset-reset: earliest
Deserializers: StringDeserializer (key + value)
Error handling: DefaultErrorHandler, 3 retries, 1s fixed backoff

Topics (created by kafka-init container, 3 partitions each):
  - codeops-decisions
  - codeops-outcomes
  - codeops-hypotheses
  - codeops-integrations
  - codeops-notifications

Consumers:
  - KafkaLogConsumer.consume(String message, Headers headers)
    Listens: codeops-logs topic
    Extracts: X-Team-Id header
    Delegates to: LogIngestionService

Publishers:
  - No explicit Kafka producers detected in source
```

---

## 18. Cache Layer

```
Cache Provider: Redis 7 (available but not used for application caching)
Connection: localhost:6379 (append-only persistence)

No @Cacheable, @CacheEvict, or CacheManager annotations detected.
Redis is provisioned in docker-compose but not actively used for caching.
Rate limiting uses in-memory ConcurrentHashMap, not Redis.
Token blacklist uses in-memory ConcurrentHashMap, not Redis.
```

---

## 19. Environment Variable Inventory

| Variable | Used In | Default | Required in Prod |
|----------|---------|---------|-----------------|
| DATABASE_URL | application-prod.yml | localhost:5432/codeops | YES |
| JWT_SECRET | application-prod.yml | (none) | YES |
| ENCRYPTION_KEY | application-prod.yml | (none) | YES |
| AWS_S3_BUCKET | application-prod.yml | (none) | YES |
| AWS_S3_REGION | application-prod.yml | us-east-1 | YES |
| MAIL_FROM_EMAIL | application-prod.yml | noreply@codeops.dev | NO |
| CORS_ORIGINS | application-prod.yml | (none) | YES |
| KAFKA_BOOTSTRAP_SERVERS | application-prod.yml | localhost:9094 | YES |
| SPRING_PROFILES_ACTIVE | Dockerfile | dev | YES (set to prod) |

---

## 20. Service Dependency Map

```
This Service → Depends On
--------------------------
PostgreSQL: Primary data store (all 84 tables)
Redis: Provisioned but not actively used
Kafka: Log ingestion (codeops-logs topic)
GitHub API: Repository access via PAT/OAuth (encrypted credentials)
Jira API: Issue tracking via API token (encrypted credentials)
Microsoft Teams: Webhook notifications (job completion, critical findings)
AWS S3: File/report storage (disabled in dev, local filesystem fallback)
AWS SES: Email sending (disabled in dev, logged to console)

Downstream Consumers (services that call this one):
- CodeOps-Client (Flutter desktop app) — all /api/v1/** endpoints
- CodeOps-Analytics (port 8081) — reads analytics schema (separate)
```

---

## 21. Known Technical Debt & Issues

| Issue | Location | Severity | Notes |
|-------|----------|----------|-------|
| No CI/CD pipeline | Project root | High | No .github/workflows or Jenkinsfile detected |
| Redis provisioned but unused | docker-compose.yml | Low | Rate limiting and token blacklist use in-memory maps instead |
| 4 System.out/printStackTrace | Various source files | Low | Should use SLF4J logger |
| No HTTPS enforcement | application-prod.yml | Medium | Should be handled at LB/proxy layer |
| TokenBlacklistService in-memory | TokenBlacklistService.java | Medium | Blacklisted tokens lost on restart; consider Redis-backed implementation |
| 1 potential SQL string concat | Source files | Medium | Verify no SQL injection risk |
| MFA recovery codes in memory | MfaService.java | Low | Encrypted but stored in entity TEXT field, not separate secure store |
| Kafka topics auto-create disabled | docker-compose.yml | Info | Topics created by kafka-init container — ensure prod provisioning |
| No request/response logging filter | Configuration | Low | LoggingInterceptor exists but CommonsRequestLoggingFilter not used |
| 2 field injection instances | Various | Low | Should be converted to constructor injection |

END OF AUDIT FILE.
