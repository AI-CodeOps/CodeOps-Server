package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.fleet.dto.mapper.SolutionProfileMapper;
import com.codeops.fleet.dto.mapper.SolutionServiceMapper;
import com.codeops.fleet.dto.request.AddSolutionServiceRequest;
import com.codeops.fleet.dto.request.CreateSolutionProfileRequest;
import com.codeops.fleet.dto.request.StartContainerRequest;
import com.codeops.fleet.dto.request.UpdateSolutionProfileRequest;
import com.codeops.fleet.dto.response.SolutionProfileDetailResponse;
import com.codeops.fleet.dto.response.SolutionProfileResponse;
import com.codeops.fleet.dto.response.SolutionServiceResponse;
import com.codeops.fleet.entity.ContainerInstance;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.SolutionProfile;
import com.codeops.fleet.entity.SolutionService;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.repository.ContainerInstanceRepository;
import com.codeops.fleet.repository.ServiceProfileRepository;
import com.codeops.fleet.repository.SolutionProfileRepository;
import com.codeops.fleet.repository.SolutionServiceRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for managing solution profiles and orchestrated multi-service operations.
 *
 * <p>A solution profile groups multiple service profiles into a deployable stack.
 * This service provides full CRUD for solution profiles and their service assignments,
 * plus orchestrated start/stop that launches services in start-order sequence with
 * health check polling between each service.</p>
 *
 * @see SolutionProfile
 * @see SolutionService
 * @see ContainerManagementService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SolutionProfileService {

    private static final long HEALTH_POLL_INTERVAL_MS = 2000;
    private static final long HEALTH_POLL_TIMEOUT_MS = 60_000;

    private final SolutionProfileRepository solutionProfileRepository;
    private final SolutionServiceRepository solutionServiceRepository;
    private final ServiceProfileRepository serviceProfileRepository;
    private final ContainerInstanceRepository containerInstanceRepository;
    private final SolutionProfileMapper solutionProfileMapper;
    private final SolutionServiceMapper solutionServiceMapper;
    private final ContainerManagementService containerManagementService;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final AuditLogService auditLogService;
    private final DockerEngineService dockerEngineService;

    // ═══════════════════════════════════════════════════════════════════
    //  CRUD Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Creates a new solution profile for a team.
     *
     * <p>If {@code isDefault} is set to true, any existing default solution profile
     * for the team is cleared first.</p>
     *
     * @param teamId  the team ID
     * @param request the create solution profile request
     * @return the created solution profile detail response
     * @throws AuthorizationException if the user is not a team member
     * @throws ValidationException    if a profile with the same name exists or the limit is reached
     */
    @Transactional
    public SolutionProfileDetailResponse createSolutionProfile(UUID teamId,
                                                                CreateSolutionProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        if (solutionProfileRepository.existsByTeamIdAndName(teamId, request.name())) {
            throw new ValidationException("Solution profile already exists with name: " + request.name());
        }

        long count = solutionProfileRepository.countByTeamId(teamId);
        if (count >= AppConstants.FLEET_MAX_SOLUTIONS) {
            throw new ValidationException("Team has reached the maximum number of solution profiles: "
                    + AppConstants.FLEET_MAX_SOLUTIONS);
        }

        // Clear existing default if this one is being set as default
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            clearTeamDefault(teamId);
        }

        SolutionProfile profile = SolutionProfile.builder()
                .name(request.name())
                .description(request.description())
                .isDefault(makeDefault)
                .team(teamRepository.findById(teamId)
                        .orElseThrow(() -> new NotFoundException("Team", teamId)))
                .build();

        profile = solutionProfileRepository.save(profile);
        log.info("Created solution profile '{}' for team {}", profile.getName(), teamId);

        auditLogService.log(userId, teamId, "CREATE_SOLUTION_PROFILE", "SolutionProfile",
                profile.getId(), "Created solution profile " + profile.getName());

        return buildDetailResponse(profile);
    }

    /**
     * Updates an existing solution profile.
     *
     * @param teamId    the team ID
     * @param profileId the solution profile ID
     * @param request   the update request with fields to modify
     * @return the updated solution profile detail response
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another team
     */
    @Transactional
    public SolutionProfileDetailResponse updateSolutionProfile(UUID teamId, UUID profileId,
                                                                UpdateSolutionProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        SolutionProfile profile = findProfileForTeam(profileId, teamId);

        if (request.name() != null) {
            if (!request.name().equals(profile.getName())
                    && solutionProfileRepository.existsByTeamIdAndName(teamId, request.name())) {
                throw new ValidationException("Solution profile already exists with name: " + request.name());
            }
            profile.setName(request.name());
        }
        if (request.description() != null) {
            profile.setDescription(request.description());
        }
        if (request.isDefault() != null) {
            if (Boolean.TRUE.equals(request.isDefault())) {
                clearTeamDefault(teamId);
            }
            profile.setDefault(request.isDefault());
        }

        profile = solutionProfileRepository.save(profile);
        log.info("Updated solution profile '{}' for team {}", profile.getName(), teamId);

        auditLogService.log(userId, teamId, "UPDATE_SOLUTION_PROFILE", "SolutionProfile",
                profile.getId(), "Updated solution profile " + profile.getName());

        return buildDetailResponse(profile);
    }

    /**
     * Retrieves a solution profile by ID.
     *
     * @param teamId    the team ID
     * @param profileId the solution profile ID
     * @return the solution profile detail response
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another team
     */
    @Transactional(readOnly = true)
    public SolutionProfileDetailResponse getSolutionProfile(UUID teamId, UUID profileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        SolutionProfile profile = findProfileForTeam(profileId, teamId);
        return buildDetailResponse(profile);
    }

    /**
     * Lists all solution profiles for a team.
     *
     * @param teamId the team ID
     * @return list of solution profile summary responses
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional(readOnly = true)
    public List<SolutionProfileResponse> listSolutionProfiles(UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        List<SolutionProfile> profiles = solutionProfileRepository.findByTeamId(teamId);
        return solutionProfileMapper.toResponseList(profiles);
    }

    /**
     * Deletes a solution profile and all its service assignments.
     *
     * @param teamId    the team ID
     * @param profileId the solution profile ID
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another team
     */
    @Transactional
    public void deleteSolutionProfile(UUID teamId, UUID profileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        SolutionProfile profile = findProfileForTeam(profileId, teamId);
        String profileName = profile.getName();

        solutionProfileRepository.delete(profile);
        log.info("Deleted solution profile '{}' from team {}", profileName, teamId);

        auditLogService.log(userId, teamId, "DELETE_SOLUTION_PROFILE", "SolutionProfile",
                profileId, "Deleted solution profile " + profileName);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Service Assignment Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Adds a service profile to a solution.
     *
     * @param teamId            the team ID
     * @param solutionProfileId the solution profile ID
     * @param request           the add service request
     * @return the created solution service response
     * @throws NotFoundException      if the solution or service profile is not found
     * @throws AuthorizationException if the user is not a team member
     * @throws ValidationException    if the service is already in the solution or the limit is reached
     */
    @Transactional
    public SolutionServiceResponse addService(UUID teamId, UUID solutionProfileId,
                                               AddSolutionServiceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        SolutionProfile solution = findProfileForTeam(solutionProfileId, teamId);

        ServiceProfile serviceProfile = serviceProfileRepository.findById(request.serviceProfileId())
                .orElseThrow(() -> new NotFoundException("ServiceProfile", request.serviceProfileId()));
        if (!serviceProfile.getTeam().getId().equals(teamId)) {
            throw new AuthorizationException("Service profile does not belong to this team");
        }

        if (solutionServiceRepository.existsBySolutionProfileIdAndServiceProfileId(
                solutionProfileId, request.serviceProfileId())) {
            throw new ValidationException("Service profile is already assigned to this solution");
        }

        long serviceCount = solutionServiceRepository.countBySolutionProfileId(solutionProfileId);
        if (serviceCount >= AppConstants.FLEET_MAX_SERVICES_PER_SOLUTION) {
            throw new ValidationException("Solution has reached the maximum number of services: "
                    + AppConstants.FLEET_MAX_SERVICES_PER_SOLUTION);
        }

        SolutionService solutionService = SolutionService.builder()
                .startOrder(request.startOrder() != null ? request.startOrder() : 0)
                .solutionProfile(solution)
                .serviceProfile(serviceProfile)
                .build();

        solutionService = solutionServiceRepository.save(solutionService);
        log.info("Added service '{}' to solution '{}' for team {}",
                serviceProfile.getServiceName(), solution.getName(), teamId);

        auditLogService.log(userId, teamId, "ADD_SOLUTION_SERVICE", "SolutionService",
                solutionService.getId(), "Added " + serviceProfile.getServiceName() + " to " + solution.getName());

        return solutionServiceMapper.toResponse(solutionService);
    }

    /**
     * Removes a service profile from a solution.
     *
     * @param teamId            the team ID
     * @param solutionProfileId the solution profile ID
     * @param serviceProfileId  the service profile ID to remove
     * @throws NotFoundException      if the solution or service assignment is not found
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional
    public void removeService(UUID teamId, UUID solutionProfileId, UUID serviceProfileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        findProfileForTeam(solutionProfileId, teamId);

        SolutionService solutionService = solutionServiceRepository
                .findBySolutionProfileIdAndServiceProfileId(solutionProfileId, serviceProfileId)
                .orElseThrow(() -> new NotFoundException("Service not found in this solution"));

        solutionServiceRepository.delete(solutionService);
        log.info("Removed service profile {} from solution {} for team {}",
                serviceProfileId, solutionProfileId, teamId);

        auditLogService.log(userId, teamId, "REMOVE_SOLUTION_SERVICE", "SolutionService",
                solutionService.getId(), "Removed service from solution " + solutionProfileId);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Orchestrated Start/Stop
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Starts all services in a solution in ascending start-order sequence.
     *
     * <p>For each service, a container is started via {@link ContainerManagementService}.
     * If the service profile has a health check command, the method polls the container's
     * health status every 2 seconds for up to 60 seconds. On timeout, a warning is logged
     * and the next service is started.</p>
     *
     * @param teamId            the team ID
     * @param solutionProfileId the solution profile ID
     * @throws NotFoundException      if the solution is not found
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional
    public void startSolution(UUID teamId, UUID solutionProfileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        SolutionProfile solution = findProfileForTeam(solutionProfileId, teamId);
        List<SolutionService> services = solutionServiceRepository
                .findBySolutionProfileIdOrderByStartOrderAsc(solutionProfileId);

        log.info("Starting solution '{}' with {} services for team {}",
                solution.getName(), services.size(), teamId);

        for (SolutionService svc : services) {
            ServiceProfile serviceProfile = svc.getServiceProfile();
            if (!serviceProfile.isEnabled()) {
                log.debug("Skipping disabled service '{}' in solution '{}'",
                        serviceProfile.getServiceName(), solution.getName());
                continue;
            }

            StartContainerRequest startRequest = new StartContainerRequest(
                    serviceProfile.getId(), null, null, null, null);

            var containerResponse = containerManagementService.startContainer(teamId, startRequest);
            log.info("Started service '{}' (container {}) in solution '{}'",
                    serviceProfile.getServiceName(), containerResponse.containerName(), solution.getName());

            // Poll health if a health check is configured
            if (serviceProfile.getHealthCheckCommand() != null
                    && !serviceProfile.getHealthCheckCommand().isBlank()) {
                awaitContainerHealth(containerResponse.containerId(), serviceProfile.getServiceName());
            }
        }

        auditLogService.log(userId, teamId, "START_SOLUTION", "SolutionProfile",
                solutionProfileId, "Started solution " + solution.getName());
    }

    /**
     * Stops all running containers for a solution in descending start-order sequence.
     *
     * @param teamId            the team ID
     * @param solutionProfileId the solution profile ID
     * @throws NotFoundException      if the solution is not found
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional
    public void stopSolution(UUID teamId, UUID solutionProfileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        SolutionProfile solution = findProfileForTeam(solutionProfileId, teamId);
        List<SolutionService> services = solutionServiceRepository
                .findBySolutionProfileIdOrderByStartOrderAsc(solutionProfileId);

        log.info("Stopping solution '{}' with {} services for team {}",
                solution.getName(), services.size(), teamId);

        // Stop in reverse order (descending startOrder)
        for (int i = services.size() - 1; i >= 0; i--) {
            ServiceProfile serviceProfile = services.get(i).getServiceProfile();
            List<ContainerInstance> containers = containerInstanceRepository
                    .findByServiceProfileId(serviceProfile.getId());

            for (ContainerInstance container : containers) {
                if (container.getStatus() == ContainerStatus.RUNNING
                        || container.getStatus() == ContainerStatus.RESTARTING) {
                    containerManagementService.stopContainer(teamId, container.getId(), 10);
                    log.info("Stopped container '{}' for service '{}' in solution '{}'",
                            container.getContainerName(), serviceProfile.getServiceName(), solution.getName());
                }
            }
        }

        auditLogService.log(userId, teamId, "STOP_SOLUTION", "SolutionProfile",
                solutionProfileId, "Stopped solution " + solution.getName());
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
     * Finds a solution profile and verifies it belongs to the specified team.
     *
     * @param profileId the solution profile ID
     * @param teamId    the team ID
     * @return the solution profile
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the profile belongs to another team
     */
    private SolutionProfile findProfileForTeam(UUID profileId, UUID teamId) {
        SolutionProfile profile = solutionProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("SolutionProfile", profileId));
        if (!profile.getTeam().getId().equals(teamId)) {
            throw new AuthorizationException("Solution profile does not belong to this team");
        }
        return profile;
    }

    /**
     * Clears the isDefault flag on the current default solution profile for a team.
     *
     * @param teamId the team ID
     */
    private void clearTeamDefault(UUID teamId) {
        solutionProfileRepository.findByTeamIdAndIsDefault(teamId, true)
                .ifPresent(existing -> {
                    existing.setDefault(false);
                    solutionProfileRepository.save(existing);
                });
    }

    /**
     * Builds a full detail response for a solution profile including services.
     *
     * @param profile the solution profile entity
     * @return the detail response DTO
     */
    private SolutionProfileDetailResponse buildDetailResponse(SolutionProfile profile) {
        List<SolutionService> services = solutionServiceRepository
                .findBySolutionProfileIdOrderByStartOrderAsc(profile.getId());
        List<SolutionServiceResponse> serviceResponses = solutionServiceMapper.toResponseList(services);
        return solutionProfileMapper.toDetailResponse(profile, serviceResponses);
    }

    /**
     * Polls a container's health status after startup until healthy or timeout.
     *
     * <p>Checks health every 2 seconds for up to 60 seconds. If the container reaches
     * {@link HealthStatus#HEALTHY}, polling stops. On timeout, a warning is logged
     * and execution continues to the next service.</p>
     *
     * @param dockerContainerId the Docker container ID
     * @param serviceName       the service name for logging
     */
    @SuppressWarnings("unchecked")
    private void awaitContainerHealth(String dockerContainerId, String serviceName) {
        long deadline = System.currentTimeMillis() + HEALTH_POLL_TIMEOUT_MS;
        log.debug("Polling health for service '{}' (container {})", serviceName, dockerContainerId);

        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(HEALTH_POLL_INTERVAL_MS);
                var inspection = dockerEngineService.inspectContainer(dockerContainerId);
                var state = (java.util.Map<String, Object>) inspection.get("State");
                if (state != null) {
                    var health = (java.util.Map<String, Object>) state.get("Health");
                    if (health != null && "healthy".equalsIgnoreCase((String) health.get("Status"))) {
                        log.info("Service '{}' is healthy", serviceName);
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Health polling interrupted for service '{}'", serviceName);
                return;
            } catch (Exception e) {
                log.warn("Health check poll failed for service '{}': {}", serviceName, e.getMessage());
            }
        }

        log.warn("Health check timeout for service '{}' after {}ms — continuing",
                serviceName, HEALTH_POLL_TIMEOUT_MS);
    }
}
