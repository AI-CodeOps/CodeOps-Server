package com.codeops.mcp.entity.enums;

/**
 * Type of MCP activity feed entry.
 *
 * <p>Classifies events that appear in the team's MCP activity
 * feed for visibility into AI agent operations.</p>
 */
public enum ActivityType {
    /** AI session finished with results. */
    SESSION_COMPLETED,
    /** AI session ended with error. */
    SESSION_FAILED,
    /** Project document created or updated. */
    DOCUMENT_UPDATED,
    /** Team convention modified. */
    CONVENTION_CHANGED,
    /** Team directive modified. */
    DIRECTIVE_CHANGED,
    /** Cross-project impact flagged. */
    IMPACT_DETECTED
}
