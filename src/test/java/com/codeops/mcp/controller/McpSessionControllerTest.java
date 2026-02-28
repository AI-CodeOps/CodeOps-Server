package com.codeops.mcp.controller;

import com.codeops.config.AppConstants;
import com.codeops.config.RequestCorrelationFilter;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.mcp.dto.request.CompleteSessionRequest;
import com.codeops.mcp.dto.request.InitSessionRequest;
import com.codeops.mcp.dto.response.McpSessionDetailResponse;
import com.codeops.mcp.dto.response.McpSessionResponse;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.McpTransport;
import com.codeops.mcp.entity.enums.SessionStatus;
import com.codeops.mcp.repository.SessionToolCallRepository;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.mcp.service.DeveloperProfileService;
import com.codeops.mcp.service.McpSessionService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link McpSessionController}.
 *
 * <p>Tests session lifecycle endpoints: init, complete, get, history,
 * my sessions, cancel, and tool call summary.</p>
 */
@WebMvcTest(McpSessionController.class)
@Import(McpSessionControllerTest.TestSecurityConfig.class)
class McpSessionControllerTest {

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
    @Autowired ObjectMapper objectMapper;

    @MockBean McpSessionService mcpSessionService;
    @MockBean DeveloperProfileService developerProfileService;
    @MockBean SessionToolCallRepository toolCallRepository;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/mcp/sessions";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private DeveloperProfile buildProfile() {
        DeveloperProfile profile = new DeveloperProfile();
        profile.setId(PROFILE_ID);
        return profile;
    }

    private McpSessionDetailResponse buildDetailResponse() {
        return new McpSessionDetailResponse(SESSION_ID, SessionStatus.ACTIVE,
                "Test Project", "Adam", Environment.LOCAL, McpTransport.HTTP,
                Instant.now(), null, Instant.now(), 120, 0, null,
                List.of(), null, Instant.now(), Instant.now());
    }

    private McpSessionResponse buildResponse() {
        return new McpSessionResponse(SESSION_ID, SessionStatus.ACTIVE,
                "Test Project", "Adam", Environment.LOCAL, McpTransport.HTTP,
                Instant.now(), null, 0, Instant.now());
    }

    // ── initSession ──────────────────────────────────────────────────────

    @Test
    void initSession_201() throws Exception {
        var request = new InitSessionRequest(PROJECT_ID, "LOCAL", McpTransport.HTTP, null);
        when(developerProfileService.getOrCreateProfile(TEAM_ID, USER_ID)).thenReturn(buildProfile());
        when(mcpSessionService.initSession(eq(PROFILE_ID), any())).thenReturn(buildDetailResponse());

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()));
    }

    @Test
    void initSession_invalid_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authentication(adminAuth())))
                .andExpect(status().isBadRequest());
    }

    // ── completeSession ──────────────────────────────────────────────────

    @Test
    void completeSession_200() throws Exception {
        var request = new CompleteSessionRequest("Summary", List.of("abc123"), null,
                null, 5, 90.0, 100, 10, null, 30, 10000L, null);
        var detail = new McpSessionDetailResponse(SESSION_ID, SessionStatus.COMPLETED,
                "Test Project", "Adam", Environment.LOCAL, McpTransport.HTTP,
                Instant.now(), Instant.now(), Instant.now(), 120, 3, null,
                List.of(), null, Instant.now(), Instant.now());
        when(mcpSessionService.completeSession(eq(SESSION_ID), any())).thenReturn(detail);

        mockMvc.perform(post(BASE_URL + "/" + SESSION_ID + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // ── getSession ───────────────────────────────────────────────────────

    @Test
    void getSession_200() throws Exception {
        when(mcpSessionService.getSession(SESSION_ID)).thenReturn(buildDetailResponse());

        mockMvc.perform(get(BASE_URL + "/" + SESSION_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()));
    }

    @Test
    void getSession_notFound_404() throws Exception {
        when(mcpSessionService.getSession(SESSION_ID))
                .thenThrow(new NotFoundException("McpSession", SESSION_ID));

        mockMvc.perform(get(BASE_URL + "/" + SESSION_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNotFound());
    }

    // ── getSessionHistory ────────────────────────────────────────────────

    @Test
    void getSessionHistory_200() throws Exception {
        when(mcpSessionService.getSessionHistory(PROJECT_ID, AppConstants.MCP_SESSION_HISTORY_LIMIT))
                .thenReturn(List.of(buildResponse()));

        mockMvc.perform(get(BASE_URL + "/history")
                        .param("projectId", PROJECT_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SESSION_ID.toString()));
    }

    // ── getMySessions ────────────────────────────────────────────────────

    @Test
    void getMySessions_200() throws Exception {
        when(developerProfileService.getOrCreateProfile(TEAM_ID, USER_ID)).thenReturn(buildProfile());
        Page<McpSessionResponse> page = new PageImpl<>(List.of(buildResponse()));
        when(mcpSessionService.getDeveloperSessions(eq(PROFILE_ID), any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/mine")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(SESSION_ID.toString()));
    }

    // ── cancelSession ────────────────────────────────────────────────────

    @Test
    void cancelSession_200() throws Exception {
        McpSessionResponse cancelled = new McpSessionResponse(SESSION_ID, SessionStatus.CANCELLED,
                "Test Project", "Adam", Environment.LOCAL, McpTransport.HTTP,
                Instant.now(), Instant.now(), 0, Instant.now());
        when(mcpSessionService.cancelSession(SESSION_ID)).thenReturn(cancelled);

        mockMvc.perform(post(BASE_URL + "/" + SESSION_ID + "/cancel")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ── getToolCalls ─────────────────────────────────────────────────────

    @Test
    void getToolCallSummary_200() throws Exception {
        List<Object[]> summary = new java.util.ArrayList<>();
        summary.add(new Object[]{"registry.listServices", 5L});
        when(toolCallRepository.getToolCallSummary(SESSION_ID)).thenReturn(summary);

        mockMvc.perform(get(BASE_URL + "/" + SESSION_ID + "/tool-calls")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].toolName").value("registry.listServices"))
                .andExpect(jsonPath("$[0].callCount").value(5));
    }
}
