package com.codeops.fleet.controller;

import com.codeops.config.AppConstants;
import com.codeops.fleet.dto.request.AddSolutionServiceRequest;
import com.codeops.fleet.dto.request.CreateSolutionProfileRequest;
import com.codeops.fleet.dto.request.UpdateSolutionProfileRequest;
import com.codeops.fleet.dto.response.SolutionProfileDetailResponse;
import com.codeops.fleet.dto.response.SolutionProfileResponse;
import com.codeops.fleet.dto.response.SolutionServiceResponse;
import com.codeops.fleet.service.SolutionProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing solution profiles in the Fleet module.
 *
 * <p>Provides endpoints for creating, reading, updating, and deleting solution profiles,
 * managing the service profiles within a solution, and orchestrating solution-wide
 * start and stop operations.</p>
 *
 * <p>All endpoints require authentication and ADMIN or OWNER role.
 * Team membership is verified in the service layer.</p>
 */
@RestController
@RequestMapping(AppConstants.FLEET_API_PREFIX + "/solution-profiles")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class SolutionProfileController {

    private final SolutionProfileService solutionProfileService;

    /**
     * Creates a new solution profile for a team.
     *
     * @param teamId  the team ID
     * @param request the solution profile creation request
     * @return the created solution profile details
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolutionProfileDetailResponse createSolutionProfile(
            @RequestParam UUID teamId,
            @RequestBody @Valid CreateSolutionProfileRequest request) {
        return solutionProfileService.createSolutionProfile(teamId, request);
    }

    /**
     * Updates an existing solution profile.
     *
     * @param teamId    the team ID
     * @param profileId the solution profile ID
     * @param request   the update request
     * @return the updated solution profile details
     */
    @PutMapping("/{profileId}")
    public SolutionProfileDetailResponse updateSolutionProfile(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId,
            @RequestBody @Valid UpdateSolutionProfileRequest request) {
        return solutionProfileService.updateSolutionProfile(teamId, profileId, request);
    }

    /**
     * Retrieves a solution profile by ID.
     *
     * @param teamId    the team ID
     * @param profileId the solution profile ID
     * @return the solution profile details
     */
    @GetMapping("/{profileId}")
    public SolutionProfileDetailResponse getSolutionProfile(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        return solutionProfileService.getSolutionProfile(teamId, profileId);
    }

    /**
     * Lists all solution profiles for a team.
     *
     * @param teamId the team ID
     * @return the list of solution profile summaries
     */
    @GetMapping
    public List<SolutionProfileResponse> listSolutionProfiles(@RequestParam UUID teamId) {
        return solutionProfileService.listSolutionProfiles(teamId);
    }

    /**
     * Deletes a solution profile.
     *
     * @param teamId    the team ID
     * @param profileId the solution profile ID
     */
    @DeleteMapping("/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSolutionProfile(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        solutionProfileService.deleteSolutionProfile(teamId, profileId);
    }

    /**
     * Adds a service profile to a solution.
     *
     * @param teamId    the team ID
     * @param profileId the solution profile ID
     * @param request   the request containing the service profile ID and optional start order
     * @return the created solution service link
     */
    @PostMapping("/{profileId}/services")
    @ResponseStatus(HttpStatus.CREATED)
    public SolutionServiceResponse addService(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId,
            @RequestBody @Valid AddSolutionServiceRequest request) {
        return solutionProfileService.addService(teamId, profileId, request);
    }

    /**
     * Removes a service profile from a solution.
     *
     * @param teamId           the team ID
     * @param profileId        the solution profile ID
     * @param serviceProfileId the service profile ID to remove
     */
    @DeleteMapping("/{profileId}/services/{serviceProfileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeService(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId,
            @PathVariable UUID serviceProfileId) {
        solutionProfileService.removeService(teamId, profileId, serviceProfileId);
    }

    /**
     * Starts all services in a solution in start-order sequence.
     *
     * @param teamId    the team ID
     * @param profileId the solution profile ID
     */
    @PostMapping("/{profileId}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startSolution(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        solutionProfileService.startSolution(teamId, profileId);
    }

    /**
     * Stops all running services in a solution.
     *
     * @param teamId    the team ID
     * @param profileId the solution profile ID
     */
    @PostMapping("/{profileId}/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stopSolution(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        solutionProfileService.stopSolution(teamId, profileId);
    }
}
