package com.codeops.fleet.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.fleet.dto.request.AddWorkstationSolutionRequest;
import com.codeops.fleet.dto.request.CreateWorkstationProfileRequest;
import com.codeops.fleet.dto.request.UpdateWorkstationProfileRequest;
import com.codeops.fleet.dto.response.WorkstationProfileDetailResponse;
import com.codeops.fleet.dto.response.WorkstationProfileResponse;
import com.codeops.fleet.dto.response.WorkstationSolutionResponse;
import com.codeops.fleet.service.WorkstationProfileService;
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
 * Unit tests for {@link WorkstationProfileController}.
 *
 * <p>Covers CRUD operations for workstation profiles, solution linking, and
 * workstation-wide start/stop orchestration.</p>
 */
@WebMvcTest(WorkstationProfileController.class)
@Import(WorkstationProfileControllerTest.TestSecurityConfig.class)
class WorkstationProfileControllerTest {

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

    @MockBean WorkstationProfileService workstationProfileService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID SOLUTION_PROFILE_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/fleet/workstation-profiles";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── createWorkstationProfile ─────────────────────────────────────────

    @Test
    void createWorkstationProfile_201() throws Exception {
        var request = new CreateWorkstationProfileRequest("Dev Setup", "My local dev environment", true);
        when(workstationProfileService.createWorkstationProfile(eq(TEAM_ID), any()))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROFILE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Dev Setup"));
    }

    @Test
    void createWorkstationProfile_invalidBody_400() throws Exception {
        var request = new CreateWorkstationProfileRequest(null, null, null);

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWorkstationProfile_unauthorized_401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── updateWorkstationProfile ─────────────────────────────────────────

    @Test
    void updateWorkstationProfile_200() throws Exception {
        var request = new UpdateWorkstationProfileRequest("Updated", null, null);
        when(workstationProfileService.updateWorkstationProfile(eq(TEAM_ID), eq(PROFILE_ID), any()))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(put(BASE_URL + "/" + PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── getWorkstationProfile ────────────────────────────────────────────

    @Test
    void getWorkstationProfile_200() throws Exception {
        when(workstationProfileService.getWorkstationProfile(TEAM_ID, PROFILE_ID))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(get(BASE_URL + "/" + PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROFILE_ID.toString()));
    }

    // ── listWorkstationProfiles ──────────────────────────────────────────

    @Test
    void listWorkstationProfiles_200() throws Exception {
        when(workstationProfileService.listWorkstationProfiles(TEAM_ID))
                .thenReturn(List.of(buildSummaryResponse()));

        mockMvc.perform(get(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PROFILE_ID.toString()));
    }

    // ── deleteWorkstationProfile ─────────────────────────────────────────

    @Test
    void deleteWorkstationProfile_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(workstationProfileService).deleteWorkstationProfile(TEAM_ID, PROFILE_ID);
    }

    // ── addSolution ──────────────────────────────────────────────────────

    @Test
    void addSolution_201() throws Exception {
        var request = new AddWorkstationSolutionRequest(SOLUTION_PROFILE_ID, 0, null);
        var response = new WorkstationSolutionResponse(UUID.randomUUID(), 0, null,
                SOLUTION_PROFILE_ID, "Backend Stack");
        when(workstationProfileService.addSolution(eq(TEAM_ID), eq(PROFILE_ID), any())).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/" + PROFILE_ID + "/solutions")
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solutionProfileId").value(SOLUTION_PROFILE_ID.toString()));
    }

    // ── removeSolution ───────────────────────────────────────────────────

    @Test
    void removeSolution_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + PROFILE_ID + "/solutions/" + SOLUTION_PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(workstationProfileService).removeSolution(TEAM_ID, PROFILE_ID, SOLUTION_PROFILE_ID);
    }

    // ── startWorkstation ─────────────────────────────────────────────────

    @Test
    void startWorkstation_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + PROFILE_ID + "/start")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(workstationProfileService).startWorkstation(TEAM_ID, PROFILE_ID);
    }

    // ── stopWorkstation ──────────────────────────────────────────────────

    @Test
    void stopWorkstation_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + PROFILE_ID + "/stop")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(workstationProfileService).stopWorkstation(TEAM_ID, PROFILE_ID);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private WorkstationProfileDetailResponse buildDetailResponse() {
        return new WorkstationProfileDetailResponse(PROFILE_ID, "Dev Setup",
                "My local dev environment", true, USER_ID, TEAM_ID, List.of(),
                Instant.now(), Instant.now());
    }

    private WorkstationProfileResponse buildSummaryResponse() {
        return new WorkstationProfileResponse(PROFILE_ID, "Dev Setup",
                "My local dev environment", true, 1, USER_ID, TEAM_ID, Instant.now());
    }
}
