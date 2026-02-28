package com.codeops.relay.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.dto.response.PageResponse;
import com.codeops.relay.dto.response.PlatformEventResponse;
import com.codeops.relay.entity.enums.PlatformEventType;
import com.codeops.relay.service.PlatformEventService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link PlatformEventController}.
 *
 * <p>Covers event listing, type filtering, entity lookups,
 * undelivered event queries, and retry operations.</p>
 */
@WebMvcTest(PlatformEventController.class)
@Import(PlatformEventControllerTest.TestSecurityConfig.class)
class PlatformEventControllerTest {

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

    @MockBean PlatformEventService platformEventService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID ENTITY_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/relay/events";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── getEventsForTeam ──────────────────────────────────────────────────

    @Test
    void getEventsForTeam_200() throws Exception {
        var page = new PageResponse<>(List.of(buildEventResponse()), 0, 50, 1, 1, true);
        when(platformEventService.getEventsForTeam(TEAM_ID, 0, 50)).thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getEventsForTeam_unauthorized_401() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("teamId", TEAM_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── getEvent ──────────────────────────────────────────────────────────

    @Test
    void getEvent_200() throws Exception {
        when(platformEventService.getEvent(EVENT_ID)).thenReturn(buildEventResponse());

        mockMvc.perform(get(BASE_URL + "/" + EVENT_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()));
    }

    // ── getEventsForTeamByType ────────────────────────────────────────────

    @Test
    void getEventsForTeamByType_200() throws Exception {
        var page = new PageResponse<>(List.of(buildEventResponse()), 0, 50, 1, 1, true);
        when(platformEventService.getEventsForTeamByType(
                TEAM_ID, PlatformEventType.DEPLOYMENT_COMPLETED, 0, 50)).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/type/DEPLOYMENT_COMPLETED")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── getEventsForEntity ────────────────────────────────────────────────

    @Test
    void getEventsForEntity_200() throws Exception {
        when(platformEventService.getEventsForEntity(ENTITY_ID))
                .thenReturn(List.of(buildEventResponse()));

        mockMvc.perform(get(BASE_URL + "/entity/" + ENTITY_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(EVENT_ID.toString()));
    }

    // ── getUndeliveredEvents ──────────────────────────────────────────────

    @Test
    void getUndeliveredEvents_200() throws Exception {
        when(platformEventService.getUndeliveredEvents(TEAM_ID))
                .thenReturn(List.of(buildEventResponse()));

        mockMvc.perform(get(BASE_URL + "/undelivered")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── retryDelivery ─────────────────────────────────────────────────────

    @Test
    void retryDelivery_200() throws Exception {
        when(platformEventService.retryDelivery(EVENT_ID, USER_ID)).thenReturn(buildEventResponse());

        mockMvc.perform(post(BASE_URL + "/" + EVENT_ID + "/retry")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()));
    }

    // ── retryAllUndelivered ───────────────────────────────────────────────

    @Test
    void retryAllUndelivered_200() throws Exception {
        when(platformEventService.retryAllUndelivered(TEAM_ID, USER_ID)).thenReturn(5);

        mockMvc.perform(post(BASE_URL + "/retry-all")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retriedCount").value(5));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private PlatformEventResponse buildEventResponse() {
        return new PlatformEventResponse(EVENT_ID, PlatformEventType.DEPLOYMENT_COMPLETED,
                TEAM_ID, "CodeOps-Server", ENTITY_ID, "Deployment completed",
                "Deployed v2.1.0", null, null, true, Instant.now(), Instant.now());
    }
}
