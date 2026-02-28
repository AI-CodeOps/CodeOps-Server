package com.codeops.relay.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.dto.response.PageResponse;
import com.codeops.relay.dto.request.MarkReadRequest;
import com.codeops.relay.dto.request.SendMessageRequest;
import com.codeops.relay.dto.request.UpdateMessageRequest;
import com.codeops.relay.dto.response.*;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.service.MessageService;
import com.codeops.relay.service.ThreadService;
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
 * Unit tests for {@link MessageController}.
 *
 * <p>Covers message CRUD, thread retrieval, search, read receipts,
 * and unread count endpoints.</p>
 */
@WebMvcTest(MessageController.class)
@Import(MessageControllerTest.TestSecurityConfig.class)
class MessageControllerTest {

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

    @MockBean MessageService messageService;
    @MockBean ThreadService threadService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final String CHANNEL_BASE = "/api/v1/relay/channels/" + CHANNEL_ID + "/messages";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── sendMessage ───────────────────────────────────────────────────────

    @Test
    void sendMessage_201() throws Exception {
        var request = new SendMessageRequest("Hello!", null, null, null);
        when(messageService.sendMessage(eq(CHANNEL_ID), any(), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(buildMessageResponse());

        mockMvc.perform(post(CHANNEL_BASE)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(MESSAGE_ID.toString()));
    }

    @Test
    void sendMessage_invalidBody_400() throws Exception {
        var request = new SendMessageRequest(null, null, null, null);

        mockMvc.perform(post(CHANNEL_BASE)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_unauthorized_401() throws Exception {
        mockMvc.perform(post(CHANNEL_BASE)
                        .param("teamId", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── getChannelMessages ────────────────────────────────────────────────

    @Test
    void getChannelMessages_200() throws Exception {
        var page = new PageResponse<>(List.of(buildMessageResponse()), 0, 50, 1, 1, true);
        when(messageService.getChannelMessages(CHANNEL_ID, TEAM_ID, USER_ID, 0, 50)).thenReturn(page);

        mockMvc.perform(get(CHANNEL_BASE)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── getMessage ────────────────────────────────────────────────────────

    @Test
    void getMessage_200() throws Exception {
        when(messageService.getMessage(MESSAGE_ID)).thenReturn(buildMessageResponse());

        mockMvc.perform(get(CHANNEL_BASE + "/" + MESSAGE_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello world"));
    }

    // ── editMessage ───────────────────────────────────────────────────────

    @Test
    void editMessage_200() throws Exception {
        var request = new UpdateMessageRequest("Updated content");
        when(messageService.editMessage(eq(MESSAGE_ID), any(), eq(USER_ID)))
                .thenReturn(buildMessageResponse());

        mockMvc.perform(put(CHANNEL_BASE + "/" + MESSAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    // ── deleteMessage ─────────────────────────────────────────────────────

    @Test
    void deleteMessage_204() throws Exception {
        mockMvc.perform(delete(CHANNEL_BASE + "/" + MESSAGE_ID)
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(messageService).deleteMessage(MESSAGE_ID, USER_ID, TEAM_ID);
    }

    // ── getThreadReplies ──────────────────────────────────────────────────

    @Test
    void getThreadReplies_200() throws Exception {
        when(messageService.getThreadReplies(MESSAGE_ID, USER_ID)).thenReturn(List.of(buildMessageResponse()));

        mockMvc.perform(get(CHANNEL_BASE + "/" + MESSAGE_ID + "/thread")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(MESSAGE_ID.toString()));
    }

    // ── searchMessages ────────────────────────────────────────────────────

    @Test
    void searchMessages_200() throws Exception {
        var page = new PageResponse<>(List.of(buildMessageResponse()), 0, 50, 1, 1, true);
        when(messageService.searchMessages(CHANNEL_ID, "hello", TEAM_ID, USER_ID, 0, 50)).thenReturn(page);

        mockMvc.perform(get(CHANNEL_BASE + "/search")
                        .param("query", "hello")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── searchMessagesAcrossChannels ───────────────────────────────────────

    @Test
    void searchMessagesAcrossChannels_200() throws Exception {
        var result = new ChannelSearchResultResponse(MESSAGE_ID, CHANNEL_ID, "general",
                USER_ID, "Test User", "Hello", Instant.now());
        var page = new PageResponse<>(List.of(result), 0, 50, 1, 1, true);
        when(messageService.searchMessagesAcrossChannels("hello", TEAM_ID, USER_ID, 0, 50)).thenReturn(page);

        mockMvc.perform(get("/api/v1/relay/messages/search-all")
                        .param("query", "hello")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── markRead ──────────────────────────────────────────────────────────

    @Test
    void markRead_200() throws Exception {
        var request = new MarkReadRequest(MESSAGE_ID);
        var receipt = new ReadReceiptResponse(CHANNEL_ID, USER_ID, MESSAGE_ID, Instant.now());
        when(messageService.markRead(eq(CHANNEL_ID), any(), eq(USER_ID))).thenReturn(receipt);

        mockMvc.perform(post(CHANNEL_BASE + "/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelId").value(CHANNEL_ID.toString()));
    }

    // ── getUnreadCounts ───────────────────────────────────────────────────

    @Test
    void getUnreadCounts_200() throws Exception {
        var unread = new UnreadCountResponse(CHANNEL_ID, "general", "general", 5, Instant.now());
        when(messageService.getUnreadCounts(TEAM_ID, USER_ID)).thenReturn(List.of(unread));

        mockMvc.perform(get("/api/v1/relay/messages/unread-counts")
                        .param("teamId", TEAM_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unreadCount").value(5));
    }

    // ── getActiveThreads ──────────────────────────────────────────────────

    @Test
    void getActiveThreads_200() throws Exception {
        var thread = new MessageThreadResponse(MESSAGE_ID, CHANNEL_ID, 3, Instant.now(),
                USER_ID, List.of(USER_ID), List.of());
        when(threadService.getActiveThreads(CHANNEL_ID)).thenReturn(List.of(thread));

        mockMvc.perform(get("/api/v1/relay/channels/" + CHANNEL_ID + "/threads/active")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].replyCount").value(3));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private MessageResponse buildMessageResponse() {
        return new MessageResponse(MESSAGE_ID, CHANNEL_ID, USER_ID, "Test User",
                "Hello world", MessageType.TEXT, null, false, null, false, false,
                List.of(), null, List.of(), List.of(), 0, null,
                Instant.now(), Instant.now());
    }
}
