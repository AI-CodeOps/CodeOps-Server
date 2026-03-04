package com.codeops.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Scenario 6: Full Cross-Module Walk.
 *
 * <p>The comprehensive integration test touching ALL CodeOps modules in one flow:
 * Registry, Fleet, Logger, Courier, MCP, and Relay. Proves the entire integration
 * chain works end-to-end within a single test execution.
 */
@SuppressWarnings("unchecked")
class FullCrossModuleE2ETest extends E2ETestBase {

    private TestContext ctx;

    @BeforeEach
    void setUp() {
        ctx = setupFull("CrossModule");
    }

    @Test
    void everythingWalk() {
        // ══════════════════════════════════════════════════════════════
        //  REGISTRY: Register 2 services (API + DB) with dependency
        // ══════════════════════════════════════════════════════════════

        var apiBody = Map.of(
                "teamId", ctx.teamId().toString(),
                "name", "Inventory API",
                "serviceType", "SPRING_BOOT_API",
                "description", "Inventory management API"
        );
        ResponseEntity<Map> apiResp = post(
                "/api/v1/registry/teams/" + ctx.teamId() + "/services", ctx.token(), apiBody);
        assertThat(apiResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID apiServiceId = extractId(apiResp.getBody());

        var dbBody = Map.of(
                "teamId", ctx.teamId().toString(),
                "name", "Inventory DB",
                "serviceType", "DATABASE_SERVICE",
                "description", "Inventory PostgreSQL"
        );
        ResponseEntity<Map> dbResp = post(
                "/api/v1/registry/teams/" + ctx.teamId() + "/services", ctx.token(), dbBody);
        assertThat(dbResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID dbServiceId = extractId(dbResp.getBody());

        // Add dependency
        var depBody = Map.of(
                "sourceServiceId", apiServiceId.toString(),
                "targetServiceId", dbServiceId.toString(),
                "dependencyType", "DATABASE_SHARED",
                "isRequired", true
        );
        ResponseEntity<Map> depResp = post("/api/v1/registry/dependencies", ctx.token(), depBody);
        assertThat(depResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Add routes
        var routeBody = Map.of(
                "serviceId", apiServiceId.toString(),
                "routePrefix", "/api/v1/inventory",
                "httpMethods", "GET,POST,PUT,DELETE",
                "environment", "local"
        );
        ResponseEntity<Map> routeResp = post("/api/v1/registry/routes", ctx.token(), routeBody);
        assertThat(routeResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // ══════════════════════════════════════════════════════════════
        //  FLEET: Create service + solution profiles with order
        // ══════════════════════════════════════════════════════════════

        var dbProfileBody = Map.of(
                "serviceName", "inventory-db",
                "displayName", "Inventory DB",
                "description", "Inventory PostgreSQL",
                "imageName", "postgres",
                "imageTag", "16-alpine",
                "envVarsJson", "{\"POSTGRES_DB\":\"inventory\"}",
                "portsJson", "[{\"containerPort\":5432,\"hostPort\":5441}]",
                "healthCheckCommand", "pg_isready",
                "restartPolicy", "UNLESS_STOPPED",
                "startOrder", 1
        );
        ResponseEntity<Map> dbProfileResp = post(
                "/api/v1/fleet/service-profiles?teamId=" + ctx.teamId(), ctx.token(), dbProfileBody);
        assertThat(dbProfileResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID dbProfileId = extractId(dbProfileResp.getBody());

        var apiProfileBody = Map.of(
                "serviceName", "inventory-api",
                "displayName", "Inventory API",
                "description", "Inventory management API container",
                "imageName", "codeops/inventory-api",
                "imageTag", "latest",
                "envVarsJson", "{\"DB_HOST\":\"inventory-db\"}",
                "portsJson", "[{\"containerPort\":8080,\"hostPort\":8086}]",
                "healthCheckCommand", "curl -f http://localhost:8080/health || exit 1",
                "restartPolicy", "ON_FAILURE",
                "startOrder", 2
        );
        ResponseEntity<Map> apiProfileResp = post(
                "/api/v1/fleet/service-profiles?teamId=" + ctx.teamId(), ctx.token(), apiProfileBody);
        assertThat(apiProfileResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID apiProfileId = extractId(apiProfileResp.getBody());

        // Solution profile
        var solutionBody = Map.of("name", "Inventory Stack", "description", "Full inventory services");
        ResponseEntity<Map> solResp = post(
                "/api/v1/fleet/solution-profiles?teamId=" + ctx.teamId(), ctx.token(), solutionBody);
        assertThat(solResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID solutionId = extractId(solResp.getBody());

        post("/api/v1/fleet/solution-profiles/" + solutionId + "/services?teamId=" + ctx.teamId(),
                ctx.token(), Map.of("serviceProfileId", dbProfileId.toString(), "startOrder", 1));
        post("/api/v1/fleet/solution-profiles/" + solutionId + "/services?teamId=" + ctx.teamId(),
                ctx.token(), Map.of("serviceProfileId", apiProfileId.toString(), "startOrder", 2));

        // ══════════════════════════════════════════════════════════════
        //  LOGGER: Create log trap, ingest logs for both services
        // ══════════════════════════════════════════════════════════════

        var trapBody = Map.of(
                "name", "Inventory Error Trap",
                "trapType", "PATTERN",
                "conditions", List.of(Map.of(
                        "conditionType", "KEYWORD",
                        "field", "message",
                        "pattern", "inventory",
                        "logLevel", "ERROR"
                ))
        );
        ResponseEntity<Map> trapResp = postWithTeamHeader(
                "/api/v1/logger/traps", ctx.token(), ctx.teamId(), trapBody);
        assertThat(trapResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID trapId = extractId(trapResp.getBody());

        // Ingest logs
        postWithTeamHeader("/api/v1/logger/logs", ctx.token(), ctx.teamId(), Map.of(
                "level", "INFO", "message", "Inventory API started", "serviceName", "inventory-api"));
        postWithTeamHeader("/api/v1/logger/logs", ctx.token(), ctx.teamId(), Map.of(
                "level", "ERROR", "message", "Failed to sync inventory items",
                "serviceName", "inventory-api"));
        postWithTeamHeader("/api/v1/logger/logs", ctx.token(), ctx.teamId(), Map.of(
                "level", "INFO", "message", "Database backup completed", "serviceName", "inventory-db"));

        // ══════════════════════════════════════════════════════════════
        //  COURIER: Create collection with requests, create environment
        // ══════════════════════════════════════════════════════════════

        var collBody = Map.of("name", "Inventory API Tests", "description", "E2E collection");
        ResponseEntity<Map> collResp = postWithTeamHeader(
                "/api/v1/courier/collections", ctx.token(), ctx.teamId(), collBody);
        assertThat(collResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID collectionId = extractId(collResp.getBody());

        // Create folder
        var folderBody = Map.of("collectionId", collectionId.toString(), "name", "Inventory CRUD");
        ResponseEntity<Map> folderResp = postWithTeamHeader(
                "/api/v1/courier/folders", ctx.token(), ctx.teamId(), folderBody);
        assertThat(folderResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID folderId = extractId(folderResp.getBody());

        // Create request
        var reqBody = Map.of(
                "folderId", folderId.toString(),
                "name", "List Inventory",
                "method", "GET",
                "url", "{{baseUrl}}/api/v1/inventory"
        );
        ResponseEntity<Map> reqResp = postWithTeamHeader(
                "/api/v1/courier/requests", ctx.token(), ctx.teamId(), reqBody);
        assertThat(reqResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Create environment
        var envBody = Map.of("name", "Local", "description", "Local dev");
        ResponseEntity<Map> envResp = postWithTeamHeader(
                "/api/v1/courier/environments", ctx.token(), ctx.teamId(), envBody);
        assertThat(envResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // ══════════════════════════════════════════════════════════════
        //  MCP: Developer profile, session lifecycle, document
        // ══════════════════════════════════════════════════════════════

        var profileBody = Map.of(
                "displayName", "Cross-Module Dev",
                "defaultEnvironment", "LOCAL",
                "timezone", "UTC"
        );
        ResponseEntity<Map> profileResp = post(
                "/api/v1/mcp/developers/profile?teamId=" + ctx.teamId(), ctx.token(), profileBody);
        assertThat(profileResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Init session
        var sessionBody = Map.of(
                "projectId", ctx.projectId().toString(),
                "environment", "LOCAL",
                "transport", "HTTP"
        );
        ResponseEntity<Map> sessionResp = post(
                "/api/v1/mcp/sessions?teamId=" + ctx.teamId(), ctx.token(), sessionBody);
        assertThat(sessionResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID sessionId = extractId(sessionResp.getBody());

        // Complete session
        var completeBody = Map.of(
                "summary", "Implemented inventory CRUD endpoints and tests",
                "commitHashes", List.of("aaa111"),
                "filesChanged", Map.of(
                        "created", List.of("InventoryController.java"),
                        "modified", List.of("pom.xml"),
                        "deleted", List.of()
                ),
                "testsAdded", 10,
                "testCoverage", 0.95,
                "linesAdded", 500,
                "linesRemoved", 50,
                "durationMinutes", 20,
                "tokenUsage", 60000
        );
        ResponseEntity<Map> completeResp = post(
                "/api/v1/mcp/sessions/" + sessionId + "/complete", ctx.token(), completeBody);
        assertThat(completeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completeResp.getBody().get("status")).isEqualTo("COMPLETED");

        // Create project document (AUDIT type)
        var docBody = Map.of(
                "documentType", "AUDIT_MD",
                "content", "# Inventory Audit\n\nAll endpoints verified.",
                "changeDescription", "Post-session audit"
        );
        ResponseEntity<Map> docResp = post(
                "/api/v1/mcp/documents?projectId=" + ctx.projectId(), ctx.token(), docBody);
        assertThat(docResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify activity feed endpoint is accessible
        ResponseEntity<Map> activityResp = get(
                "/api/v1/mcp/activity/project?projectId=" + ctx.projectId() + "&page=0&size=50",
                ctx.token());
        assertThat(activityResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activityResp.getBody()).containsKey("content");

        // ══════════════════════════════════════════════════════════════
        //  RELAY: Verify channel and messaging
        // ══════════════════════════════════════════════════════════════

        // Create channel
        var chBody = Map.of(
                "name", "inventory-updates",
                "channelType", "PROJECT",
                "description", "Inventory project updates"
        );
        ResponseEntity<Map> chResp = post(
                "/api/v1/relay/channels?teamId=" + ctx.teamId(), ctx.token(), chBody);
        assertThat(chResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID channelId = extractId(chResp.getBody());

        // Post message summarizing session results
        var msgBody = Map.of("content", "MCP session completed: Implemented inventory CRUD endpoints");
        ResponseEntity<Map> msgResp = post(
                "/api/v1/relay/channels/" + channelId + "/messages?teamId=" + ctx.teamId(),
                ctx.token(), msgBody);
        assertThat(msgResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Search for session summary
        ResponseEntity<Map> searchResp = get(
                "/api/v1/relay/channels/" + channelId
                        + "/messages/search?query=inventory&teamId=" + ctx.teamId(),
                ctx.token());
        assertThat(searchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map> searchResults = (List<Map>) searchResp.getBody().get("content");
        assertThat(searchResults).isNotEmpty();

        // Verify platform events endpoint accessible
        ResponseEntity<Map> eventsResp = get(
                "/api/v1/relay/events?teamId=" + ctx.teamId() + "&page=0&size=20", ctx.token());
        assertThat(eventsResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ══════════════════════════════════════════════════════════════
        //  FINAL VERIFICATION: Cross-module data integrity
        // ══════════════════════════════════════════════════════════════

        // Verify Registry services still accessible
        ResponseEntity<Map> regCheck = get(
                "/api/v1/registry/services/" + apiServiceId, ctx.token());
        assertThat(regCheck.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(regCheck.getBody().get("name")).isEqualTo("Inventory API");

        // Verify Fleet solution still has 2 services
        ResponseEntity<Map> solCheck = get(
                "/api/v1/fleet/solution-profiles/" + solutionId + "?teamId=" + ctx.teamId(),
                ctx.token());
        assertThat(solCheck.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List) solCheck.getBody().get("services"))).hasSize(2);

        // Verify Courier collection has 1 request
        ResponseEntity<Map> collCheck = getWithTeamHeader(
                "/api/v1/courier/collections/" + collectionId, ctx.token(), ctx.teamId());
        assertThat(collCheck.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) collCheck.getBody().get("requestCount")).intValue()).isEqualTo(1);

        // Verify MCP session is completed
        ResponseEntity<Map> sessCheck = get(
                "/api/v1/mcp/sessions/" + sessionId, ctx.token());
        assertThat(sessCheck.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sessCheck.getBody().get("status")).isEqualTo("COMPLETED");
    }
}
