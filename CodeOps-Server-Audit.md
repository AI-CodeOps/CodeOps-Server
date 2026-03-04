# CodeOps-Server — Codebase Audit

**Audit Date:** 2026-03-03T16:08:00Z
**Branch:** main
**Commit:** 2cf0617d3c35631a0210ab0bd845c0f8c99ad8a3 Codebase audit and quality scorecard — 2026-03-02
**Auditor:** Claude Code (Automated)
**Purpose:** Zero-context reference for AI-assisted development
**Audit File:** CodeOps-Server-Audit.md
**OpenAPI Spec:** openapi.yaml

> This audit is the single source of truth for the CodeOps-Server codebase.
> The OpenAPI spec (openapi.yaml) is the source of truth for all endpoints, DTOs, and API contracts.
> An AI reading this audit + the OpenAPI spec should be able to generate accurate code
> changes, new features, tests, and fixes without filesystem access.

---

## 1. Project Identity

```
Project Name: CodeOps-Server
Repository URL: (local at ~/Documents/GitHub/CodeOps-Server)
Primary Language / Framework: Java 21 / Spring Boot 3.3.0
Build Tool + Version: Maven 3 (via wrapper)
Current Branch: main
Latest Commit Hash: 2cf0617d3c35631a0210ab0bd845c0f8c99ad8a3
Latest Commit Message: Codebase audit and quality scorecard — 2026-03-02
Audit Timestamp: 2026-03-03T16:08:00Z
```

---

## 2. Directory Structure

```
CodeOps-Server/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── start-codeops.sh
├── scripts/
│   └── seed-codeops.sh
├── seed-data.md
├── CLAUDE.md
├── src/main/java/com/codeops/
│   ├── CodeOpsApplication.java
│   ├── config/
│   │   ├── AppConstants.java
│   │   ├── CorsConfig.java
│   │   ├── DataSeeder.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── HealthController.java
│   │   ├── JacksonConfig.java
│   │   ├── KafkaConfig.java
│   │   ├── OpenApiConfig.java
│   │   ├── RedisConfig.java
│   │   ├── RequestCorrelationFilter.java
│   │   ├── S3Config.java
│   │   ├── WebMvcConfig.java
│   │   └── WebSocketConfig.java
│   ├── security/
│   │   ├── JwtAuthFilter.java
│   │   ├── JwtTokenProvider.java
│   │   ├── RateLimitFilter.java
│   │   ├── SecurityConfig.java
│   │   └── SecurityUtils.java
│   ├── entity/                          ← 25 core entities + BaseEntity
│   │   └── enums/                       ← 25 core enums
│   ├── repository/                      ← 26 core repositories
│   ├── dto/
│   │   ├── mapper/                      ← 15 core MapStruct mappers
│   │   ├── request/                     ← Core request DTOs
│   │   └── response/                    ← Core response DTOs
│   ├── service/                         ← 26+ core services
│   ├── controller/                      ← 17 core controllers
│   ├── exception/                       ← Custom exception classes
│   ├── notification/                    ← Email, Teams webhook, notification dispatch
│   ├── courier/                         ← Courier module (HTTP API testing)
│   │   ├── entity/ (18 entities)
│   │   ├── entity/enums/ (7 enums)
│   │   ├── repository/ (18 repositories)
│   │   ├── service/ (22 services)
│   │   ├── controller/ (14 controllers)
│   │   ├── dto/mapper/ (13 mappers)
│   │   ├── dto/request/
│   │   └── dto/response/
│   ├── fleet/                           ← Fleet module (Docker container mgmt)
│   │   ├── entity/ (14 entities)
│   │   ├── entity/enums/ (4 enums)
│   │   ├── repository/ (14 repositories)
│   │   ├── service/ (6 services)
│   │   ├── controller/ (8 controllers)
│   │   ├── dto/mapper/ (10 mappers)
│   │   ├── dto/request/
│   │   └── dto/response/
│   ├── logger/                          ← Logger module (centralized logging)
│   │   ├── entity/ (16 entities)
│   │   ├── entity/enums/ (10 enums)
│   │   ├── repository/ (16 repositories)
│   │   ├── service/ (19 services)
│   │   ├── controller/ (10 controllers)
│   │   ├── dto/mapper/ (13 mappers)
│   │   ├── dto/request/
│   │   └── dto/response/
│   ├── mcp/                             ← MCP module (AI coding sessions)
│   │   ├── entity/ (8 entities)
│   │   ├── entity/enums/ (8 enums)
│   │   ├── repository/ (8 repositories)
│   │   ├── service/ (8 services)
│   │   ├── controller/ (4 controllers)
│   │   ├── dto/mapper/
│   │   ├── dto/request/
│   │   └── dto/response/
│   ├── registry/                        ← Registry module (service registry)
│   │   ├── entity/ (11 entities)
│   │   ├── entity/enums/ (11 enums)
│   │   ├── repository/ (11 repositories)
│   │   ├── service/ (10 services)
│   │   ├── controller/ (10 controllers)
│   │   ├── dto/mapper/ (11 mappers)
│   │   ├── dto/request/
│   │   └── dto/response/
│   └── relay/                           ← Relay module (real-time messaging)
│       ├── entity/ (12 entities)
│       ├── entity/enums/ (8 enums)
│       ├── repository/ (12 repositories)
│       ├── service/ (8 services)
│       ├── controller/ (8 controllers)
│       ├── dto/mapper/ (11 mappers)
│       ├── dto/request/
│       └── dto/response/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── application-test.yml
│   └── logback-spring.xml
└── src/test/java/                       ← 243 test files
```

Monolith with 7 logical modules under `com.codeops`. File counts: 1158 total, 905 main Java, 243 test Java. Each module follows the same layered architecture (controller → service → repository → entity).

---

## 3. Build & Dependency Manifest

**Build file:** `pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-parent | 3.3.0 | Parent POM |
| spring-boot-starter-web | (managed) | REST API framework |
| spring-boot-starter-data-jpa | (managed) | JPA/Hibernate ORM |
| spring-boot-starter-security | (managed) | Spring Security |
| spring-boot-starter-websocket | (managed) | WebSocket/STOMP for Relay module |
| spring-boot-starter-mail | (managed) | Email notifications |
| spring-boot-starter-validation | (managed) | Jakarta Bean Validation |
| spring-kafka | (managed) | Kafka integration (Logger module) |
| postgresql | (managed) | PostgreSQL JDBC driver |
| h2 | (managed) | In-memory DB for tests |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT token creation/validation |
| mapstruct | 1.5.5.Final | Entity-to-DTO mapping |
| lombok | 1.18.42 | Boilerplate reduction (Java 25 compat override) |
| aws-java-sdk-s3 | 2.25.0 | AWS S3 storage (reports, specs, personas) |
| graal-sdk + polyglot | 24.1.1 | GraalJS script engine (Courier scripts) |
| testcontainers | 1.19.8 | Integration test containers |
| jacoco-maven-plugin | 0.8.14 | Code coverage reporting |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI + OpenAPI generation |
| logstash-logback-encoder | (included) | JSON structured logging (prod) |
| spring-boot-starter-data-redis | (managed) | Redis (token blacklist) |
| mockito | 5.21.0 | Mocking (Java 25 compat override) |
| byte-buddy | 1.18.4 | ByteBuddy (Java 25 compat override) |

**Build Plugins:**
- `spring-boot-maven-plugin` — Excludes Lombok from final JAR
- `maven-compiler-plugin` — Java 21 source/target, annotation processors: Lombok + MapStruct
- `maven-surefire-plugin` — `--add-opens` for Java 25, includes `*Test.java` and `*IT.java`
- `jacoco-maven-plugin` 0.8.14 — Code coverage with prepare-agent + report

**Build Commands:**
```
Build: mvn clean compile -DskipTests
Test: mvn test
Run: mvn spring-boot:run
Package: mvn clean package -DskipTests
```

---

## 4. Configuration & Infrastructure Summary

- **`application.yml`** — Default profile: dev, server port: 8090, API prefix: `/api/v1/`
- **`application-dev.yml`** — PostgreSQL at localhost:5432/codeops (user=codeops, pass=codeops), Kafka localhost:9094, JWT dev secret (32+ chars), S3 disabled (local filesystem at ~/.codeops/storage/), mail disabled (logged to console), Hibernate ddl-auto: update
- **`application-prod.yml`** — All secrets from env vars (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, JWT_SECRET, AWS_S3_BUCKET, AWS_S3_REGION, MAIL_FROM_EMAIL), Hibernate ddl-auto: validate
- **`application-test.yml`** — H2 in-memory database, ddl-auto: create-drop
- **`logback-spring.xml`** — dev: human-readable console with MDC (correlationId, userId, teamId), prod: LogstashEncoder JSON with service name, test: WARN level only
- **`docker-compose.yml`** — PostgreSQL 16-alpine (5432, container `codeops-db`), Redis 7-alpine (6379, container `codeops-redis`), Zookeeper, Kafka Confluent 7.5.0 (9094/29094, container `codeops-kafka`), kafka-init (creates 10 topics)
- **`Dockerfile`** — eclipse-temurin:21-jre-alpine, non-root user (appuser), EXPOSE 8090

**Connection Map:**
```
Database: PostgreSQL 16, localhost:5432, database: codeops
Cache: Redis 7, localhost:6379 (token blacklist, session store)
Message Broker: Kafka (Confluent 7.5.0), localhost:9094
External APIs: GitHub API, Jira API, Microsoft Teams webhooks
Cloud Services: AWS S3 (reports, specs, personas), AWS SES (email)
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

**Entry point:** `CodeOpsApplication.main()` — `@SpringBootApplication`

**Startup sequence:**
1. Spring Boot auto-configuration (JPA, Security, Web, Validation, Kafka, Redis, WebSocket)
2. `@PostConstruct` in `JwtTokenProvider.validateSecret()` — validates JWT secret >= 32 chars
3. `DataSeeder.run()` (dev profile only) — seeds users, teams, projects, QA entities, Registry data, Logger data, Courier data. Default login: adam@allard.com / pass. Idempotent.

**Scheduled tasks:**
- `AnomalyDetectionService.recalculateAllBaselines()` — daily at 3:00 AM
- `RetentionExecutor.executeAllActivePolicies()` — daily at 2:00 AM

**Health check:** `GET /api/v1/health` → `{"status":"UP","service":"codeops-server","timestamp":"..."}`

---

## 6. Entity / Data Model Layer

### Core Module (25 entities + 1 superclass + 1 embeddable)

---

### === BaseEntity.java ===
Table: N/A (mapped superclass)
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - id: UUID [@Id, @GeneratedValue(UUID)]
  - createdAt: Instant [@Column(updatable=false)]
  - updatedAt: Instant

Auditing: @PrePersist sets createdAt+updatedAt, @PreUpdate sets updatedAt. No @Version.

---

### === User.java ===
Table: `users`
Primary Key: inherited UUID from BaseEntity

Fields:
  - email: String(255) [unique, not null]
  - passwordHash: String(255) [not null]
  - displayName: String(100) [not null]
  - avatarUrl: String(500) [nullable]
  - isActive: boolean [default true]
  - lastLoginAt: Instant [nullable]
  - mfaEnabled: boolean [default false]
  - mfaMethod: MfaMethod [default NONE]
  - mfaSecret: String(500) [nullable]
  - mfaRecoveryCodes: String(2000) [nullable]

Relationships: None outgoing
Auditing: createdAt, updatedAt via BaseEntity. No @Version.

---

### === Team.java ===
Table: `teams`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(100) [not null]
  - description: String(TEXT) [nullable]
  - teamsWebhookUrl: String(500) [nullable]
  - settingsJson: String(TEXT) [nullable]

Relationships: @ManyToOne → User (owner_id)
Auditing: createdAt, updatedAt via BaseEntity. No @Version.

---

### === TeamMember.java ===
Table: `team_members`
Primary Key: inherited UUID from BaseEntity

Fields:
  - role: TeamRole [@Enumerated(STRING)]
  - joinedAt: Instant

Relationships: @ManyToOne → Team (team_id), @ManyToOne → User (user_id)
Unique Constraints: (team_id, user_id)
Indexes: idx_tm_team_id, idx_tm_user_id

---

### === Project.java ===
Table: `projects`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - description: String(TEXT) [nullable]
  - repoUrl: String(500) [nullable]
  - repoFullName: String(200) [nullable]
  - defaultBranch: String [default "main"]
  - jiraProjectKey: String(20) [nullable]
  - jiraDefaultIssueType: String [default "Task"]
  - jiraLabels: String(TEXT) [nullable]
  - jiraComponent: String(100) [nullable]
  - techStack: String(200) [nullable]
  - healthScore: Integer [nullable]
  - lastAuditAt: Instant [nullable]
  - settingsJson: String(TEXT) [nullable]
  - isArchived: boolean [default false]

Relationships: @ManyToOne → Team (team_id), @ManyToOne → GitHubConnection (nullable), @ManyToOne → JiraConnection (nullable), @ManyToOne → User (created_by)

---

### === QaJob.java ===
Table: `qa_jobs`
Primary Key: inherited UUID from BaseEntity
Optimistic Locking: @Version present

Fields:
  - mode: JobMode [@Enumerated(STRING)]
  - status: JobStatus [@Enumerated(STRING)]
  - name: String(200) [nullable]
  - branch: String(100) [nullable]
  - configJson: String(TEXT) [nullable]
  - summaryMd: String(TEXT) [nullable]
  - overallResult: JobResult [nullable]
  - healthScore: Integer [nullable]
  - totalFindings: Integer [default 0]
  - criticalCount: Integer [default 0]
  - highCount: Integer [default 0]
  - mediumCount: Integer [default 0]
  - lowCount: Integer [default 0]
  - jiraTicketKey: String(50) [nullable]
  - startedAt: Instant [nullable]
  - completedAt: Instant [nullable]
  - version: Long [@Version]

Relationships: @ManyToOne → Project (project_id), @ManyToOne → User (started_by)

---

### === AgentRun.java ===
Table: `agent_runs`
Primary Key: inherited UUID from BaseEntity
Optimistic Locking: @Version present

Fields:
  - agentType: AgentType [@Enumerated(STRING)]
  - status: AgentStatus [@Enumerated(STRING)]
  - result: AgentResult [nullable]
  - reportS3Key: String(500) [nullable]
  - score: Integer [nullable]
  - findingsCount: Integer [default 0]
  - criticalCount: Integer [default 0]
  - highCount: Integer [default 0]
  - startedAt: Instant [nullable]
  - completedAt: Instant [nullable]
  - version: Long [@Version]

Relationships: @ManyToOne → QaJob (job_id)

---

### === Finding.java ===
Table: `findings`
Primary Key: inherited UUID from BaseEntity
Optimistic Locking: @Version present

Fields:
  - agentType: AgentType [@Enumerated(STRING)]
  - severity: Severity [@Enumerated(STRING)]
  - title: String(500)
  - description: String(TEXT) [nullable]
  - filePath: String(500) [nullable]
  - lineNumber: Integer [nullable]
  - recommendation: String(TEXT) [nullable]
  - evidence: String(TEXT) [nullable]
  - effortEstimate: Effort [nullable]
  - debtCategory: DebtCategory [nullable]
  - status: FindingStatus [default OPEN]
  - statusChangedAt: Instant [nullable]
  - version: Long [@Version]

Relationships: @ManyToOne → QaJob (job_id), @ManyToOne → User (status_changed_by, nullable)
Indexes: idx_finding_job_id, idx_finding_status

---

### === RemediationTask.java ===
Table: `remediation_tasks`
Primary Key: inherited UUID from BaseEntity
Optimistic Locking: @Version present

Fields:
  - taskNumber: Integer
  - title: String(500)
  - description: String(TEXT) [nullable]
  - promptMd: String(TEXT) [nullable]
  - promptS3Key: String(500) [nullable]
  - priority: Priority [@Enumerated(STRING)]
  - status: TaskStatus [default PENDING]
  - jiraKey: String(50) [nullable]
  - version: Long [@Version]

Relationships: @ManyToOne → QaJob (job_id), @ManyToMany → Finding (via remediation_task_findings join table), @ManyToOne → User (assigned_to, nullable)

---

### === BugInvestigation.java ===
Table: `bug_investigations`
Primary Key: inherited UUID from BaseEntity

Fields:
  - jiraKey: String(50) [nullable]
  - jiraSummary: String(TEXT) [nullable]
  - jiraDescription: String(TEXT) [nullable]
  - jiraCommentsJson: String(TEXT) [nullable]
  - jiraAttachmentsJson: String(TEXT) [nullable]
  - jiraLinkedIssues: String(TEXT) [nullable]
  - additionalContext: String(TEXT) [nullable]
  - rcaMd: String(TEXT) [nullable]
  - impactAssessmentMd: String(TEXT) [nullable]
  - rcaS3Key: String(500) [nullable]
  - rcaPostedToJira: boolean [default false]
  - fixTasksCreatedInJira: boolean [default false]

Relationships: @ManyToOne → QaJob (job_id)

---

### === ComplianceItem.java ===
Table: `compliance_items`
Primary Key: inherited UUID from BaseEntity

Fields:
  - requirement: String(TEXT) [not null]
  - status: ComplianceStatus [@Enumerated(STRING)]
  - evidence: String(TEXT) [nullable]
  - agentType: AgentType [nullable]
  - notes: String(TEXT) [nullable]

Relationships: @ManyToOne → QaJob (job_id), @ManyToOne → Specification (spec_id, nullable)

---

### === Specification.java ===
Table: `specifications`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - specType: SpecType [@Enumerated(STRING)]
  - s3Key: String(500) [not null]

Relationships: @ManyToOne → QaJob (job_id)

---

### === TechDebtItem.java ===
Table: `tech_debt_items`
Primary Key: inherited UUID from BaseEntity
Optimistic Locking: @Version present

Fields:
  - category: DebtCategory [@Enumerated(STRING)]
  - title: String(500)
  - description: String(TEXT) [nullable]
  - filePath: String(500) [nullable]
  - effortEstimate: Effort [nullable]
  - businessImpact: BusinessImpact [nullable]
  - status: DebtStatus [default IDENTIFIED]
  - version: Long [@Version]

Relationships: @ManyToOne → Project (project_id), @ManyToOne → QaJob (first_detected_job_id, nullable), @ManyToOne → QaJob (resolved_job_id, nullable)

---

### === DependencyScan.java ===
Table: `dependency_scans`
Primary Key: inherited UUID from BaseEntity

Fields:
  - manifestFile: String(200) [nullable]
  - totalDependencies: Integer [nullable]
  - outdatedCount: Integer [nullable]
  - vulnerableCount: Integer [nullable]
  - scanDataJson: String(TEXT) [nullable]

Relationships: @ManyToOne → Project (project_id), @ManyToOne → QaJob (job_id, nullable)

---

### === DependencyVulnerability.java ===
Table: `dependency_vulnerabilities`
Primary Key: inherited UUID from BaseEntity

Fields:
  - dependencyName: String(200)
  - currentVersion: String(50) [nullable]
  - fixedVersion: String(50) [nullable]
  - cveId: String(30) [nullable]
  - severity: Severity [@Enumerated(STRING)]
  - description: String(TEXT) [nullable]
  - status: VulnerabilityStatus [default OPEN]

Relationships: @ManyToOne → DependencyScan (scan_id)

---

### === HealthSnapshot.java ===
Table: `health_snapshots`
Primary Key: inherited UUID from BaseEntity

Fields:
  - healthScore: Integer [not null]
  - findingsBySeverity: String(TEXT) [nullable]
  - techDebtScore: Integer [nullable]
  - dependencyScore: Integer [nullable]
  - testCoveragePercent: BigDecimal(5,2) [nullable]
  - capturedAt: Instant [not null]

Relationships: @ManyToOne → Project (project_id), @ManyToOne → QaJob (job_id, nullable)

---

### === HealthSchedule.java ===
Table: `health_schedules`
Primary Key: inherited UUID from BaseEntity

Fields:
  - scheduleType: ScheduleType [@Enumerated(STRING)]
  - cronExpression: String(50) [nullable]
  - agentTypes: String(TEXT) [not null]
  - isActive: boolean [default true]
  - lastRunAt: Instant [nullable]
  - nextRunAt: Instant [nullable]

Relationships: @ManyToOne → Project (project_id), @ManyToOne → User (created_by)

---

### === Directive.java ===
Table: `directives`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - description: String(TEXT) [nullable]
  - contentMd: String(TEXT) [not null]
  - category: DirectiveCategory [nullable]
  - scope: DirectiveScope [@Enumerated(STRING)]
  - version: Integer [default 1]

Relationships: @ManyToOne → Team (team_id, nullable), @ManyToOne → Project (project_id, nullable), @ManyToOne → User (created_by)

---

### === ProjectDirective.java ===
Table: `project_directives`
Primary Key: ProjectDirectiveId (composite: projectId + directiveId) — does NOT extend BaseEntity

Fields:
  - id: ProjectDirectiveId (composite)
  - enabled: boolean [default true]

Relationships: @ManyToOne → Project (@MapsId), @ManyToOne → Directive (@MapsId)

---

### === Persona.java ===
Table: `personas`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(100) [not null]
  - agentType: AgentType [@Enumerated(STRING)]
  - description: String(TEXT) [nullable]
  - contentMd: String(TEXT) [not null]
  - scope: Scope [@Enumerated(STRING)]
  - isDefault: boolean [default false]
  - version: Integer [default 1]

Relationships: @ManyToOne → Team (team_id, nullable), @ManyToOne → User (created_by)

---

### === GitHubConnection.java ===
Table: `github_connections`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(100) [not null]
  - authType: GitHubAuthType [@Enumerated(STRING)]
  - encryptedCredentials: String(TEXT) [not null]
  - githubUsername: String(100) [nullable]
  - isActive: boolean [default true]

Relationships: @ManyToOne → Team (team_id), @ManyToOne → User (created_by)

---

### === JiraConnection.java ===
Table: `jira_connections`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(100) [not null]
  - instanceUrl: String(500) [not null]
  - email: String(255) [not null]
  - encryptedApiToken: String(TEXT) [not null]
  - isActive: boolean [default true]

Relationships: @ManyToOne → Team (team_id), @ManyToOne → User (created_by)

---

### === Invitation.java ===
Table: `invitations`
Primary Key: inherited UUID from BaseEntity

Fields:
  - email: String(255) [not null]
  - role: TeamRole [@Enumerated(STRING)]
  - token: String(100) [unique, not null]
  - status: InvitationStatus [@Enumerated(STRING)]
  - expiresAt: Instant [not null]

Relationships: @ManyToOne → Team (team_id), @ManyToOne → User (invited_by)

---

### === NotificationPreference.java ===
Table: `notification_preferences`
Primary Key: inherited UUID from BaseEntity

Fields:
  - eventType: String(50) [not null]
  - inApp: boolean [default true]
  - email: boolean [default false]

Relationships: @ManyToOne → User (user_id)
Unique Constraints: (user_id, event_type)

---

### === AuditLog.java ===
Table: `audit_log` — does NOT extend BaseEntity
Primary Key: `id` Long (IDENTITY)

Fields:
  - id: Long [@Id, @GeneratedValue(IDENTITY)]
  - action: String(50)
  - entityType: String(30) [nullable]
  - entityId: UUID [nullable]
  - details: String(TEXT) [nullable]
  - ipAddress: String(45) [nullable]
  - createdAt: Instant

Relationships: @ManyToOne → User (user_id, nullable), @ManyToOne → Team (team_id, nullable)

---

### === SystemSetting.java ===
Table: `system_settings` — does NOT extend BaseEntity
Primary Key: `settingKey` String(100)

Fields:
  - settingKey: String(100) [@Id]
  - value: String(TEXT)
  - updatedAt: Instant

Relationships: @ManyToOne → User (updated_by, nullable)

---

### === MfaEmailCode.java ===
Table: `mfa_email_codes` — does NOT extend BaseEntity
Primary Key: UUID

Fields:
  - id: UUID [@Id, @GeneratedValue(UUID)]
  - userId: UUID [not null, raw — no JPA FK]
  - codeHash: String(255)
  - expiresAt: Instant
  - used: boolean [default false]
  - createdAt: Instant

Relationships: None (userId is raw UUID)

---

### Courier Module (18 entities)

---

### === Collection.java (Courier) ===
Table: `collections`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [not null, raw]
  - name: String(200) [not null]
  - description: String(2000) [nullable]
  - preRequestScript: String(TEXT) [nullable]
  - postResponseScript: String(TEXT) [nullable]
  - authType: AuthType [nullable]
  - authConfig: String(TEXT) [nullable]
  - isShared: boolean [default false]
  - createdBy: UUID [raw]

Relationships: @OneToMany → Folder, @OneToMany → EnvironmentVariable
Unique Constraints: (team_id, name)

---

### === Folder.java (Courier) ===
Table: `folders`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - description: String(2000) [nullable]
  - sortOrder: int [default 0]
  - preRequestScript: String(TEXT) [nullable]
  - postResponseScript: String(TEXT) [nullable]
  - authType: AuthType [nullable]
  - authConfig: String(TEXT) [nullable]

Relationships: @ManyToOne → Collection, @ManyToOne → Folder (self-referential parent, nullable), @OneToMany → Folder (subFolders), @OneToMany → Request

---

### === Request.java (Courier) ===
Table: `requests`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - description: String(2000) [nullable]
  - method: HttpMethod [@Enumerated(STRING)]
  - url: String(2000) [not null]
  - sortOrder: int [default 0]

Relationships: @ManyToOne → Folder, @OneToMany → RequestHeader, @OneToMany → RequestParam, @OneToOne → RequestBody, @OneToOne → RequestAuth, @OneToMany → RequestScript

---

### === RequestHeader.java (Courier) ===
Table: `request_headers`
Primary Key: inherited UUID from BaseEntity

Fields:
  - headerKey: String(500) [not null]
  - headerValue: String(5000) [nullable]
  - description: String(500) [nullable]
  - isEnabled: boolean [default true]

Relationships: @ManyToOne → Request

---

### === RequestParam.java (Courier) ===
Table: `request_params`
Primary Key: inherited UUID from BaseEntity

Fields:
  - paramKey: String(500) [not null]
  - paramValue: String(5000) [nullable]
  - description: String(500) [nullable]
  - isEnabled: boolean [default true]

Relationships: @ManyToOne → Request

---

### === RequestBody.java (Courier) ===
Table: `request_bodies`
Primary Key: inherited UUID from BaseEntity

Fields:
  - bodyType: BodyType [@Enumerated(STRING)]
  - rawContent: String(TEXT) [nullable]
  - formData: String(TEXT) [nullable]
  - graphqlQuery: String(TEXT) [nullable]
  - graphqlVariables: String(TEXT) [nullable]
  - binaryFileName: String(500) [nullable]

Relationships: @OneToOne → Request

---

### === RequestAuth.java (Courier) ===
Table: `request_auths`
Primary Key: inherited UUID from BaseEntity

Fields:
  - authType: AuthType [@Enumerated(STRING)]
  - apiKeyKey: String(500) [nullable]
  - apiKeyValue: String(5000) [nullable]
  - apiKeyAddTo: String(20) [nullable]
  - bearerToken: String(5000) [nullable]
  - basicUsername: String(500) [nullable]
  - basicPassword: String(5000) [nullable]
  - oauth2GrantType: String(50) [nullable]
  - oauth2AccessTokenUrl: String(2000) [nullable]
  - oauth2AuthUrl: String(2000) [nullable]
  - oauth2ClientId: String(500) [nullable]
  - oauth2ClientSecret: String(5000) [nullable]
  - oauth2Scope: String(1000) [nullable]
  - oauth2State: String(500) [nullable]
  - oauth2CallbackUrl: String(2000) [nullable]
  - jwtPayload: String(TEXT) [nullable]
  - jwtSecret: String(5000) [nullable]
  - jwtAlgorithm: String(20) [nullable]

Relationships: @OneToOne → Request

---

### === RequestScript.java (Courier) ===
Table: `request_scripts`
Primary Key: inherited UUID from BaseEntity

Fields:
  - scriptType: ScriptType [@Enumerated(STRING)]
  - content: String(TEXT) [nullable]

Relationships: @ManyToOne → Request
Unique Constraints: (request_id, script_type)

---

### === Environment.java (Courier) ===
Table: `environments`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [raw]
  - name: String(200) [not null]
  - description: String(2000) [nullable]
  - isActive: boolean
  - createdBy: UUID [raw]

Relationships: @OneToMany → EnvironmentVariable
Unique Constraints: (team_id, name)

---

### === EnvironmentVariable.java (Courier) ===
Table: `environment_variables`
Primary Key: inherited UUID from BaseEntity

Fields:
  - variableKey: String(500) [not null]
  - variableValue: String(5000) [nullable]
  - isSecret: boolean [default false]
  - isEnabled: boolean [default true]
  - scope: String(20) [not null]

Relationships: @ManyToOne → Environment (nullable), @ManyToOne → Collection (nullable)

---

### === GlobalVariable.java (Courier) ===
Table: `global_variables`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [raw]
  - variableKey: String(500) [not null]
  - variableValue: String(5000) [nullable]
  - isSecret: boolean [default false]
  - isEnabled: boolean [default true]

Unique Constraints: (team_id, variable_key)

---

### === RequestHistory.java (Courier) ===
Table: `request_history`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [raw]
  - userId: UUID [raw]
  - requestMethod: HttpMethod [@Enumerated(STRING)]
  - requestUrl: String(2000)
  - requestHeaders: String(TEXT) [nullable]
  - requestBody: String(TEXT) [nullable]
  - responseHeaders: String(TEXT) [nullable]
  - responseBody: String(TEXT) [nullable]
  - responseStatus: Integer [nullable]
  - responseSizeBytes: Long [nullable]
  - responseTimeMs: Long [nullable]
  - contentType: String(200) [nullable]
  - collectionId: UUID [nullable, raw]
  - requestId: UUID [nullable, raw]
  - environmentId: UUID [nullable, raw]

---

### === RunResult.java (Courier) ===
Table: `run_results`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [raw]
  - collectionId: UUID [raw]
  - environmentId: UUID [nullable, raw]
  - status: RunStatus [@Enumerated(STRING)]
  - totalRequests: int
  - passedRequests: int
  - failedRequests: int
  - totalAssertions: int
  - passedAssertions: int
  - failedAssertions: int
  - totalDurationMs: long
  - iterationCount: int [default 1]
  - delayBetweenRequestsMs: int [default 0]
  - dataFilename: String(500) [nullable]
  - startedAt: Instant
  - completedAt: Instant [nullable]
  - startedByUserId: UUID [raw]

Relationships: @OneToMany → RunIteration

---

### === RunIteration.java (Courier) ===
Table: `run_iterations`
Primary Key: inherited UUID from BaseEntity

Fields:
  - iterationNumber: int
  - requestName: String(200)
  - requestMethod: HttpMethod [@Enumerated(STRING)]
  - requestUrl: String(2000)
  - responseStatus: Integer [nullable]
  - responseTimeMs: Long [nullable]
  - responseSizeBytes: Long [nullable]
  - passed: boolean [default true]
  - assertionResults: String(TEXT) [nullable]
  - errorMessage: String(TEXT) [nullable]
  - requestData: String(TEXT) [nullable]
  - responseData: String(TEXT) [nullable]

Relationships: @ManyToOne → RunResult

---

### === CollectionShare.java (Courier) ===
Table: `collection_shares`
Primary Key: inherited UUID from BaseEntity

Fields:
  - permission: SharePermission [@Enumerated(STRING)]
  - sharedWithUserId: UUID [raw]
  - sharedByUserId: UUID [raw]

Relationships: @ManyToOne → Collection
Unique Constraints: (collection_id, shared_with_user_id)

---

### === Fork.java (Courier) ===
Table: `forks`
Primary Key: inherited UUID from BaseEntity

Fields:
  - forkedByUserId: UUID [raw]
  - forkedAt: Instant
  - label: String(200) [nullable]

Relationships: @ManyToOne → Collection (source), @OneToOne → Collection (forked)

---

### === MergeRequest.java (Courier) ===
Table: `merge_requests`
Primary Key: inherited UUID from BaseEntity

Fields:
  - title: String(200) [not null]
  - description: String(5000) [nullable]
  - status: String(20)
  - requestedByUserId: UUID [raw]
  - reviewedByUserId: UUID [nullable, raw]
  - mergedAt: Instant [nullable]
  - conflictDetails: String(TEXT) [nullable]

Relationships: @ManyToOne → Fork (source), @ManyToOne → Collection (target)

---

### === CodeSnippetTemplate.java (Courier) ===
Table: `code_snippet_templates`
Primary Key: inherited UUID from BaseEntity

Fields:
  - language: CodeLanguage [@Enumerated(STRING), unique]
  - displayName: String(100) [not null]
  - templateContent: String(TEXT) [not null]
  - fileExtension: String(20) [not null]
  - contentType: String(100) [nullable]

---

### Fleet Module (14 entities)

---

### === ServiceProfile.java (Fleet) ===
Table: `fleet_service_profiles`
Primary Key: inherited UUID from BaseEntity

Fields:
  - serviceName: String(200) [not null]
  - displayName: String(200) [nullable]
  - description: String(TEXT) [nullable]
  - imageName: String(500) [not null]
  - imageTag: String(100) [default "latest"]
  - command: String [nullable]
  - entrypoint: String [nullable]
  - workingDir: String [nullable]
  - healthCheckCommand: String(TEXT) [nullable]
  - healthCheckIntervalSeconds: Integer [default 30]
  - healthCheckTimeoutSeconds: Integer [default 10]
  - healthCheckRetries: Integer [default 3]
  - restartPolicy: RestartPolicy [default UNLESS_STOPPED]
  - memoryLimitMb: Integer [nullable]
  - cpuLimit: Double [nullable]
  - startOrder: int [default 0]
  - isAutoGenerated: boolean [default false]
  - isEnabled: boolean [default true]

Relationships: @ManyToOne → ServiceRegistration (nullable), @ManyToOne → Team, @OneToMany → VolumeMount, @OneToMany → NetworkConfig, @OneToMany → PortMapping, @OneToMany → EnvironmentVariable (Fleet)
Unique Constraints: (team_id, service_name)

---

### === ContainerInstance.java (Fleet) ===
Table: `fleet_container_instances`
Primary Key: inherited UUID from BaseEntity

Fields:
  - containerId: String(64) [nullable]
  - containerName: String(200) [not null]
  - serviceName: String(200) [not null]
  - imageName: String(500)
  - imageTag: String(100)
  - status: ContainerStatus [default CREATED]
  - healthStatus: HealthStatus [default NONE]
  - restartPolicy: RestartPolicy [default NO]
  - restartCount: int [default 0]
  - exitCode: Integer [nullable]
  - cpuPercent: Double [nullable]
  - memoryBytes: Long [nullable]
  - memoryLimitBytes: Long [nullable]
  - pid: Integer [nullable]
  - startedAt: Instant [nullable]
  - finishedAt: Instant [nullable]
  - errorMessage: String(TEXT) [nullable]

Relationships: @ManyToOne → ServiceProfile (nullable), @ManyToOne → Team

---

### === PortMapping.java (Fleet) ===
Table: `fleet_port_mappings`
Primary Key: inherited UUID from BaseEntity

Fields:
  - hostPort: int
  - containerPort: int
  - protocol: String [default "tcp"]

Relationships: @ManyToOne → ServiceProfile

---

### === VolumeMount.java (Fleet) ===
Table: `fleet_volume_mounts`
Primary Key: inherited UUID from BaseEntity

Fields:
  - hostPath: String [nullable]
  - volumeName: String [nullable]
  - containerPath: String(500) [not null]
  - isReadOnly: boolean [default false]

Relationships: @ManyToOne → ServiceProfile

---

### === NetworkConfig.java (Fleet) ===
Table: `fleet_network_configs`
Primary Key: inherited UUID from BaseEntity

Fields:
  - networkName: String(200) [not null]
  - aliases: String(TEXT) [nullable]
  - ipAddress: String(45) [nullable]

Relationships: @ManyToOne → ServiceProfile

---

### === EnvironmentVariable.java (Fleet) ===
Entity class: `FleetEnvironmentVariable`
Table: `fleet_environment_variables`
Primary Key: inherited UUID from BaseEntity

Fields:
  - variableKey: String(200) [not null]
  - variableValue: String(TEXT) [not null]
  - isSecret: boolean [default false]

Relationships: @ManyToOne → ServiceProfile

---

### === ContainerHealthCheck.java (Fleet) ===
Table: `fleet_container_health_checks`
Primary Key: inherited UUID from BaseEntity

Fields:
  - status: HealthStatus [@Enumerated(STRING)]
  - output: String(TEXT) [nullable]
  - exitCode: Integer [nullable]
  - durationMs: Long [nullable]

Relationships: @ManyToOne → ContainerInstance

---

### === ContainerLog.java (Fleet) ===
Table: `fleet_container_logs`
Primary Key: inherited UUID from BaseEntity

Fields:
  - stream: String(10) [not null]
  - content: String(TEXT) [not null]
  - timestamp: Instant [not null]

Relationships: @ManyToOne → ContainerInstance

---

### === DeploymentRecord.java (Fleet) ===
Table: `fleet_deployment_records`
Primary Key: inherited UUID from BaseEntity

Fields:
  - action: DeploymentAction [@Enumerated(STRING)]
  - description: String(TEXT) [nullable]
  - serviceCount: int
  - successCount: int
  - failureCount: int
  - durationMs: Long [nullable]

Relationships: @ManyToOne → User (triggeredBy), @ManyToOne → Team, @OneToMany → DeploymentContainer

---

### === DeploymentContainer.java (Fleet) ===
Table: `fleet_deployment_containers`
Primary Key: inherited UUID from BaseEntity

Fields:
  - success: boolean
  - errorMessage: String(TEXT) [nullable]

Relationships: @ManyToOne → DeploymentRecord, @ManyToOne → ContainerInstance

---

### === SolutionProfile.java (Fleet) ===
Table: `fleet_solution_profiles`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - description: String(TEXT) [nullable]
  - isDefault: boolean [default false]

Relationships: @ManyToOne → Team, @OneToMany → SolutionService
Unique Constraints: (team_id, name)

---

### === SolutionService.java (Fleet) ===
Table: `fleet_solution_services`
Primary Key: inherited UUID from BaseEntity

Fields:
  - startOrder: int

Relationships: @ManyToOne → SolutionProfile, @ManyToOne → ServiceProfile

---

### === WorkstationProfile.java (Fleet) ===
Entity class: `FleetWorkstationProfile`
Table: `fleet_workstation_profiles`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - description: String(TEXT) [nullable]
  - isDefault: boolean [default false]

Relationships: @ManyToOne → User, @ManyToOne → Team, @OneToMany → WorkstationSolution

---

### === WorkstationSolution.java (Fleet) ===
Table: `fleet_workstation_solutions`
Primary Key: inherited UUID from BaseEntity

Fields:
  - startOrder: int
  - overrideEnvVarsJson: String(TEXT) [nullable]

Relationships: @ManyToOne → WorkstationProfile, @ManyToOne → SolutionProfile

---

### Logger Module (16 entities)

---

### === LogEntry.java ===
Table: `log_entries`
Primary Key: inherited UUID from BaseEntity

Fields:
  - level: LogLevel [@Enumerated(STRING)]
  - message: String(TEXT) [not null]
  - timestamp: Instant [not null]
  - serviceName: String(200) [not null]
  - correlationId: String [nullable]
  - traceId: String [nullable]
  - spanId: String [nullable]
  - loggerName: String [nullable]
  - threadName: String [nullable]
  - exceptionClass: String [nullable]
  - exceptionMessage: String [nullable]
  - stackTrace: String [nullable]
  - customFields: String(TEXT) [nullable]
  - hostName: String [nullable]
  - ipAddress: String [nullable]
  - teamId: UUID [raw]

Relationships: @ManyToOne → LogSource (nullable)
Indexes: Composite (service_name, level, timestamp)

---

### === LogSource.java ===
Table: `log_sources`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - serviceId: UUID [nullable]
  - description: String [nullable]
  - environment: String(50) [nullable]
  - isActive: boolean [default true]
  - teamId: UUID [raw]
  - lastLogReceivedAt: Instant [nullable]
  - logCount: Long [default 0]

---

### === TraceSpan.java ===
Table: `trace_spans`
Primary Key: inherited UUID from BaseEntity

Fields:
  - correlationId: String [not null]
  - traceId: String [not null]
  - spanId: String [not null]
  - parentSpanId: String [nullable]
  - serviceName: String [not null]
  - operationName: String [not null]
  - startTime: Instant [not null]
  - endTime: Instant [nullable]
  - durationMs: Long [nullable]
  - status: SpanStatus [default OK]
  - statusMessage: String [nullable]
  - tags: String [nullable]
  - teamId: UUID [raw]

---

### === Metric.java ===
Table: `metrics`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - metricType: MetricType [@Enumerated(STRING)]
  - description: String [nullable]
  - unit: String [nullable]
  - serviceName: String(200) [not null]
  - tags: String [nullable]
  - teamId: UUID [raw]

Unique Constraints: (name, service_name, team_id)

---

### === MetricSeries.java ===
Table: `metric_series`
Primary Key: inherited UUID from BaseEntity

Fields:
  - timestamp: Instant [not null]
  - value: Double [not null]
  - tags: String [nullable]
  - resolution: int [not null]

Relationships: @ManyToOne → Metric

---

### === LogTrap.java ===
Table: `log_traps`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - description: String [nullable]
  - trapType: TrapType [@Enumerated(STRING)]
  - isActive: boolean [default true]
  - teamId: UUID [raw]
  - createdBy: UUID [raw]
  - lastTriggeredAt: Instant [nullable]
  - triggerCount: Long [default 0]

Relationships: @OneToMany → TrapCondition

---

### === TrapCondition.java ===
Table: `trap_conditions`
Primary Key: inherited UUID from BaseEntity

Fields:
  - conditionType: ConditionType [@Enumerated(STRING)]
  - field: String(100) [not null]
  - pattern: String [nullable]
  - threshold: Integer [nullable]
  - windowSeconds: Integer [nullable]
  - serviceName: String [nullable]
  - logLevel: LogLevel [nullable]

Relationships: @ManyToOne → LogTrap

---

### === AlertRule.java ===
Table: `alert_rules`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - severity: AlertSeverity [@Enumerated(STRING)]
  - isActive: boolean [default true]
  - throttleMinutes: int [default 5]
  - teamId: UUID [raw]

Relationships: @ManyToOne → LogTrap, @ManyToOne → AlertChannel

---

### === AlertChannel.java ===
Table: `alert_channels`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - channelType: AlertChannelType [@Enumerated(STRING)]
  - configuration: String(TEXT) [not null]
  - isActive: boolean [default true]
  - teamId: UUID [raw]
  - createdBy: UUID [raw]

---

### === AlertHistory.java ===
Table: `alert_history`
Primary Key: inherited UUID from BaseEntity

Fields:
  - severity: AlertSeverity [@Enumerated(STRING)]
  - status: AlertStatus [default FIRED]
  - message: String [nullable]
  - acknowledgedBy: UUID [nullable, raw]
  - resolvedBy: UUID [nullable, raw]
  - acknowledgedAt: Instant [nullable]
  - resolvedAt: Instant [nullable]
  - teamId: UUID [raw]

Relationships: @ManyToOne → AlertRule, @ManyToOne → LogTrap, @ManyToOne → AlertChannel

---

### === Dashboard.java ===
Table: `dashboards`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - description: String [nullable]
  - teamId: UUID [raw]
  - createdBy: UUID [raw]
  - isShared: boolean [default true]
  - isTemplate: boolean [default false]
  - refreshIntervalSeconds: int [default 30]
  - layoutJson: String [nullable]

Relationships: @OneToMany → DashboardWidget

---

### === DashboardWidget.java ===
Table: `dashboard_widgets`
Primary Key: inherited UUID from BaseEntity

Fields:
  - title: String(200) [not null]
  - widgetType: WidgetType [@Enumerated(STRING)]
  - queryJson: String [nullable]
  - configJson: String [nullable]
  - gridX: int
  - gridY: int
  - gridWidth: int
  - gridHeight: int
  - sortOrder: int

Relationships: @ManyToOne → Dashboard

---

### === SavedQuery.java ===
Table: `saved_queries`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - description: String [nullable]
  - queryJson: String(TEXT) [not null]
  - queryDsl: String [nullable]
  - teamId: UUID [raw]
  - createdBy: UUID [raw]
  - isShared: boolean [default false]
  - lastExecutedAt: Instant [nullable]
  - executionCount: Long [default 0]

---

### === QueryHistory.java ===
Table: `query_history`
Primary Key: inherited UUID from BaseEntity

Fields:
  - queryJson: String(TEXT) [not null]
  - queryDsl: String [nullable]
  - resultCount: Long
  - executionTimeMs: Long
  - teamId: UUID [raw]
  - createdBy: UUID [raw]

---

### === RetentionPolicy.java ===
Table: `retention_policies`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - sourceName: String [nullable]
  - logLevel: LogLevel [nullable]
  - retentionDays: int [not null]
  - action: RetentionAction [@Enumerated(STRING)]
  - archiveDestination: String [nullable]
  - isActive: boolean [default true]
  - teamId: UUID [raw]
  - createdBy: UUID [raw]
  - lastExecutedAt: Instant [nullable]

---

### === AnomalyBaseline.java ===
Table: `anomaly_baselines`
Primary Key: inherited UUID from BaseEntity

Fields:
  - serviceName: String(200) [not null]
  - metricName: String(200) [not null]
  - baselineValue: Double [not null]
  - standardDeviation: Double [not null]
  - sampleCount: Long [not null]
  - windowStartTime: Instant [not null]
  - windowEndTime: Instant [not null]
  - deviationThreshold: Double [default 2.0]
  - isActive: boolean [default true]
  - teamId: UUID [raw]
  - lastComputedAt: Instant [nullable]

---

### MCP Module (8 entities)

---

### === DeveloperProfile.java (MCP) ===
Table: `mcp_developer_profiles`
Primary Key: inherited UUID from BaseEntity

Fields:
  - displayName: String [nullable]
  - bio: String [nullable]
  - defaultEnvironment: Environment [nullable]
  - preferencesJson: String [nullable]
  - timezone: String [nullable]
  - isActive: boolean [default true]

Relationships: @ManyToOne → Team, @ManyToOne → User, @OneToMany → McpApiToken, @OneToMany → McpSession
Unique Constraints: (team_id, user_id)

---

### === McpApiToken.java (MCP) ===
Table: `mcp_api_tokens`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(200) [not null]
  - tokenHash: String(500) [unique, not null]
  - tokenPrefix: String(10) [not null]
  - status: TokenStatus [default ACTIVE]
  - lastUsedAt: Instant [nullable]
  - expiresAt: Instant [nullable]
  - scopesJson: String [nullable]

Relationships: @ManyToOne → DeveloperProfile

---

### === McpSession.java (MCP) ===
Table: `mcp_sessions`
Primary Key: inherited UUID from BaseEntity

Fields:
  - status: SessionStatus [default INITIALIZING]
  - environment: Environment [@Enumerated(STRING)]
  - transport: McpTransport [@Enumerated(STRING)]
  - startedAt: Instant [nullable]
  - completedAt: Instant [nullable]
  - lastActivityAt: Instant [nullable]
  - timeoutMinutes: int [default 120]
  - totalToolCalls: int [default 0]
  - errorMessage: String [nullable]

Relationships: @ManyToOne → DeveloperProfile, @ManyToOne → Project, @OneToMany → SessionToolCall, @OneToOne → SessionResult

---

### === SessionToolCall.java (MCP) ===
Table: `mcp_session_tool_calls`
Primary Key: inherited UUID from BaseEntity

Fields:
  - toolName: String(200) [not null]
  - toolCategory: String(100) [not null]
  - requestJson: String [nullable]
  - responseJson: String [nullable]
  - errorMessage: String [nullable]
  - status: ToolCallStatus [@Enumerated(STRING)]
  - durationMs: long
  - calledAt: Instant

Relationships: @ManyToOne → McpSession

---

### === SessionResult.java (MCP) ===
Table: `mcp_session_results`
Primary Key: inherited UUID from BaseEntity

Fields:
  - summary: String(TEXT) [not null]
  - commitHashesJson: String [nullable]
  - filesChangedJson: String [nullable]
  - endpointsChangedJson: String [nullable]
  - dependencyChangesJson: String [nullable]
  - testsAdded: int
  - linesAdded: int
  - linesRemoved: int
  - durationMinutes: int
  - testCoverage: Double [nullable]
  - tokenUsage: Long [nullable]

Relationships: @OneToOne → McpSession

---

### === ProjectDocument.java (MCP) ===
Table: `mcp_project_documents`
Primary Key: inherited UUID from BaseEntity

Fields:
  - documentType: DocumentType [@Enumerated(STRING)]
  - customName: String [nullable]
  - currentContent: String [nullable]
  - lastAuthorType: AuthorType [nullable]
  - lastSessionId: UUID [nullable, raw]
  - isFlagged: boolean [default false]
  - flagReason: String [nullable]

Relationships: @ManyToOne → Project, @ManyToOne → User (nullable), @OneToMany → ProjectDocumentVersion
Unique Constraints: (project_id, document_type, custom_name)

---

### === ProjectDocumentVersion.java (MCP) ===
Table: `mcp_project_document_versions`
Primary Key: inherited UUID from BaseEntity

Fields:
  - versionNumber: int
  - content: String(TEXT) [not null]
  - authorType: AuthorType [@Enumerated(STRING)]
  - commitHash: String [nullable]
  - changeDescription: String [nullable]

Relationships: @ManyToOne → ProjectDocument, @ManyToOne → User (nullable), @ManyToOne → McpSession (nullable)

---

### === ActivityFeedEntry.java (MCP) ===
Table: `mcp_activity_feed`
Primary Key: inherited UUID from BaseEntity

Fields:
  - activityType: ActivityType [@Enumerated(STRING)]
  - title: String(500) [not null]
  - detail: String [nullable]
  - sourceModule: String [nullable]
  - sourceEntityId: UUID [nullable]
  - projectName: String [nullable]
  - impactedServiceIdsJson: String [nullable]
  - relayMessageId: String [nullable]

Relationships: @ManyToOne → Team, @ManyToOne → User (nullable), @ManyToOne → Project (nullable), @ManyToOne → McpSession (nullable)

---

### Registry Module (11 entities)

---

### === ServiceRegistration.java (Registry) ===
Table: `service_registrations`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [raw — not JPA FK, cross-module decoupling]
  - name: String(100) [not null]
  - slug: String(63) [not null]
  - serviceType: ServiceType [@Enumerated(STRING)]
  - description: String [nullable]
  - repoUrl: String [nullable]
  - repoFullName: String [nullable]
  - defaultBranch: String [default "main"]
  - techStack: String [nullable]
  - status: ServiceStatus [default ACTIVE]
  - healthCheckUrl: String [nullable]
  - healthCheckIntervalSeconds: int [default 30]
  - lastHealthStatus: HealthStatus [nullable]
  - lastHealthCheckAt: Instant [nullable]
  - environmentsJson: String [nullable]
  - metadataJson: String [nullable]
  - createdByUserId: UUID [raw]

Relationships: @OneToMany → PortAllocation, @OneToMany → ServiceDependency (as source), @OneToMany → ServiceDependency (as target), @OneToMany → ApiRouteRegistration, @OneToMany → SolutionMember, @OneToMany → ConfigTemplate, @OneToMany → EnvironmentConfig
Unique Constraints: (team_id, slug)
Note: teamId is raw UUID — not a JPA FK — for cross-module decoupling.

---

### === ServiceDependency.java (Registry) ===
Table: `service_dependencies`
Primary Key: inherited UUID from BaseEntity

Fields:
  - dependencyType: DependencyType [@Enumerated(STRING)]
  - description: String [nullable]
  - targetEndpoint: String [nullable]
  - isRequired: boolean [default true]

Relationships: @ManyToOne → ServiceRegistration (source), @ManyToOne → ServiceRegistration (target)
Unique Constraints: (source, target, type)

---

### === PortAllocation.java (Registry) ===
Table: `port_allocations`
Primary Key: inherited UUID from BaseEntity

Fields:
  - environment: String(50) [not null]
  - portType: PortType [@Enumerated(STRING)]
  - portNumber: int [not null]
  - protocol: String [default "TCP"]
  - description: String [nullable]
  - isAutoAllocated: boolean [default true]
  - allocatedByUserId: UUID [raw]

Relationships: @ManyToOne → ServiceRegistration
Unique Constraints: (service_id, environment, port_number)

---

### === PortRange.java (Registry) ===
Table: `port_ranges`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [raw]
  - portType: PortType [@Enumerated(STRING)]
  - rangeStart: int
  - rangeEnd: int
  - environment: String [not null]
  - description: String [nullable]

Unique Constraints: (team_id, port_type, environment)

---

### === ApiRouteRegistration.java (Registry) ===
Table: `api_route_registrations`
Primary Key: inherited UUID from BaseEntity

Fields:
  - routePrefix: String(200) [not null]
  - httpMethods: String [nullable]
  - environment: String(50) [not null]
  - description: String [nullable]

Relationships: @ManyToOne → ServiceRegistration (service), @ManyToOne → ServiceRegistration (gateway, nullable)

---

### === EnvironmentConfig.java (Registry) ===
Table: `environment_configs`
Primary Key: inherited UUID from BaseEntity

Fields:
  - environment: String [not null]
  - configKey: String [not null]
  - configValue: String(TEXT) [not null]
  - configSource: ConfigSource [@Enumerated(STRING)]
  - description: String [nullable]

Relationships: @ManyToOne → ServiceRegistration
Unique Constraints: (service_id, environment, config_key)

---

### === InfraResource.java (Registry) ===
Table: `infra_resources`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [raw]
  - resourceType: InfraResourceType [@Enumerated(STRING)]
  - resourceName: String(300) [not null]
  - environment: String [not null]
  - region: String [nullable]
  - arnOrUrl: String [nullable]
  - metadataJson: String [nullable]
  - description: String [nullable]
  - createdByUserId: UUID [raw]

Relationships: @ManyToOne → ServiceRegistration (nullable)

---

### === Solution.java (Registry) ===
Table: `solutions`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [raw]
  - name: String(200) [not null]
  - slug: String(63) [not null]
  - description: String [nullable]
  - category: SolutionCategory [@Enumerated(STRING)]
  - status: SolutionStatus [default ACTIVE]
  - iconName: String [nullable]
  - colorHex: String [nullable]
  - ownerUserId: UUID [nullable, raw]
  - repositoryUrl: String [nullable]
  - documentationUrl: String [nullable]
  - metadataJson: String [nullable]
  - createdByUserId: UUID [raw]

Relationships: @OneToMany → SolutionMember
Unique Constraints: (team_id, slug)

---

### === SolutionMember.java (Registry) ===
Table: `solution_members`
Primary Key: inherited UUID from BaseEntity

Fields:
  - role: SolutionMemberRole [@Enumerated(STRING)]
  - displayOrder: int [default 0]
  - notes: String [nullable]

Relationships: @ManyToOne → Solution, @ManyToOne → ServiceRegistration
Unique Constraints: (solution_id, service_id)

---

### === WorkstationProfile.java (Registry) ===
Table: `workstation_profiles`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [raw]
  - name: String(100) [not null]
  - description: String [nullable]
  - solutionId: UUID [nullable, raw]
  - servicesJson: String(TEXT) [not null]
  - startupOrder: String [nullable]
  - createdByUserId: UUID [raw]
  - isDefault: boolean [default false]

Note: No JPA FK relationships — all IDs are raw UUIDs.

---

### === ConfigTemplate.java (Registry) ===
Table: `config_templates`
Primary Key: inherited UUID from BaseEntity

Fields:
  - templateType: ConfigTemplateType [@Enumerated(STRING)]
  - environment: String [not null]
  - contentText: String(TEXT) [not null]
  - isAutoGenerated: boolean [default true]
  - generatedFrom: String [nullable]
  - version: int [default 1]

Relationships: @ManyToOne → ServiceRegistration
Unique Constraints: (service_id, template_type, environment)

---

### Relay Module (12 entities)

---

### === Channel.java (Relay) ===
Table: `channels`
Primary Key: inherited UUID from BaseEntity

Fields:
  - name: String(100) [not null]
  - slug: String(100) [not null]
  - description: String [nullable]
  - topic: String [nullable]
  - channelType: ChannelType [@Enumerated(STRING)]
  - teamId: UUID [raw]
  - projectId: UUID [nullable, raw]
  - serviceId: UUID [nullable, raw]
  - isArchived: boolean [default false]
  - createdBy: UUID [raw]

Relationships: @OneToMany → ChannelMember, @OneToMany → PinnedMessage
Unique Constraints: (team_id, slug)
Note: All IDs are raw UUIDs — cross-module decoupling.

---

### === ChannelMember.java (Relay) ===
Table: `channel_members`
Primary Key: inherited UUID from BaseEntity

Fields:
  - channelId: UUID [raw]
  - userId: UUID [raw]
  - role: MemberRole [default MEMBER]
  - lastReadAt: Instant [nullable]
  - isMuted: boolean [default false]
  - joinedAt: Instant

Relationships: Read-only @ManyToOne → Channel
Unique Constraints: (channel_id, user_id)

---

### === Message.java (Relay) ===
Table: `messages`
Primary Key: inherited UUID from BaseEntity

Fields:
  - channelId: UUID [raw]
  - senderId: UUID [raw]
  - content: String(TEXT) [not null]
  - messageType: MessageType [default TEXT]
  - parentId: UUID [nullable, threaded replies]
  - isEdited: boolean [default false]
  - isDeleted: boolean [default false]
  - mentionsEveryone: boolean [default false]
  - editedAt: Instant [nullable]
  - mentionedUserIds: String [nullable, CSV UUIDs]
  - platformEventId: UUID [nullable]

Relationships: @OneToMany → Reaction, @OneToMany → FileAttachment
Indexes: Composite (channel_id, created_at DESC)

---

### === MessageThread.java (Relay) ===
Table: `message_threads`
Primary Key: inherited UUID from BaseEntity

Fields:
  - rootMessageId: UUID [not null]
  - channelId: UUID [not null]
  - replyCount: int [default 0]
  - lastReplyAt: Instant [nullable]
  - lastReplyBy: UUID [nullable]
  - participantIds: String [nullable, CSV]

Unique Constraints: (root_message_id)

---

### === DirectConversation.java (Relay) ===
Table: `direct_conversations`
Primary Key: inherited UUID from BaseEntity

Fields:
  - teamId: UUID [not null]
  - conversationType: ConversationType [@Enumerated(STRING)]
  - name: String(200) [nullable]
  - participantIds: String(TEXT) [sorted CSV]
  - lastMessageAt: Instant [nullable]
  - lastMessagePreview: String(500) [nullable]

Unique Constraints: (team_id, participant_ids)

---

### === DirectMessage.java (Relay) ===
Table: `direct_messages`
Primary Key: inherited UUID from BaseEntity

Fields:
  - conversationId: UUID [not null]
  - senderId: UUID [not null]
  - content: String(TEXT) [not null]
  - messageType: MessageType [default TEXT]
  - isEdited: boolean [default false]
  - isDeleted: boolean [default false]
  - editedAt: Instant [nullable]

Relationships: Read-only @ManyToOne → DirectConversation, @OneToMany → Reaction, @OneToMany → FileAttachment

---

### === Reaction.java (Relay) ===
Table: `reactions`
Primary Key: inherited UUID from BaseEntity

Fields:
  - userId: UUID [not null]
  - emoji: String(50) [not null]
  - reactionType: ReactionType [default EMOJI]
  - messageId: UUID [nullable, polymorphic]
  - directMessageId: UUID [nullable, polymorphic]

Unique Constraints: (message_id, user_id, emoji), (direct_message_id, user_id, emoji)

---

### === FileAttachment.java (Relay) ===
Table: `file_attachments`
Primary Key: inherited UUID from BaseEntity

Fields:
  - fileName: String(500) [not null]
  - contentType: String(200) [not null]
  - fileSizeBytes: Long [not null]
  - storagePath: String(1000) [not null]
  - thumbnailPath: String [nullable]
  - status: FileUploadStatus [default UPLOADING]
  - uploadedBy: UUID [not null]
  - teamId: UUID [not null]
  - messageId: UUID [nullable, polymorphic]
  - directMessageId: UUID [nullable, polymorphic]

---

### === PinnedMessage.java (Relay) ===
Table: `pinned_messages`
Primary Key: inherited UUID from BaseEntity

Fields:
  - messageId: UUID [not null]
  - pinnedBy: UUID [not null]

Relationships: @ManyToOne → Channel
Unique Constraints: (channel_id, message_id)

---

### === ReadReceipt.java (Relay) ===
Table: `read_receipts`
Primary Key: inherited UUID from BaseEntity

Fields:
  - channelId: UUID [not null]
  - userId: UUID [not null]
  - lastReadMessageId: UUID [not null]
  - lastReadAt: Instant [not null]

Unique Constraints: (channel_id, user_id)

---

### === UserPresence.java (Relay) ===
Table: `user_presences`
Primary Key: inherited UUID from BaseEntity

Fields:
  - userId: UUID [not null]
  - teamId: UUID [not null]
  - status: PresenceStatus [default OFFLINE]
  - lastSeenAt: Instant [nullable]
  - lastHeartbeatAt: Instant [nullable]
  - statusMessage: String(200) [nullable]

Unique Constraints: (user_id, team_id)

---

### === PlatformEvent.java (Relay) ===
Table: `platform_events`
Primary Key: inherited UUID from BaseEntity

Fields:
  - eventType: PlatformEventType [@Enumerated(STRING)]
  - teamId: UUID [not null]
  - sourceModule: String(50) [not null]
  - sourceEntityId: UUID [nullable]
  - title: String(500) [not null]
  - detail: String [nullable]
  - targetChannelId: UUID [nullable]
  - targetChannelSlug: String [nullable]
  - postedMessageId: UUID [nullable]
  - isDelivered: boolean [default false]
  - deliveredAt: Instant [nullable]

---

**ENTITY TOTALS: 25 (Core) + 18 (Courier) + 14 (Fleet) + 16 (Logger) + 8 (MCP) + 11 (Registry) + 12 (Relay) = 104 entities + BaseEntity + ProjectDirectiveId embeddable = 106 classes**

---

## 7. Enums

### Core Enums (25)

| Enum | Values |
|------|--------|
| AgentResult | PASS, WARN, FAIL |
| AgentStatus | PENDING, RUNNING, COMPLETED, FAILED |
| AgentType | 16 values across 3 tiers (SECURITY, CODE_QUALITY, ARCHITECTURE, PERFORMANCE, ACCESSIBILITY, API_CONFORMANCE, DATABASE, DOCUMENTATION, TEST_COVERAGE, DEPENDENCY, COMPLIANCE, TECH_DEBT, BUG_INVESTIGATE, REMEDIATE, HEALTH_MONITOR, FULL_AUDIT) |
| BusinessImpact | LOW, MEDIUM, HIGH, CRITICAL |
| ComplianceStatus | MET, PARTIAL, MISSING, NOT_APPLICABLE |
| DebtCategory | ARCHITECTURE, CODE, TEST, DEPENDENCY, DOCUMENTATION |
| DebtStatus | IDENTIFIED, PLANNED, IN_PROGRESS, RESOLVED |
| DirectiveCategory | ARCHITECTURE, STANDARDS, CONVENTIONS, CONTEXT, OTHER |
| DirectiveScope | TEAM, PROJECT, USER |
| Effort | S, M, L, XL |
| FindingStatus | OPEN, ACKNOWLEDGED, FALSE_POSITIVE, FIXED, WONT_FIX |
| GitHubAuthType | PAT, OAUTH, SSH |
| InvitationStatus | PENDING, ACCEPTED, EXPIRED |
| JobMode | AUDIT, COMPLIANCE, BUG_INVESTIGATE, REMEDIATE, TECH_DEBT, DEPENDENCY, HEALTH_MONITOR |
| JobResult | PASS, WARN, FAIL |
| JobStatus | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED |
| MfaMethod | NONE, TOTP, EMAIL |
| Priority | P0, P1, P2, P3 |
| ScheduleType | DAILY, WEEKLY, ON_COMMIT |
| Scope | SYSTEM, TEAM, USER |
| Severity | CRITICAL, HIGH, MEDIUM, LOW |
| SpecType | OPENAPI, MARKDOWN, SCREENSHOT, FIGMA |
| TaskStatus | PENDING, ASSIGNED, EXPORTED, JIRA_CREATED, COMPLETED |
| TeamRole | OWNER, ADMIN, MEMBER, VIEWER |
| VulnerabilityStatus | OPEN, UPDATING, SUPPRESSED, RESOLVED |

### Courier Enums (7)

| Enum | Values |
|------|--------|
| AuthType | NO_AUTH, API_KEY, BEARER_TOKEN, BASIC_AUTH, OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS, OAUTH2_IMPLICIT, OAUTH2_PASSWORD, JWT_BEARER, INHERIT_FROM_PARENT |
| BodyType | NONE, FORM_DATA, X_WWW_FORM_URLENCODED, RAW_JSON, RAW_XML, RAW_HTML, RAW_TEXT, RAW_YAML, BINARY, GRAPHQL |
| CodeLanguage | 12 values (JAVA, PYTHON, JAVASCRIPT, TYPESCRIPT, CSHARP, GO, RUBY, PHP, SWIFT, KOTLIN, DART, RUST) |
| HttpMethod | GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS |
| RunStatus | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED |
| ScriptType | PRE_REQUEST, POST_RESPONSE |
| SharePermission | VIEWER, EDITOR, ADMIN |

### Fleet Enums (4)

| Enum | Values |
|------|--------|
| ContainerStatus | CREATED, RUNNING, PAUSED, RESTARTING, REMOVING, EXITED, DEAD, UNKNOWN |
| DeploymentAction | CREATE, START, STOP, RESTART, REMOVE, UPDATE |
| HealthStatus | HEALTHY, UNHEALTHY, STARTING, NONE |
| RestartPolicy | NO, ALWAYS, ON_FAILURE, UNLESS_STOPPED |

### Logger Enums (10)

| Enum | Values |
|------|--------|
| AlertChannelType | EMAIL, WEBHOOK, TEAMS, SLACK |
| AlertSeverity | INFO, WARNING, CRITICAL |
| AlertStatus | FIRED, ACKNOWLEDGED, RESOLVED |
| ConditionType | REGEX, KEYWORD, FREQUENCY_THRESHOLD, ABSENCE |
| LogLevel | TRACE, DEBUG, INFO, WARN, ERROR, FATAL |
| MetricType | COUNTER, GAUGE, HISTOGRAM, TIMER |
| RetentionAction | PURGE, ARCHIVE |
| SpanStatus | OK, ERROR |
| TrapType | PATTERN, FREQUENCY, ABSENCE |
| WidgetType | LOG_TABLE, LOG_CHART, METRIC_LINE, METRIC_BAR, METRIC_GAUGE, TRACE_TIMELINE, ALERT_LIST, STAT_CARD |

### MCP Enums (8)

| Enum | Values |
|------|--------|
| ActivityType | SESSION_STARTED, SESSION_COMPLETED, DOCUMENT_UPDATED, TOOL_CALL, DEPLOYMENT, ALERT |
| AuthorType | HUMAN, AI |
| DocumentType | AUDIT, OPENAPI, ARCHITECTURE, README, CHANGELOG, CUSTOM |
| Environment | LOCAL, DEVELOPMENT, STAGING, PRODUCTION |
| McpTransport | SSE, HTTP, STDIO |
| SessionStatus | INITIALIZING, ACTIVE, PAUSED, COMPLETING, COMPLETED, FAILED, TIMED_OUT |
| TokenStatus | ACTIVE, REVOKED, EXPIRED |
| ToolCallStatus | PENDING, SUCCESS, FAILURE, TIMEOUT |

### Registry Enums (11)

| Enum | Values |
|------|--------|
| ConfigSource | MANUAL, AUTO_GENERATED, INHERITED, ENVIRONMENT |
| ConfigTemplateType | DOCKER_COMPOSE, APPLICATION_YML, APPLICATION_PROPERTIES, ENV_FILE, NGINX_CONF, DOCKERFILE, MAKEFILE, GITHUB_ACTIONS, HELM_VALUES, TERRAFORM, KUBERNETES, CUSTOM |
| DependencyType | HTTP_SYNC, HTTP_ASYNC, GRPC, GRAPHQL, MESSAGE_QUEUE, DATABASE_SHARED, FILE_SYSTEM, SERVICE_MESH, EVENT_STREAM, CUSTOM |
| HealthStatus (Registry) | UP, DOWN, DEGRADED, UNKNOWN |
| InfraResourceType | DATABASE, CACHE, MESSAGE_QUEUE, OBJECT_STORAGE, CDN, DNS, LOAD_BALANCER, API_GATEWAY, CONTAINER_REGISTRY, SECRETS_MANAGER, MONITORING, LOGGING, CI_CD, VPN, FIREWALL, CERTIFICATE, DOMAIN, EMAIL_SERVICE, SEARCH_ENGINE, CUSTOM |
| PortType | HTTP, HTTPS, GRPC, WEBSOCKET, DATABASE, CACHE, MESSAGE_QUEUE, ADMIN, DEBUG, METRICS, CUSTOM, RESERVED |
| ServiceStatus | ACTIVE, INACTIVE, DEPRECATED, ARCHIVED |
| ServiceType | 20 values (SPRING_BOOT, NODE_EXPRESS, PYTHON_FLASK, PYTHON_DJANGO, GO_SERVICE, RUST_SERVICE, DOTNET_SERVICE, RUBY_RAILS, PHP_LARAVEL, FLUTTER_WEB, REACT_APP, ANGULAR_APP, VUE_APP, NEXTJS_APP, STATIC_SITE, DATABASE, CACHE, MESSAGE_QUEUE, API_GATEWAY, CUSTOM) |
| SolutionCategory | BACKEND, FRONTEND, FULLSTACK, INFRASTRUCTURE, MONITORING, CUSTOM |
| SolutionMemberRole | PRIMARY, SUPPORTING, INFRASTRUCTURE, OPTIONAL |
| SolutionStatus | ACTIVE, INACTIVE, DEPRECATED, ARCHIVED |

### Relay Enums (8)

| Enum | Values |
|------|--------|
| ChannelType | PUBLIC, PRIVATE, PROJECT, SERVICE |
| ConversationType | ONE_ON_ONE, GROUP |
| FileUploadStatus | UPLOADING, COMPLETE, FAILED |
| MemberRole | OWNER, ADMIN, MEMBER |
| MessageType | TEXT, SYSTEM, PLATFORM_EVENT, FILE |
| PlatformEventType | QA_JOB_COMPLETED, FINDING_CREATED, DEPLOYMENT_COMPLETED, SERVICE_HEALTH_CHANGED, ALERT_FIRED, MEMBER_JOINED, MEMBER_LEFT, PROJECT_CREATED, BUILD_COMPLETED, CUSTOM |
| PresenceStatus | ONLINE, AWAY, DND, OFFLINE |
| ReactionType | EMOJI |

**TOTAL ENUMS: 73** (all plain — no display labels, no custom methods)

---

## 8. Repository Layer

All repositories extend `JpaRepository<Entity, UUID>` (except SystemSettingRepository which uses String key, AuditLogRepository which uses Long key, and ProjectDirectiveRepository which uses ProjectDirectiveId).

### Core Repositories (26)

| Repository | Entity | Notable Custom Queries |
|---|---|---|
| UserRepository | User | findByEmail, existsByEmail |
| TeamRepository | Team | findByOwnerId |
| TeamMemberRepository | TeamMember | findByTeamIdAndUserId, findByTeamId, findByUserId, existsByTeamIdAndUserId |
| ProjectRepository | Project | findByTeamId, findByTeamIdAndIsArchivedFalse |
| QaJobRepository | QaJob | findByProjectId, findByProjectIdAndStatus, findByProjectIdOrderByCreatedAtDesc |
| AgentRunRepository | AgentRun | findByJobId, findByJobIdAndAgentType |
| FindingRepository | Finding | findByJobId, findByJobIdAndSeverity, findByJobIdAndStatus, countByJobIdAndSeverity |
| RemediationTaskRepository | RemediationTask | findByJobId, findByJobIdOrderByTaskNumber. Has native query for task number sequencing. |
| BugInvestigationRepository | BugInvestigation | findByJobId |
| ComplianceItemRepository | ComplianceItem | findByJobId, findByJobIdAndStatus |
| SpecificationRepository | Specification | findByJobId |
| TechDebtItemRepository | TechDebtItem | findByProjectId, findByProjectIdAndStatus |
| DependencyScanRepository | DependencyScan | findByProjectId, findByJobId |
| DependencyVulnerabilityRepository | DependencyVulnerability | findByScanId, findByScanIdAndSeverity |
| HealthSnapshotRepository | HealthSnapshot | findByProjectIdOrderByCapturedAtDesc |
| HealthScheduleRepository | HealthSchedule | findByProjectId, findByIsActiveTrue |
| DirectiveRepository | Directive | findByTeamId, findByScope |
| ProjectDirectiveRepository | ProjectDirective | findByIdProjectId, findByIdDirectiveId |
| PersonaRepository | Persona | findByTeamId, findByScope, findByAgentTypeAndIsDefaultTrue |
| GitHubConnectionRepository | GitHubConnection | findByTeamId, findByTeamIdAndIsActiveTrue |
| JiraConnectionRepository | JiraConnection | findByTeamId, findByTeamIdAndIsActiveTrue |
| InvitationRepository | Invitation | findByToken (pessimistic write lock), findByTeamId, findByEmailAndTeamIdAndStatus |
| NotificationPreferenceRepository | NotificationPreference | findByUserId, findByUserIdAndEventType |
| AuditLogRepository | AuditLog | findByTeamIdOrderByCreatedAtDesc |
| SystemSettingRepository | SystemSetting | — (uses String PK) |
| MfaEmailCodeRepository | MfaEmailCode | findByUserIdAndUsedFalseAndExpiresAtAfter |

### Courier Repositories (18)

| Repository | Entity | Notable Custom Queries |
|---|---|---|
| CollectionRepository | Collection | findByTeamId, findByTeamIdAndIsSharedTrue |
| FolderRepository | Folder | findByCollectionId, findByParentFolderId |
| RequestRepository | Request | findByFolderId |
| RequestHeaderRepository | RequestHeader | findByRequestId, deleteByRequestId |
| RequestParamRepository | RequestParam | findByRequestId, deleteByRequestId |
| RequestBodyRepository | RequestBody | findByRequestId, deleteByRequestId |
| RequestAuthRepository | RequestAuth | findByRequestId, deleteByRequestId |
| RequestScriptRepository | RequestScript | findByRequestId, findByRequestIdAndScriptType |
| EnvironmentRepository | Environment | findByTeamId, findByTeamIdAndIsActiveTrue |
| EnvironmentVariableRepository | EnvironmentVariable | findByEnvironmentId, findByCollectionId |
| GlobalVariableRepository | GlobalVariable | findByTeamId, findByTeamIdAndIsEnabledTrue |
| RequestHistoryRepository | RequestHistory | findByTeamIdOrderByCreatedAtDesc, findByTeamIdAndUserId |
| RunResultRepository | RunResult | findByTeamId, findByCollectionId |
| RunIterationRepository | RunIteration | findByRunResultId |
| CollectionShareRepository | CollectionShare | findByCollectionId, findBySharedWithUserId |
| ForkRepository | Fork | findBySourceCollectionId, findByForkedByUserId |
| MergeRequestRepository | MergeRequest | findByForkId, findByTargetCollectionId |
| CodeSnippetTemplateRepository | CodeSnippetTemplate | findByLanguage |

### Fleet Repositories (14)

| Repository | Entity | Notable Custom Queries |
|---|---|---|
| ServiceProfileRepository | ServiceProfile | findByTeamId, findByTeamIdAndIsEnabledTrue |
| ContainerInstanceRepository | ContainerInstance | findByTeamId, findByServiceProfileId, findByContainerId |
| PortMappingRepository | PortMapping | findByServiceProfileId |
| VolumeMountRepository | VolumeMount | findByServiceProfileId |
| NetworkConfigRepository | NetworkConfig | findByServiceProfileId |
| EnvironmentVariableRepository (Fleet) | FleetEnvironmentVariable | findByServiceProfileId |
| ContainerHealthCheckRepository | ContainerHealthCheck | findByContainerInstanceId |
| ContainerLogRepository | ContainerLog | findByContainerInstanceId |
| DeploymentRecordRepository | DeploymentRecord | findByTeamIdOrderByCreatedAtDesc |
| DeploymentContainerRepository | DeploymentContainer | findByDeploymentRecordId |
| SolutionProfileRepository | SolutionProfile | findByTeamId |
| SolutionServiceRepository | SolutionService | findBySolutionProfileId |
| WorkstationProfileRepository (Fleet) | FleetWorkstationProfile | findByTeamId, findByUserId |
| WorkstationSolutionRepository | WorkstationSolution | findByWorkstationProfileId |

### Logger Repositories (16)

| Repository | Entity | Notable Custom Queries |
|---|---|---|
| LogEntryRepository | LogEntry | findByTeamId, findByServiceName, complex query methods for filtering |
| LogSourceRepository | LogSource | findByTeamId, findByTeamIdAndIsActiveTrue |
| TraceSpanRepository | TraceSpan | findByTeamIdAndTraceId, findByTeamIdAndCorrelationId |
| MetricRepository | Metric | findByTeamId, findByTeamIdAndServiceName |
| MetricSeriesRepository | MetricSeries | findByMetricIdAndTimestampBetween |
| LogTrapRepository | LogTrap | findByTeamId, findByTeamIdAndIsActiveTrue |
| TrapConditionRepository | TrapCondition | findByLogTrapId |
| AlertRuleRepository | AlertRule | findByTeamId, findByLogTrapId |
| AlertChannelRepository | AlertChannel | findByTeamId, findByTeamIdAndIsActiveTrue |
| AlertHistoryRepository | AlertHistory | findByTeamIdOrderByCreatedAtDesc, findByAlertRuleId |
| DashboardRepository | Dashboard | findByTeamId, findByTeamIdAndIsSharedTrue |
| DashboardWidgetRepository | DashboardWidget | findByDashboardId |
| SavedQueryRepository | SavedQuery | findByTeamId, findByTeamIdAndIsSharedTrue |
| QueryHistoryRepository | QueryHistory | findByTeamIdOrderByCreatedAtDesc |
| RetentionPolicyRepository | RetentionPolicy | findByTeamId, findByIsActiveTrue |
| AnomalyBaselineRepository | AnomalyBaseline | findByTeamId, findByTeamIdAndServiceName, findByIsActiveTrue |

### MCP Repositories (8)

| Repository | Entity | Notable Custom Queries |
|---|---|---|
| DeveloperProfileRepository | DeveloperProfile | findByTeamIdAndUserId, findByTeamId |
| McpApiTokenRepository | McpApiToken | findByTokenHash, findByDeveloperProfileId |
| McpSessionRepository | McpSession | findByDeveloperProfileId, findByProjectId |
| SessionToolCallRepository | SessionToolCall | findByMcpSessionId |
| SessionResultRepository | SessionResult | findByMcpSessionId |
| ProjectDocumentRepository | ProjectDocument | findByProjectId, findByProjectIdAndDocumentType |
| ProjectDocumentVersionRepository | ProjectDocumentVersion | findByProjectDocumentId |
| ActivityFeedEntryRepository | ActivityFeedEntry | findByTeamIdOrderByCreatedAtDesc |

### Registry Repositories (11)

| Repository | Entity | Notable Custom Queries |
|---|---|---|
| ServiceRegistrationRepository | ServiceRegistration | findByTeamId, findByTeamIdAndSlug, findByTeamIdAndStatus |
| ServiceDependencyRepository | ServiceDependency | findBySourceId, findByTargetId |
| PortAllocationRepository | PortAllocation | findByServiceId, findByServiceIdAndEnvironment |
| PortRangeRepository | PortRange | findByTeamId, findByTeamIdAndPortTypeAndEnvironment |
| ApiRouteRegistrationRepository | ApiRouteRegistration | findByServiceId, findByEnvironment |
| EnvironmentConfigRepository | EnvironmentConfig | findByServiceId, findByServiceIdAndEnvironment |
| InfraResourceRepository | InfraResource | findByTeamId, findByServiceRegistrationId |
| SolutionRepository | Solution | findByTeamId, findByTeamIdAndSlug |
| SolutionMemberRepository | SolutionMember | findBySolutionId, findByServiceRegistrationId |
| WorkstationProfileRepository (Registry) | WorkstationProfile | findByTeamId |
| ConfigTemplateRepository | ConfigTemplate | findByServiceRegistrationId, findByServiceRegistrationIdAndEnvironment |

### Relay Repositories (12)

| Repository | Entity | Notable Custom Queries |
|---|---|---|
| ChannelRepository | Channel | findByTeamId, findByTeamIdAndSlug, findByTeamIdAndChannelType |
| ChannelMemberRepository | ChannelMember | findByChannelId, findByUserId, findByChannelIdAndUserId |
| MessageRepository | Message | findByChannelIdOrderByCreatedAtDesc, findByParentId |
| MessageThreadRepository | MessageThread | findByRootMessageId, findByChannelId |
| DirectConversationRepository | DirectConversation | findByTeamIdAndParticipantIds |
| DirectMessageRepository | DirectMessage | findByConversationIdOrderByCreatedAtDesc |
| ReactionRepository | Reaction | findByMessageId, findByDirectMessageId |
| FileAttachmentRepository | FileAttachment | findByMessageId, findByDirectMessageId |
| PinnedMessageRepository | PinnedMessage | findByChannelId |
| ReadReceiptRepository | ReadReceipt | findByChannelIdAndUserId |
| UserPresenceRepository | UserPresence | findByUserIdAndTeamId, findByTeamId |
| PlatformEventRepository | PlatformEvent | findByTeamIdAndIsDeliveredFalse |

**REPOSITORY TOTALS: 26 (Core) + 18 (Courier) + 14 (Fleet) + 16 (Logger) + 8 (MCP) + 11 (Registry) + 12 (Relay) = 105 repositories**

No @EntityGraph usage anywhere. No projections. Only InvitationRepository uses pessimistic locking. Only RemediationTaskRepository uses a native query.

---

## 9. Service Layer

All services use `@RequiredArgsConstructor` for constructor injection via Lombok. All services are `@Service` annotated.

### Core Services (26+)

**AuthService** — register(RegisterRequest), login(LoginRequest), refreshToken(RefreshTokenRequest), initiateMfaChallenge(UUID userId), verifyMfaChallenge(MfaChallengeRequest). Handles JWT issuance, BCrypt password hashing, MFA flow (TOTP + Email). Returns AuthResponse with access/refresh tokens.

**UserService** — getCurrentUser(UUID), updateProfile(UUID, UpdateProfileRequest), changePassword(UUID, ChangePasswordRequest), getUsersByIds(List<UUID>). Manages user CRUD and profile updates.

**TeamService** — create(UUID, CreateTeamRequest), getById(UUID), getByUserId(UUID), update(UUID, UUID, UpdateTeamRequest), delete(UUID, UUID). Verifies ownership for mutations.

**TeamMemberService** — addMember(UUID, UUID, AddTeamMemberRequest), removeMember(UUID, UUID, UUID), updateRole(UUID, UUID, UUID, UpdateRoleRequest), getMembers(UUID, UUID), isMember(UUID, UUID). Team-scoped authorization.

**ProjectService** — create(UUID, UUID, CreateProjectRequest), getById(UUID), getByTeamId(UUID), update(UUID, UUID, UpdateProjectRequest), delete(UUID, UUID). Cascading delete of 13 child entity types (QaJobs, AgentRuns, Findings, RemediationTasks, BugInvestigations, ComplianceItems, Specifications, TechDebtItems, DependencyScans, DependencyVulnerabilities, HealthSnapshots, HealthSchedules, ProjectDirectives).

**QaJobService** — create(UUID, UUID, CreateQaJobRequest), getById(UUID), getByProjectId(UUID), updateStatus(UUID, UpdateQaJobStatusRequest), cancel(UUID). Lifecycle: PENDING → RUNNING → COMPLETED/FAILED/CANCELLED.

**AgentRunService** — create(UUID, CreateAgentRunRequest), getById(UUID), getByJobId(UUID), updateStatus(UUID, UpdateAgentRunRequest).

**FindingService** — create(UUID, CreateFindingRequest), getById(UUID), getByJobId(UUID, Pageable), updateStatus(UUID, UUID, UpdateFindingStatusRequest). Status transitions validated.

**RemediationTaskService** — create(UUID, CreateRemediationTaskRequest), getById(UUID), getByJobId(UUID), update(UUID, UpdateRemediationTaskRequest), assignTo(UUID, UUID). Auto-incrementing task numbers via native query.

**BugInvestigationService** — create(UUID, CreateBugInvestigationRequest), getById(UUID), getByJobId(UUID), update(UUID, UpdateBugInvestigationRequest).

**ComplianceItemService** — create(UUID, CreateComplianceItemRequest), getByJobId(UUID).

**SpecificationService** — create(UUID, CreateSpecificationRequest), getByJobId(UUID).

**TechDebtItemService** — create(UUID, CreateTechDebtItemRequest), getByProjectId(UUID), updateStatus(UUID, UpdateTechDebtStatusRequest).

**DependencyScanService** — create(UUID, CreateDependencyScanRequest), getByProjectId(UUID).

**DependencyVulnerabilityService** — create(UUID, CreateVulnerabilityRequest), getByScanId(UUID).

**HealthSnapshotService** — create(UUID, CreateHealthSnapshotRequest), getByProjectId(UUID).

**HealthScheduleService** — create(UUID, CreateHealthScheduleRequest), getByProjectId(UUID), toggle(UUID).

**DirectiveService** — create(UUID, CreateDirectiveRequest), getById(UUID), getByTeamId(UUID), update(UUID, UpdateDirectiveRequest), delete(UUID).

**ProjectDirectiveService** — assign(UUID, UUID), unassign(UUID, UUID), getByProjectId(UUID), toggle(UUID, UUID).

**PersonaService** — create(UUID, CreatePersonaRequest), getById(UUID), getByTeamId(UUID), update(UUID, UpdatePersonaRequest), delete(UUID). Scope validation: SYSTEM personas are read-only.

**GitHubConnectionService** — create(UUID, UUID, CreateGitHubConnectionRequest), getByTeamId(UUID), deactivate(UUID). Credentials encrypted via EncryptionService. Never returns decrypted credentials.

**JiraConnectionService** — create(UUID, UUID, CreateJiraConnectionRequest), getByTeamId(UUID), deactivate(UUID). API token encrypted via EncryptionService.

**InvitationService** — create(UUID, UUID, CreateInvitationRequest), accept(String token, UUID userId), getByTeamId(UUID), expire(UUID). Token-based flow with pessimistic locking on accept.

**NotificationService** — dispatches notifications (in-app, email, Teams webhook) based on user preferences. Delegates to EmailService and TeamsWebhookService.

**AuditLogService** — log(AuditLogEntry). @Async — fire-and-forget. Records all mutations with user, team, entity context.

**TokenBlacklistService** — blacklist(String token), isBlacklisted(String token). Uses Redis SET with TTL matching token expiry.

**SystemSettingService** — get(String key), set(String key, String value, UUID userId).

**MetricsService (Core)** — getProjectMetrics(UUID), getTeamMetrics(UUID). Aggregates health scores, finding counts, debt stats.

**EncryptionService** — encrypt(String), decrypt(String). AES-256-GCM with static master key. Used for GitHub PATs and Jira API tokens.

**MfaService** — generateSecret(), generateQrCodeUri(String secret, String email), verifyTotp(String secret, String code), generateEmailCode(UUID userId), verifyEmailCode(UUID userId, String code). TOTP (RFC 6238) + email-based MFA.

**S3StorageService** — upload(String key, byte[] data), download(String key), delete(String key), generatePresignedUrl(String key). S3 disabled in dev — falls back to local filesystem at ~/.codeops/storage/.

### Courier Services (22)

**CollectionService** — CRUD for collections. Team-scoped. Cascading operations on folders/requests.

**FolderService** — CRUD for folders within collections. Self-referential parent-child hierarchy. Cascade delete of sub-folders and requests.

**RequestService** — CRUD for HTTP requests. Manages headers, params, body, auth, scripts as child entities.

**RequestProxyService** — execute(UUID requestId, UUID environmentId). Executes HTTP requests via RestTemplate/WebClient. Variable substitution, auth resolution (inheriting from folder/collection), pre/post scripts. Returns RequestHistory entry.

**ScriptEngineService** — executeScript(String script, ScriptContext context). GraalJS sandbox with 5-second timeout, 100K statement limit. Provides `pm` object API (pm.environment.set/get, pm.test, pm.response, pm.variables).

**CollectionRunnerService** — run(UUID collectionId, RunRequest). Executes all requests in a collection sequentially with iterations, delays, and data files. Creates RunResult with RunIterations.

**HistoryService** — getByTeamId(UUID, Pageable), getByUserId(UUID, UUID, Pageable), clearHistory(UUID, UUID).

**EnvironmentService** — CRUD for environments. Team-scoped with unique name constraint.

**EnvironmentVariableService** — CRUD for environment variables within environments and collections.

**GlobalVariableService** — CRUD for team-level global variables.

**CollectionShareService** — share(UUID collectionId, ShareRequest), revoke(UUID shareId), getShares(UUID collectionId).

**ForkService** — fork(UUID collectionId, UUID userId), getForks(UUID collectionId). Deep-clones collection with all sub-entities.

**MergeService** — createMergeRequest(UUID forkId, MergeRequestRequest), review(UUID mrId, ReviewRequest), merge(UUID mrId). Conflict detection and resolution.

**ImportService** — importPostmanCollection(UUID teamId, String json), importOpenApi(UUID teamId, String yaml), importCurl(UUID teamId, String curl). Converts external formats to Courier collections.

**ExportService** — exportAsPostman(UUID collectionId), exportAsOpenApi(UUID collectionId), exportAsCurl(UUID requestId). Converts Courier data to external formats.

**CodeSnippetService** — generate(UUID requestId, CodeLanguage language). Generates code snippets from templates for 12 languages.

**RequestAuthService, RequestBodyService, RequestHeaderService, RequestParamService, RequestScriptService** — Dedicated CRUD services for request sub-entities.

**VariableResolverService** — resolve(String template, UUID environmentId, UUID collectionId, UUID teamId). Resolves `{{variable}}` placeholders from environment, collection, and global scopes (in priority order).

### Fleet Services (6)

**DockerEngineService** — Raw Docker Engine API client via HTTP (Unix socket or TCP). listContainers(), inspectContainer(String id), createContainer(CreateContainerRequest), startContainer(String id), stopContainer(String id), removeContainer(String id), getContainerLogs(String id), getContainerStats(String id), pullImage(String name, String tag), listImages(), listVolumes(), listNetworks().

**ContainerManagementService** — Higher-level container lifecycle management. Creates ContainerInstance records, syncs with Docker Engine, manages start/stop/restart/remove operations, records DeploymentRecords.

**ServiceProfileService** — CRUD for service profiles. Manages sub-entities (ports, volumes, networks, env vars). Generates profiles from Registry ServiceRegistrations.

**SolutionProfileService** — CRUD for solution profiles. Manages ordered lists of service profiles. Deploy/undeploy operations that cascade to ContainerManagementService.

**WorkstationProfileService** — CRUD for workstation profiles (user-specific). Manages solutions with startup order and environment overrides.

**FleetHealthService** — monitorHealth(UUID teamId). Polls container health status. Crash loop detection (restartCount > threshold). Auto-restart on unhealthy status.

### Logger Services (19)

**LogIngestionService** — ingest(LogIngestRequest). Parses, validates, and persists log entries. Updates LogSource stats. Triggers TrapEvaluationEngine.

**LogParsingService** — parse(String rawLog). Auto-detects format: JSON, key-value, Spring Boot, syslog, plaintext. Extracts structured fields.

**LogQueryService** — query(LogQueryRequest). Criteria API-based querying with filtering by service, level, time range, keyword, regex. Supports pagination and sorting.

**LogQueryDslParser** — parse(String dsl). Parses human-readable query DSL (e.g., `service:my-app level:ERROR after:1h "search term"`) into LogQueryRequest.

**LogSourceService** — CRUD for log sources. Tracks last log received and log counts.

**LogTrapService** — CRUD for log traps. Manages trap conditions (pattern match, frequency threshold, absence detection).

**TrapEvaluationEngine** — evaluate(LogEntry entry). Evaluates all active traps against incoming log entries. Triggers AlertService on match.

**AlertService** — fire(AlertRule rule, String message). Creates AlertHistory, dispatches to AlertChannelService. Throttling via throttleMinutes.

**AlertChannelService** — send(AlertChannel channel, AlertHistory alert). @Async dispatch to EMAIL, WEBHOOK, TEAMS, SLACK channels.

**AnomalyDetectionService** — @Scheduled daily at 3 AM. recalculateAllBaselines(). Computes rolling mean/stddev for metrics. detectAnomaly(MetricSeries point) checks deviation threshold.

**AnomalyBaselineCalculator** — calculate(UUID metricId, Instant windowStart, Instant windowEnd). Statistical computation for anomaly detection baselines.

**MetricsService (Logger)** — ingest(MetricIngestRequest), query(MetricQueryRequest). Manages metrics and time-series data points.

**MetricAggregationService** — aggregate(UUID metricId, Instant start, Instant end, int resolution). Downsamples metric series for dashboard display.

**DashboardService** — CRUD for dashboards and widgets. Supports shared/template dashboards.

**RetentionService** — CRUD for retention policies. Team-scoped.

**RetentionExecutor** — @Scheduled daily at 2 AM. executeAllActivePolicies(). Purges log entries older than retentionDays. ARCHIVE action not yet implemented.

**TraceService** — CRUD for trace spans. Query by traceId, correlationId.

**TraceAnalysisService** — analyzeTrace(String traceId, UUID teamId). Reconstructs trace tree, calculates critical path, identifies bottleneck spans.

**KafkaLogConsumer** — @KafkaListener on `codeops-logs` topic, group `codeops-server`. Deserializes JSON log entries and delegates to LogIngestionService. 3 retries with 1s backoff.

### MCP Services (8)

**DeveloperProfileService** — CRUD for developer profiles. Team+user scoped with unique constraint.

**McpApiTokenService** — create(UUID profileId, CreateTokenRequest), revoke(UUID tokenId), validateToken(String rawToken). SHA-256 hash storage. Token prefix `mcp_*`.

**McpSessionService** — create(UUID profileId, CreateSessionRequest), updateStatus(UUID sessionId, SessionStatus), complete(UUID sessionId, SessionResultRequest). Lifecycle: INITIALIZING → ACTIVE → COMPLETING → COMPLETED/FAILED/TIMED_OUT.

**SessionToolCallService** — record(UUID sessionId, ToolCallRequest). Records tool calls within MCP sessions.

**ProjectDocumentService** — CRUD for project documents. Versioning with ProjectDocumentVersion. Flagging support for review.

**ActivityFeedService** — record(ActivityFeedRequest). Creates feed entries for team-wide activity visibility.

**McpSecurityService** — validateTokenAndGetProfile(String token). In-memory token validation cache. Returns DeveloperProfile for authenticated MCP requests.

**McpTokenAuthFilter** — OncePerRequestFilter. Intercepts "Bearer mcp_*" tokens. Delegates to McpSecurityService. Sets SecurityContext with MCP principal.

### Registry Services (10)

**ServiceRegistryService** — CRUD for service registrations. Slug generation via SlugUtils. Team-scoped with unique slug constraint. Cascading management of ports, dependencies, routes, configs.

**DependencyGraphService** — buildGraph(UUID teamId). Kahn's algorithm for topological sort. Cycle detection. Returns adjacency list and dependency layers for visualization.

**PortAllocationService** — allocate(UUID serviceId, PortAllocationRequest), autoAllocate(UUID serviceId, PortType, String environment). Auto-allocation from PortRange definitions. Conflict detection.

**ConfigEngineService** — generateDockerCompose(UUID teamId, String environment), generateApplicationYml(UUID serviceId, String environment). Template-based config generation from service registrations, ports, dependencies, and environment configs.

**TopologyService** — getTopology(UUID teamId). Aggregates services, dependencies, ports, health status into a topology map for visualization.

**SolutionService** — CRUD for solutions. Manages solution members (services) with roles and display order.

**InfraResourceService** — CRUD for infrastructure resources. Team-scoped. Linked to services optionally.

**ApiRouteService** — CRUD for API route registrations. Manages route prefixes and gateway assignments.

**EnvironmentConfigService** — CRUD for environment-specific config key-value pairs per service.

**WorkstationProfileService (Registry)** — CRUD for workstation profiles. JSON-based service configuration. Team-scoped.

### Relay Services (8)

**ChannelService** — CRUD for channels. Slug-based lookup. Manages members (join/leave/kick). Archive/unarchive. Team-scoped with unique slug.

**MessageService** — send(UUID channelId, UUID senderId, SendMessageRequest), edit(UUID messageId, UUID userId, EditMessageRequest), delete(UUID messageId, UUID userId). Thread support via parentId. Mentions parsing. Broadcast via RelayWebSocketService.

**DirectMessageService** — send(UUID conversationId, UUID senderId, SendDirectMessageRequest), edit, delete. Updates DirectConversation.lastMessageAt/lastMessagePreview.

**DirectConversationService** — findOrCreate(UUID teamId, List<UUID> participantIds). Sorted participant IDs for unique lookup. ONE_ON_ONE (2 participants) or GROUP.

**PresenceService** — updatePresence(UUID userId, UUID teamId, PresenceStatus), heartbeat(UUID userId, UUID teamId), getPresence(UUID teamId). Online/Away/DND/Offline with heartbeat-based timeout.

**PlatformEventService** — publish(PlatformEvent event). Delivers events to target channels as PLATFORM_EVENT messages. Marks isDelivered on successful delivery.

**FileAttachmentService** — upload(UUID messageId, MultipartFile), download(UUID attachmentId). S3-backed storage with thumbnail generation. Status: UPLOADING → COMPLETE/FAILED.

**RelayWebSocketService** — Manages STOMP subscriptions. broadcastToChannel(UUID channelId, Message), sendToUser(UUID userId, Object payload). Topic structure: /topic/channel/{id}, /topic/dm/{conversationId}, /topic/presence/{teamId}.

---

## 10. Controller Layer

All controllers use `@RestController`, `@RequestMapping`, `@PreAuthorize` for method-level authorization. Principal is UUID from JWT SecurityContext.

### Core Controllers (17)

**AuthController** — `@RequestMapping("/api/v1/auth")`
- POST /register → AuthResponse (201)
- POST /login → AuthResponse (200)
- POST /refresh → AuthResponse (200)
- POST /mfa/challenge → MfaChallengeResponse (200)
- POST /mfa/verify → AuthResponse (200)
- POST /logout → void (200)

**UserController** — `@RequestMapping("/api/v1/users")`
- GET /me → UserResponse (200)
- PUT /me → UserResponse (200)
- PUT /me/password → void (200)
- PUT /me/mfa → MfaSetupResponse (200)
- DELETE /me/mfa → void (200)

**TeamController** — `@RequestMapping("/api/v1/teams")`
- POST / → TeamResponse (201)
- GET / → List<TeamResponse> (200)
- GET /{teamId} → TeamResponse (200)
- PUT /{teamId} → TeamResponse (200)
- DELETE /{teamId} → void (204)

**TeamMemberController** — `@RequestMapping("/api/v1/teams/{teamId}/members")`
- GET / → List<TeamMemberResponse> (200)
- POST / → TeamMemberResponse (201)
- PUT /{memberId}/role → TeamMemberResponse (200)
- DELETE /{memberId} → void (204)

**ProjectController** — `@RequestMapping("/api/v1/teams/{teamId}/projects")`
- POST / → ProjectResponse (201)
- GET / → List<ProjectResponse> (200)
- GET /{projectId} → ProjectResponse (200)
- PUT /{projectId} → ProjectResponse (200)
- DELETE /{projectId} → void (204)

**QaJobController** — `@RequestMapping("/api/v1/projects/{projectId}/jobs")`
- POST / → QaJobResponse (201)
- GET / → List<QaJobResponse> (200)
- GET /{jobId} → QaJobResponse (200)
- PUT /{jobId}/status → QaJobResponse (200)
- POST /{jobId}/cancel → QaJobResponse (200)

**AgentRunController** — `@RequestMapping("/api/v1/jobs/{jobId}/agents")`
- POST / → AgentRunResponse (201)
- GET / → List<AgentRunResponse> (200)
- GET /{agentRunId} → AgentRunResponse (200)
- PUT /{agentRunId} → AgentRunResponse (200)

**FindingController** — `@RequestMapping("/api/v1/jobs/{jobId}/findings")`
- POST / → FindingResponse (201)
- GET / → PageResponse<FindingResponse> (200)
- GET /{findingId} → FindingResponse (200)
- PUT /{findingId}/status → FindingResponse (200)

**RemediationTaskController** — `@RequestMapping("/api/v1/jobs/{jobId}/tasks")`
- POST / → RemediationTaskResponse (201)
- GET / → List<RemediationTaskResponse> (200)
- GET /{taskId} → RemediationTaskResponse (200)
- PUT /{taskId} → RemediationTaskResponse (200)
- PUT /{taskId}/assign → RemediationTaskResponse (200)

**BugInvestigationController** — `@RequestMapping("/api/v1/jobs/{jobId}/bugs")`
- POST / → BugInvestigationResponse (201)
- GET / → List<BugInvestigationResponse> (200)
- GET /{bugId} → BugInvestigationResponse (200)
- PUT /{bugId} → BugInvestigationResponse (200)

**DirectiveController** — `@RequestMapping("/api/v1/teams/{teamId}/directives")`
- POST / → DirectiveResponse (201)
- GET / → List<DirectiveResponse> (200)
- GET /{directiveId} → DirectiveResponse (200)
- PUT /{directiveId} → DirectiveResponse (200)
- DELETE /{directiveId} → void (204)

**PersonaController** — `@RequestMapping("/api/v1/teams/{teamId}/personas")`
- POST / → PersonaResponse (201)
- GET / → List<PersonaResponse> (200)
- GET /{personaId} → PersonaResponse (200)
- PUT /{personaId} → PersonaResponse (200)
- DELETE /{personaId} → void (204)

**GitHubConnectionController** — `@RequestMapping("/api/v1/teams/{teamId}/github-connections")`
- POST / → GitHubConnectionResponse (201)
- GET / → List<GitHubConnectionResponse> (200)
- DELETE /{connectionId} → void (204)

**JiraConnectionController** — `@RequestMapping("/api/v1/teams/{teamId}/jira-connections")`
- POST / → JiraConnectionResponse (201)
- GET / → List<JiraConnectionResponse> (200)
- DELETE /{connectionId} → void (204)

**InvitationController** — `@RequestMapping("/api/v1/teams/{teamId}/invitations")`
- POST / → InvitationResponse (201)
- GET / → List<InvitationResponse> (200)
- POST /accept → TeamMemberResponse (200)

**SystemSettingController** — `@RequestMapping("/api/v1/admin/settings")`
- GET /{key} → SystemSettingResponse (200)
- PUT /{key} → SystemSettingResponse (200)

**HealthController** — `@RequestMapping("/api/v1/health")`
- GET / → HealthResponse (200)

### Courier Controllers (14+)

**CollectionController** — `/api/v1/teams/{teamId}/courier/collections` — CRUD (5 endpoints)
**FolderController** — `/api/v1/courier/collections/{collectionId}/folders` — CRUD (5 endpoints)
**RequestController** — `/api/v1/courier/folders/{folderId}/requests` — CRUD (5 endpoints)
**ProxyController** — `/api/v1/courier/requests/{requestId}/send` — POST execute (1 endpoint)
**RunController** — `/api/v1/courier/collections/{collectionId}/run` — POST run, GET results (3 endpoints)
**HistoryController** — `/api/v1/teams/{teamId}/courier/history` — GET list, DELETE clear (2 endpoints)
**EnvironmentController** — `/api/v1/teams/{teamId}/courier/environments` — CRUD (5 endpoints)
**EnvironmentVariableController** — `/api/v1/courier/environments/{envId}/variables` — CRUD (4 endpoints)
**GlobalVariableController** — `/api/v1/teams/{teamId}/courier/globals` — CRUD (4 endpoints)
**ShareController** — `/api/v1/courier/collections/{collectionId}/shares` — POST share, GET list, DELETE revoke (3 endpoints)
**ForkController** — `/api/v1/courier/collections/{collectionId}/forks` — POST fork, GET list (2 endpoints)
**MergeController** — `/api/v1/courier/forks/{forkId}/merge-requests` — POST create, PUT review, POST merge (3 endpoints)
**ImportExportController** — `/api/v1/teams/{teamId}/courier/import` and `/api/v1/courier/export` — POST import (3 formats), GET export (3 formats)
**CodeSnippetController** — `/api/v1/courier/requests/{requestId}/snippets/{language}` — GET generate (1 endpoint)
**Courier HealthController** — `/api/v1/courier/health` — GET health (1 endpoint)

### Fleet Controllers (8)

**ContainerController** — `/api/v1/teams/{teamId}/fleet/containers` — 11 endpoints: list, inspect, create, start, stop, restart, remove, logs, stats, start-all, stop-all
**ServiceProfileController** — `/api/v1/teams/{teamId}/fleet/profiles` — 6 endpoints: CRUD + list by team + generate from registry
**SolutionProfileController** — `/api/v1/teams/{teamId}/fleet/solutions` — 9 endpoints: CRUD + add/remove service + deploy/undeploy + list
**WorkstationProfileController** — `/api/v1/teams/{teamId}/fleet/workstations` — 9 endpoints: CRUD + add/remove solution + activate/deactivate + list
**FleetHealthController** — `/api/v1/teams/{teamId}/fleet/health` — 5 endpoints: overview, service health, container health, health checks, trigger monitor
**ImageController** — `/api/v1/teams/{teamId}/fleet/images` — 4 endpoints: list, pull, inspect, remove
**VolumeController** — `/api/v1/teams/{teamId}/fleet/volumes` — 4 endpoints: list, create, inspect, remove
**NetworkController** — `/api/v1/teams/{teamId}/fleet/networks` — 5 endpoints: list, create, inspect, remove, connect

### Logger Controllers (10)

**LogIngestionController** — `/api/v1/teams/{teamId}/logger/ingest` — POST single, POST batch (2 endpoints)
**LogQueryController** — `/api/v1/teams/{teamId}/logger/logs` — POST query, POST dsl-query, GET saved-queries CRUD, POST query-history (6+ endpoints)
**LogSourceController** — `/api/v1/teams/{teamId}/logger/sources` — CRUD (5 endpoints)
**LogTrapController** — `/api/v1/teams/{teamId}/logger/traps` — CRUD + conditions management (7+ endpoints)
**AlertController** — `/api/v1/teams/{teamId}/logger/alerts` — rules CRUD, channels CRUD, history list/acknowledge/resolve (10+ endpoints)
**AnomalyController** — `/api/v1/teams/{teamId}/logger/anomalies` — baselines CRUD, trigger recalculation, detect (5+ endpoints)
**DashboardController** — `/api/v1/teams/{teamId}/logger/dashboards` — CRUD + widgets management (8+ endpoints)
**MetricsController** — `/api/v1/teams/{teamId}/logger/metrics` — ingest, query, aggregate, list (5+ endpoints)
**RetentionController** — `/api/v1/teams/{teamId}/logger/retention` — CRUD + execute (5 endpoints)
**TraceController** — `/api/v1/teams/{teamId}/logger/traces` — query, get-by-trace-id, analyze (4+ endpoints)

### MCP Controllers (4)

**DeveloperProfileController** — `/api/v1/teams/{teamId}/mcp/profiles` — CRUD + token management (6+ endpoints)
**McpSessionController** — `/api/v1/mcp/sessions` — CRUD + status updates + tool calls + results (8+ endpoints)
**ProjectDocumentController** — `/api/v1/projects/{projectId}/mcp/documents` — CRUD + versions + flagging (7+ endpoints)
**ActivityFeedController** — `/api/v1/teams/{teamId}/mcp/activity` — GET list (1 endpoint)

### Registry Controllers (10)

**ServiceRegistrationController** — `/api/v1/teams/{teamId}/registry/services` — CRUD (5 endpoints)
**DependencyController** — `/api/v1/registry/services/{serviceId}/dependencies` — CRUD + graph (4+ endpoints)
**PortAllocationController** — `/api/v1/registry/services/{serviceId}/ports` — CRUD + auto-allocate (5 endpoints)
**ConfigEngineController** — `/api/v1/teams/{teamId}/registry/config` — generate docker-compose, generate app config (3+ endpoints)
**TopologyController** — `/api/v1/teams/{teamId}/registry/topology` — GET topology map (1 endpoint)
**SolutionController** — `/api/v1/teams/{teamId}/registry/solutions` — CRUD + members management (7+ endpoints)
**InfraResourceController** — `/api/v1/teams/{teamId}/registry/infra` — CRUD (5 endpoints)
**ApiRouteController** — `/api/v1/registry/services/{serviceId}/routes` — CRUD (4 endpoints)
**EnvironmentConfigController** — `/api/v1/registry/services/{serviceId}/config` — CRUD (4 endpoints)
**WorkstationProfileController (Registry)** — `/api/v1/teams/{teamId}/registry/workstations` — CRUD (5 endpoints)

### Relay Controllers (8)

**ChannelController** — `/api/v1/teams/{teamId}/relay/channels` — CRUD + join/leave/members/archive (9+ endpoints)
**MessageController** — `/api/v1/relay/channels/{channelId}/messages` — send, list, edit, delete, pin/unpin, thread (8+ endpoints)
**DirectMessageController** — `/api/v1/relay/conversations/{conversationId}/messages` — send, list, edit, delete (4 endpoints)
**DirectConversationController** — `/api/v1/teams/{teamId}/relay/conversations` — create/find, list (3 endpoints)
**PresenceController** — `/api/v1/teams/{teamId}/relay/presence` — update, heartbeat, get-team (3 endpoints)
**PlatformEventController** — `/api/v1/teams/{teamId}/relay/events` — publish, list-undelivered (2 endpoints)
**FileAttachmentController** — `/api/v1/relay/attachments` — upload, download (2 endpoints)
**SearchController** — `/api/v1/teams/{teamId}/relay/search` — search messages across channels (1 endpoint)

---

## 11. Security Architecture

**Authentication:** JWT (HS256), stateless. Access token 24h, Refresh token 30d, MFA Challenge token 5min.

**Token Issuer:** Internal — JwtTokenProvider creates and validates tokens. Secret minimum 32 characters, validated at startup via @PostConstruct.

**Password Encoder:** BCrypt, strength factor 12.

**Principal:** UUID stored in JWT subject claim. Extracted via `SecurityUtils.getCurrentUserId()`.

**Filter Chain Order:**
1. `RequestCorrelationFilter` — Adds X-Correlation-ID to MDC for request tracing
2. `RateLimitFilter` — 10 requests/60 seconds on `/api/v1/auth/**` (in-memory ConcurrentHashMap)
3. `McpTokenAuthFilter` — Intercepts "Bearer mcp_*" tokens, validates via SHA-256 hash lookup, sets MCP principal
4. `JwtAuthFilter` — Extracts JWT from Authorization header, validates signature + expiry + blacklist, sets SecurityContext

**MCP Dual-Auth:** McpTokenAuthFilter handles `mcp_*` prefixed tokens independently from JWT. Validated via SHA-256 hash lookup in McpApiTokenRepository.

---

## 12. Authorization Model

**Public Endpoints (no auth required):**
- `/api/v1/auth/**` — Registration, login, token refresh, MFA
- `/api/v1/health` — Server health check
- `/api/v1/courier/health` — Courier module health check
- `/api/v1/fleet/health` — Fleet module health check
- `/swagger-ui/**`, `/v3/api-docs/**` — API documentation
- `/ws/relay/**` — WebSocket handshake (Relay module)

**Protected Endpoints:** All `/api/**` require authentication. Per-method `@PreAuthorize` annotations enforce role-based access. Team membership verified in service layer.

**Roles:** OWNER > ADMIN > MEMBER > VIEWER (team-scoped, stored in TeamMember.role)

**CORS:** Configured via `codeops.cors.allowed-origins` property (default: localhost:3000)

**CSRF:** Disabled — stateless JWT API, no session cookies.

**Security Headers:** Content-Security-Policy default-src 'self', X-Frame-Options DENY, Strict-Transport-Security max-age=31536000.

**Rate Limiting:**
- `/api/v1/auth/**`: 10 requests per 60 seconds per IP (RateLimitFilter, in-memory ConcurrentHashMap)
- MCP tokens: In-memory rate limiting via McpSecurityService

**Token Validation Flow:** Extract from header → Check signature → Check expiry → Check blacklist (Redis) → Set SecurityContext

---

## 13. Exception Handling

**GlobalExceptionHandler** (`@RestControllerAdvice`):

| Exception | HTTP Status | Response |
|---|---|---|
| NotFoundException | 404 | ErrorResponse(404, message) |
| ValidationException | 400 | ErrorResponse(400, message) |
| AuthorizationException | 403 | ErrorResponse(403, message) |
| MethodArgumentNotValidException | 400 | ErrorResponse(400, comma-separated field errors) |
| HttpMessageNotReadableException | 400 | ErrorResponse(400, "Malformed request body") |
| NoResourceFoundException | 404 | ErrorResponse(404, "Resource not found") |
| CodeOpsException | 500 | ErrorResponse(500, "An internal error occurred") — masks internal details |
| Exception (catch-all) | 500 | ErrorResponse(500, "An internal error occurred") — masks internal details |

**Response format:** `ErrorResponse` record — `{ "status": int, "message": String }`

All exceptions are logged with correlation ID from MDC. Stack traces never exposed to clients.

---

## 14. DTO Mapping Layer

All mappers are MapStruct interfaces with `@Mapper(componentModel = "spring")` and `@Builder(disableBuilder = true)`. All handle Lombok `boolean is*` field naming with explicit `@Mapping(target = "isXxx", source = "xxx")` annotations.

### Core Mappers (15)

UserMapper, TeamMapper, ProjectMapper, QaJobMapper, AgentRunMapper, FindingMapper, RemediationTaskMapper, BugInvestigationMapper, ComplianceItemMapper, TechDebtItemMapper, DependencyScanMapper, DirectiveMapper, PersonaMapper, GitHubConnectionMapper, JiraConnectionMapper

### Courier Mappers (13)

CollectionMapper, FolderMapper, RequestMapper, RequestHeaderMapper, RequestParamMapper, RequestBodyMapper, RequestAuthMapper, RequestScriptMapper, EnvironmentMapper, EnvironmentVariableMapper, GlobalVariableMapper, HistoryMapper, RunResultMapper

### Fleet Mappers (10)

ContainerInstanceMapper, ServiceProfileMapper, PortMappingMapper, VolumeMountMapper, NetworkConfigMapper, FleetEnvironmentVariableMapper, ContainerHealthCheckMapper, DeploymentRecordMapper, SolutionProfileMapper, WorkstationProfileMapper

### Logger Mappers (13)

LogEntryMapper, LogSourceMapper, TraceSpanMapper, MetricMapper, MetricSeriesMapper, LogTrapMapper, TrapConditionMapper, AlertRuleMapper, AlertChannelMapper, AlertHistoryMapper, DashboardMapper, DashboardWidgetMapper, SavedQueryMapper

### Registry Mappers (11)

ServiceRegistrationMapper, ServiceDependencyMapper, PortAllocationMapper, PortRangeMapper, ApiRouteMapper, EnvironmentConfigMapper, InfraResourceMapper, SolutionMapper, SolutionMemberMapper, WorkstationProfileMapper, ConfigTemplateMapper

### Relay Mappers (11)

ChannelMapper, ChannelMemberMapper, MessageMapper, MessageThreadMapper, DirectConversationMapper, DirectMessageMapper, ReactionMapper, FileAttachmentMapper, PinnedMessageMapper, ReadReceiptMapper, UserPresenceMapper

---

## 15. Utilities & Cross-Cutting Concerns

**AppConstants.java** — 200+ static final constants organized by module:
- Auth: TOKEN_PREFIX, MAX_LOGIN_ATTEMPTS, MFA_CODE_LENGTH, etc.
- Team: MAX_TEAMS_PER_USER, MAX_MEMBERS_PER_TEAM, etc.
- Pagination: DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE
- File: MAX_FILE_SIZE_BYTES, ALLOWED_CONTENT_TYPES
- Courier: MAX_COLLECTIONS, MAX_REQUESTS_PER_FOLDER, SCRIPT_TIMEOUT_MS, MAX_ITERATIONS
- Fleet: Container limits, health check defaults
- Logger: MAX_LOG_ENTRY_SIZE, DEFAULT_RETENTION_DAYS, MAX_QUERY_RESULTS
- Registry: PORT_RANGE_MIN/MAX, SLUG_MAX_LENGTH, MAX_SERVICES_PER_TEAM
- Relay: MAX_MESSAGE_LENGTH, MAX_CHANNELS_PER_TEAM, PRESENCE_TIMEOUT_SECONDS

**SecurityUtils.java** — Static utility methods:
- getCurrentUserId(): UUID — Extracts user ID from SecurityContext
- hasRole(TeamRole role): boolean — Checks role in current context
- isAdmin(): boolean — Checks for ADMIN or OWNER role

**SlugUtils.java** — URL-safe identifier generation:
- generateSlug(String name): String — Lowercases, replaces non-alphanumeric with hyphens, trims
- makeUnique(String slug, Set<String> existing): String — Appends numeric suffix if collision
- validateSlug(String slug): boolean — Validates format and length (max 63 chars)

**RequestCorrelationFilter** — Generates UUID correlation ID, sets in MDC (correlationId), adds X-Correlation-ID response header.

**JacksonConfig** — Custom ObjectMapper: JavaTimeModule, LenientInstantDeserializer (falls back to LocalDateTime.parse for timestamps without timezone suffix), FAIL_ON_UNKNOWN_PROPERTIES=false.

---

## 16. Database Schema Summary

**Engine:** PostgreSQL 16 (Alpine)
**Schema:** `public` — all application tables
**Total Tables:** ~84 (25 core + 18 courier + 14 fleet + 16 logger + 8 mcp + 11 registry + 12 relay)
**Schema Management:** Hibernate ddl-auto: update (dev), validate (prod), create-drop (test)
**Naming Strategy:** Spring Physical Naming Strategy (camelCase → snake_case)

Notable patterns:
- All UUID primary keys (except AuditLog with Long IDENTITY and SystemSetting with String key)
- @Version optimistic locking on QaJob, AgentRun, Finding, RemediationTask, TechDebtItem
- Cross-module references use raw UUID columns (no JPA FK) for decoupling (Registry, Relay, Logger modules)
- Single @ManyToMany: RemediationTask ↔ Finding via remediation_task_findings join table
- Self-referential: Folder (parent-child hierarchy in Courier module)

No Flyway migrations — development uses Hibernate schema generation exclusively.

---

## 17. Message Broker

**Broker:** Apache Kafka (Confluent 7.5.0) at localhost:9094 (external) / 29094 (internal).
**Zookeeper:** Confluent 7.5.0 at localhost:2181.

**Topics (10):**
```
codeops-logs          — Log entries from external services
codeops-audit         — Audit log events
codeops-health        — Health check results
codeops-alerts        — Alert notifications
codeops-qa            — QA job lifecycle events
codeops-findings      — Finding creation/status events
codeops-tasks         — Remediation task events
codeops-metrics       — Metric data points
codeops-events        — Platform events (cross-module)
codeops-notifications — User notification dispatch
```

**Consumer:** `KafkaLogConsumer.consume()` — @KafkaListener on `codeops-logs` topic, consumer group `codeops-server`. StringDeserializer for key and value. auto-offset-reset: earliest. 3 retries with 1-second backoff via RetryTemplate.

**Producer:** KafkaTemplate configured but primarily used for internal event publishing. No explicit producers detected in service layer (most cross-module communication is in-process).

---

## 18. Caching

**Engine:** Redis 7 (Alpine) at localhost:6379.

**Usage:**
- **TokenBlacklistService** — Redis SET operations with TTL matching token expiry. Keys: `blacklist:{token}`. Used for JWT logout/revocation.
- **RateLimitFilter** — In-memory ConcurrentHashMap (NOT Redis). Per-IP counters with 60-second sliding window.
- **McpSecurityService** — In-memory token validation cache.

**No @Cacheable/@CacheEvict annotations detected.** Redis is used exclusively for token blacklist, not general-purpose caching.

---

## 19. Environment Variables

| Variable | Used In | Default | Required in Prod |
|----------|---------|---------|-----------------|
| DB_HOST | application-prod.yml | localhost | YES |
| DB_PORT | application-prod.yml | 5432 | YES |
| DB_NAME | application-prod.yml | codeops | YES |
| DB_USER | application-prod.yml | codeops | YES |
| DB_PASSWORD | application-prod.yml | codeops | YES |
| JWT_SECRET | application-prod.yml | (dev secret in dev profile) | YES |
| AWS_S3_BUCKET | application-prod.yml | (none) | YES |
| AWS_S3_REGION | application-prod.yml | (none) | YES |
| MAIL_FROM_EMAIL | application-prod.yml | noreply@codeops.dev | NO |

Total: 9 environment variables (8 required in production).

---

## 20. External Service Dependencies

| Service | Used By | Purpose |
|---------|---------|---------|
| GitHub API | GitHubConnectionService | Repository data, webhooks |
| Jira API | JiraConnectionService, BugInvestigationService | Issue creation, comments, RCA posting |
| Microsoft Teams Webhooks | TeamsWebhookService | Alert/notification delivery (SSRF protection enabled) |
| Docker Engine API | DockerEngineService | Container lifecycle management via Unix socket/HTTP |
| AWS S3 | S3StorageService | Report storage, spec files, persona content |
| AWS SES | EmailService | Transactional email (MFA codes, notifications, invitations) |

**Internal Architecture:** Monolith — all 7 modules run in the same JVM process. Cross-module references use raw UUID columns instead of JPA ForeignKey for logical decoupling. This pattern is used by Registry (teamId), Relay (all IDs), Logger (teamId), and Courier (teamId, createdBy).

---

## 21. Technical Debt

| ID | Issue | Location | Severity | Notes |
|----|-------|----------|----------|-------|
| TD-01 | TODO: Key derivation invalidating encrypted data | EncryptionService.java:56 | CRITICAL | Key rotation would invalidate all encrypted credentials (GitHub PATs, Jira tokens) |
| TD-02 | TODO: Add junixsocket dependency | DockerConfig.java:17 | CRITICAL | Docker socket support requires junixsocket for Unix domain sockets |
| TD-03 | ARCHIVE action not yet implemented | RetentionExecutor.java:82 | CRITICAL | RetentionAction.ARCHIVE exists in enum but executor only handles PURGE |
| TD-04 | S3Presigner not yet implemented | S3StorageService.java:157 | CRITICAL | generatePresignedUrl() is stubbed — returns null or throws |
| TD-05 | Potential SQL string concatenation | LogQueryService (SEC-03 area) | MEDIUM | Criteria API usage needs review for parameter binding |
| TD-06 | System.out/printStackTrace calls (4 instances) | Various | LOW | Should use SLF4J logging framework instead |

---

## 22. Snyk Security Scan

**Scan Date:** 2026-03-03
**Snyk CLI Version:** 1.1303.0
**Package Manager:** Maven

### Open Source Dependency Vulnerabilities (64 total)

| Severity | Count |
|----------|-------|
| Critical | 5 |
| High | 29 |
| Medium | 21 |
| Low | 9 |

**Critical Vulnerabilities:**

| Package | Version | CVE/Issue | Description |
|---------|---------|-----------|-------------|
| tomcat-embed-core | 10.1.24 | SNYK-JAVA-ORGAPACHETOMCATEMBED-* | Uncaught Exception — DoS via malformed HTTP requests |
| tomcat-embed-core | 10.1.24 | SNYK-JAVA-ORGAPACHETOMCATEMBED-* | TOCTOU Race Condition — potential request smuggling |
| spring-security-core | 6.3.0 | SNYK-JAVA-ORGSPRINGFRAMEWORKSECURITY-* | Missing Authorization — authentication bypass under specific conditions |
| spring-security-web | 6.3.0 | SNYK-JAVA-ORGSPRINGFRAMEWORKSECURITY-* | Authentication Bypass — filter chain misconfiguration exploit |

**High-Severity Highlights:**

| Package | Version | Vuln Count | Key Issues |
|---------|---------|------------|------------|
| netty (various modules) | 4.1.110 | 6 (4 HIGH) | HTTP/2 resource exhaustion, header smuggling |
| kafka-clients | 3.7.0 | 5 (3 HIGH) | Deserialization vulnerabilities |
| spring-core/web/webmvc | 6.1.8 | 8 (3 HIGH) | Path traversal, DoS |
| jackson-databind | 2.17.1 | 3 (2 HIGH) | Deserialization gadgets |

**Recommended Remediation:** Upgrade Spring Boot from 3.3.0 to latest 3.3.x patch release (3.3.7+) to resolve most transitive dependency vulnerabilities.

### Code Vulnerabilities (SAST)
**Status:** Skipped — Snyk Code returned exit code 2 (analysis error).

### Infrastructure as Code (IaC)
**Status:** Skipped — No valid IaC files detected by scanner (docker-compose.yml not classified as IaC by Snyk).
