package com.codeops.mcp.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.dto.response.ActivityFeedEntryResponse;
import com.codeops.mcp.entity.enums.ActivityType;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.mcp.service.ActivityFeedService;
import com.codeops.security.JwtAuthFilter;
import com.codeops.security.JwtTokenProvider;
import com.codeops.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link McpActivityController}.
 *
 * <p>Tests team feed, project feed, and team activity since timestamp endpoints.</p>
 */
@WebMvcTest(McpActivityController.class)
@Import(McpActivityControllerTest.TestSecurityConfig.class)
class McpActivityControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((req, resp, authEx) ->
                                    resp.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED)));
            return http.build();
        }

        @Bean FilterRegistrationBean<JwtAuthFilter> disableJwtAuth(JwtAuthFilter f) {
            var reg = new FilterRegistrationBean<>(f); reg.setEnabled(false); return reg;
        }
        @Bean FilterRegistrationBean<RateLimitFilter> disableRateLimit(RateLimitFilter f) {
            var reg = new FilterRegistrationBean<>(f); reg.setEnabled(false); return reg;
        }
        @Bean FilterRegistrationBean<RequestCorrelationFilter> disableCorrelation(RequestCorrelationFilter f) {
            var reg = new FilterRegistrationBean<>(f); reg.setEnabled(false); return reg;
        }
        @Bean FilterRegistrationBean<McpTokenAuthFilter> disableMcpTokenAuth(McpTokenAuthFilter f) {
            var reg = new FilterRegistrationBean<>(f); reg.setEnabled(false); return reg;
        }
    }

    @Autowired MockMvc mockMvc;

    @MockBean ActivityFeedService activityFeedService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID ENTRY_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/mcp/activity";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private ActivityFeedEntryResponse buildEntry() {
        return new ActivityFeedEntryResponse(ENTRY_ID, ActivityType.SESSION_COMPLETED,
                "Adam completed session on TestProject", "Summary detail",
                "mcp", UUID.randomUUID(), "TestProject", null, null,
                "Adam", PROJECT_ID, UUID.randomUUID(), Instant.now());
    }

    // ── getTeamFeed ──────────────────────────────────────────────────────

    @Test
    void getTeamFeed_200() throws Exception {
        Page<ActivityFeedEntryResponse> page = new PageImpl<>(List.of(buildEntry()));
        when(activityFeedService.getTeamFeed(eq(TEAM_ID), any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/team")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activityType").value("SESSION_COMPLETED"));
    }

    // ── getProjectFeed ───────────────────────────────────────────────────

    @Test
    void getProjectFeed_200() throws Exception {
        Page<ActivityFeedEntryResponse> page = new PageImpl<>(List.of(buildEntry()));
        when(activityFeedService.getProjectFeed(eq(PROJECT_ID), any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/project")
                        .param("projectId", PROJECT_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").exists());
    }

    // ── getTeamActivitySince ─────────────────────────────────────────────

    @Test
    void getTeamActivitySince_200() throws Exception {
        Instant since = Instant.now().minusSeconds(3600);
        when(activityFeedService.getTeamActivitySince(eq(TEAM_ID), any()))
                .thenReturn(List.of(buildEntry()));

        mockMvc.perform(get(BASE_URL + "/team/since")
                        .param("teamId", TEAM_ID.toString())
                        .param("since", since.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ENTRY_ID.toString()));
    }
}
