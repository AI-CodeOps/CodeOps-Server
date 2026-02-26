package com.codeops.fleet.repository;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.fleet.entity.ContainerInstance;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.enums.ContainerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for {@link ContainerInstanceRepository}.
 *
 * <p>Verifies CRUD roundtrip, team-scoped queries, status filtering,
 * service name filtering, status counting, and container name lookup.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ContainerInstanceRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ContainerInstanceRepository repository;

    private Team team;
    private Team otherTeam;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("cir-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        em.persistAndFlush(user);

        team = Team.builder()
                .name("CIR Team " + UUID.randomUUID())
                .owner(user)
                .build();
        em.persistAndFlush(team);

        User otherUser = User.builder()
                .email("cir-other-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Other User")
                .build();
        em.persistAndFlush(otherUser);

        otherTeam = Team.builder()
                .name("CIR Other " + UUID.randomUUID())
                .owner(otherUser)
                .build();
        em.persistAndFlush(otherTeam);
    }

    private ContainerInstance createContainer(Team t, String name, String serviceName,
                                              ContainerStatus status) {
        ContainerInstance ci = ContainerInstance.builder()
                .containerName(name)
                .serviceName(serviceName)
                .imageName("alpine")
                .status(status)
                .team(t)
                .build();
        return em.persistAndFlush(ci);
    }

    @Test
    void crudRoundtrip_persistsAndRetrieves() {
        ContainerInstance ci = createContainer(team, "roundtrip-test",
                "test-service", ContainerStatus.CREATED);

        Optional<ContainerInstance> found = repository.findById(ci.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getContainerName()).isEqualTo("roundtrip-test");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void findByTeamId_returnsOnlyTeamContainers() {
        createContainer(team, "c1", "svc-a", ContainerStatus.RUNNING);
        createContainer(team, "c2", "svc-b", ContainerStatus.RUNNING);
        createContainer(otherTeam, "c3", "svc-c", ContainerStatus.RUNNING);

        List<ContainerInstance> result = repository.findByTeamId(team.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ci -> ci.getTeam().getId())
                .containsOnly(team.getId());
    }

    @Test
    void findByTeamIdAndStatus_filtersCorrectly() {
        createContainer(team, "running-1", "svc-a", ContainerStatus.RUNNING);
        createContainer(team, "running-2", "svc-b", ContainerStatus.RUNNING);
        createContainer(team, "stopped-1", "svc-c", ContainerStatus.STOPPED);

        List<ContainerInstance> running = repository.findByTeamIdAndStatus(
                team.getId(), ContainerStatus.RUNNING);
        List<ContainerInstance> stopped = repository.findByTeamIdAndStatus(
                team.getId(), ContainerStatus.STOPPED);

        assertThat(running).hasSize(2);
        assertThat(stopped).hasSize(1);
    }

    @Test
    void findByTeamIdAndServiceName_filtersCorrectly() {
        createContainer(team, "pg-1", "postgres", ContainerStatus.RUNNING);
        createContainer(team, "pg-2", "postgres", ContainerStatus.STOPPED);
        createContainer(team, "redis-1", "redis", ContainerStatus.RUNNING);

        List<ContainerInstance> postgres = repository.findByTeamIdAndServiceName(
                team.getId(), "postgres");

        assertThat(postgres).hasSize(2);
        assertThat(postgres).extracting(ContainerInstance::getServiceName)
                .containsOnly("postgres");
    }

    @Test
    void countByTeamIdAndStatus_countsCorrectly() {
        createContainer(team, "cnt-1", "svc-a", ContainerStatus.RUNNING);
        createContainer(team, "cnt-2", "svc-b", ContainerStatus.RUNNING);
        createContainer(team, "cnt-3", "svc-c", ContainerStatus.EXITED);
        createContainer(otherTeam, "cnt-4", "svc-d", ContainerStatus.RUNNING);

        long runningCount = repository.countByTeamIdAndStatus(
                team.getId(), ContainerStatus.RUNNING);
        long exitedCount = repository.countByTeamIdAndStatus(
                team.getId(), ContainerStatus.EXITED);

        assertThat(runningCount).isEqualTo(2);
        assertThat(exitedCount).isEqualTo(1);
    }

    @Test
    void findByTeamIdAndContainerName_findsUniqueContainer() {
        createContainer(team, "unique-name", "svc-a", ContainerStatus.RUNNING);

        Optional<ContainerInstance> found = repository.findByTeamIdAndContainerName(
                team.getId(), "unique-name");
        Optional<ContainerInstance> notFound = repository.findByTeamIdAndContainerName(
                team.getId(), "nonexistent");

        assertThat(found).isPresent();
        assertThat(found.get().getContainerName()).isEqualTo("unique-name");
        assertThat(notFound).isEmpty();
    }
}
