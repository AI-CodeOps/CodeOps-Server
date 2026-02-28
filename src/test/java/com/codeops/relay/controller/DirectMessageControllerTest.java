package com.codeops.relay.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.dto.response.PageResponse;
import com.codeops.relay.dto.request.CreateDirectConversationRequest;
import com.codeops.relay.dto.request.SendDirectMessageRequest;
import com.codeops.relay.dto.request.UpdateDirectMessageRequest;
import com.codeops.relay.dto.response.DirectConversationResponse;
import com.codeops.relay.dto.response.DirectConversationSummaryResponse;
import com.codeops.relay.dto.response.DirectMessageResponse;
import com.codeops.relay.entity.enums.ConversationType;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.service.DirectMessageService;
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
 * Unit tests for {@link DirectMessageController}.
 *
 * <p>Covers DM conversation management, direct message CRUD,
 * read receipts, and unread count endpoints.</p>
 */
@WebMvcTest(DirectMessageController.class)
@Import(DirectMessageControllerTest.TestSecurityConfig.class)
class DirectMessageControllerTest {

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

    @MockBean DirectMessageService directMessageService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RECIPIENT_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/relay/dm";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── getOrCreateConversation ───────────────────────────────────────────

    @Test
    void getOrCreateConversation_201() throws Exception {
        var request = new CreateDirectConversationRequest(List.of(RECIPIENT_ID), null);
        when(directMessageService.getOrCreateConversation(any(), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(buildConversationResponse());

        mockMvc.perform(post(BASE_URL + "/conversations")
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CONVERSATION_ID.toString()));
    }

    @Test
    void getOrCreateConversation_unauthorized_401() throws Exception {
        mockMvc.perform(post(BASE_URL + "/conversations")
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantIds\":[\"" + RECIPIENT_ID + "\"]}"))
                .andExpect(status().isUnauthorized());
    }

    // ── getConversation ───────────────────────────────────────────────────

    @Test
    void getConversation_200() throws Exception {
        when(directMessageService.getConversation(CONVERSATION_ID, USER_ID))
                .thenReturn(buildConversationResponse());

        mockMvc.perform(get(BASE_URL + "/conversations/" + CONVERSATION_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CONVERSATION_ID.toString()));
    }

    // ── getConversations ──────────────────────────────────────────────────

    @Test
    void getConversations_200() throws Exception {
        when(directMessageService.getConversations(TEAM_ID, USER_ID))
                .thenReturn(List.of(buildConversationSummary()));

        mockMvc.perform(get(BASE_URL + "/conversations")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CONVERSATION_ID.toString()));
    }

    // ── deleteConversation ────────────────────────────────────────────────

    @Test
    void deleteConversation_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/conversations/" + CONVERSATION_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(directMessageService).deleteConversation(CONVERSATION_ID, USER_ID);
    }

    // ── sendDirectMessage ─────────────────────────────────────────────────

    @Test
    void sendDirectMessage_201() throws Exception {
        var request = new SendDirectMessageRequest("Hello DM!");
        when(directMessageService.sendDirectMessage(eq(CONVERSATION_ID), any(), eq(USER_ID)))
                .thenReturn(buildDirectMessageResponse());

        mockMvc.perform(post(BASE_URL + "/conversations/" + CONVERSATION_ID + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(MESSAGE_ID.toString()));
    }

    // ── getMessages ───────────────────────────────────────────────────────

    @Test
    void getMessages_200() throws Exception {
        var page = new PageResponse<>(List.of(buildDirectMessageResponse()), 0, 50, 1, 1, true);
        when(directMessageService.getMessages(CONVERSATION_ID, USER_ID, 0, 50)).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/conversations/" + CONVERSATION_ID + "/messages")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── editDirectMessage ─────────────────────────────────────────────────

    @Test
    void editDirectMessage_200() throws Exception {
        var request = new UpdateDirectMessageRequest("Edited content");
        when(directMessageService.editDirectMessage(eq(MESSAGE_ID), any(), eq(USER_ID)))
                .thenReturn(buildDirectMessageResponse());

        mockMvc.perform(put(BASE_URL + "/messages/" + MESSAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── deleteDirectMessage ───────────────────────────────────────────────

    @Test
    void deleteDirectMessage_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/messages/" + MESSAGE_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(directMessageService).deleteDirectMessage(MESSAGE_ID, USER_ID);
    }

    // ── markConversationRead ──────────────────────────────────────────────

    @Test
    void markConversationRead_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/conversations/" + CONVERSATION_ID + "/read")
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(directMessageService).markConversationRead(CONVERSATION_ID, USER_ID);
    }

    // ── getUnreadCount ────────────────────────────────────────────────────

    @Test
    void getUnreadCount_200() throws Exception {
        when(directMessageService.getUnreadCount(CONVERSATION_ID, USER_ID)).thenReturn(3L);

        mockMvc.perform(get(BASE_URL + "/conversations/" + CONVERSATION_ID + "/unread")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private DirectConversationResponse buildConversationResponse() {
        return new DirectConversationResponse(CONVERSATION_ID, TEAM_ID,
                ConversationType.ONE_ON_ONE, null, List.of(USER_ID, RECIPIENT_ID),
                null, null, Instant.now(), Instant.now());
    }

    private DirectConversationSummaryResponse buildConversationSummary() {
        return new DirectConversationSummaryResponse(CONVERSATION_ID,
                ConversationType.ONE_ON_ONE, null, List.of(USER_ID, RECIPIENT_ID),
                List.of("Test User", "Recipient"), null, null, 0);
    }

    private DirectMessageResponse buildDirectMessageResponse() {
        return new DirectMessageResponse(MESSAGE_ID, CONVERSATION_ID, USER_ID,
                "Test User", "Hello DM!", MessageType.TEXT, false, null, false,
                List.of(), List.of(), Instant.now(), Instant.now());
    }
}
