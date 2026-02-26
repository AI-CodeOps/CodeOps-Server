package com.codeops.fleet.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerConfig} base URL construction and default values.
 */
class DockerConfigTest {

    /**
     * Verifies that a TCP Docker host is converted to a proper HTTP base URL.
     * {@code tcp://localhost:2375} → {@code http://localhost:2375/v1.43}
     */
    @Test
    void getBaseUrl_tcpHost_returnsHttpUrl() throws Exception {
        DockerConfig config = createConfig("tcp://localhost:2375", "v1.43", 5, 30);

        assertThat(config.getBaseUrl()).isEqualTo("http://localhost:2375/v1.43");
    }

    /**
     * Verifies that a Unix socket Docker host is converted to a localhost HTTP URL.
     * {@code unix:///var/run/docker.sock} → {@code http://localhost/v1.43}
     */
    @Test
    void getBaseUrl_unixSocket_returnsLocalhostUrl() throws Exception {
        DockerConfig config = createConfig("unix:///var/run/docker.sock", "v1.43", 5, 30);

        assertThat(config.getBaseUrl()).isEqualTo("http://localhost/v1.43");
    }

    /**
     * Verifies that default field values are correctly set.
     */
    @Test
    void defaultValues_areCorrectlySet() throws Exception {
        DockerConfig config = createConfig("unix:///var/run/docker.sock", "v1.43", 5, 30);

        assertThat(config.getDockerHost()).isEqualTo("unix:///var/run/docker.sock");
        assertThat(config.getApiVersion()).isEqualTo("v1.43");
        assertThat(config.getReadTimeoutSeconds()).isEqualTo(30);
    }

    /**
     * Verifies that a TCP host with a custom port and API version works correctly.
     */
    @Test
    void getBaseUrl_customPortAndVersion_returnsCorrectUrl() throws Exception {
        DockerConfig config = createConfig("tcp://192.168.1.100:2376", "v1.44", 10, 60);

        assertThat(config.getBaseUrl()).isEqualTo("http://192.168.1.100:2376/v1.44");
        assertThat(config.getDockerHost()).isEqualTo("tcp://192.168.1.100:2376");
        assertThat(config.getApiVersion()).isEqualTo("v1.44");
        assertThat(config.getReadTimeoutSeconds()).isEqualTo(60);
    }

    /**
     * Creates a DockerConfig with the given field values via reflection.
     * This avoids needing a full Spring context for unit tests.
     */
    private DockerConfig createConfig(String dockerHost, String apiVersion,
                                       int connectTimeoutSeconds, int readTimeoutSeconds)
            throws Exception {
        DockerConfig config = new DockerConfig();
        setField(config, "dockerHost", dockerHost);
        setField(config, "apiVersion", apiVersion);
        setField(config, "connectTimeoutSeconds", connectTimeoutSeconds);
        setField(config, "readTimeoutSeconds", readTimeoutSeconds);
        return config;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
