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
 * Entity tests for {@link SolutionProfile} and {@link SolutionService}.
 *
 * <p>Verifies persistence, default field values, relationship integrity,
 * and cascade behavior between solution profiles and their services.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class SolutionProfileTest {

    @Autowired
    private TestEntityManager em;

    private Team createTeam() {
        User user = User.builder()
                .email("sol-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        em.persistAndFlush(user);

        Team team = Team.builder()
                .name("Solution Team " + UUID.randomUUID())
                .owner(user)
                .build();
        return em.persistAndFlush(team);
    }

    private ServiceProfile createServiceProfile(Team team) {
        ServiceProfile sp = ServiceProfile.builder()
                .serviceName("svc-" + UUID.randomUUID().toString().substring(0, 8))
                .imageName("nginx")
                .imageTag("alpine")
                .team(team)
                .build();
        return em.persistAndFlush(sp);
    }

    @Test
    void createSolutionProfile_allFields_persistsCorrectly() {
        Team team = createTeam();

        SolutionProfile profile = SolutionProfile.builder()
                .name("Full Stack Dev")
                .description("API + Database + Cache")
                .isDefault(true)
                .team(team)
                .build();

        SolutionProfile saved = em.persistAndFlush(profile);
        em.clear();

        SolutionProfile found = em.find(SolutionProfile.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Full Stack Dev");
        assertThat(found.getDescription()).isEqualTo("API + Database + Cache");
        assertThat(found.isDefault()).isTrue();
        assertThat(found.getTeam().getId()).isEqualTo(team.getId());
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void createSolutionProfile_defaults_appliedCorrectly() {
        Team team = createTeam();

        SolutionProfile profile = SolutionProfile.builder()
                .name("Minimal Profile")
                .team(team)
                .build();

        SolutionProfile saved = em.persistAndFlush(profile);
        em.clear();

        SolutionProfile found = em.find(SolutionProfile.class, saved.getId());
        assertThat(found.isDefault()).isFalse();
        assertThat(found.getDescription()).isNull();
        assertThat(found.getServices()).isEmpty();
    }

    @Test
    void solutionService_persistsWithOrdering() {
        Team team = createTeam();
        ServiceProfile sp1 = createServiceProfile(team);
        ServiceProfile sp2 = createServiceProfile(team);

        SolutionProfile solution = SolutionProfile.builder()
                .name("Ordered Solution")
                .team(team)
                .build();
        solution = em.persistAndFlush(solution);

        SolutionService svc1 = SolutionService.builder()
                .startOrder(1)
                .solutionProfile(solution)
                .serviceProfile(sp1)
                .build();
        em.persistAndFlush(svc1);

        SolutionService svc2 = SolutionService.builder()
                .startOrder(2)
                .solutionProfile(solution)
                .serviceProfile(sp2)
                .build();
        em.persistAndFlush(svc2);
        em.clear();

        SolutionService found1 = em.find(SolutionService.class, svc1.getId());
        assertThat(found1.getStartOrder()).isEqualTo(1);
        assertThat(found1.getSolutionProfile().getId()).isEqualTo(solution.getId());
        assertThat(found1.getServiceProfile().getId()).isEqualTo(sp1.getId());

        SolutionService found2 = em.find(SolutionService.class, svc2.getId());
        assertThat(found2.getStartOrder()).isEqualTo(2);
    }

    @Test
    void solutionService_defaultStartOrder_isZero() {
        Team team = createTeam();
        ServiceProfile sp = createServiceProfile(team);

        SolutionProfile solution = SolutionProfile.builder()
                .name("Default Order Solution")
                .team(team)
                .build();
        solution = em.persistAndFlush(solution);

        SolutionService svc = SolutionService.builder()
                .solutionProfile(solution)
                .serviceProfile(sp)
                .build();
        SolutionService saved = em.persistAndFlush(svc);
        em.clear();

        SolutionService found = em.find(SolutionService.class, saved.getId());
        assertThat(found.getStartOrder()).isZero();
    }

    @Test
    void teamRelationship_resolvesCorrectly() {
        Team team = createTeam();

        SolutionProfile profile = SolutionProfile.builder()
                .name("Team Relation Test")
                .team(team)
                .build();

        SolutionProfile saved = em.persistAndFlush(profile);
        em.clear();

        SolutionProfile found = em.find(SolutionProfile.class, saved.getId());
        assertThat(found.getTeam()).isNotNull();
        assertThat(found.getTeam().getId()).isEqualTo(team.getId());
        assertThat(found.getTeam().getName()).isEqualTo(team.getName());
    }
}
