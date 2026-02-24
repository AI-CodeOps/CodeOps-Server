package com.codeops.relay.repository;

import com.codeops.relay.entity.PlatformEvent;
import com.codeops.relay.entity.enums.PlatformEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PlatformEventRepository}.
 *
 * <p>Verifies paginated event retrieval, filtering by event type and
 * source module, undelivered event queries, and event counting since
 * a given timestamp.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class PlatformEventRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PlatformEventRepository platformEventRepository;

    private UUID teamId;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
    }

    private PlatformEvent createEvent(PlatformEventType type, String module, boolean delivered) {
        PlatformEvent event = PlatformEvent.builder()
                .eventType(type)
                .teamId(teamId)
                .sourceModule(module)
                .title("Event: " + type.name())
                .isDelivered(delivered)
                .build();
        return em.persistAndFlush(event);
    }

    @Test
    void findByTeamIdOrderByCreatedAtDesc_returnsPaged() {
        createEvent(PlatformEventType.ALERT_FIRED, "logger", true);
        createEvent(PlatformEventType.DEPLOYMENT_COMPLETED, "core", true);
        createEvent(PlatformEventType.SERVICE_REGISTERED, "registry", false);

        Page<PlatformEvent> page = platformEventRepository
                .findByTeamIdOrderByCreatedAtDesc(teamId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findByTeamIdAndEventType_filtersCorrectly() {
        createEvent(PlatformEventType.ALERT_FIRED, "logger", true);
        createEvent(PlatformEventType.ALERT_FIRED, "logger", true);
        createEvent(PlatformEventType.BUILD_COMPLETED, "core", true);

        List<PlatformEvent> alerts = platformEventRepository
                .findByTeamIdAndEventType(teamId, PlatformEventType.ALERT_FIRED);
        List<PlatformEvent> builds = platformEventRepository
                .findByTeamIdAndEventType(teamId, PlatformEventType.BUILD_COMPLETED);

        assertThat(alerts).hasSize(2);
        assertThat(builds).hasSize(1);
    }

    @Test
    void findByTeamIdAndSourceModule_filtersCorrectly() {
        createEvent(PlatformEventType.ALERT_FIRED, "logger", true);
        createEvent(PlatformEventType.SERVICE_REGISTERED, "registry", true);
        createEvent(PlatformEventType.AUDIT_COMPLETED, "core", true);

        List<PlatformEvent> loggerEvents = platformEventRepository
                .findByTeamIdAndSourceModule(teamId, "logger");

        assertThat(loggerEvents).hasSize(1);
        assertThat(loggerEvents.get(0).getEventType()).isEqualTo(PlatformEventType.ALERT_FIRED);
    }

    @Test
    void findByTeamIdAndIsDeliveredFalse_returnsUndelivered() {
        createEvent(PlatformEventType.ALERT_FIRED, "logger", true);
        createEvent(PlatformEventType.DEPLOYMENT_COMPLETED, "core", false);
        createEvent(PlatformEventType.SECRET_ROTATED, "core", false);

        List<PlatformEvent> undelivered = platformEventRepository
                .findByTeamIdAndIsDeliveredFalse(teamId);

        assertThat(undelivered).hasSize(2);
        assertThat(undelivered).allMatch(e -> !e.isDelivered());
    }

    @Test
    void countByTeamIdAndCreatedAtAfter_countsRecentEvents() {
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);

        createEvent(PlatformEventType.ALERT_FIRED, "logger", true);
        createEvent(PlatformEventType.BUILD_COMPLETED, "core", true);

        long count = platformEventRepository.countByTeamIdAndCreatedAtAfter(teamId, twoHoursAgo);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void teamIsolation_queriesOnlyTargetTeam() {
        UUID otherTeam = UUID.randomUUID();

        createEvent(PlatformEventType.ALERT_FIRED, "logger", true);

        PlatformEvent otherEvent = PlatformEvent.builder()
                .eventType(PlatformEventType.BUILD_COMPLETED)
                .teamId(otherTeam)
                .sourceModule("core")
                .title("Other team event")
                .build();
        em.persistAndFlush(otherEvent);

        Page<PlatformEvent> page = platformEventRepository
                .findByTeamIdOrderByCreatedAtDesc(teamId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTeamId()).isEqualTo(teamId);
    }

    @Test
    void pagination_worksCorrectly() {
        for (int i = 0; i < 7; i++) {
            createEvent(PlatformEventType.ALERT_FIRED, "logger", true);
        }

        Page<PlatformEvent> page1 = platformEventRepository
                .findByTeamIdOrderByCreatedAtDesc(teamId, PageRequest.of(0, 3));
        Page<PlatformEvent> page2 = platformEventRepository
                .findByTeamIdOrderByCreatedAtDesc(teamId, PageRequest.of(1, 3));

        assertThat(page1.getContent()).hasSize(3);
        assertThat(page2.getContent()).hasSize(3);
        assertThat(page1.getTotalElements()).isEqualTo(7);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }
}
