package com.codeops.service;

import com.codeops.dto.request.CreateAgentRunRequest;
import com.codeops.dto.request.UpdateAgentRunRequest;
import com.codeops.dto.response.AgentRunResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.*;
import com.codeops.repository.AgentRunRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.TeamMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentRunServiceTest {

    @Mock private AgentRunRepository agentRunRepository;
    @Mock private QaJobRepository qaJobRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private AgentRunService agentRunService;

    private UUID userId;
    private UUID teamId;
    private UUID jobId;
    private UUID agentRunId;
    private Team team;
    private Project project;
    private QaJob job;
    private User user;
    private AgentRun agentRun;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        agentRunId = UUID.randomUUID();

        setSecurityContext(userId);

        team = Team.builder().name("Test Team").build();
        team.setId(teamId);
        team.setCreatedAt(Instant.now());

        user = User.builder()
                .email("test@codeops.dev")
                .passwordHash("hash")
                .displayName("Test User")
                .isActive(true)
                .build();
        user.setId(userId);
        user.setCreatedAt(Instant.now());

        project = Project.builder()
                .team(team)
                .name("Test Project")
                .createdBy(user)
                .build();
        project.setId(UUID.randomUUID());
        project.setCreatedAt(Instant.now());

        job = QaJob.builder()
                .project(project)
                .mode(JobMode.AUDIT)
                .status(JobStatus.RUNNING)
                .name("Test Job")
                .startedBy(user)
                .build();
        job.setId(jobId);
        job.setCreatedAt(Instant.now());

        agentRun = AgentRun.builder()
                .job(job)
                .agentType(AgentType.SECURITY)
                .status(AgentStatus.PENDING)
                .findingsCount(0)
                .criticalCount(0)
                .highCount(0)
                .build();
        agentRun.setId(agentRunId);
        agentRun.setCreatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createAgentRun ---

    @Test
    void createAgentRun_success() {
        CreateAgentRunRequest request = new CreateAgentRunRequest(jobId, AgentType.SECURITY);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.save(any(AgentRun.class))).thenReturn(agentRun);

        AgentRunResponse response = agentRunService.createAgentRun(request);

        assertNotNull(response);
        assertEquals(agentRunId, response.id());
        assertEquals(jobId, response.jobId());
        assertEquals(AgentType.SECURITY, response.agentType());
        assertEquals(AgentStatus.PENDING, response.status());
        assertEquals(0, response.findingsCount());
        verify(agentRunRepository).save(any(AgentRun.class));
    }

    @Test
    void createAgentRun_jobNotFound_throws() {
        CreateAgentRunRequest request = new CreateAgentRunRequest(jobId, AgentType.SECURITY);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> agentRunService.createAgentRun(request));
        verify(agentRunRepository, never()).save(any());
    }

    @Test
    void createAgentRun_notTeamMember_throws() {
        CreateAgentRunRequest request = new CreateAgentRunRequest(jobId, AgentType.SECURITY);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> agentRunService.createAgentRun(request));
        verify(agentRunRepository, never()).save(any());
    }

    // --- createAgentRuns (batch) ---

    @Test
    void createAgentRuns_success() {
        List<AgentType> agentTypes = List.of(AgentType.SECURITY, AgentType.CODE_QUALITY);

        AgentRun agentRun2 = AgentRun.builder()
                .job(job)
                .agentType(AgentType.CODE_QUALITY)
                .status(AgentStatus.PENDING)
                .findingsCount(0)
                .criticalCount(0)
                .highCount(0)
                .build();
        agentRun2.setId(UUID.randomUUID());
        agentRun2.setCreatedAt(Instant.now());

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.saveAll(anyList())).thenReturn(List.of(agentRun, agentRun2));

        List<AgentRunResponse> responses = agentRunService.createAgentRuns(jobId, agentTypes);

        assertEquals(2, responses.size());
        assertEquals(AgentType.SECURITY, responses.get(0).agentType());
        assertEquals(AgentType.CODE_QUALITY, responses.get(1).agentType());
        verify(agentRunRepository).saveAll(anyList());
    }

    @Test
    void createAgentRuns_jobNotFound_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> agentRunService.createAgentRuns(jobId, List.of(AgentType.SECURITY)));
    }

    @Test
    void createAgentRuns_notTeamMember_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> agentRunService.createAgentRuns(jobId, List.of(AgentType.SECURITY)));
    }

    // --- getAgentRuns ---

    @Test
    void getAgentRuns_success() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.findByJobId(jobId)).thenReturn(List.of(agentRun));

        List<AgentRunResponse> responses = agentRunService.getAgentRuns(jobId);

        assertEquals(1, responses.size());
        assertEquals(agentRunId, responses.get(0).id());
    }

    @Test
    void getAgentRuns_jobNotFound_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> agentRunService.getAgentRuns(jobId));
    }

    @Test
    void getAgentRuns_notTeamMember_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> agentRunService.getAgentRuns(jobId));
    }

    @Test
    void getAgentRuns_emptyList_returnsEmpty() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.findByJobId(jobId)).thenReturn(List.of());

        List<AgentRunResponse> responses = agentRunService.getAgentRuns(jobId);

        assertTrue(responses.isEmpty());
    }

    // --- getAgentRun ---

    @Test
    void getAgentRun_success() {
        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.of(agentRun));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        AgentRunResponse response = agentRunService.getAgentRun(agentRunId);

        assertEquals(agentRunId, response.id());
        assertEquals(jobId, response.jobId());
        assertEquals(AgentType.SECURITY, response.agentType());
    }

    @Test
    void getAgentRun_notFound_throws() {
        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> agentRunService.getAgentRun(agentRunId));
    }

    @Test
    void getAgentRun_notTeamMember_throws() {
        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.of(agentRun));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> agentRunService.getAgentRun(agentRunId));
    }

    // --- updateAgentRun ---

    @Test
    void updateAgentRun_allFields_success() {
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant completedAt = Instant.now();
        UpdateAgentRunRequest request = new UpdateAgentRunRequest(
                AgentStatus.COMPLETED, AgentResult.PASS, "reports/report.json",
                85, 10, 2, 3, completedAt, startedAt
        );

        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.of(agentRun));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.save(any(AgentRun.class))).thenReturn(agentRun);

        AgentRunResponse response = agentRunService.updateAgentRun(agentRunId, request);

        assertNotNull(response);
        assertEquals(AgentStatus.COMPLETED, agentRun.getStatus());
        assertEquals(AgentResult.PASS, agentRun.getResult());
        assertEquals("reports/report.json", agentRun.getReportS3Key());
        assertEquals(85, agentRun.getScore());
        assertEquals(10, agentRun.getFindingsCount());
        assertEquals(2, agentRun.getCriticalCount());
        assertEquals(3, agentRun.getHighCount());
        assertEquals(completedAt, agentRun.getCompletedAt());
        assertEquals(startedAt, agentRun.getStartedAt());
    }

    @Test
    void updateAgentRun_nullFields_notUpdated() {
        UpdateAgentRunRequest request = new UpdateAgentRunRequest(
                null, null, null, null, null, null, null, null, null
        );

        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.of(agentRun));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.save(any(AgentRun.class))).thenReturn(agentRun);

        agentRunService.updateAgentRun(agentRunId, request);

        assertEquals(AgentStatus.PENDING, agentRun.getStatus());
        assertNull(agentRun.getResult());
        assertNull(agentRun.getReportS3Key());
        assertNull(agentRun.getScore());
        assertEquals(0, agentRun.getFindingsCount());
        assertEquals(0, agentRun.getCriticalCount());
        assertEquals(0, agentRun.getHighCount());
    }

    @Test
    void updateAgentRun_startedAtNotOverwritten() {
        Instant originalStartedAt = Instant.now().minusSeconds(120);
        agentRun.setStartedAt(originalStartedAt);

        Instant newStartedAt = Instant.now();
        UpdateAgentRunRequest request = new UpdateAgentRunRequest(
                null, null, null, null, null, null, null, null, newStartedAt
        );

        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.of(agentRun));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.save(any(AgentRun.class))).thenReturn(agentRun);

        agentRunService.updateAgentRun(agentRunId, request);

        assertEquals(originalStartedAt, agentRun.getStartedAt());
    }

    @Test
    void updateAgentRun_startedAtSetWhenNull() {
        assertNull(agentRun.getStartedAt());

        Instant startedAt = Instant.now();
        UpdateAgentRunRequest request = new UpdateAgentRunRequest(
                null, null, null, null, null, null, null, null, startedAt
        );

        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.of(agentRun));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.save(any(AgentRun.class))).thenReturn(agentRun);

        agentRunService.updateAgentRun(agentRunId, request);

        assertEquals(startedAt, agentRun.getStartedAt());
    }

    @Test
    void updateAgentRun_notFound_throws() {
        UpdateAgentRunRequest request = new UpdateAgentRunRequest(
                AgentStatus.RUNNING, null, null, null, null, null, null, null, null
        );
        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> agentRunService.updateAgentRun(agentRunId, request));
    }

    @Test
    void updateAgentRun_notTeamMember_throws() {
        UpdateAgentRunRequest request = new UpdateAgentRunRequest(
                AgentStatus.RUNNING, null, null, null, null, null, null, null, null
        );
        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.of(agentRun));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> agentRunService.updateAgentRun(agentRunId, request));
    }

    // --- mapToResponse fields ---

    @Test
    void mapToResponse_allFieldsMapped() {
        agentRun.setResult(AgentResult.WARN);
        agentRun.setReportS3Key("s3/key");
        agentRun.setScore(72);
        agentRun.setFindingsCount(5);
        agentRun.setCriticalCount(1);
        agentRun.setHighCount(2);
        agentRun.setStartedAt(Instant.now().minusSeconds(30));
        agentRun.setCompletedAt(Instant.now());

        when(agentRunRepository.findById(agentRunId)).thenReturn(Optional.of(agentRun));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        AgentRunResponse response = agentRunService.getAgentRun(agentRunId);

        assertEquals(agentRunId, response.id());
        assertEquals(jobId, response.jobId());
        assertEquals(AgentType.SECURITY, response.agentType());
        assertEquals(AgentStatus.PENDING, response.status());
        assertEquals(AgentResult.WARN, response.result());
        assertEquals("s3/key", response.reportS3Key());
        assertEquals(72, response.score());
        assertEquals(5, response.findingsCount());
        assertEquals(1, response.criticalCount());
        assertEquals(2, response.highCount());
        assertNotNull(response.startedAt());
        assertNotNull(response.completedAt());
    }

    // --- getEligibleAdversarialAgents ---

    @Test
    void getEligibleAdversarialAgents_chaosMonkey_whenTestCoveragePassed() {
        project.setTechStack("Go 1.22");
        project.setDescription("CLI utility");
        AgentRun testCoverageRun = AgentRun.builder()
                .job(job).agentType(AgentType.TEST_COVERAGE)
                .status(AgentStatus.COMPLETED).findingsCount(0).criticalCount(0).highCount(0).build();
        testCoverageRun.setResult(AgentResult.PASS);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.findByJobIdAndAgentType(jobId, AgentType.TEST_COVERAGE))
                .thenReturn(Optional.of(testCoverageRun));

        List<AgentType> eligible = agentRunService.getEligibleAdversarialAgents(jobId);

        assertTrue(eligible.contains(AgentType.CHAOS_MONKEY),
                "CHAOS_MONKEY should be eligible when TEST_COVERAGE passed");
    }

    @Test
    void getEligibleAdversarialAgents_noChaosMonkey_whenTestCoverageFailed() {
        project.setTechStack("Go 1.22");
        project.setDescription("CLI utility");
        AgentRun testCoverageRun = AgentRun.builder()
                .job(job).agentType(AgentType.TEST_COVERAGE)
                .status(AgentStatus.COMPLETED).findingsCount(0).criticalCount(0).highCount(0).build();
        testCoverageRun.setResult(AgentResult.FAIL);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.findByJobIdAndAgentType(jobId, AgentType.TEST_COVERAGE))
                .thenReturn(Optional.of(testCoverageRun));

        List<AgentType> eligible = agentRunService.getEligibleAdversarialAgents(jobId);

        assertFalse(eligible.contains(AgentType.CHAOS_MONKEY),
                "CHAOS_MONKEY should not be eligible when TEST_COVERAGE failed");
    }

    @Test
    void getEligibleAdversarialAgents_hostileUser_whenProjectHasApiEndpoints() {
        project.setTechStack("Spring Boot 3.3, Java 21, PostgreSQL");
        project.setDescription("REST API microservice");

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.findByJobIdAndAgentType(jobId, AgentType.TEST_COVERAGE))
                .thenReturn(Optional.empty());

        List<AgentType> eligible = agentRunService.getEligibleAdversarialAgents(jobId);

        assertTrue(eligible.contains(AgentType.HOSTILE_USER),
                "HOSTILE_USER should be eligible for projects with API endpoints");
        assertTrue(eligible.contains(AgentType.LOAD_SABOTEUR),
                "LOAD_SABOTEUR should be eligible for backend API projects");
    }

    @Test
    void getEligibleAdversarialAgents_complianceAuditor_whenPiiInDescription() {
        project.setTechStack("Node.js, Express");
        project.setDescription("Healthcare service handling HIPAA-regulated patient data");

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.findByJobIdAndAgentType(jobId, AgentType.TEST_COVERAGE))
                .thenReturn(Optional.empty());

        List<AgentType> eligible = agentRunService.getEligibleAdversarialAgents(jobId);

        assertTrue(eligible.contains(AgentType.COMPLIANCE_AUDITOR),
                "COMPLIANCE_AUDITOR should be eligible for projects handling HIPAA data");
    }

    @Test
    void getEligibleAdversarialAgents_empty_whenNoIndicators() {
        project.setTechStack("Haskell");
        project.setDescription("Pure math library");

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.findByJobIdAndAgentType(jobId, AgentType.TEST_COVERAGE))
                .thenReturn(Optional.empty());

        List<AgentType> eligible = agentRunService.getEligibleAdversarialAgents(jobId);

        assertTrue(eligible.isEmpty(),
                "No adversarial agents should be eligible for a project with no matching indicators");
    }

    @Test
    void getEligibleAdversarialAgents_jobNotFound_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> agentRunService.getEligibleAdversarialAgents(jobId));
    }

    @Test
    void getEligibleAdversarialAgents_notTeamMember_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> agentRunService.getEligibleAdversarialAgents(jobId));
    }

    @Test
    void createAgentRun_withAdversarialType_success() {
        AgentRun adversarialRun = AgentRun.builder()
                .job(job).agentType(AgentType.CHAOS_MONKEY)
                .status(AgentStatus.PENDING).findingsCount(0).criticalCount(0).highCount(0).build();
        adversarialRun.setId(UUID.randomUUID());
        adversarialRun.setCreatedAt(Instant.now());

        CreateAgentRunRequest request = new CreateAgentRunRequest(jobId, AgentType.CHAOS_MONKEY);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(agentRunRepository.save(any(AgentRun.class))).thenReturn(adversarialRun);

        AgentRunResponse response = agentRunService.createAgentRun(request);

        assertNotNull(response);
        assertEquals(AgentType.CHAOS_MONKEY, response.agentType());
        assertEquals(AgentStatus.PENDING, response.status());
    }

    // --- heuristic helper tests ---

    @Test
    void hasApiOrUiIndicators_matchesTechStackKeywords() {
        assertTrue(AgentRunService.hasApiOrUiIndicators("", "rest api service"));
        assertTrue(AgentRunService.hasApiOrUiIndicators("react, node.js", ""));
        assertTrue(AgentRunService.hasApiOrUiIndicators("flutter", ""));
        assertTrue(AgentRunService.hasApiOrUiIndicators("", "graphql endpoint"));
        assertFalse(AgentRunService.hasApiOrUiIndicators("haskell", "math library"));
    }

    @Test
    void hasComplianceIndicators_matchesComplianceKeywords() {
        assertTrue(AgentRunService.hasComplianceIndicators("", "handles pii data"));
        assertTrue(AgentRunService.hasComplianceIndicators("", "hipaa regulated"));
        assertTrue(AgentRunService.hasComplianceIndicators("", "payment processing"));
        assertFalse(AgentRunService.hasComplianceIndicators("go", "utility tool"));
    }

    @Test
    void hasBackendApiIndicators_matchesBackendKeywords() {
        assertTrue(AgentRunService.hasBackendApiIndicators("spring boot 3.3", ""));
        assertTrue(AgentRunService.hasBackendApiIndicators("express", ""));
        assertTrue(AgentRunService.hasBackendApiIndicators("django", ""));
        assertTrue(AgentRunService.hasBackendApiIndicators("", "microservice for orders"));
        assertFalse(AgentRunService.hasBackendApiIndicators("haskell", "pure library"));
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
