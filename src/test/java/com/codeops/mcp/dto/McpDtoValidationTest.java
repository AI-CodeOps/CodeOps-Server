package com.codeops.mcp.dto;

import com.codeops.mcp.dto.request.*;
import com.codeops.mcp.entity.enums.DocumentType;
import com.codeops.mcp.entity.enums.McpTransport;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation tests for all MCP request DTOs.
 *
 * <p>Verifies Jakarta Validation constraints ({@code @NotBlank}, {@code @NotNull},
 * {@code @Size}) on every request record.</p>
 */
class McpDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ── InitSessionRequest ──

    @Nested
    class InitSessionRequestTests {

        @Test
        void valid_noViolations() {
            var req = new InitSessionRequest(UUID.randomUUID(), "DEVELOPMENT", McpTransport.SSE, null);
            Set<ConstraintViolation<InitSessionRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void missingProjectId_violates() {
            var req = new InitSessionRequest(null, "DEVELOPMENT", McpTransport.SSE, null);
            Set<ConstraintViolation<InitSessionRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("projectId"));
        }
    }

    // ── CompleteSessionRequest ──

    @Nested
    class CompleteSessionRequestTests {

        @Test
        void valid_withAllOptionalFields() {
            var req = new CompleteSessionRequest(
                    "Implemented auth module",
                    List.of("abc123"),
                    new CompleteSessionRequest.SessionFilesChanged(
                            List.of("Auth.java"), List.of("Config.java"), List.of()),
                    new CompleteSessionRequest.SessionEndpointsChanged(
                            List.of("/api/auth"), List.of(), List.of()),
                    5, 92.5, 200, 50, null, 45, 150000L, null
            );
            Set<ConstraintViolation<CompleteSessionRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void missingSummary_violates() {
            var req = new CompleteSessionRequest(
                    null, null, null, null, null, null, null, null, null, null, null, null
            );
            Set<ConstraintViolation<CompleteSessionRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("summary"));
        }

        @Test
        void blankSummary_violates() {
            var req = new CompleteSessionRequest(
                    "   ", null, null, null, null, null, null, null, null, null, null, null
            );
            Set<ConstraintViolation<CompleteSessionRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("summary"));
        }
    }

    // ── CreateApiTokenRequest ──

    @Nested
    class CreateApiTokenRequestTests {

        @Test
        void valid_noViolations() {
            var req = new CreateApiTokenRequest("Claude Code Laptop", null, null);
            Set<ConstraintViolation<CreateApiTokenRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void blankName_violates() {
            var req = new CreateApiTokenRequest("", null, null);
            Set<ConstraintViolation<CreateApiTokenRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }
    }

    // ── CreateProjectDocumentRequest ──

    @Nested
    class CreateProjectDocumentRequestTests {

        @Test
        void valid_noViolations() {
            var req = new CreateProjectDocumentRequest(
                    DocumentType.CLAUDE_MD, null, "# Project Instructions",
                    "Initial document", null
            );
            Set<ConstraintViolation<CreateProjectDocumentRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nullDocumentType_violates() {
            var req = new CreateProjectDocumentRequest(
                    null, null, "content", null, null
            );
            Set<ConstraintViolation<CreateProjectDocumentRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("documentType"));
        }

        @Test
        void blankContent_violates() {
            var req = new CreateProjectDocumentRequest(
                    DocumentType.CLAUDE_MD, null, "   ", null, null
            );
            Set<ConstraintViolation<CreateProjectDocumentRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }
    }

    // ── ToolCallRequest ──

    @Nested
    class ToolCallRequestTests {

        @Test
        void valid_noViolations() {
            var req = new ToolCallRequest("registry.listServices", "registry", "{\"teamId\":\"123\"}");
            Set<ConstraintViolation<ToolCallRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void blankToolName_violates() {
            var req = new ToolCallRequest("", "registry", null);
            Set<ConstraintViolation<ToolCallRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("toolName"));
        }
    }
}
