package com.codeops.fleet.entity;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity tests for {@link ContainerInstance}.
 *
 * <p>Verifies persistence, default field values, enum storage,
 * and the team relationship.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ContainerInstanceTest {

    @Autowired
    private TestEntityManager em;

    private Team createTeam() {
        User user = User.builder()
                .email("ci-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        em.persistAndFlush(user);

        Team team = Team.builder()
                .name("CI Team " + UUID.randomUUID())
                .owner(user)
                .build();
        return em.persistAndFlush(team);
    }

    @Test
    void createContainerInstance_allFields_persistsCorrectly() {
        Team team = createTeam();
        Instant now = Instant.now();

        ContainerInstance ci = ContainerInstance.builder()
                .containerId("abc123def456abc123def456abc123def456abc123def456abc123def456abcd")
                .containerName("my-postgres")
                .serviceName("postgres-service")
                .imageName("postgres")
                .imageTag("16")
                .status(ContainerStatus.RUNNING)
                .healthStatus(HealthStatus.HEALTHY)
                .restartPolicy(RestartPolicy.ALWAYS)
                .restartCount(2)
                .exitCode(null)
                .cpuPercent(12.5)
                .memoryBytes(536870912L)
                .memoryLimitBytes(1073741824L)
                .pid(1234)
                .startedAt(now)
                .errorMessage(null)
                .team(team)
                .build();

        ContainerInstance saved = em.persistAndFlush(ci);
        em.clear();

        ContainerInstance found = em.find(ContainerInstance.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getContainerId()).startsWith("abc123");
        assertThat(found.getContainerName()).isEqualTo("my-postgres");
        assertThat(found.getServiceName()).isEqualTo("postgres-service");
        assertThat(found.getImageName()).isEqualTo("postgres");
        assertThat(found.getImageTag()).isEqualTo("16");
        assertThat(found.getStatus()).isEqualTo(ContainerStatus.RUNNING);
        assertThat(found.getHealthStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(found.getRestartPolicy()).isEqualTo(RestartPolicy.ALWAYS);
        assertThat(found.getRestartCount()).isEqualTo(2);
        assertThat(found.getCpuPercent()).isEqualTo(12.5);
        assertThat(found.getMemoryBytes()).isEqualTo(536870912L);
        assertThat(found.getPid()).isEqualTo(1234);
        assertThat(found.getTeam().getId()).isEqualTo(team.getId());
    }

    @Test
    void createContainerInstance_defaults_appliedCorrectly() {
        Team team = createTeam();

        ContainerInstance ci = ContainerInstance.builder()
                .containerName("defaults-test")
                .serviceName("test-service")
                .imageName("nginx")
                .team(team)
                .build();

        ContainerInstance saved = em.persistAndFlush(ci);
        em.clear();

        ContainerInstance found = em.find(ContainerInstance.class, saved.getId());
        assertThat(found.getImageTag()).isEqualTo("latest");
        assertThat(found.getStatus()).isEqualTo(ContainerStatus.CREATED);
        assertThat(found.getHealthStatus()).isEqualTo(HealthStatus.NONE);
        assertThat(found.getRestartPolicy()).isEqualTo(RestartPolicy.NO);
        assertThat(found.getRestartCount()).isZero();
    }

    @Test
    void containerStatus_persistsAllEnumValues() {
        Team team = createTeam();

        for (ContainerStatus status : ContainerStatus.values()) {
            ContainerInstance ci = ContainerInstance.builder()
                    .containerName("status-" + status.name())
                    .serviceName("test-service")
                    .imageName("alpine")
                    .status(status)
                    .team(team)
                    .build();

            ContainerInstance saved = em.persistAndFlush(ci);
            em.clear();

            ContainerInstance found = em.find(ContainerInstance.class, saved.getId());
            assertThat(found.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void healthStatus_persistsAllEnumValues() {
        Team team = createTeam();

        for (HealthStatus hs : HealthStatus.values()) {
            ContainerInstance ci = ContainerInstance.builder()
                    .containerName("health-" + hs.name())
                    .serviceName("test-service")
                    .imageName("alpine")
                    .healthStatus(hs)
                    .team(team)
                    .build();

            ContainerInstance saved = em.persistAndFlush(ci);
            em.clear();

            ContainerInstance found = em.find(ContainerInstance.class, saved.getId());
            assertThat(found.getHealthStatus()).isEqualTo(hs);
        }
    }

    @Test
    void teamRelationship_lazyLoaded_resolvesCorrectly() {
        Team team = createTeam();

        ContainerInstance ci = ContainerInstance.builder()
                .containerName("team-test")
                .serviceName("test-service")
                .imageName("redis")
                .team(team)
                .build();

        ContainerInstance saved = em.persistAndFlush(ci);
        em.clear();

        ContainerInstance found = em.find(ContainerInstance.class, saved.getId());
        assertThat(found.getTeam()).isNotNull();
        assertThat(found.getTeam().getId()).isEqualTo(team.getId());
        assertThat(found.getTeam().getName()).isEqualTo(team.getName());
    }
}
