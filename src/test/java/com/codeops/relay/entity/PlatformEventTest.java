package com.codeops.relay.entity;

import com.codeops.relay.entity.enums.PlatformEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link PlatformEvent} entity.
 *
 * <p>Verifies event creation with all PlatformEventType values,
 * delivery tracking fields, and default values.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class PlatformEventTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void createPlatformEvent_allFields_persistsCorrectly() {
        UUID teamId = UUID.randomUUID();
        UUID sourceEntityId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();

        PlatformEvent event = PlatformEvent.builder()
                .eventType(PlatformEventType.DEPLOYMENT_COMPLETED)
                .teamId(teamId)
                .sourceModule("courier")
                .sourceEntityId(sourceEntityId)
                .title("Deployment completed for API Gateway")
                .detail("Version 2.1.0 deployed successfully to production.")
                .targetChannelId(channelId)
                .targetChannelSlug("deployments")
                .build();

        PlatformEvent saved = em.persistAndFlush(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo(PlatformEventType.DEPLOYMENT_COMPLETED);
        assertThat(saved.getTeamId()).isEqualTo(teamId);
        assertThat(saved.getSourceModule()).isEqualTo("courier");
        assertThat(saved.getSourceEntityId()).isEqualTo(sourceEntityId);
        assertThat(saved.getTitle()).isEqualTo("Deployment completed for API Gateway");
        assertThat(saved.getDetail()).isEqualTo("Version 2.1.0 deployed successfully to production.");
        assertThat(saved.getTargetChannelId()).isEqualTo(channelId);
        assertThat(saved.getTargetChannelSlug()).isEqualTo("deployments");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void isDelivered_defaultsFalse() {
        PlatformEvent event = PlatformEvent.builder()
                .eventType(PlatformEventType.ALERT_FIRED)
                .teamId(UUID.randomUUID())
                .sourceModule("logger")
                .title("Alert fired: High error rate")
                .build();

        PlatformEvent saved = em.persistAndFlush(event);

        assertThat(saved.isDelivered()).isFalse();
        assertThat(saved.getDeliveredAt()).isNull();
        assertThat(saved.getPostedMessageId()).isNull();
    }

    @Test
    void deliveryTracking_updatesOnDelivery() {
        PlatformEvent event = PlatformEvent.builder()
                .eventType(PlatformEventType.BUILD_COMPLETED)
                .teamId(UUID.randomUUID())
                .sourceModule("core")
                .title("Build completed")
                .build();

        PlatformEvent saved = em.persistAndFlush(event);

        UUID messageId = UUID.randomUUID();
        Instant deliveredAt = Instant.now();
        saved.setDelivered(true);
        saved.setDeliveredAt(deliveredAt);
        saved.setPostedMessageId(messageId);
        em.persistAndFlush(saved);
        em.clear();

        PlatformEvent found = em.find(PlatformEvent.class, saved.getId());
        assertThat(found.isDelivered()).isTrue();
        assertThat(found.getDeliveredAt()).isEqualTo(deliveredAt);
        assertThat(found.getPostedMessageId()).isEqualTo(messageId);
    }

    @Test
    void eventType_persistsAllEnumValues() {
        for (PlatformEventType type : PlatformEventType.values()) {
            PlatformEvent event = PlatformEvent.builder()
                    .eventType(type)
                    .teamId(UUID.randomUUID())
                    .sourceModule("core")
                    .title("Event: " + type.name())
                    .build();

            PlatformEvent saved = em.persistAndFlush(event);
            em.clear();

            PlatformEvent found = em.find(PlatformEvent.class, saved.getId());
            assertThat(found.getEventType()).isEqualTo(type);
        }
    }

    @Test
    void optionalFields_canBeNull() {
        PlatformEvent event = PlatformEvent.builder()
                .eventType(PlatformEventType.SERVICE_REGISTERED)
                .teamId(UUID.randomUUID())
                .sourceModule("registry")
                .title("New service registered")
                .build();

        PlatformEvent saved = em.persistAndFlush(event);

        assertThat(saved.getSourceEntityId()).isNull();
        assertThat(saved.getDetail()).isNull();
        assertThat(saved.getTargetChannelId()).isNull();
        assertThat(saved.getTargetChannelSlug()).isNull();
        assertThat(saved.getPostedMessageId()).isNull();
        assertThat(saved.getDeliveredAt()).isNull();
    }

    @Test
    void sourceModule_storesModuleName() {
        String[] modules = {"core", "registry", "logger", "courier"};

        for (String module : modules) {
            PlatformEvent event = PlatformEvent.builder()
                    .eventType(PlatformEventType.AUDIT_COMPLETED)
                    .teamId(UUID.randomUUID())
                    .sourceModule(module)
                    .title("From " + module)
                    .build();

            PlatformEvent saved = em.persistAndFlush(event);
            em.clear();

            PlatformEvent found = em.find(PlatformEvent.class, saved.getId());
            assertThat(found.getSourceModule()).isEqualTo(module);
        }
    }
}
