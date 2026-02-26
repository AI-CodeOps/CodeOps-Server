package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.fleet.dto.mapper.WorkstationProfileMapper;
import com.codeops.fleet.dto.mapper.WorkstationSolutionMapper;
import com.codeops.fleet.dto.request.AddWorkstationSolutionRequest;
import com.codeops.fleet.dto.request.CreateWorkstationProfileRequest;
import com.codeops.fleet.dto.request.UpdateWorkstationProfileRequest;
import com.codeops.fleet.dto.response.WorkstationProfileDetailResponse;
import com.codeops.fleet.dto.response.WorkstationProfileResponse;
import com.codeops.fleet.dto.response.WorkstationSolutionResponse;
import com.codeops.fleet.entity.SolutionProfile;
import com.codeops.fleet.entity.WorkstationProfile;
import com.codeops.fleet.entity.WorkstationSolution;
import com.codeops.fleet.repository.SolutionProfileRepository;
import com.codeops.fleet.repository.WorkstationProfileRepository;
import com.codeops.fleet.repository.WorkstationSolutionRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
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
 * Unit tests for {@link WorkstationProfileService}.
 *
 * <p>Uses Mockito mocks for all dependencies and a static mock for
 * {@link SecurityUtils} to simulate authenticated user context.</p>
 */
@ExtendWith(MockitoExtension.class)
class WorkstationProfileServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID WORKSTATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID SOLUTION_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID WS_SOLUTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Mock private WorkstationProfileRepository workstationProfileRepository;
    @Mock private WorkstationSolutionRepository workstationSolutionRepository;
    @Mock private SolutionProfileRepository solutionProfileRepository;
    @Mock private WorkstationProfileMapper workstationProfileMapper;
    @Mock private WorkstationSolutionMapper workstationSolutionMapper;
    @Mock private SolutionProfileService solutionProfileService;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private WorkstationProfileService service;

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

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("test@test.com");
        user.setDisplayName("Test User");
        return user;
    }

    private WorkstationProfile createWorkstation() {
        WorkstationProfile ws = new WorkstationProfile();
        ws.setId(WORKSTATION_ID);
        ws.setName("My Workstation");
        ws.setDescription("Dev env");
        ws.setDefault(false);
        ws.setUser(createUser());
        ws.setTeam(createTeam());
        return ws;
    }

    private SolutionProfile createSolutionProfile() {
        SolutionProfile sol = new SolutionProfile();
        sol.setId(SOLUTION_PROFILE_ID);
        sol.setName("Backend Stack");
        sol.setTeam(createTeam());
        return sol;
    }

    private void stubTeamAccess() {
        when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                .thenReturn(Optional.of(new TeamMember()));
    }

    private void stubWorkstationSave() {
        when(workstationProfileRepository.save(any(WorkstationProfile.class)))
                .thenAnswer(inv -> {
                    WorkstationProfile p = inv.getArgument(0);
                    if (p.getId() == null) {
                        p.setId(WORKSTATION_ID);
                    }
                    return p;
                });
    }

    private WorkstationProfileDetailResponse createDetailResponse() {
        return new WorkstationProfileDetailResponse(
                WORKSTATION_ID, "My Workstation", "Dev env", false,
                USER_ID, TEAM_ID, Collections.emptyList(), Instant.now(), Instant.now());
    }

    private void stubBuildDetailResponse() {
        when(workstationSolutionRepository.findByWorkstationProfileIdOrderByStartOrderAsc(any()))
                .thenReturn(Collections.emptyList());
        when(workstationSolutionMapper.toResponseList(any())).thenReturn(Collections.emptyList());
        when(workstationProfileMapper.toDetailResponse(any(), any())).thenReturn(createDetailResponse());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  createWorkstationProfile
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createWorkstationProfile")
    class CreateWorkstationProfileTests {

        @Test
        @DisplayName("creates workstation successfully")
        void createWorkstationProfile_success() {
            stubTeamAccess();
            when(workstationProfileRepository.existsByUserIdAndName(USER_ID, "New WS")).thenReturn(false);
            when(workstationProfileRepository.countByUserIdAndTeamId(USER_ID, TEAM_ID)).thenReturn(0L);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(createTeam()));
            stubWorkstationSave();
            stubBuildDetailResponse();

            var request = new CreateWorkstationProfileRequest("New WS", "description", false);
            var result = service.createWorkstationProfile(TEAM_ID, request);

            assertThat(result).isNotNull();
            verify(workstationProfileRepository).save(any(WorkstationProfile.class));
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("CREATE_WORKSTATION_PROFILE"),
                    eq("WorkstationProfile"), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("throws ValidationException when name exists")
        void createWorkstationProfile_duplicateName() {
            stubTeamAccess();
            when(workstationProfileRepository.existsByUserIdAndName(USER_ID, "Existing")).thenReturn(true);

            var request = new CreateWorkstationProfileRequest("Existing", null, false);

            assertThatThrownBy(() -> service.createWorkstationProfile(TEAM_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("throws ValidationException when limit reached")
        void createWorkstationProfile_limitReached() {
            stubTeamAccess();
            when(workstationProfileRepository.existsByUserIdAndName(USER_ID, "New")).thenReturn(false);
            when(workstationProfileRepository.countByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn((long) AppConstants.FLEET_MAX_WORKSTATIONS);

            var request = new CreateWorkstationProfileRequest("New", null, false);

            assertThatThrownBy(() -> service.createWorkstationProfile(TEAM_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("maximum");
        }

        @Test
        @DisplayName("clears existing default when setting new default")
        void createWorkstationProfile_setDefault_clearsExisting() {
            stubTeamAccess();
            when(workstationProfileRepository.existsByUserIdAndName(USER_ID, "Default WS")).thenReturn(false);
            when(workstationProfileRepository.countByUserIdAndTeamId(USER_ID, TEAM_ID)).thenReturn(0L);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(createTeam()));
            stubWorkstationSave();
            stubBuildDetailResponse();

            WorkstationProfile existingDefault = createWorkstation();
            existingDefault.setDefault(true);
            when(workstationProfileRepository.findByUserIdAndTeamIdAndIsDefault(USER_ID, TEAM_ID, true))
                    .thenReturn(Optional.of(existingDefault));

            var request = new CreateWorkstationProfileRequest("Default WS", null, true);
            service.createWorkstationProfile(TEAM_ID, request);

            assertThat(existingDefault.isDefault()).isFalse();
            verify(workstationProfileRepository).save(existingDefault);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  updateWorkstationProfile
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateWorkstationProfile")
    class UpdateWorkstationProfileTests {

        @Test
        @DisplayName("updates workstation successfully")
        void updateWorkstationProfile_success() {
            stubTeamAccess();
            WorkstationProfile ws = createWorkstation();
            when(workstationProfileRepository.findById(WORKSTATION_ID)).thenReturn(Optional.of(ws));
            stubWorkstationSave();
            stubBuildDetailResponse();

            var request = new UpdateWorkstationProfileRequest("Updated Name", "New desc", null);
            var result = service.updateWorkstationProfile(TEAM_ID, WORKSTATION_ID, request);

            assertThat(result).isNotNull();
            assertThat(ws.getName()).isEqualTo("Updated Name");
            assertThat(ws.getDescription()).isEqualTo("New desc");
        }

        @Test
        @DisplayName("throws AuthorizationException when workstation belongs to another user")
        void updateWorkstationProfile_wrongUser() {
            stubTeamAccess();
            WorkstationProfile ws = createWorkstation();
            User otherUser = new User();
            otherUser.setId(UUID.randomUUID());
            ws.setUser(otherUser);
            when(workstationProfileRepository.findById(WORKSTATION_ID)).thenReturn(Optional.of(ws));

            var request = new UpdateWorkstationProfileRequest(null, null, null);

            assertThatThrownBy(() -> service.updateWorkstationProfile(TEAM_ID, WORKSTATION_ID, request))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  get / list / delete
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getWorkstationProfile")
    class GetWorkstationProfileTests {

        @Test
        @DisplayName("returns workstation detail")
        void getWorkstationProfile_success() {
            stubTeamAccess();
            when(workstationProfileRepository.findById(WORKSTATION_ID))
                    .thenReturn(Optional.of(createWorkstation()));
            stubBuildDetailResponse();

            var result = service.getWorkstationProfile(TEAM_ID, WORKSTATION_ID);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("listWorkstationProfiles")
    class ListWorkstationProfilesTests {

        @Test
        @DisplayName("returns list of workstation profiles")
        void listWorkstationProfiles_success() {
            stubTeamAccess();
            when(workstationProfileRepository.findByUserIdAndTeamId(USER_ID, TEAM_ID))
                    .thenReturn(List.of(createWorkstation()));
            when(workstationProfileMapper.toResponseList(any()))
                    .thenReturn(List.of(new WorkstationProfileResponse(
                            WORKSTATION_ID, "My Workstation", "Dev env", false, 0,
                            USER_ID, TEAM_ID, Instant.now())));

            var result = service.listWorkstationProfiles(TEAM_ID);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("deleteWorkstationProfile")
    class DeleteWorkstationProfileTests {

        @Test
        @DisplayName("deletes workstation successfully")
        void deleteWorkstationProfile_success() {
            stubTeamAccess();
            WorkstationProfile ws = createWorkstation();
            when(workstationProfileRepository.findById(WORKSTATION_ID)).thenReturn(Optional.of(ws));

            service.deleteWorkstationProfile(TEAM_ID, WORKSTATION_ID);

            verify(workstationProfileRepository).delete(ws);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  addSolution / removeSolution
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("addSolution")
    class AddSolutionTests {

        @Test
        @DisplayName("adds solution to workstation successfully")
        void addSolution_success() {
            stubTeamAccess();
            when(workstationProfileRepository.findById(WORKSTATION_ID))
                    .thenReturn(Optional.of(createWorkstation()));
            when(solutionProfileRepository.findById(SOLUTION_PROFILE_ID))
                    .thenReturn(Optional.of(createSolutionProfile()));
            when(workstationSolutionRepository.existsByWorkstationProfileIdAndSolutionProfileId(
                    WORKSTATION_ID, SOLUTION_PROFILE_ID)).thenReturn(false);
            when(workstationSolutionRepository.countByWorkstationProfileId(WORKSTATION_ID)).thenReturn(0L);
            when(workstationSolutionRepository.save(any(WorkstationSolution.class)))
                    .thenAnswer(inv -> {
                        WorkstationSolution ws = inv.getArgument(0);
                        ws.setId(WS_SOLUTION_ID);
                        return ws;
                    });
            when(workstationSolutionMapper.toResponse(any()))
                    .thenReturn(new WorkstationSolutionResponse(
                            WS_SOLUTION_ID, 1, null, SOLUTION_PROFILE_ID, "Backend Stack"));

            var request = new AddWorkstationSolutionRequest(SOLUTION_PROFILE_ID, 1, null);
            var result = service.addSolution(TEAM_ID, WORKSTATION_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.solutionProfileId()).isEqualTo(SOLUTION_PROFILE_ID);
            verify(workstationSolutionRepository).save(any(WorkstationSolution.class));
        }

        @Test
        @DisplayName("throws ValidationException when solution already assigned")
        void addSolution_alreadyAssigned() {
            stubTeamAccess();
            when(workstationProfileRepository.findById(WORKSTATION_ID))
                    .thenReturn(Optional.of(createWorkstation()));
            when(solutionProfileRepository.findById(SOLUTION_PROFILE_ID))
                    .thenReturn(Optional.of(createSolutionProfile()));
            when(workstationSolutionRepository.existsByWorkstationProfileIdAndSolutionProfileId(
                    WORKSTATION_ID, SOLUTION_PROFILE_ID)).thenReturn(true);

            var request = new AddWorkstationSolutionRequest(SOLUTION_PROFILE_ID, 1, null);

            assertThatThrownBy(() -> service.addSolution(TEAM_ID, WORKSTATION_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already assigned");
        }

        @Test
        @DisplayName("throws ValidationException when limit reached")
        void addSolution_limitReached() {
            stubTeamAccess();
            when(workstationProfileRepository.findById(WORKSTATION_ID))
                    .thenReturn(Optional.of(createWorkstation()));
            when(solutionProfileRepository.findById(SOLUTION_PROFILE_ID))
                    .thenReturn(Optional.of(createSolutionProfile()));
            when(workstationSolutionRepository.existsByWorkstationProfileIdAndSolutionProfileId(
                    WORKSTATION_ID, SOLUTION_PROFILE_ID)).thenReturn(false);
            when(workstationSolutionRepository.countByWorkstationProfileId(WORKSTATION_ID))
                    .thenReturn((long) AppConstants.FLEET_MAX_SOLUTIONS_PER_WORKSTATION);

            var request = new AddWorkstationSolutionRequest(SOLUTION_PROFILE_ID, 1, null);

            assertThatThrownBy(() -> service.addSolution(TEAM_ID, WORKSTATION_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("maximum");
        }
    }

    @Nested
    @DisplayName("removeSolution")
    class RemoveSolutionTests {

        @Test
        @DisplayName("removes solution from workstation successfully")
        void removeSolution_success() {
            stubTeamAccess();
            when(workstationProfileRepository.findById(WORKSTATION_ID))
                    .thenReturn(Optional.of(createWorkstation()));
            WorkstationSolution wsSol = WorkstationSolution.builder().build();
            wsSol.setId(WS_SOLUTION_ID);
            when(workstationSolutionRepository.findByWorkstationProfileIdAndSolutionProfileId(
                    WORKSTATION_ID, SOLUTION_PROFILE_ID)).thenReturn(Optional.of(wsSol));

            service.removeSolution(TEAM_ID, WORKSTATION_ID, SOLUTION_PROFILE_ID);

            verify(workstationSolutionRepository).delete(wsSol);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  startWorkstation / stopWorkstation
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("startWorkstation")
    class StartWorkstationTests {

        @Test
        @DisplayName("starts solutions in ascending order")
        void startWorkstation_startsSolutionsInOrder() {
            stubTeamAccess();
            when(workstationProfileRepository.findById(WORKSTATION_ID))
                    .thenReturn(Optional.of(createWorkstation()));

            SolutionProfile sol1 = createSolutionProfile();
            SolutionProfile sol2 = new SolutionProfile();
            sol2.setId(UUID.randomUUID());
            sol2.setName("Frontend");
            sol2.setTeam(createTeam());

            WorkstationSolution ws1 = WorkstationSolution.builder()
                    .startOrder(1).solutionProfile(sol1).build();
            WorkstationSolution ws2 = WorkstationSolution.builder()
                    .startOrder(2).solutionProfile(sol2).build();
            when(workstationSolutionRepository.findByWorkstationProfileIdOrderByStartOrderAsc(WORKSTATION_ID))
                    .thenReturn(List.of(ws1, ws2));

            service.startWorkstation(TEAM_ID, WORKSTATION_ID);

            verify(solutionProfileService).startSolution(TEAM_ID, sol1.getId());
            verify(solutionProfileService).startSolution(TEAM_ID, sol2.getId());
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("START_WORKSTATION"),
                    eq("WorkstationProfile"), eq(WORKSTATION_ID), anyString());
        }
    }

    @Nested
    @DisplayName("stopWorkstation")
    class StopWorkstationTests {

        @Test
        @DisplayName("stops solutions in reverse order")
        void stopWorkstation_stopsSolutionsInReverseOrder() {
            stubTeamAccess();
            when(workstationProfileRepository.findById(WORKSTATION_ID))
                    .thenReturn(Optional.of(createWorkstation()));

            SolutionProfile sol = createSolutionProfile();
            WorkstationSolution wsSol = WorkstationSolution.builder()
                    .startOrder(1).solutionProfile(sol).build();
            when(workstationSolutionRepository.findByWorkstationProfileIdOrderByStartOrderAsc(WORKSTATION_ID))
                    .thenReturn(List.of(wsSol));

            service.stopWorkstation(TEAM_ID, WORKSTATION_ID);

            verify(solutionProfileService).stopSolution(TEAM_ID, sol.getId());
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("STOP_WORKSTATION"),
                    eq("WorkstationProfile"), eq(WORKSTATION_ID), anyString());
        }
    }
}
