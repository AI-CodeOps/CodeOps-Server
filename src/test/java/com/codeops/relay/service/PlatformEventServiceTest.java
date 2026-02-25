package com.codeops.relay.service;

import com.codeops.dto.response.PageResponse;
import com.codeops.exception.NotFoundException;
import com.codeops.relay.dto.mapper.PlatformEventMapper;
import com.codeops.relay.dto.response.PlatformEventResponse;
import com.codeops.relay.entity.Channel;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.PlatformEvent;
import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.PlatformEventType;
import com.codeops.relay.repository.ChannelRepository;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.PlatformEventRepository;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PlatformEventService}.
 *
 * <p>Covers event publishing, retrieval, delivery retry, message formatting,
 * and channel resolution.</p>
 */
@ExtendWith(MockitoExtension.class)
class PlatformEventServiceTest {

    @Mock private PlatformEventRepository platformEventRepository;
    @Mock private ChannelRepository channelRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlatformEventMapper platformEventMapper;

    @InjectMocks private PlatformEventService platformEventService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private Channel generalChannel;
    private Channel serviceChannel;
    private Channel projectChannel;

    @BeforeEach
    void setUp() {
        generalChannel = buildChannel(CHANNEL_ID, "general", ChannelType.PUBLIC);
        serviceChannel = buildChannel(UUID.randomUUID(), "svc-api", ChannelType.SERVICE);
        serviceChannel.setServiceId(SERVICE_ID);
        projectChannel = buildChannel(UUID.randomUUID(), "proj-main", ChannelType.PROJECT);
        projectChannel.setProjectId(PROJECT_ID);
    }

    // ── Publishing ───────────────────────────────────────────────────────

    @Nested
    class PublishingTests {

        @Test
        void publishEvent_success_withTargetChannel() {
            UUID targetId = UUID.randomUUID();
            Channel target = buildChannel(targetId, "alerts", ChannelType.PRIVATE);

            when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> {
                PlatformEvent e = inv.getArgument(0);
                if (e.getId() == null) e.setId(EVENT_ID);
                e.setCreatedAt(NOW);
                return e;
            });
            when(channelRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                return m;
            });

            PlatformEventResponse result = platformEventService.publishEvent(
                    PlatformEventType.ALERT_FIRED, TEAM_ID, UUID.randomUUID(), "logger",
                    "CPU Alert", "CPU > 90%", USER_ID, targetId);

            assertThat(result).isNotNull();
            assertThat(result.isDelivered()).isTrue();
            assertThat(result.targetChannelId()).isEqualTo(targetId);
        }

        @Test
        void publishEvent_success_deliverToServiceChannel() {
            when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> {
                PlatformEvent e = inv.getArgument(0);
                if (e.getId() == null) e.setId(EVENT_ID);
                e.setCreatedAt(NOW);
                return e;
            });
            when(channelRepository.findByTeamIdAndServiceId(TEAM_ID, SERVICE_ID))
                    .thenReturn(Optional.of(serviceChannel));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                return m;
            });

            PlatformEventResponse result = platformEventService.publishEvent(
                    PlatformEventType.SERVICE_REGISTERED, TEAM_ID, SERVICE_ID, "registry",
                    "API Gateway", null, USER_ID, null);

            assertThat(result.isDelivered()).isTrue();
            assertThat(result.targetChannelSlug()).isEqualTo("svc-api");
        }

        @Test
        void publishEvent_success_fallbackToGeneral() {
            when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> {
                PlatformEvent e = inv.getArgument(0);
                if (e.getId() == null) e.setId(EVENT_ID);
                e.setCreatedAt(NOW);
                return e;
            });
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.of(generalChannel));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                return m;
            });

            PlatformEventResponse result = platformEventService.publishEvent(
                    PlatformEventType.SECRET_ROTATED, TEAM_ID, null, "core",
                    "DB Password", null, USER_ID, null);

            assertThat(result.isDelivered()).isTrue();
            assertThat(result.targetChannelSlug()).isEqualTo("general");
        }

        @Test
        void publishEvent_noChannelFound_staysUndelivered() {
            when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> {
                PlatformEvent e = inv.getArgument(0);
                if (e.getId() == null) e.setId(EVENT_ID);
                e.setCreatedAt(NOW);
                return e;
            });
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.empty());

            PlatformEventResponse result = platformEventService.publishEvent(
                    PlatformEventType.SESSION_COMPLETED, TEAM_ID, null, "core",
                    "Session done", null, USER_ID, null);

            assertThat(result.isDelivered()).isFalse();
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        void publishEvent_createsSystemMessage() {
            when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> {
                PlatformEvent e = inv.getArgument(0);
                if (e.getId() == null) e.setId(EVENT_ID);
                e.setCreatedAt(NOW);
                return e;
            });
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.of(generalChannel));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                return m;
            });

            platformEventService.publishEvent(
                    PlatformEventType.ALERT_FIRED, TEAM_ID, null, "logger",
                    "Disk Full", "Usage > 95%", USER_ID, null);

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            Message saved = captor.getValue();
            assertThat(saved.getMessageType()).isEqualTo(com.codeops.relay.entity.enums.MessageType.PLATFORM_EVENT);
            assertThat(saved.getChannelId()).isEqualTo(CHANNEL_ID);
            assertThat(saved.getSenderId()).isEqualTo(USER_ID);
            assertThat(saved.getPlatformEventId()).isEqualTo(EVENT_ID);
        }

        @Test
        void publishEvent_setsDeliveredTrue() {
            when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> {
                PlatformEvent e = inv.getArgument(0);
                if (e.getId() == null) e.setId(EVENT_ID);
                e.setCreatedAt(NOW);
                return e;
            });
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.of(generalChannel));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                return m;
            });

            platformEventService.publishEvent(
                    PlatformEventType.DEPLOYMENT_COMPLETED, TEAM_ID, null, "registry",
                    "v1.2.3", null, USER_ID, null);

            // save called twice: initial save + delivery update
            verify(platformEventRepository, times(2)).save(any(PlatformEvent.class));
        }

        @Test
        void publishEventSimple_success() {
            when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> {
                PlatformEvent e = inv.getArgument(0);
                if (e.getId() == null) e.setId(EVENT_ID);
                e.setCreatedAt(NOW);
                return e;
            });
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.of(generalChannel));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                return m;
            });

            PlatformEventResponse result = platformEventService.publishEventSimple(
                    PlatformEventType.SESSION_COMPLETED, TEAM_ID, "QA Session done", USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.sourceModule()).isEqualTo("system");
        }
    }

    // ── Retrieval ────────────────────────────────────────────────────────

    @Nested
    class RetrievalTests {

        @Test
        void getEvent_success() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.ALERT_FIRED, true);

            when(platformEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            PlatformEventResponse result = platformEventService.getEvent(EVENT_ID);

            assertThat(result.id()).isEqualTo(EVENT_ID);
            assertThat(result.eventType()).isEqualTo(PlatformEventType.ALERT_FIRED);
        }

        @Test
        void getEvent_notFound_throws() {
            when(platformEventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> platformEventService.getEvent(EVENT_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void getEventsForTeam_paginatedDesc() {
            PlatformEvent e1 = buildEvent(UUID.randomUUID(), PlatformEventType.ALERT_FIRED, true);
            PlatformEvent e2 = buildEvent(UUID.randomUUID(), PlatformEventType.AUDIT_COMPLETED, false);
            Page<PlatformEvent> page = new PageImpl<>(List.of(e1, e2), PageRequest.of(0, 10), 2);

            when(platformEventRepository.findByTeamIdOrderByCreatedAtDesc(
                    eq(TEAM_ID), any(Pageable.class))).thenReturn(page);

            PageResponse<PlatformEventResponse> result = platformEventService.getEventsForTeam(
                    TEAM_ID, 0, 10);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
        }

        @Test
        void getEventsForTeamByType_filtersCorrectly() {
            PlatformEvent e = buildEvent(UUID.randomUUID(), PlatformEventType.ALERT_FIRED, true);
            Page<PlatformEvent> page = new PageImpl<>(List.of(e), PageRequest.of(0, 10), 1);

            when(platformEventRepository.findByTeamIdAndEventTypeOrderByCreatedAtDesc(
                    eq(TEAM_ID), eq(PlatformEventType.ALERT_FIRED), any(Pageable.class))).thenReturn(page);

            PageResponse<PlatformEventResponse> result = platformEventService.getEventsForTeamByType(
                    TEAM_ID, PlatformEventType.ALERT_FIRED, 0, 10);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).eventType()).isEqualTo(PlatformEventType.ALERT_FIRED);
        }

        @Test
        void getEventsForEntity_returnsHistory() {
            UUID entityId = UUID.randomUUID();
            PlatformEvent e1 = buildEvent(UUID.randomUUID(), PlatformEventType.DEPLOYMENT_COMPLETED, true);
            e1.setSourceEntityId(entityId);
            PlatformEvent e2 = buildEvent(UUID.randomUUID(), PlatformEventType.BUILD_COMPLETED, true);
            e2.setSourceEntityId(entityId);

            when(platformEventRepository.findBySourceEntityIdOrderByCreatedAtDesc(entityId))
                    .thenReturn(List.of(e1, e2));

            List<PlatformEventResponse> result = platformEventService.getEventsForEntity(entityId);

            assertThat(result).hasSize(2);
        }

        @Test
        void getUndeliveredEvents_onlyUndelivered() {
            PlatformEvent e = buildEvent(UUID.randomUUID(), PlatformEventType.ALERT_FIRED, false);

            when(platformEventRepository.findByTeamIdAndIsDeliveredFalseOrderByCreatedAtAsc(TEAM_ID))
                    .thenReturn(List.of(e));

            List<PlatformEventResponse> result = platformEventService.getUndeliveredEvents(TEAM_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isDelivered()).isFalse();
        }
    }

    // ── Retry ────────────────────────────────────────────────────────────

    @Nested
    class RetryTests {

        @Test
        void retryDelivery_deliversPrevUndelivered() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.ALERT_FIRED, false);

            when(platformEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.of(generalChannel));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                return m;
            });
            when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            PlatformEventResponse result = platformEventService.retryDelivery(EVENT_ID, USER_ID);

            assertThat(result.isDelivered()).isTrue();
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        void retryDelivery_alreadyDelivered_noOp() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.ALERT_FIRED, true);

            when(platformEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            PlatformEventResponse result = platformEventService.retryDelivery(EVENT_ID, USER_ID);

            assertThat(result.isDelivered()).isTrue();
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        void retryDelivery_stillNoChannel_staysUndelivered() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.ALERT_FIRED, false);

            when(platformEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.empty());

            PlatformEventResponse result = platformEventService.retryDelivery(EVENT_ID, USER_ID);

            assertThat(result.isDelivered()).isFalse();
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        void retryAllUndelivered_partialSuccess() {
            PlatformEvent e1 = buildEvent(UUID.randomUUID(), PlatformEventType.ALERT_FIRED, false);
            PlatformEvent e2 = buildEvent(UUID.randomUUID(), PlatformEventType.SECRET_ROTATED, false);

            when(platformEventRepository.findByTeamIdAndIsDeliveredFalseOrderByCreatedAtAsc(TEAM_ID))
                    .thenReturn(List.of(e1, e2));
            // First event finds general channel, second does not
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.of(generalChannel))
                    .thenReturn(Optional.empty());
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(UUID.randomUUID());
                return m;
            });
            when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            int count = platformEventService.retryAllUndelivered(TEAM_ID, USER_ID);

            assertThat(count).isEqualTo(1);
        }
    }

    // ── Formatting ───────────────────────────────────────────────────────

    @Nested
    class FormattingTests {

        @Test
        void formatEventMessage_serviceRegistered() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.SERVICE_REGISTERED, false);
            event.setTitle("API Gateway");

            String result = platformEventService.formatEventMessage(event);

            assertThat(result).contains("New service registered").contains("API Gateway");
        }

        @Test
        void formatEventMessage_alertFired() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.ALERT_FIRED, false);
            event.setTitle("CPU Alert");
            event.setDetail("CPU > 90%");

            String result = platformEventService.formatEventMessage(event);

            assertThat(result).contains("Alert fired").contains("CPU Alert").contains("CPU > 90%");
        }

        @Test
        void formatEventMessage_secretRotated() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.SECRET_ROTATED, false);
            event.setTitle("DB Password");

            String result = platformEventService.formatEventMessage(event);

            assertThat(result).contains("Secret rotated").contains("DB Password");
        }

        @Test
        void formatEventMessage_deploymentCompleted() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.DEPLOYMENT_COMPLETED, false);
            event.setTitle("api-gateway v1.2.3");

            String result = platformEventService.formatEventMessage(event);

            assertThat(result).contains("Deployment completed").contains("api-gateway v1.2.3");
        }

        @Test
        void formatEventMessage_auditCompleted() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.AUDIT_COMPLETED, false);
            event.setTitle("Project Alpha");

            String result = platformEventService.formatEventMessage(event);

            assertThat(result).contains("Audit completed").contains("Project Alpha");
        }

        @Test
        void formatEventMessage_containerCrashed() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.CONTAINER_CRASHED, false);
            event.setTitle("web-server");
            event.setDetail("OOMKilled");

            String result = platformEventService.formatEventMessage(event);

            assertThat(result).contains("Container crashed").contains("web-server").contains("OOMKilled");
        }

        @Test
        void formatEventMessage_findingCritical() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.FINDING_CRITICAL, false);
            event.setTitle("SQL Injection");
            event.setDetail("In UserController.java:45");

            String result = platformEventService.formatEventMessage(event);

            assertThat(result).contains("Critical finding").contains("SQL Injection");
        }

        @Test
        void formatEventMessage_mergeRequestCreated() {
            PlatformEvent event = buildEvent(EVENT_ID, PlatformEventType.MERGE_REQUEST_CREATED, false);
            event.setTitle("Feature/auth into main");

            String result = platformEventService.formatEventMessage(event);

            assertThat(result).contains("Merge request created").contains("Feature/auth into main");
        }
    }

    // ── Channel Resolution ───────────────────────────────────────────────

    @Nested
    class ChannelResolutionTests {

        @Test
        void resolveTargetChannel_explicitChannelId() {
            UUID targetId = UUID.randomUUID();
            Channel target = buildChannel(targetId, "alerts", ChannelType.PRIVATE);

            when(channelRepository.findById(targetId)).thenReturn(Optional.of(target));

            Optional<Channel> result = platformEventService.resolveTargetChannel(
                    PlatformEventType.ALERT_FIRED, null, TEAM_ID, targetId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(targetId);
        }

        @Test
        void resolveTargetChannel_serviceChannel() {
            when(channelRepository.findByTeamIdAndServiceId(TEAM_ID, SERVICE_ID))
                    .thenReturn(Optional.of(serviceChannel));

            Optional<Channel> result = platformEventService.resolveTargetChannel(
                    PlatformEventType.SERVICE_REGISTERED, SERVICE_ID, TEAM_ID, null);

            assertThat(result).isPresent();
            assertThat(result.get().getSlug()).isEqualTo("svc-api");
        }

        @Test
        void resolveTargetChannel_projectChannel() {
            when(channelRepository.findByTeamIdAndProjectId(TEAM_ID, PROJECT_ID))
                    .thenReturn(Optional.of(projectChannel));

            Optional<Channel> result = platformEventService.resolveTargetChannel(
                    PlatformEventType.AUDIT_COMPLETED, PROJECT_ID, TEAM_ID, null);

            assertThat(result).isPresent();
            assertThat(result.get().getSlug()).isEqualTo("proj-main");
        }

        @Test
        void resolveTargetChannel_fallbackGeneral() {
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.of(generalChannel));

            Optional<Channel> result = platformEventService.resolveTargetChannel(
                    PlatformEventType.SESSION_COMPLETED, null, TEAM_ID, null);

            assertThat(result).isPresent();
            assertThat(result.get().getSlug()).isEqualTo("general");
        }

        @Test
        void resolveTargetChannel_noneFound() {
            when(channelRepository.findByTeamIdAndSlug(TEAM_ID, "general"))
                    .thenReturn(Optional.empty());

            Optional<Channel> result = platformEventService.resolveTargetChannel(
                    PlatformEventType.SESSION_COMPLETED, null, TEAM_ID, null);

            assertThat(result).isEmpty();
        }
    }

    // ── Test Data Builders ───────────────────────────────────────────────

    private PlatformEvent buildEvent(UUID id, PlatformEventType type, boolean delivered) {
        PlatformEvent event = PlatformEvent.builder()
                .eventType(type)
                .teamId(TEAM_ID)
                .sourceModule("test")
                .title("Test Event")
                .detail(null)
                .isDelivered(delivered)
                .deliveredAt(delivered ? NOW : null)
                .targetChannelId(delivered ? CHANNEL_ID : null)
                .targetChannelSlug(delivered ? "general" : null)
                .build();
        event.setId(id);
        event.setCreatedAt(NOW);
        event.setUpdatedAt(NOW);
        return event;
    }

    private Channel buildChannel(UUID id, String slug, ChannelType type) {
        Channel channel = Channel.builder()
                .name(slug)
                .slug(slug)
                .channelType(type)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();
        channel.setId(id);
        channel.setCreatedAt(NOW);
        channel.setUpdatedAt(NOW);
        return channel;
    }
}
