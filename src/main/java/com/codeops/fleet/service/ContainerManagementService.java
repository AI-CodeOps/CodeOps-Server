package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.CodeOpsException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.fleet.dto.mapper.ContainerInstanceMapper;
import com.codeops.fleet.dto.request.ContainerExecRequest;
import com.codeops.fleet.dto.request.StartContainerRequest;
import com.codeops.fleet.dto.response.ContainerDetailResponse;
import com.codeops.fleet.dto.response.ContainerInstanceResponse;
import com.codeops.fleet.dto.response.ContainerStatsResponse;
import com.codeops.fleet.entity.ContainerInstance;
import com.codeops.fleet.entity.EnvironmentVariable;
import com.codeops.fleet.entity.NetworkConfig;
import com.codeops.fleet.entity.PortMapping;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.VolumeMount;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.repository.ContainerInstanceRepository;
import com.codeops.fleet.repository.EnvironmentVariableRepository;
import com.codeops.fleet.repository.NetworkConfigRepository;
import com.codeops.fleet.repository.PortMappingRepository;
import com.codeops.fleet.repository.ServiceProfileRepository;
import com.codeops.fleet.repository.VolumeMountRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic layer managing Docker container lifecycle.
 *
 * <p>Bridges service profiles with the {@link DockerEngineService} to provide
 * complete container lifecycle management: start from profiles, stop, restart,
 * remove, inspect, fetch logs and stats. All operations are team-scoped and
 * persist container state to the database.</p>
 *
 * @see DockerEngineService
 * @see ServiceProfile
 * @see ContainerInstance
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContainerManagementService {

    private final DockerEngineService dockerEngineService;
    private final ContainerInstanceRepository containerInstanceRepository;
    private final ServiceProfileRepository serviceProfileRepository;
    private final PortMappingRepository portMappingRepository;
    private final EnvironmentVariableRepository environmentVariableRepository;
    private final VolumeMountRepository volumeMountRepository;
    private final NetworkConfigRepository networkConfigRepository;
    private final ContainerInstanceMapper containerInstanceMapper;
    private final TeamMemberRepository teamMemberRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // ═══════════════════════════════════════════════════════════════════
    //  Container Lifecycle Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Starts a new container from a service profile.
     *
     * <p>Performs the full start flow: verifies team access, loads the profile and its
     * child configurations (ports, environment variables, volumes, networks), checks
     * container limits, builds the Docker container config, creates and starts the
     * container, connects to networks, and persists the result.</p>
     *
     * @param teamId  the team ID
     * @param request the start container request with profile ID and optional overrides
     * @return the detail response for the newly created container
     * @throws NotFoundException      if the service profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another team
     * @throws ValidationException    if the profile is disabled or the container limit is reached
     */
    @Transactional
    public ContainerDetailResponse startContainer(UUID teamId, StartContainerRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        // Load and verify service profile
        ServiceProfile profile = serviceProfileRepository.findById(request.serviceProfileId())
                .orElseThrow(() -> new NotFoundException("ServiceProfile", request.serviceProfileId()));

        if (!profile.getTeam().getId().equals(teamId)) {
            throw new AuthorizationException("Service profile does not belong to this team");
        }

        if (!profile.isEnabled()) {
            throw new ValidationException("Service profile is disabled: " + profile.getServiceName());
        }

        // Check container limit
        long runningCount = containerInstanceRepository.countByTeamIdAndStatus(teamId, ContainerStatus.RUNNING);
        if (runningCount >= AppConstants.FLEET_MAX_CONTAINERS_PER_TEAM) {
            throw new ValidationException("Team has reached the maximum number of running containers: "
                    + AppConstants.FLEET_MAX_CONTAINERS_PER_TEAM);
        }

        // Load child configurations
        List<PortMapping> ports = portMappingRepository.findByServiceProfileId(profile.getId());
        List<EnvironmentVariable> envVars = environmentVariableRepository.findByServiceProfileId(profile.getId());
        List<VolumeMount> volumes = volumeMountRepository.findByServiceProfileId(profile.getId());
        List<NetworkConfig> networks = networkConfigRepository.findByServiceProfileId(profile.getId());

        // Apply overrides
        String imageTag = request.imageTagOverride() != null ? request.imageTagOverride() : profile.getImageTag();
        RestartPolicy restartPolicy = request.restartPolicyOverride() != null
                ? request.restartPolicyOverride() : profile.getRestartPolicy();

        // Generate container name
        String containerName = request.containerNameOverride() != null
                ? request.containerNameOverride()
                : profile.getServiceName() + "-" + UUID.randomUUID().toString().substring(0, 8);

        // Build Docker config and create container
        Map<String, Object> dockerConfig = buildDockerConfig(
                profile, ports, envVars, volumes, imageTag, request.envVarOverrides(), restartPolicy);
        String dockerContainerId = dockerEngineService.createContainer(containerName, dockerConfig);

        // Start container
        dockerEngineService.startContainer(dockerContainerId);

        // Connect to networks
        for (NetworkConfig network : networks) {
            List<String> aliases = parseAliases(network.getAliases());
            dockerEngineService.connectContainerToNetwork(
                    network.getNetworkName(), dockerContainerId, aliases);
        }

        // Persist container instance
        ContainerInstance container = ContainerInstance.builder()
                .containerId(dockerContainerId)
                .containerName(containerName)
                .serviceName(profile.getServiceName())
                .imageName(profile.getImageName())
                .imageTag(imageTag)
                .status(ContainerStatus.RUNNING)
                .healthStatus(HealthStatus.NONE)
                .restartPolicy(restartPolicy)
                .restartCount(0)
                .startedAt(Instant.now())
                .memoryLimitBytes(profile.getMemoryLimitMb() != null
                        ? (long) profile.getMemoryLimitMb() * 1024 * 1024 : null)
                .serviceProfile(profile)
                .team(profile.getTeam())
                .build();

        container = containerInstanceRepository.save(container);
        log.info("Started container {} ({}) for team {}", containerName, dockerContainerId, teamId);

        auditLogService.log(userId, teamId, "START_CONTAINER", "ContainerInstance",
                container.getId(), "Started container " + containerName + " from profile " + profile.getServiceName());

        return containerInstanceMapper.toDetailResponse(container);
    }

    /**
     * Stops a running container.
     *
     * @param teamId         the team ID
     * @param containerId    the container instance database ID
     * @param timeoutSeconds seconds to wait before sending SIGKILL
     * @return the updated container detail response
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the user is not a team member or the container belongs to another team
     */
    @Transactional
    public ContainerDetailResponse stopContainer(UUID teamId, UUID containerId, int timeoutSeconds) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ContainerInstance container = findContainerForTeam(containerId, teamId);

        dockerEngineService.stopContainer(container.getContainerId(), timeoutSeconds);

        container.setStatus(ContainerStatus.STOPPED);
        container.setFinishedAt(Instant.now());
        container = containerInstanceRepository.save(container);

        log.info("Stopped container {} for team {}", container.getContainerName(), teamId);
        auditLogService.log(userId, teamId, "STOP_CONTAINER", "ContainerInstance",
                container.getId(), "Stopped container " + container.getContainerName());

        return containerInstanceMapper.toDetailResponse(container);
    }

    /**
     * Restarts a container.
     *
     * @param teamId         the team ID
     * @param containerId    the container instance database ID
     * @param timeoutSeconds seconds to wait before sending SIGKILL during stop phase
     * @return the updated container detail response
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the user is not a team member or the container belongs to another team
     */
    @Transactional
    public ContainerDetailResponse restartContainer(UUID teamId, UUID containerId, int timeoutSeconds) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ContainerInstance container = findContainerForTeam(containerId, teamId);

        dockerEngineService.restartContainer(container.getContainerId(), timeoutSeconds);

        container.setStatus(ContainerStatus.RUNNING);
        container.setRestartCount(container.getRestartCount() + 1);
        container.setStartedAt(Instant.now());
        container = containerInstanceRepository.save(container);

        log.info("Restarted container {} for team {}", container.getContainerName(), teamId);
        auditLogService.log(userId, teamId, "RESTART_CONTAINER", "ContainerInstance",
                container.getId(), "Restarted container " + container.getContainerName());

        return containerInstanceMapper.toDetailResponse(container);
    }

    /**
     * Removes a container from Docker and deletes the database record.
     *
     * @param teamId      the team ID
     * @param containerId the container instance database ID
     * @param force       if true, force-removes even if the container is running
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the user is not a team member or the container belongs to another team
     */
    @Transactional
    public void removeContainer(UUID teamId, UUID containerId, boolean force) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ContainerInstance container = findContainerForTeam(containerId, teamId);

        if (container.getContainerId() != null) {
            dockerEngineService.removeContainer(container.getContainerId(), force, false);
        }

        String containerName = container.getContainerName();
        containerInstanceRepository.delete(container);

        log.info("Removed container {} from team {}", containerName, teamId);
        auditLogService.log(userId, teamId, "REMOVE_CONTAINER", "ContainerInstance",
                containerId, "Removed container " + containerName);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Container Inspection and Monitoring
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Retrieves detailed information for a container from the database.
     *
     * @param teamId      the team ID
     * @param containerId the container instance database ID
     * @return the container detail response
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the user is not a team member or the container belongs to another team
     */
    @Transactional(readOnly = true)
    public ContainerDetailResponse inspectContainer(UUID teamId, UUID containerId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ContainerInstance container = findContainerForTeam(containerId, teamId);
        return containerInstanceMapper.toDetailResponse(container);
    }

    /**
     * Retrieves container logs from the Docker daemon.
     *
     * @param teamId      the team ID
     * @param containerId the container instance database ID
     * @param tailLines   number of lines from the end of the log
     * @param timestamps  if true, includes timestamps in output
     * @return the log output string
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the user is not a team member or the container belongs to another team
     */
    @Transactional(readOnly = true)
    public String getContainerLogs(UUID teamId, UUID containerId, int tailLines, boolean timestamps) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ContainerInstance container = findContainerForTeam(containerId, teamId);
        return dockerEngineService.getContainerLogs(container.getContainerId(), tailLines, timestamps);
    }

    /**
     * Retrieves real-time resource statistics for a container.
     *
     * <p>The returned response includes the database entity UUID as the container ID,
     * not the Docker container ID.</p>
     *
     * @param teamId      the team ID
     * @param containerId the container instance database ID
     * @return the container stats response with entity UUID mapped
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the user is not a team member or the container belongs to another team
     */
    @Transactional(readOnly = true)
    public ContainerStatsResponse getContainerStats(UUID teamId, UUID containerId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ContainerInstance container = findContainerForTeam(containerId, teamId);
        ContainerStatsResponse stats = dockerEngineService.getContainerStats(container.getContainerId());

        return new ContainerStatsResponse(
                container.getId(),
                stats.containerName(),
                stats.cpuPercent(),
                stats.memoryUsageBytes(),
                stats.memoryLimitBytes(),
                stats.networkRxBytes(),
                stats.networkTxBytes(),
                stats.blockReadBytes(),
                stats.blockWriteBytes(),
                stats.pids(),
                stats.timestamp()
        );
    }

    /**
     * Executes a command inside a running container.
     *
     * @param teamId      the team ID
     * @param containerId the container instance database ID
     * @param request     the exec request with command and attachment flags
     * @return the command output string
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the user is not a team member or the container belongs to another team
     */
    @Transactional(readOnly = true)
    public String execInContainer(UUID teamId, UUID containerId, ContainerExecRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ContainerInstance container = findContainerForTeam(containerId, teamId);
        return dockerEngineService.execInContainer(
                container.getContainerId(), request.command(),
                request.attachStdout(), request.attachStderr());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Container Listing
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Lists all containers for a team.
     *
     * @param teamId the team ID
     * @return list of container summary responses
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional(readOnly = true)
    public List<ContainerInstanceResponse> listContainers(UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        List<ContainerInstance> containers = containerInstanceRepository.findByTeamId(teamId);
        return containerInstanceMapper.toResponseList(containers);
    }

    /**
     * Lists containers for a team filtered by status.
     *
     * @param teamId the team ID
     * @param status the container status filter
     * @return list of matching container summary responses
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional(readOnly = true)
    public List<ContainerInstanceResponse> listContainersByStatus(UUID teamId, ContainerStatus status) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        List<ContainerInstance> containers = containerInstanceRepository.findByTeamIdAndStatus(teamId, status);
        return containerInstanceMapper.toResponseList(containers);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  State Synchronization
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Synchronizes local container state with the Docker daemon.
     *
     * <p>Inspects each active container tracked in the database and updates its status,
     * exit code, PID, timestamps, error message, and health status from Docker.
     * Containers already in {@link ContainerStatus#EXITED} or {@link ContainerStatus#DEAD}
     * status are skipped. If Docker returns a 404 (container removed externally),
     * the container is marked as {@link ContainerStatus#EXITED}.</p>
     *
     * @param teamId the team ID
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional
    public void syncContainerState(UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        List<ContainerInstance> containers = containerInstanceRepository.findByTeamId(teamId);
        log.debug("Syncing state for {} containers in team {}", containers.size(), teamId);

        for (ContainerInstance container : containers) {
            if (container.getContainerId() == null) {
                continue;
            }
            if (container.getStatus() == ContainerStatus.EXITED
                    || container.getStatus() == ContainerStatus.DEAD) {
                continue;
            }
            syncSingleContainer(container);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Verifies that the user is a member of the specified team.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @throws AuthorizationException if the user is not a team member
     */
    private void verifyTeamAccess(UUID teamId, UUID userId) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new AuthorizationException("Not a member of this team"));
    }

    /**
     * Finds a container instance and verifies it belongs to the specified team.
     *
     * @param containerId the container instance database ID
     * @param teamId      the team ID
     * @return the container instance
     * @throws NotFoundException      if the container is not found
     * @throws AuthorizationException if the container belongs to another team
     */
    private ContainerInstance findContainerForTeam(UUID containerId, UUID teamId) {
        ContainerInstance container = containerInstanceRepository.findById(containerId)
                .orElseThrow(() -> new NotFoundException("ContainerInstance", containerId));
        if (!container.getTeam().getId().equals(teamId)) {
            throw new AuthorizationException("Container does not belong to this team");
        }
        return container;
    }

    /**
     * Builds the Docker Engine API container creation configuration from a service profile
     * and its child entities.
     *
     * @param profile         the service profile
     * @param ports           port mappings
     * @param envVars         environment variables
     * @param volumes         volume mounts
     * @param imageTag        effective image tag (may be overridden)
     * @param envVarOverrides environment variable overrides from the request
     * @param restartPolicy   effective restart policy (may be overridden)
     * @return the Docker container configuration map
     */
    private Map<String, Object> buildDockerConfig(ServiceProfile profile,
                                                   List<PortMapping> ports,
                                                   List<EnvironmentVariable> envVars,
                                                   List<VolumeMount> volumes,
                                                   String imageTag,
                                                   Map<String, String> envVarOverrides,
                                                   RestartPolicy restartPolicy) {
        Map<String, Object> config = new LinkedHashMap<>();

        config.put("Image", profile.getImageName() + ":" + imageTag);

        if (profile.getCommand() != null && !profile.getCommand().isBlank()) {
            config.put("Cmd", List.of("/bin/sh", "-c", profile.getCommand()));
        }

        if (profile.getEntrypoint() != null && !profile.getEntrypoint().isBlank()) {
            config.put("Entrypoint", List.of(profile.getEntrypoint()));
        }

        if (profile.getWorkingDir() != null && !profile.getWorkingDir().isBlank()) {
            config.put("WorkingDir", profile.getWorkingDir());
        }

        // Environment variables
        List<String> env = new ArrayList<>();
        for (EnvironmentVariable ev : envVars) {
            env.add(ev.getVariableKey() + "=" + ev.getVariableValue());
        }
        if (envVarOverrides != null) {
            envVarOverrides.forEach((k, v) -> env.add(k + "=" + v));
        }
        if (!env.isEmpty()) {
            config.put("Env", env);
        }

        // Exposed ports
        if (!ports.isEmpty()) {
            Map<String, Object> exposedPorts = new LinkedHashMap<>();
            for (PortMapping pm : ports) {
                exposedPorts.put(pm.getContainerPort() + "/" + pm.getProtocol(), Map.of());
            }
            config.put("ExposedPorts", exposedPorts);
        }

        // Health check
        if (profile.getHealthCheckCommand() != null && !profile.getHealthCheckCommand().isBlank()) {
            Map<String, Object> healthcheck = new LinkedHashMap<>();
            healthcheck.put("Test", List.of("CMD-SHELL", profile.getHealthCheckCommand()));
            healthcheck.put("Interval", (long) profile.getHealthCheckIntervalSeconds() * 1_000_000_000L);
            healthcheck.put("Timeout", (long) profile.getHealthCheckTimeoutSeconds() * 1_000_000_000L);
            healthcheck.put("Retries", profile.getHealthCheckRetries());
            config.put("Healthcheck", healthcheck);
        }

        // Host config
        Map<String, Object> hostConfig = new LinkedHashMap<>();

        if (!ports.isEmpty()) {
            Map<String, Object> portBindings = new LinkedHashMap<>();
            for (PortMapping pm : ports) {
                String key = pm.getContainerPort() + "/" + pm.getProtocol();
                portBindings.put(key, List.of(Map.of("HostPort", String.valueOf(pm.getHostPort()))));
            }
            hostConfig.put("PortBindings", portBindings);
        }

        if (!volumes.isEmpty()) {
            List<String> binds = new ArrayList<>();
            for (VolumeMount vm : volumes) {
                String source = vm.getHostPath() != null ? vm.getHostPath() : vm.getVolumeName();
                String bind = source + ":" + vm.getContainerPath();
                if (vm.isReadOnly()) {
                    bind += ":ro";
                }
                binds.add(bind);
            }
            hostConfig.put("Binds", binds);
        }

        hostConfig.put("RestartPolicy", Map.of("Name", mapRestartPolicyToDocker(restartPolicy)));

        if (profile.getMemoryLimitMb() != null) {
            hostConfig.put("Memory", (long) profile.getMemoryLimitMb() * 1024 * 1024);
        }

        if (profile.getCpuLimit() != null) {
            hostConfig.put("NanoCpus", (long) (profile.getCpuLimit() * 1_000_000_000L));
        }

        config.put("HostConfig", hostConfig);

        return config;
    }

    /**
     * Syncs a single container's state from Docker daemon inspection.
     *
     * @param container the container instance to sync
     */
    @SuppressWarnings("unchecked")
    private void syncSingleContainer(ContainerInstance container) {
        try {
            Map<String, Object> inspection = dockerEngineService.inspectContainer(container.getContainerId());
            Map<String, Object> state = (Map<String, Object>) inspection.get("State");
            if (state != null) {
                String dockerStatus = (String) state.get("Status");
                container.setStatus(mapDockerStatusToContainerStatus(dockerStatus));

                if (state.get("ExitCode") != null) {
                    container.setExitCode(((Number) state.get("ExitCode")).intValue());
                }

                if (state.get("Pid") != null && ((Number) state.get("Pid")).intValue() > 0) {
                    container.setPid(((Number) state.get("Pid")).intValue());
                }

                String startedAt = (String) state.get("StartedAt");
                if (startedAt != null && !startedAt.isEmpty() && !startedAt.startsWith("0001")) {
                    container.setStartedAt(Instant.parse(startedAt));
                }

                String finishedAt = (String) state.get("FinishedAt");
                if (finishedAt != null && !finishedAt.isEmpty() && !finishedAt.startsWith("0001")) {
                    container.setFinishedAt(Instant.parse(finishedAt));
                }

                String errorMsg = (String) state.get("Error");
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    container.setErrorMessage(errorMsg);
                }

                Map<String, Object> health = (Map<String, Object>) state.get("Health");
                if (health != null) {
                    String healthStatus = (String) health.get("Status");
                    container.setHealthStatus(mapDockerHealthStatus(healthStatus));
                }
            }
        } catch (CodeOpsException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("Container {} not found in Docker, marking as EXITED", container.getContainerId());
                container.setStatus(ContainerStatus.EXITED);
                container.setFinishedAt(Instant.now());
            } else {
                log.error("Failed to sync state for container {}: {}",
                        container.getContainerId(), e.getMessage());
            }
        }
        containerInstanceRepository.save(container);
    }

    /**
     * Parses a JSON array string of network aliases into a list.
     *
     * @param aliasesJson the JSON array string, or null
     * @return list of alias strings, empty if null or on parse error
     */
    private List<String> parseAliases(String aliasesJson) {
        if (aliasesJson == null || aliasesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(aliasesJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse network aliases: {}", aliasesJson);
            return Collections.emptyList();
        }
    }

    /**
     * Maps a {@link RestartPolicy} enum value to its Docker Engine API string representation.
     *
     * @param policy the restart policy
     * @return the Docker API restart policy name
     */
    private String mapRestartPolicyToDocker(RestartPolicy policy) {
        return switch (policy) {
            case NO -> "no";
            case ALWAYS -> "always";
            case ON_FAILURE -> "on-failure";
            case UNLESS_STOPPED -> "unless-stopped";
        };
    }

    /**
     * Maps a Docker container status string to a {@link ContainerStatus} enum value.
     *
     * @param dockerStatus the Docker status string (e.g., "running", "exited")
     * @return the corresponding ContainerStatus enum value
     */
    private ContainerStatus mapDockerStatusToContainerStatus(String dockerStatus) {
        if (dockerStatus == null) {
            return ContainerStatus.STOPPED;
        }
        return switch (dockerStatus.toLowerCase()) {
            case "running" -> ContainerStatus.RUNNING;
            case "paused" -> ContainerStatus.PAUSED;
            case "restarting" -> ContainerStatus.RESTARTING;
            case "removing" -> ContainerStatus.REMOVING;
            case "exited" -> ContainerStatus.EXITED;
            case "dead" -> ContainerStatus.DEAD;
            case "created" -> ContainerStatus.CREATED;
            default -> ContainerStatus.STOPPED;
        };
    }

    /**
     * Maps a Docker health status string to a {@link HealthStatus} enum value.
     *
     * @param dockerHealth the Docker health status string
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
