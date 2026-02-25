package com.codeops.relay.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.relay.dto.request.UpdatePresenceRequest;
import com.codeops.relay.dto.response.UserPresenceResponse;
import com.codeops.relay.entity.enums.PresenceStatus;
import com.codeops.relay.service.PresenceService;
import com.codeops.security.JwtAuthFilter;
import com.codeops.security.JwtTokenProvider;
import com.codeops.security.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link PresenceController}.
 *
 * <p>Covers presence updates, queries, DND management, offline
 * transitions, and presence count endpoints.</p>
 */
@WebMvcTest(PresenceController.class)
@Import(PresenceControllerTest.TestSecurityConfig.class)
class PresenceControllerTest {

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
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PresenceService presenceService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/relay/presence";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── updatePresence ────────────────────────────────────────────────────

    @Test
    void updatePresence_200() throws Exception {
        var request = new UpdatePresenceRequest(PresenceStatus.ONLINE, null);
        when(presenceService.updatePresence(any(), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(buildPresenceResponse(PresenceStatus.ONLINE));

        mockMvc.perform(put(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    void updatePresence_unauthorized_401() throws Exception {
        mockMvc.perform(put(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ONLINE\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── getPresence ───────────────────────────────────────────────────────

    @Test
    void getPresence_200() throws Exception {
        when(presenceService.getPresence(TEAM_ID, USER_ID))
                .thenReturn(buildPresenceResponse(PresenceStatus.ONLINE));

        mockMvc.perform(get(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    // ── getTeamPresence ───────────────────────────────────────────────────

    @Test
    void getTeamPresence_200() throws Exception {
        when(presenceService.getTeamPresence(TEAM_ID))
                .thenReturn(List.of(buildPresenceResponse(PresenceStatus.ONLINE)));

        mockMvc.perform(get(BASE_URL + "/team")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ONLINE"));
    }

    // ── getOnlineUsers ────────────────────────────────────────────────────

    @Test
    void getOnlineUsers_200() throws Exception {
        when(presenceService.getOnlineUsers(TEAM_ID))
                .thenReturn(List.of(buildPresenceResponse(PresenceStatus.ONLINE)));

        mockMvc.perform(get(BASE_URL + "/online")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── setDoNotDisturb ───────────────────────────────────────────────────

    @Test
    void setDoNotDisturb_200() throws Exception {
        when(presenceService.setDoNotDisturb(TEAM_ID, USER_ID, "Busy"))
                .thenReturn(buildPresenceResponse(PresenceStatus.DND));

        mockMvc.perform(post(BASE_URL + "/dnd")
                        .param("teamId", TEAM_ID.toString())
                        .param("statusMessage", "Busy")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DND"));
    }

    // ── clearDoNotDisturb ─────────────────────────────────────────────────

    @Test
    void clearDoNotDisturb_200() throws Exception {
        when(presenceService.clearDoNotDisturb(TEAM_ID, USER_ID))
                .thenReturn(buildPresenceResponse(PresenceStatus.ONLINE));

        mockMvc.perform(delete(BASE_URL + "/dnd")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── goOffline ─────────────────────────────────────────────────────────

    @Test
    void goOffline_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/offline")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(presenceService).goOffline(TEAM_ID, USER_ID);
    }

    // ── getPresenceCount ──────────────────────────────────────────────────

    @Test
    void getPresenceCount_200() throws Exception {
        when(presenceService.getPresenceCount(TEAM_ID))
                .thenReturn(Map.of(PresenceStatus.ONLINE, 5L, PresenceStatus.AWAY, 2L));

        mockMvc.perform(get(BASE_URL + "/count")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ONLINE").value(5));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private UserPresenceResponse buildPresenceResponse(PresenceStatus status) {
        return new UserPresenceResponse(USER_ID, "Test User", TEAM_ID, status,
                null, Instant.now(), Instant.now());
    }
}
