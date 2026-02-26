package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.CodeOpsException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.fleet.dto.mapper.ContainerInstanceMapper;
import com.codeops.fleet.dto.request.ContainerExecRequest;
import com.codeops.fleet.dto.request.StartContainerRequest;
import com.codeops.fleet.dto.response.ContainerDetailResponse;
import com.codeops.fleet.dto.response.ContainerInstanceResponse;
import com.codeops.fleet.dto.response.ContainerStatsResponse;
import com.codeops.fleet.entity.ContainerInstance;
import com.codeops.fleet.entity.EnvironmentVariable;
import com.codeops.fleet.entity.NetworkConfig;
import com.codeops.fleet.entity.PortMapping;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.VolumeMount;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.repository.ContainerInstanceRepository;
import com.codeops.fleet.repository.EnvironmentVariableRepository;
import com.codeops.fleet.repository.NetworkConfigRepository;
import com.codeops.fleet.repository.PortMappingRepository;
import com.codeops.fleet.repository.ServiceProfileRepository;
import com.codeops.fleet.repository.VolumeMountRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContainerManagementService}.
 *
 * <p>Uses Mockito mocks for all dependencies and a static mock for
 * {@link SecurityUtils} to simulate authenticated user context.</p>
 */
@ExtendWith(MockitoExtension.class)
class ContainerManagementServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONTAINER_DB_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final String DOCKER_CONTAINER_ID = "abc123def456";

    @Mock private DockerEngineService dockerEngineService;
    @Mock private ContainerInstanceRepository containerInstanceRepository;
    @Mock private ServiceProfileRepository serviceProfileRepository;
    @Mock private PortMappingRepository portMappingRepository;
    @Mock private EnvironmentVariableRepository environmentVariableRepository;
    @Mock private VolumeMountRepository volumeMountRepository;
    @Mock private NetworkConfigRepository networkConfigRepository;
    @Mock private ContainerInstanceMapper containerInstanceMapper;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ContainerManagementService service;

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

    private ContainerInstance createContainer() {
        ContainerInstance container = new ContainerInstance();
        container.setId(CONTAINER_DB_ID);
        container.setContainerId(DOCKER_CONTAINER_ID);
        container.setContainerName("test-service-abc12345");
        container.setServiceName("test-service");
        container.setImageName("nginx");
        container.setImageTag("latest");
        container.setStatus(ContainerStatus.RUNNING);
        container.setHealthStatus(HealthStatus.NONE);
        container.setRestartPolicy(RestartPolicy.UNLESS_STOPPED);
        container.setRestartCount(0);
        container.setStartedAt(Instant.now());
        container.setTeam(createTeam());
        return container;
    }

    private ContainerDetailResponse createDetailResponse() {
        return new ContainerDetailResponse(
                CONTAINER_DB_ID, DOCKER_CONTAINER_ID, "test-service-abc12345",
                "test-service", "nginx", "latest",
                ContainerStatus.RUNNING, HealthStatus.NONE, RestartPolicy.UNLESS_STOPPED,
                0, null, null, null, null, null,
                Instant.now(), null, PROFILE_ID, "test-service", TEAM_ID,
                Instant.now(), Instant.now());
    }

    private void stubTeamAccess() {
        when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                .thenReturn(Optional.of(new TeamMember()));
    }

    private void stubProfileWithEmptyChildren(ServiceProfile profile) {
        when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(containerInstanceRepository.countByTeamIdAndStatus(TEAM_ID, ContainerStatus.RUNNING)).thenReturn(0L);
        when(portMappingRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(Collections.emptyList());
        when(environmentVariableRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(Collections.emptyList());
        when(volumeMountRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(Collections.emptyList());
        when(networkConfigRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(Collections.emptyList());
    }

    private void stubContainerSave() {
        when(containerInstanceRepository.save(any(ContainerInstance.class)))
                .thenAnswer(inv -> {
                    ContainerInstance c = inv.getArgument(0);
                    if (c.getId() == null) {
                        c.setId(CONTAINER_DB_ID);
                    }
                    return c;
                });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  startContainer
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("startContainer")
    class StartContainerTests {

        @Test
        @DisplayName("starts container from profile successfully")
        void startContainer_success() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            stubProfileWithEmptyChildren(profile);
            when(dockerEngineService.createContainer(anyString(), anyMap())).thenReturn(DOCKER_CONTAINER_ID);
            stubContainerSave();
            when(containerInstanceMapper.toDetailResponse(any(ContainerInstance.class)))
                    .thenReturn(createDetailResponse());

            StartContainerRequest request = new StartContainerRequest(PROFILE_ID, null, null, null, null);
            ContainerDetailResponse result = service.startContainer(TEAM_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.containerId()).isEqualTo(DOCKER_CONTAINER_ID);
            verify(dockerEngineService).createContainer(anyString(), anyMap());
            verify(dockerEngineService).startContainer(DOCKER_CONTAINER_ID);
            verify(containerInstanceRepository).save(any(ContainerInstance.class));
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("START_CONTAINER"),
                    eq("ContainerInstance"), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("throws NotFoundException when profile not found")
        void startContainer_profileNotFound() {
            stubTeamAccess();
            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.empty());

            StartContainerRequest request = new StartContainerRequest(PROFILE_ID, null, null, null, null);

            assertThatThrownBy(() -> service.startContainer(TEAM_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("ServiceProfile");
        }

        @Test
        @DisplayName("throws AuthorizationException when profile belongs to another team")
        void startContainer_profileWrongTeam() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            Team otherTeam = new Team();
            otherTeam.setId(UUID.randomUUID());
            profile.setTeam(otherTeam);
            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

            StartContainerRequest request = new StartContainerRequest(PROFILE_ID, null, null, null, null);

            assertThatThrownBy(() -> service.startContainer(TEAM_ID, request))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("throws ValidationException when profile is disabled")
        void startContainer_profileDisabled() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            profile.setEnabled(false);
            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

            StartContainerRequest request = new StartContainerRequest(PROFILE_ID, null, null, null, null);

            assertThatThrownBy(() -> service.startContainer(TEAM_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        @DisplayName("throws ValidationException when container limit reached")
        void startContainer_containerLimitReached() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
            when(containerInstanceRepository.countByTeamIdAndStatus(TEAM_ID, ContainerStatus.RUNNING))
                    .thenReturn((long) AppConstants.FLEET_MAX_CONTAINERS_PER_TEAM);

            StartContainerRequest request = new StartContainerRequest(PROFILE_ID, null, null, null, null);

            assertThatThrownBy(() -> service.startContainer(TEAM_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("maximum");
        }

        @Test
        @DisplayName("applies overrides from request")
        @SuppressWarnings("unchecked")
        void startContainer_withOverrides() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            stubProfileWithEmptyChildren(profile);
            when(dockerEngineService.createContainer(eq("custom-name"), anyMap())).thenReturn(DOCKER_CONTAINER_ID);
            stubContainerSave();
            when(containerInstanceMapper.toDetailResponse(any(ContainerInstance.class)))
                    .thenReturn(createDetailResponse());

            Map<String, String> envOverrides = Map.of("DB_HOST", "localhost");
            StartContainerRequest request = new StartContainerRequest(
                    PROFILE_ID, "custom-name", "v2.0", envOverrides, RestartPolicy.ALWAYS);
            service.startContainer(TEAM_ID, request);

            ArgumentCaptor<Map<String, Object>> configCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dockerEngineService).createContainer(eq("custom-name"), configCaptor.capture());
            Map<String, Object> config = configCaptor.getValue();

            assertThat(config.get("Image")).isEqualTo("nginx:v2.0");
            List<String> env = (List<String>) config.get("Env");
            assertThat(env).contains("DB_HOST=localhost");
            Map<String, Object> hostConfig = (Map<String, Object>) config.get("HostConfig");
            Map<String, String> restartPolicyMap = (Map<String, String>) hostConfig.get("RestartPolicy");
            assertThat(restartPolicyMap.get("Name")).isEqualTo("always");
        }

        @Test
        @DisplayName("connects to networks after starting")
        void startContainer_withNetworks() throws Exception {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
            when(containerInstanceRepository.countByTeamIdAndStatus(TEAM_ID, ContainerStatus.RUNNING)).thenReturn(0L);
            when(portMappingRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(Collections.emptyList());
            when(environmentVariableRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(Collections.emptyList());
            when(volumeMountRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(Collections.emptyList());

            NetworkConfig network = new NetworkConfig();
            network.setNetworkName("my-network");
            network.setAliases("[\"db\",\"postgres\"]");
            when(networkConfigRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(List.of(network));
            when(objectMapper.readValue(eq("[\"db\",\"postgres\"]"), any(TypeReference.class)))
                    .thenReturn(List.of("db", "postgres"));
            when(dockerEngineService.createContainer(anyString(), anyMap())).thenReturn(DOCKER_CONTAINER_ID);
            stubContainerSave();
            when(containerInstanceMapper.toDetailResponse(any(ContainerInstance.class)))
                    .thenReturn(createDetailResponse());

            StartContainerRequest request = new StartContainerRequest(PROFILE_ID, null, null, null, null);
            service.startContainer(TEAM_ID, request);

            verify(dockerEngineService).connectContainerToNetwork(
                    "my-network", DOCKER_CONTAINER_ID, List.of("db", "postgres"));
        }

        @Test
        @DisplayName("builds Docker config with ports, volumes, health check, and resource limits")
        @SuppressWarnings("unchecked")
        void startContainer_withFullConfig() {
            stubTeamAccess();
            ServiceProfile profile = createProfile();
            profile.setCommand("npm start");
            profile.setEntrypoint("/docker-entrypoint.sh");
            profile.setWorkingDir("/app");
            profile.setHealthCheckCommand("curl -f http://localhost/health");
            profile.setHealthCheckIntervalSeconds(30);
            profile.setHealthCheckTimeoutSeconds(10);
            profile.setHealthCheckRetries(3);
            profile.setMemoryLimitMb(512);
            profile.setCpuLimit(1.5);

            when(serviceProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
            when(containerInstanceRepository.countByTeamIdAndStatus(TEAM_ID, ContainerStatus.RUNNING)).thenReturn(0L);

            PortMapping port = new PortMapping();
            port.setHostPort(8080);
            port.setContainerPort(80);
            port.setProtocol("tcp");
            when(portMappingRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(List.of(port));

            EnvironmentVariable envVar = new EnvironmentVariable();
            envVar.setVariableKey("NODE_ENV");
            envVar.setVariableValue("production");
            when(environmentVariableRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(List.of(envVar));

            VolumeMount volume = new VolumeMount();
            volume.setHostPath("/data");
            volume.setContainerPath("/app/data");
            volume.setReadOnly(true);
            when(volumeMountRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(List.of(volume));
            when(networkConfigRepository.findByServiceProfileId(PROFILE_ID)).thenReturn(Collections.emptyList());

            when(dockerEngineService.createContainer(anyString(), anyMap())).thenReturn(DOCKER_CONTAINER_ID);
            stubContainerSave();
            when(containerInstanceMapper.toDetailResponse(any(ContainerInstance.class)))
                    .thenReturn(createDetailResponse());

            StartContainerRequest request = new StartContainerRequest(PROFILE_ID, null, null, null, null);
            service.startContainer(TEAM_ID, request);

            ArgumentCaptor<Map<String, Object>> configCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dockerEngineService).createContainer(anyString(), configCaptor.capture());
            Map<String, Object> config = configCaptor.getValue();

            assertThat(config.get("Image")).isEqualTo("nginx:latest");
            assertThat(config.get("Cmd")).isEqualTo(List.of("/bin/sh", "-c", "npm start"));
            assertThat(config.get("Entrypoint")).isEqualTo(List.of("/docker-entrypoint.sh"));
            assertThat(config.get("WorkingDir")).isEqualTo("/app");

            List<String> env = (List<String>) config.get("Env");
            assertThat(env).contains("NODE_ENV=production");

            Map<String, Object> exposedPorts = (Map<String, Object>) config.get("ExposedPorts");
            assertThat(exposedPorts).containsKey("80/tcp");

            Map<String, Object> healthcheck = (Map<String, Object>) config.get("Healthcheck");
            assertThat(healthcheck.get("Test")).isEqualTo(List.of("CMD-SHELL", "curl -f http://localhost/health"));
            assertThat(healthcheck.get("Interval")).isEqualTo(30_000_000_000L);
            assertThat(healthcheck.get("Timeout")).isEqualTo(10_000_000_000L);
            assertThat(healthcheck.get("Retries")).isEqualTo(3);

            Map<String, Object> hostConfig = (Map<String, Object>) config.get("HostConfig");
            Map<String, Object> portBindings = (Map<String, Object>) hostConfig.get("PortBindings");
            assertThat(portBindings).containsKey("80/tcp");

            List<String> binds = (List<String>) hostConfig.get("Binds");
            assertThat(binds).contains("/data:/app/data:ro");

            assertThat(hostConfig.get("Memory")).isEqualTo(512L * 1024 * 1024);
            assertThat(hostConfig.get("NanoCpus")).isEqualTo((long) (1.5 * 1_000_000_000L));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  stopContainer
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("stopContainer")
    class StopContainerTests {

        @Test
        @DisplayName("stops container successfully")
        void stopContainer_success() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));
            when(containerInstanceRepository.save(any(ContainerInstance.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(containerInstanceMapper.toDetailResponse(any(ContainerInstance.class)))
                    .thenReturn(createDetailResponse());

            ContainerDetailResponse result = service.stopContainer(TEAM_ID, CONTAINER_DB_ID, 10);

            assertThat(result).isNotNull();
            verify(dockerEngineService).stopContainer(DOCKER_CONTAINER_ID, 10);
            assertThat(container.getStatus()).isEqualTo(ContainerStatus.STOPPED);
            assertThat(container.getFinishedAt()).isNotNull();
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("STOP_CONTAINER"),
                    eq("ContainerInstance"), eq(CONTAINER_DB_ID), anyString());
        }

        @Test
        @DisplayName("throws NotFoundException when container not found")
        void stopContainer_notFound() {
            stubTeamAccess();
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.stopContainer(TEAM_ID, CONTAINER_DB_ID, 10))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws AuthorizationException when container belongs to another team")
        void stopContainer_wrongTeam() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            Team otherTeam = new Team();
            otherTeam.setId(UUID.randomUUID());
            container.setTeam(otherTeam);
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));

            assertThatThrownBy(() -> service.stopContainer(TEAM_ID, CONTAINER_DB_ID, 10))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  restartContainer
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("restartContainer")
    class RestartContainerTests {

        @Test
        @DisplayName("restarts container successfully")
        void restartContainer_success() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));
            when(containerInstanceRepository.save(any(ContainerInstance.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(containerInstanceMapper.toDetailResponse(any(ContainerInstance.class)))
                    .thenReturn(createDetailResponse());

            ContainerDetailResponse result = service.restartContainer(TEAM_ID, CONTAINER_DB_ID, 10);

            assertThat(result).isNotNull();
            verify(dockerEngineService).restartContainer(DOCKER_CONTAINER_ID, 10);
            assertThat(container.getStatus()).isEqualTo(ContainerStatus.RUNNING);
            assertThat(container.getRestartCount()).isEqualTo(1);
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("RESTART_CONTAINER"),
                    eq("ContainerInstance"), eq(CONTAINER_DB_ID), anyString());
        }

        @Test
        @DisplayName("increments restart count from existing value")
        void restartContainer_incrementsCount() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setRestartCount(3);
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));
            when(containerInstanceRepository.save(any(ContainerInstance.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(containerInstanceMapper.toDetailResponse(any(ContainerInstance.class)))
                    .thenReturn(createDetailResponse());

            service.restartContainer(TEAM_ID, CONTAINER_DB_ID, 10);

            assertThat(container.getRestartCount()).isEqualTo(4);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  removeContainer
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("removeContainer")
    class RemoveContainerTests {

        @Test
        @DisplayName("removes container successfully")
        void removeContainer_success() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));

            service.removeContainer(TEAM_ID, CONTAINER_DB_ID, true);

            verify(dockerEngineService).removeContainer(DOCKER_CONTAINER_ID, true, false);
            verify(containerInstanceRepository).delete(container);
            verify(auditLogService).log(eq(USER_ID), eq(TEAM_ID), eq("REMOVE_CONTAINER"),
                    eq("ContainerInstance"), eq(CONTAINER_DB_ID), anyString());
        }

        @Test
        @DisplayName("skips Docker removal when container has no Docker ID")
        void removeContainer_nullDockerId() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setContainerId(null);
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));

            service.removeContainer(TEAM_ID, CONTAINER_DB_ID, false);

            verify(dockerEngineService, never()).removeContainer(anyString(), anyBoolean(), anyBoolean());
            verify(containerInstanceRepository).delete(container);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  inspectContainer
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("inspectContainer")
    class InspectContainerTests {

        @Test
        @DisplayName("returns container detail")
        void inspectContainer_success() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            ContainerDetailResponse expected = createDetailResponse();
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));
            when(containerInstanceMapper.toDetailResponse(container)).thenReturn(expected);

            ContainerDetailResponse result = service.inspectContainer(TEAM_ID, CONTAINER_DB_ID);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("throws NotFoundException when container not found")
        void inspectContainer_notFound() {
            stubTeamAccess();
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.inspectContainer(TEAM_ID, CONTAINER_DB_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  getContainerLogs
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getContainerLogs")
    class GetContainerLogsTests {

        @Test
        @DisplayName("returns Docker logs")
        void getContainerLogs_success() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));
            when(dockerEngineService.getContainerLogs(DOCKER_CONTAINER_ID, 100, true))
                    .thenReturn("log line 1\nlog line 2");

            String result = service.getContainerLogs(TEAM_ID, CONTAINER_DB_ID, 100, true);

            assertThat(result).isEqualTo("log line 1\nlog line 2");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  getContainerStats
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getContainerStats")
    class GetContainerStatsTests {

        @Test
        @DisplayName("returns stats with entity UUID mapped")
        void getContainerStats_success() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));

            Instant now = Instant.now();
            ContainerStatsResponse dockerStats = new ContainerStatsResponse(
                    null, "test-service", 25.5, 256_000_000L, 512_000_000L,
                    1024L, 2048L, 4096L, 8192L, 10, now);
            when(dockerEngineService.getContainerStats(DOCKER_CONTAINER_ID)).thenReturn(dockerStats);

            ContainerStatsResponse result = service.getContainerStats(TEAM_ID, CONTAINER_DB_ID);

            assertThat(result.containerId()).isEqualTo(CONTAINER_DB_ID);
            assertThat(result.cpuPercent()).isEqualTo(25.5);
            assertThat(result.memoryUsageBytes()).isEqualTo(256_000_000L);
            assertThat(result.containerName()).isEqualTo("test-service");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  execInContainer
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("execInContainer")
    class ExecInContainerTests {

        @Test
        @DisplayName("executes command and returns output")
        void execInContainer_success() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            when(containerInstanceRepository.findById(CONTAINER_DB_ID)).thenReturn(Optional.of(container));
            when(dockerEngineService.execInContainer(DOCKER_CONTAINER_ID, "ls -la", true, false))
                    .thenReturn("total 0\n");

            ContainerExecRequest request = new ContainerExecRequest("ls -la", true, false);
            String result = service.execInContainer(TEAM_ID, CONTAINER_DB_ID, request);

            assertThat(result).isEqualTo("total 0\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  listContainers
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listContainers")
    class ListContainersTests {

        @Test
        @DisplayName("returns all containers for team")
        void listContainers_success() {
            stubTeamAccess();
            List<ContainerInstance> containers = List.of(createContainer());
            when(containerInstanceRepository.findByTeamId(TEAM_ID)).thenReturn(containers);

            ContainerInstanceResponse response = new ContainerInstanceResponse(
                    CONTAINER_DB_ID, DOCKER_CONTAINER_ID, "test-service-abc12345",
                    "test-service", "nginx", "latest",
                    ContainerStatus.RUNNING, HealthStatus.NONE, RestartPolicy.UNLESS_STOPPED,
                    0, null, null, null, Instant.now(), Instant.now());
            when(containerInstanceMapper.toResponseList(containers)).thenReturn(List.of(response));

            List<ContainerInstanceResponse> result = service.listContainers(TEAM_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).containerName()).isEqualTo("test-service-abc12345");
        }

        @Test
        @DisplayName("returns empty list when no containers")
        void listContainers_empty() {
            stubTeamAccess();
            when(containerInstanceRepository.findByTeamId(TEAM_ID)).thenReturn(Collections.emptyList());
            when(containerInstanceMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ContainerInstanceResponse> result = service.listContainers(TEAM_ID);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  listContainersByStatus
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listContainersByStatus")
    class ListContainersByStatusTests {

        @Test
        @DisplayName("returns containers filtered by status")
        void listContainersByStatus_success() {
            stubTeamAccess();
            List<ContainerInstance> containers = List.of(createContainer());
            when(containerInstanceRepository.findByTeamIdAndStatus(TEAM_ID, ContainerStatus.RUNNING))
                    .thenReturn(containers);

            ContainerInstanceResponse response = new ContainerInstanceResponse(
                    CONTAINER_DB_ID, DOCKER_CONTAINER_ID, "test-service-abc12345",
                    "test-service", "nginx", "latest",
                    ContainerStatus.RUNNING, HealthStatus.NONE, RestartPolicy.UNLESS_STOPPED,
                    0, null, null, null, Instant.now(), Instant.now());
            when(containerInstanceMapper.toResponseList(containers)).thenReturn(List.of(response));

            List<ContainerInstanceResponse> result = service.listContainersByStatus(TEAM_ID, ContainerStatus.RUNNING);

            assertThat(result).hasSize(1);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  syncContainerState
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("syncContainerState")
    class SyncContainerStateTests {

        @Test
        @DisplayName("updates container state from Docker inspection")
        void syncContainerState_updatesRunningContainer() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            when(containerInstanceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(container));

            Map<String, Object> state = new LinkedHashMap<>();
            state.put("Status", "running");
            state.put("ExitCode", 0);
            state.put("Pid", 1234);
            state.put("StartedAt", "2026-02-26T10:00:00Z");
            state.put("FinishedAt", "");
            state.put("Error", "");
            Map<String, Object> inspection = Map.of("State", state);
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID)).thenReturn(inspection);

            service.syncContainerState(TEAM_ID);

            assertThat(container.getStatus()).isEqualTo(ContainerStatus.RUNNING);
            assertThat(container.getPid()).isEqualTo(1234);
            assertThat(container.getExitCode()).isEqualTo(0);
            verify(containerInstanceRepository).save(container);
        }

        @Test
        @DisplayName("marks container as EXITED when Docker returns 404")
        void syncContainerState_docker404MarksExited() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            when(containerInstanceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(container));
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID))
                    .thenThrow(new CodeOpsException("Docker API error: 404 - no such container"));

            service.syncContainerState(TEAM_ID);

            assertThat(container.getStatus()).isEqualTo(ContainerStatus.EXITED);
            assertThat(container.getFinishedAt()).isNotNull();
            verify(containerInstanceRepository).save(container);
        }

        @Test
        @DisplayName("skips containers with EXITED status")
        void syncContainerState_skipsExited() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setStatus(ContainerStatus.EXITED);
            when(containerInstanceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(container));

            service.syncContainerState(TEAM_ID);

            verify(dockerEngineService, never()).inspectContainer(anyString());
        }

        @Test
        @DisplayName("skips containers with DEAD status")
        void syncContainerState_skipsDead() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setStatus(ContainerStatus.DEAD);
            when(containerInstanceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(container));

            service.syncContainerState(TEAM_ID);

            verify(dockerEngineService, never()).inspectContainer(anyString());
        }

        @Test
        @DisplayName("skips containers with null Docker container ID")
        void syncContainerState_skipsNullDockerId() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            container.setContainerId(null);
            when(containerInstanceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(container));

            service.syncContainerState(TEAM_ID);

            verify(dockerEngineService, never()).inspectContainer(anyString());
        }

        @Test
        @DisplayName("updates health status from Docker")
        void syncContainerState_updatesHealthStatus() {
            stubTeamAccess();
            ContainerInstance container = createContainer();
            when(containerInstanceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(container));

            Map<String, Object> health = Map.of("Status", "healthy");
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("Status", "running");
            state.put("ExitCode", 0);
            state.put("Pid", 1234);
            state.put("StartedAt", "2026-02-26T10:00:00Z");
            state.put("FinishedAt", "");
            state.put("Error", "");
            state.put("Health", health);
            Map<String, Object> inspection = Map.of("State", state);
            when(dockerEngineService.inspectContainer(DOCKER_CONTAINER_ID)).thenReturn(inspection);

            service.syncContainerState(TEAM_ID);

            assertThat(container.getHealthStatus()).isEqualTo(HealthStatus.HEALTHY);
        }

        @Test
        @DisplayName("continues syncing other containers when one fails with non-404 error")
        void syncContainerState_continuesOnNon404Error() {
            stubTeamAccess();
            ContainerInstance container1 = createContainer();
            container1.setContainerId("container-1");

            ContainerInstance container2 = createContainer();
            container2.setContainerId("container-2");
            container2.setId(UUID.randomUUID());

            when(containerInstanceRepository.findByTeamId(TEAM_ID))
                    .thenReturn(List.of(container1, container2));
            when(dockerEngineService.inspectContainer("container-1"))
                    .thenThrow(new CodeOpsException("Docker API error: 500 - internal error"));

            Map<String, Object> state = new LinkedHashMap<>();
            state.put("Status", "running");
            state.put("ExitCode", 0);
            state.put("Pid", 5678);
            state.put("StartedAt", "2026-02-26T10:00:00Z");
            state.put("FinishedAt", "");
            state.put("Error", "");
            when(dockerEngineService.inspectContainer("container-2"))
                    .thenReturn(Map.of("State", state));

            service.syncContainerState(TEAM_ID);

            // container1 should NOT be marked EXITED (non-404 error)
            assertThat(container1.getStatus()).isEqualTo(ContainerStatus.RUNNING);
            // container2 should be updated normally
            assertThat(container2.getPid()).isEqualTo(5678);
            verify(containerInstanceRepository, times(2)).save(any(ContainerInstance.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Team Access Verification
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Team Access")
    class TeamAccessTests {

        @Test
        @DisplayName("throws AuthorizationException when user is not a team member")
        void verifyTeamAccess_notMember() {
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listContainers(TEAM_ID))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Not a member");
        }
    }
}
