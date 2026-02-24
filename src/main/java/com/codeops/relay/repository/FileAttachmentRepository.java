package com.codeops.relay.repository;

import com.codeops.relay.entity.FileAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link FileAttachment} entities.
 *
 * <p>Provides queries for finding attachments by message, team-wide file listing,
 * and a storage aggregation query for quota enforcement.</p>
 */
@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, UUID> {

    /**
     * Finds all attachments on a channel message.
     *
     * @param messageId the channel message ID
     * @return list of file attachments
     */
    List<FileAttachment> findByMessageId(UUID messageId);

    /**
     * Finds all attachments on a direct message.
     *
     * @param directMessageId the direct message ID
     * @return list of file attachments
     */
    List<FileAttachment> findByDirectMessageId(UUID directMessageId);

    /**
     * Finds all attachments for a team ordered by most recent first.
     *
     * @param teamId the team ID
     * @return list of file attachments
     */
    List<FileAttachment> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    /**
     * Finds all attachments for a team with pagination.
     *
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return a page of file attachments
     */
    Page<FileAttachment> findByTeamId(UUID teamId, Pageable pageable);

    /**
     * Counts total attachments for a team.
     *
     * @param teamId the team ID
     * @return attachment count
     */
    long countByTeamId(UUID teamId);

    /**
     * Calculates total storage bytes used by a team's file attachments.
     *
     * @param teamId the team ID
     * @return total bytes (0 if no attachments)
     */
    @Query("SELECT COALESCE(SUM(f.fileSizeBytes), 0) FROM FileAttachment f WHERE f.teamId = :teamId")
    long totalStorageByTeamId(@Param("teamId") UUID teamId);
}
