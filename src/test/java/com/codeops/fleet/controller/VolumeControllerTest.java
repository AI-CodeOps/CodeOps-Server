package com.codeops.fleet.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.fleet.dto.response.DockerVolumeResponse;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link VolumeController}.
 *
 * <p>Covers listing, creating, removing, and pruning Docker volumes.</p>
 */
@WebMvcTest(VolumeController.class)
@Import(VolumeControllerTest.TestSecurityConfig.class)
class VolumeControllerTest {

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
    private static final String BASE_URL = "/api/v1/fleet/volumes";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── listVolumes ──────────────────────────────────────────────────────

    @Test
    void listVolumes_200() throws Exception {
        var vol = new DockerVolumeResponse("pgdata", "local", "/var/lib/docker/volumes/pgdata/_data",
                Map.of(), Instant.now());
        when(dockerEngineService.listVolumes()).thenReturn(List.of(vol));

        mockMvc.perform(get(BASE_URL)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("pgdata"));
    }

    @Test
    void listVolumes_unauthorized_401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    // ── createVolume ─────────────────────────────────────────────────────

    @Test
    void createVolume_201() throws Exception {
        var vol = new DockerVolumeResponse("new-vol", "local", "/var/lib/docker/volumes/new-vol/_data",
                Map.of(), Instant.now());
        when(dockerEngineService.createVolume("new-vol", "local")).thenReturn(vol);

        mockMvc.perform(post(BASE_URL)
                        .param("name", "new-vol")
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("new-vol"));
    }

    // ── removeVolume ─────────────────────────────────────────────────────

    @Test
    void removeVolume_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/pgdata")
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(dockerEngineService).removeVolume("pgdata", false);
    }

    // ── pruneVolumes ─────────────────────────────────────────────────────

    @Test
    void pruneVolumes_200() throws Exception {
        when(dockerEngineService.pruneVolumes()).thenReturn(500_000_000L);

        mockMvc.perform(post(BASE_URL + "/prune")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(content().string("500000000"));
    }
}
