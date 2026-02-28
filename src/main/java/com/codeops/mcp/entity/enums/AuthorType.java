package com.codeops.mcp.entity.enums;

/**
 * Author type for document updates.
 *
 * <p>Distinguishes whether a project document was last updated
 * by a human developer or by an AI agent during an MCP session.</p>
 */
public enum AuthorType {
    /** Document updated by a human developer. */
    HUMAN,
    /** Document updated by an AI agent. */
    AI
}
