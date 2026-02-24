# CodeOps Server — Codebase Audit

## 1. Project Identity

| Field | Value |
|---|---|
| Project Name | CodeOps Server |
| Repository URL | https://github.com/AI-CodeOps/CodeOps-Server |
| Primary Language / Framework | Java 21 / Spring Boot 3.3.0 |
| Build Tool | Maven 3.x (wrapper) |
| Current Branch | main |
| Latest Commit Hash | 792ab8eb11e1da0bfb39d87107bdd4717d74c6a8 |
| Latest Commit Message | CON-003: Merge CodeOps-Courier into CodeOps-Server |
| Audit Timestamp | 2026-02-24T19:10:00Z |
| Architecture | Consolidated monolith (4 modules: core, registry, logger, courier) |

---

## 2. Directory Structure

Single-module Maven project. Four domain modules under `src/main/java/com/codeops/`.

```
CodeOps-Server/
├── pom.xml
├── docker-compose.yml
├── Dockerfile
├── CLAUDE.md
├── CONVENTIONS.md
├── CodeOps-Server-Audit.md
├── CodeOps-Server-Architecture.md
├── openapi.yaml
├── src/
│   ├── main/
│   │   ├── java/com/codeops/
│   │   │   ├── CodeOpsApplication.java
│   │   │   ├── config/                         ← 16 shared config classes
│   │   │   ├── security/                       ← 5 security classes
│   │   │   ├── exception/                      ← 4 custom exceptions + GlobalExceptionHandler
│   │   │   ├── entity/                         ← 28 core entities + 25 enums
│   │   │   ├── repository/                     ← 26 core repositories
│   │   │   ├── dto/request/ + dto/response/    ← Core DTOs (Java records)
│   │   │   ├── service/                        ← 26 core services
│   │   │   ├── controller/                     ← 17 core REST controllers
│   │   │   ├── notification/                   ← 3 classes (Email, Teams, Dispatcher)
│   │   │   │
│   │   │   ├── registry/                       ← Registry module
│   │   │   │   ├── entity/ + entity/enums/     ← 11 entities + 11 enums
│   │   │   │   ├── repository/                 ← 11 repositories
│   │   │   │   ├── dto/request/ + dto/response/
│   │   │   │   ├── service/                    ← 10 services
│   │   │   │   ├── controller/                 ← 10 controllers
│   │   │   │   └── util/                       ← SlugUtils
│   │   │   │
│   │   │   ├── logger/                         ← Logger module
│   │   │   │   ├── entity/ + entity/enums/     ← 16 entities + 10 enums
│   │   │   │   ├── repository/                 ← 16 repositories
│   │   │   │   ├── dto/request/ + dto/response/ + dto/mapper/
│   │   │   │   ├── service/                    ← 19 services
│   │   │   │   ├── controller/                 ← 11 controllers
│   │   │   │   └── event/                      ← LogEntryEventListener
│   │   │   │
│   │   │   └── courier/                        ← Courier module
│   │   │       ├── entity/ + entity/enums/     ← 18 entities + 7 enums
│   │   │       ├── repository/                 ← 18 repositories
│   │   │       ├── dto/request/ + dto/response/ + dto/mapper/
│   │   │       ├── service/                    ← 22 services
│   │   │       ├── controller/                 ← 13 controllers
│   │   │       └── config/                     ← HttpClientConfig
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/codeops/
│           ├── config/                          ← 11 config test files
│           ├── security/                        ← 4 security test files
│           ├── controller/                      ← 18 core controller tests
│           ├── service/                         ← 27 core service tests
│           ├── notification/                    ← 3 notification tests
│           ├── integration/                     ← 17 integration test files
│           ├── registry/controller/ + service/  ← 21 registry tests
│           ├── logger/controller/ + service/ + event/ ← 27 logger tests
│           └── courier/controller/ + service/   ← 34 courier tests
└── (infrastructure files)
```

**Totals:** 642 source files, 163 test files, 2540 test methods.

---

## 3. Build & Dependency Manifest

**Parent:** `spring-boot-starter-parent:3.3.0`, Java 21.

**Properties:**

| Property | Value |
|---|---|
| jjwt.version | 0.12.6 |
| mapstruct.version | 1.5.5.Final |
| lombok.version | 1.18.42 |
| mockito.version | 5.21.0 |
| byte-buddy.version | 1.18.4 |

**Dependencies:**

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.3.0 (parent) | REST API framework |
| spring-boot-starter-data-jpa | 3.3.0 (parent) | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.3.0 (parent) | JWT authentication |
| spring-boot-starter-validation | 3.3.0 (parent) | Jakarta Validation |
| spring-boot-starter-mail | 3.3.0 (parent) | SMTP email |
| spring-kafka | 3.2.x (parent) | Kafka producer/consumer |
| postgresql | runtime (parent) | PostgreSQL driver |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT token creation/validation |
| aws-sdk-s3 | 2.25.0 | S3 file storage |
| totp | 1.7.1 | MFA TOTP support |
| lombok | 1.18.42 (provided) | Boilerplate reduction |
| mapstruct | 1.5.5.Final | DTO mapping (Logger, Courier) |
| jackson-datatype-jsr310 | parent-managed | Java 8 date/time serialization |
| jackson-dataformat-yaml | parent-managed | YAML parsing (Courier OpenAPI import) |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI / OpenAPI generation |
| logstash-logback-encoder | 7.4 | Structured JSON logging |
| graalvm-polyglot | 24.1.1 | Courier JavaScript sandboxing |
| graalvm-js | 24.1.1 (pom) | GraalVM JS engine |
| h2 | test (parent) | In-memory test database |
| testcontainers (postgresql, junit-jupiter, kafka) | 1.19.8 | Integration test containers |
| spring-kafka-test | test (parent) | Kafka test utilities |
| spring-security-test | test (parent) | Security test utilities |

**Build Plugins:** spring-boot-maven-plugin, maven-compiler-plugin (Lombok + MapStruct annotation processors), maven-surefire-plugin (--add-opens for Java 25), jacoco-maven-plugin (0.8.14).

---

## 4. Consolidated Statistics

| Category | Core | Registry | Logger | Courier | Total |
|----------|------|----------|--------|---------|-------|
| Entities | 28 | 11 | 16 | 18 | **73** |
| Enums | 25 | 11 | 10 | 7 | **53** |
| Controllers | 17 | 10 | 11 | 13 | **51** |
| Services | 26 | 10 | 19 | 22 | **77** |
| Repositories | 26 | 11 | 16 | 18 | **71** |
| Mappers | 0 | 0 | 13 | 13 | **26** |
| Source Files | — | — | — | — | **642** |
| Test Files | — | — | — | — | **163** |
| Test Methods | ~900 | ~548 | ~450 | ~617 | **2540** |
| Database Tables | 28 | 10 | 16 | 18 | **72** |
| API Paths | 127 | 65 | 75 | 62 | **329** |
| API Operations | 151 | 77 | 104 | 79 | **411** |

---

## 5. Entity Inventory

### 5.1 Core Entities (28)

| Entity | Table | Key Fields |
|--------|-------|------------|
| User | users | email, passwordHash, displayName, isActive, mfaEnabled, mfaMethod |
| Team | teams | name, owner_id |
| TeamMember | team_members | team_id, user_id, role (TeamRole), joinedAt |
| Project | projects | team_id, name, healthScore, isArchived, repoUrl, techStack |
| Invitation | invitations | team_id, email, invitedBy, role (TeamRole), token, status |
| Persona | personas | name, agentType, scope, team_id, isDefault |
| Directive | directives | name, contentMd, category, scope, team_id |
| ProjectDirective | project_directives | project_id, directive_id (composite PK), enabled |
| QaJob | qa_jobs | project_id, mode (JobMode), status (JobStatus), branch, healthScore |
| AgentRun | agent_runs | job_id, agentType, status (AgentStatus), result (AgentResult), score |
| Finding | findings | job_id, agentType, severity, title, status (FindingStatus), version |
| RemediationTask | remediation_tasks | job_id, taskNumber, title, priority, status (TaskStatus) |
| BugInvestigation | bug_investigations | job_id, jiraKey, jiraSummary, rcaMd, rcaPostedToJira |
| Specification | specifications | job_id, name, specType, s3Key |
| ComplianceItem | compliance_items | job_id, requirement, spec_id, status (ComplianceStatus) |
| TechDebtItem | tech_debt_items | project_id, category (DebtCategory), title, status (DebtStatus) |
| DependencyScan | dependency_scans | project_id, job_id, manifestFile, vulnerableCount |
| DependencyVulnerability | dependency_vulnerabilities | scan_id, dependencyName, severity, status |
| HealthSnapshot | health_snapshots | project_id, job_id, healthScore, techDebtScore, capturedAt |
| HealthSchedule | health_schedules | project_id, scheduleType, cronExpression, isActive |
| AuditLog | audit_log | user_id, team_id, action, entityType, entityId (Long PK) |
| SystemSetting | system_settings | settingKey (String PK), value, updatedBy |
| GitHubConnection | github_connections | team_id, name, authType, encryptedCredentials, isActive |
| JiraConnection | jira_connections | team_id, name, instanceUrl, email, encryptedApiToken, isActive |
| NotificationPreference | notification_preferences | user_id, eventType, inApp, email |
| MfaEmailCode | mfa_email_codes | userId, codeHash, expiresAt, used |

### 5.2 Registry Entities (11)

| Entity | Table | Key Fields |
|--------|-------|------------|
| ServiceRegistration | service_registrations | teamId, name, slug, serviceType, status, healthCheckUrl |
| ServiceDependency | service_dependencies | sourceService_id, targetService_id, dependencyType, isRequired |
| Solution | solutions | teamId, name, slug, category, status, ownerUserId |
| SolutionMember | solution_members | solution_id, service_id, role, displayOrder |
| PortAllocation | port_allocations | service_id, environment, portType, portNumber, protocol |
| PortRange | port_ranges | teamId, portType, rangeStart, rangeEnd, environment |
| ApiRouteRegistration | api_route_registrations | service_id, gatewayService_id, routePrefix, environment |
| EnvironmentConfig | environment_configs | service_id, environment, configKey, configValue, configSource |
| ConfigTemplate | config_templates | service_id, templateType, environment, contentText, version |
| InfraResource | infra_resources | teamId, service_id, resourceType, resourceName, environment |
| WorkstationProfile | workstation_profiles | teamId, name, solutionId, servicesJson, isDefault |

### 5.3 Logger Entities (16)

| Entity | Table | Key Fields |
|--------|-------|------------|
| LogEntry | log_entries | source_id, level, message, timestamp, serviceName, correlationId, teamId |
| LogSource | log_sources | name, environment, isActive, teamId, logCount |
| LogTrap | log_traps | name, trapType, isActive, teamId, createdBy, triggerCount |
| TrapCondition | trap_conditions | trap_id, conditionType, field, pattern, threshold, windowSeconds |
| AlertRule | alert_rules | name, trap_id, channel_id, severity, isActive, teamId |
| AlertChannel | alert_channels | name, channelType, configuration, isActive, teamId |
| AlertHistory | alert_history | rule_id, trap_id, channel_id, severity, status, teamId |
| Dashboard | dashboards | name, teamId, createdBy, isShared, isTemplate, refreshIntervalSeconds |
| DashboardWidget | dashboard_widgets | dashboard_id, title, widgetType, queryJson, gridX/Y/W/H |
| SavedQuery | saved_queries | name, queryJson, teamId, createdBy, isShared, executionCount |
| QueryHistory | query_history | queryJson, resultCount, executionTimeMs, teamId |
| TraceSpan | trace_spans | correlationId, traceId, spanId, parentSpanId, serviceName, durationMs, status |
| Metric | metrics | name, metricType, unit, serviceName, tags, teamId |
| MetricSeries | metric_series | metric_id, timestamp, value, tags, resolution |
| RetentionPolicy | retention_policies | name, sourceName, logLevel, retentionDays, action, isActive, teamId |
| AnomalyBaseline | anomaly_baselines | serviceName, metricName, baselineValue, standardDeviation, deviationThreshold, teamId |

### 5.4 Courier Entities (18)

| Entity | Table | Key Fields |
|--------|-------|------------|
| Collection | collections | teamId, name, authType, isShared, createdBy |
| Folder | folders | name, sortOrder, collection_id, parentFolder_id, authType |
| Request | requests | name, method (HttpMethod), url, sortOrder, folder_id |
| RequestHeader | request_headers | headerKey, headerValue, isEnabled, request_id |
| RequestParam | request_params | paramKey, paramValue, isEnabled, request_id |
| RequestBody | request_bodies | bodyType, rawContent, formData, graphqlQuery, request_id |
| RequestAuth | request_auths | authType, apiKeyHeader, bearerToken, basicUsername, request_id |
| RequestScript | request_scripts | scriptType, content, request_id |
| Environment | environments | teamId, name, isActive, createdBy |
| EnvironmentVariable | environment_variables | variableKey, variableValue, isSecret, isEnabled, scope, environment_id |
| GlobalVariable | global_variables | teamId, variableKey, variableValue, isSecret, isEnabled |
| RequestHistory | request_history | teamId, userId, requestMethod, requestUrl, responseStatus, responseTimeMs |
| RunResult | run_results | teamId, collectionId, environmentId, status (RunStatus), totalRequests, passedRequests |
| RunIteration | run_iterations | iterationNumber, requestName, requestMethod, responseStatus, passed |
| CollectionShare | collection_shares | permission (SharePermission), sharedWithUserId, sharedByUserId, collection_id |
| Fork | forks | forkedByUserId, forkedAt, label, sourceCollection_id, forkedCollection_id |
| MergeRequest | merge_requests | title, status, requestedByUserId, sourceFork_id, targetCollection_id |
| CodeSnippetTemplate | code_snippet_templates | language (CodeLanguage), displayName, templateContent, fileExtension |

---

## 6. Enum Inventory

### Core (25)
AgentType, AgentStatus, AgentResult, JobStatus, JobMode, JobResult, TeamRole, TaskStatus, Priority, Severity, FindingStatus, Effort, DebtStatus, DebtCategory, BusinessImpact, VulnerabilityStatus, ComplianceStatus, SpecType, DirectiveCategory, DirectiveScope, ScheduleType, Scope, GitHubAuthType, InvitationStatus, MfaMethod

### Registry (11)
ServiceType, ServiceStatus, SolutionCategory, SolutionStatus, SolutionMemberRole, DependencyType, HealthStatus, PortType, ConfigTemplateType, ConfigSource, InfraResourceType

### Logger (10)
LogLevel, AlertSeverity, AlertStatus, AlertChannelType, TrapType, ConditionType, WidgetType, MetricType, SpanStatus, RetentionAction

### Courier (7)
HttpMethod, AuthType, BodyType, ScriptType, RunStatus, SharePermission, CodeLanguage

---

## 7. Controller & Endpoint Summary

### 7.1 Core Controllers (17) — `/api/v1/`

| Controller | Base Path | Endpoints |
|---|---|---|
| AuthController | /api/v1/auth | 14 (register, login, refresh, logout, change-password, MFA setup/verify/login/disable/recovery/status/email) |
| TeamController | /api/v1/teams | 12 (CRUD, members, invitations) |
| ProjectController | /api/v1/projects | 7 (CRUD, archive/unarchive) |
| JobController | /api/v1/jobs | 13 (CRUD, agents, investigations) |
| FindingController | /api/v1/findings | 10 (CRUD, filter by severity/agent/status, bulk update) |
| PersonaController | /api/v1/personas | 11 (CRUD, system/team/mine, default management) |
| DirectiveController | /api/v1/directives | 11 (CRUD, assign/remove/toggle) |
| ComplianceController | /api/v1/compliance | 7 (specs, items, summary) |
| TechDebtController | /api/v1/tech-debt | 9 (CRUD, filter, summary) |
| DependencyController | /api/v1/dependencies | 10 (scans, vulnerabilities) |
| HealthMonitorController | /api/v1/health-monitor | 8 (schedules, snapshots, trend) |
| MetricsController | /api/v1/metrics | 3 (project/team metrics, trend) |
| TaskController | /api/v1/tasks | 6 (CRUD, batch, assigned) |
| UserController | /api/v1/users | 6 (me, search, activate/deactivate) |
| AdminController | /api/v1/admin | 10 (users, settings, usage, audit-log, reset-mfa) |
| IntegrationController | /api/v1/integrations | 8 (GitHub/Jira connections) |
| ReportController | /api/v1/reports | 5 (upload/download agent/summary/spec reports) |
| HealthController | /api/v1/health | 1 (public health check) |

### 7.2 Registry Controllers (10) — `/api/v1/registry/`

| Controller | Endpoints |
|---|---|
| RegistryController | 11 (service CRUD, clone, status, health, slug lookup) |
| SolutionController | 11 (CRUD, members, health) |
| PortController | 11 (auto/manual allocate, release, map, conflicts, ranges) |
| DependencyController | 6 (create/delete, graph, impact, startup-order, cycles) |
| ConfigController | 6 (generate, generate-all, docker-compose, get, delete) |
| TopologyController | 4 (team/solution topology, neighborhood, stats) |
| HealthManagementController | 6 (summary, check, unhealthy, never-checked) |
| InfraController | 8 (CRUD, orphans, reassign) |
| RouteController | 5 (CRUD, check availability) |
| WorkstationController | 9 (CRUD, default, from-solution, refresh) |

### 7.3 Logger Controllers (11) — `/api/v1/logger/`

| Controller | Endpoints |
|---|---|
| LogIngestionController | 3 (ingest single/batch, stream) |
| LogQueryController | 6 (search, aggregate, histogram, export, saved queries) |
| LogSourceController | 5 (CRUD, stats) |
| LogTrapController | 6 (CRUD, test, toggle) |
| AlertController | 10 (rules CRUD, channels CRUD, history, test) |
| DashboardController | 9 (CRUD, widgets CRUD, templates, share) |
| MetricsController | 8 (list, query, aggregate, top, record, series) |
| TraceController | 7 (search, get, waterfall, service map, analyze) |
| RetentionController | 5 (policies CRUD, execute) |
| AnomalyController | 5 (baselines, detect, train, configure) |
| BaseController | (shared utilities) |

### 7.4 Courier Controllers (13) — `/api/v1/courier/`

| Controller | Endpoints |
|---|---|
| HealthController | 1 (public health check) |
| CollectionController | 8 (CRUD, tree, summary, paged) |
| FolderController | 6 (CRUD, reorder, move) |
| RequestController | 8 (CRUD, duplicate, headers/params/body/auth/scripts) |
| EnvironmentController | 8 (CRUD, activate, clone, variables) |
| VariableController | 4 (global CRUD, batch) |
| ProxyController | 2 (send request, send with scripts) |
| HistoryController | 5 (list, detail, clear, stats, request history) |
| ShareController | 5 (share, list, update permission, revoke, shared-with-me) |
| ImportController | 3 (import collection — Postman/OpenAPI/cURL) |
| GraphQLController | 4 (execute, introspect, validate, format) |
| RunnerController | 5 (start run, get results, get iterations, cancel, paged) |
| CodeGenerationController | 3 (generate, languages, templates) |

---

## 8. Service Layer

### 8.1 Core Services (26)
AuthService, UserService, TeamService, ProjectService, AdminService, PersonaService, DirectiveService, MfaService, NotificationService, TokenBlacklistService, EncryptionService, ComplianceService, FindingService, RemediationTaskService, BugInvestigationService, TechDebtService, DependencyService, HealthMonitorService, MetricsService, QaJobService, AgentRunService, S3StorageService, ReportStorageService, GitHubConnectionService, JiraConnectionService, AuditLogService

### 8.2 Registry Services (10)
ServiceRegistryService, SolutionService, PortAllocationService, DependencyGraphService, ConfigEngineService, TopologyService, HealthCheckService, ApiRouteService, InfraResourceService, WorkstationProfileService

### 8.3 Logger Services (19)
LogIngestionService, LogQueryService, LogParsingService, LogSourceService, LogTrapService, TrapEvaluationEngine, AlertService, AlertChannelService, DashboardService, MetricsService (@Service("loggerMetricsService")), MetricAggregationService, TraceService, TraceAnalysisService, RetentionService, RetentionExecutor, AnomalyDetectionService, AnomalyBaselineCalculator, KafkaLogConsumer, LogQueryDslParser

### 8.4 Courier Services (22)
CollectionService, FolderService, RequestService, EnvironmentService, VariableService, RequestProxyService, GraphQLService, CollectionRunnerService, CodeGenerationService, HistoryService, ShareService, ForkService, MergeService, ImportService, ExportService, ScriptEngineService, AuthResolverService, PostmanImporter, OpenApiImporter, CurlImporter, DataFileParser, ScriptContext

---

## 9. Configuration

### 9.1 Shared Config Classes (16)
AppConstants, AsyncConfig, CorsConfig, DataSeeder, GlobalExceptionHandler, HealthController, JacksonConfig, JwtProperties, KafkaConsumerConfig, LoggingInterceptor, MailProperties, RequestCorrelationFilter, RestTemplateConfig, S3Config, SecurityConfig, WebMvcConfig

### 9.2 Module-Specific Config
- **Courier:** HttpClientConfig (java.net.http.HttpClient bean with 30s timeout, HTTP/1.1)

### 9.3 Application Properties
- **Port:** 8090
- **Database:** PostgreSQL (codeops/codeops/codeops)
- **Hibernate:** ddl-auto: update, open-in-view: false
- **Kafka:** bootstrap-servers: localhost:9092
- **JWT:** HS256, 24h access / 30d refresh
- **Profiles:** dev (DataSeeder), test (H2), prod

---

## 10. Security

- **Authentication:** JWT (HS256) via JwtAuthFilter → JwtTokenProvider
- **Public endpoints:** `/api/v1/auth/**`, `/api/v1/health`, `/api/v1/courier/health`, `/swagger-ui/**`, `/v3/api-docs/**`
- **Authorization:** `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")` on Registry/Logger/Courier controllers; service-layer team membership verification on Core
- **Roles:** OWNER, ADMIN, MEMBER, VIEWER (team-scoped, embedded in JWT)
- **MFA:** TOTP and Email-based, optional per user
- **Rate Limiting:** RateLimitFilter (Redis-backed)
- **Encryption:** AES-256-GCM for stored credentials (GitHubConnection, JiraConnection)

---

## 11. Infrastructure

| Service | Port | Container | Purpose |
|---------|------|-----------|---------|
| CodeOps-Server | 8090 | — | Application (all 4 modules) |
| PostgreSQL | 5432 | codeops-db | 72 tables in `public` schema |
| Redis | 6379 | codeops-redis | Rate limiting, caching |
| Kafka | 9092 | codeops-kafka | Log ingestion pipeline |
| Zookeeper | 2181 | codeops-zookeeper | Kafka coordination |

---

## 12. Test Coverage

| Module | Controller Tests | Service Tests | Other Tests | Total |
|--------|-----------------|---------------|-------------|-------|
| Core | 18 files | 27 files | 35 files (config, security, notification, integration) | ~900 methods |
| Registry | 10 files | 11 files | 1 file (SlugUtils) | ~548 methods |
| Logger | 11 files | 15 files | 1 file (event) | ~450 methods |
| Courier | 13 files | 21 files | — | ~617 methods |
| **Total** | **52 files** | **74 files** | **37 files** | **2540 methods** |

All tests pass: `mvn test` → 2540 tests, 0 failures, 0 errors, 0 skipped.

---

## 13. Bean Name Qualifiers

Duplicate simple class names across modules are resolved with explicit bean names:

| Bean Name | Class | Package |
|---|---|---|
| `loggerMetricsService` | MetricsService | com.codeops.logger.service |
| `loggerMetricsController` | MetricsController | com.codeops.logger.controller |
| `courierHealthController` | HealthController | com.codeops.courier.controller |
| `registryDependencyController` | DependencyController | com.codeops.registry.controller |

Core-side classes retain default bean names (metricsService, metricsController, healthController, dependencyController).

---

## 14. Consolidation History

| Task | Date | Commit | Description |
|---|---|---|---|
| CON-001 | 2026-02-22 | e480689 | Merged CodeOps-Registry into CodeOps-Server |
| CON-002 | 2026-02-23 | 290a9d6 | Merged CodeOps-Logger into CodeOps-Server |
| CON-003 | 2026-02-24 | 792ab8e | Merged CodeOps-Courier into CodeOps-Server |
| CON-004 | 2026-02-24 | (pending) | Post-consolidation verification and artifact generation |
