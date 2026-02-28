# CodeOps-Server — Codebase Audit

**Audit Date:** 2026-02-28T21:02:36Z
**Branch:** main
**Commit:** 30465ca90c67b0413d9bbfa330d1e90adc2fc91e
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
Repository URL: https://github.com/adamallard/CodeOps-Server
Primary Language / Framework: Java / Spring Boot 3.3.0
Java Version: 21 (target) / 25 (runtime)
Build Tool + Version: Maven 3.9.x (Maven Wrapper)
Current Branch: main
Latest Commit Hash: 30465ca90c67b0413d9bbfa330d1e90adc2fc91e
Audit Timestamp: 2026-02-28T21:02:36Z
```

---

## 2. Directory Structure

Spring Boot monolith with 7 logical modules under a single Maven artifact. 900+ Java source files.

```
CodeOps-Server/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── start-codeops.sh
├── scripts/seed-codeops.sh
├── src/main/java/com/codeops/
│   ├── CodeOpsApplication.java          ← Entry point
│   ├── config/                          ← 16 config classes
│   ├── controller/                      ← 18 core controllers
│   ├── dto/request/                     ← 42 core request DTOs
│   ├── dto/response/                    ← 40 core response DTOs
│   ├── entity/                          ← 27 core entities
│   ├── entity/enums/                    ← 25 core enums
│   ├── exception/                       ← 4 custom exceptions
│   ├── notification/                    ← 3 notification classes
│   ├── repository/                      ← 26 core repositories
│   ├── security/                        ← 5 security classes
│   ├── service/                         ← 26 core services
│   ├── courier/                         ← Courier module (API testing)
│   │   ├── config/                      ← HttpClientConfig
│   │   ├── controller/                  ← 13 controllers
│   │   ├── dto/mapper/                  ← 13 MapStruct mappers
│   │   ├── dto/request/                 ← 31 request DTOs
│   │   ├── dto/response/               ← 29 response DTOs
│   │   ├── entity/                      ← 18 entities
│   │   ├── entity/enums/               ← 7 enums
│   │   ├── repository/                  ← 18 repositories
│   │   └── service/                     ← 22 services
│   ├── fleet/                           ← Fleet module (Docker mgmt)
│   │   ├── config/                      ← DockerConfig
│   │   ├── controller/                  ← 8 controllers
│   │   ├── dto/mapper/                  ← 10 mappers
│   │   ├── entity/                      ← 11 entities
│   │   ├── entity/enums/               ← 6 enums
│   │   ├── repository/                  ← 14 repositories
│   │   └── service/                     ← 6 services
│   ├── logger/                          ← Logger module (logging/monitoring)
│   │   ├── controller/                  ← 10 controllers
│   │   ├── dto/mapper/                  ← 15 mappers
│   │   ├── entity/                      ← 14 entities
│   │   ├── entity/enums/               ← 10 enums
│   │   ├── repository/                  ← 14 repositories
│   │   └── service/                     ← 20 services
│   ├── mcp/                             ← MCP module (Model Context Protocol)
│   │   ├── controller/                  ← 5 controllers
│   │   ├── dto/mapper/                  ← 8 mappers
│   │   ├── entity/                      ← 9 entities
│   │   ├── repository/                  ← 8 repositories
│   │   ├── security/                    ← McpTokenAuthFilter
│   │   └── service/                     ← 8 services
│   ├── registry/                        ← Registry module (service registry)
│   │   ├── controller/                  ← 10 controllers
│   │   ├── entity/                      ← 18 entities
│   │   ├── entity/enums/               ← 8 enums
│   │   ├── repository/                  ← 11 repositories
│   │   ├── service/                     ← 10 services
│   │   └── util/                        ← SlugUtils
│   └── relay/                           ← Relay module (messaging/WebSocket)
│       ├── config/                      ← WebSocketConfig
│       ├── controller/                  ← 8 controllers
│       ├── dto/mapper/                  ← 11 mappers
│       ├── entity/                      ← 14 entities
│       ├── entity/enums/               ← 6 enums
│       ├── repository/                  ← 12 repositories
│       └── service/                     ← 8 services
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── logback-spring.xml
└── src/test/
    ├── 225 unit test files
    └── 16 integration test files
```

---

## 3. Build & Dependency Manifest

**File:** `pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.3.0 | REST API framework |
| spring-boot-starter-data-jpa | 3.3.0 | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.3.0 | Authentication/authorization |
| spring-boot-starter-validation | 3.3.0 | Bean validation (Jakarta) |
| spring-boot-starter-websocket | 3.3.0 | WebSocket/STOMP support |
| spring-boot-starter-mail | 3.3.0 | Email sending |
| spring-kafka | 3.3.0 | Kafka producer/consumer |
| postgresql | 42.7.3 | PostgreSQL JDBC driver |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT token creation/validation |
| lombok | 1.18.42 (override) | Boilerplate reduction |
| mapstruct | 1.5.5.Final | DTO mapping |
| mapstruct-processor | 1.5.5.Final | MapStruct annotation processor |
| software.amazon.awssdk:s3 | 2.25.0 | AWS S3 storage |
| graalvm-polyglot | 24.1.1 | Script execution engine (Courier) |
| spring-boot-starter-test | 3.3.0 | Test framework |
| testcontainers (postgresql) | 1.19.8 | Integration test containers |
| mockito-core | 5.21.0 (override) | Mocking framework |
| byte-buddy | 1.18.4 (override) | Bytecode generation (Java 25 compat) |
| jacoco-maven-plugin | 0.8.14 | Code coverage |

**Build plugins:** maven-compiler-plugin (Java 21, Lombok + MapStruct processors), maven-surefire-plugin (--add-opens for Java 25), spring-boot-maven-plugin, jacoco-maven-plugin.

```
Build: ./mvnw clean package -DskipTests
Test: ./mvnw test
Run: ./mvnw spring-boot:run
Package: ./mvnw clean package
```

---

## 4. Configuration & Infrastructure Summary

- **`application.yml`** — `src/main/resources/application.yml` — Sets `spring.application.name: codeops-server`, `spring.profiles.active: dev`, `server.port: 8090`.
- **`application-dev.yml`** — `src/main/resources/application-dev.yml` — PostgreSQL `localhost:5432/codeops` (user/pass: `codeops/codeops`), Hibernate `ddl-auto: update`, Kafka `localhost:9094`, JWT dev fallback secret, encryption dev key, CORS `localhost:3000,5173`, S3 disabled (local fallback `~/.codeops/storage`), mail disabled (logged to console), Fleet Docker `tcp://localhost:2375`, DEBUG logging.
- **`application-prod.yml`** — `src/main/resources/application-prod.yml` — All secrets from env vars (`$DATABASE_URL`, `$DATABASE_USERNAME`, `$DATABASE_PASSWORD`, `$JWT_SECRET`, `$ENCRYPTION_KEY`, `$CORS_ALLOWED_ORIGINS`, `$S3_BUCKET`, `$AWS_REGION`, `$MAIL_FROM_EMAIL`), Hibernate `ddl-auto: validate`, S3 enabled, mail enabled, INFO logging.
- **`logback-spring.xml`** — `src/main/resources/logback-spring.xml` — dev: human-readable console; prod: JSON via LogstashEncoder with MDC fields (correlationId, userId, teamId, requestPath); test: WARN only.
- **`docker-compose.yml`** — PostgreSQL 16 Alpine (5432, `codeops-db`), Redis 7 Alpine (6379, `codeops-redis`), Zookeeper (2181), Kafka Confluent 7.5.0 (9092/9094, `codeops-kafka`), kafka-init creates 10 topics.
- **`Dockerfile`** — Eclipse Temurin 21 JRE Alpine, non-root user `appuser`, exposes 8090.
- **`.env`** — Not committed. `.env.example` not present.

**Connection map:**
```
Database: PostgreSQL, localhost, 5432, codeops
Cache: Redis, localhost, 6379 (declared in docker-compose, not used in app code)
Message Broker: Kafka, localhost, 9094, 10 topics (codeops.core.decision.*, codeops.core.outcome.*, codeops.core.hypothesis.*, codeops.integrations.sync, codeops.notifications)
External APIs: Microsoft Teams webhooks (SSRF-protected), SMTP mail
Cloud Services: AWS S3 (optional, disabled in dev)
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

- **Entry point:** `com.codeops.CodeOpsApplication` (standard Spring Boot main class)
- **@PostConstruct:** `JwtTokenProvider` validates JWT secret length (min 32 chars)
- **Seed data:** `DataSeeder` (`@Profile("dev")`, `CommandLineRunner`) seeds users, teams, projects, personas, directives, jobs, findings, tasks, specs, compliance items, tech debt, scans, vulnerabilities, health snapshots, system settings, audit logs, and module-specific data (Registry, Logger, Courier). Skips if `userRepository.count() > 0`.
- **Scheduled tasks:** `MfaService.cleanupExpiredCodes()` — `@Scheduled(fixedRate = 900_000)` (every 15 min), deletes expired MFA email codes.
- **Health check:** GET `/api/v1/health` — returns `{"status":"UP","service":"codeops-server","timestamp":"<ISO-8601>"}` (public, no auth required). Also `/api/v1/courier/health` and `/api/v1/fleet/health`.

---

## 6. Entity / Data Model Layer

### BaseEntity (Abstract Superclass)

**File:** `src/main/java/com/codeops/entity/BaseEntity.java`
**Lombok:** `@Getter`, `@Setter`, `@MappedSuperclass`

| Field | Type | Column | Annotations |
|-------|------|--------|-------------|
| `id` | `UUID` | `id` | `@Id`, `@GeneratedValue(UUID)`, `@Column(nullable=false, updatable=false)` |
| `createdAt` | `Instant` | `created_at` | `@Column(nullable=false, updatable=false)`, set via `@PrePersist` |
| `updatedAt` | `Instant` | `updated_at` | `@Column(nullable=false)`, set via `@PrePersist` + `@PreUpdate` |

22 core entities extend BaseEntity. 4 standalone: AuditLog, MfaEmailCode, SystemSetting, ProjectDirective.

### Core Entities (com.codeops.entity)

#### AgentRun
**Table:** `agent_runs` | **Extends:** BaseEntity | **@Version:** Yes (Long)
**Fields:** `job` (QaJob, @ManyToOne LAZY), `agentType` (AgentType enum), `status` (AgentStatus enum), `result` (AgentResult enum), `s3ReportKey` (String 500), `summaryMd` (TEXT), `findingsCount` (Integer, default 0), `startedAt` (Instant), `completedAt` (Instant), `version` (Long @Version)

#### AuditLog
**Table:** `audit_log` | **Extends:** STANDALONE (own UUID PK)
**Fields:** `id` (UUID), `userId` (UUID), `action` (String 100), `entityType` (String 100), `entityId` (String 100), `details` (TEXT), `ipAddress` (String 45), `timestamp` (Instant, default Instant.now())

#### BugInvestigation
**Table:** `bug_investigations` | **Extends:** BaseEntity
**Fields:** `job` (QaJob @ManyToOne LAZY), `title` (String 500), `description` (TEXT), `rootCauseMd` (TEXT), `impactMd` (TEXT), `stepsToReproduceMd` (TEXT), `suggestedFixMd` (TEXT), `affectedFiles` (TEXT), `severity` (Severity enum), `isConfirmed` (Boolean, default false)

#### ComplianceItem
**Table:** `compliance_items` | **Extends:** BaseEntity
**Fields:** `job` (QaJob @ManyToOne LAZY), `agentType` (AgentType enum), `ruleName` (String 200), `description` (TEXT), `status` (ComplianceStatus enum), `evidenceMd` (TEXT), `remediationMd` (TEXT)

#### DependencyScan
**Table:** `dependency_scans` | **Extends:** BaseEntity
**Fields:** `project` (Project @ManyToOne LAZY), `job` (QaJob @ManyToOne LAZY), `totalDependencies` (Integer, default 0), `vulnerableCount` (Integer, default 0), `outdatedCount` (Integer, default 0), `scanToolVersion` (String 100)

#### DependencyVulnerability
**Table:** `dependency_vulnerabilities` | **Extends:** BaseEntity
**Fields:** `scan` (DependencyScan @ManyToOne LAZY), `packageName` (String 200), `currentVersion` (String 50), `fixedVersion` (String 50), `severity` (Severity enum), `cveId` (String 50), `title` (String 500), `description` (TEXT), `status` (VulnerabilityStatus enum, default OPEN)

#### Directive
**Table:** `directives` | **Extends:** BaseEntity
**Fields:** `name` (String 200), `category` (DirectiveCategory enum), `scope` (DirectiveScope enum), `contentMd` (TEXT), `team` (Team @ManyToOne LAZY, nullable), `createdBy` (User @ManyToOne LAZY), `isActive` (Boolean, default true), `version` (Integer, default 1 — business version, NOT @Version)

#### Finding
**Table:** `findings` | **Extends:** BaseEntity | **@Version:** Yes (Long)
**Fields:** `job` (QaJob @ManyToOne LAZY), `agentRun` (AgentRun @ManyToOne LAZY, nullable), `agentType` (AgentType enum), `severity` (Severity enum), `title` (String 500), `description` (TEXT), `filePath` (String 500), `lineNumber` (Integer), `codeSnippet` (TEXT), `suggestionMd` (TEXT), `debtCategory` (DebtCategory enum), `effortEstimate` (Effort enum), `status` (FindingStatus enum, default OPEN), `version` (Long @Version)
**Indexes:** `idx_finding_job_id`, `idx_finding_severity`, `idx_finding_status`

#### GitHubConnection
**Table:** `github_connections` | **Extends:** BaseEntity
**Fields:** `team` (Team @ManyToOne LAZY), `name` (String 100), `authType` (GitHubAuthType enum), `encryptedToken` (String 2000), `sshKeyFingerprint` (String 100), `apiUrl` (String 500, default "https://api.github.com"), `lastValidatedAt` (Instant), `isActive` (Boolean, default true)

#### HealthSchedule
**Table:** `health_schedules` | **Extends:** BaseEntity
**Fields:** `project` (Project @ManyToOne LAZY), `scheduleType` (ScheduleType enum), `isActive` (Boolean, default true), `cronExpression` (String 50), `lastRunAt` (Instant), `nextRunAt` (Instant)

#### HealthSnapshot
**Table:** `health_snapshots` | **Extends:** BaseEntity
**Fields:** `project` (Project @ManyToOne LAZY), `healthScore` (Integer), `criticalCount` (Integer, default 0), `highCount` (Integer, default 0), `mediumCount` (Integer, default 0), `lowCount` (Integer, default 0), `totalFindings` (Integer, default 0), `capturedAt` (Instant)

#### Invitation
**Table:** `invitations` | **Extends:** BaseEntity
**Fields:** `team` (Team @ManyToOne LAZY), `invitedBy` (User @ManyToOne LAZY), `email` (String 255), `role` (TeamRole enum), `token` (String, unique), `status` (InvitationStatus enum, default PENDING), `expiresAt` (Instant)

#### JiraConnection
**Table:** `jira_connections` | **Extends:** BaseEntity
**Fields:** `team` (Team @ManyToOne LAZY), `name` (String 100), `baseUrl` (String 500), `email` (String 255), `encryptedApiToken` (String 2000), `isActive` (Boolean, default true)

#### MfaEmailCode
**Table:** `mfa_email_codes` | **Extends:** STANDALONE (own UUID PK)
**Fields:** `id` (UUID), `userId` (UUID — raw FK, not @ManyToOne), `codeHash` (String 255), `expiresAt` (Instant), `used` (boolean primitive, default false), `createdAt` (Instant, default Instant.now())

#### NotificationPreference
**Table:** `notification_preferences` | **Extends:** BaseEntity
**Unique:** `(user_id, event_type)`
**Fields:** `user` (User @ManyToOne LAZY), `eventType` (String 50), `inApp` (Boolean, default true), `email` (Boolean, default false)

#### Persona
**Table:** `personas` | **Extends:** BaseEntity
**Fields:** `name` (String 100), `agentType` (AgentType enum), `description` (TEXT), `contentMd` (TEXT), `scope` (Scope enum), `team` (Team @ManyToOne LAZY, nullable), `createdBy` (User @ManyToOne LAZY), `isDefault` (Boolean, default false), `version` (Integer, default 1 — business version)

#### Project
**Table:** `projects` | **Extends:** BaseEntity
**Fields:** `team` (Team @ManyToOne LAZY), `name` (String 200), `description` (TEXT), `githubConnection` (GitHubConnection @ManyToOne LAZY, nullable), `repoUrl` (String 500), `repoFullName` (String 200), `defaultBranch` (String 100, default "main"), `jiraConnection` (JiraConnection @ManyToOne LAZY, nullable), `jiraProjectKey` (String 20), `jiraDefaultIssueType` (String 50, default "Task"), `jiraLabels` (TEXT), `jiraComponent` (String 100), `techStack` (String 200), `healthScore` (Integer), `lastAuditAt` (Instant), `settingsJson` (TEXT), `isArchived` (Boolean, default false), `createdBy` (User @ManyToOne LAZY)

#### ProjectDirective
**Table:** `project_directives` | **Extends:** STANDALONE (composite @EmbeddedId)
**PK:** `ProjectDirectiveId` (projectId UUID + directiveId UUID)
**Fields:** `project` (Project @ManyToOne LAZY, @MapsId), `directive` (Directive @ManyToOne LAZY, @MapsId), `enabled` (Boolean, default true)

#### QaJob
**Table:** `qa_jobs` | **Extends:** BaseEntity | **@Version:** Yes (Long)
**Fields:** `project` (Project @ManyToOne LAZY), `mode` (JobMode enum), `status` (JobStatus enum), `name` (String 200), `branch` (String 100), `configJson` (TEXT), `summaryMd` (TEXT), `overallResult` (JobResult enum), `healthScore` (Integer), `totalFindings`/`criticalCount`/`highCount`/`mediumCount`/`lowCount` (Integer, all default 0), `jiraTicketKey` (String 50), `startedBy` (User @ManyToOne LAZY), `startedAt`/`completedAt` (Instant), `version` (Long @Version)

#### RemediationTask
**Table:** `remediation_tasks` | **Extends:** BaseEntity | **@Version:** Yes (Long)
**Fields:** `job` (QaJob @ManyToOne LAZY), `taskNumber` (Integer), `title` (String 500), `description` (TEXT), `promptMd` (TEXT), `promptS3Key` (String 500), `findings` (List\<Finding\> @ManyToMany LAZY, join table `remediation_task_findings`), `priority` (Priority enum), `status` (TaskStatus enum, default PENDING), `assignedTo` (User @ManyToOne LAZY, nullable), `jiraKey` (String 50), `version` (Long @Version)

#### Specification
**Table:** `specifications` | **Extends:** BaseEntity
**Fields:** `job` (QaJob @ManyToOne LAZY), `name` (String 200), `specType` (SpecType enum), `s3Key` (String 500)

#### SystemSetting
**Table:** `system_settings` | **Extends:** STANDALONE (String PK)
**PK:** `settingKey` (String mapped to column `key`, length 100)
**Fields:** `value` (TEXT), `updatedBy` (User @ManyToOne LAZY, nullable), `updatedAt` (Instant)

#### Team
**Table:** `teams` | **Extends:** BaseEntity
**Fields:** `name` (String 100), `description` (TEXT), `owner` (User @ManyToOne LAZY), `teamsWebhookUrl` (String 500), `settingsJson` (TEXT)

#### TeamMember
**Table:** `team_members` | **Extends:** BaseEntity
**Unique:** `(team_id, user_id)`
**Fields:** `team` (Team @ManyToOne LAZY), `user` (User @ManyToOne LAZY), `role` (TeamRole enum), `joinedAt` (Instant)

#### TechDebtItem
**Table:** `tech_debt_items` | **Extends:** BaseEntity | **@Version:** Yes (Long)
**Fields:** `project` (Project @ManyToOne LAZY), `category` (DebtCategory enum), `title` (String 500), `description` (TEXT), `filePath` (String 500), `effortEstimate` (Effort enum), `businessImpact` (BusinessImpact enum), `status` (DebtStatus enum, default IDENTIFIED), `firstDetectedJob`/`resolvedJob` (QaJob @ManyToOne LAZY, nullable), `version` (Long @Version)

#### User
**Table:** `users` | **Extends:** BaseEntity
**Fields:** `email` (String 255, unique), `passwordHash` (String 255), `displayName` (String 100), `avatarUrl` (String 500), `isActive` (Boolean, default true), `lastLoginAt` (Instant), `mfaEnabled` (Boolean, default false), `mfaMethod` (MfaMethod enum, default NONE), `mfaSecret` (String 500), `mfaRecoveryCodes` (String 2000)

### Courier Module Entities (com.codeops.courier.entity)

18 entities. Key entities: `Collection` (teamId, createdBy, name, description, shared, pre/postScripts), `Folder` (collection @ManyToOne, parentFolder @ManyToOne self-ref, name, sortOrder, authType, authConfig), `Request` (folder @ManyToOne, name, method, url, description, sortOrder), `RequestHeader`, `RequestParam`, `RequestBody` (bodyType BodyType enum, rawContent, formDataJson), `RequestAuth` (authType AuthType enum, configJson), `RequestScript` (scriptType ScriptType enum, scriptContent), `RequestHistory`, `Environment` (collectionId, name, isActive), `EnvironmentVariable`, `GlobalVariable`, `RunResult`, `RunIteration`, `Fork`, `MergeRequest`, `CollectionShare`, `CodeSnippetTemplate`.

All Courier entities extend BaseEntity. All relationships @ManyToOne LAZY.

### Fleet Module Entities (com.codeops.fleet.entity)

11 entities. Key entities: `ServiceProfile` (serviceName, imageName, imageTag, command, healthCheck*, restartPolicy RestartPolicy enum, memoryLimitMb, cpuLimit, startOrder, isAutoGenerated, isEnabled, serviceRegistration @ManyToOne, team @ManyToOne, volumeMounts @OneToMany CascadeType.ALL, networkConfigs @OneToMany CascadeType.ALL, portMappings @OneToMany CascadeType.ALL, environmentVariables @OneToMany CascadeType.ALL), `ContainerInstance` (serviceProfile @ManyToOne, containerId, containerName, status ContainerStatus enum, hostPort, startedAt, stoppedAt), `ContainerLog`, `ContainerHealthCheck`, `VolumeMount`, `NetworkConfig`, `PortMapping`, `EnvironmentVariable`, `SolutionProfile`, `SolutionService`, `WorkstationProfile`, `WorkstationSolution`, `DeploymentRecord`, `DeploymentContainer`.

Fleet is the only module using `@OneToMany` with `CascadeType.ALL` + `orphanRemoval = true` (on ServiceProfile children).

**Fleet Enums:** ContainerStatus (CREATED, RUNNING, PAUSED, RESTARTING, REMOVING, EXITED, DEAD, STOPPED), RestartPolicy (NO, ALWAYS, ON_FAILURE, UNLESS_STOPPED), NetworkMode (BRIDGE, HOST, NONE, CUSTOM), VolumeType (BIND, VOLUME, TMPFS), ProtocolType (TCP, UDP), DeploymentStatus (PENDING, IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK)

### Logger Module Entities (com.codeops.logger.entity)

14 entities. Key entities: `LogSource` (teamId, name, serviceName, sourceType, configJson), `LogEntry` (source @ManyToOne, level LogLevel enum, message TEXT, timestamp, serviceName denormalized, correlationId, traceId, spanId, loggerName, threadName, exception*, customFields, hostName, ipAddress, teamId), `LogTrap` (teamId, name, description, trapType TrapType enum, isEnabled, conditions @OneToMany), `TrapCondition`, `AlertRule` (name, trap @ManyToOne, channel @ManyToOne, severity AlertSeverity enum, isActive, throttleMinutes), `AlertChannel` (teamId, name, channelType AlertChannelType enum, configJson, isEnabled), `AlertHistory`, `Metric` (teamId, name, serviceName, metricType MetricType enum, unit, description, tags), `MetricSeries` (metric @ManyToOne, timestamp, value, tags, resolution), `RetentionPolicy`, `Dashboard`, `DashboardWidget`, `SavedQuery`, `QueryHistory`, `AnomalyBaseline`, `TraceSpan`.

**Logger Enums:** LogLevel (TRACE, DEBUG, INFO, WARN, ERROR, FATAL), AlertSeverity (INFO, WARNING, CRITICAL), AlertStatus (TRIGGERED, ACKNOWLEDGED, RESOLVED), AlertChannelType (EMAIL, WEBHOOK, SLACK, TEAMS), ConditionType (CONTAINS, NOT_CONTAINS, REGEX, EQUALS, GREATER_THAN, LESS_THAN), MetricType (COUNTER, GAUGE, HISTOGRAM, SUMMARY), RetentionAction (DELETE, ARCHIVE, DOWNSAMPLE), SpanStatus (OK, ERROR, TIMEOUT), TrapType (LOG_PATTERN, METRIC_THRESHOLD, ANOMALY), WidgetType (LINE_CHART, BAR_CHART, PIE_CHART, TABLE, STAT, LOG_STREAM)

### MCP Module Entities (com.codeops.mcp.entity)

9 entities: `McpSession` (project @ManyToOne, developerProfile @ManyToOne, status, startedAt, endedAt, summaryMd), `SessionResult`, `SessionToolCall`, `DeveloperProfile` (user @ManyToOne, team @ManyToOne, preferences), `McpApiToken` (teamId, name, tokenHash, expiresAt, isActive), `ProjectDocument`, `ProjectDocumentVersion`, `ActivityFeedEntry`.

### Registry Module Entities (com.codeops.registry.entity)

18 entities: `ServiceRegistration` (teamId, slug unique, name, description, serviceType, port, healthEndpoint, dependsOn), `ServiceDependency`, `ApiRouteRegistration`, `Solution` (teamId, slug unique, name, description), `SolutionMember`, `InfraResource`, `EnvironmentConfig`, `ConfigTemplate`, and related entities.

**Registry Enums:** ServiceType, DependencyType, RouteMethod, InfraResourceType, EnvironmentType, ResourceStatus, ConfigValueType, SolutionStatus

### Relay Module Entities (com.codeops.relay.entity)

14 entities: `Channel` (teamId, name, slug, description, isArchived, topic, isPinned), `ChannelMember` (channel @ManyToOne, userId, role ChannelRole enum, joinedAt), `Message` (channel @ManyToOne, senderId, content TEXT, messageType, parentMessageId, isEdited, isPinned), `MessageThread`, `DirectConversation` (teamId, user1Id, user2Id), `DirectMessage`, `FileAttachment`, `Reaction`, `PinnedMessage`, `ReadReceipt`, `UserPresence`, `PlatformEvent`.

**Relay Enums:** ChannelRole (OWNER, ADMIN, MEMBER), MessageType (TEXT, SYSTEM, FILE, CODE_SNIPPET), PresenceStatus (ONLINE, AWAY, DO_NOT_DISTURB, OFFLINE), EventType (CHANNEL_CREATED, MEMBER_JOINED, MEMBER_LEFT, MESSAGE_PINNED, etc.), FileType (IMAGE, DOCUMENT, VIDEO, AUDIO, CODE, OTHER)

### Entity Summary Statistics

| Metric | Count |
|--------|-------|
| Total entities (all modules) | 100+ |
| Core entities extending BaseEntity | 22 |
| Core standalone entities | 4 |
| Courier entities | 18 |
| Fleet entities | 11 |
| Logger entities | 14 |
| MCP entities | 9 |
| Registry entities | 18 |
| Relay entities | 14 |
| Core @ManyToOne relationships | 38 |
| Core @ManyToMany | 1 (RemediationTask ↔ Finding) |
| Core @OneToMany | 0 (all unidirectional ManyToOne) |
| Fleet @OneToMany with CascadeType.ALL | 4 (ServiceProfile children) |
| Entities with @Version (optimistic locking) | 5 (AgentRun, Finding, QaJob, RemediationTask, TechDebtItem) |
| All fetch strategies | LAZY |
| All enum storage | @Enumerated(STRING) |

---

## 7. Enum Inventory

### Core Enums (com.codeops.entity.enums) — 25 enums

| Enum | Values | Used By |
|------|--------|---------|
| AgentType | SECURITY, CODE_QUALITY, BUILD_HEALTH, COMPLETENESS, API_CONTRACT, TEST_COVERAGE, UI_UX, DOCUMENTATION, DATABASE, PERFORMANCE, DEPENDENCY, ARCHITECTURE | AgentRun, Finding, ComplianceItem, Persona |
| AgentStatus | PENDING, RUNNING, COMPLETED, FAILED | AgentRun |
| AgentResult | PASS, WARN, FAIL | AgentRun |
| BusinessImpact | LOW, MEDIUM, HIGH, CRITICAL | TechDebtItem |
| ComplianceStatus | MET, PARTIAL, MISSING, NOT_APPLICABLE | ComplianceItem |
| DebtCategory | ARCHITECTURE, CODE, TEST, DEPENDENCY, DOCUMENTATION | TechDebtItem, Finding |
| DebtStatus | IDENTIFIED, PLANNED, IN_PROGRESS, RESOLVED | TechDebtItem |
| DirectiveCategory | ARCHITECTURE, STANDARDS, CONVENTIONS, CONTEXT, OTHER | Directive |
| DirectiveScope | TEAM, PROJECT, USER | Directive |
| Effort | S, M, L, XL | Finding, TechDebtItem |
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

### Courier Enums (com.codeops.courier.entity.enums) — 7 enums

| Enum | Values |
|------|--------|
| BodyType | NONE, FORM_DATA, X_WWW_FORM_URLENCODED, RAW_JSON, RAW_XML, RAW_HTML, RAW_TEXT, RAW_YAML, BINARY, GRAPHQL |
| AuthType | NO_AUTH, API_KEY, BEARER_TOKEN, BASIC_AUTH, OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS, OAUTH2_IMPLICIT, OAUTH2_PASSWORD, JWT_BEARER, INHERIT_FROM_PARENT |
| ScriptType | PRE_REQUEST, POST_RESPONSE |
| HttpMethod | GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS |
| ExportFormat | JSON, YAML |
| SharePermission | VIEW, EDIT |
| RunStatus | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED |

### Module Enums Summary

Fleet (6), Logger (10), Registry (8), Relay (6) — see entity sections above for values.

---

## 8. Repository Layer

### Core Repositories (com.codeops.repository) — 26 total

All extend `JpaRepository<Entity, UUID>`. No `@EntityGraph` usage. No projections.

| Repository | Custom Methods |
|---|---|
| AgentRunRepository | `findByJobId(UUID)`, `findByJobIdAndAgentType(UUID, AgentType)`, `deleteAllByJobId(UUID)` |
| AuditLogRepository | `findByUserId(UUID, Pageable)`, `findByEntityTypeAndEntityId(String, String, Pageable)`, `findByAction(String, Pageable)` |
| BugInvestigationRepository | `findByJobId(UUID)`, `deleteAllByProjectId(UUID)` |
| ComplianceItemRepository | `findByJobId(UUID, Pageable)`, `findByJobIdAndStatus(UUID, ComplianceStatus, Pageable)`, `countByJobIdAndStatus(UUID, ComplianceStatus)`, `deleteAllByProjectId(UUID)` |
| DependencyScanRepository | `findByProjectId(UUID, Pageable)`, `findFirstByProjectIdOrderByCreatedAtDesc(UUID)`, `deleteAllByProjectId(UUID)` |
| DependencyVulnerabilityRepository | `findByScanId(UUID, Pageable)`, `findByScanIdAndSeverity(UUID, Severity, Pageable)`, `countByScanIdAndStatus(UUID, VulnerabilityStatus)`, `countByScanIdAndSeverity(UUID, Severity)`, `deleteAllByScanId(UUID)` |
| DirectiveRepository | `findByTeamId(UUID)`, `findByScope(DirectiveScope)`, `findByCreatedById(UUID)`, `deleteAllByTeamId(UUID)` |
| FindingRepository | `findByJobId(UUID, Pageable)`, `findByJobIdAndSeverity(UUID, Severity, Pageable)`, `findByJobIdAndStatus(UUID, FindingStatus, Pageable)`, `countByJobIdAndSeverity(UUID, Severity)`, `countByJobIdAndSeverityAndStatus(UUID, Severity, FindingStatus)`, `deleteAllByProjectId(UUID)` |
| GitHubConnectionRepository | `findByTeamId(UUID)`, `findByTeamIdAndIsActiveTrue(UUID)` |
| HealthScheduleRepository | `findByProjectId(UUID)`, `findByIsActiveTrueAndNextRunAtBefore(Instant)`, `deleteAllByProjectId(UUID)` |
| HealthSnapshotRepository | `findByProjectIdOrderByCapturedAtDesc(UUID)`, `findByProjectId(UUID, Pageable)`, `deleteAllByProjectId(UUID)` |
| InvitationRepository | `findByToken(String)`, `findByTeamIdAndStatus(UUID, InvitationStatus)`, `@Lock(PESSIMISTIC_WRITE) findByTeamIdAndEmailAndStatusForUpdate(UUID, String, InvitationStatus)` |
| JiraConnectionRepository | `findByTeamId(UUID)`, `findByTeamIdAndIsActiveTrue(UUID)` |
| MfaEmailCodeRepository | `findByUserIdAndUsedFalseOrderByCreatedAtDesc(UUID)`, `deleteByUserId(UUID)`, `deleteByExpiresAtBefore(Instant)` |
| NotificationPreferenceRepository | `findByUserId(UUID)`, `findByUserIdAndEventType(UUID, String)` |
| PersonaRepository | `findByTeamId(UUID)`, `findByTeamId(UUID, Pageable)`, `findByTeamIdAndAgentType(UUID, AgentType)`, `findByTeamIdAndAgentTypeAndIsDefaultTrue(UUID, AgentType)`, `findByCreatedById(UUID)`, `findByScope(Scope)` |
| ProjectDirectiveRepository | `findByIdProjectId(UUID)`, `deleteByIdProjectId(UUID)`, `deleteByIdDirectiveId(UUID)` |
| ProjectRepository | `findByTeamId(UUID, Pageable)`, `findByTeamIdAndIsArchivedFalse(UUID)`, `findByTeamIdAndIsArchivedFalse(UUID, Pageable)`, `countByTeamId(UUID)` |
| QaJobRepository | `findByProjectId(UUID, Pageable)`, `findByStartedById(UUID, Pageable)`, `findByProjectIdOrderByCreatedAtDesc(UUID)`, `deleteAllByProjectId(UUID)` |
| RemediationTaskRepository | `findByJobId(UUID, Pageable)`, `findByAssignedToId(UUID, Pageable)`, `deleteAllByJobId(UUID)`, `@Query(native) deleteJoinTableByProjectId(UUID)`, `deleteAllByProjectId(UUID)` |
| SpecificationRepository | `findByJobId(UUID)`, `deleteAllByProjectId(UUID)` |
| SystemSettingRepository | extends `JpaRepository<SystemSetting, String>` (String PK) |
| TeamMemberRepository | `findByTeamId(UUID)`, `findByUserId(UUID)`, `findByTeamIdAndUserId(UUID, UUID)`, `existsByTeamIdAndUserId(UUID, UUID)`, `countByTeamId(UUID)` |
| TeamRepository | (standard JpaRepository methods only) |
| TechDebtItemRepository | `findByProjectId(UUID, Pageable)`, `findByProjectIdAndStatus(UUID, DebtStatus, Pageable)`, `findByProjectIdAndCategory(UUID, DebtCategory, Pageable)`, `findByProjectId(UUID)`, `countByProjectIdAndStatus(UUID, DebtStatus)`, `deleteAllByProjectId(UUID)` |
| UserRepository | `findByEmail(String)`, `existsByEmail(String)`, `findByDisplayNameContainingIgnoreCase(String, Pageable)` |

**Notable patterns:**
- 1 native query: `RemediationTaskRepository.deleteJoinTableByProjectId`
- 1 pessimistic lock: `InvitationRepository.findByTeamIdAndEmailAndStatusForUpdate`
- 15 repositories have bulk `deleteAllByProjectId()` (used in ProjectService cascade delete)

### Module Repositories

**Courier:** 18 repos (Collection, Folder, Request, RequestHeader, RequestParam, RequestBody, RequestAuth, RequestScript, RequestHistory, Environment, EnvironmentVariable, GlobalVariable, RunResult, RunIteration, Fork, MergeRequest, CollectionShare, CodeSnippetTemplate)

**Fleet:** 14 repos (ServiceProfile, ContainerInstance, ContainerLog, ContainerHealthCheck, VolumeMount, NetworkConfig, PortMapping, EnvironmentVariable, SolutionProfile, SolutionService, WorkstationProfile, WorkstationSolution, DeploymentRecord, DeploymentContainer)

**Logger:** 14 repos (LogSource, LogEntry, LogTrap, TrapCondition, AlertRule, AlertChannel, AlertHistory, Metric, MetricSeries, RetentionPolicy, Dashboard, DashboardWidget, SavedQuery, QueryHistory, AnomalyBaseline, TraceSpan)

**MCP:** 8 repos (McpSession, SessionResult, SessionToolCall, DeveloperProfile, McpApiToken, ProjectDocument, ProjectDocumentVersion, ActivityFeedEntry)

**Registry:** 11 repos (ServiceRegistration, ServiceDependency, ApiRouteRegistration, Solution, SolutionMember, InfraResource, EnvironmentConfig, ConfigTemplate, and related)

**Relay:** 12 repos (Channel, ChannelMember, Message, MessageThread, DirectConversation, DirectMessage, FileAttachment, Reaction, PinnedMessage, ReadReceipt, UserPresence, PlatformEvent)

---

## 9. Service Layer — Full Method Signatures

### Core Services (com.codeops.service) — 26 services

#### AdminService
**Injects:** UserRepository, TeamRepository, TeamMemberRepository, ProjectRepository, QaJobRepository, SystemSettingRepository
- `getSystemStats()`: SystemStatsResponse — system-wide counts
- `getSystemSettings()`: List\<SystemSettingResponse\> — all settings
- `updateSystemSetting(UpdateSystemSettingRequest)`: SystemSettingResponse — create/update setting
- `getAllUsers(Pageable)`: PageResponse\<UserResponse\> — paginated user list

#### AgentRunService
**Injects:** AgentRunRepository, QaJobRepository, TeamMemberRepository
- `createAgentRun(CreateAgentRunRequest)`: AgentRunResponse — @Transactional
- `createAgentRuns(UUID jobId, List<AgentType>)`: List\<AgentRunResponse\> — batch create
- `getAgentRuns(UUID jobId)`: List\<AgentRunResponse\> — readOnly
- `updateAgentRun(UUID, UpdateAgentRunRequest)`: AgentRunResponse — @Transactional
- `deleteAgentRun(UUID)`: void — @Transactional

#### AuditLogService
**Injects:** AuditLogRepository
- `@Async log(UUID userId, String action, String entityType, String entityId, String details, String ip)`: void
- `getLogsForUser(UUID, Pageable)`: PageResponse — readOnly
- `getLogsForEntity(String, String, Pageable)`: PageResponse — readOnly
- `getLogsByAction(String, Pageable)`: PageResponse — readOnly

#### AuthService
**Injects:** UserRepository, PasswordEncoder, JwtTokenProvider, TeamMemberRepository, MfaEmailCodeRepository, EmailService
- `register(RegisterRequest)`: AuthResponse — @Transactional
- `login(LoginRequest)`: AuthResponse — readOnly (returns MFA challenge token if MFA enabled)
- `refreshToken(RefreshTokenRequest)`: AuthResponse — readOnly
- `logout(LogoutRequest)`: void
- `changePassword(ChangePasswordRequest)`: void — @Transactional

#### BugInvestigationService
**Injects:** BugInvestigationRepository, QaJobRepository, TeamMemberRepository
- `createInvestigation(CreateBugInvestigationRequest)`: BugInvestigationResponse
- `getInvestigationByJob(UUID)`: BugInvestigationResponse — readOnly
- `updateInvestigation(UUID, UpdateBugInvestigationRequest)`: BugInvestigationResponse

#### ComplianceService
**Injects:** ComplianceItemRepository, QaJobRepository, TeamMemberRepository
- `createComplianceItem(CreateComplianceItemRequest)`: ComplianceItemResponse
- `createComplianceItems(List<CreateComplianceItemRequest>)`: List — batch
- `getComplianceItems(UUID jobId, Pageable)`: PageResponse — readOnly
- `getComplianceByStatus(UUID jobId, ComplianceStatus, Pageable)`: PageResponse — readOnly
- `updateComplianceStatus(UUID, UpdateComplianceStatusRequest)`: ComplianceItemResponse

#### DependencyService
**Injects:** DependencyScanRepository, DependencyVulnerabilityRepository, ProjectRepository, TeamMemberRepository
- `createScan(CreateDependencyScanRequest)`: DependencyScanResponse
- `getScansForProject(UUID, Pageable)`: PageResponse — readOnly
- `getLatestScan(UUID)`: DependencyScanResponse — readOnly
- `addVulnerability(CreateVulnerabilityRequest)`: VulnerabilityResponse
- `addVulnerabilities(List)`: List — batch
- `getVulnerabilities(UUID scanId, Pageable)`: PageResponse — readOnly
- `updateVulnerabilityStatus(UUID, UpdateVulnerabilityStatusRequest)`: VulnerabilityResponse

#### DirectiveService
**Injects:** DirectiveRepository, ProjectDirectiveRepository, TeamMemberRepository, UserRepository, TeamRepository, ProjectRepository
- `createDirective(CreateDirectiveRequest)`: DirectiveResponse
- `getDirective(UUID)`: DirectiveResponse — readOnly
- `getDirectivesForTeam(UUID)`: List — readOnly
- `updateDirective(UUID, UpdateDirectiveRequest)`: DirectiveResponse
- `deleteDirective(UUID)`: void
- `getProjectDirectives(UUID projectId)`: List — readOnly
- `assignDirectiveToProject(UUID projectId, UUID directiveId)`: ProjectDirectiveResponse
- `removeDirectiveFromProject(UUID, UUID)`: void
- `toggleProjectDirective(UUID, UUID, boolean)`: ProjectDirectiveResponse

#### EncryptionService
**Injects:** `@Value codeops.encryption.key`
AES-256-GCM encryption. Manual constructor validates key length.
- `encrypt(String)`: String — returns Base64(IV + ciphertext + tag)
- `decrypt(String)`: String — reverses encrypt

#### FindingService
**Injects:** FindingRepository, QaJobRepository, TeamMemberRepository, AgentRunRepository
- `createFinding(CreateFindingRequest)`: FindingResponse
- `createFindings(List)`: List — batch
- `getFindingsForJob(UUID, Pageable)`: PageResponse — readOnly
- `getFindingsBySeverity(UUID, Severity, Pageable)`: PageResponse — readOnly
- `getFindingsByStatus(UUID, FindingStatus, Pageable)`: PageResponse — readOnly
- `updateFindingStatus(UUID, UpdateFindingStatusRequest)`: FindingResponse
- `deleteFinding(UUID)`: void

#### GitHubConnectionService
**Injects:** GitHubConnectionRepository, TeamMemberRepository, EncryptionService
- `createConnection(CreateGitHubConnectionRequest)`: GitHubConnectionResponse — encrypts token
- `getConnectionsForTeam(UUID)`: List — readOnly
- `updateConnection(UUID, UpdateGitHubConnectionRequest)`: GitHubConnectionResponse
- `deleteConnection(UUID)`: void
- `validateConnection(UUID)`: GitHubConnectionResponse

#### HealthMonitorService
**Injects:** HealthScheduleRepository, HealthSnapshotRepository, ProjectRepository, TeamMemberRepository
- `createSchedule(CreateHealthScheduleRequest)`: HealthScheduleResponse
- `getSchedulesForProject(UUID)`: List — readOnly
- `updateSchedule(UUID, UpdateHealthScheduleRequest)`: HealthScheduleResponse
- `deleteSchedule(UUID)`: void
- `createSnapshot(CreateHealthSnapshotRequest)`: HealthSnapshotResponse
- `getSnapshots(UUID, Pageable)`: PageResponse — readOnly

#### JiraConnectionService
**Injects:** JiraConnectionRepository, TeamMemberRepository, EncryptionService
- `createConnection(CreateJiraConnectionRequest)`: JiraConnectionResponse — encrypts token
- `getConnectionsForTeam(UUID)`: List — readOnly
- `updateConnection(UUID, UpdateJiraConnectionRequest)`: JiraConnectionResponse
- `deleteConnection(UUID)`: void

#### MetricsService (core)
**Injects:** ProjectRepository, QaJobRepository, FindingRepository, AgentRunRepository, TeamMemberRepository, ComplianceItemRepository, TechDebtItemRepository, DependencyScanRepository, DependencyVulnerabilityRepository, HealthSnapshotRepository
- `getProjectMetrics(UUID)`: ProjectMetricsResponse — readOnly
- `getTeamMetrics(UUID)`: TeamMetricsResponse — readOnly
- `getHealthTrend(UUID, int days)`: List\<HealthSnapshotResponse\> — readOnly

#### MfaService
**Injects:** UserRepository, PasswordEncoder, JwtTokenProvider, EncryptionService, TeamMemberRepository, ObjectMapper, MfaEmailCodeRepository, EmailService
- `setupMfa(MfaSetupRequest)`: MfaSetupResponse — TOTP setup
- `verifyAndEnableMfa(MfaVerifyRequest)`: MfaStatusResponse
- `setupEmailMfa(MfaEmailSetupRequest)`: MfaRecoveryResponse — email MFA setup
- `verifyEmailSetupAndEnable(MfaVerifyRequest)`: MfaStatusResponse
- `verifyMfaLogin(MfaLoginRequest)`: AuthResponse — completes MFA login
- `sendLoginMfaCode(MfaResendRequest)`: void — resend email code
- `disableMfa(MfaSetupRequest)`: MfaStatusResponse
- `regenerateRecoveryCodes(MfaSetupRequest)`: MfaRecoveryResponse
- `getMfaStatus()`: MfaStatusResponse — readOnly
- `adminResetMfa(UUID)`: void — force reset
- `@Scheduled cleanupExpiredCodes()`: void — every 15 min

#### NotificationService
**Injects:** NotificationPreferenceRepository, UserRepository
- `getPreferences(UUID)`: List — readOnly
- `updatePreference(UUID, UpdateNotificationPreferenceRequest)`: NotificationPreferenceResponse
- `updatePreferences(UUID, List)`: List — batch
- `shouldNotify(UUID, String eventType, String channel)`: boolean — readOnly

#### PersonaService
**Injects:** PersonaRepository, TeamMemberRepository, UserRepository, TeamRepository
- `createPersona(CreatePersonaRequest)`: PersonaResponse
- `getPersona(UUID)`: PersonaResponse — readOnly
- `getPersonasForTeam(UUID, Pageable)`: PageResponse — readOnly
- `getPersonasByAgentType(UUID, AgentType)`: List — readOnly
- `getDefaultPersona(UUID, AgentType)`: PersonaResponse — readOnly
- `getPersonasByUser(UUID)`: List — readOnly
- `getSystemPersonas()`: List — readOnly
- `updatePersona(UUID, UpdatePersonaRequest)`: PersonaResponse — increments version on content change
- `deletePersona(UUID)`: void — SYSTEM prohibited
- `setAsDefault(UUID)`: PersonaResponse
- `removeDefault(UUID)`: PersonaResponse

#### ProjectService
**Injects:** 21 repositories (all child entity repos for cascade delete)
- `createProject(UUID teamId, CreateProjectRequest)`: ProjectResponse
- `getProject(UUID)`: ProjectResponse — readOnly
- `getProjectsForTeam(UUID)`: List — readOnly (non-archived)
- `getAllProjectsForTeam(UUID, boolean, Pageable)`: PageResponse — readOnly
- `updateProject(UUID, UpdateProjectRequest)`: ProjectResponse
- `archiveProject(UUID)`: void
- `unarchiveProject(UUID)`: void
- `deleteProject(UUID)`: void — FK-safe cascade delete (OWNER only, deletes 15 child tables)
- `updateHealthScore(UUID, int)`: void

#### QaJobService
**Injects:** QaJobRepository, AgentRunRepository, FindingRepository, ProjectRepository, UserRepository, TeamMemberRepository, ProjectService
- `createJob(CreateJobRequest)`: JobResponse — PENDING status
- `getJob(UUID)`: JobResponse — readOnly
- `getJobsForProject(UUID, Pageable)`: PageResponse — readOnly
- `getJobsByUser(UUID, Pageable)`: PageResponse — readOnly
- `updateJob(UUID, UpdateJobRequest)`: JobResponse — auto-updates health score on COMPLETED
- `deleteJob(UUID)`: void

#### RemediationTaskService
**Injects:** RemediationTaskRepository, QaJobRepository, UserRepository, TeamMemberRepository, FindingRepository, S3StorageService
- `createTask(CreateTaskRequest)`: TaskResponse
- `createTasks(List)`: List — batch
- `getTasksForJob(UUID, Pageable)`: PageResponse — readOnly
- `getTask(UUID)`: TaskResponse — readOnly
- `getTasksAssignedToUser(UUID, Pageable)`: PageResponse — readOnly
- `updateTask(UUID, UpdateTaskRequest)`: TaskResponse
- `uploadTaskPrompt(UUID, int, String)`: String — uploads to S3/local

#### ReportStorageService
**Injects:** S3StorageService, AgentRunRepository
- `uploadReport(UUID jobId, AgentType, String md)`: String
- `uploadSummaryReport(UUID, String)`: String
- `downloadReport(String s3Key)`: String
- `deleteReportsForJob(UUID)`: void
- `uploadSpecification(UUID, String, byte[], String)`: String
- `downloadSpecification(String)`: byte[]

#### S3StorageService
**Injects:** `@Value s3Enabled, bucket, localStoragePath`, `@Autowired(required=false) S3Client`
- `upload(String key, byte[], String contentType)`: String — S3 or local filesystem
- `download(String key)`: byte[]
- `delete(String key)`: void
- `generatePresignedUrl(String, Duration)`: String

#### TeamService
**Injects:** TeamRepository, TeamMemberRepository, UserRepository, InvitationRepository
- `createTeam(CreateTeamRequest)`: TeamResponse — adds creator as OWNER
- `getTeam(UUID)`: TeamResponse — readOnly
- `getTeamsForUser()`: List — readOnly
- `updateTeam(UUID, UpdateTeamRequest)`: TeamResponse
- `deleteTeam(UUID)`: void — OWNER only
- `getTeamMembers(UUID)`: List — readOnly
- `updateMemberRole(UUID, UUID, UpdateMemberRoleRequest)`: TeamMemberResponse — ownership transfer
- `removeMember(UUID, UUID)`: void — self-removal allowed
- `inviteMember(UUID, InviteMemberRequest)`: InvitationResponse — pessimistic lock
- `acceptInvitation(String token)`: TeamResponse
- `getTeamInvitations(UUID)`: List — readOnly
- `cancelInvitation(UUID)`: void

#### TechDebtService
**Injects:** TechDebtItemRepository, ProjectRepository, TeamMemberRepository, QaJobRepository
- `createTechDebtItem(CreateTechDebtItemRequest)`: TechDebtItemResponse
- `createTechDebtItems(List)`: List — batch
- `getTechDebtItem(UUID)`: TechDebtItemResponse — readOnly
- `getTechDebtForProject(UUID, Pageable)`: PageResponse — readOnly
- `getTechDebtByStatus(UUID, DebtStatus, Pageable)`: PageResponse — readOnly
- `getTechDebtByCategory(UUID, DebtCategory, Pageable)`: PageResponse — readOnly
- `updateTechDebtStatus(UUID, UpdateTechDebtStatusRequest)`: TechDebtItemResponse
- `deleteTechDebtItem(UUID)`: void
- `getDebtSummary(UUID)`: Map — readOnly

#### TokenBlacklistService
In-memory `ConcurrentHashMap<String, Instant>`. No persistence.
- `blacklist(String jti, Instant expiry)`: void
- `isBlacklisted(String jti)`: boolean

#### UserService
**Injects:** UserRepository
Class-level `@Transactional(readOnly = true)`
- `getUserById(UUID)`: UserResponse
- `getUserByEmail(String)`: UserResponse
- `getCurrentUser()`: UserResponse
- `updateUser(UUID, UpdateUserRequest)`: UserResponse — @Transactional
- `searchUsers(String query)`: List — max 20 results
- `deactivateUser(UUID)`: void — @Transactional
- `activateUser(UUID)`: void — @Transactional

### Module Services Summary

**Courier (22 services):** CollectionService, FolderService, RequestService, RequestProxyService (HTTP proxy with redirect following via java.net.http.HttpClient, 30s timeout), CollectionRunnerService, CodeGenerationService (GraalVM polyglot), ImportService (Postman/OpenAPI/cURL), GraphQLService, EnvironmentService, EnvironmentVariableService, GlobalVariableService, HistoryService, ShareService, ForkService, MergeService, VariableResolverService, ScriptExecutionService, RequestAuthService, RequestBodyService, RequestHeaderService, RequestParamService, RequestScriptService.

**Fleet (6 services):** ContainerService, ServiceProfileService, SolutionProfileService, WorkstationService, DeploymentService, FleetHealthService.

**Logger (20 services):** LogIngestionService, LogQueryService, AlertService, AlertRuleService, AlertChannelService, MetricsService (bean name: `loggerMetricsService`), MetricAggregationService, RetentionService, DashboardService, DashboardWidgetService, LogSourceService, LogTrapService, SavedQueryService, QueryHistoryService, AnomalyBaselineService, TraceSpanService, RetentionExecutor, AlertHistoryService, LogExportService, TrapConditionService.

**MCP (8 services):** McpService, McpSessionService, DeveloperProfileService, McpApiTokenService, ProjectDocumentService, SessionResultService, SessionToolCallService, ActivityFeedService.

**Registry (10 services):** ServiceRegistrationService, DependencyService (registry), PortService, ApiRouteService, SolutionService, SolutionMemberService, InfraResourceService, EnvironmentConfigService, ConfigTemplateService, RegistryHealthService.

**Relay (8 services):** ChannelService, MessageService, DirectMessageService, FileAttachmentService, ReactionService, UserPresenceService, PlatformEventService, RelayHealthService.

---

## 10. Controller / API Layer — Method Signatures Only

### Core Controllers (com.codeops.controller) — 18 controllers

All require `isAuthenticated()` unless noted. All inject `AuditLogService` for mutation logging.

#### AdminController — `/api/v1/admin` — `hasRole('ADMIN') or hasRole('OWNER')`
- `getSystemStats()` → `adminService.getSystemStats()`
- `getSystemSettings()` → `adminService.getSystemSettings()`
- `updateSystemSetting()` → `adminService.updateSystemSetting()`
- `getAllUsers()` → `adminService.getAllUsers()`
- `adminResetMfa()` → `mfaService.adminResetMfa()`

#### AuthController — `/api/v1/auth` — PUBLIC (no auth)
- `register()` → `authService.register()`
- `login()` → `authService.login()`
- `refreshToken()` → `authService.refreshToken()`
- `logout()` → `authService.logout()`
- `changePassword()` → `authService.changePassword()`
- MFA endpoints: `setupMfa()`, `verifyMfa()`, `setupEmailMfa()`, `verifyEmailMfa()`, `verifyMfaLogin()`, `resendMfaCode()`, `disableMfa()`, `regenerateRecoveryCodes()`, `getMfaStatus()`

#### ComplianceController — `/api/v1/compliance`
- CRUD for compliance items + batch create + status filter

#### DependencyController — `/api/v1/dependencies`
- Scan CRUD + vulnerability CRUD + batch add + status update

#### DirectiveController — `/api/v1/directives`
- Directive CRUD + project assignment/toggle

#### FindingController — `/api/v1/findings`
- Finding CRUD + batch create + severity/status filters

#### HealthMonitorController — `/api/v1/health-monitor`
- Schedule CRUD + snapshot CRUD

#### IntegrationController — `/api/v1/integrations`
- GitHub connection CRUD + validation
- Jira connection CRUD

#### JobController — `/api/v1/jobs`
- Job CRUD + agent run CRUD (single + batch) + bug investigation CRUD

#### MetricsController — `/api/v1/metrics`
- `getProjectMetrics()`, `getTeamMetrics()`, `getHealthTrend()`

#### PersonaController — `/api/v1/personas`
- Persona CRUD + team list + agent type filter + default management + system personas

#### ProjectController — `/api/v1/projects`
- Project CRUD + archive/unarchive + delete (cascade)

#### ReportController — `/api/v1/reports`
- Upload/download agent reports, summary reports, specifications (50MB max, content type allow-list, S3 key validation)

#### TaskController — `/api/v1/tasks`
- Task CRUD + batch create + assigned-to-me

#### TeamController — `/api/v1/teams`
- Team CRUD + member management + invitation flow (create, accept, cancel, list)

#### TechDebtController — `/api/v1/tech-debt`
- Tech debt CRUD + batch create + status/category filters + debt summary

#### UserController — `/api/v1/users`
- `getCurrentUser()`, `getUserById()`, `updateUser()`, `searchUsers()`
- `deactivateUser()`, `activateUser()` — `hasRole('ADMIN') or hasRole('OWNER')`

### Module Controllers

**Courier (13 controllers):** CollectionController, FolderController, RequestController, HistoryController, EnvironmentController, VariableController, ShareController, RunnerController, ImportController, ProxyController, GraphQLController, CodeGenerationController, CourierHealthController. Base path: `/api/v1/courier/`. All `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")` with `@RequestHeader("X-Team-ID")`.

**Fleet (8 controllers):** ServiceProfileController, ContainerController, SolutionProfileController, WorkstationController, DeploymentController, FleetController, FleetHealthController. Base path: `/api/v1/fleet/`.

**Logger (10 controllers):** LogController, LogSourceController, MetricController, AlertController, AlertRuleController, AlertChannelController, DashboardController, QueryController, RetentionController, TraceController. Base path: `/api/v1/logger/`.

**MCP (5 controllers):** McpController, McpSessionController, DeveloperProfileController, ProjectDocumentController, ActivityFeedController. Base path: `/api/v1/mcp/`.

**Registry (10 controllers):** ServiceController, DependencyController, PortController, RouteController, SolutionController, SolutionMemberController, InfraResourceController, EnvironmentConfigController, ConfigTemplateController, RegistryHealthController. Base path: `/api/v1/registry/`.

**Relay (8 controllers):** ChannelController, MessageController, DirectMessageController, FileAttachmentController, ReactionController, UserPresenceController, PlatformEventController, RelayHealthController. Base path: `/api/v1/relay/`.

---

## 11. Security Configuration

```
Authentication: JWT (stateless, HS256)
Token issuer/validator: Internal (JwtTokenProvider)
Password encoder: BCrypt strength 12

Public endpoints (no auth required):
  - /api/v1/auth/**
  - /api/v1/health
  - /api/v1/courier/health
  - /api/v1/fleet/health
  - /swagger-ui/**
  - /v3/api-docs/**
  - /ws/relay/**

Protected endpoints:
  - /api/v1/admin/** → hasRole('ADMIN') or hasRole('OWNER')
  - /api/v1/courier/** → hasRole('ADMIN') or hasRole('OWNER')
  - /api/v1/** → isAuthenticated()

CORS: Origins from codeops.cors.allowed-origins (dev: localhost:3000,5173), methods GET/POST/PUT/DELETE/PATCH/OPTIONS, credentials enabled, max-age 3600s

CSRF: Disabled (stateless JWT API)

Rate limiting: 10 req/60s on /api/v1/auth/** per IP (in-memory ConcurrentHashMap)

Security Headers:
  - CSP: default-src 'self'; frame-ancestors 'none'
  - X-Frame-Options: DENY
  - X-Content-Type-Options: nosniff
  - HSTS: max-age=31536000; includeSubDomains

Filter Chain Order (before UsernamePasswordAuthenticationFilter):
  1. RequestCorrelationFilter (@Order HIGHEST_PRECEDENCE)
  2. RateLimitFilter
  3. McpTokenAuthFilter
  4. JwtAuthFilter
```

---

## 12. Custom Security Components

#### JwtAuthFilter
**Extends:** `OncePerRequestFilter`
Extracts Bearer token from Authorization header. Valid non-MFA tokens → sets `UsernamePasswordAuthenticationToken` with UUID principal, email, roles (with ROLE_ prefix) in SecurityContextHolder. Invalid/missing/MFA challenge tokens → pass through unauthenticated.

#### JwtTokenProvider
**Algorithm:** HS256. **Secret:** from `JwtProperties.secret` (min 32 chars, validated @PostConstruct).
Access token: 24h, claims: sub (UUID), email, roles, jti. Refresh token: 30d, type: "refresh". MFA challenge: 5 min, type: "mfa_challenge". Validates signature, expiration, blacklist (TokenBlacklistService).

#### RateLimitFilter
**Extends:** `OncePerRequestFilter`. Only `/api/v1/auth/**`. 10 req/60s per client IP (X-Forwarded-For or remoteAddr). In-memory `ConcurrentHashMap<String, RateWindow>`. Exceeds → 429 JSON response.

#### McpTokenAuthFilter
**Extends:** `OncePerRequestFilter`. Authenticates MCP API tokens (X-MCP-Token header) for MCP module endpoints. Sets authentication with token-derived principal.

#### SecurityUtils
Static utility: `getCurrentUserId()` → UUID, `hasRole(String)` → boolean, `isAdmin()` → boolean (ADMIN or OWNER).

---

## 13. Exception Handling & Error Responses

**File:** `src/main/java/com/codeops/config/GlobalExceptionHandler.java`
`@RestControllerAdvice`. Returns `ErrorResponse(int status, String message)` record.

| Exception | HTTP | Message |
|-----------|------|---------|
| EntityNotFoundException | 404 | "Resource not found" |
| IllegalArgumentException | 400 | "Invalid request" |
| AccessDeniedException | 403 | "Access denied" |
| MethodArgumentNotValidException | 400 | Comma-separated field errors |
| NotFoundException | 404 | Exception message |
| ValidationException | 400 | Exception message |
| AuthorizationException | 403 | Exception message |
| MissingServletRequestParameterException | 400 | "Missing required parameter: {name}" |
| MissingRequestHeaderException | 400 | "Missing required header: {name}" |
| MethodArgumentTypeMismatchException | 400 | "Invalid value for parameter '{name}': {value}" |
| HttpRequestMethodNotSupportedException | 405 | "HTTP method '{method}' is not supported..." |
| HttpMessageNotReadableException | 400 | "Malformed request body" |
| NoResourceFoundException | 404 | "Resource not found" |
| CodeOpsException | 500 | "An internal error occurred" |
| Exception (catch-all) | 500 | "An internal error occurred" |

Custom exceptions: `CodeOpsException` (500 base), `NotFoundException` (404), `ValidationException` (400), `AuthorizationException` (403).

---

## 14. Mappers / DTOs

**Framework:** MapStruct 1.5.5.Final. All mappers: `@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))`.

**55 MapStruct mapper interfaces total:**
- Core: No mappers (manual mapping in services)
- Courier: 13 (CollectionMapper, FolderMapper, RequestMapper, RequestAuthMapper, RequestBodyMapper, RequestHeaderMapper, RequestParamMapper, RequestScriptMapper, RequestHistoryMapper, EnvironmentMapper, EnvironmentVariableMapper, GlobalVariableMapper, RunResultMapper)
- Fleet: 10 (ServiceProfileMapper, ContainerInstanceMapper, ContainerLogMapper, ContainerHealthCheckMapper, NetworkConfigMapper, VolumeMountMapper, SolutionProfileMapper, SolutionServiceMapper, WorkstationProfileMapper, WorkstationSolutionMapper)
- Logger: 15 (LogEntryMapper, LogSourceMapper, AlertRuleMapper, AlertChannelMapper, AlertHistoryMapper, MetricMapper, RetentionPolicyMapper, DashboardMapper, LogTrapMapper, SavedQueryMapper, QueryHistoryMapper, AnomalyBaselineMapper, TraceSpanMapper, DashboardMapper — 2 separate, plus others)
- MCP: 8 (McpSessionMapper, ActivityFeedEntryMapper, DeveloperProfileMapper, McpApiTokenMapper, ProjectDocumentMapper, ProjectDocumentVersionMapper, SessionResultMapper, SessionToolCallMapper)
- Relay: 11 (ChannelMapper, ChannelMemberMapper, MessageMapper, MessageThreadMapper, DirectConversationMapper, DirectMessageMapper, FileAttachmentMapper, ReactionMapper, PinnedMessageMapper, PlatformEventMapper, UserPresenceMapper)

**Notable @Mapping annotations:**
- `CollectionMapper`: `@Mapping(target = "isShared", source = "shared")` — Lombok boolean field naming
- `ServiceProfileMapper`: `@Mapping(target = "isAutoGenerated", source = "autoGenerated")`, `@Mapping(target = "isEnabled", source = "enabled")`
- `McpSessionMapper`: `@Mapping(source = "project.name", target = "projectName")`, `@Mapping(source = "developerProfile.user.displayName", target = "developerName")`
- `ChannelMapper`: `@Mapping(target = "isArchived", source = "archived")`
- `LogEntryMapper`: `@Mapping(source = "source.id", target = "sourceId")`, `@Mapping(source = "source.name", target = "sourceName")`

---

## 15. Utility Classes & Shared Components

#### AppConstants
**File:** `src/main/java/com/codeops/config/AppConstants.java`
Final class with 300+ `public static final` constants. Key values: `MAX_TEAM_MEMBERS=50`, `MAX_PROJECTS_PER_TEAM=100`, `JWT_EXPIRY_HOURS=24`, `REFRESH_TOKEN_EXPIRY_DAYS=30`, `INVITATION_EXPIRY_DAYS=7`, `MIN_PASSWORD_LENGTH=1`, `DEFAULT_PAGE_SIZE=20`, `MAX_PAGE_SIZE=100`, `MAX_CONCURRENT_AGENTS=5`, `AGENT_TIMEOUT_MINUTES=15`, `DEFAULT_HEALTH_SCORE=100`, `MAX_REPORT_SIZE_MB=25`, `MAX_SPEC_FILE_SIZE_MB=50`. Also contains module-specific constants (Courier, Fleet, Logger, Registry, Relay, MCP prefixes and limits).

#### SecurityUtils
**File:** `src/main/java/com/codeops/security/SecurityUtils.java`
Static methods: `getCurrentUserId()`: UUID, `hasRole(String)`: boolean, `isAdmin()`: boolean.

#### SlugUtils
**File:** `src/main/java/com/codeops/registry/util/SlugUtils.java`
Static methods: `generateSlug(String name)`: String (lowercase, hyphenated, validated), `makeUnique(String, Predicate)`: String (appends -2, -3...), `validateSlug(String)`: void (throws ValidationException).

---

## 16. Database Schema (Live)

84 tables in `public` schema. PostgreSQL 16. Managed by Hibernate `ddl-auto: update`.

**Core (27 tables):** `users`, `teams`, `team_members`, `invitations`, `projects`, `project_directives`, `directives`, `personas`, `specifications`, `findings`, `compliance_items`, `remediation_tasks`, `remediation_task_findings` (join table), `bug_investigations`, `dependency_scans`, `dependency_vulnerabilities`, `tech_debt_items`, `qa_jobs`, `agent_runs`, `health_schedules`, `health_snapshots`, `github_connections`, `jira_connections`, `notification_preferences`, `system_settings`, `audit_log`, `mfa_email_codes`

**Courier (18 tables):** `collections`, `collection_shares`, `folders`, `requests`, `request_auths`, `request_bodies`, `request_headers`, `request_params`, `request_scripts`, `request_history`, `environments`, `environment_variables`, `global_variables`, `run_results`, `run_iterations`, `forks`, `merge_requests`, `code_snippet_templates`

**Fleet (14 tables):** `fleet_service_profiles`, `container_instances`, `container_logs`, `container_health_check`, `network_configs`, `volume_mounts`, `port_mappings`, `fleet_environment_variables`, `solution_profiles`, `solution_services`, `workstation_profiles`, `workstation_solutions`, `deployment_records`, `deployment_containers`

**Logger (16 tables):** `log_sources`, `log_entries`, `log_traps`, `trap_conditions`, `metrics`, `metric_series`, `dashboards`, `dashboard_widgets`, `alert_rules`, `alert_channels`, `alert_history`, `anomaly_baselines`, `saved_queries`, `query_history`, `retention_policies`, `trace_spans`

**MCP (8 tables):** `mcp_sessions`, `session_results`, `session_tool_calls`, `developer_profiles`, `project_documents`, `project_document_versions`, `mcp_api_tokens`, `activity_feed_entries`

**Registry (8 tables):** `service_registrations`, `service_dependencies`, `api_route_registrations`, `solutions`, `solution_members`, `infra_resources`, `environment_configs`, `config_templates`

**Relay (12 tables):** `channels`, `channel_members`, `messages`, `message_threads`, `direct_conversations`, `direct_messages`, `file_attachments`, `reactions`, `pinned_messages`, `read_receipts`, `user_presences`, `platform_events`

---

## 17. Message Broker Configuration

```
Broker: Kafka (Confluent 7.5.0)
Connection: localhost:9094 (internal: 29092)
Group ID: codeops-server
Auto Offset Reset: earliest
Deserialization: String/String

Topics (created by kafka-init):
  - codeops.core.decision.created
  - codeops.core.decision.resolved
  - codeops.core.decision.escalated
  - codeops.core.outcome.created
  - codeops.core.outcome.validated
  - codeops.core.outcome.invalidated
  - codeops.core.hypothesis.created
  - codeops.core.hypothesis.concluded
  - codeops.integrations.sync
  - codeops.notifications

Consumer Config (KafkaConsumerConfig):
  - @EnableKafka
  - DefaultErrorHandler with 3 retries, 1s backoff
  - StringDeserializer for keys and values
```

No `@KafkaListener` methods detected in source code. Topics are declared and infrastructure is provisioned but consumers are not yet implemented.

---

## 18. Cache Layer

Redis 7 Alpine is declared in `docker-compose.yml` (port 6379, AOF persistence) but **no Redis client dependency** exists in `pom.xml` and **no `@Cacheable`, `@CacheEvict`, or `CacheManager`** usage found in source code.

In-memory caching:
- `TokenBlacklistService`: `ConcurrentHashMap<String, Instant>` for JWT blacklist
- `RateLimitFilter`: `ConcurrentHashMap<String, RateWindow>` for rate limiting

No Redis or distributed caching layer detected in application code.

---

## 19. Environment Variable Inventory

| Variable | Used In | Default | Required in Prod |
|----------|---------|---------|------------------|
| `DATABASE_URL` | application-prod.yml | localhost:5432/codeops | YES |
| `DATABASE_USERNAME` | application-prod.yml | codeops | YES |
| `DATABASE_PASSWORD` | application-prod.yml | codeops | YES |
| `JWT_SECRET` | application-dev/prod.yml | dev fallback (32+ chars) | YES |
| `ENCRYPTION_KEY` | application-dev/prod.yml | dev fallback (32+ chars) | YES |
| `CORS_ALLOWED_ORIGINS` | application-prod.yml | localhost:3000 | YES |
| `S3_BUCKET` | application-prod.yml | codeops-dev | YES |
| `AWS_REGION` | application-prod.yml | (none) | YES |
| `MAIL_FROM_EMAIL` | application-prod.yml | noreply@codeops.dev | YES |
| `codeops.aws.s3.enabled` | S3Config.java, S3StorageService | false | NO (optional) |
| `codeops.fleet.docker.host` | DockerConfig.java | unix:///var/run/docker.sock | NO |
| `codeops.fleet.docker.api-version` | DockerConfig.java | v1.43 | NO |

---

## 20. Service Dependency Map

```
This Service → Depends On
--------------------------
PostgreSQL 16: localhost:5432 (required)
Kafka: localhost:9094 (required for startup, consumers not implemented)
Redis: localhost:6379 (declared but unused)
Docker Engine: tcp://localhost:2375 (Fleet module, optional)
AWS S3: Optional (disabled in dev, fallback to local filesystem)
SMTP Server: Optional (disabled in dev, logged to console)
Microsoft Teams: Webhook URLs (outbound, SSRF-protected)
```

Standalone service — no inter-service HTTP dependencies. CodeOps-Client (Flutter desktop app) consumes this API. CodeOps-Analytics (port 8081) is a separate service.

---

## 21. Known Technical Debt & Issues

| Issue | Location | Severity | Notes |
|-------|----------|----------|-------|
| TODO: Key rotation | EncryptionService.java:56 | CRITICAL | Comment indicates key rotation not implemented |
| TODO: Docker config | DockerConfig.java:17 | CRITICAL | Configuration placeholder |
| "not yet implemented" | RetentionExecutor.java:82 | CRITICAL | Incomplete implementation |
| "not yet implemented" | S3StorageService.java:157 | CRITICAL | Incomplete implementation |
| Redis declared but unused | docker-compose.yml | Medium | Infrastructure cost with no app benefit |
| Kafka consumers not implemented | KafkaConsumerConfig.java | Medium | 10 topics provisioned, no @KafkaListener |
| TokenBlacklistService in-memory | TokenBlacklistService.java | Medium | Lost on restart, not cluster-safe |
| RateLimitFilter in-memory | RateLimitFilter.java | Medium | Not cluster-safe |
| MIN_PASSWORD_LENGTH = 1 | AppConstants.java | Low | Dev convenience, must increase for prod |
| System.out/printStackTrace usage | 4 occurrences | Low | Should use SLF4J |
| Field injection (2 instances) | S3StorageService, 1 other | Low | Should use constructor injection |
| 63 Snyk dependency vulnerabilities | pom.xml dependencies | CRITICAL | 5 critical, 28 high — see Section 22 |
| Doc coverage gaps (classes) | 48 undocumented classes | CRITICAL | 269/317 = 84.9% (BLOCKING) |
| Doc coverage gaps (methods) | 612 undocumented methods | CRITICAL | 373/985 = 37.9% (BLOCKING) |

---

## 22. Security Vulnerability Scan (Snyk)

Scan Date: 2026-02-28T21:02:36Z
Snyk CLI Version: Latest

### Dependency Vulnerabilities (Open Source)
Critical: 5
High: 28
Medium: 21
Low: 9
Total unique: 63

| Severity | Package | Version | Vulnerability | Fix Available |
|----------|---------|---------|---------------|---------------|
| CRITICAL | tomcat-embed-core | 10.1.24 | Uncaught Exception | 9.0.96 |
| CRITICAL | tomcat-embed-core | 10.1.24 | TOCTOU Race Condition (x2) | 9.0.98 |
| CRITICAL | spring-security-web | 6.3.0 | Missing Authorization | 5.7.13 |
| CRITICAL | spring-security-crypto | 6.3.0 | Authentication Bypass | 6.3.8 |
| HIGH | netty-codec-http2 | 4.1.110 | Resource Allocation / Data Amplification | 4.1.125 |
| HIGH | netty-codec-http | 4.1.110 | HTTP Request Smuggling / Data Amplification | 4.1.125 |
| HIGH | netty-handler | 4.1.110 | Input Validation | 4.1.118 |
| HIGH | commons-lang3 | 3.14.0 | Uncontrolled Recursion | 3.18.0 |
| HIGH | kafka-clients | 3.7.0 | SSRF / Deserialization (x3) / Auth Algorithm | 3.9.1 |
| HIGH | tomcat-embed-core | 10.1.24 | Multiple (10 HIGH) | Various |
| HIGH | lz4-java | 1.8.0 | Out-of-bounds Read / Info Leak | 1.8.1 |
| HIGH | spring-beans | 6.1.8 | Path Traversal | 6.2.10 |
| HIGH | spring-core | 6.1.8 | Incorrect Authorization | 6.2.11 |
| HIGH | spring-webmvc | 6.1.8 | Path Traversal (x2) | 6.1.13/14 |
| MEDIUM | logback-core | 1.5.6 | Multiple (2) | 1.3.15/16 |
| MEDIUM | jcommander | 1.72 | Unsafe Dependency Resolution | 1.75 |
| MEDIUM | Various netty/tomcat/spring | Various | Multiple (21 total) | Various |
| LOW | Various | Various | 9 low severity | Various |

### Code Vulnerabilities (SAST)
Errors: 0
Warnings: 0
Notes: 0
**PASS** — No code vulnerabilities detected.

### IaC Findings
Snyk IaC scan not available (exit code 3).

**Root cause:** Spring Boot 3.3.0 ships with older transitive dependencies. Fix by upgrading Spring Boot to 3.4.x+ or overriding individual dependency versions in pom.xml.
