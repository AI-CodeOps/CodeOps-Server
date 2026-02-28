package com.codeops.mcp.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.dto.request.CreateApiTokenRequest;
import com.codeops.mcp.dto.request.UpdateDeveloperProfileRequest;
import com.codeops.mcp.dto.response.ApiTokenCreatedResponse;
import com.codeops.mcp.dto.response.ApiTokenResponse;
import com.codeops.mcp.dto.response.DeveloperProfileResponse;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.TokenStatus;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.mcp.service.DeveloperProfileService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link McpDeveloperController}.
 *
 * <p>Tests developer profile CRUD and API token lifecycle operations.</p>
 */
@WebMvcTest(McpDeveloperController.class)
@Import(McpDeveloperControllerTest.TestSecurityConfig.class)
class McpDeveloperControllerTest {

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

    @MockBean DeveloperProfileService developerProfileService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID TOKEN_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/mcp/developers";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private DeveloperProfile buildProfile() {
        DeveloperProfile profile = new DeveloperProfile();
        profile.setId(PROFILE_ID);
        return profile;
    }

    private DeveloperProfileResponse buildProfileResponse() {
        return new DeveloperProfileResponse(PROFILE_ID, "Adam", "Dev bio",
                Environment.LOCAL, null, "America/Chicago", true,
                TEAM_ID, USER_ID, "Adam Allard", Instant.now(), Instant.now());
    }

    // ── getOrCreateProfile ───────────────────────────────────────────────

    @Test
    void getOrCreateProfile_201() throws Exception {
        when(developerProfileService.getOrCreateProfile(TEAM_ID, USER_ID)).thenReturn(buildProfile());
        when(developerProfileService.getProfileById(PROFILE_ID)).thenReturn(buildProfileResponse());

        mockMvc.perform(post(BASE_URL + "/profile")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROFILE_ID.toString()));
    }

    // ── getProfile ───────────────────────────────────────────────────────

    @Test
    void getProfile_200() throws Exception {
        when(developerProfileService.getProfile(TEAM_ID, USER_ID)).thenReturn(buildProfileResponse());

        mockMvc.perform(get(BASE_URL + "/profile")
                        .param("teamId", TEAM_ID.toString())
                        .param("userId", USER_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Adam"));
    }

    // ── getTeamProfiles ──────────────────────────────────────────────────

    @Test
    void getTeamProfiles_200() throws Exception {
        when(developerProfileService.getTeamProfiles(TEAM_ID))
                .thenReturn(List.of(buildProfileResponse()));

        mockMvc.perform(get(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PROFILE_ID.toString()));
    }

    // ── updateProfile ────────────────────────────────────────────────────

    @Test
    void updateProfile_200() throws Exception {
        var request = new UpdateDeveloperProfileRequest("New Name", null, null, null, null);
        DeveloperProfileResponse updated = new DeveloperProfileResponse(PROFILE_ID, "New Name",
                "Dev bio", Environment.LOCAL, null, "America/Chicago", true,
                TEAM_ID, USER_ID, "Adam Allard", Instant.now(), Instant.now());
        when(developerProfileService.updateProfile(eq(PROFILE_ID), any())).thenReturn(updated);

        mockMvc.perform(put(BASE_URL + "/" + PROFILE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"));
    }

    // ── createToken ──────────────────────────────────────────────────────

    @Test
    void createToken_201() throws Exception {
        var request = new CreateApiTokenRequest("Test Token", null, null);
        var tokenResponse = new ApiTokenCreatedResponse(TOKEN_ID, "Test Token",
                "mcp_abcd", "rawTokenValue", TokenStatus.ACTIVE, null, null, Instant.now());
        when(developerProfileService.createApiToken(eq(PROFILE_ID), any())).thenReturn(tokenResponse);

        mockMvc.perform(post(BASE_URL + "/" + PROFILE_ID + "/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawToken").value("rawTokenValue"));
    }

    // ── getTokens ────────────────────────────────────────────────────────

    @Test
    void getTokens_200() throws Exception {
        var tokenResp = new ApiTokenResponse(TOKEN_ID, "Test Token", "mcp_abcd",
                TokenStatus.ACTIVE, null, null, null, Instant.now());
        when(developerProfileService.getTokens(PROFILE_ID)).thenReturn(List.of(tokenResp));

        mockMvc.perform(get(BASE_URL + "/" + PROFILE_ID + "/tokens")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Token"));
    }

    // ── revokeToken ──────────────────────────────────────────────────────

    @Test
    void revokeToken_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/tokens/" + TOKEN_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(developerProfileService).revokeToken(TOKEN_ID);
    }
}
