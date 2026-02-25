package com.codeops.relay.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.User;
import com.codeops.exception.NotFoundException;
import com.codeops.relay.dto.mapper.UserPresenceMapper;
import com.codeops.relay.dto.request.UpdatePresenceRequest;
import com.codeops.relay.dto.response.UserPresenceResponse;
import com.codeops.relay.entity.UserPresence;
import com.codeops.relay.entity.enums.PresenceStatus;
import com.codeops.relay.repository.UserPresenceRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PresenceService}.
 *
 * <p>Covers presence updates, heartbeats, staleness detection, DND lifecycle,
 * team presence retrieval, cleanup, and count aggregation.</p>
 */
@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock private UserPresenceRepository userPresenceRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserPresenceMapper userPresenceMapper;

    @InjectMocks private PresenceService presenceService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID USER_2_ID = UUID.randomUUID();
    private static final UUID USER_3_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();
    private static final Instant STALE = NOW.minusSeconds(
            AppConstants.RELAY_PRESENCE_HEARTBEAT_TIMEOUT_SECONDS + 60);
    private static final Instant RECENT = NOW.minusSeconds(10);

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setDisplayName("Test User");
        testUser.setEmail("test@example.com");
    }

    // ── updatePresence ───────────────────────────────────────────────────

    @Nested
    class UpdatePresenceTests {

        @Test
        void updatePresence_setsStatus() {
            var request = new UpdatePresenceRequest(PresenceStatus.AWAY, null);
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.updatePresence(request, TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.AWAY);
        }

        @Test
        void updatePresence_setsCustomStatus() {
            var request = new UpdatePresenceRequest(PresenceStatus.DND, "In a meeting");
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.updatePresence(request, TEAM_ID, USER_ID);

            assertThat(result.statusMessage()).isEqualTo("In a meeting");
        }

        @Test
        void updatePresence_clearsCustomStatus() {
            var request = new UpdatePresenceRequest(PresenceStatus.ONLINE, null);
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.DND, RECENT);
            existing.setStatusMessage("Old status");

            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.updatePresence(request, TEAM_ID, USER_ID);

            assertThat(result.statusMessage()).isNull();
        }

        @Test
        void updatePresence_createsNewIfNotExists() {
            var request = new UpdatePresenceRequest(PresenceStatus.ONLINE, null);

            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.empty());
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.updatePresence(request, TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.ONLINE);
            verify(userPresenceRepository).save(any(UserPresence.class));
        }

        @Test
        void updatePresence_notTeamMember_throwsNotFound() {
            var request = new UpdatePresenceRequest(PresenceStatus.ONLINE, null);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> presenceService.updatePresence(request, TEAM_ID, USER_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── heartbeat ────────────────────────────────────────────────────────

    @Nested
    class HeartbeatTests {

        @Test
        void heartbeat_createsNewAsOnline() {
            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.empty());
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.heartbeat(TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.ONLINE);
        }

        @Test
        void heartbeat_offlineToOnline() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.OFFLINE, STALE);

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.heartbeat(TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.ONLINE);
        }

        @Test
        void heartbeat_awayToOnline() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.AWAY, RECENT);

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.heartbeat(TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.ONLINE);
        }

        @Test
        void heartbeat_dndStaysDnd() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.DND, RECENT);
            existing.setStatusMessage("Focusing");

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.heartbeat(TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.DND);
        }

        @Test
        void heartbeat_updatesTimestamp() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            presenceService.heartbeat(TEAM_ID, USER_ID);

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(userPresenceRepository).save(captor.capture());
            assertThat(captor.getValue().getLastHeartbeatAt()).isAfterOrEqualTo(RECENT);
        }
    }

    // ── getPresence ──────────────────────────────────────────────────────

    @Nested
    class GetPresenceTests {

        @Test
        void getPresence_found() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.getPresence(TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.ONLINE);
            assertThat(result.userDisplayName()).isEqualTo("Test User");
        }

        @Test
        void getPresence_notFound_defaultsOffline() {
            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.getPresence(TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.OFFLINE);
            assertThat(result.userDisplayName()).isEqualTo("Test User");
        }

        @Test
        void getPresence_staleHeartbeat_transitionsToOffline() {
            UserPresence stale = buildPresence(USER_ID, PresenceStatus.ONLINE, STALE);

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(stale));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.getPresence(TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.OFFLINE);
            verify(userPresenceRepository).save(any(UserPresence.class));
        }
    }

    // ── getTeamPresence ──────────────────────────────────────────────────

    @Nested
    class GetTeamPresenceTests {

        @Test
        void getTeamPresence_sortsCorrectly() {
            User user2 = new User();
            user2.setId(USER_2_ID);
            user2.setDisplayName("Alice");
            User user3 = new User();
            user3.setId(USER_3_ID);
            user3.setDisplayName("Bob");

            UserPresence online = buildPresence(USER_2_ID, PresenceStatus.ONLINE, RECENT);
            UserPresence offline = buildPresence(USER_ID, PresenceStatus.OFFLINE, STALE);
            UserPresence dnd = buildPresence(USER_3_ID, PresenceStatus.DND, RECENT);

            when(userPresenceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(online, offline, dnd));
            when(userRepository.findById(USER_2_ID)).thenReturn(Optional.of(user2));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(USER_3_ID)).thenReturn(Optional.of(user3));

            List<UserPresenceResponse> result = presenceService.getTeamPresence(TEAM_ID);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).status()).isEqualTo(PresenceStatus.ONLINE);
            assertThat(result.get(1).status()).isEqualTo(PresenceStatus.DND);
            assertThat(result.get(2).status()).isEqualTo(PresenceStatus.OFFLINE);
        }

        @Test
        void getTeamPresence_transitionsStaleToOffline() {
            UserPresence stale = buildPresence(USER_ID, PresenceStatus.ONLINE, STALE);

            when(userPresenceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(stale));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            List<UserPresenceResponse> result = presenceService.getTeamPresence(TEAM_ID);

            assertThat(result.get(0).status()).isEqualTo(PresenceStatus.OFFLINE);
            verify(userPresenceRepository).save(any(UserPresence.class));
        }

        @Test
        void getTeamPresence_populatesDisplayNames() {
            UserPresence presence = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(userPresenceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(presence));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            List<UserPresenceResponse> result = presenceService.getTeamPresence(TEAM_ID);

            assertThat(result.get(0).userDisplayName()).isEqualTo("Test User");
        }
    }

    // ── getOnlineUsers ───────────────────────────────────────────────────

    @Nested
    class GetOnlineUsersTests {

        @Test
        void getOnlineUsers_excludesStale() {
            UserPresence stale = buildPresence(USER_ID, PresenceStatus.ONLINE, STALE);
            UserPresence fresh = buildPresence(USER_2_ID, PresenceStatus.ONLINE, RECENT);

            User user2 = new User();
            user2.setId(USER_2_ID);
            user2.setDisplayName("Fresh User");

            when(userPresenceRepository.findByTeamIdAndStatus(TEAM_ID, PresenceStatus.ONLINE))
                    .thenReturn(List.of(stale, fresh));
            when(userRepository.findById(USER_2_ID)).thenReturn(Optional.of(user2));

            List<UserPresenceResponse> result = presenceService.getOnlineUsers(TEAM_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).userId()).isEqualTo(USER_2_ID);
        }

        @Test
        void getOnlineUsers_returnsOnlyOnline() {
            UserPresence online = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(userPresenceRepository.findByTeamIdAndStatus(TEAM_ID, PresenceStatus.ONLINE))
                    .thenReturn(List.of(online));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            List<UserPresenceResponse> result = presenceService.getOnlineUsers(TEAM_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(PresenceStatus.ONLINE);
        }
    }

    // ── DND ──────────────────────────────────────────────────────────────

    @Nested
    class DndTests {

        @Test
        void setDoNotDisturb_success() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.setDoNotDisturb(TEAM_ID, USER_ID, null);

            assertThat(result.status()).isEqualTo(PresenceStatus.DND);
        }

        @Test
        void setDoNotDisturb_setsCustomStatus() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.setDoNotDisturb(
                    TEAM_ID, USER_ID, "In a meeting");

            assertThat(result.statusMessage()).isEqualTo("In a meeting");
        }

        @Test
        void clearDoNotDisturb_transitionsToOnline() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.DND, RECENT);
            existing.setStatusMessage("Focusing");

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.clearDoNotDisturb(TEAM_ID, USER_ID);

            assertThat(result.status()).isEqualTo(PresenceStatus.ONLINE);
        }

        @Test
        void clearDoNotDisturb_clearsCustomStatus() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.DND, RECENT);
            existing.setStatusMessage("Focusing");

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UserPresenceResponse result = presenceService.clearDoNotDisturb(TEAM_ID, USER_ID);

            assertThat(result.statusMessage()).isNull();
        }
    }

    // ── goOffline ────────────────────────────────────────────────────────

    @Nested
    class GoOfflineTests {

        @Test
        void goOffline_setsOffline() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));

            presenceService.goOffline(TEAM_ID, USER_ID);

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(userPresenceRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PresenceStatus.OFFLINE);
        }

        @Test
        void goOffline_clearsCustomStatus() {
            UserPresence existing = buildPresence(USER_ID, PresenceStatus.DND, RECENT);
            existing.setStatusMessage("Away message");

            when(userPresenceRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(Optional.of(existing));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));

            presenceService.goOffline(TEAM_ID, USER_ID);

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(userPresenceRepository).save(captor.capture());
            assertThat(captor.getValue().getStatusMessage()).isNull();
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    @Nested
    class CleanupTests {

        @Test
        void cleanupStalePresences_transitionsStale() {
            UserPresence stale = buildPresence(USER_ID, PresenceStatus.ONLINE, STALE);

            when(userPresenceRepository.findByTeamIdAndStatusNot(TEAM_ID, PresenceStatus.OFFLINE))
                    .thenReturn(List.of(stale));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> inv.getArgument(0));

            int count = presenceService.cleanupStalePresences(TEAM_ID);

            assertThat(count).isEqualTo(1);
            verify(userPresenceRepository).save(any(UserPresence.class));
        }

        @Test
        void cleanupStalePresences_noStale_returnsZero() {
            UserPresence fresh = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            when(userPresenceRepository.findByTeamIdAndStatusNot(TEAM_ID, PresenceStatus.OFFLINE))
                    .thenReturn(List.of(fresh));

            int count = presenceService.cleanupStalePresences(TEAM_ID);

            assertThat(count).isEqualTo(0);
            verify(userPresenceRepository, never()).save(any(UserPresence.class));
        }

        @Test
        void cleanupStalePresences_skipsDndIfRecent() {
            UserPresence dnd = buildPresence(USER_ID, PresenceStatus.DND, RECENT);

            when(userPresenceRepository.findByTeamIdAndStatusNot(TEAM_ID, PresenceStatus.OFFLINE))
                    .thenReturn(List.of(dnd));

            int count = presenceService.cleanupStalePresences(TEAM_ID);

            assertThat(count).isEqualTo(0);
            verify(userPresenceRepository, never()).save(any(UserPresence.class));
        }
    }

    // ── getPresenceCount ─────────────────────────────────────────────────

    @Nested
    class GetPresenceCountTests {

        @Test
        void getPresenceCount_groupsByStatus() {
            UserPresence online1 = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);
            UserPresence online2 = buildPresence(USER_2_ID, PresenceStatus.ONLINE, RECENT);
            UserPresence offline = buildPresence(USER_3_ID, PresenceStatus.OFFLINE, STALE);

            when(userPresenceRepository.findByTeamIdAndStatusNot(TEAM_ID, PresenceStatus.OFFLINE))
                    .thenReturn(List.of(online1, online2));
            when(userPresenceRepository.findByTeamId(TEAM_ID))
                    .thenReturn(List.of(online1, online2, offline));

            Map<PresenceStatus, Long> result = presenceService.getPresenceCount(TEAM_ID);

            assertThat(result.get(PresenceStatus.ONLINE)).isEqualTo(2);
            assertThat(result.get(PresenceStatus.OFFLINE)).isEqualTo(1);
        }

        @Test
        void getPresenceCount_cleansUpFirst() {
            UserPresence stale = buildPresence(USER_ID, PresenceStatus.ONLINE, STALE);

            when(userPresenceRepository.findByTeamIdAndStatusNot(TEAM_ID, PresenceStatus.OFFLINE))
                    .thenReturn(List.of(stale));
            when(userPresenceRepository.save(any(UserPresence.class))).thenAnswer(inv -> {
                UserPresence p = inv.getArgument(0);
                return p;
            });
            when(userPresenceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(stale));

            Map<PresenceStatus, Long> result = presenceService.getPresenceCount(TEAM_ID);

            assertThat(result.get(PresenceStatus.OFFLINE)).isEqualTo(1);
            assertThat(result.containsKey(PresenceStatus.ONLINE)).isFalse();
        }
    }

    // ── isStale helper ───────────────────────────────────────────────────

    @Nested
    class IsStaleTests {

        @Test
        void isStale_nullHeartbeat_true() {
            UserPresence presence = buildPresence(USER_ID, PresenceStatus.ONLINE, null);

            assertThat(presenceService.isStale(presence)).isTrue();
        }

        @Test
        void isStale_recentHeartbeat_false() {
            UserPresence presence = buildPresence(USER_ID, PresenceStatus.ONLINE, RECENT);

            assertThat(presenceService.isStale(presence)).isFalse();
        }
    }

    // ── Test Data Builders ───────────────────────────────────────────────

    private UserPresence buildPresence(UUID userId, PresenceStatus status, Instant lastHeartbeatAt) {
        UserPresence presence = UserPresence.builder()
                .userId(userId)
                .teamId(TEAM_ID)
                .status(status)
                .lastHeartbeatAt(lastHeartbeatAt)
                .lastSeenAt(lastHeartbeatAt)
                .build();
        presence.setId(UUID.randomUUID());
        presence.setCreatedAt(NOW);
        presence.setUpdatedAt(NOW);
        return presence;
    }
}
