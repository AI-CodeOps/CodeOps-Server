package com.codeops.relay.controller;

import com.codeops.config.AppConstants;
import com.codeops.relay.dto.request.AddReactionRequest;
import com.codeops.relay.dto.response.ReactionResponse;
import com.codeops.relay.dto.response.ReactionSummaryResponse;
import com.codeops.relay.service.ReactionService;
import com.codeops.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for message reactions in the Relay module.
 *
 * <p>Provides endpoints for toggling reactions on messages and
 * retrieving reaction summaries.</p>
 *
 * <p>All endpoints require authentication.</p>
 */
@RestController
@RequestMapping(AppConstants.RELAY_API_PREFIX + "/reactions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class ReactionController {

    private final ReactionService reactionService;

    /**
     * Toggles a reaction on a message (adds if absent, removes if present).
     *
     * @param messageId the message ID
     * @param request   the reaction request containing the emoji
     * @return the reaction details
     */
    @PostMapping("/messages/{messageId}/toggle")
    public ReactionResponse toggleReaction(
            @PathVariable UUID messageId,
            @RequestBody @Valid AddReactionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return reactionService.toggleReaction(messageId, request, userId);
    }

    /**
     * Retrieves reaction summaries for a message.
     *
     * @param messageId the message ID
     * @return the list of reaction summaries with counts and user IDs
     */
    @GetMapping("/messages/{messageId}")
    public List<ReactionSummaryResponse> getReactionsForMessage(@PathVariable UUID messageId) {
        return reactionService.getReactionsForMessage(messageId);
    }

    /**
     * Retrieves reaction summaries for a message, including whether
     * the current user has reacted.
     *
     * @param messageId the message ID
     * @return the list of reaction summaries with current-user flag
     */
    @GetMapping("/messages/{messageId}/mine")
    public List<ReactionSummaryResponse> getReactionsForMessageWithUser(
            @PathVariable UUID messageId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return reactionService.getReactionsForMessageWithUser(messageId, userId);
    }
}
