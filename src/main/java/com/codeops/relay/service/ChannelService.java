package com.codeops.relay.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.TeamRole;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.relay.dto.mapper.ChannelMapper;
import com.codeops.relay.dto.mapper.ChannelMemberMapper;
import com.codeops.relay.dto.request.CreateChannelRequest;
import com.codeops.relay.dto.request.InviteMemberRequest;
import com.codeops.relay.dto.request.PinMessageRequest;
import com.codeops.relay.dto.request.UpdateChannelRequest;
import com.codeops.relay.dto.request.UpdateChannelTopicRequest;
import com.codeops.relay.dto.request.UpdateMemberRoleRequest;
import com.codeops.relay.dto.response.ChannelMemberResponse;
import com.codeops.relay.dto.response.ChannelResponse;
import com.codeops.relay.dto.response.ChannelSummaryResponse;
import com.codeops.relay.dto.response.MessageResponse;
import com.codeops.relay.dto.response.PinnedMessageResponse;
import com.codeops.relay.entity.Channel;
import com.codeops.relay.entity.ChannelMember;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.PinnedMessage;
import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.MemberRole;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.ChannelRepository;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.PinnedMessageRepository;
import com.codeops.relay.repository.ReadReceiptRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Core service for managing Relay messaging channels.
 *
 * <p>Handles channel CRUD operations, topic management, member management
 * (join, leave, invite, remove, role updates), pinned messages, and
 * auto-channel creation for projects and services. Channels are the primary
 * messaging containers supporting four types: PUBLIC, PRIVATE, PROJECT, and SERVICE.</p>
 *
 * @see Channel
 * @see ChannelMember
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelService {

    private static final int MAX_SLUG_RETRIES = 100;

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final MessageRepository messageRepository;
    private final ReadReceiptRepository readReceiptRepository;
    private final PinnedMessageRepository pinnedMessageRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ChannelMapper channelMapper;
    private final ChannelMemberMapper channelMemberMapper;

    // ── Channel CRUD ──────────────────────────────────────────────────────

    /**
     * Creates a new messaging channel within a team.
     *
     * <p>Generates a URL-safe slug from the channel name, ensuring uniqueness per team
     * by appending numeric suffixes if necessary. The creator is automatically added
     * as a channel member with the OWNER role.</p>
     *
     * @param request the channel creation request
     * @param teamId  the team that owns the channel
     * @param userId  the user creating the channel
     * @return the created channel response with memberCount of 1
     * @throws NotFoundException   if the user is not a team member
     * @throws ValidationException if the channel name is blank or a unique slug cannot be generated
     */
    @Transactional
    public ChannelResponse createChannel(CreateChannelRequest request, UUID teamId, UUID userId) {
        verifyTeamMember(teamId, userId);

        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Channel name cannot be blank");
        }

        String slug = generateUniqueSlug(request.name(), teamId);

        Channel channel = channelMapper.toEntity(request);
        channel.setSlug(slug);
        channel.setTeamId(teamId);
        channel.setCreatedBy(userId);
        channel = channelRepository.save(channel);

        ChannelMember ownerMember = ChannelMember.builder()
                .channelId(channel.getId())
                .userId(userId)
                .role(MemberRole.OWNER)
                .joinedAt(Instant.now())
                .build();
        channelMemberRepository.save(ownerMember);

        log.info("Channel created: {} in team {}", slug, teamId);
        return buildChannelResponse(channel, 1);
    }

    /**
     * Retrieves a channel by ID with team verification.
     *
     * <p>For PRIVATE channels, the requesting user must be a channel member.</p>
     *
     * @param channelId the channel ID
     * @param teamId    the expected team ID
     * @param userId    the requesting user
     * @return the channel response with populated member count
     * @throws NotFoundException      if the channel does not exist or belongs to a different team
     * @throws AuthorizationException if the channel is private and the user is not a member
     */
    @Transactional(readOnly = true)
    public ChannelResponse getChannel(UUID channelId, UUID teamId, UUID userId) {
        Channel channel = findChannelByIdAndTeam(channelId, teamId);

        if (channel.getChannelType() == ChannelType.PRIVATE
                && !channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new AuthorizationException("User is not authorized to access this private channel");
        }

        int memberCount = (int) channelMemberRepository.countByChannelId(channelId);
        return buildChannelResponse(channel, memberCount);
    }

    /**
     * Retrieves all visible non-archived channels for a team.
     *
     * <p>Returns all PUBLIC, PROJECT, and SERVICE channels plus PRIVATE channels
     * where the user is a member. Each summary includes member count, unread count,
     * and last message timestamp. Results are sorted with unread channels first,
     * then by most recent activity.</p>
     *
     * @param teamId the team ID
     * @param userId the requesting user
     * @return sorted list of channel summaries
     */
    @Transactional(readOnly = true)
    public List<ChannelSummaryResponse> getChannelsByTeam(UUID teamId, UUID userId) {
        List<Channel> channels = channelRepository.findByTeamIdAndIsArchivedFalse(teamId);
        Set<UUID> userChannelIds = new HashSet<>(channelMemberRepository.findChannelIdsByUserId(userId));

        return channels.stream()
                .filter(ch -> ch.getChannelType() != ChannelType.PRIVATE
                        || userChannelIds.contains(ch.getId()))
                .map(ch -> buildChannelSummary(ch, userId, userChannelIds))
                .sorted(Comparator.comparingLong(
                                (ChannelSummaryResponse r) -> r.unreadCount() > 0 ? 0 : 1)
                        .thenComparing(ChannelSummaryResponse::lastMessageAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Retrieves visible channels for a team with pagination.
     *
     * @param teamId the team ID
     * @param userId the requesting user
     * @param page   zero-based page number
     * @param size   page size
     * @return paginated channel summaries
     */
    @Transactional(readOnly = true)
    public PageResponse<ChannelSummaryResponse> getChannelsByTeamPaged(
            UUID teamId, UUID userId, int page, int size) {
        List<ChannelSummaryResponse> all = getChannelsByTeam(teamId, userId);
        int start = page * size;
        int end = Math.min(start + size, all.size());
        List<ChannelSummaryResponse> content = start >= all.size()
                ? List.of() : all.subList(start, end);
        int totalPages = (int) Math.ceil((double) all.size() / size);
        boolean isLast = (page + 1) >= totalPages || totalPages == 0;
        return new PageResponse<>(content, page, size, all.size(), totalPages, isLast);
    }

    /**
     * Updates a channel's name, description, or archive status.
     *
     * <p>Only channel OWNERs and ADMINs may update a channel. If the name changes,
     * the slug is regenerated with uniqueness enforcement.</p>
     *
     * @param channelId the channel to update
     * @param request   the update request with optional fields
     * @param teamId    the team ID
     * @param userId    the requesting user
     * @return the updated channel response
     * @throws NotFoundException      if the channel does not exist
     * @throws AuthorizationException if the user is not a channel OWNER or ADMIN
     */
    @Transactional
    public ChannelResponse updateChannel(UUID channelId, UpdateChannelRequest request,
                                         UUID teamId, UUID userId) {
        Channel channel = findChannelByIdAndTeam(channelId, teamId);
        verifyChannelAdminOrOwner(channelId, userId);

        if (request.name() != null) {
            channel.setName(request.name());
            channel.setSlug(generateUniqueSlug(request.name(), teamId));
        }
        if (request.description() != null) {
            channel.setDescription(request.description());
        }
        if (request.isArchived() != null) {
            channel.setArchived(request.isArchived());
        }

        channel = channelRepository.save(channel);
        int memberCount = (int) channelMemberRepository.countByChannelId(channelId);
        return buildChannelResponse(channel, memberCount);
    }

    /**
     * Permanently deletes a channel and all associated data.
     *
     * <p>Only the channel OWNER or team ADMIN/OWNER may delete a channel.
     * The #general channel cannot be deleted. Deletes messages, read receipts,
     * and the channel itself (members and pins cascade).</p>
     *
     * @param channelId the channel to delete
     * @param teamId    the team ID
     * @param userId    the requesting user
     * @throws NotFoundException      if the channel does not exist
     * @throws AuthorizationException if the user lacks permission
     * @throws ValidationException    if attempting to delete the #general channel
     */
    @Transactional
    public void deleteChannel(UUID channelId, UUID teamId, UUID userId) {
        Channel channel = findChannelByIdAndTeam(channelId, teamId);
        verifyChannelOwnerOrTeamAdmin(channelId, teamId, userId);

        if (AppConstants.RELAY_GENERAL_CHANNEL_SLUG.equals(channel.getSlug())) {
            throw new ValidationException("Cannot delete the #general channel");
        }

        messageRepository.deleteByChannelId(channelId);
        readReceiptRepository.deleteByChannelId(channelId);
        channelRepository.delete(channel);

        log.info("Channel deleted: {} by {}", channel.getSlug(), userId);
    }

    /**
     * Archives a channel, making it read-only.
     *
     * @param channelId the channel to archive
     * @param teamId    the team ID
     * @param userId    the requesting user
     * @return the updated channel response
     * @throws NotFoundException      if the channel does not exist
     * @throws AuthorizationException if the user lacks permission
     * @throws ValidationException    if attempting to archive #general
     */
    @Transactional
    public ChannelResponse archiveChannel(UUID channelId, UUID teamId, UUID userId) {
        Channel channel = findChannelByIdAndTeam(channelId, teamId);
        verifyChannelAdminOwnerOrTeamAdmin(channelId, teamId, userId);

        if (AppConstants.RELAY_GENERAL_CHANNEL_SLUG.equals(channel.getSlug())) {
            throw new ValidationException("Cannot archive the #general channel");
        }

        channel.setArchived(true);
        channel = channelRepository.save(channel);
        int memberCount = (int) channelMemberRepository.countByChannelId(channelId);
        return buildChannelResponse(channel, memberCount);
    }

    /**
     * Unarchives a previously archived channel.
     *
     * @param channelId the channel to unarchive
     * @param teamId    the team ID
     * @param userId    the requesting user
     * @return the updated channel response
     * @throws NotFoundException      if the channel does not exist
     * @throws AuthorizationException if the user lacks permission
     */
    @Transactional
    public ChannelResponse unarchiveChannel(UUID channelId, UUID teamId, UUID userId) {
        Channel channel = findChannelByIdAndTeam(channelId, teamId);
        verifyChannelAdminOwnerOrTeamAdmin(channelId, teamId, userId);

        channel.setArchived(false);
        channel = channelRepository.save(channel);
        int memberCount = (int) channelMemberRepository.countByChannelId(channelId);
        return buildChannelResponse(channel, memberCount);
    }

    // ── Channel Topic ─────────────────────────────────────────────────────

    /**
     * Updates a channel's topic. Any channel member can change the topic.
     *
     * @param channelId the channel ID
     * @param request   the topic update request
     * @param teamId    the team ID
     * @param userId    the requesting user
     * @return the updated channel response
     * @throws NotFoundException      if the channel does not exist
     * @throws AuthorizationException if the user is not a channel member
     */
    @Transactional
    public ChannelResponse updateTopic(UUID channelId, UpdateChannelTopicRequest request,
                                       UUID teamId, UUID userId) {
        Channel channel = findChannelByIdAndTeam(channelId, teamId);
        verifyChannelMember(channelId, userId);

        channel.setTopic(request.topic());
        channel = channelRepository.save(channel);
        int memberCount = (int) channelMemberRepository.countByChannelId(channelId);
        return buildChannelResponse(channel, memberCount);
    }

    // ── Member Management ─────────────────────────────────────────────────

    /**
     * Joins a public, project, or service channel.
     *
     * @param channelId the channel to join
     * @param teamId    the team ID
     * @param userId    the user joining
     * @return the new membership response
     * @throws NotFoundException      if the channel does not exist
     * @throws AuthorizationException if the channel is private
     * @throws ValidationException    if the user is already a member
     */
    @Transactional
    public ChannelMemberResponse joinChannel(UUID channelId, UUID teamId, UUID userId) {
        Channel channel = findChannelByIdAndTeam(channelId, teamId);

        if (channel.getChannelType() == ChannelType.PRIVATE) {
            throw new AuthorizationException("Use invite to join private channels");
        }
        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new ValidationException("User is already a member of this channel");
        }

        ChannelMember member = ChannelMember.builder()
                .channelId(channelId)
                .userId(userId)
                .role(MemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();
        member = channelMemberRepository.save(member);

        log.info("User {} joined channel {}", userId, channel.getSlug());
        return buildMemberResponse(member);
    }

    /**
     * Leaves a channel. The last owner/admin cannot leave without transferring ownership.
     *
     * @param channelId the channel to leave
     * @param teamId    the team ID
     * @param userId    the user leaving
     * @throws NotFoundException   if the user is not a channel member
     * @throws ValidationException if the user is the last owner/admin
     */
    @Transactional
    public void leaveChannel(UUID channelId, UUID teamId, UUID userId) {
        findChannelByIdAndTeam(channelId, teamId);
        ChannelMember member = channelMemberRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new NotFoundException("Channel membership not found"));

        if (member.getRole() == MemberRole.OWNER) {
            boolean hasOtherAdminOrOwner = channelMemberRepository.findByChannelId(channelId).stream()
                    .filter(m -> !m.getUserId().equals(userId))
                    .anyMatch(m -> m.getRole() == MemberRole.OWNER || m.getRole() == MemberRole.ADMIN);
            if (!hasOtherAdminOrOwner) {
                throw new ValidationException(
                        "Cannot leave channel: you are the last owner/admin. Transfer ownership first.");
            }
        }

        channelMemberRepository.delete(member);
        readReceiptRepository.findByChannelIdAndUserId(channelId, userId)
                .ifPresent(readReceiptRepository::delete);

        log.info("User {} left channel {}", userId, channelId);
    }

    /**
     * Invites a user to a channel. Only channel OWNERs and ADMINs may invite.
     *
     * @param channelId the channel ID
     * @param request   the invite request containing userId and optional role
     * @param teamId    the team ID
     * @param inviterId the user performing the invite
     * @return the new membership response
     * @throws NotFoundException      if the channel does not exist
     * @throws AuthorizationException if the inviter is not a channel OWNER/ADMIN
     * @throws ValidationException    if the user is already a member or not a team member
     */
    @Transactional
    public ChannelMemberResponse inviteMember(UUID channelId, InviteMemberRequest request,
                                              UUID teamId, UUID inviterId) {
        findChannelByIdAndTeam(channelId, teamId);
        verifyChannelAdminOrOwner(channelId, inviterId);

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, request.userId())) {
            throw new ValidationException("Invited user is not a member of this team");
        }
        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, request.userId())) {
            throw new ValidationException("User is already a member of this channel");
        }

        MemberRole role = request.role() != null ? request.role() : MemberRole.MEMBER;
        ChannelMember member = ChannelMember.builder()
                .channelId(channelId)
                .userId(request.userId())
                .role(role)
                .joinedAt(Instant.now())
                .build();
        member = channelMemberRepository.save(member);

        return buildMemberResponse(member);
    }

    /**
     * Removes a member from a channel.
     *
     * @param channelId    the channel ID
     * @param targetUserId the user to remove
     * @param teamId       the team ID
     * @param removerId    the user performing the removal
     * @throws AuthorizationException if the remover lacks permission
     * @throws ValidationException    if attempting self-removal or removing an owner without team owner role
     */
    @Transactional
    public void removeMember(UUID channelId, UUID targetUserId, UUID teamId, UUID removerId) {
        if (targetUserId.equals(removerId)) {
            throw new ValidationException("Cannot remove yourself. Use leaveChannel instead.");
        }

        verifyChannelOwnerOrTeamAdmin(channelId, teamId, removerId);

        ChannelMember targetMember = channelMemberRepository.findByChannelIdAndUserId(channelId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Channel member not found"));

        if (targetMember.getRole() == MemberRole.OWNER) {
            TeamMember removerTeamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, removerId)
                    .orElseThrow(() -> new AuthorizationException("Not a team member"));
            if (removerTeamMember.getRole() != TeamRole.OWNER) {
                throw new AuthorizationException("Only team owner can remove a channel owner");
            }
        }

        channelMemberRepository.delete(targetMember);
        readReceiptRepository.findByChannelIdAndUserId(channelId, targetUserId)
                .ifPresent(readReceiptRepository::delete);
    }

    /**
     * Updates a channel member's role.
     *
     * @param channelId    the channel ID
     * @param targetUserId the user whose role to change
     * @param request      the role update request
     * @param teamId       the team ID
     * @param updaterId    the user performing the update
     * @return the updated membership response
     * @throws AuthorizationException if the updater lacks permission
     * @throws ValidationException    if attempting to change own role
     */
    @Transactional
    public ChannelMemberResponse updateMemberRole(UUID channelId, UUID targetUserId,
                                                   UpdateMemberRoleRequest request,
                                                   UUID teamId, UUID updaterId) {
        if (targetUserId.equals(updaterId)) {
            throw new ValidationException("Cannot change your own role");
        }

        ChannelMember updaterMember = channelMemberRepository
                .findByChannelIdAndUserId(channelId, updaterId).orElse(null);
        boolean isChannelOwner = updaterMember != null && updaterMember.getRole() == MemberRole.OWNER;

        TeamMember updaterTeamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, updaterId)
                .orElseThrow(() -> new AuthorizationException("Not a team member"));
        boolean isTeamAdminOrOwner = updaterTeamMember.getRole() == TeamRole.OWNER
                || updaterTeamMember.getRole() == TeamRole.ADMIN;

        if (!isChannelOwner && !isTeamAdminOrOwner) {
            throw new AuthorizationException(
                    "Requires channel OWNER or team ADMIN/OWNER role to update member roles");
        }

        if (request.role() == MemberRole.OWNER && !isChannelOwner
                && updaterTeamMember.getRole() != TeamRole.OWNER) {
            throw new AuthorizationException(
                    "Only channel OWNER or team OWNER can promote to OWNER");
        }

        ChannelMember targetMember = channelMemberRepository
                .findByChannelIdAndUserId(channelId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Channel member not found"));

        targetMember.setRole(request.role());
        targetMember = channelMemberRepository.save(targetMember);

        return buildMemberResponse(targetMember);
    }

    /**
     * Retrieves all members of a channel with populated display names.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @return list of channel member responses
     * @throws NotFoundException if the channel does not exist
     */
    @Transactional(readOnly = true)
    public List<ChannelMemberResponse> getMembers(UUID channelId, UUID teamId) {
        findChannelByIdAndTeam(channelId, teamId);
        List<ChannelMember> members = channelMemberRepository.findByChannelId(channelId);

        return members.stream()
                .map(this::buildMemberResponse)
                .toList();
    }

    // ── Pinned Messages ───────────────────────────────────────────────────

    /**
     * Pins a message in a channel.
     *
     * @param channelId the channel ID
     * @param request   the pin request containing the message ID
     * @param teamId    the team ID
     * @param userId    the user pinning the message
     * @return the pinned message response with embedded message details
     * @throws NotFoundException   if the channel or message does not exist
     * @throws ValidationException if the message is already pinned or max pins exceeded
     */
    @Transactional
    public PinnedMessageResponse pinMessage(UUID channelId, PinMessageRequest request,
                                            UUID teamId, UUID userId) {
        Channel channel = findChannelByIdAndTeam(channelId, teamId);
        verifyChannelMember(channelId, userId);

        Message message = messageRepository.findById(request.messageId())
                .orElseThrow(() -> new NotFoundException("Message", request.messageId()));

        if (!message.getChannelId().equals(channelId)) {
            throw new ValidationException("Message does not belong to this channel");
        }
        if (pinnedMessageRepository.existsByChannelIdAndMessageId(channelId, request.messageId())) {
            throw new ValidationException("Message is already pinned in this channel");
        }
        if (pinnedMessageRepository.countByChannelId(channelId)
                >= AppConstants.RELAY_MAX_PINS_PER_CHANNEL) {
            throw new ValidationException(
                    "Channel has reached the maximum number of pinned messages ("
                            + AppConstants.RELAY_MAX_PINS_PER_CHANNEL + ")");
        }

        PinnedMessage pin = PinnedMessage.builder()
                .messageId(request.messageId())
                .pinnedBy(userId)
                .channel(channel)
                .build();
        pin = pinnedMessageRepository.save(pin);

        return buildPinnedMessageResponse(pin, message);
    }

    /**
     * Unpins a message from a channel.
     *
     * @param channelId the channel ID
     * @param messageId the message to unpin
     * @param teamId    the team ID
     * @param userId    the requesting user
     * @throws NotFoundException      if the pin does not exist
     * @throws AuthorizationException if the user is not a channel member
     */
    @Transactional
    public void unpinMessage(UUID channelId, UUID messageId, UUID teamId, UUID userId) {
        findChannelByIdAndTeam(channelId, teamId);
        verifyChannelMember(channelId, userId);

        PinnedMessage pin = pinnedMessageRepository.findByChannelIdAndMessageId(channelId, messageId)
                .orElseThrow(() -> new NotFoundException("Pinned message not found"));

        pinnedMessageRepository.delete(pin);
    }

    /**
     * Retrieves all pinned messages in a channel.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @return list of pinned message responses ordered by most recently pinned
     */
    @Transactional(readOnly = true)
    public List<PinnedMessageResponse> getPinnedMessages(UUID channelId, UUID teamId) {
        findChannelByIdAndTeam(channelId, teamId);
        List<PinnedMessage> pins = pinnedMessageRepository.findByChannelIdOrderByCreatedAtDesc(channelId);

        return pins.stream()
                .map(pin -> {
                    Message message = messageRepository.findById(pin.getMessageId()).orElse(null);
                    return buildPinnedMessageResponse(pin, message);
                })
                .toList();
    }

    // ── Auto-Channel Creation ─────────────────────────────────────────────

    /**
     * Creates an auto-linked PROJECT channel for a newly created project.
     *
     * <p>Called internally by PlatformEventService. All team members are
     * auto-added as channel members. No authorization check is performed.</p>
     *
     * @param projectId   the project ID to link
     * @param projectName the project name used to generate the channel name and slug
     * @param teamId      the team ID
     * @return the created channel entity
     */
    @Transactional
    public Channel createProjectChannel(UUID projectId, String projectName, UUID teamId) {
        String slug = generateUniqueSlug("project-" + projectName, teamId);

        Channel channel = Channel.builder()
                .name(projectName)
                .slug(slug)
                .channelType(ChannelType.PROJECT)
                .teamId(teamId)
                .projectId(projectId)
                .createdBy(teamId)
                .build();
        channel = channelRepository.save(channel);

        addAllTeamMembers(channel.getId(), teamId);

        log.info("Project channel created: {} for project {}", slug, projectId);
        return channel;
    }

    /**
     * Creates an auto-linked SERVICE channel for a newly registered service.
     *
     * <p>Called internally when a service is registered in Registry.
     * Only team ADMIN and OWNER users are auto-added as members.</p>
     *
     * @param serviceId   the service ID to link
     * @param serviceName the service name used to generate the channel name and slug
     * @param teamId      the team ID
     * @return the created channel entity
     */
    @Transactional
    public Channel createServiceChannel(UUID serviceId, String serviceName, UUID teamId) {
        String slug = generateUniqueSlug("service-" + serviceName, teamId);

        Channel channel = Channel.builder()
                .name(serviceName)
                .slug(slug)
                .channelType(ChannelType.SERVICE)
                .teamId(teamId)
                .serviceId(serviceId)
                .createdBy(teamId)
                .build();
        channel = channelRepository.save(channel);

        List<TeamMember> admins = teamMemberRepository.findByTeamId(teamId).stream()
                .filter(tm -> tm.getRole() == TeamRole.OWNER || tm.getRole() == TeamRole.ADMIN)
                .toList();
        for (TeamMember tm : admins) {
            ChannelMember member = ChannelMember.builder()
                    .channelId(channel.getId())
                    .userId(tm.getUser().getId())
                    .role(MemberRole.MEMBER)
                    .joinedAt(Instant.now())
                    .build();
            channelMemberRepository.save(member);
        }

        log.info("Service channel created: {} for service {}", slug, serviceId);
        return channel;
    }

    /**
     * Ensures a #general channel exists for a team, creating one if missing.
     *
     * <p>Used by DataSeeder and when a new team is created. If the channel
     * already exists, it is returned as-is. All team members are auto-added
     * when creating a new #general channel.</p>
     *
     * @param teamId        the team ID
     * @param creatorUserId the user to record as the channel creator
     * @return the existing or newly created #general channel
     */
    @Transactional
    public Channel ensureGeneralChannel(UUID teamId, UUID creatorUserId) {
        return channelRepository.findByTeamIdAndSlug(teamId, AppConstants.RELAY_GENERAL_CHANNEL_SLUG)
                .orElseGet(() -> {
                    Channel channel = Channel.builder()
                            .name("#general")
                            .slug(AppConstants.RELAY_GENERAL_CHANNEL_SLUG)
                            .channelType(ChannelType.PUBLIC)
                            .teamId(teamId)
                            .createdBy(creatorUserId)
                            .description("General discussion for the team")
                            .build();
                    channel = channelRepository.save(channel);
                    addAllTeamMembers(channel.getId(), teamId);
                    log.info("General channel created for team {}", teamId);
                    return channel;
                });
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String generateUniqueSlug(String name, UUID teamId) {
        String baseSlug = slugify(name);
        if (baseSlug.isEmpty()) {
            baseSlug = "channel";
        }
        String slug = baseSlug;
        int suffix = 1;
        while (channelRepository.existsByTeamIdAndSlug(teamId, slug) && suffix <= MAX_SLUG_RETRIES) {
            slug = baseSlug + "-" + suffix++;
        }
        if (channelRepository.existsByTeamIdAndSlug(teamId, slug)) {
            throw new ValidationException(
                    "Unable to generate unique slug for channel name: " + name);
        }
        return slug;
    }

    private Channel findChannelByIdAndTeam(UUID channelId, UUID teamId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Channel", channelId));
        if (!channel.getTeamId().equals(teamId)) {
            throw new NotFoundException("Channel", channelId);
        }
        return channel;
    }

    private void verifyTeamMember(UUID teamId, UUID userId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new NotFoundException("User is not a member of this team");
        }
    }

    private ChannelMember verifyChannelMember(UUID channelId, UUID userId) {
        return channelMemberRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new AuthorizationException(
                        "User is not a member of this channel"));
    }

    private ChannelMember verifyChannelAdminOrOwner(UUID channelId, UUID userId) {
        ChannelMember member = verifyChannelMember(channelId, userId);
        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new AuthorizationException(
                    "Requires channel OWNER or ADMIN role");
        }
        return member;
    }

    private void verifyChannelOwnerOrTeamAdmin(UUID channelId, UUID teamId, UUID userId) {
        ChannelMember channelMember = channelMemberRepository
                .findByChannelIdAndUserId(channelId, userId).orElse(null);
        if (channelMember != null && channelMember.getRole() == MemberRole.OWNER) {
            return;
        }
        TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new AuthorizationException("Not a team member"));
        if (teamMember.getRole() != TeamRole.OWNER && teamMember.getRole() != TeamRole.ADMIN) {
            throw new AuthorizationException(
                    "Requires channel OWNER or team ADMIN/OWNER role");
        }
    }

    private void verifyChannelAdminOwnerOrTeamAdmin(UUID channelId, UUID teamId, UUID userId) {
        ChannelMember channelMember = channelMemberRepository
                .findByChannelIdAndUserId(channelId, userId).orElse(null);
        if (channelMember != null
                && (channelMember.getRole() == MemberRole.OWNER
                || channelMember.getRole() == MemberRole.ADMIN)) {
            return;
        }
        TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new AuthorizationException("Not a team member"));
        if (teamMember.getRole() != TeamRole.OWNER && teamMember.getRole() != TeamRole.ADMIN) {
            throw new AuthorizationException(
                    "Requires channel OWNER/ADMIN or team ADMIN/OWNER role");
        }
    }

    private String populateUserDisplayName(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getDisplayName() != null ? user.getDisplayName() : user.getEmail())
                .orElse("Unknown User");
    }

    private void addAllTeamMembers(UUID channelId, UUID teamId) {
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(teamId);
        for (TeamMember tm : teamMembers) {
            ChannelMember member = ChannelMember.builder()
                    .channelId(channelId)
                    .userId(tm.getUser().getId())
                    .role(MemberRole.MEMBER)
                    .joinedAt(Instant.now())
                    .build();
            channelMemberRepository.save(member);
        }
    }

    private ChannelResponse buildChannelResponse(Channel channel, int memberCount) {
        return new ChannelResponse(
                channel.getId(), channel.getName(), channel.getSlug(),
                channel.getDescription(), channel.getTopic(),
                channel.getChannelType(), channel.getTeamId(),
                channel.getProjectId(), channel.getServiceId(),
                channel.isArchived(), channel.getCreatedBy(),
                memberCount, channel.getCreatedAt(), channel.getUpdatedAt());
    }

    private ChannelSummaryResponse buildChannelSummary(Channel channel, UUID userId,
                                                        Set<UUID> userChannelIds) {
        int memberCount = (int) channelMemberRepository.countByChannelId(channel.getId());
        long unreadCount = 0;
        Instant lastMessageAt = null;

        Page<Message> latestMessages = messageRepository
                .findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        channel.getId(), PageRequest.of(0, 1));
        if (latestMessages.hasContent()) {
            lastMessageAt = latestMessages.getContent().get(0).getCreatedAt();
        }

        if (userChannelIds.contains(channel.getId())) {
            ChannelMember membership = channelMemberRepository
                    .findByChannelIdAndUserId(channel.getId(), userId).orElse(null);
            if (membership != null && membership.getLastReadAt() != null) {
                unreadCount = messageRepository.countUnreadMessages(
                        channel.getId(), membership.getLastReadAt());
            } else if (membership != null) {
                unreadCount = messageRepository.countUnreadMessages(
                        channel.getId(), Instant.EPOCH);
            }
        }

        return new ChannelSummaryResponse(
                channel.getId(), channel.getName(), channel.getSlug(),
                channel.getChannelType(), channel.getTopic(),
                channel.isArchived(), memberCount, unreadCount, lastMessageAt);
    }

    private ChannelMemberResponse buildMemberResponse(ChannelMember member) {
        String displayName = populateUserDisplayName(member.getUserId());
        return new ChannelMemberResponse(
                member.getId(), member.getChannelId(), member.getUserId(),
                displayName, member.getRole(), member.isMuted(),
                member.getLastReadAt(), member.getJoinedAt());
    }

    private PinnedMessageResponse buildPinnedMessageResponse(PinnedMessage pin, Message message) {
        MessageResponse messageResponse = null;
        if (message != null) {
            String senderDisplayName = populateUserDisplayName(message.getSenderId());
            messageResponse = new MessageResponse(
                    message.getId(), message.getChannelId(), message.getSenderId(),
                    senderDisplayName, message.getContent(), message.getMessageType(),
                    message.getParentId(), message.isEdited(), message.getEditedAt(),
                    message.isDeleted(), message.isMentionsEveryone(),
                    null, message.getPlatformEventId(),
                    null, null, 0, null,
                    message.getCreatedAt(), message.getUpdatedAt());
        }
        return new PinnedMessageResponse(
                pin.getId(), pin.getMessageId(), pin.getChannel().getId(),
                messageResponse, pin.getPinnedBy(), pin.getCreatedAt());
    }
}
