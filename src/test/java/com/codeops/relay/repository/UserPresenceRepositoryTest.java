package com.codeops.relay.repository;

import com.codeops.relay.entity.UserPresence;
import com.codeops.relay.entity.enums.PresenceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UserPresenceRepository}.
 *
 * <p>Verifies user/team presence lookups, status filtering,
 * and the custom JPQL query for detecting stale online users
 * whose heartbeat has expired.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class UserPresenceRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserPresenceRepository userPresenceRepository;

    private UUID teamId;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
    }

    private UserPresence createPresence(UUID user, PresenceStatus status, Instant heartbeat) {
        UserPresence presence = UserPresence.builder()
                .userId(user)
                .teamId(teamId)
                .status(status)
                .lastSeenAt(Instant.now())
                .lastHeartbeatAt(heartbeat)
                .build();
        return em.persistAndFlush(presence);
    }

    @Test
    void findByUserIdAndTeamId_returnsPresence() {
        UUID userId = UUID.randomUUID();
        createPresence(userId, PresenceStatus.ONLINE, Instant.now());

        Optional<UserPresence> found = userPresenceRepository
                .findByUserIdAndTeamId(userId, teamId);
        Optional<UserPresence> notFound = userPresenceRepository
                .findByUserIdAndTeamId(UUID.randomUUID(), teamId);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(notFound).isEmpty();
    }

    @Test
    void findByTeamId_returnsAllTeamPresences() {
        createPresence(UUID.randomUUID(), PresenceStatus.ONLINE, Instant.now());
        createPresence(UUID.randomUUID(), PresenceStatus.AWAY, Instant.now());

        UUID otherTeam = UUID.randomUUID();
        UserPresence otherPresence = UserPresence.builder()
                .userId(UUID.randomUUID())
                .teamId(otherTeam)
                .status(PresenceStatus.ONLINE)
                .lastHeartbeatAt(Instant.now())
                .build();
        em.persistAndFlush(otherPresence);

        List<UserPresence> result = userPresenceRepository.findByTeamId(teamId);

        assertThat(result).hasSize(2);
    }

    @Test
    void findByTeamIdAndStatus_filtersCorrectly() {
        createPresence(UUID.randomUUID(), PresenceStatus.ONLINE, Instant.now());
        createPresence(UUID.randomUUID(), PresenceStatus.ONLINE, Instant.now());
        createPresence(UUID.randomUUID(), PresenceStatus.AWAY, Instant.now());
        createPresence(UUID.randomUUID(), PresenceStatus.DND, Instant.now());

        List<UserPresence> online = userPresenceRepository
                .findByTeamIdAndStatus(teamId, PresenceStatus.ONLINE);
        List<UserPresence> away = userPresenceRepository
                .findByTeamIdAndStatus(teamId, PresenceStatus.AWAY);

        assertThat(online).hasSize(2);
        assertThat(away).hasSize(1);
    }

    @Test
    void findByTeamIdAndStatusNot_excludesStatus() {
        createPresence(UUID.randomUUID(), PresenceStatus.ONLINE, Instant.now());
        createPresence(UUID.randomUUID(), PresenceStatus.OFFLINE, Instant.now());
        createPresence(UUID.randomUUID(), PresenceStatus.AWAY, Instant.now());

        List<UserPresence> notOffline = userPresenceRepository
                .findByTeamIdAndStatusNot(teamId, PresenceStatus.OFFLINE);

        assertThat(notOffline).hasSize(2);
        assertThat(notOffline).noneMatch(p -> p.getStatus() == PresenceStatus.OFFLINE);
    }

    @Test
    void findStaleOnlineUsers_findsExpiredHeartbeats() {
        Instant now = Instant.now();
        Instant staleTime = now.minus(5, ChronoUnit.MINUTES);
        Instant recentTime = now.minus(30, ChronoUnit.SECONDS);
        Instant cutoff = now.minus(2, ChronoUnit.MINUTES);

        createPresence(UUID.randomUUID(), PresenceStatus.ONLINE, staleTime);
        createPresence(UUID.randomUUID(), PresenceStatus.ONLINE, recentTime);
        createPresence(UUID.randomUUID(), PresenceStatus.AWAY, staleTime);

        List<UserPresence> stale = userPresenceRepository.findStaleOnlineUsers(cutoff);

        assertThat(stale).hasSize(1);
        assertThat(stale.get(0).getStatus()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(stale.get(0).getLastHeartbeatAt()).isBefore(cutoff);
    }

    @Test
    void statusMessage_persistsCustomText() {
        UUID userId = UUID.randomUUID();
        UserPresence presence = UserPresence.builder()
                .userId(userId)
                .teamId(teamId)
                .status(PresenceStatus.DND)
                .statusMessage("In a meeting")
                .lastHeartbeatAt(Instant.now())
                .build();
        em.persistAndFlush(presence);
        em.clear();

        Optional<UserPresence> found = userPresenceRepository.findByUserIdAndTeamId(userId, teamId);

        assertThat(found).isPresent();
        assertThat(found.get().getStatusMessage()).isEqualTo("In a meeting");
    }
}
