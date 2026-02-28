package com.codeops.mcp.entity.enums;

/**
 * Lifecycle state of an MCP AI development session.
 *
 * <p>Tracks a session from initial creation through active work
 * to completion, failure, timeout, or cancellation.</p>
 */
public enum SessionStatus {
    /** Session created, context being assembled. */
    INITIALIZING,
    /** AI agent actively working. */
    ACTIVE,
    /** Agent called completeSession, writeback in progress. */
    COMPLETING,
    /** Session finished successfully with results. */
    COMPLETED,
    /** Session ended with error. */
    FAILED,
    /** Session exceeded timeout limit. */
    TIMED_OUT,
    /** Session cancelled by developer or system. */
    CANCELLED
}
