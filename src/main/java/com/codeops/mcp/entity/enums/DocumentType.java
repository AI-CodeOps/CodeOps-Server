package com.codeops.mcp.entity.enums;

/**
 * Type of project document managed by MCP.
 *
 * <p>Classifies documents that AI agents read and update
 * during development sessions.</p>
 */
public enum DocumentType {
    /** Project-specific AI instructions. */
    CLAUDE_MD,
    /** Coding conventions (team canonical + project extensions). */
    CONVENTIONS_MD,
    /** System architecture specification. */
    ARCHITECTURE_MD,
    /** Most recent codebase audit results. */
    AUDIT_MD,
    /** API specification for backend services. */
    OPENAPI_YAML,
    /** Team-defined custom document type. */
    CUSTOM
}
