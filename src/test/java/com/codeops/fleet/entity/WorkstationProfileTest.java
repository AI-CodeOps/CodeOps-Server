package com.codeops.fleet.entity;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity tests for {@link WorkstationProfile} and {@link WorkstationSolution}.
 *
 * <p>Verifies persistence, default field values, relationship integrity,
 * and cascade behavior between workstation profiles and their solutions.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class WorkstationProfileTest {

    @Autowired
    private TestEntityManager em;

    private User createUser() {
        User user = User.builder()
                .email("ws-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        return em.persistAndFlush(user);
    }

    private Team createTeam(User owner) {
        Team team = Team.builder()
                .name("Workstation Team " + UUID.randomUUID())
                .owner(owner)
                .build();
        return em.persistAndFlush(team);
    }

    private SolutionProfile createSolutionProfile(Team team) {
        SolutionProfile sol = SolutionProfile.builder()
                .name("Sol-" + UUID.randomUUID().toString().substring(0, 8))
                .team(team)
                .build();
        return em.persistAndFlush(sol);
    }

    @Test
    void createWorkstationProfile_allFields_persistsCorrectly() {
        User user = createUser();
        Team team = createTeam(user);

        WorkstationProfile profile = WorkstationProfile.builder()
                .name("My Dev Workstation")
                .description("Full development environment")
                .isDefault(true)
                .user(user)
                .team(team)
                .build();

        WorkstationProfile saved = em.persistAndFlush(profile);
        em.clear();

        WorkstationProfile found = em.find(WorkstationProfile.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("My Dev Workstation");
        assertThat(found.getDescription()).isEqualTo("Full development environment");
        assertThat(found.isDefault()).isTrue();
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getTeam().getId()).isEqualTo(team.getId());
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void createWorkstationProfile_defaults_appliedCorrectly() {
        User user = createUser();
        Team team = createTeam(user);

        WorkstationProfile profile = WorkstationProfile.builder()
                .name("Defaults Profile")
                .user(user)
                .team(team)
                .build();

        WorkstationProfile saved = em.persistAndFlush(profile);
        em.clear();

        WorkstationProfile found = em.find(WorkstationProfile.class, saved.getId());
        assertThat(found.isDefault()).isFalse();
        assertThat(found.getDescription()).isNull();
        assertThat(found.getSolutions()).isEmpty();
    }

    @Test
    void workstationSolution_persistsWithOrdering() {
        User user = createUser();
        Team team = createTeam(user);
        SolutionProfile sol1 = createSolutionProfile(team);
        SolutionProfile sol2 = createSolutionProfile(team);

        WorkstationProfile ws = WorkstationProfile.builder()
                .name("Ordered Workstation")
                .user(user)
                .team(team)
                .build();
        ws = em.persistAndFlush(ws);

        WorkstationSolution wSol1 = WorkstationSolution.builder()
                .startOrder(1)
                .overrideEnvVarsJson("{\"DB_HOST\": \"localhost\"}")
                .workstationProfile(ws)
                .solutionProfile(sol1)
                .build();
        em.persistAndFlush(wSol1);

        WorkstationSolution wSol2 = WorkstationSolution.builder()
                .startOrder(2)
                .workstationProfile(ws)
                .solutionProfile(sol2)
                .build();
        em.persistAndFlush(wSol2);
        em.clear();

        WorkstationSolution found1 = em.find(WorkstationSolution.class, wSol1.getId());
        assertThat(found1.getStartOrder()).isEqualTo(1);
        assertThat(found1.getOverrideEnvVarsJson()).isEqualTo("{\"DB_HOST\": \"localhost\"}");
        assertThat(found1.getWorkstationProfile().getId()).isEqualTo(ws.getId());
        assertThat(found1.getSolutionProfile().getId()).isEqualTo(sol1.getId());

        WorkstationSolution found2 = em.find(WorkstationSolution.class, wSol2.getId());
        assertThat(found2.getStartOrder()).isEqualTo(2);
        assertThat(found2.getOverrideEnvVarsJson()).isNull();
    }

    @Test
    void workstationSolution_defaultStartOrder_isZero() {
        User user = createUser();
        Team team = createTeam(user);
        SolutionProfile sol = createSolutionProfile(team);

        WorkstationProfile ws = WorkstationProfile.builder()
                .name("Default Order WS")
                .user(user)
                .team(team)
                .build();
        ws = em.persistAndFlush(ws);

        WorkstationSolution wSol = WorkstationSolution.builder()
                .workstationProfile(ws)
                .solutionProfile(sol)
                .build();
        WorkstationSolution saved = em.persistAndFlush(wSol);
        em.clear();

        WorkstationSolution found = em.find(WorkstationSolution.class, saved.getId());
        assertThat(found.getStartOrder()).isZero();
    }

    @Test
    void userAndTeamRelationships_resolveCorrectly() {
        User user = createUser();
        Team team = createTeam(user);

        WorkstationProfile profile = WorkstationProfile.builder()
                .name("Relations Test")
                .user(user)
                .team(team)
                .build();

        WorkstationProfile saved = em.persistAndFlush(profile);
        em.clear();

        WorkstationProfile found = em.find(WorkstationProfile.class, saved.getId());
        assertThat(found.getUser()).isNotNull();
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getTeam()).isNotNull();
        assertThat(found.getTeam().getId()).isEqualTo(team.getId());
    }
}
