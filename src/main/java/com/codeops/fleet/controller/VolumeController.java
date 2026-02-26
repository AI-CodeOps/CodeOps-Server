package com.codeops.fleet.controller;

import com.codeops.config.AppConstants;
import com.codeops.fleet.dto.response.DockerVolumeResponse;
import com.codeops.fleet.service.DockerEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Docker volume management in the Fleet module.
 *
 * <p>Provides endpoints for listing, creating, removing, and pruning Docker volumes
 * on the local Docker daemon. All endpoints require authentication and ADMIN or
 * OWNER role.</p>
 */
@RestController
@RequestMapping(AppConstants.FLEET_API_PREFIX + "/volumes")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class VolumeController {

    private final DockerEngineService dockerEngineService;

    /**
     * Lists all Docker volumes on the host.
     *
     * @return the list of Docker volumes
     */
    @GetMapping
    public List<DockerVolumeResponse> listVolumes() {
        return dockerEngineService.listVolumes();
    }

    /**
     * Creates a new Docker volume.
     *
     * @param name   the volume name
     * @param driver the volume driver (default {@code local})
     * @return the created volume details
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DockerVolumeResponse createVolume(
            @RequestParam String name,
            @RequestParam(defaultValue = "local") String driver) {
        return dockerEngineService.createVolume(name, driver);
    }

    /**
     * Removes a Docker volume.
     *
     * @param name  the volume name
     * @param force whether to force-remove (default false)
     */
    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVolume(
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean force) {
        dockerEngineService.removeVolume(name, force);
    }

    /**
     * Prunes unused Docker volumes from the host.
     *
     * @return the amount of disk space reclaimed in bytes
     */
    @PostMapping("/prune")
    public long pruneVolumes() {
        return dockerEngineService.pruneVolumes();
    }
}
