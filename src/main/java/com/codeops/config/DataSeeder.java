package com.codeops.config;

import com.codeops.courier.entity.*;
import com.codeops.courier.entity.Collection;
import com.codeops.courier.entity.enums.AuthType;
import com.codeops.courier.entity.enums.HttpMethod;
import com.codeops.courier.repository.*;
import com.codeops.entity.*;
import com.codeops.entity.enums.*;
import com.codeops.logger.entity.*;
import com.codeops.logger.entity.enums.*;
import com.codeops.logger.repository.*;
import com.codeops.registry.entity.*;
import com.codeops.registry.entity.enums.*;
import com.codeops.repository.*;
import com.codeops.registry.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Profile("dev")
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final PersonaRepository personaRepository;
    private final DirectiveRepository directiveRepository;
    private final ProjectDirectiveRepository projectDirectiveRepository;
    private final QaJobRepository qaJobRepository;
    private final BugInvestigationRepository bugInvestigationRepository;
    private final AgentRunRepository agentRunRepository;
    private final FindingRepository findingRepository;
    private final RemediationTaskRepository remediationTaskRepository;
    private final SpecificationRepository specificationRepository;
    private final ComplianceItemRepository complianceItemRepository;
    private final TechDebtItemRepository techDebtItemRepository;
    private final DependencyScanRepository dependencyScanRepository;
    private final DependencyVulnerabilityRepository dependencyVulnerabilityRepository;
    private final HealthScheduleRepository healthScheduleRepository;
    private final HealthSnapshotRepository healthSnapshotRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final AuditLogRepository auditLogRepository;

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // Registry repositories
    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final ServiceDependencyRepository serviceDependencyRepository;
    private final SolutionRepository solutionRepository;
    private final SolutionMemberRepository solutionMemberRepository;
    private final PortAllocationRepository portAllocationRepository;
    private final PortRangeRepository portRangeRepository;
    private final ApiRouteRegistrationRepository apiRouteRegistrationRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final WorkstationProfileRepository workstationProfileRepository;
    private final InfraResourceRepository infraResourceRepository;

    // Logger repositories
    private final LogEntryRepository logEntryRepository;
    private final LogSourceRepository logSourceRepository;
    private final LogTrapRepository logTrapRepository;
    private final TrapConditionRepository trapConditionRepository;
    private final AlertChannelRepository alertChannelRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MetricRepository metricRepository;
    private final MetricSeriesRepository metricSeriesRepository;
    private final DashboardRepository dashboardRepository;
    private final DashboardWidgetRepository dashboardWidgetRepository;
    private final TraceSpanRepository traceSpanRepository;
    private final RetentionPolicyRepository retentionPolicyRepository;
    private final AnomalyBaselineRepository anomalyBaselineRepository;
    private final SavedQueryRepository savedQueryRepository;

    // Courier repositories
    private final CollectionRepository collectionRepository;
    private final FolderRepository folderRepository;
    private final RequestRepository requestRepository;
    private final EnvironmentRepository courierEnvironmentRepository;
    private final EnvironmentVariableRepository environmentVariableRepository;
    private final GlobalVariableRepository globalVariableRepository;

    // Shared references across seed methods
    private User adam, sarah, mike;
    private Team team;
    private Project serverProject, clientProject, locksmithProject;
    private List<Directive> directives;
    private QaJob auditJob1, complianceJob, bugJob, remediateJob, auditJob2, techDebtJob, depJob, runningJob;
    private List<Finding> savedFindings;
    private Specification apiSpec, markdownSpec;
    private DependencyScan serverScan, locksmithScan;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Core data already seeded — skipping core seed");
            // Load references needed by Registry seeder
            adam = userRepository.findByEmail("adam@allard.com").orElse(null);
            team = teamRepository.findAll().stream().findFirst().orElse(null);
        } else {
            seedUsers();
            seedTeam();
            seedTeamMembers();
            seedProjects();
            seedPersonas();
            seedDirectives();
            seedProjectDirectives();
            seedQaJobs();
            seedBugInvestigation();
            seedAgentRuns();
            seedFindings();
            seedRemediationTasks();
            seedSpecifications();
            seedComplianceItems();
            seedTechDebtItems();
            seedDependencyScans();
            seedDependencyVulnerabilities();
            seedHealthSchedules();
            seedHealthSnapshots();
            seedSystemSettings();
            seedAuditLog();
            log.info("Core development data seeded successfully");
        }

        seedRegistryData();
        seedLoggerData();
        seedCourierData();
    }

    private void seedUsers() {
        String hash = passwordEncoder.encode("pass");
        adam = userRepository.save(User.builder()
                .email("adam@allard.com").passwordHash(hash).displayName("Adam Allard").build());
        sarah = userRepository.save(User.builder()
                .email("sarah@codeops.dev").passwordHash(hash).displayName("Sarah Chen").build());
        mike = userRepository.save(User.builder()
                .email("mike@codeops.dev").passwordHash(hash).displayName("Mike Torres").build());
        log.info("Seeded 3 users");
    }

    private void seedTeam() {
        team = teamRepository.save(Team.builder()
                .name("CodeOps Core")
                .description("Core engineering team for CodeOps platform development")
                .owner(adam)
                .build());
        log.info("Seeded 1 team");
    }

    private void seedTeamMembers() {
        Instant now = Instant.now();
        teamMemberRepository.saveAll(List.of(
                TeamMember.builder().team(team).user(adam).role(TeamRole.OWNER)
                        .joinedAt(now.minus(90, ChronoUnit.DAYS)).build(),
                TeamMember.builder().team(team).user(sarah).role(TeamRole.ADMIN)
                        .joinedAt(now.minus(60, ChronoUnit.DAYS)).build(),
                TeamMember.builder().team(team).user(mike).role(TeamRole.MEMBER)
                        .joinedAt(now.minus(30, ChronoUnit.DAYS)).build()
        ));
        log.info("Seeded 3 team members");
    }

    private void seedProjects() {
        serverProject = projectRepository.save(Project.builder()
                .team(team).name("CodeOps Server")
                .description("Spring Boot backend API for the CodeOps platform")
                .repoUrl("https://github.com/codeops-dev/codeops-server")
                .repoFullName("codeops-dev/codeops-server")
                .techStack("Java 21, Spring Boot 3.3, PostgreSQL 16")
                .healthScore(78).createdBy(adam).build());
        clientProject = projectRepository.save(Project.builder()
                .team(team).name("CodeOps Client")
                .description("Flutter web application for the CodeOps platform")
                .repoUrl("https://github.com/codeops-dev/codeops-client")
                .repoFullName("codeops-dev/codeops-client")
                .techStack("Flutter 3.22, Dart 3.4, Riverpod")
                .healthScore(82).createdBy(adam).build());
        locksmithProject = projectRepository.save(Project.builder()
                .team(team).name("Locksmith Auth Service")
                .description("OAuth2/OIDC authentication microservice")
                .repoUrl("https://github.com/codeops-dev/locksmith")
                .repoFullName("codeops-dev/locksmith")
                .techStack("Go 1.22, Chi Router, PostgreSQL")
                .healthScore(65).createdBy(sarah).build());
        log.info("Seeded 3 projects");
    }

    private void seedPersonas() {
        personaRepository.saveAll(List.of(
                Persona.builder().name("Security Auditor").agentType(AgentType.SECURITY)
                        .description("Identifies security vulnerabilities and OWASP Top 10 risks")
                        .contentMd("# Security Auditor\nYou are a security-focused code reviewer. Analyze code for SQL injection, XSS, CSRF, authentication flaws, and insecure configurations. Reference OWASP guidelines.")
                        .scope(Scope.SYSTEM).createdBy(adam).isDefault(true).version(1).build(),
                Persona.builder().name("Code Quality Inspector").agentType(AgentType.CODE_QUALITY)
                        .description("Reviews code for maintainability, complexity, and best practices")
                        .contentMd("# Code Quality Inspector\nYou review code for cyclomatic complexity, code duplication, naming conventions, SOLID principles, and maintainability. Suggest concrete refactoring steps.")
                        .scope(Scope.SYSTEM).createdBy(adam).isDefault(true).version(1).build(),
                Persona.builder().name("Architecture Reviewer").agentType(AgentType.ARCHITECTURE)
                        .description("Evaluates system architecture and design patterns")
                        .contentMd("# Architecture Reviewer\nYou evaluate architectural decisions, dependency structure, module boundaries, and design patterns. Flag circular dependencies and layering violations.")
                        .scope(Scope.SYSTEM).createdBy(adam).isDefault(true).version(1).build(),
                Persona.builder().name("Test Coverage Analyst").agentType(AgentType.TEST_COVERAGE)
                        .description("Analyzes test coverage and testing strategy")
                        .contentMd("# Test Coverage Analyst\nYou analyze test suites for coverage gaps, flaky tests, missing edge cases, and testing strategy. Recommend specific test cases to add.")
                        .scope(Scope.SYSTEM).createdBy(adam).isDefault(true).version(1).build(),
                Persona.builder().name("Spring Boot Expert").agentType(AgentType.CODE_QUALITY)
                        .description("Specialized reviewer for Spring Boot applications")
                        .contentMd("# Spring Boot Expert\nYou are an expert in Spring Boot best practices. Review for proper use of dependency injection, transaction management, bean scoping, and Spring Security configuration.")
                        .scope(Scope.TEAM).team(team).createdBy(adam).version(1).build(),
                Persona.builder().name("Flutter Specialist").agentType(AgentType.CODE_QUALITY)
                        .description("Specialized reviewer for Flutter/Dart applications")
                        .contentMd("# Flutter Specialist\nYou are a Flutter and Dart expert. Review for proper state management with Riverpod, widget composition, performance anti-patterns, and platform-specific issues.")
                        .scope(Scope.TEAM).team(team).createdBy(sarah).version(1).build()
        ));
        log.info("Seeded 6 personas");
    }

    private void seedDirectives() {
        directives = directiveRepository.saveAll(List.of(
                Directive.builder().name("REST API Standards")
                        .description("Standard conventions for all REST API endpoints")
                        .contentMd("# REST API Standards\n- Use plural nouns for resources\n- Return 201 for successful creation\n- Use pagination for list endpoints\n- Include error codes in error responses\n- Version APIs via URL path prefix")
                        .category(DirectiveCategory.STANDARDS).scope(DirectiveScope.TEAM)
                        .team(team).createdBy(adam).version(1).build(),
                Directive.builder().name("Security Requirements")
                        .description("Mandatory security practices for all projects")
                        .contentMd("# Security Requirements\n- All endpoints must require authentication except health checks\n- Input validation on all request DTOs\n- SQL parameters must use prepared statements\n- Secrets must never appear in logs\n- CORS must be explicitly configured")
                        .category(DirectiveCategory.STANDARDS).scope(DirectiveScope.TEAM)
                        .team(team).createdBy(adam).version(1).build(),
                Directive.builder().name("Code Review Checklist")
                        .description("Standard checklist for code reviews")
                        .contentMd("# Code Review Checklist\n- No TODOs or commented-out code\n- All public methods have Javadoc\n- Error handling covers edge cases\n- No hardcoded credentials or magic numbers\n- Unit test coverage for new code >= 80%")
                        .category(DirectiveCategory.CONVENTIONS).scope(DirectiveScope.TEAM)
                        .team(team).createdBy(sarah).version(1).build(),
                Directive.builder().name("Architecture Context")
                        .description("High-level architecture description for AI agents")
                        .contentMd("# Architecture Context\nThis is a microservices platform with a Spring Boot API server, Flutter web client, and Go auth service. PostgreSQL is the primary database. Services communicate via REST APIs. Authentication uses JWT tokens.")
                        .category(DirectiveCategory.CONTEXT).scope(DirectiveScope.TEAM)
                        .team(team).createdBy(adam).version(1).build()
        ));
        log.info("Seeded 4 directives");
    }

    private void seedProjectDirectives() {
        Directive d0 = directives.get(0), d1 = directives.get(1);
        Directive d2 = directives.get(2), d3 = directives.get(3);
        projectDirectiveRepository.saveAll(List.of(
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(serverProject.getId(), d0.getId()))
                        .project(serverProject).directive(d0).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(serverProject.getId(), d1.getId()))
                        .project(serverProject).directive(d1).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(serverProject.getId(), d3.getId()))
                        .project(serverProject).directive(d3).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(clientProject.getId(), d1.getId()))
                        .project(clientProject).directive(d1).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(clientProject.getId(), d2.getId()))
                        .project(clientProject).directive(d2).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(locksmithProject.getId(), d1.getId()))
                        .project(locksmithProject).directive(d1).enabled(true).build()
        ));
        log.info("Seeded 6 project directives");
    }

    private void seedQaJobs() {
        Instant now = Instant.now();
        auditJob1 = qaJobRepository.save(QaJob.builder()
                .project(serverProject).mode(JobMode.AUDIT).status(JobStatus.COMPLETED)
                .name("Server Full Audit").branch("main")
                .overallResult(JobResult.WARN).healthScore(78)
                .totalFindings(8).criticalCount(2).highCount(2).mediumCount(2).lowCount(2)
                .startedBy(adam).startedAt(now.minus(7, ChronoUnit.DAYS))
                .completedAt(now.minus(7, ChronoUnit.DAYS).plus(12, ChronoUnit.MINUTES)).build());
        complianceJob = qaJobRepository.save(QaJob.builder()
                .project(serverProject).mode(JobMode.COMPLIANCE).status(JobStatus.COMPLETED)
                .name("API Contract Compliance").branch("main")
                .overallResult(JobResult.WARN).healthScore(85)
                .totalFindings(2).highCount(1).mediumCount(1)
                .startedBy(sarah).startedAt(now.minus(5, ChronoUnit.DAYS))
                .completedAt(now.minus(5, ChronoUnit.DAYS).plus(8, ChronoUnit.MINUTES)).build());
        bugJob = qaJobRepository.save(QaJob.builder()
                .project(serverProject).mode(JobMode.BUG_INVESTIGATE).status(JobStatus.COMPLETED)
                .name("Auth Token Expiry Bug").branch("fix/token-refresh")
                .overallResult(JobResult.PASS).healthScore(90)
                .totalFindings(3).highCount(1).mediumCount(2)
                .jiraTicketKey("COD-142")
                .startedBy(mike).startedAt(now.minus(4, ChronoUnit.DAYS))
                .completedAt(now.minus(4, ChronoUnit.DAYS).plus(15, ChronoUnit.MINUTES)).build());
        remediateJob = qaJobRepository.save(QaJob.builder()
                .project(serverProject).mode(JobMode.REMEDIATE).status(JobStatus.COMPLETED)
                .name("Security Findings Remediation").branch("main")
                .overallResult(JobResult.PASS)
                .totalFindings(3).highCount(1).mediumCount(2)
                .startedBy(adam).startedAt(now.minus(3, ChronoUnit.DAYS))
                .completedAt(now.minus(3, ChronoUnit.DAYS).plus(20, ChronoUnit.MINUTES)).build());
        auditJob2 = qaJobRepository.save(QaJob.builder()
                .project(clientProject).mode(JobMode.AUDIT).status(JobStatus.COMPLETED)
                .name("Client Full Audit").branch("main")
                .overallResult(JobResult.WARN).healthScore(82)
                .totalFindings(4).highCount(1).mediumCount(2).lowCount(1)
                .startedBy(sarah).startedAt(now.minus(6, ChronoUnit.DAYS))
                .completedAt(now.minus(6, ChronoUnit.DAYS).plus(10, ChronoUnit.MINUTES)).build());
        techDebtJob = qaJobRepository.save(QaJob.builder()
                .project(clientProject).mode(JobMode.TECH_DEBT).status(JobStatus.COMPLETED)
                .name("Client Tech Debt Analysis").branch("main")
                .overallResult(JobResult.WARN)
                .totalFindings(2).mediumCount(1).lowCount(1)
                .startedBy(sarah).startedAt(now.minus(2, ChronoUnit.DAYS))
                .completedAt(now.minus(2, ChronoUnit.DAYS).plus(7, ChronoUnit.MINUTES)).build());
        depJob = qaJobRepository.save(QaJob.builder()
                .project(locksmithProject).mode(JobMode.DEPENDENCY).status(JobStatus.COMPLETED)
                .name("Locksmith Dependency Scan").branch("main")
                .overallResult(JobResult.FAIL).healthScore(65)
                .totalFindings(2).criticalCount(1).highCount(1)
                .startedBy(adam).startedAt(now.minus(1, ChronoUnit.DAYS))
                .completedAt(now.minus(1, ChronoUnit.DAYS).plus(5, ChronoUnit.MINUTES)).build());
        runningJob = qaJobRepository.save(QaJob.builder()
                .project(locksmithProject).mode(JobMode.AUDIT).status(JobStatus.RUNNING)
                .name("Locksmith Security Audit").branch("main")
                .startedBy(mike).startedAt(now.minus(10, ChronoUnit.MINUTES)).build());
        log.info("Seeded 8 QA jobs");
    }

    private void seedBugInvestigation() {
        bugInvestigationRepository.save(BugInvestigation.builder()
                .job(bugJob).jiraKey("COD-142")
                .jiraSummary("JWT refresh token not extending session correctly")
                .jiraDescription("Users are being logged out after 24 hours even though they have a valid refresh token. The refresh endpoint returns a new access token but the expiry is not being updated in the client.")
                .rcaMd("## Root Cause Analysis\n\nThe `refreshToken` endpoint in `AuthService.java` generates a new access token but copies the original `issuedAt` claim from the old token. This means the new token's expiry is calculated from the original login time, not from the refresh time.\n\n### Fix\nUpdate `JwtTokenProvider.refreshAccessToken()` to use `Instant.now()` as the `issuedAt` for the new token.")
                .impactAssessmentMd("## Impact Assessment\n\n**Severity:** HIGH\n**Affected Users:** All users with sessions >24h\n**Data Loss Risk:** None — this is a session management issue\n\n### Mitigation\nUsers can work around this by logging out and back in. No data is lost.")
                .build());
        log.info("Seeded 1 bug investigation");
    }

    private void seedAgentRuns() {
        agentRunRepository.saveAll(List.of(
                // auditJob1: SECURITY, CODE_QUALITY, ARCHITECTURE
                AgentRun.builder().job(auditJob1).agentType(AgentType.SECURITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(72).findingsCount(3).criticalCount(1).highCount(1)
                        .startedAt(auditJob1.getStartedAt())
                        .completedAt(auditJob1.getStartedAt().plus(4, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(auditJob1).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(80).findingsCount(3).criticalCount(1).highCount(1)
                        .startedAt(auditJob1.getStartedAt())
                        .completedAt(auditJob1.getStartedAt().plus(5, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(auditJob1).agentType(AgentType.ARCHITECTURE).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(88).findingsCount(2)
                        .startedAt(auditJob1.getStartedAt())
                        .completedAt(auditJob1.getStartedAt().plus(6, ChronoUnit.MINUTES)).build(),
                // complianceJob: API_CONTRACT, COMPLETENESS
                AgentRun.builder().job(complianceJob).agentType(AgentType.API_CONTRACT).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(85).findingsCount(1).highCount(1)
                        .startedAt(complianceJob.getStartedAt())
                        .completedAt(complianceJob.getStartedAt().plus(3, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(complianceJob).agentType(AgentType.COMPLETENESS).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(90).findingsCount(1)
                        .startedAt(complianceJob.getStartedAt())
                        .completedAt(complianceJob.getStartedAt().plus(4, ChronoUnit.MINUTES)).build(),
                // bugJob: SECURITY, CODE_QUALITY
                AgentRun.builder().job(bugJob).agentType(AgentType.SECURITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(92).findingsCount(1).highCount(1)
                        .startedAt(bugJob.getStartedAt())
                        .completedAt(bugJob.getStartedAt().plus(5, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(bugJob).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(88).findingsCount(2)
                        .startedAt(bugJob.getStartedAt())
                        .completedAt(bugJob.getStartedAt().plus(6, ChronoUnit.MINUTES)).build(),
                // remediateJob: CODE_QUALITY, SECURITY
                AgentRun.builder().job(remediateJob).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(90).findingsCount(2)
                        .startedAt(remediateJob.getStartedAt())
                        .completedAt(remediateJob.getStartedAt().plus(8, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(remediateJob).agentType(AgentType.SECURITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(95).findingsCount(1)
                        .startedAt(remediateJob.getStartedAt())
                        .completedAt(remediateJob.getStartedAt().plus(10, ChronoUnit.MINUTES)).build(),
                // auditJob2: SECURITY, CODE_QUALITY, TEST_COVERAGE
                AgentRun.builder().job(auditJob2).agentType(AgentType.SECURITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(90).findingsCount(1).highCount(1)
                        .startedAt(auditJob2.getStartedAt())
                        .completedAt(auditJob2.getStartedAt().plus(3, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(auditJob2).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(78).findingsCount(2)
                        .startedAt(auditJob2.getStartedAt())
                        .completedAt(auditJob2.getStartedAt().plus(4, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(auditJob2).agentType(AgentType.TEST_COVERAGE).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(65).findingsCount(1)
                        .startedAt(auditJob2.getStartedAt())
                        .completedAt(auditJob2.getStartedAt().plus(5, ChronoUnit.MINUTES)).build(),
                // techDebtJob: ARCHITECTURE, CODE_QUALITY
                AgentRun.builder().job(techDebtJob).agentType(AgentType.ARCHITECTURE).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(75).findingsCount(1)
                        .startedAt(techDebtJob.getStartedAt())
                        .completedAt(techDebtJob.getStartedAt().plus(3, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(techDebtJob).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(82).findingsCount(1)
                        .startedAt(techDebtJob.getStartedAt())
                        .completedAt(techDebtJob.getStartedAt().plus(4, ChronoUnit.MINUTES)).build(),
                // depJob: DEPENDENCY, BUILD_HEALTH
                AgentRun.builder().job(depJob).agentType(AgentType.DEPENDENCY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.FAIL).score(45).findingsCount(2).criticalCount(1).highCount(1)
                        .startedAt(depJob.getStartedAt())
                        .completedAt(depJob.getStartedAt().plus(2, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(depJob).agentType(AgentType.BUILD_HEALTH).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(88)
                        .startedAt(depJob.getStartedAt())
                        .completedAt(depJob.getStartedAt().plus(3, ChronoUnit.MINUTES)).build(),
                // runningJob: SECURITY (running), CODE_QUALITY (pending)
                AgentRun.builder().job(runningJob).agentType(AgentType.SECURITY).status(AgentStatus.RUNNING)
                        .startedAt(runningJob.getStartedAt()).build(),
                AgentRun.builder().job(runningJob).agentType(AgentType.CODE_QUALITY).status(AgentStatus.PENDING).build()
        ));
        log.info("Seeded 18 agent runs");
    }

    private void seedFindings() {
        Instant now = Instant.now();
        List<Finding> findings = new ArrayList<>();

        // auditJob1: 8 findings [indices 0-7]
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.SECURITY).severity(Severity.CRITICAL)
                .title("SQL Injection in search endpoint")
                .description("The /api/v1/projects/search endpoint concatenates user input directly into a SQL query string without parameterization.")
                .filePath("src/main/java/com/codeops/repository/CustomProjectRepository.java").lineNumber(45)
                .recommendation("Use parameterized queries or Spring Data JPA @Query with named parameters.")
                .evidence("String query = \"SELECT * FROM projects WHERE name LIKE '%\" + searchTerm + \"%'\";")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.SECURITY).severity(Severity.CRITICAL)
                .title("Hardcoded JWT secret in application.yml")
                .description("The JWT signing secret is hardcoded as a default value in application-dev.yml and could leak to production if environment variable is not set.")
                .filePath("src/main/resources/application-dev.yml").lineNumber(23)
                .recommendation("Remove default secret value and fail fast if JWT_SECRET env var is not set in production profile.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.SECURITY).severity(Severity.HIGH)
                .title("Missing rate limiting on login endpoint")
                .description("The POST /api/v1/auth/login endpoint has no rate limiting, making it vulnerable to brute-force password attacks.")
                .filePath("src/main/java/com/codeops/controller/AuthController.java").lineNumber(32)
                .recommendation("Add rate limiting using a Redis-based sliding window counter.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.CODE_QUALITY).severity(Severity.HIGH)
                .title("God class: ProjectService has 42 methods")
                .description("ProjectService.java has grown to 42 public methods and 1,200 lines, violating the Single Responsibility Principle.")
                .filePath("src/main/java/com/codeops/service/ProjectService.java").lineNumber(1)
                .recommendation("Extract related methods into focused services: ProjectQueryService, ProjectCommandService.")
                .effortEstimate(Effort.L).debtCategory(DebtCategory.ARCHITECTURE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Duplicate error handling logic across controllers")
                .description("Exception handling try-catch blocks are duplicated in 12 controllers instead of using the global exception handler.")
                .filePath("src/main/java/com/codeops/controller/QaJobController.java").lineNumber(87)
                .recommendation("Remove try-catch blocks from controllers and let GlobalExceptionHandler manage all exceptions.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("N+1 query in job listing endpoint")
                .description("GET /api/v1/jobs loads each job's project relationship lazily, causing N+1 queries when listing jobs.")
                .filePath("src/main/java/com/codeops/service/QaJobService.java").lineNumber(65)
                .recommendation("Use @EntityGraph or a JOIN FETCH query in the repository method.")
                .evidence("Hibernate logs show 51 SELECT queries for a page of 50 jobs")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.ARCHITECTURE).severity(Severity.LOW)
                .title("Unused NotificationPreference entity")
                .description("The NotificationPreference entity and repository exist but are never referenced by any service or controller.")
                .filePath("src/main/java/com/codeops/entity/NotificationPreference.java").lineNumber(1)
                .recommendation("Either implement notification preferences or remove the unused entity.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.ARCHITECTURE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.ARCHITECTURE).severity(Severity.LOW)
                .title("Inconsistent DTO naming convention")
                .description("Some DTOs use Request/Response suffix while others use Create/Update prefix.")
                .filePath("src/main/java/com/codeops/dto/request/").lineNumber(0)
                .recommendation("Standardize on one naming convention across all DTOs.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());

        // complianceJob: 2 findings [indices 8-9]
        findings.add(Finding.builder().job(complianceJob).agentType(AgentType.API_CONTRACT).severity(Severity.HIGH)
                .title("Missing pagination on GET /api/v1/findings endpoint")
                .description("The API spec requires pagination for all list endpoints, but GET /api/v1/findings returns all findings without pagination.")
                .filePath("src/main/java/com/codeops/controller/FindingController.java").lineNumber(28)
                .recommendation("Add Pageable parameter and return PageResponse<FindingResponse>.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(complianceJob).agentType(AgentType.COMPLETENESS).severity(Severity.MEDIUM)
                .title("OpenAPI spec missing error response schemas")
                .description("15 endpoints are missing 400/401/403/404 error response definitions in the OpenAPI documentation.")
                .filePath("src/main/java/com/codeops/controller/").lineNumber(0)
                .recommendation("Add @ApiResponse annotations with error schema definitions to all controller methods.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.DOCUMENTATION).build());

        // bugJob: 3 findings [indices 10-12]
        findings.add(Finding.builder().job(bugJob).agentType(AgentType.SECURITY).severity(Severity.HIGH)
                .title("Token refresh reuses original issuedAt timestamp")
                .description("JwtTokenProvider.refreshAccessToken() copies the original issuedAt claim, causing refreshed tokens to expire based on original login time.")
                .filePath("src/main/java/com/codeops/security/JwtTokenProvider.java").lineNumber(89)
                .recommendation("Use Instant.now() for the issuedAt claim in refreshed tokens.")
                .evidence("Token generated at login: iat=1704067200, refreshed token: iat=1704067200 (same)")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(bugJob).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Missing null check in refresh token flow")
                .description("AuthService.refreshToken() does not check if the user account is still active before issuing a new token.")
                .filePath("src/main/java/com/codeops/service/AuthService.java").lineNumber(72)
                .recommendation("Add isActive check after loading user from refresh token claims.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(bugJob).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Refresh token endpoint returns 500 on expired token")
                .description("When a refresh token is expired, the endpoint returns 500 instead of 401 Unauthorized.")
                .filePath("src/main/java/com/codeops/controller/AuthController.java").lineNumber(55)
                .recommendation("Catch ExpiredJwtException and return 401 with a descriptive message.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());

        // remediateJob: 3 findings (FIXED) [indices 13-15]
        findings.add(Finding.builder().job(remediateJob).agentType(AgentType.SECURITY).severity(Severity.HIGH)
                .title("CORS allows wildcard origin in dev")
                .description("CorsConfig sets Access-Control-Allow-Origin to * which would be dangerous if deployed without proper configuration.")
                .filePath("src/main/java/com/codeops/config/CorsConfig.java").lineNumber(15)
                .recommendation("Use explicit allowed origins from configuration.")
                .status(FindingStatus.FIXED).statusChangedBy(adam).statusChangedAt(now.minus(3, ChronoUnit.DAYS))
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(remediateJob).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Unencrypted sensitive data in audit log details")
                .description("AuditLogService logs full request bodies including potentially sensitive fields like passwords and tokens.")
                .filePath("src/main/java/com/codeops/service/AuditLogService.java").lineNumber(34)
                .recommendation("Implement a sanitizer that masks sensitive fields before logging.")
                .status(FindingStatus.FIXED).statusChangedBy(adam).statusChangedAt(now.minus(3, ChronoUnit.DAYS))
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(remediateJob).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Stack trace exposed in production error responses")
                .description("GlobalExceptionHandler includes stack traces in error response bodies regardless of environment.")
                .filePath("src/main/java/com/codeops/config/GlobalExceptionHandler.java").lineNumber(28)
                .recommendation("Only include stack traces when spring.profiles.active=dev.")
                .status(FindingStatus.FIXED).statusChangedBy(adam).statusChangedAt(now.minus(3, ChronoUnit.DAYS))
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());

        // auditJob2: 4 findings [indices 16-19]
        findings.add(Finding.builder().job(auditJob2).agentType(AgentType.SECURITY).severity(Severity.HIGH)
                .title("API token stored in localStorage")
                .description("The Flutter web app stores JWT access tokens in localStorage which is vulnerable to XSS attacks.")
                .filePath("lib/core/storage/token_storage.dart").lineNumber(12)
                .recommendation("Use secure HTTP-only cookies or flutter_secure_storage for token persistence.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob2).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Excessive widget rebuilds in job list")
                .description("JobListScreen rebuilds the entire widget tree on every state change because it watches the full job list provider.")
                .filePath("lib/features/jobs/screens/job_list_screen.dart").lineNumber(45)
                .recommendation("Use select() to watch only the specific fields needed.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob2).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Missing error boundaries in widget tree")
                .description("No ErrorWidget.builder configured and no error handling widgets around async operations in 8 screens.")
                .filePath("lib/main.dart").lineNumber(1)
                .recommendation("Add ErrorBoundary widgets and configure ErrorWidget.builder for graceful error display.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob2).agentType(AgentType.TEST_COVERAGE).severity(Severity.LOW)
                .title("No widget tests for core UI components")
                .description("The shared/widgets/ directory contains 15 reusable components with zero widget tests.")
                .filePath("test/").lineNumber(0)
                .recommendation("Add widget tests for AppShell, SideNav, DataTable, and other core components.")
                .effortEstimate(Effort.L).debtCategory(DebtCategory.TEST).build());

        // techDebtJob: 2 findings [indices 20-21]
        findings.add(Finding.builder().job(techDebtJob).agentType(AgentType.ARCHITECTURE).severity(Severity.MEDIUM)
                .title("Circular dependency between features/auth and core/router")
                .description("The auth feature imports from core/router for navigation, while core/router imports from features/auth for guard logic.")
                .filePath("lib/features/auth/providers/auth_provider.dart").lineNumber(5)
                .recommendation("Extract auth state interface to core/ and have both depend on the abstraction.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.ARCHITECTURE).build());
        findings.add(Finding.builder().job(techDebtJob).agentType(AgentType.CODE_QUALITY).severity(Severity.LOW)
                .title("Deprecated Flutter APIs used in 6 files")
                .description("Several widgets use deprecated APIs: WillPopScope (use PopScope), FlatButton (use TextButton).")
                .filePath("lib/shared/widgets/").lineNumber(0)
                .recommendation("Replace deprecated APIs with their modern equivalents.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());

        // depJob: 2 findings [indices 22-23]
        findings.add(Finding.builder().job(depJob).agentType(AgentType.DEPENDENCY).severity(Severity.CRITICAL)
                .title("Critical CVE in golang.org/x/crypto")
                .description("golang.org/x/crypto v0.17.0 has CVE-2024-45337 — SSH connection bypass allowing unauthorized access.")
                .filePath("go.mod").lineNumber(8)
                .recommendation("Upgrade to golang.org/x/crypto v0.31.0 or later.")
                .evidence("CVE-2024-45337: CVSS 9.1")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.DEPENDENCY).build());
        findings.add(Finding.builder().job(depJob).agentType(AgentType.DEPENDENCY).severity(Severity.HIGH)
                .title("Outdated chi router with known vulnerability")
                .description("go-chi/chi v5.0.10 has a path traversal vulnerability in static file serving middleware.")
                .filePath("go.mod").lineNumber(12)
                .recommendation("Upgrade to go-chi/chi v5.0.12 or later.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.DEPENDENCY).build());

        savedFindings = findingRepository.saveAll(findings);
        log.info("Seeded 24 findings");
    }

    private void seedRemediationTasks() {
        remediationTaskRepository.saveAll(List.of(
                RemediationTask.builder().job(auditJob1).taskNumber(1)
                        .title("Fix SQL injection in project search")
                        .description("Replace string concatenation with parameterized query in CustomProjectRepository.")
                        .promptMd("Fix the SQL injection vulnerability by replacing the raw SQL string concatenation with a parameterized @Query annotation.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(0))))
                        .priority(Priority.P0).status(TaskStatus.EXPORTED).assignedTo(adam).build(),
                RemediationTask.builder().job(auditJob1).taskNumber(2)
                        .title("Remove hardcoded JWT secret default")
                        .description("Remove the default JWT secret from application-dev.yml and add startup validation in production profile.")
                        .promptMd("Remove the hardcoded default secret and add a @PostConstruct validator that fails if JWT_SECRET is not set in prod.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(1))))
                        .priority(Priority.P0).status(TaskStatus.COMPLETED).assignedTo(adam).build(),
                RemediationTask.builder().job(auditJob1).taskNumber(3)
                        .title("Add rate limiting to auth endpoints")
                        .description("Implement rate limiting on login and refresh endpoints to prevent brute-force attacks.")
                        .promptMd("Add a Redis-based rate limiter that limits login attempts to 5 per minute per IP address.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(2))))
                        .priority(Priority.P1).status(TaskStatus.ASSIGNED).assignedTo(mike).build(),
                RemediationTask.builder().job(auditJob1).taskNumber(4)
                        .title("Refactor ProjectService and fix N+1 queries")
                        .description("Split ProjectService into focused services and add JOIN FETCH for job listing.")
                        .promptMd("Extract ProjectService methods into ProjectQueryService and ProjectCommandService. Add @EntityGraph to job listing repository method.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(3), savedFindings.get(5))))
                        .priority(Priority.P2).status(TaskStatus.PENDING).build(),
                RemediationTask.builder().job(remediateJob).taskNumber(1)
                        .title("Fix CORS configuration")
                        .description("Replace wildcard CORS origin with explicit allowed origins from configuration.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(13))))
                        .priority(Priority.P1).status(TaskStatus.COMPLETED).assignedTo(adam).build(),
                RemediationTask.builder().job(remediateJob).taskNumber(2)
                        .title("Sanitize audit log data and hide stack traces")
                        .description("Add field sanitizer to AuditLogService and make stack traces conditional on dev profile.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(14), savedFindings.get(15))))
                        .priority(Priority.P1).status(TaskStatus.COMPLETED).assignedTo(adam).build(),
                RemediationTask.builder().job(auditJob2).taskNumber(1)
                        .title("Migrate token storage to secure cookies")
                        .description("Replace localStorage token storage with secure HTTP-only cookies for XSS protection.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(16))))
                        .priority(Priority.P0).status(TaskStatus.PENDING).build(),
                RemediationTask.builder().job(auditJob2).taskNumber(2)
                        .title("Optimize widget rebuilds and add error boundaries")
                        .description("Add select() to providers and implement error boundary widgets.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(17), savedFindings.get(18))))
                        .priority(Priority.P2).status(TaskStatus.PENDING).build()
        ));
        log.info("Seeded 8 remediation tasks");
    }

    private void seedSpecifications() {
        List<Specification> specs = specificationRepository.saveAll(List.of(
                Specification.builder().job(complianceJob)
                        .name("CodeOps API v1 OpenAPI Spec").specType(SpecType.OPENAPI)
                        .s3Key("specs/codeops-api-v1.yaml").build(),
                Specification.builder().job(complianceJob)
                        .name("API Design Guidelines").specType(SpecType.MARKDOWN)
                        .s3Key("specs/api-design-guidelines.md").build()
        ));
        apiSpec = specs.get(0);
        markdownSpec = specs.get(1);
        log.info("Seeded 2 specifications");
    }

    private void seedComplianceItems() {
        complianceItemRepository.saveAll(List.of(
                ComplianceItem.builder().job(complianceJob)
                        .requirement("All list endpoints must support pagination with page and size parameters")
                        .spec(apiSpec).status(ComplianceStatus.PARTIAL).agentType(AgentType.API_CONTRACT)
                        .evidence("22 of 28 list endpoints support pagination. 6 endpoints return unpaginated lists.")
                        .notes("Failing: GET /findings, GET /agent-runs, GET /directives, GET /personas, GET /compliance-items, GET /specifications").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("All endpoints must return consistent error response format with code and message")
                        .spec(apiSpec).status(ComplianceStatus.MET).agentType(AgentType.API_CONTRACT)
                        .evidence("GlobalExceptionHandler produces consistent ErrorResponse record for all exception types.").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("All mutation endpoints must require authentication")
                        .spec(markdownSpec).status(ComplianceStatus.MET).agentType(AgentType.API_CONTRACT)
                        .evidence("SecurityConfig requires authentication for all endpoints except /auth/**, /health, and Swagger UI.").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("Request DTOs must use Jakarta Validation annotations")
                        .spec(markdownSpec).status(ComplianceStatus.PARTIAL).agentType(AgentType.COMPLETENESS)
                        .evidence("28 of 35 request DTOs use validation annotations. 7 DTOs have no validation.")
                        .notes("Missing: UpdateProjectRequest, CreateDirectiveRequest, UpdatePersonaRequest, and 4 others").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("All endpoints must be documented with OpenAPI annotations")
                        .spec(apiSpec).status(ComplianceStatus.MISSING).agentType(AgentType.COMPLETENESS)
                        .evidence("Only 5 of 18 controllers have @Operation annotations. Most endpoints rely on auto-generated docs.")
                        .notes("Priority: AuthController, QaJobController, FindingController").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("Health check endpoint must return service dependencies status")
                        .spec(markdownSpec).status(ComplianceStatus.MET).agentType(AgentType.API_CONTRACT)
                        .evidence("GET /health returns {status, database, timestamp} with actual DB connectivity check.").build()
        ));
        log.info("Seeded 6 compliance items");
    }

    private void seedTechDebtItems() {
        techDebtItemRepository.saveAll(List.of(
                TechDebtItem.builder().project(serverProject).category(DebtCategory.ARCHITECTURE)
                        .title("No database migration tool — relying on Hibernate ddl-auto")
                        .description("Using hibernate.ddl-auto=update in all environments. This is dangerous for production and doesn't support rollbacks.")
                        .effortEstimate(Effort.L).businessImpact(BusinessImpact.HIGH)
                        .status(DebtStatus.PLANNED).firstDetectedJob(auditJob1).build(),
                TechDebtItem.builder().project(serverProject).category(DebtCategory.TEST)
                        .title("Integration test coverage below 20%")
                        .description("Only 4 integration tests exist. Most services and controllers have no integration test coverage.")
                        .effortEstimate(Effort.XL).businessImpact(BusinessImpact.HIGH)
                        .status(DebtStatus.IDENTIFIED).firstDetectedJob(auditJob1).build(),
                TechDebtItem.builder().project(serverProject).category(DebtCategory.CODE)
                        .title("Inconsistent error response format across controllers")
                        .description("Some controllers return custom error maps while others rely on GlobalExceptionHandler. Should standardize.")
                        .effortEstimate(Effort.M).businessImpact(BusinessImpact.MEDIUM)
                        .status(DebtStatus.IN_PROGRESS).firstDetectedJob(auditJob1).build(),
                TechDebtItem.builder().project(clientProject).category(DebtCategory.ARCHITECTURE)
                        .title("No offline support or caching strategy")
                        .description("The Flutter client makes fresh API calls on every navigation. No data caching or offline fallback exists.")
                        .effortEstimate(Effort.XL).businessImpact(BusinessImpact.MEDIUM)
                        .status(DebtStatus.IDENTIFIED).firstDetectedJob(auditJob2).build(),
                TechDebtItem.builder().project(clientProject).category(DebtCategory.CODE)
                        .title("Riverpod providers not using code generation")
                        .description("Half of the providers are manually written instead of using @riverpod code generation, leading to inconsistency.")
                        .effortEstimate(Effort.M).businessImpact(BusinessImpact.LOW)
                        .status(DebtStatus.PLANNED).firstDetectedJob(techDebtJob).build(),
                TechDebtItem.builder().project(clientProject).category(DebtCategory.DOCUMENTATION)
                        .title("No component storybook or design system documentation")
                        .description("Shared widgets have no visual documentation. New developers must read source code to understand available components.")
                        .effortEstimate(Effort.L).businessImpact(BusinessImpact.LOW)
                        .status(DebtStatus.IDENTIFIED).firstDetectedJob(techDebtJob).build(),
                TechDebtItem.builder().project(locksmithProject).category(DebtCategory.DEPENDENCY)
                        .title("Multiple Go dependencies 2+ major versions behind")
                        .description("golang.org/x/crypto, go-chi/chi, and pgx are all behind latest major versions with known CVEs.")
                        .effortEstimate(Effort.M).businessImpact(BusinessImpact.CRITICAL)
                        .status(DebtStatus.IN_PROGRESS).firstDetectedJob(depJob).build(),
                TechDebtItem.builder().project(locksmithProject).category(DebtCategory.TEST)
                        .title("No end-to-end test for OAuth2 flow")
                        .description("The complete OAuth2 authorization code flow has no automated test. Only unit tests for individual handlers exist.")
                        .effortEstimate(Effort.L).businessImpact(BusinessImpact.HIGH)
                        .status(DebtStatus.IDENTIFIED).firstDetectedJob(depJob).build()
        ));
        log.info("Seeded 8 tech debt items");
    }

    private void seedDependencyScans() {
        serverScan = dependencyScanRepository.save(DependencyScan.builder()
                .project(serverProject).manifestFile("pom.xml")
                .totalDependencies(34).outdatedCount(8).vulnerableCount(2).build());
        locksmithScan = dependencyScanRepository.save(DependencyScan.builder()
                .project(locksmithProject).job(depJob).manifestFile("go.mod")
                .totalDependencies(22).outdatedCount(6).vulnerableCount(5).build());
        log.info("Seeded 2 dependency scans");
    }

    private void seedDependencyVulnerabilities() {
        dependencyVulnerabilityRepository.saveAll(List.of(
                DependencyVulnerability.builder().scan(serverScan)
                        .dependencyName("org.postgresql:postgresql").currentVersion("42.7.2").fixedVersion("42.7.4")
                        .cveId("CVE-2024-1597").severity(Severity.HIGH)
                        .description("SQL injection via line comment generation in PostgreSQL JDBC driver.").build(),
                DependencyVulnerability.builder().scan(serverScan)
                        .dependencyName("io.jsonwebtoken:jjwt-impl").currentVersion("0.12.3").fixedVersion("0.12.6")
                        .severity(Severity.MEDIUM)
                        .description("Potential key confusion attack when using RSA with HMAC verification.")
                        .status(VulnerabilityStatus.RESOLVED).build(),
                DependencyVulnerability.builder().scan(serverScan)
                        .dependencyName("org.springdoc:springdoc-openapi-starter-webmvc-ui").currentVersion("2.3.0").fixedVersion("2.5.0")
                        .severity(Severity.LOW)
                        .description("XSS via crafted OpenAPI schema definitions in Swagger UI.")
                        .status(VulnerabilityStatus.UPDATING).build(),
                DependencyVulnerability.builder().scan(locksmithScan)
                        .dependencyName("golang.org/x/crypto").currentVersion("0.17.0").fixedVersion("0.31.0")
                        .cveId("CVE-2024-45337").severity(Severity.CRITICAL)
                        .description("SSH connection bypass allowing unauthorized access via crafted authentication request.").build(),
                DependencyVulnerability.builder().scan(locksmithScan)
                        .dependencyName("github.com/go-chi/chi/v5").currentVersion("5.0.10").fixedVersion("5.0.12")
                        .severity(Severity.HIGH)
                        .description("Path traversal vulnerability in chi.FileServer static file serving middleware.").build(),
                DependencyVulnerability.builder().scan(locksmithScan)
                        .dependencyName("github.com/jackc/pgx/v5").currentVersion("5.5.0").fixedVersion("5.5.4")
                        .cveId("CVE-2024-27304").severity(Severity.HIGH)
                        .description("SQL injection via crafted connection string parameters.").build(),
                DependencyVulnerability.builder().scan(locksmithScan)
                        .dependencyName("golang.org/x/net").currentVersion("0.19.0").fixedVersion("0.23.0")
                        .cveId("CVE-2024-24790").severity(Severity.MEDIUM)
                        .description("HTTP/2 CONTINUATION frames flood causing denial of service.").build()
        ));
        log.info("Seeded 7 dependency vulnerabilities");
    }

    private void seedHealthSchedules() {
        Instant now = Instant.now();
        healthScheduleRepository.saveAll(List.of(
                HealthSchedule.builder().project(serverProject).scheduleType(ScheduleType.DAILY)
                        .cronExpression("0 0 6 * * *")
                        .agentTypes(toJson(List.of("SECURITY", "CODE_QUALITY", "ARCHITECTURE")))
                        .createdBy(adam).nextRunAt(now.plus(1, ChronoUnit.DAYS)).build(),
                HealthSchedule.builder().project(clientProject).scheduleType(ScheduleType.WEEKLY)
                        .cronExpression("0 0 6 * * MON")
                        .agentTypes(toJson(List.of("SECURITY", "CODE_QUALITY", "TEST_COVERAGE")))
                        .createdBy(sarah).nextRunAt(now.plus(7, ChronoUnit.DAYS)).build(),
                HealthSchedule.builder().project(locksmithProject).scheduleType(ScheduleType.ON_COMMIT)
                        .agentTypes(toJson(List.of("SECURITY", "DEPENDENCY")))
                        .createdBy(adam).build()
        ));
        log.info("Seeded 3 health schedules");
    }

    private void seedHealthSnapshots() {
        Instant now = Instant.now();
        healthSnapshotRepository.saveAll(List.of(
                // serverProject
                HealthSnapshot.builder().project(serverProject).job(auditJob1).healthScore(72)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 2, "HIGH", 2, "MEDIUM", 2, "LOW", 2)))
                        .techDebtScore(55).dependencyScore(70).testCoveragePercent(new BigDecimal("42.5"))
                        .capturedAt(now.minus(14, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(serverProject).healthScore(75)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 1, "HIGH", 2, "MEDIUM", 3, "LOW", 2)))
                        .techDebtScore(58).dependencyScore(72).testCoveragePercent(new BigDecimal("45.0"))
                        .capturedAt(now.minus(7, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(serverProject).healthScore(78)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 0, "HIGH", 1, "MEDIUM", 3, "LOW", 2)))
                        .techDebtScore(60).dependencyScore(75).testCoveragePercent(new BigDecimal("48.2"))
                        .capturedAt(now.minus(1, ChronoUnit.DAYS)).build(),
                // clientProject
                HealthSnapshot.builder().project(clientProject).job(auditJob2).healthScore(78)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 0, "HIGH", 1, "MEDIUM", 2, "LOW", 1)))
                        .techDebtScore(65).dependencyScore(88).testCoveragePercent(new BigDecimal("35.0"))
                        .capturedAt(now.minus(14, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(clientProject).healthScore(80)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 0, "HIGH", 1, "MEDIUM", 1, "LOW", 1)))
                        .techDebtScore(68).dependencyScore(90).testCoveragePercent(new BigDecimal("38.5"))
                        .capturedAt(now.minus(7, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(clientProject).healthScore(82)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 0, "HIGH", 0, "MEDIUM", 2, "LOW", 1)))
                        .techDebtScore(70).dependencyScore(90).testCoveragePercent(new BigDecimal("41.0"))
                        .capturedAt(now.minus(1, ChronoUnit.DAYS)).build(),
                // locksmithProject
                HealthSnapshot.builder().project(locksmithProject).healthScore(58)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 2, "HIGH", 3, "MEDIUM", 1, "LOW", 0)))
                        .techDebtScore(40).dependencyScore(35).testCoveragePercent(new BigDecimal("62.0"))
                        .capturedAt(now.minus(14, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(locksmithProject).healthScore(62)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 1, "HIGH", 2, "MEDIUM", 2, "LOW", 0)))
                        .techDebtScore(45).dependencyScore(40).testCoveragePercent(new BigDecimal("62.5"))
                        .capturedAt(now.minus(7, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(locksmithProject).job(depJob).healthScore(65)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 1, "HIGH", 1, "MEDIUM", 2, "LOW", 0)))
                        .techDebtScore(48).dependencyScore(45).testCoveragePercent(new BigDecimal("63.0"))
                        .capturedAt(now.minus(1, ChronoUnit.DAYS)).build()
        ));
        log.info("Seeded 9 health snapshots");
    }

    private void seedSystemSettings() {
        Instant now = Instant.now();
        systemSettingRepository.saveAll(List.of(
                SystemSetting.builder().settingKey("app.version").value("0.1.0-SNAPSHOT").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("notification.email.enabled").value("false").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("notification.teams.enabled").value("false").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("agent.max.concurrent.runs").value("3").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("health.snapshot.retention.days").value("90").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("jira.sync.enabled").value("false").updatedBy(adam).updatedAt(now).build()
        ));
        log.info("Seeded 6 system settings");
    }

    private void seedAuditLog() {
        Instant now = Instant.now();
        auditLogRepository.saveAll(List.of(
                AuditLog.builder().user(adam).team(team).action("CREATE").entityType("Team").entityId(team.getId())
                        .details("Created team 'CodeOps Core'").ipAddress("127.0.0.1").createdAt(now.minus(90, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(adam).team(team).action("CREATE").entityType("Project").entityId(serverProject.getId())
                        .details("Created project 'CodeOps Server'").ipAddress("127.0.0.1").createdAt(now.minus(89, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(adam).team(team).action("CREATE").entityType("Project").entityId(clientProject.getId())
                        .details("Created project 'CodeOps Client'").ipAddress("127.0.0.1").createdAt(now.minus(88, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(sarah).team(team).action("CREATE").entityType("Project").entityId(locksmithProject.getId())
                        .details("Created project 'Locksmith Auth Service'").ipAddress("127.0.0.1").createdAt(now.minus(60, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(adam).team(team).action("START_JOB").entityType("QaJob").entityId(auditJob1.getId())
                        .details("Started audit job for CodeOps Server").ipAddress("127.0.0.1").createdAt(auditJob1.getStartedAt()).build(),
                AuditLog.builder().user(sarah).team(team).action("START_JOB").entityType("QaJob").entityId(complianceJob.getId())
                        .details("Started compliance job for CodeOps Server").ipAddress("127.0.0.1").createdAt(complianceJob.getStartedAt()).build(),
                AuditLog.builder().user(mike).team(team).action("START_JOB").entityType("QaJob").entityId(bugJob.getId())
                        .details("Started bug investigation for COD-142").ipAddress("127.0.0.1").createdAt(bugJob.getStartedAt()).build(),
                AuditLog.builder().user(adam).team(team).action("UPDATE").entityType("Finding").entityId(savedFindings.get(13).getId())
                        .details("Marked finding as FIXED").ipAddress("127.0.0.1").createdAt(now.minus(3, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(sarah).team(team).action("CREATE").entityType("Directive")
                        .details("Created directive 'Code Review Checklist'").ipAddress("127.0.0.1").createdAt(now.minus(30, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(mike).team(team).action("START_JOB").entityType("QaJob").entityId(runningJob.getId())
                        .details("Started security audit for Locksmith").ipAddress("127.0.0.1").createdAt(runningJob.getStartedAt()).build()
        ));
        log.info("Seeded 10 audit log entries");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON for seed data", e);
        }
    }

    // ── Registry Seed Data ──

    private void seedRegistryData() {
        if (serviceRegistrationRepository.count() > 0) {
            log.info("Registry data already seeded — skipping");
            return;
        }
        log.info("Seeding Registry data...");

        UUID teamId = team.getId();
        UUID userId = adam.getId();

        Map<String, ServiceRegistration> services = seedRegistryServices(teamId, userId);
        seedRegistryDependencies(services);
        Map<String, Solution> solutions = seedRegistrySolutions(teamId, userId);
        seedRegistrySolutionMembers(solutions, services);
        seedRegistryPortRanges(teamId);
        seedRegistryPortAllocations(services, userId);
        seedRegistryApiRoutes(services);
        seedRegistryEnvironmentConfigs(services);
        seedRegistryWorkstationProfiles(services, teamId, userId);
        seedRegistryInfraResources(services, teamId, userId);

        log.info("Registry data seeded successfully");
    }

    private Map<String, ServiceRegistration> seedRegistryServices(UUID teamId, UUID userId) {
        List<ServiceRegistration> services = List.of(
                buildRegistryService(teamId, userId, "CodeOps Server", "codeops-server", ServiceType.SPRING_BOOT_API,
                        "Core authentication and team management server",
                        "https://github.com/aallard/CodeOps-Server", "aallard/CodeOps-Server",
                        "Java, Spring Boot 3.3, PostgreSQL, Redis, Kafka", "http://localhost:8095/api/v1/health"),
                buildRegistryService(teamId, userId, "CodeOps Registry", "codeops-registry", ServiceType.SPRING_BOOT_API,
                        "Service registry and development control plane",
                        "https://github.com/aallard/CodeOps-Registry", "aallard/CodeOps-Registry",
                        "Java, Spring Boot 3.3, PostgreSQL", "http://localhost:8096/api/v1/health"),
                buildRegistryService(teamId, userId, "CodeOps Vault", "codeops-vault", ServiceType.SPRING_BOOT_API,
                        "Secrets and credential management service",
                        "https://github.com/aallard/CodeOps-Vault", "aallard/CodeOps-Vault",
                        "Java, Spring Boot 3.3, PostgreSQL", "http://localhost:8097/api/v1/health"),
                buildRegistryService(teamId, userId, "CodeOps Logger", "codeops-logger", ServiceType.SPRING_BOOT_API,
                        "Centralized logging and audit trail service",
                        "https://github.com/aallard/CodeOps-Logger", "aallard/CodeOps-Logger",
                        "Java, Spring Boot 3.3, PostgreSQL", "http://localhost:8098/api/v1/health"),
                buildRegistryService(teamId, userId, "CodeOps Courier", "codeops-courier", ServiceType.SPRING_BOOT_API,
                        "Notification and messaging delivery service",
                        "https://github.com/aallard/CodeOps-Courier", "aallard/CodeOps-Courier",
                        "Java, Spring Boot 3.3, PostgreSQL", "http://localhost:8099/api/v1/health"),
                buildRegistryService(teamId, userId, "CodeOps DataLens", "codeops-datalens", ServiceType.SPRING_BOOT_API,
                        "Analytics and data visualization service",
                        "https://github.com/aallard/CodeOps-DataLens", "aallard/CodeOps-DataLens",
                        "Java, Spring Boot 3.3, PostgreSQL", "http://localhost:8100/api/v1/health"),
                buildRegistryService(teamId, userId, "CodeOps Client", "codeops-client", ServiceType.REACT_SPA,
                        "Primary web application for the CodeOps platform",
                        "https://github.com/aallard/CodeOps-Client", "aallard/CodeOps-Client",
                        "React, TypeScript, Vite", "http://localhost:5173"),
                buildRegistryService(teamId, userId, "CodeOps Scribe", "codeops-scribe", ServiceType.REACT_SPA,
                        "Documentation and knowledge base module",
                        "https://github.com/aallard/CodeOps-Scribe", "aallard/CodeOps-Scribe",
                        "React, TypeScript", null),
                buildRegistryService(teamId, userId, "CodeOps Gateway", "codeops-gateway", ServiceType.GATEWAY,
                        "API gateway for routing and load balancing",
                        "https://github.com/aallard/CodeOps-Gateway", "aallard/CodeOps-Gateway",
                        "Spring Cloud Gateway", "http://localhost:8080/actuator/health")
        );

        List<ServiceRegistration> saved = serviceRegistrationRepository.saveAll(services);
        log.info("Seeded {} registry services", saved.size());
        return saved.stream().collect(Collectors.toMap(ServiceRegistration::getSlug, s -> s));
    }

    private void seedRegistryDependencies(Map<String, ServiceRegistration> services) {
        ServiceRegistration server = services.get("codeops-server");
        ServiceRegistration registry = services.get("codeops-registry");
        ServiceRegistration vault = services.get("codeops-vault");
        ServiceRegistration logger = services.get("codeops-logger");
        ServiceRegistration courier = services.get("codeops-courier");
        ServiceRegistration datalens = services.get("codeops-datalens");
        ServiceRegistration client = services.get("codeops-client");
        ServiceRegistration gateway = services.get("codeops-gateway");

        List<ServiceDependency> deps = List.of(
                buildRegistryDep(client, server, DependencyType.HTTP_REST, "/api/v1/auth"),
                buildRegistryDep(client, registry, DependencyType.HTTP_REST, "/api/v1/registry"),
                buildRegistryDep(client, vault, DependencyType.HTTP_REST, "/api/v1/vault"),
                buildRegistryDep(client, logger, DependencyType.HTTP_REST, "/api/v1/logs"),
                buildRegistryDep(client, courier, DependencyType.HTTP_REST, "/api/v1/courier"),
                buildRegistryDep(client, datalens, DependencyType.HTTP_REST, "/api/v1/datalens"),
                buildRegistryDep(registry, server, DependencyType.HTTP_REST, "/api/v1/auth"),
                buildRegistryDep(vault, server, DependencyType.HTTP_REST, "/api/v1/auth"),
                buildRegistryDep(logger, server, DependencyType.HTTP_REST, "/api/v1/auth"),
                buildRegistryDep(courier, server, DependencyType.HTTP_REST, "/api/v1/auth"),
                buildRegistryDep(datalens, server, DependencyType.HTTP_REST, "/api/v1/auth"),
                buildRegistryDep(gateway, server, DependencyType.HTTP_REST, "/api/v1/auth"),
                buildRegistryDep(gateway, registry, DependencyType.HTTP_REST, "/api/v1/registry")
        );

        serviceDependencyRepository.saveAll(deps);
        log.info("Seeded {} registry dependencies", deps.size());
    }

    private Map<String, Solution> seedRegistrySolutions(UUID teamId, UUID userId) {
        List<Solution> solutions = List.of(
                Solution.builder()
                        .teamId(teamId).name("CodeOps Control Plane").slug("codeops-control-plane")
                        .description("Core platform services powering the CodeOps ecosystem")
                        .category(SolutionCategory.PLATFORM).status(SolutionStatus.ACTIVE)
                        .iconName("dashboard").colorHex("#2196F3")
                        .ownerUserId(userId).createdByUserId(userId).build(),
                Solution.builder()
                        .teamId(teamId).name("CodeOps Infrastructure").slug("codeops-infrastructure")
                        .description("Infrastructure and routing layer for the platform")
                        .category(SolutionCategory.INFRASTRUCTURE).status(SolutionStatus.ACTIVE)
                        .iconName("cloud").colorHex("#FF9800")
                        .ownerUserId(userId).createdByUserId(userId).build(),
                Solution.builder()
                        .teamId(teamId).name("CodeOps Developer Tools").slug("codeops-developer-tools")
                        .description("Developer productivity and communication tools")
                        .category(SolutionCategory.TOOLING).status(SolutionStatus.ACTIVE)
                        .iconName("build").colorHex("#4CAF50")
                        .ownerUserId(userId).createdByUserId(userId).build()
        );

        List<Solution> saved = solutionRepository.saveAll(solutions);
        log.info("Seeded {} registry solutions", saved.size());
        return saved.stream().collect(Collectors.toMap(Solution::getSlug, s -> s));
    }

    private void seedRegistrySolutionMembers(Map<String, Solution> solutions,
                                              Map<String, ServiceRegistration> services) {
        Solution controlPlane = solutions.get("codeops-control-plane");
        Solution infra = solutions.get("codeops-infrastructure");
        Solution devTools = solutions.get("codeops-developer-tools");

        List<SolutionMember> members = List.of(
                buildRegistryMember(controlPlane, services.get("codeops-server"), SolutionMemberRole.CORE, 0),
                buildRegistryMember(controlPlane, services.get("codeops-registry"), SolutionMemberRole.CORE, 1),
                buildRegistryMember(controlPlane, services.get("codeops-vault"), SolutionMemberRole.CORE, 2),
                buildRegistryMember(controlPlane, services.get("codeops-logger"), SolutionMemberRole.SUPPORTING, 3),
                buildRegistryMember(controlPlane, services.get("codeops-courier"), SolutionMemberRole.SUPPORTING, 4),
                buildRegistryMember(controlPlane, services.get("codeops-datalens"), SolutionMemberRole.SUPPORTING, 5),
                buildRegistryMember(controlPlane, services.get("codeops-client"), SolutionMemberRole.CORE, 6),
                buildRegistryMember(infra, services.get("codeops-server"), SolutionMemberRole.CORE, 0),
                buildRegistryMember(infra, services.get("codeops-gateway"), SolutionMemberRole.CORE, 1),
                buildRegistryMember(devTools, services.get("codeops-courier"), SolutionMemberRole.CORE, 0),
                buildRegistryMember(devTools, services.get("codeops-datalens"), SolutionMemberRole.CORE, 1),
                buildRegistryMember(devTools, services.get("codeops-scribe"), SolutionMemberRole.CORE, 2)
        );

        solutionMemberRepository.saveAll(members);
        log.info("Seeded {} registry solution members", members.size());
    }

    private void seedRegistryPortRanges(UUID teamId) {
        List<PortRange> ranges = List.of(
                PortRange.builder().teamId(teamId).portType(PortType.HTTP_API)
                        .rangeStart(8080).rangeEnd(8199).environment("local")
                        .description("HTTP API ports for backend services").build(),
                PortRange.builder().teamId(teamId).portType(PortType.FRONTEND_DEV)
                        .rangeStart(5170).rangeEnd(5199).environment("local")
                        .description("Frontend development server ports").build(),
                PortRange.builder().teamId(teamId).portType(PortType.DATABASE)
                        .rangeStart(5430).rangeEnd(5499).environment("local")
                        .description("Database ports for PostgreSQL instances").build()
        );

        portRangeRepository.saveAll(ranges);
        log.info("Seeded {} registry port ranges", ranges.size());
    }

    private void seedRegistryPortAllocations(Map<String, ServiceRegistration> services, UUID userId) {
        List<PortAllocation> allocations = new ArrayList<>();
        allocations.addAll(buildRegistryPorts(services.get("codeops-server"), 8095, 5432, null, userId));
        allocations.addAll(buildRegistryPorts(services.get("codeops-registry"), 8096, 5435, null, userId));
        allocations.addAll(buildRegistryPorts(services.get("codeops-vault"), 8097, 5436, null, userId));
        allocations.addAll(buildRegistryPorts(services.get("codeops-logger"), 8098, 5437, null, userId));
        allocations.addAll(buildRegistryPorts(services.get("codeops-courier"), 8099, 5438, null, userId));
        allocations.addAll(buildRegistryPorts(services.get("codeops-datalens"), 8100, 5439, null, userId));
        allocations.addAll(buildRegistryPorts(services.get("codeops-client"), null, null, 5173, userId));
        allocations.addAll(buildRegistryPorts(services.get("codeops-scribe"), null, null, 5173, userId));
        allocations.addAll(buildRegistryPorts(services.get("codeops-gateway"), 8080, null, null, userId));

        portAllocationRepository.saveAll(allocations);
        log.info("Seeded {} registry port allocations", allocations.size());
    }

    private void seedRegistryApiRoutes(Map<String, ServiceRegistration> services) {
        List<ApiRouteRegistration> routes = List.of(
                buildRegistryRoute(services.get("codeops-server"), "/api/v1/auth", "Authentication and team management"),
                buildRegistryRoute(services.get("codeops-registry"), "/api/v1/registry", "Service registry CRUD"),
                buildRegistryRoute(services.get("codeops-vault"), "/api/v1/vault", "Secrets management"),
                buildRegistryRoute(services.get("codeops-logger"), "/api/v1/logs", "Logging and audit trail"),
                buildRegistryRoute(services.get("codeops-courier"), "/api/v1/courier", "Notification delivery"),
                buildRegistryRoute(services.get("codeops-datalens"), "/api/v1/datalens", "Analytics and reporting")
        );

        apiRouteRegistrationRepository.saveAll(routes);
        log.info("Seeded {} registry API routes", routes.size());
    }

    private void seedRegistryEnvironmentConfigs(Map<String, ServiceRegistration> services) {
        ServiceRegistration registry = services.get("codeops-registry");

        List<EnvironmentConfig> configs = List.of(
                buildRegistryConfig(registry, "spring.datasource.url",
                        "jdbc:postgresql://localhost:5435/codeops_registry", "JDBC connection URL"),
                buildRegistryConfig(registry, "spring.datasource.username", "postgres", "Database username"),
                buildRegistryConfig(registry, "spring.datasource.password", "postgres", "Database password"),
                buildRegistryConfig(registry, "spring.jpa.hibernate.ddl-auto", "update", "Hibernate schema strategy"),
                buildRegistryConfig(registry, "codeops.jwt.secret",
                        "dev-secret-key-minimum-32-characters-long-for-hs256", "JWT shared secret (dev default)")
        );

        environmentConfigRepository.saveAll(configs);
        log.info("Seeded {} registry environment configs", configs.size());
    }

    private void seedRegistryWorkstationProfiles(Map<String, ServiceRegistration> services,
                                                  UUID teamId, UUID userId) {
        List<UUID> allIds = List.of(
                services.get("codeops-server").getId(), services.get("codeops-gateway").getId(),
                services.get("codeops-registry").getId(), services.get("codeops-vault").getId(),
                services.get("codeops-logger").getId(), services.get("codeops-courier").getId(),
                services.get("codeops-datalens").getId(), services.get("codeops-scribe").getId(),
                services.get("codeops-client").getId());
        List<UUID> backendIds = List.of(
                services.get("codeops-server").getId(), services.get("codeops-registry").getId(),
                services.get("codeops-vault").getId(), services.get("codeops-logger").getId(),
                services.get("codeops-courier").getId(), services.get("codeops-datalens").getId());
        List<UUID> registryDevIds = List.of(
                services.get("codeops-server").getId(), services.get("codeops-registry").getId(),
                services.get("codeops-client").getId());

        List<WorkstationProfile> profiles = List.of(
                WorkstationProfile.builder().teamId(teamId).name("Full Platform")
                        .description("All 9 services for full platform development")
                        .servicesJson(toJson(allIds)).startupOrder(toJson(allIds))
                        .createdByUserId(userId).isDefault(true).build(),
                WorkstationProfile.builder().teamId(teamId).name("Backend Only")
                        .description("Backend services without frontend")
                        .servicesJson(toJson(backendIds)).startupOrder(toJson(backendIds))
                        .createdByUserId(userId).isDefault(false).build(),
                WorkstationProfile.builder().teamId(teamId).name("Registry Dev")
                        .description("Minimal setup for Registry development")
                        .servicesJson(toJson(registryDevIds)).startupOrder(toJson(registryDevIds))
                        .createdByUserId(userId).isDefault(false).build()
        );

        workstationProfileRepository.saveAll(profiles);
        log.info("Seeded {} registry workstation profiles", profiles.size());
    }

    private void seedRegistryInfraResources(Map<String, ServiceRegistration> services,
                                             UUID teamId, UUID userId) {
        List<InfraResource> resources = List.of(
                InfraResource.builder().teamId(teamId).resourceType(InfraResourceType.DOCKER_NETWORK)
                        .resourceName("codeops-network").environment("local")
                        .description("Shared Docker network for inter-service communication")
                        .createdByUserId(userId).build(),
                InfraResource.builder().teamId(teamId).service(services.get("codeops-server"))
                        .resourceType(InfraResourceType.DOCKER_VOLUME)
                        .resourceName("codeops-pg-data").environment("local")
                        .description("Persistent volume for PostgreSQL data")
                        .createdByUserId(userId).build()
        );

        infraResourceRepository.saveAll(resources);
        log.info("Seeded {} registry infra resources", resources.size());
    }

    // ── Registry Builder Helpers ──

    private ServiceRegistration buildRegistryService(UUID teamId, UUID userId, String name, String slug,
                                                      ServiceType type, String description, String repoUrl,
                                                      String repoFullName, String techStack, String healthCheckUrl) {
        return ServiceRegistration.builder()
                .teamId(teamId).name(name).slug(slug).serviceType(type)
                .description(description).repoUrl(repoUrl).repoFullName(repoFullName)
                .techStack(techStack).status(ServiceStatus.ACTIVE)
                .healthCheckUrl(healthCheckUrl).createdByUserId(userId).build();
    }

    private ServiceDependency buildRegistryDep(ServiceRegistration source, ServiceRegistration target,
                                                DependencyType type, String endpoint) {
        return ServiceDependency.builder()
                .sourceService(source).targetService(target)
                .dependencyType(type).isRequired(true)
                .description(source.getName() + " depends on " + target.getName())
                .targetEndpoint(endpoint).build();
    }

    private SolutionMember buildRegistryMember(Solution solution, ServiceRegistration service,
                                                SolutionMemberRole role, int order) {
        return SolutionMember.builder()
                .solution(solution).service(service)
                .role(role).displayOrder(order).build();
    }

    private List<PortAllocation> buildRegistryPorts(ServiceRegistration service, Integer httpPort,
                                                     Integer dbPort, Integer frontendPort, UUID userId) {
        List<PortAllocation> ports = new ArrayList<>();
        if (httpPort != null) {
            ports.add(PortAllocation.builder().service(service).environment("local")
                    .portType(PortType.HTTP_API).portNumber(httpPort).protocol("TCP")
                    .isAutoAllocated(false).allocatedByUserId(userId).build());
        }
        if (dbPort != null) {
            ports.add(PortAllocation.builder().service(service).environment("local")
                    .portType(PortType.DATABASE).portNumber(dbPort).protocol("TCP")
                    .isAutoAllocated(false).allocatedByUserId(userId).build());
        }
        if (frontendPort != null) {
            ports.add(PortAllocation.builder().service(service).environment("local")
                    .portType(PortType.FRONTEND_DEV).portNumber(frontendPort).protocol("TCP")
                    .isAutoAllocated(false).allocatedByUserId(userId).build());
        }
        return ports;
    }

    private ApiRouteRegistration buildRegistryRoute(ServiceRegistration service, String prefix,
                                                     String description) {
        return ApiRouteRegistration.builder()
                .service(service).routePrefix(prefix)
                .httpMethods("GET,POST,PUT,DELETE,PATCH").environment("local")
                .description(description).build();
    }

    private EnvironmentConfig buildRegistryConfig(ServiceRegistration service, String key,
                                                   String value, String description) {
        return EnvironmentConfig.builder()
                .service(service).environment("local").configKey(key)
                .configValue(value).configSource(ConfigSource.MANUAL)
                .description(description).build();
    }

    // ── Logger Seed Data ──

    private static final Random LOGGER_RANDOM = new Random(42);

    private void seedLoggerData() {
        if (logSourceRepository.count() > 0) {
            log.info("Logger data already seeded — skipping");
            return;
        }
        log.info("Seeding Logger data...");

        UUID teamId = team.getId();
        UUID userId = adam.getId();

        List<LogSource> sources = seedLogSources(teamId);
        seedLogEntries(sources, teamId);
        List<LogTrap> traps = seedLogTraps(teamId, userId);
        List<AlertChannel> channels = seedAlertChannels(teamId, userId);
        seedAlertRules(traps, channels, teamId);
        List<Metric> metrics = seedLoggerMetrics(teamId);
        seedMetricSeries(metrics);
        seedDashboards(teamId, userId);
        seedRetentionPolicies(teamId, userId);
        seedAnomalyBaselines(teamId);
        seedTraceSpans(teamId);

        log.info("Logger development data seeded successfully");
    }

    private List<LogSource> seedLogSources(UUID teamId) {
        List<LogSource> sources = List.of(
                LogSource.builder().name("codeops-server").environment("local").isActive(true).teamId(teamId).logCount(0L).build(),
                LogSource.builder().name("codeops-registry").environment("local").isActive(true).teamId(teamId).logCount(0L).build(),
                LogSource.builder().name("codeops-vault").environment("local").isActive(true).teamId(teamId).logCount(0L).build(),
                LogSource.builder().name("codeops-courier").environment("local").isActive(true).teamId(teamId).logCount(0L).build(),
                LogSource.builder().name("codeops-logger").environment("local").isActive(true).teamId(teamId).logCount(0L).build()
        );
        List<LogSource> saved = logSourceRepository.saveAll(sources);
        log.info("Seeded {} log sources", saved.size());
        return saved;
    }

    private void seedLogEntries(List<LogSource> sources, UUID teamId) {
        Instant now = Instant.now();
        List<LogEntry> entries = new ArrayList<>();
        String[] infoMessages = {
                "Application started successfully", "Health check passed", "Request processed",
                "Cache refreshed", "Configuration loaded", "Scheduled task completed",
                "Connection pool initialized", "JWT token validated", "API response sent",
                "Metrics collected", "Session created", "Data synced", "Backup completed",
                "Index rebuilt", "Queue drained", "Websocket connected", "File uploaded",
                "Email sent", "Webhook delivered", "Rate limit reset"
        };

        for (int i = 0; i < 20; i++) {
            LogSource source = sources.get(i % sources.size());
            entries.add(LogEntry.builder()
                    .source(source).level(LogLevel.INFO).message(infoMessages[i])
                    .timestamp(now.minus(i * 15L, ChronoUnit.MINUTES))
                    .serviceName(source.getName()).teamId(teamId)
                    .loggerName("com.codeops." + source.getName().replace("codeops-", ""))
                    .threadName("main").hostName("localhost").build());
        }
        for (int i = 0; i < 10; i++) {
            LogSource source = sources.get(i % sources.size());
            entries.add(LogEntry.builder()
                    .source(source).level(LogLevel.DEBUG).message("Debug trace #" + i)
                    .timestamp(now.minus(i * 30L, ChronoUnit.MINUTES))
                    .serviceName(source.getName()).teamId(teamId).build());
        }
        for (int i = 0; i < 10; i++) {
            LogSource source = sources.get(i % sources.size());
            entries.add(LogEntry.builder()
                    .source(source).level(LogLevel.WARN).message("High memory usage detected: " + (80 + i) + "%")
                    .timestamp(now.minus(i * 45L, ChronoUnit.MINUTES))
                    .serviceName(source.getName()).teamId(teamId).build());
        }
        for (int i = 0; i < 7; i++) {
            LogSource source = sources.get(i % sources.size());
            entries.add(LogEntry.builder()
                    .source(source).level(LogLevel.ERROR).message("Connection timeout after 5000ms")
                    .timestamp(now.minus(i * 60L, ChronoUnit.MINUTES))
                    .serviceName(source.getName()).teamId(teamId)
                    .exceptionClass("java.net.SocketTimeoutException")
                    .exceptionMessage("Connection timed out")
                    .stackTrace("java.net.SocketTimeoutException: Connection timed out\n\tat java.net.Socket.connect(Socket.java:600)").build());
        }
        for (int i = 0; i < 3; i++) {
            LogSource source = sources.get(i % sources.size());
            entries.add(LogEntry.builder()
                    .source(source).level(LogLevel.FATAL).message("Out of memory error — JVM heap exhausted")
                    .timestamp(now.minus(i * 120L, ChronoUnit.MINUTES))
                    .serviceName(source.getName()).teamId(teamId)
                    .exceptionClass("java.lang.OutOfMemoryError")
                    .exceptionMessage("Java heap space").build());
        }

        logEntryRepository.saveAll(entries);
        for (LogSource source : sources) {
            long count = entries.stream().filter(e -> e.getSource().getId().equals(source.getId())).count();
            source.setLogCount(count);
            source.setLastLogReceivedAt(now);
        }
        logSourceRepository.saveAll(sources);
        log.info("Seeded {} log entries", entries.size());
    }

    private List<LogTrap> seedLogTraps(UUID teamId, UUID userId) {
        LogTrap highError = LogTrap.builder().name("High Error Rate").description("Triggers on repeated errors")
                .trapType(TrapType.PATTERN).isActive(true).teamId(teamId).createdBy(userId).triggerCount(0L).build();
        LogTrap fatal = LogTrap.builder().name("Fatal Alert").description("Triggers on any FATAL log")
                .trapType(TrapType.PATTERN).isActive(true).teamId(teamId).createdBy(userId).triggerCount(0L).build();
        LogTrap authFail = LogTrap.builder().name("Auth Failures").description("Triggers on authentication failures")
                .trapType(TrapType.FREQUENCY).isActive(true).teamId(teamId).createdBy(userId).triggerCount(0L).build();

        List<LogTrap> traps = logTrapRepository.saveAll(List.of(highError, fatal, authFail));

        trapConditionRepository.saveAll(List.of(
                TrapCondition.builder().trap(traps.get(0)).conditionType(ConditionType.REGEX)
                        .field("message").pattern("(?i)(error|exception|fail)").build(),
                TrapCondition.builder().trap(traps.get(0)).conditionType(ConditionType.KEYWORD)
                        .field("level").pattern("ERROR").build(),
                TrapCondition.builder().trap(traps.get(1)).conditionType(ConditionType.KEYWORD)
                        .field("level").pattern("FATAL").build(),
                TrapCondition.builder().trap(traps.get(2)).conditionType(ConditionType.FREQUENCY_THRESHOLD)
                        .field("message").pattern("authentication failed").threshold(5).windowSeconds(300).build()
        ));
        log.info("Seeded {} log traps with conditions", traps.size());
        return traps;
    }

    private List<AlertChannel> seedAlertChannels(UUID teamId, UUID userId) {
        List<AlertChannel> channels = alertChannelRepository.saveAll(List.of(
                AlertChannel.builder().name("Dev Email").channelType(AlertChannelType.EMAIL)
                        .configuration("{\"recipients\":[\"dev@codeops.dev\"]}")
                        .isActive(true).teamId(teamId).createdBy(userId).build(),
                AlertChannel.builder().name("Ops Webhook").channelType(AlertChannelType.WEBHOOK)
                        .configuration("{\"url\":\"https://hooks.example.com/codeops\"}")
                        .isActive(true).teamId(teamId).createdBy(userId).build(),
                AlertChannel.builder().name("Slack Alerts").channelType(AlertChannelType.SLACK)
                        .configuration("{\"webhook_url\":\"https://hooks.slack.com/services/FAKE/FAKE/FAKE\"}")
                        .isActive(true).teamId(teamId).createdBy(userId).build()
        ));
        log.info("Seeded {} alert channels", channels.size());
        return channels;
    }

    private void seedAlertRules(List<LogTrap> traps, List<AlertChannel> channels, UUID teamId) {
        alertRuleRepository.saveAll(List.of(
                AlertRule.builder().name("High Error Rate -> Slack").trap(traps.get(0)).channel(channels.get(2))
                        .severity(AlertSeverity.WARNING).isActive(true).throttleMinutes(15).teamId(teamId).build(),
                AlertRule.builder().name("Fatal Alert -> Email").trap(traps.get(1)).channel(channels.get(0))
                        .severity(AlertSeverity.CRITICAL).isActive(true).throttleMinutes(5).teamId(teamId).build()
        ));
        log.info("Seeded 2 alert rules");
    }

    private List<Metric> seedLoggerMetrics(UUID teamId) {
        List<Metric> metrics = metricRepository.saveAll(List.of(
                Metric.builder().name("http_requests_total").metricType(MetricType.COUNTER)
                        .description("Total HTTP requests").unit("requests").serviceName("codeops-server").teamId(teamId).build(),
                Metric.builder().name("active_connections").metricType(MetricType.GAUGE)
                        .description("Active database connections").unit("connections").serviceName("codeops-server").teamId(teamId).build(),
                Metric.builder().name("response_time_ms").metricType(MetricType.TIMER)
                        .description("API response time").unit("ms").serviceName("codeops-server").teamId(teamId).build(),
                Metric.builder().name("request_size_bytes").metricType(MetricType.HISTOGRAM)
                        .description("Request body size distribution").unit("bytes").serviceName("codeops-server").teamId(teamId).build(),
                Metric.builder().name("cache_hit_ratio").metricType(MetricType.GAUGE)
                        .description("Cache hit ratio").unit("ratio").serviceName("codeops-server").teamId(teamId).build(),
                Metric.builder().name("queue_depth").metricType(MetricType.GAUGE)
                        .description("Message queue depth").unit("messages").serviceName("codeops-server").teamId(teamId).build()
        ));
        log.info("Seeded {} metrics", metrics.size());
        return metrics;
    }

    private void seedMetricSeries(List<Metric> metrics) {
        Instant now = Instant.now();
        List<MetricSeries> allSeries = new ArrayList<>();
        for (Metric metric : metrics) {
            for (int h = 0; h < 24; h++) {
                allSeries.add(MetricSeries.builder()
                        .metric(metric).timestamp(now.minus(h, ChronoUnit.HOURS))
                        .value(generateMetricValue(metric.getName(), h)).resolution(3600).build());
            }
        }
        metricSeriesRepository.saveAll(allSeries);
        log.info("Seeded {} metric series data points", allSeries.size());
    }

    private double generateMetricValue(String metricName, int hour) {
        return switch (metricName) {
            case "http_requests_total" -> 1000 + 500 * Math.sin(hour * Math.PI / 12) + LOGGER_RANDOM.nextInt(100);
            case "active_connections" -> 20 + 10 * Math.sin(hour * Math.PI / 12) + LOGGER_RANDOM.nextInt(5);
            case "response_time_ms" -> 50 + 30 * Math.sin(hour * Math.PI / 12) + LOGGER_RANDOM.nextInt(20);
            case "request_size_bytes" -> 2048 + 1024 * Math.sin(hour * Math.PI / 12) + LOGGER_RANDOM.nextInt(512);
            case "cache_hit_ratio" -> 0.85 + 0.1 * Math.sin(hour * Math.PI / 12) + LOGGER_RANDOM.nextDouble() * 0.05;
            case "queue_depth" -> 5 + 3 * Math.sin(hour * Math.PI / 12) + LOGGER_RANDOM.nextInt(3);
            default -> LOGGER_RANDOM.nextDouble() * 100;
        };
    }

    private void seedDashboards(UUID teamId, UUID userId) {
        Dashboard ops = dashboardRepository.save(Dashboard.builder()
                .name("Operations Overview").description("Real-time operations monitoring")
                .teamId(teamId).createdBy(userId).isShared(true).isTemplate(false)
                .refreshIntervalSeconds(30).build());

        dashboardWidgetRepository.saveAll(List.of(
                DashboardWidget.builder().dashboard(ops).title("Error Rate").widgetType(WidgetType.TIME_SERIES_CHART)
                        .gridX(0).gridY(0).gridWidth(6).gridHeight(3).sortOrder(0).build(),
                DashboardWidget.builder().dashboard(ops).title("Active Connections").widgetType(WidgetType.GAUGE)
                        .gridX(6).gridY(0).gridWidth(3).gridHeight(3).sortOrder(1).build(),
                DashboardWidget.builder().dashboard(ops).title("Request Volume").widgetType(WidgetType.COUNTER)
                        .gridX(9).gridY(0).gridWidth(3).gridHeight(3).sortOrder(2).build(),
                DashboardWidget.builder().dashboard(ops).title("Recent Errors").widgetType(WidgetType.LOG_STREAM)
                        .gridX(0).gridY(3).gridWidth(12).gridHeight(4).sortOrder(3).build()
        ));

        Dashboard healthTpl = dashboardRepository.save(Dashboard.builder()
                .name("Service Health Template").description("Template for service health monitoring")
                .teamId(teamId).createdBy(userId).isShared(true).isTemplate(true)
                .refreshIntervalSeconds(60).build());

        dashboardWidgetRepository.saveAll(List.of(
                DashboardWidget.builder().dashboard(healthTpl).title("Response Time P95").widgetType(WidgetType.TIME_SERIES_CHART)
                        .gridX(0).gridY(0).gridWidth(6).gridHeight(3).sortOrder(0).build(),
                DashboardWidget.builder().dashboard(healthTpl).title("Error Count").widgetType(WidgetType.COUNTER)
                        .gridX(6).gridY(0).gridWidth(3).gridHeight(3).sortOrder(1).build(),
                DashboardWidget.builder().dashboard(healthTpl).title("Recent Logs").widgetType(WidgetType.LOG_STREAM)
                        .gridX(0).gridY(3).gridWidth(12).gridHeight(4).sortOrder(2).build()
        ));
        log.info("Seeded 2 dashboards with 7 widgets");
    }

    private void seedRetentionPolicies(UUID teamId, UUID userId) {
        retentionPolicyRepository.saveAll(List.of(
                RetentionPolicy.builder().name("Debug Log Cleanup").retentionDays(7)
                        .action(RetentionAction.PURGE).logLevel(LogLevel.DEBUG)
                        .isActive(true).teamId(teamId).createdBy(userId).build(),
                RetentionPolicy.builder().name("Error Log Archive").retentionDays(90)
                        .action(RetentionAction.ARCHIVE).logLevel(LogLevel.ERROR)
                        .archiveDestination("s3://codeops-archive/logs/errors/")
                        .isActive(true).teamId(teamId).createdBy(userId).build(),
                RetentionPolicy.builder().name("Default Retention").retentionDays(30)
                        .action(RetentionAction.PURGE)
                        .isActive(true).teamId(teamId).createdBy(userId).build()
        ));
        log.info("Seeded 3 retention policies");
    }

    private void seedAnomalyBaselines(UUID teamId) {
        Instant now = Instant.now();
        anomalyBaselineRepository.saveAll(List.of(
                AnomalyBaseline.builder().serviceName("codeops-server").metricName("log_volume")
                        .baselineValue(150.0).standardDeviation(25.0).sampleCount(168L)
                        .windowStartTime(now.minus(7, ChronoUnit.DAYS)).windowEndTime(now)
                        .deviationThreshold(2.0).isActive(true).teamId(teamId).lastComputedAt(now).build(),
                AnomalyBaseline.builder().serviceName("codeops-server").metricName("error_rate")
                        .baselineValue(2.5).standardDeviation(1.0).sampleCount(168L)
                        .windowStartTime(now.minus(7, ChronoUnit.DAYS)).windowEndTime(now)
                        .deviationThreshold(2.5).isActive(true).teamId(teamId).lastComputedAt(now).build()
        ));
        log.info("Seeded 2 anomaly baselines");
    }

    private void seedTraceSpans(UUID teamId) {
        Instant now = Instant.now();
        String corrId1 = "corr-trace-001";
        String traceId1 = "trace-001";

        traceSpanRepository.saveAll(List.of(
                TraceSpan.builder().correlationId(corrId1).traceId(traceId1).spanId("span-001")
                        .serviceName("codeops-server").operationName("POST /api/v1/jobs")
                        .startTime(now.minus(1, ChronoUnit.HOURS)).endTime(now.minus(1, ChronoUnit.HOURS).plusMillis(250))
                        .durationMs(250L).status(SpanStatus.OK).teamId(teamId).build(),
                TraceSpan.builder().correlationId(corrId1).traceId(traceId1).spanId("span-002").parentSpanId("span-001")
                        .serviceName("codeops-server").operationName("AuthService.validate")
                        .startTime(now.minus(1, ChronoUnit.HOURS).plusMillis(5)).endTime(now.minus(1, ChronoUnit.HOURS).plusMillis(30))
                        .durationMs(25L).status(SpanStatus.OK).teamId(teamId).build(),
                TraceSpan.builder().correlationId(corrId1).traceId(traceId1).spanId("span-003").parentSpanId("span-001")
                        .serviceName("codeops-server").operationName("QaJobService.create")
                        .startTime(now.minus(1, ChronoUnit.HOURS).plusMillis(35)).endTime(now.minus(1, ChronoUnit.HOURS).plusMillis(200))
                        .durationMs(165L).status(SpanStatus.OK).teamId(teamId).build(),
                TraceSpan.builder().correlationId(corrId1).traceId(traceId1).spanId("span-004").parentSpanId("span-003")
                        .serviceName("codeops-server").operationName("PostgreSQL INSERT")
                        .startTime(now.minus(1, ChronoUnit.HOURS).plusMillis(40)).endTime(now.minus(1, ChronoUnit.HOURS).plusMillis(55))
                        .durationMs(15L).status(SpanStatus.OK).teamId(teamId).build(),
                TraceSpan.builder().correlationId(corrId1).traceId(traceId1).spanId("span-005").parentSpanId("span-003")
                        .serviceName("codeops-server").operationName("Kafka PRODUCE codeops-logs")
                        .startTime(now.minus(1, ChronoUnit.HOURS).plusMillis(60)).endTime(now.minus(1, ChronoUnit.HOURS).plusMillis(75))
                        .durationMs(15L).status(SpanStatus.OK).teamId(teamId).build()
        ));

        String corrId2 = "corr-trace-002";
        String traceId2 = "trace-002";
        traceSpanRepository.saveAll(List.of(
                TraceSpan.builder().correlationId(corrId2).traceId(traceId2).spanId("span-010")
                        .serviceName("codeops-server").operationName("GET /api/v1/projects/999")
                        .startTime(now.minus(30, ChronoUnit.MINUTES)).endTime(now.minus(30, ChronoUnit.MINUTES).plusMillis(120))
                        .durationMs(120L).status(SpanStatus.ERROR).statusMessage("404 Not Found").teamId(teamId).build(),
                TraceSpan.builder().correlationId(corrId2).traceId(traceId2).spanId("span-011").parentSpanId("span-010")
                        .serviceName("codeops-server").operationName("ProjectService.findById")
                        .startTime(now.minus(30, ChronoUnit.MINUTES).plusMillis(10)).endTime(now.minus(30, ChronoUnit.MINUTES).plusMillis(100))
                        .durationMs(90L).status(SpanStatus.ERROR).statusMessage("Project not found").teamId(teamId).build(),
                TraceSpan.builder().correlationId(corrId2).traceId(traceId2).spanId("span-012").parentSpanId("span-011")
                        .serviceName("codeops-server").operationName("PostgreSQL SELECT")
                        .startTime(now.minus(30, ChronoUnit.MINUTES).plusMillis(15)).endTime(now.minus(30, ChronoUnit.MINUTES).plusMillis(25))
                        .durationMs(10L).status(SpanStatus.OK).teamId(teamId).build()
        ));
        log.info("Seeded 8 trace spans across 2 traces");
    }

    // ── Courier seed methods ──

    private void seedCourierData() {
        if (collectionRepository.countByTeamId(team.getId()) > 0) {
            log.info("Courier data already seeded — skipping");
            return;
        }
        log.info("Seeding Courier data...");

        UUID teamId = team.getId();
        UUID userId = adam.getId();

        Collection collection = seedCourierCollection(teamId, userId);
        int folderCount = 0;
        int requestCount = 0;

        Folder authFolder = seedCourierFolder(collection, null, "Authentication", "Auth endpoints", 0);
        folderCount++;
        seedCourierRequest(authFolder, "Login", HttpMethod.POST, "{{baseUrl}}/api/v1/auth/login", 0);
        seedCourierRequest(authFolder, "Register", HttpMethod.POST, "{{baseUrl}}/api/v1/auth/register", 1);
        seedCourierRequest(authFolder, "Refresh Token", HttpMethod.POST, "{{baseUrl}}/api/v1/auth/refresh", 2);
        requestCount += 3;

        Folder teamsFolder = seedCourierFolder(collection, null, "Teams", "Team management endpoints", 1);
        folderCount++;
        seedCourierRequest(teamsFolder, "List Teams", HttpMethod.GET, "{{baseUrl}}/api/v1/teams", 0);
        seedCourierRequest(teamsFolder, "Create Team", HttpMethod.POST, "{{baseUrl}}/api/v1/teams", 1);
        seedCourierRequest(teamsFolder, "Get Team", HttpMethod.GET, "{{baseUrl}}/api/v1/teams/{{teamId}}", 2);
        requestCount += 3;

        Folder healthFolder = seedCourierFolder(collection, null, "Health Checks", "Service health endpoints", 2);
        folderCount++;
        seedCourierRequest(healthFolder, "Courier Health", HttpMethod.GET, "{{baseUrl}}/api/v1/courier/health", 0);
        seedCourierRequest(healthFolder, "Registry Health", HttpMethod.GET, "{{baseUrl}}/api/v1/registry/health", 1);
        requestCount += 2;

        seedCourierEnvironment(teamId, userId);
        seedCourierGlobalVariables(teamId);

        log.info("Courier development data seeded: 1 collection, {} folders, {} requests, 1 environment", folderCount, requestCount);
    }

    private Collection seedCourierCollection(UUID teamId, UUID userId) {
        Collection collection = Collection.builder()
                .teamId(teamId)
                .name("CodeOps API")
                .description("Sample API collection for CodeOps platform testing")
                .authType(AuthType.NO_AUTH)
                .isShared(false)
                .createdBy(userId)
                .build();
        return collectionRepository.save(collection);
    }

    private Folder seedCourierFolder(Collection collection, Folder parent, String name, String description, int sortOrder) {
        Folder folder = Folder.builder()
                .collection(collection)
                .parentFolder(parent)
                .name(name)
                .description(description)
                .sortOrder(sortOrder)
                .authType(AuthType.INHERIT_FROM_PARENT)
                .build();
        return folderRepository.save(folder);
    }

    private void seedCourierRequest(Folder folder, String name, HttpMethod method, String url, int sortOrder) {
        Request request = Request.builder()
                .folder(folder)
                .name(name)
                .method(method)
                .url(url)
                .sortOrder(sortOrder)
                .build();
        requestRepository.save(request);
    }

    private void seedCourierEnvironment(UUID teamId, UUID userId) {
        Environment env = Environment.builder()
                .teamId(teamId)
                .name("Local Development")
                .description("Local development environment")
                .isActive(true)
                .createdBy(userId)
                .build();
        env = courierEnvironmentRepository.save(env);

        environmentVariableRepository.save(EnvironmentVariable.builder()
                .environment(env)
                .variableKey("baseUrl")
                .variableValue("http://localhost:8090")
                .isSecret(false)
                .isEnabled(true)
                .scope("ENVIRONMENT")
                .build());

        environmentVariableRepository.save(EnvironmentVariable.builder()
                .environment(env)
                .variableKey("token")
                .variableValue("")
                .isSecret(true)
                .isEnabled(true)
                .scope("ENVIRONMENT")
                .build());
    }

    private void seedCourierGlobalVariables(UUID teamId) {
        globalVariableRepository.save(GlobalVariable.builder()
                .teamId(teamId)
                .variableKey("baseUrl")
                .variableValue("http://localhost:8090")
                .isSecret(false)
                .isEnabled(true)
                .build());

        globalVariableRepository.save(GlobalVariable.builder()
                .teamId(teamId)
                .variableKey("authToken")
                .variableValue("")
                .isSecret(false)
                .isEnabled(true)
                .build());
    }
}
