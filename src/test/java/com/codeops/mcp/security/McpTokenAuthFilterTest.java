package com.codeops.mcp.security;

import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.TeamRole;
import com.codeops.exception.AuthorizationException;
import com.codeops.mcp.dto.McpSessionContext;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.service.DeveloperProfileService;
import com.codeops.repository.TeamMemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link McpTokenAuthFilter}.
 *
 * <p>Verifies MCP API token authentication, JWT passthrough behavior,
 * request attribute setting, Spring Security context population, and
 * proper error responses for invalid/revoked/expired tokens.</p>
 */
@ExtendWith(MockitoExtension.class)
class McpTokenAuthFilterTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String VALID_MCP_TOKEN = "mcp_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";

    @Mock private DeveloperProfileService developerProfileService;
    @Mock private McpSecurityService mcpSecurityService;
    @Mock private TeamMemberRepository teamMemberRepository;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private McpTokenAuthFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    // ── Test Helpers ──

    private DeveloperProfile createProfile() {
        User user = new User();
        user.setId(USER_ID);

        Team team = new Team();
        team.setId(TEAM_ID);

        DeveloperProfile profile = new DeveloperProfile();
        profile.setId(PROFILE_ID);
        profile.setUser(user);
        profile.setTeam(team);
        return profile;
    }

    private TeamMember createTeamMember(TeamRole role) {
        TeamMember member = new TeamMember();
        member.setRole(role);
        return member;
    }

    // ══════════════════════════════════════════════════════════════
    //                    FILTER SCOPE
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Filter Scope")
    class FilterScopeTests {

        @Test
        @DisplayName("non-MCP endpoint is not filtered")
        void nonMcpEndpoint_notFiltered() {
            when(request.getRequestURI()).thenReturn("/api/v1/projects");

            boolean shouldSkip = filter.shouldNotFilter(request);

            assertThat(shouldSkip).isTrue();
        }

        @Test
        @DisplayName("MCP endpoint is filtered")
        void mcpEndpoint_isFiltered() {
            when(request.getRequestURI()).thenReturn("/api/v1/mcp/sessions");

            boolean shouldSkip = filter.shouldNotFilter(request);

            assertThat(shouldSkip).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    MCP TOKEN AUTH
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("MCP Token Authentication")
    class McpTokenAuthTests {

        @Test
        @DisplayName("valid MCP token authenticates successfully")
        void mcpToken_validToken_authenticates() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_MCP_TOKEN);

            DeveloperProfile profile = createProfile();
            when(developerProfileService.validateToken(VALID_MCP_TOKEN)).thenReturn(profile);
            when(mcpSecurityService.resolveTokenScopes(VALID_MCP_TOKEN)).thenReturn(List.of("registry", "fleet"));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.of(createTeamMember(TeamRole.ADMIN)));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).setStatus(anyInt());
        }

        @Test
        @DisplayName("valid MCP token sets request attribute with McpSessionContext")
        void mcpToken_setsRequestAttribute() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_MCP_TOKEN);

            DeveloperProfile profile = createProfile();
            when(developerProfileService.validateToken(VALID_MCP_TOKEN)).thenReturn(profile);
            when(mcpSecurityService.resolveTokenScopes(VALID_MCP_TOKEN)).thenReturn(List.of());
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.of(createTeamMember(TeamRole.MEMBER)));

            filter.doFilterInternal(request, response, filterChain);

            verify(request).setAttribute(eq(McpTokenAuthFilter.MCP_CONTEXT_ATTRIBUTE),
                    argThat(ctx -> {
                        McpSessionContext context = (McpSessionContext) ctx;
                        return context.developerProfileId().equals(PROFILE_ID)
                                && context.teamId().equals(TEAM_ID)
                                && context.userId().equals(USER_ID)
                                && context.sessionId() == null;
                    }));
        }

        @Test
        @DisplayName("valid MCP token sets Spring Security context")
        void mcpToken_setsSecurityContext() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_MCP_TOKEN);

            DeveloperProfile profile = createProfile();
            when(developerProfileService.validateToken(VALID_MCP_TOKEN)).thenReturn(profile);
            when(mcpSecurityService.resolveTokenScopes(VALID_MCP_TOKEN)).thenReturn(List.of());
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.of(createTeamMember(TeamRole.ADMIN)));

            filter.doFilterInternal(request, response, filterChain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isEqualTo(USER_ID);
            assertThat(auth.getAuthorities()).hasSize(1);
            assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("invalid MCP token returns 401")
        void mcpToken_invalidToken_returns401() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer mcp_invalid_token_value");

            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));
            when(developerProfileService.validateToken("mcp_invalid_token_value"))
                    .thenThrow(new AuthorizationException("Invalid API token"));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            assertThat(sw.toString()).contains("Invalid MCP token");
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("revoked MCP token returns 401")
        void mcpToken_revokedToken_returns401() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer mcp_revoked_token");

            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));
            when(developerProfileService.validateToken("mcp_revoked_token"))
                    .thenThrow(new AuthorizationException("API token has been revoked"));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("expired MCP token returns 401")
        void mcpToken_expiredToken_returns401() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer mcp_expired_token");

            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));
            when(developerProfileService.validateToken("mcp_expired_token"))
                    .thenThrow(new AuthorizationException("API token has expired"));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    PASSTHROUGH
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Passthrough")
    class PassthroughTests {

        @Test
        @DisplayName("JWT token passes through without interception")
        void jwtToken_passesThrough() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer eyJhbGciOiJIUzI1NiJ9.xxx");

            filter.doFilterInternal(request, response, filterChain);

            verify(developerProfileService, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("no auth header passes through")
        void noAuthHeader_passesThrough() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(developerProfileService, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }
}
