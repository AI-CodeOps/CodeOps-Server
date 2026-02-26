package com.codeops.fleet.entity;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.fleet.entity.enums.HealthStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity tests for {@link ContainerHealthCheck}.
 *
 * <p>Verifies persistence, health status enum storage,
 * and the container relationship.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ContainerHealthCheckTest {

    @Autowired
    private TestEntityManager em;

    private ContainerInstance createContainer() {
        User user = User.builder()
                .email("hc-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        em.persistAndFlush(user);

        Team team = Team.builder()
                .name("HC Team " + UUID.randomUUID())
                .owner(user)
                .build();
        em.persistAndFlush(team);

        ContainerInstance ci = ContainerInstance.builder()
                .containerName("hc-container-" + UUID.randomUUID())
                .serviceName("test-service")
                .imageName("alpine")
                .team(team)
                .build();
        return em.persistAndFlush(ci);
    }

    @Test
    void createHealthCheck_allFields_persistsCorrectly() {
        ContainerInstance container = createContainer();

        ContainerHealthCheck hc = ContainerHealthCheck.builder()
                .status(HealthStatus.HEALTHY)
                .output("Connection to localhost port 5432 [tcp/postgresql]: succeeded!")
                .exitCode(0)
                .durationMs(150L)
                .container(container)
                .build();

        ContainerHealthCheck saved = em.persistAndFlush(hc);
        em.clear();

        ContainerHealthCheck found = em.find(ContainerHealthCheck.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(found.getOutput()).contains("succeeded");
        assertThat(found.getExitCode()).isZero();
        assertThat(found.getDurationMs()).isEqualTo(150L);
    }

    @Test
    void healthStatus_persistsAllEnumValues() {
        ContainerInstance container = createContainer();

        for (HealthStatus hs : HealthStatus.values()) {
            ContainerHealthCheck hc = ContainerHealthCheck.builder()
                    .status(hs)
                    .container(container)
                    .build();

            ContainerHealthCheck saved = em.persistAndFlush(hc);
            em.clear();

            ContainerHealthCheck found = em.find(ContainerHealthCheck.class, saved.getId());
            assertThat(found.getStatus()).isEqualTo(hs);
        }
    }

    @Test
    void containerRelationship_resolvesCorrectly() {
        ContainerInstance container = createContainer();

        ContainerHealthCheck hc = ContainerHealthCheck.builder()
                .status(HealthStatus.UNHEALTHY)
                .exitCode(1)
                .container(container)
                .build();

        ContainerHealthCheck saved = em.persistAndFlush(hc);
        em.clear();

        ContainerHealthCheck found = em.find(ContainerHealthCheck.class, saved.getId());
        assertThat(found.getContainer()).isNotNull();
        assertThat(found.getContainer().getId()).isEqualTo(container.getId());
        assertThat(found.getContainer().getContainerName()).isEqualTo(container.getContainerName());
    }
}
