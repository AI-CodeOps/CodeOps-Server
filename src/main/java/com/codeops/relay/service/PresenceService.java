package com.codeops.relay.service;

import com.codeops.config.AppConstants;
import com.codeops.exception.NotFoundException;
import com.codeops.relay.dto.mapper.UserPresenceMapper;
import com.codeops.relay.dto.request.UpdatePresenceRequest;
import com.codeops.relay.dto.response.UserPresenceResponse;
import com.codeops.relay.entity.UserPresence;
import com.codeops.relay.entity.enums.PresenceStatus;
import com.codeops.relay.repository.UserPresenceRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for tracking user online/away/DND/offline presence within teams.
 *
 * <p>Users send periodic heartbeats; if no heartbeat arrives within the timeout
 * window ({@link AppConstants#RELAY_PRESENCE_HEARTBEAT_TIMEOUT_SECONDS}), the user
 * transitions to OFFLINE. Supports explicit status changes, custom status messages,
 * and DND mode.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PresenceService {

    private final UserPresenceRepository userPresenceRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final UserPresenceMapper userPresenceMapper;

    /**
     * Updates a user's presence status and/or custom status message.
     *
     * <p>Any presence update also refreshes the heartbeat timestamp and last-seen time.</p>
     *
     * @param request the update request containing status and optional statusMessage
     * @param teamId  the team ID
     * @param userId  the user ID
     * @return the updated presence response
     * @throws NotFoundException if the user is not a member of the team
     */
    @Transactional
    public UserPresenceResponse updatePresence(UpdatePresenceRequest request, UUID teamId, UUID userId) {
        verifyTeamMember(teamId, userId);
        UserPresence presence = findOrCreatePresence(teamId, userId);
        presence.setStatus(request.status());
        presence.setStatusMessage(request.statusMessage());
        presence.setLastHeartbeatAt(Instant.now());
        presence.setLastSeenAt(Instant.now());
        presence = userPresenceRepository.save(presence);
        log.debug("Presence updated: user {} → {}", userId, request.status());
        return buildResponse(presence);
    }

    /**
     * Records a heartbeat from a user, transitioning them to ONLINE if appropriate.
     *
     * <p>If the user is OFFLINE or AWAY, transitions to ONLINE. If DND, leaves the
     * status unchanged (user explicitly set it). Called frequently by clients
     * (every 30–60 seconds).</p>
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return the updated presence response
     */
    @Transactional
    public UserPresenceResponse heartbeat(UUID teamId, UUID userId) {
        UserPresence presence = findOrCreatePresence(teamId, userId);

        if (presence.getId() == null) {
            presence.setStatus(PresenceStatus.ONLINE);
        } else if (presence.getStatus() == PresenceStatus.OFFLINE
                || presence.getStatus() == PresenceStatus.AWAY) {
            presence.setStatus(PresenceStatus.ONLINE);
        }

        presence.setLastHeartbeatAt(Instant.now());
        presence.setLastSeenAt(Instant.now());
        presence = userPresenceRepository.save(presence);
        return buildResponse(presence);
    }

    /**
     * Retrieves a user's presence within a team.
     *
     * <p>If no presence record exists, returns a default OFFLINE response. If the
     * presence is stale (heartbeat expired), transitions to OFFLINE before returning.</p>
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return the presence response
     */
    @Transactional
    public UserPresenceResponse getPresence(UUID teamId, UUID userId) {
        var opt = userPresenceRepository.findByUserIdAndTeamId(userId, teamId);
        if (opt.isEmpty()) {
            return buildDefaultOfflineResponse(teamId, userId);
        }

        UserPresence presence = opt.get();
        if (presence.getStatus() != PresenceStatus.OFFLINE && isStale(presence)) {
            presence.setStatus(PresenceStatus.OFFLINE);
            presence = userPresenceRepository.save(presence);
        }
        return buildResponse(presence);
    }

    /**
     * Retrieves all presence records for a team with staleness cleanup.
     *
     * <p>Transitions any stale presences to OFFLINE, then sorts by status order
     * (ONLINE → AWAY → DND → OFFLINE) and alphabetically by display name within
     * each group.</p>
     *
     * @param teamId the team ID
     * @return sorted list of presence responses
     */
    @Transactional
    public List<UserPresenceResponse> getTeamPresence(UUID teamId) {
        List<UserPresence> presences = userPresenceRepository.findByTeamId(teamId);

        for (UserPresence presence : presences) {
            if (presence.getStatus() != PresenceStatus.OFFLINE && isStale(presence)) {
                presence.setStatus(PresenceStatus.OFFLINE);
                userPresenceRepository.save(presence);
            }
        }

        return presences.stream()
                .map(this::buildResponse)
                .sorted(Comparator.comparingInt((UserPresenceResponse r) -> statusSortOrder(r.status()))
                        .thenComparing(r -> r.userDisplayName() != null ? r.userDisplayName() : ""))
                .toList();
    }

    /**
     * Retrieves all truly online users for a team, filtering out stale heartbeats.
     *
     * @param teamId the team ID
     * @return list of online user presence responses
     */
    @Transactional(readOnly = true)
    public List<UserPresenceResponse> getOnlineUsers(UUID teamId) {
        List<UserPresence> onlinePresences = userPresenceRepository.findByTeamIdAndStatus(
                teamId, PresenceStatus.ONLINE);
        return onlinePresences.stream()
                .filter(p -> !isStale(p))
                .map(this::buildResponse)
                .toList();
    }

    /**
     * Sets a user's status to Do Not Disturb with an optional custom message.
     *
     * @param teamId       the team ID
     * @param userId       the user ID
     * @param statusMessage custom status text (e.g., "In a meeting")
     * @return the updated presence response
     */
    @Transactional
    public UserPresenceResponse setDoNotDisturb(UUID teamId, UUID userId, String statusMessage) {
        UserPresence presence = findOrCreatePresence(teamId, userId);
        presence.setStatus(PresenceStatus.DND);
        presence.setStatusMessage(statusMessage);
        presence.setLastHeartbeatAt(Instant.now());
        presence.setLastSeenAt(Instant.now());
        presence = userPresenceRepository.save(presence);
        log.debug("Presence updated: user {} → DND", userId);
        return buildResponse(presence);
    }

    /**
     * Clears Do Not Disturb status, transitioning the user to ONLINE.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return the updated presence response
     */
    @Transactional
    public UserPresenceResponse clearDoNotDisturb(UUID teamId, UUID userId) {
        UserPresence presence = findOrCreatePresence(teamId, userId);
        if (presence.getStatus() == PresenceStatus.DND) {
            presence.setStatus(PresenceStatus.ONLINE);
        }
        presence.setStatusMessage(null);
        presence.setLastHeartbeatAt(Instant.now());
        presence.setLastSeenAt(Instant.now());
        presence = userPresenceRepository.save(presence);
        log.debug("DND cleared for user {}", userId);
        return buildResponse(presence);
    }

    /**
     * Sets a user's status to OFFLINE, clearing any custom status message.
     *
     * <p>Called when a user explicitly logs out or disconnects.</p>
     *
     * @param teamId the team ID
     * @param userId the user ID
     */
    @Transactional
    public void goOffline(UUID teamId, UUID userId) {
        var opt = userPresenceRepository.findByUserIdAndTeamId(userId, teamId);
        if (opt.isPresent()) {
            UserPresence presence = opt.get();
            presence.setStatus(PresenceStatus.OFFLINE);
            presence.setStatusMessage(null);
            userPresenceRepository.save(presence);
            log.debug("User {} went offline", userId);
        }
    }

    /**
     * Transitions stale non-offline presences to OFFLINE for a team.
     *
     * <p>Can be called by a scheduled task or on-demand. Finds all presences where
     * status is not OFFLINE and the heartbeat has expired.</p>
     *
     * @param teamId the team ID
     * @return the count of users transitioned to OFFLINE
     */
    @Transactional
    public int cleanupStalePresences(UUID teamId) {
        List<UserPresence> active = userPresenceRepository.findByTeamIdAndStatusNot(
                teamId, PresenceStatus.OFFLINE);
        int count = 0;
        for (UserPresence presence : active) {
            if (isStale(presence)) {
                presence.setStatus(PresenceStatus.OFFLINE);
                userPresenceRepository.save(presence);
                count++;
            }
        }
        if (count > 0) {
            log.info("Cleaned up {} stale presences for team {}", count, teamId);
        }
        return count;
    }

    /**
     * Returns a count of users grouped by presence status for a team.
     *
     * <p>Cleans up stale presences first to ensure accurate counts.</p>
     *
     * @param teamId the team ID
     * @return map of status to count (e.g., {ONLINE: 5, AWAY: 2, DND: 1, OFFLINE: 12})
     */
    @Transactional
    public Map<PresenceStatus, Long> getPresenceCount(UUID teamId) {
        cleanupStalePresences(teamId);
        List<UserPresence> presences = userPresenceRepository.findByTeamId(teamId);
        return presences.stream()
                .collect(Collectors.groupingBy(UserPresence::getStatus, Collectors.counting()));
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Finds an existing presence record or creates a new unsaved one.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return the presence entity (may be unsaved if new)
     */
    private UserPresence findOrCreatePresence(UUID teamId, UUID userId) {
        return userPresenceRepository.findByUserIdAndTeamId(userId, teamId)
                .orElseGet(() -> UserPresence.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .status(PresenceStatus.OFFLINE)
                        .build());
    }

    /**
     * Checks whether a presence record's heartbeat has expired.
     *
     * @param presence the presence entity
     * @return true if the heartbeat is null or older than the timeout threshold
     */
    boolean isStale(UserPresence presence) {
        if (presence.getLastHeartbeatAt() == null) {
            return true;
        }
        Instant cutoff = Instant.now().minusSeconds(AppConstants.RELAY_PRESENCE_HEARTBEAT_TIMEOUT_SECONDS);
        return presence.getLastHeartbeatAt().isBefore(cutoff);
    }

    /**
     * Resolves a display name for a user, falling back to email.
     *
     * @param userId the user ID
     * @return the display name or email
     */
    private String resolveDisplayName(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getDisplayName() != null ? user.getDisplayName() : user.getEmail())
                .orElse("Unknown User");
    }

    /**
     * Returns a sort order for presence statuses.
     *
     * @param status the presence status
     * @return sort priority (ONLINE=0, AWAY=1, DND=2, OFFLINE=3)
     */
    private int statusSortOrder(PresenceStatus status) {
        return switch (status) {
            case ONLINE -> 0;
            case AWAY -> 1;
            case DND -> 2;
            case OFFLINE -> 3;
        };
    }

    /**
     * Verifies that a user is a member of the specified team.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @throws NotFoundException if the user is not a team member
     */
    private void verifyTeamMember(UUID teamId, UUID userId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new NotFoundException("Team member not found for team " + teamId);
        }
    }

    /**
     * Builds a UserPresenceResponse from a UserPresence entity.
     *
     * @param presence the presence entity
     * @return the response DTO with display name
     */
    private UserPresenceResponse buildResponse(UserPresence presence) {
        String displayName = resolveDisplayName(presence.getUserId());
        return new UserPresenceResponse(
                presence.getUserId(),
                displayName,
                presence.getTeamId(),
                presence.getStatus(),
                presence.getStatusMessage(),
                presence.getLastSeenAt(),
                presence.getUpdatedAt());
    }

    /**
     * Builds a default OFFLINE response for a user with no presence record.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return a default offline presence response
     */
    private UserPresenceResponse buildDefaultOfflineResponse(UUID teamId, UUID userId) {
        String displayName = resolveDisplayName(userId);
        return new UserPresenceResponse(userId, displayName, teamId,
                PresenceStatus.OFFLINE, null, null, null);
    }
}
