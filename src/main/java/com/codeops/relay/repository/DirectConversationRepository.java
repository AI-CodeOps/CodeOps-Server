package com.codeops.relay.repository;

import com.codeops.relay.entity.DirectConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DirectConversation} entities.
 *
 * <p>Provides queries for finding conversations a user participates in,
 * locating exact participant combinations, and counting conversations.</p>
 */
@Repository
public interface DirectConversationRepository extends JpaRepository<DirectConversation, UUID> {

    /**
     * Finds conversations in a team where the user is a participant, ordered by most recent.
     *
     * @param teamId the team ID
     * @param userId the user ID substring to match in participantIds
     * @return list of conversations
     */
    List<DirectConversation> findByTeamIdAndParticipantIdsContainingOrderByLastMessageAtDesc(
            UUID teamId, String userId);

    /**
     * Finds a conversation by its exact participant combination.
     *
     * @param teamId         the team ID
     * @param participantIds the sorted comma-separated participant UUIDs
     * @return the conversation, if found
     */
    Optional<DirectConversation> findByTeamIdAndParticipantIds(UUID teamId, String participantIds);

    /**
     * Counts conversations a user participates in within a team.
     *
     * @param teamId the team ID
     * @param userId the user ID substring to match in participantIds
     * @return conversation count
     */
    long countByTeamIdAndParticipantIdsContaining(UUID teamId, String userId);
}
