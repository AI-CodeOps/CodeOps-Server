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
 * E2E Scenario 3: MCP Session Lifecycle.
 *
 * <p>Exercises the full MCP workflow: create developer profile, generate API token,
 * initialize a session, record tool calls, complete the session with results,
 * verify session detail and activity feed, and test document creation/versioning/flagging.
 */
@SuppressWarnings("unchecked")
class McpSessionLifecycleE2ETest extends E2ETestBase {

    private TestContext ctx;

    @BeforeEach
    void setUp() {
        ctx = setupFull("McpLifecycle");
    }

    @Test
    void fullLifecycle() {
        // ── Step 1: Create developer profile ──
        var profileBody = Map.of(
                "displayName", "MCP Test Developer",
                "bio", "E2E test developer profile",
                "defaultEnvironment", "LOCAL",
                "timezone", "America/Chicago"
        );
        ResponseEntity<Map> profileResp = post(
                "/api/v1/mcp/developers/profile?teamId=" + ctx.teamId(), ctx.token(), profileBody);
        assertThat(profileResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UUID profileId = extractId(profileResp.getBody());
        assertThat(profileResp.getBody().get("displayName")).isNotNull();

        // ── Step 2: Create API token ──
        var tokenBody = Map.of(
                "name", "E2E Test Token"
        );
        ResponseEntity<Map> tokenResp = post(
                "/api/v1/mcp/developers/" + profileId + "/tokens", ctx.token(), tokenBody);
        assertThat(tokenResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map tokenData = tokenResp.getBody();
        assertThat(tokenData.get("name")).isEqualTo("E2E Test Token");
        assertThat(tokenData.get("rawToken")).isNotNull();
        assertThat(tokenData.get("status")).isEqualTo("ACTIVE");

        // ── Step 3: Init session ──
        var sessionBody = Map.of(
                "projectId", ctx.projectId().toString(),
                "environment", "LOCAL",
                "transport", "HTTP",
                "timeoutMinutes", 60
        );
        ResponseEntity<Map> sessionResp = post(
                "/api/v1/mcp/sessions?teamId=" + ctx.teamId(), ctx.token(), sessionBody);
        assertThat(sessionResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UUID sessionId = extractId(sessionResp.getBody());
        assertThat(sessionResp.getBody().get("transport")).isEqualTo("HTTP");

        // ── Step 4: Complete session with results ──
        var completeBody = Map.of(
                "summary", "Implemented user service with CRUD endpoints",
                "commitHashes", List.of("abc123", "def456"),
                "filesChanged", Map.of(
                        "created", List.of("src/main/java/UserService.java", "src/test/java/UserServiceTest.java"),
                        "modified", List.of("pom.xml"),
                        "deleted", List.of()
                ),
                "testsAdded", 5,
                "testCoverage", 0.92,
                "linesAdded", 250,
                "linesRemoved", 30,
                "durationMinutes", 15,
                "tokenUsage", 45000
        );
        ResponseEntity<Map> completeResp = post(
                "/api/v1/mcp/sessions/" + sessionId + "/complete", ctx.token(), completeBody);
        assertThat(completeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map completedSession = completeResp.getBody();
        assertThat(completedSession.get("status")).isEqualTo("COMPLETED");

        // ── Step 5: Verify session detail has all data ──
        ResponseEntity<Map> detailResp = get(
                "/api/v1/mcp/sessions/" + sessionId, ctx.token());
        assertThat(detailResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map detail = detailResp.getBody();
        assertThat(detail.get("status")).isEqualTo("COMPLETED");
        Map result = (Map) detail.get("result");
        assertThat(result).isNotNull();
        assertThat(result.get("summary")).isEqualTo("Implemented user service with CRUD endpoints");
        assertThat(((Number) result.get("testsAdded")).intValue()).isEqualTo(5);

        // ── Step 6: Verify activity feed endpoint is accessible ──
        ResponseEntity<Map> activityResp = get(
                "/api/v1/mcp/activity/project?projectId=" + ctx.projectId() + "&page=0&size=20",
                ctx.token());
        assertThat(activityResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activityResp.getBody()).containsKey("content");

        // ── Step 7: Create project document (AUDIT type) ──
        var docBody = Map.of(
                "documentType", "AUDIT_MD",
                "content", "# Audit Report\n\nAll checks passed.",
                "changeDescription", "Initial audit"
        );
        ResponseEntity<Map> docResp = post(
                "/api/v1/mcp/documents?projectId=" + ctx.projectId(), ctx.token(), docBody);
        assertThat(docResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UUID documentId = extractId(docResp.getBody());
        assertThat(docResp.getBody().get("documentType")).isEqualTo("AUDIT_MD");

        // ── Step 8: Update document (creates new version) ──
        var updateDocBody = Map.of(
                "content", "# Audit Report v2\n\n2 issues found and resolved.",
                "changeDescription", "Updated after fixes",
                "authorType", "AI"
        );
        ResponseEntity<Map> updateDocResp = put(
                "/api/v1/mcp/documents/" + documentId, ctx.token(), updateDocBody);
        assertThat(updateDocResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify versioning — should have at least 2 versions
        Map docDetail = updateDocResp.getBody();
        List<Map> versions = (List<Map>) docDetail.get("versions");
        assertThat(versions).hasSizeGreaterThanOrEqualTo(2);

        // ── Step 9: Verify document flagging ──
        ResponseEntity<List> flaggedDocs = getList(
                "/api/v1/mcp/documents/flagged?projectId=" + ctx.projectId(), ctx.token());
        assertThat(flaggedDocs.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Flagged docs list is accessible (may be empty if not auto-flagged)

        // ── Step 10: Verify session history ──
        ResponseEntity<List> historyResp = getList(
                "/api/v1/mcp/sessions/history?projectId=" + ctx.projectId() + "&limit=10",
                ctx.token());
        assertThat(historyResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResp.getBody()).hasSizeGreaterThanOrEqualTo(1);
    }
}
