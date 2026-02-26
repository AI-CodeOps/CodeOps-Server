package com.codeops.fleet.controller;

import com.codeops.config.AppConstants;
import com.codeops.fleet.dto.request.AddWorkstationSolutionRequest;
import com.codeops.fleet.dto.request.CreateWorkstationProfileRequest;
import com.codeops.fleet.dto.request.UpdateWorkstationProfileRequest;
import com.codeops.fleet.dto.response.WorkstationProfileDetailResponse;
import com.codeops.fleet.dto.response.WorkstationProfileResponse;
import com.codeops.fleet.dto.response.WorkstationSolutionResponse;
import com.codeops.fleet.service.WorkstationProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing workstation profiles in the Fleet module.
 *
 * <p>Provides endpoints for creating, reading, updating, and deleting workstation profiles,
 * managing the solution profiles within a workstation, and orchestrating workstation-wide
 * start and stop operations.</p>
 *
 * <p>All endpoints require authentication and ADMIN or OWNER role.
 * Team membership is verified in the service layer.</p>
 */
@RestController
@RequestMapping(AppConstants.FLEET_API_PREFIX + "/workstation-profiles")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class WorkstationProfileController {

    private final WorkstationProfileService workstationProfileService;

    /**
     * Creates a new workstation profile for a team.
     *
     * @param teamId  the team ID
     * @param request the workstation profile creation request
     * @return the created workstation profile details
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkstationProfileDetailResponse createWorkstationProfile(
            @RequestParam UUID teamId,
            @RequestBody @Valid CreateWorkstationProfileRequest request) {
        return workstationProfileService.createWorkstationProfile(teamId, request);
    }

    /**
     * Updates an existing workstation profile.
     *
     * @param teamId    the team ID
     * @param profileId the workstation profile ID
     * @param request   the update request
     * @return the updated workstation profile details
     */
    @PutMapping("/{profileId}")
    public WorkstationProfileDetailResponse updateWorkstationProfile(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId,
            @RequestBody @Valid UpdateWorkstationProfileRequest request) {
        return workstationProfileService.updateWorkstationProfile(teamId, profileId, request);
    }

    /**
     * Retrieves a workstation profile by ID.
     *
     * @param teamId    the team ID
     * @param profileId the workstation profile ID
     * @return the workstation profile details
     */
    @GetMapping("/{profileId}")
    public WorkstationProfileDetailResponse getWorkstationProfile(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        return workstationProfileService.getWorkstationProfile(teamId, profileId);
    }

    /**
     * Lists all workstation profiles for a team.
     *
     * @param teamId the team ID
     * @return the list of workstation profile summaries
     */
    @GetMapping
    public List<WorkstationProfileResponse> listWorkstationProfiles(@RequestParam UUID teamId) {
        return workstationProfileService.listWorkstationProfiles(teamId);
    }

    /**
     * Deletes a workstation profile.
     *
     * @param teamId    the team ID
     * @param profileId the workstation profile ID
     */
    @DeleteMapping("/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkstationProfile(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        workstationProfileService.deleteWorkstationProfile(teamId, profileId);
    }

    /**
     * Adds a solution profile to a workstation.
     *
     * @param teamId    the team ID
     * @param profileId the workstation profile ID
     * @param request   the request containing the solution profile ID and optional overrides
     * @return the created workstation solution link
     */
    @PostMapping("/{profileId}/solutions")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkstationSolutionResponse addSolution(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId,
            @RequestBody @Valid AddWorkstationSolutionRequest request) {
        return workstationProfileService.addSolution(teamId, profileId, request);
    }

    /**
     * Removes a solution profile from a workstation.
     *
     * @param teamId            the team ID
     * @param profileId         the workstation profile ID
     * @param solutionProfileId the solution profile ID to remove
     */
    @DeleteMapping("/{profileId}/solutions/{solutionProfileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSolution(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId,
            @PathVariable UUID solutionProfileId) {
        workstationProfileService.removeSolution(teamId, profileId, solutionProfileId);
    }

    /**
     * Starts all solutions in a workstation in start-order sequence.
     *
     * @param teamId    the team ID
     * @param profileId the workstation profile ID
     */
    @PostMapping("/{profileId}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startWorkstation(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        workstationProfileService.startWorkstation(teamId, profileId);
    }

    /**
     * Stops all running solutions in a workstation.
     *
     * @param teamId    the team ID
     * @param profileId the workstation profile ID
     */
    @PostMapping("/{profileId}/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stopWorkstation(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        workstationProfileService.stopWorkstation(teamId, profileId);
    }
}
