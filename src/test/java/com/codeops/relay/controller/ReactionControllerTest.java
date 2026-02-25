package com.codeops.relay.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.relay.dto.request.AddReactionRequest;
import com.codeops.relay.dto.response.ReactionResponse;
import com.codeops.relay.dto.response.ReactionSummaryResponse;
import com.codeops.relay.entity.enums.ReactionType;
import com.codeops.relay.service.ReactionService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link ReactionController}.
 *
 * <p>Covers reaction toggle, reaction listing, and user-specific
 * reaction queries.</p>
 */
@WebMvcTest(ReactionController.class)
@Import(ReactionControllerTest.TestSecurityConfig.class)
class ReactionControllerTest {

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

    @MockBean ReactionService reactionService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/relay/reactions";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── toggleReaction ────────────────────────────────────────────────────

    @Test
    void toggleReaction_200() throws Exception {
        var request = new AddReactionRequest("\uD83D\uDC4D");
        var response = new ReactionResponse(UUID.randomUUID(), USER_ID, "Test User",
                "\uD83D\uDC4D", ReactionType.EMOJI, Instant.now());
        when(reactionService.toggleReaction(eq(MESSAGE_ID), any(), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/messages/" + MESSAGE_ID + "/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emoji").value("\uD83D\uDC4D"));
    }

    @Test
    void toggleReaction_unauthorized_401() throws Exception {
        mockMvc.perform(post(BASE_URL + "/messages/" + MESSAGE_ID + "/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"\uD83D\uDC4D\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── getReactionsForMessage ─────────────────────────────────────────────

    @Test
    void getReactionsForMessage_200() throws Exception {
        var summary = new ReactionSummaryResponse("\uD83D\uDC4D", 2, false, List.of(USER_ID));
        when(reactionService.getReactionsForMessage(MESSAGE_ID)).thenReturn(List.of(summary));

        mockMvc.perform(get(BASE_URL + "/messages/" + MESSAGE_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].emoji").value("\uD83D\uDC4D"))
                .andExpect(jsonPath("$[0].count").value(2));
    }

    // ── getReactionsForMessageWithUser ─────────────────────────────────────

    @Test
    void getReactionsForMessageWithUser_200() throws Exception {
        var summary = new ReactionSummaryResponse("\uD83D\uDC4D", 2, true, List.of(USER_ID));
        when(reactionService.getReactionsForMessageWithUser(MESSAGE_ID, USER_ID))
                .thenReturn(List.of(summary));

        mockMvc.perform(get(BASE_URL + "/messages/" + MESSAGE_ID + "/mine")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentUserReacted").value(true));
    }
}
