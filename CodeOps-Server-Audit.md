# CodeOps-Server Codebase Audit

| Field | Value |
|-------|-------|
| **Project** | CodeOps Server |
| **Repository** | https://github.com/AI-CodeOps/CodeOps-Server.git |
| **Branch** | main |
| **Commit** | f27f566acf8b53d9ee7a15081fea752f7ee3484a |
| **Audit Date** | 2026-02-24 |
| **Tech Stack** | Spring Boot 3.3.0 / Java 21 / PostgreSQL 16 / Redis 7 / Kafka (Confluent 7.5.0) |
| **Port** | 8090 |
| **API Prefix** | `/api/v1/` |

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Module Inventory](#2-module-inventory)
3. [Core Module -- Entities](#3-core-module----entities)
4. [Core Module -- Enums](#4-core-module----enums)
5. [Core Module -- Repositories](#5-core-module----repositories)
6. [Core Module -- Services](#6-core-module----services)
7. [Core Module -- Controllers](#7-core-module----controllers)
8. [Core Module -- Security](#8-core-module----security)
9. [Core Module -- Config](#9-core-module----config)
10. [Core Module -- Exceptions](#10-core-module----exceptions)
11. [Core Module -- Notifications](#11-core-module----notifications)
12. [Courier Module -- Entities](#12-courier-module----entities)
13. [Courier Module -- Enums](#13-courier-module----enums)
14. [Courier Module -- Repositories](#14-courier-module----repositories)
15. [Courier Module -- Services](#15-courier-module----services)
16. [Courier Module -- Controllers](#16-courier-module----controllers)
17. [Courier Module -- DTOs](#17-courier-module----dtos)
18. [Courier Module -- Mappers](#18-courier-module----mappers)
19. [Logger Module -- Entities](#19-logger-module----entities)
20. [Logger Module -- Enums](#20-logger-module----enums)
21. [Logger Module -- Repositories](#21-logger-module----repositories)
22. [Logger Module -- Services](#22-logger-module----services)
23. [Logger Module -- Controllers](#23-logger-module----controllers)
24. [Logger Module -- DTOs](#24-logger-module----dtos)
25. [Logger Module -- Mappers](#25-logger-module----mappers)
26. [Logger Module -- Events](#26-logger-module----events)
27. [Registry Module -- Entities](#27-registry-module----entities)
28. [Registry Module -- Enums](#28-registry-module----enums)
29. [Registry Module -- Repositories](#29-registry-module----repositories)
30. [Registry Module -- Services](#30-registry-module----services)
31. [Registry Module -- Controllers](#31-registry-module----controllers)
32. [Registry Module -- DTOs](#32-registry-module----dtos)
33. [Registry Module -- Util](#33-registry-module----util)
34. [Infrastructure](#34-infrastructure)
35. [Database Schema](#35-database-schema)
36. [Cross-Cutting Patterns](#36-cross-cutting-patterns)
37. [Test Inventory](#37-test-inventory)
38. [Endpoint Summary](#38-endpoint-summary)
39. [File Count Summary](#39-file-count-summary)

---

## 1. Architecture Overview

CodeOps Server is a Spring Boot 3.3.0 monolith composed of four logical modules sharing a single database:

- **Core** -- Authentication, teams, projects, QA jobs, findings, compliance, tech debt, dependency scanning, health monitoring, personas, directives, integrations (GitHub/Jira), admin, reports, metrics
- **Courier** -- API testing client (collections, requests, folders, environments, variables, shares, forks, merge requests, history, proxy, GraphQL, code generation, collection runner, import/export)
- **Logger** -- Log management, monitoring, alerting, distributed tracing, metrics, anomaly detection, dashboards, retention policies
- **Registry** -- Service registry, solution grouping, port allocation, dependency graph, API route registration, config engine, infrastructure resources, topology visualization, workstation profiles

**Key Architectural Patterns:**
- BaseEntity pattern: UUID PK, createdAt, updatedAt with `@PrePersist`/`@PreUpdate`
- All `@ManyToOne` relationships use `FetchType.LAZY`
- All enum fields use `@Enumerated(EnumType.STRING)`
- Optimistic locking (`@Version`) on QaJob, Finding, TechDebtItem, RemediationTask, AgentRun
- JWT authentication (HS256), BCrypt password hashing (strength 12)
- MFA support (TOTP and Email)
- AES-256-GCM encryption for credentials
- Team-scoped authorization (verifyTeamMembership, verifyTeamAdmin)
- Role-based access: OWNER, ADMIN, MEMBER, VIEWER
- MapStruct for DTO mapping with Lombok boolean field mapping pattern
- Async operations via `@Async` with ThreadPool (core=5, max=20, queue=100)
- Rate limiting on auth endpoints (10 req/60s per IP)
- S3 storage with local filesystem fallback in dev
- Kafka consumer for Logger log ingestion
- Spring ApplicationEvent for trap evaluation pipeline

---

## 2. Module Inventory

| Module | Package | Entities | Enums | Repos | Services | Controllers | Endpoints |
|--------|---------|----------|-------|-------|----------|-------------|-----------|
| Core | `com.codeops` | 28 | 25 | 26 | 26 | 18 | 151 |
| Courier | `com.codeops.courier` | 18 | 7 | 18 | 22 | 14 | ~87 |
| Logger | `com.codeops.logger` | 16 | 10 | 16 | 19 | 11 | ~95 |
| Registry | `com.codeops.registry` | 11 | 11 | 11 | 10 | 10 | 64 |
| **Total** | | **73** | **53** | **71** | **77** | **53** | **~397** |

---

## 3. Core Module -- Entities

### 3.1 BaseEntity (MappedSuperclass)
**File:** `src/main/java/com/codeops/entity/BaseEntity.java`
**Type:** `@MappedSuperclass` (abstract, not a table)

| Field | Type | Annotations |
|-------|------|-------------|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = GenerationType.UUID)` |
| `createdAt` | `Instant` | `@Column(name = "created_at", nullable = false, updatable = false)` |
| `updatedAt` | `Instant` | `@Column(name = "updated_at")` |

**Auditing:** `@PrePersist` -> `onCreate()` sets both; `@PreUpdate` -> `onUpdate()` sets `updatedAt`

---

### 3.2 User
**File:** `src/main/java/com/codeops/entity/User.java`
**Table:** `users` | **Extends:** BaseEntity

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `email` | String | `email` | NOT NULL, UNIQUE, length=255 |
| `passwordHash` | String | `password_hash` | NOT NULL, length=255 |
| `displayName` | String | `display_name` | NOT NULL, length=100 |
| `avatarUrl` | String | `avatar_url` | length=500 |
| `isActive` | Boolean | `is_active` | NOT NULL, default=true |
| `lastLoginAt` | Instant | `last_login_at` | nullable |
| `mfaEnabled` | Boolean | `mfa_enabled` | NOT NULL, default=false |
| `mfaMethod` | MfaMethod | `mfa_method` | NOT NULL, default=NONE, `@Enumerated(STRING)` |
| `mfaSecret` | String | `mfa_secret` | length=500 |
| `mfaRecoveryCodes` | String | `mfa_recovery_codes` | length=2000 |

---

### 3.3 Team
**File:** `src/main/java/com/codeops/entity/Team.java`
**Table:** `teams` | **Extends:** BaseEntity

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `name` | String | `name` | NOT NULL, length=100 |
| `description` | String | `description` | TEXT |
| `teamsWebhookUrl` | String | `teams_webhook_url` | length=500 |
| `settingsJson` | String | `settings_json` | TEXT |
| `owner` | User | `owner_id` | `@ManyToOne(LAZY)`, NOT NULL |

---

### 3.4 TeamMember
**File:** `src/main/java/com/codeops/entity/TeamMember.java`
**Table:** `team_members` | **Extends:** BaseEntity
**Unique:** `(team_id, user_id)` | **Indexes:** `idx_tm_team_id`, `idx_tm_user_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `team` | Team | `team_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `user` | User | `user_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `role` | TeamRole | `role` | NOT NULL, `@Enumerated(STRING)` |
| `joinedAt` | Instant | `joined_at` | NOT NULL |

---

### 3.5 Project
**File:** `src/main/java/com/codeops/entity/Project.java`
**Table:** `projects` | **Extends:** BaseEntity | **Index:** `idx_project_team_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `name` | String | `name` | NOT NULL, length=200 |
| `description` | String | `description` | TEXT |
| `repoUrl` | String | `repo_url` | length=500 |
| `repoFullName` | String | `repo_full_name` | length=200 |
| `defaultBranch` | String | `default_branch` | default='main' |
| `jiraProjectKey` | String | `jira_project_key` | length=20 |
| `jiraDefaultIssueType` | String | `jira_default_issue_type` | default='Task' |
| `jiraLabels` | String | `jira_labels` | TEXT |
| `jiraComponent` | String | `jira_component` | length=100 |
| `techStack` | String | `tech_stack` | length=200 |
| `healthScore` | Integer | `health_score` | nullable |
| `lastAuditAt` | Instant | `last_audit_at` | nullable |
| `settingsJson` | String | `settings_json` | TEXT |
| `isArchived` | Boolean | `is_archived` | NOT NULL, default=false |
| `team` | Team | `team_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `githubConnection` | GitHubConnection | `github_connection_id` | `@ManyToOne(LAZY)`, nullable |
| `jiraConnection` | JiraConnection | `jira_connection_id` | `@ManyToOne(LAZY)`, nullable |
| `createdBy` | User | `created_by` | `@ManyToOne(LAZY)`, NOT NULL |

---

### 3.6 Invitation
**File:** `src/main/java/com/codeops/entity/Invitation.java`
**Table:** `invitations` | **Extends:** BaseEntity
**Indexes:** `idx_inv_team_id`, `idx_inv_email`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `email` | String | `email` | NOT NULL, length=255 |
| `token` | String | `token` | NOT NULL, UNIQUE, length=100 |
| `expiresAt` | Instant | `expires_at` | NOT NULL |
| `team` | Team | `team_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `invitedBy` | User | `invited_by` | `@ManyToOne(LAZY)`, NOT NULL |
| `role` | TeamRole | `role` | NOT NULL, `@Enumerated(STRING)` |
| `status` | InvitationStatus | `status` | NOT NULL, `@Enumerated(STRING)` |

---

### 3.7 Persona
**File:** `src/main/java/com/codeops/entity/Persona.java`
**Table:** `personas` | **Extends:** BaseEntity | **Index:** `idx_persona_team_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `name` | String | `name` | NOT NULL, length=100 |
| `description` | String | `description` | TEXT |
| `contentMd` | String | `content_md` | NOT NULL, TEXT |
| `isDefault` | Boolean | `is_default` | default=false |
| `version` | Integer | `version` | NOT NULL, default=1 (business version, NOT @Version) |
| `team` | Team | `team_id` | `@ManyToOne(LAZY)`, nullable |
| `createdBy` | User | `created_by` | `@ManyToOne(LAZY)`, NOT NULL |
| `agentType` | AgentType | `agent_type` | NOT NULL, `@Enumerated(STRING)` |
| `scope` | Scope | `scope` | NOT NULL, `@Enumerated(STRING)` |

---

### 3.8 Directive
**File:** `src/main/java/com/codeops/entity/Directive.java`
**Table:** `directives` | **Extends:** BaseEntity | **Index:** `idx_directive_team_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `name` | String | `name` | NOT NULL, length=200 |
| `description` | String | `description` | TEXT |
| `contentMd` | String | `content_md` | NOT NULL, TEXT |
| `version` | Integer | `version` | default=1 (business version, NOT @Version) |
| `team` | Team | `team_id` | `@ManyToOne(LAZY)`, nullable |
| `project` | Project | `project_id` | `@ManyToOne(LAZY)`, nullable |
| `createdBy` | User | `created_by` | `@ManyToOne(LAZY)`, NOT NULL |
| `category` | DirectiveCategory | `category` | `@Enumerated(STRING)`, nullable |
| `scope` | DirectiveScope | `scope` | NOT NULL, `@Enumerated(STRING)` |

---

### 3.9 ProjectDirective
**File:** `src/main/java/com/codeops/entity/ProjectDirective.java`
**Table:** `project_directives` | **Does NOT extend BaseEntity**
**PK:** `ProjectDirectiveId` (composite: projectId + directiveId)

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `id` | ProjectDirectiveId | | `@EmbeddedId` |
| `enabled` | Boolean | `enabled` | NOT NULL, default=true |
| `project` | Project | `project_id` | `@MapsId("projectId")`, `@ManyToOne(LAZY)` |
| `directive` | Directive | `directive_id` | `@MapsId("directiveId")`, `@ManyToOne(LAZY)` |

### 3.10 ProjectDirectiveId
**File:** `src/main/java/com/codeops/entity/ProjectDirectiveId.java`
**Type:** `@Embeddable`, implements `Serializable`
**Fields:** `projectId: UUID`, `directiveId: UUID`

---

### 3.11 QaJob
**File:** `src/main/java/com/codeops/entity/QaJob.java`
**Table:** `qa_jobs` | **Extends:** BaseEntity
**Indexes:** `idx_job_project_id`, `idx_job_started_by` | **Optimistic Locking:** `@Version`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `name` | String | `name` | length=200 |
| `branch` | String | `branch` | length=100 |
| `configJson` | String | `config_json` | TEXT |
| `summaryMd` | String | `summary_md` | TEXT |
| `healthScore` | Integer | `health_score` | nullable |
| `totalFindings` | Integer | `total_findings` | default=0 |
| `criticalCount` | Integer | `critical_count` | default=0 |
| `highCount` | Integer | `high_count` | default=0 |
| `mediumCount` | Integer | `medium_count` | default=0 |
| `lowCount` | Integer | `low_count` | default=0 |
| `jiraTicketKey` | String | `jira_ticket_key` | length=50 |
| `startedAt` | Instant | `started_at` | nullable |
| `completedAt` | Instant | `completed_at` | nullable |
| `version` | Long | `version` | `@Version` |
| `project` | Project | `project_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `startedBy` | User | `started_by` | `@ManyToOne(LAZY)`, NOT NULL |
| `mode` | JobMode | `mode` | NOT NULL, `@Enumerated(STRING)` |
| `status` | JobStatus | `status` | NOT NULL, `@Enumerated(STRING)` |
| `overallResult` | JobResult | `overall_result` | `@Enumerated(STRING)`, nullable |

---

### 3.12 Specification
**File:** `src/main/java/com/codeops/entity/Specification.java`
**Table:** `specifications` | **Extends:** BaseEntity | **Index:** `idx_spec_job_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `name` | String | `name` | NOT NULL, length=200 |
| `s3Key` | String | `s3_key` | NOT NULL, length=500 |
| `job` | QaJob | `job_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `specType` | SpecType | `spec_type` | NOT NULL, `@Enumerated(STRING)` |

---

### 3.13 Finding
**File:** `src/main/java/com/codeops/entity/Finding.java`
**Table:** `findings` | **Extends:** BaseEntity
**Indexes:** `idx_finding_job_id`, `idx_finding_status` | **Optimistic Locking:** `@Version`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `title` | String | `title` | NOT NULL, length=500 |
| `description` | String | `description` | TEXT |
| `filePath` | String | `file_path` | length=500 |
| `lineNumber` | Integer | `line_number` | nullable |
| `recommendation` | String | `recommendation` | TEXT |
| `evidence` | String | `evidence` | TEXT |
| `statusChangedAt` | Instant | `status_changed_at` | nullable |
| `version` | Long | `version` | `@Version` |
| `job` | QaJob | `job_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `statusChangedBy` | User | `status_changed_by` | `@ManyToOne(LAZY)`, nullable |
| `agentType` | AgentType | `agent_type` | NOT NULL, `@Enumerated(STRING)` |
| `severity` | Severity | `severity` | NOT NULL, `@Enumerated(STRING)` |
| `effortEstimate` | Effort | `effort_estimate` | `@Enumerated(STRING)`, nullable |
| `debtCategory` | DebtCategory | `debt_category` | `@Enumerated(STRING)`, nullable |
| `status` | FindingStatus | `status` | default=OPEN, `@Enumerated(STRING)` |

---

### 3.14 ComplianceItem
**File:** `src/main/java/com/codeops/entity/ComplianceItem.java`
**Table:** `compliance_items` | **Extends:** BaseEntity | **Index:** `idx_compliance_job_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `requirement` | String | `requirement` | NOT NULL, TEXT |
| `evidence` | String | `evidence` | TEXT |
| `notes` | String | `notes` | TEXT |
| `job` | QaJob | `job_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `spec` | Specification | `spec_id` | `@ManyToOne(LAZY)`, nullable |
| `status` | ComplianceStatus | `status` | NOT NULL, `@Enumerated(STRING)` |
| `agentType` | AgentType | `agent_type` | `@Enumerated(STRING)`, nullable |

---

### 3.15 TechDebtItem
**File:** `src/main/java/com/codeops/entity/TechDebtItem.java`
**Table:** `tech_debt_items` | **Extends:** BaseEntity
**Index:** `idx_tech_debt_project_id` | **Optimistic Locking:** `@Version`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `title` | String | `title` | NOT NULL, length=500 |
| `description` | String | `description` | TEXT |
| `filePath` | String | `file_path` | length=500 |
| `version` | Long | `version` | `@Version` |
| `project` | Project | `project_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `firstDetectedJob` | QaJob | `first_detected_job_id` | `@ManyToOne(LAZY)`, nullable |
| `resolvedJob` | QaJob | `resolved_job_id` | `@ManyToOne(LAZY)`, nullable |
| `category` | DebtCategory | `category` | NOT NULL, `@Enumerated(STRING)` |
| `effortEstimate` | Effort | `effort_estimate` | `@Enumerated(STRING)`, nullable |
| `businessImpact` | BusinessImpact | `business_impact` | `@Enumerated(STRING)`, nullable |
| `status` | DebtStatus | `status` | default=IDENTIFIED, `@Enumerated(STRING)` |

---

### 3.16 RemediationTask
**File:** `src/main/java/com/codeops/entity/RemediationTask.java`
**Table:** `remediation_tasks` | **Extends:** BaseEntity
**Index:** `idx_task_job_id` | **Optimistic Locking:** `@Version`
**Join Table:** `remediation_task_findings` (task_id, finding_id)

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `taskNumber` | Integer | `task_number` | NOT NULL |
| `title` | String | `title` | NOT NULL, length=500 |
| `description` | String | `description` | TEXT |
| `promptMd` | String | `prompt_md` | TEXT |
| `promptS3Key` | String | `prompt_s3_key` | length=500 |
| `jiraKey` | String | `jira_key` | length=50 |
| `version` | Long | `version` | `@Version` |
| `job` | QaJob | `job_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `findings` | List\<Finding\> | join table | `@ManyToMany(LAZY)` |
| `assignedTo` | User | `assigned_to` | `@ManyToOne(LAZY)`, nullable |
| `priority` | Priority | `priority` | NOT NULL, `@Enumerated(STRING)` |
| `status` | TaskStatus | `status` | default=PENDING, `@Enumerated(STRING)` |

---

### 3.17 BugInvestigation
**File:** `src/main/java/com/codeops/entity/BugInvestigation.java`
**Table:** `bug_investigations` | **Extends:** BaseEntity

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `jiraKey` | String | `jira_key` | length=50 |
| `jiraSummary` | String | `jira_summary` | TEXT |
| `jiraDescription` | String | `jira_description` | TEXT |
| `jiraCommentsJson` | String | `jira_comments_json` | TEXT |
| `jiraAttachmentsJson` | String | `jira_attachments_json` | TEXT |
| `jiraLinkedIssues` | String | `jira_linked_issues` | TEXT |
| `additionalContext` | String | `additional_context` | TEXT |
| `rcaMd` | String | `rca_md` | TEXT |
| `impactAssessmentMd` | String | `impact_assessment_md` | TEXT |
| `rcaS3Key` | String | `rca_s3_key` | length=500 |
| `rcaPostedToJira` | Boolean | `rca_posted_to_jira` | default=false |
| `fixTasksCreatedInJira` | Boolean | `fix_tasks_created_in_jira` | default=false |
| `job` | QaJob | `job_id` | `@ManyToOne(LAZY)`, NOT NULL |

---

### 3.18 DependencyScan
**File:** `src/main/java/com/codeops/entity/DependencyScan.java`
**Table:** `dependency_scans` | **Extends:** BaseEntity | **Index:** `idx_dep_scan_project_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `manifestFile` | String | `manifest_file` | length=200 |
| `totalDependencies` | Integer | `total_dependencies` | nullable |
| `outdatedCount` | Integer | `outdated_count` | nullable |
| `vulnerableCount` | Integer | `vulnerable_count` | nullable |
| `scanDataJson` | String | `scan_data_json` | TEXT |
| `project` | Project | `project_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `job` | QaJob | `job_id` | `@ManyToOne(LAZY)`, nullable |

---

### 3.19 DependencyVulnerability
**File:** `src/main/java/com/codeops/entity/DependencyVulnerability.java`
**Table:** `dependency_vulnerabilities` | **Extends:** BaseEntity | **Index:** `idx_vuln_scan_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `dependencyName` | String | `dependency_name` | NOT NULL, length=200 |
| `currentVersion` | String | `current_version` | length=50 |
| `fixedVersion` | String | `fixed_version` | length=50 |
| `cveId` | String | `cve_id` | length=30 |
| `description` | String | `description` | TEXT |
| `scan` | DependencyScan | `scan_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `severity` | Severity | `severity` | NOT NULL, `@Enumerated(STRING)` |
| `status` | VulnerabilityStatus | `status` | default=OPEN, `@Enumerated(STRING)` |

---

### 3.20 HealthSnapshot
**File:** `src/main/java/com/codeops/entity/HealthSnapshot.java`
**Table:** `health_snapshots` | **Extends:** BaseEntity | **Index:** `idx_snapshot_project_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `healthScore` | Integer | `health_score` | NOT NULL |
| `findingsBySeverity` | String | `findings_by_severity` | TEXT |
| `techDebtScore` | Integer | `tech_debt_score` | nullable |
| `dependencyScore` | Integer | `dependency_score` | nullable |
| `testCoveragePercent` | BigDecimal | `test_coverage_percent` | precision=5, scale=2 |
| `capturedAt` | Instant | `captured_at` | NOT NULL |
| `project` | Project | `project_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `job` | QaJob | `job_id` | `@ManyToOne(LAZY)`, nullable |

---

### 3.21 HealthSchedule
**File:** `src/main/java/com/codeops/entity/HealthSchedule.java`
**Table:** `health_schedules` | **Extends:** BaseEntity | **Index:** `idx_schedule_project_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `cronExpression` | String | `cron_expression` | length=50 |
| `agentTypes` | String | `agent_types` | NOT NULL, TEXT |
| `isActive` | Boolean | `is_active` | NOT NULL, default=true |
| `lastRunAt` | Instant | `last_run_at` | nullable |
| `nextRunAt` | Instant | `next_run_at` | nullable |
| `project` | Project | `project_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `createdBy` | User | `created_by` | `@ManyToOne(LAZY)`, NOT NULL |
| `scheduleType` | ScheduleType | `schedule_type` | NOT NULL, `@Enumerated(STRING)` |

---

### 3.22 AgentRun
**File:** `src/main/java/com/codeops/entity/AgentRun.java`
**Table:** `agent_runs` | **Extends:** BaseEntity
**Index:** `idx_agent_run_job_id` | **Optimistic Locking:** `@Version`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `reportS3Key` | String | `report_s3_key` | length=500 |
| `score` | Integer | `score` | nullable |
| `findingsCount` | Integer | `findings_count` | default=0 |
| `criticalCount` | Integer | `critical_count` | default=0 |
| `highCount` | Integer | `high_count` | default=0 |
| `startedAt` | Instant | `started_at` | nullable |
| `completedAt` | Instant | `completed_at` | nullable |
| `version` | Long | `version` | `@Version` |
| `job` | QaJob | `job_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `agentType` | AgentType | `agent_type` | NOT NULL, `@Enumerated(STRING)` |
| `status` | AgentStatus | `status` | NOT NULL, `@Enumerated(STRING)` |
| `result` | AgentResult | `result` | `@Enumerated(STRING)`, nullable |

---

### 3.23 NotificationPreference
**File:** `src/main/java/com/codeops/entity/NotificationPreference.java`
**Table:** `notification_preferences` | **Extends:** BaseEntity
**Unique:** `(user_id, event_type)` | **Index:** `idx_notif_user_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `eventType` | String | `event_type` | NOT NULL, length=50 |
| `inApp` | Boolean | `in_app` | NOT NULL, default=true |
| `email` | Boolean | `email` | NOT NULL, default=false |
| `user` | User | `user_id` | `@ManyToOne(LAZY)`, NOT NULL |

---

### 3.24 SystemSetting
**File:** `src/main/java/com/codeops/entity/SystemSetting.java`
**Table:** `system_settings` | **Does NOT extend BaseEntity**
**PK:** `settingKey: String` (length=100)

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `settingKey` | String | `key` | `@Id`, length=100 |
| `value` | String | `value` | NOT NULL, TEXT |
| `updatedAt` | Instant | `updated_at` | NOT NULL |
| `updatedBy` | User | `updated_by` | `@ManyToOne(LAZY)`, nullable |

---

### 3.25 AuditLog
**File:** `src/main/java/com/codeops/entity/AuditLog.java`
**Table:** `audit_log` | **Does NOT extend BaseEntity**
**PK:** `id: Long` (IDENTITY) | **Indexes:** `idx_audit_user_id`, `idx_audit_team_id`

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `id` | Long | `id` | `@Id @GeneratedValue(IDENTITY)` |
| `action` | String | `action` | NOT NULL, length=50 |
| `entityType` | String | `entity_type` | length=30 |
| `entityId` | UUID | `entity_id` | nullable |
| `details` | String | `details` | TEXT |
| `ipAddress` | String | `ip_address` | length=45 |
| `createdAt` | Instant | `created_at` | NOT NULL |
| `user` | User | `user_id` | `@ManyToOne(LAZY)`, nullable |
| `team` | Team | `team_id` | `@ManyToOne(LAZY)`, nullable |

---

### 3.26 GitHubConnection
**File:** `src/main/java/com/codeops/entity/GitHubConnection.java`
**Table:** `github_connections` | **Extends:** BaseEntity

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `name` | String | `name` | NOT NULL, length=100 |
| `encryptedCredentials` | String | `encrypted_credentials` | NOT NULL, TEXT |
| `githubUsername` | String | `github_username` | length=100 |
| `isActive` | Boolean | `is_active` | NOT NULL, default=true |
| `team` | Team | `team_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `createdBy` | User | `created_by` | `@ManyToOne(LAZY)`, NOT NULL |
| `authType` | GitHubAuthType | `auth_type` | NOT NULL, `@Enumerated(STRING)` |

---

### 3.27 JiraConnection
**File:** `src/main/java/com/codeops/entity/JiraConnection.java`
**Table:** `jira_connections` | **Extends:** BaseEntity

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `name` | String | `name` | NOT NULL, length=100 |
| `instanceUrl` | String | `instance_url` | NOT NULL, length=500 |
| `email` | String | `email` | NOT NULL, length=255 |
| `encryptedApiToken` | String | `encrypted_api_token` | NOT NULL, TEXT |
| `isActive` | Boolean | `is_active` | NOT NULL, default=true |
| `team` | Team | `team_id` | `@ManyToOne(LAZY)`, NOT NULL |
| `createdBy` | User | `created_by` | `@ManyToOne(LAZY)`, NOT NULL |

---

### 3.28 MfaEmailCode
**File:** `src/main/java/com/codeops/entity/MfaEmailCode.java`
**Table:** `mfa_email_codes` | **Does NOT extend BaseEntity**

| Field | Type | Column | Constraints |
|-------|------|--------|-------------|
| `id` | UUID | `id` | `@Id @GeneratedValue(UUID)` |
| `userId` | UUID | `user_id` | NOT NULL (plain UUID, not a relationship) |
| `codeHash` | String | `code_hash` | NOT NULL, length=255 |
| `expiresAt` | Instant | `expires_at` | NOT NULL |
| `used` | boolean | `used` | NOT NULL, default=false |
| `createdAt` | Instant | `created_at` | NOT NULL, default=Instant.now() |

---

### Entity Relationship Summary

```
Team --[ManyToOne]--> User               (owner)
TeamMember --[ManyToOne]--> Team, User   (team_id, user_id)
Project --[ManyToOne]--> Team, User, GitHubConnection?, JiraConnection?
Invitation --[ManyToOne]--> Team, User   (team_id, invited_by)
Persona --[ManyToOne]--> Team?, User     (team_id nullable, created_by)
Directive --[ManyToOne]--> Team?, Project?, User
ProjectDirective --[ManyToOne]--> Project, Directive (composite PK)
QaJob --[ManyToOne]--> Project, User     (project_id, started_by)
Specification --[ManyToOne]--> QaJob
Finding --[ManyToOne]--> QaJob, User?    (job_id, status_changed_by)
ComplianceItem --[ManyToOne]--> QaJob, Specification?
TechDebtItem --[ManyToOne]--> Project, QaJob?, QaJob?
RemediationTask --[ManyToOne]--> QaJob, User?; --[ManyToMany]--> Finding
BugInvestigation --[ManyToOne]--> QaJob
DependencyScan --[ManyToOne]--> Project, QaJob?
DependencyVulnerability --[ManyToOne]--> DependencyScan
HealthSnapshot --[ManyToOne]--> Project, QaJob?
HealthSchedule --[ManyToOne]--> Project, User
AgentRun --[ManyToOne]--> QaJob
NotificationPreference --[ManyToOne]--> User
SystemSetting --[ManyToOne]--> User?     (updatedBy)
AuditLog --[ManyToOne]--> User?, Team?
GitHubConnection --[ManyToOne]--> Team, User
JiraConnection --[ManyToOne]--> Team, User
MfaEmailCode -- no relationships (userId is plain UUID)
```

---

## 4. Core Module -- Enums

| Enum | Values | Used By |
|------|--------|---------|
| `AgentType` | SECURITY, CODE_QUALITY, BUILD_HEALTH, COMPLETENESS, API_CONTRACT, TEST_COVERAGE, UI_UX, DOCUMENTATION, DATABASE, PERFORMANCE, DEPENDENCY, ARCHITECTURE | Persona, Finding, ComplianceItem, AgentRun, HealthSchedule |
| `AgentStatus` | PENDING, RUNNING, COMPLETED, FAILED | AgentRun.status |
| `AgentResult` | PASS, WARN, FAIL | AgentRun.result |
| `BusinessImpact` | LOW, MEDIUM, HIGH, CRITICAL | TechDebtItem.businessImpact |
| `ComplianceStatus` | MET, PARTIAL, MISSING, NOT_APPLICABLE | ComplianceItem.status |
| `DebtCategory` | ARCHITECTURE, CODE, TEST, DEPENDENCY, DOCUMENTATION | TechDebtItem.category, Finding.debtCategory |
| `DebtStatus` | IDENTIFIED, PLANNED, IN_PROGRESS, RESOLVED | TechDebtItem.status |
| `DirectiveCategory` | ARCHITECTURE, STANDARDS, CONVENTIONS, CONTEXT, OTHER | Directive.category |
| `DirectiveScope` | TEAM, PROJECT, USER | Directive.scope |
| `Effort` | S, M, L, XL | Finding.effortEstimate, TechDebtItem.effortEstimate |
| `FindingStatus` | OPEN, ACKNOWLEDGED, FALSE_POSITIVE, FIXED, WONT_FIX | Finding.status |
| `GitHubAuthType` | PAT, OAUTH, SSH | GitHubConnection.authType |
| `InvitationStatus` | PENDING, ACCEPTED, EXPIRED | Invitation.status |
| `JobMode` | AUDIT, COMPLIANCE, BUG_INVESTIGATE, REMEDIATE, TECH_DEBT, DEPENDENCY, HEALTH_MONITOR | QaJob.mode |
| `JobResult` | PASS, WARN, FAIL | QaJob.overallResult |
| `JobStatus` | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED | QaJob.status |
| `MfaMethod` | NONE, TOTP, EMAIL | User.mfaMethod |
| `Priority` | P0, P1, P2, P3 | RemediationTask.priority |
| `ScheduleType` | DAILY, WEEKLY, ON_COMMIT | HealthSchedule.scheduleType |
| `Scope` | SYSTEM, TEAM, USER | Persona.scope |
| `Severity` | CRITICAL, HIGH, MEDIUM, LOW | Finding.severity, DependencyVulnerability.severity |
| `SpecType` | OPENAPI, MARKDOWN, SCREENSHOT, FIGMA | Specification.specType |
| `TaskStatus` | PENDING, ASSIGNED, EXPORTED, JIRA_CREATED, COMPLETED | RemediationTask.status |
| `TeamRole` | OWNER, ADMIN, MEMBER, VIEWER | TeamMember.role, Invitation.role |
| `VulnerabilityStatus` | OPEN, UPDATING, SUPPRESSED, RESOLVED | DependencyVulnerability.status |

**Total: 25 enums**

---

## 5. Core Module -- Repositories

All repositories extend `JpaRepository` with the entity type and its PK type.

### 5.1 UserRepository (`JpaRepository<User, UUID>`)
- `findByEmail(String)` -> `Optional<User>`
- `existsByEmail(String)` -> `boolean`
- `findByDisplayNameContainingIgnoreCase(String)` -> `List<User>`
- `countByIsActiveTrue()` -> `long`

### 5.2 TeamRepository (`JpaRepository<Team, UUID>`)
- `findByOwnerId(UUID)` -> `List<Team>`

### 5.3 TeamMemberRepository (`JpaRepository<TeamMember, UUID>`)
- `findByTeamId(UUID)` -> `List<TeamMember>`
- `findByUserId(UUID)` -> `List<TeamMember>`
- `findByTeamIdAndUserId(UUID, UUID)` -> `Optional<TeamMember>`
- `existsByTeamIdAndUserId(UUID, UUID)` -> `boolean`
- `countByTeamId(UUID)` -> `long`
- `deleteByTeamIdAndUserId(UUID, UUID)` -> `void`

### 5.4 ProjectRepository (`JpaRepository<Project, UUID>`)
- `findByTeamIdAndIsArchivedFalse(UUID)` -> `List<Project>`
- `findByTeamId(UUID)` -> `List<Project>`
- `findByTeamIdAndRepoFullName(UUID, String)` -> `Optional<Project>`
- `countByTeamId(UUID)` -> `long`
- `findByTeamId(UUID, Pageable)` -> `Page<Project>`
- `findByTeamIdAndIsArchivedFalse(UUID, Pageable)` -> `Page<Project>`

### 5.5 InvitationRepository (`JpaRepository<Invitation, UUID>`)
- `findByToken(String)` -> `Optional<Invitation>`
- `findByTeamIdAndStatus(UUID, InvitationStatus)` -> `List<Invitation>`
- `findByEmailAndStatus(String, InvitationStatus)` -> `List<Invitation>`
- `findByTeamIdAndEmailAndStatusForUpdate(...)` -> `List<Invitation>` (`@Lock(PESSIMISTIC_WRITE)`)

### 5.6 PersonaRepository (`JpaRepository<Persona, UUID>`)
- `findByTeamId(UUID)` / `findByTeamId(UUID, Pageable)`
- `findByScope(Scope)` -> `List<Persona>`
- `findByTeamIdAndAgentType(UUID, AgentType)` -> `List<Persona>`
- `findByTeamIdAndAgentTypeAndIsDefaultTrue(UUID, AgentType)` -> `Optional<Persona>`
- `findByCreatedById(UUID)` -> `List<Persona>`

### 5.7 DirectiveRepository (`JpaRepository<Directive, UUID>`)
- `findByTeamId(UUID)`, `findByProjectId(UUID)`, `findByTeamIdAndScope(UUID, DirectiveScope)`
- `deleteAllByProjectId(UUID)` (JPQL `@Modifying`)

### 5.8 ProjectDirectiveRepository (`JpaRepository<ProjectDirective, ProjectDirectiveId>`)
- `findByProjectId(UUID)`, `findByProjectIdAndEnabledTrue(UUID)`, `findByDirectiveId(UUID)`
- `deleteByProjectIdAndDirectiveId(UUID, UUID)`, `deleteAllByProjectId(UUID)` (JPQL)

### 5.9 QaJobRepository (`JpaRepository<QaJob, UUID>`)
- `findByProjectIdOrderByCreatedAtDesc(UUID)`, `findByProjectIdAndMode(UUID, JobMode)`
- `findByStartedById(UUID)` / paginated, `findByProjectId(UUID, Pageable)`
- `countByProjectIdAndStatus(UUID, JobStatus)`, `deleteAllByProjectId(UUID)` (JPQL)

### 5.10-5.26 Remaining Core Repositories
SpecificationRepository, FindingRepository, ComplianceItemRepository, TechDebtItemRepository, RemediationTaskRepository, BugInvestigationRepository, DependencyScanRepository, DependencyVulnerabilityRepository, HealthSnapshotRepository, HealthScheduleRepository, AgentRunRepository, NotificationPreferenceRepository, SystemSettingRepository, AuditLogRepository, GitHubConnectionRepository, JiraConnectionRepository, MfaEmailCodeRepository -- all follow the same pattern with team-scoped queries, paginated finders, and JPQL cascade delete methods.

---

## 6. Core Module -- Services

**Package:** `com.codeops.service` (26 service files)

### 6.1 AdminService
**Dependencies:** UserRepository, TeamRepository, ProjectRepository, QaJobRepository, SystemSettingRepository

| Method | Purpose | Auth |
|--------|---------|------|
| `getAllUsers(Pageable)` | Paginated all users | `isAdmin()` |
| `getUserById(UUID)` | Single user | `isAdmin()` |
| `updateUserStatus(UUID, AdminUpdateUserRequest)` | Activate/deactivate | Controller `@PreAuthorize` |
| `getSystemSetting(String)` | Get setting by key | Controller `@PreAuthorize` |
| `updateSystemSetting(...)` | Upsert setting | Controller `@PreAuthorize` |
| `getAllSettings()` | All settings | Controller `@PreAuthorize` |
| `getUsageStats()` | Platform usage counts | `isAdmin()` |

### 6.2 AgentRunService
Team membership auth. CRUD for agent runs: `createAgentRun()`, `createAgentRuns()` (batch), `getAgentRuns()`, `getAgentRun()`, `updateAgentRun()`.

### 6.3 AuditLogService
`log()` is `@Async @Transactional`. `getTeamAuditLog()` requires team membership. `getUserAuditLog()` requires self.

### 6.4 AuthService
Public: `register()`, `login()` (with MFA flow), `refreshToken()`. Authenticated: `changePassword()`.

### 6.5 BugInvestigationService
Team membership auth. CRUD + `uploadRca()` to S3.

### 6.6 ComplianceService
Team membership auth. Spec CRUD, compliance item CRUD (single + batch), summary with weighted score.

### 6.7 DependencyService
Team membership auth. Scan CRUD, vulnerability CRUD (single + batch), status management.

### 6.8 DirectiveService
Team admin for create/assign. Creator-or-admin for update/delete. Team membership for read. Version increment on content change.

### 6.9 EncryptionService
AES-256-GCM encryption for stored credentials.

### 6.10 FindingService
Team membership auth. CRUD (single + batch), filter by severity/agent/status, bulk status update.

### 6.11 GitHubConnectionService
Team membership auth. CRUD with credential encryption.

### 6.12 HealthMonitorService
Team membership auth. Schedule CRUD, snapshot CRUD, trend data.

### 6.13 JiraConnectionService
Team membership auth. CRUD with credential encryption.

### 6.14 MetricsService
Project/team metrics aggregation, health trend.

### 6.15 MfaService
TOTP and email MFA setup, verification, recovery codes.

### 6.16 NotificationService
Preference management, `shouldNotify()` check.

### 6.17 PersonaService
Scope-based auth. CRUD, set/remove default per team+agentType.

### 6.18 ProjectService
Team membership for CRUD. Team admin for archive. OWNER only for delete (14-step cascade).

### 6.19 QaJobService
Team membership auth. CRUD, triggers notification on completion.

### 6.20 RemediationTaskService
Team membership auth. CRUD (single + batch), assignment, prompt upload to S3.

### 6.21 ReportStorageService
Upload/download markdown reports and spec files.

### 6.22 S3StorageService
S3 with local filesystem fallback at `~/.codeops/storage/`.

### 6.23 TeamService
CRUD, member management, invitation flow (pessimistic lock), OWNER-only for delete and role changes.

### 6.24 TechDebtService
Team membership auth. CRUD (single + batch), status management, summary stats.

### 6.25 TokenBlacklistService
In-memory token blacklist with expiration cleanup.

### 6.26 UserService
Self for profile update. Admin for activate/deactivate.

---

## 7. Core Module -- Controllers

### 7.1 AuthController (`/api/v1/auth`) -- 14 endpoints

| # | HTTP | Path | Auth | Status |
|---|------|------|------|--------|
| 1 | POST | `/register` | Public | 201 |
| 2 | POST | `/login` | Public | 200 |
| 3 | POST | `/refresh` | Public | 200 |
| 4 | POST | `/logout` | Authenticated | 204 |
| 5 | POST | `/change-password` | Authenticated | 200 |
| 6 | POST | `/mfa/setup` | Authenticated | 200 |
| 7 | POST | `/mfa/verify` | Authenticated | 200 |
| 8 | POST | `/mfa/login` | Public | 200 |
| 9 | POST | `/mfa/disable` | Authenticated | 200 |
| 10 | POST | `/mfa/recovery-codes` | Authenticated | 200 |
| 11 | GET | `/mfa/status` | Authenticated | 200 |
| 12 | POST | `/mfa/setup/email` | Authenticated | 200 |
| 13 | POST | `/mfa/verify-setup/email` | Authenticated | 200 |
| 14 | POST | `/mfa/resend` | Public | 200 |

### 7.2 AdminController (`/api/v1/admin`) -- 10 endpoints
Class-level `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")`. Users CRUD, settings CRUD, usage stats, audit log, MFA reset.

### 7.3 UserController (`/api/v1/users`) -- 6 endpoints
GET /me, GET /{id}, PUT /{id}, GET /search, PUT /{id}/deactivate, PUT /{id}/activate

### 7.4 TeamController (`/api/v1/teams`) -- 12 endpoints
Team CRUD, member management, invitation flow.

### 7.5 ProjectController (`/api/v1/projects`) -- 7 endpoints
CRUD, archive/unarchive, delete.

### 7.6 TaskController (`/api/v1/tasks`) -- 6 endpoints
Create (single + batch), list by job, get, list assigned-to-me, update.

### 7.7 JobController (`/api/v1/jobs`) -- 13 endpoints
Job CRUD, agent run management, bug investigation CRUD.

### 7.8 FindingController (`/api/v1/findings`) -- 10 endpoints
CRUD (single + batch), filter by severity/agent/status, severity counts, status update (single + bulk).

### 7.9 PersonaController (`/api/v1/personas`) -- 11 endpoints
CRUD, list by team/agent/default/mine/system, set/remove default.

### 7.10 DirectiveController (`/api/v1/directives`) -- 11 endpoints
CRUD, assign/remove from project, list assignments, list enabled, toggle.

### 7.11 TechDebtController (`/api/v1/tech-debt`) -- 9 endpoints
CRUD (single + batch), filter by status/category, status update, delete, summary.

### 7.12 ComplianceController (`/api/v1/compliance`) -- 7 endpoints
Spec CRUD, compliance item CRUD (single + batch), filter by status, summary.

### 7.13 DependencyController (`/api/v1/dependencies`) -- 10 endpoints
Scan CRUD, vulnerability CRUD (single + batch), filter by severity, open vulns, status update.

### 7.14 HealthMonitorController (`/api/v1/health-monitor`) -- 8 endpoints
Schedule CRUD, snapshot CRUD, latest, trend.

### 7.15 MetricsController (`/api/v1/metrics`) -- 3 endpoints
Project metrics, team metrics, health trend.

### 7.16 IntegrationController (`/api/v1/integrations`) -- 8 endpoints
GitHub connections CRUD (4), Jira connections CRUD (4).

### 7.17 ReportController (`/api/v1/reports`) -- 5 endpoints
Upload agent report, upload summary, download, upload spec file, download spec.

### 7.18 HealthController (`/api/v1/health`) -- 1 endpoint (public)

**Core Endpoints Total: 151**

---

## 8. Core Module -- Security

### 8.1 JwtAuthFilter
- Extends `OncePerRequestFilter`
- Extracts `Authorization: Bearer <token>` header
- Builds `UsernamePasswordAuthenticationToken` with UUID principal and `ROLE_` prefixed authorities
- Rejects MFA challenge tokens for normal API access

### 8.2 JwtTokenProvider
- **Algorithm:** HS256, minimum 32-char secret
- **Access token:** Subject=userId, claims: email, roles, jti. Expiry: 24h
- **Refresh token:** Subject=userId, claim: type="refresh", jti. Expiry: 30d
- **MFA challenge token:** Subject=userId, claim: type="mfa_challenge". Expiry: 5 min
- **Blacklist check:** JTI via `TokenBlacklistService`

### 8.3 RateLimitFilter
- Applies to `/api/v1/auth/` only
- 10 requests per 60s per IP (`X-Forwarded-For` or `getRemoteAddr()`)
- In-memory `ConcurrentHashMap`, HTTP 429 on exceeded

### 8.4 SecurityConfig
- CSRF disabled, session STATELESS
- Public: `/api/v1/auth/**`, `/api/v1/health`, `/api/v1/courier/health`, Swagger
- Headers: CSP, X-Frame-Options: DENY, HSTS, X-Content-Type-Options
- Filter order: RequestCorrelationFilter -> RateLimitFilter -> JwtAuthFilter
- BCrypt strength 12

### 8.5 SecurityUtils
- `getCurrentUserId()`, `hasRole(String)`, `isAdmin()` (ADMIN or OWNER)

---

## 9. Core Module -- Config

| Config | Purpose |
|--------|---------|
| **AppConstants** | All limits, defaults, S3 prefixes, module constants |
| **AsyncConfig** | ThreadPool: core=5, max=20, queue=100, CallerRunsPolicy |
| **CorsConfig** | Configurable origins, standard methods, credentials=true |
| **DataSeeder** | `@Profile("dev")`, seeds all 4 modules |
| **GlobalExceptionHandler** | Centralized exception mapping |
| **HealthController** | GET `/api/v1/health` public |
| **JacksonConfig** | LenientInstantDeserializer for timezone-less timestamps |
| **JwtProperties** | secret, expirationHours(24), refreshExpirationDays(30) |
| **KafkaConsumerConfig** | Bootstrap, consumer group, FixedBackOff(1s, 3 retries) |
| **LoggingInterceptor** | MDC, request/response logging with duration |
| **MailProperties** | enabled(false), fromEmail |
| **RequestCorrelationFilter** | X-Correlation-ID, MDC setup |
| **RestTemplateConfig** | 5s connect, 10s read timeout |
| **S3Config** | S3 when enabled, local fallback |
| **WebMvcConfig** | Registers LoggingInterceptor |

---

## 10. Core Module -- Exceptions

```
RuntimeException
  +-- CodeOpsException (500 -- message not exposed)
        +-- AuthorizationException (403)
        +-- ValidationException (400)
        +-- NotFoundException (404)
```

All responses use `ErrorResponse(int status, String message)` record. GlobalExceptionHandler maps all standard Spring exceptions (MethodArgumentNotValid, MissingServletRequestParameter, HttpRequestMethodNotSupported, HttpMessageNotReadable, NoResourceFound) plus CodeOps exceptions. Catch-all returns 500 with generic message.

---

## 11. Core Module -- Notifications

### 11.1 EmailService
SMTP via JavaMailSender. Disabled by default (logs at WARN). HTML-escapes user content. Methods: `sendEmail()`, `sendInvitationEmail()`, `sendCriticalFindingAlert()`, `sendHealthDigest()`, `sendMfaCode()`.

### 11.2 NotificationDispatcher
All `@Async`. `dispatchJobCompleted()` (Teams), `dispatchCriticalFinding()` (Teams + Email with preference check), `dispatchTaskAssigned()` (Email with preference check), `dispatchInvitation()` (Email always).

### 11.3 TeamsWebhookService
MessageCard JSON via RestTemplate. SSRF protection (HTTPS, rejects loopback/private). `postJobCompleted()`, `postCriticalAlert()`.

---

## 12. Courier Module -- Entities

**Package:** `com.codeops.courier.entity` (18 entities)

| # | Entity | Table | Key Relationships |
|---|--------|-------|-------------------|
| 1 | Collection | `collections` | teamId (UUID), createdBy (UUID) |
| 2 | Folder | `folders` | `@ManyToOne` Collection, self-referential parentFolder, `@OneToMany` subFolders + requests |
| 3 | Request | `requests` | `@ManyToOne` Folder |
| 4 | RequestHeader | `request_headers` | `@ManyToOne` Request |
| 5 | RequestParam | `request_params` | `@ManyToOne` Request |
| 6 | RequestBody | `request_bodies` | `@ManyToOne` Request |
| 7 | RequestAuth | `request_auths` | `@ManyToOne` Request |
| 8 | RequestScript | `request_scripts` | `@ManyToOne` Request |
| 9 | Environment | `environments` | teamId, `@OneToMany` variables |
| 10 | EnvironmentVariable | `environment_variables` | `@ManyToOne` Environment |
| 11 | GlobalVariable | `global_variables` | teamId |
| 12 | CollectionShare | `collection_shares` | `@ManyToOne` Collection |
| 13 | CollectionFork | `collection_forks` | sourceCollection, forkedCollection |
| 14 | MergeRequest | `merge_requests` | sourceFork, targetCollection |
| 15 | RequestHistory | `request_history` | userId, collectionId, requestId, environmentId |
| 16 | RunResult | `run_results` | teamId, collectionId, `@OneToMany` iterations |
| 17 | RunIteration | `run_iterations` | `@ManyToOne` RunResult |
| 18 | CodeSnippetTemplate | `code_snippet_templates` | language, templateContent |

---

## 13. Courier Module -- Enums

| Enum | Values |
|------|--------|
| `AuthType` | NO_AUTH, API_KEY, BEARER_TOKEN, BASIC_AUTH, OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS, OAUTH2_IMPLICIT, OAUTH2_PASSWORD, JWT_BEARER, INHERIT_FROM_PARENT |
| `BodyType` | NONE, FORM_DATA, X_WWW_FORM_URLENCODED, RAW_JSON, RAW_XML, RAW_HTML, RAW_TEXT, RAW_YAML, BINARY, GRAPHQL |
| `CodeLanguage` | CURL, PYTHON_REQUESTS, JAVASCRIPT_FETCH, JAVASCRIPT_AXIOS, JAVA_HTTP_CLIENT, JAVA_OKHTTP, CSHARP_HTTP_CLIENT, GO_NET_HTTP, PHP_CURL, RUBY_NET_HTTP, SWIFT_URL_SESSION, KOTLIN_OKHTTP, DART_HTTP, RUST_REQWEST |
| `HttpMethod` | GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS |
| `RunStatus` | RUNNING, COMPLETED, FAILED, CANCELLED |
| `ScriptType` | PRE_REQUEST, POST_RESPONSE |
| `SharePermission` | VIEW, EDIT |

---

## 14. Courier Module -- Repositories

18 repositories extending `JpaRepository` for each Courier entity with team-scoped queries, paginated finders, and cascade delete.

---

## 15. Courier Module -- Services

**Package:** `com.codeops.courier.service` (22 services)

| Service | Purpose |
|---------|---------|
| `CollectionService` | CRUD with team-scoped auth, fork/merge support |
| `RequestService` | CRUD within folders, component assembly |
| `RequestComponentService` | Headers, params, body, auth, scripts management |
| `FolderService` | Hierarchy CRUD, reorder, tree building |
| `EnvironmentService` | CRUD, clone, activate/deactivate |
| `EnvironmentVariableService` | Batch save/replace with secret encryption |
| `GlobalVariableService` | Team-scoped global variables |
| `CollectionShareService` | Share with VIEW/EDIT permissions |
| `CollectionForkService` | Deep copy fork |
| `MergeRequestService` | Create/review/merge merge requests |
| `RequestHistoryService` | Save and query execution history |
| `ImportService` | Import from Postman v2.1, OpenAPI 3.0 |
| `ExportService` | Export to Postman v2.1, OpenAPI 3.0 |
| `RequestProxyService` | Execute HTTP requests with variable substitution |
| `ScriptEngineService` | GraalJS sandboxed script execution |
| `VariableResolverService` | Resolve `{{variable}}` placeholders |
| `AuthResolverService` | Auth inheritance (request -> folder -> collection) |
| `GraphQLService` | GraphQL queries and introspection |
| `CodeGenerationService` | Code snippets in 14 languages |
| `CollectionRunnerService` | Run collections with iterations and delays |
| `CourierHealthService` | Module health endpoint |
| `CourierCorsConfig` | Proxy endpoint CORS |

---

## 16. Courier Module -- Controllers

| Controller | Base Path | Endpoints |
|------------|-----------|-----------|
| CollectionController | `/api/v1/courier/teams/{teamId}/collections` | CRUD, search |
| RequestController | `.../folders/{folderId}/requests` | CRUD, reorder |
| FolderController | `.../collections/{collectionId}/folders` | CRUD, reorder, tree |
| EnvironmentController | `/api/v1/courier/teams/{teamId}/environments` | CRUD, clone, activate |
| VariableController | various | Env variables, global variables |
| ShareController | `.../collections/{collectionId}/shares` | Share, forks, merge |
| HistoryController | `/api/v1/courier/teams/{teamId}/history` | List, detail, delete |
| ImportController | `/api/v1/courier/teams/{teamId}/import` | Import collections |
| ProxyController | `/api/v1/courier/proxy` | Send request |
| GraphQLController | `/api/v1/courier/graphql` | Execute, introspect |
| CodeGenController | `/api/v1/courier/code-gen` | Generate, languages, templates |
| RunnerController | `/api/v1/courier/runner` | Start run, results |
| HealthController | `/api/v1/courier/health` | Public health check |

**Courier Endpoints Total: ~87**

---

## 17. Courier Module -- DTOs

### Request DTOs (31 records)
CreateCollectionRequest, UpdateCollectionRequest, CreateRequestRequest, UpdateRequestRequest, ReorderRequestsRequest, SaveRequestHeadersRequest, SaveRequestParamsRequest, SaveRequestBodyRequest, SaveRequestAuthRequest, SaveRequestScriptRequest, CreateFolderRequest, UpdateFolderRequest, ReorderFolderRequest, CreateEnvironmentRequest, UpdateEnvironmentRequest, CloneEnvironmentRequest, SaveEnvironmentVariablesRequest, SaveGlobalVariableRequest, BatchSaveGlobalVariablesRequest, ShareCollectionRequest, UpdateSharePermissionRequest, CreateForkRequest, CreateMergeRequestRequest, ResolveMergeRequest, ImportCollectionRequest, SendRequestProxyRequest, ExecuteGraphQLRequest, IntrospectGraphQLRequest, GenerateCodeRequest, StartCollectionRunRequest

### Response DTOs (29 records)
CollectionResponse, CollectionSummaryResponse, CollectionShareResponse, RequestResponse, RequestSummaryResponse, RequestHeaderResponse, RequestParamResponse, RequestBodyResponse, RequestAuthResponse, RequestScriptResponse, FolderResponse, FolderTreeResponse, EnvironmentResponse, EnvironmentVariableResponse, GlobalVariableResponse, ForkResponse, MergeRequestResponse, RequestHistoryResponse, RequestHistoryDetailResponse, ImportResultResponse, ExportCollectionResponse, ProxyResponse, GraphQLResponse, CodeSnippetResponse, CodeSnippetTemplateResponse, RunResultResponse, RunResultDetailResponse, RunIterationResponse, PageResponse

---

## 18. Courier Module -- Mappers

13 MapStruct interfaces: CollectionMapper, RequestMapper, FolderMapper, EnvironmentMapper, EnvironmentVariableMapper, RequestHeaderMapper, RequestParamMapper, RequestBodyMapper, RequestAuthMapper, RequestScriptMapper, RequestHistoryMapper, RunResultMapper, GlobalVariableMapper.

**Boolean field pattern:** `@Mapping(target = "isXxx", source = "xxx")` required for all `boolean is*` entity fields due to Lombok/JavaBeans property name mismatch.

---

## 19. Logger Module -- Entities

**Package:** `com.codeops.logger.entity` (16 entities)
**Pipeline:** Log Ingested -> Trap Evaluates -> Alert Fires -> Notification Sent

| # | Entity | Table | Key Features |
|---|--------|-------|--------------|
| 1 | LogEntry | `log_entries` | 7 indexes, denormalized serviceName, full exception capture |
| 2 | LogSource | `log_sources` | Team-scoped, optional Registry link, log count tracking |
| 3 | AlertRule | `alert_rules` | Links trap to channel, severity, throttle minutes |
| 4 | AlertChannel | `alert_channels` | EMAIL/WEBHOOK/TEAMS/SLACK, JSON config |
| 5 | AlertHistory | `alert_history` | Status lifecycle: FIRED->ACKNOWLEDGED->RESOLVED |
| 6 | LogTrap | `log_traps` | `@OneToMany` conditions, trigger tracking |
| 7 | TrapCondition | `trap_conditions` | REGEX/KEYWORD/FREQUENCY_THRESHOLD/ABSENCE |
| 8 | Dashboard | `dashboards` | `@OneToMany` widgets, templates, sharing |
| 9 | DashboardWidget | `dashboard_widgets` | Grid layout, 8 widget types |
| 10 | Metric | `metrics` | Unique (name, service, team), 4 metric types |
| 11 | MetricSeries | `metric_series` | Time-series data points with resolution |
| 12 | TraceSpan | `trace_spans` | 6 indexes, parent-child hierarchy |
| 13 | SavedQuery | `saved_queries` | Reusable queries, execution tracking |
| 14 | QueryHistory | `query_history` | Query audit trail |
| 15 | AnomalyBaseline | `anomaly_baselines` | Statistical baselines for anomaly detection |
| 16 | RetentionPolicy | `retention_policies` | PURGE/ARCHIVE policies |

---

## 20. Logger Module -- Enums

| Enum | Values |
|------|--------|
| `LogLevel` | TRACE, DEBUG, INFO, WARN, ERROR, FATAL |
| `AlertSeverity` | INFO, WARNING, CRITICAL |
| `AlertChannelType` | EMAIL, WEBHOOK, TEAMS, SLACK |
| `AlertStatus` | FIRED, ACKNOWLEDGED, RESOLVED |
| `ConditionType` | REGEX, KEYWORD, FREQUENCY_THRESHOLD, ABSENCE |
| `TrapType` | PATTERN, FREQUENCY, ABSENCE |
| `WidgetType` | LOG_STREAM, TIME_SERIES_CHART, COUNTER, GAUGE, TABLE, HEATMAP, PIE_CHART, BAR_CHART |
| `MetricType` | COUNTER, GAUGE, HISTOGRAM, TIMER |
| `SpanStatus` | OK, ERROR |
| `RetentionAction` | PURGE, ARCHIVE |

---

## 21. Logger Module -- Repositories

16 repositories. Key method counts: LogEntryRepository (21), AlertHistoryRepository (8), TraceSpanRepository (8), MetricSeriesRepository (7), AnomalyBaselineRepository (7).

---

## 22. Logger Module -- Services

**Package:** `com.codeops.logger.service` (19 services)

| Service | Purpose |
|---------|---------|
| `LogIngestionService` | Ingest single/batch/raw, auto-create sources, publish events |
| `LogSourceService` | CRUD for log sources |
| `LogQueryService` | JPA Criteria queries, DSL, full-text search, saved queries, history |
| `LogQueryDslParser` | DSL syntax -> predicates (14 fields, 12 operators, AND/OR) |
| `LogParsingService` | JSON -> KV -> Spring Boot -> Syslog -> plain text |
| `AlertService` | Rule CRUD, fire with throttling, lifecycle management |
| `AlertChannelService` | Channel CRUD, `@Async` delivery, SSRF protection |
| `LogTrapService` | Trap CRUD, evaluate entries, test traps |
| `TrapEvaluationEngine` | Regex/keyword/frequency/absence evaluation, pattern cache |
| `KafkaLogConsumer` | Consume from `codeops-logs` topic |
| `DashboardService` | Full CRUD + widgets + templates + duplicate |
| `MetricsService` | Registration, time series, aggregation (bean: `loggerMetricsService`) |
| `MetricAggregationService` | sum, avg, min, max, stddev, p50/p95/p99 |
| `TraceService` | Span CRUD, trace flow, waterfall, RCA |
| `TraceAnalysisService` | Waterfall computation, critical path, root cause |
| `AnomalyService` | Baseline CRUD, compute, detect anomalies |
| `RetentionService` | Policy CRUD, execute (purge/archive), estimate |
| `LogStatsService` | Statistics by level, service, time range |
| `LogExportService` | Export as JSON/CSV |

---

## 23. Logger Module -- Controllers

All under `/api/v1/logger` with `@PreAuthorize("isAuthenticated()")`. Abstract `BaseController` provides team membership verification.

| Controller | Endpoints |
|------------|-----------|
| LogIngestionController | ingest, batch, raw |
| LogQueryController | query, search, DSL |
| LogSourceController | CRUD (5) |
| AlertController | Rule CRUD + fire + history + lifecycle |
| AlertChannelController | CRUD (5) |
| LogTrapController | CRUD + toggle + test (7) |
| DashboardController | Full CRUD + widgets + templates + layout |
| MetricController | Register, query, push, time series, aggregation |
| TraceController | Create spans, trace flow, waterfall, RCA |
| AnomalyController | Baseline CRUD, compute, detect |
| RetentionController | CRUD + execute + estimate |

**Logger Endpoints Total: ~95**

---

## 24. Logger Module -- DTOs

### Request DTOs (28 records)
IngestLogEntryRequest, LogQueryRequest, SearchLogsRequest, ExecuteDslRequest, CreateLogSourceRequest, UpdateLogSourceRequest, CreateAlertRuleRequest, UpdateAlertRuleRequest, CreateAlertChannelRequest, UpdateAlertChannelRequest, UpdateAlertStatusRequest, CreateLogTrapRequest, UpdateLogTrapRequest, TrapConditionRequest, TestTrapRequest, CreateDashboardRequest, UpdateDashboardRequest, CreateWidgetRequest, UpdateWidgetRequest, RegisterMetricRequest, PushMetricDataRequest, MetricDataPointRequest, CreateTraceSpanRequest, CreateAnomalyBaselineRequest, UpdateAnomalyBaselineRequest, CreateRetentionPolicyRequest, UpdateRetentionPolicyRequest, SaveQueryRequest

### Response DTOs (30 records)
LogEntryResponse, LogSourceResponse, AlertRuleResponse, AlertChannelResponse, AlertHistoryResponse, ActiveAlertCountsResponse, LogTrapResponse, TrapConditionResponse, TrapTestResultResponse, DashboardResponse, DashboardDetailResponse, DashboardWidgetResponse, MetricResponse, MetricSeriesResponse, MetricSeriesPointResponse, MetricAggregationResponse, MetricLatestValueResponse, ServiceMetricsSummaryResponse, TraceSpanResponse, TraceFlowResponse, TraceWaterfallResponse, WaterfallSpanResponse, RootCauseAnalysisResponse, AnomalyBaselineResponse, AnomalyDetectionResponse, AnomalyResultResponse, RetentionPolicyResponse, RetentionEstimateResponse, SavedQueryResponse, QueryHistoryResponse

---

## 25. Logger Module -- Mappers

13 MapStruct interfaces: LogEntryMapper, LogSourceMapper, AlertRuleMapper, AlertChannelMapper, AlertHistoryMapper, LogTrapMapper, TrapConditionMapper, DashboardMapper, DashboardWidgetMapper, MetricMapper, MetricSeriesMapper, TraceSpanMapper, AnomalyBaselineMapper

---

## 26. Logger Module -- Events

### 26.1 LogEntryIngestedEvent
Extends `ApplicationEvent`. Carries `LogEntry` and `UUID teamId`. Published by `LogIngestionService` after persistence.

### 26.2 LogEntryEventListener
`@EventListener` on `LogEntryIngestedEvent`. Evaluates all active traps via `LogTrapService.evaluateEntry()`. Fires alerts via `AlertService.fireAlerts()`.

---

## 27. Registry Module -- Entities

**Package:** `com.codeops.registry.entity` (11 entities)

| # | Entity | Table | Key Features |
|---|--------|-------|--------------|
| 1 | ServiceRegistration | `service_registrations` | slug (UNIQUE), 20 service types, health tracking |
| 2 | Solution | `solutions` | slug (UNIQUE), `@OneToMany` members, 8 categories |
| 3 | SolutionMember | `solution_members` | Unique (solution_id, service_id), role, displayOrder |
| 4 | PortAllocation | `port_allocations` | 12 port types, auto/manual allocation |
| 5 | PortRange | `port_ranges` | Unique (team_id, port_type, environment) |
| 6 | ServiceDependency | `service_dependencies` | Unique (source, target), 8 dep types, isRequired |
| 7 | ApiRouteRegistration | `api_route_registrations` | Route prefix, gateway association |
| 8 | ConfigTemplate | `config_templates` | Unique (service, type, environment), versioned |
| 9 | EnvironmentConfig | `environment_configs` | Unique (service, environment, key), 4 sources |
| 10 | InfraResource | `infra_resources` | 12 resource types, optional service ownership |
| 11 | WorkstationProfile | `workstation_profiles` | servicesJson, startupOrder, isDefault |

---

## 28. Registry Module -- Enums

| Enum | Values |
|------|--------|
| `ServiceType` | SPRING_BOOT_API, EXPRESS_API, FASTAPI, DOTNET_API, GO_API, REACT_SPA, VUE_SPA, NEXT_JS, FLUTTER_WEB, FLUTTER_DESKTOP, FLUTTER_MOBILE, DATABASE_SERVICE, CACHE_SERVICE, MESSAGE_BROKER, GATEWAY, WORKER, MCP_SERVER, LIBRARY, CLI_TOOL, OTHER |
| `ServiceStatus` | ACTIVE, INACTIVE, DEPRECATED, ARCHIVED |
| `HealthStatus` | UP, DOWN, DEGRADED, UNKNOWN |
| `SolutionCategory` | PLATFORM, APPLICATION, MICROSERVICE_GROUP, SHARED_INFRASTRUCTURE, DATA_PIPELINE, MONITORING, DEVELOPMENT_TOOLS, OTHER |
| `SolutionStatus` | ACTIVE, PLANNING, DEPRECATED, ARCHIVED |
| `SolutionMemberRole` | PRIMARY, SUPPORTING, INFRASTRUCTURE, OPTIONAL |
| `PortType` | HTTP, HTTPS, FRONTEND_DEV, DATABASE, REDIS, KAFKA, GRPC, WEBSOCKET, DEBUG, MANAGEMENT, METRICS, CUSTOM |
| `DependencyType` | REST_API, GRPC, DATABASE, CACHE, MESSAGE_QUEUE, FILE_SYSTEM, SHARED_LIBRARY, EVENT_BUS |
| `ConfigTemplateType` | DOCKER_COMPOSE, APPLICATION_YML, CLAUDE_CODE_HEADER |
| `ConfigSource` | MANUAL, GENERATED, ENVIRONMENT, VAULT |
| `InfraResourceType` | RDS, S3_BUCKET, SQS_QUEUE, SNS_TOPIC, ELASTICACHE, LAMBDA, EC2, ECS_SERVICE, LOAD_BALANCER, CLOUDFRONT, ROUTE53, CUSTOM |

---

## 29. Registry Module -- Repositories

11 repositories with team-scoped queries, slug lookups, paginated finders.

| Repository | Notable Methods |
|------------|----------------|
| ServiceRegistrationRepository | findByTeamId (paginated), findByTeamIdAndSlug, status/type filters |
| SolutionRepository | findByTeamId (paginated), findByTeamIdAndSlug, status/category filters |
| SolutionMemberRepository | findBySolutionId, findByServiceId, existsBySolutionIdAndServiceId |
| PortAllocationRepository | findByServiceId/TeamId with env filter, port conflict check |
| PortRangeRepository | findByTeamId, findByTeamIdAndPortTypeAndEnvironment |
| ServiceDependencyRepository | findBySource/TargetServiceId, findByTeamId (custom @Query) |
| ApiRouteRegistrationRepository | findByServiceId, findByGateway with env/prefix |
| ConfigTemplateRepository | findByServiceId, findByServiceIdAndTemplateTypeAndEnvironment |
| EnvironmentConfigRepository | findByServiceId, findByServiceIdAndEnvironment |
| InfraResourceRepository | paginated with type/env filters, findOrphansByTeamId (WHERE service IS NULL) |
| WorkstationProfileRepository | findByTeamId, findByTeamIdAndIsDefaultTrue, findByTeamIdAndName |

---

## 30. Registry Module -- Services

**Package:** `com.codeops.registry.service` (10 services)

### 30.1 ServiceRegistryService
CRUD with slug generation, team limit checks, port auto-allocation, cloning, identity assembly, live HTTP health checks, parallel health check for all active services.

### 30.2 SolutionService
CRUD with member management (add/update/remove/reorder), slug generation, aggregated health (DOWN > DEGRADED > UNKNOWN > UP).

### 30.3 PortAllocationService
Auto-allocate from ranges, manual with conflict check, release, availability, port map, conflict detection, range management, default range seeding (12 per env).

### 30.4 ApiRouteService
Route CRUD with prefix normalization, gateway validation, overlap checking.

### 30.5 DependencyGraphService
Edge CRUD with validation (no self-dep, same team, no duplicate, limit, cycle detection via BFS). Full graph, BFS impact analysis, topological sort (Kahn's), three-color DFS cycle detection.

### 30.6 ConfigEngineService
Generate docker-compose.yml, application.yml, Claude Code header. Solution-level compose (startup-order-aware). Template upsert with versioning.

### 30.7 InfraResourceService
CRUD with team validation, duplicate checking, reassignment, orphaning.

### 30.8 HealthCheckService
Team health summary (cached + live), unhealthy/never-checked queries, solution health (parallel live checks).

### 30.9 TopologyService
Full topology (nodes + edges + solution groups + layers + stats), solution-scoped, BFS neighborhood (depth 3 max), ecosystem stats. Layers: INFRASTRUCTURE, BACKEND, FRONTEND, GATEWAY, STANDALONE.

### 30.10 WorkstationProfileService
Create from serviceIds or solutionId with startup order computation. CRUD, set default, refresh startup order.

---

## 31. Registry Module -- Controllers

All under `/api/v1/registry` with `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")`.

| Controller | Tag | Key Endpoints |
|------------|-----|---------------|
| RegistryController | Services | CRUD, status, clone, identity, health (11 endpoints) |
| SolutionController | Solutions | CRUD, detail, members, reorder, health (11 endpoints) |
| PortController | Ports | Allocate, release, map, check, ranges (11 endpoints) |
| DependencyController | Dependencies | CRUD, graph, impact, startup-order, cycles (6 endpoints) |
| RouteController | Routes | CRUD, check (5 endpoints) |
| ConfigController | Config | Generate, get, delete (6 endpoints) |
| InfraController | InfraResources | CRUD, orphans, reassign (8 endpoints) |
| TopologyController | Topology | Team, solution, neighborhood, stats (4 endpoints) |
| HealthManagementController | Health | Summary, check, unhealthy (6 endpoints) |
| WorkstationController | Profiles | CRUD, default, from-solution, refresh (9 endpoints) |

**Registry Endpoints Total: 64**

---

## 32. Registry Module -- DTOs

### Request DTOs (22 records)
CreateServiceRequest, UpdateServiceRequest, UpdateServiceStatusRequest, CloneServiceRequest, CreateSolutionRequest, UpdateSolutionRequest, AddSolutionMemberRequest, UpdateSolutionMemberRequest, AllocatePortRequest, AutoAllocatePortRequest, UpdatePortRangeRequest, CreateDependencyRequest, CreateRouteRequest, CreateInfraResourceRequest, UpdateInfraResourceRequest, UpsertEnvironmentConfigRequest, GenerateConfigRequest, CreateWorkstationProfileRequest, UpdateWorkstationProfileRequest, CreateProfileRequest, UpdateProfileRequest

### Response DTOs (27+ records)
ServiceRegistrationResponse, SolutionResponse, SolutionDetailResponse, SolutionMemberResponse, SolutionHealthResponse, PortAllocationResponse, PortRangeResponse, PortMapResponse, PortRangeWithAllocationsResponse, PortCheckResponse, PortConflictResponse, ServiceDependencyResponse, DependencyGraphResponse, DependencyNodeResponse, DependencyEdgeResponse, ImpactAnalysisResponse, ImpactedServiceResponse, ApiRouteResponse, RouteCheckResponse, ConfigTemplateResponse, EnvironmentConfigResponse, InfraResourceResponse, ServiceIdentityResponse, ServiceHealthResponse, TeamHealthSummaryResponse, WorkstationProfileResponse, WorkstationServiceEntry, TopologyResponse, TopologyNodeResponse, TopologySolutionGroup, TopologyLayerResponse, TopologyStatsResponse, PageResponse, ErrorResponse

---

## 33. Registry Module -- Util

### SlugUtils
**File:** `src/main/java/com/codeops/registry/util/SlugUtils.java`
- `generateSlug(String name)` -- lowercase, replace spaces/underscores, strip non-alphanumeric, enforce min/max
- `makeUnique(String baseSlug, Predicate<String>)` -- appends -2, -3, etc.
- `validateSlug(String slug)` -- validates against `AppConstants.SLUG_PATTERN`

---

## 34. Infrastructure

### Docker Compose

| Service | Image | Port | Container |
|---------|-------|------|-----------|
| PostgreSQL 16 | `postgres:16` | 5432 | `codeops-db` |
| Redis 7 | `redis:7-alpine` | 6379 | `codeops-redis` |
| Zookeeper | `confluentinc/cp-zookeeper:7.5.0` | 2181 | `codeops-zookeeper` |
| Kafka | `confluentinc/cp-kafka:7.5.0` | 9094 | `codeops-kafka` |

### Database
- Schema: `public` for all tables
- Hibernate `ddl-auto: update`
- Credentials: codeops/codeops/codeops

### Storage
- S3 when `codeops.aws.s3.enabled=true`
- Local: `~/.codeops/storage/` (dev default)

---

## 35. Database Schema

**Total Tables: 72** (all in `public` schema)

**Core (29):** users, teams, team_members, projects, invitations, personas, directives, project_directives, qa_jobs, specifications, findings, compliance_items, tech_debt_items, remediation_tasks, remediation_task_findings, bug_investigations, dependency_scans, dependency_vulnerabilities, health_snapshots, health_schedules, agent_runs, notification_preferences, system_settings, audit_log, github_connections, jira_connections, mfa_email_codes

**Courier (18):** collections, folders, requests, request_headers, request_params, request_bodies, request_auths, request_scripts, environments, environment_variables, global_variables, collection_shares, collection_forks, merge_requests, request_history, run_results, run_iterations, code_snippet_templates

**Logger (16):** log_entries, log_sources, alert_rules, alert_channels, alert_history, log_traps, trap_conditions, dashboards, dashboard_widgets, metrics, metric_series, trace_spans, saved_queries, query_history, anomaly_baselines, retention_policies

**Registry (11):** service_registrations, solutions, solution_members, port_allocations, port_ranges, service_dependencies, api_route_registrations, config_templates, environment_configs, infra_resources, workstation_profiles

---

## 36. Cross-Cutting Patterns

### Authorization Hierarchy
1. **Public** -- register, login, health
2. **Authenticated** -- `isAuthenticated()`
3. **Team Membership** -- `verifyTeamMembership(teamId)`
4. **Team Admin** -- OWNER or ADMIN role
5. **Creator or Admin** -- for user-created resources
6. **OWNER Only** -- team/project deletion, role changes
7. **System Admin** -- `hasRole('ADMIN') or hasRole('OWNER')`

### Pagination
`PageResponse<T>` with `from(Page<T>)` factory. Default size: 20, max: 100.

### Error Handling
`ErrorResponse(int status, String message)`. All exceptions via GlobalExceptionHandler. 500 never exposes internals.

### Audit Logging
`@Async` fire-and-forget. Pattern: ENTITY_ACTION (e.g., TEAM_CREATED).

### DTO Mapping
MapStruct with `componentModel = "spring"`. Boolean fields require explicit `@Mapping`.

---

## 37. Test Inventory

| Category | Files | Methods |
|----------|-------|---------|
| Unit Tests | 146 | ~2200 |
| Integration Tests | 16 | ~352 |
| **Total** | **162** | **~2552** |

---

## 38. Endpoint Summary

| Module | Controllers | Endpoints |
|--------|-------------|-----------|
| Core | 18 | 151 |
| Courier | 14 | ~87 |
| Logger | 11 | ~95 |
| Registry | 10 | 64 |
| **Total** | **53** | **~397** |

---

## 39. File Count Summary

| Layer | Core | Courier | Logger | Registry | Total |
|-------|------|---------|--------|----------|-------|
| Entities | 28 | 18 | 16 | 11 | 73 |
| Enums | 25 | 7 | 10 | 11 | 53 |
| Repositories | 26 | 18 | 16 | 11 | 71 |
| Services | 26 | 22 | 19 | 10 | 77 |
| Controllers | 18 | 14 | 11 | 10 | 53 |
| Request DTOs | -- | 31 | 28 | 22 | 81 |
| Response DTOs | -- | 29 | 30 | 27 | 86 |
| Mappers | -- | 13 | 13 | 0 | 26 |
| Config/Security | 24 | 2 | 2 | 1 | 29 |
| **Total** | | | | | **~549** |
