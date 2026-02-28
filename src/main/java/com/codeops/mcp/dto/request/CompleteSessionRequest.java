package com.codeops.mcp.dto.request;

import com.codeops.mcp.entity.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request to complete an MCP session with writeback results.
 *
 * @param summary            AI-generated summary of session work
 * @param commitHashes       list of Git commit hashes produced
 * @param filesChanged       files created, modified, and deleted
 * @param endpointsChanged   API endpoints added, modified, and removed
 * @param testsAdded         number of test methods added
 * @param testCoverage       code coverage percentage
 * @param linesAdded         lines of code added
 * @param linesRemoved       lines of code removed
 * @param dependencyChanges  JSON string of dependency changes
 * @param durationMinutes    total session duration in minutes
 * @param tokenUsage         estimated total token usage
 * @param documentUpdates    optional list of document updates to apply
 */
public record CompleteSessionRequest(
        @NotBlank String summary,
        List<String> commitHashes,
        SessionFilesChanged filesChanged,
        SessionEndpointsChanged endpointsChanged,
        Integer testsAdded,
        Double testCoverage,
        Integer linesAdded,
        Integer linesRemoved,
        String dependencyChanges,
        Integer durationMinutes,
        Long tokenUsage,
        List<DocumentUpdate> documentUpdates
) {

    /**
     * Files changed during the session.
     *
     * @param created  list of created file paths
     * @param modified list of modified file paths
     * @param deleted  list of deleted file paths
     */
    public record SessionFilesChanged(
            List<String> created,
            List<String> modified,
            List<String> deleted
    ) {}

    /**
     * API endpoints changed during the session.
     *
     * @param added    list of added endpoints
     * @param modified list of modified endpoints
     * @param removed  list of removed endpoints
     */
    public record SessionEndpointsChanged(
            List<String> added,
            List<String> modified,
            List<String> removed
    ) {}

    /**
     * A document update to apply when completing a session.
     *
     * @param documentType      the type of document to update
     * @param content           new document content
     * @param changeDescription what changed in this update
     */
    public record DocumentUpdate(
            DocumentType documentType,
            String content,
            String changeDescription
    ) {}
}
