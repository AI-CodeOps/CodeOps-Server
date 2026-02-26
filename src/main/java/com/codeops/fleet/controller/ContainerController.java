package com.codeops.fleet.controller;

import com.codeops.config.AppConstants;
import com.codeops.fleet.dto.request.ContainerExecRequest;
import com.codeops.fleet.dto.request.StartContainerRequest;
import com.codeops.fleet.dto.response.ContainerDetailResponse;
import com.codeops.fleet.dto.response.ContainerInstanceResponse;
import com.codeops.fleet.dto.response.ContainerStatsResponse;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.service.ContainerManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Docker container lifecycle management in the Fleet module.
 *
 * <p>Provides endpoints for starting, stopping, restarting, and removing containers,
 * as well as inspection, log retrieval, resource stats, command execution, and
 * state synchronisation with the Docker daemon.</p>
 *
 * <p>All endpoints require authentication and ADMIN or OWNER role.
 * Team membership is verified in the service layer.</p>
 */
@RestController
@RequestMapping(AppConstants.FLEET_API_PREFIX + "/containers")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class ContainerController {

    private final ContainerManagementService containerManagementService;

    /**
     * Starts a new container from a service profile.
     *
     * @param teamId  the team ID
     * @param request the container start request
     * @return the started container details
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContainerDetailResponse startContainer(
            @RequestParam UUID teamId,
            @RequestBody @Valid StartContainerRequest request) {
        return containerManagementService.startContainer(teamId, request);
    }

    /**
     * Stops a running container.
     *
     * @param teamId         the team ID
     * @param containerId    the container instance ID
     * @param timeoutSeconds seconds to wait before force-killing (default 10)
     * @return the updated container details
     */
    @PostMapping("/{containerId}/stop")
    public ContainerDetailResponse stopContainer(
            @RequestParam UUID teamId,
            @PathVariable UUID containerId,
            @RequestParam(defaultValue = "10") int timeoutSeconds) {
        return containerManagementService.stopContainer(teamId, containerId, timeoutSeconds);
    }

    /**
     * Restarts a container.
     *
     * @param teamId         the team ID
     * @param containerId    the container instance ID
     * @param timeoutSeconds seconds to wait before force-killing (default 10)
     * @return the updated container details
     */
    @PostMapping("/{containerId}/restart")
    public ContainerDetailResponse restartContainer(
            @RequestParam UUID teamId,
            @PathVariable UUID containerId,
            @RequestParam(defaultValue = "10") int timeoutSeconds) {
        return containerManagementService.restartContainer(teamId, containerId, timeoutSeconds);
    }

    /**
     * Removes a container.
     *
     * @param teamId      the team ID
     * @param containerId the container instance ID
     * @param force       whether to force-remove a running container
     */
    @DeleteMapping("/{containerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeContainer(
            @RequestParam UUID teamId,
            @PathVariable UUID containerId,
            @RequestParam(defaultValue = "false") boolean force) {
        containerManagementService.removeContainer(teamId, containerId, force);
    }

    /**
     * Inspects a container and returns full details.
     *
     * @param teamId      the team ID
     * @param containerId the container instance ID
     * @return the container detail response
     */
    @GetMapping("/{containerId}")
    public ContainerDetailResponse inspectContainer(
            @RequestParam UUID teamId,
            @PathVariable UUID containerId) {
        return containerManagementService.inspectContainer(teamId, containerId);
    }

    /**
     * Retrieves container log output.
     *
     * @param teamId      the team ID
     * @param containerId the container instance ID
     * @param tailLines   number of tail lines to retrieve (default 100)
     * @param timestamps  whether to include timestamps (default false)
     * @return the raw log output
     */
    @GetMapping("/{containerId}/logs")
    public String getContainerLogs(
            @RequestParam UUID teamId,
            @PathVariable UUID containerId,
            @RequestParam(defaultValue = "100") int tailLines,
            @RequestParam(defaultValue = "false") boolean timestamps) {
        return containerManagementService.getContainerLogs(teamId, containerId, tailLines, timestamps);
    }

    /**
     * Retrieves real-time resource stats for a container.
     *
     * @param teamId      the team ID
     * @param containerId the container instance ID
     * @return the container stats
     */
    @GetMapping("/{containerId}/stats")
    public ContainerStatsResponse getContainerStats(
            @RequestParam UUID teamId,
            @PathVariable UUID containerId) {
        return containerManagementService.getContainerStats(teamId, containerId);
    }

    /**
     * Executes a command inside a running container.
     *
     * @param teamId      the team ID
     * @param containerId the container instance ID
     * @param request     the exec request containing the command
     * @return the command output
     */
    @PostMapping("/{containerId}/exec")
    public String execInContainer(
            @RequestParam UUID teamId,
            @PathVariable UUID containerId,
            @RequestBody @Valid ContainerExecRequest request) {
        return containerManagementService.execInContainer(teamId, containerId, request);
    }

    /**
     * Lists all containers for a team.
     *
     * @param teamId the team ID
     * @return the list of container instances
     */
    @GetMapping
    public List<ContainerInstanceResponse> listContainers(@RequestParam UUID teamId) {
        return containerManagementService.listContainers(teamId);
    }

    /**
     * Lists containers for a team filtered by status.
     *
     * @param teamId the team ID
     * @param status the container status to filter by
     * @return the filtered list of container instances
     */
    @GetMapping("/by-status")
    public List<ContainerInstanceResponse> listContainersByStatus(
            @RequestParam UUID teamId,
            @RequestParam ContainerStatus status) {
        return containerManagementService.listContainersByStatus(teamId, status);
    }

    /**
     * Synchronises local container state with the Docker daemon.
     *
     * @param teamId the team ID
     */
    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncContainerState(@RequestParam UUID teamId) {
        containerManagementService.syncContainerState(teamId);
    }
}
