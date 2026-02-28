package com.codeops.mcp.security;

import com.codeops.exception.AuthorizationException;
import com.codeops.mcp.dto.McpSessionContext;
import com.codeops.mcp.entity.McpApiToken;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.enums.SessionStatus;
import com.codeops.mcp.repository.McpApiTokenRepository;
import com.codeops.mcp.repository.McpSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security services for MCP: rate limiting, session authorization, tool access control.
 *
 * <p>Provides enforcement of concurrent session limits, per-session tool call rate
 * limiting (in-memory sliding window), scope-based tool access control, session
 * ownership validation, and context extraction from HTTP requests.</p>
 *
 * @see McpTokenAuthFilter
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpSecurityService {

    private final McpSessionRepository sessionRepository;
    private final McpApiTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;

    /** Sliding window rate limit buckets keyed by sessionId. */
    private final ConcurrentHashMap<UUID, List<Long>> rateLimitBuckets = new ConcurrentHashMap<>();

    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;

    // ══════════════════════════════════════════════════════════════
    //                    SESSION LIMITS
    // ══════════════════════════════════════════════════════════════

    /**
     * Checks if a developer has not exceeded the concurrent active session limit.
     *
     * <p>Counts ACTIVE sessions for the given developer profile and compares
     * against the maximum allowed concurrent sessions.</p>
     *
     * @param developerProfileId  the developer profile to check
     * @param maxConcurrentSessions the maximum allowed concurrent sessions
     * @return {@code true} if within limit, {@code false} if at or over limit
     */
    public boolean isWithinSessionLimit(UUID developerProfileId, int maxConcurrentSessions) {
        long activeSessions = sessionRepository.countByDeveloperProfileIdAndStatus(
                developerProfileId, SessionStatus.ACTIVE);
        return activeSessions < maxConcurrentSessions;
    }

    // ══════════════════════════════════════════════════════════════
    //                    RATE LIMITING
    // ══════════════════════════════════════════════════════════════

    /**
     * Checks if a session has not exceeded the tool call rate limit.
     *
     * <p>Uses an in-memory sliding window strategy. Each call records a timestamp,
     * and expired entries (older than 60 seconds) are pruned. If the count of
     * recent calls exceeds {@code maxCallsPerMinute}, returns {@code false}.</p>
     *
     * @param sessionId         the session to check
     * @param maxCallsPerMinute the maximum tool calls allowed per minute
     * @return {@code true} if within limit, {@code false} if over limit
     */
    public boolean isWithinRateLimit(UUID sessionId, int maxCallsPerMinute) {
        long now = System.currentTimeMillis();
        long cutoff = now - RATE_LIMIT_WINDOW_MS;

        List<Long> timestamps = rateLimitBuckets.compute(sessionId, (key, existing) -> {
            List<Long> bucket = existing != null ? existing : new ArrayList<>();
            // Prune expired entries
            bucket.removeIf(ts -> ts < cutoff);
            // Record this call
            bucket.add(now);
            return bucket;
        });

        return timestamps.size() <= maxCallsPerMinute;
    }

    // ══════════════════════════════════════════════════════════════
    //                    SCOPE CHECKING
    // ══════════════════════════════════════════════════════════════

    /**
     * Checks if a tool is allowed by the token's scope restrictions.
     *
     * <p>If scopes are empty or null, all tools are allowed. Otherwise,
     * the tool's category (prefix before the dot) must be present in the
     * allowed scopes list.</p>
     *
     * @param toolName      the fully qualified tool name (e.g., "registry.listServices")
     * @param allowedScopes the list of allowed scope categories (e.g., ["registry", "fleet"])
     * @return {@code true} if the tool is allowed, {@code false} if denied
     */
    public boolean isToolAllowed(String toolName, List<String> allowedScopes) {
        if (allowedScopes == null || allowedScopes.isEmpty()) {
            return true;
        }

        String category = toolName.contains(".")
                ? toolName.substring(0, toolName.indexOf('.'))
                : toolName;

        return allowedScopes.contains(category);
    }

    // ══════════════════════════════════════════════════════════════
    //                    SESSION OWNERSHIP
    // ══════════════════════════════════════════════════════════════

    /**
     * Validates that a session belongs to the authenticated developer profile.
     *
     * <p>Looks up the session by ID and verifies that its developer profile
     * matches the authenticated profile. Throws {@link AuthorizationException}
     * if the session does not exist or belongs to a different developer.</p>
     *
     * @param sessionId          the session to validate
     * @param developerProfileId the authenticated developer profile ID
     * @throws AuthorizationException if the session does not belong to the developer
     */
    public void validateSessionOwnership(UUID sessionId, UUID developerProfileId) {
        McpSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AuthorizationException(
                        "Session " + sessionId + " not found"));

        if (!session.getDeveloperProfile().getId().equals(developerProfileId)) {
            throw new AuthorizationException(
                    "Session " + sessionId + " does not belong to developer profile " + developerProfileId);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    CONTEXT EXTRACTION
    // ══════════════════════════════════════════════════════════════

    /**
     * Extracts the {@link McpSessionContext} from the current HTTP request.
     *
     * <p>The context is stored as a request attribute by {@link McpTokenAuthFilter}
     * during MCP API token authentication.</p>
     *
     * @param request the HTTP request
     * @return the MCP session context, or {@code null} if not present
     */
    public McpSessionContext getCurrentContext(HttpServletRequest request) {
        Object context = request.getAttribute(McpTokenAuthFilter.MCP_CONTEXT_ATTRIBUTE);
        return context instanceof McpSessionContext ctx ? ctx : null;
    }

    /**
     * Builds an {@link McpSessionContext} for JWT-authenticated requests.
     *
     * <p>Used when the Flutter UI accesses MCP endpoints via standard JWT
     * authentication. Creates a context with no session ID and no scope
     * restrictions (full access).</p>
     *
     * @param userId the authenticated user's UUID
     * @param teamId the team UUID from the request context
     * @return a new MCP session context with no session or scope restrictions
     */
    public McpSessionContext buildContextFromJwt(UUID userId, UUID teamId) {
        return new McpSessionContext(null, teamId, userId, null, List.of());
    }

    // ══════════════════════════════════════════════════════════════
    //                    TOKEN SCOPES
    // ══════════════════════════════════════════════════════════════

    /**
     * Resolves the allowed scopes from an MCP API token.
     *
     * <p>Hashes the raw token with SHA-256, looks up the token record, and
     * parses the JSON scopes array. Returns an empty list if the token has
     * no scope restrictions (meaning all tools are allowed).</p>
     *
     * @param rawToken the raw MCP token value (with {@code mcp_} prefix)
     * @return the list of allowed scope categories, or an empty list if unrestricted
     */
    public List<String> resolveTokenScopes(String rawToken) {
        String tokenHash = hashToken(rawToken);

        return tokenRepository.findByTokenHash(tokenHash)
                .map(McpApiToken::getScopesJson)
                .map(this::parseScopesJson)
                .orElse(Collections.emptyList());
    }

    // ══════════════════════════════════════════════════════════════
    //                    PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Hashes a raw token with SHA-256 (same algorithm as DeveloperProfileService).
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Parses a JSON array string into a list of scope strings.
     */
    private List<String> parseScopesJson(String scopesJson) {
        if (scopesJson == null || scopesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(scopesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse token scopes JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
