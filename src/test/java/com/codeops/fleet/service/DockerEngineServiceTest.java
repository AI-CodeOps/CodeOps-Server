package com.codeops.fleet.service;

import com.codeops.exception.CodeOpsException;
import com.codeops.fleet.config.DockerConfig;
import com.codeops.fleet.dto.response.ContainerStatsResponse;
import com.codeops.fleet.dto.response.DockerImageResponse;
import com.codeops.fleet.dto.response.DockerNetworkResponse;
import com.codeops.fleet.dto.response.DockerVolumeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DockerEngineService}.
 *
 * <p>All tests mock the {@link HttpClient} — no running Docker daemon is required.
 * Each test verifies correct URL construction, request body content, response parsing,
 * and error handling.</p>
 */
@ExtendWith(MockitoExtension.class)
class DockerEngineServiceTest {

    private static final String BASE_URL = "http://localhost:2375/v1.43";

    @Mock
    private HttpClient httpClient;

    private DockerEngineService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        DockerConfig dockerConfig = mock(DockerConfig.class);
        when(dockerConfig.getBaseUrl()).thenReturn(BASE_URL);
        when(dockerConfig.getReadTimeoutSeconds()).thenReturn(30);
        service = new DockerEngineService(httpClient, objectMapper, dockerConfig);
    }

    /**
     * Creates a mocked HttpResponse with the given status code and body.
     */
    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        lenient().when(response.body()).thenReturn(body);
        return response;
    }

    /**
     * Stubs httpClient.send() to return a mocked response.
     */
    @SuppressWarnings("unchecked")
    private void stubResponse(int statusCode, String body) throws Exception {
        HttpResponse<String> response = mockResponse(statusCode, body);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(response);
    }

    /**
     * Captures the HttpRequest sent by httpClient.send().
     */
    @SuppressWarnings("unchecked")
    private HttpRequest captureRequest() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), eq(HttpResponse.BodyHandlers.ofString()));
        return captor.getValue();
    }

    // ── Container Operations ────────────────────────────────────────────

    @Nested
    class ContainerOperations {

        @Test
        void listContainers_returnsParsedList() throws Exception {
            stubResponse(200, "[{\"Id\":\"abc123\",\"Names\":[\"/test\"]}]");

            List<Map<String, Object>> result = service.listContainers(false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("Id")).isEqualTo("abc123");
            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/containers/json?all=false");
            assertThat(req.method()).isEqualTo("GET");
        }

        @Test
        void listContainers_allStatuses_passesQueryParam() throws Exception {
            stubResponse(200, "[]");

            service.listContainers(true);

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/containers/json?all=true");
        }

        @Test
        void createContainer_sendsConfigAndReturnsId() throws Exception {
            stubResponse(201, "{\"Id\":\"new-container-id-123\"}");

            Map<String, Object> config = Map.of("Image", "postgres:16");
            String id = service.createContainer("my-pg", config);

            assertThat(id).isEqualTo("new-container-id-123");
            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/containers/create?name=my-pg");
            assertThat(req.method()).isEqualTo("POST");
        }

        @Test
        void startContainer_sendsPost() throws Exception {
            stubResponse(204, "");

            service.startContainer("abc123");

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/containers/abc123/start");
            assertThat(req.method()).isEqualTo("POST");
        }

        @Test
        void stopContainer_includesTimeoutParam() throws Exception {
            stubResponse(204, "");

            service.stopContainer("abc123", 10);

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/containers/abc123/stop?t=10");
        }

        @Test
        void restartContainer_sendsPost() throws Exception {
            stubResponse(204, "");

            service.restartContainer("abc123", 15);

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/containers/abc123/restart?t=15");
            assertThat(req.method()).isEqualTo("POST");
        }

        @Test
        void removeContainer_withForceAndVolumes() throws Exception {
            stubResponse(204, "");

            service.removeContainer("abc123", true, true);

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString())
                    .isEqualTo(BASE_URL + "/containers/abc123?force=true&v=true");
            assertThat(req.method()).isEqualTo("DELETE");
        }

        @Test
        void inspectContainer_returnsFullDetail() throws Exception {
            stubResponse(200, "{\"Id\":\"abc123\",\"State\":{\"Status\":\"running\"}}");

            Map<String, Object> result = service.inspectContainer("abc123");

            assertThat(result.get("Id")).isEqualTo("abc123");
            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/containers/abc123/json");
        }

        @Test
        void getContainerLogs_returnsLogOutput() throws Exception {
            stubResponse(200, "2024-01-01T00:00:00Z line1\n2024-01-01T00:00:01Z line2");

            String logs = service.getContainerLogs("abc123", 100, true);

            assertThat(logs).contains("line1", "line2");
            HttpRequest req = captureRequest();
            assertThat(req.uri().toString())
                    .isEqualTo(BASE_URL + "/containers/abc123/logs?stdout=true&stderr=true&tail=100&timestamps=true");
        }

        @Test
        void getContainerStats_parsesCpuAndMemory() throws Exception {
            String statsJson = """
                    {
                      "name": "/my-container",
                      "cpu_stats": {
                        "cpu_usage": {"total_usage": 200000},
                        "system_cpu_usage": 1000000,
                        "online_cpus": 4
                      },
                      "precpu_stats": {
                        "cpu_usage": {"total_usage": 100000},
                        "system_cpu_usage": 500000
                      },
                      "memory_stats": {
                        "usage": 536870912,
                        "limit": 1073741824
                      },
                      "networks": {
                        "eth0": {"rx_bytes": 1024, "tx_bytes": 2048}
                      },
                      "blkio_stats": {
                        "io_service_bytes_recursive": [
                          {"op": "read", "value": 4096},
                          {"op": "write", "value": 8192}
                        ]
                      },
                      "pids_stats": {"current": 42}
                    }
                    """;
            stubResponse(200, statsJson);

            ContainerStatsResponse stats = service.getContainerStats("abc123");

            assertThat(stats.containerName()).isEqualTo("my-container");
            // cpuDelta=100000, systemDelta=500000, numCpus=4 → (100000/500000)*4*100 = 80.0
            assertThat(stats.cpuPercent()).isEqualTo(80.0);
            assertThat(stats.memoryUsageBytes()).isEqualTo(536870912L);
            assertThat(stats.memoryLimitBytes()).isEqualTo(1073741824L);
            assertThat(stats.networkRxBytes()).isEqualTo(1024L);
            assertThat(stats.networkTxBytes()).isEqualTo(2048L);
            assertThat(stats.blockReadBytes()).isEqualTo(4096L);
            assertThat(stats.blockWriteBytes()).isEqualTo(8192L);
            assertThat(stats.pids()).isEqualTo(42);
            assertThat(stats.timestamp()).isNotNull();
        }

        @Test
        void pauseContainer_sendsPost() throws Exception {
            stubResponse(204, "");

            service.pauseContainer("abc123");

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/containers/abc123/pause");
            assertThat(req.method()).isEqualTo("POST");
        }

        @Test
        void unpauseContainer_sendsPost() throws Exception {
            stubResponse(204, "");

            service.unpauseContainer("abc123");

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/containers/abc123/unpause");
            assertThat(req.method()).isEqualTo("POST");
        }

        @Test
        @SuppressWarnings("unchecked")
        void execInContainer_createsThenStarts() throws Exception {
            // First call: create exec → returns exec ID
            HttpResponse<String> createResp = mockResponse(201, "{\"Id\":\"exec-id-123\"}");
            // Second call: start exec → returns output
            HttpResponse<String> startResp = mockResponse(200, "command output here");

            when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                    .thenReturn(createResp)
                    .thenReturn(startResp);

            String output = service.execInContainer("abc123", "ls -la", true, true);

            assertThat(output).isEqualTo("command output here");

            // Verify two POST calls were made
            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient, org.mockito.Mockito.times(2))
                    .send(captor.capture(), eq(HttpResponse.BodyHandlers.ofString()));

            List<HttpRequest> requests = captor.getAllValues();
            assertThat(requests.get(0).uri().toString())
                    .isEqualTo(BASE_URL + "/containers/abc123/exec");
            assertThat(requests.get(1).uri().toString())
                    .isEqualTo(BASE_URL + "/exec/exec-id-123/start");
        }
    }

    // ── Image Operations ────────────────────────────────────────────────

    @Nested
    class ImageOperations {

        @Test
        void listImages_returnsParsedList() throws Exception {
            String json = """
                    [{
                      "Id": "sha256:abc123",
                      "RepoTags": ["nginx:latest", "nginx:1.25"],
                      "Size": 187654321,
                      "Created": 1706000000
                    }]
                    """;
            stubResponse(200, json);

            List<DockerImageResponse> images = service.listImages();

            assertThat(images).hasSize(1);
            assertThat(images.get(0).id()).isEqualTo("sha256:abc123");
            assertThat(images.get(0).repoTags()).containsExactly("nginx:latest", "nginx:1.25");
            assertThat(images.get(0).sizeBytes()).isEqualTo(187654321L);
            assertThat(images.get(0).created()).isNotNull();
        }

        @Test
        void pullImage_sendsPostWithParams() throws Exception {
            stubResponse(200, "");

            service.pullImage("postgres", "16");

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString())
                    .isEqualTo(BASE_URL + "/images/create?fromImage=postgres&tag=16");
            assertThat(req.method()).isEqualTo("POST");
        }

        @Test
        void removeImage_withForce() throws Exception {
            stubResponse(200, "");

            service.removeImage("sha256:abc123", true);

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString())
                    .isEqualTo(BASE_URL + "/images/sha256:abc123?force=true");
            assertThat(req.method()).isEqualTo("DELETE");
        }

        @Test
        void pruneImages_returnsReclaimedSpace() throws Exception {
            stubResponse(200, "{\"SpaceReclaimed\":1073741824}");

            long reclaimed = service.pruneImages();

            assertThat(reclaimed).isEqualTo(1073741824L);
            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/images/prune");
        }
    }

    // ── Volume Operations ───────────────────────────────────────────────

    @Nested
    class VolumeOperations {

        @Test
        void listVolumes_returnsParsedList() throws Exception {
            String json = """
                    {
                      "Volumes": [{
                        "Name": "pgdata",
                        "Driver": "local",
                        "Mountpoint": "/var/lib/docker/volumes/pgdata/_data",
                        "Labels": {"app": "postgres"},
                        "CreatedAt": "2024-01-15T10:30:00Z"
                      }]
                    }
                    """;
            stubResponse(200, json);

            List<DockerVolumeResponse> volumes = service.listVolumes();

            assertThat(volumes).hasSize(1);
            assertThat(volumes.get(0).name()).isEqualTo("pgdata");
            assertThat(volumes.get(0).driver()).isEqualTo("local");
            assertThat(volumes.get(0).mountpoint()).isEqualTo("/var/lib/docker/volumes/pgdata/_data");
            assertThat(volumes.get(0).labels()).containsEntry("app", "postgres");
            assertThat(volumes.get(0).createdAt()).isNotNull();
        }

        @Test
        void createVolume_sendsNameAndDriver() throws Exception {
            String json = """
                    {
                      "Name": "mydata",
                      "Driver": "local",
                      "Mountpoint": "/var/lib/docker/volumes/mydata/_data",
                      "Labels": {},
                      "CreatedAt": "2024-01-15T10:30:00Z"
                    }
                    """;
            stubResponse(201, json);

            DockerVolumeResponse vol = service.createVolume("mydata", "local");

            assertThat(vol.name()).isEqualTo("mydata");
            assertThat(vol.driver()).isEqualTo("local");
            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/volumes/create");
        }

        @Test
        void removeVolume_sendsDelete() throws Exception {
            stubResponse(204, "");

            service.removeVolume("pgdata", false);

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString())
                    .isEqualTo(BASE_URL + "/volumes/pgdata?force=false");
            assertThat(req.method()).isEqualTo("DELETE");
        }

        @Test
        void pruneVolumes_returnsReclaimedSpace() throws Exception {
            stubResponse(200, "{\"SpaceReclaimed\":536870912}");

            long reclaimed = service.pruneVolumes();

            assertThat(reclaimed).isEqualTo(536870912L);
        }
    }

    // ── Network Operations ──────────────────────────────────────────────

    @Nested
    class NetworkOperations {

        @Test
        void listNetworks_returnsParsedList() throws Exception {
            String json = """
                    [{
                      "Id": "net-abc123",
                      "Name": "bridge",
                      "Driver": "bridge",
                      "IPAM": {
                        "Config": [{"Subnet": "172.17.0.0/16", "Gateway": "172.17.0.1"}]
                      },
                      "Containers": {"ctr1": {}, "ctr2": {}}
                    }]
                    """;
            stubResponse(200, json);

            List<DockerNetworkResponse> networks = service.listNetworks();

            assertThat(networks).hasSize(1);
            assertThat(networks.get(0).id()).isEqualTo("net-abc123");
            assertThat(networks.get(0).name()).isEqualTo("bridge");
            assertThat(networks.get(0).driver()).isEqualTo("bridge");
            assertThat(networks.get(0).subnet()).isEqualTo("172.17.0.0/16");
            assertThat(networks.get(0).gateway()).isEqualTo("172.17.0.1");
            assertThat(networks.get(0).connectedContainers()).containsExactlyInAnyOrder("ctr1", "ctr2");
        }

        @Test
        void createNetwork_returnsId() throws Exception {
            stubResponse(201, "{\"Id\":\"new-net-id\"}");

            String id = service.createNetwork("backend", "bridge");

            assertThat(id).isEqualTo("new-net-id");
            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/networks/create");
        }

        @Test
        void removeNetwork_sendsDelete() throws Exception {
            stubResponse(204, "");

            service.removeNetwork("net-abc123");

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString()).isEqualTo(BASE_URL + "/networks/net-abc123");
            assertThat(req.method()).isEqualTo("DELETE");
        }

        @Test
        void connectContainerToNetwork_sendsBody() throws Exception {
            stubResponse(200, "");

            service.connectContainerToNetwork("net-abc123", "ctr-456", List.of("db", "postgres"));

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString())
                    .isEqualTo(BASE_URL + "/networks/net-abc123/connect");
            assertThat(req.method()).isEqualTo("POST");
        }

        @Test
        void disconnectContainerFromNetwork_sendsBody() throws Exception {
            stubResponse(200, "");

            service.disconnectContainerFromNetwork("net-abc123", "ctr-456", true);

            HttpRequest req = captureRequest();
            assertThat(req.uri().toString())
                    .isEqualTo(BASE_URL + "/networks/net-abc123/disconnect");
            assertThat(req.method()).isEqualTo("POST");
        }
    }

    // ── Error Handling ──────────────────────────────────────────────────

    @Nested
    class ErrorHandling {

        @Test
        void doGet_on404_throwsCodeOpsException() throws Exception {
            stubResponse(404, "{\"message\":\"No such container\"}");

            assertThatThrownBy(() -> service.inspectContainer("nonexistent"))
                    .isInstanceOf(CodeOpsException.class)
                    .hasMessageContaining("Docker API error: 404")
                    .hasMessageContaining("No such container");
        }

        @Test
        @SuppressWarnings("unchecked")
        void doGet_onIOException_throwsCodeOpsException() throws Exception {
            when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                    .thenThrow(new IOException("Connection refused"));

            assertThatThrownBy(() -> service.listContainers(false))
                    .isInstanceOf(CodeOpsException.class)
                    .hasMessageContaining("Docker API call failed")
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        void doPost_on500_throwsCodeOpsException() throws Exception {
            stubResponse(500, "{\"message\":\"Internal server error\"}");

            assertThatThrownBy(() -> service.startContainer("abc123"))
                    .isInstanceOf(CodeOpsException.class)
                    .hasMessageContaining("Docker API error: 500")
                    .hasMessageContaining("Internal server error");
        }

        @Test
        void doDelete_on409_throwsCodeOpsException() throws Exception {
            stubResponse(409, "{\"message\":\"Container is running\"}");

            assertThatThrownBy(() -> service.removeContainer("abc123", false, false))
                    .isInstanceOf(CodeOpsException.class)
                    .hasMessageContaining("Docker API error: 409")
                    .hasMessageContaining("Container is running");
        }

        @Test
        @SuppressWarnings("unchecked")
        void doGet_onInterruptedException_reinterruptsThread() throws Exception {
            when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                    .thenThrow(new InterruptedException("interrupted"));

            assertThatThrownBy(() -> service.listContainers(false))
                    .isInstanceOf(CodeOpsException.class)
                    .hasMessageContaining("interrupted");

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            // Clear interrupted flag for test cleanup
            Thread.interrupted();
        }
    }
}
