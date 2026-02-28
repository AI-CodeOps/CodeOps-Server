package com.codeops.mcp.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.mcp.dto.mapper.DeveloperProfileMapper;
import com.codeops.mcp.dto.mapper.McpApiTokenMapper;
import com.codeops.mcp.dto.request.CreateApiTokenRequest;
import com.codeops.mcp.dto.request.CreateDeveloperProfileRequest;
import com.codeops.mcp.dto.request.UpdateDeveloperProfileRequest;
import com.codeops.mcp.dto.response.ApiTokenCreatedResponse;
import com.codeops.mcp.dto.response.ApiTokenResponse;
import com.codeops.mcp.dto.response.DeveloperProfileResponse;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.entity.McpApiToken;
import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.TokenStatus;
import com.codeops.mcp.repository.DeveloperProfileRepository;
import com.codeops.mcp.repository.McpApiTokenRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Manages MCP developer profiles and API token lifecycle.
 *
 * <p>Handles developer identity within the MCP module: profile creation
 * and management, API token generation with SHA-256 hashing, token
 * validation for MCP authentication, and scheduled token expiry.</p>
 *
 * <p>API tokens use a hash-only storage model — the raw token is returned
 * exactly once at creation time and never persisted. Tokens are validated
 * by hashing the incoming token and comparing against stored hashes.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeveloperProfileService {

    private final DeveloperProfileRepository profileRepository;
    private final McpApiTokenRepository tokenRepository;
    private final DeveloperProfileMapper profileMapper;
    private final McpApiTokenMapper tokenMapper;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ObjectMapper objectMapper;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Creates a developer profile for a user within a team.
     *
     * <p>Validates that the user is a team member and that no profile
     * already exists for the (team, user) pair.</p>
     *
     * @param teamId  the team ID
     * @param userId  the user ID
     * @param request the profile creation request
     * @return the created profile response
     * @throws NotFoundException   if the user is not found
     * @throws ValidationException if a profile already exists or user isn't a team member
     */
    @Transactional
    public DeveloperProfileResponse createProfile(UUID teamId, UUID userId,
                                                    CreateDeveloperProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new ValidationException("User " + userId + " is not a member of team " + teamId);
        }

        if (profileRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new ValidationException("Developer profile already exists for user "
                    + userId + " in team " + teamId);
        }

        Team team = new Team();
        team.setId(teamId);

        Environment defaultEnv = null;
        if (request.defaultEnvironment() != null && !request.defaultEnvironment().isBlank()) {
            defaultEnv = Environment.valueOf(request.defaultEnvironment());
        }

        DeveloperProfile profile = DeveloperProfile.builder()
                .displayName(request.displayName() != null ? request.displayName() : user.getDisplayName())
                .bio(request.bio())
                .defaultEnvironment(defaultEnv)
                .preferencesJson(request.preferencesJson())
                .timezone(request.timezone())
                .team(team)
                .user(user)
                .build();

        profile = profileRepository.save(profile);
        log.info("Created developer profile {} for user {} in team {}", profile.getId(), userId, teamId);
        return profileMapper.toResponse(profile);
    }

    /**
     * Gets a developer profile by team and user.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return the profile response
     * @throws NotFoundException if no profile exists for the team/user
     */
    @Transactional(readOnly = true)
    public DeveloperProfileResponse getProfile(UUID teamId, UUID userId) {
        DeveloperProfile profile = profileRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new NotFoundException("DeveloperProfile", "teamId/userId",
                        teamId + "/" + userId));
        return profileMapper.toResponse(profile);
    }

    /**
     * Gets a developer profile by its ID.
     *
     * @param profileId the profile ID
     * @return the profile response
     * @throws NotFoundException if the profile is not found
     */
    @Transactional(readOnly = true)
    public DeveloperProfileResponse getProfileById(UUID profileId) {
        DeveloperProfile profile = findProfile(profileId);
        return profileMapper.toResponse(profile);
    }

    /**
     * Gets all active profiles for a team.
     *
     * @param teamId the team ID
     * @return list of active profile responses
     */
    @Transactional(readOnly = true)
    public List<DeveloperProfileResponse> getTeamProfiles(UUID teamId) {
        List<DeveloperProfile> profiles = profileRepository.findByTeamIdAndIsActiveTrue(teamId);
        return profileMapper.toResponseList(profiles);
    }

    /**
     * Updates a developer profile's fields.
     *
     * <p>Only non-null fields in the request are applied.</p>
     *
     * @param profileId the profile to update
     * @param request   the update request
     * @return the updated profile response
     * @throws NotFoundException if the profile is not found
     */
    @Transactional
    public DeveloperProfileResponse updateProfile(UUID profileId,
                                                    UpdateDeveloperProfileRequest request) {
        DeveloperProfile profile = findProfile(profileId);

        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.defaultEnvironment() != null) {
            profile.setDefaultEnvironment(Environment.valueOf(request.defaultEnvironment()));
        }
        if (request.preferencesJson() != null) {
            profile.setPreferencesJson(request.preferencesJson());
        }
        if (request.timezone() != null) {
            profile.setTimezone(request.timezone());
        }

        profile = profileRepository.save(profile);
        log.info("Updated developer profile {}", profileId);
        return profileMapper.toResponse(profile);
    }

    /**
     * Deactivates a developer profile (soft delete).
     *
     * @param profileId the profile to deactivate
     * @throws NotFoundException if the profile is not found
     */
    @Transactional
    public void deactivateProfile(UUID profileId) {
        DeveloperProfile profile = findProfile(profileId);
        profile.setActive(false);
        profileRepository.save(profile);
        log.info("Deactivated developer profile {}", profileId);
    }

    /**
     * Creates an API token for MCP connections.
     *
     * <p>Generates a cryptographically random token, hashes it with SHA-256,
     * and stores only the hash. The raw token is returned in the response
     * and can never be retrieved again.</p>
     *
     * @param profileId the developer profile to create the token for
     * @param request   the token creation request
     * @return the created token response including the raw token (one-time only)
     * @throws NotFoundException if the profile is not found
     */
    @Transactional
    public ApiTokenCreatedResponse createApiToken(UUID profileId, CreateApiTokenRequest request) {
        DeveloperProfile profile = findProfile(profileId);

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        String tokenPrefix = AppConstants.MCP_TOKEN_PREFIX
                + rawToken.substring(0, Math.min(AppConstants.MCP_TOKEN_PREFIX_LENGTH, rawToken.length()));

        String scopesJson = null;
        if (request.scopes() != null && !request.scopes().isEmpty()) {
            scopesJson = serializeJson(request.scopes());
        }

        McpApiToken token = McpApiToken.builder()
                .name(request.name())
                .tokenHash(tokenHash)
                .tokenPrefix(tokenPrefix)
                .status(TokenStatus.ACTIVE)
                .expiresAt(request.expiresAt())
                .scopesJson(scopesJson)
                .developerProfile(profile)
                .build();

        token = tokenRepository.save(token);

        log.info("Created API token '{}' (prefix: {}) for profile {}",
                request.name(), tokenPrefix, profileId);

        return new ApiTokenCreatedResponse(
                token.getId(),
                token.getName(),
                token.getTokenPrefix(),
                rawToken,
                token.getStatus(),
                token.getExpiresAt(),
                token.getScopesJson(),
                token.getCreatedAt());
    }

    /**
     * Gets all tokens for a developer profile (hashes excluded).
     *
     * @param profileId the developer profile ID
     * @return list of token responses
     * @throws NotFoundException if the profile is not found
     */
    @Transactional(readOnly = true)
    public List<ApiTokenResponse> getTokens(UUID profileId) {
        findProfile(profileId); // verify exists
        List<McpApiToken> tokens = tokenRepository.findByDeveloperProfileId(profileId);
        return tokenMapper.toResponseList(tokens);
    }

    /**
     * Revokes an API token.
     *
     * @param tokenId the token to revoke
     * @throws NotFoundException if the token is not found
     */
    @Transactional
    public void revokeToken(UUID tokenId) {
        McpApiToken token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new NotFoundException("McpApiToken", tokenId));

        token.setStatus(TokenStatus.REVOKED);
        tokenRepository.save(token);

        log.info("Revoked API token {} (prefix: {})", tokenId, token.getTokenPrefix());
    }

    /**
     * Validates a raw API token and returns the associated developer profile.
     *
     * <p>Hashes the incoming token, looks up by hash, and verifies the token
     * is ACTIVE and not expired. Updates lastUsedAt on successful validation.</p>
     *
     * @param rawToken the raw token value to validate
     * @return the developer profile associated with the valid token
     * @throws AuthorizationException if the token is invalid, revoked, or expired
     */
    @Transactional
    public DeveloperProfile validateToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        McpApiToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthorizationException("Invalid API token"));

        if (token.getStatus() == TokenStatus.REVOKED) {
            throw new AuthorizationException("API token has been revoked");
        }

        if (token.getStatus() == TokenStatus.EXPIRED) {
            throw new AuthorizationException("API token has expired");
        }

        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
            token.setStatus(TokenStatus.EXPIRED);
            tokenRepository.save(token);
            throw new AuthorizationException("API token has expired");
        }

        token.setLastUsedAt(Instant.now());
        tokenRepository.save(token);

        return token.getDeveloperProfile();
    }

    /**
     * Expires tokens that have passed their expiration date.
     *
     * <p>Called hourly by the Spring scheduler. Uses a bulk update query
     * to set ACTIVE tokens past their expiresAt to EXPIRED.</p>
     *
     * @return the number of tokens expired
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public int expireTokens() {
        int count = tokenRepository.expireTokens(Instant.now(), TokenStatus.EXPIRED);
        if (count > 0) {
            log.info("Expired {} MCP API tokens", count);
        }
        return count;
    }

    /**
     * Gets an existing profile or creates one with defaults.
     *
     * <p>Convenience method for session initialization — if no profile exists
     * for the (team, user) pair, one is auto-created using defaults from
     * the User entity.</p>
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return the developer profile (existing or newly created)
     * @throws NotFoundException if the user is not found
     */
    @Transactional
    public DeveloperProfile getOrCreateProfile(UUID teamId, UUID userId) {
        return profileRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("User", userId));

                    Team team = new Team();
                    team.setId(teamId);

                    DeveloperProfile profile = DeveloperProfile.builder()
                            .displayName(user.getDisplayName())
                            .team(team)
                            .user(user)
                            .build();

                    profile = profileRepository.save(profile);
                    log.info("Auto-created developer profile {} for user {} in team {}",
                            profile.getId(), userId, teamId);
                    return profile;
                });
    }

    // ── Private Helpers ──

    /**
     * Finds a profile by ID or throws NotFoundException.
     */
    private DeveloperProfile findProfile(UUID profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("DeveloperProfile", profileId));
    }

    /**
     * Generates a cryptographically random token string.
     */
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashes a raw token with SHA-256.
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
     * Serializes an object to JSON, returning null on failure.
     */
    private String serializeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize value to JSON", e);
            return null;
        }
    }
}
