package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.fleet.dto.mapper.NetworkConfigMapper;
import com.codeops.fleet.dto.mapper.ServiceProfileMapper;
import com.codeops.fleet.dto.mapper.VolumeMountMapper;
import com.codeops.fleet.dto.request.CreateServiceProfileRequest;
import com.codeops.fleet.dto.request.UpdateServiceProfileRequest;
import com.codeops.fleet.dto.response.ServiceProfileDetailResponse;
import com.codeops.fleet.dto.response.ServiceProfileResponse;
import com.codeops.fleet.entity.PortMapping;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.repository.NetworkConfigRepository;
import com.codeops.fleet.repository.PortMappingRepository;
import com.codeops.fleet.repository.ServiceProfileRepository;
import com.codeops.fleet.repository.VolumeMountRepository;
import com.codeops.registry.entity.ServiceRegistration;
import com.codeops.registry.entity.enums.ServiceType;
import com.codeops.registry.repository.ServiceRegistrationRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServiceProfileService}.
 *
 * <p>Uses Mockito mocks for all dependencies and a static mock for
 * {@link SecurityUtils} to simulate authenticated user context.</p>
 */
@ExtendWith(MockitoExtension.class)
class ServiceProfileServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID REGISTRATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock private ServiceProfileRepository serviceProfileRepository;
    @Mock private PortMappingRepository portMappingRepository;
    @Mock private VolumeMountRepository volumeMountRepository;
    @Mock private NetworkConfigRepository networkConfigRepository;
    @Mock private ServiceProfileMapper serviceProfileMapper;
    @Mock private VolumeMountMapper volumeMountMapper;
    @Mock private NetworkConfigMapper networkConfigMapper;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private ServiceRegistrationRepository serviceRegistrationRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ServiceProfileService service;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Test Data Helpers
    // ═══════════════════════════════════════════════════════════════════

    private Team createTeam() {
        Team team = new Team();
        team.setId(TEAM_ID);
        return team;
    }

    private ServiceProfile createProfile() {
        ServiceProfile profile = new ServiceProfile();
        profile.setId(PROFILE_ID);
        profile.setServiceName("test-service");
        profile.setImageName("nginx");
        profile.setImageTag("latest");
        profile.setRestartPolicy(RestartPolicy.UNLESS_STOPPED);
        profile.setEnabled(true);
        profile.setTeam(createTeam());
        return profile;
    }

    private void stubTeamAccess() {
        when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                .thenReturn(Optional.of(new TeamMember()));
    }

    private void stubProfileSave() {
        when(serviceProfileRepository.save(any(ServiceProfile.class)))
                .thenAnswer(inv -> {
                    ServiceProfile p = inv.getArgument(0);
                    if (p.getId() == null) {
                        p.setId(PROFILE_ID);
                    }
                    return p;
                });
    }

    private ServiceProfileDetailResponse createDetailResponse() {
        return new ServiceProfileDetailResponse(
                PROFILE_ID, "test-service", "Test Service", "A test service",
                "nginx", "latest", null, null, null, null, null,
                null, null, null, RestartPolicy.UNLESS_STOPPED,
                null, null, false, true, 0, null,
                TEAM_ID, Collections.emptyList(), Collections.emptyList(),
                Instant.now(), Instant.now());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  createServiceProfile
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createServiceProfile")
    class CreateServiceProfileTests {

        @Test
        @DisplayName("creates profile successfully")
        void createServiceProfile_success() {
            stubTeamAccess();
            when(serviceProfileRepository.existsByTeamIdAndServiceName(TEAM_ID, "new-svc")).thenReturn(false);
            when(serviceProfileRepository.findByTeamId(TEAM_ID)).thenReturn(Collections.emptyList());
            ServiceProfile mapped = createProfile();
            mapped.setServiceName("new-svc");
            when(serviceProfileMapper.toEntity(any(CreateServiceProfileRequest.class))).thenReturn(mapped);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(createTeam()));
            stubProfileSave();
            when(serviceProfileMapper.toDetailResponse(any(), any(), any())).thenReturn(createDetailResponse());
            when(volumeMountMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(networkConfigMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(volumeMountRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());
            when(networkConfigRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());

            var request = new CreateServiceProfileRequest(
                    "new-svc", "New Service", "desc", "nginx", "latest",
                    null, null, null, null, null, null, null, null,
                    RestartPolicy.UNLESS_STOPPED, null, null, 0, null);
            var result = service.createServiceProfile(TEAM_ID, request);

            assertThat(result).isNotNull();
            verify(serviceProfileRepository).save(any(ServiceProfile.class));
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("CREATE_SERVICE_PROFILE"),
                    eq("ServiceProfile"), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("throws ValidationException when name exists")
        void createServiceProfile_duplicateName() {
            stubTeamAccess();
            when(serviceProfileRepository.existsByTeamIdAndServiceName(TEAM_ID, "existing")).thenReturn(true);

            var request = new CreateServiceProfileRequest(
                    "existing", null, null, "nginx", "latest",
                    null, null, null, null, null, null, null, null,
                    null, null, null, 0, null);

            assertThatThrownBy(() -> service.createServiceProfile(TEAM_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("throws AuthorizationException when not team member")
        void createServiceProfile_notTeamMember() {
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.empty());

            var request = new CreateServiceProfileRequest(
                    "new-svc", null, null, "nginx", "latest",
                    null, null, null, null, null, null, null, null,
                    null, null, null, 0, null);

            assertThatThrownBy(() -> service.createServiceProfile(TEAM_ID, request))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  updateServiceProfile
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateServiceProfile")
    class UpdateServiceProfileTests {

        @Test
        @DisplayName("updates profile successfully")
        void updateServiceProfile_success() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
            stubProfileSave();
            when(serviceProfileMapper.toDetailResponse(any(), any(), any())).thenReturn(createDetailResponse());
            when(volumeMountMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(networkConfigMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(volumeMountRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());
            when(networkConfigRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());

            var request = new UpdateServiceProfileRequest(
                    "Updated Display", "New desc", "nginx", "alpine",
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null);
            var result = service.updateServiceProfile(TEAM_ID, PROFILE_ID, request);

            assertThat(result).isNotNull();
            assertThat(profile.getDisplayName()).isEqualTo("Updated Display");
            assertThat(profile.getDescription()).isEqualTo("New desc");
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("UPDATE_SERVICE_PROFILE"),
                    eq("ServiceProfile"), eq(PROFILE_ID), anyString());
        }

        @Test
        @DisplayName("throws AuthorizationException when profile belongs to another team")
        void updateServiceProfile_wrongTeam() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            Team otherTeam = new Team();
            otherTeam.setId(UUID.randomUUID());
            profile.setTeam(otherTeam);
            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

            var request = new UpdateServiceProfileRequest(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null);

            assertThatThrownBy(() -> service.updateServiceProfile(TEAM_ID, PROFILE_ID, request))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  getServiceProfile / listServiceProfiles / deleteServiceProfile
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getServiceProfile")
    class GetServiceProfileTests {

        @Test
        @DisplayName("returns profile detail")
        void getServiceProfile_success() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
            when(serviceProfileMapper.toDetailResponse(any(), any(), any())).thenReturn(createDetailResponse());
            when(volumeMountMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(networkConfigMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(volumeMountRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());
            when(networkConfigRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());

            var result = service.getServiceProfile(TEAM_ID, PROFILE_ID);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("listServiceProfiles")
    class ListServiceProfilesTests {

        @Test
        @DisplayName("returns list of profiles")
        void listServiceProfiles_success() {
            stubTeamAccess();
            when(serviceProfileRepository.findByTeamId(TEAM_ID))
                    .thenReturn(List.of(createProfile()));
            when(serviceProfileMapper.toResponseList(any()))
                    .thenReturn(List.of(new ServiceProfileResponse(
                            PROFILE_ID, "test-service", "Test", "nginx", "latest",
                            RestartPolicy.UNLESS_STOPPED, false, true, 0, null, Instant.now())));

            var result = service.listServiceProfiles(TEAM_ID);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("deleteServiceProfile")
    class DeleteServiceProfileTests {

        @Test
        @DisplayName("deletes profile successfully")
        void deleteServiceProfile_success() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

            service.deleteServiceProfile(TEAM_ID, PROFILE_ID);

            verify(serviceProfileRepository).delete(profile);
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("DELETE_SERVICE_PROFILE"),
                    eq("ServiceProfile"), eq(PROFILE_ID), anyString());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  autoGenerateFromRegistry
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("autoGenerateFromRegistry")
    class AutoGenerateTests {

        @Test
        @DisplayName("creates profile from registration with port mapping")
        void autoGenerateFromRegistry_success() {
            stubTeamAccess();
            when(serviceProfileRepository.findByServiceRegistrationId(REGISTRATION_ID))
                    .thenReturn(Optional.empty());

            ServiceRegistration reg = ServiceRegistration.builder()
                    .name("my-api")
                    .slug("my-api")
                    .serviceType(ServiceType.SPRING_BOOT_API)
                    .teamId(TEAM_ID)
                    .createdByUserId(USER_ID)
                    .build();
            reg.setId(REGISTRATION_ID);
            when(serviceRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(reg));
            when(serviceProfileRepository.findByTeamId(TEAM_ID)).thenReturn(Collections.emptyList());
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(createTeam()));
            stubProfileSave();
            when(portMappingRepository.save(any(PortMapping.class))).thenAnswer(inv -> inv.getArgument(0));
            when(serviceProfileMapper.toDetailResponse(any(), any(), any())).thenReturn(createDetailResponse());
            when(volumeMountMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(networkConfigMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(volumeMountRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());
            when(networkConfigRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());

            var result = service.autoGenerateFromRegistry(TEAM_ID, REGISTRATION_ID);

            assertThat(result).isNotNull();
            verify(serviceProfileRepository).save(any(ServiceProfile.class));
            verify(portMappingRepository).save(any(PortMapping.class));
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("AUTO_GENERATE_SERVICE_PROFILE"),
                    eq("ServiceProfile"), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("returns existing profile when already generated (idempotent)")
        void autoGenerateFromRegistry_existingProfile_returnsExisting() {
            stubTeamAccess();
            ServiceProfile existing = createProfile();
            when(serviceProfileRepository.findByServiceRegistrationId(REGISTRATION_ID))
                    .thenReturn(Optional.of(existing));
            when(serviceProfileMapper.toDetailResponse(any(), any(), any())).thenReturn(createDetailResponse());
            when(volumeMountMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(networkConfigMapper.toResponseList(any())).thenReturn(Collections.emptyList());
            when(volumeMountRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());
            when(networkConfigRepository.findByServiceProfileId(any())).thenReturn(Collections.emptyList());

            var result = service.autoGenerateFromRegistry(TEAM_ID, REGISTRATION_ID);

            assertThat(result).isNotNull();
            verify(serviceProfileRepository, never()).save(any());
            verify(serviceRegistrationRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws NotFoundException when registration not found")
        void autoGenerateFromRegistry_registrationNotFound() {
            stubTeamAccess();
            when(serviceProfileRepository.findByServiceRegistrationId(REGISTRATION_ID))
                    .thenReturn(Optional.empty());
            when(serviceRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.autoGenerateFromRegistry(TEAM_ID, REGISTRATION_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("ServiceRegistration");
        }
    }
}
