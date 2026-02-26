package com.codeops.fleet.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.fleet.dto.request.AddSolutionServiceRequest;
import com.codeops.fleet.dto.request.CreateSolutionProfileRequest;
import com.codeops.fleet.dto.request.UpdateSolutionProfileRequest;
import com.codeops.fleet.dto.response.SolutionProfileDetailResponse;
import com.codeops.fleet.dto.response.SolutionProfileResponse;
import com.codeops.fleet.dto.response.SolutionServiceResponse;
import com.codeops.fleet.service.SolutionProfileService;
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
 * Unit tests for {@link SolutionProfileController}.
 *
 * <p>Covers CRUD operations for solution profiles, service linking, and
 * solution-wide start/stop orchestration.</p>
 */
@WebMvcTest(SolutionProfileController.class)
@Import(SolutionProfileControllerTest.TestSecurityConfig.class)
class SolutionProfileControllerTest {

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

    @MockBean SolutionProfileService solutionProfileService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID SERVICE_PROFILE_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/fleet/solution-profiles";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── createSolutionProfile ────────────────────────────────────────────

    @Test
    void createSolutionProfile_201() throws Exception {
        var request = new CreateSolutionProfileRequest("Backend Stack", "All backend services", true);
        when(solutionProfileService.createSolutionProfile(eq(TEAM_ID), any())).thenReturn(buildDetailResponse());

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROFILE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Backend Stack"));
    }

    @Test
    void createSolutionProfile_invalidBody_400() throws Exception {
        var request = new CreateSolutionProfileRequest(null, null, null);

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSolutionProfile_unauthorized_401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── updateSolutionProfile ────────────────────────────────────────────

    @Test
    void updateSolutionProfile_200() throws Exception {
        var request = new UpdateSolutionProfileRequest("Updated", null, null);
        when(solutionProfileService.updateSolutionProfile(eq(TEAM_ID), eq(PROFILE_ID), any()))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(put(BASE_URL + "/" + PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── getSolutionProfile ───────────────────────────────────────────────

    @Test
    void getSolutionProfile_200() throws Exception {
        when(solutionProfileService.getSolutionProfile(TEAM_ID, PROFILE_ID)).thenReturn(buildDetailResponse());

        mockMvc.perform(get(BASE_URL + "/" + PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROFILE_ID.toString()));
    }

    // ── listSolutionProfiles ─────────────────────────────────────────────

    @Test
    void listSolutionProfiles_200() throws Exception {
        when(solutionProfileService.listSolutionProfiles(TEAM_ID)).thenReturn(List.of(buildSummaryResponse()));

        mockMvc.perform(get(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PROFILE_ID.toString()));
    }

    // ── deleteSolutionProfile ────────────────────────────────────────────

    @Test
    void deleteSolutionProfile_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(solutionProfileService).deleteSolutionProfile(TEAM_ID, PROFILE_ID);
    }

    // ── addService ───────────────────────────────────────────────────────

    @Test
    void addService_201() throws Exception {
        var request = new AddSolutionServiceRequest(SERVICE_PROFILE_ID, 0);
        var response = new SolutionServiceResponse(UUID.randomUUID(), 0,
                SERVICE_PROFILE_ID, "postgres-db", "postgres", true);
        when(solutionProfileService.addService(eq(TEAM_ID), eq(PROFILE_ID), any())).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/" + PROFILE_ID + "/services")
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceProfileId").value(SERVICE_PROFILE_ID.toString()));
    }

    // ── removeService ────────────────────────────────────────────────────

    @Test
    void removeService_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + PROFILE_ID + "/services/" + SERVICE_PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(solutionProfileService).removeService(TEAM_ID, PROFILE_ID, SERVICE_PROFILE_ID);
    }

    // ── startSolution ────────────────────────────────────────────────────

    @Test
    void startSolution_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + PROFILE_ID + "/start")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(solutionProfileService).startSolution(TEAM_ID, PROFILE_ID);
    }

    // ── stopSolution ─────────────────────────────────────────────────────

    @Test
    void stopSolution_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + PROFILE_ID + "/stop")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(solutionProfileService).stopSolution(TEAM_ID, PROFILE_ID);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private SolutionProfileDetailResponse buildDetailResponse() {
        return new SolutionProfileDetailResponse(PROFILE_ID, "Backend Stack",
                "All backend services", true, TEAM_ID, List.of(), Instant.now(), Instant.now());
    }

    private SolutionProfileResponse buildSummaryResponse() {
        return new SolutionProfileResponse(PROFILE_ID, "Backend Stack",
                "All backend services", true, 3, TEAM_ID, Instant.now());
    }
}
