package com.codeops.relay.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.response.PageResponse;
import com.codeops.relay.dto.request.*;
import com.codeops.relay.dto.response.*;
import com.codeops.relay.service.ChannelService;
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
 * REST controller for channel management in the Relay module.
 *
 * <p>Provides endpoints for creating, reading, updating, and deleting channels,
 * managing channel membership (join, leave, invite, remove, role updates),
 * channel lifecycle operations (archive, unarchive, topic updates),
 * and pinned message management.</p>
 *
 * <p>All endpoints require authentication and team membership is verified
 * in the service layer.</p>
 */
@RestController
@RequestMapping(AppConstants.RELAY_API_PREFIX + "/channels")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class ChannelController {

    private final ChannelService channelService;

    /**
     * Creates a new channel in a team.
     *
     * @param request  the channel creation request
     * @param teamId   the team ID
     * @return the created channel
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelResponse createChannel(
            @RequestBody @Valid CreateChannelRequest request,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.createChannel(request, teamId, userId);
    }

    /**
     * Retrieves a channel by ID.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @return the channel details
     */
    @GetMapping("/{channelId}")
    public ChannelResponse getChannel(
            @PathVariable UUID channelId,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.getChannel(channelId, teamId, userId);
    }

    /**
     * Lists channels for a team with pagination.
     *
     * @param teamId the team ID
     * @param page   the page number (zero-based)
     * @param size   the page size
     * @return a page of channel summaries
     */
    @GetMapping
    public PageResponse<ChannelSummaryResponse> getChannels(
            @RequestParam UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.getChannelsByTeamPaged(teamId, userId, page, size);
    }

    /**
     * Updates a channel's name, description, or archived status.
     *
     * @param channelId the channel ID
     * @param request   the update request
     * @param teamId    the team ID
     * @return the updated channel
     */
    @PutMapping("/{channelId}")
    public ChannelResponse updateChannel(
            @PathVariable UUID channelId,
            @RequestBody @Valid UpdateChannelRequest request,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.updateChannel(channelId, request, teamId, userId);
    }

    /**
     * Deletes a channel.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     */
    @DeleteMapping("/{channelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChannel(
            @PathVariable UUID channelId,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        channelService.deleteChannel(channelId, teamId, userId);
    }

    /**
     * Archives a channel.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @return the archived channel
     */
    @PostMapping("/{channelId}/archive")
    public ChannelResponse archiveChannel(
            @PathVariable UUID channelId,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.archiveChannel(channelId, teamId, userId);
    }

    /**
     * Unarchives a channel.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @return the unarchived channel
     */
    @PostMapping("/{channelId}/unarchive")
    public ChannelResponse unarchiveChannel(
            @PathVariable UUID channelId,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.unarchiveChannel(channelId, teamId, userId);
    }

    /**
     * Updates a channel's topic.
     *
     * @param channelId the channel ID
     * @param request   the topic update request
     * @param teamId    the team ID
     * @return the updated channel
     */
    @PatchMapping("/{channelId}/topic")
    public ChannelResponse updateTopic(
            @PathVariable UUID channelId,
            @RequestBody @Valid UpdateChannelTopicRequest request,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.updateTopic(channelId, request, teamId, userId);
    }

    /**
     * Joins a channel.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @return the channel membership
     */
    @PostMapping("/{channelId}/join")
    public ChannelMemberResponse joinChannel(
            @PathVariable UUID channelId,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.joinChannel(channelId, teamId, userId);
    }

    /**
     * Leaves a channel.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     */
    @PostMapping("/{channelId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveChannel(
            @PathVariable UUID channelId,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        channelService.leaveChannel(channelId, teamId, userId);
    }

    /**
     * Invites a user to a channel.
     *
     * @param channelId the channel ID
     * @param request   the invite request containing the user ID and optional role
     * @param teamId    the team ID
     * @return the new channel membership
     */
    @PostMapping("/{channelId}/members/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelMemberResponse inviteMember(
            @PathVariable UUID channelId,
            @RequestBody @Valid InviteMemberRequest request,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.inviteMember(channelId, request, teamId, userId);
    }

    /**
     * Removes a member from a channel.
     *
     * @param channelId    the channel ID
     * @param targetUserId the user ID to remove
     * @param teamId       the team ID
     */
    @DeleteMapping("/{channelId}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID channelId,
            @PathVariable UUID targetUserId,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        channelService.removeMember(channelId, targetUserId, teamId, userId);
    }

    /**
     * Updates a member's role in a channel.
     *
     * @param channelId    the channel ID
     * @param targetUserId the target user ID
     * @param request      the role update request
     * @param teamId       the team ID
     * @return the updated membership
     */
    @PutMapping("/{channelId}/members/{targetUserId}/role")
    public ChannelMemberResponse updateMemberRole(
            @PathVariable UUID channelId,
            @PathVariable UUID targetUserId,
            @RequestBody @Valid UpdateMemberRoleRequest request,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.updateMemberRole(channelId, targetUserId, request, teamId, userId);
    }

    /**
     * Lists all members of a channel.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @return the list of channel members
     */
    @GetMapping("/{channelId}/members")
    public List<ChannelMemberResponse> getMembers(
            @PathVariable UUID channelId,
            @RequestParam UUID teamId) {
        return channelService.getMembers(channelId, teamId);
    }

    /**
     * Pins a message in a channel.
     *
     * @param channelId the channel ID
     * @param request   the pin request containing the message ID
     * @param teamId    the team ID
     * @return the pinned message details
     */
    @PostMapping("/{channelId}/pins")
    @ResponseStatus(HttpStatus.CREATED)
    public PinnedMessageResponse pinMessage(
            @PathVariable UUID channelId,
            @RequestBody @Valid PinMessageRequest request,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return channelService.pinMessage(channelId, request, teamId, userId);
    }

    /**
     * Unpins a message from a channel.
     *
     * @param channelId the channel ID
     * @param messageId the message ID to unpin
     * @param teamId    the team ID
     */
    @DeleteMapping("/{channelId}/pins/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpinMessage(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        channelService.unpinMessage(channelId, messageId, teamId, userId);
    }

    /**
     * Lists all pinned messages in a channel.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @return the list of pinned messages
     */
    @GetMapping("/{channelId}/pins")
    public List<PinnedMessageResponse> getPinnedMessages(
            @PathVariable UUID channelId,
            @RequestParam UUID teamId) {
        return channelService.getPinnedMessages(channelId, teamId);
    }
}
