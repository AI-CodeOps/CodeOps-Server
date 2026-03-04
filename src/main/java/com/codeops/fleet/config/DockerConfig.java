package com.codeops.fleet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Docker Engine connection configuration.
 *
 * <p>Supports both Unix socket and TCP connections to the Docker daemon.
 * In development, Docker Desktop exposes the API via TCP on port 2375.
 * In production, the Unix socket at {@code /var/run/docker.sock} is used.</p>
 *
 * <p>TODO: Add {@code org.newsclub.net.unix:junixsocket-common} dependency
 * for native Unix socket support. Currently TCP is used for all environments.</p>
 */
@Configuration
public class DockerConfig {

    @Value("${codeops.fleet.docker.host:unix:///var/run/docker.sock}")
    private String dockerHost;

    @Value("${codeops.fleet.docker.api-version:v1.47}")
    private String apiVersion;

    @Value("${codeops.fleet.docker.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${codeops.fleet.docker.read-timeout-seconds:30}")
    private int readTimeoutSeconds;

    /**
     * Creates an {@link HttpClient} configured for Docker Engine API communication.
     *
     * @return the configured HTTP client for Docker API calls
     */
    @Bean
    public HttpClient dockerHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * Returns the configured Docker host URI (e.g., {@code tcp://localhost:2375}
     * or {@code unix:///var/run/docker.sock}).
     *
     * @return the Docker host URI string
     */
    public String getDockerHost() {
        return dockerHost;
    }

    /**
     * Returns the Docker Engine API version (e.g., {@code v1.43}).
     *
     * @return the API version string
     */
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * Returns the read timeout in seconds for Docker API calls.
     *
     * @return the read timeout in seconds
     */
    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    /**
     * Builds the base URL for Docker Engine API calls.
     *
     * <p>For TCP hosts (e.g., {@code tcp://localhost:2375}), returns
     * {@code http://localhost:2375/v1.43}.</p>
     *
     * <p>For Unix socket hosts, returns {@code http://localhost/v1.43}
     * (the actual socket path is handled at the transport level).</p>
     *
     * @return the base URL string including API version
     */
    public String getBaseUrl() {
        if (dockerHost.startsWith("tcp://")) {
            String hostPort = dockerHost.substring("tcp://".length());
            return "http://" + hostPort + "/" + apiVersion;
        }
        // Unix socket — route through localhost; actual socket handled at transport level
        return "http://localhost/" + apiVersion;
    }
}
