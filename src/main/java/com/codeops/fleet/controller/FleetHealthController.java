package com.codeops.fleet.controller;

import com.codeops.config.AppConstants;
import com.codeops.fleet.dto.response.ContainerHealthCheckResponse;
import com.codeops.fleet.dto.response.FleetHealthSummaryResponse;
import com.codeops.fleet.service.FleetHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Fleet health monitoring.
 *
 * <p>Provides endpoints for checking individual or all container health,
 * retrieving health check history, getting fleet-wide health summaries,
 * and purging stale health check records.</p>
 *
 * <p>The {@code GET /summary} endpoint is publicly accessible (whitelisted in
 * {@link com.codeops.security.SecurityConfig}) for monitoring dashboards.
 * All other endpoints require authentication and ADMIN or OWNER role.</p>
 */
@RestController
@RequestMapping(AppConstants.FLEET_API_PREFIX + "/health")
@RequiredArgsConstructor
@Slf4j
public class FleetHealthController {

    private final FleetHealthService fleetHealthService;

    /**
     * Returns a fleet-wide health summary for a team.
     *
     * <p>This endpoint is publicly accessible for monitoring dashboards.</p>
     *
     * @param teamId the team ID
     * @return the fleet health summary
     */
    @GetMapping("/summary")
    public FleetHealthSummaryResponse getFleetHealthSummary(@RequestParam UUID teamId) {
        return fleetHealthService.getFleetHealthSummary(teamId);
    }

    /**
     * Runs a health check on a single container.
     *
     * @param teamId      the team ID
     * @param containerId the container instance ID
     * @return the health check result
     */
    @PostMapping("/containers/{containerId}/check")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ContainerHealthCheckResponse checkContainerHealth(
            @RequestParam UUID teamId,
            @PathVariable UUID containerId) {
        return fleetHealthService.checkContainerHealth(teamId, containerId);
    }

    /**
     * Runs health checks on all running containers for a team.
     *
     * @param teamId the team ID
     * @return the list of health check results
     */
    @PostMapping("/containers/check-all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public List<ContainerHealthCheckResponse> checkAllContainerHealth(@RequestParam UUID teamId) {
        return fleetHealthService.checkAllContainerHealth(teamId);
    }

    /**
     * Retrieves health check history for a container.
     *
     * @param teamId      the team ID
     * @param containerId the container instance ID
     * @param limit       maximum number of records to return (default 20)
     * @return the list of historical health check results
     */
    @GetMapping("/containers/{containerId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public List<ContainerHealthCheckResponse> getHealthCheckHistory(
            @RequestParam UUID teamId,
            @PathVariable UUID containerId,
            @RequestParam(defaultValue = "20") int limit) {
        return fleetHealthService.getHealthCheckHistory(teamId, containerId, limit);
    }

    /**
     * Purges health check records older than the retention period.
     *
     * @param teamId the team ID
     * @return the number of records deleted
     */
    @PostMapping("/purge")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    @ResponseStatus(HttpStatus.OK)
    public int purgeOldHealthChecks(@RequestParam UUID teamId) {
        return fleetHealthService.purgeOldHealthChecks(teamId);
    }
}
