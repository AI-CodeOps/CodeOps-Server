package com.codeops.fleet.controller;

import com.codeops.config.AppConstants;
import com.codeops.fleet.dto.response.DockerNetworkResponse;
import com.codeops.fleet.service.DockerEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Docker network management in the Fleet module.
 *
 * <p>Provides endpoints for listing, creating, and removing Docker networks,
 * as well as connecting and disconnecting containers from networks.
 * All endpoints require authentication and ADMIN or OWNER role.</p>
 */
@RestController
@RequestMapping(AppConstants.FLEET_API_PREFIX + "/networks")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class NetworkController {

    private final DockerEngineService dockerEngineService;

    /**
     * Lists all Docker networks on the host.
     *
     * @return the list of Docker networks
     */
    @GetMapping
    public List<DockerNetworkResponse> listNetworks() {
        return dockerEngineService.listNetworks();
    }

    /**
     * Creates a new Docker network.
     *
     * @param name   the network name
     * @param driver the network driver (default {@code bridge})
     * @return the created network ID
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String createNetwork(
            @RequestParam String name,
            @RequestParam(defaultValue = "bridge") String driver) {
        return dockerEngineService.createNetwork(name, driver);
    }

    /**
     * Removes a Docker network.
     *
     * @param networkId the network ID
     */
    @DeleteMapping("/{networkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeNetwork(@PathVariable String networkId) {
        dockerEngineService.removeNetwork(networkId);
    }

    /**
     * Connects a container to a network.
     *
     * @param networkId   the network ID
     * @param containerId the Docker container ID
     * @param aliases     optional network aliases for the container
     */
    @PostMapping("/{networkId}/connect")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void connectContainerToNetwork(
            @PathVariable String networkId,
            @RequestParam String containerId,
            @RequestParam(required = false) List<String> aliases) {
        dockerEngineService.connectContainerToNetwork(networkId, containerId, aliases);
    }

    /**
     * Disconnects a container from a network.
     *
     * @param networkId   the network ID
     * @param containerId the Docker container ID
     * @param force       whether to force-disconnect (default false)
     */
    @PostMapping("/{networkId}/disconnect")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnectContainerFromNetwork(
            @PathVariable String networkId,
            @RequestParam String containerId,
            @RequestParam(defaultValue = "false") boolean force) {
        dockerEngineService.disconnectContainerFromNetwork(networkId, containerId, force);
    }
}
