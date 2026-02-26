package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.fleet.dto.mapper.WorkstationProfileMapper;
import com.codeops.fleet.dto.mapper.WorkstationSolutionMapper;
import com.codeops.fleet.dto.request.AddWorkstationSolutionRequest;
import com.codeops.fleet.dto.request.CreateWorkstationProfileRequest;
import com.codeops.fleet.dto.request.UpdateWorkstationProfileRequest;
import com.codeops.fleet.dto.response.WorkstationProfileDetailResponse;
import com.codeops.fleet.dto.response.WorkstationProfileResponse;
import com.codeops.fleet.dto.response.WorkstationSolutionResponse;
import com.codeops.fleet.entity.SolutionProfile;
import com.codeops.fleet.entity.WorkstationProfile;
import com.codeops.fleet.entity.WorkstationSolution;
import com.codeops.fleet.repository.SolutionProfileRepository;
import com.codeops.fleet.repository.WorkstationProfileRepository;
import com.codeops.fleet.repository.WorkstationSolutionRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for managing workstation profiles and one-click development environment operations.
 *
 * <p>A workstation profile composes one or more solution profiles into a developer's
 * personal workspace. This service provides full CRUD for workstation profiles and their
 * solution assignments, plus one-click start/stop that launches all solutions in
 * start-order sequence.</p>
 *
 * @see WorkstationProfile
 * @see WorkstationSolution
 * @see SolutionProfileService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkstationProfileService {

    private final WorkstationProfileRepository workstationProfileRepository;
    private final WorkstationSolutionRepository workstationSolutionRepository;
    private final SolutionProfileRepository solutionProfileRepository;
    private final WorkstationProfileMapper workstationProfileMapper;
    private final WorkstationSolutionMapper workstationSolutionMapper;
    private final SolutionProfileService solutionProfileService;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    // ═══════════════════════════════════════════════════════════════════
    //  CRUD Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Creates a new workstation profile for the current user within a team.
     *
     * <p>If {@code isDefault} is set to true, any existing default workstation profile
     * for the user in this team is cleared first.</p>
     *
     * @param teamId  the team ID
     * @param request the create workstation profile request
     * @return the created workstation profile detail response
     * @throws AuthorizationException if the user is not a team member
     * @throws ValidationException    if a profile with the same name exists or the limit is reached
     */
    @Transactional
    public WorkstationProfileDetailResponse createWorkstationProfile(UUID teamId,
                                                                      CreateWorkstationProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        if (workstationProfileRepository.existsByUserIdAndName(userId, request.name())) {
            throw new ValidationException("Workstation profile already exists with name: " + request.name());
        }

        long count = workstationProfileRepository.countByUserIdAndTeamId(userId, teamId);
        if (count >= AppConstants.FLEET_MAX_WORKSTATIONS) {
            throw new ValidationException("You have reached the maximum number of workstation profiles: "
                    + AppConstants.FLEET_MAX_WORKSTATIONS);
        }

        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            clearUserDefault(userId, teamId);
        }

        WorkstationProfile profile = WorkstationProfile.builder()
                .name(request.name())
                .description(request.description())
                .isDefault(makeDefault)
                .user(userRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("User", userId)))
                .team(teamRepository.findById(teamId)
                        .orElseThrow(() -> new NotFoundException("Team", teamId)))
                .build();

        profile = workstationProfileRepository.save(profile);
        log.info("Created workstation profile '{}' for user {} in team {}", profile.getName(), userId, teamId);

        auditLogService.log(userId, teamId, "CREATE_WORKSTATION_PROFILE", "WorkstationProfile",
                profile.getId(), "Created workstation profile " + profile.getName());

        return buildDetailResponse(profile);
    }

    /**
     * Updates an existing workstation profile.
     *
     * @param teamId    the team ID
     * @param profileId the workstation profile ID
     * @param request   the update request with fields to modify
     * @return the updated workstation profile detail response
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another user
     */
    @Transactional
    public WorkstationProfileDetailResponse updateWorkstationProfile(UUID teamId, UUID profileId,
                                                                      UpdateWorkstationProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        WorkstationProfile profile = findProfileForUser(profileId, userId);

        if (request.name() != null) {
            if (!request.name().equals(profile.getName())
                    && workstationProfileRepository.existsByUserIdAndName(userId, request.name())) {
                throw new ValidationException("Workstation profile already exists with name: " + request.name());
            }
            profile.setName(request.name());
        }
        if (request.description() != null) {
            profile.setDescription(request.description());
        }
        if (request.isDefault() != null) {
            if (Boolean.TRUE.equals(request.isDefault())) {
                clearUserDefault(userId, teamId);
            }
            profile.setDefault(request.isDefault());
        }

        profile = workstationProfileRepository.save(profile);
        log.info("Updated workstation profile '{}' for user {} in team {}", profile.getName(), userId, teamId);

        auditLogService.log(userId, teamId, "UPDATE_WORKSTATION_PROFILE", "WorkstationProfile",
                profile.getId(), "Updated workstation profile " + profile.getName());

        return buildDetailResponse(profile);
    }

    /**
     * Retrieves a workstation profile by ID.
     *
     * @param teamId    the team ID
     * @param profileId the workstation profile ID
     * @return the workstation profile detail response
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another user
     */
    @Transactional(readOnly = true)
    public WorkstationProfileDetailResponse getWorkstationProfile(UUID teamId, UUID profileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        WorkstationProfile profile = findProfileForUser(profileId, userId);
        return buildDetailResponse(profile);
    }

    /**
     * Lists all workstation profiles for the current user within a team.
     *
     * @param teamId the team ID
     * @return list of workstation profile summary responses
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional(readOnly = true)
    public List<WorkstationProfileResponse> listWorkstationProfiles(UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        List<WorkstationProfile> profiles = workstationProfileRepository.findByUserIdAndTeamId(userId, teamId);
        return workstationProfileMapper.toResponseList(profiles);
    }

    /**
     * Deletes a workstation profile and all its solution assignments.
     *
     * @param teamId    the team ID
     * @param profileId the workstation profile ID
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another user
     */
    @Transactional
    public void deleteWorkstationProfile(UUID teamId, UUID profileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        WorkstationProfile profile = findProfileForUser(profileId, userId);
        String profileName = profile.getName();

        workstationProfileRepository.delete(profile);
        log.info("Deleted workstation profile '{}' for user {} in team {}", profileName, userId, teamId);

        auditLogService.log(userId, teamId, "DELETE_WORKSTATION_PROFILE", "WorkstationProfile",
                profileId, "Deleted workstation profile " + profileName);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Solution Assignment Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Adds a solution profile to a workstation.
     *
     * @param teamId               the team ID
     * @param workstationProfileId the workstation profile ID
     * @param request              the add solution request
     * @return the created workstation solution response
     * @throws NotFoundException      if the workstation or solution profile is not found
     * @throws AuthorizationException if the user is not a team member
     * @throws ValidationException    if the solution is already in the workstation or the limit is reached
     */
    @Transactional
    public WorkstationSolutionResponse addSolution(UUID teamId, UUID workstationProfileId,
                                                    AddWorkstationSolutionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        WorkstationProfile workstation = findProfileForUser(workstationProfileId, userId);

        SolutionProfile solutionProfile = solutionProfileRepository.findById(request.solutionProfileId())
                .orElseThrow(() -> new NotFoundException("SolutionProfile", request.solutionProfileId()));
        if (!solutionProfile.getTeam().getId().equals(teamId)) {
            throw new AuthorizationException("Solution profile does not belong to this team");
        }

        if (workstationSolutionRepository.existsByWorkstationProfileIdAndSolutionProfileId(
                workstationProfileId, request.solutionProfileId())) {
            throw new ValidationException("Solution profile is already assigned to this workstation");
        }

        long solutionCount = workstationSolutionRepository.countByWorkstationProfileId(workstationProfileId);
        if (solutionCount >= AppConstants.FLEET_MAX_SOLUTIONS_PER_WORKSTATION) {
            throw new ValidationException("Workstation has reached the maximum number of solutions: "
                    + AppConstants.FLEET_MAX_SOLUTIONS_PER_WORKSTATION);
        }

        WorkstationSolution workstationSolution = WorkstationSolution.builder()
                .startOrder(request.startOrder() != null ? request.startOrder() : 0)
                .overrideEnvVarsJson(request.overrideEnvVarsJson())
                .workstationProfile(workstation)
                .solutionProfile(solutionProfile)
                .build();

        workstationSolution = workstationSolutionRepository.save(workstationSolution);
        log.info("Added solution '{}' to workstation '{}' for user {} in team {}",
                solutionProfile.getName(), workstation.getName(), userId, teamId);

        auditLogService.log(userId, teamId, "ADD_WORKSTATION_SOLUTION", "WorkstationSolution",
                workstationSolution.getId(),
                "Added " + solutionProfile.getName() + " to " + workstation.getName());

        return workstationSolutionMapper.toResponse(workstationSolution);
    }

    /**
     * Removes a solution profile from a workstation.
     *
     * @param teamId               the team ID
     * @param workstationProfileId the workstation profile ID
     * @param solutionProfileId    the solution profile ID to remove
     * @throws NotFoundException      if the workstation or solution assignment is not found
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional
    public void removeSolution(UUID teamId, UUID workstationProfileId, UUID solutionProfileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        findProfileForUser(workstationProfileId, userId);

        WorkstationSolution workstationSolution = workstationSolutionRepository
                .findByWorkstationProfileIdAndSolutionProfileId(workstationProfileId, solutionProfileId)
                .orElseThrow(() -> new NotFoundException("Solution not found in this workstation"));

        workstationSolutionRepository.delete(workstationSolution);
        log.info("Removed solution profile {} from workstation {} for user {} in team {}",
                solutionProfileId, workstationProfileId, userId, teamId);

        auditLogService.log(userId, teamId, "REMOVE_WORKSTATION_SOLUTION", "WorkstationSolution",
                workstationSolution.getId(), "Removed solution from workstation " + workstationProfileId);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  One-Click Start/Stop
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Starts all solutions in a workstation in ascending start-order sequence.
     *
     * <p>Iterates through each solution assignment ordered by start order and delegates
     * to {@link SolutionProfileService#startSolution} for orchestrated service startup
     * with health check polling.</p>
     *
     * @param teamId               the team ID
     * @param workstationProfileId the workstation profile ID
     * @throws NotFoundException      if the workstation is not found
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional
    public void startWorkstation(UUID teamId, UUID workstationProfileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        WorkstationProfile workstation = findProfileForUser(workstationProfileId, userId);
        List<WorkstationSolution> solutions = workstationSolutionRepository
                .findByWorkstationProfileIdOrderByStartOrderAsc(workstationProfileId);

        log.info("Starting workstation '{}' with {} solutions for user {} in team {}",
                workstation.getName(), solutions.size(), userId, teamId);

        for (WorkstationSolution ws : solutions) {
            SolutionProfile solutionProfile = ws.getSolutionProfile();
            log.info("Starting solution '{}' (order {}) in workstation '{}'",
                    solutionProfile.getName(), ws.getStartOrder(), workstation.getName());
            solutionProfileService.startSolution(teamId, solutionProfile.getId());
        }

        auditLogService.log(userId, teamId, "START_WORKSTATION", "WorkstationProfile",
                workstationProfileId, "Started workstation " + workstation.getName());
    }

    /**
     * Stops all solutions in a workstation in descending start-order sequence.
     *
     * <p>Iterates through each solution assignment in reverse start order and delegates
     * to {@link SolutionProfileService#stopSolution} for orchestrated service shutdown.</p>
     *
     * @param teamId               the team ID
     * @param workstationProfileId the workstation profile ID
     * @throws NotFoundException      if the workstation is not found
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional
    public void stopWorkstation(UUID teamId, UUID workstationProfileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        WorkstationProfile workstation = findProfileForUser(workstationProfileId, userId);
        List<WorkstationSolution> solutions = workstationSolutionRepository
                .findByWorkstationProfileIdOrderByStartOrderAsc(workstationProfileId);

        log.info("Stopping workstation '{}' with {} solutions for user {} in team {}",
                workstation.getName(), solutions.size(), userId, teamId);

        // Stop in reverse order (descending startOrder)
        for (int i = solutions.size() - 1; i >= 0; i--) {
            SolutionProfile solutionProfile = solutions.get(i).getSolutionProfile();
            log.info("Stopping solution '{}' (order {}) in workstation '{}'",
                    solutionProfile.getName(), solutions.get(i).getStartOrder(), workstation.getName());
            solutionProfileService.stopSolution(teamId, solutionProfile.getId());
        }

        auditLogService.log(userId, teamId, "STOP_WORKSTATION", "WorkstationProfile",
                workstationProfileId, "Stopped workstation " + workstation.getName());
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
     * Finds a workstation profile and verifies it belongs to the current user.
     *
     * @param profileId the workstation profile ID
     * @param userId    the expected owner user ID
     * @return the workstation profile
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the profile belongs to another user
     */
    private WorkstationProfile findProfileForUser(UUID profileId, UUID userId) {
        WorkstationProfile profile = workstationProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("WorkstationProfile", profileId));
        if (!profile.getUser().getId().equals(userId)) {
            throw new AuthorizationException("Workstation profile does not belong to this user");
        }
        return profile;
    }

    /**
     * Clears the isDefault flag on the current default workstation profile for a user in a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     */
    private void clearUserDefault(UUID userId, UUID teamId) {
        workstationProfileRepository.findByUserIdAndTeamIdAndIsDefault(userId, teamId, true)
                .ifPresent(existing -> {
                    existing.setDefault(false);
                    workstationProfileRepository.save(existing);
                });
    }

    /**
     * Builds a full detail response for a workstation profile including solutions.
     *
     * @param profile the workstation profile entity
     * @return the detail response DTO
     */
    private WorkstationProfileDetailResponse buildDetailResponse(WorkstationProfile profile) {
        List<WorkstationSolution> solutions = workstationSolutionRepository
                .findByWorkstationProfileIdOrderByStartOrderAsc(profile.getId());
        List<WorkstationSolutionResponse> solutionResponses = workstationSolutionMapper.toResponseList(solutions);
        return workstationProfileMapper.toDetailResponse(profile, solutionResponses);
    }
}
