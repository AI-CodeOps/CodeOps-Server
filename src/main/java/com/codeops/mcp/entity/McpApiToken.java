package com.codeops.mcp.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.mcp.entity.enums.TokenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * API token for authenticating MCP client connections.
 *
 * <p>Stores a SHA-256 hash of the actual token (never the plaintext),
 * along with a display prefix for identification. Tokens can be
 * scoped to specific tool categories and have optional expiration.</p>
 */
@Entity
@Table(name = "mcp_api_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mcp_token_hash",
                columnNames = {"token_hash"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpApiToken extends BaseEntity {

    /** Human-readable token name (e.g., "Claude Code Laptop"). */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** SHA-256 hash of the actual token value. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 500)
    private String tokenHash;

    /** First 8 characters of the token for display (e.g., "mcp_a1b2..."). */
    @Column(name = "token_prefix", nullable = false, length = 20)
    private String tokenPrefix;

    /** Current lifecycle state of this token. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TokenStatus status = TokenStatus.ACTIVE;

    /** Timestamp of the last successful authentication using this token. */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /** Expiration timestamp; null means the token never expires. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** JSON array of allowed tool categories (null = all categories). */
    @Column(name = "scopes_json", columnDefinition = "TEXT")
    private String scopesJson;

    /** Developer profile that owns this token. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_profile_id", nullable = false)
    private DeveloperProfile developerProfile;
}
