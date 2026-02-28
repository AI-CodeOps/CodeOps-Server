package com.codeops.mcp.security;

import com.codeops.config.AppConstants;
import com.codeops.entity.TeamMember;
import com.codeops.mcp.dto.McpSessionContext;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.service.DeveloperProfileService;
import com.codeops.repository.TeamMemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Authentication filter for MCP endpoints supporting dual auth (JWT + API token).
 *
 * <p>Intercepts requests to {@code /api/v1/mcp/**} and checks the Authorization
 * header for MCP API tokens ({@code Bearer mcp_*}). When an API token is detected,
 * it is validated via {@link DeveloperProfileService#validateToken(String)}, a
 * {@link McpSessionContext} is built and stored as a request attribute, and
 * Spring Security context is populated with the user's team role.</p>
 *
 * <p>Requests with JWT tokens (non-{@code mcp_} prefix) or no Authorization header
 * are passed through to the existing {@link com.codeops.security.JwtAuthFilter}
 * for standard JWT authentication.</p>
 *
 * @see McpSecurityService
 * @see DeveloperProfileService
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class McpTokenAuthFilter extends OncePerRequestFilter {

    /** Request attribute key for the authenticated MCP session context. */
    public static final String MCP_CONTEXT_ATTRIBUTE = "mcpContext";

    private static final String BEARER_MCP_PREFIX = "Bearer " + AppConstants.MCP_TOKEN_PREFIX;

    private final DeveloperProfileService developerProfileService;
    private final McpSecurityService mcpSecurityService;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Processes MCP API token authentication for incoming requests.
     *
     * <p>If the Authorization header contains a {@code Bearer mcp_*} token:
     * validates the token, builds an {@link McpSessionContext}, sets it as
     * a request attribute, and populates Spring Security context. On token
     * validation failure, returns 401 Unauthorized.</p>
     *
     * <p>Non-MCP tokens are passed through to downstream filters.</p>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain to pass the request/response to
     * @throws ServletException if a servlet error occurs during filtering
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_MCP_PREFIX)) {
            String rawToken = authHeader.substring(7); // Strip "Bearer "

            try {
                DeveloperProfile profile = developerProfileService.validateToken(rawToken);

                UUID userId = profile.getUser().getId();
                UUID teamId = profile.getTeam().getId();
                UUID profileId = profile.getId();

                // Resolve allowed scopes from the token
                List<String> scopes = mcpSecurityService.resolveTokenScopes(rawToken);

                McpSessionContext context = new McpSessionContext(
                        profileId, teamId, userId, null, scopes);

                request.setAttribute(MCP_CONTEXT_ATTRIBUTE, context);

                // Set Spring Security context with team role
                setSecurityContext(userId, teamId);

                log.debug("MCP API token authenticated for profile {} user {} team {}",
                        profileId, userId, teamId);

            } catch (Exception e) {
                log.warn("MCP API token authentication failed: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid MCP token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determines whether this filter should be skipped for the given request.
     *
     * <p>Only applies to MCP endpoints ({@code /api/v1/mcp/**}). All other
     * request paths are excluded from MCP token processing.</p>
     *
     * @param request the incoming HTTP request
     * @return {@code true} if the request is not an MCP endpoint
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(AppConstants.MCP_API_PREFIX + "/");
    }

    /**
     * Populates the Spring Security context with the user's team role authorities.
     *
     * <p>Looks up the user's {@link TeamMember} record to determine their role
     * within the team, then creates a {@link UsernamePasswordAuthenticationToken}
     * with the user's UUID as principal and the role mapped to a
     * {@link SimpleGrantedAuthority} with the {@code ROLE_} prefix.</p>
     *
     * @param userId the authenticated user's ID
     * @param teamId the team ID from the developer profile
     */
    private void setSecurityContext(UUID userId, UUID teamId) {
        List<SimpleGrantedAuthority> authorities = teamMemberRepository
                .findByTeamIdAndUserId(teamId, userId)
                .map(TeamMember::getRole)
                .map(role -> List.of(new SimpleGrantedAuthority("ROLE_" + role.name())))
                .orElse(List.of());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
