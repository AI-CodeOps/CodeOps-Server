package com.codeops.mcp.security;

import com.codeops.exception.AuthorizationException;
import com.codeops.mcp.dto.McpSessionContext;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.entity.McpApiToken;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.enums.SessionStatus;
import com.codeops.mcp.repository.McpApiTokenRepository;
import com.codeops.mcp.repository.McpSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link McpSecurityService}.
 *
 * <p>Verifies session limit enforcement, rate limiting, scope-based tool
 * access control, session ownership validation, and context building.</p>
 */
@ExtendWith(MockitoExtension.class)
class McpSecurityServiceTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private McpSessionRepository sessionRepository;
    @Mock private McpApiTokenRepository tokenRepository;
    @Spy private ObjectMapper objectMapper;

    @InjectMocks
    private McpSecurityService service;

    // ══════════════════════════════════════════════════════════════
    //                    SESSION LIMITS
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Session Limits")
    class SessionLimitTests {

        @Test
        @DisplayName("under limit returns true")
        void isWithinSessionLimit_underLimit_returnsTrue() {
            when(sessionRepository.countByDeveloperProfileIdAndStatus(PROFILE_ID, SessionStatus.ACTIVE))
                    .thenReturn(1L);

            assertThat(service.isWithinSessionLimit(PROFILE_ID, 3)).isTrue();
        }

        @Test
        @DisplayName("at limit returns false")
        void isWithinSessionLimit_atLimit_returnsFalse() {
            when(sessionRepository.countByDeveloperProfileIdAndStatus(PROFILE_ID, SessionStatus.ACTIVE))
                    .thenReturn(3L);

            assertThat(service.isWithinSessionLimit(PROFILE_ID, 3)).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    RATE LIMITING
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimitTests {

        @Test
        @DisplayName("under limit returns true")
        void isWithinRateLimit_underLimit_returnsTrue() {
            UUID sessionId = UUID.randomUUID();

            // First 10 calls should all be within limit
            for (int i = 0; i < 10; i++) {
                assertThat(service.isWithinRateLimit(sessionId, 60)).isTrue();
            }
        }

        @Test
        @DisplayName("over limit returns false")
        void isWithinRateLimit_overLimit_returnsFalse() {
            UUID sessionId = UUID.randomUUID();

            // Fill up to the limit
            for (int i = 0; i < 60; i++) {
                service.isWithinRateLimit(sessionId, 60);
            }

            // 61st call should exceed the limit
            assertThat(service.isWithinRateLimit(sessionId, 60)).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    SCOPE CHECKING
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Scope Checking")
    class ScopeCheckTests {

        @Test
        @DisplayName("no scopes allows all tools")
        void isToolAllowed_noScopes_allowsAll() {
            assertThat(service.isToolAllowed("registry.listServices", null)).isTrue();
            assertThat(service.isToolAllowed("fleet.listContainers", List.of())).isTrue();
        }

        @Test
        @DisplayName("matching scope allows tool")
        void isToolAllowed_matchingScope_allows() {
            List<String> scopes = List.of("registry", "fleet");

            assertThat(service.isToolAllowed("registry.listServices", scopes)).isTrue();
            assertThat(service.isToolAllowed("fleet.listContainers", scopes)).isTrue();
        }

        @Test
        @DisplayName("non-matching scope denies tool")
        void isToolAllowed_nonMatchingScope_denies() {
            List<String> scopes = List.of("registry", "fleet");

            assertThat(service.isToolAllowed("logger.queryLogs", scopes)).isFalse();
            assertThat(service.isToolAllowed("courier.sendRequest", scopes)).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    SESSION OWNERSHIP
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Session Ownership")
    class SessionOwnershipTests {

        @Test
        @DisplayName("owner passes validation")
        void validateSessionOwnership_owner_passes() {
            DeveloperProfile profile = new DeveloperProfile();
            profile.setId(PROFILE_ID);

            McpSession session = new McpSession();
            session.setId(SESSION_ID);
            session.setDeveloperProfile(profile);

            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            service.validateSessionOwnership(SESSION_ID, PROFILE_ID);

            verify(sessionRepository).findById(SESSION_ID);
        }

        @Test
        @DisplayName("non-owner throws AuthorizationException")
        void validateSessionOwnership_notOwner_throws() {
            DeveloperProfile profile = new DeveloperProfile();
            profile.setId(PROFILE_ID);

            McpSession session = new McpSession();
            session.setId(SESSION_ID);
            session.setDeveloperProfile(profile);

            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            UUID otherProfileId = UUID.randomUUID();

            assertThatThrownBy(() -> service.validateSessionOwnership(SESSION_ID, otherProfileId))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("does not belong to developer profile");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    CONTEXT
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Context")
    class ContextTests {

        @Test
        @DisplayName("buildContextFromJwt creates context with no scopes")
        void buildContextFromJwt_buildsContext() {
            McpSessionContext context = service.buildContextFromJwt(USER_ID, TEAM_ID);

            assertThat(context).isNotNull();
            assertThat(context.userId()).isEqualTo(USER_ID);
            assertThat(context.teamId()).isEqualTo(TEAM_ID);
            assertThat(context.developerProfileId()).isNull();
            assertThat(context.sessionId()).isNull();
            assertThat(context.allowedScopes()).isEmpty();
        }

        @Test
        @DisplayName("getCurrentContext extracts context from request attribute")
        void getCurrentContext_extractsContext() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            McpSessionContext expected = new McpSessionContext(
                    PROFILE_ID, TEAM_ID, USER_ID, SESSION_ID, List.of("registry"));

            when(request.getAttribute(McpTokenAuthFilter.MCP_CONTEXT_ATTRIBUTE)).thenReturn(expected);

            McpSessionContext actual = service.getCurrentContext(request);

            assertThat(actual).isSameAs(expected);
        }
    }
}
