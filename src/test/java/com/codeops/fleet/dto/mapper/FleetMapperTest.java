package com.codeops.fleet.dto.mapper;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.fleet.dto.request.CreateServiceProfileRequest;
import com.codeops.fleet.dto.response.*;
import com.codeops.fleet.entity.*;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.registry.entity.ServiceRegistration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for all Fleet module MapStruct mappers.
 *
 * <p>Verifies entity-to-response mapping (including boolean field name translation),
 * request-to-entity mapping (including constant defaults and ignored fields),
 * list mapping, and proper handling of nullable relationships.</p>
 */
class FleetMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID CONTAINER_ID = UUID.randomUUID();
    private static final UUID SERVICE_PROFILE_ID = UUID.randomUUID();
    private static final UUID SERVICE_REGISTRATION_ID = UUID.randomUUID();
    private static final UUID SOLUTION_PROFILE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    // ── ContainerInstanceMapper ─────────────────────────────────────────

    @Nested
    class ContainerInstanceMapperTests {

        private final ContainerInstanceMapper mapper = Mappers.getMapper(ContainerInstanceMapper.class);

        @Test
        void toResponse_mapsLightweightFields() {
            ContainerInstance entity = ContainerInstance.builder()
                    .containerId("abc123def456abc123def456abc123def456abc123def456abc123def456abcd")
                    .containerName("my-postgres")
                    .serviceName("postgres")
                    .imageName("postgres")
                    .imageTag("16")
                    .status(ContainerStatus.RUNNING)
                    .healthStatus(HealthStatus.HEALTHY)
                    .restartPolicy(RestartPolicy.ALWAYS)
                    .restartCount(2)
                    .cpuPercent(25.5)
                    .memoryBytes(536870912L)
                    .memoryLimitBytes(1073741824L)
                    .startedAt(NOW)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            ContainerInstanceResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.containerId()).isEqualTo("abc123def456abc123def456abc123def456abc123def456abc123def456abcd");
            assertThat(resp.containerName()).isEqualTo("my-postgres");
            assertThat(resp.serviceName()).isEqualTo("postgres");
            assertThat(resp.imageName()).isEqualTo("postgres");
            assertThat(resp.imageTag()).isEqualTo("16");
            assertThat(resp.status()).isEqualTo(ContainerStatus.RUNNING);
            assertThat(resp.healthStatus()).isEqualTo(HealthStatus.HEALTHY);
            assertThat(resp.restartPolicy()).isEqualTo(RestartPolicy.ALWAYS);
            assertThat(resp.restartCount()).isEqualTo(2);
            assertThat(resp.cpuPercent()).isEqualTo(25.5);
            assertThat(resp.memoryBytes()).isEqualTo(536870912L);
            assertThat(resp.memoryLimitBytes()).isEqualTo(1073741824L);
            assertThat(resp.startedAt()).isEqualTo(NOW);
            assertThat(resp.createdAt()).isEqualTo(NOW);
        }

        @Test
        void toDetailResponse_mapsAllFieldsWithServiceProfile() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            ServiceProfile profile = ServiceProfile.builder()
                    .serviceName("postgres")
                    .imageName("postgres")
                    .build();
            profile.setId(SERVICE_PROFILE_ID);

            ContainerInstance entity = ContainerInstance.builder()
                    .containerId("abc123def456")
                    .containerName("my-postgres")
                    .serviceName("postgres")
                    .imageName("postgres")
                    .imageTag("16")
                    .status(ContainerStatus.RUNNING)
                    .healthStatus(HealthStatus.HEALTHY)
                    .restartPolicy(RestartPolicy.ALWAYS)
                    .restartCount(1)
                    .exitCode(null)
                    .cpuPercent(10.0)
                    .memoryBytes(268435456L)
                    .memoryLimitBytes(536870912L)
                    .pid(1234)
                    .startedAt(NOW)
                    .finishedAt(null)
                    .serviceProfile(profile)
                    .team(team)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);
            entity.setUpdatedAt(NOW);

            ContainerDetailResponse resp = mapper.toDetailResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.containerId()).isEqualTo("abc123def456");
            assertThat(resp.containerName()).isEqualTo("my-postgres");
            assertThat(resp.serviceName()).isEqualTo("postgres");
            assertThat(resp.imageName()).isEqualTo("postgres");
            assertThat(resp.imageTag()).isEqualTo("16");
            assertThat(resp.status()).isEqualTo(ContainerStatus.RUNNING);
            assertThat(resp.healthStatus()).isEqualTo(HealthStatus.HEALTHY);
            assertThat(resp.restartPolicy()).isEqualTo(RestartPolicy.ALWAYS);
            assertThat(resp.restartCount()).isEqualTo(1);
            assertThat(resp.exitCode()).isNull();
            assertThat(resp.cpuPercent()).isEqualTo(10.0);
            assertThat(resp.memoryBytes()).isEqualTo(268435456L);
            assertThat(resp.memoryLimitBytes()).isEqualTo(536870912L);
            assertThat(resp.pid()).isEqualTo(1234);
            assertThat(resp.startedAt()).isEqualTo(NOW);
            assertThat(resp.finishedAt()).isNull();
            assertThat(resp.serviceProfileId()).isEqualTo(SERVICE_PROFILE_ID);
            assertThat(resp.serviceProfileName()).isEqualTo("postgres");
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.createdAt()).isEqualTo(NOW);
            assertThat(resp.updatedAt()).isEqualTo(NOW);
        }

        @Test
        void toDetailResponse_nullServiceProfile_mapsNulls() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            ContainerInstance entity = ContainerInstance.builder()
                    .containerName("standalone")
                    .serviceName("custom")
                    .imageName("alpine")
                    .team(team)
                    .build();
            entity.setId(ID);

            ContainerDetailResponse resp = mapper.toDetailResponse(entity);

            assertThat(resp.serviceProfileId()).isNull();
            assertThat(resp.serviceProfileName()).isNull();
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
        }

        @Test
        void toResponseList_mapsAllElements() {
            ContainerInstance e1 = ContainerInstance.builder()
                    .containerName("c1").serviceName("svc1").imageName("img1").build();
            ContainerInstance e2 = ContainerInstance.builder()
                    .containerName("c2").serviceName("svc2").imageName("img2").build();

            List<ContainerInstanceResponse> list = mapper.toResponseList(List.of(e1, e2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).containerName()).isEqualTo("c1");
            assertThat(list.get(1).containerName()).isEqualTo("c2");
        }

        @Test
        void toResponseList_emptyList_returnsEmpty() {
            List<ContainerInstanceResponse> list = mapper.toResponseList(Collections.emptyList());

            assertThat(list).isEmpty();
        }
    }

    // ── ServiceProfileMapper ────────────────────────────────────────────

    @Nested
    class ServiceProfileMapperTests {

        private final ServiceProfileMapper mapper = Mappers.getMapper(ServiceProfileMapper.class);

        @Test
        void toResponse_mapsFieldsIncludingBooleans() {
            ServiceRegistration reg = ServiceRegistration.builder().build();
            reg.setId(SERVICE_REGISTRATION_ID);

            ServiceProfile entity = ServiceProfile.builder()
                    .serviceName("postgres")
                    .displayName("PostgreSQL")
                    .imageName("postgres")
                    .imageTag("16")
                    .restartPolicy(RestartPolicy.UNLESS_STOPPED)
                    .isAutoGenerated(true)
                    .isEnabled(true)
                    .startOrder(1)
                    .serviceRegistration(reg)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            ServiceProfileResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.serviceName()).isEqualTo("postgres");
            assertThat(resp.displayName()).isEqualTo("PostgreSQL");
            assertThat(resp.imageName()).isEqualTo("postgres");
            assertThat(resp.imageTag()).isEqualTo("16");
            assertThat(resp.restartPolicy()).isEqualTo(RestartPolicy.UNLESS_STOPPED);
            assertThat(resp.isAutoGenerated()).isTrue();
            assertThat(resp.isEnabled()).isTrue();
            assertThat(resp.startOrder()).isEqualTo(1);
            assertThat(resp.serviceRegistrationId()).isEqualTo(SERVICE_REGISTRATION_ID);
            assertThat(resp.createdAt()).isEqualTo(NOW);
        }

        @Test
        void toResponse_autoGeneratedFalse_mapsFalse() {
            ServiceProfile entity = ServiceProfile.builder()
                    .serviceName("custom")
                    .imageName("alpine")
                    .build();

            ServiceProfileResponse resp = mapper.toResponse(entity);

            assertThat(resp.isAutoGenerated()).isFalse();
            assertThat(resp.isEnabled()).isTrue();
        }

        @Test
        void toResponse_nullServiceRegistration_mapsNull() {
            ServiceProfile entity = ServiceProfile.builder()
                    .serviceName("custom")
                    .imageName("alpine")
                    .build();

            ServiceProfileResponse resp = mapper.toResponse(entity);

            assertThat(resp.serviceRegistrationId()).isNull();
        }

        @Test
        void toDetailResponse_mapsAllFieldsWithVolumesAndNetworks() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            ServiceProfile entity = ServiceProfile.builder()
                    .serviceName("postgres")
                    .displayName("PostgreSQL")
                    .description("Main database")
                    .imageName("postgres")
                    .imageTag("16")
                    .command("postgres -c shared_buffers=256MB")
                    .workingDir("/var/lib/postgresql")
                    .healthCheckCommand("pg_isready")
                    .healthCheckIntervalSeconds(15)
                    .healthCheckTimeoutSeconds(5)
                    .healthCheckRetries(5)
                    .restartPolicy(RestartPolicy.ALWAYS)
                    .memoryLimitMb(512)
                    .cpuLimit(2.0)
                    .isAutoGenerated(false)
                    .isEnabled(true)
                    .startOrder(1)
                    .team(team)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);
            entity.setUpdatedAt(NOW);

            List<VolumeMountResponse> volumes = List.of(
                    new VolumeMountResponse(UUID.randomUUID(), null, "/var/lib/postgresql/data", "pgdata", false));
            List<NetworkConfigResponse> networks = List.of(
                    new NetworkConfigResponse(UUID.randomUUID(), "backend", null, null));

            ServiceProfileDetailResponse resp = mapper.toDetailResponse(entity, volumes, networks);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.serviceName()).isEqualTo("postgres");
            assertThat(resp.displayName()).isEqualTo("PostgreSQL");
            assertThat(resp.description()).isEqualTo("Main database");
            assertThat(resp.imageName()).isEqualTo("postgres");
            assertThat(resp.imageTag()).isEqualTo("16");
            assertThat(resp.command()).isEqualTo("postgres -c shared_buffers=256MB");
            assertThat(resp.workingDir()).isEqualTo("/var/lib/postgresql");
            assertThat(resp.healthCheckCommand()).isEqualTo("pg_isready");
            assertThat(resp.healthCheckIntervalSeconds()).isEqualTo(15);
            assertThat(resp.healthCheckTimeoutSeconds()).isEqualTo(5);
            assertThat(resp.healthCheckRetries()).isEqualTo(5);
            assertThat(resp.restartPolicy()).isEqualTo(RestartPolicy.ALWAYS);
            assertThat(resp.memoryLimitMb()).isEqualTo(512);
            assertThat(resp.cpuLimit()).isEqualTo(2.0);
            assertThat(resp.isAutoGenerated()).isFalse();
            assertThat(resp.isEnabled()).isTrue();
            assertThat(resp.startOrder()).isEqualTo(1);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.volumes()).hasSize(1);
            assertThat(resp.networks()).hasSize(1);
            assertThat(resp.envVarsJson()).isNull();
            assertThat(resp.portsJson()).isNull();
            assertThat(resp.createdAt()).isEqualTo(NOW);
            assertThat(resp.updatedAt()).isEqualTo(NOW);
        }

        @Test
        void toEntity_mapsRequestFieldsAndSetsDefaults() {
            var request = new CreateServiceProfileRequest(
                    "redis", "Redis", "Cache server",
                    "redis", "7-alpine", null, null,
                    null, null, null, null, null, null,
                    RestartPolicy.ALWAYS, 256, 1.0, 2, null);

            ServiceProfile entity = mapper.toEntity(request);

            assertThat(entity.getServiceName()).isEqualTo("redis");
            assertThat(entity.getDisplayName()).isEqualTo("Redis");
            assertThat(entity.getDescription()).isEqualTo("Cache server");
            assertThat(entity.getImageName()).isEqualTo("redis");
            assertThat(entity.getImageTag()).isEqualTo("7-alpine");
            assertThat(entity.getRestartPolicy()).isEqualTo(RestartPolicy.ALWAYS);
            assertThat(entity.getMemoryLimitMb()).isEqualTo(256);
            assertThat(entity.getCpuLimit()).isEqualTo(1.0);
            assertThat(entity.getStartOrder()).isEqualTo(2);
            assertThat(entity.isAutoGenerated()).isFalse();
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.getId()).isNull();
            assertThat(entity.getTeam()).isNull();
            assertThat(entity.getServiceRegistration()).isNull();
        }

        @Test
        void toResponseList_mapsAllElements() {
            ServiceProfile e1 = ServiceProfile.builder().serviceName("svc1").imageName("img1").build();
            ServiceProfile e2 = ServiceProfile.builder().serviceName("svc2").imageName("img2").build();

            List<ServiceProfileResponse> list = mapper.toResponseList(List.of(e1, e2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).serviceName()).isEqualTo("svc1");
            assertThat(list.get(1).serviceName()).isEqualTo("svc2");
        }
    }

    // ── ContainerHealthCheckMapper ──────────────────────────────────────

    @Nested
    class ContainerHealthCheckMapperTests {

        private final ContainerHealthCheckMapper mapper = Mappers.getMapper(ContainerHealthCheckMapper.class);

        @Test
        void toResponse_mapsFieldsAndExtractsContainerId() {
            ContainerInstance container = ContainerInstance.builder()
                    .containerName("my-postgres")
                    .serviceName("postgres")
                    .imageName("postgres")
                    .build();
            container.setId(CONTAINER_ID);

            ContainerHealthCheck entity = ContainerHealthCheck.builder()
                    .status(HealthStatus.HEALTHY)
                    .output("OK")
                    .exitCode(0)
                    .durationMs(150L)
                    .container(container)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            ContainerHealthCheckResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.status()).isEqualTo(HealthStatus.HEALTHY);
            assertThat(resp.output()).isEqualTo("OK");
            assertThat(resp.exitCode()).isEqualTo(0);
            assertThat(resp.durationMs()).isEqualTo(150L);
            assertThat(resp.containerId()).isEqualTo(CONTAINER_ID);
            assertThat(resp.createdAt()).isEqualTo(NOW);
        }

        @Test
        void toResponse_unhealthyCheck_mapsCorrectly() {
            ContainerInstance container = ContainerInstance.builder()
                    .containerName("test").serviceName("test").imageName("test").build();
            container.setId(CONTAINER_ID);

            ContainerHealthCheck entity = ContainerHealthCheck.builder()
                    .status(HealthStatus.UNHEALTHY)
                    .output("Connection refused")
                    .exitCode(1)
                    .durationMs(10000L)
                    .container(container)
                    .build();

            ContainerHealthCheckResponse resp = mapper.toResponse(entity);

            assertThat(resp.status()).isEqualTo(HealthStatus.UNHEALTHY);
            assertThat(resp.exitCode()).isEqualTo(1);
            assertThat(resp.output()).isEqualTo("Connection refused");
        }

        @Test
        void toResponseList_mapsAllElements() {
            ContainerInstance container = ContainerInstance.builder()
                    .containerName("test").serviceName("test").imageName("test").build();
            container.setId(CONTAINER_ID);

            ContainerHealthCheck hc1 = ContainerHealthCheck.builder()
                    .status(HealthStatus.HEALTHY).container(container).build();
            ContainerHealthCheck hc2 = ContainerHealthCheck.builder()
                    .status(HealthStatus.UNHEALTHY).container(container).build();

            List<ContainerHealthCheckResponse> list = mapper.toResponseList(List.of(hc1, hc2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).status()).isEqualTo(HealthStatus.HEALTHY);
            assertThat(list.get(1).status()).isEqualTo(HealthStatus.UNHEALTHY);
        }
    }

    // ── ContainerLogMapper ──────────────────────────────────────────────

    @Nested
    class ContainerLogMapperTests {

        private final ContainerLogMapper mapper = Mappers.getMapper(ContainerLogMapper.class);

        @Test
        void toResponse_mapsFieldsAndExtractsContainerId() {
            ContainerInstance container = ContainerInstance.builder()
                    .containerName("my-postgres")
                    .serviceName("postgres")
                    .imageName("postgres")
                    .build();
            container.setId(CONTAINER_ID);

            ContainerLog entity = ContainerLog.builder()
                    .stream("stdout")
                    .content("database system is ready to accept connections")
                    .timestamp(NOW)
                    .container(container)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            ContainerLogResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.stream()).isEqualTo("stdout");
            assertThat(resp.content()).isEqualTo("database system is ready to accept connections");
            assertThat(resp.timestamp()).isEqualTo(NOW);
            assertThat(resp.containerId()).isEqualTo(CONTAINER_ID);
            assertThat(resp.createdAt()).isEqualTo(NOW);
        }

        @Test
        void toResponse_stderrLog_mapsCorrectly() {
            ContainerInstance container = ContainerInstance.builder()
                    .containerName("test").serviceName("test").imageName("test").build();
            container.setId(CONTAINER_ID);

            ContainerLog entity = ContainerLog.builder()
                    .stream("stderr")
                    .content("ERROR: permission denied")
                    .timestamp(NOW)
                    .container(container)
                    .build();

            ContainerLogResponse resp = mapper.toResponse(entity);

            assertThat(resp.stream()).isEqualTo("stderr");
            assertThat(resp.content()).isEqualTo("ERROR: permission denied");
        }

        @Test
        void toResponseList_mapsAllElements() {
            ContainerInstance container = ContainerInstance.builder()
                    .containerName("test").serviceName("test").imageName("test").build();
            container.setId(CONTAINER_ID);

            ContainerLog log1 = ContainerLog.builder()
                    .stream("stdout").content("line1").timestamp(NOW).container(container).build();
            ContainerLog log2 = ContainerLog.builder()
                    .stream("stderr").content("line2").timestamp(NOW).container(container).build();

            List<ContainerLogResponse> list = mapper.toResponseList(List.of(log1, log2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).stream()).isEqualTo("stdout");
            assertThat(list.get(1).stream()).isEqualTo("stderr");
        }
    }

    // ── VolumeMountMapper ───────────────────────────────────────────────

    @Nested
    class VolumeMountMapperTests {

        private final VolumeMountMapper mapper = Mappers.getMapper(VolumeMountMapper.class);

        @Test
        void toResponse_mapsFieldsIncludingBooleanIsReadOnly() {
            VolumeMount entity = VolumeMount.builder()
                    .hostPath("/data/postgres")
                    .containerPath("/var/lib/postgresql/data")
                    .volumeName("pgdata")
                    .isReadOnly(true)
                    .build();
            entity.setId(ID);

            VolumeMountResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.hostPath()).isEqualTo("/data/postgres");
            assertThat(resp.containerPath()).isEqualTo("/var/lib/postgresql/data");
            assertThat(resp.volumeName()).isEqualTo("pgdata");
            assertThat(resp.isReadOnly()).isTrue();
        }

        @Test
        void toResponse_readOnlyFalse_mapsFalse() {
            VolumeMount entity = VolumeMount.builder()
                    .containerPath("/app/data")
                    .build();

            VolumeMountResponse resp = mapper.toResponse(entity);

            assertThat(resp.isReadOnly()).isFalse();
        }

        @Test
        void toResponseList_mapsAllElements() {
            VolumeMount v1 = VolumeMount.builder().containerPath("/data1").build();
            VolumeMount v2 = VolumeMount.builder().containerPath("/data2").build();

            List<VolumeMountResponse> list = mapper.toResponseList(List.of(v1, v2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).containerPath()).isEqualTo("/data1");
            assertThat(list.get(1).containerPath()).isEqualTo("/data2");
        }
    }

    // ── NetworkConfigMapper ─────────────────────────────────────────────

    @Nested
    class NetworkConfigMapperTests {

        private final NetworkConfigMapper mapper = Mappers.getMapper(NetworkConfigMapper.class);

        @Test
        void toResponse_mapsAllFields() {
            NetworkConfig entity = NetworkConfig.builder()
                    .networkName("backend")
                    .aliases("[\"db\",\"postgres\"]")
                    .ipAddress("172.18.0.5")
                    .build();
            entity.setId(ID);

            NetworkConfigResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.networkName()).isEqualTo("backend");
            assertThat(resp.aliases()).isEqualTo("[\"db\",\"postgres\"]");
            assertThat(resp.ipAddress()).isEqualTo("172.18.0.5");
        }

        @Test
        void toResponse_nullOptionalFields_mapsNulls() {
            NetworkConfig entity = NetworkConfig.builder()
                    .networkName("frontend")
                    .build();

            NetworkConfigResponse resp = mapper.toResponse(entity);

            assertThat(resp.networkName()).isEqualTo("frontend");
            assertThat(resp.aliases()).isNull();
            assertThat(resp.ipAddress()).isNull();
        }

        @Test
        void toResponseList_mapsAllElements() {
            NetworkConfig n1 = NetworkConfig.builder().networkName("net1").build();
            NetworkConfig n2 = NetworkConfig.builder().networkName("net2").build();

            List<NetworkConfigResponse> list = mapper.toResponseList(List.of(n1, n2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).networkName()).isEqualTo("net1");
            assertThat(list.get(1).networkName()).isEqualTo("net2");
        }
    }

    // ── SolutionProfileMapper ───────────────────────────────────────────

    @Nested
    class SolutionProfileMapperTests {

        private final SolutionProfileMapper mapper = Mappers.getMapper(SolutionProfileMapper.class);

        @Test
        void toResponse_mapsFieldsIncludingBooleanIsDefault() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            ServiceProfile sp = ServiceProfile.builder().serviceName("svc1").imageName("img1").build();
            sp.setId(UUID.randomUUID());

            SolutionService svc = SolutionService.builder()
                    .startOrder(1).serviceProfile(sp).build();
            List<SolutionService> services = new ArrayList<>();
            services.add(svc);

            SolutionProfile entity = SolutionProfile.builder()
                    .name("Backend Stack")
                    .description("API + DB")
                    .isDefault(true)
                    .team(team)
                    .services(services)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            SolutionProfileResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.name()).isEqualTo("Backend Stack");
            assertThat(resp.description()).isEqualTo("API + DB");
            assertThat(resp.isDefault()).isTrue();
            assertThat(resp.serviceCount()).isEqualTo(1);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.createdAt()).isEqualTo(NOW);
        }

        @Test
        void toResponse_defaultFalse_andEmptyServices_mapsCorrectly() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            SolutionProfile entity = SolutionProfile.builder()
                    .name("Empty Solution")
                    .team(team)
                    .build();

            SolutionProfileResponse resp = mapper.toResponse(entity);

            assertThat(resp.isDefault()).isFalse();
            assertThat(resp.serviceCount()).isZero();
        }

        @Test
        void toDetailResponse_mapsAllFieldsWithServices() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            SolutionProfile entity = SolutionProfile.builder()
                    .name("Full Detail")
                    .description("With services")
                    .isDefault(true)
                    .team(team)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);
            entity.setUpdatedAt(NOW);

            List<SolutionServiceResponse> services = List.of(
                    new SolutionServiceResponse(UUID.randomUUID(), 1,
                            SERVICE_PROFILE_ID, "postgres", "postgres", true));

            SolutionProfileDetailResponse resp = mapper.toDetailResponse(entity, services);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.name()).isEqualTo("Full Detail");
            assertThat(resp.description()).isEqualTo("With services");
            assertThat(resp.isDefault()).isTrue();
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.services()).hasSize(1);
            assertThat(resp.createdAt()).isEqualTo(NOW);
            assertThat(resp.updatedAt()).isEqualTo(NOW);
        }

        @Test
        void toResponseList_mapsAllElements() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            SolutionProfile e1 = SolutionProfile.builder().name("sol1").team(team).build();
            SolutionProfile e2 = SolutionProfile.builder().name("sol2").team(team).build();

            List<SolutionProfileResponse> list = mapper.toResponseList(List.of(e1, e2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).name()).isEqualTo("sol1");
            assertThat(list.get(1).name()).isEqualTo("sol2");
        }
    }

    // ── SolutionServiceMapper ───────────────────────────────────────────

    @Nested
    class SolutionServiceMapperTests {

        private final SolutionServiceMapper mapper = Mappers.getMapper(SolutionServiceMapper.class);

        @Test
        void toResponse_mapsFieldsFromNestedServiceProfile() {
            ServiceProfile sp = ServiceProfile.builder()
                    .serviceName("postgres")
                    .imageName("postgres")
                    .isEnabled(true)
                    .build();
            sp.setId(SERVICE_PROFILE_ID);

            SolutionService entity = SolutionService.builder()
                    .startOrder(2)
                    .serviceProfile(sp)
                    .build();
            entity.setId(ID);

            SolutionServiceResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.startOrder()).isEqualTo(2);
            assertThat(resp.serviceProfileId()).isEqualTo(SERVICE_PROFILE_ID);
            assertThat(resp.serviceProfileName()).isEqualTo("postgres");
            assertThat(resp.imageName()).isEqualTo("postgres");
            assertThat(resp.isEnabled()).isTrue();
        }

        @Test
        void toResponse_disabledServiceProfile_mapsEnabledFalse() {
            ServiceProfile sp = ServiceProfile.builder()
                    .serviceName("disabled-svc")
                    .imageName("alpine")
                    .isEnabled(false)
                    .build();
            sp.setId(SERVICE_PROFILE_ID);

            SolutionService entity = SolutionService.builder()
                    .serviceProfile(sp)
                    .build();

            SolutionServiceResponse resp = mapper.toResponse(entity);

            assertThat(resp.isEnabled()).isFalse();
        }

        @Test
        void toResponseList_mapsAllElements() {
            ServiceProfile sp1 = ServiceProfile.builder().serviceName("svc1").imageName("img1").build();
            sp1.setId(UUID.randomUUID());
            ServiceProfile sp2 = ServiceProfile.builder().serviceName("svc2").imageName("img2").build();
            sp2.setId(UUID.randomUUID());

            SolutionService s1 = SolutionService.builder().startOrder(1).serviceProfile(sp1).build();
            SolutionService s2 = SolutionService.builder().startOrder(2).serviceProfile(sp2).build();

            List<SolutionServiceResponse> list = mapper.toResponseList(List.of(s1, s2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).serviceProfileName()).isEqualTo("svc1");
            assertThat(list.get(1).serviceProfileName()).isEqualTo("svc2");
        }
    }

    // ── WorkstationProfileMapper ────────────────────────────────────────

    @Nested
    class WorkstationProfileMapperTests {

        private final WorkstationProfileMapper mapper = Mappers.getMapper(WorkstationProfileMapper.class);

        @Test
        void toResponse_mapsFieldsIncludingBooleanIsDefault() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            User user = User.builder()
                    .email("test@test.com").passwordHash("h").displayName("Test").build();
            user.setId(USER_ID);

            SolutionProfile sol = SolutionProfile.builder().name("sol1").team(team).build();
            sol.setId(UUID.randomUUID());
            WorkstationSolution ws = WorkstationSolution.builder()
                    .startOrder(1).solutionProfile(sol).build();
            List<WorkstationSolution> solutions = new ArrayList<>();
            solutions.add(ws);

            WorkstationProfile entity = WorkstationProfile.builder()
                    .name("My Workstation")
                    .description("Dev env")
                    .isDefault(true)
                    .user(user)
                    .team(team)
                    .solutions(solutions)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            WorkstationProfileResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.name()).isEqualTo("My Workstation");
            assertThat(resp.description()).isEqualTo("Dev env");
            assertThat(resp.isDefault()).isTrue();
            assertThat(resp.solutionCount()).isEqualTo(1);
            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.createdAt()).isEqualTo(NOW);
        }

        @Test
        void toResponse_defaultFalse_andEmptySolutions_mapsCorrectly() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);
            User user = User.builder()
                    .email("t@t.com").passwordHash("h").displayName("T").build();
            user.setId(USER_ID);

            WorkstationProfile entity = WorkstationProfile.builder()
                    .name("Empty WS")
                    .user(user)
                    .team(team)
                    .build();

            WorkstationProfileResponse resp = mapper.toResponse(entity);

            assertThat(resp.isDefault()).isFalse();
            assertThat(resp.solutionCount()).isZero();
        }

        @Test
        void toDetailResponse_mapsAllFieldsWithSolutions() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);
            User user = User.builder()
                    .email("d@t.com").passwordHash("h").displayName("D").build();
            user.setId(USER_ID);

            WorkstationProfile entity = WorkstationProfile.builder()
                    .name("Detail WS")
                    .description("Full detail")
                    .isDefault(true)
                    .user(user)
                    .team(team)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);
            entity.setUpdatedAt(NOW);

            List<WorkstationSolutionResponse> solutions = List.of(
                    new WorkstationSolutionResponse(UUID.randomUUID(), 1, null,
                            SOLUTION_PROFILE_ID, "Backend Stack"));

            WorkstationProfileDetailResponse resp = mapper.toDetailResponse(entity, solutions);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.name()).isEqualTo("Detail WS");
            assertThat(resp.description()).isEqualTo("Full detail");
            assertThat(resp.isDefault()).isTrue();
            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.solutions()).hasSize(1);
            assertThat(resp.createdAt()).isEqualTo(NOW);
            assertThat(resp.updatedAt()).isEqualTo(NOW);
        }

        @Test
        void toResponseList_mapsAllElements() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);
            User user = User.builder()
                    .email("l@t.com").passwordHash("h").displayName("L").build();
            user.setId(USER_ID);

            WorkstationProfile e1 = WorkstationProfile.builder()
                    .name("ws1").user(user).team(team).build();
            WorkstationProfile e2 = WorkstationProfile.builder()
                    .name("ws2").user(user).team(team).build();

            List<WorkstationProfileResponse> list = mapper.toResponseList(List.of(e1, e2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).name()).isEqualTo("ws1");
            assertThat(list.get(1).name()).isEqualTo("ws2");
        }
    }

    // ── WorkstationSolutionMapper ───────────────────────────────────────

    @Nested
    class WorkstationSolutionMapperTests {

        private final WorkstationSolutionMapper mapper = Mappers.getMapper(WorkstationSolutionMapper.class);

        @Test
        void toResponse_mapsFieldsFromNestedSolutionProfile() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            SolutionProfile sol = SolutionProfile.builder()
                    .name("Backend Stack")
                    .team(team)
                    .build();
            sol.setId(SOLUTION_PROFILE_ID);

            WorkstationSolution entity = WorkstationSolution.builder()
                    .startOrder(1)
                    .overrideEnvVarsJson("{\"DB_HOST\": \"localhost\"}")
                    .solutionProfile(sol)
                    .build();
            entity.setId(ID);

            WorkstationSolutionResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.startOrder()).isEqualTo(1);
            assertThat(resp.overrideEnvVarsJson()).isEqualTo("{\"DB_HOST\": \"localhost\"}");
            assertThat(resp.solutionProfileId()).isEqualTo(SOLUTION_PROFILE_ID);
            assertThat(resp.solutionProfileName()).isEqualTo("Backend Stack");
        }

        @Test
        void toResponse_nullOverrideEnvVarsJson_mapsNull() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            SolutionProfile sol = SolutionProfile.builder().name("sol").team(team).build();
            sol.setId(SOLUTION_PROFILE_ID);

            WorkstationSolution entity = WorkstationSolution.builder()
                    .solutionProfile(sol)
                    .build();

            WorkstationSolutionResponse resp = mapper.toResponse(entity);

            assertThat(resp.overrideEnvVarsJson()).isNull();
        }

        @Test
        void toResponseList_mapsAllElements() {
            Team team = Team.builder().build();
            team.setId(TEAM_ID);

            SolutionProfile sol1 = SolutionProfile.builder().name("sol1").team(team).build();
            sol1.setId(UUID.randomUUID());
            SolutionProfile sol2 = SolutionProfile.builder().name("sol2").team(team).build();
            sol2.setId(UUID.randomUUID());

            WorkstationSolution ws1 = WorkstationSolution.builder()
                    .startOrder(1).solutionProfile(sol1).build();
            WorkstationSolution ws2 = WorkstationSolution.builder()
                    .startOrder(2).solutionProfile(sol2).build();

            List<WorkstationSolutionResponse> list = mapper.toResponseList(List.of(ws1, ws2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).solutionProfileName()).isEqualTo("sol1");
            assertThat(list.get(1).solutionProfileName()).isEqualTo("sol2");
        }
    }
}
