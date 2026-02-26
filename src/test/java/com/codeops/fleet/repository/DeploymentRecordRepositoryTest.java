package com.codeops.fleet.repository;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.fleet.entity.ContainerInstance;
import com.codeops.fleet.entity.DeploymentContainer;
import com.codeops.fleet.entity.DeploymentRecord;
import com.codeops.fleet.entity.enums.DeploymentAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for {@link DeploymentRecordRepository}.
 *
 * <p>Verifies cascade persistence with deployment containers, team-scoped pagination,
 * user-scoped queries, and the deployment container join entity.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class DeploymentRecordRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DeploymentRecordRepository repository;

    @Autowired
    private DeploymentContainerRepository deploymentContainerRepository;

    private User user;
    private Team team;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("drr-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        em.persistAndFlush(user);

        team = Team.builder()
                .name("DRR Team " + UUID.randomUUID())
                .owner(user)
                .build();
        em.persistAndFlush(team);
    }

    private DeploymentRecord createRecord(DeploymentAction action, User triggeredBy) {
        DeploymentRecord dr = DeploymentRecord.builder()
                .action(action)
                .triggeredBy(triggeredBy)
                .team(team)
                .build();
        return em.persistAndFlush(dr);
    }

    @Test
    void cascadePersist_deploymentContainers_persistedWithRecord() {
        ContainerInstance ci = ContainerInstance.builder()
                .containerName("cascade-container-" + UUID.randomUUID())
                .serviceName("test-service")
                .imageName("alpine")
                .team(team)
                .build();
        em.persistAndFlush(ci);

        DeploymentRecord dr = DeploymentRecord.builder()
                .action(DeploymentAction.START)
                .serviceCount(1)
                .successCount(1)
                .triggeredBy(user)
                .team(team)
                .build();

        DeploymentContainer dc = DeploymentContainer.builder()
                .success(true)
                .deploymentRecord(dr)
                .container(ci)
                .build();
        dr.getContainers().add(dc);

        DeploymentRecord saved = em.persistAndFlush(dr);
        em.clear();

        DeploymentRecord found = em.find(DeploymentRecord.class, saved.getId());
        assertThat(found.getContainers()).hasSize(1);
        assertThat(found.getContainers().get(0).isSuccess()).isTrue();
        assertThat(found.getContainers().get(0).getContainer().getId())
                .isEqualTo(ci.getId());
    }

    @Test
    void findByTeamId_paged_returnsPagedResults() {
        for (int i = 0; i < 5; i++) {
            createRecord(DeploymentAction.START, user);
        }

        Page<DeploymentRecord> page = repository.findByTeamId(
                team.getId(), PageRequest.of(0, 3));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findByTriggeredById_filtersCorrectly() {
        User otherUser = User.builder()
                .email("drr-other-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Other User")
                .build();
        em.persistAndFlush(otherUser);

        createRecord(DeploymentAction.START, user);
        createRecord(DeploymentAction.STOP, user);

        DeploymentRecord otherRecord = DeploymentRecord.builder()
                .action(DeploymentAction.RESTART)
                .triggeredBy(otherUser)
                .team(team)
                .build();
        em.persistAndFlush(otherRecord);

        Page<DeploymentRecord> userRecords = repository.findByTriggeredById(
                user.getId(), PageRequest.of(0, 10));
        Page<DeploymentRecord> otherRecords = repository.findByTriggeredById(
                otherUser.getId(), PageRequest.of(0, 10));

        assertThat(userRecords.getContent()).hasSize(2);
        assertThat(otherRecords.getContent()).hasSize(1);
    }

    @Test
    void deploymentContainerJoin_queryByContainerId() {
        ContainerInstance ci = ContainerInstance.builder()
                .containerName("join-container-" + UUID.randomUUID())
                .serviceName("test-service")
                .imageName("alpine")
                .team(team)
                .build();
        em.persistAndFlush(ci);

        DeploymentRecord dr1 = DeploymentRecord.builder()
                .action(DeploymentAction.START)
                .triggeredBy(user)
                .team(team)
                .build();
        DeploymentContainer dc1 = DeploymentContainer.builder()
                .success(true)
                .deploymentRecord(dr1)
                .container(ci)
                .build();
        dr1.getContainers().add(dc1);
        em.persistAndFlush(dr1);

        DeploymentRecord dr2 = DeploymentRecord.builder()
                .action(DeploymentAction.RESTART)
                .triggeredBy(user)
                .team(team)
                .build();
        DeploymentContainer dc2 = DeploymentContainer.builder()
                .success(false)
                .errorMessage("Timeout during restart")
                .deploymentRecord(dr2)
                .container(ci)
                .build();
        dr2.getContainers().add(dc2);
        em.persistAndFlush(dr2);

        em.clear();

        var containerDeployments = deploymentContainerRepository.findByContainerId(ci.getId());
        assertThat(containerDeployments).hasSize(2);
        assertThat(containerDeployments)
                .extracting(DeploymentContainer::isSuccess)
                .containsExactlyInAnyOrder(true, false);
    }
}
