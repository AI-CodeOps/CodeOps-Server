package com.codeops.fleet.controller;

import com.codeops.config.AppConstants;
import com.codeops.fleet.dto.response.DockerImageResponse;
import com.codeops.fleet.service.DockerEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Docker image management in the Fleet module.
 *
 * <p>Provides endpoints for listing, pulling, removing, and pruning Docker images
 * on the local Docker daemon. All endpoints require authentication and ADMIN or
 * OWNER role.</p>
 */
@RestController
@RequestMapping(AppConstants.FLEET_API_PREFIX + "/images")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class ImageController {

    private final DockerEngineService dockerEngineService;

    /**
     * Lists all Docker images available on the host.
     *
     * @return the list of Docker images
     */
    @GetMapping
    public List<DockerImageResponse> listImages() {
        return dockerEngineService.listImages();
    }

    /**
     * Pulls a Docker image from a registry.
     *
     * @param imageName the image name (e.g. {@code postgres})
     * @param tag       the image tag (default {@code latest})
     */
    @PostMapping("/pull")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pullImage(
            @RequestParam String imageName,
            @RequestParam(defaultValue = "latest") String tag) {
        dockerEngineService.pullImage(imageName, tag);
    }

    /**
     * Removes a Docker image from the host.
     *
     * @param imageId the image ID or tag to remove
     * @param force   whether to force-remove (default false)
     */
    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeImage(
            @PathVariable String imageId,
            @RequestParam(defaultValue = "false") boolean force) {
        dockerEngineService.removeImage(imageId, force);
    }

    /**
     * Prunes unused Docker images from the host.
     *
     * @return the amount of disk space reclaimed in bytes
     */
    @PostMapping("/prune")
    public long pruneImages() {
        return dockerEngineService.pruneImages();
    }
}
