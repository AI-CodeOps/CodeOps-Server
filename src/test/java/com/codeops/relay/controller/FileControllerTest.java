package com.codeops.relay.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.relay.dto.response.FileAttachmentResponse;
import com.codeops.relay.entity.enums.FileUploadStatus;
import com.codeops.relay.service.FileAttachmentService;
import com.codeops.relay.service.FileAttachmentService.FileDownloadResult;
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
import org.springframework.mock.web.MockMultipartFile;
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
 * Unit tests for {@link FileController}.
 *
 * <p>Covers file upload, download, listing, and deletion endpoints.</p>
 */
@WebMvcTest(FileController.class)
@Import(FileControllerTest.TestSecurityConfig.class)
class FileControllerTest {

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

    @MockBean FileAttachmentService fileAttachmentService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenValidator;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID ATTACHMENT_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/relay/files";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── uploadFile ────────────────────────────────────────────────────────

    @Test
    void uploadFile_201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "file content".getBytes());
        when(fileAttachmentService.uploadFile(eq(MESSAGE_ID), eq("test.txt"),
                eq("text/plain"), any(byte[].class), eq(USER_ID)))
                .thenReturn(buildAttachmentResponse());

        mockMvc.perform(multipart(BASE_URL + "/upload")
                        .file(file)
                        .param("messageId", MESSAGE_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ATTACHMENT_ID.toString()));
    }

    @Test
    void uploadFile_unauthorized_401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "content".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/upload")
                        .file(file)
                        .param("messageId", MESSAGE_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── downloadFile ──────────────────────────────────────────────────────

    @Test
    void downloadFile_200() throws Exception {
        var result = new FileDownloadResult("test.txt", "text/plain", "file content".getBytes());
        when(fileAttachmentService.downloadFile(ATTACHMENT_ID, USER_ID)).thenReturn(result);

        mockMvc.perform(get(BASE_URL + "/" + ATTACHMENT_ID + "/download")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""))
                .andExpect(content().bytes("file content".getBytes()));
    }

    // ── getAttachmentsForMessage ──────────────────────────────────────────

    @Test
    void getAttachmentsForMessage_200() throws Exception {
        when(fileAttachmentService.getAttachmentsForMessage(MESSAGE_ID))
                .thenReturn(List.of(buildAttachmentResponse()));

        mockMvc.perform(get(BASE_URL + "/messages/" + MESSAGE_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("test.txt"));
    }

    // ── deleteAttachment ──────────────────────────────────────────────────

    @Test
    void deleteAttachment_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + ATTACHMENT_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(fileAttachmentService).deleteAttachment(ATTACHMENT_ID, USER_ID);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private FileAttachmentResponse buildAttachmentResponse() {
        return new FileAttachmentResponse(ATTACHMENT_ID, "test.txt", "text/plain",
                1024L, "/download/test.txt", null, FileUploadStatus.COMPLETE,
                USER_ID, Instant.now());
    }
}
