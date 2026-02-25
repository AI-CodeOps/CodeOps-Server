package com.codeops.relay.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.response.PageResponse;
import com.codeops.relay.dto.response.PlatformEventResponse;
import com.codeops.relay.entity.enums.PlatformEventType;
import com.codeops.relay.service.PlatformEventService;
import com.codeops.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for platform event management in the Relay module.
 *
 * <p>Provides endpoints for querying platform events by team, type,
 * and entity, as well as retrying undelivered event notifications.</p>
 *
 * <p>All endpoints require authentication.</p>
 */
@RestController
@RequestMapping(AppConstants.RELAY_API_PREFIX + "/events")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class PlatformEventController {

    private final PlatformEventService platformEventService;

    /**
     * Lists platform events for a team with pagination.
     *
     * @param teamId the team ID
     * @param page   the page number (zero-based)
     * @param size   the page size
     * @return a page of platform events
     */
    @GetMapping
    public PageResponse<PlatformEventResponse> getEventsForTeam(
            @RequestParam UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return platformEventService.getEventsForTeam(teamId, page, size);
    }

    /**
     * Retrieves a single platform event by ID.
     *
     * @param eventId the event ID
     * @return the platform event
     */
    @GetMapping("/{eventId}")
    public PlatformEventResponse getEvent(@PathVariable UUID eventId) {
        return platformEventService.getEvent(eventId);
    }

    /**
     * Lists platform events for a team filtered by event type.
     *
     * @param teamId    the team ID
     * @param eventType the event type filter
     * @param page      the page number (zero-based)
     * @param size      the page size
     * @return a page of platform events of the specified type
     */
    @GetMapping("/type/{eventType}")
    public PageResponse<PlatformEventResponse> getEventsForTeamByType(
            @RequestParam UUID teamId,
            @PathVariable PlatformEventType eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return platformEventService.getEventsForTeamByType(teamId, eventType, page, size);
    }

    /**
     * Lists platform events for a specific source entity.
     *
     * @param sourceEntityId the source entity ID
     * @return the list of platform events for the entity
     */
    @GetMapping("/entity/{sourceEntityId}")
    public List<PlatformEventResponse> getEventsForEntity(@PathVariable UUID sourceEntityId) {
        return platformEventService.getEventsForEntity(sourceEntityId);
    }

    /**
     * Lists undelivered platform events for a team.
     *
     * @param teamId the team ID
     * @return the list of undelivered events
     */
    @GetMapping("/undelivered")
    public List<PlatformEventResponse> getUndeliveredEvents(@RequestParam UUID teamId) {
        return platformEventService.getUndeliveredEvents(teamId);
    }

    /**
     * Retries delivery of a single platform event.
     *
     * @param eventId the event ID
     * @return the retried event
     */
    @PostMapping("/{eventId}/retry")
    public PlatformEventResponse retryDelivery(@PathVariable UUID eventId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return platformEventService.retryDelivery(eventId, userId);
    }

    /**
     * Retries delivery of all undelivered events for a team.
     *
     * @param teamId the team ID
     * @return the count of retried events
     */
    @PostMapping("/retry-all")
    public Map<String, Integer> retryAllUndelivered(@RequestParam UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        int count = platformEventService.retryAllUndelivered(teamId, userId);
        return Map.of("retriedCount", count);
    }
}
