package com.codeops.mcp.entity.enums;

/**
 * Lifecycle state of an MCP API token.
 *
 * <p>Tokens start as active and can be revoked manually
 * or expired automatically based on their expiration date.</p>
 */
public enum TokenStatus {
    /** Token is valid and can be used for authentication. */
    ACTIVE,
    /** Token was manually revoked. */
    REVOKED,
    /** Token has passed its expiration date. */
    EXPIRED
}
