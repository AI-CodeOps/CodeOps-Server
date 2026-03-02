package com.codeops.service;

import com.codeops.dto.request.CreateAgentRunRequest;
import com.codeops.dto.request.UpdateAgentRunRequest;
import com.codeops.dto.response.AgentRunResponse;
import com.codeops.entity.AgentRun;
import com.codeops.entity.Project;
import com.codeops.entity.enums.AgentResult;
import com.codeops.entity.enums.AgentStatus;
import com.codeops.entity.enums.AgentType;
import com.codeops.repository.AgentRunRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Manages the lifecycle of agent runs within QA jobs, including creation, retrieval, and status updates.
 *
 * <p>An agent run represents a single execution of a specific {@link AgentType} analysis agent
 * against a QA job. All operations verify that the current user is a member of the team
 * that owns the associated project.</p>
 *
 * @see AgentRunRepository
 * @see AgentRun
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AgentRunService {

    private static final Logger log = LoggerFactory.getLogger(AgentRunService.class);

    private final AgentRunRepository agentRunRepository;
    private final QaJobRepository qaJobRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Creates a single agent run for a QA job with initial status {@link AgentStatus#PENDING}
     * and all finding counters set to zero.
     *
     * @param request the creation request containing the job ID and agent type
     * @return the newly created agent run as a response DTO
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public AgentRunResponse createAgentRun(CreateAgentRunRequest request) {
        log.debug("createAgentRun called with jobId={}, agentType={}", request.jobId(), request.agentType());
        var job = qaJobRepository.findById(request.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        AgentRun run = AgentRun.builder()
                .job(job)
                .agentType(request.agentType())
                .status(AgentStatus.PENDING)
                .findingsCount(0)
                .criticalCount(0)
                .highCount(0)
                .build();

        run = agentRunRepository.save(run);
        log.info("Agent run created: id={}, jobId={}, agentType={}, status={}", run.getId(), request.jobId(), request.agentType(), AgentStatus.PENDING);
        return mapToResponse(run);
    }

    /**
     * Creates multiple agent runs for a QA job in a single batch, one per specified agent type.
     * Each run is initialized with {@link AgentStatus#PENDING} status and zero finding counters.
     *
     * @param jobId      the UUID of the QA job to associate the agent runs with
     * @param agentTypes the list of agent types to create runs for
     * @return a list of the newly created agent runs as response DTOs
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public List<AgentRunResponse> createAgentRuns(UUID jobId, List<AgentType> agentTypes) {
        log.debug("createAgentRuns called with jobId={}, agentTypes={}", jobId, agentTypes);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        List<AgentRun> runs = agentTypes.stream()
                .map(agentType -> AgentRun.builder()
                        .job(job)
                        .agentType(agentType)
                        .status(AgentStatus.PENDING)
                        .findingsCount(0)
                        .criticalCount(0)
                        .highCount(0)
                        .build())
                .toList();

        runs = agentRunRepository.saveAll(runs);
        log.info("Batch agent runs created: jobId={}, count={}, agentTypes={}", jobId, runs.size(), agentTypes);
        return runs.stream().map(this::mapToResponse).toList();
    }

    /**
     * Retrieves all agent runs associated with a given QA job.
     *
     * @param jobId the UUID of the QA job to retrieve runs for
     * @return a list of agent run response DTOs for the specified job
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public List<AgentRunResponse> getAgentRuns(UUID jobId) {
        log.debug("getAgentRuns called with jobId={}", jobId);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        return agentRunRepository.findByJobId(jobId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves a single agent run by its unique identifier.
     *
     * @param agentRunId the UUID of the agent run to retrieve
     * @return the agent run as a response DTO
     * @throws EntityNotFoundException if no agent run exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    @Transactional(readOnly = true)
    public AgentRunResponse getAgentRun(UUID agentRunId) {
        log.debug("getAgentRun called with agentRunId={}", agentRunId);
        AgentRun run = agentRunRepository.findById(agentRunId)
                .orElseThrow(() -> new EntityNotFoundException("Agent run not found"));
        verifyTeamMembership(run.getJob().getProject().getTeam().getId());
        return mapToResponse(run);
    }

    /**
     * Partially updates an agent run with the non-null fields from the request.
     *
     * <p>Supports updating status, result, report S3 key, score, finding counts,
     * and completion timestamp. The {@code startedAt} field is only set if it has not
     * been previously assigned (write-once semantics).</p>
     *
     * @param agentRunId the UUID of the agent run to update
     * @param request    the update request containing fields to modify (null fields are skipped)
     * @return the updated agent run as a response DTO
     * @throws EntityNotFoundException if no agent run exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    public AgentRunResponse updateAgentRun(UUID agentRunId, UpdateAgentRunRequest request) {
        log.debug("updateAgentRun called with agentRunId={}", agentRunId);
        AgentRun run = agentRunRepository.findById(agentRunId)
                .orElseThrow(() -> new EntityNotFoundException("Agent run not found"));
        verifyTeamMembership(run.getJob().getProject().getTeam().getId());

        AgentStatus previousStatus = run.getStatus();
        if (request.status() != null) run.setStatus(request.status());
        if (request.result() != null) run.setResult(request.result());
        if (request.reportS3Key() != null) run.setReportS3Key(request.reportS3Key());
        if (request.score() != null) run.setScore(request.score());
        if (request.findingsCount() != null) run.setFindingsCount(request.findingsCount());
        if (request.criticalCount() != null) run.setCriticalCount(request.criticalCount());
        if (request.highCount() != null) run.setHighCount(request.highCount());
        if (request.completedAt() != null) run.setCompletedAt(request.completedAt());
        if (request.startedAt() != null && run.getStartedAt() == null) {
            run.setStartedAt(request.startedAt());
        }

        run = agentRunRepository.save(run);
        if (request.status() != null && previousStatus != request.status()) {
            log.info("Agent run status transition: id={}, from={}, to={}", agentRunId, previousStatus, request.status());
        }
        if (request.completedAt() != null) {
            log.info("Agent run completed: id={}, status={}, score={}, findingsCount={}", agentRunId, run.getStatus(), run.getScore(), run.getFindingsCount());
        }
        return mapToResponse(run);
    }

    /**
     * Determines which Tier 3 (adversarial) agent types are eligible to run for a given QA job
     * based on the job's current agent run results and the project's characteristics.
     *
     * <p>Eligibility rules:</p>
     * <ul>
     *   <li>{@link AgentType#CHAOS_MONKEY} — Only when the {@link AgentType#TEST_COVERAGE} worker
     *       has completed with a {@link AgentResult#PASS} result.</li>
     *   <li>{@link AgentType#HOSTILE_USER} — When the project has API endpoints or UI components
     *       (determined by tech stack and description heuristics).</li>
     *   <li>{@link AgentType#COMPLIANCE_AUDITOR} — When the project handles PII, financial data,
     *       or has compliance-related requirements.</li>
     *   <li>{@link AgentType#LOAD_SABOTEUR} — When the project is a backend service with API endpoints.</li>
     * </ul>
     *
     * @param jobId the UUID of the QA job to evaluate
     * @return a list of eligible adversarial agent types (may be empty)
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public List<AgentType> getEligibleAdversarialAgents(UUID jobId) {
        log.debug("getEligibleAdversarialAgents called with jobId={}", jobId);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        Project project = job.getProject();
        List<AgentType> eligible = new ArrayList<>();

        // CHAOS_MONKEY: spawn only when TEST_COVERAGE worker confirms all tests pass
        agentRunRepository.findByJobIdAndAgentType(jobId, AgentType.TEST_COVERAGE)
                .filter(run -> run.getStatus() == AgentStatus.COMPLETED
                        && run.getResult() == AgentResult.PASS)
                .ifPresent(run -> eligible.add(AgentType.CHAOS_MONKEY));

        String techStack = project.getTechStack() != null
                ? project.getTechStack().toLowerCase(Locale.ROOT) : "";
        String description = project.getDescription() != null
                ? project.getDescription().toLowerCase(Locale.ROOT) : "";

        // HOSTILE_USER: spawn when project has API endpoints or UI components
        if (hasApiOrUiIndicators(techStack, description)) {
            eligible.add(AgentType.HOSTILE_USER);
        }

        // COMPLIANCE_AUDITOR: spawn when project handles PII, financial data, or compliance
        if (hasComplianceIndicators(techStack, description)) {
            eligible.add(AgentType.COMPLIANCE_AUDITOR);
        }

        // LOAD_SABOTEUR: spawn when project is a backend service with API endpoints
        if (hasBackendApiIndicators(techStack, description)) {
            eligible.add(AgentType.LOAD_SABOTEUR);
        }

        log.info("Eligible adversarial agents for jobId={}: {}", jobId, eligible);
        return eligible;
    }

    private static final Pattern API_UI_PATTERN = Pattern.compile(
            "rest|graphql|grpc|swagger|openapi|endpoint|api|react|angular|vue|flutter|"
                    + "swing|javafx|ui|frontend|web\\s*app|mobile",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COMPLIANCE_PATTERN = Pattern.compile(
            "pii|gdpr|hipaa|sox|pci|financial|payment|billing|healthcare|"
                    + "compliance|regulated|phi|ferpa|personal\\s*data|credit\\s*card",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BACKEND_API_PATTERN = Pattern.compile(
            "spring\\s*boot|express|fastify|django|flask|rails|"
                    + "gin|chi|fiber|actix|axum|nest|koa|hapi|"
                    + "microservice|backend|server|api\\s*gateway|rest\\s*api",
            Pattern.CASE_INSENSITIVE);

    /**
     * Checks if project indicators suggest it has API endpoints or UI components.
     */
    static boolean hasApiOrUiIndicators(String techStack, String description) {
        return API_UI_PATTERN.matcher(techStack).find()
                || API_UI_PATTERN.matcher(description).find();
    }

    /**
     * Checks if project indicators suggest it handles PII, financial data, or compliance requirements.
     */
    static boolean hasComplianceIndicators(String techStack, String description) {
        return COMPLIANCE_PATTERN.matcher(techStack).find()
                || COMPLIANCE_PATTERN.matcher(description).find();
    }

    /**
     * Checks if project indicators suggest it is a backend service with API endpoints.
     */
    static boolean hasBackendApiIndicators(String techStack, String description) {
        return BACKEND_API_PATTERN.matcher(techStack).find()
                || BACKEND_API_PATTERN.matcher(description).find();
    }

    private AgentRunResponse mapToResponse(AgentRun run) {
        return new AgentRunResponse(
                run.getId(),
                run.getJob().getId(),
                run.getAgentType(),
                run.getStatus(),
                run.getResult(),
                run.getReportS3Key(),
                run.getScore(),
                run.getFindingsCount(),
                run.getCriticalCount(),
                run.getHighCount(),
                run.getStartedAt(),
                run.getCompletedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }
}
