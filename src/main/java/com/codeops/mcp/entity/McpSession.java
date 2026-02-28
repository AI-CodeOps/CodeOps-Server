package com.codeops.mcp.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.entity.Project;
import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.McpTransport;
import com.codeops.mcp.entity.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an MCP AI development session.
 *
 * <p>Tracks the full lifecycle of an AI agent working on a project,
 * from initialization through active work to completion or failure.
 * Records tool calls, timing, and links to the session result.</p>
 */
@Entity
@Table(name = "mcp_sessions",
        indexes = {
                @Index(name = "idx_mcp_session_profile_status",
                        columnList = "developer_profile_id, status"),
                @Index(name = "idx_mcp_session_project_created",
                        columnList = "project_id, created_at DESC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpSession extends BaseEntity {

    /** Current lifecycle state of this session. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status = SessionStatus.INITIALIZING;

    /** Target deployment environment for this session. */
    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false)
    private Environment environment;

    /** Protocol transport used by the AI agent. */
    @Enumerated(EnumType.STRING)
    @Column(name = "transport", nullable = false)
    private McpTransport transport;

    /** Timestamp when the session transitioned to ACTIVE. */
    @Column(name = "started_at")
    private Instant startedAt;

    /** Timestamp when the session completed or failed. */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Timestamp of the last tool call in this session. */
    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    /** Maximum session duration in minutes before automatic timeout. */
    @Builder.Default
    @Column(name = "timeout_minutes", nullable = false)
    private int timeoutMinutes = 120;

    /** Running count of tool calls made during this session. */
    @Builder.Default
    @Column(name = "total_tool_calls", nullable = false)
    private int totalToolCalls = 0;

    /** Error message when session status is FAILED or TIMED_OUT. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Developer profile that initiated this session. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_profile_id", nullable = false)
    private DeveloperProfile developerProfile;

    /** Project this session is working on. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Tool calls made during this session. */
    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionToolCall> toolCalls = new ArrayList<>();

    /** Result summary produced when the session completes. */
    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private SessionResult result;
}
