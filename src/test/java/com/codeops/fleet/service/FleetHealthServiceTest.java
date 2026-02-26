package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.CodeOpsException;
import com.codeops.exception.NotFoundException;
import com.codeops.fleet.dto.mapper.ContainerHealthCheckMapper;
import com.codeops.fleet.dto.response.ContainerDetailResponse;
import com.codeops.fleet.dto.response.ContainerHealthCheckResponse;
import com.codeops.fleet.dto.response.FleetHealthSummaryResponse;
import com.codeops.fleet.entity.ContainerHealthCheck;
import com.codeops.fleet.entity.ContainerInstance;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.repository.ContainerHealthCheckRepository;
import com.codeops.fleet.repository.ContainerInstanceRepository;
import com.codeops.fleet.repository.ServiceProfileRepository;
import com.codeops.registry.entity.ServiceRegistration;
import com.codeops.relay.entity.enums.PlatformEventType;
import com.codeops.relay.service.PlatformEventService;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.security.SecurityUtils;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FleetHealthService}.
 *
 * <p>Uses Mockito mocks for all dependencies and a static mock for
 * {@link SecurityUtils} to simulate authenticated user context.</p>
 */
@ExtendWith(MockitoExtension.class)
class FleetHealthServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CONTAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID REGISTRATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID HEALTH_CHECK_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final String DOCKER_CONTAINER_ID = "abc123def456";

    @Mock private DockerEngineService dockerEngineService;
    @Mock private ContainerManagementService containerManagementService;
    @Mock private ContainerInstanceRepository containerInstanceRepository;
    @Mock private ContainerHealthCheckRepository containerHealthCheckRepository;
    @Mock private ServiceProfileRepository serviceProfileRepository;
    @Mock private ContainerHealthCheckMapper containerHealthCheckMapper;
    @Mock private PlatformEventService platformEventService;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private FleetHealthService service;

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

    private ServiceRegistration createServiceRegistration() {
        ServiceRegistration reg = new ServiceRegistration();
        reg.setId(REGISTRATION_ID);
        return reg;
    }

    private ServiceProfile createServiceProfile() {
        ServiceProfile profile = new ServiceProfile();
        profile.setId(PROFILE_ID);
        profile.setServiceName("test-service");
        profile.setServiceRegistration(createServiceRegistration());
        profile.setTeam(createTeam());
        return profile;
    }

    private ContainerInstance createContainer() {
        ContainerInstance container = new ContainerInstance();
        container.setId(CONTAINER_ID);
        container.setContainerId(DOCKER_CONTAINER_ID);
        container.setContainerName("test-container");
        container.setServiceName("test-service");
        container.setImageName("nginx");
        container.setImageTag("latest");
        container.setStatus(ContainerStatus.RUNNING);
        container.setHealthStatus(HealthStatus.HEALTHY);
        container.setRestartPolicy(RestartPolicy.ALWAYS);
        container.setRestartCount(0);
        container.setCpuPercent(15.5);
        container.setMemoryBytes(256_000_000L);
        container.setMemoryLimitBytes(512_000_000L);
        container.setTeam(createTeam());
        container.setServiceProfile(createServiceProfile());
        return container;
    }

    private ContainerInstance createContainerWithoutDocker() {
        ContainerInstance container = createContainer();
        container.setContainerId(null);
        return container;
    }

    private ContainerHealthCheck createHealthCheck(HealthStatus status) {
        ContainerHealthCheck check = new ContainerHealthCheck();
        check.setId(HEALTH_CHECK_ID);
        check.setStatus(status);
        check.setOutput("OK");
        check.setExitCode(0);
        check.setDurationMs(150L);
        check.setContainer(createContainer());
        return check;
    }

    private ContainerHealthCheckResponse createHealthCheckResponse(HealthStatus status) {
        return new ContainerHealthCheckResponse(
                HEALTH_CHECK_ID, status, "OK", 0, 150L, CONTAINER_ID, Instant.now());
    }

    private Map<String, Object> createDockerInspectResponse(String healthStatus) {
        return Map.of("State", Map.of(
                "Status", "running",
                "Health", Map.of(
                        "Status", healthStatus,
                        "Log", List.of(Map.of(
                                "Output", "health check output",
                                "ExitCode", 0
                        ))
                )
        ));
    }

    private Map<String, Object> createDockerInspectWithoutHealth(String containerStatus) {
        return Map.of("State", Map.of("Status", containerStatus));
    }

    private void stubTeamAccess() {
        when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                .thenReturn(Optional.of(new TeamMember()));
    }

    private void stubContainerLookup() {
        when(containerInstanceRepository.findById(CONTAINER_ID))
                .thenReturn(Optional.of(createContainer()));
    }

    private void stubHealthCheckSave() {
        when(containerHealthCheckRepository.save(any(ContainerHealthCheck.class)))
                .thenAnswer(inv -> {
                    ContainerHealthCheck hc = inv.getArgument(0);
                    if (hc.getId() == null) {
                        hc.setId(HEALTH_CHECK_ID);
                    }
                    return hc;
                });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  checkContainerHealth
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkContainerHealth")
    class CheckContainerHealthTests {

        @Test
        @DisplayName("persists health check record")
        void checkContainerHealth_persistsRecord() {
            stubTeamAccess();
            stubContainerLookup();
            stubHealthCheckSave();
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID))
                    .thenReturn(createDockerInspectResponse("healthy"));
            when(containerHealthCheckMapper.toResponse(any()))
                    .thenReturn(createHealthCheckResponse(HealthStatus.HEALTHY));

            ContainerHealthCheckResponse result = service.checkContainerHealth(TEAM_ID, CONTAINER_ID);

            assertThat(result).isNotNull();
            verify(containerHealthCheckRepository).save(any(ContainerHealthCheck.class));
        }

        @Test
        @DisplayName("maps Docker health status to enum correctly")
        void checkContainerHealth_mapsHealthStatus() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setHealthStatus(HealthStatus.NONE); // Set different to trigger change
            when(containerInstanceRepository.findById(CONTAINER_ID)).thenReturn(Optional.of(container));
            stubHealthCheckSave();
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID))
                    .thenReturn(createDockerInspectResponse("healthy"));
            when(containerHealthCheckMapper.toResponse(any()))
                    .thenReturn(createHealthCheckResponse(HealthStatus.HEALTHY));

            service.checkContainerHealth(TEAM_ID, CONTAINER_ID);

            verify(containerHealthCheckRepository).save(any(ContainerHealthCheck.class));
            // Container health status updated to HEALTHY
            verify(containerInstanceRepository).save(any(ContainerInstance.class));
        }

        @Test
        @DisplayName("detects status change from HEALTHY to UNHEALTHY")
        void checkContainerHealth_detectsStatusChange() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setHealthStatus(HealthStatus.HEALTHY);
            when(containerInstanceRepository.findById(CONTAINER_ID)).thenReturn(Optional.of(container));
            stubHealthCheckSave();
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID))
                    .thenReturn(createDockerInspectResponse("unhealthy"));
            when(containerHealthCheckMapper.toResponse(any()))
                    .thenReturn(createHealthCheckResponse(HealthStatus.UNHEALTHY));
            // No crash loop
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any())).thenReturn(0L);

            service.checkContainerHealth(TEAM_ID, CONTAINER_ID);

            // Status changed → save + event fired
            verify(containerInstanceRepository).save(any(ContainerInstance.class));
            verify(platformEventService).publishEvent(
                    eq(PlatformEventType.ALERT_FIRED), eq(TEAM_ID),
                    any(), eq("fleet"), anyString(), anyString(), eq(USER_ID), any());
        }

        @Test
        @DisplayName("fires platform event on status change to UNHEALTHY")
        void checkContainerHealth_firesEventOnUnhealthy() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setHealthStatus(HealthStatus.HEALTHY);
            when(containerInstanceRepository.findById(CONTAINER_ID)).thenReturn(Optional.of(container));
            stubHealthCheckSave();
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID))
                    .thenReturn(createDockerInspectResponse("unhealthy"));
            when(containerHealthCheckMapper.toResponse(any()))
                    .thenReturn(createHealthCheckResponse(HealthStatus.UNHEALTHY));
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any())).thenReturn(0L);

            service.checkContainerHealth(TEAM_ID, CONTAINER_ID);

            verify(platformEventService).publishEvent(
                    eq(PlatformEventType.ALERT_FIRED), eq(TEAM_ID),
                    eq(REGISTRATION_ID), eq("fleet"),
                    anyString(), anyString(), eq(USER_ID), any());
        }

        @Test
        @DisplayName("triggers restart on UNHEALTHY with ALWAYS policy")
        void checkContainerHealth_triggersRestartOnUnhealthy() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setHealthStatus(HealthStatus.HEALTHY);
            container.setRestartPolicy(RestartPolicy.ALWAYS);
            when(containerInstanceRepository.findById(CONTAINER_ID)).thenReturn(Optional.of(container));
            stubHealthCheckSave();
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID))
                    .thenReturn(createDockerInspectResponse("unhealthy"));
            when(containerHealthCheckMapper.toResponse(any()))
                    .thenReturn(createHealthCheckResponse(HealthStatus.UNHEALTHY));
            // No crash loop
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any())).thenReturn(0L);

            service.checkContainerHealth(TEAM_ID, CONTAINER_ID);

            verify(containerManagementService).restartContainer(eq(TEAM_ID), eq(CONTAINER_ID), anyInt());
        }

        @Test
        @DisplayName("returns NONE when container has no Docker ID")
        void checkContainerHealth_noDockerContainer() {
            stubTeamAccess();
            when(containerInstanceRepository.findById(CONTAINER_ID))
                    .thenReturn(Optional.of(createContainerWithoutDocker()));
            stubHealthCheckSave();
            when(containerHealthCheckMapper.toResponse(any()))
                    .thenReturn(createHealthCheckResponse(HealthStatus.NONE));

            ContainerHealthCheckResponse result = service.checkContainerHealth(TEAM_ID, CONTAINER_ID);

            assertThat(result).isNotNull();
            verify(dockerEngineService, never()).inspectContainer(anyString());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  checkAllContainerHealth
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkAllContainerHealth")
    class CheckAllContainerHealthTests {

        @Test
        @DisplayName("checks all running containers in team")
        void checkAllContainerHealth_iteratesRunningContainers() {
            stubTeamAccess();
            ContainerInstance c1 = createContainer();
            ContainerInstance c2 = createContainer();
            c2.setId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
            c2.setContainerId("def789ghi012");
            when(containerInstanceRepository.findByTeamIdAndStatus(TEAM_ID, ContainerStatus.RUNNING))
                    .thenReturn(List.of(c1, c2));
            when(containerInstanceRepository.findById(any())).thenReturn(Optional.of(c1));
            stubHealthCheckSave();
            when(dockerEngineService.inspectContainer(anyString()))
                    .thenReturn(createDockerInspectResponse("healthy"));
            when(containerHealthCheckMapper.toResponse(any()))
                    .thenReturn(createHealthCheckResponse(HealthStatus.HEALTHY));

            List<ContainerHealthCheckResponse> results = service.checkAllContainerHealth(TEAM_ID);

            assertThat(results).hasSize(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  getHealthCheckHistory
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getHealthCheckHistory")
    class GetHealthCheckHistoryTests {

        @Test
        @DisplayName("returns paginated results newest first")
        void getHealthCheckHistory_returnsPaginated() {
            stubTeamAccess();
            stubContainerLookup();
            ContainerHealthCheck hc1 = createHealthCheck(HealthStatus.HEALTHY);
            ContainerHealthCheck hc2 = createHealthCheck(HealthStatus.UNHEALTHY);
            Page<ContainerHealthCheck> page = new PageImpl<>(List.of(hc1, hc2));
            when(containerHealthCheckRepository.findByContainerId(eq(CONTAINER_ID), any(Pageable.class)))
                    .thenReturn(page);
            when(containerHealthCheckMapper.toResponseList(any()))
                    .thenReturn(List.of(
                            createHealthCheckResponse(HealthStatus.HEALTHY),
                            createHealthCheckResponse(HealthStatus.UNHEALTHY)));

            List<ContainerHealthCheckResponse> results = service.getHealthCheckHistory(TEAM_ID, CONTAINER_ID, 10);

            assertThat(results).hasSize(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  getFleetHealthSummary
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getFleetHealthSummary")
    class GetFleetHealthSummaryTests {

        @Test
        @DisplayName("counts containers by status correctly")
        void getFleetHealthSummary_countsByStatus() {
            stubTeamAccess();
            ContainerInstance running = createContainer();
            running.setStatus(ContainerStatus.RUNNING);

            ContainerInstance stopped = createContainer();
            stopped.setStatus(ContainerStatus.STOPPED);
            stopped.setCpuPercent(null);
            stopped.setMemoryBytes(null);
            stopped.setMemoryLimitBytes(null);

            ContainerInstance unhealthy = createContainer();
            unhealthy.setStatus(ContainerStatus.RUNNING);
            unhealthy.setHealthStatus(HealthStatus.UNHEALTHY);

            ContainerInstance restarting = createContainer();
            restarting.setStatus(ContainerStatus.RESTARTING);
            restarting.setCpuPercent(null);
            restarting.setMemoryBytes(null);
            restarting.setMemoryLimitBytes(null);

            when(containerInstanceRepository.findByTeamId(TEAM_ID))
                    .thenReturn(List.of(running, stopped, unhealthy, restarting));

            FleetHealthSummaryResponse result = service.getFleetHealthSummary(TEAM_ID);

            assertThat(result.totalContainers()).isEqualTo(4);
            assertThat(result.runningContainers()).isEqualTo(2); // running + unhealthy (still RUNNING status)
            assertThat(result.stoppedContainers()).isEqualTo(1);
            assertThat(result.unhealthyContainers()).isEqualTo(1);
            assertThat(result.restartingContainers()).isEqualTo(1);
        }

        @Test
        @DisplayName("sums CPU and memory usage from cached values")
        void getFleetHealthSummary_sumsResources() {
            stubTeamAccess();
            ContainerInstance c1 = createContainer();
            c1.setCpuPercent(25.0);
            c1.setMemoryBytes(100_000_000L);
            c1.setMemoryLimitBytes(200_000_000L);

            ContainerInstance c2 = createContainer();
            c2.setCpuPercent(10.0);
            c2.setMemoryBytes(50_000_000L);
            c2.setMemoryLimitBytes(100_000_000L);

            when(containerInstanceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(c1, c2));

            FleetHealthSummaryResponse result = service.getFleetHealthSummary(TEAM_ID);

            assertThat(result.totalCpuPercent()).isEqualTo(35.0);
            assertThat(result.totalMemoryBytes()).isEqualTo(150_000_000L);
            assertThat(result.totalMemoryLimitBytes()).isEqualTo(300_000_000L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  detectCrashLoop
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("detectCrashLoop")
    class DetectCrashLoopTests {

        @Test
        @DisplayName("returns true when at threshold")
        void detectCrashLoop_trueAtThreshold() {
            ContainerInstance container = createContainer();
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    eq(CONTAINER_ID), eq(HealthStatus.UNHEALTHY), any(Instant.class)))
                    .thenReturn((long) AppConstants.FLEET_CRASH_LOOP_THRESHOLD);

            boolean result = service.detectCrashLoop(container);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false below threshold")
        void detectCrashLoop_falseBelowThreshold() {
            ContainerInstance container = createContainer();
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    eq(CONTAINER_ID), eq(HealthStatus.UNHEALTHY), any(Instant.class)))
                    .thenReturn((long) AppConstants.FLEET_CRASH_LOOP_THRESHOLD - 1);

            boolean result = service.detectCrashLoop(container);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("stops container on crash loop detection")
        void detectCrashLoop_stopsContainer() {
            ContainerInstance container = createContainer();
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    eq(CONTAINER_ID), eq(HealthStatus.UNHEALTHY), any(Instant.class)))
                    .thenReturn((long) AppConstants.FLEET_CRASH_LOOP_THRESHOLD);

            service.detectCrashLoop(container);

            verify(containerManagementService).stopContainer(eq(TEAM_ID), eq(CONTAINER_ID), anyInt());
        }

        @Test
        @DisplayName("fires CONTAINER_CRASHED event on detection")
        void detectCrashLoop_firesEvent() {
            ContainerInstance container = createContainer();
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    eq(CONTAINER_ID), eq(HealthStatus.UNHEALTHY), any(Instant.class)))
                    .thenReturn((long) AppConstants.FLEET_CRASH_LOOP_THRESHOLD);

            service.detectCrashLoop(container);

            verify(platformEventService).publishEvent(
                    eq(PlatformEventType.CONTAINER_CRASHED), eq(TEAM_ID),
                    eq(REGISTRATION_ID), eq("fleet"),
                    anyString(), anyString(), eq(USER_ID), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  handleUnhealthyContainer
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleUnhealthyContainer")
    class HandleUnhealthyContainerTests {

        @Test
        @DisplayName("restarts with ALWAYS policy")
        void handleUnhealthyContainer_alwaysRestart() {
            ContainerInstance container = createContainer();
            container.setRestartPolicy(RestartPolicy.ALWAYS);
            // No crash loop
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any())).thenReturn(0L);

            service.handleUnhealthyContainer(container);

            verify(containerManagementService).restartContainer(eq(TEAM_ID), eq(CONTAINER_ID), anyInt());
        }

        @Test
        @DisplayName("restarts ON_FAILURE with non-zero exit code")
        void handleUnhealthyContainer_onFailureNonZero() {
            ContainerInstance container = createContainer();
            container.setRestartPolicy(RestartPolicy.ON_FAILURE);
            container.setExitCode(1);
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any())).thenReturn(0L);

            service.handleUnhealthyContainer(container);

            verify(containerManagementService).restartContainer(eq(TEAM_ID), eq(CONTAINER_ID), anyInt());
        }

        @Test
        @DisplayName("skips ON_FAILURE with zero exit code")
        void handleUnhealthyContainer_onFailureZeroExit() {
            ContainerInstance container = createContainer();
            container.setRestartPolicy(RestartPolicy.ON_FAILURE);
            container.setExitCode(0);
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any())).thenReturn(0L);

            service.handleUnhealthyContainer(container);

            verify(containerManagementService, never()).restartContainer(any(), any(), anyInt());
        }

        @Test
        @DisplayName("skips with NO policy")
        void handleUnhealthyContainer_noPolicy() {
            ContainerInstance container = createContainer();
            container.setRestartPolicy(RestartPolicy.NO);
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any())).thenReturn(0L);

            service.handleUnhealthyContainer(container);

            verify(containerManagementService, never()).restartContainer(any(), any(), anyInt());
        }

        @Test
        @DisplayName("checks crash loop before restarting")
        void handleUnhealthyContainer_checksCrashLoopFirst() {
            ContainerInstance container = createContainer();
            container.setRestartPolicy(RestartPolicy.ALWAYS);
            // Crash loop detected — at threshold
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any()))
                    .thenReturn((long) AppConstants.FLEET_CRASH_LOOP_THRESHOLD);

            service.handleUnhealthyContainer(container);

            // stopContainer called (by detectCrashLoop), NOT restartContainer
            verify(containerManagementService).stopContainer(eq(TEAM_ID), eq(CONTAINER_ID), anyInt());
            verify(containerManagementService, never()).restartContainer(any(), any(), anyInt());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  fireContainerCrashedEvent (tested via detectCrashLoop)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("fireContainerCrashedEvent")
    class FireContainerCrashedEventTests {

        @Test
        @DisplayName("routes to service channel via sourceEntityId")
        void fireContainerCrashedEvent_routesToServiceChannel() {
            ContainerInstance container = createContainer();
            // Has serviceProfile with serviceRegistration → sourceEntityId = REGISTRATION_ID
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any()))
                    .thenReturn((long) AppConstants.FLEET_CRASH_LOOP_THRESHOLD);

            service.detectCrashLoop(container);

            verify(platformEventService).publishEvent(
                    eq(PlatformEventType.CONTAINER_CRASHED), eq(TEAM_ID),
                    eq(REGISTRATION_ID), // sourceEntityId routes to SERVICE channel
                    eq("fleet"), anyString(), anyString(), eq(USER_ID), any());
        }

        @Test
        @DisplayName("falls back to general channel when no service registration")
        void fireContainerCrashedEvent_fallsBackToGeneral() {
            ContainerInstance container = createContainer();
            container.getServiceProfile().setServiceRegistration(null); // No registry link
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any()))
                    .thenReturn((long) AppConstants.FLEET_CRASH_LOOP_THRESHOLD);

            service.detectCrashLoop(container);

            verify(platformEventService).publishEvent(
                    eq(PlatformEventType.CONTAINER_CRASHED), eq(TEAM_ID),
                    eq(null), // null sourceEntityId → PlatformEventService falls back to #general
                    eq("fleet"), anyString(), anyString(), eq(USER_ID), any());
        }

        @Test
        @DisplayName("includes container metadata in event detail")
        void fireContainerCrashedEvent_includesMetadata() {
            ContainerInstance container = createContainer();
            container.setRestartCount(5);
            container.setExitCode(137);
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any()))
                    .thenReturn((long) AppConstants.FLEET_CRASH_LOOP_THRESHOLD);

            service.detectCrashLoop(container);

            verify(platformEventService).publishEvent(
                    eq(PlatformEventType.CONTAINER_CRASHED), eq(TEAM_ID), any(), eq("fleet"),
                    eq("Container Crashed: test-container"),
                    org.mockito.ArgumentMatchers.contains("test-service"),
                    eq(USER_ID), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  fireHealthStatusChangeEvent (tested via checkContainerHealth)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("fireHealthStatusChangeEvent")
    class FireHealthStatusChangeEventTests {

        @Test
        @DisplayName("fires ALERT_FIRED on UNHEALTHY transition")
        void fireHealthStatusChangeEvent_unhealthyAlert() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setHealthStatus(HealthStatus.HEALTHY); // Old = HEALTHY
            when(containerInstanceRepository.findById(CONTAINER_ID)).thenReturn(Optional.of(container));
            stubHealthCheckSave();
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID))
                    .thenReturn(createDockerInspectResponse("unhealthy")); // New = UNHEALTHY
            when(containerHealthCheckMapper.toResponse(any()))
                    .thenReturn(createHealthCheckResponse(HealthStatus.UNHEALTHY));
            when(containerHealthCheckRepository.countByContainerIdAndStatusAndCreatedAtAfter(
                    any(), any(), any())).thenReturn(0L);

            service.checkContainerHealth(TEAM_ID, CONTAINER_ID);

            verify(platformEventService).publishEvent(
                    eq(PlatformEventType.ALERT_FIRED), eq(TEAM_ID),
                    any(), eq("fleet"),
                    eq("Container Unhealthy: test-container"),
                    anyString(), eq(USER_ID), any());
        }

        @Test
        @DisplayName("fires recovery notification on UNHEALTHY to HEALTHY")
        void fireHealthStatusChangeEvent_recovery() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setHealthStatus(HealthStatus.UNHEALTHY); // Old = UNHEALTHY
            when(containerInstanceRepository.findById(CONTAINER_ID)).thenReturn(Optional.of(container));
            stubHealthCheckSave();
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID))
                    .thenReturn(createDockerInspectResponse("healthy")); // New = HEALTHY
            when(containerHealthCheckMapper.toResponse(any()))
                    .thenReturn(createHealthCheckResponse(HealthStatus.HEALTHY));

            service.checkContainerHealth(TEAM_ID, CONTAINER_ID);

            verify(platformEventService).publishEvent(
                    eq(PlatformEventType.ALERT_FIRED), eq(TEAM_ID),
                    any(), eq("fleet"),
                    eq("Container Recovered: test-container"),
                    anyString(), eq(USER_ID), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  purgeOldHealthChecks
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("purgeOldHealthChecks")
    class PurgeOldHealthChecksTests {

        @Test
        @DisplayName("deletes records older than retention window")
        void purgeOldHealthChecks_deletesOldRecords() {
            stubTeamAccess();
            when(containerHealthCheckRepository.deleteByTeamIdAndCreatedAtBefore(eq(TEAM_ID), any(Instant.class)))
                    .thenReturn(42);

            int deleted = service.purgeOldHealthChecks(TEAM_ID);

            assertThat(deleted).isEqualTo(42);
            verify(containerHealthCheckRepository).deleteByTeamIdAndCreatedAtBefore(
                    eq(TEAM_ID), any(Instant.class));
        }
    }
}
