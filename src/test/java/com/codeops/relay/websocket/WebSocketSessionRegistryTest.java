package com.codeops.relay.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WebSocketSessionRegistry}.
 *
 * <p>Covers session registration, removal, multi-session users, online status
 * checks, and aggregate session counting.</p>
 */
class WebSocketSessionRegistryTest {

    private WebSocketSessionRegistry registry;

    private static final UUID USER_1 = UUID.randomUUID();
    private static final UUID USER_2 = UUID.randomUUID();
    private static final UUID USER_3 = UUID.randomUUID();
    private static final String SESSION_1 = "session-1";
    private static final String SESSION_2 = "session-2";
    private static final String SESSION_3 = "session-3";

    @BeforeEach
    void setUp() {
        registry = new WebSocketSessionRegistry();
    }

    // ── registerSession ──────────────────────────────────────────────────

    @Nested
    class RegisterSessionTests {

        @Test
        void registerSession_addsSession() {
            registry.registerSession(USER_1, SESSION_1);

            assertThat(registry.getSessionIds(USER_1)).containsExactly(SESSION_1);
            assertThat(registry.isUserOnline(USER_1)).isTrue();
        }

        @Test
        void registerSession_multipleSessions() {
            registry.registerSession(USER_1, SESSION_1);
            registry.registerSession(USER_1, SESSION_2);

            assertThat(registry.getSessionIds(USER_1)).containsExactlyInAnyOrder(SESSION_1, SESSION_2);
        }

        @Test
        void registerSession_duplicateSessionIdempotent() {
            registry.registerSession(USER_1, SESSION_1);
            registry.registerSession(USER_1, SESSION_1);

            assertThat(registry.getSessionIds(USER_1)).containsExactly(SESSION_1);
        }
    }

    // ── removeSession ────────────────────────────────────────────────────

    @Nested
    class RemoveSessionTests {

        @Test
        void removeSession_removesSession() {
            registry.registerSession(USER_1, SESSION_1);
            registry.registerSession(USER_1, SESSION_2);

            registry.removeSession(USER_1, SESSION_1);

            assertThat(registry.getSessionIds(USER_1)).containsExactly(SESSION_2);
            assertThat(registry.isUserOnline(USER_1)).isTrue();
        }

        @Test
        void removeSession_lastSession_removesUser() {
            registry.registerSession(USER_1, SESSION_1);

            registry.removeSession(USER_1, SESSION_1);

            assertThat(registry.getSessionIds(USER_1)).isEmpty();
            assertThat(registry.isUserOnline(USER_1)).isFalse();
        }

        @Test
        void removeSession_unknownUser_noOp() {
            registry.removeSession(USER_1, SESSION_1);

            assertThat(registry.isUserOnline(USER_1)).isFalse();
        }

        @Test
        void removeSession_unknownSession_noOp() {
            registry.registerSession(USER_1, SESSION_1);

            registry.removeSession(USER_1, "unknown-session");

            assertThat(registry.getSessionIds(USER_1)).containsExactly(SESSION_1);
        }
    }

    // ── getSessionIds ────────────────────────────────────────────────────

    @Nested
    class GetSessionIdsTests {

        @Test
        void getSessionIds_noSessions_returnsEmpty() {
            assertThat(registry.getSessionIds(USER_1)).isEmpty();
        }

        @Test
        void getSessionIds_returnsUnmodifiable() {
            registry.registerSession(USER_1, SESSION_1);

            Set<String> sessions = registry.getSessionIds(USER_1);

            assertThat(sessions).isUnmodifiable();
        }
    }

    // ── isUserOnline ─────────────────────────────────────────────────────

    @Nested
    class IsUserOnlineTests {

        @Test
        void isUserOnline_noSessions_false() {
            assertThat(registry.isUserOnline(USER_1)).isFalse();
        }

        @Test
        void isUserOnline_withSessions_true() {
            registry.registerSession(USER_1, SESSION_1);

            assertThat(registry.isUserOnline(USER_1)).isTrue();
        }
    }

    // ── getOnlineUserIds ─────────────────────────────────────────────────

    @Nested
    class GetOnlineUserIdsTests {

        @Test
        void getOnlineUserIds_empty() {
            assertThat(registry.getOnlineUserIds()).isEmpty();
        }

        @Test
        void getOnlineUserIds_multipleUsers() {
            registry.registerSession(USER_1, SESSION_1);
            registry.registerSession(USER_2, SESSION_2);

            assertThat(registry.getOnlineUserIds()).containsExactlyInAnyOrder(USER_1, USER_2);
        }

        @Test
        void getOnlineUserIds_afterRemoval_excludesOffline() {
            registry.registerSession(USER_1, SESSION_1);
            registry.registerSession(USER_2, SESSION_2);
            registry.removeSession(USER_1, SESSION_1);

            assertThat(registry.getOnlineUserIds()).containsExactly(USER_2);
        }
    }

    // ── getActiveSessionCount ────────────────────────────────────────────

    @Nested
    class GetActiveSessionCountTests {

        @Test
        void getActiveSessionCount_empty() {
            assertThat(registry.getActiveSessionCount()).isZero();
        }

        @Test
        void getActiveSessionCount_multipleUsersAndSessions() {
            registry.registerSession(USER_1, SESSION_1);
            registry.registerSession(USER_1, SESSION_2);
            registry.registerSession(USER_2, SESSION_3);

            assertThat(registry.getActiveSessionCount()).isEqualTo(3);
        }

        @Test
        void getActiveSessionCount_afterRemoval() {
            registry.registerSession(USER_1, SESSION_1);
            registry.registerSession(USER_2, SESSION_2);
            registry.removeSession(USER_1, SESSION_1);

            assertThat(registry.getActiveSessionCount()).isEqualTo(1);
        }
    }
}
