package com.codeops.relay.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.dto.response.PageResponse;
import com.codeops.relay.dto.request.*;
import com.codeops.relay.dto.response.*;
import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.MemberRole;
import com.codeops.relay.service.ChannelService;
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
 * Unit tests for {@link ChannelController}.
 *
 * <p>Covers channel CRUD, membership operations, topic management,
 * archive/unarchive, and pinned message management.</p>
 */
@WebMvcTest(ChannelController.class)
@Import(ChannelControllerTest.TestSecurityConfig.class)
class ChannelControllerTest {

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

    @MockBean ChannelService channelService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/relay/channels";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── createChannel ─────────────────────────────────────────────────────

    @Test
    void createChannel_201() throws Exception {
        var request = new CreateChannelRequest("general", "General chat", ChannelType.PUBLIC, null);
        when(channelService.createChannel(any(), eq(TEAM_ID), eq(USER_ID))).thenReturn(buildChannelResponse());

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CHANNEL_ID.toString()))
                .andExpect(jsonPath("$.name").value("general"));
    }

    @Test
    void createChannel_invalidBody_400() throws Exception {
        var request = new CreateChannelRequest(null, null, null, null);

        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createChannel_unauthorized_401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\",\"channelType\":\"PUBLIC\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── getChannel ────────────────────────────────────────────────────────

    @Test
    void getChannel_200() throws Exception {
        when(channelService.getChannel(CHANNEL_ID, TEAM_ID, USER_ID)).thenReturn(buildChannelResponse());

        mockMvc.perform(get(BASE_URL + "/" + CHANNEL_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CHANNEL_ID.toString()));
    }

    // ── getChannels ───────────────────────────────────────────────────────

    @Test
    void getChannels_200() throws Exception {
        var page = new PageResponse<>(List.of(buildChannelSummary()), 0, 50, 1, 1, true);
        when(channelService.getChannelsByTeamPaged(TEAM_ID, USER_ID, 0, 50)).thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── updateChannel ─────────────────────────────────────────────────────

    @Test
    void updateChannel_200() throws Exception {
        var request = new UpdateChannelRequest("updated", "Updated desc", null);
        when(channelService.updateChannel(eq(CHANNEL_ID), any(), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(buildChannelResponse());

        mockMvc.perform(put(BASE_URL + "/" + CHANNEL_ID)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── deleteChannel ─────────────────────────────────────────────────────

    @Test
    void deleteChannel_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + CHANNEL_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(channelService).deleteChannel(CHANNEL_ID, TEAM_ID, USER_ID);
    }

    // ── archiveChannel ────────────────────────────────────────────────────

    @Test
    void archiveChannel_200() throws Exception {
        when(channelService.archiveChannel(CHANNEL_ID, TEAM_ID, USER_ID)).thenReturn(buildChannelResponse());

        mockMvc.perform(post(BASE_URL + "/" + CHANNEL_ID + "/archive")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── unarchiveChannel ──────────────────────────────────────────────────

    @Test
    void unarchiveChannel_200() throws Exception {
        when(channelService.unarchiveChannel(CHANNEL_ID, TEAM_ID, USER_ID)).thenReturn(buildChannelResponse());

        mockMvc.perform(post(BASE_URL + "/" + CHANNEL_ID + "/unarchive")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── updateTopic ───────────────────────────────────────────────────────

    @Test
    void updateTopic_200() throws Exception {
        var request = new UpdateChannelTopicRequest("New topic");
        when(channelService.updateTopic(eq(CHANNEL_ID), any(), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(buildChannelResponse());

        mockMvc.perform(patch(BASE_URL + "/" + CHANNEL_ID + "/topic")
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── joinChannel ───────────────────────────────────────────────────────

    @Test
    void joinChannel_200() throws Exception {
        when(channelService.joinChannel(CHANNEL_ID, TEAM_ID, USER_ID)).thenReturn(buildMemberResponse());

        mockMvc.perform(post(BASE_URL + "/" + CHANNEL_ID + "/join")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── leaveChannel ──────────────────────────────────────────────────────

    @Test
    void leaveChannel_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + CHANNEL_ID + "/leave")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(channelService).leaveChannel(CHANNEL_ID, TEAM_ID, USER_ID);
    }

    // ── inviteMember ──────────────────────────────────────────────────────

    @Test
    void inviteMember_201() throws Exception {
        var request = new InviteMemberRequest(TARGET_USER_ID, MemberRole.MEMBER);
        when(channelService.inviteMember(eq(CHANNEL_ID), any(), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(buildMemberResponse());

        mockMvc.perform(post(BASE_URL + "/" + CHANNEL_ID + "/members/invite")
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated());
    }

    // ── removeMember ──────────────────────────────────────────────────────

    @Test
    void removeMember_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + CHANNEL_ID + "/members/" + TARGET_USER_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(channelService).removeMember(CHANNEL_ID, TARGET_USER_ID, TEAM_ID, USER_ID);
    }

    // ── updateMemberRole ──────────────────────────────────────────────────

    @Test
    void updateMemberRole_200() throws Exception {
        var request = new UpdateMemberRoleRequest(MemberRole.ADMIN);
        when(channelService.updateMemberRole(eq(CHANNEL_ID), eq(TARGET_USER_ID), any(), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(buildMemberResponse());

        mockMvc.perform(put(BASE_URL + "/" + CHANNEL_ID + "/members/" + TARGET_USER_ID + "/role")
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── getMembers ────────────────────────────────────────────────────────

    @Test
    void getMembers_200() throws Exception {
        when(channelService.getMembers(CHANNEL_ID, TEAM_ID)).thenReturn(List.of(buildMemberResponse()));

        mockMvc.perform(get(BASE_URL + "/" + CHANNEL_ID + "/members")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(USER_ID.toString()));
    }

    // ── pinMessage ────────────────────────────────────────────────────────

    @Test
    void pinMessage_201() throws Exception {
        var request = new PinMessageRequest(MESSAGE_ID);
        when(channelService.pinMessage(eq(CHANNEL_ID), any(), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(buildPinnedMessageResponse());

        mockMvc.perform(post(BASE_URL + "/" + CHANNEL_ID + "/pins")
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated());
    }

    // ── unpinMessage ──────────────────────────────────────────────────────

    @Test
    void unpinMessage_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + CHANNEL_ID + "/pins/" + MESSAGE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(channelService).unpinMessage(CHANNEL_ID, MESSAGE_ID, TEAM_ID, USER_ID);
    }

    // ── getPinnedMessages ─────────────────────────────────────────────────

    @Test
    void getPinnedMessages_200() throws Exception {
        when(channelService.getPinnedMessages(CHANNEL_ID, TEAM_ID))
                .thenReturn(List.of(buildPinnedMessageResponse()));

        mockMvc.perform(get(BASE_URL + "/" + CHANNEL_ID + "/pins")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(MESSAGE_ID.toString()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ChannelResponse buildChannelResponse() {
        return new ChannelResponse(CHANNEL_ID, "general", "general", "General chat",
                "Welcome", ChannelType.PUBLIC, TEAM_ID, null, null, false,
                USER_ID, 1, Instant.now(), Instant.now());
    }

    private ChannelSummaryResponse buildChannelSummary() {
        return new ChannelSummaryResponse(CHANNEL_ID, "general", "general",
                ChannelType.PUBLIC, "Welcome", false, 1, 0, null);
    }

    private ChannelMemberResponse buildMemberResponse() {
        return new ChannelMemberResponse(UUID.randomUUID(), CHANNEL_ID, USER_ID,
                "Test User", MemberRole.MEMBER, false, null, Instant.now());
    }

    private PinnedMessageResponse buildPinnedMessageResponse() {
        return new PinnedMessageResponse(UUID.randomUUID(), MESSAGE_ID, CHANNEL_ID,
                null, USER_ID, Instant.now());
    }
}
