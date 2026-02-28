package com.codeops.mcp.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeveloperProfileService}.
 *
 * <p>Verifies profile CRUD, API token lifecycle (create, validate,
 * revoke, expire), and auto-creation convenience method.</p>
 */
@ExtendWith(MockitoExtension.class)
class DeveloperProfileServiceTest {

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID TOKEN_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Mock private DeveloperProfileRepository profileRepository;
    @Mock private McpApiTokenRepository tokenRepository;
    @Mock private DeveloperProfileMapper profileMapper;
    @Mock private McpApiTokenMapper tokenMapper;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Spy private ObjectMapper objectMapper;

    @InjectMocks
    private DeveloperProfileService service;

    // ── Test Data Builders ──

    private User createUser() {
        User user = User.builder()
                .displayName("Adam Allard")
                .email("adam@allard.com")
                .passwordHash("hash")
                .build();
        user.setId(USER_ID);
        return user;
    }

    private Team createTeam() {
        Team team = Team.builder()
                .name("Test Team")
                .owner(createUser())
                .build();
        team.setId(TEAM_ID);
        return team;
    }

    private DeveloperProfile createProfile() {
        DeveloperProfile profile = DeveloperProfile.builder()
                .displayName("Adam Allard")
                .team(createTeam())
                .user(createUser())
                .defaultEnvironment(Environment.DEVELOPMENT)
                .build();
        profile.setId(PROFILE_ID);
        return profile;
    }

    private DeveloperProfileResponse createProfileResponse() {
        return new DeveloperProfileResponse(
                PROFILE_ID, "Adam Allard", null, Environment.DEVELOPMENT,
                null, null, true, TEAM_ID, USER_ID,
                "Adam Allard", NOW, NOW);
    }

    private McpApiToken createToken(TokenStatus status) {
        McpApiToken token = McpApiToken.builder()
                .name("Test Token")
                .tokenHash("somehash")
                .tokenPrefix("mcp_abcd1234")
                .status(status)
                .developerProfile(createProfile())
                .build();
        token.setId(TOKEN_ID);
        return token;
    }

    // ── createProfile ──

    @Nested
    @DisplayName("createProfile")
    class CreateProfileTests {

        @Test
        @DisplayName("creates profile with defaults")
        void createProfile_createsWithDefaults() {
            CreateDeveloperProfileRequest request = new CreateDeveloperProfileRequest(
                    null, "Java developer", "DEVELOPMENT", null, "America/Chicago");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(profileRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(false);
            when(profileRepository.save(any(DeveloperProfile.class)))
                    .thenAnswer(inv -> {
                        DeveloperProfile p = inv.getArgument(0);
                        p.setId(PROFILE_ID);
                        return p;
                    });
            when(profileMapper.toResponse(any(DeveloperProfile.class)))
                    .thenReturn(createProfileResponse());

            DeveloperProfileResponse response = service.createProfile(TEAM_ID, USER_ID, request);

            assertThat(response).isNotNull();

            ArgumentCaptor<DeveloperProfile> captor = ArgumentCaptor.forClass(DeveloperProfile.class);
            verify(profileRepository).save(captor.capture());
            // displayName defaults to user's displayName when null
            assertThat(captor.getValue().getDisplayName()).isEqualTo("Adam Allard");
            assertThat(captor.getValue().getBio()).isEqualTo("Java developer");
            assertThat(captor.getValue().getDefaultEnvironment()).isEqualTo(Environment.DEVELOPMENT);
            assertThat(captor.getValue().getTimezone()).isEqualTo("America/Chicago");
        }

        @Test
        @DisplayName("duplicate profile throws validation exception")
        void createProfile_duplicate_throwsConflict() {
            CreateDeveloperProfileRequest request = new CreateDeveloperProfileRequest(
                    "Test", null, null, null, null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(profileRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.createProfile(TEAM_ID, USER_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already exists");
        }
    }

    // ── getProfile ──

    @Nested
    @DisplayName("getProfile")
    class GetProfileTests {

        @Test
        @DisplayName("returns profile by team and user")
        void getProfile_byTeamAndUser() {
            when(profileRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.of(createProfile()));
            when(profileMapper.toResponse(any(DeveloperProfile.class)))
                    .thenReturn(createProfileResponse());

            DeveloperProfileResponse response = service.getProfile(TEAM_ID, USER_ID);

            assertThat(response).isNotNull();
            verify(profileRepository).findByTeamIdAndUserId(TEAM_ID, USER_ID);
        }

        @Test
        @DisplayName("throws when profile not found")
        void getProfile_notFound_throws() {
            when(profileRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProfile(TEAM_ID, USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("DeveloperProfile");
        }
    }

    // ── getTeamProfiles ──

    @Nested
    @DisplayName("getTeamProfiles")
    class GetTeamProfilesTests {

        @Test
        @DisplayName("returns active profiles only")
        void getTeamProfiles_returnsActiveOnly() {
            when(profileRepository.findByTeamIdAndIsActiveTrue(TEAM_ID))
                    .thenReturn(List.of(createProfile()));
            when(profileMapper.toResponseList(anyList()))
                    .thenReturn(List.of(createProfileResponse()));

            List<DeveloperProfileResponse> result = service.getTeamProfiles(TEAM_ID);

            assertThat(result).hasSize(1);
            verify(profileRepository).findByTeamIdAndIsActiveTrue(TEAM_ID);
        }
    }

    // ── updateProfile ──

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfileTests {

        @Test
        @DisplayName("updates only non-null fields")
        void updateProfile_updatesFields() {
            UpdateDeveloperProfileRequest request = new UpdateDeveloperProfileRequest(
                    "New Name", null, null, null, "Europe/London");

            DeveloperProfile profile = createProfile();
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(DeveloperProfile.class))).thenReturn(profile);
            when(profileMapper.toResponse(any(DeveloperProfile.class)))
                    .thenReturn(createProfileResponse());

            service.updateProfile(PROFILE_ID, request);

            ArgumentCaptor<DeveloperProfile> captor = ArgumentCaptor.forClass(DeveloperProfile.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getDisplayName()).isEqualTo("New Name");
            assertThat(captor.getValue().getTimezone()).isEqualTo("Europe/London");
            // bio was null in request, so original value unchanged
            assertThat(captor.getValue().getBio()).isNull();
        }
    }

    // ── deactivateProfile ──

    @Nested
    @DisplayName("deactivateProfile")
    class DeactivateProfileTests {

        @Test
        @DisplayName("sets profile inactive")
        void deactivateProfile_setsInactive() {
            DeveloperProfile profile = createProfile();
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(DeveloperProfile.class))).thenReturn(profile);

            service.deactivateProfile(PROFILE_ID);

            ArgumentCaptor<DeveloperProfile> captor = ArgumentCaptor.forClass(DeveloperProfile.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }
    }

    // ── createApiToken ──

    @Nested
    @DisplayName("createApiToken")
    class CreateApiTokenTests {

        @Test
        @DisplayName("returns raw token once, stores hash")
        void createApiToken_returnsRawTokenOnce() {
            CreateApiTokenRequest request = new CreateApiTokenRequest("My Token", null, null);

            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(createProfile()));
            when(tokenRepository.save(any(McpApiToken.class)))
                    .thenAnswer(inv -> {
                        McpApiToken t = inv.getArgument(0);
                        t.setId(TOKEN_ID);
                        t.setCreatedAt(NOW);
                        return t;
                    });

            ApiTokenCreatedResponse response = service.createApiToken(PROFILE_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.rawToken()).isNotNull();
            assertThat(response.rawToken()).isNotEmpty();
            assertThat(response.tokenPrefix()).startsWith("mcp_");
            assertThat(response.name()).isEqualTo("My Token");
        }

        @Test
        @DisplayName("stores hash only — not raw token")
        void createApiToken_storesHashOnly() {
            CreateApiTokenRequest request = new CreateApiTokenRequest("My Token", null, null);

            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(createProfile()));
            when(tokenRepository.save(any(McpApiToken.class)))
                    .thenAnswer(inv -> {
                        McpApiToken t = inv.getArgument(0);
                        t.setId(TOKEN_ID);
                        t.setCreatedAt(NOW);
                        return t;
                    });

            ApiTokenCreatedResponse response = service.createApiToken(PROFILE_ID, request);

            ArgumentCaptor<McpApiToken> captor = ArgumentCaptor.forClass(McpApiToken.class);
            verify(tokenRepository).save(captor.capture());
            McpApiToken savedToken = captor.getValue();
            // Hash should be a 64-char hex string (SHA-256)
            assertThat(savedToken.getTokenHash()).hasSize(64);
            // Raw token should NOT equal the hash
            assertThat(savedToken.getTokenHash()).isNotEqualTo(response.rawToken());
        }
    }

    // ── getTokens ──

    @Nested
    @DisplayName("getTokens")
    class GetTokensTests {

        @Test
        @DisplayName("returns token list without raw values")
        void getTokens_returnsList() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(createProfile()));
            when(tokenRepository.findByDeveloperProfileId(PROFILE_ID))
                    .thenReturn(List.of(createToken(TokenStatus.ACTIVE)));
            when(tokenMapper.toResponseList(anyList()))
                    .thenReturn(List.of(new ApiTokenResponse(
                            TOKEN_ID, "My Token", "mcp_abcd1234",
                            TokenStatus.ACTIVE, null, null, null, NOW)));

            List<ApiTokenResponse> result = service.getTokens(PROFILE_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).tokenPrefix()).isEqualTo("mcp_abcd1234");
        }
    }

    // ── revokeToken ──

    @Nested
    @DisplayName("revokeToken")
    class RevokeTokenTests {

        @Test
        @DisplayName("sets token status to REVOKED")
        void revokeToken_setsRevoked() {
            McpApiToken token = createToken(TokenStatus.ACTIVE);
            when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
            when(tokenRepository.save(any(McpApiToken.class))).thenReturn(token);

            service.revokeToken(TOKEN_ID);

            ArgumentCaptor<McpApiToken> captor = ArgumentCaptor.forClass(McpApiToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(TokenStatus.REVOKED);
        }
    }

    // ── validateToken ──

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        private McpApiToken createTokenWithHash(String rawToken, TokenStatus status) {
            // Hash the raw token with SHA-256 (same algorithm as service)
            String hash;
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                hash = hexString.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            McpApiToken token = McpApiToken.builder()
                    .name("Test Token")
                    .tokenHash(hash)
                    .tokenPrefix("mcp_test1234")
                    .status(status)
                    .developerProfile(createProfile())
                    .build();
            token.setId(TOKEN_ID);
            return token;
        }

        @Test
        @DisplayName("valid token returns profile")
        void validateToken_validToken_returnsProfile() {
            String rawToken = "test-raw-token-value";
            McpApiToken token = createTokenWithHash(rawToken, TokenStatus.ACTIVE);

            when(tokenRepository.findByTokenHash(token.getTokenHash()))
                    .thenReturn(Optional.of(token));
            when(tokenRepository.save(any(McpApiToken.class))).thenReturn(token);

            DeveloperProfile result = service.validateToken(rawToken);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PROFILE_ID);
            verify(tokenRepository).save(any(McpApiToken.class)); // updates lastUsedAt
        }

        @Test
        @DisplayName("revoked token throws authorization exception")
        void validateToken_revokedToken_throwsUnauthorized() {
            String rawToken = "test-raw-token-value";
            McpApiToken token = createTokenWithHash(rawToken, TokenStatus.REVOKED);

            when(tokenRepository.findByTokenHash(token.getTokenHash()))
                    .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.validateToken(rawToken))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("revoked");
        }

        @Test
        @DisplayName("expired token throws authorization exception")
        void validateToken_expiredToken_throwsUnauthorized() {
            String rawToken = "test-raw-token-value";
            McpApiToken token = createTokenWithHash(rawToken, TokenStatus.EXPIRED);

            when(tokenRepository.findByTokenHash(token.getTokenHash()))
                    .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.validateToken(rawToken))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("unknown token throws authorization exception")
        void validateToken_invalidToken_throwsUnauthorized() {
            when(tokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.validateToken("nonexistent-token"))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Invalid");
        }
    }

    // ── expireTokens ──

    @Nested
    @DisplayName("expireTokens")
    class ExpireTokensTests {

        @Test
        @DisplayName("expires old tokens via bulk update")
        void expireTokens_expiresOldTokens() {
            when(tokenRepository.expireTokens(any(Instant.class), eq(TokenStatus.EXPIRED)))
                    .thenReturn(3);

            int count = service.expireTokens();

            assertThat(count).isEqualTo(3);
            verify(tokenRepository).expireTokens(any(Instant.class), eq(TokenStatus.EXPIRED));
        }
    }

    // ── getOrCreateProfile ──

    @Nested
    @DisplayName("getOrCreateProfile")
    class GetOrCreateProfileTests {

        @Test
        @DisplayName("returns existing profile when found")
        void getOrCreateProfile_existingReturnsIt() {
            DeveloperProfile existing = createProfile();
            when(profileRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.of(existing));

            DeveloperProfile result = service.getOrCreateProfile(TEAM_ID, USER_ID);

            assertThat(result.getId()).isEqualTo(PROFILE_ID);
            verify(profileRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates new profile when not found")
        void getOrCreateProfile_newCreatesIt() {
            when(profileRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(profileRepository.save(any(DeveloperProfile.class)))
                    .thenAnswer(inv -> {
                        DeveloperProfile p = inv.getArgument(0);
                        p.setId(UUID.randomUUID());
                        return p;
                    });

            DeveloperProfile result = service.getOrCreateProfile(TEAM_ID, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getDisplayName()).isEqualTo("Adam Allard");
            verify(profileRepository).save(any(DeveloperProfile.class));
        }
    }
}
