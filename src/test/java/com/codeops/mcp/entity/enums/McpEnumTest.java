package com.codeops.mcp.entity.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MCP enum types.
 *
 * <p>Verifies that each enum has the expected number of values
 * and contains all specified constants.</p>
 */
class McpEnumTest {

    @Test
    void sessionStatus_allValues() {
        assertThat(SessionStatus.values()).hasSize(7);
        assertThat(SessionStatus.values()).containsExactly(
                SessionStatus.INITIALIZING,
                SessionStatus.ACTIVE,
                SessionStatus.COMPLETING,
                SessionStatus.COMPLETED,
                SessionStatus.FAILED,
                SessionStatus.TIMED_OUT,
                SessionStatus.CANCELLED
        );
    }

    @Test
    void documentType_allValues() {
        assertThat(DocumentType.values()).hasSize(6);
        assertThat(DocumentType.values()).containsExactly(
                DocumentType.CLAUDE_MD,
                DocumentType.CONVENTIONS_MD,
                DocumentType.ARCHITECTURE_MD,
                DocumentType.AUDIT_MD,
                DocumentType.OPENAPI_YAML,
                DocumentType.CUSTOM
        );
    }

    @Test
    void toolCallStatus_allValues() {
        assertThat(ToolCallStatus.values()).hasSize(4);
        assertThat(ToolCallStatus.values()).containsExactly(
                ToolCallStatus.SUCCESS,
                ToolCallStatus.FAILURE,
                ToolCallStatus.TIMEOUT,
                ToolCallStatus.UNAUTHORIZED
        );
    }

    @Test
    void activityType_allValues() {
        assertThat(ActivityType.values()).hasSize(6);
        assertThat(ActivityType.values()).containsExactly(
                ActivityType.SESSION_COMPLETED,
                ActivityType.SESSION_FAILED,
                ActivityType.DOCUMENT_UPDATED,
                ActivityType.CONVENTION_CHANGED,
                ActivityType.DIRECTIVE_CHANGED,
                ActivityType.IMPACT_DETECTED
        );
    }

    @Test
    void tokenStatus_allValues() {
        assertThat(TokenStatus.values()).hasSize(3);
        assertThat(TokenStatus.values()).containsExactly(
                TokenStatus.ACTIVE,
                TokenStatus.REVOKED,
                TokenStatus.EXPIRED
        );
    }

    @Test
    void mcpTransport_allValues() {
        assertThat(McpTransport.values()).hasSize(3);
        assertThat(McpTransport.values()).containsExactly(
                McpTransport.SSE,
                McpTransport.HTTP,
                McpTransport.STDIO
        );
    }

    @Test
    void authorType_allValues() {
        assertThat(AuthorType.values()).hasSize(2);
        assertThat(AuthorType.values()).containsExactly(
                AuthorType.HUMAN,
                AuthorType.AI
        );
    }
}
