package com.codeops.mcp.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.mcp.entity.enums.ToolCallStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Records a single tool call made by an AI agent during an MCP session.
 *
 * <p>Captures the tool name, category, request/response payloads,
 * execution status, and timing for auditing and analytics.</p>
 */
@Entity
@Table(name = "mcp_session_tool_calls",
        indexes = {
                @Index(name = "idx_mcp_tc_session_called",
                        columnList = "session_id, called_at"),
                @Index(name = "idx_mcp_tc_tool_status",
                        columnList = "tool_name, status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionToolCall extends BaseEntity {

    /** Fully qualified tool name (e.g., "registry.listServices"). */
    @Column(name = "tool_name", nullable = false, length = 200)
    private String toolName;

    /** Tool category (e.g., "registry", "fleet", "logger"). */
    @Column(name = "tool_category", nullable = false, length = 100)
    private String toolCategory;

    /** JSON string of tool call arguments. */
    @Column(name = "request_json", columnDefinition = "TEXT")
    private String requestJson;

    /** JSON string of tool call result (truncated if large). */
    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    /** Result status of this tool call. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ToolCallStatus status;

    /** Execution time in milliseconds. */
    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    /** Error message on FAILURE or TIMEOUT. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Timestamp when the tool call was made. */
    @Column(name = "called_at", nullable = false)
    private Instant calledAt;

    /** MCP session this tool call belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private McpSession session;
}
