package com.codeops.fleet.entity;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.fleet.entity.enums.DeploymentAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity tests for {@link DeploymentRecord}.
 *
 * <p>Verifies persistence, default counter values, container list initialization,
 * and relationships to User and Team.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class DeploymentRecordTest {

    @Autowired
    private TestEntityManager em;

    private User createUser() {
        User user = User.builder()
                .email("dr-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        return em.persistAndFlush(user);
    }

    private Team createTeam(User owner) {
        Team team = Team.builder()
                .name("DR Team " + UUID.randomUUID())
                .owner(owner)
                .build();
        return em.persistAndFlush(team);
    }

    @Test
    void createDeploymentRecord_allFields_persistsCorrectly() {
        User user = createUser();
        Team team = createTeam(user);

        DeploymentRecord dr = DeploymentRecord.builder()
                .action(DeploymentAction.START)
                .description("Starting all services")
                .serviceCount(5)
                .successCount(4)
                .failureCount(1)
                .durationMs(12345L)
                .triggeredBy(user)
                .team(team)
                .build();

        DeploymentRecord saved = em.persistAndFlush(dr);
        em.clear();

        DeploymentRecord found = em.find(DeploymentRecord.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getAction()).isEqualTo(DeploymentAction.START);
        assertThat(found.getDescription()).isEqualTo("Starting all services");
        assertThat(found.getServiceCount()).isEqualTo(5);
        assertThat(found.getSuccessCount()).isEqualTo(4);
        assertThat(found.getFailureCount()).isEqualTo(1);
        assertThat(found.getDurationMs()).isEqualTo(12345L);
    }

    @Test
    void createDeploymentRecord_defaults_countsAreZero() {
        User user = createUser();
        Team team = createTeam(user);

        DeploymentRecord dr = DeploymentRecord.builder()
                .action(DeploymentAction.RESTART)
                .triggeredBy(user)
                .team(team)
                .build();

        DeploymentRecord saved = em.persistAndFlush(dr);
        em.clear();

        DeploymentRecord found = em.find(DeploymentRecord.class, saved.getId());
        assertThat(found.getServiceCount()).isZero();
        assertThat(found.getSuccessCount()).isZero();
        assertThat(found.getFailureCount()).isZero();
    }

    @Test
    void containersList_initializedEmpty() {
        User user = createUser();
        Team team = createTeam(user);

        DeploymentRecord dr = DeploymentRecord.builder()
                .action(DeploymentAction.STOP)
                .triggeredBy(user)
                .team(team)
                .build();

        DeploymentRecord saved = em.persistAndFlush(dr);
        em.clear();

        DeploymentRecord found = em.find(DeploymentRecord.class, saved.getId());
        assertThat(found.getContainers()).isNotNull().isEmpty();
    }

    @Test
    void userRelationship_triggeredBy_resolvesCorrectly() {
        User user = createUser();
        Team team = createTeam(user);

        DeploymentRecord dr = DeploymentRecord.builder()
                .action(DeploymentAction.DESTROY)
                .triggeredBy(user)
                .team(team)
                .build();

        DeploymentRecord saved = em.persistAndFlush(dr);
        em.clear();

        DeploymentRecord found = em.find(DeploymentRecord.class, saved.getId());
        assertThat(found.getTriggeredBy()).isNotNull();
        assertThat(found.getTriggeredBy().getId()).isEqualTo(user.getId());
        assertThat(found.getTeam()).isNotNull();
        assertThat(found.getTeam().getId()).isEqualTo(team.getId());
    }
}
