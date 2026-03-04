package com.codeops.e2e;

import com.codeops.integration.TestRateLimitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for end-to-end integration tests using Testcontainers.
 *
 * <p>Boots the full Spring application context with a real PostgreSQL database.
 * Kafka listeners are disabled since E2E tests exercise REST APIs only.
 * Provides helper methods for authentication, entity creation, and HTTP operations
 * across all CodeOps modules (Core, Registry, Courier, Logger, Fleet, MCP, Relay).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Import(TestRateLimitConfig.class)
public abstract class E2ETestBase {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("codeops_e2e")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start();
    }

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:19092");
        registry.add("logging.level.com.codeops", () -> "ERROR");
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // ──────────────────────────────────────────────────────────────
    //  Records
    // ──────────────────────────────────────────────────────────────

    /** Holds authentication tokens, user ID, and credentials for re-login. */
    protected record AuthResult(String token, String refreshToken, UUID userId, String email, String password) {}

    /** Holds the full setup context: auth + team + project. */
    protected record TestContext(String token, UUID userId, UUID teamId, UUID projectId) {}

    // ──────────────────────────────────────────────────────────────
    //  Auth Helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Registers a new user and returns the auth result.
     *
     * @param displayName display name for the new user
     * @return authentication result with token, refresh token, and user ID
     */
    @SuppressWarnings("unchecked")
    protected AuthResult registerAndLogin(String displayName) {
        String email = "e2e" + COUNTER.incrementAndGet() + "@test.com";
        String password = "Test@123456";
        var body = Map.of("email", email, "password", password, "displayName", displayName);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Registration failed: " + response.getStatusCode());
        }
        AuthResult reg = parseAuthResult(response.getBody());
        return new AuthResult(reg.token(), reg.refreshToken(), reg.userId(), email, password);
    }

    /**
     * Logs in an existing user and returns the auth result.
     *
     * @param email user email
     * @param password user password
     * @return authentication result with token, refresh token, and user ID
     */
    @SuppressWarnings("unchecked")
    protected AuthResult login(String email, String password) {
        var body = Map.of("email", email, "password", password);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/login", body, Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Login failed: " + response.getStatusCode());
        }
        AuthResult parsed = parseAuthResult(response.getBody());
        return new AuthResult(parsed.token(), parsed.refreshToken(), parsed.userId(), email, password);
    }

    // ──────────────────────────────────────────────────────────────
    //  Entity Creation Helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Creates a team and returns its ID.
     *
     * @param token JWT token
     * @param name team name
     * @return the new team's UUID
     */
    @SuppressWarnings("unchecked")
    protected UUID createTeam(String token, String name) {
        Map body = post("/api/v1/teams", token, Map.of("name", name)).getBody();
        return UUID.fromString((String) body.get("id"));
    }

    /**
     * Creates a project under a team and returns its ID.
     *
     * @param token JWT token
     * @param teamId team to create the project in
     * @param name project name
     * @return the new project's UUID
     */
    @SuppressWarnings("unchecked")
    protected UUID createProject(String token, UUID teamId, String name) {
        Map body = post("/api/v1/projects/" + teamId, token, Map.of("name", name)).getBody();
        return UUID.fromString((String) body.get("id"));
    }

    /**
     * Registers a user, creates a team and project, and returns the full context.
     * Re-logs in after team creation so the JWT includes the OWNER role.
     *
     * @param label label used for naming entities
     * @return a TestContext with token, userId, teamId, and projectId
     */
    protected TestContext setupFull(String label) {
        AuthResult auth = registerAndLogin(label + " User");
        UUID teamId = createTeam(auth.token(), label + " Team");
        // Re-login to get JWT with OWNER role on the newly created team
        AuthResult refreshed = login(auth.email(), auth.password());
        UUID projectId = createProject(refreshed.token(), teamId, label + " Project");
        return new TestContext(refreshed.token(), refreshed.userId(), teamId, projectId);
    }

    /**
     * Invites a user to a team and accepts the invitation programmatically.
     * The invitation token is retrieved from the database since the API does not expose it.
     *
     * @param ownerToken JWT token of the team owner
     * @param teamId     the team to invite into
     * @param invitee    the auth result of the user to invite
     * @param role       the team role (e.g., "MEMBER")
     */
    @SuppressWarnings("unchecked")
    protected void inviteAndAccept(String ownerToken, UUID teamId, AuthResult invitee, String role) {
        var inviteBody = Map.of("email", invitee.email(), "role", role);
        ResponseEntity<Map> inviteResp = post(
                "/api/v1/teams/" + teamId + "/invitations", ownerToken, inviteBody);
        if (!inviteResp.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Invitation failed: " + inviteResp.getStatusCode()
                    + " body=" + inviteResp.getBody());
        }
        UUID invitationId = extractId(inviteResp.getBody());

        // Retrieve the invitation token from the database (not exposed via API)
        String invitationToken = jdbcTemplate.queryForObject(
                "SELECT token FROM invitations WHERE id = ?", String.class, invitationId);

        // Accept the invitation as the invitee
        ResponseEntity<Map> acceptResp = post(
                "/api/v1/teams/invitations/" + invitationToken + "/accept",
                invitee.token(), Map.of());
        if (!acceptResp.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Accept invitation failed: " + acceptResp.getStatusCode()
                    + " body=" + acceptResp.getBody());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  HTTP Helpers — Core / Registry (path-based team context)
    // ──────────────────────────────────────────────────────────────

    /**
     * Sends a POST request with Bearer auth.
     *
     * @param path URL path
     * @param token JWT token
     * @param body request body
     * @return the response entity
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> post(String path, String token, Object body) {
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(token));
        return restTemplate.exchange(path, HttpMethod.POST, entity, Map.class);
    }

    /**
     * Sends a GET request with Bearer auth.
     *
     * @param path URL path
     * @param token JWT token
     * @return the response entity
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> get(String path, String token) {
        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(token));
        return restTemplate.exchange(path, HttpMethod.GET, entity, Map.class);
    }

    /**
     * Sends a PUT request with Bearer auth.
     *
     * @param path URL path
     * @param token JWT token
     * @param body request body
     * @return the response entity
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> put(String path, String token, Object body) {
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(token));
        return restTemplate.exchange(path, HttpMethod.PUT, entity, Map.class);
    }

    /**
     * Sends a DELETE request with Bearer auth.
     *
     * @param path URL path
     * @param token JWT token
     * @return the response entity
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> delete(String path, String token) {
        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(token));
        return restTemplate.exchange(path, HttpMethod.DELETE, entity, Map.class);
    }

    /**
     * Sends a GET request and returns the response as a List.
     *
     * @param path URL path
     * @param token JWT token
     * @return the response entity as a List
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<List> getList(String path, String token) {
        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(token));
        return restTemplate.exchange(path, HttpMethod.GET, entity, List.class);
    }

    // ──────────────────────────────────────────────────────────────
    //  HTTP Helpers — Courier / Logger (X-Team-Id header)
    // ──────────────────────────────────────────────────────────────

    /**
     * Sends a POST request with Bearer auth and X-Team-Id header.
     *
     * @param path URL path
     * @param token JWT token
     * @param teamId team UUID for X-Team-Id header
     * @param body request body
     * @return the response entity
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> postWithTeamHeader(String path, String token, UUID teamId, Object body) {
        HttpEntity<?> entity = new HttpEntity<>(body, teamHeaders(token, teamId));
        return restTemplate.exchange(path, HttpMethod.POST, entity, Map.class);
    }

    /**
     * Sends a GET request with Bearer auth and X-Team-Id header.
     *
     * @param path URL path
     * @param token JWT token
     * @param teamId team UUID for X-Team-Id header
     * @return the response entity
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> getWithTeamHeader(String path, String token, UUID teamId) {
        HttpEntity<?> entity = new HttpEntity<>(null, teamHeaders(token, teamId));
        return restTemplate.exchange(path, HttpMethod.GET, entity, Map.class);
    }

    /**
     * Sends a GET request with X-Team-Id header and returns a List.
     *
     * @param path URL path
     * @param token JWT token
     * @param teamId team UUID for X-Team-Id header
     * @return the response entity as a List
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<List> getListWithTeamHeader(String path, String token, UUID teamId) {
        HttpEntity<?> entity = new HttpEntity<>(null, teamHeaders(token, teamId));
        return restTemplate.exchange(path, HttpMethod.GET, entity, List.class);
    }

    /**
     * Sends a PUT request with Bearer auth and X-Team-Id header.
     *
     * @param path URL path
     * @param token JWT token
     * @param teamId team UUID for X-Team-Id header
     * @param body request body
     * @return the response entity
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> putWithTeamHeader(String path, String token, UUID teamId, Object body) {
        HttpEntity<?> entity = new HttpEntity<>(body, teamHeaders(token, teamId));
        return restTemplate.exchange(path, HttpMethod.PUT, entity, Map.class);
    }

    /**
     * Sends a DELETE request with Bearer auth and X-Team-Id header.
     *
     * @param path URL path
     * @param token JWT token
     * @param teamId team UUID for X-Team-Id header
     * @return the response entity
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> deleteWithTeamHeader(String path, String token, UUID teamId) {
        HttpEntity<?> entity = new HttpEntity<>(null, teamHeaders(token, teamId));
        return restTemplate.exchange(path, HttpMethod.DELETE, entity, Map.class);
    }

    // ──────────────────────────────────────────────────────────────
    //  Header Builders
    // ──────────────────────────────────────────────────────────────

    /**
     * Creates HTTP headers with Bearer auth.
     *
     * @param token JWT token
     * @return headers with Authorization and Content-Type
     */
    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Creates HTTP headers with Bearer auth and X-Team-Id.
     *
     * @param token JWT token
     * @param teamId team UUID
     * @return headers with Authorization, Content-Type, and X-Team-Id
     */
    protected HttpHeaders teamHeaders(String token, UUID teamId) {
        HttpHeaders headers = authHeaders(token);
        headers.set("X-Team-Id", teamId.toString());
        return headers;
    }

    // ──────────────────────────────────────────────────────────────
    //  Utility
    // ──────────────────────────────────────────────────────────────

    /**
     * Extracts a UUID from a response map field.
     *
     * @param body response body map
     * @param field field name containing the UUID string
     * @return the parsed UUID
     */
    @SuppressWarnings("unchecked")
    protected UUID extractId(Map body, String field) {
        return UUID.fromString((String) body.get(field));
    }

    /**
     * Extracts the "id" field from a response map.
     *
     * @param body response body map
     * @return the parsed UUID
     */
    protected UUID extractId(Map body) {
        return extractId(body, "id");
    }

    /**
     * Waits briefly for async operations to complete.
     */
    protected void waitForAsync() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private AuthResult parseAuthResult(Map body) {
        String token = (String) body.get("token");
        String refreshToken = (String) body.get("refreshToken");
        Map<String, Object> userMap = (Map<String, Object>) body.get("user");
        UUID userId = UUID.fromString((String) userMap.get("id"));
        return new AuthResult(token, refreshToken, userId, null, null);
    }
}
