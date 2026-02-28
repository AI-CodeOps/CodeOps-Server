package com.codeops.mcp.controller;

import com.codeops.config.AppConstants;
import com.codeops.mcp.dto.response.ActivityFeedEntryResponse;
import com.codeops.mcp.service.ActivityFeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the MCP activity feed.
 *
 * <p>Provides endpoints for querying team and project activity feeds,
 * which track significant MCP events such as session completions,
 * document updates, and cross-project impact detections.</p>
 *
 * <p>All endpoints require ADMIN or OWNER role.</p>
 */
@RestController
@RequestMapping(AppConstants.MCP_API_PREFIX + "/activity")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class McpActivityController {

    private final ActivityFeedService activityFeedService;

    /**
     * Retrieves the team activity feed with pagination.
     *
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return page of activity feed entries, newest first
     */
    @GetMapping("/team")
    public Page<ActivityFeedEntryResponse> getTeamFeed(@RequestParam UUID teamId,
                                                        Pageable pageable) {
        return activityFeedService.getTeamFeed(teamId, pageable);
    }

    /**
     * Retrieves the project activity feed with pagination.
     *
     * @param projectId the project ID
     * @param pageable  pagination parameters
     * @return page of activity feed entries, newest first
     */
    @GetMapping("/project")
    public Page<ActivityFeedEntryResponse> getProjectFeed(@RequestParam UUID projectId,
                                                            Pageable pageable) {
        return activityFeedService.getProjectFeed(projectId, pageable);
    }

    /**
     * Retrieves team activity since a given timestamp.
     *
     * @param teamId the team ID
     * @param since  cutoff timestamp (entries after this time are returned)
     * @return list of activity entries, newest first
     */
    @GetMapping("/team/since")
    public List<ActivityFeedEntryResponse> getTeamActivitySince(@RequestParam UUID teamId,
                                                                  @RequestParam Instant since) {
        return activityFeedService.getTeamActivitySince(teamId, since);
    }
}
