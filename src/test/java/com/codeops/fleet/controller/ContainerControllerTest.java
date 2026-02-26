package com.codeops.fleet.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.fleet.dto.request.ContainerExecRequest;
import com.codeops.fleet.dto.request.StartContainerRequest;
import com.codeops.fleet.dto.response.ContainerDetailResponse;
import com.codeops.fleet.dto.response.ContainerInstanceResponse;
import com.codeops.fleet.dto.response.ContainerStatsResponse;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.service.ContainerManagementService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link ContainerController}.
 *
 * <p>Covers container lifecycle operations (start, stop, restart, remove),
 * inspection, logs, stats, exec, listing, status filtering, and state sync.</p>
 */
@WebMvcTest(ContainerController.class)
@Import(ContainerControllerTest.TestSecurityConfig.class)
class ContainerControllerTest {

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

    @MockBean ContainerManagementService containerManagementService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTAINER_ID = UUID.randomUUID();
    private static final UUID SERVICE_PROFILE_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/fleet/containers";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── startContainer ───────────────────────────────────────────────────

    @Test
    void startContainer_201() throws Exception {
        var request = new StartContainerRequest(SERVICE_PROFILE_ID, null, null, null, null);
        when(containerManagementService.startContainer(eq(TEAM_ID), any())).thenReturn(buildDetailResponse());

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CONTAINER_ID.toString()));
    }

    @Test
    void startContainer_unauthorized_401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceProfileId\":\"" + SERVICE_PROFILE_ID + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── stopContainer ────────────────────────────────────────────────────

    @Test
    void stopContainer_200() throws Exception {
        when(containerManagementService.stopContainer(TEAM_ID, CONTAINER_ID, 10)).thenReturn(buildDetailResponse());

        mockMvc.perform(post(BASE_URL + "/" + CONTAINER_ID + "/stop")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CONTAINER_ID.toString()));
    }

    // ── restartContainer ─────────────────────────────────────────────────

    @Test
    void restartContainer_200() throws Exception {
        when(containerManagementService.restartContainer(TEAM_ID, CONTAINER_ID, 10)).thenReturn(buildDetailResponse());

        mockMvc.perform(post(BASE_URL + "/" + CONTAINER_ID + "/restart")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── removeContainer ──────────────────────────────────────────────────

    @Test
    void removeContainer_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + CONTAINER_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(containerManagementService).removeContainer(TEAM_ID, CONTAINER_ID, false);
    }

    // ── inspectContainer ─────────────────────────────────────────────────

    @Test
    void inspectContainer_200() throws Exception {
        when(containerManagementService.inspectContainer(TEAM_ID, CONTAINER_ID)).thenReturn(buildDetailResponse());

        mockMvc.perform(get(BASE_URL + "/" + CONTAINER_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.containerName").value("my-container"));
    }

    // ── getContainerLogs ─────────────────────────────────────────────────

    @Test
    void getContainerLogs_200() throws Exception {
        when(containerManagementService.getContainerLogs(TEAM_ID, CONTAINER_ID, 100, false))
                .thenReturn("log line 1\nlog line 2");

        mockMvc.perform(get(BASE_URL + "/" + CONTAINER_ID + "/logs")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(content().string("log line 1\nlog line 2"));
    }

    // ── getContainerStats ────────────────────────────────────────────────

    @Test
    void getContainerStats_200() throws Exception {
        var stats = new ContainerStatsResponse(CONTAINER_ID, "my-container", 25.5,
                512_000_000L, 1_073_741_824L, 1000L, 2000L, 500L, 300L, 5, Instant.now());
        when(containerManagementService.getContainerStats(TEAM_ID, CONTAINER_ID)).thenReturn(stats);

        mockMvc.perform(get(BASE_URL + "/" + CONTAINER_ID + "/stats")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpuPercent").value(25.5));
    }

    // ── execInContainer ──────────────────────────────────────────────────

    @Test
    void execInContainer_200() throws Exception {
        var request = new ContainerExecRequest("echo hello", true, false);
        when(containerManagementService.execInContainer(eq(TEAM_ID), eq(CONTAINER_ID), any()))
                .thenReturn("hello");

        mockMvc.perform(post(BASE_URL + "/" + CONTAINER_ID + "/exec")
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(content().string("hello"));
    }

    // ── listContainers ───────────────────────────────────────────────────

    @Test
    void listContainers_200() throws Exception {
        when(containerManagementService.listContainers(TEAM_ID)).thenReturn(List.of(buildInstanceResponse()));

        mockMvc.perform(get(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CONTAINER_ID.toString()));
    }

    // ── listContainersByStatus ───────────────────────────────────────────

    @Test
    void listContainersByStatus_200() throws Exception {
        when(containerManagementService.listContainersByStatus(TEAM_ID, ContainerStatus.RUNNING))
                .thenReturn(List.of(buildInstanceResponse()));

        mockMvc.perform(get(BASE_URL + "/by-status")
                        .param("teamId", TEAM_ID.toString())
                        .param("status", "RUNNING")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RUNNING"));
    }

    // ── syncContainerState ───────────────────────────────────────────────

    @Test
    void syncContainerState_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/sync")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(containerManagementService).syncContainerState(TEAM_ID);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ContainerDetailResponse buildDetailResponse() {
        return new ContainerDetailResponse(CONTAINER_ID, "abc123", "my-container",
                "test-service", "postgres", "16", ContainerStatus.RUNNING, HealthStatus.HEALTHY,
                RestartPolicy.UNLESS_STOPPED, 0, 0, 10.0, 512_000_000L, 1_073_741_824L,
                42, Instant.now(), null, SERVICE_PROFILE_ID, "test-service",
                TEAM_ID, Instant.now(), Instant.now());
    }

    private ContainerInstanceResponse buildInstanceResponse() {
        return new ContainerInstanceResponse(CONTAINER_ID, "abc123", "my-container",
                "test-service", "postgres", "16", ContainerStatus.RUNNING, HealthStatus.HEALTHY,
                RestartPolicy.UNLESS_STOPPED, 0, 10.0, 512_000_000L, 1_073_741_824L,
                Instant.now(), Instant.now());
    }
}
