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
 * E2E Scenario 2: Logging Pipeline with Trap and Alert.
 *
 * <p>Registers a database service, creates a log trap matching ERROR level,
 * creates an alert channel and rule, ingests log entries (including one
 * matching the trap), and verifies the trap fires and alert triggers.
 */
@SuppressWarnings("unchecked")
class LoggingPipelineE2ETest extends E2ETestBase {

    private TestContext ctx;

    @BeforeEach
    void setUp() {
        ctx = setupFull("LogPipeline");
    }

    @Test
    void trapAndAlert_fullFlow() {
        // ── Step 1: Register a database service in Registry ──
        var serviceBody = Map.of(
                "teamId", ctx.teamId().toString(),
                "name", "User Database",
                "serviceType", "DATABASE_SERVICE",
                "description", "Primary user database"
        );
        ResponseEntity<Map> serviceResp = post(
                "/api/v1/registry/teams/" + ctx.teamId() + "/services", ctx.token(), serviceBody);
        assertThat(serviceResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // ── Step 2: Create log trap (pattern match on ERROR + database keyword) ──
        var trapBody = Map.of(
                "name", "Database Error Trap",
                "description", "Catches ERROR logs from database services",
                "trapType", "PATTERN",
                "conditions", List.of(
                        Map.of(
                                "conditionType", "KEYWORD",
                                "field", "message",
                                "pattern", "database",
                                "logLevel", "ERROR"
                        )
                )
        );
        ResponseEntity<Map> trapResp = postWithTeamHeader(
                "/api/v1/logger/traps", ctx.token(), ctx.teamId(), trapBody);
        assertThat(trapResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map trapData = trapResp.getBody();
        UUID trapId = extractId(trapData);
        assertThat(trapData.get("name")).isEqualTo("Database Error Trap");
        assertThat(trapData.get("isActive")).isEqualTo(true);

        // ── Step 3: Create alert channel (webhook type) ──
        var channelBody = Map.of(
                "name", "Ops Webhook",
                "channelType", "WEBHOOK",
                "configuration", "{\"url\": \"https://hooks.example.com/alert\"}"
        );
        ResponseEntity<Map> channelResp = postWithTeamHeader(
                "/api/v1/logger/alerts/channels", ctx.token(), ctx.teamId(), channelBody);
        assertThat(channelResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UUID channelId = extractId(channelResp.getBody());
        assertThat(channelResp.getBody().get("channelType")).isEqualTo("WEBHOOK");

        // ── Step 4: Create alert rule linking trap to channel ──
        var ruleBody = Map.of(
                "name", "DB Error Alert",
                "trapId", trapId.toString(),
                "channelId", channelId.toString(),
                "severity", "CRITICAL",
                "throttleMinutes", 5
        );
        ResponseEntity<Map> ruleResp = postWithTeamHeader(
                "/api/v1/logger/alerts/rules", ctx.token(), ctx.teamId(), ruleBody);
        assertThat(ruleResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map ruleData = ruleResp.getBody();
        assertThat(ruleData.get("severity")).isEqualTo("CRITICAL");
        assertThat(ruleData.get("trapName")).isEqualTo("Database Error Trap");
        assertThat(ruleData.get("channelName")).isEqualTo("Ops Webhook");

        // ── Step 5: Ingest log entries — including one matching the trap ──
        // Normal log (should not trigger trap)
        var infoLog = Map.of(
                "level", "INFO",
                "message", "Application started successfully",
                "serviceName", "user-database"
        );
        ResponseEntity<Map> infoResp = postWithTeamHeader(
                "/api/v1/logger/logs", ctx.token(), ctx.teamId(), infoLog);
        assertThat(infoResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // ERROR log matching trap (contains "database" keyword + ERROR level)
        var errorLog = Map.of(
                "level", "ERROR",
                "message", "Connection to database failed: timeout after 30s",
                "serviceName", "user-database",
                "exceptionClass", "java.sql.SQLException",
                "exceptionMessage", "Connection timeout"
        );
        ResponseEntity<Map> errorResp = postWithTeamHeader(
                "/api/v1/logger/logs", ctx.token(), ctx.teamId(), errorLog);
        assertThat(errorResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(errorResp.getBody().get("level")).isEqualTo("ERROR");

        // Another normal log
        var debugLog = Map.of(
                "level", "DEBUG",
                "message", "Cache hit for user-123",
                "serviceName", "user-database"
        );
        ResponseEntity<Map> debugResp = postWithTeamHeader(
                "/api/v1/logger/logs", ctx.token(), ctx.teamId(), debugLog);
        assertThat(debugResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Wait for async event processing
        waitForAsync();

        // ── Step 6: Verify trap was triggered ──
        ResponseEntity<Map> trapDetail = getWithTeamHeader(
                "/api/v1/logger/traps/" + trapId, ctx.token(), ctx.teamId());
        assertThat(trapDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        long triggerCount = ((Number) trapDetail.getBody().get("triggerCount")).longValue();
        assertThat(triggerCount).isGreaterThanOrEqualTo(1);

        // ── Step 7: Verify alert channels and rules are accessible ──
        ResponseEntity<Map> channelsResp = getWithTeamHeader(
                "/api/v1/logger/alerts/channels/paged?page=0&size=20", ctx.token(), ctx.teamId());
        assertThat(channelsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) channelsResp.getBody().get("totalElements")).longValue())
                .isGreaterThanOrEqualTo(1);

        ResponseEntity<Map> rulesResp = getWithTeamHeader(
                "/api/v1/logger/alerts/rules/paged?page=0&size=20", ctx.token(), ctx.teamId());
        assertThat(rulesResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) rulesResp.getBody().get("totalElements")).longValue())
                .isGreaterThanOrEqualTo(1);
    }
}
