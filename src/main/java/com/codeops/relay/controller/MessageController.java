package com.codeops.relay.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.response.PageResponse;
import com.codeops.relay.dto.request.MarkReadRequest;
import com.codeops.relay.dto.request.SendMessageRequest;
import com.codeops.relay.dto.request.UpdateMessageRequest;
import com.codeops.relay.dto.response.*;
import com.codeops.relay.service.MessageService;
import com.codeops.relay.service.ThreadService;
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
 * REST controller for message operations in the Relay module.
 *
 * <p>Provides endpoints for sending, reading, editing, and deleting messages
 * within channels. Also supports thread management, message search (within
 * a channel and across all channels), read receipts, and unread counts.</p>
 *
 * <p>All endpoints require authentication and authorization is verified
 * in the service layer.</p>
 */
@RestController
@RequestMapping(AppConstants.RELAY_API_PREFIX)
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class MessageController {

    private final MessageService messageService;
    private final ThreadService threadService;

    /**
     * Sends a message to a channel.
     *
     * @param channelId the channel ID
     * @param request   the message content and metadata
     * @param teamId    the team ID
     * @return the created message
     */
    @PostMapping("/channels/{channelId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @PathVariable UUID channelId,
            @RequestBody @Valid SendMessageRequest request,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return messageService.sendMessage(channelId, request, teamId, userId);
    }

    /**
     * Lists messages in a channel with pagination.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @param page      the page number (zero-based)
     * @param size      the page size
     * @return a page of messages
     */
    @GetMapping("/channels/{channelId}/messages")
    public PageResponse<MessageResponse> getChannelMessages(
            @PathVariable UUID channelId,
            @RequestParam UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return messageService.getChannelMessages(channelId, teamId, userId, page, size);
    }

    /**
     * Retrieves a single message by ID.
     *
     * @param messageId the message ID
     * @return the message
     */
    @GetMapping("/channels/{channelId}/messages/{messageId}")
    public MessageResponse getMessage(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId) {
        return messageService.getMessage(messageId);
    }

    /**
     * Edits an existing message.
     *
     * @param messageId the message ID
     * @param request   the updated content
     * @return the edited message
     */
    @PutMapping("/channels/{channelId}/messages/{messageId}")
    public MessageResponse editMessage(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId,
            @RequestBody @Valid UpdateMessageRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return messageService.editMessage(messageId, request, userId);
    }

    /**
     * Deletes a message.
     *
     * @param channelId the channel ID
     * @param messageId the message ID
     * @param teamId    the team ID
     */
    @DeleteMapping("/channels/{channelId}/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId,
            @RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        messageService.deleteMessage(messageId, userId, teamId);
    }

    /**
     * Retrieves thread replies for a message.
     *
     * @param parentMessageId the parent message ID
     * @return the list of reply messages
     */
    @GetMapping("/channels/{channelId}/messages/{parentMessageId}/thread")
    public List<MessageResponse> getThreadReplies(
            @PathVariable UUID channelId,
            @PathVariable UUID parentMessageId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return messageService.getThreadReplies(parentMessageId, userId);
    }

    /**
     * Searches messages within a channel.
     *
     * @param channelId the channel ID
     * @param query     the search query
     * @param teamId    the team ID
     * @param page      the page number (zero-based)
     * @param size      the page size
     * @return a page of matching messages
     */
    @GetMapping("/channels/{channelId}/messages/search")
    public PageResponse<MessageResponse> searchMessages(
            @PathVariable UUID channelId,
            @RequestParam String query,
            @RequestParam UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return messageService.searchMessages(channelId, query, teamId, userId, page, size);
    }

    /**
     * Searches messages across all channels the user has access to.
     *
     * @param query  the search query
     * @param teamId the team ID
     * @param page   the page number (zero-based)
     * @param size   the page size
     * @return a page of search results with channel context
     */
    @GetMapping("/messages/search-all")
    public PageResponse<ChannelSearchResultResponse> searchMessagesAcrossChannels(
            @RequestParam String query,
            @RequestParam UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return messageService.searchMessagesAcrossChannels(query, teamId, userId, page, size);
    }

    /**
     * Marks messages in a channel as read up to a given message ID.
     *
     * @param channelId the channel ID
     * @param request   the mark-read request containing the last read message ID
     * @return the read receipt
     */
    @PostMapping("/channels/{channelId}/messages/read")
    public ReadReceiptResponse markRead(
            @PathVariable UUID channelId,
            @RequestBody @Valid MarkReadRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return messageService.markRead(channelId, request, userId);
    }

    /**
     * Retrieves unread message counts for all channels the user belongs to.
     *
     * @param teamId the team ID
     * @return the list of unread counts per channel
     */
    @GetMapping("/messages/unread-counts")
    public List<UnreadCountResponse> getUnreadCounts(@RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return messageService.getUnreadCounts(teamId, userId);
    }

    /**
     * Lists active threads in a channel.
     *
     * @param channelId the channel ID
     * @return the list of active threads
     */
    @GetMapping("/channels/{channelId}/threads/active")
    public List<MessageThreadResponse> getActiveThreads(@PathVariable UUID channelId) {
        return threadService.getActiveThreads(channelId);
    }
}
