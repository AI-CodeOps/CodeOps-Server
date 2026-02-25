package com.codeops.relay.controller;

import com.codeops.config.AppConstants;
import com.codeops.relay.dto.request.UpdatePresenceRequest;
import com.codeops.relay.dto.response.UserPresenceResponse;
import com.codeops.relay.entity.enums.PresenceStatus;
import com.codeops.relay.service.PresenceService;
import com.codeops.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user presence management in the Relay module.
 *
 * <p>Provides endpoints for updating and querying user presence status,
 * managing Do Not Disturb mode, and retrieving team-wide presence
 * information.</p>
 *
 * <p>All endpoints require authentication.</p>
 */
@RestController
@RequestMapping(AppConstants.RELAY_API_PREFIX + "/presence")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class PresenceController {

    private final PresenceService presenceService;

    /**
     * Updates the current user's presence status.
     *
     * @param request the presence update request
     * @param teamId  the team ID
     * @return the updated presence
     */
    @PutMapping
    public UserPresenceResponse updatePresence(
            @RequestBody @Valid UpdatePresenceRequest request,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return presenceService.updatePresence(request, teamId, userId);
    }

    /**
     * Retrieves the current user's presence in a team.
     *
     * @param teamId the team ID
     * @return the user's presence
     */
    @GetMapping
    public UserPresenceResponse getPresence(@RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return presenceService.getPresence(teamId, userId);
    }

    /**
     * Retrieves all team members' presence status.
     *
     * @param teamId the team ID
     * @return the list of user presences
     */
    @GetMapping("/team")
    public List<UserPresenceResponse> getTeamPresence(@RequestParam UUID teamId) {
        return presenceService.getTeamPresence(teamId);
    }

    /**
     * Retrieves currently online users in a team.
     *
     * @param teamId the team ID
     * @return the list of online users' presences
     */
    @GetMapping("/online")
    public List<UserPresenceResponse> getOnlineUsers(@RequestParam UUID teamId) {
        return presenceService.getOnlineUsers(teamId);
    }

    /**
     * Enables Do Not Disturb mode for the current user.
     *
     * @param teamId        the team ID
     * @param statusMessage an optional status message
     * @return the updated presence
     */
    @PostMapping("/dnd")
    public UserPresenceResponse setDoNotDisturb(
            @RequestParam UUID teamId,
            @RequestParam(required = false) String statusMessage) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return presenceService.setDoNotDisturb(teamId, userId, statusMessage);
    }

    /**
     * Disables Do Not Disturb mode for the current user.
     *
     * @param teamId the team ID
     * @return the updated presence
     */
    @DeleteMapping("/dnd")
    public UserPresenceResponse clearDoNotDisturb(@RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return presenceService.clearDoNotDisturb(teamId, userId);
    }

    /**
     * Sets the current user's status to offline.
     *
     * @param teamId the team ID
     */
    @PostMapping("/offline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void goOffline(@RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        presenceService.goOffline(teamId, userId);
    }

    /**
     * Retrieves presence status counts for a team.
     *
     * @param teamId the team ID
     * @return a map of presence status to user count
     */
    @GetMapping("/count")
    public Map<PresenceStatus, Long> getPresenceCount(@RequestParam UUID teamId) {
        return presenceService.getPresenceCount(teamId);
    }
}
