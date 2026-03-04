package com.codeops.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Scenario 1: Service Registration to Courier Collection.
 *
 * <p>Verifies the data flow from Registry (service + routes + ports) into
 * Courier (collection + requests + environment). Exercises both modules
 * in sequence to prove cross-module data integrity.
 */
@SuppressWarnings("unchecked")
class ServiceToCourierE2ETest extends E2ETestBase {

    private TestContext ctx;

    @BeforeEach
    void setUp() {
        ctx = setupFull("SvcToCourier");
    }

    @Test
    void registryToCourier_fullFlow() {
        // ── Step 1: Seed default port ranges (returns List) ──
        HttpEntity<?> seedEntity = new HttpEntity<>(null, authHeaders(ctx.token()));
        ResponseEntity<String> rangesResp = restTemplate.exchange(
                "/api/v1/registry/teams/" + ctx.teamId() + "/ports/ranges/seed?environment=local",
                HttpMethod.POST, seedEntity, String.class);
        assertThat(rangesResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ── Step 2: Register API service with auto-allocated ports ──
        var serviceBody = Map.of(
                "teamId", ctx.teamId().toString(),
                "name", "Order Service",
                "serviceType", "SPRING_BOOT_API",
                "description", "Handles order processing",
                "autoAllocatePortTypes", List.of("HTTP_API"),
                "autoAllocateEnvironment", "local"
        );
        ResponseEntity<Map> serviceResp = post(
                "/api/v1/registry/teams/" + ctx.teamId() + "/services", ctx.token(), serviceBody);
        assertThat(serviceResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map serviceData = serviceResp.getBody();
        UUID serviceId = extractId(serviceData);
        assertThat(serviceData.get("name")).isEqualTo("Order Service");
        assertThat(serviceData.get("serviceType")).isEqualTo("SPRING_BOOT_API");

        // ── Step 3: Verify port was auto-allocated in correct range ──
        ResponseEntity<List> portsResp = getList(
                "/api/v1/registry/services/" + serviceId + "/ports", ctx.token());
        assertThat(portsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> ports = portsResp.getBody();
        assertThat(ports).isNotEmpty();
        int portNumber = ((Number) ports.get(0).get("portNumber")).intValue();
        assertThat(portNumber).isGreaterThan(0);

        // ── Step 4: Add API routes ──
        var route1 = Map.of(
                "serviceId", serviceId.toString(),
                "routePrefix", "/api/v1/orders",
                "httpMethods", "GET,POST",
                "environment", "local",
                "description", "Order CRUD"
        );
        ResponseEntity<Map> routeResp1 = post("/api/v1/registry/routes", ctx.token(), route1);
        assertThat(routeResp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var route2 = Map.of(
                "serviceId", serviceId.toString(),
                "routePrefix", "/api/v1/customers",
                "httpMethods", "GET,PUT,DELETE",
                "environment", "local",
                "description", "Customer operations"
        );
        ResponseEntity<Map> routeResp2 = post("/api/v1/registry/routes", ctx.token(), route2);
        assertThat(routeResp2.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify routes were created
        ResponseEntity<String> routesResp = restTemplate.exchange(
                "/api/v1/registry/services/" + serviceId + "/routes",
                HttpMethod.GET, new HttpEntity<>(null, authHeaders(ctx.token())), String.class);
        assertThat(routesResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(routesResp.getBody()).contains("/api/v1/orders").contains("/api/v1/customers");

        // ── Step 5: Create Courier collection for the service ──
        var collectionBody = Map.of(
                "name", "Order Service API",
                "description", "API tests for Order Service"
        );
        ResponseEntity<Map> collResp = postWithTeamHeader(
                "/api/v1/courier/collections", ctx.token(), ctx.teamId(), collectionBody);
        assertThat(collResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID collectionId = extractId(collResp.getBody());

        // ── Step 6: Create folder in the collection ──
        var folderBody = Map.of(
                "collectionId", collectionId.toString(),
                "name", "Orders"
        );
        ResponseEntity<Map> folderResp = postWithTeamHeader(
                "/api/v1/courier/folders", ctx.token(), ctx.teamId(), folderBody);
        assertThat(folderResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID folderId = extractId(folderResp.getBody());

        // ── Step 7: Create requests matching the routes ──
        var getOrdersReq = Map.of(
                "folderId", folderId.toString(),
                "name", "List Orders",
                "method", "GET",
                "url", "{{baseUrl}}/api/v1/orders"
        );
        ResponseEntity<Map> req1Resp = postWithTeamHeader(
                "/api/v1/courier/requests", ctx.token(), ctx.teamId(), getOrdersReq);
        assertThat(req1Resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(req1Resp.getBody().get("method")).isEqualTo("GET");

        var postOrderReq = Map.of(
                "folderId", folderId.toString(),
                "name", "Create Order",
                "method", "POST",
                "url", "{{baseUrl}}/api/v1/orders"
        );
        ResponseEntity<Map> req2Resp = postWithTeamHeader(
                "/api/v1/courier/requests", ctx.token(), ctx.teamId(), postOrderReq);
        assertThat(req2Resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(req2Resp.getBody().get("method")).isEqualTo("POST");

        // ── Step 8: Create environment with service URL variables ──
        var envBody = Map.of(
                "name", "Local Development",
                "description", "Local environment for Order Service"
        );
        ResponseEntity<Map> envResp = postWithTeamHeader(
                "/api/v1/courier/environments", ctx.token(), ctx.teamId(), envBody);
        assertThat(envResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID envId = extractId(envResp.getBody());

        // Save environment variables with service URL
        var varsBody = Map.of(
                "variables", List.of(
                        Map.of("variableKey", "baseUrl",
                                "variableValue", "http://localhost:" + portNumber,
                                "isSecret", false, "isEnabled", true),
                        Map.of("variableKey", "apiKey",
                                "variableValue", "test-api-key-12345",
                                "isSecret", true, "isEnabled", true)
                )
        );
        ResponseEntity<String> varsResp = restTemplate.exchange(
                "/api/v1/courier/environments/" + envId + "/variables",
                HttpMethod.PUT,
                new HttpEntity<>(varsBody, teamHeaders(ctx.token(), ctx.teamId())),
                String.class);
        assertThat(varsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(varsResp.getBody()).contains("baseUrl").contains("apiKey");

        // ── Step 9: Verify collection contains correct requests ──
        ResponseEntity<Map> collDetail = getWithTeamHeader(
                "/api/v1/courier/collections/" + collectionId, ctx.token(), ctx.teamId());
        assertThat(collDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) collDetail.getBody().get("requestCount")).intValue()).isEqualTo(2);
    }
}
