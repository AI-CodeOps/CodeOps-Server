package com.codeops.fleet.controller;

import com.codeops.config.AppConstants;
import com.codeops.fleet.dto.request.CreateServiceProfileRequest;
import com.codeops.fleet.dto.request.UpdateServiceProfileRequest;
import com.codeops.fleet.dto.response.ServiceProfileDetailResponse;
import com.codeops.fleet.dto.response.ServiceProfileResponse;
import com.codeops.fleet.service.ServiceProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing service profiles in the Fleet module.
 *
 * <p>Provides endpoints for creating, reading, updating, and deleting service profiles,
 * as well as auto-generating profiles from existing service registrations in the
 * Registry module.</p>
 *
 * <p>All endpoints require authentication and ADMIN or OWNER role.
 * Team membership is verified in the service layer.</p>
 */
@RestController
@RequestMapping(AppConstants.FLEET_API_PREFIX + "/service-profiles")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class ServiceProfileController {

    private final ServiceProfileService serviceProfileService;

    /**
     * Creates a new service profile for a team.
     *
     * @param teamId  the team ID
     * @param request the service profile creation request
     * @return the created service profile details
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceProfileDetailResponse createServiceProfile(
            @RequestParam UUID teamId,
            @RequestBody @Valid CreateServiceProfileRequest request) {
        return serviceProfileService.createServiceProfile(teamId, request);
    }

    /**
     * Updates an existing service profile.
     *
     * @param teamId    the team ID
     * @param profileId the service profile ID
     * @param request   the update request
     * @return the updated service profile details
     */
    @PutMapping("/{profileId}")
    public ServiceProfileDetailResponse updateServiceProfile(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId,
            @RequestBody @Valid UpdateServiceProfileRequest request) {
        return serviceProfileService.updateServiceProfile(teamId, profileId, request);
    }

    /**
     * Retrieves a service profile by ID.
     *
     * @param teamId    the team ID
     * @param profileId the service profile ID
     * @return the service profile details
     */
    @GetMapping("/{profileId}")
    public ServiceProfileDetailResponse getServiceProfile(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        return serviceProfileService.getServiceProfile(teamId, profileId);
    }

    /**
     * Lists all service profiles for a team.
     *
     * @param teamId the team ID
     * @return the list of service profile summaries
     */
    @GetMapping
    public List<ServiceProfileResponse> listServiceProfiles(@RequestParam UUID teamId) {
        return serviceProfileService.listServiceProfiles(teamId);
    }

    /**
     * Deletes a service profile.
     *
     * @param teamId    the team ID
     * @param profileId the service profile ID
     */
    @DeleteMapping("/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteServiceProfile(
            @RequestParam UUID teamId,
            @PathVariable UUID profileId) {
        serviceProfileService.deleteServiceProfile(teamId, profileId);
    }

    /**
     * Auto-generates a service profile from an existing service registration.
     *
     * @param teamId                the team ID
     * @param serviceRegistrationId the source service registration ID
     * @return the generated service profile details
     */
    @PostMapping("/auto-generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceProfileDetailResponse autoGenerateFromRegistry(
            @RequestParam UUID teamId,
            @RequestParam UUID serviceRegistrationId) {
        return serviceProfileService.autoGenerateFromRegistry(teamId, serviceRegistrationId);
    }
}
