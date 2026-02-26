package com.codeops.fleet.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.fleet.dto.request.CreateServiceProfileRequest;
import com.codeops.fleet.dto.request.UpdateServiceProfileRequest;
import com.codeops.fleet.dto.response.ServiceProfileDetailResponse;
import com.codeops.fleet.dto.response.ServiceProfileResponse;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.service.ServiceProfileService;
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
 * Unit tests for {@link ServiceProfileController}.
 *
 * <p>Covers CRUD operations for service profiles and auto-generation from Registry.</p>
 */
@WebMvcTest(ServiceProfileController.class)
@Import(ServiceProfileControllerTest.TestSecurityConfig.class)
class ServiceProfileControllerTest {

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

    @MockBean ServiceProfileService serviceProfileService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID REGISTRATION_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/fleet/service-profiles";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── createServiceProfile ─────────────────────────────────────────────

    @Test
    void createServiceProfile_201() throws Exception {
        var request = new CreateServiceProfileRequest("postgres-db", "PostgreSQL", "Database service",
                "postgres", "16", null, null, null, null, null, null, null, null,
                RestartPolicy.UNLESS_STOPPED, 512, 1.0, 0, null);
        when(serviceProfileService.createServiceProfile(eq(TEAM_ID), any())).thenReturn(buildDetailResponse());

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROFILE_ID.toString()))
                .andExpect(jsonPath("$.serviceName").value("postgres-db"));
    }

    @Test
    void createServiceProfile_invalidBody_400() throws Exception {
        var request = new CreateServiceProfileRequest(null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createServiceProfile_unauthorized_401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceName\":\"test\",\"imageName\":\"alpine\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── updateServiceProfile ─────────────────────────────────────────────

    @Test
    void updateServiceProfile_200() throws Exception {
        var request = new UpdateServiceProfileRequest("Updated Name", null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(serviceProfileService.updateServiceProfile(eq(TEAM_ID), eq(PROFILE_ID), any()))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(put(BASE_URL + "/" + PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── getServiceProfile ────────────────────────────────────────────────

    @Test
    void getServiceProfile_200() throws Exception {
        when(serviceProfileService.getServiceProfile(TEAM_ID, PROFILE_ID)).thenReturn(buildDetailResponse());

        mockMvc.perform(get(BASE_URL + "/" + PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROFILE_ID.toString()));
    }

    // ── listServiceProfiles ──────────────────────────────────────────────

    @Test
    void listServiceProfiles_200() throws Exception {
        when(serviceProfileService.listServiceProfiles(TEAM_ID)).thenReturn(List.of(buildSummaryResponse()));

        mockMvc.perform(get(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PROFILE_ID.toString()));
    }

    // ── deleteServiceProfile ─────────────────────────────────────────────

    @Test
    void deleteServiceProfile_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + PROFILE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(serviceProfileService).deleteServiceProfile(TEAM_ID, PROFILE_ID);
    }

    // ── autoGenerateFromRegistry ─────────────────────────────────────────

    @Test
    void autoGenerateFromRegistry_201() throws Exception {
        when(serviceProfileService.autoGenerateFromRegistry(TEAM_ID, REGISTRATION_ID))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(post(BASE_URL + "/auto-generate")
                        .param("teamId", TEAM_ID.toString())
                        .param("serviceRegistrationId", REGISTRATION_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROFILE_ID.toString()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ServiceProfileDetailResponse buildDetailResponse() {
        return new ServiceProfileDetailResponse(PROFILE_ID, "postgres-db", "PostgreSQL",
                "Database service", "postgres", "16", null, null, null, null, null,
                null, null, null, RestartPolicy.UNLESS_STOPPED, 512, 1.0, false, true, 0,
                null, TEAM_ID, List.of(), List.of(), Instant.now(), Instant.now());
    }

    private ServiceProfileResponse buildSummaryResponse() {
        return new ServiceProfileResponse(PROFILE_ID, "postgres-db", "PostgreSQL",
                "postgres", "16", RestartPolicy.UNLESS_STOPPED, false, true, 0,
                null, Instant.now());
    }
}
