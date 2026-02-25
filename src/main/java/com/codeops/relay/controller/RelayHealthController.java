package com.codeops.relay.controller;

import com.codeops.config.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check endpoint for the Relay module.
 *
 * <p>Provides an unauthenticated health probe that returns the module's
 * status and current timestamp. Used by load balancers and monitoring
 * systems to verify module availability.</p>
 */
@RestController("relayHealthController")
@RequestMapping(AppConstants.RELAY_API_PREFIX + "/health")
@Slf4j
public class RelayHealthController {

    /**
     * Returns the health status of the Relay module.
     *
     * @return a map containing status, module name, and timestamp
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "module", "relay",
                "timestamp", Instant.now().toString()));
    }
}
