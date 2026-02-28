package com.codeops.mcp.controller;

import com.codeops.config.RequestCorrelationFilter;
import com.codeops.exception.NotFoundException;
import com.codeops.mcp.dto.request.CreateProjectDocumentRequest;
import com.codeops.mcp.dto.request.UpdateProjectDocumentRequest;
import com.codeops.mcp.dto.response.ProjectDocumentDetailResponse;
import com.codeops.mcp.dto.response.ProjectDocumentResponse;
import com.codeops.mcp.dto.response.ProjectDocumentVersionResponse;
import com.codeops.mcp.entity.enums.AuthorType;
import com.codeops.mcp.entity.enums.DocumentType;
import com.codeops.mcp.security.McpTokenAuthFilter;
import com.codeops.mcp.service.DocumentManagementService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
 * Unit tests for {@link McpDocumentController}.
 *
 * <p>Tests document CRUD operations, version retrieval, flagged documents,
 * and staleness flag clearing.</p>
 */
@WebMvcTest(McpDocumentController.class)
@Import(McpDocumentControllerTest.TestSecurityConfig.class)
class McpDocumentControllerTest {

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

    @MockBean DocumentManagementService documentManagementService;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean RateLimitFilter rateLimitFilter;
    @MockBean RequestCorrelationFilter requestCorrelationFilter;
    @MockBean McpTokenAuthFilter mcpTokenAuthFilter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/mcp/documents";

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private ProjectDocumentDetailResponse buildDetailResponse() {
        return new ProjectDocumentDetailResponse(DOCUMENT_ID, DocumentType.CLAUDE_MD,
                null, "# CLAUDE.md content", AuthorType.HUMAN, null, false, null,
                PROJECT_ID, "Adam Allard", List.of(), Instant.now(), Instant.now());
    }

    private ProjectDocumentResponse buildResponse() {
        return new ProjectDocumentResponse(DOCUMENT_ID, DocumentType.CLAUDE_MD,
                null, AuthorType.HUMAN, null, false, null, PROJECT_ID,
                "Adam Allard", Instant.now(), Instant.now());
    }

    // ── createDocument ───────────────────────────────────────────────────

    @Test
    void createDocument_201() throws Exception {
        var request = new CreateProjectDocumentRequest(DocumentType.CLAUDE_MD, null,
                "# Content", "Initial", null);
        when(documentManagementService.createDocument(eq(PROJECT_ID), any(), eq(USER_ID), eq(AuthorType.HUMAN)))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(post(BASE_URL)
                        .param("projectId", PROJECT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()));
    }

    // ── getProjectDocuments ──────────────────────────────────────────────

    @Test
    void getProjectDocuments_200() throws Exception {
        when(documentManagementService.getProjectDocuments(PROJECT_ID))
                .thenReturn(List.of(buildResponse()));

        mockMvc.perform(get(BASE_URL)
                        .param("projectId", PROJECT_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(DOCUMENT_ID.toString()));
    }

    // ── getDocumentByType ────────────────────────────────────────────────

    @Test
    void getDocument_byType_200() throws Exception {
        when(documentManagementService.getDocument(PROJECT_ID, DocumentType.CLAUDE_MD))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(get(BASE_URL + "/by-type")
                        .param("projectId", PROJECT_ID.toString())
                        .param("documentType", "CLAUDE_MD")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("CLAUDE_MD"));
    }

    // ── updateDocument ───────────────────────────────────────────────────

    @Test
    void updateDocument_200() throws Exception {
        var request = new UpdateProjectDocumentRequest("Updated content",
                "Updated description", null, null);
        when(documentManagementService.updateDocument(eq(DOCUMENT_ID), any(), eq(USER_ID), isNull()))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(put(BASE_URL + "/" + DOCUMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()));
    }

    // ── getVersions ──────────────────────────────────────────────────────

    @Test
    void getDocumentVersions_200() throws Exception {
        var versionResp = new ProjectDocumentVersionResponse(UUID.randomUUID(), 1,
                "Content v1", AuthorType.HUMAN, null, "Initial", "Adam", null, Instant.now());
        Page<ProjectDocumentVersionResponse> page = new PageImpl<>(List.of(versionResp));
        when(documentManagementService.getDocumentVersions(eq(DOCUMENT_ID), any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/" + DOCUMENT_ID + "/versions")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].versionNumber").value(1));
    }

    // ── getVersion ───────────────────────────────────────────────────────

    @Test
    void getDocumentVersion_200() throws Exception {
        var versionResp = new ProjectDocumentVersionResponse(UUID.randomUUID(), 1,
                "Content v1", AuthorType.HUMAN, null, "Initial", "Adam", null, Instant.now());
        when(documentManagementService.getDocumentVersion(DOCUMENT_ID, 1)).thenReturn(versionResp);

        mockMvc.perform(get(BASE_URL + "/" + DOCUMENT_ID + "/versions/1")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(1));
    }

    // ── deleteDocument ───────────────────────────────────────────────────

    @Test
    void deleteDocument_204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + DOCUMENT_ID)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(documentManagementService).deleteDocument(DOCUMENT_ID);
    }

    // ── getFlaggedDocuments ──────────────────────────────────────────────

    @Test
    void getFlaggedDocuments_200() throws Exception {
        var flagged = new ProjectDocumentResponse(DOCUMENT_ID, DocumentType.AUDIT_MD,
                null, AuthorType.AI, null, true, "3 sessions since update",
                PROJECT_ID, "Adam", Instant.now(), Instant.now());
        when(documentManagementService.getFlaggedDocuments(PROJECT_ID)).thenReturn(List.of(flagged));

        mockMvc.perform(get(BASE_URL + "/flagged")
                        .param("projectId", PROJECT_ID.toString())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isFlagged").value(true));
    }

    // ── clearFlag ────────────────────────────────────────────────────────

    @Test
    void clearFlag_204() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + DOCUMENT_ID + "/clear-flag")
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(documentManagementService).clearFlag(DOCUMENT_ID);
    }
}
