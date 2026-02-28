package com.codeops.mcp.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Captures the outcome of a completed MCP session.
 *
 * <p>Contains an AI-generated summary, code change metrics,
 * commit references, and test coverage data. Each session
 * has at most one result, created when the session completes.</p>
 */
@Entity
@Table(name = "mcp_session_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mcp_sr_session",
                columnNames = {"session_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResult extends BaseEntity {

    /** AI-generated summary of what was accomplished in the session. */
    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    /** JSON array of Git commit hashes produced during the session. */
    @Column(name = "commit_hashes_json", columnDefinition = "TEXT")
    private String commitHashesJson;

    /** JSON object describing files changed: {created:[], modified:[], deleted:[]}. */
    @Column(name = "files_changed_json", columnDefinition = "TEXT")
    private String filesChangedJson;

    /** JSON object describing endpoints changed: {added:[], modified:[], removed:[]}. */
    @Column(name = "endpoints_changed_json", columnDefinition = "TEXT")
    private String endpointsChangedJson;

    /** Number of test methods added during the session. */
    @Builder.Default
    @Column(name = "tests_added", nullable = false)
    private int testsAdded = 0;

    /** Code coverage percentage after the session. */
    @Column(name = "test_coverage")
    private Double testCoverage;

    /** Number of lines of code added during the session. */
    @Builder.Default
    @Column(name = "lines_added", nullable = false)
    private int linesAdded = 0;

    /** Number of lines of code removed during the session. */
    @Builder.Default
    @Column(name = "lines_removed", nullable = false)
    private int linesRemoved = 0;

    /** JSON object describing dependency changes. */
    @Column(name = "dependency_changes_json", columnDefinition = "TEXT")
    private String dependencyChangesJson;

    /** Total session duration in minutes. */
    @Builder.Default
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 0;

    /** Estimated total token usage during the session. */
    @Column(name = "token_usage")
    private Long tokenUsage;

    /** MCP session that produced this result. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private McpSession session;
}
