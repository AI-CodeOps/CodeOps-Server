package com.codeops.fleet.service;

import com.codeops.exception.CodeOpsException;
import com.codeops.fleet.config.DockerConfig;
import com.codeops.fleet.dto.response.ContainerStatsResponse;
import com.codeops.fleet.dto.response.DockerImageResponse;
import com.codeops.fleet.dto.response.DockerNetworkResponse;
import com.codeops.fleet.dto.response.DockerVolumeResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Low-level Docker Engine API client.
 *
 * <p>Handles HTTP communication with the Docker daemon for container,
 * image, volume, and network operations. This is a pure infrastructure service
 * with no business logic or persistence — higher-level services
 * (ContainerManagementService, FleetHealthService) call this service.</p>
 *
 * <p>All Docker API errors are wrapped in {@link CodeOpsException} with
 * the HTTP status code and response body for diagnostics.</p>
 */
@Service
@Slf4j
public class DockerEngineService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DockerConfig dockerConfig;

    /**
     * Constructs a new DockerEngineService.
     *
     * @param httpClient   the HTTP client for Docker API communication
     * @param objectMapper the Jackson object mapper for JSON parsing
     * @param dockerConfig the Docker connection configuration
     */
    public DockerEngineService(@Qualifier("dockerHttpClient") HttpClient httpClient,
                               ObjectMapper objectMapper,
                               DockerConfig dockerConfig) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.dockerConfig = dockerConfig;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Container Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Lists all containers from the Docker daemon.
     *
     * <p>Calls {@code GET /containers/json?all={allStatuses}}.</p>
     *
     * @param allStatuses if true, includes stopped containers; if false, only running
     * @return list of container metadata maps
     */
    public List<Map<String, Object>> listContainers(boolean allStatuses) {
        String path = "/containers/json?all=" + allStatuses;
        log.debug("Listing containers (allStatuses={})", allStatuses);
        return doGet(path, new TypeReference<>() {});
    }

    /**
     * Creates a container from the given configuration.
     *
     * <p>Calls {@code POST /containers/create?name={name}} with the config as the request body.</p>
     *
     * @param name   the container name
     * @param config the container configuration map (image, env, host config, etc.)
     * @return the created container ID
     */
    @SuppressWarnings("unchecked")
    public String createContainer(String name, Map<String, Object> config) {
        String path = "/containers/create?name=" + name;
        log.debug("Creating container name={}", name);
        Map<String, Object> response = doPost(path, config, Map.class);
        return (String) response.get("Id");
    }

    /**
     * Starts a stopped container.
     *
     * <p>Calls {@code POST /containers/{containerId}/start}.</p>
     *
     * @param containerId the Docker container ID
     */
    public void startContainer(String containerId) {
        String path = "/containers/" + containerId + "/start";
        log.debug("Starting container {}", containerId);
        doPost(path, null);
    }

    /**
     * Stops a running container with a grace period.
     *
     * <p>Calls {@code POST /containers/{containerId}/stop?t={timeoutSeconds}}.</p>
     *
     * @param containerId    the Docker container ID
     * @param timeoutSeconds seconds to wait before sending SIGKILL
     */
    public void stopContainer(String containerId, int timeoutSeconds) {
        String path = "/containers/" + containerId + "/stop?t=" + timeoutSeconds;
        log.debug("Stopping container {} (timeout={}s)", containerId, timeoutSeconds);
        doPost(path, null);
    }

    /**
     * Restarts a container with a grace period.
     *
     * <p>Calls {@code POST /containers/{containerId}/restart?t={timeoutSeconds}}.</p>
     *
     * @param containerId    the Docker container ID
     * @param timeoutSeconds seconds to wait before sending SIGKILL
     */
    public void restartContainer(String containerId, int timeoutSeconds) {
        String path = "/containers/" + containerId + "/restart?t=" + timeoutSeconds;
        log.debug("Restarting container {} (timeout={}s)", containerId, timeoutSeconds);
        doPost(path, null);
    }

    /**
     * Removes a container from the Docker daemon.
     *
     * <p>Calls {@code DELETE /containers/{containerId}?force={force}&v={removeVolumes}}.</p>
     *
     * @param containerId   the Docker container ID
     * @param force         if true, force-removes a running container
     * @param removeVolumes if true, removes anonymous volumes attached to the container
     */
    public void removeContainer(String containerId, boolean force, boolean removeVolumes) {
        String path = "/containers/" + containerId + "?force=" + force + "&v=" + removeVolumes;
        log.debug("Removing container {} (force={}, removeVolumes={})", containerId, force, removeVolumes);
        doDelete(path);
    }

    /**
     * Inspects a container and returns full detail.
     *
     * <p>Calls {@code GET /containers/{containerId}/json}.</p>
     *
     * @param containerId the Docker container ID
     * @return the full container inspection map
     */
    public Map<String, Object> inspectContainer(String containerId) {
        String path = "/containers/" + containerId + "/json";
        log.debug("Inspecting container {}", containerId);
        return doGet(path, new TypeReference<>() {});
    }

    /**
     * Retrieves container logs from stdout and stderr.
     *
     * <p>Calls {@code GET /containers/{containerId}/logs?stdout=true&stderr=true&tail={tailLines}&timestamps={timestamps}}.</p>
     *
     * @param containerId the Docker container ID
     * @param tailLines   number of lines to return from the end of the log
     * @param timestamps  if true, includes timestamps in log output
     * @return the log output as a string
     */
    public String getContainerLogs(String containerId, int tailLines, boolean timestamps) {
        String path = "/containers/" + containerId
                + "/logs?stdout=true&stderr=true&tail=" + tailLines
                + "&timestamps=" + timestamps;
        log.debug("Getting logs for container {} (tail={}, timestamps={})", containerId, tailLines, timestamps);
        return doGetString(path);
    }

    /**
     * Retrieves one-shot resource usage statistics for a container.
     *
     * <p>Calls {@code GET /containers/{containerId}/stats?stream=false} and parses
     * the response into a {@link ContainerStatsResponse}. CPU percentage is calculated as
     * {@code (cpuDelta / systemDelta) * numCpus * 100.0}.</p>
     *
     * @param containerId the Docker container ID
     * @return the parsed container stats
     */
    @SuppressWarnings("unchecked")
    public ContainerStatsResponse getContainerStats(String containerId) {
        String path = "/containers/" + containerId + "/stats?stream=false";
        log.debug("Getting stats for container {}", containerId);
        Map<String, Object> raw = doGet(path, new TypeReference<>() {});
        return parseContainerStats(containerId, raw);
    }

    /**
     * Pauses a running container.
     *
     * <p>Calls {@code POST /containers/{containerId}/pause}.</p>
     *
     * @param containerId the Docker container ID
     */
    public void pauseContainer(String containerId) {
        String path = "/containers/" + containerId + "/pause";
        log.debug("Pausing container {}", containerId);
        doPost(path, null);
    }

    /**
     * Unpauses a paused container.
     *
     * <p>Calls {@code POST /containers/{containerId}/unpause}.</p>
     *
     * @param containerId the Docker container ID
     */
    public void unpauseContainer(String containerId) {
        String path = "/containers/" + containerId + "/unpause";
        log.debug("Unpausing container {}", containerId);
        doPost(path, null);
    }

    /**
     * Executes a command inside a running container.
     *
     * <p>This is a two-step operation:
     * <ol>
     *   <li>{@code POST /containers/{containerId}/exec} — creates an exec instance</li>
     *   <li>{@code POST /exec/{execId}/start} — runs the exec and captures output</li>
     * </ol></p>
     *
     * @param containerId  the Docker container ID
     * @param command      the command to execute (passed to {@code /bin/sh -c})
     * @param attachStdout if true, captures stdout
     * @param attachStderr if true, captures stderr
     * @return the exec output as a string
     */
    @SuppressWarnings("unchecked")
    public String execInContainer(String containerId, String command,
                                  boolean attachStdout, boolean attachStderr) {
        log.debug("Executing in container {}: {}", containerId, command);

        // Step 1: Create exec instance
        String createPath = "/containers/" + containerId + "/exec";
        Map<String, Object> execConfig = Map.of(
                "AttachStdout", attachStdout,
                "AttachStderr", attachStderr,
                "Cmd", List.of("/bin/sh", "-c", command)
        );
        Map<String, Object> createResponse = doPost(createPath, execConfig, Map.class);
        String execId = (String) createResponse.get("Id");

        // Step 2: Start exec and capture output
        String startPath = "/exec/" + execId + "/start";
        Map<String, Object> startConfig = Map.of("Detach", false, "Tty", false);
        return doPostString(startPath, startConfig);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Image Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Lists all local Docker images.
     *
     * <p>Calls {@code GET /images/json} and maps each entry to a
     * {@link DockerImageResponse}.</p>
     *
     * @return list of Docker image metadata
     */
    @SuppressWarnings("unchecked")
    public List<DockerImageResponse> listImages() {
        log.debug("Listing Docker images");
        List<Map<String, Object>> raw = doGet("/images/json", new TypeReference<>() {});
        List<DockerImageResponse> images = new ArrayList<>();
        for (Map<String, Object> entry : raw) {
            String id = (String) entry.get("Id");
            List<String> repoTags = entry.get("RepoTags") != null
                    ? (List<String>) entry.get("RepoTags")
                    : Collections.emptyList();
            Long size = toLong(entry.get("Size"));
            Instant created = entry.get("Created") != null
                    ? Instant.ofEpochSecond(toLong(entry.get("Created")))
                    : null;
            images.add(new DockerImageResponse(id, repoTags, size, created));
        }
        return images;
    }

    /**
     * Pulls an image from a registry. Blocks until the pull is complete.
     *
     * <p>Calls {@code POST /images/create?fromImage={imageName}&tag={tag}}.</p>
     *
     * @param imageName the image name (e.g., {@code postgres})
     * @param tag       the image tag (e.g., {@code 16}, {@code latest})
     */
    public void pullImage(String imageName, String tag) {
        String path = "/images/create?fromImage=" + imageName + "&tag=" + tag;
        log.debug("Pulling image {}:{}", imageName, tag);
        doPost(path, null);
    }

    /**
     * Removes a local Docker image.
     *
     * <p>Calls {@code DELETE /images/{imageId}?force={force}}.</p>
     *
     * @param imageId the Docker image ID or name:tag
     * @param force   if true, force-removes even if in use
     */
    public void removeImage(String imageId, boolean force) {
        String path = "/images/" + imageId + "?force=" + force;
        log.debug("Removing image {} (force={})", imageId, force);
        doDelete(path);
    }

    /**
     * Prunes unused Docker images and returns the space reclaimed.
     *
     * <p>Calls {@code POST /images/prune}.</p>
     *
     * @return bytes of disk space reclaimed
     */
    @SuppressWarnings("unchecked")
    public long pruneImages() {
        log.debug("Pruning unused images");
        Map<String, Object> response = doPost("/images/prune", null, Map.class);
        return toLong(response.get("SpaceReclaimed"));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Volume Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Lists all Docker volumes.
     *
     * <p>Calls {@code GET /volumes} and extracts volumes from the
     * {@code Volumes} array in the response.</p>
     *
     * @return list of Docker volume metadata
     */
    @SuppressWarnings("unchecked")
    public List<DockerVolumeResponse> listVolumes() {
        log.debug("Listing Docker volumes");
        Map<String, Object> raw = doGet("/volumes", new TypeReference<>() {});
        List<Map<String, Object>> volumes = (List<Map<String, Object>>) raw.get("Volumes");
        if (volumes == null) {
            return Collections.emptyList();
        }
        List<DockerVolumeResponse> result = new ArrayList<>();
        for (Map<String, Object> v : volumes) {
            result.add(parseVolumeResponse(v));
        }
        return result;
    }

    /**
     * Creates a named Docker volume.
     *
     * <p>Calls {@code POST /volumes/create} with name and driver.</p>
     *
     * @param name   the volume name
     * @param driver the storage driver (e.g., {@code local})
     * @return the created volume metadata
     */
    @SuppressWarnings("unchecked")
    public DockerVolumeResponse createVolume(String name, String driver) {
        log.debug("Creating volume name={} driver={}", name, driver);
        Map<String, Object> body = Map.of("Name", name, "Driver", driver);
        Map<String, Object> response = doPost("/volumes/create", body, Map.class);
        return parseVolumeResponse(response);
    }

    /**
     * Removes a Docker volume.
     *
     * <p>Calls {@code DELETE /volumes/{name}?force={force}}.</p>
     *
     * @param name  the volume name
     * @param force if true, force-removes even if in use
     */
    public void removeVolume(String name, boolean force) {
        String path = "/volumes/" + name + "?force=" + force;
        log.debug("Removing volume {} (force={})", name, force);
        doDelete(path);
    }

    /**
     * Prunes unused Docker volumes and returns the space reclaimed.
     *
     * <p>Calls {@code POST /volumes/prune}.</p>
     *
     * @return bytes of disk space reclaimed
     */
    @SuppressWarnings("unchecked")
    public long pruneVolumes() {
        log.debug("Pruning unused volumes");
        Map<String, Object> response = doPost("/volumes/prune", null, Map.class);
        return toLong(response.get("SpaceReclaimed"));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Network Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Lists all Docker networks.
     *
     * <p>Calls {@code GET /networks} and maps each entry to a
     * {@link DockerNetworkResponse}.</p>
     *
     * @return list of Docker network metadata
     */
    @SuppressWarnings("unchecked")
    public List<DockerNetworkResponse> listNetworks() {
        log.debug("Listing Docker networks");
        List<Map<String, Object>> raw = doGet("/networks", new TypeReference<>() {});
        List<DockerNetworkResponse> result = new ArrayList<>();
        for (Map<String, Object> entry : raw) {
            result.add(parseNetworkResponse(entry));
        }
        return result;
    }

    /**
     * Creates a Docker network.
     *
     * <p>Calls {@code POST /networks/create} with name and driver.</p>
     *
     * @param name   the network name
     * @param driver the network driver (e.g., {@code bridge}, {@code overlay})
     * @return the created network ID
     */
    @SuppressWarnings("unchecked")
    public String createNetwork(String name, String driver) {
        log.debug("Creating network name={} driver={}", name, driver);
        Map<String, Object> body = Map.of("Name", name, "Driver", driver);
        Map<String, Object> response = doPost("/networks/create", body, Map.class);
        return (String) response.get("Id");
    }

    /**
     * Removes a Docker network.
     *
     * <p>Calls {@code DELETE /networks/{networkId}}.</p>
     *
     * @param networkId the Docker network ID
     */
    public void removeNetwork(String networkId) {
        String path = "/networks/" + networkId;
        log.debug("Removing network {}", networkId);
        doDelete(path);
    }

    /**
     * Connects a container to a network with optional aliases.
     *
     * <p>Calls {@code POST /networks/{networkId}/connect} with the container ID
     * and endpoint configuration.</p>
     *
     * @param networkId   the Docker network ID
     * @param containerId the Docker container ID
     * @param aliases     network aliases for service discovery (may be null or empty)
     */
    public void connectContainerToNetwork(String networkId, String containerId,
                                          List<String> aliases) {
        String path = "/networks/" + networkId + "/connect";
        log.debug("Connecting container {} to network {} with aliases {}", containerId, networkId, aliases);
        Map<String, Object> endpointConfig = aliases != null && !aliases.isEmpty()
                ? Map.of("Aliases", aliases)
                : Map.of();
        Map<String, Object> body = Map.of(
                "Container", containerId,
                "EndpointConfig", endpointConfig
        );
        doPost(path, body);
    }

    /**
     * Disconnects a container from a network.
     *
     * <p>Calls {@code POST /networks/{networkId}/disconnect} with the container ID
     * and force flag.</p>
     *
     * @param networkId   the Docker network ID
     * @param containerId the Docker container ID
     * @param force       if true, force-disconnects even if the container is running
     */
    public void disconnectContainerFromNetwork(String networkId, String containerId,
                                               boolean force) {
        String path = "/networks/" + networkId + "/disconnect";
        log.debug("Disconnecting container {} from network {} (force={})", containerId, networkId, force);
        Map<String, Object> body = Map.of("Container", containerId, "Force", force);
        doPost(path, body);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Internal HTTP Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Sends a GET request and deserializes the JSON response using a TypeReference.
     *
     * @param path    the API path (appended to base URL)
     * @param typeRef the Jackson type reference for deserialization
     * @param <T>     the response type
     * @return the deserialized response
     */
    <T> T doGet(String path, TypeReference<T> typeRef) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl(path)))
                    .GET()
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(dockerConfig.getReadTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Docker API GET {} returned {}: {}", path, response.statusCode(), response.body());
                throw new CodeOpsException(
                        "Docker API error: " + response.statusCode() + " - " + response.body());
            }

            log.debug("Docker API GET {} returned {}", path, response.statusCode());
            return objectMapper.readValue(response.body(), typeRef);
        } catch (CodeOpsException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeOpsException("Docker API call interrupted: GET " + path, e);
        } catch (IOException e) {
            log.error("Docker API GET {} failed: {}", path, e.getMessage());
            throw new CodeOpsException("Docker API call failed: GET " + path, e);
        }
    }

    /**
     * Sends a GET request and returns the raw response body as a string.
     *
     * @param path the API path (appended to base URL)
     * @return the response body string
     */
    String doGetString(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl(path)))
                    .GET()
                    .timeout(Duration.ofSeconds(dockerConfig.getReadTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Docker API GET {} returned {}: {}", path, response.statusCode(), response.body());
                throw new CodeOpsException(
                        "Docker API error: " + response.statusCode() + " - " + response.body());
            }

            log.debug("Docker API GET {} returned {}", path, response.statusCode());
            return response.body();
        } catch (CodeOpsException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeOpsException("Docker API call interrupted: GET " + path, e);
        } catch (IOException e) {
            log.error("Docker API GET {} failed: {}", path, e.getMessage());
            throw new CodeOpsException("Docker API call failed: GET " + path, e);
        }
    }

    /**
     * Sends a POST request with a JSON body and deserializes the response.
     *
     * @param path         the API path (appended to base URL)
     * @param body         the request body (serialized to JSON), or null for empty body
     * @param responseType the response class type
     * @param <T>          the response type
     * @return the deserialized response
     */
    <T> T doPost(String path, Object body, Class<T> responseType) {
        try {
            String jsonBody = body != null ? objectMapper.writeValueAsString(body) : "";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl(path)))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(dockerConfig.getReadTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Docker API POST {} returned {}: {}", path, response.statusCode(), response.body());
                throw new CodeOpsException(
                        "Docker API error: " + response.statusCode() + " - " + response.body());
            }

            log.debug("Docker API POST {} returned {}", path, response.statusCode());
            return objectMapper.readValue(response.body(), responseType);
        } catch (CodeOpsException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeOpsException("Docker API call interrupted: POST " + path, e);
        } catch (IOException e) {
            log.error("Docker API POST {} failed: {}", path, e.getMessage());
            throw new CodeOpsException("Docker API call failed: POST " + path, e);
        }
    }

    /**
     * Sends a POST request with a JSON body, ignoring the response body.
     *
     * @param path the API path (appended to base URL)
     * @param body the request body (serialized to JSON), or null for empty body
     */
    void doPost(String path, Object body) {
        try {
            String jsonBody = body != null ? objectMapper.writeValueAsString(body) : "";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl(path)))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(dockerConfig.getReadTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Docker API POST {} returned {}: {}", path, response.statusCode(), response.body());
                throw new CodeOpsException(
                        "Docker API error: " + response.statusCode() + " - " + response.body());
            }

            log.debug("Docker API POST {} returned {}", path, response.statusCode());
        } catch (CodeOpsException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeOpsException("Docker API call interrupted: POST " + path, e);
        } catch (IOException e) {
            log.error("Docker API POST {} failed: {}", path, e.getMessage());
            throw new CodeOpsException("Docker API call failed: POST " + path, e);
        }
    }

    /**
     * Sends a POST request and returns the raw response body as a string.
     *
     * @param path the API path (appended to base URL)
     * @param body the request body (serialized to JSON), or null for empty body
     * @return the response body string
     */
    String doPostString(String path, Object body) {
        try {
            String jsonBody = body != null ? objectMapper.writeValueAsString(body) : "";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl(path)))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(dockerConfig.getReadTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Docker API POST {} returned {}: {}", path, response.statusCode(), response.body());
                throw new CodeOpsException(
                        "Docker API error: " + response.statusCode() + " - " + response.body());
            }

            log.debug("Docker API POST {} returned {}", path, response.statusCode());
            return response.body();
        } catch (CodeOpsException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeOpsException("Docker API call interrupted: POST " + path, e);
        } catch (IOException e) {
            log.error("Docker API POST {} failed: {}", path, e.getMessage());
            throw new CodeOpsException("Docker API call failed: POST " + path, e);
        }
    }

    /**
     * Sends a DELETE request, ignoring the response body.
     *
     * @param path the API path (appended to base URL)
     */
    void doDelete(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl(path)))
                    .DELETE()
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(dockerConfig.getReadTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Docker API DELETE {} returned {}: {}", path, response.statusCode(), response.body());
                throw new CodeOpsException(
                        "Docker API error: " + response.statusCode() + " - " + response.body());
            }

            log.debug("Docker API DELETE {} returned {}", path, response.statusCode());
        } catch (CodeOpsException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeOpsException("Docker API call interrupted: DELETE " + path, e);
        } catch (IOException e) {
            log.error("Docker API DELETE {} failed: {}", path, e.getMessage());
            throw new CodeOpsException("Docker API call failed: DELETE " + path, e);
        }
    }

    /**
     * Builds the full URL for a Docker Engine API call.
     *
     * @param path the API path (e.g., {@code /containers/json})
     * @return the full URL string
     */
    String buildUrl(String path) {
        return dockerConfig.getBaseUrl() + path;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Response Parsers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Parses a raw Docker stats JSON map into a {@link ContainerStatsResponse}.
     *
     * <p>CPU percentage is calculated as:
     * {@code (cpuDelta / systemDelta) * numCpus * 100.0}</p>
     */
    @SuppressWarnings("unchecked")
    private ContainerStatsResponse parseContainerStats(String containerId, Map<String, Object> raw) {
        String name = (String) raw.get("name");
        if (name != null && name.startsWith("/")) {
            name = name.substring(1);
        }

        // CPU calculation
        Map<String, Object> cpuStats = (Map<String, Object>) raw.getOrDefault("cpu_stats", Map.of());
        Map<String, Object> preCpuStats = (Map<String, Object>) raw.getOrDefault("precpu_stats", Map.of());
        Map<String, Object> cpuUsage = (Map<String, Object>) cpuStats.getOrDefault("cpu_usage", Map.of());
        Map<String, Object> preCpuUsage = (Map<String, Object>) preCpuStats.getOrDefault("cpu_usage", Map.of());

        long totalUsage = toLong(cpuUsage.get("total_usage"));
        long preTotalUsage = toLong(preCpuUsage.get("total_usage"));
        long systemUsage = toLong(cpuStats.get("system_cpu_usage"));
        long preSystemUsage = toLong(preCpuStats.get("system_cpu_usage"));
        int numCpus = cpuStats.get("online_cpus") != null
                ? ((Number) cpuStats.get("online_cpus")).intValue()
                : 1;

        double cpuPercent = 0.0;
        long cpuDelta = totalUsage - preTotalUsage;
        long systemDelta = systemUsage - preSystemUsage;
        if (systemDelta > 0 && cpuDelta > 0) {
            cpuPercent = ((double) cpuDelta / systemDelta) * numCpus * 100.0;
        }

        // Memory
        Map<String, Object> memStats = (Map<String, Object>) raw.getOrDefault("memory_stats", Map.of());
        long memoryUsage = toLong(memStats.get("usage"));
        long memoryLimit = toLong(memStats.get("limit"));

        // Network
        Map<String, Object> networks = (Map<String, Object>) raw.getOrDefault("networks", Map.of());
        long rxBytes = 0;
        long txBytes = 0;
        for (Object netObj : networks.values()) {
            Map<String, Object> net = (Map<String, Object>) netObj;
            rxBytes += toLong(net.get("rx_bytes"));
            txBytes += toLong(net.get("tx_bytes"));
        }

        // Block I/O
        Map<String, Object> blkioStats = (Map<String, Object>) raw.getOrDefault("blkio_stats", Map.of());
        List<Map<String, Object>> ioEntries = (List<Map<String, Object>>) blkioStats.getOrDefault(
                "io_service_bytes_recursive", Collections.emptyList());
        long blockRead = 0;
        long blockWrite = 0;
        if (ioEntries != null) {
            for (Map<String, Object> entry : ioEntries) {
                String op = (String) entry.get("op");
                long value = toLong(entry.get("value"));
                if ("read".equalsIgnoreCase(op)) {
                    blockRead += value;
                } else if ("write".equalsIgnoreCase(op)) {
                    blockWrite += value;
                }
            }
        }

        // PIDs
        Map<String, Object> pidsStats = (Map<String, Object>) raw.getOrDefault("pids_stats", Map.of());
        Integer pids = pidsStats.get("current") != null
                ? ((Number) pidsStats.get("current")).intValue()
                : null;

        return new ContainerStatsResponse(
                null, // containerId is the DB UUID — the caller maps this
                name,
                cpuPercent,
                memoryUsage,
                memoryLimit,
                rxBytes,
                txBytes,
                blockRead,
                blockWrite,
                pids,
                Instant.now()
        );
    }

    /**
     * Parses a raw Docker volume JSON map into a {@link DockerVolumeResponse}.
     */
    @SuppressWarnings("unchecked")
    private DockerVolumeResponse parseVolumeResponse(Map<String, Object> v) {
        String name = (String) v.get("Name");
        String driver = (String) v.get("Driver");
        String mountpoint = (String) v.get("Mountpoint");
        Map<String, String> labels = v.get("Labels") != null
                ? (Map<String, String>) v.get("Labels")
                : Collections.emptyMap();
        Instant createdAt = v.get("CreatedAt") != null
                ? Instant.parse((String) v.get("CreatedAt"))
                : null;
        return new DockerVolumeResponse(name, driver, mountpoint, labels, createdAt);
    }

    /**
     * Parses a raw Docker network JSON map into a {@link DockerNetworkResponse}.
     */
    @SuppressWarnings("unchecked")
    private DockerNetworkResponse parseNetworkResponse(Map<String, Object> entry) {
        String id = (String) entry.get("Id");
        String name = (String) entry.get("Name");
        String driver = (String) entry.get("Driver");

        // Extract subnet and gateway from IPAM config
        String subnet = null;
        String gateway = null;
        Map<String, Object> ipam = (Map<String, Object>) entry.get("IPAM");
        if (ipam != null) {
            List<Map<String, String>> configs = (List<Map<String, String>>) ipam.get("Config");
            if (configs != null && !configs.isEmpty()) {
                subnet = configs.get(0).get("Subnet");
                gateway = configs.get(0).get("Gateway");
            }
        }

        // Extract connected containers
        Map<String, Object> containers = (Map<String, Object>) entry.getOrDefault("Containers", Map.of());
        List<String> connectedContainers = new ArrayList<>(containers.keySet());

        return new DockerNetworkResponse(id, name, driver, subnet, gateway, connectedContainers);
    }

    /**
     * Safely converts a value to a long, returning 0 if null.
     */
    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }
}
