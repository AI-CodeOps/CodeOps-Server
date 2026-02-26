package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.fleet.dto.mapper.SolutionProfileMapper;
import com.codeops.fleet.dto.mapper.SolutionServiceMapper;
import com.codeops.fleet.dto.request.AddSolutionServiceRequest;
import com.codeops.fleet.dto.request.CreateSolutionProfileRequest;
import com.codeops.fleet.dto.request.UpdateSolutionProfileRequest;
import com.codeops.fleet.dto.response.ContainerDetailResponse;
import com.codeops.fleet.dto.response.SolutionProfileDetailResponse;
import com.codeops.fleet.dto.response.SolutionProfileResponse;
import com.codeops.fleet.dto.response.SolutionServiceResponse;
import com.codeops.fleet.entity.ContainerInstance;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.SolutionProfile;
import com.codeops.fleet.entity.SolutionService;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.repository.ContainerInstanceRepository;
import com.codeops.fleet.repository.ServiceProfileRepository;
import com.codeops.fleet.repository.SolutionProfileRepository;
import com.codeops.fleet.repository.SolutionServiceRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolutionProfileService}.
 *
 * <p>Uses Mockito mocks for all dependencies and a static mock for
 * {@link SecurityUtils} to simulate authenticated user context.</p>
 */
@ExtendWith(MockitoExtension.class)
class SolutionProfileServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SOLUTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID SERVICE_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID SOLUTION_SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Mock private SolutionProfileRepository solutionProfileRepository;
    @Mock private SolutionServiceRepository solutionServiceRepository;
    @Mock private ServiceProfileRepository serviceProfileRepository;
    @Mock private ContainerInstanceRepository containerInstanceRepository;
    @Mock private SolutionProfileMapper solutionProfileMapper;
    @Mock private SolutionServiceMapper solutionServiceMapper;
    @Mock private ContainerManagementService containerManagementService;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private DockerEngineService dockerEngineService;

    @InjectMocks
    private SolutionProfileService service;

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

    private SolutionProfile createSolution() {
        SolutionProfile profile = new SolutionProfile();
        profile.setId(SOLUTION_ID);
        profile.setName("Backend Stack");
        profile.setDescription("API + DB");
        profile.setDefault(false);
        profile.setTeam(createTeam());
        return profile;
    }

    private ServiceProfile createServiceProfile() {
        ServiceProfile sp = new ServiceProfile();
        sp.setId(SERVICE_PROFILE_ID);
        sp.setServiceName("postgres");
        sp.setImageName("postgres");
        sp.setImageTag("16");
        sp.setEnabled(true);
        sp.setTeam(createTeam());
        return sp;
    }

    private void stubTeamAccess() {
        when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                .thenReturn(Optional.of(new TeamMember()));
    }

    private void stubSolutionSave() {
        when(solutionProfileRepository.save(any(SolutionProfile.class)))
                .thenAnswer(inv -> {
                    SolutionProfile p = inv.getArgument(0);
                    if (p.getId() == null) {
                        p.setId(SOLUTION_ID);
                    }
                    return p;
                });
    }

    private SolutionProfileDetailResponse createDetailResponse() {
        return new SolutionProfileDetailResponse(
                SOLUTION_ID, "Backend Stack", "API + DB", false,
                TEAM_ID, Collections.emptyList(), Instant.now(), Instant.now());
    }

    private void stubBuildDetailResponse() {
        when(solutionServiceRepository.findBySolutionProfileIdOrderByStartOrderAsc(any()))
                .thenReturn(Collections.emptyList());
        when(solutionServiceMapper.toResponseList(any())).thenReturn(Collections.emptyList());
        when(solutionProfileMapper.toDetailResponse(any(), any())).thenReturn(createDetailResponse());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  createSolutionProfile
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createSolutionProfile")
    class CreateSolutionProfileTests {

        @Test
        @DisplayName("creates profile successfully")
        void createSolutionProfile_success() {
            stubTeamAccess();
            when(solutionProfileRepository.existsByTeamIdAndName(TEAM_ID, "New Stack")).thenReturn(false);
            when(solutionProfileRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(createTeam()));
            stubSolutionSave();
            stubBuildDetailResponse();

            var request = new CreateSolutionProfileRequest("New Stack", "description", false);
            var result = service.createSolutionProfile(TEAM_ID, request);

            assertThat(result).isNotNull();
            verify(solutionProfileRepository).save(any(SolutionProfile.class));
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("CREATE_SOLUTION_PROFILE"),
                    eq("SolutionProfile"), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("throws ValidationException when name exists")
        void createSolutionProfile_duplicateName() {
            stubTeamAccess();
            when(solutionProfileRepository.existsByTeamIdAndName(TEAM_ID, "Existing")).thenReturn(true);

            var request = new CreateSolutionProfileRequest("Existing", null, false);

            assertThatThrownBy(() -> service.createSolutionProfile(TEAM_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("throws ValidationException when limit reached")
        void createSolutionProfile_limitReached() {
            stubTeamAccess();
            when(solutionProfileRepository.existsByTeamIdAndName(TEAM_ID, "New")).thenReturn(false);
            when(solutionProfileRepository.countByTeamId(TEAM_ID))
                    .thenReturn((long) AppConstants.FLEET_MAX_SOLUTIONS);

            var request = new CreateSolutionProfileRequest("New", null, false);

            assertThatThrownBy(() -> service.createSolutionProfile(TEAM_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("maximum");
        }

        @Test
        @DisplayName("clears existing default when setting new default")
        void createSolutionProfile_setDefault_clearsExisting() {
            stubTeamAccess();
            when(solutionProfileRepository.existsByTeamIdAndName(TEAM_ID, "Default")).thenReturn(false);
            when(solutionProfileRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(createTeam()));
            stubSolutionSave();
            stubBuildDetailResponse();

            SolutionProfile existingDefault = createSolution();
            existingDefault.setDefault(true);
            when(solutionProfileRepository.findByTeamIdAndIsDefault(TEAM_ID, true))
                    .thenReturn(Optional.of(existingDefault));

            var request = new CreateSolutionProfileRequest("Default", null, true);
            service.createSolutionProfile(TEAM_ID, request);

            assertThat(existingDefault.isDefault()).isFalse();
            verify(solutionProfileRepository).save(existingDefault);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  updateSolutionProfile
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateSolutionProfile")
    class UpdateSolutionProfileTests {

        @Test
        @DisplayName("updates profile successfully")
        void updateSolutionProfile_success() {
            stubTeamAccess();
            SolutionProfile solution = createSolution();
            when(solutionProfileRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(solution));
            stubSolutionSave();
            stubBuildDetailResponse();

            var request = new UpdateSolutionProfileRequest("Updated Name", "New desc", null);
            var result = service.updateSolutionProfile(TEAM_ID, SOLUTION_ID, request);

            assertThat(result).isNotNull();
            assertThat(solution.getName()).isEqualTo("Updated Name");
            assertThat(solution.getDescription()).isEqualTo("New desc");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  get / list / delete
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getSolutionProfile")
    class GetSolutionProfileTests {

        @Test
        @DisplayName("returns profile detail")
        void getSolutionProfile_success() {
            stubTeamAccess();
            when(solutionProfileRepository.findById(SOLUTION_ID))
                    .thenReturn(Optional.of(createSolution()));
            stubBuildDetailResponse();

            var result = service.getSolutionProfile(TEAM_ID, SOLUTION_ID);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("listSolutionProfiles")
    class ListSolutionProfilesTests {

        @Test
        @DisplayName("returns list of profiles")
        void listSolutionProfiles_success() {
            stubTeamAccess();
            when(solutionProfileRepository.findByTeamId(TEAM_ID))
                    .thenReturn(List.of(createSolution()));
            when(solutionProfileMapper.toResponseList(any()))
                    .thenReturn(List.of(new SolutionProfileResponse(
                            SOLUTION_ID, "Backend Stack", "API + DB", false, 0,
                            TEAM_ID, Instant.now())));

            var result = service.listSolutionProfiles(TEAM_ID);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("deleteSolutionProfile")
    class DeleteSolutionProfileTests {

        @Test
        @DisplayName("deletes profile successfully")
        void deleteSolutionProfile_success() {
            stubTeamAccess();
            SolutionProfile solution = createSolution();
            when(solutionProfileRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(solution));

            service.deleteSolutionProfile(TEAM_ID, SOLUTION_ID);

            verify(solutionProfileRepository).delete(solution);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  addService / removeService
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("addService")
    class AddServiceTests {

        @Test
        @DisplayName("adds service to solution successfully")
        void addService_success() {
            stubTeamAccess();
            when(solutionProfileRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(createSolution()));
            when(serviceProfileRepository.findById(SERVICE_PROFILE_ID))
                    .thenReturn(Optional.of(createServiceProfile()));
            when(solutionServiceRepository.existsBySolutionProfileIdAndServiceProfileId(
                    SOLUTION_ID, SERVICE_PROFILE_ID)).thenReturn(false);
            when(solutionServiceRepository.countBySolutionProfileId(SOLUTION_ID)).thenReturn(0L);
            when(solutionServiceRepository.save(any(SolutionService.class)))
                    .thenAnswer(inv -> {
                        SolutionService s = inv.getArgument(0);
                        s.setId(SOLUTION_SERVICE_ID);
                        return s;
                    });
            when(solutionServiceMapper.toResponse(any()))
                    .thenReturn(new SolutionServiceResponse(
                            SOLUTION_SERVICE_ID, 1, SERVICE_PROFILE_ID, "postgres", "postgres", true));

            var request = new AddSolutionServiceRequest(SERVICE_PROFILE_ID, 1);
            var result = service.addService(TEAM_ID, SOLUTION_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.serviceProfileId()).isEqualTo(SERVICE_PROFILE_ID);
            verify(solutionServiceRepository).save(any(SolutionService.class));
        }

        @Test
        @DisplayName("throws ValidationException when service already assigned")
        void addService_alreadyAssigned() {
            stubTeamAccess();
            when(solutionProfileRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(createSolution()));
            when(serviceProfileRepository.findById(SERVICE_PROFILE_ID))
                    .thenReturn(Optional.of(createServiceProfile()));
            when(solutionServiceRepository.existsBySolutionProfileIdAndServiceProfileId(
                    SOLUTION_ID, SERVICE_PROFILE_ID)).thenReturn(true);

            var request = new AddSolutionServiceRequest(SERVICE_PROFILE_ID, 1);

            assertThatThrownBy(() -> service.addService(TEAM_ID, SOLUTION_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already assigned");
        }
    }

    @Nested
    @DisplayName("removeService")
    class RemoveServiceTests {

        @Test
        @DisplayName("removes service from solution successfully")
        void removeService_success() {
            stubTeamAccess();
            when(solutionProfileRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(createSolution()));
            SolutionService svc = SolutionService.builder().build();
            svc.setId(SOLUTION_SERVICE_ID);
            when(solutionServiceRepository.findBySolutionProfileIdAndServiceProfileId(
                    SOLUTION_ID, SERVICE_PROFILE_ID)).thenReturn(Optional.of(svc));

            service.removeService(TEAM_ID, SOLUTION_ID, SERVICE_PROFILE_ID);

            verify(solutionServiceRepository).delete(svc);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  startSolution / stopSolution
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("startSolution")
    class StartSolutionTests {

        @Test
        @DisplayName("starts services in ascending order")
        void startSolution_startsServicesInOrder() {
            stubTeamAccess();
            SolutionProfile solution = createSolution();
            when(solutionProfileRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(solution));

            ServiceProfile sp1 = createServiceProfile();
            sp1.setServiceName("db");
            ServiceProfile sp2 = new ServiceProfile();
            sp2.setId(UUID.randomUUID());
            sp2.setServiceName("api");
            sp2.setImageName("nginx");
            sp2.setEnabled(true);
            sp2.setTeam(createTeam());

            SolutionService svc1 = SolutionService.builder().startOrder(1).serviceProfile(sp1).build();
            SolutionService svc2 = SolutionService.builder().startOrder(2).serviceProfile(sp2).build();
            when(solutionServiceRepository.findBySolutionProfileIdOrderByStartOrderAsc(SOLUTION_ID))
                    .thenReturn(List.of(svc1, svc2));

            ContainerDetailResponse detailResp = new ContainerDetailResponse(
                    UUID.randomUUID(), "docker-id", "container",
                    "svc", "img", "latest",
                    ContainerStatus.RUNNING, HealthStatus.NONE, RestartPolicy.UNLESS_STOPPED,
                    0, null, null, null, null, null,
                    Instant.now(), null, SERVICE_PROFILE_ID, "svc", TEAM_ID,
                    Instant.now(), Instant.now());
            when(containerManagementService.startContainer(eq(TEAM_ID), any()))
                    .thenReturn(detailResp);

            service.startSolution(TEAM_ID, SOLUTION_ID);

            verify(containerManagementService, Mockito.times(2)).startContainer(eq(TEAM_ID), any());
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("START_SOLUTION"),
                    eq("SolutionProfile"), eq(SOLUTION_ID), anyString());
        }

        @Test
        @DisplayName("skips disabled services")
        void startSolution_skipsDisabledServices() {
            stubTeamAccess();
            when(solutionProfileRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(createSolution()));

            ServiceProfile disabledSp = createServiceProfile();
            disabledSp.setEnabled(false);
            SolutionService svc = SolutionService.builder().startOrder(1).serviceProfile(disabledSp).build();
            when(solutionServiceRepository.findBySolutionProfileIdOrderByStartOrderAsc(SOLUTION_ID))
                    .thenReturn(List.of(svc));

            service.startSolution(TEAM_ID, SOLUTION_ID);

            verify(containerManagementService, Mockito.never()).startContainer(any(), any());
        }
    }

    @Nested
    @DisplayName("stopSolution")
    class StopSolutionTests {

        @Test
        @DisplayName("stops running containers in reverse order")
        void stopSolution_stopsInReverseOrder() {
            stubTeamAccess();
            when(solutionProfileRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(createSolution()));

            ServiceProfile sp = createServiceProfile();
            SolutionService svc = SolutionService.builder().startOrder(1).serviceProfile(sp).build();
            when(solutionServiceRepository.findBySolutionProfileIdOrderByStartOrderAsc(SOLUTION_ID))
                    .thenReturn(List.of(svc));

            ContainerInstance container = new ContainerInstance();
            container.setId(UUID.randomUUID());
            container.setContainerId("docker-id");
            container.setStatus(ContainerStatus.RUNNING);
            when(containerInstanceRepository.findByServiceProfileId(SERVICE_PROFILE_ID))
                    .thenReturn(List.of(container));

            ContainerDetailResponse stopResp = new ContainerDetailResponse(
                    container.getId(), "docker-id", "container",
                    "postgres", "postgres", "16",
                    ContainerStatus.STOPPED, HealthStatus.NONE, RestartPolicy.UNLESS_STOPPED,
                    0, null, null, null, null, null,
                    null, Instant.now(), SERVICE_PROFILE_ID, "postgres", TEAM_ID,
                    Instant.now(), Instant.now());
            when(containerManagementService.stopContainer(TEAM_ID, container.getId(), 10))
                    .thenReturn(stopResp);

            service.stopSolution(TEAM_ID, SOLUTION_ID);

            verify(containerManagementService).stopContainer(TEAM_ID, container.getId(), 10);
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("STOP_SOLUTION"),
                    eq("SolutionProfile"), eq(SOLUTION_ID), anyString());
        }
    }
}
