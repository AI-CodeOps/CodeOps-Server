package com.codeops.fleet.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.fleet.dto.response.DockerNetworkResponse;
import com.codeops.fleet.service.DockerEngineService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link NetworkController}.
 *
 * <p>Covers listing, creating, removing Docker networks, and connecting/disconnecting
 * containers from networks.</p>
 */
@WebMvcTest(NetworkController.class)
@Import(NetworkControllerTest.TestSecurityConfig.class)
class NetworkControllerTest {

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

    @MockBean DockerEngineService dockerEngineService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/fleet/networks";
    private static final String NETWORK_ID = "net-abc123";
    private static final String CONTAINER_DOCKER_ID = "ctr-xyz789";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── listNetworks ─────────────────────────────────────────────────────

    @Test
    void listNetworks_200() throws Exception {
        var network = new DockerNetworkResponse(NETWORK_ID, "codeops-network", "bridge",
                "172.18.0.0/16", "172.18.0.1", List.of());
        when(dockerEngineService.listNetworks()).thenReturn(List.of(network));

        mockMvc.perform(get(BASE_URL)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("codeops-network"));
    }

    @Test
    void listNetworks_unauthorized_401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    // ── createNetwork ────────────────────────────────────────────────────

    @Test
    void createNetwork_201() throws Exception {
        when(dockerEngineService.createNetwork("my-network", "bridge")).thenReturn(NETWORK_ID);

        mockMvc.perform(post(BASE_URL)
                        .param("name", "my-network")
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(content().string(NETWORK_ID));
    }

    // ── removeNetwork ────────────────────────────────────────────────────

    @Test
    void removeNetwork_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + NETWORK_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(dockerEngineService).removeNetwork(NETWORK_ID);
    }

    // ── connectContainerToNetwork ────────────────────────────────────────

    @Test
    void connectContainerToNetwork_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + NETWORK_ID + "/connect")
                        .param("containerId", CONTAINER_DOCKER_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(dockerEngineService).connectContainerToNetwork(NETWORK_ID, CONTAINER_DOCKER_ID, null);
    }

    // ── disconnectContainerFromNetwork ───────────────────────────────────

    @Test
    void disconnectContainerFromNetwork_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + NETWORK_ID + "/disconnect")
                        .param("containerId", CONTAINER_DOCKER_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(dockerEngineService).disconnectContainerFromNetwork(NETWORK_ID, CONTAINER_DOCKER_ID, false);
    }
}
