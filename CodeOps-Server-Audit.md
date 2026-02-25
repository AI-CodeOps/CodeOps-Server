# CodeOps-Server — Codebase Audit

**Audit Date:** 2026-02-25T13:38:32Z
**Branch:** main
**Commit:** f6de00aa5cd46eaa09904c5ad16def6784c11f21 REL-010: Relay module — REST controllers, DataSeeder, and controller tests
**Auditor:** Claude Code (Automated)
**Purpose:** Zero-context reference for AI-assisted development
**Audit File:** CodeOps-Server-Audit.md
**Scorecard:** CodeOps-Server-Scorecard.md
**OpenAPI Spec:** CodeOps-Server-OpenAPI.yaml

> This audit is the single source of truth for the CodeOps-Server codebase.
> The OpenAPI spec (CodeOps-Server-OpenAPI.yaml) is the source of truth for all endpoints, DTOs, and API contracts.
> An AI reading this audit + the OpenAPI spec should be able to generate accurate code
> changes, new features, tests, and fixes without filesystem access.

---

## 1. Project Identity

```
Project Name:           CodeOps Server
Repository URL:         https://github.com/adamallard/CodeOps-Server
Primary Language:       Java 21 / Spring Boot 3.3.0
Build Tool:             Apache Maven (via spring-boot-starter-parent 3.3.0)
Current Branch:         main
Latest Commit Hash:     f6de00aa5cd46eaa09904c5ad16def6784c11f21
Latest Commit Message:  REL-010: Relay module — REST controllers, DataSeeder, and controller tests
Audit Timestamp:        2026-02-25T13:38:32Z
```

---

## 2. Directory Structure

Single-module Maven project. Source code under `src/main/java/com/codeops/` organized into 5 feature modules plus shared packages.

```
CodeOps-Server/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── src/main/java/com/codeops/
│   ├── CodeOpsApplication.java            ← Entry point
│   ├── config/                            ← 15 config classes (AppConstants, CORS, JWT, Jackson, etc.)
│   ├── security/                          ← 5 files (SecurityConfig, JwtAuthFilter, JwtTokenProvider, RateLimitFilter, SecurityUtils)
│   ├── notification/                      ← 3 files (EmailService, TeamsWebhookService, NotificationDispatcher)
│   ├── exception/                         ← 4 exception classes
│   ├── entity/                            ← 28 Core entities
│   │   └── enums/                         ← 25 Core enums
│   ├── repository/                        ← 26 Core repositories
│   ├── service/                           ← 26 Core services
│   ├── controller/                        ← 17 Core controllers
│   ├── dto/request/                       ← Core request DTOs
│   ├── dto/response/                      ← Core response DTOs
│   ├── registry/                          ← Registry module (109 files)
│   │   ├── entity/ + enums/               ← 11 entities, 11 enums
│   │   ├── repository/                    ← 11 repositories
│   │   ├── service/                       ← 10 services + 1 utility
│   │   ├── controller/                    ← 10 controllers
│   │   └── dto/                           ← Request/Response DTOs
│   ├── logger/                            ← Logger module (151 files)
│   │   ├── entity/ + enums/               ← 16 entities, 10 enums
│   │   ├── repository/                    ← 16 repositories
│   │   ├── service/                       ← 19 services + 2 event classes
│   │   ├── controller/                    ← 11 controllers (incl. BaseController)
│   │   └── dto/                           ← Request/Response DTOs
│   ├── courier/                           ← Courier module (152 files)
│   │   ├── entity/ + enums/               ← ~15 entities, ~12 enums
│   │   ├── repository/                    ← ~15 repositories
│   │   ├── service/                       ← ~12 services
│   │   ├── controller/                    ← 13 controllers
│   │   ├── config/                        ← HttpClientConfig
│   │   └── dto/                           ← Request/Response DTOs
│   └── relay/                             ← Relay module (103 files)
│       ├── entity/ + enums/               ← 12 entities, 8 enums
│       ├── repository/                    ← 12 repositories
│       ├── service/                       ← 8 services
│       ├── controller/                    ← 8 controllers
│       ├── websocket/                     ← WebSocket config + controller
│       ├── mapper/                        ← 11 MapStruct mappers
│       ├── config/                        ← RelayDataSeeder
│       └── dto/                           ← Request/Response DTOs
├── src/main/resources/
│   ├── application.yml                    ← Default profile (port 8090, active: dev)
│   ├── application-dev.yml                ← Dev config (local Postgres, Kafka, JWT)
│   ├── application-prod.yml               ← Prod config (all env vars)
│   └── logback-spring.xml                 ← Structured logging (console dev, JSON prod)
├── src/test/
│   ├── java/                              ← 179 unit + 16 integration test files
│   └── resources/
│       ├── application-test.yml           ← H2 in-memory for unit tests
│       └── application-integration.yml    ← Testcontainers PostgreSQL for ITs
```

**Total:** 745 Java source files, 195 test files, 3,062 @Test methods.

---

## 3. Build & Dependency Manifest

**Build file:** `pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.3.0 | REST API framework |
| spring-boot-starter-data-jpa | 3.3.0 | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.3.0 | Authentication/Authorization |
| spring-boot-starter-validation | 3.3.0 | Jakarta Bean Validation |
| spring-boot-starter-websocket | 3.3.0 | STOMP/SockJS WebSocket support |
| spring-boot-starter-mail | 3.3.0 | SMTP email sending |
| postgresql | (managed) | PostgreSQL JDBC driver |
| jjwt-api/impl/jackson | 0.12.6 | JWT token generation/validation |
| aws-sdk-s3 | 2.25.0 | S3 file storage (prod only) |
| totp | 1.7.1 | TOTP MFA (dev.samstevens) |
| lombok | 1.18.42 | Boilerplate reduction |
| mapstruct | 1.5.5.Final | Entity↔DTO mapping |
| jackson-datatype-jsr310 | (managed) | Java 8 date/time serialization |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI + OpenAPI spec |
| logstash-logback-encoder | 7.4 | Structured JSON logging |
| spring-kafka | (managed) | Kafka producer/consumer |
| graalvm-polyglot + js | 24.1.1 | JavaScript script engine (Courier) |
| jackson-dataformat-yaml | (managed) | YAML parsing (Courier OpenAPI import) |
| spring-boot-starter-test | 3.3.0 | Test framework |
| spring-security-test | (managed) | Security test utilities |
| testcontainers-postgresql | 1.19.8 | Integration test PostgreSQL |
| testcontainers-junit-jupiter | 1.19.8 | Testcontainers JUnit5 integration |
| testcontainers-kafka | 1.19.8 | Integration test Kafka |
| h2 | (managed) | In-memory test database |
| spring-kafka-test | (managed) | Kafka test utilities |

**Build plugins:**
- `spring-boot-maven-plugin` — excludes Lombok from fat JAR
- `maven-compiler-plugin` — Java 21 source/target, annotation processors: Lombok + MapStruct
- `maven-surefire-plugin` — includes `*Test.java` and `*IT.java`, `--add-opens` for Java 25 compatibility
- `jacoco-maven-plugin` 0.8.14 — code coverage reporting

**Overrides for Java 25 compatibility:**
- `lombok.version` → 1.18.42 (fixes TypeTag :: UNKNOWN crash)
- `mockito.version` → 5.21.0 (ByteBuddy compatibility)
- `byte-buddy.version` → 1.18.4

**Build commands:**
```
Build:   mvn clean package -DskipTests
Test:    mvn test
Run:     mvn spring-boot:run
Package: mvn clean package
```

---

## 4. Configuration & Infrastructure Summary

**`src/main/resources/application.yml`** — Default profile. Server port 8090, active profile `dev`.

**`src/main/resources/application-dev.yml`** — PostgreSQL at `localhost:5432/codeops` (user/pass: `codeops`/`codeops`). Hibernate `ddl-auto: update`, SQL logging enabled. Kafka at `localhost:9094`. JWT secret has dev fallback. S3 disabled (local filesystem at `~/.codeops/storage/`). Email disabled (logged to console). CORS allows `localhost:3000,5173`.

**`src/main/resources/application-prod.yml`** — All secrets from env vars. Hibernate `ddl-auto: validate`. S3 and email enabled. Logging at INFO/WARN.

**`src/test/resources/application-test.yml`** — H2 in-memory with `create-drop`. Kafka listener auto-startup disabled. Flyway disabled.

**`src/test/resources/application-integration.yml`** — Testcontainers PostgreSQL (overridden by @DynamicPropertySource). `create-drop` schema.

**`src/main/resources/logback-spring.xml`** — Console encoder for dev, LogstashEncoder (JSON) for prod.

**`docker-compose.yml`** — 5 services:
- PostgreSQL 16 Alpine (`codeops-db`, `127.0.0.1:5432`)
- Redis 7 Alpine (`codeops-redis`, `6379`)
- Zookeeper (`codeops-zookeeper`, `2181`)
- Kafka (`codeops-kafka`, `9092`)
- Kafka Init (creates 10 topics on startup)

**`Dockerfile`** — eclipse-temurin:21-jre-alpine, non-root user `appuser`, exposes 8090.

**Connection map:**
```
Database:       PostgreSQL 16, localhost:5432, database: codeops
Cache:          Redis 7 (docker-compose only — not used by application code)
Message Broker: Apache Kafka (localhost:9092), 10 topics
External APIs:  RestTemplate → alert webhook URLs (Teams), health check URLs
Cloud Services: AWS S3 (prod only), SMTP email (prod only)
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

**Entry point:** `com.codeops.CodeOpsApplication` — `@SpringBootApplication`, `@EnableScheduling`, `@EnableConfigurationProperties({JwtProperties, MailProperties})`

**Startup initialization:**
1. Hibernate `ddl-auto: update` creates/updates all 72 database tables
2. `DataSeeder` (`@PostConstruct`) — seeds default user (`adam@allard.com`/`pass`), team, projects, personas, directives, connections, QA jobs, findings, remediation tasks, compliance items, dependency scans, health snapshots, schedules, notification preferences, system settings, audit logs
3. `RelayDataSeeder` (`@PostConstruct`) — seeds channels, messages, threads, reactions, DMs, file attachments, presence, platform events for the Relay module
4. `KafkaConsumerConfig` configures Kafka consumer factory

**Scheduled tasks:**
- `MfaService.cleanupExpiredCodes()` — `@Scheduled(fixedRate = 3600000)` — deletes expired MFA email codes hourly
- Logger: `RetentionExecutor` — `@Scheduled(cron = "0 0 2 * * *")` — daily at 2 AM, deletes logs older than retention policy
- Logger: `AnomalyDetectionService` — `@Scheduled(fixedRate)` — recalculates anomaly baselines periodically

**Health check endpoints:**
- `GET /api/v1/health` — Core module health (200 `{"status":"UP","module":"core"}`)
- `GET /api/v1/logger/health` — Logger module health
- `GET /api/v1/courier/health` — Courier module health
- `GET /api/v1/relay/health` — Relay module health
- `GET /api/v1/registry/health` — Registry module health

---

## 6. Entity / Data Model Layer

### Core Module


**NOTE:** BaseEntity (@MappedSuperclass) and ProjectDirectiveId (@Embeddable) are excluded from this section per instructions. BaseEntity provides: UUID id (@GeneratedValue UUID), Instant createdAt (@PrePersist), Instant updatedAt (@PrePersist/@PreUpdate). All entities below extend BaseEntity unless otherwise noted.

---

### 6.1 User
- **File:** `src/main/java/com/codeops/entity/User.java`
- **Table:** `users`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `email` — String, nullable=false, unique=true, length=255
  - `passwordHash` — String, column=`password_hash`, nullable=false, length=255
  - `displayName` — String, column=`display_name`, nullable=false, length=100
  - `avatarUrl` — String, column=`avatar_url`, length=500
  - `isActive` — Boolean, column=`is_active`, nullable=false, @Builder.Default=true
  - `lastLoginAt` — Instant, column=`last_login_at`
  - `mfaEnabled` — Boolean, column=`mfa_enabled`, nullable=false, @Builder.Default=false
  - `mfaMethod` — MfaMethod, column=`mfa_method`, @Enumerated(STRING), nullable=false, length=10, @Builder.Default=NONE
  - `mfaSecret` — String, column=`mfa_secret`, length=500
  - `mfaRecoveryCodes` — String, column=`mfa_recovery_codes`, length=2000
- **Relationships:** None declared (referenced by other entities)
- **Indexes:** Unique constraint on email (via unique=true)
- **@Version:** No

### 6.2 Team
- **File:** `src/main/java/com/codeops/entity/Team.java`
- **Table:** `teams`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `name` — String, nullable=false, length=100
  - `description` — String, columnDefinition=TEXT
  - `owner` — User, @ManyToOne(LAZY), @JoinColumn(name=`owner_id`, nullable=false)
  - `teamsWebhookUrl` — String, column=`teams_webhook_url`, length=500
  - `settingsJson` — String, column=`settings_json`, columnDefinition=TEXT
- **Relationships:** @ManyToOne -> User (owner)
- **Indexes:** None declared
- **@Version:** No

### 6.3 TeamMember
- **File:** `src/main/java/com/codeops/entity/TeamMember.java`
- **Table:** `team_members`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `team` — Team, @ManyToOne(LAZY), @JoinColumn(name=`team_id`, nullable=false)
  - `user` — User, @ManyToOne(LAZY), @JoinColumn(name=`user_id`, nullable=false)
  - `role` — TeamRole, @Enumerated(STRING), nullable=false
  - `joinedAt` — Instant, column=`joined_at`, nullable=false
- **Relationships:** @ManyToOne -> Team, @ManyToOne -> User
- **Indexes:** idx_tm_team_id (team_id), idx_tm_user_id (user_id)
- **Unique Constraints:** UC(team_id, user_id)
- **@Version:** No

### 6.4 Project
- **File:** `src/main/java/com/codeops/entity/Project.java`
- **Table:** `projects`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `team` — Team, @ManyToOne(LAZY), @JoinColumn(name=`team_id`, nullable=false)
  - `name` — String, nullable=false, length=200
  - `description` — String, columnDefinition=TEXT
  - `githubConnection` — GitHubConnection, @ManyToOne(LAZY), @JoinColumn(name=`github_connection_id`, nullable=true)
  - `repoUrl` — String, column=`repo_url`, length=500
  - `repoFullName` — String, column=`repo_full_name`, length=200
  - `defaultBranch` — String, column=`default_branch`, columnDefinition=varchar(100) default 'main', @Builder.Default="main"
  - `jiraConnection` — JiraConnection, @ManyToOne(LAZY), @JoinColumn(name=`jira_connection_id`, nullable=true)
  - `jiraProjectKey` — String, column=`jira_project_key`, length=20
  - `jiraDefaultIssueType` — String, column=`jira_default_issue_type`, columnDefinition=varchar(50) default 'Task', @Builder.Default="Task"
  - `jiraLabels` — String, column=`jira_labels`, columnDefinition=TEXT
  - `jiraComponent` — String, column=`jira_component`, length=100
  - `techStack` — String, column=`tech_stack`, length=200
  - `healthScore` — Integer, column=`health_score`
  - `lastAuditAt` — Instant, column=`last_audit_at`
  - `settingsJson` — String, column=`settings_json`, columnDefinition=TEXT
  - `isArchived` — Boolean, column=`is_archived`, nullable=false, @Builder.Default=false
  - `createdBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`created_by`, nullable=false)
- **Relationships:** @ManyToOne -> Team, @ManyToOne -> GitHubConnection, @ManyToOne -> JiraConnection, @ManyToOne -> User (createdBy)
- **Indexes:** idx_project_team_id (team_id)
- **@Version:** No

### 6.5 Invitation
- **File:** `src/main/java/com/codeops/entity/Invitation.java`
- **Table:** `invitations`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `team` — Team, @ManyToOne(LAZY), @JoinColumn(name=`team_id`, nullable=false)
  - `email` — String, nullable=false, length=255
  - `invitedBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`invited_by`, nullable=false)
  - `role` — TeamRole, @Enumerated(STRING), nullable=false
  - `token` — String, nullable=false, unique=true, length=100
  - `status` — InvitationStatus, @Enumerated(STRING), nullable=false
  - `expiresAt` — Instant, column=`expires_at`, nullable=false
- **Relationships:** @ManyToOne -> Team, @ManyToOne -> User (invitedBy)
- **Indexes:** idx_inv_team_id (team_id), idx_inv_email (email)
- **@Version:** No

### 6.6 Persona
- **File:** `src/main/java/com/codeops/entity/Persona.java`
- **Table:** `personas`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `name` — String, nullable=false, length=100
  - `agentType` — AgentType, column=`agent_type`, @Enumerated(STRING), nullable=false
  - `description` — String, columnDefinition=TEXT
  - `contentMd` — String, column=`content_md`, nullable=false, columnDefinition=TEXT
  - `scope` — Scope, @Enumerated(STRING), nullable=false
  - `team` — Team, @ManyToOne(LAZY), @JoinColumn(name=`team_id`, nullable=true)
  - `createdBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`created_by`, nullable=false)
  - `isDefault` — Boolean, column=`is_default`, @Builder.Default=false
  - `version` — Integer, nullable=false, @Builder.Default=1
- **Relationships:** @ManyToOne -> Team, @ManyToOne -> User (createdBy)
- **Indexes:** idx_persona_team_id (team_id)
- **@Version:** No (the `version` field is an application-managed version, not JPA @Version)

### 6.7 Directive
- **File:** `src/main/java/com/codeops/entity/Directive.java`
- **Table:** `directives`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `name` — String, nullable=false, length=200
  - `description` — String, columnDefinition=TEXT
  - `contentMd` — String, column=`content_md`, nullable=false, columnDefinition=TEXT
  - `category` — DirectiveCategory, @Enumerated(STRING)
  - `scope` — DirectiveScope, @Enumerated(STRING), nullable=false
  - `team` — Team, @ManyToOne(LAZY), @JoinColumn(name=`team_id`, nullable=true)
  - `project` — Project, @ManyToOne(LAZY), @JoinColumn(name=`project_id`, nullable=true)
  - `createdBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`created_by`, nullable=false)
  - `version` — Integer, columnDefinition=integer default 1, @Builder.Default=1
- **Relationships:** @ManyToOne -> Team, @ManyToOne -> Project, @ManyToOne -> User (createdBy)
- **Indexes:** idx_directive_team_id (team_id)
- **@Version:** No (application-managed version field)

### 6.8 ProjectDirective
- **File:** `src/main/java/com/codeops/entity/ProjectDirective.java`
- **Table:** `project_directives`
- **Extends:** None (does NOT extend BaseEntity)
- **PK:** @EmbeddedId ProjectDirectiveId (composite: project_id UUID + directive_id UUID)
- **Fields:**
  - `id` — ProjectDirectiveId, @EmbeddedId
  - `project` — Project, @ManyToOne(LAZY), @MapsId("projectId"), @JoinColumn(name=`project_id`)
  - `directive` — Directive, @ManyToOne(LAZY), @MapsId("directiveId"), @JoinColumn(name=`directive_id`)
  - `enabled` — Boolean, nullable=false, columnDefinition=boolean default true, @Builder.Default=true
- **Relationships:** @ManyToOne -> Project, @ManyToOne -> Directive
- **Indexes:** None declared (PK is composite)
- **@Version:** No

### 6.9 GitHubConnection
- **File:** `src/main/java/com/codeops/entity/GitHubConnection.java`
- **Table:** `github_connections`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `team` — Team, @ManyToOne(LAZY), @JoinColumn(name=`team_id`, nullable=false)
  - `name` — String, nullable=false, length=100
  - `authType` — GitHubAuthType, column=`auth_type`, @Enumerated(STRING), nullable=false
  - `encryptedCredentials` — String, column=`encrypted_credentials`, nullable=false, columnDefinition=TEXT
  - `githubUsername` — String, column=`github_username`, length=100
  - `isActive` — Boolean, column=`is_active`, nullable=false, @Builder.Default=true
  - `createdBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`created_by`, nullable=false)
- **Relationships:** @ManyToOne -> Team, @ManyToOne -> User (createdBy)
- **Indexes:** None declared
- **@Version:** No

### 6.10 JiraConnection
- **File:** `src/main/java/com/codeops/entity/JiraConnection.java`
- **Table:** `jira_connections`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `team` — Team, @ManyToOne(LAZY), @JoinColumn(name=`team_id`, nullable=false)
  - `name` — String, nullable=false, length=100
  - `instanceUrl` — String, column=`instance_url`, nullable=false, length=500
  - `email` — String, nullable=false, length=255
  - `encryptedApiToken` — String, column=`encrypted_api_token`, nullable=false, columnDefinition=TEXT
  - `isActive` — Boolean, column=`is_active`, nullable=false, @Builder.Default=true
  - `createdBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`created_by`, nullable=false)
- **Relationships:** @ManyToOne -> Team, @ManyToOne -> User (createdBy)
- **Indexes:** None declared
- **@Version:** No

### 6.11 QaJob
- **File:** `src/main/java/com/codeops/entity/QaJob.java`
- **Table:** `qa_jobs`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `project` — Project, @ManyToOne(LAZY), @JoinColumn(name=`project_id`, nullable=false)
  - `mode` — JobMode, @Enumerated(STRING), nullable=false
  - `status` — JobStatus, @Enumerated(STRING), nullable=false
  - `name` — String, length=200
  - `branch` — String, length=100
  - `configJson` — String, column=`config_json`, columnDefinition=TEXT
  - `summaryMd` — String, column=`summary_md`, columnDefinition=TEXT
  - `overallResult` — JobResult, column=`overall_result`, @Enumerated(STRING)
  - `healthScore` — Integer, column=`health_score`
  - `totalFindings` — Integer, column=`total_findings`, @Builder.Default=0
  - `criticalCount` — Integer, column=`critical_count`, @Builder.Default=0
  - `highCount` — Integer, column=`high_count`, @Builder.Default=0
  - `mediumCount` — Integer, column=`medium_count`, @Builder.Default=0
  - `lowCount` — Integer, column=`low_count`, @Builder.Default=0
  - `jiraTicketKey` — String, column=`jira_ticket_key`, length=50
  - `startedBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`started_by`, nullable=false)
  - `startedAt` — Instant, column=`started_at`
  - `completedAt` — Instant, column=`completed_at`
  - `version` — Long, @Version
- **Relationships:** @ManyToOne -> Project, @ManyToOne -> User (startedBy)
- **Indexes:** idx_job_project_id (project_id), idx_job_started_by (started_by)
- **@Version:** Yes (Long version)

### 6.12 Finding
- **File:** `src/main/java/com/codeops/entity/Finding.java`
- **Table:** `findings`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `job` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`job_id`, nullable=false)
  - `agentType` — AgentType, column=`agent_type`, @Enumerated(STRING), nullable=false
  - `severity` — Severity, @Enumerated(STRING), nullable=false
  - `title` — String, nullable=false, length=500
  - `description` — String, columnDefinition=TEXT
  - `filePath` — String, column=`file_path`, length=500
  - `lineNumber` — Integer, column=`line_number`
  - `recommendation` — String, columnDefinition=TEXT
  - `evidence` — String, columnDefinition=TEXT
  - `effortEstimate` — Effort, column=`effort_estimate`, @Enumerated(STRING)
  - `debtCategory` — DebtCategory, column=`debt_category`, @Enumerated(STRING)
  - `status` — FindingStatus, @Enumerated(STRING), columnDefinition=varchar(20) default 'OPEN', @Builder.Default=OPEN
  - `statusChangedBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`status_changed_by`, nullable=true)
  - `statusChangedAt` — Instant, column=`status_changed_at`
  - `version` — Long, @Version
- **Relationships:** @ManyToOne -> QaJob, @ManyToOne -> User (statusChangedBy)
- **Indexes:** idx_finding_job_id (job_id), idx_finding_status (status)
- **@Version:** Yes (Long version)

### 6.13 BugInvestigation
- **File:** `src/main/java/com/codeops/entity/BugInvestigation.java`
- **Table:** `bug_investigations`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `job` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`job_id`, nullable=false)
  - `jiraKey` — String, column=`jira_key`, length=50
  - `jiraSummary` — String, column=`jira_summary`, columnDefinition=TEXT
  - `jiraDescription` — String, column=`jira_description`, columnDefinition=TEXT
  - `jiraCommentsJson` — String, column=`jira_comments_json`, columnDefinition=TEXT
  - `jiraAttachmentsJson` — String, column=`jira_attachments_json`, columnDefinition=TEXT
  - `jiraLinkedIssues` — String, column=`jira_linked_issues`, columnDefinition=TEXT
  - `additionalContext` — String, column=`additional_context`, columnDefinition=TEXT
  - `rcaMd` — String, column=`rca_md`, columnDefinition=TEXT
  - `impactAssessmentMd` — String, column=`impact_assessment_md`, columnDefinition=TEXT
  - `rcaS3Key` — String, column=`rca_s3_key`, length=500
  - `rcaPostedToJira` — Boolean, column=`rca_posted_to_jira`, @Builder.Default=false
  - `fixTasksCreatedInJira` — Boolean, column=`fix_tasks_created_in_jira`, @Builder.Default=false
- **Relationships:** @ManyToOne -> QaJob
- **Indexes:** None declared
- **@Version:** No

### 6.14 DependencyScan
- **File:** `src/main/java/com/codeops/entity/DependencyScan.java`
- **Table:** `dependency_scans`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `project` — Project, @ManyToOne(LAZY), @JoinColumn(name=`project_id`, nullable=false)
  - `job` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`job_id`, nullable=true)
  - `manifestFile` — String, column=`manifest_file`, length=200
  - `totalDependencies` — Integer, column=`total_dependencies`
  - `outdatedCount` — Integer, column=`outdated_count`
  - `vulnerableCount` — Integer, column=`vulnerable_count`
  - `scanDataJson` — String, column=`scan_data_json`, columnDefinition=TEXT
- **Relationships:** @ManyToOne -> Project, @ManyToOne -> QaJob
- **Indexes:** idx_dep_scan_project_id (project_id)
- **@Version:** No

### 6.15 DependencyVulnerability
- **File:** `src/main/java/com/codeops/entity/DependencyVulnerability.java`
- **Table:** `dependency_vulnerabilities`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `scan` — DependencyScan, @ManyToOne(LAZY), @JoinColumn(name=`scan_id`, nullable=false)
  - `dependencyName` — String, column=`dependency_name`, nullable=false, length=200
  - `currentVersion` — String, column=`current_version`, length=50
  - `fixedVersion` — String, column=`fixed_version`, length=50
  - `cveId` — String, column=`cve_id`, length=30
  - `severity` — Severity, @Enumerated(STRING), nullable=false
  - `description` — String, columnDefinition=TEXT
  - `status` — VulnerabilityStatus, @Enumerated(STRING), columnDefinition=varchar(20) default 'OPEN', @Builder.Default=OPEN
- **Relationships:** @ManyToOne -> DependencyScan
- **Indexes:** idx_vuln_scan_id (scan_id)
- **@Version:** No

### 6.16 ComplianceItem
- **File:** `src/main/java/com/codeops/entity/ComplianceItem.java`
- **Table:** `compliance_items`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `job` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`job_id`, nullable=false)
  - `requirement` — String, nullable=false, columnDefinition=TEXT
  - `spec` — Specification, @ManyToOne(LAZY), @JoinColumn(name=`spec_id`, nullable=true)
  - `status` — ComplianceStatus, @Enumerated(STRING), nullable=false
  - `evidence` — String, columnDefinition=TEXT
  - `agentType` — AgentType, column=`agent_type`, @Enumerated(STRING)
  - `notes` — String, columnDefinition=TEXT
- **Relationships:** @ManyToOne -> QaJob, @ManyToOne -> Specification
- **Indexes:** idx_compliance_job_id (job_id)
- **@Version:** No

### 6.17 TechDebtItem
- **File:** `src/main/java/com/codeops/entity/TechDebtItem.java`
- **Table:** `tech_debt_items`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `project` — Project, @ManyToOne(LAZY), @JoinColumn(name=`project_id`, nullable=false)
  - `category` — DebtCategory, @Enumerated(STRING), nullable=false
  - `title` — String, nullable=false, length=500
  - `description` — String, columnDefinition=TEXT
  - `filePath` — String, column=`file_path`, length=500
  - `effortEstimate` — Effort, column=`effort_estimate`, @Enumerated(STRING)
  - `businessImpact` — BusinessImpact, column=`business_impact`, @Enumerated(STRING)
  - `status` — DebtStatus, @Enumerated(STRING), columnDefinition=varchar(20) default 'IDENTIFIED', @Builder.Default=IDENTIFIED
  - `firstDetectedJob` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`first_detected_job_id`, nullable=true)
  - `resolvedJob` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`resolved_job_id`, nullable=true)
  - `version` — Long, @Version
- **Relationships:** @ManyToOne -> Project, @ManyToOne -> QaJob (firstDetectedJob), @ManyToOne -> QaJob (resolvedJob)
- **Indexes:** idx_tech_debt_project_id (project_id)
- **@Version:** Yes (Long version)

### 6.18 RemediationTask
- **File:** `src/main/java/com/codeops/entity/RemediationTask.java`
- **Table:** `remediation_tasks`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `job` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`job_id`, nullable=false)
  - `taskNumber` — Integer, column=`task_number`, nullable=false
  - `title` — String, nullable=false, length=500
  - `description` — String, columnDefinition=TEXT
  - `promptMd` — String, column=`prompt_md`, columnDefinition=TEXT
  - `promptS3Key` — String, column=`prompt_s3_key`, length=500
  - `findings` — List<Finding>, @ManyToMany(LAZY), @JoinTable(name=`remediation_task_findings`, joinColumns=task_id, inverseJoinColumns=finding_id), @Builder.Default=new ArrayList<>()
  - `priority` — Priority, @Enumerated(STRING), nullable=false
  - `status` — TaskStatus, @Enumerated(STRING), columnDefinition=varchar(20) default 'PENDING', @Builder.Default=PENDING
  - `assignedTo` — User, @ManyToOne(LAZY), @JoinColumn(name=`assigned_to`, nullable=true)
  - `jiraKey` — String, column=`jira_key`, length=50
  - `version` — Long, @Version
- **Relationships:** @ManyToOne -> QaJob, @ManyToMany -> Finding (via remediation_task_findings), @ManyToOne -> User (assignedTo)
- **Indexes:** idx_task_job_id (job_id)
- **@Version:** Yes (Long version)

### 6.19 HealthSnapshot
- **File:** `src/main/java/com/codeops/entity/HealthSnapshot.java`
- **Table:** `health_snapshots`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `project` — Project, @ManyToOne(LAZY), @JoinColumn(name=`project_id`, nullable=false)
  - `job` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`job_id`, nullable=true)
  - `healthScore` — Integer, column=`health_score`, nullable=false
  - `findingsBySeverity` — String, column=`findings_by_severity`, columnDefinition=TEXT
  - `techDebtScore` — Integer, column=`tech_debt_score`
  - `dependencyScore` — Integer, column=`dependency_score`
  - `testCoveragePercent` — BigDecimal, column=`test_coverage_percent`, precision=5, scale=2
  - `capturedAt` — Instant, column=`captured_at`, nullable=false
- **Relationships:** @ManyToOne -> Project, @ManyToOne -> QaJob
- **Indexes:** idx_snapshot_project_id (project_id)
- **@Version:** No

### 6.20 HealthSchedule
- **File:** `src/main/java/com/codeops/entity/HealthSchedule.java`
- **Table:** `health_schedules`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `project` — Project, @ManyToOne(LAZY), @JoinColumn(name=`project_id`, nullable=false)
  - `scheduleType` — ScheduleType, column=`schedule_type`, @Enumerated(STRING), nullable=false
  - `cronExpression` — String, column=`cron_expression`, length=50
  - `agentTypes` — String, column=`agent_types`, nullable=false, columnDefinition=TEXT
  - `isActive` — Boolean, column=`is_active`, nullable=false, columnDefinition=boolean default true, @Builder.Default=true
  - `lastRunAt` — Instant, column=`last_run_at`
  - `nextRunAt` — Instant, column=`next_run_at`
  - `createdBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`created_by`, nullable=false)
- **Relationships:** @ManyToOne -> Project, @ManyToOne -> User (createdBy)
- **Indexes:** idx_schedule_project_id (project_id)
- **@Version:** No

### 6.21 Specification
- **File:** `src/main/java/com/codeops/entity/Specification.java`
- **Table:** `specifications`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `job` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`job_id`, nullable=false)
  - `name` — String, nullable=false, length=200
  - `specType` — SpecType, column=`spec_type`, @Enumerated(STRING), nullable=false
  - `s3Key` — String, column=`s3_key`, nullable=false, length=500
- **Relationships:** @ManyToOne -> QaJob
- **Indexes:** idx_spec_job_id (job_id)
- **@Version:** No

### 6.22 NotificationPreference
- **File:** `src/main/java/com/codeops/entity/NotificationPreference.java`
- **Table:** `notification_preferences`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `user` — User, @ManyToOne(LAZY), @JoinColumn(name=`user_id`, nullable=false)
  - `eventType` — String, column=`event_type`, nullable=false, length=50
  - `inApp` — Boolean, column=`in_app`, nullable=false, columnDefinition=boolean default true, @Builder.Default=true
  - `email` — Boolean, nullable=false, columnDefinition=boolean default false, @Builder.Default=false
- **Relationships:** @ManyToOne -> User
- **Indexes:** idx_notif_user_id (user_id)
- **Unique Constraints:** UC(user_id, event_type)
- **@Version:** No

### 6.23 SystemSetting
- **File:** `src/main/java/com/codeops/entity/SystemSetting.java`
- **Table:** `system_settings`
- **Extends:** None (does NOT extend BaseEntity)
- **PK:** String settingKey, column=`key`, @Id, length=100
- **Fields:**
  - `settingKey` — String, @Id, column=`key`, length=100
  - `value` — String, nullable=false, columnDefinition=TEXT
  - `updatedBy` — User, @ManyToOne(LAZY), @JoinColumn(name=`updated_by`, nullable=true)
  - `updatedAt` — Instant, column=`updated_at`, nullable=false
- **Relationships:** @ManyToOne -> User (updatedBy)
- **Indexes:** None declared
- **@Version:** No

### 6.24 AuditLog
- **File:** `src/main/java/com/codeops/entity/AuditLog.java`
- **Table:** `audit_log`
- **Extends:** None (does NOT extend BaseEntity)
- **PK:** Long id, @GeneratedValue(IDENTITY)
- **Fields:**
  - `id` — Long, @Id, @GeneratedValue(IDENTITY)
  - `user` — User, @ManyToOne(LAZY), @JoinColumn(name=`user_id`, nullable=true)
  - `team` — Team, @ManyToOne(LAZY), @JoinColumn(name=`team_id`, nullable=true)
  - `action` — String, nullable=false, length=50
  - `entityType` — String, column=`entity_type`, length=30
  - `entityId` — UUID, column=`entity_id`
  - `details` — String, columnDefinition=TEXT
  - `ipAddress` — String, column=`ip_address`, length=45
  - `createdAt` — Instant, column=`created_at`, nullable=false
- **Relationships:** @ManyToOne -> User, @ManyToOne -> Team
- **Indexes:** idx_audit_user_id (user_id), idx_audit_team_id (team_id)
- **@Version:** No

### 6.25 AgentRun
- **File:** `src/main/java/com/codeops/entity/AgentRun.java`
- **Table:** `agent_runs`
- **Extends:** BaseEntity
- **PK:** UUID id (inherited)
- **Fields:**
  - `job` — QaJob, @ManyToOne(LAZY), @JoinColumn(name=`job_id`, nullable=false)
  - `agentType` — AgentType, column=`agent_type`, @Enumerated(STRING), nullable=false
  - `status` — AgentStatus, @Enumerated(STRING), nullable=false
  - `result` — AgentResult, @Enumerated(STRING)
  - `reportS3Key` — String, column=`report_s3_key`, length=500
  - `score` — Integer
  - `findingsCount` — Integer, column=`findings_count`, @Builder.Default=0
  - `criticalCount` — Integer, column=`critical_count`, @Builder.Default=0
  - `highCount` — Integer, column=`high_count`, @Builder.Default=0
  - `startedAt` — Instant, column=`started_at`
  - `completedAt` — Instant, column=`completed_at`
  - `version` — Long, @Version
- **Relationships:** @ManyToOne -> QaJob
- **Indexes:** idx_agent_run_job_id (job_id)
- **@Version:** Yes (Long version)

### 6.26 MfaEmailCode
- **File:** `src/main/java/com/codeops/entity/MfaEmailCode.java`
- **Table:** `mfa_email_codes`
- **Extends:** None (does NOT extend BaseEntity)
- **PK:** UUID id, @Id, @GeneratedValue(UUID)
- **Fields:**
  - `id` — UUID, @Id, @GeneratedValue(UUID), nullable=false, updatable=false
  - `userId` — UUID, column=`user_id`, nullable=false
  - `codeHash` — String, column=`code_hash`, nullable=false, length=255
  - `expiresAt` — Instant, column=`expires_at`, nullable=false
  - `used` — boolean (primitive), nullable=false, @Builder.Default=false
  - `createdAt` — Instant, column=`created_at`, nullable=false, @Builder.Default=Instant.now()
- **Relationships:** None (userId stored as plain UUID, not a @ManyToOne)
- **Indexes:** None declared
- **@Version:** No

---


### Registry Module


```
=== ServiceRegistration ===
Table: service_registrations
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  teamId: UUID @Column(name="team_id", nullable=false)
  name: String @Column(name="name", nullable=false, length=100)
  slug: String @Column(name="slug", nullable=false, length=63)
  serviceType: ServiceType @Enumerated(STRING) @Column(name="service_type", nullable=false, length=30)
  description: String @Column(name="description", columnDefinition="TEXT") — nullable
  repoUrl: String @Column(name="repo_url", length=500) — nullable
  repoFullName: String @Column(name="repo_full_name", length=200) — nullable
  defaultBranch: String @Column(name="default_branch", length=50) — default "main"
  techStack: String @Column(name="tech_stack", length=500) — nullable
  status: ServiceStatus @Enumerated(STRING) @Column(name="status", nullable=false, length=20) — default ACTIVE
  healthCheckUrl: String @Column(name="health_check_url", length=500) — nullable
  healthCheckIntervalSeconds: Integer @Column(name="health_check_interval_seconds") — default 30
  lastHealthStatus: HealthStatus @Enumerated(STRING) @Column(name="last_health_status", length=20) — nullable
  lastHealthCheckAt: Instant @Column(name="last_health_check_at") — nullable
  environmentsJson: String @Column(name="environments_json", columnDefinition="TEXT") — nullable
  metadataJson: String @Column(name="metadata_json", columnDefinition="TEXT") — nullable
  createdByUserId: UUID @Column(name="created_by_user_id", nullable=false)
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships:
  @OneToMany(mappedBy="service", cascade=ALL, orphanRemoval=true) → PortAllocation (portAllocations)
  @OneToMany(mappedBy="sourceService") → ServiceDependency (dependenciesAsSource)
  @OneToMany(mappedBy="targetService") → ServiceDependency (dependenciesAsTarget)
  @OneToMany(mappedBy="service", cascade=ALL, orphanRemoval=true) → ApiRouteRegistration (routes)
  @OneToMany(mappedBy="service") → SolutionMember (solutionMemberships)
  @OneToMany(mappedBy="service", cascade=ALL, orphanRemoval=true) → ConfigTemplate (configTemplates)
  @OneToMany(mappedBy="service", cascade=ALL, orphanRemoval=true) → EnvironmentConfig (environmentConfigs)
Indexes:
  idx_sr_team_id on (team_id)
  idx_sr_status on (status)
Unique Constraints:
  uk_sr_team_slug on (team_id, slug)
Version: No


=== Solution ===
Table: solutions
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  teamId: UUID @Column(name="team_id", nullable=false)
  name: String @Column(name="name", nullable=false, length=200)
  slug: String @Column(name="slug", nullable=false, length=63)
  description: String @Column(name="description", columnDefinition="TEXT") — nullable
  category: SolutionCategory @Enumerated(STRING) @Column(name="category", nullable=false, length=30)
  status: SolutionStatus @Enumerated(STRING) @Column(name="status", nullable=false, length=20) — default ACTIVE
  iconName: String @Column(name="icon_name", length=50) — nullable
  colorHex: String @Column(name="color_hex", length=7) — nullable
  ownerUserId: UUID @Column(name="owner_user_id") — nullable
  repositoryUrl: String @Column(name="repository_url", length=500) — nullable
  documentationUrl: String @Column(name="documentation_url", length=500) — nullable
  metadataJson: String @Column(name="metadata_json", columnDefinition="TEXT") — nullable
  createdByUserId: UUID @Column(name="created_by_user_id", nullable=false)
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships:
  @OneToMany(mappedBy="solution", cascade=ALL, orphanRemoval=true) → SolutionMember (members)
Indexes:
  idx_sol_team_id on (team_id)
Unique Constraints:
  uk_sol_team_slug on (team_id, slug)
Version: No


=== SolutionMember ===
Table: solution_members
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  role: SolutionMemberRole @Enumerated(STRING) @Column(name="role", nullable=false, length=30)
  displayOrder: Integer @Column(name="display_order") — default 0
  notes: String @Column(name="notes", length=500) — nullable
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships:
  @ManyToOne(fetch=LAZY) → Solution @JoinColumn(name="solution_id", nullable=false)
  @ManyToOne(fetch=LAZY) → ServiceRegistration @JoinColumn(name="service_id", nullable=false)
Indexes:
  idx_sm_solution_id on (solution_id)
  idx_sm_service_id on (service_id)
Unique Constraints:
  uk_sm_solution_service on (solution_id, service_id)
Version: No


=== PortAllocation ===
Table: port_allocations
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  environment: String @Column(name="environment", nullable=false, length=50)
  portType: PortType @Enumerated(STRING) @Column(name="port_type", nullable=false, length=30)
  portNumber: Integer @Column(name="port_number", nullable=false)
  protocol: String @Column(name="protocol", length=10) — default "TCP"
  description: String @Column(name="description", length=200) — nullable
  isAutoAllocated: Boolean @Column(name="is_auto_allocated", nullable=false) — default true
  allocatedByUserId: UUID @Column(name="allocated_by_user_id", nullable=false)
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships:
  @ManyToOne(fetch=LAZY) → ServiceRegistration @JoinColumn(name="service_id", nullable=false)
Indexes:
  idx_pa_service_id on (service_id)
  idx_pa_environment on (environment)
  idx_pa_port_number on (port_number)
Unique Constraints:
  uk_pa_service_env_port on (service_id, environment, port_number)
Version: No


=== PortRange ===
Table: port_ranges
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  teamId: UUID @Column(name="team_id", nullable=false)
  portType: PortType @Enumerated(STRING) @Column(name="port_type", nullable=false, length=30)
  rangeStart: Integer @Column(name="range_start", nullable=false)
  rangeEnd: Integer @Column(name="range_end", nullable=false)
  environment: String @Column(name="environment", nullable=false, length=50)
  description: String @Column(name="description", length=200) — nullable
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships: None
Indexes:
  idx_pr_team_id on (team_id)
Unique Constraints:
  uk_pr_team_type_env on (team_id, port_type, environment)
Version: No


=== ServiceDependency ===
Table: service_dependencies
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  dependencyType: DependencyType @Enumerated(STRING) @Column(name="dependency_type", nullable=false, length=30)
  description: String @Column(name="description", length=500) — nullable
  isRequired: Boolean @Column(name="is_required", nullable=false) — default true
  targetEndpoint: String @Column(name="target_endpoint", length=500) — nullable
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships:
  @ManyToOne(fetch=LAZY) → ServiceRegistration @JoinColumn(name="source_service_id", nullable=false) (sourceService)
  @ManyToOne(fetch=LAZY) → ServiceRegistration @JoinColumn(name="target_service_id", nullable=false) (targetService)
Indexes:
  idx_sd_source_id on (source_service_id)
  idx_sd_target_id on (target_service_id)
Unique Constraints:
  uk_sd_source_target_type on (source_service_id, target_service_id, dependency_type)
Version: No


=== ApiRouteRegistration ===
Table: api_route_registrations
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  routePrefix: String @Column(name="route_prefix", nullable=false, length=200)
  httpMethods: String @Column(name="http_methods", length=100) — nullable
  environment: String @Column(name="environment", nullable=false, length=50)
  description: String @Column(name="description", length=500) — nullable
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships:
  @ManyToOne(fetch=LAZY) → ServiceRegistration @JoinColumn(name="service_id", nullable=false) (service)
  @ManyToOne(fetch=LAZY) → ServiceRegistration @JoinColumn(name="gateway_service_id") (gatewayService) — nullable
Indexes:
  idx_arr_service_id on (service_id)
  idx_arr_gateway_id on (gateway_service_id)
Unique Constraints: None (no @UniqueConstraint declared)
Version: No


=== ConfigTemplate ===
Table: config_templates
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  templateType: ConfigTemplateType @Enumerated(STRING) @Column(name="template_type", nullable=false, length=30)
  environment: String @Column(name="environment", nullable=false, length=50)
  contentText: String @Column(name="content_text", nullable=false, columnDefinition="TEXT")
  isAutoGenerated: Boolean @Column(name="is_auto_generated", nullable=false) — default true
  generatedFrom: String @Column(name="generated_from", length=200) — nullable
  version: Integer @Column(name="version", nullable=false) — default 1
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships:
  @ManyToOne(fetch=LAZY) → ServiceRegistration @JoinColumn(name="service_id", nullable=false) (service)
Indexes:
  idx_ct_service_id on (service_id)
Unique Constraints:
  uk_ct_service_type_env on (service_id, template_type, environment)
Version: No (field named "version" is application-level versioning, not @Version)


=== EnvironmentConfig ===
Table: environment_configs
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  environment: String @Column(name="environment", nullable=false, length=50)
  configKey: String @Column(name="config_key", nullable=false, length=200)
  configValue: String @Column(name="config_value", nullable=false, columnDefinition="TEXT")
  configSource: ConfigSource @Enumerated(STRING) @Column(name="config_source", nullable=false, length=20)
  description: String @Column(name="description", length=500) — nullable
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships:
  @ManyToOne(fetch=LAZY) → ServiceRegistration @JoinColumn(name="service_id", nullable=false) (service)
Indexes:
  idx_ec_service_id on (service_id)
Unique Constraints:
  uk_ec_service_env_key on (service_id, environment, config_key)
Version: No


=== InfraResource ===
Table: infra_resources
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  teamId: UUID @Column(name="team_id", nullable=false)
  resourceType: InfraResourceType @Enumerated(STRING) @Column(name="resource_type", nullable=false, length=30)
  resourceName: String @Column(name="resource_name", nullable=false, length=300)
  environment: String @Column(name="environment", nullable=false, length=50)
  region: String @Column(name="region", length=30) — nullable
  arnOrUrl: String @Column(name="arn_or_url", length=500) — nullable
  metadataJson: String @Column(name="metadata_json", columnDefinition="TEXT") — nullable
  description: String @Column(name="description", length=500) — nullable
  createdByUserId: UUID @Column(name="created_by_user_id", nullable=false)
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships:
  @ManyToOne(fetch=LAZY) → ServiceRegistration @JoinColumn(name="service_id") (service) — nullable
Indexes:
  idx_ir_team_id on (team_id)
  idx_ir_service_id on (service_id)
Unique Constraints:
  uk_ir_team_type_name_env on (team_id, resource_type, resource_name, environment)
Version: No


=== WorkstationProfile ===
Table: workstation_profiles
PK: id: UUID (GenerationType.UUID) — inherited from BaseEntity
Fields:
  teamId: UUID @Column(name="team_id", nullable=false)
  name: String @Column(name="name", nullable=false, length=100)
  description: String @Column(name="description", columnDefinition="TEXT") — nullable
  solutionId: UUID @Column(name="solution_id") — nullable
  servicesJson: String @Column(name="services_json", nullable=false, columnDefinition="TEXT")
  startupOrder: String @Column(name="startup_order", columnDefinition="TEXT") — nullable
  createdByUserId: UUID @Column(name="created_by_user_id", nullable=false)
  isDefault: Boolean @Column(name="is_default", nullable=false) — default false
  createdAt: Instant — inherited from BaseEntity, nullable=false, updatable=false
  updatedAt: Instant — inherited from BaseEntity
Relationships: None
Indexes:
  idx_wp_team_id on (team_id)
Unique Constraints: None
Version: No
```


### Logger Module


All entities extend `BaseEntity` which provides:
- PK: `id` UUID (GenerationType.UUID)
- `createdAt`: Instant [@PrePersist, non-null, non-updatable]
- `updatedAt`: Instant [@PrePersist + @PreUpdate]

```
=== LogEntry ===
Table: log_entries
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  source: LogSource [@ManyToOne LAZY, @JoinColumn("source_id")]
  level: LogLevel [@Enumerated STRING, nullable=false, length=10]
  message: String [nullable=false, columnDefinition=TEXT]
  timestamp: Instant [nullable=false]
  serviceName: String [nullable=false, length=200] (denormalized from LogSource)
  correlationId: String [length=100]
  traceId: String [length=100]
  spanId: String [length=100]
  loggerName: String [length=500]
  threadName: String [length=200]
  exceptionClass: String [length=500]
  exceptionMessage: String [columnDefinition=TEXT]
  stackTrace: String [columnDefinition=TEXT]
  customFields: String [columnDefinition=TEXT] (JSON key-value pairs)
  hostName: String [length=200]
  ipAddress: String [length=45]
  teamId: UUID [nullable=false]
Relationships: @ManyToOne(LAZY) -> LogSource (via source_id)
Indexes:
  idx_log_entry_team_id (team_id)
  idx_log_entry_source_id (source_id)
  idx_log_entry_level (level)
  idx_log_entry_timestamp (timestamp)
  idx_log_entry_correlation_id (correlation_id)
  idx_log_entry_service_name (service_name)
  idx_log_entry_service_level_ts (service_name, level, timestamp)
Version: No

=== LogSource ===
Table: log_sources
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  name: String [nullable=false, length=200]
  serviceId: UUID [nullable] (reference to CodeOps-Registry service)
  description: String [columnDefinition=TEXT]
  environment: String [length=50]
  isActive: Boolean [nullable=false, default=true]
  teamId: UUID [nullable=false]
  lastLogReceivedAt: Instant [nullable]
  logCount: Long [nullable=false, default=0]
Relationships: None (standalone)
Indexes:
  idx_log_source_team_id (team_id)
  idx_log_source_name (name)
  idx_log_source_service_id (service_id)
Version: No

=== AlertRule ===
Table: alert_rules
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  name: String [nullable=false, length=200]
  trap: LogTrap [@ManyToOne LAZY, @JoinColumn("trap_id"), nullable=false]
  channel: AlertChannel [@ManyToOne LAZY, @JoinColumn("channel_id"), nullable=false]
  severity: AlertSeverity [@Enumerated STRING, nullable=false, length=20]
  isActive: Boolean [nullable=false, default=true]
  throttleMinutes: Integer [nullable=false, default=5]
  teamId: UUID [nullable=false]
Relationships:
  @ManyToOne(LAZY) -> LogTrap (via trap_id)
  @ManyToOne(LAZY) -> AlertChannel (via channel_id)
Indexes:
  idx_alert_rule_team_id (team_id)
  idx_alert_rule_trap_id (trap_id)
  idx_alert_rule_channel_id (channel_id)
Version: No

=== AlertChannel ===
Table: alert_channels
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  name: String [nullable=false, length=200]
  channelType: AlertChannelType [@Enumerated STRING, nullable=false, length=20]
  configuration: String [nullable=false, columnDefinition=TEXT] (JSON config per channel type)
  isActive: Boolean [nullable=false, default=true]
  teamId: UUID [nullable=false]
  createdBy: UUID [nullable=false]
Relationships: None (standalone)
Indexes:
  idx_alert_channel_team_id (team_id)
  idx_alert_channel_type (channel_type)
Version: No

=== AlertHistory ===
Table: alert_history
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  rule: AlertRule [@ManyToOne LAZY, @JoinColumn("rule_id"), nullable=false]
  trap: LogTrap [@ManyToOne LAZY, @JoinColumn("trap_id"), nullable=false]
  channel: AlertChannel [@ManyToOne LAZY, @JoinColumn("channel_id"), nullable=false]
  severity: AlertSeverity [@Enumerated STRING, nullable=false, length=20]
  status: AlertStatus [@Enumerated STRING, nullable=false, length=20, default=FIRED]
  message: String [columnDefinition=TEXT]
  acknowledgedBy: UUID [nullable]
  acknowledgedAt: Instant [nullable]
  resolvedBy: UUID [nullable]
  resolvedAt: Instant [nullable]
  teamId: UUID [nullable=false]
Relationships:
  @ManyToOne(LAZY) -> AlertRule (via rule_id)
  @ManyToOne(LAZY) -> LogTrap (via trap_id)
  @ManyToOne(LAZY) -> AlertChannel (via channel_id)
Indexes:
  idx_alert_history_team_id (team_id)
  idx_alert_history_rule_id (rule_id)
  idx_alert_history_status (status)
  idx_alert_history_severity (severity)
  idx_alert_history_created_at (created_at)
Version: No

=== TraceSpan ===
Table: trace_spans
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  correlationId: String [nullable=false, length=100]
  traceId: String [nullable=false, length=100]
  spanId: String [nullable=false, length=100]
  parentSpanId: String [length=100, nullable] (null for root span)
  serviceName: String [nullable=false, length=200]
  operationName: String [nullable=false, length=500]
  startTime: Instant [nullable=false]
  endTime: Instant [nullable]
  durationMs: Long [nullable] (computed: endTime - startTime)
  status: SpanStatus [@Enumerated STRING, nullable=false, length=10, default=OK]
  statusMessage: String [columnDefinition=TEXT]
  tags: String [columnDefinition=TEXT] (JSON key-value)
  teamId: UUID [nullable=false]
Relationships: None (standalone)
Indexes:
  idx_trace_span_team_id (team_id)
  idx_trace_span_correlation_id (correlation_id)
  idx_trace_span_trace_id (trace_id)
  idx_trace_span_service_name (service_name)
  idx_trace_span_start_time (start_time)
  idx_trace_span_parent_span_id (parent_span_id)
Version: No

=== RetentionPolicy ===
Table: retention_policies
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  name: String [nullable=false, length=200]
  sourceName: String [length=200, nullable] (optional source filter)
  logLevel: LogLevel [@Enumerated STRING, length=10, nullable] (optional level filter)
  retentionDays: Integer [nullable=false]
  action: RetentionAction [@Enumerated STRING, nullable=false, length=20]
  archiveDestination: String [length=500, nullable] (S3 path for ARCHIVE)
  isActive: Boolean [nullable=false, default=true]
  teamId: UUID [nullable=false]
  createdBy: UUID [nullable=false]
  lastExecutedAt: Instant [nullable]
Relationships: None (standalone)
Indexes:
  idx_retention_policy_team_id (team_id)
  idx_retention_policy_is_active (is_active)
Version: No

=== Dashboard ===
Table: dashboards
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  name: String [nullable=false, length=200]
  description: String [columnDefinition=TEXT]
  teamId: UUID [nullable=false]
  createdBy: UUID [nullable=false]
  isShared: Boolean [nullable=false, default=true]
  isTemplate: Boolean [nullable=false, default=false]
  refreshIntervalSeconds: Integer [default=30]
  layoutJson: String [columnDefinition=TEXT] (JSON grid layout config)
  widgets: List<DashboardWidget> [@OneToMany mappedBy="dashboard", CascadeType.ALL, orphanRemoval=true, LAZY, default=empty]
Relationships: @OneToMany -> DashboardWidget (cascade ALL, orphan removal)
Indexes:
  idx_dashboard_team_id (team_id)
  idx_dashboard_created_by (created_by)
Version: No

=== DashboardWidget ===
Table: dashboard_widgets
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  dashboard: Dashboard [@ManyToOne LAZY, @JoinColumn("dashboard_id"), nullable=false]
  title: String [nullable=false, length=200]
  widgetType: WidgetType [@Enumerated STRING, nullable=false, length=30]
  queryJson: String [columnDefinition=TEXT] (JSON data query binding)
  configJson: String [columnDefinition=TEXT] (JSON widget config)
  gridX: Integer [nullable=false, default=0]
  gridY: Integer [nullable=false, default=0]
  gridWidth: Integer [nullable=false, default=4]
  gridHeight: Integer [nullable=false, default=3]
  sortOrder: Integer [nullable=false, default=0]
Relationships: @ManyToOne(LAZY) -> Dashboard (via dashboard_id)
Indexes:
  idx_widget_dashboard_id (dashboard_id)
Version: No

=== LogTrap ===
Table: log_traps
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  name: String [nullable=false, length=200]
  description: String [columnDefinition=TEXT]
  trapType: TrapType [@Enumerated STRING, nullable=false, length=20]
  isActive: Boolean [nullable=false, default=true]
  teamId: UUID [nullable=false]
  createdBy: UUID [nullable=false]
  lastTriggeredAt: Instant [nullable]
  triggerCount: Long [nullable=false, default=0]
  conditions: List<TrapCondition> [@OneToMany mappedBy="trap", CascadeType.ALL, orphanRemoval=true, LAZY, default=empty]
Relationships: @OneToMany -> TrapCondition (cascade ALL, orphan removal)
Indexes:
  idx_log_trap_team_id (team_id)
  idx_log_trap_is_active (is_active)
Version: No

=== TrapCondition ===
Table: trap_conditions
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  trap: LogTrap [@ManyToOne LAZY, @JoinColumn("trap_id"), nullable=false]
  conditionType: ConditionType [@Enumerated STRING, nullable=false, length=30]
  field: String [nullable=false, length=100] (log field to evaluate)
  pattern: String [columnDefinition=TEXT, nullable] (regex/keyword for REGEX/KEYWORD types)
  threshold: Integer [nullable] (count threshold for FREQUENCY_THRESHOLD)
  windowSeconds: Integer [nullable] (time window for FREQUENCY_THRESHOLD and ABSENCE)
  serviceName: String [length=200, nullable] (optional service filter)
  logLevel: LogLevel [@Enumerated STRING, length=10, nullable] (optional level filter)
Relationships: @ManyToOne(LAZY) -> LogTrap (via trap_id)
Indexes:
  idx_trap_condition_trap_id (trap_id)
Version: No

=== SavedQuery ===
Table: saved_queries
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  name: String [nullable=false, length=200]
  description: String [columnDefinition=TEXT]
  queryJson: String [nullable=false, columnDefinition=TEXT] (serialized query params)
  queryDsl: String [columnDefinition=TEXT, nullable] (optional SQL-like DSL string)
  teamId: UUID [nullable=false]
  createdBy: UUID [nullable=false]
  isShared: Boolean [nullable=false, default=false]
  lastExecutedAt: Instant [nullable]
  executionCount: Long [nullable=false, default=0]
Relationships: None (standalone)
Indexes:
  idx_saved_query_team_id (team_id)
  idx_saved_query_created_by (created_by)
Version: No

=== QueryHistory ===
Table: query_history
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  queryJson: String [nullable=false, columnDefinition=TEXT]
  queryDsl: String [columnDefinition=TEXT, nullable]
  resultCount: Long [nullable=false]
  executionTimeMs: Long [nullable=false]
  teamId: UUID [nullable=false]
  createdBy: UUID [nullable=false]
Relationships: None (standalone)
Indexes:
  idx_query_history_team_id (team_id)
  idx_query_history_created_by (created_by)
  idx_query_history_created_at (created_at)
Version: No

=== Metric ===
Table: metrics
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  name: String [nullable=false, length=200]
  metricType: MetricType [@Enumerated STRING, nullable=false, length=20]
  description: String [columnDefinition=TEXT]
  unit: String [length=50, nullable] (e.g., "ms", "bytes", "count", "percent")
  serviceName: String [nullable=false, length=200]
  tags: String [columnDefinition=TEXT, nullable] (JSON key-value labels)
  teamId: UUID [nullable=false]
Relationships: None (standalone)
Indexes:
  idx_metric_team_id (team_id)
  idx_metric_service_name (service_name)
  idx_metric_name_service (name, service_name)
Unique Constraints:
  uk_metric_name_service_team (name, service_name, team_id)
Version: No

=== MetricSeries ===
Table: metric_series
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  metric: Metric [@ManyToOne LAZY, @JoinColumn("metric_id"), nullable=false]
  timestamp: Instant [nullable=false]
  value: Double [nullable=false]
  tags: String [columnDefinition=TEXT, nullable] (JSON dimension tags)
  resolution: Integer [nullable=false] (aggregation window in seconds)
Relationships: @ManyToOne(LAZY) -> Metric (via metric_id)
Indexes:
  idx_metric_series_metric_id (metric_id)
  idx_metric_series_timestamp (timestamp)
  idx_metric_series_metric_ts (metric_id, timestamp)
Version: No

=== AnomalyBaseline ===
Table: anomaly_baselines
PK: id, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
  serviceName: String [nullable=false, length=200]
  metricName: String [nullable=false, length=200]
  baselineValue: Double [nullable=false]
  standardDeviation: Double [nullable=false]
  sampleCount: Long [nullable=false]
  windowStartTime: Instant [nullable=false]
  windowEndTime: Instant [nullable=false]
  deviationThreshold: Double [nullable=false, default=2.0]
  isActive: Boolean [nullable=false, default=true]
  teamId: UUID [nullable=false]
  lastComputedAt: Instant [nullable]
Relationships: None (standalone)
Indexes:
  idx_anomaly_baseline_team_id (team_id)
  idx_anomaly_baseline_service (service_name)
  idx_anomaly_baseline_metric (service_name, metric_name)
Version: No
```

---


### Courier Module


**Package:** `com.codeops.courier.entity`
**Superclass:** All entities extend `com.codeops.entity.BaseEntity` (provides `id UUID PK`, `createdAt Instant`, `updatedAt Instant`)

---

### 6.1 Collection

- **Table:** `collections`
- **PK:** `id UUID` (inherited from BaseEntity)
- **Unique Constraints:** `UC(team_id, name)`
- **Fields:**
  - `teamId` -- `UUID`, `@Column(name = "team_id", nullable = false)`
  - `name` -- `String`, `@Column(nullable = false, length = 200)`
  - `description` -- `String`, `@Column(length = 2000)`
  - `preRequestScript` -- `String`, `@Column(name = "pre_request_script", columnDefinition = "TEXT")`
  - `postResponseScript` -- `String`, `@Column(name = "post_response_script", columnDefinition = "TEXT")`
  - `authType` -- `AuthType`, `@Enumerated(EnumType.STRING)`, `@Column(name = "auth_type")`
  - `authConfig` -- `String`, `@Column(name = "auth_config", columnDefinition = "TEXT")`
  - `isShared` -- `boolean`, `@Column(name = "is_shared", nullable = false)`, default `false`
  - `createdBy` -- `UUID`, `@Column(name = "created_by", nullable = false)`
- **Relationships:**
  - `folders` -- `@OneToMany(mappedBy = "collection", cascade = ALL, orphanRemoval = true)` -> `List<Folder>`
  - `variables` -- `@OneToMany(mappedBy = "collection", cascade = ALL, orphanRemoval = true)` -> `List<EnvironmentVariable>`
- **Indexes:**
  - `idx_collections_team_id` on `team_id`
  - `idx_collections_created_by` on `created_by`
- **@Version:** No

---

### 6.2 Folder

- **Table:** `folders`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None
- **Fields:**
  - `name` -- `String`, `@Column(nullable = false, length = 200)`
  - `description` -- `String`, `@Column(length = 2000)`
  - `sortOrder` -- `int`, `@Column(name = "sort_order", nullable = false)`, default `0`
  - `preRequestScript` -- `String`, `@Column(name = "pre_request_script", columnDefinition = "TEXT")`
  - `postResponseScript` -- `String`, `@Column(name = "post_response_script", columnDefinition = "TEXT")`
  - `authType` -- `AuthType`, `@Enumerated(EnumType.STRING)`, `@Column(name = "auth_type")`
  - `authConfig` -- `String`, `@Column(name = "auth_config", columnDefinition = "TEXT")`
- **Relationships:**
  - `collection` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "collection_id", nullable = false)` -> `Collection`
  - `parentFolder` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "parent_folder_id")` -> `Folder` (self-referential, nullable for root folders)
  - `subFolders` -- `@OneToMany(mappedBy = "parentFolder", cascade = ALL, orphanRemoval = true)` -> `List<Folder>`
  - `requests` -- `@OneToMany(mappedBy = "folder", cascade = ALL, orphanRemoval = true)` -> `List<Request>`
- **Indexes:**
  - `idx_folders_collection_id` on `collection_id`
  - `idx_folders_parent_folder_id` on `parent_folder_id`
- **@Version:** No

---

### 6.3 Request

- **Table:** `requests`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None
- **Fields:**
  - `name` -- `String`, `@Column(nullable = false, length = 200)`
  - `description` -- `String`, `@Column(length = 2000)`
  - `method` -- `HttpMethod`, `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`
  - `url` -- `String`, `@Column(nullable = false, length = 2000)`
  - `sortOrder` -- `int`, `@Column(name = "sort_order", nullable = false)`, default `0`
- **Relationships:**
  - `folder` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "folder_id", nullable = false)` -> `Folder`
  - `headers` -- `@OneToMany(mappedBy = "request", cascade = ALL, orphanRemoval = true)` -> `List<RequestHeader>`
  - `params` -- `@OneToMany(mappedBy = "request", cascade = ALL, orphanRemoval = true)` -> `List<RequestParam>`
  - `body` -- `@OneToOne(mappedBy = "request", cascade = ALL, orphanRemoval = true)` -> `RequestBody`
  - `auth` -- `@OneToOne(mappedBy = "request", cascade = ALL, orphanRemoval = true)` -> `RequestAuth`
  - `scripts` -- `@OneToMany(mappedBy = "request", cascade = ALL, orphanRemoval = true)` -> `List<RequestScript>`
- **Indexes:**
  - `idx_requests_folder_id` on `folder_id`
- **@Version:** No

---

### 6.4 RequestHeader

- **Table:** `request_headers`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None
- **Fields:**
  - `headerKey` -- `String`, `@Column(name = "header_key", nullable = false, length = 500)`
  - `headerValue` -- `String`, `@Column(name = "header_value", length = 5000)`
  - `description` -- `String`, `@Column(length = 500)`
  - `isEnabled` -- `boolean`, `@Column(name = "is_enabled", nullable = false)`, default `true`
- **Relationships:**
  - `request` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "request_id", nullable = false)` -> `Request`
- **Indexes:**
  - `idx_request_headers_request_id` on `request_id`
- **@Version:** No

---

### 6.5 RequestParam

- **Table:** `request_params`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None
- **Fields:**
  - `paramKey` -- `String`, `@Column(name = "param_key", nullable = false, length = 500)`
  - `paramValue` -- `String`, `@Column(name = "param_value", length = 5000)`
  - `description` -- `String`, `@Column(length = 500)`
  - `isEnabled` -- `boolean`, `@Column(name = "is_enabled", nullable = false)`, default `true`
- **Relationships:**
  - `request` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "request_id", nullable = false)` -> `Request`
- **Indexes:**
  - `idx_request_params_request_id` on `request_id`
- **@Version:** No

---

### 6.6 RequestBody

- **Table:** `request_bodies`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None (uniqueness enforced by `@JoinColumn(unique = true)` on request_id)
- **Fields:**
  - `bodyType` -- `BodyType`, `@Enumerated(EnumType.STRING)`, `@Column(name = "body_type", nullable = false)`
  - `rawContent` -- `String`, `@Column(name = "raw_content", columnDefinition = "TEXT")`
  - `formData` -- `String`, `@Column(name = "form_data", columnDefinition = "TEXT")`
  - `graphqlQuery` -- `String`, `@Column(name = "graphql_query", columnDefinition = "TEXT")`
  - `graphqlVariables` -- `String`, `@Column(name = "graphql_variables", columnDefinition = "TEXT")`
  - `binaryFileName` -- `String`, `@Column(name = "binary_file_name", length = 500)`
- **Relationships:**
  - `request` -- `@OneToOne(fetch = LAZY)`, `@JoinColumn(name = "request_id", nullable = false, unique = true)` -> `Request`
- **Indexes:** None explicit (unique constraint on request_id creates implicit index)
- **@Version:** No

---

### 6.7 RequestAuth

- **Table:** `request_auths`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None (uniqueness enforced by `@JoinColumn(unique = true)` on request_id)
- **Fields:**
  - `authType` -- `AuthType`, `@Enumerated(EnumType.STRING)`, `@Column(name = "auth_type", nullable = false)`
  - `apiKeyHeader` -- `String`, `@Column(name = "api_key_header", length = 200)`
  - `apiKeyValue` -- `String`, `@Column(name = "api_key_value", length = 2000)`
  - `apiKeyAddTo` -- `String`, `@Column(name = "api_key_add_to", length = 20)`
  - `bearerToken` -- `String`, `@Column(name = "bearer_token", length = 5000)`
  - `basicUsername` -- `String`, `@Column(name = "basic_username", length = 500)`
  - `basicPassword` -- `String`, `@Column(name = "basic_password", length = 500)`
  - `oauth2GrantType` -- `String`, `@Column(name = "oauth2_grant_type", length = 50)`
  - `oauth2AuthUrl` -- `String`, `@Column(name = "oauth2_auth_url", length = 2000)`
  - `oauth2TokenUrl` -- `String`, `@Column(name = "oauth2_token_url", length = 2000)`
  - `oauth2ClientId` -- `String`, `@Column(name = "oauth2_client_id", length = 500)`
  - `oauth2ClientSecret` -- `String`, `@Column(name = "oauth2_client_secret", length = 500)`
  - `oauth2Scope` -- `String`, `@Column(name = "oauth2_scope", length = 1000)`
  - `oauth2CallbackUrl` -- `String`, `@Column(name = "oauth2_callback_url", length = 2000)`
  - `oauth2AccessToken` -- `String`, `@Column(name = "oauth2_access_token", length = 5000)`
  - `jwtSecret` -- `String`, `@Column(name = "jwt_secret", length = 2000)`
  - `jwtPayload` -- `String`, `@Column(name = "jwt_payload", columnDefinition = "TEXT")`
  - `jwtAlgorithm` -- `String`, `@Column(name = "jwt_algorithm", length = 20)`
- **Relationships:**
  - `request` -- `@OneToOne(fetch = LAZY)`, `@JoinColumn(name = "request_id", nullable = false, unique = true)` -> `Request`
- **Indexes:** None explicit
- **@Version:** No

---

### 6.8 RequestScript

- **Table:** `request_scripts`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** `UC(request_id, script_type)`
- **Fields:**
  - `scriptType` -- `ScriptType`, `@Enumerated(EnumType.STRING)`, `@Column(name = "script_type", nullable = false)`
  - `content` -- `String`, `@Column(columnDefinition = "TEXT")`
- **Relationships:**
  - `request` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "request_id", nullable = false)` -> `Request`
- **Indexes:**
  - `idx_request_scripts_request_id` on `request_id`
- **@Version:** No

---

### 6.9 RequestHistory

- **Table:** `request_history`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None
- **Fields:**
  - `teamId` -- `UUID`, `@Column(name = "team_id", nullable = false)`
  - `userId` -- `UUID`, `@Column(name = "user_id", nullable = false)`
  - `requestMethod` -- `HttpMethod`, `@Enumerated(EnumType.STRING)`, `@Column(name = "request_method", nullable = false)`
  - `requestUrl` -- `String`, `@Column(name = "request_url", nullable = false, length = 2000)`
  - `requestHeaders` -- `String`, `@Column(name = "request_headers", columnDefinition = "TEXT")`
  - `requestBody` -- `String`, `@Column(name = "request_body", columnDefinition = "TEXT")`
  - `responseStatus` -- `Integer`, `@Column(name = "response_status")`
  - `responseHeaders` -- `String`, `@Column(name = "response_headers", columnDefinition = "TEXT")`
  - `responseBody` -- `String`, `@Column(name = "response_body", columnDefinition = "TEXT")`
  - `responseSizeBytes` -- `Long`, `@Column(name = "response_size_bytes")`
  - `responseTimeMs` -- `Long`, `@Column(name = "response_time_ms")`
  - `contentType` -- `String`, `@Column(name = "content_type", length = 200)`
  - `collectionId` -- `UUID`, `@Column(name = "collection_id")`
  - `requestId` -- `UUID`, `@Column(name = "request_id")`
  - `environmentId` -- `UUID`, `@Column(name = "environment_id")`
- **Relationships:** None (standalone, uses raw UUID references rather than JPA relationships)
- **Indexes:**
  - `idx_request_history_team_id` on `team_id`
  - `idx_request_history_user_id` on `user_id`
  - `idx_request_history_request_method` on `request_method`
  - `idx_request_history_created_at` on `created_at`
- **@Version:** No

---

### 6.10 Environment

- **Table:** `environments`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** `UC(team_id, name)`
- **Fields:**
  - `teamId` -- `UUID`, `@Column(name = "team_id", nullable = false)`
  - `name` -- `String`, `@Column(nullable = false, length = 200)`
  - `description` -- `String`, `@Column(length = 2000)`
  - `isActive` -- `boolean`, `@Column(name = "is_active", nullable = false)`, default `false`
  - `createdBy` -- `UUID`, `@Column(name = "created_by", nullable = false)`
- **Relationships:**
  - `variables` -- `@OneToMany(mappedBy = "environment", cascade = ALL, orphanRemoval = true)` -> `List<EnvironmentVariable>`
- **Indexes:**
  - `idx_environments_team_id` on `team_id`
- **@Version:** No

---

### 6.11 EnvironmentVariable

- **Table:** `environment_variables`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None
- **Fields:**
  - `variableKey` -- `String`, `@Column(name = "variable_key", nullable = false, length = 500)`
  - `variableValue` -- `String`, `@Column(name = "variable_value", length = 5000)`
  - `isSecret` -- `boolean`, `@Column(name = "is_secret", nullable = false)`, default `false`
  - `isEnabled` -- `boolean`, `@Column(name = "is_enabled", nullable = false)`, default `true`
  - `scope` -- `String`, `@Column(nullable = false, length = 20)`
- **Relationships:**
  - `environment` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "environment_id")` -> `Environment` (nullable)
  - `collection` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "collection_id")` -> `Collection` (nullable)
- **Indexes:**
  - `idx_env_variables_environment_id` on `environment_id`
  - `idx_env_variables_collection_id` on `collection_id`
- **@Version:** No

---

### 6.12 GlobalVariable

- **Table:** `global_variables`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** `UC(team_id, variable_key)`
- **Fields:**
  - `teamId` -- `UUID`, `@Column(name = "team_id", nullable = false)`
  - `variableKey` -- `String`, `@Column(name = "variable_key", nullable = false, length = 500)`
  - `variableValue` -- `String`, `@Column(name = "variable_value", length = 5000)`
  - `isSecret` -- `boolean`, `@Column(name = "is_secret", nullable = false)`, default `false`
  - `isEnabled` -- `boolean`, `@Column(name = "is_enabled", nullable = false)`, default `true`
- **Relationships:** None
- **Indexes:**
  - `idx_global_variables_team_id` on `team_id`
- **@Version:** No

---

### 6.13 CollectionShare

- **Table:** `collection_shares`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** `UC(collection_id, shared_with_user_id)`
- **Fields:**
  - `permission` -- `SharePermission`, `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`
  - `sharedWithUserId` -- `UUID`, `@Column(name = "shared_with_user_id", nullable = false)`
  - `sharedByUserId` -- `UUID`, `@Column(name = "shared_by_user_id", nullable = false)`
- **Relationships:**
  - `collection` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "collection_id", nullable = false)` -> `Collection`
- **Indexes:**
  - `idx_collection_shares_collection_id` on `collection_id`
  - `idx_collection_shares_shared_with` on `shared_with_user_id`
- **@Version:** No

---

### 6.14 Fork

- **Table:** `forks`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None (uniqueness on forked_collection_id enforced by `@JoinColumn(unique = true)`)
- **Fields:**
  - `forkedByUserId` -- `UUID`, `@Column(name = "forked_by_user_id", nullable = false)`
  - `forkedAt` -- `Instant`, `@Column(name = "forked_at", nullable = false)`
  - `label` -- `String`, `@Column(length = 200)`
- **Relationships:**
  - `sourceCollection` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "source_collection_id", nullable = false)` -> `Collection`
  - `forkedCollection` -- `@OneToOne(fetch = LAZY)`, `@JoinColumn(name = "forked_collection_id", nullable = false, unique = true)` -> `Collection`
- **Indexes:**
  - `idx_forks_source_collection_id` on `source_collection_id`
  - `idx_forks_forked_by_user_id` on `forked_by_user_id`
- **@Version:** No

---

### 6.15 MergeRequest

- **Table:** `merge_requests`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None
- **Fields:**
  - `title` -- `String`, `@Column(nullable = false, length = 200)`
  - `description` -- `String`, `@Column(length = 5000)`
  - `status` -- `String`, `@Column(nullable = false, length = 20)` (values: OPEN, MERGED, CLOSED, CONFLICT)
  - `requestedByUserId` -- `UUID`, `@Column(name = "requested_by_user_id", nullable = false)`
  - `reviewedByUserId` -- `UUID`, `@Column(name = "reviewed_by_user_id")`
  - `mergedAt` -- `Instant`, `@Column(name = "merged_at")`
  - `conflictDetails` -- `String`, `@Column(name = "conflict_details", columnDefinition = "TEXT")`
- **Relationships:**
  - `sourceFork` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "source_fork_id", nullable = false)` -> `Fork`
  - `targetCollection` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "target_collection_id", nullable = false)` -> `Collection`
- **Indexes:**
  - `idx_merge_requests_source_fork_id` on `source_fork_id`
  - `idx_merge_requests_target_collection_id` on `target_collection_id`
  - `idx_merge_requests_status` on `status`
- **@Version:** No

---

### 6.16 RunResult

- **Table:** `run_results`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None
- **Fields:**
  - `teamId` -- `UUID`, `@Column(name = "team_id", nullable = false)`
  - `collectionId` -- `UUID`, `@Column(name = "collection_id", nullable = false)`
  - `environmentId` -- `UUID`, `@Column(name = "environment_id")`
  - `status` -- `RunStatus`, `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`
  - `totalRequests` -- `int`, `@Column(name = "total_requests", nullable = false)`, default `0`
  - `passedRequests` -- `int`, `@Column(name = "passed_requests", nullable = false)`, default `0`
  - `failedRequests` -- `int`, `@Column(name = "failed_requests", nullable = false)`, default `0`
  - `totalAssertions` -- `int`, `@Column(name = "total_assertions", nullable = false)`, default `0`
  - `passedAssertions` -- `int`, `@Column(name = "passed_assertions", nullable = false)`, default `0`
  - `failedAssertions` -- `int`, `@Column(name = "failed_assertions", nullable = false)`, default `0`
  - `totalDurationMs` -- `long`, `@Column(name = "total_duration_ms", nullable = false)`, default `0`
  - `iterationCount` -- `int`, `@Column(name = "iteration_count", nullable = false)`, default `1`
  - `delayBetweenRequestsMs` -- `int`, `@Column(name = "delay_between_requests_ms", nullable = false)`, default `0`
  - `dataFilename` -- `String`, `@Column(name = "data_filename", length = 500)`
  - `startedAt` -- `Instant`, `@Column(name = "started_at", nullable = false)`
  - `completedAt` -- `Instant`, `@Column(name = "completed_at")`
  - `startedByUserId` -- `UUID`, `@Column(name = "started_by_user_id", nullable = false)`
- **Relationships:**
  - `iterations` -- `@OneToMany(mappedBy = "runResult", cascade = ALL, orphanRemoval = true)` -> `List<RunIteration>`
- **Indexes:**
  - `idx_run_results_team_id` on `team_id`
  - `idx_run_results_collection_id` on `collection_id`
  - `idx_run_results_status` on `status`
- **@Version:** No

---

### 6.17 RunIteration

- **Table:** `run_iterations`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** None
- **Fields:**
  - `iterationNumber` -- `int`, `@Column(name = "iteration_number", nullable = false)`
  - `requestName` -- `String`, `@Column(name = "request_name", nullable = false, length = 200)`
  - `requestMethod` -- `HttpMethod`, `@Enumerated(EnumType.STRING)`, `@Column(name = "request_method", nullable = false)`
  - `requestUrl` -- `String`, `@Column(name = "request_url", nullable = false, length = 2000)`
  - `responseStatus` -- `Integer`, `@Column(name = "response_status")`
  - `responseTimeMs` -- `Long`, `@Column(name = "response_time_ms")`
  - `responseSizeBytes` -- `Long`, `@Column(name = "response_size_bytes")`
  - `passed` -- `boolean`, `@Column(nullable = false)`, default `true`
  - `assertionResults` -- `String`, `@Column(name = "assertion_results", columnDefinition = "TEXT")`
  - `errorMessage` -- `String`, `@Column(name = "error_message", length = 5000)`
  - `requestData` -- `String`, `@Column(name = "request_data", columnDefinition = "TEXT")`
  - `responseData` -- `String`, `@Column(name = "response_data", columnDefinition = "TEXT")`
- **Relationships:**
  - `runResult` -- `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "run_result_id", nullable = false)` -> `RunResult`
- **Indexes:**
  - `idx_run_iterations_run_result_id` on `run_result_id`
- **@Version:** No

---

### 6.18 CodeSnippetTemplate

- **Table:** `code_snippet_templates`
- **PK:** `id UUID` (inherited)
- **Unique Constraints:** `language` column has `unique = true`
- **Fields:**
  - `language` -- `CodeLanguage`, `@Enumerated(EnumType.STRING)`, `@Column(nullable = false, unique = true)`
  - `displayName` -- `String`, `@Column(name = "display_name", nullable = false, length = 100)`
  - `templateContent` -- `String`, `@Column(name = "template_content", columnDefinition = "TEXT", nullable = false)`
  - `fileExtension` -- `String`, `@Column(name = "file_extension", nullable = false, length = 20)`
  - `contentType` -- `String`, `@Column(name = "content_type", length = 100)`
- **Relationships:** None
- **Indexes:** None explicit (unique constraint on language creates implicit index)
- **@Version:** No

---


### Relay Module


All Relay entities extend `BaseEntity` which provides:
- `id: UUID` [@Id, @GeneratedValue(strategy = UUID)]
- `createdAt: Instant` [@Column(name = "created_at", nullable = false, updatable = false)]
- `updatedAt: Instant` [@Column(name = "updated_at")]

File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/entity/BaseEntity.java`

---

### === Channel ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/Channel.java`
Table: `channels`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `name: String` [@Column(nullable = false, length = 100)] — display name
- `slug: String` [@Column(nullable = false, length = 100)] — URL-safe lowercase slug
- `description: String` [@Column(columnDefinition = "TEXT")] — nullable
- `topic: String` [@Column(length = 500)] — nullable, channel header topic
- `channelType: ChannelType` [@Enumerated(STRING), @Column(name = "channel_type", nullable = false)]
- `teamId: UUID` [@Column(name = "team_id", nullable = false)]
- `projectId: UUID` [@Column(name = "project_id")] — nullable, for PROJECT channels
- `serviceId: UUID` [@Column(name = "service_id")] — nullable, for SERVICE channels
- `isArchived: boolean` [@Column(name = "is_archived", nullable = false)] — default false
- `createdBy: UUID` [@Column(name = "created_by", nullable = false)]
Relationships:
- @OneToMany(mappedBy = "channel", cascade = ALL, orphanRemoval = true) -> List\<ChannelMember\> members
- @OneToMany(mappedBy = "channel", cascade = ALL, orphanRemoval = true) -> List\<PinnedMessage\> pinnedMessages
Unique Constraints: `uk_channel_team_slug` on (team_id, slug)
Indexes:
- `idx_channel_team_id` on team_id
- `idx_channel_type` on channel_type
- `idx_channel_project_id` on project_id
- `idx_channel_service_id` on service_id
Version: No

---

### === ChannelMember ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/ChannelMember.java`
Table: `channel_members`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `channelId: UUID` [@Column(name = "channel_id", nullable = false)]
- `userId: UUID` [@Column(name = "user_id", nullable = false)]
- `role: MemberRole` [@Enumerated(STRING), @Column(nullable = false)] — default MEMBER
- `lastReadAt: Instant` [@Column(name = "last_read_at")] — nullable
- `isMuted: boolean` [@Column(name = "is_muted", nullable = false)] — default false
- `joinedAt: Instant` [@Column(name = "joined_at", nullable = false)]
Relationships:
- @ManyToOne(fetch = LAZY) -> Channel channel [@JoinColumn(name = "channel_id", insertable = false, updatable = false)]
Unique Constraints: `uk_channel_member` on (channel_id, user_id)
Indexes:
- `idx_channel_member_user_id` on user_id
- `idx_channel_member_channel_id` on channel_id
Version: No

---

### === Message ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/Message.java`
Table: `messages`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `channelId: UUID` [@Column(name = "channel_id", nullable = false)]
- `senderId: UUID` [@Column(name = "sender_id", nullable = false)]
- `content: String` [@Column(columnDefinition = "TEXT", nullable = false)]
- `messageType: MessageType` [@Enumerated(STRING), @Column(name = "message_type", nullable = false)] — default TEXT
- `parentId: UUID` [@Column(name = "parent_id")] — nullable, for threaded replies
- `isEdited: boolean` [@Column(name = "is_edited", nullable = false)] — default false
- `editedAt: Instant` [@Column(name = "edited_at")] — nullable
- `isDeleted: boolean` [@Column(name = "is_deleted", nullable = false)] — default false
- `mentionsEveryone: boolean` [@Column(name = "mentions_everyone", nullable = false)] — default false
- `mentionedUserIds: String` [@Column(name = "mentioned_user_ids", columnDefinition = "TEXT")] — nullable, comma-separated UUIDs
- `platformEventId: UUID` [@Column(name = "platform_event_id")] — nullable, ref to PlatformEvent
Relationships:
- @OneToMany(mappedBy = "message", cascade = ALL, orphanRemoval = true) -> List\<Reaction\> reactions
- @OneToMany(mappedBy = "message", cascade = ALL, orphanRemoval = true) -> List\<FileAttachment\> attachments
Indexes:
- `idx_message_channel_id` on channel_id
- `idx_message_sender_id` on sender_id
- `idx_message_parent_id` on parent_id
- `idx_message_created_at` on created_at
- `idx_message_channel_created` on (channel_id, created_at DESC)
Version: No

---

### === MessageThread ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/MessageThread.java`
Table: `message_threads`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `rootMessageId: UUID` [@Column(name = "root_message_id", nullable = false)]
- `channelId: UUID` [@Column(name = "channel_id", nullable = false)]
- `replyCount: int` [@Column(name = "reply_count", nullable = false)] — default 0
- `lastReplyAt: Instant` [@Column(name = "last_reply_at")] — nullable
- `lastReplyBy: UUID` [@Column(name = "last_reply_by")] — nullable
- `participantIds: String` [@Column(name = "participant_ids", columnDefinition = "TEXT")] — nullable, comma-separated UUIDs
Relationships: None
Unique Constraints: `uk_thread_root_message` on (root_message_id)
Indexes:
- `idx_thread_channel_id` on channel_id
- `idx_thread_last_reply_at` on last_reply_at
Version: No

---

### === DirectConversation ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/DirectConversation.java`
Table: `direct_conversations`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `teamId: UUID` [@Column(name = "team_id", nullable = false)]
- `conversationType: ConversationType` [@Enumerated(STRING), @Column(name = "conversation_type", nullable = false)]
- `name: String` [@Column(length = 200)] — nullable, for group conversations
- `participantIds: String` [@Column(name = "participant_ids", columnDefinition = "TEXT", nullable = false)] — sorted comma-separated UUIDs
- `lastMessageAt: Instant` [@Column(name = "last_message_at")] — nullable
- `lastMessagePreview: String` [@Column(name = "last_message_preview", length = 500)] — nullable
Relationships: None
Unique Constraints: `uk_dm_participants` on (team_id, participant_ids)
Indexes:
- `idx_dm_team_id` on team_id
- `idx_dm_last_message_at` on last_message_at
Version: No

---

### === DirectMessage ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/DirectMessage.java`
Table: `direct_messages`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `conversationId: UUID` [@Column(name = "conversation_id", nullable = false)]
- `senderId: UUID` [@Column(name = "sender_id", nullable = false)]
- `content: String` [@Column(columnDefinition = "TEXT", nullable = false)]
- `messageType: MessageType` [@Enumerated(STRING), @Column(name = "message_type", nullable = false)] — default TEXT
- `isEdited: boolean` [@Column(name = "is_edited", nullable = false)] — default false
- `editedAt: Instant` [@Column(name = "edited_at")] — nullable
- `isDeleted: boolean` [@Column(name = "is_deleted", nullable = false)] — default false
Relationships:
- @ManyToOne(fetch = LAZY) -> DirectConversation conversation [@JoinColumn(name = "conversation_id", insertable = false, updatable = false)]
- @OneToMany(mappedBy = "directMessage", cascade = ALL, orphanRemoval = true) -> List\<Reaction\> reactions
- @OneToMany(mappedBy = "directMessage", cascade = ALL, orphanRemoval = true) -> List\<FileAttachment\> attachments
Indexes:
- `idx_dm_message_conversation_id` on conversation_id
- `idx_dm_message_created_at` on created_at
- `idx_dm_message_conv_created` on (conversation_id, created_at DESC)
Version: No

---

### === Reaction ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/Reaction.java`
Table: `reactions`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `userId: UUID` [@Column(name = "user_id", nullable = false)]
- `emoji: String` [@Column(nullable = false, length = 50)]
- `reactionType: ReactionType` [@Enumerated(STRING), @Column(name = "reaction_type", nullable = false)] — default EMOJI
- `messageId: UUID` [@Column(name = "message_id")] — nullable, for channel messages
- `directMessageId: UUID` [@Column(name = "direct_message_id")] — nullable, for DMs
Relationships:
- @ManyToOne(fetch = LAZY) -> Message message [@JoinColumn(name = "message_id", insertable = false, updatable = false)]
- @ManyToOne(fetch = LAZY) -> DirectMessage directMessage [@JoinColumn(name = "direct_message_id", insertable = false, updatable = false)]
Unique Constraints:
- `uk_reaction_message` on (message_id, user_id, emoji)
- `uk_reaction_dm` on (direct_message_id, user_id, emoji)
Indexes:
- `idx_reaction_message_id` on message_id
- `idx_reaction_dm_id` on direct_message_id
- `idx_reaction_user_id` on user_id
Version: No

---

### === FileAttachment ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/FileAttachment.java`
Table: `file_attachments`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `fileName: String` [@Column(name = "file_name", nullable = false, length = 500)]
- `contentType: String` [@Column(name = "content_type", nullable = false, length = 200)]
- `fileSizeBytes: Long` [@Column(name = "file_size_bytes", nullable = false)]
- `storagePath: String` [@Column(name = "storage_path", nullable = false, length = 1000)]
- `thumbnailPath: String` [@Column(name = "thumbnail_path", length = 1000)] — nullable
- `status: FileUploadStatus` [@Enumerated(STRING), @Column(nullable = false)] — default UPLOADING
- `uploadedBy: UUID` [@Column(name = "uploaded_by", nullable = false)]
- `teamId: UUID` [@Column(name = "team_id", nullable = false)]
- `messageId: UUID` [@Column(name = "message_id")] — nullable, for channel messages
- `directMessageId: UUID` [@Column(name = "direct_message_id")] — nullable, for DMs
Relationships:
- @ManyToOne(fetch = LAZY) -> Message message [@JoinColumn(name = "message_id", insertable = false, updatable = false)]
- @ManyToOne(fetch = LAZY) -> DirectMessage directMessage [@JoinColumn(name = "direct_message_id", insertable = false, updatable = false)]
Indexes:
- `idx_file_attachment_message_id` on message_id
- `idx_file_attachment_dm_id` on direct_message_id
- `idx_file_attachment_team_id` on team_id
Version: No

---

### === PinnedMessage ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/PinnedMessage.java`
Table: `pinned_messages`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `messageId: UUID` [@Column(name = "message_id", nullable = false)]
- `pinnedBy: UUID` [@Column(name = "pinned_by", nullable = false)]
Relationships:
- @ManyToOne(fetch = LAZY) -> Channel channel [@JoinColumn(name = "channel_id", nullable = false)]
Unique Constraints: `uk_pinned_message` on (channel_id, message_id)
Indexes:
- `idx_pinned_message_channel_id` on channel_id
Version: No

---

### === ReadReceipt ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/ReadReceipt.java`
Table: `read_receipts`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `channelId: UUID` [@Column(name = "channel_id", nullable = false)] — also used for DM conversation IDs
- `userId: UUID` [@Column(name = "user_id", nullable = false)]
- `lastReadMessageId: UUID` [@Column(name = "last_read_message_id", nullable = false)]
- `lastReadAt: Instant` [@Column(name = "last_read_at", nullable = false)]
Relationships: None
Unique Constraints: `uk_read_receipt` on (channel_id, user_id)
Indexes:
- `idx_read_receipt_channel_id` on channel_id
- `idx_read_receipt_user_id` on user_id
Version: No

---

### === UserPresence ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/UserPresence.java`
Table: `user_presences`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `userId: UUID` [@Column(name = "user_id", nullable = false)]
- `teamId: UUID` [@Column(name = "team_id", nullable = false)]
- `status: PresenceStatus` [@Enumerated(STRING), @Column(nullable = false)] — default OFFLINE
- `lastSeenAt: Instant` [@Column(name = "last_seen_at")] — nullable
- `lastHeartbeatAt: Instant` [@Column(name = "last_heartbeat_at")] — nullable
- `statusMessage: String` [@Column(name = "status_message", length = 200)] — nullable
Relationships: None
Unique Constraints: `uk_presence_user_team` on (user_id, team_id)
Indexes:
- `idx_presence_team_id` on team_id
- `idx_presence_status` on status
Version: No

---

### === PlatformEvent ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/PlatformEvent.java`
Table: `platform_events`
PK: `id`, UUID, GenerationType.UUID (inherited from BaseEntity)
Fields:
- `eventType: PlatformEventType` [@Enumerated(STRING), @Column(name = "event_type", nullable = false)]
- `teamId: UUID` [@Column(name = "team_id", nullable = false)]
- `sourceModule: String` [@Column(name = "source_module", nullable = false, length = 50)]
- `sourceEntityId: UUID` [@Column(name = "source_entity_id")] — nullable
- `title: String` [@Column(nullable = false, length = 500)]
- `detail: String` [@Column(columnDefinition = "TEXT")] — nullable
- `targetChannelId: UUID` [@Column(name = "target_channel_id")] — nullable
- `targetChannelSlug: String` [@Column(name = "target_channel_slug", length = 100)] — nullable
- `postedMessageId: UUID` [@Column(name = "posted_message_id")] — nullable
- `isDelivered: boolean` [@Column(name = "is_delivered", nullable = false)] — default false
- `deliveredAt: Instant` [@Column(name = "delivered_at")] — nullable
Relationships: None
Indexes:
- `idx_platform_event_team_id` on team_id
- `idx_platform_event_type` on event_type
- `idx_platform_event_created_at` on created_at
- `idx_platform_event_source_module` on source_module
Version: No

---


---

## 7. Enum Definitions

### Core Module


All enums are in package `com.codeops.entity.enums`.

| # | Enum | Values | File |
|---|------|--------|------|
| 7.1 | AgentType | SECURITY, CODE_QUALITY, BUILD_HEALTH, COMPLETENESS, API_CONTRACT, TEST_COVERAGE, UI_UX, DOCUMENTATION, DATABASE, PERFORMANCE, DEPENDENCY, ARCHITECTURE | AgentType.java |
| 7.2 | AgentStatus | PENDING, RUNNING, COMPLETED, FAILED | AgentStatus.java |
| 7.3 | AgentResult | PASS, WARN, FAIL | AgentResult.java |
| 7.4 | BusinessImpact | LOW, MEDIUM, HIGH, CRITICAL | BusinessImpact.java |
| 7.5 | ComplianceStatus | MET, PARTIAL, MISSING, NOT_APPLICABLE | ComplianceStatus.java |
| 7.6 | DebtCategory | ARCHITECTURE, CODE, TEST, DEPENDENCY, DOCUMENTATION | DebtCategory.java |
| 7.7 | DebtStatus | IDENTIFIED, PLANNED, IN_PROGRESS, RESOLVED | DebtStatus.java |
| 7.8 | DirectiveCategory | ARCHITECTURE, STANDARDS, CONVENTIONS, CONTEXT, OTHER | DirectiveCategory.java |
| 7.9 | DirectiveScope | TEAM, PROJECT, USER | DirectiveScope.java |
| 7.10 | Effort | S, M, L, XL | Effort.java |
| 7.11 | FindingStatus | OPEN, ACKNOWLEDGED, FALSE_POSITIVE, FIXED, WONT_FIX | FindingStatus.java |
| 7.12 | GitHubAuthType | PAT, OAUTH, SSH | GitHubAuthType.java |
| 7.13 | InvitationStatus | PENDING, ACCEPTED, EXPIRED | InvitationStatus.java |
| 7.14 | JobMode | AUDIT, COMPLIANCE, BUG_INVESTIGATE, REMEDIATE, TECH_DEBT, DEPENDENCY, HEALTH_MONITOR | JobMode.java |
| 7.15 | JobResult | PASS, WARN, FAIL | JobResult.java |
| 7.16 | JobStatus | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED | JobStatus.java |
| 7.17 | MfaMethod | NONE, TOTP, EMAIL | MfaMethod.java |
| 7.18 | Priority | P0, P1, P2, P3 | Priority.java |
| 7.19 | ScheduleType | DAILY, WEEKLY, ON_COMMIT | ScheduleType.java |
| 7.20 | Scope | SYSTEM, TEAM, USER | Scope.java |
| 7.21 | Severity | CRITICAL, HIGH, MEDIUM, LOW | Severity.java |
| 7.22 | SpecType | OPENAPI, MARKDOWN, SCREENSHOT, FIGMA | SpecType.java |
| 7.23 | TaskStatus | PENDING, ASSIGNED, EXPORTED, JIRA_CREATED, COMPLETED | TaskStatus.java |
| 7.24 | TeamRole | OWNER, ADMIN, MEMBER, VIEWER | TeamRole.java |
| 7.25 | VulnerabilityStatus | OPEN, UPDATING, SUPPRESSED, RESOLVED | VulnerabilityStatus.java |

---


### Registry Module


```
=== ServiceType ===
Values: SPRING_BOOT_API, FLUTTER_WEB, FLUTTER_DESKTOP, FLUTTER_MOBILE, REACT_SPA, VUE_SPA, NEXT_JS, EXPRESS_API, FASTAPI, DOTNET_API, GO_API, LIBRARY, WORKER, GATEWAY, DATABASE_SERVICE, MESSAGE_BROKER, CACHE_SERVICE, MCP_SERVER, CLI_TOOL, OTHER

=== ServiceStatus ===
Values: ACTIVE, INACTIVE, DEPRECATED, ARCHIVED

=== HealthStatus ===
Values: UP, DOWN, DEGRADED, UNKNOWN

=== PortType ===
Values: HTTP_API, FRONTEND_DEV, DATABASE, REDIS, KAFKA, KAFKA_INTERNAL, ZOOKEEPER, GRPC, WEBSOCKET, DEBUG, ACTUATOR, CUSTOM

=== DependencyType ===
Values: HTTP_REST, GRPC, KAFKA_TOPIC, DATABASE_SHARED, REDIS_SHARED, LIBRARY, GATEWAY_ROUTE, WEBSOCKET, FILE_SYSTEM, OTHER

=== SolutionStatus ===
Values: ACTIVE, IN_DEVELOPMENT, DEPRECATED, ARCHIVED

=== SolutionCategory ===
Values: PLATFORM, APPLICATION, LIBRARY_SUITE, INFRASTRUCTURE, TOOLING, OTHER

=== SolutionMemberRole ===
Values: CORE, SUPPORTING, INFRASTRUCTURE, EXTERNAL_DEPENDENCY

=== ConfigTemplateType ===
Values: DOCKER_COMPOSE, APPLICATION_YML, APPLICATION_PROPERTIES, ENV_FILE, TERRAFORM_MODULE, CLAUDE_CODE_HEADER, CONVENTIONS_MD, NGINX_CONF, GITHUB_ACTIONS, DOCKERFILE, MAKEFILE, README_SECTION

=== ConfigSource ===
Values: AUTO_GENERATED, MANUAL, INHERITED, REGISTRY_DERIVED

=== InfraResourceType ===
Values: S3_BUCKET, SQS_QUEUE, SNS_TOPIC, CLOUDWATCH_LOG_GROUP, IAM_ROLE, SECRETS_MANAGER_PATH, SSM_PARAMETER, RDS_INSTANCE, ELASTICACHE_CLUSTER, ECR_REPOSITORY, CLOUD_MAP_NAMESPACE, ROUTE53_RECORD, ACM_CERTIFICATE, ALB_TARGET_GROUP, ECS_SERVICE, LAMBDA_FUNCTION, DYNAMODB_TABLE, DOCKER_NETWORK, DOCKER_VOLUME, OTHER
```


### Logger Module


```
=== LogLevel ===
Values: TRACE, DEBUG, INFO, WARN, ERROR, FATAL

=== AlertSeverity ===
Values: INFO, WARNING, CRITICAL

=== AlertStatus ===
Values: FIRED, ACKNOWLEDGED, RESOLVED

=== AlertChannelType ===
Values: EMAIL, WEBHOOK, TEAMS, SLACK

=== SpanStatus ===
Values: OK, ERROR

=== MetricType ===
Values: COUNTER, GAUGE, HISTOGRAM, TIMER

=== WidgetType ===
Values: LOG_STREAM, TIME_SERIES_CHART, COUNTER, GAUGE, TABLE, HEATMAP, PIE_CHART, BAR_CHART

=== RetentionAction ===
Values: PURGE, ARCHIVE

=== ConditionType ===
Values: REGEX, KEYWORD, FREQUENCY_THRESHOLD, ABSENCE

=== TrapType ===
Values: PATTERN, FREQUENCY, ABSENCE
```

---


### Courier Module


**Package:** `com.codeops.courier.entity.enums`

| Enum | Values |
|------|--------|
| `AuthType` | `NO_AUTH`, `API_KEY`, `BEARER_TOKEN`, `BASIC_AUTH`, `OAUTH2_AUTHORIZATION_CODE`, `OAUTH2_CLIENT_CREDENTIALS`, `OAUTH2_IMPLICIT`, `OAUTH2_PASSWORD`, `JWT_BEARER`, `INHERIT_FROM_PARENT` |
| `BodyType` | `NONE`, `FORM_DATA`, `X_WWW_FORM_URLENCODED`, `RAW_JSON`, `RAW_XML`, `RAW_HTML`, `RAW_TEXT`, `RAW_YAML`, `BINARY`, `GRAPHQL` |
| `CodeLanguage` | `CURL`, `PYTHON_REQUESTS`, `JAVASCRIPT_FETCH`, `JAVASCRIPT_AXIOS`, `JAVA_HTTP_CLIENT`, `JAVA_OKHTTP`, `CSHARP_HTTP_CLIENT`, `GO`, `RUBY`, `PHP`, `SWIFT`, `KOTLIN` |
| `HttpMethod` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS` |
| `RunStatus` | `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED` |
| `ScriptType` | `PRE_REQUEST`, `POST_RESPONSE` |
| `SharePermission` | `VIEWER`, `EDITOR`, `ADMIN` |

---


### Relay Module


### === ChannelType ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/enums/ChannelType.java`
Values: PUBLIC, PRIVATE, PROJECT, SERVICE

### === MessageType ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/enums/MessageType.java`
Values: TEXT, SYSTEM, PLATFORM_EVENT, FILE

### === ReactionType ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/enums/ReactionType.java`
Values: EMOJI

### === PresenceStatus ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/enums/PresenceStatus.java`
Values: ONLINE, AWAY, DND, OFFLINE

### === PlatformEventType ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/enums/PlatformEventType.java`
Values: AUDIT_COMPLETED, ALERT_FIRED, SESSION_COMPLETED, SECRET_ROTATED, CONTAINER_CRASHED, SERVICE_REGISTERED, DEPLOYMENT_COMPLETED, BUILD_COMPLETED, FINDING_CRITICAL, MERGE_REQUEST_CREATED

### === FileUploadStatus ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/enums/FileUploadStatus.java`
Values: UPLOADING, COMPLETE, FAILED

### === ConversationType ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/enums/ConversationType.java`
Values: ONE_ON_ONE, GROUP

### === MemberRole ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/entity/enums/MemberRole.java`
Values: OWNER, ADMIN, MEMBER

---


---

## 8. Repository Layer

### Core Module


All repositories are in package `com.codeops.repository`.

### 8.1 UserRepository
- **File:** `UserRepository.java`
- **Extends:** `JpaRepository<User, UUID>`
- **Derived query methods:**
  - `Optional<User> findByEmail(String email)`
  - `boolean existsByEmail(String email)`
  - `List<User> findByDisplayNameContainingIgnoreCase(String search)`
  - `long countByIsActiveTrue()`

### 8.2 TeamRepository
- **File:** `TeamRepository.java`
- **Extends:** `JpaRepository<Team, UUID>`
- **Derived query methods:**
  - `List<Team> findByOwnerId(UUID ownerId)`

### 8.3 TeamMemberRepository
- **File:** `TeamMemberRepository.java`
- **Extends:** `JpaRepository<TeamMember, UUID>`
- **Derived query methods:**
  - `List<TeamMember> findByTeamId(UUID teamId)`
  - `List<TeamMember> findByUserId(UUID userId)`
  - `Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId)`
  - `boolean existsByTeamIdAndUserId(UUID teamId, UUID userId)`
  - `long countByTeamId(UUID teamId)`
  - `void deleteByTeamIdAndUserId(UUID teamId, UUID userId)`

### 8.4 InvitationRepository
- **File:** `InvitationRepository.java`
- **Extends:** `JpaRepository<Invitation, UUID>`
- **Derived query methods:**
  - `Optional<Invitation> findByToken(String token)`
  - `List<Invitation> findByTeamIdAndStatus(UUID teamId, InvitationStatus status)`
  - `List<Invitation> findByEmailAndStatus(String email, InvitationStatus status)`
- **Custom @Query methods:**
  - `@Lock(PESSIMISTIC_WRITE) @Query("SELECT i FROM Invitation i WHERE i.team.id = :teamId AND i.email = :email AND i.status = :status") List<Invitation> findByTeamIdAndEmailAndStatusForUpdate(UUID teamId, String email, InvitationStatus status)`

### 8.5 ProjectRepository
- **File:** `ProjectRepository.java`
- **Extends:** `JpaRepository<Project, UUID>`
- **Derived query methods:**
  - `List<Project> findByTeamIdAndIsArchivedFalse(UUID teamId)`
  - `List<Project> findByTeamId(UUID teamId)`
  - `Optional<Project> findByTeamIdAndRepoFullName(UUID teamId, String repoFullName)`
  - `long countByTeamId(UUID teamId)`
- **Paginated methods:**
  - `Page<Project> findByTeamId(UUID teamId, Pageable pageable)`
  - `Page<Project> findByTeamIdAndIsArchivedFalse(UUID teamId, Pageable pageable)`

### 8.6 PersonaRepository
- **File:** `PersonaRepository.java`
- **Extends:** `JpaRepository<Persona, UUID>`
- **Derived query methods:**
  - `List<Persona> findByTeamId(UUID teamId)`
  - `List<Persona> findByScope(Scope scope)`
  - `List<Persona> findByTeamIdAndAgentType(UUID teamId, AgentType agentType)`
  - `Optional<Persona> findByTeamIdAndAgentTypeAndIsDefaultTrue(UUID teamId, AgentType agentType)`
  - `List<Persona> findByCreatedById(UUID userId)`
- **Paginated methods:**
  - `Page<Persona> findByTeamId(UUID teamId, Pageable pageable)`

### 8.7 DirectiveRepository
- **File:** `DirectiveRepository.java`
- **Extends:** `JpaRepository<Directive, UUID>`
- **Derived query methods:**
  - `List<Directive> findByTeamId(UUID teamId)`
  - `List<Directive> findByProjectId(UUID projectId)`
  - `List<Directive> findByTeamIdAndScope(UUID teamId, DirectiveScope scope)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM Directive d WHERE d.project.id = :projectId") void deleteAllByProjectId(UUID projectId)`

### 8.8 ProjectDirectiveRepository
- **File:** `ProjectDirectiveRepository.java`
- **Extends:** `JpaRepository<ProjectDirective, ProjectDirectiveId>`
- **Derived query methods:**
  - `List<ProjectDirective> findByProjectId(UUID projectId)`
  - `List<ProjectDirective> findByProjectIdAndEnabledTrue(UUID projectId)`
  - `List<ProjectDirective> findByDirectiveId(UUID directiveId)`
  - `void deleteByProjectIdAndDirectiveId(UUID projectId, UUID directiveId)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM ProjectDirective pd WHERE pd.project.id = :projectId") void deleteAllByProjectId(UUID projectId)`

### 8.9 GitHubConnectionRepository
- **File:** `GitHubConnectionRepository.java`
- **Extends:** `JpaRepository<GitHubConnection, UUID>`
- **Derived query methods:**
  - `List<GitHubConnection> findByTeamIdAndIsActiveTrue(UUID teamId)`

### 8.10 JiraConnectionRepository
- **File:** `JiraConnectionRepository.java`
- **Extends:** `JpaRepository<JiraConnection, UUID>`
- **Derived query methods:**
  - `List<JiraConnection> findByTeamIdAndIsActiveTrue(UUID teamId)`

### 8.11 QaJobRepository
- **File:** `QaJobRepository.java`
- **Extends:** `JpaRepository<QaJob, UUID>`
- **Derived query methods:**
  - `List<QaJob> findByProjectIdOrderByCreatedAtDesc(UUID projectId)`
  - `List<QaJob> findByProjectIdAndMode(UUID projectId, JobMode mode)`
  - `List<QaJob> findByStartedById(UUID userId)`
  - `long countByProjectIdAndStatus(UUID projectId, JobStatus status)`
- **Paginated methods:**
  - `Page<QaJob> findByStartedById(UUID userId, Pageable pageable)`
  - `Page<QaJob> findByProjectId(UUID projectId, Pageable pageable)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM QaJob j WHERE j.project.id = :projectId") void deleteAllByProjectId(UUID projectId)`

### 8.12 FindingRepository
- **File:** `FindingRepository.java`
- **Extends:** `JpaRepository<Finding, UUID>`
- **Derived query methods:**
  - `List<Finding> findByJobId(UUID jobId)`
  - `List<Finding> findByJobIdAndAgentType(UUID jobId, AgentType agentType)`
  - `List<Finding> findByJobIdAndSeverity(UUID jobId, Severity severity)`
  - `List<Finding> findByJobIdAndStatus(UUID jobId, FindingStatus status)`
  - `long countByJobIdAndSeverity(UUID jobId, Severity severity)`
  - `long countByJobIdAndSeverityAndStatus(UUID jobId, Severity severity, FindingStatus status)`
- **Paginated methods:**
  - `Page<Finding> findByJobId(UUID jobId, Pageable pageable)`
  - `Page<Finding> findByJobIdAndSeverity(UUID jobId, Severity severity, Pageable pageable)`
  - `Page<Finding> findByJobIdAndAgentType(UUID jobId, AgentType agentType, Pageable pageable)`
  - `Page<Finding> findByJobIdAndStatus(UUID jobId, FindingStatus status, Pageable pageable)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM Finding f WHERE f.job.id IN (SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)") void deleteAllByProjectId(UUID projectId)`

### 8.13 AgentRunRepository
- **File:** `AgentRunRepository.java`
- **Extends:** `JpaRepository<AgentRun, UUID>`
- **Derived query methods:**
  - `List<AgentRun> findByJobId(UUID jobId)`
  - `List<AgentRun> findByJobIdAndStatus(UUID jobId, AgentStatus status)`
  - `Optional<AgentRun> findByJobIdAndAgentType(UUID jobId, AgentType agentType)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM AgentRun a WHERE a.job.id IN (SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)") void deleteAllByProjectId(UUID projectId)`

### 8.14 BugInvestigationRepository
- **File:** `BugInvestigationRepository.java`
- **Extends:** `JpaRepository<BugInvestigation, UUID>`
- **Derived query methods:**
  - `Optional<BugInvestigation> findByJobId(UUID jobId)`
  - `Optional<BugInvestigation> findByJiraKey(String jiraKey)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM BugInvestigation b WHERE b.job.id IN (SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)") void deleteAllByProjectId(UUID projectId)`

### 8.15 ComplianceItemRepository
- **File:** `ComplianceItemRepository.java`
- **Extends:** `JpaRepository<ComplianceItem, UUID>`
- **Derived query methods:**
  - `List<ComplianceItem> findByJobId(UUID jobId)`
  - `List<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status)`
- **Paginated methods:**
  - `Page<ComplianceItem> findByJobId(UUID jobId, Pageable pageable)`
  - `Page<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status, Pageable pageable)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM ComplianceItem c WHERE c.job.id IN (SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)") void deleteAllByProjectId(UUID projectId)`

### 8.16 SpecificationRepository
- **File:** `SpecificationRepository.java`
- **Extends:** `JpaRepository<Specification, UUID>`
- **Derived query methods:**
  - `List<Specification> findByJobId(UUID jobId)`
- **Paginated methods:**
  - `Page<Specification> findByJobId(UUID jobId, Pageable pageable)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM Specification s WHERE s.job.id IN (SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)") void deleteAllByProjectId(UUID projectId)`

### 8.17 TechDebtItemRepository
- **File:** `TechDebtItemRepository.java`
- **Extends:** `JpaRepository<TechDebtItem, UUID>`
- **Derived query methods:**
  - `List<TechDebtItem> findByProjectId(UUID projectId)`
  - `List<TechDebtItem> findByProjectIdAndStatus(UUID projectId, DebtStatus status)`
  - `List<TechDebtItem> findByProjectIdAndCategory(UUID projectId, DebtCategory category)`
  - `long countByProjectIdAndStatus(UUID projectId, DebtStatus status)`
- **Paginated methods:**
  - `Page<TechDebtItem> findByProjectId(UUID projectId, Pageable pageable)`
  - `Page<TechDebtItem> findByProjectIdAndStatus(UUID projectId, DebtStatus status, Pageable pageable)`
  - `Page<TechDebtItem> findByProjectIdAndCategory(UUID projectId, DebtCategory category, Pageable pageable)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM TechDebtItem t WHERE t.project.id = :projectId") void deleteAllByProjectId(UUID projectId)`

### 8.18 DependencyScanRepository
- **File:** `DependencyScanRepository.java`
- **Extends:** `JpaRepository<DependencyScan, UUID>`
- **Derived query methods:**
  - `List<DependencyScan> findByProjectIdOrderByCreatedAtDesc(UUID projectId)`
  - `Optional<DependencyScan> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId)`
- **Paginated methods:**
  - `Page<DependencyScan> findByProjectId(UUID projectId, Pageable pageable)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM DependencyScan d WHERE d.project.id = :projectId") void deleteAllByProjectId(UUID projectId)`

### 8.19 DependencyVulnerabilityRepository
- **File:** `DependencyVulnerabilityRepository.java`
- **Extends:** `JpaRepository<DependencyVulnerability, UUID>`
- **Derived query methods:**
  - `List<DependencyVulnerability> findByScanId(UUID scanId)`
  - `List<DependencyVulnerability> findByScanIdAndStatus(UUID scanId, VulnerabilityStatus status)`
  - `List<DependencyVulnerability> findByScanIdAndSeverity(UUID scanId, Severity severity)`
  - `long countByScanIdAndStatus(UUID scanId, VulnerabilityStatus status)`
- **Paginated methods:**
  - `Page<DependencyVulnerability> findByScanId(UUID scanId, Pageable pageable)`
  - `Page<DependencyVulnerability> findByScanIdAndStatus(UUID scanId, VulnerabilityStatus status, Pageable pageable)`
  - `Page<DependencyVulnerability> findByScanIdAndSeverity(UUID scanId, Severity severity, Pageable pageable)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM DependencyVulnerability v WHERE v.scan.id IN (SELECT s.id FROM DependencyScan s WHERE s.project.id = :projectId)") void deleteAllByProjectId(UUID projectId)`

### 8.20 RemediationTaskRepository
- **File:** `RemediationTaskRepository.java`
- **Extends:** `JpaRepository<RemediationTask, UUID>`
- **Derived query methods:**
  - `List<RemediationTask> findByJobIdOrderByTaskNumberAsc(UUID jobId)`
  - `List<RemediationTask> findByAssignedToId(UUID userId)`
- **Paginated methods:**
  - `Page<RemediationTask> findByJobId(UUID jobId, Pageable pageable)`
  - `Page<RemediationTask> findByAssignedToId(UUID userId, Pageable pageable)`
- **Custom @Query methods:**
  - `@Modifying @Query(nativeQuery=true, "DELETE FROM remediation_task_findings WHERE task_id IN (SELECT id FROM remediation_tasks WHERE job_id IN (SELECT id FROM qa_jobs WHERE project_id = :projectId))") void deleteJoinTableByProjectId(UUID projectId)`
  - `@Modifying @Query("DELETE FROM RemediationTask t WHERE t.job.id IN (SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)") void deleteAllByProjectId(UUID projectId)`

### 8.21 HealthSnapshotRepository
- **File:** `HealthSnapshotRepository.java`
- **Extends:** `JpaRepository<HealthSnapshot, UUID>`
- **Derived query methods:**
  - `List<HealthSnapshot> findByProjectIdOrderByCapturedAtDesc(UUID projectId)`
  - `Optional<HealthSnapshot> findFirstByProjectIdOrderByCapturedAtDesc(UUID projectId)`
- **Paginated methods:**
  - `Page<HealthSnapshot> findByProjectId(UUID projectId, Pageable pageable)`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM HealthSnapshot h WHERE h.project.id = :projectId") void deleteAllByProjectId(UUID projectId)`

### 8.22 HealthScheduleRepository
- **File:** `HealthScheduleRepository.java`
- **Extends:** `JpaRepository<HealthSchedule, UUID>`
- **Derived query methods:**
  - `List<HealthSchedule> findByProjectId(UUID projectId)`
  - `List<HealthSchedule> findByIsActiveTrue()`
- **Custom @Query methods:**
  - `@Modifying @Query("DELETE FROM HealthSchedule h WHERE h.project.id = :projectId") void deleteAllByProjectId(UUID projectId)`

### 8.23 NotificationPreferenceRepository
- **File:** `NotificationPreferenceRepository.java`
- **Extends:** `JpaRepository<NotificationPreference, UUID>`
- **Derived query methods:**
  - `List<NotificationPreference> findByUserId(UUID userId)`
  - `Optional<NotificationPreference> findByUserIdAndEventType(UUID userId, String eventType)`

### 8.24 SystemSettingRepository
- **File:** `SystemSettingRepository.java`
- **Extends:** `JpaRepository<SystemSetting, String>`
- **Custom methods:** None (inherits standard CRUD from JpaRepository)

### 8.25 AuditLogRepository
- **File:** `AuditLogRepository.java`
- **Extends:** `JpaRepository<AuditLog, Long>`
- **Derived query methods:**
  - `List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId)`
- **Paginated methods:**
  - `Page<AuditLog> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable)`
  - `Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable)`

### 8.26 MfaEmailCodeRepository
- **File:** `MfaEmailCodeRepository.java`
- **Extends:** `JpaRepository<MfaEmailCode, UUID>`
- **Derived query methods:**
  - `List<MfaEmailCode> findByUserIdAndUsedFalseAndExpiresAtAfter(UUID userId, Instant now)`
  - `void deleteByExpiresAtBefore(Instant now)`
  - `void deleteByUserId(UUID userId)`

---


### Registry Module


```
=== ServiceRegistrationRepository ===
Extends: JpaRepository<ServiceRegistration, UUID>
Custom Methods: (none)
Derived Methods:
  findByTeamIdAndSlug(UUID teamId, String slug) → Optional<ServiceRegistration>
  findByTeamId(UUID teamId) → List<ServiceRegistration>
  findByTeamIdAndStatus(UUID teamId, ServiceStatus status) → List<ServiceRegistration>
  findByTeamIdAndIdIn(UUID teamId, List<UUID> ids) → List<ServiceRegistration>
  countByTeamId(UUID teamId) → long
  countByTeamIdAndStatus(UUID teamId, ServiceStatus status) → long
  existsByTeamIdAndSlug(UUID teamId, String slug) → boolean
Paginated Methods:
  findByTeamId(UUID teamId, Pageable pageable) → Page<ServiceRegistration>
  findByTeamIdAndStatus(UUID teamId, ServiceStatus status, Pageable pageable) → Page<ServiceRegistration>
  findByTeamIdAndServiceType(UUID teamId, ServiceType type, Pageable pageable) → Page<ServiceRegistration>
  findByTeamIdAndStatusAndServiceType(UUID teamId, ServiceStatus status, ServiceType type, Pageable pageable) → Page<ServiceRegistration>
  findByTeamIdAndNameContainingIgnoreCase(UUID teamId, String name, Pageable pageable) → Page<ServiceRegistration>


=== SolutionRepository ===
Extends: JpaRepository<Solution, UUID>
Custom Methods: (none)
Derived Methods:
  findByTeamIdAndSlug(UUID teamId, String slug) → Optional<Solution>
  findByTeamId(UUID teamId) → List<Solution>
  countByTeamId(UUID teamId) → long
  existsByTeamIdAndSlug(UUID teamId, String slug) → boolean
Paginated Methods:
  findByTeamId(UUID teamId, Pageable pageable) → Page<Solution>
  findByTeamIdAndStatus(UUID teamId, SolutionStatus status, Pageable pageable) → Page<Solution>
  findByTeamIdAndCategory(UUID teamId, SolutionCategory category, Pageable pageable) → Page<Solution>


=== SolutionMemberRepository ===
Extends: JpaRepository<SolutionMember, UUID>
Custom Methods:
  deleteBySolutionIdAndServiceId(UUID solutionId, UUID serviceId) → void
Derived Methods:
  findBySolutionId(UUID solutionId) → List<SolutionMember>
  findBySolutionIdOrderByDisplayOrderAsc(UUID solutionId) → List<SolutionMember>
  findByServiceId(UUID serviceId) → List<SolutionMember>
  findBySolutionIdAndServiceId(UUID solutionId, UUID serviceId) → Optional<SolutionMember>
  existsBySolutionIdAndServiceId(UUID solutionId, UUID serviceId) → boolean
  countBySolutionId(UUID solutionId) → long
  countByServiceId(UUID serviceId) → long
Paginated Methods: (none)


=== PortAllocationRepository ===
Extends: JpaRepository<PortAllocation, UUID>
Custom Methods (JPQL @Query):
  findByTeamIdAndEnvironment(@Param("teamId") UUID, @Param("environment") String) → List<PortAllocation>
  findByTeamIdAndEnvironmentAndPortNumber(@Param("teamId") UUID, @Param("environment") String, @Param("portNumber") Integer) → Optional<PortAllocation>
  findByTeamIdAndEnvironmentAndPortType(@Param("teamId") UUID, @Param("environment") String, @Param("portType") PortType) → List<PortAllocation>
  findConflictingPorts(@Param("teamId") UUID) → List<Object[]>
Derived Methods:
  findByServiceId(UUID serviceId) → List<PortAllocation>
  findByServiceIdAndEnvironment(UUID serviceId, String environment) → List<PortAllocation>
  existsByServiceIdAndEnvironmentAndPortNumber(UUID serviceId, String environment, Integer portNumber) → boolean
  countByServiceId(UUID serviceId) → long
Paginated Methods: (none)


=== PortRangeRepository ===
Extends: JpaRepository<PortRange, UUID>
Custom Methods: (none)
Derived Methods:
  findByTeamId(UUID teamId) → List<PortRange>
  findByTeamIdAndEnvironment(UUID teamId, String environment) → List<PortRange>
  findByTeamIdAndPortTypeAndEnvironment(UUID teamId, PortType portType, String environment) → Optional<PortRange>
  existsByTeamId(UUID teamId) → boolean
Paginated Methods: (none)


=== ServiceDependencyRepository ===
Extends: JpaRepository<ServiceDependency, UUID>
Custom Methods (JPQL @Query):
  findAllByTeamId(@Param("teamId") UUID teamId) → List<ServiceDependency>
Derived Methods:
  findBySourceServiceId(UUID sourceServiceId) → List<ServiceDependency>
  findByTargetServiceId(UUID targetServiceId) → List<ServiceDependency>
  findBySourceServiceIdAndTargetServiceIdAndDependencyType(UUID sourceId, UUID targetId, DependencyType type) → Optional<ServiceDependency>
  existsBySourceServiceIdAndTargetServiceId(UUID sourceId, UUID targetId) → boolean
  countBySourceServiceId(UUID sourceServiceId) → long
  countByTargetServiceId(UUID targetServiceId) → long
Paginated Methods: (none)


=== ApiRouteRegistrationRepository ===
Extends: JpaRepository<ApiRouteRegistration, UUID>
Custom Methods (JPQL @Query):
  findByGatewayAndPrefixAndEnvironment(@Param("gatewayId") UUID, @Param("environment") String, @Param("prefix") String) → Optional<ApiRouteRegistration>
  findOverlappingRoutes(@Param("gatewayId") UUID, @Param("environment") String, @Param("prefix") String) → List<ApiRouteRegistration>
  findOverlappingDirectRoutes(@Param("teamId") UUID, @Param("environment") String, @Param("prefix") String) → List<ApiRouteRegistration>
Derived Methods:
  findByServiceId(UUID serviceId) → List<ApiRouteRegistration>
  findByGatewayServiceIdAndEnvironment(UUID gatewayServiceId, String environment) → List<ApiRouteRegistration>
Paginated Methods: (none)


=== ConfigTemplateRepository ===
Extends: JpaRepository<ConfigTemplate, UUID>
Custom Methods: (none)
Derived Methods:
  findByServiceId(UUID serviceId) → List<ConfigTemplate>
  findByServiceIdAndEnvironment(UUID serviceId, String environment) → List<ConfigTemplate>
  findByServiceIdAndTemplateType(UUID serviceId, ConfigTemplateType type) → List<ConfigTemplate>
  findByServiceIdAndTemplateTypeAndEnvironment(UUID serviceId, ConfigTemplateType type, String environment) → Optional<ConfigTemplate>
Paginated Methods: (none)


=== EnvironmentConfigRepository ===
Extends: JpaRepository<EnvironmentConfig, UUID>
Custom Methods:
  deleteByServiceIdAndEnvironmentAndConfigKey(UUID serviceId, String environment, String configKey) → void
Derived Methods:
  findByServiceIdAndEnvironment(UUID serviceId, String environment) → List<EnvironmentConfig>
  findByServiceIdAndEnvironmentAndConfigKey(UUID serviceId, String environment, String configKey) → Optional<EnvironmentConfig>
  findByServiceId(UUID serviceId) → List<EnvironmentConfig>
Paginated Methods: (none)


=== InfraResourceRepository ===
Extends: JpaRepository<InfraResource, UUID>
Custom Methods (JPQL @Query):
  findOrphansByTeamId(@Param("teamId") UUID teamId) → List<InfraResource>
Derived Methods:
  findByTeamId(UUID teamId) → List<InfraResource>
  findByServiceId(UUID serviceId) → List<InfraResource>
  findByTeamIdAndResourceTypeAndResourceNameAndEnvironment(UUID teamId, InfraResourceType type, String name, String environment) → Optional<InfraResource>
Paginated Methods:
  findByTeamId(UUID teamId, Pageable pageable) → Page<InfraResource>
  findByTeamIdAndResourceType(UUID teamId, InfraResourceType type, Pageable pageable) → Page<InfraResource>
  findByTeamIdAndEnvironment(UUID teamId, String environment, Pageable pageable) → Page<InfraResource>
  findByTeamIdAndResourceTypeAndEnvironment(UUID teamId, InfraResourceType type, String environment, Pageable pageable) → Page<InfraResource>


=== WorkstationProfileRepository ===
Extends: JpaRepository<WorkstationProfile, UUID>
Custom Methods: (none)
Derived Methods:
  findByTeamId(UUID teamId) → List<WorkstationProfile>
  findByTeamIdAndIsDefaultTrue(UUID teamId) → Optional<WorkstationProfile>
  findByTeamIdAndName(UUID teamId, String name) → Optional<WorkstationProfile>
  countByTeamId(UUID teamId) → long
Paginated Methods: (none)
```


### Logger Module


```
=== LogEntryRepository ===
Extends: JpaRepository<LogEntry, UUID>
Custom Methods:
  @Modifying @Query deleteByTeamIdAndTimestampBeforeAndLevelIn(UUID teamId, Instant cutoff, List<LogLevel> levels) -> void
  @Modifying @Query deleteByTeamIdAndServiceNameAndTimestampBeforeAndLevelIn(UUID teamId, String serviceName, Instant cutoff, List<LogLevel> levels) -> void
  @Query countGroupByServiceName() -> List<Object[]>
  @Query countGroupByLevel() -> List<Object[]>
  @Query findOldestTimestamp() -> Optional<Instant>
  @Query findNewestTimestamp() -> Optional<Instant>
Derived Methods:
  findByCorrelationIdOrderByTimestampAsc(String correlationId) -> List<LogEntry>
  countByTeamIdAndLevel(UUID teamId, LogLevel level) -> long
  countByTeamIdAndServiceNameAndLevelAndTimestampBetween(UUID teamId, String serviceName, LogLevel level, Instant start, Instant end) -> long
  countByTeamIdAndTimestampBetween(UUID teamId, Instant start, Instant end) -> long
  countByTeamIdAndServiceNameAndTimestampBetween(UUID teamId, String serviceName, Instant start, Instant end) -> long
  deleteByTimestampBefore(Instant cutoff) -> void
  deleteByTeamIdAndTimestampBefore(UUID teamId, Instant cutoff) -> void
  deleteByTeamIdAndServiceNameAndTimestampBefore(UUID teamId, String serviceName, Instant cutoff) -> void
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<LogEntry>
  findByTeamIdAndServiceName(UUID teamId, String serviceName, Pageable) -> Page<LogEntry>
  findByTeamIdAndLevel(UUID teamId, LogLevel level, Pageable) -> Page<LogEntry>
  findByTeamIdAndServiceNameAndLevel(UUID teamId, String serviceName, LogLevel level, Pageable) -> Page<LogEntry>
  findByTeamIdAndTimestampBetween(UUID teamId, Instant start, Instant end, Pageable) -> Page<LogEntry>
  findByTeamIdAndServiceNameAndTimestampBetween(UUID teamId, String serviceName, Instant start, Instant end, Pageable) -> Page<LogEntry>
  findByTeamIdAndLevelAndTimestampBetween(UUID teamId, LogLevel level, Instant start, Instant end, Pageable) -> Page<LogEntry>
  findByTeamIdAndCorrelationId(UUID teamId, String correlationId, Pageable) -> Page<LogEntry>

=== LogSourceRepository ===
Extends: JpaRepository<LogSource, UUID>
Derived Methods:
  findByTeamId(UUID teamId) -> List<LogSource>
  findByTeamIdAndIsActiveTrue(UUID teamId) -> List<LogSource>
  findByTeamIdAndName(UUID teamId, String name) -> Optional<LogSource>
  findByServiceId(UUID serviceId) -> Optional<LogSource>
  existsByTeamIdAndName(UUID teamId, String name) -> boolean
  countByTeamId(UUID teamId) -> long
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<LogSource>

=== AlertRuleRepository ===
Extends: JpaRepository<AlertRule, UUID>
Derived Methods:
  findByTeamId(UUID teamId) -> List<AlertRule>
  findByTrapId(UUID trapId) -> List<AlertRule>
  findByTrapIdAndIsActiveTrue(UUID trapId) -> List<AlertRule>
  findByChannelId(UUID channelId) -> List<AlertRule>
  countByTeamId(UUID teamId) -> long
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<AlertRule>

=== AlertChannelRepository ===
Extends: JpaRepository<AlertChannel, UUID>
Derived Methods:
  findByTeamId(UUID teamId) -> List<AlertChannel>
  findByTeamIdAndIsActiveTrue(UUID teamId) -> List<AlertChannel>
  findByTeamIdAndChannelType(UUID teamId, AlertChannelType channelType) -> List<AlertChannel>
  countByTeamId(UUID teamId) -> long
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<AlertChannel>

=== AlertHistoryRepository ===
Extends: JpaRepository<AlertHistory, UUID>
Derived Methods:
  findByTeamIdAndStatusAndCreatedAtAfter(UUID teamId, AlertStatus status, Instant since) -> List<AlertHistory>
  countByTeamIdAndStatus(UUID teamId, AlertStatus status) -> long
  countByTeamIdAndSeverityAndStatus(UUID teamId, AlertSeverity severity, AlertStatus status) -> long
  existsByRuleIdAndCreatedAtAfter(UUID ruleId, Instant since) -> boolean
Paginated Methods:
  findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable) -> Page<AlertHistory>
  findByTeamIdAndStatus(UUID teamId, AlertStatus status, Pageable) -> Page<AlertHistory>
  findByTeamIdAndSeverity(UUID teamId, AlertSeverity severity, Pageable) -> Page<AlertHistory>
  findByRuleId(UUID ruleId, Pageable) -> Page<AlertHistory>

=== TraceSpanRepository ===
Extends: JpaRepository<TraceSpan, UUID>
Derived Methods:
  findByCorrelationIdOrderByStartTimeAsc(String correlationId) -> List<TraceSpan>
  findByTraceIdOrderByStartTimeAsc(String traceId) -> List<TraceSpan>
  findByTeamIdAndStartTimeBetween(UUID teamId, Instant start, Instant end) -> List<TraceSpan>
  findByTeamIdAndServiceNameAndStatus(UUID teamId, String serviceName, SpanStatus status) -> List<TraceSpan>
  deleteByStartTimeBefore(Instant cutoff) -> void
  countByTeamId(UUID teamId) -> long
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<TraceSpan>
  findByTeamIdAndServiceName(UUID teamId, String serviceName, Pageable) -> Page<TraceSpan>

=== RetentionPolicyRepository ===
Extends: JpaRepository<RetentionPolicy, UUID>
Derived Methods:
  findByTeamId(UUID teamId) -> List<RetentionPolicy>
  findByIsActiveTrue() -> List<RetentionPolicy>
  findByTeamIdAndIsActiveTrue(UUID teamId) -> List<RetentionPolicy>
  countByTeamId(UUID teamId) -> long
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<RetentionPolicy>

=== DashboardRepository ===
Extends: JpaRepository<Dashboard, UUID>
Derived Methods:
  findByTeamId(UUID teamId) -> List<Dashboard>
  findByTeamIdAndIsSharedTrue(UUID teamId) -> List<Dashboard>
  findByTeamIdAndIsTemplateTrue(UUID teamId) -> List<Dashboard>
  findByCreatedBy(UUID createdBy) -> List<Dashboard>
  countByTeamId(UUID teamId) -> long
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<Dashboard>

=== DashboardWidgetRepository ===
Extends: JpaRepository<DashboardWidget, UUID>
Derived Methods:
  findByDashboardIdOrderBySortOrderAsc(UUID dashboardId) -> List<DashboardWidget>
  deleteByDashboardId(UUID dashboardId) -> void
  countByDashboardId(UUID dashboardId) -> long
Paginated Methods: None

=== LogTrapRepository ===
Extends: JpaRepository<LogTrap, UUID>
Derived Methods:
  findByTeamId(UUID teamId) -> List<LogTrap>
  findByTeamIdAndIsActiveTrue(UUID teamId) -> List<LogTrap>
  findByTrapTypeAndIsActiveTrue(TrapType trapType) -> List<LogTrap>
  countByTeamId(UUID teamId) -> long
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<LogTrap>

=== TrapConditionRepository ===
Extends: JpaRepository<TrapCondition, UUID>
Derived Methods:
  findByTrapId(UUID trapId) -> List<TrapCondition>
  deleteByTrapId(UUID trapId) -> void
Paginated Methods: None

=== SavedQueryRepository ===
Extends: JpaRepository<SavedQuery, UUID>
Derived Methods:
  findByTeamId(UUID teamId) -> List<SavedQuery>
  findByCreatedBy(UUID createdBy) -> List<SavedQuery>
  findByTeamIdAndIsSharedTrue(UUID teamId) -> List<SavedQuery>
  existsByTeamIdAndName(UUID teamId, String name) -> boolean
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<SavedQuery>

=== QueryHistoryRepository ===
Extends: JpaRepository<QueryHistory, UUID>
Derived Methods:
  deleteByCreatedAtBefore(Instant cutoff) -> void
Paginated Methods:
  findByCreatedByOrderByCreatedAtDesc(UUID createdBy, Pageable) -> Page<QueryHistory>
  findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable) -> Page<QueryHistory>

=== MetricRepository ===
Extends: JpaRepository<Metric, UUID>
Derived Methods:
  findByTeamId(UUID teamId) -> List<Metric>
  findByTeamIdAndServiceName(UUID teamId, String serviceName) -> List<Metric>
  findByTeamIdAndNameAndServiceName(UUID teamId, String name, String serviceName) -> Optional<Metric>
  findByTeamIdAndMetricType(UUID teamId, MetricType metricType) -> List<Metric>
  countByTeamId(UUID teamId) -> long
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<Metric>

=== MetricSeriesRepository ===
Extends: JpaRepository<MetricSeries, UUID>
Custom Methods:
  @Query findAverageValueByMetricIdAndTimestampBetween(UUID metricId, Instant start, Instant end) -> Optional<Double>
  @Query findMaxValueByMetricIdAndTimestampBetween(UUID metricId, Instant start, Instant end) -> Optional<Double>
  @Query findMinValueByMetricIdAndTimestampBetween(UUID metricId, Instant start, Instant end) -> Optional<Double>
Derived Methods:
  findByMetricIdAndTimestampBetweenOrderByTimestampAsc(UUID metricId, Instant start, Instant end) -> List<MetricSeries>
  deleteByTimestampBefore(Instant cutoff) -> void
  deleteByMetricId(UUID metricId) -> void
  countByMetricId(UUID metricId) -> long
Paginated Methods:
  findByMetricId(UUID metricId, Pageable) -> Page<MetricSeries>

=== AnomalyBaselineRepository ===
Extends: JpaRepository<AnomalyBaseline, UUID>
Derived Methods:
  findByTeamId(UUID teamId) -> List<AnomalyBaseline>
  findByTeamIdAndServiceName(UUID teamId, String serviceName) -> List<AnomalyBaseline>
  findByTeamIdAndServiceNameAndMetricName(UUID teamId, String serviceName, String metricName) -> Optional<AnomalyBaseline>
  findByIsActiveTrue() -> List<AnomalyBaseline>
  findByTeamIdAndIsActiveTrue(UUID teamId) -> List<AnomalyBaseline>
  countByTeamId(UUID teamId) -> long
Paginated Methods:
  findByTeamId(UUID teamId, Pageable) -> Page<AnomalyBaseline>
```

---


### Courier Module


**Package:** `com.codeops.courier.repository`
All extend `JpaRepository<Entity, UUID>` and are annotated with `@Repository`.

---

### 8.1 CollectionRepository
- **Extends:** `JpaRepository<Collection, UUID>`
- **Derived queries:**
  - `List<Collection> findByTeamId(UUID teamId)`
  - `Optional<Collection> findByTeamIdAndName(UUID teamId, String name)`
  - `List<Collection> findByTeamIdAndIsSharedTrue(UUID teamId)`
  - `List<Collection> findByCreatedBy(UUID userId)`
  - `boolean existsByTeamIdAndName(UUID teamId, String name)`
  - `long countByTeamId(UUID teamId)`
  - `List<Collection> findByTeamIdAndNameContainingIgnoreCase(UUID teamId, String name)`
- **Paginated queries:**
  - `Page<Collection> findByTeamId(UUID teamId, Pageable pageable)`

### 8.2 FolderRepository
- **Extends:** `JpaRepository<Folder, UUID>`
- **Derived queries:**
  - `List<Folder> findByCollectionIdOrderBySortOrder(UUID collectionId)`
  - `List<Folder> findByParentFolderIdOrderBySortOrder(UUID parentFolderId)`
  - `List<Folder> findByCollectionIdAndParentFolderIsNullOrderBySortOrder(UUID collectionId)`
  - `long countByCollectionId(UUID collectionId)`
  - `long countByParentFolderId(UUID parentFolderId)`

### 8.3 RequestRepository
- **Extends:** `JpaRepository<Request, UUID>`
- **Derived queries:**
  - `List<Request> findByFolderIdOrderBySortOrder(UUID folderId)`
  - `long countByFolderId(UUID folderId)`

### 8.4 RequestHeaderRepository
- **Extends:** `JpaRepository<RequestHeader, UUID>`
- **Derived queries:**
  - `List<RequestHeader> findByRequestId(UUID requestId)`
  - `void deleteByRequestId(UUID requestId)`

### 8.5 RequestParamRepository
- **Extends:** `JpaRepository<RequestParam, UUID>`
- **Derived queries:**
  - `List<RequestParam> findByRequestId(UUID requestId)`
  - `void deleteByRequestId(UUID requestId)`

### 8.6 RequestBodyRepository
- **Extends:** `JpaRepository<RequestBody, UUID>`
- **Derived queries:**
  - `Optional<RequestBody> findByRequestId(UUID requestId)`
  - `void deleteByRequestId(UUID requestId)`

### 8.7 RequestAuthRepository
- **Extends:** `JpaRepository<RequestAuth, UUID>`
- **Derived queries:**
  - `Optional<RequestAuth> findByRequestId(UUID requestId)`
  - `void deleteByRequestId(UUID requestId)`

### 8.8 RequestScriptRepository
- **Extends:** `JpaRepository<RequestScript, UUID>`
- **Derived queries:**
  - `List<RequestScript> findByRequestId(UUID requestId)`
  - `Optional<RequestScript> findByRequestIdAndScriptType(UUID requestId, ScriptType scriptType)`
  - `void deleteByRequestId(UUID requestId)`

### 8.9 RequestHistoryRepository
- **Extends:** `JpaRepository<RequestHistory, UUID>`
- **Derived queries:**
  - `List<RequestHistory> findByTeamIdAndRequestUrlContainingIgnoreCase(UUID teamId, String urlFragment)`
  - `void deleteByTeamIdAndCreatedAtBefore(UUID teamId, Instant cutoff)`
  - `void deleteByTeamId(UUID teamId)`
  - `long countByTeamId(UUID teamId)`
  - `List<RequestHistory> findByTeamIdAndCreatedAtBefore(UUID teamId, Instant cutoff)`
- **Paginated queries:**
  - `Page<RequestHistory> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable)`
  - `Page<RequestHistory> findByTeamIdAndUserId(UUID teamId, UUID userId, Pageable pageable)`
  - `Page<RequestHistory> findByTeamIdAndRequestMethod(UUID teamId, HttpMethod method, Pageable pageable)`

### 8.10 EnvironmentRepository
- **Extends:** `JpaRepository<Environment, UUID>`
- **Derived queries:**
  - `List<Environment> findByTeamId(UUID teamId)`
  - `Optional<Environment> findByTeamIdAndName(UUID teamId, String name)`
  - `Optional<Environment> findByTeamIdAndIsActiveTrue(UUID teamId)`
  - `boolean existsByTeamIdAndName(UUID teamId, String name)`
  - `long countByTeamId(UUID teamId)`

### 8.11 EnvironmentVariableRepository
- **Extends:** `JpaRepository<EnvironmentVariable, UUID>`
- **Derived queries:**
  - `List<EnvironmentVariable> findByEnvironmentId(UUID environmentId)`
  - `List<EnvironmentVariable> findByCollectionId(UUID collectionId)`
  - `List<EnvironmentVariable> findByEnvironmentIdAndIsEnabledTrue(UUID environmentId)`
  - `List<EnvironmentVariable> findByCollectionIdAndIsEnabledTrue(UUID collectionId)`
  - `void deleteByEnvironmentId(UUID environmentId)`

### 8.12 GlobalVariableRepository
- **Extends:** `JpaRepository<GlobalVariable, UUID>`
- **Derived queries:**
  - `List<GlobalVariable> findByTeamId(UUID teamId)`
  - `List<GlobalVariable> findByTeamIdAndIsEnabledTrue(UUID teamId)`
  - `Optional<GlobalVariable> findByTeamIdAndVariableKey(UUID teamId, String variableKey)`
  - `boolean existsByTeamIdAndVariableKey(UUID teamId, String variableKey)`

### 8.13 CollectionShareRepository
- **Extends:** `JpaRepository<CollectionShare, UUID>`
- **Derived queries:**
  - `List<CollectionShare> findByCollectionId(UUID collectionId)`
  - `List<CollectionShare> findBySharedWithUserId(UUID userId)`
  - `Optional<CollectionShare> findByCollectionIdAndSharedWithUserId(UUID collectionId, UUID userId)`
  - `boolean existsByCollectionIdAndSharedWithUserId(UUID collectionId, UUID userId)`
  - `void deleteByCollectionIdAndSharedWithUserId(UUID collectionId, UUID userId)`

### 8.14 ForkRepository
- **Extends:** `JpaRepository<Fork, UUID>`
- **Derived queries:**
  - `List<Fork> findBySourceCollectionId(UUID collectionId)`
  - `Optional<Fork> findByForkedCollectionId(UUID collectionId)`
  - `List<Fork> findByForkedByUserId(UUID userId)`
  - `boolean existsBySourceCollectionIdAndForkedByUserId(UUID collectionId, UUID userId)`

### 8.15 MergeRequestRepository
- **Extends:** `JpaRepository<MergeRequest, UUID>`
- **Derived queries:**
  - `List<MergeRequest> findByTargetCollectionId(UUID collectionId)`
  - `List<MergeRequest> findBySourceForkId(UUID forkId)`
  - `List<MergeRequest> findByTargetCollectionIdAndStatus(UUID collectionId, String status)`
  - `List<MergeRequest> findByRequestedByUserId(UUID userId)`

### 8.16 RunResultRepository
- **Extends:** `JpaRepository<RunResult, UUID>`
- **Derived queries:**
  - `List<RunResult> findByTeamIdOrderByCreatedAtDesc(UUID teamId)`
  - `List<RunResult> findByCollectionIdOrderByCreatedAtDesc(UUID collectionId)`
  - `List<RunResult> findByStatus(RunStatus status)`
- **Paginated queries:**
  - `Page<RunResult> findByTeamId(UUID teamId, Pageable pageable)`

### 8.17 RunIterationRepository
- **Extends:** `JpaRepository<RunIteration, UUID>`
- **Derived queries:**
  - `List<RunIteration> findByRunResultIdOrderByIterationNumber(UUID runResultId)`

### 8.18 CodeSnippetTemplateRepository
- **Extends:** `JpaRepository<CodeSnippetTemplate, UUID>`
- **Derived queries:**
  - `Optional<CodeSnippetTemplate> findByLanguage(CodeLanguage language)`
  - `List<CodeSnippetTemplate> findAllByOrderByDisplayNameAsc()`
  - `boolean existsByLanguage(CodeLanguage language)`

---


### Relay Module


### === ChannelRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/ChannelRepository.java`
Extends: `JpaRepository<Channel, UUID>`
Derived Methods:
- `findByTeamId(UUID teamId): List<Channel>`
- `findByTeamIdAndChannelType(UUID teamId, ChannelType channelType): List<Channel>`
- `findByTeamIdAndIsArchivedFalse(UUID teamId): List<Channel>`
- `findByTeamIdAndSlug(UUID teamId, String slug): Optional<Channel>`
- `findByTeamIdAndProjectId(UUID teamId, UUID projectId): Optional<Channel>`
- `findByTeamIdAndServiceId(UUID teamId, UUID serviceId): Optional<Channel>`
- `existsByTeamIdAndSlug(UUID teamId, String slug): boolean`
- `countByTeamId(UUID teamId): long`
Paginated Methods:
- `findByTeamId(UUID teamId, Pageable pageable): Page<Channel>`
Custom Methods: None

### === ChannelMemberRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/ChannelMemberRepository.java`
Extends: `JpaRepository<ChannelMember, UUID>`
Derived Methods:
- `findByChannelId(UUID channelId): List<ChannelMember>`
- `findByUserId(UUID userId): List<ChannelMember>`
- `findByUserIdAndIsMutedFalse(UUID userId): List<ChannelMember>`
- `findByChannelIdAndUserId(UUID channelId, UUID userId): Optional<ChannelMember>`
- `existsByChannelIdAndUserId(UUID channelId, UUID userId): boolean`
- `countByChannelId(UUID channelId): long`
- `deleteByChannelIdAndUserId(UUID channelId, UUID userId): void`
Custom Methods (JPQL @Query):
- `findChannelIdsByUserId(@Param("userId") UUID userId): List<UUID>` — SELECT cm.channelId FROM ChannelMember cm WHERE cm.userId = :userId

### === MessageRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/MessageRepository.java`
Extends: `JpaRepository<Message, UUID>`
Derived Methods:
- `findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentId): List<Message>`
- `countByChannelIdAndCreatedAtAfter(UUID channelId, Instant after): long`
- `countByChannelIdAndCreatedAtAfterAndIsDeletedFalse(UUID channelId, Instant after): long`
- `deleteByChannelId(UUID channelId): void`
Paginated Methods:
- `findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID channelId, Pageable pageable): Page<Message>`
- `findByChannelIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(UUID channelId, Pageable pageable): Page<Message>`
Custom Methods (JPQL @Query):
- `countUnreadMessages(@Param("channelId") UUID channelId, @Param("since") Instant since): long` — counts non-deleted messages after since timestamp
- `searchInChannel(@Param("channelId") UUID channelId, @Param("query") String query, Pageable pageable): Page<Message>` — case-insensitive LIKE search within one channel
- `searchAcrossChannels(@Param("channelIds") List<UUID> channelIds, @Param("query") String query, Pageable pageable): Page<Message>` — case-insensitive LIKE search across multiple channels

### === MessageThreadRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/MessageThreadRepository.java`
Extends: `JpaRepository<MessageThread, UUID>`
Derived Methods:
- `findByRootMessageId(UUID rootMessageId): Optional<MessageThread>`
- `findByChannelIdOrderByLastReplyAtDesc(UUID channelId): List<MessageThread>`
- `existsByRootMessageId(UUID rootMessageId): boolean`

### === DirectConversationRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/DirectConversationRepository.java`
Extends: `JpaRepository<DirectConversation, UUID>`
Derived Methods:
- `findByTeamIdAndParticipantIdsContainingOrderByLastMessageAtDesc(UUID teamId, String userId): List<DirectConversation>`
- `findByTeamIdAndParticipantIds(UUID teamId, String participantIds): Optional<DirectConversation>`
- `countByTeamIdAndParticipantIdsContaining(UUID teamId, String userId): long`

### === DirectMessageRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/DirectMessageRepository.java`
Extends: `JpaRepository<DirectMessage, UUID>`
Derived Methods:
- `countByConversationIdAndCreatedAtAfterAndIsDeletedFalse(UUID conversationId, Instant after): long`
- `countByConversationIdAndSenderIdNotAndCreatedAtAfterAndIsDeletedFalse(UUID conversationId, UUID senderId, Instant after): long`
- `countByConversationIdAndSenderIdNotAndIsDeletedFalse(UUID conversationId, UUID senderId): long`
- `deleteByConversationId(UUID conversationId): void`
Paginated Methods:
- `findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID conversationId, Pageable pageable): Page<DirectMessage>`

### === ReactionRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/ReactionRepository.java`
Extends: `JpaRepository<Reaction, UUID>`
Derived Methods:
- `findByMessageId(UUID messageId): List<Reaction>`
- `findByDirectMessageId(UUID directMessageId): List<Reaction>`
- `findByMessageIdAndUserIdAndEmoji(UUID messageId, UUID userId, String emoji): Optional<Reaction>`
- `findByDirectMessageIdAndUserIdAndEmoji(UUID directMessageId, UUID userId, String emoji): Optional<Reaction>`
- `countByMessageIdAndEmoji(UUID messageId, String emoji): long`
- `deleteByMessageId(UUID messageId): void`
- `deleteByDirectMessageId(UUID directMessageId): void`
Custom Methods (JPQL @Query):
- `findByUserIdAndMessageChannelId(@Param("userId") UUID userId, @Param("channelId") UUID channelId): List<Reaction>` — joins via r.message to filter by channel
- `countReactionsByMessageId(@Param("messageId") UUID messageId): List<Object[]>` — returns [emoji, count] pairs grouped by emoji, ordered by count DESC

### === FileAttachmentRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/FileAttachmentRepository.java`
Extends: `JpaRepository<FileAttachment, UUID>`
Derived Methods:
- `findByMessageId(UUID messageId): List<FileAttachment>`
- `findByMessageIdOrderByCreatedAtAsc(UUID messageId): List<FileAttachment>`
- `countByMessageId(UUID messageId): long`
- `findByDirectMessageId(UUID directMessageId): List<FileAttachment>`
- `findByTeamIdOrderByCreatedAtDesc(UUID teamId): List<FileAttachment>`
- `countByTeamId(UUID teamId): long`
Paginated Methods:
- `findByTeamId(UUID teamId, Pageable pageable): Page<FileAttachment>`
Custom Methods (JPQL @Query):
- `totalStorageByTeamId(@Param("teamId") UUID teamId): long` — SELECT COALESCE(SUM(f.fileSizeBytes), 0) for quota enforcement

### === PinnedMessageRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/PinnedMessageRepository.java`
Extends: `JpaRepository<PinnedMessage, UUID>`
Derived Methods:
- `findByChannelIdOrderByCreatedAtDesc(UUID channelId): List<PinnedMessage>`
- `findByChannelIdAndMessageId(UUID channelId, UUID messageId): Optional<PinnedMessage>`
- `existsByChannelIdAndMessageId(UUID channelId, UUID messageId): boolean`
- `countByChannelId(UUID channelId): long`

### === ReadReceiptRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/ReadReceiptRepository.java`
Extends: `JpaRepository<ReadReceipt, UUID>`
Derived Methods:
- `findByChannelIdAndUserId(UUID channelId, UUID userId): Optional<ReadReceipt>`
- `findByChannelId(UUID channelId): List<ReadReceipt>`
- `deleteByChannelId(UUID channelId): void`

### === UserPresenceRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/UserPresenceRepository.java`
Extends: `JpaRepository<UserPresence, UUID>`
Derived Methods:
- `findByUserIdAndTeamId(UUID userId, UUID teamId): Optional<UserPresence>`
- `findByTeamId(UUID teamId): List<UserPresence>`
- `findByTeamIdAndStatus(UUID teamId, PresenceStatus status): List<UserPresence>`
- `findByTeamIdAndStatusNot(UUID teamId, PresenceStatus status): List<UserPresence>`
Custom Methods (JPQL @Query):
- `findStaleOnlineUsers(@Param("cutoff") Instant cutoff): List<UserPresence>` — SELECT up WHERE status = 'ONLINE' AND lastHeartbeatAt < cutoff

### === PlatformEventRepository ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/repository/PlatformEventRepository.java`
Extends: `JpaRepository<PlatformEvent, UUID>`
Derived Methods:
- `findByTeamIdAndEventType(UUID teamId, PlatformEventType eventType): List<PlatformEvent>`
- `findBySourceEntityIdOrderByCreatedAtDesc(UUID sourceEntityId): List<PlatformEvent>`
- `findByTeamIdAndIsDeliveredFalseOrderByCreatedAtAsc(UUID teamId): List<PlatformEvent>`
- `findByTeamIdAndSourceModule(UUID teamId, String sourceModule): List<PlatformEvent>`
- `findByTeamIdAndIsDeliveredFalse(UUID teamId): List<PlatformEvent>`
- `countByTeamIdAndCreatedAtAfter(UUID teamId, Instant after): long`
Paginated Methods:
- `findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable): Page<PlatformEvent>`
- `findByTeamIdAndEventTypeOrderByCreatedAtDesc(UUID teamId, PlatformEventType eventType, Pageable pageable): Page<PlatformEvent>`

---


---

## 9. Service Layer

### Core Module


All services are in package `com.codeops.service`. All use `@RequiredArgsConstructor` for dependency injection unless otherwise noted.

### 9.1 AuthService
- **File:** `AuthService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** UserRepository, PasswordEncoder, JwtTokenProvider, TeamMemberRepository, MfaEmailCodeRepository, EmailService
- **Public methods:**
  - `AuthResponse register(RegisterRequest request)` -- Registers a new user, hashes password, issues JWT tokens
  - `AuthResponse login(LoginRequest request)` -- Authenticates user, handles MFA challenge if enabled, issues JWT tokens
  - `AuthResponse refreshToken(RefreshTokenRequest request)` -- Exchanges valid refresh token for new token pair
  - `void changePassword(ChangePasswordRequest request)` -- Changes current user's password after verifying current password

### 9.2 UserService
- **File:** `UserService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional(readOnly=true)
- **Dependencies:** UserRepository
- **Public methods:**
  - `UserResponse getUserById(UUID id)` -- Retrieves user by UUID
  - `UserResponse getUserByEmail(String email)` -- Retrieves user by email
  - `UserResponse getCurrentUser()` -- Retrieves the currently authenticated user's profile
  - `UserResponse updateUser(UUID userId, UpdateUserRequest request)` -- Updates user profile (display name, avatar URL)
  - `List<UserResponse> searchUsers(String query)` -- Searches users by display name (max 20 results)
  - `void deactivateUser(UUID userId)` -- Sets user isActive to false
  - `void activateUser(UUID userId)` -- Sets user isActive to true

### 9.3 TeamService
- **File:** `TeamService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** TeamRepository, TeamMemberRepository, UserRepository, InvitationRepository
- **Public methods:**
  - `TeamResponse createTeam(CreateTeamRequest request)` -- Creates team, assigns current user as OWNER
  - `TeamResponse getTeam(UUID teamId)` -- Retrieves team by ID with member count
  - `List<TeamResponse> getTeamsForUser()` -- Retrieves all teams for current user
  - `TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request)` -- Updates team name, description, webhook URL
  - `void deleteTeam(UUID teamId)` -- Hard-deletes team (OWNER only)
  - `List<TeamMemberResponse> getTeamMembers(UUID teamId)` -- Retrieves all members of a team
  - `TeamMemberResponse updateMemberRole(UUID teamId, UUID userId, UpdateMemberRoleRequest request)` -- Updates member role, supports ownership transfer
  - `void removeMember(UUID teamId, UUID userId)` -- Removes member (self-removal or admin)
  - `InvitationResponse inviteMember(UUID teamId, InviteMemberRequest request)` -- Creates pending invitation
  - `TeamResponse acceptInvitation(String token)` -- Accepts invitation, creates team member
  - `List<InvitationResponse> getTeamInvitations(UUID teamId)` -- Retrieves pending invitations
  - `void cancelInvitation(UUID invitationId)` -- Cancels invitation (sets status to EXPIRED)

### 9.4 ProjectService
- **File:** `ProjectService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** ProjectRepository, TeamMemberRepository, UserRepository, TeamRepository, GitHubConnectionRepository, JiraConnectionRepository, ObjectMapper, RemediationTaskRepository, ComplianceItemRepository, SpecificationRepository, FindingRepository, AgentRunRepository, BugInvestigationRepository, TechDebtItemRepository, DependencyVulnerabilityRepository, DependencyScanRepository, HealthSnapshotRepository, QaJobRepository, HealthScheduleRepository, ProjectDirectiveRepository, DirectiveRepository
- **Public methods:**
  - `ProjectResponse createProject(UUID teamId, CreateProjectRequest request)` -- Creates project with optional GitHub/Jira connections
  - `ProjectResponse getProject(UUID projectId)` -- Retrieves project by ID
  - `List<ProjectResponse> getProjectsForTeam(UUID teamId)` -- Retrieves non-archived projects for team
  - `PageResponse<ProjectResponse> getAllProjectsForTeam(UUID teamId, boolean includeArchived, Pageable pageable)` -- Paginated project listing with archive filter
  - `ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request)` -- Updates project fields
  - `void archiveProject(UUID projectId)` -- Sets isArchived to true
  - `void unarchiveProject(UUID projectId)` -- Sets isArchived to false
  - `void deleteProject(UUID projectId)` -- Hard-deletes project and all child records (OWNER only, FK-safe cascade)
  - `void updateHealthScore(UUID projectId, int score)` -- Updates project health score and lastAuditAt

### 9.5 AdminService
- **File:** `AdminService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** UserRepository, TeamRepository, ProjectRepository, QaJobRepository, SystemSettingRepository
- **Public methods:**
  - `Page<UserResponse> getAllUsers(Pageable pageable)` -- Paginated list of all users (admin only)
  - `UserResponse getUserById(UUID userId)` -- Retrieves user by ID (admin only)
  - `UserResponse updateUserStatus(UUID userId, AdminUpdateUserRequest request)` -- Updates user active status
  - `SystemSettingResponse getSystemSetting(String key)` -- Retrieves system setting by key
  - `SystemSettingResponse updateSystemSetting(UpdateSystemSettingRequest request)` -- Creates or updates system setting
  - `List<SystemSettingResponse> getAllSettings()` -- Retrieves all system settings
  - `Map<String, Object> getUsageStats()` -- Returns platform usage statistics (admin only)

### 9.6 PersonaService
- **File:** `PersonaService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** PersonaRepository, TeamMemberRepository, UserRepository, TeamRepository
- **Public methods:**
  - `PersonaResponse createPersona(CreatePersonaRequest request)` -- Creates persona with scope validation
  - `PersonaResponse getPersona(UUID personaId)` -- Retrieves persona by ID
  - `PageResponse<PersonaResponse> getPersonasForTeam(UUID teamId, Pageable pageable)` -- Paginated personas for team
  - `List<PersonaResponse> getPersonasByAgentType(UUID teamId, AgentType agentType)` -- Personas filtered by agent type
  - `PersonaResponse getDefaultPersona(UUID teamId, AgentType agentType)` -- Retrieves default persona for team/agent type
  - `List<PersonaResponse> getPersonasByUser(UUID userId)` -- Personas created by a user
  - `List<PersonaResponse> getSystemPersonas()` -- All SYSTEM-scoped personas
  - `PersonaResponse updatePersona(UUID personaId, UpdatePersonaRequest request)` -- Updates persona, increments version on content change
  - `void deletePersona(UUID personaId)` -- Deletes persona (not SYSTEM)
  - `PersonaResponse setAsDefault(UUID personaId)` -- Sets persona as default for its team/agent type
  - `PersonaResponse removeDefault(UUID personaId)` -- Removes default designation

### 9.7 DirectiveService
- **File:** `DirectiveService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** DirectiveRepository, ProjectDirectiveRepository, ProjectRepository, TeamMemberRepository, UserRepository, TeamRepository
- **Public methods:**
  - `DirectiveResponse createDirective(CreateDirectiveRequest request)` -- Creates directive with scope/category
  - `DirectiveResponse getDirective(UUID directiveId)` -- Retrieves directive by ID
  - `List<DirectiveResponse> getDirectivesForTeam(UUID teamId)` -- All directives for a team
  - `List<DirectiveResponse> getDirectivesForProject(UUID projectId)` -- All directives scoped to a project
  - `List<DirectiveResponse> getDirectivesByCategory(UUID teamId, DirectiveScope scope)` -- Directives filtered by scope
  - `DirectiveResponse updateDirective(UUID directiveId, UpdateDirectiveRequest request)` -- Updates directive, increments version on content change
  - `void deleteDirective(UUID directiveId)` -- Deletes directive and all project assignments
  - `ProjectDirectiveResponse assignToProject(AssignDirectiveRequest request)` -- Assigns directive to project
  - `void removeFromProject(UUID projectId, UUID directiveId)` -- Removes directive assignment from project
  - `List<ProjectDirectiveResponse> getProjectDirectives(UUID projectId)` -- All directive assignments for a project
  - `List<DirectiveResponse> getEnabledDirectivesForProject(UUID projectId)` -- Only enabled directives for a project
  - `ProjectDirectiveResponse toggleProjectDirective(UUID projectId, UUID directiveId, boolean enabled)` -- Toggles directive enabled/disabled

### 9.8 QaJobService
- **File:** `QaJobService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** QaJobRepository, AgentRunRepository, FindingRepository, ProjectRepository, UserRepository, TeamMemberRepository, ProjectService
- **Public methods:**
  - `JobResponse createJob(CreateJobRequest request)` -- Creates job with PENDING status
  - `JobResponse getJob(UUID jobId)` -- Retrieves job with full detail
  - `PageResponse<JobSummaryResponse> getJobsForProject(UUID projectId, Pageable pageable)` -- Paginated job summaries for project
  - `PageResponse<JobSummaryResponse> getJobsByUser(UUID userId, Pageable pageable)` -- Paginated job summaries by user
  - `JobResponse updateJob(UUID jobId, UpdateJobRequest request)` -- Updates job fields, auto-updates project health on completion
  - `void deleteJob(UUID jobId)` -- Deletes job (admin/owner only)

### 9.9 FindingService
- **File:** `FindingService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** FindingRepository, QaJobRepository, UserRepository, TeamMemberRepository
- **Public methods:**
  - `FindingResponse createFinding(CreateFindingRequest request)` -- Creates single finding with OPEN status
  - `List<FindingResponse> createFindings(List<CreateFindingRequest> requests)` -- Batch creates findings (same job)
  - `FindingResponse getFinding(UUID findingId)` -- Retrieves finding by ID
  - `PageResponse<FindingResponse> getFindingsForJob(UUID jobId, Pageable pageable)` -- Paginated findings for job
  - `PageResponse<FindingResponse> getFindingsByJobAndSeverity(UUID jobId, Severity severity, Pageable pageable)` -- Filtered by severity
  - `PageResponse<FindingResponse> getFindingsByJobAndAgent(UUID jobId, AgentType agentType, Pageable pageable)` -- Filtered by agent type
  - `PageResponse<FindingResponse> getFindingsByJobAndStatus(UUID jobId, FindingStatus status, Pageable pageable)` -- Filtered by status
  - `FindingResponse updateFindingStatus(UUID findingId, UpdateFindingStatusRequest request)` -- Updates finding status with audit trail
  - `List<FindingResponse> bulkUpdateFindingStatus(BulkUpdateFindingsRequest request)` -- Batch status update (same job)
  - `Map<Severity, Long> countFindingsBySeverity(UUID jobId)` -- Counts findings grouped by severity

### 9.10 AgentRunService
- **File:** `AgentRunService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** AgentRunRepository, QaJobRepository, TeamMemberRepository
- **Public methods:**
  - `AgentRunResponse createAgentRun(CreateAgentRunRequest request)` -- Creates single agent run with PENDING status
  - `List<AgentRunResponse> createAgentRuns(UUID jobId, List<AgentType> agentTypes)` -- Batch creates agent runs
  - `List<AgentRunResponse> getAgentRuns(UUID jobId)` -- Retrieves all agent runs for a job
  - `AgentRunResponse getAgentRun(UUID agentRunId)` -- Retrieves single agent run
  - `AgentRunResponse updateAgentRun(UUID agentRunId, UpdateAgentRunRequest request)` -- Updates agent run fields

### 9.11 BugInvestigationService
- **File:** `BugInvestigationService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** BugInvestigationRepository, QaJobRepository, TeamMemberRepository, S3StorageService
- **Public methods:**
  - `BugInvestigationResponse createInvestigation(CreateBugInvestigationRequest request)` -- Creates bug investigation linked to job
  - `BugInvestigationResponse getInvestigation(UUID investigationId)` -- Retrieves investigation by ID
  - `BugInvestigationResponse getInvestigationByJob(UUID jobId)` -- Retrieves investigation by job ID
  - `BugInvestigationResponse getInvestigationByJiraKey(String jiraKey)` -- Retrieves investigation by Jira key
  - `BugInvestigationResponse updateInvestigation(UUID investigationId, UpdateBugInvestigationRequest request)` -- Updates investigation fields
  - `String uploadRca(UUID jobId, String rcaMd)` -- Uploads RCA markdown to S3

### 9.12 ComplianceService
- **File:** `ComplianceService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** ComplianceItemRepository, SpecificationRepository, QaJobRepository, TeamMemberRepository
- **Public methods:**
  - `SpecificationResponse createSpecification(CreateSpecificationRequest request)` -- Creates specification for a job
  - `PageResponse<SpecificationResponse> getSpecificationsForJob(UUID jobId, Pageable pageable)` -- Paginated specifications for job
  - `ComplianceItemResponse createComplianceItem(CreateComplianceItemRequest request)` -- Creates single compliance item
  - `List<ComplianceItemResponse> createComplianceItems(List<CreateComplianceItemRequest> requests)` -- Batch creates compliance items
  - `PageResponse<ComplianceItemResponse> getComplianceItemsForJob(UUID jobId, Pageable pageable)` -- Paginated compliance items for job
  - `PageResponse<ComplianceItemResponse> getComplianceItemsByStatus(UUID jobId, ComplianceStatus status, Pageable pageable)` -- Filtered by status
  - `Map<String, Object> getComplianceSummary(UUID jobId)` -- Compliance summary statistics

### 9.13 TechDebtService
- **File:** `TechDebtService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** TechDebtItemRepository, ProjectRepository, TeamMemberRepository, QaJobRepository
- **Public methods:**
  - `TechDebtItemResponse createTechDebtItem(CreateTechDebtItemRequest request)` -- Creates tech debt item
  - `List<TechDebtItemResponse> createTechDebtItems(List<CreateTechDebtItemRequest> requests)` -- Batch creates tech debt items
  - `TechDebtItemResponse getTechDebtItem(UUID itemId)` -- Retrieves tech debt item by ID
  - `PageResponse<TechDebtItemResponse> getTechDebtForProject(UUID projectId, Pageable pageable)` -- Paginated tech debt for project
  - `PageResponse<TechDebtItemResponse> getTechDebtByStatus(UUID projectId, DebtStatus status, Pageable pageable)` -- Filtered by status
  - `PageResponse<TechDebtItemResponse> getTechDebtByCategory(UUID projectId, DebtCategory category, Pageable pageable)` -- Filtered by category
  - `TechDebtItemResponse updateTechDebtStatus(UUID itemId, UpdateTechDebtStatusRequest request)` -- Updates debt item status
  - `void deleteTechDebtItem(UUID itemId)` -- Deletes tech debt item
  - `Map<String, Object> getDebtSummary(UUID projectId)` -- Tech debt summary statistics

### 9.14 DependencyService
- **File:** `DependencyService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** DependencyScanRepository, DependencyVulnerabilityRepository (as vulnerabilityRepository), ProjectRepository, TeamMemberRepository, QaJobRepository
- **Public methods:**
  - `DependencyScanResponse createScan(CreateDependencyScanRequest request)` -- Creates dependency scan
  - `DependencyScanResponse getScan(UUID scanId)` -- Retrieves scan by ID
  - `PageResponse<DependencyScanResponse> getScansForProject(UUID projectId, Pageable pageable)` -- Paginated scans for project
  - `DependencyScanResponse getLatestScan(UUID projectId)` -- Retrieves most recent scan for project
  - `VulnerabilityResponse addVulnerability(CreateVulnerabilityRequest request)` -- Creates single vulnerability
  - `List<VulnerabilityResponse> addVulnerabilities(List<CreateVulnerabilityRequest> requests)` -- Batch creates vulnerabilities
  - `PageResponse<VulnerabilityResponse> getVulnerabilities(UUID scanId, Pageable pageable)` -- Paginated vulnerabilities for scan
  - `PageResponse<VulnerabilityResponse> getVulnerabilitiesBySeverity(UUID scanId, Severity severity, Pageable pageable)` -- Filtered by severity
  - `PageResponse<VulnerabilityResponse> getOpenVulnerabilities(UUID scanId, Pageable pageable)` -- Open vulnerabilities only
  - `VulnerabilityResponse updateVulnerabilityStatus(UUID vulnerabilityId, VulnerabilityStatus status)` -- Updates vulnerability status

### 9.15 RemediationTaskService
- **File:** `RemediationTaskService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** RemediationTaskRepository, QaJobRepository, UserRepository, TeamMemberRepository, FindingRepository, S3StorageService
- **Public methods:**
  - `TaskResponse createTask(CreateTaskRequest request)` -- Creates remediation task with linked findings
  - `List<TaskResponse> createTasks(List<CreateTaskRequest> requests)` -- Batch creates remediation tasks
  - `PageResponse<TaskResponse> getTasksForJob(UUID jobId, Pageable pageable)` -- Paginated tasks for job
  - `TaskResponse getTask(UUID taskId)` -- Retrieves task by ID
  - `PageResponse<TaskResponse> getTasksAssignedToUser(UUID userId, Pageable pageable)` -- Paginated tasks assigned to user
  - `TaskResponse updateTask(UUID taskId, UpdateTaskRequest request)` -- Updates task fields
  - `String uploadTaskPrompt(UUID jobId, int taskNumber, String promptMd)` -- Uploads task prompt to S3

### 9.16 HealthMonitorService
- **File:** `HealthMonitorService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** HealthScheduleRepository, HealthSnapshotRepository, ProjectRepository, TeamMemberRepository, UserRepository, QaJobRepository, ObjectMapper
- **Public methods:**
  - `HealthScheduleResponse createSchedule(CreateHealthScheduleRequest request)` -- Creates health monitoring schedule
  - `List<HealthScheduleResponse> getSchedulesForProject(UUID projectId)` -- All schedules for a project
  - `List<HealthScheduleResponse> getActiveSchedules()` -- All active schedules (system-wide)
  - `HealthScheduleResponse updateSchedule(UUID scheduleId, boolean isActive)` -- Toggles schedule active status
  - `void deleteSchedule(UUID scheduleId)` -- Deletes schedule
  - `void markScheduleRun(UUID scheduleId)` -- Records schedule execution time
  - `HealthSnapshotResponse createSnapshot(CreateHealthSnapshotRequest request)` -- Creates health snapshot
  - `PageResponse<HealthSnapshotResponse> getSnapshots(UUID projectId, Pageable pageable)` -- Paginated snapshots for project
  - `HealthSnapshotResponse getLatestSnapshot(UUID projectId)` -- Most recent snapshot for project
  - `List<HealthSnapshotResponse> getHealthTrend(UUID projectId, int limit)` -- Recent snapshots for trend analysis

### 9.17 GitHubConnectionService
- **File:** `GitHubConnectionService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** GitHubConnectionRepository, TeamMemberRepository, EncryptionService, TeamRepository, UserRepository
- **Public methods:**
  - `GitHubConnectionResponse createConnection(UUID teamId, CreateGitHubConnectionRequest request)` -- Creates connection with encrypted credentials
  - `List<GitHubConnectionResponse> getConnections(UUID teamId)` -- Active connections for team
  - `GitHubConnectionResponse getConnection(UUID connectionId)` -- Retrieves connection by ID
  - `void deleteConnection(UUID connectionId)` -- Soft-deletes connection (sets isActive=false)
  - `String getDecryptedCredentials(UUID connectionId)` -- Decrypts and returns credentials (internal use)

### 9.18 JiraConnectionService
- **File:** `JiraConnectionService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** JiraConnectionRepository, TeamMemberRepository, EncryptionService, TeamRepository, UserRepository
- **Public methods:**
  - `JiraConnectionResponse createConnection(UUID teamId, CreateJiraConnectionRequest request)` -- Creates connection with encrypted API token
  - `List<JiraConnectionResponse> getConnections(UUID teamId)` -- Active connections for team
  - `JiraConnectionResponse getConnection(UUID connectionId)` -- Retrieves connection by ID
  - `void deleteConnection(UUID connectionId)` -- Soft-deletes connection (sets isActive=false)
  - `String getDecryptedApiToken(UUID connectionId)` -- Decrypts and returns API token (internal use)
  - `JiraConnectionDetails getConnectionDetails(UUID connectionId)` -- Returns instance URL, email, and decrypted token

### 9.19 NotificationService
- **File:** `NotificationService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** NotificationPreferenceRepository (as preferenceRepository), UserRepository
- **Public methods:**
  - `List<NotificationPreferenceResponse> getPreferences(UUID userId)` -- Retrieves all notification preferences for user
  - `NotificationPreferenceResponse updatePreference(UUID userId, UpdateNotificationPreferenceRequest request)` -- Creates or updates a single preference
  - `List<NotificationPreferenceResponse> updatePreferences(UUID userId, List<UpdateNotificationPreferenceRequest> requests)` -- Batch updates preferences
  - `boolean shouldNotify(UUID userId, String eventType, String channel)` -- Checks if user should receive notification

### 9.20 AuditLogService
- **File:** `AuditLogService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** AuditLogRepository, UserRepository, TeamRepository, TeamMemberRepository
- **Public methods:**
  - `@Async void log(UUID userId, UUID teamId, String action, String entityType, UUID entityId, String details)` -- Creates audit log entry asynchronously
  - `Page<AuditLogResponse> getTeamAuditLog(UUID teamId, Pageable pageable)` -- Paginated audit log for team
  - `Page<AuditLogResponse> getUserAuditLog(UUID userId, Pageable pageable)` -- Paginated audit log for user

### 9.21 MetricsService
- **File:** `MetricsService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional(readOnly=true)
- **Dependencies:** ProjectRepository, QaJobRepository, FindingRepository, TechDebtItemRepository, DependencyVulnerabilityRepository (as vulnerabilityRepository), DependencyScanRepository, HealthSnapshotRepository, TeamMemberRepository
- **Public methods:**
  - `ProjectMetricsResponse getProjectMetrics(UUID projectId)` -- Aggregated metrics for a project
  - `TeamMetricsResponse getTeamMetrics(UUID teamId)` -- Aggregated metrics for a team
  - `List<HealthSnapshotResponse> getHealthTrend(UUID projectId, int days)` -- Health snapshots for trend visualization

### 9.22 S3StorageService
- **File:** `S3StorageService.java`
- **Annotations:** @Service
- **Dependencies:** @Value-injected fields: s3Enabled (boolean), bucket (String), localStoragePath (String); S3Client (optional, nullable)
- **Public methods:**
  - `String upload(String key, byte[] data, String contentType)` -- Uploads to S3 or local filesystem
  - `byte[] download(String key)` -- Downloads from S3 or local filesystem
  - `void delete(String key)` -- Deletes from S3 or local filesystem
  - `String generatePresignedUrl(String key, Duration expiry)` -- Generates presigned URL (S3 only, returns null for local)

### 9.23 EncryptionService
- **File:** `EncryptionService.java`
- **Annotations:** @Service
- **Dependencies:** SecretKey (derived from config property via @PostConstruct)
- **Public methods:**
  - `String encrypt(String plaintext)` -- Encrypts with AES-256-GCM, returns Base64-encoded ciphertext
  - `String decrypt(String encryptedBase64)` -- Decrypts Base64-encoded AES-256-GCM ciphertext

### 9.24 TokenBlacklistService
- **File:** `TokenBlacklistService.java`
- **Annotations:** @Service
- **Dependencies:** None (uses internal ConcurrentHashMap.KeySetView)
- **Public methods:**
  - `void blacklist(String jti, Instant expiry)` -- Adds token JTI to in-memory blacklist
  - `boolean isBlacklisted(String jti)` -- Checks if token JTI is blacklisted

### 9.25 ReportStorageService
- **File:** `ReportStorageService.java`
- **Annotations:** @Service, @RequiredArgsConstructor
- **Dependencies:** S3StorageService, AgentRunRepository
- **Public methods:**
  - `String uploadReport(UUID jobId, AgentType agentType, String markdownContent)` -- Uploads agent report markdown to S3
  - `String uploadSummaryReport(UUID jobId, String markdownContent)` -- Uploads job summary report to S3
  - `String downloadReport(String s3Key)` -- Downloads report content as string
  - `void deleteReportsForJob(UUID jobId)` -- Deletes all reports for a job from S3
  - `String uploadSpecification(UUID jobId, String fileName, byte[] fileData, String contentType)` -- Uploads specification file to S3
  - `byte[] downloadSpecification(String s3Key)` -- Downloads specification file as bytes

### 9.26 MfaService
- **File:** `MfaService.java`
- **Annotations:** @Service, @RequiredArgsConstructor, @Transactional
- **Dependencies:** UserRepository, PasswordEncoder, JwtTokenProvider, EncryptionService, TeamMemberRepository, ObjectMapper, MfaEmailCodeRepository, EmailService
- **Public methods:**
  - `MfaSetupResponse setupMfa(MfaSetupRequest request)` -- Generates TOTP secret and QR code URI for MFA setup
  - `MfaStatusResponse verifyAndEnableMfa(MfaVerifyRequest request)` -- Verifies TOTP code and enables MFA on account
  - `MfaRecoveryResponse setupEmailMfa(MfaEmailSetupRequest request)` -- Initiates email-based MFA setup, sends verification code
  - `MfaStatusResponse verifyEmailSetupAndEnable(MfaVerifyRequest request)` -- Verifies email code and enables email MFA
  - `AuthResponse verifyMfaLogin(MfaLoginRequest request)` -- Verifies MFA code during login, issues JWT tokens
  - `void sendLoginMfaCode(MfaResendRequest request)` -- Resends email MFA code during login
  - `MfaStatusResponse disableMfa(MfaSetupRequest request)` -- Disables MFA on account (requires password)
  - `MfaRecoveryResponse regenerateRecoveryCodes(MfaSetupRequest request)` -- Generates new recovery codes (requires password)
  - `MfaStatusResponse getMfaStatus()` -- Returns current user's MFA status
  - `void adminResetMfa(UUID targetUserId)` -- Admin-only: resets MFA on a target user account
  - `@Scheduled void cleanupExpiredCodes()` -- Scheduled task to delete expired email MFA codes
  - `String maskEmail(String email)` -- Masks email address for display (e.g., "a***@domain.com")

### Registry Module


```
=== ServiceRegistryService ===
Dependencies: ServiceRegistrationRepository, PortAllocationRepository, ServiceDependencyRepository, SolutionMemberRepository, ApiRouteRegistrationRepository, InfraResourceRepository, EnvironmentConfigRepository, PortAllocationService, RestTemplate
Methods:
  - createService(CreateServiceRequest, UUID currentUserId) → ServiceRegistrationResponse — Registers a new service with slug generation, optional auto-port allocation
  - getService(UUID serviceId) → ServiceRegistrationResponse — Retrieves a single service by ID with derived counts
  - getServiceBySlug(UUID teamId, String slug) → ServiceRegistrationResponse — Retrieves a service by team and slug
  - getServicesForTeam(UUID teamId, ServiceStatus, ServiceType, String search, Pageable) → PageResponse<ServiceRegistrationResponse> — Paginated service listing with optional status/type/name filters
  - updateService(UUID serviceId, UpdateServiceRequest) → ServiceRegistrationResponse — Partial update of service fields
  - updateServiceStatus(UUID serviceId, ServiceStatus) → ServiceRegistrationResponse — Updates a service's lifecycle status
  - deleteService(UUID serviceId) → void — Deletes a service after checking solution memberships and required dependents
  - cloneService(UUID serviceId, CloneServiceRequest, UUID currentUserId) → ServiceRegistrationResponse — Clones a service under a new name/slug with fresh port auto-allocation
  - getServiceIdentity(UUID serviceId, String environment) → ServiceIdentityResponse — Assembles complete service identity (ports, deps, routes, infra, configs)
  - checkHealth(UUID serviceId) → ServiceHealthResponse — Performs live HTTP health check against the service's health URL
  - checkAllHealth(UUID teamId) → List<ServiceHealthResponse> — Parallel live health checks on all active services for a team


=== SolutionService ===
Dependencies: SolutionRepository, SolutionMemberRepository, ServiceRegistrationRepository
Methods:
  - createSolution(CreateSolutionRequest, UUID currentUserId) → SolutionResponse — Creates a new solution with slug generation and team limit enforcement
  - getSolution(UUID solutionId) → SolutionResponse — Retrieves a solution by ID with computed member count
  - getSolutionBySlug(UUID teamId, String slug) → SolutionResponse — Retrieves a solution by team and slug
  - getSolutionDetail(UUID solutionId) → SolutionDetailResponse — Retrieves full solution detail including ordered member list
  - getSolutionsForTeam(UUID teamId, SolutionStatus, SolutionCategory, Pageable) → PageResponse<SolutionResponse> — Paginated solution listing with optional status/category filters
  - updateSolution(UUID solutionId, UpdateSolutionRequest) → SolutionResponse — Partial update of solution fields
  - deleteSolution(UUID solutionId) → void — Deletes a solution (members cascade-deleted)
  - addMember(UUID solutionId, AddSolutionMemberRequest) → SolutionMemberResponse — Adds a service to a solution with role and display order
  - updateMember(UUID solutionId, UUID serviceId, UpdateSolutionMemberRequest) → SolutionMemberResponse — Updates a member's role, display order, or notes
  - removeMember(UUID solutionId, UUID serviceId) → void — Removes a service from a solution
  - reorderMembers(UUID solutionId, List<UUID> orderedServiceIds) → List<SolutionMemberResponse> — Reorders members by assigning sequential display orders
  - getSolutionHealth(UUID solutionId) → SolutionHealthResponse — Aggregates cached health status across all solution members


=== PortAllocationService ===
Dependencies: PortAllocationRepository, PortRangeRepository, ServiceRegistrationRepository
Methods:
  - autoAllocate(UUID serviceId, String environment, PortType, UUID currentUserId) → PortAllocationResponse — Auto-allocates next available port from configured range
  - autoAllocateAll(UUID serviceId, String environment, List<PortType>, UUID currentUserId) → List<PortAllocationResponse> — Auto-allocates ports for multiple port types in one call
  - manualAllocate(AllocatePortRequest, UUID currentUserId) → PortAllocationResponse — Manually allocates a specific port number with conflict checking
  - releasePort(UUID allocationId) → void — Releases (deletes) a port allocation
  - checkAvailability(UUID teamId, int portNumber, String environment) → PortCheckResponse — Checks if a port is available in a team/environment
  - getPortsForService(UUID serviceId, String environment) → List<PortAllocationResponse> — Lists ports for a service, optionally filtered by environment
  - getPortsForTeam(UUID teamId, String environment) → List<PortAllocationResponse> — Lists all port allocations for a team in an environment
  - getPortMap(UUID teamId, String environment) → PortMapResponse — Assembles the structured port map with ranges and allocation counts
  - detectConflicts(UUID teamId) → List<PortConflictResponse> — Detects same-port allocations to multiple services
  - getPortRanges(UUID teamId) → List<PortRangeResponse> — Lists all port ranges for a team
  - updatePortRange(UUID rangeId, UpdatePortRangeRequest) → PortRangeResponse — Updates a port range's start/end values with shrink protection
  - seedDefaultRanges(UUID teamId, String environment) → List<PortRangeResponse> — Seeds default port ranges per PortType from AppConstants


=== DependencyGraphService ===
Dependencies: ServiceDependencyRepository, ServiceRegistrationRepository
Methods:
  - createDependency(CreateDependencyRequest) → ServiceDependencyResponse — Creates a directed dependency edge with cycle detection
  - removeDependency(UUID dependencyId) → void — Removes a dependency edge
  - getDependencyGraph(UUID teamId) → DependencyGraphResponse — Builds the complete dependency graph (nodes + edges) for visualization
  - getImpactAnalysis(UUID serviceId) → ImpactAnalysisResponse — BFS reverse-dependency impact analysis from a source service
  - getStartupOrder(UUID teamId) → List<DependencyNodeResponse> — Computes topological startup order via Kahn's algorithm
  - detectCycles(UUID teamId) → List<UUID> — Detects cycles in the dependency graph via DFS three-color marking


=== ApiRouteService ===
Dependencies: ApiRouteRegistrationRepository, ServiceRegistrationRepository
Methods:
  - createRoute(CreateRouteRequest, UUID currentUserId) → ApiRouteResponse — Registers an API route prefix with prefix overlap detection
  - deleteRoute(UUID routeId) → void — Deletes an API route registration
  - getRoutesForService(UUID serviceId) → List<ApiRouteResponse> — Lists all routes for a service
  - getRoutesForGateway(UUID gatewayServiceId, String environment) → List<ApiRouteResponse> — Lists all routes behind a gateway in an environment
  - checkRouteAvailability(UUID gatewayServiceId, String environment, String routePrefix) → RouteCheckResponse — Checks if a route prefix is available with conflict details


=== ConfigEngineService ===
Dependencies: ServiceRegistrationRepository, PortAllocationRepository, ServiceDependencyRepository, EnvironmentConfigRepository, ConfigTemplateRepository, ApiRouteRegistrationRepository, InfraResourceRepository, SolutionRepository, SolutionMemberRepository, DependencyGraphService
Methods:
  - generateDockerCompose(UUID serviceId, String environment) → ConfigTemplateResponse — Generates a docker-compose.yml fragment for a single service
  - generateApplicationYml(UUID serviceId, String environment) → ConfigTemplateResponse — Generates a Spring Boot application.yml fragment for a service
  - generateClaudeCodeHeader(UUID serviceId, String environment) → ConfigTemplateResponse — Generates a Claude Code AI context header for a service
  - generateAllForService(UUID serviceId, String environment) → List<ConfigTemplateResponse> — Generates all three config types (Docker Compose, application.yml, Claude Code Header) in one call
  - generateSolutionDockerCompose(UUID solutionId, String environment) → ConfigTemplateResponse — Generates a complete docker-compose.yml for an entire solution with topological ordering
  - getTemplate(UUID serviceId, ConfigTemplateType, String environment) → ConfigTemplateResponse — Retrieves a previously generated template
  - getTemplatesForService(UUID serviceId) → List<ConfigTemplateResponse> — Retrieves all templates for a service
  - deleteTemplate(UUID templateId) → void — Deletes a config template


=== InfraResourceService ===
Dependencies: InfraResourceRepository, ServiceRegistrationRepository
Methods:
  - createResource(CreateInfraResourceRequest, UUID currentUserId) → InfraResourceResponse — Registers an infrastructure resource with duplicate detection
  - updateResource(UUID resourceId, UpdateInfraResourceRequest) → InfraResourceResponse — Partial update of resource fields including service reassignment
  - deleteResource(UUID resourceId) → void — Deletes an infrastructure resource
  - getResourcesForTeam(UUID teamId, InfraResourceType, String environment, Pageable) → PageResponse<InfraResourceResponse> — Paginated resource listing with optional type/environment filters
  - getResourcesForService(UUID serviceId) → List<InfraResourceResponse> — Lists all resources owned by a service
  - findOrphanedResources(UUID teamId) → List<InfraResourceResponse> — Finds resources with no owning service
  - reassignResource(UUID resourceId, UUID newServiceId) → InfraResourceResponse — Reassigns a resource to a different service with team validation
  - orphanResource(UUID resourceId) → InfraResourceResponse — Removes service ownership from a resource


=== TopologyService ===
Dependencies: ServiceRegistrationRepository, ServiceDependencyRepository, SolutionRepository, SolutionMemberRepository, PortAllocationRepository
Methods:
  - getTopology(UUID teamId) → TopologyResponse — Builds a complete ecosystem topology map (nodes, edges, solution groups, layers, stats)
  - getTopologyForSolution(UUID solutionId) → TopologyResponse — Builds a topology view scoped to a solution's member services
  - getServiceNeighborhood(UUID serviceId, int depth) → TopologyResponse — Builds a BFS neighborhood topology view from a center service with capped depth
  - getEcosystemStats(UUID teamId) → TopologyStatsResponse — Computes quick aggregate statistics without building the full topology graph


=== HealthCheckService ===
Dependencies: ServiceRegistrationRepository, SolutionMemberRepository, SolutionRepository, ServiceRegistryService
Methods:
  - getTeamHealthSummary(UUID teamId) → TeamHealthSummaryResponse — Aggregates cached health summary for all services in a team
  - checkTeamHealth(UUID teamId) → TeamHealthSummaryResponse — Performs live health checks then returns fresh aggregated summary
  - getUnhealthyServices(UUID teamId) → List<ServiceHealthResponse> — Returns only DOWN or DEGRADED active services from cached data
  - getServicesNeverChecked(UUID teamId) → List<ServiceHealthResponse> — Returns services with health URL configured but never checked
  - checkSolutionHealth(UUID solutionId) → SolutionHealthResponse — Parallel live health checks on all solution members with aggregation
  - getServiceHealthHistory(UUID serviceId) → ServiceHealthResponse — Returns cached health status for a single service without live check


=== WorkstationProfileService ===
Dependencies: WorkstationProfileRepository, ServiceRegistrationRepository, SolutionMemberRepository, SolutionRepository, DependencyGraphService
Methods:
  - createProfile(CreateWorkstationProfileRequest, UUID currentUserId) → WorkstationProfileResponse — Creates a profile with name uniqueness, service validation, and computed startup order
  - getProfile(UUID profileId) → WorkstationProfileResponse — Retrieves a profile with enriched service details and startup positions
  - getProfilesForTeam(UUID teamId) → List<WorkstationProfileResponse> — Lists all profiles for a team (lightweight, no service details)
  - getDefaultProfile(UUID teamId) → WorkstationProfileResponse — Retrieves the team's default profile with enriched details
  - updateProfile(UUID profileId, UpdateWorkstationProfileRequest) → WorkstationProfileResponse — Partial update with name uniqueness validation and startup order recomputation
  - deleteProfile(UUID profileId) → void — Deletes a workstation profile
  - setDefault(UUID profileId) → WorkstationProfileResponse — Sets a profile as the team default, clearing any existing default
  - createFromSolution(UUID solutionId, UUID teamId, UUID currentUserId) → WorkstationProfileResponse — Quick-creates a profile from a solution's member services
  - refreshStartupOrder(UUID profileId) → WorkstationProfileResponse — Recomputes startup order from the current dependency graph
```

---

All files were read directly from disk. The documentation above covers all 11 entities, 11 enums, 11 repositories, and 10 services in the Registry module at `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/registry/`.

### Logger Module


```
=== LogIngestionService ===
Dependencies: LogEntryRepository, LogSourceRepository, LogEntryMapper, LogParsingService, ApplicationEventPublisher
Methods:
  - ingest(IngestLogEntryRequest, UUID teamId) -> LogEntryResponse — Ingests a single structured log entry via HTTP push
  - ingestBatch(List<IngestLogEntryRequest>, UUID teamId) -> int — Ingests a batch of log entries, returns success count
  - ingestRaw(String rawLog, String defaultServiceName, UUID teamId) -> void — Parses a raw log string and delegates to ingest()
  - resolveOrCreateSource(String serviceName, UUID teamId) -> LogSource — Resolves or auto-creates a LogSource for a service name
  - validateLevel(String level) -> LogLevel — Validates and converts a log level string to LogLevel enum

=== LogSourceService ===
Dependencies: LogSourceRepository, LogSourceMapper
Methods:
  - createSource(CreateLogSourceRequest, UUID teamId) -> LogSourceResponse — Registers a new log source for a team
  - getSourcesByTeam(UUID teamId) -> List<LogSourceResponse> — Returns all log sources for a team
  - getSourcesByTeamPaged(UUID teamId, int page, int size) -> PageResponse<LogSourceResponse> — Returns paginated log sources
  - getSource(UUID sourceId) -> LogSourceResponse — Returns a single log source by ID
  - updateSource(UUID sourceId, UpdateLogSourceRequest) -> LogSourceResponse — Updates an existing log source
  - deleteSource(UUID sourceId) -> void — Deletes a log source

=== LogQueryService ===
Dependencies: LogEntryRepository, SavedQueryRepository, QueryHistoryRepository, LogEntryMapper, SavedQueryMapper, QueryHistoryMapper, ObjectMapper, EntityManager, LogQueryDslParser
Methods:
  - getLogEntry(UUID logEntryId) -> LogEntryResponse — Retrieves a single log entry by ID
  - query(LogQueryRequest, UUID teamId, UUID userId) -> PageResponse<LogEntryResponse> — Executes a structured log query with JPA Criteria
  - search(String searchTerm, UUID teamId, Instant startTime, Instant endTime, int page, int size) -> PageResponse<LogEntryResponse> — Full-text search across message, loggerName, exceptionClass, exceptionMessage, customFields
  - executeDsl(String dslQuery, UUID teamId, UUID userId, int page, int size) -> PageResponse<LogEntryResponse> — Parses and executes a SQL-like DSL query
  - saveQuery(CreateSavedQueryRequest, UUID teamId, UUID userId) -> SavedQueryResponse — Saves a query for later reuse
  - getSavedQueries(UUID teamId, UUID userId) -> List<SavedQueryResponse> — Returns user's own + shared team queries
  - getSavedQuery(UUID queryId) -> SavedQueryResponse — Returns a single saved query by ID
  - updateSavedQuery(UUID queryId, UpdateSavedQueryRequest, UUID userId) -> SavedQueryResponse — Updates a saved query (owner-only)
  - deleteSavedQuery(UUID queryId, UUID userId) -> void — Deletes a saved query (owner-only)
  - executeSavedQuery(UUID queryId, UUID teamId, UUID userId, int page, int size) -> PageResponse<LogEntryResponse> — Executes a saved query and updates stats
  - getQueryHistory(UUID userId, int page, int size) -> PageResponse<QueryHistoryResponse> — Returns paginated query history for a user
  - recordQueryHistory(String queryJson, String queryDsl, long resultCount, long executionTimeMs, UUID teamId, UUID userId) -> void — Records a query execution in history

=== LogParsingService ===
Dependencies: ObjectMapper
Methods:
  - parse(String rawLog, String defaultServiceName) -> IngestLogEntryRequest — Parses raw log string into structured request (tries JSON, key-value, Spring Boot, syslog, then plain text)
  - tryParseJson(String rawLog) -> IngestLogEntryRequest — Attempts JSON parsing of raw log string
  - tryParseKeyValue(String rawLog) -> IngestLogEntryRequest — Attempts key-value pair parsing
  - tryParseSpringBoot(String rawLog) -> IngestLogEntryRequest — Attempts Spring Boot default log format parsing
  - tryParseSyslog(String rawLog) -> IngestLogEntryRequest — Attempts syslog (RFC 3164) format parsing
  - fallbackPlainText(String rawLog, String defaultServiceName) -> IngestLogEntryRequest — Wraps raw text as a plain INFO log entry
  - parseTimestamp(String timestampStr) -> Instant — Parses timestamps in ISO-8601, epoch millis, epoch seconds, and other common formats

=== LogQueryDslParser ===
Dependencies: None (stateless @Component)
Methods:
  - parse(String dsl) -> List<DslCondition> — Parses a DSL query string into structured conditions
  - parseCondition(String conditionStr, String conjunction) -> DslCondition — Parses a single DSL condition
  - validateField(String field) -> void — Validates that a field name is recognized
  - validateOperator(String operator) -> void — Validates that an operator is recognized
  - mapFieldToEntityField(String dslField) -> String — Maps a DSL field name to the JPA entity field name
  - stripQuotes(String value) -> String — Strips surrounding quotes and parens from value strings

=== AlertService ===
Dependencies: AlertRuleRepository, AlertHistoryRepository, LogTrapRepository, AlertChannelRepository, AlertRuleMapper, AlertHistoryMapper, AlertChannelService
Methods:
  - createRule(CreateAlertRuleRequest, UUID teamId) -> AlertRuleResponse — Creates a new alert rule connecting a trap to a channel
  - getRulesByTeam(UUID teamId) -> List<AlertRuleResponse> — Returns all rules for a team
  - getRulesByTeamPaged(UUID teamId, int page, int size) -> PageResponse<AlertRuleResponse> — Returns paginated rules
  - getRulesByTrap(UUID trapId) -> List<AlertRuleResponse> — Returns rules for a specific trap
  - getRule(UUID ruleId) -> AlertRuleResponse — Returns a single rule by ID
  - updateRule(UUID ruleId, UpdateAlertRuleRequest) -> AlertRuleResponse — Updates an existing alert rule
  - deleteRule(UUID ruleId) -> void — Deletes an alert rule
  - fireAlerts(UUID trapId, String triggerMessage) -> void — Fires alerts for a triggered trap with throttle checking
  - isThrottled(UUID ruleId, int throttleMinutes) -> boolean — Checks if a rule is within its throttle window
  - acknowledgeAlert(UUID alertId, UUID userId) -> AlertHistoryResponse — Acknowledges a fired alert
  - resolveAlert(UUID alertId, UUID userId) -> AlertHistoryResponse — Resolves an alert
  - updateAlertStatus(UUID alertId, UpdateAlertStatusRequest, UUID userId) -> AlertHistoryResponse — Generic status transition handler
  - getAlertHistory(UUID teamId, int page, int size) -> PageResponse<AlertHistoryResponse> — Returns paginated alert history
  - getAlertHistoryByStatus(UUID teamId, AlertStatus, int page, int size) -> PageResponse<AlertHistoryResponse> — History filtered by status
  - getAlertHistoryBySeverity(UUID teamId, AlertSeverity, int page, int size) -> PageResponse<AlertHistoryResponse> — History filtered by severity
  - getAlertHistoryByRule(UUID ruleId, int page, int size) -> PageResponse<AlertHistoryResponse> — History for a specific rule
  - getActiveAlertCounts(UUID teamId) -> Map<String, Long> — Returns count of non-resolved alerts by severity

=== AlertChannelService ===
Dependencies: AlertChannelRepository, AlertChannelMapper, RestTemplate, ObjectMapper
Methods:
  - createChannel(CreateAlertChannelRequest, UUID teamId, UUID userId) -> AlertChannelResponse — Creates a new notification channel
  - getChannelsByTeam(UUID teamId) -> List<AlertChannelResponse> — Returns all channels for a team
  - getChannelsByTeamPaged(UUID teamId, int page, int size) -> PageResponse<AlertChannelResponse> — Returns paginated channels
  - getChannel(UUID channelId) -> AlertChannelResponse — Returns a single channel by ID
  - updateChannel(UUID channelId, UpdateAlertChannelRequest) -> AlertChannelResponse — Updates an existing channel
  - deleteChannel(UUID channelId) -> void — Deletes a channel
  - deliverNotification(AlertChannel, String alertMessage, AlertSeverity, String trapName) -> void — @Async delivers alert to channel (email/webhook/teams/slack)
  - deliverEmail(AlertChannel, String, AlertSeverity, String) -> void — Delivers alert via email (logs in dev)
  - deliverWebhook(AlertChannel, String, AlertSeverity, String) -> void — Delivers alert via generic HTTP webhook POST
  - deliverTeams(AlertChannel, String, AlertSeverity, String) -> void — Delivers alert via Microsoft Teams MessageCard webhook
  - deliverSlack(AlertChannel, String, AlertSeverity, String) -> void — Delivers alert via Slack incoming webhook
  - validateWebhookUrl(String url) -> void — SSRF-safe webhook URL validation (HTTPS required, internal IPs blocked)
  - validateConfiguration(AlertChannelType, String configuration) -> void — Validates channel config JSON based on type

=== LogTrapService ===
Dependencies: LogTrapRepository, TrapConditionRepository, LogEntryRepository, LogTrapMapper, TrapEvaluationEngine
Methods:
  - createTrap(CreateLogTrapRequest, UUID teamId, UUID userId) -> LogTrapResponse — Creates a new trap with conditions
  - getTrapsByTeam(UUID teamId) -> List<LogTrapResponse> — Returns all traps for a team
  - getTrap(UUID trapId) -> LogTrapResponse — Returns a single trap with conditions
  - getTrapsByTeamPaged(UUID teamId, int page, int size) -> PageResponse<LogTrapResponse> — Returns paginated traps
  - updateTrap(UUID trapId, UpdateLogTrapRequest, UUID userId) -> LogTrapResponse — Updates a trap (full condition replacement)
  - deleteTrap(UUID trapId) -> void — Deletes a trap and all conditions
  - toggleTrap(UUID trapId) -> LogTrapResponse — Toggles trap active/inactive
  - evaluateEntry(LogEntry entry) -> List<UUID> — Evaluates a log entry against all active team traps, returns fired trap IDs
  - testTrap(UUID trapId, int hoursBack) -> TrapTestResult — Tests a saved trap against historical logs (diagnostic only)
  - testTrapDefinition(CreateLogTrapRequest, UUID teamId, int hoursBack) -> TrapTestResult — Tests a trap definition before saving
  - validateRegexPattern(String pattern) -> void — Validates a regex pattern

=== TrapEvaluationEngine ===
Dependencies: LogEntryRepository
Methods:
  - evaluatePatternConditions(LogEntry entry, List<TrapCondition> conditions) -> boolean — Evaluates REGEX/KEYWORD conditions (AND logic, all must match)
  - evaluateRegex(LogEntry entry, TrapCondition condition) -> boolean — Evaluates a single regex condition against a log field
  - evaluateKeyword(LogEntry entry, TrapCondition condition) -> boolean — Evaluates a keyword condition (case-insensitive substring match)
  - evaluateFrequencyThreshold(TrapCondition condition, UUID teamId) -> boolean — Checks if log count exceeds threshold in time window
  - evaluateAbsence(TrapCondition condition, UUID teamId) -> boolean — Checks if no matching logs exist in time window
  - extractFieldValue(LogEntry entry, String fieldName) -> String — Extracts a field value from a log entry by name (snake_case/camelCase)
  - isLevelAtOrAbove(LogLevel entryLevel, LogLevel requiredLevel) -> boolean — Compares log levels by ordinal

=== TraceService ===
Dependencies: TraceSpanRepository, LogEntryRepository, TraceSpanMapper, TraceAnalysisService
Methods:
  - createSpan(CreateTraceSpanRequest, UUID teamId) -> TraceSpanResponse — Records a new trace span
  - createSpanBatch(List<CreateTraceSpanRequest>, UUID teamId) -> List<TraceSpanResponse> — Records multiple spans in a batch
  - getSpan(UUID spanId) -> TraceSpanResponse — Returns a single span by ID
  - getTraceFlow(String correlationId) -> TraceFlowResponse — Assembles a complete trace flow by correlationId
  - getTraceFlowByTraceId(String traceId) -> TraceFlowResponse — Assembles a trace flow by traceId
  - getWaterfall(String correlationId) -> TraceWaterfallResponse — Builds waterfall visualization with related log entries
  - getRootCauseAnalysis(String correlationId) -> Optional<RootCauseAnalysisResponse> — Identifies earliest error and propagation chain
  - listRecentTraces(UUID teamId, int page, int size) -> PageResponse<TraceListResponse> — Lists recent trace summaries
  - listTracesByService(UUID teamId, String serviceName, int page, int size) -> PageResponse<TraceListResponse> — Lists traces for a service
  - listErrorTraces(UUID teamId, int limit) -> List<TraceListResponse> — Lists traces containing errors
  - getRelatedLogEntries(String correlationId) -> List<UUID> — Returns log entry IDs associated with a trace
  - purgeOldSpans(Instant cutoff) -> void — Deletes spans older than cutoff

=== TraceAnalysisService ===
Dependencies: TraceSpanMapper
Methods:
  - buildWaterfall(List<TraceSpan>, Map<String, List<UUID>> relatedLogsBySpan) -> TraceWaterfallResponse — Builds waterfall visualization with depth levels and timing offsets
  - analyzeRootCause(List<TraceSpan>, List<UUID> relatedLogIds) -> RootCauseAnalysisResponse — Identifies earliest error span and error propagation chain
  - calculateDepths(List<TraceSpan>) -> Map<String, Integer> — Calculates BFS depth for parent-child span tree
  - findRootSpan(List<TraceSpan>) -> TraceSpan — Finds the root span (no parent, earliest start time)
  - buildTraceSummary(List<TraceSpan>) -> TraceListResponse — Builds a trace summary for list views

=== RetentionService ===
Dependencies: RetentionPolicyRepository, RetentionPolicyMapper, LogEntryRepository, MetricSeriesRepository, TraceSpanRepository
Methods:
  - createPolicy(CreateRetentionPolicyRequest, UUID teamId, UUID userId) -> RetentionPolicyResponse — Creates a new retention policy
  - updatePolicy(UUID policyId, UpdateRetentionPolicyRequest, UUID teamId) -> RetentionPolicyResponse — Updates a policy with team ownership validation
  - getPolicy(UUID policyId, UUID teamId) -> RetentionPolicyResponse — Returns a policy scoped to a team
  - getPoliciesByTeam(UUID teamId) -> List<RetentionPolicyResponse> — Lists all policies for a team
  - deletePolicy(UUID policyId, UUID teamId) -> void — Deletes a policy with team ownership validation
  - togglePolicyActive(UUID policyId, UUID teamId, boolean active) -> RetentionPolicyResponse — Activates/deactivates a policy
  - getStorageUsage() -> StorageUsageResponse — Computes storage stats (log/metric/span counts, by-service, by-level, oldest/newest timestamps)

=== RetentionExecutor ===
Dependencies: RetentionPolicyRepository, LogEntryRepository, MetricSeriesRepository, TraceSpanRepository
Methods:
  - executeAllActivePolicies() -> void — @Scheduled(cron "0 0 2 * * ?") daily 2:00 AM execution of all active policies
  - executePolicy(RetentionPolicy policy) -> void — Executes a single policy (deletes matching log entries older than retention period)
  - manualExecute(UUID policyId, UUID teamId) -> void — Manually triggers a specific policy with team ownership check
  - globalPurge(int retentionDays) -> void — Purges all data older than N days across logs, metrics, and spans
  - getLevelsAtOrBelow(LogLevel level) -> List<LogLevel> — Returns all log levels at or below the given level

=== DashboardService ===
Dependencies: DashboardRepository, DashboardWidgetRepository, DashboardMapper
Methods:
  - createDashboard(CreateDashboardRequest, UUID teamId, UUID userId) -> DashboardResponse — Creates a new dashboard
  - getDashboardsByTeam(UUID teamId) -> List<DashboardResponse> — Returns all dashboards for a team
  - getDashboardsByTeamPaged(UUID teamId, int page, int size) -> PageResponse<DashboardResponse> — Returns paginated dashboards
  - getSharedDashboards(UUID teamId) -> List<DashboardResponse> — Returns shared dashboards for a team
  - getDashboardsByUser(UUID userId) -> List<DashboardResponse> — Returns dashboards created by a user
  - getDashboard(UUID dashboardId) -> DashboardResponse — Returns a single dashboard with widgets
  - updateDashboard(UUID dashboardId, UpdateDashboardRequest) -> DashboardResponse — Updates dashboard metadata
  - deleteDashboard(UUID dashboardId) -> void — Deletes a dashboard and all widgets (cascade)
  - addWidget(UUID dashboardId, CreateDashboardWidgetRequest) -> DashboardWidgetResponse — Adds a widget to a dashboard
  - updateWidget(UUID dashboardId, UUID widgetId, UpdateDashboardWidgetRequest) -> DashboardWidgetResponse — Updates a widget
  - removeWidget(UUID dashboardId, UUID widgetId) -> void — Removes a widget and re-numbers sort orders
  - reorderWidgets(UUID dashboardId, List<UUID> widgetIds) -> DashboardResponse — Reorders widgets by sorted widget ID list
  - updateLayout(UUID dashboardId, List<WidgetPositionUpdate>) -> DashboardResponse — Updates grid positions for all widgets
  - markAsTemplate(UUID dashboardId) -> DashboardResponse — Marks a dashboard as a reusable template
  - unmarkAsTemplate(UUID dashboardId) -> DashboardResponse — Removes template flag
  - getTemplates(UUID teamId) -> List<DashboardResponse> — Returns all template dashboards for a team
  - createFromTemplate(UUID templateId, String name, UUID teamId, UUID userId) -> DashboardResponse — Deep-clones a template into a new dashboard
  - duplicateDashboard(UUID dashboardId, String newName, UUID teamId, UUID userId) -> DashboardResponse — Deep-clones any dashboard

=== MetricsService ===
Bean Name: "loggerMetricsService"
Dependencies: MetricRepository, MetricSeriesRepository, MetricMapper, MetricAggregationService
Methods:
  - registerMetric(RegisterMetricRequest, UUID teamId) -> MetricResponse — Registers a metric (idempotent: returns existing if name+service+team match)
  - getMetricsByTeam(UUID teamId) -> List<MetricResponse> — Returns all metrics for a team
  - getMetricsByTeamPaged(UUID teamId, int page, int size) -> PageResponse<MetricResponse> — Returns paginated metrics
  - getMetricsByService(UUID teamId, String serviceName) -> List<MetricResponse> — Returns metrics for a specific service
  - getServiceMetricsSummary(UUID teamId, String serviceName) -> ServiceMetricsSummaryResponse — Returns metric summary grouped by type
  - getMetric(UUID metricId) -> MetricResponse — Returns a single metric by ID
  - updateMetric(UUID metricId, UpdateMetricRequest) -> MetricResponse — Updates metric description/unit/tags (name and type immutable)
  - deleteMetric(UUID metricId) -> void — Deletes a metric and all its time-series data
  - pushMetricData(PushMetricDataRequest, UUID teamId) -> int — Pushes metric data points with team ownership validation
  - pushSingleValue(String metricName, String metricType, String serviceName, double value, UUID teamId) -> void — Auto-registers metric and pushes a single data point
  - getTimeSeries(UUID metricId, Instant startTime, Instant endTime) -> MetricTimeSeriesResponse — Retrieves raw time-series data points
  - getTimeSeriesAggregated(UUID metricId, Instant startTime, Instant endTime, int resolutionSeconds) -> MetricTimeSeriesResponse — Retrieves time-series aggregated by resolution
  - getAggregation(UUID metricId, Instant startTime, Instant endTime) -> MetricAggregationResponse — Returns full stats (sum, avg, min, max, p50, p95, p99, stddev)
  - getLatestValue(UUID metricId) -> Optional<MetricDataPointResponse> — Returns the most recent data point for a metric
  - getLatestValuesByService(UUID teamId, String serviceName) -> Map<String, Double> — Returns latest values for all metrics of a service
  - purgeOldData(Instant cutoff) -> long — Deletes metric series data older than cutoff

=== MetricAggregationService ===
Dependencies: None (stateless)
Methods:
  - aggregate(List<Double> values) -> AggregationResult — Calculates sum, avg, min, max, p50, p95, p99, stddev
  - calculatePercentile(List<Double> sortedValues, double percentile) -> double — Nearest-rank percentile calculation
  - calculateStdDev(List<Double> values, double mean) -> double — Population standard deviation
  - aggregateByResolution(List<MetricSeries>, Instant start, Instant end, int resolutionSeconds) -> List<DataPoint> — Time-bucketed aggregation (average per bucket)

=== AnomalyDetectionService ===
Dependencies: AnomalyBaselineRepository, LogEntryRepository, MetricSeriesRepository, MetricRepository, AnomalyBaselineMapper, AnomalyBaselineCalculator
Methods:
  - createOrUpdateBaseline(CreateBaselineRequest, UUID teamId) -> AnomalyBaselineResponse — Creates or recalculates a baseline for a service+metric (requires min 24 hourly data points)
  - getBaselinesByTeam(UUID teamId) -> List<AnomalyBaselineResponse> — Returns all baselines for a team
  - getBaselinesByService(UUID teamId, String serviceName) -> List<AnomalyBaselineResponse> — Returns baselines for a service
  - getBaseline(UUID baselineId) -> AnomalyBaselineResponse — Returns a single baseline by ID
  - updateBaseline(UUID baselineId, UpdateBaselineRequest) -> AnomalyBaselineResponse — Updates baseline config; recalculates if windowHours changes
  - deleteBaseline(UUID baselineId) -> void — Deletes a baseline
  - checkAnomaly(UUID teamId, String serviceName, String metricName) -> AnomalyCheckResponse — Checks current value against baseline using z-score analysis
  - runFullCheck(UUID teamId) -> AnomalyReportResponse — Runs anomaly detection across all active baselines for a team
  - recalculateAllBaselines() -> void — @Scheduled(cron "${codeops.anomaly.recalculation-cron:0 0 3 * * *}") daily recalculation
  - recalculateBaseline(AnomalyBaseline) -> void — Recalculates a single baseline from current data
  - collectHourlyLogVolume(UUID teamId, String serviceName, Instant start, Instant end) -> List<Double> — Collects hourly log counts for baseline computation
  - collectHourlyErrorRate(UUID teamId, String serviceName, Instant start, Instant end) -> List<Double> — Collects hourly error rate percentages
  - getCurrentValue(UUID teamId, String serviceName, String metricName) -> double — Gets the current 1-hour metric value for anomaly checking

=== AnomalyBaselineCalculator ===
Dependencies: None (stateless @Component)
Methods:
  - computeBaseline(List<Double> hourlyValues) -> Optional<BaselineStats> — Computes mean and stddev from hourly data (requires min 24 data points)
  - calculateZScore(double value, double mean, double stddev) -> double — Calculates absolute z-score
  - isAnomaly(double value, double mean, double stddev, double threshold) -> boolean — Determines if z-score exceeds threshold
  - getDirection(double value, double mean) -> String — Returns "ABOVE", "BELOW", or "NORMAL"

=== KafkaLogConsumer ===
Dependencies: LogIngestionService, ObjectMapper
Methods:
  - consume(@Payload String message, @Header String key, @Header String teamIdHeader) -> void — @KafkaListener(topics="codeops-logs") consumes log messages, parses JSON or raw text, delegates to LogIngestionService
```

### Courier Module


**Package:** `com.codeops.courier.service`

---

### 9.1 CollectionService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `CollectionRepository`, `FolderRepository`, `RequestRepository`, `CollectionShareRepository`, `CollectionMapper`
- **Public Methods:**
  - `CollectionResponse createCollection(UUID teamId, UUID userId, CreateCollectionRequest request)` -- Creates a new collection with name uniqueness validation
  - `CollectionResponse getCollection(UUID collectionId, UUID teamId)` -- Gets a collection by ID with folder/request counts
  - `List<CollectionSummaryResponse> getCollections(UUID teamId, UUID userId)` -- Lists team collections plus shared-with-user collections
  - `PageResponse<CollectionSummaryResponse> getCollectionsPaged(UUID teamId, int page, int size)` -- Paginated collection listing
  - `CollectionResponse updateCollection(UUID collectionId, UUID teamId, UpdateCollectionRequest request)` -- Partial update of collection fields
  - `void deleteCollection(UUID collectionId, UUID teamId)` -- Deletes collection and all contents via cascade
  - `CollectionResponse duplicateCollection(UUID collectionId, UUID teamId, UUID userId)` -- Deep copy of collection with all folders, requests, and components
  - `List<CollectionSummaryResponse> searchCollections(UUID teamId, String query)` -- Case-insensitive search by name
- **Package-private Methods:**
  - `Collection findCollectionByIdAndTeam(UUID collectionId, UUID teamId)` -- Finds collection and validates team ownership

### 9.2 FolderService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `FolderRepository`, `CollectionRepository`, `RequestRepository`, `FolderMapper`, `RequestMapper`
- **Public Methods:**
  - `FolderResponse createFolder(UUID teamId, CreateFolderRequest request)` -- Creates folder (root or nested) in a collection
  - `FolderResponse getFolder(UUID folderId, UUID teamId)` -- Gets folder with computed subfolder/request counts
  - `List<FolderTreeResponse> getFolderTree(UUID collectionId, UUID teamId)` -- Recursive folder tree with nested subfolders and request summaries
  - `List<FolderResponse> getSubFolders(UUID parentFolderId, UUID teamId)` -- Direct child folders of a parent
  - `List<FolderResponse> getRootFolders(UUID collectionId, UUID teamId)` -- Root-level folders for a collection
  - `FolderResponse updateFolder(UUID folderId, UUID teamId, UpdateFolderRequest request)` -- Partial update with circular reference prevention
  - `void deleteFolder(UUID folderId, UUID teamId)` -- Deletes folder and all contents via cascade
  - `List<FolderResponse> reorderFolders(UUID teamId, ReorderFolderRequest request)` -- Batch reorder of sibling folders
  - `FolderResponse moveFolder(UUID folderId, UUID teamId, UUID newParentFolderId)` -- Moves folder with circular reference and cross-collection prevention

### 9.3 RequestService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `RequestRepository`, `RequestHeaderRepository`, `RequestParamRepository`, `RequestBodyRepository`, `RequestAuthRepository`, `RequestScriptRepository`, `FolderRepository`, `CollectionRepository`, `RequestMapper`, `RequestHeaderMapper`, `RequestParamMapper`, `RequestBodyMapper`, `RequestAuthMapper`, `RequestScriptMapper`
- **Public Methods:**
  - `RequestResponse createRequest(UUID teamId, CreateRequestRequest request)` -- Creates a new request in a folder
  - `RequestResponse getRequest(UUID requestId, UUID teamId)` -- Gets request with all components (headers, params, body, auth, scripts)
  - `List<RequestSummaryResponse> getRequestsInFolder(UUID folderId, UUID teamId)` -- Lists request summaries in a folder
  - `RequestResponse updateRequest(UUID requestId, UUID teamId, UpdateRequestRequest request)` -- Partial update of request fields
  - `void deleteRequest(UUID requestId, UUID teamId)` -- Deletes request and all components via cascade
  - `RequestResponse duplicateRequest(UUID requestId, UUID teamId, DuplicateRequestRequest dupRequest)` -- Deep copy of request with all components
  - `RequestResponse moveRequest(UUID requestId, UUID teamId, UUID targetFolderId)` -- Moves request to different folder within same collection
  - `List<RequestSummaryResponse> reorderRequests(UUID teamId, ReorderRequestRequest request)` -- Batch reorder of sibling requests
  - `List<RequestHeaderResponse> saveHeaders(UUID requestId, UUID teamId, SaveRequestHeadersRequest request)` -- Batch replace all headers
  - `List<RequestParamResponse> saveParams(UUID requestId, UUID teamId, SaveRequestParamsRequest request)` -- Batch replace all params
  - `RequestBodyResponse saveBody(UUID requestId, UUID teamId, SaveRequestBodyRequest request)` -- Replace body (single record)
  - `RequestAuthResponse saveAuth(UUID requestId, UUID teamId, SaveRequestAuthRequest request)` -- Replace auth (single record)
  - `RequestScriptResponse saveScript(UUID requestId, UUID teamId, SaveRequestScriptRequest request)` -- Upsert script by type
  - `void clearRequestComponents(UUID requestId, UUID teamId)` -- Deletes all components without deleting the request

### 9.4 RequestProxyService
- **Annotation:** `@Service`
- **Dependencies:** `HttpClient` (courierHttpClient), `VariableService`, `AuthResolverService`, `RequestHistoryRepository`, `RequestRepository`, `RequestAuthRepository`, `ObjectMapper`
- **Public Methods:**
  - `ProxyResponse executeRequest(SendRequestProxyRequest proxyRequest, UUID teamId, UUID userId)` -- Core HTTP execution engine with variable resolution, auth handling, redirect following, and history recording
  - `ProxyResponse executeStoredRequest(UUID requestId, UUID teamId, UUID userId, UUID environmentId)` -- Executes a stored request by ID with full component loading
- **Package-private Methods:**
  - `SaveRequestAuthRequest resolveInheritedAuth(Request request)` -- Walks the folder/collection chain to resolve INHERIT_FROM_PARENT auth type

### 9.5 EnvironmentService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `EnvironmentRepository`, `EnvironmentVariableRepository`, `EnvironmentMapper`, `EnvironmentVariableMapper`
- **Public Methods:**
  - `EnvironmentResponse createEnvironment(UUID teamId, UUID userId, CreateEnvironmentRequest request)` -- Creates environment with auto-activation if first in team
  - `EnvironmentResponse getEnvironment(UUID environmentId, UUID teamId)` -- Gets environment with variable count
  - `List<EnvironmentResponse> getEnvironments(UUID teamId)` -- Lists all team environments
  - `EnvironmentResponse getActiveEnvironment(UUID teamId)` -- Gets the currently active environment
  - `EnvironmentResponse setActiveEnvironment(UUID environmentId, UUID teamId)` -- Activates an environment and deactivates the previous
  - `EnvironmentResponse updateEnvironment(UUID environmentId, UUID teamId, UpdateEnvironmentRequest request)` -- Partial update of environment fields
  - `void deleteEnvironment(UUID environmentId, UUID teamId)` -- Deletes environment with auto-activation of next
  - `EnvironmentResponse cloneEnvironment(UUID environmentId, UUID teamId, UUID userId, CloneEnvironmentRequest request)` -- Deep copy with all variables
  - `List<EnvironmentVariableResponse> getEnvironmentVariables(UUID environmentId, UUID teamId)` -- Lists variables with secret masking
  - `List<EnvironmentVariableResponse> saveEnvironmentVariables(UUID environmentId, UUID teamId, SaveEnvironmentVariablesRequest request)` -- Batch replace all variables

### 9.6 VariableService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `GlobalVariableRepository`, `EnvironmentVariableRepository`, `GlobalVariableMapper`
- **Public Methods:**
  - `String resolveVariables(String input, UUID teamId, UUID collectionId, UUID environmentId, Map<String, String> localVars)` -- Resolves {{placeholder}} variables using full scope hierarchy (Global < Collection < Environment < Local)
  - `Map<String, String> buildVariableMap(UUID teamId, UUID collectionId, UUID environmentId, Map<String, String> localVars)` -- Builds the merged variable map for a scope context
  - `String resolveUrl(String url, UUID teamId, UUID collectionId, UUID environmentId, Map<String, String> localVars)` -- Resolves variables in a URL
  - `Map<String, String> resolveHeaders(List<RequestHeaderResponse> headers, UUID teamId, UUID collectionId, UUID environmentId, Map<String, String> localVars)` -- Resolves variables in request headers
  - `String resolveBody(String body, UUID teamId, UUID collectionId, UUID environmentId, Map<String, String> localVars)` -- Resolves variables in a request body
  - `List<GlobalVariableResponse> getGlobalVariables(UUID teamId)` -- Lists global variables with secret masking
  - `GlobalVariableResponse saveGlobalVariable(UUID teamId, SaveGlobalVariableRequest request)` -- Upsert single global variable by key
  - `List<GlobalVariableResponse> batchSaveGlobalVariables(UUID teamId, BatchSaveGlobalVariablesRequest request)` -- Additive batch save of global variables
  - `void deleteGlobalVariable(UUID variableId, UUID teamId)` -- Deletes a global variable
  - `String getSecretValue(UUID variableId, UUID teamId)` -- Returns unmasked secret value (internal use only)

### 9.7 AuthResolverService
- **Annotation:** `@Service`
- **Dependencies:** `VariableService`
- **Public Methods:**
  - `ResolvedAuth resolveAuth(SaveRequestAuthRequest auth, UUID teamId, UUID collectionId, UUID environmentId, Map<String, String> localVars)` -- Resolves auth config into HTTP headers and query params with variable substitution
- **Inner Records:**
  - `record ResolvedAuth(Map<String, String> headers, Map<String, String> queryParams)` -- Resolved auth artifacts

### 9.8 HistoryService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `RequestHistoryRepository`, `RequestHistoryMapper`
- **Public Methods:**
  - `PageResponse<RequestHistoryResponse> getHistory(UUID teamId, int page, int size)` -- Paginated team history (most recent first)
  - `PageResponse<RequestHistoryResponse> getUserHistory(UUID teamId, UUID userId, int page, int size)` -- Paginated user-specific history
  - `RequestHistoryDetailResponse getHistoryDetail(UUID historyId, UUID teamId)` -- Full detail with request/response bodies
  - `List<RequestHistoryResponse> searchHistory(UUID teamId, String urlFragment)` -- Search history by URL fragment
  - `PageResponse<RequestHistoryResponse> getHistoryByMethod(UUID teamId, HttpMethod method, int page, int size)` -- Filter history by HTTP method
  - `void deleteHistoryEntry(UUID historyId, UUID teamId)` -- Deletes a single history entry
  - `void clearTeamHistory(UUID teamId)` -- Clears all history for a team
  - `int clearOldHistory(UUID teamId, int daysToRetain)` -- Retention-based cleanup, returns count of deleted entries
  - `long getHistoryCount(UUID teamId)` -- Total history entry count

### 9.9 CollectionRunnerService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `RunResultRepository`, `RunIterationRepository`, `CollectionRepository`, `FolderRepository`, `RequestRepository`, `RequestScriptRepository`, `RequestProxyService`, `ScriptEngineService`, `VariableService`, `DataFileParser`, `RunResultMapper`, `ObjectMapper`
- **Public Methods:**
  - `RunResultDetailResponse startRun(StartCollectionRunRequest request, UUID teamId, UUID userId)` -- Starts a synchronous collection run with iterations, scripts, and assertions
  - `RunResultResponse getRunResult(UUID runResultId, UUID teamId)` -- Gets run result summary
  - `RunResultDetailResponse getRunResultDetail(UUID runResultId, UUID teamId)` -- Gets run result with all iterations
  - `List<RunResultResponse> getRunResults(UUID collectionId, UUID teamId)` -- Lists run results for a collection
  - `PageResponse<RunResultResponse> getRunResultsPaged(UUID teamId, int page, int size)` -- Paginated run results for a team
  - `RunResultResponse cancelRun(UUID runResultId, UUID teamId, UUID userId)` -- Cancels a running collection run
  - `void deleteRunResult(UUID runResultId, UUID teamId)` -- Deletes a run result and its iterations
- **Package-private Methods:**
  - `List<Request> collectRequestsInOrder(UUID collectionId)` -- Depth-first traversal of folder tree for execution order
  - `Map<String, String> getIterationVars(List<Map<String, String>> dataRows, int iterationIndex)` -- Gets variable map for a specific iteration
  - `void sleepBetweenRequests(int delayMs)` -- Configurable delay between requests

### 9.10 ScriptEngineService
- **Annotation:** `@Service`
- **Dependencies:** None (uses `AppConstants` for timeout configuration)
- **Public Methods:**
  - `ScriptContext executePreRequestScript(String scriptContent, ScriptContext context)` -- Executes pre-request JavaScript in GraalJS sandbox with pm API
  - `ScriptContext executePostResponseScript(String scriptContent, ScriptContext context)` -- Executes post-response JavaScript in GraalJS sandbox with pm.response API
- **Notes:** GraalJS sandbox with `HostAccess.NONE`, no I/O, no processes, no threads. Provides Postman-compatible `pm` object with `pm.variables`, `pm.globals`, `pm.collectionVariables`, `pm.environment`, `pm.request`, `pm.response`, `pm.test()`, `pm.expect()`. Configurable timeout (default from AppConstants).

### 9.11 ScriptContext (POJO -- not a @Service)
- **Annotation:** None (plain Java class, not a Spring bean)
- **Dependencies:** None
- **Constructor:** `ScriptContext(Map<String, String> globalVars, Map<String, String> collectionVars, Map<String, String> envVars, Map<String, String> localVars)`
- **Purpose:** Mutable execution context for script engine. Holds variable scopes, request/response data, assertion results, console output, and request cancellation flag. Created fresh per script execution.
- **Public Methods:**
  - `void addAssertion(String name, boolean passed, String message)` -- Adds a test assertion result
  - `void addConsoleLog(String line)` -- Adds a console output line (capped at max)
  - Getters/setters for: `globalVariables`, `collectionVariables`, `environmentVariables`, `localVariables`, `requestUrl`, `requestMethod`, `requestHeaders`, `requestBody`, `responseStatus`, `responseHeaders`, `responseBody`, `responseTimeMs`, `assertions`, `consoleOutput`, `requestCancelled`
- **Inner Records:**
  - `record AssertionResult(String name, boolean passed, String message)` -- Single test assertion result

### 9.12 CodeGenerationService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `CodeSnippetTemplateRepository`, `RequestService`, `VariableService`
- **Public Methods:**
  - `CodeSnippetResponse generateCode(GenerateCodeRequest request, UUID teamId)` -- Generates code snippet for a stored request in one of 12 languages
  - `List<CodeSnippetResponse> getAvailableLanguages()` -- Returns metadata for all 12 code generation languages
  - `List<CodeSnippetTemplateResponse> getTemplates()` -- Lists all custom code snippet templates
  - `CodeSnippetTemplateResponse getTemplate(UUID templateId)` -- Gets a single template by ID
  - `CodeSnippetTemplateResponse saveTemplate(CodeLanguage language, String displayName, String templateContent, String fileExtension, String contentType)` -- Upsert template by language
  - `void deleteTemplate(UUID templateId)` -- Deletes a custom template
- **Notes:** Two-tier generation: custom DB templates (Mustache-style {{placeholder}}) override built-in hardcoded generators. Built-in generators for: cURL, Python Requests, JavaScript Fetch, JavaScript Axios, Java HttpClient, Java OkHttp, C# HttpClient, Go, Ruby, PHP, Swift, Kotlin.

### 9.13 ExportService
- **Annotation:** `@Service`, `@Transactional(readOnly = true)`
- **Dependencies:** `CollectionService`, `FolderRepository`, `ObjectMapper`
- **Public Methods:**
  - `ExportCollectionResponse exportAsPostman(UUID collectionId, UUID teamId)` -- Exports collection as Postman v2.1 JSON
  - `ExportCollectionResponse exportAsOpenApi(UUID collectionId, UUID teamId)` -- Exports collection as OpenAPI 3.0.3 JSON
  - `ExportCollectionResponse exportAsNative(UUID collectionId, UUID teamId)` -- Exports collection as native Courier JSON

### 9.14 ImportService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `CollectionRepository`, `PostmanImporter`, `OpenApiImporter`, `CurlImporter`
- **Public Methods:**
  - `ImportResultResponse importCollection(UUID teamId, UUID userId, ImportCollectionRequest request)` -- Imports collection from Postman/OpenAPI/cURL with format auto-detection
- **Package-private Methods:**
  - `String resolveFormat(String format, String content)` -- Resolves format string with auto-detection support

### 9.15 PostmanImporter
- **Annotation:** `@Component`
- **Dependencies:** `ObjectMapper` (constructor-injected)
- **Public Methods:**
  - `PostmanImportResult parse(String json, UUID teamId, UUID createdBy)` -- Parses Postman v2.1 JSON into Collection entity graph with nested folders/requests
- **Inner Records:**
  - `record PostmanImportResult(Collection collection, int foldersImported, int requestsImported, int environmentsImported, List<String> warnings)` -- Import result with counts and warnings

### 9.16 OpenApiImporter
- **Annotation:** `@Component`
- **Dependencies:** `ObjectMapper` (jsonMapper), `ObjectMapper` (yamlMapper) -- two ObjectMapper instances, one for JSON and one for YAML
- **Public Methods:**
  - `OpenApiImportResult parse(String content, boolean isYaml, UUID teamId, UUID createdBy)` -- Parses OpenAPI 3.x spec into Collection entity graph with tag-based folder grouping
- **Inner Records:**
  - `record OpenApiImportResult(Collection collection, int foldersImported, int requestsImported, int environmentsImported, List<String> warnings)` -- Import result with counts and warnings

### 9.17 CurlImporter
- **Annotation:** `@Component`
- **Dependencies:** None (no constructor injection)
- **Public Methods:**
  - `Request parseCurl(String curlCommand, Folder folder, int sortOrder)` -- Parses cURL command into Request entity with headers, body, auth, and query params
- **Package-private Methods:**
  - `String normalizeCommand(String command)` -- Collapses backslash-newline continuations
  - `List<String> tokenize(String command)` -- Tokenizes command respecting single/double quotes

### 9.18 DataFileParser
- **Annotation:** `@Service`
- **Dependencies:** None (uses static `ObjectMapper`)
- **Public Methods:**
  - `List<Map<String, String>> parse(String content, String filename)` -- Parses CSV or JSON data file into iteration variable maps

### 9.19 GraphQLService
- **Annotation:** `@Service`
- **Dependencies:** `RequestProxyService`, `VariableService`, `ObjectMapper`
- **Public Methods:**
  - `GraphQLResponse executeQuery(ExecuteGraphQLRequest request, UUID teamId, UUID userId)` -- Executes a GraphQL query/mutation with variable resolution
  - `GraphQLResponse introspect(IntrospectGraphQLRequest request, UUID teamId, UUID userId)` -- Introspects a GraphQL endpoint to retrieve schema
  - `List<String> validateQuery(String query)` -- Basic syntax validation (balanced braces, valid keywords)
  - `String formatQuery(String query)` -- Prettifies a GraphQL query with proper indentation

### 9.20 ShareService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `CollectionShareRepository`, `CollectionRepository`
- **Public Methods:**
  - `CollectionShareResponse shareCollection(UUID collectionId, UUID teamId, UUID sharedByUserId, ShareCollectionRequest request)` -- Shares a collection with another user at a permission level
  - `List<CollectionShareResponse> getCollectionShares(UUID collectionId, UUID teamId)` -- Lists all shares for a collection
  - `List<CollectionShareResponse> getSharedWithUser(UUID userId)` -- Lists all collections shared with a user
  - `CollectionShareResponse updateSharePermission(UUID collectionId, UUID sharedWithUserId, UUID teamId, UpdateSharePermissionRequest request)` -- Updates permission level for an existing share
  - `void revokeShare(UUID collectionId, UUID sharedWithUserId, UUID teamId)` -- Revokes a user's share
  - `boolean hasPermission(UUID collectionId, UUID userId, SharePermission requiredPermission)` -- Checks if user has at least the specified permission (owner always passes)
  - `boolean canView(UUID collectionId, UUID userId)` -- Checks VIEWER or higher permission
  - `boolean canEdit(UUID collectionId, UUID userId)` -- Checks EDITOR or higher permission
  - `boolean canAdmin(UUID collectionId, UUID userId)` -- Checks ADMIN permission

### 9.21 ForkService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `ForkRepository`, `CollectionService`
- **Public Methods:**
  - `ForkResponse forkCollection(UUID collectionId, UUID teamId, UUID userId, CreateForkRequest request)` -- Creates a fork (deep copy) of a collection
  - `List<ForkResponse> getForksForCollection(UUID collectionId, UUID teamId)` -- Lists all forks of a collection
  - `List<ForkResponse> getUserForks(UUID userId)` -- Lists all forks created by a user
  - `ForkResponse getFork(UUID forkId)` -- Gets a specific fork by ID

### 9.22 MergeService
- **Annotation:** `@Service`, `@Transactional`
- **Dependencies:** `MergeRequestRepository`, `ForkRepository`, `FolderRepository`, `CollectionService`, `ObjectMapper`
- **Public Methods:**
  - `MergeRequestResponse createMergeRequest(UUID teamId, UUID userId, CreateMergeRequestRequest request)` -- Creates a merge request from a fork back to source
  - `List<MergeRequestResponse> getMergeRequests(UUID collectionId, UUID teamId)` -- Lists merge requests for a collection
  - `MergeRequestResponse getMergeRequest(UUID mergeRequestId)` -- Gets a single merge request by ID
  - `MergeRequestResponse resolveMergeRequest(UUID mergeRequestId, UUID userId, ResolveMergeRequest request)` -- Resolves merge request (MERGE with conflict detection or CLOSE)
- **Package-private Methods:**
  - `List<ConflictDetail> detectConflicts(Collection fork, Collection target)` -- Detects method/URL conflicts between fork and target
- **Inner Records:**
  - `record ConflictDetail(String path, String conflictType, String forkValue, String targetValue)` -- Details of a single merge conflict

### Relay Module


### === ChannelService ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/service/ChannelService.java`
Dependencies: ChannelRepository, ChannelMemberRepository, MessageRepository, ReadReceiptRepository, PinnedMessageRepository, UserRepository, TeamMemberRepository, ChannelMapper, ChannelMemberMapper
Methods:
- `createChannel(CreateChannelRequest, UUID teamId, UUID userId) -> ChannelResponse` — creates channel with unique slug, auto-adds creator as OWNER
- `getChannel(UUID channelId, UUID teamId, UUID userId) -> ChannelResponse` — retrieves channel with private-access check
- `getChannelsByTeam(UUID teamId, UUID userId) -> List<ChannelSummaryResponse>` — lists non-archived visible channels with unread counts, sorted by unread-first then activity
- `getChannelsByTeamPaged(UUID teamId, UUID userId, int page, int size) -> PageResponse<ChannelSummaryResponse>` — paginated version of getChannelsByTeam
- `updateChannel(UUID channelId, UpdateChannelRequest, UUID teamId, UUID userId) -> ChannelResponse` — updates name/description/archive status (OWNER/ADMIN only)
- `deleteChannel(UUID channelId, UUID teamId, UUID userId) -> void` — hard-deletes channel and associated data (cannot delete #general)
- `archiveChannel(UUID channelId, UUID teamId, UUID userId) -> ChannelResponse` — sets channel read-only (cannot archive #general)
- `unarchiveChannel(UUID channelId, UUID teamId, UUID userId) -> ChannelResponse` — removes archive flag
- `updateTopic(UUID channelId, UpdateChannelTopicRequest, UUID teamId, UUID userId) -> ChannelResponse` — any channel member can change topic
- `joinChannel(UUID channelId, UUID teamId, UUID userId) -> ChannelMemberResponse` — joins public/project/service channels
- `leaveChannel(UUID channelId, UUID teamId, UUID userId) -> void` — leaves channel, prevents last owner/admin from leaving
- `inviteMember(UUID channelId, InviteMemberRequest, UUID teamId, UUID inviterId) -> ChannelMemberResponse` — OWNER/ADMIN invites user to channel
- `removeMember(UUID channelId, UUID targetUserId, UUID teamId, UUID removerId) -> void` — removes member (team owner required to remove channel owner)
- `updateMemberRole(UUID channelId, UUID targetUserId, UpdateMemberRoleRequest, UUID teamId, UUID updaterId) -> ChannelMemberResponse` — changes member role (OWNER or team ADMIN/OWNER required)
- `getMembers(UUID channelId, UUID teamId) -> List<ChannelMemberResponse>` — lists all channel members with display names
- `pinMessage(UUID channelId, PinMessageRequest, UUID teamId, UUID userId) -> PinnedMessageResponse` — pins message (enforces max pins per channel)
- `unpinMessage(UUID channelId, UUID messageId, UUID teamId, UUID userId) -> void` — removes pin
- `getPinnedMessages(UUID channelId, UUID teamId) -> List<PinnedMessageResponse>` — lists pinned messages ordered by most recent
- `createProjectChannel(UUID projectId, String projectName, UUID teamId) -> Channel` — auto-creates PROJECT channel with all team members
- `createServiceChannel(UUID serviceId, String serviceName, UUID teamId) -> Channel` — auto-creates SERVICE channel with admin/owner members
- `ensureGeneralChannel(UUID teamId, UUID creatorUserId) -> Channel` — creates #general if missing, idempotent

### === MessageService ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/service/MessageService.java`
Dependencies: MessageRepository, ChannelRepository, ChannelMemberRepository, ReadReceiptRepository, UserRepository, MessageMapper, ThreadService, ReactionRepository, FileAttachmentRepository, TeamMemberRepository
Methods:
- `sendMessage(UUID channelId, SendMessageRequest, UUID teamId, UUID senderId) -> MessageResponse` — sends message with @mention support, thread delegation, auto read-receipt
- `getMessage(UUID messageId) -> MessageResponse` — retrieves single message with all populated fields
- `getChannelMessages(UUID channelId, UUID teamId, UUID userId, int page, int size) -> PageResponse<MessageResponse>` — paginated top-level messages (no parent) newest first
- `getThreadReplies(UUID parentMessageId, UUID userId) -> List<MessageResponse>` — retrieves all replies to a parent message chronologically
- `editMessage(UUID messageId, UpdateMessageRequest, UUID userId) -> MessageResponse` — edits message (sender only, sets isEdited flag)
- `deleteMessage(UUID messageId, UUID userId, UUID teamId) -> void` — soft-deletes message (sender or channel/team admin)
- `searchMessages(UUID channelId, String query, UUID teamId, UUID userId, int page, int size) -> PageResponse<MessageResponse>` — searches within single channel by content substring
- `searchMessagesAcrossChannels(String query, UUID teamId, UUID userId, int page, int size) -> PageResponse<ChannelSearchResultResponse>` — searches all user's channels with ~100-char snippets
- `markRead(UUID channelId, MarkReadRequest, UUID userId) -> ReadReceiptResponse` — marks messages as read, updates read receipt and membership lastReadAt
- `getUnreadCounts(UUID teamId, UUID userId) -> List<UnreadCountResponse>` — returns unread counts for all user's channels, sorted by count DESC

### === ThreadService ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/service/ThreadService.java`
Dependencies: MessageThreadRepository, MessageRepository, MessageThreadMapper
Methods:
- `onReply(UUID parentMessageId, UUID channelId, UUID replierId) -> void` — creates or updates thread metadata on reply (increments count, adds participant)
- `getThreadInfo(UUID rootMessageId) -> Optional<MessageThread>` — retrieves thread metadata if exists
- `getThread(UUID rootMessageId, UUID userId) -> MessageThreadResponse` — retrieves full thread with parsed participants and reply list
- `getActiveThreads(UUID channelId) -> List<MessageThreadResponse>` — lists all threads in channel ordered by most recent reply

### === DirectMessageService ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/service/DirectMessageService.java`
Dependencies: DirectConversationRepository, DirectMessageRepository, TeamMemberRepository, UserRepository, DirectConversationMapper, DirectMessageMapper, ReadReceiptRepository
Methods:
- `getOrCreateConversation(CreateDirectConversationRequest, UUID teamId, UUID userId) -> DirectConversationResponse` — idempotent get-or-create 1:1 conversation with sorted participant IDs
- `getConversation(UUID conversationId, UUID userId) -> DirectConversationResponse` — retrieves conversation with participant verification
- `getConversations(UUID teamId, UUID userId) -> List<DirectConversationSummaryResponse>` — lists user's DM conversations sorted by most recent message
- `deleteConversation(UUID conversationId, UUID userId) -> void` — hard-deletes conversation and all messages
- `sendDirectMessage(UUID conversationId, SendDirectMessageRequest, UUID senderId) -> DirectMessageResponse` — sends DM, updates conversation preview
- `getMessages(UUID conversationId, UUID userId, int page, int size) -> PageResponse<DirectMessageResponse>` — paginated DM messages newest first
- `editDirectMessage(UUID messageId, UpdateDirectMessageRequest, UUID userId) -> DirectMessageResponse` — edits DM (sender only)
- `deleteDirectMessage(UUID messageId, UUID userId) -> void` — soft-deletes DM (sender only)
- `markConversationRead(UUID conversationId, UUID userId) -> void` — marks conversation as read using ReadReceipt with conversationId in channelId field
- `getUnreadCount(UUID conversationId, UUID userId) -> long` — counts unread messages from other participants

### === ReactionService ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/service/ReactionService.java`
Dependencies: ReactionRepository, MessageRepository, ChannelMemberRepository, UserRepository, ReactionMapper
Methods:
- `toggleReaction(UUID messageId, AddReactionRequest, UUID userId) -> ReactionResponse` — toggle-based add/remove reaction on channel message (returns null on remove)
- `getReactionsForMessage(UUID messageId) -> List<ReactionSummaryResponse>` — aggregated summaries grouped by emoji (no per-user flag)
- `getReactionsForMessageWithUser(UUID messageId, UUID currentUserId) -> List<ReactionSummaryResponse>` — aggregated summaries with currentUserReacted flag
- `getReactionsByUser(UUID userId, UUID channelId) -> List<ReactionResponse>` — all reactions by a user in a channel
- `removeAllReactionsForMessage(UUID messageId) -> void` — bulk deletes all reactions for a message

### === FileAttachmentService ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/service/FileAttachmentService.java`
Dependencies: FileAttachmentRepository, MessageRepository, ChannelMemberRepository, FileAttachmentMapper
Configuration Properties:
- `relay.file-storage.base-path` (default: `${user.home}/codeops-files`)
- `relay.file-storage.max-file-size` (default: 26214400 = 25MB)
Inner Records: `FileDownloadResult(String fileName, String contentType, byte[] content)`
Methods:
- `uploadFile(UUID messageId, String fileName, String contentType, byte[] fileContent, UUID userId) -> FileAttachmentResponse` — uploads file to local storage, creates entity with COMPLETE status
- `downloadFile(UUID attachmentId, UUID userId) -> FileDownloadResult` — downloads file with channel membership verification
- `getAttachmentsForMessage(UUID messageId) -> List<FileAttachmentResponse>` — lists attachments ordered by creation time
- `deleteAttachment(UUID attachmentId, UUID userId) -> void` — deletes file (uploader or channel OWNER/ADMIN)
- `deleteAllAttachmentsForMessage(UUID messageId) -> void` — bulk deletes all attachments and files for a message

### === PresenceService ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/service/PresenceService.java`
Dependencies: UserPresenceRepository, TeamMemberRepository, UserRepository, UserPresenceMapper
Methods:
- `updatePresence(UpdatePresenceRequest, UUID teamId, UUID userId) -> UserPresenceResponse` — updates status and optional statusMessage, refreshes heartbeat
- `heartbeat(UUID teamId, UUID userId) -> UserPresenceResponse` — records heartbeat, transitions OFFLINE/AWAY to ONLINE (preserves DND)
- `getPresence(UUID teamId, UUID userId) -> UserPresenceResponse` — retrieves user presence with staleness check
- `getTeamPresence(UUID teamId) -> List<UserPresenceResponse>` — all team presences with staleness cleanup, sorted by status then name
- `getOnlineUsers(UUID teamId) -> List<UserPresenceResponse>` — truly online users (excludes stale heartbeats)
- `setDoNotDisturb(UUID teamId, UUID userId, String statusMessage) -> UserPresenceResponse` — sets DND with custom message
- `clearDoNotDisturb(UUID teamId, UUID userId) -> UserPresenceResponse` — clears DND, transitions to ONLINE
- `goOffline(UUID teamId, UUID userId) -> void` — explicit offline (logout/disconnect)
- `cleanupStalePresences(UUID teamId) -> int` — transitions stale non-offline presences to OFFLINE, returns count
- `getPresenceCount(UUID teamId) -> Map<PresenceStatus, Long>` — status-grouped counts after cleanup

### === PlatformEventService ===
File: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/relay/service/PlatformEventService.java`
Dependencies: PlatformEventRepository, ChannelRepository, MessageRepository, UserRepository, PlatformEventMapper
Methods:
- `publishEvent(PlatformEventType, UUID teamId, UUID sourceEntityId, String sourceModule, String title, String detail, UUID senderId, UUID targetChannelId) -> PlatformEventResponse` — publishes event, auto-resolves target channel, delivers as PLATFORM_EVENT message
- `publishEventSimple(PlatformEventType, UUID teamId, String title, UUID senderId) -> PlatformEventResponse` — convenience wrapper for events without source entity
- `getEvent(UUID eventId) -> PlatformEventResponse` — retrieves single event by ID
- `getEventsForTeam(UUID teamId, int page, int size) -> PageResponse<PlatformEventResponse>` — paginated team events, newest first
- `getEventsForTeamByType(UUID teamId, PlatformEventType, int page, int size) -> PageResponse<PlatformEventResponse>` — paginated team events filtered by type
- `getEventsForEntity(UUID sourceEntityId) -> List<PlatformEventResponse>` — all events for a source entity
- `getUndeliveredEvents(UUID teamId) -> List<PlatformEventResponse>` — lists undelivered events in chronological order
- `retryDelivery(UUID eventId, UUID senderId) -> PlatformEventResponse` — retries single undelivered event (idempotent)
- `retryAllUndelivered(UUID teamId, UUID senderId) -> int` — retries all undelivered events for team, returns success count

---

## 10. Security Architecture

**Authentication Flow:**
- JWT (HS256) via jjwt 0.12.6
- Access token: 24h expiry, claims: `sub` (userId UUID), `email`, `roles` (list of team role strings)
- Refresh token: 30d expiry
- MFA challenge token: 5m expiry (for TOTP/email verification)
- Token validation: `JwtAuthFilter` extracts Bearer token from Authorization header, validates signature + expiry, sets `UsernamePasswordAuthenticationToken` in SecurityContext with UUID principal
- Token revocation: `TokenBlacklistService` maintains in-memory `ConcurrentHashMap<String, Instant>` of blacklisted tokens, checked during validation
- Logout: `POST /api/v1/auth/logout` blacklists current token

**Authorization Model:**
- 4 roles: OWNER, ADMIN, MEMBER, VIEWER (stored as `TeamRole` enum on `TeamMember` entity)
- Roles mapped to Spring authorities as `ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_VIEWER`
- `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")` on controller classes and methods
- Team membership verified in service layer via `TeamMemberRepository.existsByTeamIdAndUserId()`
- Admin verification via `TeamMemberRepository.findByTeamIdAndUserId()` checking role ≥ ADMIN
- Self-access verification for user profile operations

**Security Filter Chain (execution order):**
1. `RequestCorrelationFilter` — generates/propagates X-Correlation-ID header
2. `RateLimitFilter` — 10 requests per 60 seconds on `/api/v1/auth/**` paths, keyed by IP
3. `JwtAuthFilter` — extracts and validates JWT, sets SecurityContext
4. Spring Security filter chain — `authorizeHttpRequests` rules

**Public paths (permitAll):**
- `/api/v1/auth/**` — login, register, refresh, MFA
- `/api/v1/health` — core health
- `/api/v1/courier/health` — courier health
- `/swagger-ui/**`, `/v3/api-docs/**` — Swagger UI
- `/ws/relay/**` — WebSocket endpoint

**CORS Configuration:**
- `CorsConfig` class reads `codeops.cors.allowed-origins` (comma-separated)
- Dev: `http://localhost:3000,http://localhost:5173`
- Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Headers: Authorization, Content-Type, X-Team-ID, X-Correlation-ID
- Credentials: true
- Max age: 3600s

**Encryption:**
- AES-256-GCM via `EncryptionService` for GitHub PATs, Jira API tokens
- Key from `codeops.encryption.key` (env var in prod)
- 12-byte random IV prepended to ciphertext, Base64-encoded for storage

**Password Policy:**
- BCrypt with strength 12
- Minimum length: 1 character (dev-minimal by design per CONVENTIONS.md)

**Rate Limiting:**
- `RateLimitFilter` — 10 requests per 60 seconds on `/api/v1/auth/**`
- Keyed by client IP (extracted from X-Forwarded-For or remoteAddr)
- Returns 429 Too Many Requests with error message

---

## 11. Notification / Messaging Layer

**Email Service:** `EmailService` — sends via SMTP (Spring JavaMailSender). Disabled in dev (`codeops.mail.enabled: false`), logs to console instead. Used for MFA email codes, invitation emails. Plain text messages, no templates.

**Webhook Service:** `TeamsWebhookService` — sends alert notifications to Microsoft Teams via incoming webhook URLs. SSRF protection via `validateWebhookUrl()` — rejects private/loopback/link-local addresses. POST with AdaptiveCard JSON payload.

**Async Dispatcher:** `NotificationDispatcher` — `@Async` dispatch of notifications. Routes to EmailService or TeamsWebhookService based on alert channel type.

**Kafka Integration:**
- `KafkaLogConsumer` (`@KafkaListener(topics = "codeops-logs")`) — consumes log entries from external services
- `KafkaConsumerConfig` — configures consumer factory with string deserializers, group ID `codeops-server`
- 10 topics created by kafka-init container (decision events, outcome events, hypothesis events, integration sync, notifications)
- No Kafka producers in current codebase

---

## 12. Error Handling

**Global Exception Handler:** `com.codeops.config.GlobalExceptionHandler` (`@RestControllerAdvice`)

**Response format:** `ErrorResponse` record: `{ "status": int, "message": string }`

| Exception Type | HTTP Status | Client Message |
|---|---|---|
| `EntityNotFoundException` (JPA) | 404 | "Resource not found" |
| `NotFoundException` (CodeOps) | 404 | Exception message (controlled) |
| `NoResourceFoundException` | 404 | "Resource not found" |
| `IllegalArgumentException` | 400 | "Invalid request" |
| `ValidationException` (CodeOps) | 400 | Exception message (controlled) |
| `MethodArgumentNotValidException` | 400 | Field-level errors: "field: message, ..." |
| `MissingServletRequestParameterException` | 400 | "Missing required parameter: {name}" |
| `MissingRequestHeaderException` | 400 | "Missing required header: {name}" |
| `MethodArgumentTypeMismatchException` | 400 | "Invalid value for parameter '{name}': {value}" |
| `HttpMessageNotReadableException` | 400 | "Malformed request body" |
| `AccessDeniedException` (Spring) | 403 | "Access denied" |
| `AuthorizationException` (CodeOps) | 403 | Exception message (controlled) |
| `HttpRequestMethodNotSupportedException` | 405 | "HTTP method '{method}' is not supported..." |
| `CodeOpsException` (base) | 500 | "An internal error occurred" |
| `Exception` (catch-all) | 500 | "An internal error occurred" |

**Exception hierarchy:**
```
CodeOpsException (extends RuntimeException)
├── AuthorizationException
├── NotFoundException
└── ValidationException
```

Internal stack traces and messages from 500 errors are logged server-side but never exposed to clients.

---

## 13. Test Coverage

```
Unit test files:       179
Integration test files: 16
Unit @Test methods:    2,941
Integration @Test:     121
Total @Test methods:   3,062
```

**Framework:** JUnit 5 + Mockito 5.21.0 + Spring Boot Test
**Unit tests:** H2 in-memory database, `@WebMvcTest` for controllers (MockMvc), `@ExtendWith(MockitoExtension.class)` for services
**Integration tests:** Testcontainers PostgreSQL, `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("integration")`
**Test configs:** `application-test.yml` (H2), `application-integration.yml` (Testcontainers)
**Security test pattern:** `@Import(TestSecurityConfig.class)` + `FilterRegistrationBean.setEnabled(false)` for disabling servlet filters + `authentication()` post-processor for injecting UUID principal

---

## 14. Cross-Cutting Patterns & Conventions

**Naming conventions:**
- Controllers: `{Entity}Controller` at `@RequestMapping(prefix + "/{resource}")`, methods named `create/get/update/delete{Entity}`
- Services: `{Entity}Service` with `@RequiredArgsConstructor` injection
- DTOs: `{Action}{Entity}Request` / `{Entity}Response` — all Java records
- Endpoints: REST paths `/{module}/v1/{resource}` with kebab-case

**Package structure:** Feature-based modules (Core, Registry, Logger, Courier, Relay), each with entity/enums/repository/service/controller/dto sub-packages.

**Base classes:** `BaseEntity` — provides `id` (UUID, `GenerationType.UUID`), `createdAt`, `updatedAt` with `@PrePersist`/`@PreUpdate`. Used by all entities except `SystemSetting` (String PK), `AuditLog` (Long PK), `MfaEmailCode` (standalone UUID PK).

**Audit logging:** `AuditLogService.log(action, entityType, entityId, details, userId, teamId, ipAddress)` — `@Async` fire-and-forget. Called from service methods after mutations. IP sourced from `RequestContextHolder`.

**Error handling:** Services throw `NotFoundException`, `ValidationException`, `AuthorizationException`. Controllers do not catch exceptions — all handled by `GlobalExceptionHandler`.

**Pagination:** `PageResponse<T>` generic record (`content`, `page`, `size`, `totalElements`, `totalPages`, `last`). Services accept `(int page, int size)` and return `PageResponse`. Default page size from `AppConstants.DEFAULT_PAGE_SIZE` (20).

**Validation:** Jakarta Bean Validation on request DTO record components (`@NotBlank`, `@Size`, `@NotNull`, `@Email`, `@Pattern`). Business validation in service layer (throws `ValidationException`).

**Constants:** `AppConstants` — centralized class with all limits, defaults, API prefixes, port ranges, and module-specific constants.

**Documentation comments:** All new modules (Logger, Courier, Relay) have Javadoc on classes and public methods. Core module has partial coverage (38.6% classes, 40.7% methods). DTOs, entities, and enums excluded from coverage target per convention.

---

## 15. Known Issues, TODOs, and Technical Debt

**2 TODOs found:**

1. `src/main/java/com/codeops/service/EncryptionService.java:56`:
   ```
   // TODO: Changing key derivation invalidates existing encrypted data — requires re-encryption migration
   ```

2. `src/main/java/com/codeops/config/DataSeeder.java:254` — in seed data content (not actual code debt):
   ```
   "- No TODOs or commented-out code" (part of a code review checklist string)
   ```

**Actual technical debt items:**
- No CI/CD pipeline (INF-06)
- Redis provisioned in docker-compose but not used by application code
- Comma-separated UUID strings for `participantIds` and `mentionedUserIds` in Relay module instead of join tables
- Token blacklist is in-memory (lost on restart) — should migrate to Redis
- Core module Javadoc coverage at ~40%

---

## 16. OpenAPI Specification

See `CodeOps-Server-OpenAPI.yaml` in the project root. Generated from source code analysis of all 60+ controllers (~472 endpoints).

---

## 17. Database — Live Schema Audit

**72 tables** in PostgreSQL `public` schema. Schema managed by Hibernate `ddl-auto: update`.

### Table Inventory (by module)

**Core (26 tables):** users, teams, team_members, projects, invitations, personas, directives, project_directives, github_connections, jira_connections, qa_jobs, findings, compliance_items, dependency_scans, dependency_vulnerabilities, tech_debt_items, remediation_tasks, remediation_task_findings, specifications, bug_investigations, agent_runs, health_snapshots, health_schedules, notification_preferences, system_settings, audit_log, mfa_email_codes

**Registry (11 tables):** service_registrations, solutions, solution_members, service_dependencies, port_allocations, port_ranges, api_route_registrations, environment_configs, config_templates, infra_resources, workstation_profiles

**Logger (16 tables):** log_entries, log_sources, log_traps, log_trap_conditions, alert_rules, alert_channels, alert_histories, metric_definitions, metric_data_points, trace_entries, trace_spans, dashboards, dashboard_widgets, retention_policies, anomaly_baselines, saved_queries

**Courier (~15 tables):** collections, folders, api_requests, request_headers, request_params, environments, environment_variables, request_histories, request_history_headers, scripts, run_results, run_iterations, code_templates

**Relay (12 tables):** channels, channel_members, messages, message_threads, pinned_messages, reactions, direct_conversations, direct_messages, read_receipts, file_attachments, platform_events, user_presences

### Foreign Key Constraints

82 foreign key constraints link entities across modules. All FK columns are indexed per the `@Index` annotations on entities.

### JPA vs Database Drift Check

JPA model and database schema are in sync. Hibernate `ddl-auto: update` manages schema creation. All 72 tables match their JPA entity definitions.

---

## 18. Kafka / Message Broker

**Kafka integration detected.** Spring Kafka configured in `application-dev.yml` (bootstrap-servers: `localhost:9094`).

**Topics (created by kafka-init container):**
1. `codeops.core.decision.created` (3 partitions)
2. `codeops.core.decision.resolved` (3 partitions)
3. `codeops.core.decision.escalated` (3 partitions)
4. `codeops.core.outcome.created` (3 partitions)
5. `codeops.core.outcome.validated` (3 partitions)
6. `codeops.core.outcome.invalidated` (3 partitions)
7. `codeops.core.hypothesis.created` (3 partitions)
8. `codeops.core.hypothesis.concluded` (3 partitions)
9. `codeops.integrations.sync` (3 partitions)
10. `codeops.notifications` (3 partitions)

**Additional topic:** `codeops-logs` (used by Logger module, defined in AppConstants)

**Consumer:** `KafkaLogConsumer` — `@KafkaListener(topics = "codeops-logs", groupId = "codeops-logger")`. Deserializes JSON log entries and passes to `LogIngestionService.ingestSingle()`.

**Consumer Config:** `KafkaConsumerConfig` — String key/value deserializers, group ID `codeops-server`, auto-offset-reset `earliest`.

**Producers:** No Kafka producers in current codebase. Topics are created for future use.

**Error handling:** Consumer has try-catch around deserialization, logs errors and continues. No DLQ configured.

---

## 19. Redis / Cache Layer

No Redis or caching layer detected in application code. Redis is provisioned in `docker-compose.yml` (Redis 7 Alpine on port 6379) but no `RedisTemplate`, `@Cacheable`, or `CacheManager` references exist in the source code. Redis is available for future use.

---

## 20. Environment Variable Inventory

| Variable | Required | Default | Used By | Purpose |
|---|---|---|---|---|
| `DB_USERNAME` | No | `codeops` | application-dev.yml | Database username |
| `DB_PASSWORD` | No | `codeops` | application-dev.yml | Database password |
| `JWT_SECRET` | **Prod: Yes** | dev fallback | JwtProperties | JWT signing key (min 32 chars) |
| `ENCRYPTION_KEY` | **Prod: Yes** | dev fallback | EncryptionService | AES-256-GCM key for credential encryption |
| `DATABASE_URL` | **Prod: Yes** | — | application-prod.yml | JDBC connection URL |
| `DATABASE_USERNAME` | **Prod: Yes** | — | application-prod.yml | Production DB username |
| `DATABASE_PASSWORD` | **Prod: Yes** | — | application-prod.yml | Production DB password |
| `CORS_ALLOWED_ORIGINS` | **Prod: Yes** | localhost | CorsConfig | Comma-separated allowed origins |
| `S3_BUCKET` | **Prod: Yes** | — | S3Config | AWS S3 bucket name |
| `AWS_REGION` | **Prod: Yes** | — | S3Config | AWS region |
| `MAIL_FROM_EMAIL` | **Prod: Yes** | — | MailProperties | Sender email address |

**Dangerous defaults:** `DB_PASSWORD=codeops` and `JWT_SECRET` fallback in dev profile. Both are overridden by env vars in prod.

---

## 21. Inter-Service Communication Map

**Outbound HTTP calls:**

1. **TeamsWebhookService** — `RestTemplate.postForObject()` to Microsoft Teams webhook URLs (from `AlertChannel.webhookUrl`). SSRF-protected via `validateWebhookUrl()`.

2. **AlertChannelService** — Routes alert notifications to email or webhook channels via NotificationDispatcher.

3. **ServiceRegistryService** (Registry module) — `RestTemplate.getForEntity()` to registered service health check URLs for health probing.

4. **RequestProxyService** (Courier module) — `java.net.http.HttpClient` for executing user-defined HTTP requests (API testing proxy). Configurable timeout, redirect handling, and response body capture.

**Inbound dependencies:** CodeOps-Client (Flutter desktop app) and CodeOps-Analytics (port 8081) consume this API.

**Inter-module communication:** All modules share the same JVM and database. No inter-module HTTP calls. Cross-module references go through shared packages (`com.codeops.entity`, `com.codeops.security`, `com.codeops.config`).
