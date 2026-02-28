package com.codeops.mcp.controller;

import com.codeops.config.AppConstants;
import com.codeops.mcp.dto.request.CreateApiTokenRequest;
import com.codeops.mcp.dto.request.CreateDeveloperProfileRequest;
import com.codeops.mcp.dto.request.UpdateDeveloperProfileRequest;
import com.codeops.mcp.dto.response.ApiTokenCreatedResponse;
import com.codeops.mcp.dto.response.ApiTokenResponse;
import com.codeops.mcp.dto.response.DeveloperProfileResponse;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.service.DeveloperProfileService;
import com.codeops.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for MCP developer profile and API token management.
 *
 * <p>Provides endpoints for creating and managing developer profiles
 * (the MCP identity within a team), and for generating, listing, and
 * revoking API tokens used by AI agents for MCP authentication.</p>
 *
 * <p>All endpoints require ADMIN or OWNER role.</p>
 */
@RestController
@RequestMapping(AppConstants.MCP_API_PREFIX + "/developers")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class McpDeveloperController {

    private final DeveloperProfileService developerProfileService;

    /**
     * Gets or creates a developer profile for the current user within a team.
     *
     * <p>If no profile exists for the (team, user) pair, one is auto-created
     * with defaults from the User entity.</p>
     *
     * @param teamId the team ID
     * @return the developer profile response
     */
    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    public DeveloperProfileResponse getOrCreateProfile(@RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        DeveloperProfile profile = developerProfileService.getOrCreateProfile(teamId, userId);
        return developerProfileService.getProfileById(profile.getId());
    }

    /**
     * Gets a developer profile by team and user.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return the profile response
     */
    @GetMapping("/profile")
    public DeveloperProfileResponse getProfile(@RequestParam UUID teamId,
                                                 @RequestParam UUID userId) {
        return developerProfileService.getProfile(teamId, userId);
    }

    /**
     * Lists all active developer profiles for a team.
     *
     * @param teamId the team ID
     * @return list of active profile responses
     */
    @GetMapping
    public List<DeveloperProfileResponse> getTeamProfiles(@RequestParam UUID teamId) {
        return developerProfileService.getTeamProfiles(teamId);
    }

    /**
     * Updates a developer profile's fields.
     *
     * @param profileId the profile ID to update
     * @param request   the update request with fields to change
     * @return the updated profile response
     */
    @PutMapping("/{profileId}")
    public DeveloperProfileResponse updateProfile(@PathVariable UUID profileId,
                                                    @RequestBody @Valid UpdateDeveloperProfileRequest request) {
        return developerProfileService.updateProfile(profileId, request);
    }

    /**
     * Creates an API token for MCP AI agent authentication.
     *
     * <p>The raw token is returned only in this response and cannot be
     * retrieved again. Only the SHA-256 hash is stored.</p>
     *
     * @param profileId the developer profile ID
     * @param request   the token creation request
     * @return the created token response including the raw token (one-time only)
     */
    @PostMapping("/{profileId}/tokens")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiTokenCreatedResponse createToken(@PathVariable UUID profileId,
                                                 @RequestBody @Valid CreateApiTokenRequest request) {
        return developerProfileService.createApiToken(profileId, request);
    }

    /**
     * Lists all tokens for a developer profile (hashes excluded).
     *
     * @param profileId the developer profile ID
     * @return list of token responses
     */
    @GetMapping("/{profileId}/tokens")
    public List<ApiTokenResponse> getTokens(@PathVariable UUID profileId) {
        return developerProfileService.getTokens(profileId);
    }

    /**
     * Revokes an API token.
     *
     * @param tokenId the token ID to revoke
     */
    @DeleteMapping("/tokens/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeToken(@PathVariable UUID tokenId) {
        developerProfileService.revokeToken(tokenId);
    }
}
