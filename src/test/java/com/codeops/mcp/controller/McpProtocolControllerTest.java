package com.codeops.mcp.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.dto.McpSessionContext;
import com.codeops.mcp.security.McpSecurityService;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.mcp.service.McpProtocolService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link McpProtocolController}.
 *
 * <p>Tests the synchronous JSON-RPC message endpoint with various
 * MCP protocol methods including initialize, tools/list, and tools/call.</p>
 */
@WebMvcTest(McpProtocolController.class)
@Import(McpProtocolControllerTest.TestSecurityConfig.class)
class McpProtocolControllerTest {

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

    @MockBean McpProtocolService mcpProtocolService;
    @MockBean McpSecurityService mcpSecurityService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/mcp/protocol";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── message: initialize ──────────────────────────────────────────────

    @Test
    void message_initialize_200() throws Exception {
        String jsonRpc = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"initialize\",\"params\":{}}";
        String response = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"protocolVersion\":\"2024-11-05\"}}";

        when(mcpSecurityService.buildContextFromJwt(eq(USER_ID), any()))
                .thenReturn(new McpSessionContext(null, null, USER_ID, null, List.of()));
        when(mcpProtocolService.handleRequest(eq(jsonRpc), any())).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRpc)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    // ── message: tools/list ──────────────────────────────────────────────

    @Test
    void message_toolsList_200() throws Exception {
        String jsonRpc = "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"method\":\"tools/list\",\"params\":{}}";
        String response = "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"result\":{\"tools\":[]}}";

        when(mcpSecurityService.buildContextFromJwt(eq(USER_ID), any()))
                .thenReturn(new McpSessionContext(null, null, USER_ID, null, List.of()));
        when(mcpProtocolService.handleRequest(eq(jsonRpc), any())).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRpc)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    // ── message: tools/call ──────────────────────────────────────────────

    @Test
    void message_toolsCall_200() throws Exception {
        String jsonRpc = "{\"jsonrpc\":\"2.0\",\"id\":\"3\",\"method\":\"tools/call\",\"params\":{\"name\":\"registry_listServices\",\"arguments\":{}}}";
        String response = "{\"jsonrpc\":\"2.0\",\"id\":\"3\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"{}\"}]}}";

        when(mcpSecurityService.buildContextFromJwt(eq(USER_ID), any()))
                .thenReturn(new McpSessionContext(null, null, USER_ID, null, List.of()));
        when(mcpProtocolService.handleRequest(eq(jsonRpc), any())).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRpc)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    // ── message: invalid JSON ────────────────────────────────────────────

    @Test
    void message_invalidJson_200() throws Exception {
        String invalidJson = "not json";
        String response = "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}";

        when(mcpSecurityService.buildContextFromJwt(eq(USER_ID), any()))
                .thenReturn(new McpSessionContext(null, null, USER_ID, null, List.of()));
        when(mcpProtocolService.handleRequest(eq(invalidJson), any())).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    // ── message: unauthenticated ─────────────────────────────────────────

    @Test
    void message_unauthenticated_401() throws Exception {
        mockMvc.perform(post(BASE_URL + "/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"initialize\"}"))
                .andExpect(status().isUnauthorized());
    }
}
