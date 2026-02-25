package com.codeops.relay.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.response.PageResponse;
import com.codeops.exception.NotFoundException;
import com.codeops.relay.dto.mapper.PlatformEventMapper;
import com.codeops.relay.dto.response.PlatformEventResponse;
import com.codeops.relay.entity.Channel;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.PlatformEvent;
import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.entity.enums.PlatformEventType;
import com.codeops.relay.repository.ChannelRepository;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.PlatformEventRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for publishing and managing cross-module platform events in Relay.
 *
 * <p>When significant events occur in other CodeOps modules (Registry, Logger,
 * Courier), they are recorded as {@link PlatformEvent} entities and optionally
 * delivered as system messages to relevant channels. Supports event publishing,
 * paginated retrieval, filtering, delivery retry, and event formatting.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlatformEventService {

    private final PlatformEventRepository platformEventRepository;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PlatformEventMapper platformEventMapper;

    /**
     * Publishes a platform event and attempts to deliver it as a channel message.
     *
     * <p>Creates a {@link PlatformEvent}, resolves a target channel, and if found,
     * posts a {@link MessageType#PLATFORM_EVENT} message to that channel. If no
     * channel is found, the event stays undelivered for later retry.</p>
     *
     * @param eventType        the type of platform event
     * @param teamId           the team ID
     * @param sourceEntityId   the entity that triggered the event (nullable)
     * @param sourceModule     the originating module (e.g., "registry", "logger")
     * @param title            formatted event title
     * @param detail           event detail/body (nullable)
     * @param senderId         the user who triggered the event
     * @param targetChannelId  explicit target channel (nullable — auto-resolved if null)
     * @return the created platform event response
     */
    @Transactional
    public PlatformEventResponse publishEvent(PlatformEventType eventType, UUID teamId,
                                               UUID sourceEntityId, String sourceModule,
                                               String title, String detail, UUID senderId,
                                               UUID targetChannelId) {
        PlatformEvent event = PlatformEvent.builder()
                .eventType(eventType)
                .teamId(teamId)
                .sourceEntityId(sourceEntityId)
                .sourceModule(sourceModule)
                .title(title)
                .detail(detail)
                .isDelivered(false)
                .build();
        event = platformEventRepository.save(event);

        attemptDelivery(event, senderId, targetChannelId);

        log.info("Platform event published: {} for {} {}", eventType, sourceModule, sourceEntityId);
        return buildResponse(event);
    }

    /**
     * Convenience method for simple events without a specific source entity.
     *
     * @param eventType   the type of platform event
     * @param teamId      the team ID
     * @param title       formatted event title
     * @param senderId    the user who triggered the event
     * @return the created platform event response
     */
    @Transactional
    public PlatformEventResponse publishEventSimple(PlatformEventType eventType, UUID teamId,
                                                     String title, UUID senderId) {
        return publishEvent(eventType, teamId, null, "system", title, null, senderId, null);
    }

    /**
     * Retrieves a platform event by ID.
     *
     * @param eventId the event ID
     * @return the platform event response
     * @throws NotFoundException if the event does not exist
     */
    @Transactional(readOnly = true)
    public PlatformEventResponse getEvent(UUID eventId) {
        PlatformEvent event = platformEventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Platform event", eventId));
        return buildResponse(event);
    }

    /**
     * Retrieves paginated platform events for a team, ordered by most recent first.
     *
     * @param teamId the team ID
     * @param page   zero-based page number
     * @param size   page size
     * @return paginated list of platform event responses
     */
    @Transactional(readOnly = true)
    public PageResponse<PlatformEventResponse> getEventsForTeam(UUID teamId, int page, int size) {
        Page<PlatformEvent> events = platformEventRepository.findByTeamIdOrderByCreatedAtDesc(
                teamId, PageRequest.of(page, size));
        List<PlatformEventResponse> content = events.getContent().stream()
                .map(this::buildResponse)
                .toList();
        return new PageResponse<>(content, events.getNumber(), events.getSize(),
                events.getTotalElements(), events.getTotalPages(), events.isLast());
    }

    /**
     * Retrieves paginated platform events for a team filtered by event type.
     *
     * @param teamId    the team ID
     * @param eventType the event type filter
     * @param page      zero-based page number
     * @param size      page size
     * @return paginated list of platform event responses
     */
    @Transactional(readOnly = true)
    public PageResponse<PlatformEventResponse> getEventsForTeamByType(UUID teamId,
                                                                       PlatformEventType eventType,
                                                                       int page, int size) {
        Page<PlatformEvent> events = platformEventRepository.findByTeamIdAndEventTypeOrderByCreatedAtDesc(
                teamId, eventType, PageRequest.of(page, size));
        List<PlatformEventResponse> content = events.getContent().stream()
                .map(this::buildResponse)
                .toList();
        return new PageResponse<>(content, events.getNumber(), events.getSize(),
                events.getTotalElements(), events.getTotalPages(), events.isLast());
    }

    /**
     * Retrieves all events for a specific source entity, ordered by most recent first.
     *
     * @param sourceEntityId the source entity ID
     * @return list of platform event responses
     */
    @Transactional(readOnly = true)
    public List<PlatformEventResponse> getEventsForEntity(UUID sourceEntityId) {
        return platformEventRepository.findBySourceEntityIdOrderByCreatedAtDesc(sourceEntityId).stream()
                .map(this::buildResponse)
                .toList();
    }

    /**
     * Retrieves all undelivered events for a team, ordered by creation time.
     *
     * @param teamId the team ID
     * @return list of undelivered platform event responses
     */
    @Transactional(readOnly = true)
    public List<PlatformEventResponse> getUndeliveredEvents(UUID teamId) {
        return platformEventRepository.findByTeamIdAndIsDeliveredFalseOrderByCreatedAtAsc(teamId).stream()
                .map(this::buildResponse)
                .toList();
    }

    /**
     * Retries delivery of a previously undelivered event.
     *
     * <p>If the event is already delivered, returns it as-is (idempotent).</p>
     *
     * @param eventId  the event ID
     * @param senderId the user to attribute the message to
     * @return the updated platform event response
     * @throws NotFoundException if the event does not exist
     */
    @Transactional
    public PlatformEventResponse retryDelivery(UUID eventId, UUID senderId) {
        PlatformEvent event = platformEventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Platform event", eventId));

        if (event.isDelivered()) {
            return buildResponse(event);
        }

        attemptDelivery(event, senderId, null);
        return buildResponse(event);
    }

    /**
     * Retries delivery of all undelivered events for a team.
     *
     * @param teamId   the team ID
     * @param senderId the user to attribute messages to
     * @return the count of successfully delivered events
     */
    @Transactional
    public int retryAllUndelivered(UUID teamId, UUID senderId) {
        List<PlatformEvent> undelivered = platformEventRepository
                .findByTeamIdAndIsDeliveredFalseOrderByCreatedAtAsc(teamId);
        int count = 0;
        for (PlatformEvent event : undelivered) {
            attemptDelivery(event, senderId, null);
            if (event.isDelivered()) {
                count++;
            }
        }
        log.info("Retried {} undelivered events, {} delivered for team {}", undelivered.size(), count, teamId);
        return count;
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Attempts to deliver a platform event as a channel message.
     *
     * <p>Resolves the target channel, creates a system message, and updates
     * the event's delivery status. If no channel is found, the event stays
     * undelivered.</p>
     *
     * @param event           the platform event
     * @param senderId        the user to attribute the message to
     * @param targetChannelId explicit target channel (nullable)
     */
    private void attemptDelivery(PlatformEvent event, UUID senderId, UUID targetChannelId) {
        Optional<Channel> channel = resolveTargetChannel(
                event.getEventType(), event.getSourceEntityId(), event.getTeamId(), targetChannelId);

        if (channel.isPresent()) {
            Channel target = channel.get();
            String content = formatEventMessage(event);

            Message message = Message.builder()
                    .channelId(target.getId())
                    .senderId(senderId)
                    .content(content)
                    .messageType(MessageType.PLATFORM_EVENT)
                    .platformEventId(event.getId())
                    .build();
            message = messageRepository.save(message);

            event.setTargetChannelId(target.getId());
            event.setTargetChannelSlug(target.getSlug());
            event.setPostedMessageId(message.getId());
            event.setDelivered(true);
            event.setDeliveredAt(Instant.now());
            platformEventRepository.save(event);
        } else {
            log.warn("No delivery channel for event {} in team {}", event.getEventType(), event.getTeamId());
        }
    }

    /**
     * Resolves the target channel for a platform event.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Explicit targetChannelId if provided</li>
     *   <li>SERVICE channel matching sourceEntityId (for service-related events)</li>
     *   <li>PROJECT channel matching sourceEntityId (for project-related events)</li>
     *   <li>Team's #general channel as fallback</li>
     * </ol></p>
     *
     * @param eventType       the platform event type
     * @param sourceEntityId  the source entity ID
     * @param teamId          the team ID
     * @param targetChannelId explicit target channel (nullable)
     * @return the resolved channel, or empty if none found
     */
    Optional<Channel> resolveTargetChannel(PlatformEventType eventType, UUID sourceEntityId,
                                            UUID teamId, UUID targetChannelId) {
        if (targetChannelId != null) {
            return channelRepository.findById(targetChannelId);
        }

        if (sourceEntityId != null && isServiceRelatedEvent(eventType)) {
            Optional<Channel> serviceChannel = channelRepository.findByTeamIdAndServiceId(
                    teamId, sourceEntityId);
            if (serviceChannel.isPresent()) {
                return serviceChannel;
            }
        }

        if (sourceEntityId != null && isProjectRelatedEvent(eventType)) {
            Optional<Channel> projectChannel = channelRepository.findByTeamIdAndProjectId(
                    teamId, sourceEntityId);
            if (projectChannel.isPresent()) {
                return projectChannel;
            }
        }

        return channelRepository.findByTeamIdAndSlug(teamId, AppConstants.RELAY_GENERAL_CHANNEL_SLUG);
    }

    /**
     * Formats a platform event into a human-readable message string.
     *
     * @param event the platform event
     * @return the formatted message
     */
    String formatEventMessage(PlatformEvent event) {
        String detail = event.getDetail() != null ? event.getDetail() : "";

        return switch (event.getEventType()) {
            case SERVICE_REGISTERED -> "\uD83D\uDD27 New service registered: " + event.getTitle();
            case ALERT_FIRED -> "\uD83D\uDEA8 Alert fired: " + event.getTitle()
                    + (detail.isEmpty() ? "" : " — " + detail);
            case SECRET_ROTATED -> "\uD83D\uDD11 Secret rotated: " + event.getTitle();
            case DEPLOYMENT_COMPLETED -> "\u2705 Deployment completed: " + event.getTitle();
            case AUDIT_COMPLETED -> "\uD83D\uDCCB Audit completed: " + event.getTitle();
            case CONTAINER_CRASHED -> "\uD83D\uDCA5 Container crashed: " + event.getTitle()
                    + (detail.isEmpty() ? "" : " — " + detail);
            case SESSION_COMPLETED -> "\uD83C\uDFC1 Session completed: " + event.getTitle();
            case BUILD_COMPLETED -> "\uD83D\uDCE6 Build completed: " + event.getTitle();
            case FINDING_CRITICAL -> "\u26A0\uFE0F Critical finding: " + event.getTitle()
                    + (detail.isEmpty() ? "" : " — " + detail);
            case MERGE_REQUEST_CREATED -> "\uD83D\uDD00 Merge request created: " + event.getTitle();
        };
    }

    /**
     * Determines if an event type is related to a service entity.
     *
     * @param eventType the platform event type
     * @return true if service-related
     */
    private boolean isServiceRelatedEvent(PlatformEventType eventType) {
        return eventType == PlatformEventType.SERVICE_REGISTERED
                || eventType == PlatformEventType.CONTAINER_CRASHED
                || eventType == PlatformEventType.DEPLOYMENT_COMPLETED;
    }

    /**
     * Determines if an event type is related to a project entity.
     *
     * @param eventType the platform event type
     * @return true if project-related
     */
    private boolean isProjectRelatedEvent(PlatformEventType eventType) {
        return eventType == PlatformEventType.AUDIT_COMPLETED
                || eventType == PlatformEventType.FINDING_CRITICAL
                || eventType == PlatformEventType.BUILD_COMPLETED;
    }

    /**
     * Builds a PlatformEventResponse from a PlatformEvent entity.
     *
     * @param event the platform event entity
     * @return the response DTO
     */
    private PlatformEventResponse buildResponse(PlatformEvent event) {
        return new PlatformEventResponse(
                event.getId(),
                event.getEventType(),
                event.getTeamId(),
                event.getSourceModule(),
                event.getSourceEntityId(),
                event.getTitle(),
                event.getDetail(),
                event.getTargetChannelId(),
                event.getTargetChannelSlug(),
                event.isDelivered(),
                event.getDeliveredAt(),
                event.getCreatedAt());
    }
}
