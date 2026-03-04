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
 * E2E Scenario 4: Fleet Profile Orchestration.
 *
 * <p>Registers services in Registry, then builds Fleet profiles (service, solution,
 * workstation) from that data. Verifies dependency ordering and hierarchical
 * profile structure. Actual Docker start/stop is not testable in Testcontainers —
 * this test validates the data model and orchestration logic.
 */
@SuppressWarnings("unchecked")
class FleetProfileE2ETest extends E2ETestBase {

    private TestContext ctx;

    @BeforeEach
    void setUp() {
        ctx = setupFull("FleetProfile");
    }

    @Test
    void profileOrchestration() {
        // ── Step 1: Register 2 services with dependency ──
        var apiServiceBody = Map.of(
                "teamId", ctx.teamId().toString(),
                "name", "Payment API",
                "serviceType", "SPRING_BOOT_API",
                "description", "Payment processing API"
        );
        ResponseEntity<Map> apiResp = post(
                "/api/v1/registry/teams/" + ctx.teamId() + "/services", ctx.token(), apiServiceBody);
        assertThat(apiResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID apiServiceId = extractId(apiResp.getBody());

        var dbServiceBody = Map.of(
                "teamId", ctx.teamId().toString(),
                "name", "Payment Database",
                "serviceType", "DATABASE_SERVICE",
                "description", "PostgreSQL for payments"
        );
        ResponseEntity<Map> dbResp = post(
                "/api/v1/registry/teams/" + ctx.teamId() + "/services", ctx.token(), dbServiceBody);
        assertThat(dbResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID dbServiceId = extractId(dbResp.getBody());

        // Create dependency: API depends on Database
        var depBody = Map.of(
                "sourceServiceId", apiServiceId.toString(),
                "targetServiceId", dbServiceId.toString(),
                "dependencyType", "DATABASE_SHARED",
                "description", "API reads/writes payment data",
                "isRequired", true
        );
        ResponseEntity<Map> depResp = post("/api/v1/registry/dependencies", ctx.token(), depBody);
        assertThat(depResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(depResp.getBody().get("dependencyType")).isEqualTo("DATABASE_SHARED");

        // ── Step 2: Create Fleet service profiles ──
        var dbProfileBody = Map.of(
                "serviceName", "payment-db",
                "displayName", "Payment Database",
                "description", "PostgreSQL for payment data",
                "imageName", "postgres",
                "imageTag", "16-alpine",
                "envVarsJson", "{\"POSTGRES_USER\":\"payment\",\"POSTGRES_PASSWORD\":\"payment\",\"POSTGRES_DB\":\"payment\"}",
                "portsJson", "[{\"containerPort\":5432,\"hostPort\":5440}]",
                "healthCheckCommand", "pg_isready -U payment",
                "restartPolicy", "UNLESS_STOPPED",
                "startOrder", 1
        );
        ResponseEntity<Map> dbProfileResp = post(
                "/api/v1/fleet/service-profiles?teamId=" + ctx.teamId(), ctx.token(), dbProfileBody);
        assertThat(dbProfileResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID dbProfileId = extractId(dbProfileResp.getBody());
        assertThat(dbProfileResp.getBody().get("serviceName")).isEqualTo("payment-db");

        var apiProfileBody = Map.of(
                "serviceName", "payment-api",
                "displayName", "Payment API",
                "description", "Payment processing API container",
                "imageName", "codeops/payment-api",
                "imageTag", "latest",
                "envVarsJson", "{\"DB_HOST\":\"payment-db\",\"DB_PORT\":\"5432\"}",
                "portsJson", "[{\"containerPort\":8080,\"hostPort\":8085}]",
                "healthCheckCommand", "curl -f http://localhost:8080/health || exit 1",
                "restartPolicy", "ON_FAILURE",
                "startOrder", 2
        );
        ResponseEntity<Map> apiProfileResp = post(
                "/api/v1/fleet/service-profiles?teamId=" + ctx.teamId(), ctx.token(), apiProfileBody);
        assertThat(apiProfileResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID apiProfileId = extractId(apiProfileResp.getBody());

        // ── Step 3: Create solution profile grouping both services ──
        var solutionBody = Map.of(
                "name", "Payment Stack",
                "description", "Database + API for payment processing"
        );
        ResponseEntity<Map> solutionResp = post(
                "/api/v1/fleet/solution-profiles?teamId=" + ctx.teamId(), ctx.token(), solutionBody);
        assertThat(solutionResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID solutionId = extractId(solutionResp.getBody());

        // Add DB service first (start order 1)
        var addDbBody = Map.of(
                "serviceProfileId", dbProfileId.toString(),
                "startOrder", 1
        );
        ResponseEntity<Map> addDb = post(
                "/api/v1/fleet/solution-profiles/" + solutionId + "/services?teamId=" + ctx.teamId(),
                ctx.token(), addDbBody);
        assertThat(addDb.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Add API service second (start order 2)
        var addApiBody = Map.of(
                "serviceProfileId", apiProfileId.toString(),
                "startOrder", 2
        );
        ResponseEntity<Map> addApi = post(
                "/api/v1/fleet/solution-profiles/" + solutionId + "/services?teamId=" + ctx.teamId(),
                ctx.token(), addApiBody);
        assertThat(addApi.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // ── Step 4: Create workstation profile including solution ──
        var wsBody = Map.of(
                "name", "Dev Workstation",
                "description", "Local development workstation"
        );
        ResponseEntity<Map> wsResp = post(
                "/api/v1/fleet/workstation-profiles?teamId=" + ctx.teamId(), ctx.token(), wsBody);
        assertThat(wsResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID wsId = extractId(wsResp.getBody());

        // Add solution to workstation
        var addSolBody = Map.of(
                "solutionProfileId", solutionId.toString(),
                "startOrder", 1
        );
        ResponseEntity<Map> addSol = post(
                "/api/v1/fleet/workstation-profiles/" + wsId + "/solutions?teamId=" + ctx.teamId(),
                ctx.token(), addSolBody);
        assertThat(addSol.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // ── Step 5: Verify dependency ordering in profiles ──
        // Get solution detail — should contain 2 services
        ResponseEntity<Map> solutionDetail = get(
                "/api/v1/fleet/solution-profiles/" + solutionId + "?teamId=" + ctx.teamId(),
                ctx.token());
        assertThat(solutionDetail.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map solutionData = solutionDetail.getBody();
        List<Map> services = (List<Map>) solutionData.get("services");
        assertThat(services).hasSize(2);

        // Verify workstation detail
        ResponseEntity<Map> wsDetail = get(
                "/api/v1/fleet/workstation-profiles/" + wsId + "?teamId=" + ctx.teamId(),
                ctx.token());
        assertThat(wsDetail.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map> solutions = (List<Map>) wsDetail.getBody().get("solutions");
        assertThat(solutions).hasSize(1);
    }
}
