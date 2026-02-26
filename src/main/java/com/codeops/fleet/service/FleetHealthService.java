package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.fleet.dto.mapper.ContainerHealthCheckMapper;
import com.codeops.fleet.dto.response.ContainerHealthCheckResponse;
import com.codeops.fleet.dto.response.FleetHealthSummaryResponse;
import com.codeops.fleet.entity.ContainerHealthCheck;
import com.codeops.fleet.entity.ContainerInstance;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.repository.ContainerHealthCheckRepository;
import com.codeops.fleet.repository.ContainerInstanceRepository;
import com.codeops.fleet.repository.ServiceProfileRepository;
import com.codeops.relay.entity.enums.PlatformEventType;
import com.codeops.relay.service.PlatformEventService;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Monitors container health, detects crash loops, manages automatic restarts,
 * and fires platform events to Relay on health failures. Integrates with
 * {@link PlatformEventService} for cross-module alerting.
 *
 * <p>This service provides the health check logic invoked on-demand or by
 * scheduled polling (wired in the controller layer). It does not schedule
 * its own tasks.</p>
 *
 * @see DockerEngineService
 * @see ContainerManagementService
 * @see PlatformEventService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FleetHealthService {

    private final DockerEngineService dockerEngineService;
    private final ContainerManagementService containerManagementService;
    private final ContainerInstanceRepository containerInstanceRepository;
    private final ContainerHealthCheckRepository containerHealthCheckRepository;
    private final ServiceProfileRepository serviceProfileRepository;
    private final ContainerHealthCheckMapper containerHealthCheckMapper;
    private final PlatformEventService platformEventService;
    private final TeamMemberRepository teamMemberRepository;

    // ═══════════════════════════════════════════════════════════════════
    //  Health Check Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Runs a health check for a single container.
     *
     * <p>Calls Docker inspect, extracts the health status, persists the result,
     * fires a status change event if the health state transitioned, and triggers
     * auto-restart or crash loop detection if the container is unhealthy.</p>
     *
     * @param teamId      the team ID
     * @param containerId the container instance database ID
     * @return the health check response
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the user is not a team member
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public ContainerHealthCheckResponse checkContainerHealth(UUID teamId, UUID containerId) {
        verifyTeamAccess(teamId);
        ContainerInstance container = getContainerOrThrow(teamId, containerId);

        // Container not yet started — no Docker container ID to inspect
        if (container.getContainerId() == null) {
            ContainerHealthCheck check = ContainerHealthCheck.builder()
                    .status(HealthStatus.NONE)
                    .output("Container not started")
                    .container(container)
                    .build();
            check = containerHealthCheckRepository.save(check);
            return containerHealthCheckMapper.toResponse(check);
        }

        HealthStatus newStatus;
        String output = null;
        Integer exitCode = null;
        Long durationMs = null;

        try {
            Map<String, Object> inspection = dockerEngineService.inspectContainer(container.getContainerId());
            Map<String, Object> state = (Map<String, Object>) inspection.get("State");

            if (state == null) {
                newStatus = HealthStatus.NONE;
            } else {
                Map<String, Object> health = (Map<String, Object>) state.get("Health");
                if (health != null) {
                    String dockerHealthStatus = (String) health.get("Status");
                    newStatus = mapDockerHealthStatus(dockerHealthStatus);

                    // Extract last health check log entry
                    List<Map<String, Object>> healthLog =
                            (List<Map<String, Object>>) health.get("Log");
                    if (healthLog != null && !healthLog.isEmpty()) {
                        Map<String, Object> lastEntry = healthLog.get(healthLog.size() - 1);
                        output = (String) lastEntry.get("Output");
                        if (lastEntry.get("ExitCode") != null) {
                            exitCode = ((Number) lastEntry.get("ExitCode")).intValue();
                        }
                        String start = (String) lastEntry.get("Start");
                        String end = (String) lastEntry.get("End");
                        if (start != null && end != null) {
                            try {
                                Instant startTime = Instant.parse(start);
                                Instant endTime = Instant.parse(end);
                                durationMs = java.time.Duration.between(startTime, endTime).toMillis();
                            } catch (Exception ignored) {
                                // Timestamp parse errors are non-fatal
                            }
                        }
                    }
                } else {
                    // No health check configured — infer from container running state
                    String dockerStatus = (String) state.get("Status");
                    newStatus = "running".equalsIgnoreCase(dockerStatus)
                            ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to inspect container {} for health check: {}",
                    container.getContainerId(), e.getMessage());
            newStatus = HealthStatus.NONE;
            output = "Docker inspection failed: " + e.getMessage();
        }

        // Persist health check record
        ContainerHealthCheck check = ContainerHealthCheck.builder()
                .status(newStatus)
                .output(output)
                .exitCode(exitCode)
                .durationMs(durationMs)
                .container(container)
                .build();
        check = containerHealthCheckRepository.save(check);

        // Detect and handle status change
        HealthStatus oldStatus = container.getHealthStatus();
        if (oldStatus != newStatus) {
            container.setHealthStatus(newStatus);
            containerInstanceRepository.save(container);
            fireHealthStatusChangeEvent(container, oldStatus, newStatus);
        }

        // Handle unhealthy container (auto-restart / crash loop detection)
        if (newStatus == HealthStatus.UNHEALTHY) {
            handleUnhealthyContainer(container);
        }

        return containerHealthCheckMapper.toResponse(check);
    }

    /**
     * Runs health checks for all running containers in a team.
     *
     * <p>Used by scheduled polling or on-demand from the controller. Only
     * containers with status {@link ContainerStatus#RUNNING} are checked.</p>
     *
     * @param teamId the team ID
     * @return list of health check responses for each checked container
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional
    public List<ContainerHealthCheckResponse> checkAllContainerHealth(UUID teamId) {
        verifyTeamAccess(teamId);

        List<ContainerInstance> runningContainers =
                containerInstanceRepository.findByTeamIdAndStatus(teamId, ContainerStatus.RUNNING);

        List<ContainerHealthCheckResponse> results = new ArrayList<>();
        for (ContainerInstance container : runningContainers) {
            try {
                results.add(checkContainerHealth(teamId, container.getId()));
            } catch (Exception e) {
                log.warn("Health check failed for container {}: {}",
                        container.getContainerName(), e.getMessage());
            }
        }
        return results;
    }

    /**
     * Retrieves health check history for a container, newest first.
     *
     * @param teamId      the team ID
     * @param containerId the container instance database ID
     * @param limit       maximum number of records to return
     * @return list of health check responses ordered by creation time descending
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional(readOnly = true)
    public List<ContainerHealthCheckResponse> getHealthCheckHistory(UUID teamId, UUID containerId, int limit) {
        verifyTeamAccess(teamId);
        getContainerOrThrow(teamId, containerId);

        Page<ContainerHealthCheck> page = containerHealthCheckRepository.findByContainerId(
                containerId, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")));

        return containerHealthCheckMapper.toResponseList(page.getContent());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Fleet Health Summary
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Computes an aggregate fleet health summary for a team.
     *
     * <p>Counts containers by status (running, stopped, unhealthy, restarting)
     * and sums CPU and memory usage from cached values on each container entity
     * (updated by the sync operation in {@link ContainerManagementService}).</p>
     *
     * @param teamId the team ID
     * @return the fleet health summary
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional(readOnly = true)
    public FleetHealthSummaryResponse getFleetHealthSummary(UUID teamId) {
        verifyTeamAccess(teamId);

        List<ContainerInstance> containers = containerInstanceRepository.findByTeamId(teamId);

        int total = containers.size();
        int running = 0;
        int stopped = 0;
        int unhealthy = 0;
        int restarting = 0;
        double totalCpu = 0.0;
        long totalMemory = 0L;
        long totalMemoryLimit = 0L;

        for (ContainerInstance c : containers) {
            switch (c.getStatus()) {
                case RUNNING -> running++;
                case STOPPED, EXITED -> stopped++;
                case RESTARTING -> restarting++;
                default -> { /* CREATED, PAUSED, REMOVING, DEAD — not counted in named buckets */ }
            }
            if (c.getHealthStatus() == HealthStatus.UNHEALTHY) {
                unhealthy++;
            }
            if (c.getCpuPercent() != null) {
                totalCpu += c.getCpuPercent();
            }
            if (c.getMemoryBytes() != null) {
                totalMemory += c.getMemoryBytes();
            }
            if (c.getMemoryLimitBytes() != null) {
                totalMemoryLimit += c.getMemoryLimitBytes();
            }
        }

        return new FleetHealthSummaryResponse(
                total, running, stopped, unhealthy, restarting,
                totalCpu, totalMemory, totalMemoryLimit, Instant.now());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Crash Loop Detection
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Detects if a container is in a crash loop.
     *
     * <p>A crash loop is defined as {@link AppConstants#FLEET_CRASH_LOOP_THRESHOLD} (3)
     * or more UNHEALTHY health check records within the last
     * {@link AppConstants#FLEET_CRASH_LOOP_WINDOW_MINUTES} (5) minutes.
     * If a crash loop is detected, the container is stopped and a
     * {@link PlatformEventType#CONTAINER_CRASHED} event is fired.</p>
     *
     * @param container the container instance to check
     * @return true if a crash loop was detected and the container was stopped
     */
    @Transactional
    public boolean detectCrashLoop(ContainerInstance container) {
        Instant windowStart = Instant.now().minus(
                AppConstants.FLEET_CRASH_LOOP_WINDOW_MINUTES, ChronoUnit.MINUTES);

        long unhealthyCount = containerHealthCheckRepository
                .countByContainerIdAndStatusAndCreatedAtAfter(
                        container.getId(), HealthStatus.UNHEALTHY, windowStart);

        if (unhealthyCount >= AppConstants.FLEET_CRASH_LOOP_THRESHOLD) {
            log.error("Crash loop detected for container {}: {} unhealthy checks in last {} minutes",
                    container.getContainerName(), unhealthyCount,
                    AppConstants.FLEET_CRASH_LOOP_WINDOW_MINUTES);

            // Stop the container to break the crash loop
            try {
                containerManagementService.stopContainer(
                        container.getTeam().getId(), container.getId(), 10);
            } catch (Exception e) {
                log.error("Failed to stop crash-looping container {}: {}",
                        container.getContainerName(), e.getMessage());
            }

            fireContainerCrashedEvent(container, "Crash loop detected: "
                    + unhealthyCount + " failures in "
                    + AppConstants.FLEET_CRASH_LOOP_WINDOW_MINUTES + " minutes");
            return true;
        }

        return false;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Auto-Restart
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Handles an unhealthy container according to its restart policy.
     *
     * <p>Before restarting, checks for a crash loop. If a crash loop is detected,
     * the container is stopped instead of restarted. Policy behavior:</p>
     * <ul>
     *   <li>{@link RestartPolicy#ALWAYS} — restart immediately</li>
     *   <li>{@link RestartPolicy#ON_FAILURE} — restart only if exit code != 0</li>
     *   <li>{@link RestartPolicy#UNLESS_STOPPED} — restart unless manually stopped</li>
     *   <li>{@link RestartPolicy#NO} — do nothing</li>
     * </ul>
     *
     * @param container the unhealthy container instance
     */
    @Transactional
    public void handleUnhealthyContainer(ContainerInstance container) {
        // Check for crash loop before attempting any restart
        if (detectCrashLoop(container)) {
            return;
        }

        RestartPolicy policy = container.getRestartPolicy();
        switch (policy) {
            case ALWAYS -> restartContainer(container);
            case ON_FAILURE -> {
                if (container.getExitCode() != null && container.getExitCode() != 0) {
                    restartContainer(container);
                }
            }
            case UNLESS_STOPPED -> {
                if (container.getStatus() != ContainerStatus.STOPPED) {
                    restartContainer(container);
                }
            }
            case NO -> log.debug("Restart policy is NO for container {}, skipping auto-restart",
                    container.getContainerName());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Platform Event Integration
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Fires a {@link PlatformEventType#CONTAINER_CRASHED} platform event to Relay.
     *
     * <p>Routes to the service's channel (via the service registration ID
     * as sourceEntityId) if the container is linked to a Registry service.
     * Falls back to the team's #general channel otherwise.</p>
     *
     * @param container the crashed container
     * @param reason    human-readable crash reason
     */
    private void fireContainerCrashedEvent(ContainerInstance container, String reason) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID teamId = container.getTeam().getId();

        // Route to service channel if container is linked to a registry service
        UUID sourceEntityId = resolveServiceEntityId(container);

        String title = "Container Crashed: " + container.getContainerName();
        String detail = String.format(
                "Container %s crashed — %s. Service: %s, Restarts: %d, Exit code: %s",
                container.getContainerName(),
                reason,
                container.getServiceName(),
                container.getRestartCount(),
                container.getExitCode() != null ? container.getExitCode().toString() : "N/A");

        try {
            platformEventService.publishEvent(
                    PlatformEventType.CONTAINER_CRASHED,
                    teamId,
                    sourceEntityId,
                    "fleet",
                    title,
                    detail,
                    userId,
                    null);
        } catch (Exception e) {
            log.error("Failed to fire CONTAINER_CRASHED event for container {}: {}",
                    container.getContainerName(), e.getMessage());
        }
    }

    /**
     * Fires a health status change event.
     *
     * <p>Fires an {@link PlatformEventType#ALERT_FIRED} event when a container
     * transitions to UNHEALTHY (alert) or recovers from UNHEALTHY to HEALTHY
     * (recovery notification). Other transitions are not reported.</p>
     *
     * @param container the container whose health status changed
     * @param oldStatus the previous health status
     * @param newStatus the new health status
     */
    private void fireHealthStatusChangeEvent(ContainerInstance container,
                                              HealthStatus oldStatus,
                                              HealthStatus newStatus) {
        String title;
        String detail;

        if (newStatus == HealthStatus.UNHEALTHY) {
            title = "Container Unhealthy: " + container.getContainerName();
            detail = String.format("Container %s transitioned from %s to UNHEALTHY. Service: %s",
                    container.getContainerName(), oldStatus, container.getServiceName());
        } else if (newStatus == HealthStatus.HEALTHY && oldStatus == HealthStatus.UNHEALTHY) {
            title = "Container Recovered: " + container.getContainerName();
            detail = String.format("Container %s recovered from UNHEALTHY to HEALTHY. Service: %s",
                    container.getContainerName(), container.getServiceName());
        } else {
            return; // Only alert on UNHEALTHY transitions and UNHEALTHY→HEALTHY recovery
        }

        UUID userId = SecurityUtils.getCurrentUserId();
        UUID teamId = container.getTeam().getId();
        UUID sourceEntityId = resolveServiceEntityId(container);

        try {
            platformEventService.publishEvent(
                    PlatformEventType.ALERT_FIRED,
                    teamId,
                    sourceEntityId,
                    "fleet",
                    title,
                    detail,
                    userId,
                    null);
        } catch (Exception e) {
            log.error("Failed to fire health status change event for container {}: {}",
                    container.getContainerName(), e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Log Retention
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Purges health check records older than {@link AppConstants#FLEET_MAX_LOG_RETENTION_DAYS}.
     *
     * <p>Deletes all health check records for containers in the specified team
     * that were created before the retention window. Called periodically or
     * from an admin endpoint.</p>
     *
     * @param teamId the team ID
     * @return the number of health check records deleted
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional
    public int purgeOldHealthChecks(UUID teamId) {
        verifyTeamAccess(teamId);

        Instant cutoff = Instant.now().minus(AppConstants.FLEET_MAX_LOG_RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = containerHealthCheckRepository.deleteByTeamIdAndCreatedAtBefore(teamId, cutoff);

        log.info("Purged {} health check records older than {} days for team {}",
                deleted, AppConstants.FLEET_MAX_LOG_RETENTION_DAYS, teamId);
        return deleted;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Internal Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Verifies that the current user is a member of the specified team.
     *
     * @param teamId the team ID
     * @throws AuthorizationException if the user is not a team member
     */
    private void verifyTeamAccess(UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new AuthorizationException("Not a member of this team"));
    }

    /**
     * Finds a container instance and verifies it belongs to the specified team.
     *
     * @param teamId      the team ID
     * @param containerId the container instance database ID
     * @return the container instance
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the container belongs to another team
     */
    private ContainerInstance getContainerOrThrow(UUID teamId, UUID containerId) {
        ContainerInstance container = containerInstanceRepository.findById(containerId)
                .orElseThrow(() -> new NotFoundException("ContainerInstance", containerId));
        if (!container.getTeam().getId().equals(teamId)) {
            throw new AuthorizationException("Container does not belong to this team");
        }
        return container;
    }

    /**
     * Restarts a container via the container management service.
     *
     * @param container the container to restart
     */
    private void restartContainer(ContainerInstance container) {
        try {
            containerManagementService.restartContainer(
                    container.getTeam().getId(), container.getId(), 10);
            log.info("Auto-restarted unhealthy container {} per {} policy",
                    container.getContainerName(), container.getRestartPolicy());
        } catch (Exception e) {
            log.error("Failed to auto-restart container {}: {}",
                    container.getContainerName(), e.getMessage());
        }
    }

    /**
     * Resolves the service registration entity ID for channel routing.
     *
     * <p>If the container is linked to a service profile that was generated
     * from a Registry service registration, returns that registration ID
     * so that {@link PlatformEventService} can route the event to the
     * SERVICE channel. Returns null otherwise (falls back to #general).</p>
     *
     * @param container the container instance
     * @return the service registration ID, or null
     */
    private UUID resolveServiceEntityId(ContainerInstance container) {
        if (container.getServiceProfile() != null
                && container.getServiceProfile().getServiceRegistration() != null) {
            return container.getServiceProfile().getServiceRegistration().getId();
        }
        return null;
    }

    /**
     * Maps a Docker health status string to a {@link HealthStatus} enum value.
     *
     * @param dockerHealth the Docker health status string (healthy, unhealthy, starting, none)
     * @return the corresponding HealthStatus enum value
     */
    private HealthStatus mapDockerHealthStatus(String dockerHealth) {
        if (dockerHealth == null) {
            return HealthStatus.NONE;
        }
        return switch (dockerHealth.toLowerCase()) {
            case "healthy" -> HealthStatus.HEALTHY;
            case "unhealthy" -> HealthStatus.UNHEALTHY;
            case "starting" -> HealthStatus.STARTING;
            default -> HealthStatus.NONE;
        };
    }
}
