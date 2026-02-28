package com.codeops.fleet.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.fleet.dto.response.ContainerHealthCheckResponse;
import com.codeops.fleet.dto.response.FleetHealthSummaryResponse;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.service.FleetHealthService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link FleetHealthController}.
 *
 * <p>Covers health summary retrieval, individual and bulk health checks,
 * health check history, and log purge operations.</p>
 */
@WebMvcTest(FleetHealthController.class)
@Import(FleetHealthControllerTest.TestSecurityConfig.class)
class FleetHealthControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/fleet/health/summary").permitAll()
                            .anyRequest().authenticated())
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

    @MockBean FleetHealthService fleetHealthService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTAINER_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/fleet/health";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── getFleetHealthSummary ────────────────────────────────────────────

    @Test
    void getFleetHealthSummary_200() throws Exception {
        var summary = new FleetHealthSummaryResponse(5, 3, 1, 1, 0,
                45.5, 2_000_000_000L, 4_000_000_000L, Instant.now());
        when(fleetHealthService.getFleetHealthSummary(TEAM_ID)).thenReturn(summary);

        mockMvc.perform(get(BASE_URL + "/summary")
                        .param("teamId", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalContainers").value(5))
                .andExpect(jsonPath("$.runningContainers").value(3));
    }

    // ── checkContainerHealth ─────────────────────────────────────────────

    @Test
    void checkContainerHealth_200() throws Exception {
        when(fleetHealthService.checkContainerHealth(TEAM_ID, CONTAINER_ID))
                .thenReturn(buildHealthCheckResponse());

        mockMvc.perform(post(BASE_URL + "/containers/" + CONTAINER_ID + "/check")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HEALTHY"));
    }

    @Test
    void checkContainerHealth_unauthorized_401() throws Exception {
        mockMvc.perform(post(BASE_URL + "/containers/" + CONTAINER_ID + "/check")
                        .param("teamId", TEAM_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── checkAllContainerHealth ──────────────────────────────────────────

    @Test
    void checkAllContainerHealth_200() throws Exception {
        when(fleetHealthService.checkAllContainerHealth(TEAM_ID))
                .thenReturn(List.of(buildHealthCheckResponse()));

        mockMvc.perform(post(BASE_URL + "/containers/check-all")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("HEALTHY"));
    }

    // ── getHealthCheckHistory ────────────────────────────────────────────

    @Test
    void getHealthCheckHistory_200() throws Exception {
        when(fleetHealthService.getHealthCheckHistory(TEAM_ID, CONTAINER_ID, 20))
                .thenReturn(List.of(buildHealthCheckResponse()));

        mockMvc.perform(get(BASE_URL + "/containers/" + CONTAINER_ID + "/history")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].containerId").value(CONTAINER_ID.toString()));
    }

    // ── purgeOldHealthChecks ─────────────────────────────────────────────

    @Test
    void purgeOldHealthChecks_200() throws Exception {
        when(fleetHealthService.purgeOldHealthChecks(TEAM_ID)).thenReturn(42);

        mockMvc.perform(post(BASE_URL + "/purge")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ContainerHealthCheckResponse buildHealthCheckResponse() {
        return new ContainerHealthCheckResponse(UUID.randomUUID(), HealthStatus.HEALTHY,
                "OK", 0, 150L, CONTAINER_ID, Instant.now());
    }
}
