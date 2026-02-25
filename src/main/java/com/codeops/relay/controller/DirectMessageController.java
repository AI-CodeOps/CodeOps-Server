package com.codeops.relay.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.response.PageResponse;
import com.codeops.relay.dto.request.CreateDirectConversationRequest;
import com.codeops.relay.dto.request.SendDirectMessageRequest;
import com.codeops.relay.dto.request.UpdateDirectMessageRequest;
import com.codeops.relay.dto.response.DirectConversationResponse;
import com.codeops.relay.dto.response.DirectConversationSummaryResponse;
import com.codeops.relay.dto.response.DirectMessageResponse;
import com.codeops.relay.service.DirectMessageService;
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
 * REST controller for direct message conversations in the Relay module.
 *
 * <p>Provides endpoints for creating and managing direct conversations,
 * sending and managing messages within conversations, and tracking
 * read status and unread counts.</p>
 *
 * <p>All endpoints require authentication. Conversation participation
 * is verified in the service layer.</p>
 */
@RestController
@RequestMapping(AppConstants.RELAY_API_PREFIX + "/dm")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    /**
     * Creates or retrieves an existing direct conversation.
     *
     * @param request the conversation creation request with participant IDs
     * @param teamId  the team ID
     * @return the conversation details
     */
    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public DirectConversationResponse getOrCreateConversation(
            @RequestBody @Valid CreateDirectConversationRequest request,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return directMessageService.getOrCreateConversation(request, teamId, userId);
    }

    /**
     * Retrieves a conversation by ID.
     *
     * @param conversationId the conversation ID
     * @return the conversation details
     */
    @GetMapping("/conversations/{conversationId}")
    public DirectConversationResponse getConversation(@PathVariable UUID conversationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return directMessageService.getConversation(conversationId, userId);
    }

    /**
     * Lists all conversations for the current user in a team.
     *
     * @param teamId the team ID
     * @return the list of conversation summaries
     */
    @GetMapping("/conversations")
    public List<DirectConversationSummaryResponse> getConversations(@RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return directMessageService.getConversations(teamId, userId);
    }

    /**
     * Deletes a conversation.
     *
     * @param conversationId the conversation ID
     */
    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable UUID conversationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        directMessageService.deleteConversation(conversationId, userId);
    }

    /**
     * Sends a direct message in a conversation.
     *
     * @param conversationId the conversation ID
     * @param request        the message content
     * @return the sent message
     */
    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public DirectMessageResponse sendDirectMessage(
            @PathVariable UUID conversationId,
            @RequestBody @Valid SendDirectMessageRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return directMessageService.sendDirectMessage(conversationId, request, userId);
    }

    /**
     * Lists messages in a conversation with pagination.
     *
     * @param conversationId the conversation ID
     * @param page           the page number (zero-based)
     * @param size           the page size
     * @return a page of direct messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public PageResponse<DirectMessageResponse> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return directMessageService.getMessages(conversationId, userId, page, size);
    }

    /**
     * Edits a direct message.
     *
     * @param messageId the message ID
     * @param request   the updated content
     * @return the edited message
     */
    @PutMapping("/messages/{messageId}")
    public DirectMessageResponse editDirectMessage(
            @PathVariable UUID messageId,
            @RequestBody @Valid UpdateDirectMessageRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return directMessageService.editDirectMessage(messageId, request, userId);
    }

    /**
     * Deletes a direct message.
     *
     * @param messageId the message ID
     */
    @DeleteMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDirectMessage(@PathVariable UUID messageId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        directMessageService.deleteDirectMessage(messageId, userId);
    }

    /**
     * Marks all messages in a conversation as read.
     *
     * @param conversationId the conversation ID
     */
    @PostMapping("/conversations/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markConversationRead(@PathVariable UUID conversationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        directMessageService.markConversationRead(conversationId, userId);
    }

    /**
     * Retrieves the unread message count for a conversation.
     *
     * @param conversationId the conversation ID
     * @return the unread count
     */
    @GetMapping("/conversations/{conversationId}/unread")
    public Map<String, Long> getUnreadCount(@PathVariable UUID conversationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        long count = directMessageService.getUnreadCount(conversationId, userId);
        return Map.of("unreadCount", count);
    }
}
