# CodeOps Server — Codebase Audit

**Audit Date:** 2026-02-27T21:40:31Z
**Branch:** main
**Commit:** 68f626f887f60b7cc0353b2d694698a528161bda CO-AUDIT: Add comprehensive Courier module audit
**Auditor:** Claude Code (Automated)
**Purpose:** Zero-context reference for AI-assisted development
**Audit File:** CodeOps-Server-Audit.md
**Scorecard:** CodeOps-Server-Scorecard.md
**OpenAPI Spec:** CodeOps-Server-OpenAPI.yaml (generated separately)

> This audit is the source of truth for the CodeOps Server codebase structure, entities, services, and configuration.
> The OpenAPI spec (CodeOps-Server-OpenAPI.yaml) is the source of truth for all endpoints, DTOs, and API contracts.
> An AI reading this audit + the OpenAPI spec should be able to generate accurate code
> changes, new features, tests, and fixes without filesystem access.

---

## 1. Project Identity

```
Project Name: CodeOps Server
Repository URL: https://github.com/AI-CodeOps/CodeOps-Server.git
Primary Language / Framework: Java / Spring Boot 3.3.0
Java Version: 21 (runtime: Java 25 OpenJDK)
Build Tool + Version: Maven (Spring Boot parent 3.3.0)
Current Branch: main
Latest Commit Hash: 68f626f887f60b7cc0353b2d694698a528161bda
Latest Commit Message: CO-AUDIT: Add comprehensive Courier module audit
Audit Timestamp: 2026-02-27T21:40:31Z
```

---

## 2. Directory Structure

Single-module Spring Boot monolith with 6 logical modules under `com.codeops`:
- **core** (`com.codeops.*`) — Auth, teams, projects, QA jobs, findings, compliance, health monitoring, MFA, integrations
- **courier** (`com.codeops.courier.*`) — API client/testing tool (collections, requests, environments, runners, scripts)
- **fleet** (`com.codeops.fleet.*`) — Docker container management (service profiles, workstations, deployments)
- **logger** (`com.codeops.logger.*`) — Centralized logging, metrics, alerts, anomaly detection, tracing, dashboards
- **registry** (`com.codeops.registry.*`) — Service registry, port allocation, dependencies, topology, infrastructure
- **relay** (`com.codeops.relay.*`) — Team messaging (channels, DMs, threads, reactions, presence, WebSocket)

**Source counts (src/main):** 96 entities, 67 enums, 97 repositories, 85 services, ~55 controllers, ~40 mappers
**Test counts (src/test):** ~205 unit test files, 16 integration test files

**Key config files:** `pom.xml`, `docker-compose.yml`, `Dockerfile`, `src/main/resources/application.yml`, `application-dev.yml`, `application-prod.yml`, `logback-spring.xml`

---

## 3. Build & Dependency Manifest

**Build file:** `pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.3.0 (parent) | REST controllers, embedded Tomcat |
| spring-boot-starter-data-jpa | 3.3.0 | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.3.0 | Authentication/authorization |
| spring-boot-starter-validation | 3.3.0 | Jakarta Bean Validation |
| spring-boot-starter-websocket | 3.3.0 | STOMP WebSocket (Relay module) |
| spring-boot-starter-mail | 3.3.0 | SMTP email (MFA, notifications) |
| postgresql | runtime | PostgreSQL JDBC driver |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT token creation/validation |
| aws-sdk-s3 | 2.25.0 | S3 storage (prod) / local filesystem (dev) |
| totp | 1.7.1 | TOTP-based MFA |
| lombok | 1.18.42 | Boilerplate reduction |
| mapstruct | 1.5.5.Final | Entity↔DTO mapping |
| jackson-datatype-jsr310 | (managed) | Java 8+ date/time serialization |
| jackson-dataformat-yaml | (managed) | YAML parsing (OpenAPI import) |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI / OpenAPI generation |
| logstash-logback-encoder | 7.4 | Structured JSON logging |
| spring-kafka | (managed) | Kafka consumer (Logger module) |
| graalvm-polyglot / js | 24.1.1 | JavaScript script engine (Courier) |
| spring-boot-starter-test | 3.3.0 | JUnit 5, Mockito, MockMvc |
| spring-security-test | (managed) | Security test utilities |
| testcontainers-postgresql | 1.19.8 | Integration test PostgreSQL |
| testcontainers-kafka | 1.19.8 | Integration test Kafka |
| h2 | (managed) | In-memory DB for unit tests |
| mockito | 5.21.0 | Mocking (Java 25 compat override) |
| byte-buddy | 1.18.4 | Bytecode gen (Java 25 compat override) |

**Build plugins:** maven-compiler-plugin (Java 21, Lombok+MapStruct annotation processors), maven-surefire-plugin (--add-opens for Java 25, includes *Test.java + *IT.java), jacoco-maven-plugin 0.8.14 (coverage reports)

**Build commands:**
```
Build: mvn clean package -DskipTests
Test: mvn test
Run: mvn spring-boot:run (or ./start-codeops.sh)
Package: mvn clean package
```

---

## 4. Configuration & Infrastructure Summary

- **`application.yml`** — Default profile. Server port 8090, context-path `/`. JPA ddl-auto: update, open-in-view: false. JWT secret, encryption key, CORS origins from env vars. Kafka bootstrap-servers: localhost:9094.
- **`application-dev.yml`** — PostgreSQL localhost:5432/codeops (user: codeops, pass: codeops). S3 disabled (local filesystem at ~/.codeops/storage/). Email logging to console. Hibernate show-sql: true.
- **`application-prod.yml`** — All secrets from env vars: `${DATABASE_URL}`, `${DATABASE_USERNAME}`, `${DATABASE_PASSWORD}`, `${JWT_SECRET}`, `${ENCRYPTION_KEY}`, `${CORS_ALLOWED_ORIGINS}`, `${AWS_REGION}`, `${MAIL_FROM_EMAIL}`.
- **`logback-spring.xml`** — Dev profile: console pattern with colors. Prod profile: JSON via LogstashEncoder.
- **`docker-compose.yml`** — PostgreSQL 16 (5432, container `codeops-db`), Redis 7 (6379, `codeops-redis`), Zookeeper (2181), Kafka (9094, `codeops-kafka`).
- **`Dockerfile`** — Multi-stage: Maven build → Eclipse Temurin 21-jre, non-root user `codeops`, exposes 8090.

**Connection map:**
```
Database: PostgreSQL, localhost:5432, database: codeops
Cache: Redis configured in docker-compose but NOT used by application code
Message Broker: Kafka, localhost:9094 (Logger module consumes log entries)
External APIs: None (GitHub/Jira connections store credentials but proxy calls are client-initiated)
Cloud Services: AWS S3 (prod only, disabled in dev → local filesystem)
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

- **Entry point:** `CodeOpsApplication.java` — `@SpringBootApplication`, `@EnableConfigurationProperties({JwtProperties.class, MailProperties.class})`, `@EnableScheduling`
- **Startup initialization:** `DataSeeder` (dev profile) seeds 22 entity types via `@PostConstruct`. `FleetDataSeeder` seeds fleet service profiles. `RelayDataSeeder` seeds relay channels and platform events.
- **Scheduled tasks:**
  - `MfaService.cleanupExpiredCodes()` — every 15 minutes, deletes expired MFA email codes
  - `RetentionExecutor.executeRetentionPolicies()` — daily at 2:00 AM, applies log retention policies
  - `AnomalyDetectionService.runScheduledDetection()` — daily at 3:00 AM, checks metric baselines for anomalies
- **Health check:** `GET /api/v1/health` → `{ "status": "UP", ... }` (public, no auth)

---

## 6. Entity / Data Model Layer

All entities extend `BaseEntity` unless noted. BaseEntity provides: `UUID id` (@GeneratedValue UUID), `Instant createdAt` (@CreatedDate), `Instant updatedAt` (@LastModifiedDate). All use Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.

### 6.1 Core Module Entities

#### User
- **Table:** `users`
- **Fields:** `email` (String, unique, 255), `password` (String, 255), `firstName` (String, 100), `lastName` (String, 100), `avatarUrl` (String, 500), `isActive` (boolean, default true), `mfaEnabled` (boolean, default false), `mfaSecret` (String, 500, nullable), `mfaMethod` (MfaMethod enum, nullable), `mfaRecoveryCodes` (String, TEXT, nullable), `lastLoginAt` (Instant, nullable)
- **Relationships:** None (referenced by UUID in other entities)

#### Team
- **Table:** `teams`
- **Unique:** `(name)`
- **Fields:** `name` (String, 100), `description` (String, 500), `logoUrl` (String, 500, nullable)
- **Relationships:** `@OneToMany → TeamMember (mappedBy="team", cascade=ALL, orphanRemoval=true)`

#### TeamMember
- **Table:** `team_members`
- **Unique:** `(team_id, user_id)`
- **Fields:** `role` (TeamRole enum)
- **Relationships:** `@ManyToOne → Team`, `@ManyToOne → User`

#### Project
- **Table:** `projects`
- **Unique:** `(team_id, name)`
- **Fields:** `name` (String, 200), `description` (String, 2000, nullable), `repositoryUrl` (String, 500, nullable), `techStack` (String, 500, nullable), `isArchived` (boolean, default false)
- **Relationships:** `@ManyToOne → Team`, `@ManyToOne → User (createdBy)`, `@ManyToOne → GitHubConnection (nullable)`, `@ManyToOne → JiraConnection (nullable)`

#### QaJob
- **Table:** `qa_jobs`
- **Fields:** `status` (JobStatus enum), `result` (JobResult enum, nullable), `mode` (JobMode enum), `targetBranch` (String, 200, nullable), `pullRequestUrl` (String, 500, nullable), `commitSha` (String, 100, nullable), `configJson` (String, TEXT, nullable), `startedAt`/`completedAt` (Instant, nullable), `errorMessage` (String, TEXT, nullable)
- **Relationships:** `@ManyToOne → Project`, `@ManyToOne → User (startedBy)`, `@ManyToOne → Persona (nullable)`, `@OneToMany → AgentRun`, `@OneToMany → Finding`

#### AgentRun
- **Table:** `agent_runs`
- **Fields:** `agentType` (AgentType enum), `status` (AgentStatus enum), `result` (AgentResult enum, nullable), `outputMarkdown` (String, TEXT, nullable), `s3ReportKey` (String, 500, nullable), `startedAt`/`completedAt` (Instant, nullable), `errorMessage` (String, TEXT, nullable)
- **Relationships:** `@ManyToOne → QaJob`

#### Finding
- **Table:** `findings`
- **Fields:** `severity` (Severity enum), `agentType` (AgentType enum), `status` (FindingStatus enum), `title` (String, 500), `description` (String, TEXT), `filePath` (String, 500, nullable), `lineNumber` (Integer, nullable), `suggestion` (String, TEXT, nullable), `category` (String, 200, nullable)
- **Relationships:** `@ManyToOne → QaJob`, `@ManyToOne → User (assignedTo, nullable)`

#### BugInvestigation
- **Table:** `bug_investigations`
- **Fields:** `jiraKey` (String, 50, nullable), `jiraUrl` (String, 500, nullable), `bugDescription` (String, TEXT), `rootCauseAnalysis` (String, TEXT, nullable), `s3RcaKey` (String, 500, nullable), `suggestedFix` (String, TEXT, nullable), `affectedFiles` (String, TEXT, nullable), `severity` (Severity enum), `status` (FindingStatus enum)
- **Relationships:** `@ManyToOne → QaJob`

#### ComplianceItem
- **Table:** `compliance_items`
- **Fields:** `specificationName` (String, 500), `requirementId` (String, 100, nullable), `requirement` (String, TEXT), `status` (ComplianceStatus enum), `evidence` (String, TEXT, nullable), `notes` (String, TEXT, nullable)
- **Relationships:** `@ManyToOne → QaJob`, `@ManyToOne → Specification (nullable)`

#### Specification
- **Table:** `specifications`
- **Fields:** `name` (String, 500), `version` (String, 50, nullable), `specType` (SpecType enum), `s3Key` (String, 500, nullable), `contentMd` (String, TEXT, nullable)
- **Relationships:** `@ManyToOne → QaJob`

#### RemediationTask
- **Table:** `remediation_tasks`
- **Fields:** `taskNumber` (int), `title` (String, 500), `description` (String, TEXT), `filePath` (String, 500, nullable), `priority` (Priority enum), `effort` (Effort enum), `status` (TaskStatus enum), `s3PromptKey` (String, 500, nullable)
- **Relationships:** `@ManyToOne → QaJob`, `@ManyToOne → Finding (nullable)`, `@ManyToOne → User (assignedTo, nullable)`

#### TechDebtItem
- **Table:** `tech_debt_items`
- **Fields:** `title` (String, 500), `description` (String, TEXT), `filePath` (String, 500, nullable), `lineNumber` (Integer, nullable), `category` (DebtCategory enum), `status` (DebtStatus enum), `severity` (Severity enum), `effort` (Effort enum), `businessImpact` (BusinessImpact enum, nullable), `suggestion` (String, TEXT, nullable)
- **Relationships:** `@ManyToOne → Project`, `@ManyToOne → QaJob (nullable)`

#### DependencyScan
- **Table:** `dependency_scans`
- **Fields:** `scanDate` (Instant), `totalDependencies` (int), `vulnerableCount` (int), `outdatedCount` (int), `packageManager` (String, 50, nullable), `manifestFile` (String, 200, nullable)
- **Relationships:** `@ManyToOne → Project`, `@ManyToOne → QaJob (nullable)`, `@OneToMany → DependencyVulnerability`

#### DependencyVulnerability
- **Table:** `dependency_vulnerabilities`
- **Fields:** `dependencyName` (String, 500), `currentVersion` (String, 100), `recommendedVersion` (String, 100, nullable), `severity` (Severity enum), `cveId` (String, 50, nullable), `description` (String, TEXT, nullable), `status` (VulnerabilityStatus enum)
- **Relationships:** `@ManyToOne → DependencyScan`

#### HealthSchedule
- **Table:** `health_schedules`
- **Fields:** `scheduleType` (ScheduleType enum), `cronExpression` (String, 100, nullable), `intervalMinutes` (Integer, nullable), `isActive` (boolean, default true), `lastRunAt` (Instant, nullable), `nextRunAt` (Instant, nullable)
- **Relationships:** `@ManyToOne → Project`, `@ManyToOne → User (createdBy)`

#### HealthSnapshot
- **Table:** `health_snapshots`
- **Fields:** `overallScore` (int), `testCoverage` (Double, nullable), `codeQuality` (Double, nullable), `securityScore` (Double, nullable), `performanceScore` (Double, nullable), `documentationScore` (Double, nullable), `dependencyHealth` (Double, nullable), `summaryJson` (String, TEXT, nullable)
- **Relationships:** `@ManyToOne → Project`, `@ManyToOne → QaJob (nullable)`

#### Persona
- **Table:** `personas`
- **Fields:** `name` (String, 200), `description` (String, 2000, nullable), `agentType` (AgentType enum), `scope` (Scope enum), `systemPrompt` (String, TEXT), `isDefault` (boolean, default false)
- **Relationships:** `@ManyToOne → Team (nullable)`, `@ManyToOne → User (createdBy, nullable)`

#### Directive
- **Table:** `directives`
- **Fields:** `name` (String, 200), `description` (String, 2000, nullable), `category` (DirectiveCategory enum), `scope` (DirectiveScope enum), `contentMd` (String, TEXT), `isShared` (boolean, default false)
- **Relationships:** `@ManyToOne → Team`, `@ManyToOne → User (createdBy)`

#### ProjectDirective (composite PK)
- **Table:** `project_directives`
- **PK:** `ProjectDirectiveId` (projectId + directiveId) — `@IdClass`
- **Fields:** `isEnabled` (boolean, default true)
- **Relationships:** `@ManyToOne → Project (@Id)`, `@ManyToOne → Directive (@Id)`

#### Invitation
- **Table:** `invitations`
- **Unique:** `(token)`
- **Fields:** `email` (String, 255), `role` (TeamRole enum), `token` (String, 500), `status` (InvitationStatus enum), `expiresAt` (Instant)
- **Relationships:** `@ManyToOne → Team`, `@ManyToOne → User (invitedBy)`

#### GitHubConnection
- **Table:** `github_connections`
- **Fields:** `name` (String, 200), `authType` (GitHubAuthType enum), `encryptedCredentials` (String, 2000), `apiUrl` (String, 500, default "https://api.github.com"), `isActive` (boolean, default true)
- **Relationships:** `@ManyToOne → Team`, `@ManyToOne → User (createdBy)`

#### JiraConnection
- **Table:** `jira_connections`
- **Fields:** `name` (String, 200), `instanceUrl` (String, 500), `email` (String, 255), `encryptedApiToken` (String, 2000), `isActive` (boolean, default true)
- **Relationships:** `@ManyToOne → Team`, `@ManyToOne → User (createdBy)`

#### MfaEmailCode
- **Table:** `mfa_email_codes`
- **Fields:** `code` (String, 10), `expiresAt` (Instant), `used` (boolean, default false)
- **Relationships:** `@ManyToOne → User`

#### NotificationPreference
- **Table:** `notification_preferences`
- **Unique:** `(user_id, event_type, channel)`
- **Fields:** `eventType` (String, 100), `channel` (String, 50), `isEnabled` (boolean, default true)
- **Relationships:** `@ManyToOne → User`

#### SystemSetting (non-BaseEntity)
- **Table:** `system_settings`
- **PK:** `String key` (not UUID)
- **Fields:** `value` (String, TEXT), `description` (String, 500, nullable), `updatedBy` (UUID, nullable), `updatedAt` (Instant)

#### AuditLog (non-BaseEntity)
- **Table:** `audit_logs`
- **PK:** `Long id` (@GeneratedValue IDENTITY)
- **Fields:** `userId` (UUID), `teamId` (UUID, nullable), `action` (String, 100), `entityType` (String, 100), `entityId` (UUID, nullable), `details` (String, TEXT, nullable), `ipAddress` (String, 50, nullable), `timestamp` (Instant)

### 6.2 Courier Module Entities

**See detailed audit in `Courier-Audit.md`.** Summary of 18 entities:

- **Collection** — `collections` table. Fields: teamId, name, description, preRequestScript, postResponseScript, authType (AuthType), authConfig, isShared, createdBy. Has `@OneToMany → Folder, EnvironmentVariable`.
- **CollectionShare** — `collection_shares`. permission (SharePermission), sharedWithUserId, sharedByUserId. `@ManyToOne → Collection`.
- **Environment** — `environments`. teamId, name, description, isActive, createdBy. `@OneToMany → EnvironmentVariable`.
- **EnvironmentVariable** — `environment_variables`. variableKey, variableValue, isSecret, isEnabled, scope. `@ManyToOne → Environment (nullable), Collection (nullable)`.
- **Folder** — `folders`. name, description, sortOrder. `@ManyToOne → Collection, Folder (parent, nullable)`. `@OneToMany → Request, Folder (children)`.
- **Fork** — `forks`. sourceCollectionId, sourceCollectionName. `@ManyToOne → Collection (forked)`.
- **GlobalVariable** — `global_variables`. teamId, variableKey, variableValue, isSecret, isEnabled.
- **MergeRequest** — `merge_requests`. sourceCollectionId, targetCollectionId, title, description, status (RunStatus), resolvedBy, resolvedAt, conflictsJson.
- **Request** — `requests`. name, httpMethod (HttpMethod), url, description, sortOrder. `@ManyToOne → Folder`. `@OneToMany → RequestHeader, RequestParam, RequestBody (one), RequestAuth (one), RequestScript`.
- **RequestAuth** — `request_auths`. authType (AuthType), authConfig (JSON). `@ManyToOne → Request`.
- **RequestBody** — `request_bodies`. bodyType (BodyType), rawContent, formData, graphqlQuery, graphqlVariables, binaryFileName, binaryContentType, binaryStoragePath. `@ManyToOne → Request`.
- **RequestHeader** — `request_headers`. headerKey, headerValue, description, isEnabled. `@ManyToOne → Request`.
- **RequestHistory** — `request_histories`. url, httpMethod, statusCode, requestHeaders/Body, responseHeaders/Body, responseSizeBytes, responseTimeMs, error, environmentId. `@ManyToOne → Request`.
- **RequestParam** — `request_params`. paramKey, paramValue, description, isEnabled. `@ManyToOne → Request`.
- **RequestScript** — `request_scripts`. scriptType (ScriptType), scriptContent. `@ManyToOne → Request`.
- **RunIteration** — `run_iterations`. iterationNumber, status (RunStatus), startedAt, completedAt, totalRequests, passedRequests, failedRequests, dataRow. `@ManyToOne → RunResult`.
- **RunResult** — `run_results`. status (RunStatus), totalRequests, passedRequests, failedRequests, totalDurationMs, startedAt, completedAt, dataFilePath, iterationCount. `@ManyToOne → Collection, Environment (nullable)`. `@OneToMany → RunIteration`.
- **CodeSnippetTemplate** — `code_snippet_templates`. language (CodeLanguage), label, templateContent, sortOrder.

### 6.3 Fleet Module Entities

- **ServiceProfile** — `service_profiles`. teamId, name, imageName, imageTag, description, registryUrl, command, workingDir, cpuLimit, memoryLimitMb, restartPolicy (RestartPolicy), replicas, healthCheckCommand, healthCheckIntervalSeconds, healthCheckTimeoutSeconds, healthCheckRetries, createdByUserId. `@OneToMany → EnvironmentVariable, PortMapping, VolumeMount, NetworkConfig, ContainerHealthCheck`.
- **SolutionProfile** — `solution_profiles`. Unique: `(team_id, name)`. teamId, name, description, createdByUserId. `@OneToMany → SolutionService`.
- **SolutionService** — `solution_services`. Unique: `(solution_profile_id, service_profile_id)`. displayOrder, overrideEnvVars (TEXT). `@ManyToOne → SolutionProfile, ServiceProfile`.
- **WorkstationProfile** — `fleet_workstation_profiles` (note: prefixed to avoid collision with registry module). teamId, name, description, createdByUserId, isDefault. `@OneToMany → WorkstationSolution`.
- **WorkstationSolution** — `fleet_workstation_solutions`. Unique: `(workstation_profile_id, solution_profile_id)`. displayOrder. `@ManyToOne → WorkstationProfile, SolutionProfile`.
- **ContainerInstance** — `container_instances`. teamId, containerId, containerName, serviceProfileId (UUID), status (ContainerStatus), hostPort, startedAt, stoppedAt, exitCode, lastHealthCheck, healthStatus (HealthStatus), createdByUserId.
- **ContainerHealthCheck** — `container_health_checks`. command, intervalSeconds, timeoutSeconds, retries, startPeriodSeconds. `@ManyToOne → ServiceProfile`.
- **ContainerLog** — `container_logs`. containerId, containerName, message, timestamp, stream.
- **DeploymentRecord** — `deployment_records`. teamId, solutionProfileId, action (DeploymentAction), status (ContainerStatus), initiatedByUserId, startedAt, completedAt, errorMessage. `@OneToMany → DeploymentContainer`.
- **DeploymentContainer** — `deployment_containers`. containerId, containerName, serviceProfileId, status (ContainerStatus), exitCode, errorMessage. `@ManyToOne → DeploymentRecord`.
- **EnvironmentVariable** (fleet) — `fleet_environment_variables`. variableKey, variableValue, isSecret. `@ManyToOne → ServiceProfile`.
- **NetworkConfig** — `network_configs`. networkName, driver, aliases (TEXT). `@ManyToOne → ServiceProfile`.
- **PortMapping** — `port_mappings`. hostPort, containerPort, protocol. `@ManyToOne → ServiceProfile`.
- **VolumeMount** — `volume_mounts`. hostPath, containerPath, readOnly. `@ManyToOne → ServiceProfile`.

### 6.4 Logger Module Entities

- **LogSource** — `log_sources`. teamId, name, sourceType (String), environment, host, description, isActive, apiKey, createdBy.
- **LogEntry** — `log_entries`. Indexed: correlation_id, team_id, source_id, timestamp, level, service_name. teamId, sourceId (UUID), correlationId, timestamp, level (LogLevel), serviceName, message (TEXT), stackTrace (TEXT), metadata (TEXT JSON), host, environment, traceId, spanId.
- **Dashboard** — `dashboards`. teamId, name, description, isDefault, layoutJson (TEXT), createdBy. `@OneToMany → DashboardWidget`.
- **DashboardWidget** — `dashboard_widgets`. title, widgetType (WidgetType), configJson (TEXT), position, width, height. `@ManyToOne → Dashboard`.
- **SavedQuery** — `saved_queries`. name, description, queryJson (TEXT), queryDsl (TEXT), teamId, createdBy, isShared, lastExecutedAt, executionCount.
- **QueryHistory** — `query_history`. queryJson (TEXT), queryDsl (TEXT), resultCount, executionTimeMs, teamId, createdBy.
- **Metric** — `metrics`. Unique: `(team_id, name)`. name, metricType (MetricType), description, unit, serviceName, tags (TEXT), teamId.
- **MetricSeries** — `metric_series`. timestamp, value (Double), tags (TEXT), resolution (Integer). `@ManyToOne → Metric`.
- **AlertRule** — `alert_rules`. teamId, name, description, metricName, condition (String), threshold (Double), windowMinutes, severity (AlertSeverity), isEnabled, lastTriggeredAt, createdBy. `@ManyToOne → AlertChannel (nullable)`.
- **AlertChannel** — `alert_channels`. teamId, name, channelType (AlertChannelType), configJson (TEXT), isEnabled, createdBy.
- **AlertHistory** — `alert_histories`. message (TEXT), severity (AlertSeverity), status (AlertStatus), metricValue (Double), acknowledgedBy, acknowledgedAt. `@ManyToOne → AlertRule`.
- **AnomalyBaseline** — `anomaly_baselines`. Unique: `(metric_id)`. meanValue, stdDeviation, minValue, maxValue, sampleCount, periodHours, lastCalculatedAt. `@ManyToOne → Metric`.
- **LogTrap** — `log_traps`. teamId, name, description, trapType (TrapType), isEnabled, triggerCount, lastTriggeredAt, createdBy. `@ManyToOne → AlertChannel (nullable)`. `@OneToMany → TrapCondition`.
- **TrapCondition** — `trap_conditions`. conditionType (ConditionType), field, pattern (TEXT), threshold (Integer), windowSeconds, serviceName, logLevel (LogLevel). `@ManyToOne → LogTrap`.
- **RetentionPolicy** — `retention_policies`. name, sourceName, logLevel (LogLevel, nullable), retentionDays, action (RetentionAction), archiveDestination, isActive, teamId, createdBy, lastExecutedAt.
- **TraceSpan** — `trace_spans`. correlationId, traceId, spanId, parentSpanId, serviceName, operationName, startTime, endTime, durationMs, status (SpanStatus), statusMessage (TEXT), tags (TEXT), teamId.

### 6.5 Registry Module Entities

- **ServiceRegistration** — `service_registrations`. Unique: `(team_id, slug)`. teamId, name, slug, serviceType (ServiceType), description, repoUrl, repoFullName, defaultBranch, techStack, status (ServiceStatus), healthCheckUrl, healthCheckIntervalSeconds, lastHealthStatus (HealthStatus), lastHealthCheckAt, environmentsJson (TEXT), metadataJson (TEXT), createdByUserId.
- **ServiceDependency** — `service_dependencies`. Unique: `(source_service_id, target_service_id)`. dependencyType (DependencyType), description, isRequired, targetEndpoint. `@ManyToOne → ServiceRegistration (source, target)`.
- **PortAllocation** — `port_allocations`. Unique: `(service_id, environment, port_type)`. environment, portType (PortType), portNumber, protocol, description, isAutoAllocated, allocatedByUserId. `@ManyToOne → ServiceRegistration`.
- **PortRange** — `port_ranges`. Unique: `(team_id, port_type, environment)`. teamId, portType (PortType), rangeStart, rangeEnd, environment, description.
- **ApiRouteRegistration** — `api_route_registrations`. Unique: `(service_id, route_prefix, environment)`. routePrefix, httpMethods, environment, description. `@ManyToOne → ServiceRegistration (service, gatewayService nullable)`.
- **ConfigTemplate** — `config_templates`. Unique: `(service_id, template_type, environment)`. templateType (ConfigTemplateType), environment, contentText (TEXT), isAutoGenerated, generatedFrom, version. `@ManyToOne → ServiceRegistration`.
- **EnvironmentConfig** — `environment_configs`. Unique: `(service_id, environment, config_key)`. environment, configKey, configValue (TEXT), configSource (ConfigSource), description. `@ManyToOne → ServiceRegistration`.
- **InfraResource** — `infra_resources`. teamId, resourceType (InfraResourceType), resourceName, environment, region, arnOrUrl, metadataJson (TEXT), description, createdByUserId. `@ManyToOne → ServiceRegistration (nullable)`.
- **Solution** — `solutions`. Unique: `(team_id, slug)`. teamId, name, slug, description, category (SolutionCategory), status (SolutionStatus), iconName, colorHex, ownerUserId, repositoryUrl, documentationUrl, metadataJson (TEXT), createdByUserId. `@OneToMany → SolutionMember`.
- **SolutionMember** — `solution_members`. Unique: `(solution_id, service_id)`. role (SolutionMemberRole), displayOrder, notes. `@ManyToOne → Solution, ServiceRegistration`.
- **WorkstationProfile** (registry) — `workstation_profiles`. teamId, name, description, solutionId (UUID, nullable), servicesJson (TEXT), startupOrder (TEXT), createdByUserId, isDefault.

### 6.6 Relay Module Entities

- **Channel** — `channels`. Unique: `(team_id, slug)`. name, slug, description, topic, channelType (ChannelType), teamId, projectId (nullable), serviceId (nullable), isArchived, createdBy. `@OneToMany → ChannelMember, PinnedMessage`.
- **ChannelMember** — `channel_members`. Unique: `(channel_id, user_id)`. channelId, userId, role (MemberRole), lastReadAt, isMuted, joinedAt. `@ManyToOne → Channel`.
- **Message** — `messages`. channelId, senderId, content (TEXT), messageType (MessageType), parentId (nullable, for threads), isEdited, editedAt, isDeleted, mentionsEveryone, mentionedUserIds (TEXT), platformEventId (nullable). `@OneToMany → Reaction, FileAttachment`.
- **MessageThread** — `message_threads`. rootMessageId, channelId, replyCount, lastReplyAt, lastReplyBy, participantIds (TEXT).
- **DirectConversation** — `direct_conversations`. Unique: `(team_id, participant_ids)`. teamId, conversationType (ConversationType), name, participantIds (TEXT), lastMessageAt, lastMessagePreview.
- **DirectMessage** — `direct_messages`. conversationId, senderId, content (TEXT), messageType (MessageType), isEdited, editedAt, isDeleted. `@ManyToOne → DirectConversation`. `@OneToMany → Reaction, FileAttachment`.
- **Reaction** — `reactions`. Unique: `(user_id, message_id, emoji)` or `(user_id, direct_message_id, emoji)`. userId, emoji, reactionType (ReactionType), messageId (nullable), directMessageId (nullable). `@ManyToOne → Message, DirectMessage`.
- **PinnedMessage** — `pinned_messages`. messageId, pinnedBy. `@ManyToOne → Channel`.
- **FileAttachment** — `file_attachments`. fileName, contentType, fileSizeBytes, storagePath, thumbnailPath, status (FileUploadStatus), uploadedBy, teamId, messageId (nullable), directMessageId (nullable). `@ManyToOne → Message, DirectMessage`.
- **PlatformEvent** — `platform_events`. eventType (PlatformEventType), teamId, sourceModule, sourceEntityId, title, detail (TEXT), targetChannelId, targetChannelSlug, postedMessageId, isDelivered, deliveredAt.
- **ReadReceipt** — `read_receipts`. Unique: `(channel_id, user_id)`. channelId, userId, lastReadMessageId, lastReadAt.
- **UserPresence** — `user_presences`. Unique: `(user_id, team_id)`. userId, teamId, status (PresenceStatus), lastSeenAt, lastHeartbeatAt, statusMessage.

---

## 7. Enum Inventory

### Core Enums
- **AgentType** — CODE_REVIEW, SECURITY_AUDIT, PERFORMANCE, ACCESSIBILITY, DOCUMENTATION, TEST_COVERAGE, DEPENDENCY_CHECK, COMPLIANCE, BUG_INVESTIGATION, CODE_QUALITY, ARCHITECTURE, SUMMARY
- **AgentStatus** — PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
- **AgentResult** — PASS, FAIL, WARNING, ERROR
- **JobStatus** — PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
- **JobResult** — PASS, FAIL, WARNING
- **JobMode** — FULL_SCAN, PULL_REQUEST, BRANCH_COMPARE, SELECTIVE, BUG_INVESTIGATION, HEALTH_CHECK, COMPLIANCE_AUDIT
- **Severity** — CRITICAL, HIGH, MEDIUM, LOW, INFO
- **FindingStatus** — OPEN, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, FALSE_POSITIVE, WONT_FIX
- **Priority** — CRITICAL, HIGH, MEDIUM, LOW
- **Effort** — TRIVIAL, SMALL, MEDIUM, LARGE, EPIC
- **TaskStatus** — PENDING, IN_PROGRESS, COMPLETED, BLOCKED, CANCELLED
- **TeamRole** — OWNER, ADMIN, MEMBER, VIEWER
- **Scope** — SYSTEM, TEAM, USER
- **ComplianceStatus** — COMPLIANT, NON_COMPLIANT, PARTIAL, NOT_APPLICABLE
- **SpecType** — REQUIREMENTS, API_CONTRACT, DESIGN, SECURITY, PERFORMANCE, ACCESSIBILITY
- **DebtCategory** — CODE_SMELL, COMPLEXITY, DUPLICATION, OUTDATED_DEPENDENCY, MISSING_TEST, MISSING_DOCS, DESIGN_FLAW, SECURITY, PERFORMANCE, CONFIGURATION
- **DebtStatus** — IDENTIFIED, ACCEPTED, IN_PROGRESS, RESOLVED, WONT_FIX
- **BusinessImpact** — CRITICAL, HIGH, MEDIUM, LOW, NONE
- **VulnerabilityStatus** — OPEN, PATCHED, IGNORED, FALSE_POSITIVE
- **ScheduleType** — CRON, INTERVAL, MANUAL
- **InvitationStatus** — PENDING, ACCEPTED, EXPIRED, CANCELLED
- **GitHubAuthType** — PERSONAL_ACCESS_TOKEN, GITHUB_APP
- **MfaMethod** — TOTP, EMAIL
- **DirectiveCategory** — CODING_STANDARD, SECURITY, TESTING, DOCUMENTATION, ARCHITECTURE, PERFORMANCE, ACCESSIBILITY, COMPLIANCE, CUSTOM
- **DirectiveScope** — TEAM, PROJECT

### Courier Enums
- **AuthType** — NO_AUTH, API_KEY, BEARER_TOKEN, BASIC_AUTH, OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS, OAUTH2_IMPLICIT, OAUTH2_PASSWORD, JWT_BEARER, INHERIT_FROM_PARENT
- **BodyType** — NONE, FORM_DATA, X_WWW_FORM_URLENCODED, RAW_JSON, RAW_XML, RAW_HTML, RAW_TEXT, RAW_YAML, BINARY, GRAPHQL
- **HttpMethod** — GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
- **ScriptType** — PRE_REQUEST, POST_RESPONSE
- **RunStatus** — PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
- **SharePermission** — VIEW, EDIT
- **CodeLanguage** — CURL, PYTHON_REQUESTS, PYTHON_HTTP, JAVASCRIPT_FETCH, JAVASCRIPT_AXIOS, JAVA_HTTP_CLIENT, JAVA_OK_HTTP, CSHARP_HTTP_CLIENT, GO_HTTP, RUBY_NET_HTTP, PHP_CURL, RUST_REQWEST

### Fleet Enums
- **ContainerStatus** — CREATED, RUNNING, PAUSED, RESTARTING, REMOVING, EXITED, DEAD, NOT_FOUND
- **DeploymentAction** — DEPLOY, SCALE, RESTART, STOP, DESTROY
- **HealthStatus** — HEALTHY, UNHEALTHY, STARTING, NONE
- **RestartPolicy** — NO, ON_FAILURE, ALWAYS, UNLESS_STOPPED

### Logger Enums
- **LogLevel** — TRACE, DEBUG, INFO, WARN, ERROR, FATAL
- **MetricType** — COUNTER, GAUGE, HISTOGRAM, SUMMARY
- **AlertSeverity** — CRITICAL, HIGH, MEDIUM, LOW, INFO
- **AlertStatus** — FIRING, ACKNOWLEDGED, RESOLVED, SILENCED
- **AlertChannelType** — EMAIL, TEAMS_WEBHOOK, SLACK_WEBHOOK, PAGERDUTY
- **ConditionType** — CONTAINS, NOT_CONTAINS, REGEX, EQUALS, LEVEL_THRESHOLD, RATE_THRESHOLD, FIELD_VALUE
- **TrapType** — PATTERN, THRESHOLD, ANOMALY
- **SpanStatus** — OK, ERROR
- **RetentionAction** — DELETE, ARCHIVE, DOWNSAMPLE
- **WidgetType** — LOG_STREAM, LOG_CHART, METRIC_LINE, METRIC_BAR, METRIC_GAUGE, METRIC_STAT, TRACE_LIST, ALERT_STATUS

### Registry Enums
- **ServiceType** — WEB_APP, API_SERVICE, MICROSERVICE, WORKER, SCHEDULER, DATABASE, CACHE, MESSAGE_BROKER, SEARCH_ENGINE, LOAD_BALANCER, REVERSE_PROXY, CDN, MONITORING, LOGGING, CI_CD, CONTAINER_REGISTRY, IDENTITY_PROVIDER, FILE_STORAGE, EMAIL_SERVICE, OTHER
- **ServiceStatus** — ACTIVE, INACTIVE, DEPRECATED, DECOMMISSIONED
- **HealthStatus** (registry) — HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN
- **DependencyType** — HTTP_REST, HTTP_GRAPHQL, GRPC, DATABASE, MESSAGE_QUEUE, FILE_SYSTEM, SHARED_LIBRARY, DNS, TCP_SOCKET, OTHER
- **PortType** — HTTP, HTTPS, GRPC, DATABASE, CACHE, MESSAGE_BROKER, MONITORING, DEBUG, MANAGEMENT, WEBSOCKET, CUSTOM, OTHER
- **ConfigTemplateType** — DOCKER_COMPOSE, DOCKERFILE, NGINX, ENVOY, KUBERNETES_DEPLOYMENT, KUBERNETES_SERVICE, KUBERNETES_INGRESS, KUBERNETES_CONFIGMAP, HELM_VALUES, TERRAFORM, GITHUB_ACTIONS, PROMETHEUS
- **ConfigSource** — MANUAL, DISCOVERED, IMPORTED
- **InfraResourceType** — EC2_INSTANCE, ECS_SERVICE, EKS_CLUSTER, LAMBDA_FUNCTION, RDS_INSTANCE, DYNAMODB_TABLE, S3_BUCKET, SQS_QUEUE, SNS_TOPIC, ELASTICACHE, CLOUDFRONT, ROUTE53, ALB, NLB, VPC, SECURITY_GROUP, IAM_ROLE, SECRETS_MANAGER, PARAMETER_STORE, CUSTOM
- **SolutionCategory** — PLATFORM, APPLICATION, INFRASTRUCTURE, DATA_PIPELINE, MONITORING, CUSTOM
- **SolutionStatus** — ACTIVE, DEPRECATED, ARCHIVED
- **SolutionMemberRole** — PRIMARY, SUPPORTING, INFRASTRUCTURE, MONITORING

### Relay Enums
- **ChannelType** — PUBLIC, PRIVATE, ANNOUNCEMENT
- **ConversationType** — DIRECT, GROUP
- **MessageType** — TEXT, SYSTEM, CODE, FILE, PLATFORM_EVENT
- **MemberRole** — OWNER, ADMIN, MEMBER
- **FileUploadStatus** — UPLOADING, COMPLETED, FAILED
- **PlatformEventType** — JOB_STARTED, JOB_COMPLETED, JOB_FAILED, FINDING_CRITICAL, DEPLOYMENT_STARTED, DEPLOYMENT_COMPLETED, DEPLOYMENT_FAILED, SERVICE_HEALTH_CHANGED, MEMBER_JOINED, MEMBER_LEFT
- **PresenceStatus** — ONLINE, AWAY, DO_NOT_DISTURB, OFFLINE
- **ReactionType** — EMOJI, CUSTOM

---

## 8. Repository Layer

All repositories extend `JpaRepository<Entity, UUID>` unless noted. Listed with custom methods only.

### Core Repositories
- **UserRepository** — `findByEmail(String)`, `existsByEmail(String)`, `findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(...)` (search)
- **TeamRepository** — `findByName(String)`, `existsByName(String)`
- **TeamMemberRepository** — `findByTeamIdAndUserId(UUID, UUID)`, `findByTeamId(UUID)`, `findByUserId(UUID)`, `existsByTeamIdAndUserId(UUID, UUID)`, `findByTeamIdAndRole(UUID, TeamRole)`, `deleteByTeamIdAndUserId(UUID, UUID)`
- **ProjectRepository** — `findByTeamIdAndName(UUID, String)`, `findByTeamId(UUID)`, `findByTeamIdAndIsArchived(UUID, boolean, Pageable)`, `countByTeamId(UUID)`
- **QaJobRepository** — `findByProjectId(UUID, Pageable)`, `findByProjectIdAndStatus(UUID, JobStatus, Pageable)`, `findByStartedById(UUID, Pageable)`, `countByProjectId(UUID)`, `countByProjectIdAndResult(UUID, JobResult)`
- **AgentRunRepository** — `findByJobId(UUID)`, `findByJobIdAndAgentType(UUID, AgentType)`
- **FindingRepository** — `findByJobId(UUID, Pageable)`, `findByJobIdAndSeverity(UUID, Severity, Pageable)`, `findByJobIdAndAgentType(UUID, AgentType, Pageable)`, `findByJobIdAndStatus(UUID, FindingStatus, Pageable)`, `countByJobIdAndSeverity(UUID, Severity)`, `countByJobId(UUID)`
- **BugInvestigationRepository** — `findByJobId(UUID)`, `findByJiraKey(String)`
- **ComplianceItemRepository** — `findByJobId(UUID, Pageable)`, `findByJobIdAndStatus(UUID, ComplianceStatus, Pageable)`, `countByJobIdAndStatus(UUID, ComplianceStatus)`, `countByJobId(UUID)`, `deleteByJobId(UUID)`
- **SpecificationRepository** — `findByJobId(UUID, Pageable)`, `deleteByJobId(UUID)`
- **RemediationTaskRepository** — `findByJobId(UUID, Pageable)`, `findByAssignedToId(UUID, Pageable)`, `findByJobIdAndTaskNumber(UUID, int)`, `countByJobId(UUID)`, `deleteByJobId(UUID)`
- **TechDebtItemRepository** — `findByProjectId(UUID, Pageable)`, `findByProjectIdAndStatus(UUID, DebtStatus, Pageable)`, `findByProjectIdAndCategory(UUID, DebtCategory, Pageable)`, `countByProjectIdAndStatus(UUID, DebtStatus)`, `countByProjectId(UUID)`, `deleteByProjectId(UUID)`
- **DependencyScanRepository** — `findByProjectId(UUID, Pageable)`, `findTopByProjectIdOrderByCreatedAtDesc(UUID)`, `deleteByProjectId(UUID)`
- **DependencyVulnerabilityRepository** — `findByScanId(UUID, Pageable)`, `findByScanIdAndSeverity(UUID, Severity, Pageable)`, `findByScanIdAndStatus(UUID, VulnerabilityStatus, Pageable)`, `countByScanIdAndSeverity(UUID, Severity)`, `countByScanIdAndStatus(UUID, VulnerabilityStatus)`, `deleteByScanId(UUID)`
- **HealthScheduleRepository** — `findByProjectId(UUID)`, `findByIsActiveTrue()`
- **HealthSnapshotRepository** — `findByProjectId(UUID, Pageable)`, `findTopByProjectIdOrderByCreatedAtDesc(UUID)`, `findTop10ByProjectIdOrderByCreatedAtDesc(UUID)`, `deleteByProjectId(UUID)`
- **PersonaRepository** — `findByTeamId(UUID, Pageable)`, `findByTeamIdAndAgentType(UUID, AgentType)`, `findByTeamIdAndAgentTypeAndIsDefaultTrue(UUID, AgentType)`, `findByCreatedById(UUID)`, `findByScopeAndIsDefaultTrue(Scope)`, `findByScope(Scope)`
- **DirectiveRepository** — `findByTeamId(UUID)`, `findByTeamIdAndCategory(UUID, DirectiveCategory)`, `findByTeamIdAndScope(UUID, DirectiveScope)`
- **ProjectDirectiveRepository** — `findByProjectId(UUID)`, `findByProjectIdAndDirective_Id(UUID, UUID)`, `findByProjectIdAndIsEnabledTrue(UUID)`, `deleteByProjectIdAndDirectiveId(UUID, UUID)`
- **InvitationRepository** — `findByToken(String)`, `findByTeamId(UUID)`, `findByTeamIdAndEmail(UUID, String)`, `findByTeamIdAndStatus(UUID, InvitationStatus)`
- **GitHubConnectionRepository** — `findByTeamId(UUID)`, `findByTeamIdAndIsActiveTrue(UUID)`
- **JiraConnectionRepository** — `findByTeamId(UUID)`, `findByTeamIdAndIsActiveTrue(UUID)`
- **MfaEmailCodeRepository** — `findByUserIdAndCodeAndUsedFalse(UUID, String)`, `deleteByExpiresAtBeforeOrUsedTrue(Instant)`
- **NotificationPreferenceRepository** — `findByUserId(UUID)`, `findByUserIdAndEventTypeAndChannel(UUID, String, String)`
- **SystemSettingRepository** — extends `JpaRepository<SystemSetting, String>`
- **AuditLogRepository** — extends `JpaRepository<AuditLog, Long>`. `findByTeamId(UUID, Pageable)`, `findByUserId(UUID, Pageable)`

### Courier Repositories
- **CollectionRepository** — `findByTeamIdAndCreatedBy(UUID, UUID)`, `findByTeamIdAndIsSharedTrue(UUID)`, `findByTeamId(UUID)`, `existsByTeamIdAndName(UUID, String)`
- **CollectionShareRepository** — `findByCollectionId(UUID)`, `findByCollectionIdAndSharedWithUserId(UUID, UUID)`, `findBySharedWithUserId(UUID)`, `deleteByCollectionIdAndSharedWithUserId(UUID, UUID)`, `existsByCollectionIdAndSharedWithUserId(UUID, UUID)`
- **EnvironmentRepository** — `findByTeamId(UUID)`, `findByTeamIdAndIsActiveTrue(UUID)`, `existsByTeamIdAndName(UUID, String)`
- **EnvironmentVariableRepository** — `findByEnvironmentId(UUID)`, `findByCollectionId(UUID)`, `deleteByEnvironmentId(UUID)`, `deleteByCollectionId(UUID)`
- **FolderRepository** — `findByCollectionId(UUID)`, `findByCollectionIdAndParentFolderIdIsNull(UUID)`, `findByParentFolderId(UUID)`
- **ForkRepository** — `findByForkedCollectionId(UUID)`, `findBySourceCollectionId(UUID)`
- **GlobalVariableRepository** — `findByTeamId(UUID)`, `findByTeamIdAndVariableKey(UUID, String)`, `deleteByTeamIdAndVariableKey(UUID, String)`
- **MergeRequestRepository** — `findBySourceCollectionId(UUID)`, `findByTargetCollectionId(UUID)`, `findByStatus(RunStatus)`
- **RequestRepository** — `findByFolderId(UUID)`, `findByFolderIdOrderBySortOrderAsc(UUID)`
- **RequestAuthRepository** — `findByRequestId(UUID)`, `deleteByRequestId(UUID)`
- **RequestBodyRepository** — `findByRequestId(UUID)`, `deleteByRequestId(UUID)`
- **RequestHeaderRepository** — `findByRequestId(UUID)`, `deleteByRequestId(UUID)`
- **RequestHistoryRepository** — `findByRequestId(UUID, Pageable)`, `findByRequestIdAndEnvironmentId(UUID, UUID, Pageable)`
- **RequestParamRepository** — `findByRequestId(UUID)`, `deleteByRequestId(UUID)`
- **RequestScriptRepository** — `findByRequestId(UUID)`, `findByRequestIdAndScriptType(UUID, ScriptType)`, `deleteByRequestId(UUID)`
- **RunIterationRepository** — `findByRunResultId(UUID)`
- **RunResultRepository** — `findByCollectionId(UUID, Pageable)`, `findByCollectionIdAndStatus(UUID, RunStatus, Pageable)`
- **CodeSnippetTemplateRepository** — `findByLanguage(CodeLanguage)`, `findAllByOrderBySortOrderAsc()`

### Fleet Repositories
- **ServiceProfileRepository** — `findByTeamId(UUID)`, `findByTeamIdAndName(UUID, String)`, `existsByTeamIdAndName(UUID, String)`
- **SolutionProfileRepository** — `findByTeamId(UUID)`, `existsByTeamIdAndName(UUID, String)`
- **SolutionServiceRepository** — `findBySolutionProfileId(UUID)`, `findBySolutionProfileIdAndServiceProfileId(UUID, UUID)`, `existsBySolutionProfileIdAndServiceProfileId(UUID, UUID)`, `deleteBySolutionProfileIdAndServiceProfileId(UUID, UUID)`
- **WorkstationProfileRepository** (fleet) — `findByTeamId(UUID)`, `findByTeamIdAndIsDefaultTrue(UUID)`, `existsByTeamIdAndName(UUID, String)`
- **WorkstationSolutionRepository** — `findByWorkstationProfileId(UUID)`, `findByWorkstationProfileIdAndSolutionProfileId(UUID, UUID)`, `existsByWorkstationProfileIdAndSolutionProfileId(UUID, UUID)`, `deleteByWorkstationProfileIdAndSolutionProfileId(UUID, UUID)`
- **ContainerInstanceRepository** — `findByTeamId(UUID)`, `findByContainerId(String)`, `findByServiceProfileId(UUID)`, `findByTeamIdAndStatus(UUID, ContainerStatus)`
- **ContainerHealthCheckRepository** — `findByServiceProfileId(UUID)`
- **ContainerLogRepository** — `findByContainerId(String, Pageable)`, `findByContainerIdAndTimestampBetween(String, Instant, Instant, Pageable)`
- **DeploymentRecordRepository** — `findByTeamId(UUID, Pageable)`, `findBySolutionProfileId(UUID, Pageable)`
- **DeploymentContainerRepository** — `findByDeploymentRecordId(UUID)`
- **EnvironmentVariableRepository** (fleet) — `findByServiceProfileId(UUID)`, `deleteByServiceProfileId(UUID)`
- **NetworkConfigRepository** — `findByServiceProfileId(UUID)`, `deleteByServiceProfileId(UUID)`
- **PortMappingRepository** — `findByServiceProfileId(UUID)`, `deleteByServiceProfileId(UUID)`
- **VolumeMountRepository** — `findByServiceProfileId(UUID)`, `deleteByServiceProfileId(UUID)`

### Logger Repositories
- **LogSourceRepository** — `findByTeamId(UUID)`, `findByTeamIdAndName(UUID, String)`, `findByTeamIdAndIsActiveTrue(UUID)`, `findByApiKey(String)`
- **LogEntryRepository** — `findByTeamId(UUID, Pageable)`, `findByTeamIdAndSourceId(UUID, UUID, Pageable)`, `findByTeamIdAndLevel(UUID, LogLevel, Pageable)`, `findByCorrelationId(String)`, `@Query deleteByTeamIdAndTimestampBefore(UUID, Instant)` (JPQL bulk delete), `@Query countByTeamIdGroupByLevel(UUID)` (aggregate)
- **DashboardRepository** — `findByTeamId(UUID)`, `findByTeamIdAndIsDefaultTrue(UUID)`
- **DashboardWidgetRepository** — `findByDashboardId(UUID)`, `findByDashboardIdOrderByPositionAsc(UUID)`, `deleteByDashboardId(UUID)`
- **SavedQueryRepository** — `findByTeamId(UUID)`, `findByCreatedBy(UUID)`, `findByTeamIdAndIsSharedTrue(UUID)`
- **QueryHistoryRepository** — `findByTeamId(UUID, Pageable)`, `findByCreatedBy(UUID, Pageable)`
- **MetricRepository** — `findByTeamId(UUID)`, `findByTeamIdAndName(UUID, String)`, `findByTeamIdAndServiceName(UUID, String)`
- **MetricSeriesRepository** — `findByMetricId(UUID, Pageable)`, `findByMetricIdAndTimestampBetween(UUID, Instant, Instant)`, `@Query avg/max/min aggregations by metric and time range`
- **AlertRuleRepository** — `findByTeamId(UUID)`, `findByTeamIdAndIsEnabledTrue(UUID)`, `findByMetricName(String)`
- **AlertChannelRepository** — `findByTeamId(UUID)`, `findByTeamIdAndIsEnabledTrue(UUID)`
- **AlertHistoryRepository** — `findByAlertRuleId(UUID, Pageable)`, `findByAlertRuleIdAndStatus(UUID, AlertStatus, Pageable)`
- **AnomalyBaselineRepository** — `findByMetricId(UUID)`
- **LogTrapRepository** — `findByTeamId(UUID)`, `findByTeamIdAndIsEnabledTrue(UUID)`
- **TrapConditionRepository** — `findByTrapId(UUID)`, `deleteByTrapId(UUID)`
- **RetentionPolicyRepository** — `findByTeamId(UUID)`, `findByIsActiveTrue()`
- **TraceSpanRepository** — `findByTraceId(String)`, `findByCorrelationId(String)`, `findByTeamIdAndServiceName(UUID, String, Pageable)`, `findByTeamId(UUID, Pageable)`

### Registry Repositories
- **ServiceRegistrationRepository** — `findByTeamId(UUID)`, `findByTeamIdAndSlug(UUID, String)`, `existsByTeamIdAndSlug(UUID, String)`, `findByTeamIdAndStatus(UUID, ServiceStatus)`
- **ServiceDependencyRepository** — `findBySourceServiceId(UUID)`, `findByTargetServiceId(UUID)`, `existsBySourceServiceIdAndTargetServiceId(UUID, UUID)`, `deleteBySourceServiceIdAndTargetServiceId(UUID, UUID)`
- **PortAllocationRepository** — `findByServiceId(UUID)`, `findByServiceIdAndEnvironment(UUID, String)`, `@Query findConflicting(portNumber, environment, excludeServiceId)` (conflict detection), `@Query findByTeamIdAndEnvironment(teamId, environment)` (team-wide port map)
- **PortRangeRepository** — `findByTeamId(UUID)`, `findByTeamIdAndPortTypeAndEnvironment(UUID, PortType, String)`
- **ApiRouteRegistrationRepository** — `findByServiceId(UUID)`, `findByServiceIdAndEnvironment(UUID, String)`, `@Query findConflictingRoutes(routePrefix, environment, excludeServiceId)`
- **ConfigTemplateRepository** — `findByServiceId(UUID)`, `findByServiceIdAndTemplateTypeAndEnvironment(UUID, ConfigTemplateType, String)`
- **EnvironmentConfigRepository** — `findByServiceId(UUID)`, `findByServiceIdAndEnvironment(UUID, String)`, `findByServiceIdAndEnvironmentAndConfigKey(UUID, String, String)`, `deleteByServiceIdAndEnvironmentAndConfigKey(UUID, String, String)`
- **InfraResourceRepository** — `findByTeamId(UUID)`, `findByServiceId(UUID)`, `findByTeamIdAndResourceType(UUID, InfraResourceType)`, `findByTeamIdAndEnvironment(UUID, String)`
- **SolutionRepository** — `findByTeamId(UUID)`, `findByTeamIdAndSlug(UUID, String)`, `existsByTeamIdAndSlug(UUID, String)`
- **SolutionMemberRepository** — `findBySolutionId(UUID)`, `findBySolutionIdAndServiceId(UUID, UUID)`, `existsBySolutionIdAndServiceId(UUID, UUID)`, `deleteBySolutionIdAndServiceId(UUID, UUID)`
- **WorkstationProfileRepository** (registry) — `findByTeamId(UUID)`, `findByTeamIdAndIsDefaultTrue(UUID)`, `existsByTeamIdAndName(UUID, String)`

### Relay Repositories
- **ChannelRepository** — `findByTeamId(UUID)`, `findByTeamIdAndSlug(UUID, String)`, `findByTeamIdAndChannelType(UUID, ChannelType)`, `findByTeamIdAndIsArchivedFalse(UUID)`, `existsByTeamIdAndSlug(UUID, String)`
- **ChannelMemberRepository** — `findByChannelId(UUID)`, `findByChannelIdAndUserId(UUID, UUID)`, `findByUserId(UUID)`, `existsByChannelIdAndUserId(UUID, UUID)`, `deleteByChannelIdAndUserId(UUID, UUID)`, `countByChannelId(UUID)`
- **MessageRepository** — `findByChannelId(UUID, Pageable)`, `findByChannelIdAndParentIdIsNull(UUID, Pageable)`, `findByParentId(UUID)`, `@Query searchByChannelIdAndContent(channelId, query, pageable)` (LIKE search), `@Query searchAcrossChannels(channelIds, query, pageable)`
- **MessageThreadRepository** — `findByRootMessageId(UUID)`, `findByChannelId(UUID)`, `findByChannelIdAndLastReplyAtAfter(UUID, Instant)`
- **DirectConversationRepository** — `findByTeamId(UUID)`, `@Query findByParticipantIds(teamId, participantIds)`, `findByTeamIdAndParticipantIdsContaining(UUID, String)`
- **DirectMessageRepository** — `findByConversationId(UUID, Pageable)`, `findByConversationIdAndIsDeletedFalse(UUID, Pageable)`
- **ReactionRepository** — `findByMessageId(UUID)`, `findByDirectMessageId(UUID)`, `findByMessageIdAndUserId(UUID, UUID)`, `findByDirectMessageIdAndUserId(UUID, UUID)`, `@Query countByMessageIdGroupByEmoji(UUID)` (aggregation), `deleteByMessageId(UUID)`
- **PinnedMessageRepository** — `findByChannelId(UUID)`, `findByChannelIdAndMessageId(UUID, UUID)`, `deleteByChannelIdAndMessageId(UUID, UUID)`, `countByChannelId(UUID)`
- **PlatformEventRepository** — `findByTeamId(UUID, Pageable)`, `findByTeamIdAndEventType(UUID, PlatformEventType, Pageable)`, `findBySourceEntityId(UUID)`, `findByTeamIdAndIsDeliveredFalse(UUID)`
- **FileAttachmentRepository** — `findByMessageId(UUID)`, `findByDirectMessageId(UUID)`, `findByTeamId(UUID, Pageable)`, `findByUploadedBy(UUID, Pageable)`
- **ReadReceiptRepository** — `findByChannelIdAndUserId(UUID, UUID)`, `findByChannelId(UUID)`
- **UserPresenceRepository** — `findByUserIdAndTeamId(UUID, UUID)`, `findByTeamId(UUID)`, `findByTeamIdAndStatus(UUID, PresenceStatus)`, `findByTeamIdAndLastHeartbeatAtBefore(UUID, Instant)`

---

## 9. Service Layer — Full Method Signatures

### Core Services

#### AuthService
**Injects:** UserRepository, PasswordEncoder, JwtTokenProvider, TeamMemberRepository, MfaEmailCodeRepository, EmailService
- `register(RegisterRequest): AuthResponse`
- `login(LoginRequest): AuthResponse`
- `refreshToken(RefreshTokenRequest): AuthResponse`
- `changePassword(ChangePasswordRequest): void`

#### UserService
**Injects:** UserRepository
- `getUserById(UUID): UserResponse`
- `getUserByEmail(String): UserResponse`
- `getCurrentUser(): UserResponse`
- `updateUser(UUID, UpdateUserRequest): UserResponse`
- `searchUsers(String): List<UserResponse>`
- `deactivateUser(UUID): void`
- `activateUser(UUID): void`

#### TeamService
**Injects:** TeamRepository, TeamMemberRepository, UserRepository, InvitationRepository
- `createTeam(CreateTeamRequest): TeamResponse`
- `getTeam(UUID): TeamResponse`
- `getTeamsForUser(): List<TeamResponse>`
- `updateTeam(UUID, UpdateTeamRequest): TeamResponse`
- `deleteTeam(UUID): void`
- `getTeamMembers(UUID): List<TeamMemberResponse>`
- `updateMemberRole(UUID, UUID, UpdateMemberRoleRequest): TeamMemberResponse`
- `removeMember(UUID, UUID): void`
- `inviteMember(UUID, InviteMemberRequest): InvitationResponse`
- `acceptInvitation(String): TeamResponse`
- `getTeamInvitations(UUID): List<InvitationResponse>`
- `cancelInvitation(UUID): void`

#### ProjectService
**Injects:** ProjectRepository, TeamMemberRepository, UserRepository, TeamRepository, GitHubConnectionRepository, JiraConnectionRepository, ObjectMapper, + 10 related repositories for cascade delete
- `createProject(UUID, CreateProjectRequest): ProjectResponse`
- `getProject(UUID): ProjectResponse`
- `getProjectsForTeam(UUID): List<ProjectResponse>`
- `getAllProjectsForTeam(UUID, boolean, Pageable): PageResponse<ProjectResponse>`
- `updateProject(UUID, UpdateProjectRequest): ProjectResponse`
- `archiveProject(UUID): void`
- `unarchiveProject(UUID): void`
- `deleteProject(UUID): void` — cascade deletes all related entities

#### QaJobService
**Injects:** QaJobRepository, AgentRunRepository, FindingRepository, ProjectRepository, UserRepository, TeamMemberRepository, ProjectService
- `createJob(CreateJobRequest): JobResponse`
- `getJob(UUID): JobResponse`
- `getJobsForProject(UUID, Pageable): PageResponse<JobSummaryResponse>`
- `getJobsByUser(UUID, Pageable): PageResponse<JobSummaryResponse>`
- `updateJob(UUID, UpdateJobRequest): JobResponse`
- `deleteJob(UUID): void`

#### AgentRunService
**Injects:** AgentRunRepository, QaJobRepository, TeamMemberRepository
- `createAgentRun(CreateAgentRunRequest): AgentRunResponse`
- `createAgentRuns(UUID, List<AgentType>): List<AgentRunResponse>`
- `getAgentRuns(UUID): List<AgentRunResponse>`
- `getAgentRun(UUID): AgentRunResponse`
- `updateAgentRun(UUID, UpdateAgentRunRequest): AgentRunResponse`

#### FindingService
**Injects:** FindingRepository, QaJobRepository, UserRepository, TeamMemberRepository
- `createFinding(CreateFindingRequest): FindingResponse`
- `createFindings(List<CreateFindingRequest>): List<FindingResponse>`
- `getFinding(UUID): FindingResponse`
- `getFindingsForJob(UUID, Pageable): PageResponse<FindingResponse>`
- `getFindingsByJobAndSeverity(UUID, Severity, Pageable): PageResponse<FindingResponse>`
- `getFindingsByJobAndAgent(UUID, AgentType, Pageable): PageResponse<FindingResponse>`
- `getFindingsByJobAndStatus(UUID, FindingStatus, Pageable): PageResponse<FindingResponse>`
- `updateFindingStatus(UUID, UpdateFindingStatusRequest): FindingResponse`
- `bulkUpdateFindingStatus(BulkUpdateFindingsRequest): List<FindingResponse>`
- `countFindingsBySeverity(UUID): Map<Severity, Long>`

#### ComplianceService
**Injects:** ComplianceItemRepository, SpecificationRepository, QaJobRepository, TeamMemberRepository
- `createSpecification(CreateSpecificationRequest): SpecificationResponse`
- `getSpecificationsForJob(UUID, Pageable): PageResponse<SpecificationResponse>`
- `createComplianceItem(CreateComplianceItemRequest): ComplianceItemResponse`
- `createComplianceItems(List<CreateComplianceItemRequest>): List<ComplianceItemResponse>`
- `getComplianceItemsForJob(UUID, Pageable): PageResponse<ComplianceItemResponse>`
- `getComplianceItemsByStatus(UUID, ComplianceStatus, Pageable): PageResponse<ComplianceItemResponse>`
- `getComplianceSummary(UUID): Map<String, Object>`

#### BugInvestigationService
**Injects:** BugInvestigationRepository, QaJobRepository, TeamMemberRepository, S3StorageService
- `createInvestigation(CreateBugInvestigationRequest): BugInvestigationResponse`
- `getInvestigation(UUID): BugInvestigationResponse`
- `getInvestigationByJob(UUID): BugInvestigationResponse`
- `getInvestigationByJiraKey(String): BugInvestigationResponse`
- `updateInvestigation(UUID, UpdateBugInvestigationRequest): BugInvestigationResponse`
- `uploadRca(UUID, String): String`

#### RemediationTaskService
**Injects:** RemediationTaskRepository, QaJobRepository, UserRepository, TeamMemberRepository, FindingRepository, S3StorageService
- `createTask(CreateTaskRequest): TaskResponse`
- `createTasks(List<CreateTaskRequest>): List<TaskResponse>`
- `getTasksForJob(UUID, Pageable): PageResponse<TaskResponse>`
- `getTask(UUID): TaskResponse`
- `getTasksAssignedToUser(UUID, Pageable): PageResponse<TaskResponse>`
- `updateTask(UUID, UpdateTaskRequest): TaskResponse`
- `uploadTaskPrompt(UUID, int, String): String`

#### TechDebtService
**Injects:** TechDebtItemRepository, ProjectRepository, TeamMemberRepository, QaJobRepository
- `createTechDebtItem(CreateTechDebtItemRequest): TechDebtItemResponse`
- `createTechDebtItems(List<CreateTechDebtItemRequest>): List<TechDebtItemResponse>`
- `getTechDebtItem(UUID): TechDebtItemResponse`
- `getTechDebtForProject(UUID, Pageable): PageResponse<TechDebtItemResponse>`
- `getTechDebtByStatus(UUID, DebtStatus, Pageable): PageResponse<TechDebtItemResponse>`
- `getTechDebtByCategory(UUID, DebtCategory, Pageable): PageResponse<TechDebtItemResponse>`
- `updateTechDebtStatus(UUID, UpdateTechDebtStatusRequest): TechDebtItemResponse`
- `deleteTechDebtItem(UUID): void`
- `getDebtSummary(UUID): Map<String, Object>`

#### DependencyService
**Injects:** DependencyScanRepository, DependencyVulnerabilityRepository, ProjectRepository, TeamMemberRepository, QaJobRepository
- `createScan(CreateDependencyScanRequest): DependencyScanResponse`
- `getScan(UUID): DependencyScanResponse`
- `getScansForProject(UUID, Pageable): PageResponse<DependencyScanResponse>`
- `getLatestScan(UUID): DependencyScanResponse`
- `addVulnerability(CreateVulnerabilityRequest): VulnerabilityResponse`
- `addVulnerabilities(List<CreateVulnerabilityRequest>): List<VulnerabilityResponse>`
- `getVulnerabilities(UUID, Pageable): PageResponse<VulnerabilityResponse>`
- `getVulnerabilitiesBySeverity(UUID, Severity, Pageable): PageResponse<VulnerabilityResponse>`
- `getOpenVulnerabilities(UUID, Pageable): PageResponse<VulnerabilityResponse>`
- `updateVulnerabilityStatus(UUID, VulnerabilityStatus): VulnerabilityResponse`

#### HealthMonitorService
**Injects:** HealthScheduleRepository, HealthSnapshotRepository, ProjectRepository, TeamMemberRepository, UserRepository, QaJobRepository, ObjectMapper
- `createSchedule(CreateHealthScheduleRequest): HealthScheduleResponse`
- `getSchedulesForProject(UUID): List<HealthScheduleResponse>`
- `getActiveSchedules(): List<HealthScheduleResponse>`
- `updateSchedule(UUID, boolean): HealthScheduleResponse`
- `deleteSchedule(UUID): void`
- `markScheduleRun(UUID): void`
- `createSnapshot(CreateHealthSnapshotRequest): HealthSnapshotResponse`
- `getSnapshots(UUID, Pageable): PageResponse<HealthSnapshotResponse>`
- `getLatestSnapshot(UUID): HealthSnapshotResponse`
- `getHealthTrend(UUID, int): List<HealthSnapshotResponse>`

#### MetricsService
**Injects:** ProjectRepository, QaJobRepository, FindingRepository, TechDebtItemRepository, DependencyVulnerabilityRepository, DependencyScanRepository, HealthSnapshotRepository, TeamMemberRepository
- `getProjectMetrics(UUID): ProjectMetricsResponse`
- `getTeamMetrics(UUID): TeamMetricsResponse`
- `getHealthTrend(UUID, int): List<HealthSnapshotResponse>`

#### DirectiveService
**Injects:** DirectiveRepository, ProjectDirectiveRepository, ProjectRepository, TeamMemberRepository, UserRepository, TeamRepository
- `createDirective(CreateDirectiveRequest): DirectiveResponse`
- `getDirective(UUID): DirectiveResponse`
- `getDirectivesForTeam(UUID): List<DirectiveResponse>`
- `getDirectivesForProject(UUID): List<DirectiveResponse>`
- `getDirectivesByCategory(UUID, DirectiveScope): List<DirectiveResponse>`
- `updateDirective(UUID, UpdateDirectiveRequest): DirectiveResponse`
- `deleteDirective(UUID): void`
- `assignToProject(AssignDirectiveRequest): ProjectDirectiveResponse`
- `removeFromProject(UUID, UUID): void`
- `getProjectDirectives(UUID): List<ProjectDirectiveResponse>`
- `getEnabledDirectivesForProject(UUID): List<DirectiveResponse>`
- `toggleProjectDirective(UUID, UUID, boolean): ProjectDirectiveResponse`

#### PersonaService
**Injects:** PersonaRepository, TeamMemberRepository, UserRepository, TeamRepository
- `createPersona(CreatePersonaRequest): PersonaResponse`
- `getPersona(UUID): PersonaResponse`
- `getPersonasForTeam(UUID, Pageable): PageResponse<PersonaResponse>`
- `getPersonasByAgentType(UUID, AgentType): List<PersonaResponse>`
- `getDefaultPersona(UUID, AgentType): PersonaResponse`
- `getPersonasByUser(UUID): List<PersonaResponse>`
- `getSystemPersonas(): List<PersonaResponse>`
- `updatePersona(UUID, UpdatePersonaRequest): PersonaResponse`
- `deletePersona(UUID): void`
- `setAsDefault(UUID): PersonaResponse`
- `removeDefault(UUID): PersonaResponse`

#### MfaService
**Injects:** UserRepository, PasswordEncoder, JwtTokenProvider, EncryptionService, TeamMemberRepository, ObjectMapper, MfaEmailCodeRepository, EmailService
- `setupMfa(MfaSetupRequest): MfaSetupResponse`
- `verifyAndEnableMfa(MfaVerifyRequest): MfaStatusResponse`
- `setupEmailMfa(MfaEmailSetupRequest): MfaRecoveryResponse`
- `verifyEmailSetupAndEnable(MfaVerifyRequest): MfaStatusResponse`
- `verifyMfaLogin(MfaLoginRequest): AuthResponse`
- `sendLoginMfaCode(MfaResendRequest): void`
- `disableMfa(MfaSetupRequest): MfaStatusResponse`
- `regenerateRecoveryCodes(MfaSetupRequest): MfaRecoveryResponse`
- `getMfaStatus(): MfaStatusResponse`
- `adminResetMfa(UUID): void`
- `cleanupExpiredCodes(): void` — @Scheduled(fixedRate = 900000)
- `maskEmail(String): String`

#### AdminService
**Injects:** UserRepository, TeamRepository, ProjectRepository, QaJobRepository, SystemSettingRepository
- `getAllUsers(Pageable): Page<UserResponse>`
- `getUserById(UUID): UserResponse`
- `updateUserStatus(UUID, AdminUpdateUserRequest): UserResponse`
- `getSystemSetting(String): SystemSettingResponse`
- `updateSystemSetting(UpdateSystemSettingRequest): SystemSettingResponse`
- `getAllSettings(): List<SystemSettingResponse>`
- `getUsageStats(): Map<String, Object>`

#### AuditLogService
**Injects:** AuditLogRepository, UserRepository, TeamRepository, TeamMemberRepository
- `log(UUID, UUID, String, String, UUID, String): void` — @Async
- `getTeamAuditLog(UUID, Pageable): Page<AuditLogResponse>`
- `getUserAuditLog(UUID, Pageable): Page<AuditLogResponse>`

#### NotificationService
**Injects:** NotificationPreferenceRepository, UserRepository
- `getPreferences(UUID): List<NotificationPreferenceResponse>`
- `updatePreference(UUID, UpdateNotificationPreferenceRequest): NotificationPreferenceResponse`
- `updatePreferences(UUID, List<UpdateNotificationPreferenceRequest>): List<NotificationPreferenceResponse>`
- `shouldNotify(UUID, String, String): boolean`

#### GitHubConnectionService
**Injects:** GitHubConnectionRepository, TeamMemberRepository, EncryptionService, TeamRepository, UserRepository
- `createConnection(UUID, CreateGitHubConnectionRequest): GitHubConnectionResponse`
- `getConnections(UUID): List<GitHubConnectionResponse>`
- `getConnection(UUID): GitHubConnectionResponse`
- `deleteConnection(UUID): void`
- `getDecryptedCredentials(UUID): String` — internal use only, never exposed via API

#### JiraConnectionService
**Injects:** JiraConnectionRepository, TeamMemberRepository, EncryptionService, TeamRepository, UserRepository
- `createConnection(UUID, CreateJiraConnectionRequest): JiraConnectionResponse`
- `getConnections(UUID): List<JiraConnectionResponse>`
- `getConnection(UUID): JiraConnectionResponse`
- `deleteConnection(UUID): void`
- `getDecryptedApiToken(UUID): String` — internal use only
- `getConnectionDetails(UUID): JiraConnectionDetails` (record)

#### ReportStorageService
**Injects:** S3StorageService, AgentRunRepository
- `uploadReport(UUID, AgentType, String): String`
- `uploadSummaryReport(UUID, String): String`
- `downloadReport(String): String`
- `deleteReportsForJob(UUID): void`
- `uploadSpecification(UUID, String, byte[], String): String`
- `downloadSpecification(String): byte[]`

#### EncryptionService
**Injects:** SecretKey (derived from `codeops.encryption.key`)
- `encrypt(String): String` — AES-256-GCM
- `decrypt(String): String`

#### S3StorageService (dual-mode)
**Injects:** S3Client (nullable), bucket name, local storage path
- `upload(String, byte[], String): String`
- `download(String): byte[]`
- `delete(String): void`
- `generatePresignedUrl(String, Duration): String`

#### TokenBlacklistService
**Injects:** ConcurrentHashMap (in-memory)
- `blacklist(String, Instant): void`
- `isBlacklisted(String): boolean`

### Courier Services
- **CollectionService** — CRUD + search. `createCollection`, `getCollection`, `getCollections`, `getSharedCollections`, `getAccessibleCollections`, `updateCollection`, `deleteCollection`.
- **FolderService** — CRUD + reorder + tree. `createFolder`, `getFolder`, `getFolders`, `getFolderTree`, `updateFolder`, `deleteFolder`, `reorderFolder`.
- **RequestService** — CRUD + reorder + duplicate. `createRequest`, `getRequest`, `getRequests`, `updateRequest`, `deleteRequest`, `reorderRequests`, `duplicateRequest`, `saveHeaders`, `saveParams`, `saveBody`, `saveAuth`, `saveScript`.
- **EnvironmentService** — CRUD + clone. `createEnvironment`, `getEnvironment`, `getEnvironments`, `updateEnvironment`, `deleteEnvironment`, `cloneEnvironment`, `saveVariables`.
- **VariableService** — Global variables. `getGlobalVariables`, `saveGlobalVariable`, `batchSaveGlobalVariables`, `deleteGlobalVariable`.
- **HistoryService** — `getHistory`, `getHistoryDetail`, `deleteHistory`.
- **ShareService** — `shareCollection`, `getShares`, `updateSharePermission`, `revokeShare`.
- **RequestProxyService** — `sendRequest(SendRequestProxyRequest)` — HTTP proxy that executes requests on behalf of user.
- **CollectionRunnerService** — `startRun`, `getRun`, `getRuns` — runs all requests in a collection.
- **ScriptEngineService** — GraalVM Polyglot JavaScript execution for pre-request/post-response scripts.
- **ImportService** — `importCollection` — Postman, cURL, OpenAPI import.
- **ExportService** — `exportCollection` — exports to JSON.
- **ForkService** — `createFork`, `getForks`.
- **MergeService** — `createMergeRequest`, `resolveMergeRequest`.
- **GraphQLService** — `execute`, `introspect` — GraphQL query execution.
- **CodeGenerationService** — `generateCode`, `getTemplates` — generates code snippets from requests.
- **AuthResolverService** — Resolves auth config (inheritance from collection/folder).

### Fleet Services
- **DockerEngineService** — Docker Engine API wrapper via RestTemplate. Container lifecycle (`listContainers`, `startContainer`, `stopContainer`, `removeContainer`, `getContainerLogs`, `getContainerStats`, `execInContainer`), images (`listImages`, `pullImage`, `removeImage`), networks (`listNetworks`, `createNetwork`, `removeNetwork`, `connectContainerToNetwork`, `disconnectContainerFromNetwork`), volumes (`listVolumes`, `createVolume`, `removeVolume`, `pruneVolumes`).
- **ContainerManagementService** — Orchestrates container operations with database persistence. `deployService`, `stopContainer`, `getContainerStatus`, `getContainerLogs`, `getTeamContainers`.
- **ServiceProfileService** — CRUD for service profiles. `createProfile`, `getProfile`, `getProfiles`, `updateProfile`, `deleteProfile`.
- **SolutionProfileService** — CRUD for solution profiles + service membership. `createSolution`, `getSolution`, `getSolutions`, `updateSolution`, `deleteSolution`, `addService`, `removeService`, `updateServiceOrder`.
- **WorkstationProfileService** — CRUD for workstation profiles + solution membership. `createWorkstation`, `getWorkstation`, `getWorkstations`, `updateWorkstation`, `deleteWorkstation`, `addSolution`, `removeSolution`, `setDefault`.
- **FleetHealthService** — `getFleetHealthSummary(UUID teamId)` — aggregates container, image, and Docker daemon health.

### Logger Services
- **LogIngestionService** — `ingestLog`, `ingestBatch` — persists log entries, publishes Spring events.
- **LogQueryService** — `queryLogs`, `getLogEntry`, `getLogsByCorrelation`.
- **LogQueryDslParser** — Parses custom DSL (`level:ERROR AND service:auth`) into JPA predicates.
- **LogParsingService** — Extracts structured fields from raw log messages.
- **LogSourceService** — CRUD for log sources.
- **DashboardService** — CRUD for dashboards + widgets. `createDashboard`, `getDashboard`, `getDashboards`, `updateDashboard`, `deleteDashboard`, `addWidget`, `updateWidget`, `removeWidget`, `reorderWidgets`, `createFromTemplate`.
- **MetricsService** (logger) — CRUD for metrics. `registerMetric`, `getMetric`, `getMetrics`, `updateMetric`, `deleteMetric`, `pushData`.
- **MetricAggregationService** — `getTimeSeries`, `getAggregation` — time-bucketed aggregations (avg, max, min, sum).
- **AlertService** — CRUD for alert rules. `createRule`, `getRule`, `getRules`, `updateRule`, `deleteRule`, `evaluateAlerts`.
- **AlertChannelService** — CRUD for notification channels. `createChannel`, `getChannel`, `getChannels`, `updateChannel`, `deleteChannel`, `testChannel`.
- **AnomalyDetectionService** — `createBaseline`, `getBaseline`, `checkForAnomalies`, `getAnomalyReport`. @Scheduled daily at 3 AM.
- **AnomalyBaselineCalculator** — Statistical calculations (mean, stddev, z-scores).
- **LogTrapService** — CRUD for log traps + conditions. `createTrap`, `getTrap`, `getTraps`, `updateTrap`, `deleteTrap`, `testTrap`.
- **TrapEvaluationEngine** — Evaluates trap conditions against log entries (pattern match, threshold, rate).
- **RetentionService** — CRUD for retention policies.
- **RetentionExecutor** — @Scheduled daily at 2 AM. Executes active retention policies (delete/archive old logs).
- **TraceService** — `createSpan`, `getTrace`, `getTraces`, `getSpansByService`.
- **TraceAnalysisService** — `getTraceFlow`, `getWaterfall`, `getRootCauseAnalysis` — trace visualization and analysis.
- **KafkaLogConsumer** — `@KafkaListener(topics = "codeops-logs")` — consumes log entries from Kafka.
- **LogEntryEventListener** — Spring event listener for `LogEntryIngestedEvent` — evaluates traps on new log entries.

### Registry Services
- **ServiceRegistryService** — CRUD for service registrations. `createService`, `getService`, `getServices`, `updateService`, `deleteService`, `updateStatus`, `cloneService`.
- **DependencyGraphService** — `addDependency`, `removeDependency`, `getDependencies`, `getDependants`, `getDependencyGraph`, `getImpactAnalysis`.
- **PortAllocationService** — `allocatePort`, `autoAllocatePort`, `deallocatePort`, `getPortAllocations`, `getPortMap`, `checkPortAvailability`, `getPortConflicts`, `getPortRanges`, `updatePortRange`.
- **ApiRouteService** — `createRoute`, `getRoutes`, `deleteRoute`, `checkRouteConflicts`.
- **ConfigEngineService** — `getConfigs`, `upsertConfig`, `deleteConfig`, `generateConfig` (auto-generates Docker Compose, Dockerfile, Nginx, K8s manifests from service metadata), `getTemplates`.
- **HealthCheckService** — `checkServiceHealth`, `checkAllServices`, `getTeamHealthSummary`.
- **InfraResourceService** — CRUD for infrastructure resources. `createResource`, `getResource`, `getResources`, `updateResource`, `deleteResource`.
- **SolutionService** — CRUD for solutions + member management. `createSolution`, `getSolution`, `getSolutions`, `updateSolution`, `deleteSolution`, `addMember`, `removeMember`, `updateMember`, `getSolutionHealth`.
- **TopologyService** — `getTopology` (layered dependency graph), `getTopologyStats`.
- **WorkstationProfileService** (registry) — CRUD for workstation profiles. `createProfile`, `getProfile`, `getProfiles`, `updateProfile`, `deleteProfile`, `setDefault`.

### Relay Services
- **ChannelService** — CRUD for channels + member management. `createChannel`, `getChannel`, `getChannels`, `updateChannel`, `archiveChannel`, `deleteChannel`, `inviteMember`, `removeMember`, `updateMemberRole`, `getMembers`, `updateTopic`, `getSummaries`.
- **MessageService** — `sendMessage`, `getMessage`, `getChannelMessages`, `getThreadReplies`, `editMessage`, `deleteMessage`, `searchMessages`, `searchMessagesAcrossChannels`, `markRead`, `getUnreadCounts`.
- **DirectMessageService** — `createConversation`, `getConversation`, `getConversations`, `sendMessage`, `getMessages`, `editMessage`, `deleteMessage`.
- **ReactionService** — `toggleReaction`, `getReactionsForMessage`, `getReactionsForMessageWithUser`, `getReactionsByUser`, `removeAllReactionsForMessage`.
- **ThreadService** — `onReply` (updates thread metadata), `getThreadInfo`, `getThread`, `getActiveThreads`.
- **FileAttachmentService** — `uploadFile`, `getAttachment`, `getAttachments`, `deleteAttachment`, `completeUpload`.
- **PlatformEventService** — `publishEvent`, `publishEventSimple`, `getEvent`, `getEventsForTeam`, `getEventsForTeamByType`, `getEventsForEntity`, `getUndeliveredEvents`, `retryDelivery`, `retryAllUndelivered`.
- **PresenceService** — `updatePresence`, `heartbeat`, `getPresence`, `getTeamPresence`, `getOnlineUsers`, `setDoNotDisturb`, `clearDoNotDisturb`, `goOffline`, `cleanupStalePresences`, `getPresenceCount`.

### Relay WebSocket
- **RelayWebSocketController** — STOMP controller. `@MessageMapping("/channel.message")` → broadcasts to `/topic/channel.{channelId}`. Also handles typing indicators and DM messages.
- **RelayWebSocketService** — `broadcastToChannel(UUID, Object)`, `broadcastTypingIndicator(UUID, UUID)`, `broadcastToConversation(UUID, Object)`, `broadcastPresenceUpdate(UUID, Object)`.
- **WebSocketConfig** — STOMP over SockJS. Endpoint: `/ws`. App destination prefix: `/app`. Topic broker: `/topic`, `/queue`.
- **WebSocketAuthInterceptor** — Extracts JWT from STOMP CONNECT frame, authenticates user.
- **WebSocketSessionRegistry** — Tracks active WebSocket sessions by user ID.

### Notification Services
- **EmailService** — Sends emails via SMTP (prod) or logs to console (dev). `sendEmail(to, subject, body)`.
- **NotificationDispatcher** — Routes notifications to appropriate channels (email, Teams webhook).
- **TeamsWebhookService** — Sends messages to Microsoft Teams via incoming webhook.

---

## 10. Controller / API Layer

All controllers use `@RequiredArgsConstructor` for DI. Auth annotations noted. Base paths from `AppConstants`.

### Core Controllers (prefix: `/api/v1/`)
- **AuthController** (`/auth`) — `register()→authService.register`, `login()→authService.login`, `refreshToken()→authService.refreshToken`, `changePassword()→authService.changePassword`. MFA endpoints: `setupMfa`, `verifyMfa`, `setupEmailMfa`, `verifyEmailMfa`, `mfaLogin`, `resendMfaCode`, `disableMfa`, `regenerateRecoveryCodes`, `mfaStatus`, `adminResetMfa`. Public: register, login, refreshToken, mfaLogin, resendMfaCode.
- **UserController** (`/users`) — @PreAuthorize("isAuthenticated()"). `getCurrentUser`, `getUserById`, `updateUser`, `searchUsers`.
- **TeamController** (`/teams`) — @PreAuthorize("isAuthenticated()"). CRUD + members + invitations.
- **ProjectController** (`/teams/{teamId}/projects`) — @PreAuthorize("isAuthenticated()"). CRUD + archive/unarchive.
- **JobController** (`/jobs`) — @PreAuthorize("isAuthenticated()"). CRUD for QA jobs.
- **FindingController** (`/findings`) — @PreAuthorize("isAuthenticated()"). CRUD + bulk update + severity counts.
- **ComplianceController** (`/compliance`) — @PreAuthorize("isAuthenticated()"). Specifications + compliance items + summary.
- **TaskController** (`/tasks`) — @PreAuthorize("isAuthenticated()"). Remediation tasks.
- **TechDebtController** (`/tech-debt`) — @PreAuthorize("isAuthenticated()"). Tech debt items + summary.
- **DependencyController** (`/dependencies`) — @PreAuthorize("isAuthenticated()"). Scans + vulnerabilities.
- **HealthMonitorController** (`/health-monitor`) — @PreAuthorize("isAuthenticated()"). Schedules + snapshots + trends.
- **MetricsController** (`/metrics`) — @PreAuthorize("isAuthenticated()"). Project + team metrics.
- **DirectiveController** (`/directives`) — @PreAuthorize("isAuthenticated()"). CRUD + project assignment.
- **PersonaController** (`/personas`) — @PreAuthorize("isAuthenticated()"). CRUD + system personas.
- **IntegrationController** (`/integrations`) — @PreAuthorize("isAuthenticated()"). GitHub + Jira connections.
- **ReportController** (`/reports`) — @PreAuthorize("isAuthenticated()"). Upload/download reports.
- **AdminController** (`/admin`) — @PreAuthorize("hasRole('OWNER')"). User management + system settings + usage stats.
- **HealthController** (`/health`) — Public. Health check endpoint.

### Courier Controllers (prefix: `/api/v1/courier/`)
- **CollectionController** — @PreAuthorize("isAuthenticated()"). CRUD + accessible collections.
- **FolderController** — @PreAuthorize("isAuthenticated()"). CRUD + tree + reorder.
- **RequestController** — @PreAuthorize("isAuthenticated()"). CRUD + save headers/params/body/auth/scripts + duplicate + reorder.
- **EnvironmentController** — @PreAuthorize("isAuthenticated()"). CRUD + clone + save variables.
- **VariableController** — @PreAuthorize("isAuthenticated()"). Global variables CRUD.
- **HistoryController** — @PreAuthorize("isAuthenticated()"). Request history.
- **ShareController** — @PreAuthorize("isAuthenticated()"). Collection sharing.
- **ProxyController** — @PreAuthorize("isAuthenticated()"). HTTP request proxy.
- **RunnerController** — @PreAuthorize("isAuthenticated()"). Collection runner.
- **ImportController** — @PreAuthorize("isAuthenticated()"). Import (Postman, cURL, OpenAPI).
- **GraphQLController** — @PreAuthorize("isAuthenticated()"). GraphQL execution + introspection.
- **CodeGenerationController** — @PreAuthorize("isAuthenticated()"). Code snippet generation.
- **HealthController** (courier) — Public. Module health check.

### Fleet Controllers (prefix: `/api/v1/fleet/`)
- **ContainerController** — @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')"). Container lifecycle management.
- **ImageController** — @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')"). Docker image management.
- **NetworkController** — @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')"). Docker network management.
- **VolumeController** — @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')"). Docker volume management.
- **ServiceProfileController** — @PreAuthorize("isAuthenticated()"). Service profile CRUD.
- **SolutionProfileController** — @PreAuthorize("isAuthenticated()"). Solution profile CRUD.
- **WorkstationProfileController** — @PreAuthorize("isAuthenticated()"). Workstation profile CRUD.
- **FleetHealthController** — @PreAuthorize("isAuthenticated()"). Fleet health summary.

### Logger Controllers (prefix: `/api/v1/logger/`)
- **LogIngestionController** — @PreAuthorize("isAuthenticated()"). Log ingestion (single + batch).
- **LogQueryController** — @PreAuthorize("isAuthenticated()"). Log querying (structured + DSL).
- **LogSourceController** — @PreAuthorize("isAuthenticated()"). Log source CRUD.
- **DashboardController** — @PreAuthorize("isAuthenticated()"). Dashboard + widget CRUD + templates.
- **MetricsController** (logger) — @PreAuthorize("isAuthenticated()"). Metric registration + data push + aggregation.
- **AlertController** — @PreAuthorize("isAuthenticated()"). Alert rules + channels + history.
- **AnomalyController** — @PreAuthorize("isAuthenticated()"). Anomaly baselines + detection + reports.
- **LogTrapController** — @PreAuthorize("isAuthenticated()"). Log trap CRUD + testing.
- **RetentionController** — @PreAuthorize("isAuthenticated()"). Retention policy CRUD.
- **TraceController** — @PreAuthorize("isAuthenticated()"). Trace spans + flow + waterfall + RCA.

### Registry Controllers (prefix: `/api/v1/registry/`)
- **RegistryController** — @PreAuthorize("isAuthenticated()"). Service registration CRUD + status + clone.
- **DependencyController** (registry) — @PreAuthorize("isAuthenticated()"). Dependencies + graph + impact analysis.
- **PortController** — @PreAuthorize("isAuthenticated()"). Port allocation + ranges + conflicts + port map.
- **RouteController** — @PreAuthorize("isAuthenticated()"). API route registration + conflict check.
- **ConfigController** — @PreAuthorize("isAuthenticated()"). Environment configs + template generation.
- **InfraController** — @PreAuthorize("isAuthenticated()"). Infrastructure resource CRUD.
- **SolutionController** — @PreAuthorize("isAuthenticated()"). Solution CRUD + member management + health.
- **TopologyController** — @PreAuthorize("isAuthenticated()"). Topology graph + stats.
- **HealthManagementController** — @PreAuthorize("isAuthenticated()"). Service health checks.
- **WorkstationController** (registry) — @PreAuthorize("isAuthenticated()"). Workstation profile CRUD.

### Relay Controllers (prefix: `/api/v1/relay/`)
- **ChannelController** — @PreAuthorize("isAuthenticated()"). Channel CRUD + members + archive.
- **MessageController** — @PreAuthorize("isAuthenticated()"). Messages + threads + search + read receipts.
- **DirectMessageController** — @PreAuthorize("isAuthenticated()"). DM conversations + messages.
- **ReactionController** — @PreAuthorize("isAuthenticated()"). Reaction toggle + summaries.
- **FileController** — @PreAuthorize("isAuthenticated()"). File upload/download.
- **PlatformEventController** — @PreAuthorize("isAuthenticated()"). Platform events + retry.
- **PresenceController** — @PreAuthorize("isAuthenticated()"). User presence + heartbeat + DND.
- **RelayHealthController** — Public. Module health check.

---

## 11. Security Configuration

```
Authentication: JWT (HS256) — stateless
Token issuer/validator: Internal (JwtTokenProvider)
Password encoder: BCrypt (strength 12)

Public endpoints (no auth required):
  - /api/v1/auth/register, /api/v1/auth/login, /api/v1/auth/refresh-token
  - /api/v1/auth/mfa/login, /api/v1/auth/mfa/resend
  - /api/v1/health, /api/v1/courier/health, /api/v1/fleet/health, /api/v1/relay/health
  - /swagger-ui/**, /v3/api-docs/**
  - /ws/** (WebSocket)

Protected endpoints:
  - /api/v1/admin/** → hasRole('OWNER')
  - /api/v1/fleet/containers/**, /api/v1/fleet/images/**, /api/v1/fleet/networks/**, /api/v1/fleet/volumes/** → hasRole('ADMIN') or hasRole('OWNER')
  - All other /api/** → isAuthenticated()

CORS: Configurable via codeops.cors.allowed-origins (env var)

CSRF: Disabled (stateless JWT API)

Security headers: CSP, X-Frame-Options DENY, HSTS, X-Content-Type-Options nosniff

Rate limiting: 10 requests per 60 seconds on /api/v1/auth/** endpoints (RateLimitFilter)

Session: SessionCreationPolicy.STATELESS
```

---

## 12. Custom Security Components

### JwtAuthFilter
- Extends `OncePerRequestFilter`
- Extracts token from `Authorization: Bearer {token}` header
- Validates via `JwtTokenProvider.validateToken()`
- Checks `TokenBlacklistService.isBlacklisted()`
- Sets `UsernamePasswordAuthenticationToken` in SecurityContext with UUID principal
- Skips filter for public endpoints

### JwtTokenProvider
- 3 token types: access (24h), refresh (30d), MFA (5min)
- HS256 signing with configurable secret (`codeops.jwt.secret`)
- Claims: `sub` (userId), `jti` (token ID), `type` (access/refresh/mfa), `roles` (team roles)
- Methods: `generateAccessToken(UUID, List<TeamRole>)`, `generateRefreshToken(UUID)`, `generateMfaToken(UUID)`, `validateToken(String)`, `getUserIdFromToken(String)`, `getTokenType(String)`

### SecurityUtils
- Static utility: `getCurrentUserId()` → extracts UUID from SecurityContext
- Throws `AuthorizationException` if not authenticated

### RateLimitFilter
- In-memory bucket per IP address (ConcurrentHashMap)
- 10 requests per 60 seconds on auth endpoints
- Returns 429 Too Many Requests when exceeded

### RequestCorrelationFilter
- Generates/propagates `X-Correlation-Id` header
- Adds to MDC for structured logging

### TokenBlacklistService
- In-memory blacklist (ConcurrentHashMap)
- Used on logout to invalidate tokens before expiry

---

## 13. Exception Handling & Error Responses

### GlobalExceptionHandler (@ControllerAdvice)
```
Exception Mappings:
  - NotFoundException → 404
  - ValidationException → 400
  - AuthorizationException → 403
  - AccessDeniedException → 403
  - MethodArgumentNotValidException → 400 (field validation errors)
  - ConstraintViolationException → 400
  - HttpMessageNotReadableException → 400 (malformed JSON, invalid enum, bad timestamp)
  - HttpRequestMethodNotSupportedException → 405
  - NoResourceFoundException → 404
  - DataIntegrityViolationException → 409 (duplicate key, FK violation)
  - MaxUploadSizeExceededException → 413
  - Exception (catch-all) → 500 (masks internal details)

Standard error response:
  ErrorResponse(int status, String error, String message, Instant timestamp, String path)
```

### Exception Hierarchy
```
CodeOpsException (RuntimeException)
├── NotFoundException → 404
├── ValidationException → 400
└── AuthorizationException → 403
```

---

## 14. Mappers / DTOs

All mappers use MapStruct: `@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))`. Boolean `is*` fields require explicit `@Mapping(target = "isXxx", source = "xxx")` due to Lombok/JavaBeans naming mismatch.

### Courier Mappers (13)
CollectionMapper, EnvironmentMapper, EnvironmentVariableMapper, FolderMapper, GlobalVariableMapper, RequestAuthMapper, RequestBodyMapper, RequestHeaderMapper, RequestHistoryMapper, RequestMapper, RequestParamMapper, RequestScriptMapper, RunResultMapper

### Fleet Mappers (10)
ContainerHealthCheckMapper, ContainerInstanceMapper, ContainerLogMapper, NetworkConfigMapper, ServiceProfileMapper, SolutionProfileMapper, SolutionServiceMapper, VolumeMountMapper, WorkstationProfileMapper, WorkstationSolutionMapper

### Logger Mappers (13)
AlertChannelMapper, AlertHistoryMapper, AlertRuleMapper, AnomalyBaselineMapper, DashboardMapper, LogEntryMapper, LogSourceMapper, LogTrapMapper, MetricMapper, QueryHistoryMapper, RetentionPolicyMapper, SavedQueryMapper, TraceSpanMapper

### Relay Mappers (11)
ChannelMapper, ChannelMemberMapper, DirectConversationMapper, DirectMessageMapper, FileAttachmentMapper, MessageMapper, MessageThreadMapper, PinnedMessageMapper, PlatformEventMapper, ReactionMapper, UserPresenceMapper

---

## 15. Utility Classes & Shared Components

### AppConstants
Centralized constants file organized by domain:
- API prefixes: `API_PREFIX = "/api/v1"`, `COURIER_API_PREFIX`, `FLEET_API_PREFIX`, `LOGGER_API_PREFIX`, `REGISTRY_API_PREFIX`, `RELAY_API_PREFIX`
- Pagination defaults: `DEFAULT_PAGE_SIZE = 20`, `MAX_PAGE_SIZE = 100`
- Validation limits, error messages, S3 key prefixes, Kafka topics

### EncryptionService
AES-256-GCM encryption for sensitive credentials (GitHub PATs, Jira API tokens). Key derived from `codeops.encryption.key` env var via SHA-256.

### S3StorageService (dual-mode)
Prod: AWS S3 client. Dev: local filesystem at `~/.codeops/storage/`. Transparent switching via `codeops.s3.enabled` flag.

### JacksonConfig
Custom `ObjectMapper` with `LenientInstantDeserializer` that accepts ISO-8601 timestamps with or without timezone suffix (`Z`). Falls back to `LocalDateTime.parse().toInstant(ZoneOffset.UTC)` for bare timestamps.

### JwtProperties
`@ConfigurationProperties("codeops.jwt")`: secret, accessTokenExpiration, refreshTokenExpiration.

### SlugUtils (registry)
`generateSlug(String name)` — lowercase, replace non-alphanumeric with hyphens, trim, max 63 chars.

### PageResponse\<T\>
Generic record for paginated responses: `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.

---

## 16. Database Schema (Live)

Hibernate `ddl-auto: update` manages schema. All tables in `public` schema. 84+ tables across 6 modules.

**Core tables:** users, teams, team_members, projects, qa_jobs, agent_runs, findings, bug_investigations, compliance_items, specifications, remediation_tasks, tech_debt_items, dependency_scans, dependency_vulnerabilities, health_schedules, health_snapshots, personas, directives, project_directives, invitations, github_connections, jira_connections, mfa_email_codes, notification_preferences, system_settings, audit_logs

**Courier tables:** collections, collection_shares, environments, environment_variables, folders, forks, global_variables, merge_requests, requests, request_auths, request_bodies, request_headers, request_histories, request_params, request_scripts, run_iterations, run_results, code_snippet_templates

**Fleet tables:** service_profiles, solution_profiles, solution_services, fleet_workstation_profiles, fleet_workstation_solutions, container_instances, container_health_checks, container_logs, deployment_records, deployment_containers, fleet_environment_variables, network_configs, port_mappings, volume_mounts

**Logger tables:** log_sources, log_entries, dashboards, dashboard_widgets, saved_queries, query_history, metrics, metric_series, alert_rules, alert_channels, alert_histories, anomaly_baselines, log_traps, trap_conditions, retention_policies, trace_spans

**Registry tables:** service_registrations, service_dependencies, port_allocations, port_ranges, api_route_registrations, config_templates, environment_configs, infra_resources, solutions, solution_members, workstation_profiles

**Relay tables:** channels, channel_members, messages, message_threads, direct_conversations, direct_messages, reactions, pinned_messages, file_attachments, platform_events, read_receipts, user_presences

---

## 17. Message Broker Configuration

```
Broker: Apache Kafka
Connection: localhost:9094 (configurable via spring.kafka.bootstrap-servers)
Consumer Group: codeops-server

Topics:
  - codeops-logs
    Consumer: KafkaLogConsumer.consumeLog()
    Message type: JSON (log entry payload)
    Ack mode: AUTO (default)

Publishers: None (server is consumer-only)
```

Kafka is used exclusively by the Logger module to receive log entries from external services. The `KafkaConsumerConfig` configures `StringDeserializer` for keys and `JsonDeserializer` for values.

---

## 18. Cache Layer

No Redis or caching layer detected in application code. Redis is configured in `docker-compose.yml` but not referenced by any Spring configuration or service class. `TokenBlacklistService` uses an in-memory `ConcurrentHashMap` for token blacklisting.

---

## 19. Environment Variable Inventory

| Variable | Used In | Default | Required in Prod |
|---|---|---|---|
| DATABASE_URL | application-prod.yml | localhost:5432/codeops | YES |
| DATABASE_USERNAME | application-prod.yml | codeops | YES |
| DATABASE_PASSWORD | application-prod.yml | codeops | YES |
| JWT_SECRET | application.yml | (dev default in application-dev.yml) | YES |
| ENCRYPTION_KEY | application.yml | (dev default in application-dev.yml) | YES |
| CORS_ALLOWED_ORIGINS | application.yml | http://localhost:3000 | YES |
| AWS_REGION | application.yml | us-east-1 | NO (S3 disabled in dev) |
| MAIL_FROM_EMAIL | application.yml | noreply@codeops.dev | NO (email logged in dev) |

---

## 20. Service Dependency Map

Standalone service — no inter-service REST dependencies. All modules are co-located in a single Spring Boot application.

**External integrations (credential-based, client-initiated):**
- GitHub API: Stored PATs used by CodeOps Client to proxy GitHub requests
- Jira API: Stored API tokens used by CodeOps Client to proxy Jira requests

**Downstream consumers:**
- CodeOps-Client (Flutter desktop app) — consumes all REST endpoints
- CodeOps-Analytics (port 8081) — reads from shared PostgreSQL database (`analytics` schema)
- External services → Kafka topic `codeops-logs` → Logger module

---

## 21. Known Technical Debt & Issues

### TODO/FIXME Scan Results

| Issue | Location | Severity | Notes |
|---|---|---|---|
| TODO: Changing key derivation invalidates existing encrypted data | EncryptionService.java:56 | CRITICAL | Requires re-encryption migration strategy before key rotation |
| TODO: Add junixsocket dependency for Unix socket Docker communication | fleet/config/DockerConfig.java:17 | Medium | Currently uses TCP connection to Docker daemon |

**Total: 2 TODOs found in production code.** The EncryptionService TODO is the most critical as it affects data migration strategy.

### Other Issues Discovered During Audit

| Issue | Location | Severity | Notes |
|---|---|---|---|
| Redis configured but unused | docker-compose.yml | Low | Redis container runs but application doesn't use it — wasted resources |
| No CI/CD pipeline | project root | Medium | No GitHub Actions, Jenkinsfile, or other CI/CD configuration detected |
| In-memory token blacklist | TokenBlacklistService.java | Medium | Blacklisted tokens lost on restart; won't work in multi-instance deployment |
| 4 System.out.println calls | various | Low | Should use SLF4J logger instead |
