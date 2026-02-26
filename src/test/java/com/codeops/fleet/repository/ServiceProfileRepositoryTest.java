package com.codeops.fleet.repository;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.VolumeMount;
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
 * Repository tests for {@link ServiceProfileRepository}.
 *
 * <p>Verifies CRUD roundtrip, team-scoped queries, existence checks,
 * enabled status filtering, and cascade persistence of volume mounts.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ServiceProfileRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ServiceProfileRepository repository;

    private Team team;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("spr-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        em.persistAndFlush(user);

        team = Team.builder()
                .name("SPR Team " + UUID.randomUUID())
                .owner(user)
                .build();
        em.persistAndFlush(team);
    }

    private ServiceProfile createProfile(String serviceName, boolean enabled) {
        ServiceProfile sp = ServiceProfile.builder()
                .serviceName(serviceName)
                .imageName("alpine")
                .isEnabled(enabled)
                .team(team)
                .build();
        return em.persistAndFlush(sp);
    }

    @Test
    void crudRoundtrip_persistsAndRetrieves() {
        ServiceProfile sp = createProfile("roundtrip-svc", true);

        Optional<ServiceProfile> found = repository.findById(sp.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getServiceName()).isEqualTo("roundtrip-svc");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findByTeamId_returnsTeamProfiles() {
        createProfile("svc-a", true);
        createProfile("svc-b", true);

        // Create profile in different team
        User otherUser = User.builder()
                .email("spr-other-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Other User")
                .build();
        em.persistAndFlush(otherUser);
        Team otherTeam = Team.builder()
                .name("SPR Other " + UUID.randomUUID())
                .owner(otherUser)
                .build();
        em.persistAndFlush(otherTeam);
        ServiceProfile otherProfile = ServiceProfile.builder()
                .serviceName("other-svc")
                .imageName("alpine")
                .team(otherTeam)
                .build();
        em.persistAndFlush(otherProfile);

        List<ServiceProfile> result = repository.findByTeamId(team.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ServiceProfile::getServiceName)
                .containsExactlyInAnyOrder("svc-a", "svc-b");
    }

    @Test
    void existsByTeamIdAndServiceName_checksCorrectly() {
        createProfile("existing-svc", true);

        assertThat(repository.existsByTeamIdAndServiceName(team.getId(), "existing-svc"))
                .isTrue();
        assertThat(repository.existsByTeamIdAndServiceName(team.getId(), "nonexistent"))
                .isFalse();
    }

    @Test
    void findByTeamIdAndIsEnabled_filtersCorrectly() {
        createProfile("enabled-1", true);
        createProfile("enabled-2", true);
        createProfile("disabled-1", false);

        List<ServiceProfile> enabled = repository.findByTeamIdAndIsEnabled(
                team.getId(), true);
        List<ServiceProfile> disabled = repository.findByTeamIdAndIsEnabled(
                team.getId(), false);

        assertThat(enabled).hasSize(2);
        assertThat(disabled).hasSize(1);
    }

    @Test
    void cascadePersist_volumeMounts_persistedWithProfile() {
        ServiceProfile sp = ServiceProfile.builder()
                .serviceName("cascade-svc")
                .imageName("postgres")
                .team(team)
                .build();
        ServiceProfile saved = em.persistAndFlush(sp);

        VolumeMount vm1 = VolumeMount.builder()
                .containerPath("/var/lib/postgresql/data")
                .volumeName("pgdata")
                .serviceProfile(saved)
                .build();
        VolumeMount vm2 = VolumeMount.builder()
                .hostPath("/tmp/backup")
                .containerPath("/backup")
                .serviceProfile(saved)
                .build();
        saved.getVolumeMounts().add(vm1);
        saved.getVolumeMounts().add(vm2);

        em.persistAndFlush(saved);
        em.clear();

        Optional<ServiceProfile> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getVolumeMounts()).hasSize(2);
        assertThat(found.get().getVolumeMounts())
                .extracting(VolumeMount::getContainerPath)
                .containsExactlyInAnyOrder("/var/lib/postgresql/data", "/backup");
    }
}
