package com.codeops.relay.service;

import com.codeops.entity.User;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.relay.dto.mapper.ReactionMapper;
import com.codeops.relay.dto.request.AddReactionRequest;
import com.codeops.relay.dto.response.ReactionResponse;
import com.codeops.relay.dto.response.ReactionSummaryResponse;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.Reaction;
import com.codeops.relay.entity.enums.ReactionType;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.ReactionRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing emoji reactions on channel messages.
 *
 * <p>Supports toggle-based add/remove, aggregated summaries grouped by emoji,
 * per-user reaction listings, and bulk deletion for message cleanup.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final UserRepository userRepository;
    private final ReactionMapper reactionMapper;

    /**
     * Toggles an emoji reaction on a channel message.
     *
     * <p>If the user already has the same emoji on the message, removes it (toggle off).
     * Otherwise, creates a new reaction (toggle on).</p>
     *
     * @param messageId the channel message ID
     * @param request   the reaction request containing the emoji
     * @param userId    the reacting user's ID
     * @return the created ReactionResponse, or {@code null} if the reaction was removed
     * @throws NotFoundException      if the message does not exist
     * @throws AuthorizationException if the user is not a member of the message's channel
     * @throws ValidationException    if the emoji is blank or exceeds 32 characters
     */
    @Transactional
    public ReactionResponse toggleReaction(UUID messageId, AddReactionRequest request, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message", messageId));
        verifyChannelMember(message.getChannelId(), userId);

        Optional<Reaction> existing = reactionRepository.findByMessageIdAndUserIdAndEmoji(
                messageId, userId, request.emoji());

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            log.debug("Reaction removed: {} from message {} by {}", request.emoji(), messageId, userId);
            return null;
        }

        if (request.emoji().isBlank()) {
            throw new ValidationException("Emoji must not be blank");
        }
        if (request.emoji().length() > 32) {
            throw new ValidationException("Emoji must not exceed 32 characters");
        }

        Reaction reaction = Reaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emoji(request.emoji())
                .reactionType(ReactionType.EMOJI)
                .build();
        reaction = reactionRepository.save(reaction);
        log.debug("Reaction added: {} to message {} by {}", request.emoji(), messageId, userId);

        return buildReactionResponse(reaction);
    }

    /**
     * Retrieves aggregated reaction summaries for a message.
     *
     * <p>Groups reactions by emoji, counts each group, and sorts by count descending
     * then by emoji ascending for stability. The {@code currentUserReacted} flag is
     * set to {@code false} — use {@link #getReactionsForMessageWithUser} for per-user flags.</p>
     *
     * @param messageId the channel message ID
     * @return list of reaction summaries grouped by emoji
     * @throws NotFoundException if the message does not exist
     */
    @Transactional(readOnly = true)
    public List<ReactionSummaryResponse> getReactionsForMessage(UUID messageId) {
        verifyMessageExists(messageId);
        List<Reaction> reactions = reactionRepository.findByMessageId(messageId);
        return buildSummaries(reactions, null);
    }

    /**
     * Retrieves aggregated reaction summaries with per-user participation flag.
     *
     * <p>Same as {@link #getReactionsForMessage} but sets {@code currentUserReacted}
     * to {@code true} for any emoji group containing the specified user.</p>
     *
     * @param messageId     the channel message ID
     * @param currentUserId the user ID to check participation for
     * @return list of reaction summaries with currentUserReacted populated
     * @throws NotFoundException if the message does not exist
     */
    @Transactional(readOnly = true)
    public List<ReactionSummaryResponse> getReactionsForMessageWithUser(UUID messageId, UUID currentUserId) {
        verifyMessageExists(messageId);
        List<Reaction> reactions = reactionRepository.findByMessageId(messageId);
        return buildSummaries(reactions, currentUserId);
    }

    /**
     * Retrieves all reactions by a specific user on messages in a channel.
     *
     * @param userId    the user ID
     * @param channelId the channel ID
     * @return list of individual reaction responses
     */
    @Transactional(readOnly = true)
    public List<ReactionResponse> getReactionsByUser(UUID userId, UUID channelId) {
        List<Reaction> reactions = reactionRepository.findByUserIdAndMessageChannelId(userId, channelId);
        return reactions.stream()
                .map(this::buildReactionResponse)
                .toList();
    }

    /**
     * Deletes all reactions for a message (bulk cleanup).
     *
     * <p>Used internally when a message is hard-deleted.</p>
     *
     * @param messageId the channel message ID
     */
    @Transactional
    public void removeAllReactionsForMessage(UUID messageId) {
        reactionRepository.deleteByMessageId(messageId);
        log.debug("All reactions removed for message {}", messageId);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Verifies that a message exists.
     *
     * @param messageId the message ID
     * @throws NotFoundException if the message does not exist
     */
    private void verifyMessageExists(UUID messageId) {
        if (!messageRepository.existsById(messageId)) {
            throw new NotFoundException("Message", messageId);
        }
    }

    /**
     * Verifies that a user is a member of the specified channel.
     *
     * @param channelId the channel ID
     * @param userId    the user ID
     * @throws AuthorizationException if the user is not a channel member
     */
    private void verifyChannelMember(UUID channelId, UUID userId) {
        if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new AuthorizationException("User is not a member of this channel");
        }
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
     * Builds a ReactionResponse from a Reaction entity, resolving the user display name.
     *
     * @param reaction the reaction entity
     * @return the response DTO
     */
    private ReactionResponse buildReactionResponse(Reaction reaction) {
        String displayName = resolveDisplayName(reaction.getUserId());
        return new ReactionResponse(
                reaction.getId(),
                reaction.getUserId(),
                displayName,
                reaction.getEmoji(),
                reaction.getReactionType(),
                reaction.getCreatedAt());
    }

    /**
     * Builds aggregated reaction summaries grouped by emoji.
     *
     * <p>Groups reactions by emoji, counts each group, collects user IDs, and sets
     * the {@code currentUserReacted} flag if a currentUserId is provided.</p>
     *
     * @param reactions     the list of reactions
     * @param currentUserId the current user ID (null to skip per-user flag)
     * @return sorted list of reaction summaries
     */
    private List<ReactionSummaryResponse> buildSummaries(List<Reaction> reactions, UUID currentUserId) {
        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String emoji = entry.getKey();
                    List<Reaction> group = entry.getValue();
                    List<UUID> userIds = group.stream()
                            .map(Reaction::getUserId)
                            .toList();
                    boolean currentUserReacted = currentUserId != null && userIds.contains(currentUserId);
                    return new ReactionSummaryResponse(emoji, group.size(), currentUserReacted, userIds);
                })
                .sorted(Comparator.comparingLong(ReactionSummaryResponse::count).reversed()
                        .thenComparing(ReactionSummaryResponse::emoji))
                .toList();
    }
}
