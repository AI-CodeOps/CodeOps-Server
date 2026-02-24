package com.codeops.relay.repository;

import com.codeops.relay.entity.MessageThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link MessageThread} entities.
 *
 * <p>Provides lookups by root message and channel, plus existence checks
 * for thread creation logic.</p>
 */
@Repository
public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {

    /**
     * Finds the thread for a given root message.
     *
     * @param rootMessageId the root message ID
     * @return the thread, if one exists
     */
    Optional<MessageThread> findByRootMessageId(UUID rootMessageId);

    /**
     * Finds all threads in a channel ordered by most recently active.
     *
     * @param channelId the channel ID
     * @return list of threads
     */
    List<MessageThread> findByChannelIdOrderByLastReplyAtDesc(UUID channelId);

    /**
     * Checks whether a thread exists for a given root message.
     *
     * @param rootMessageId the root message ID
     * @return true if a thread exists
     */
    boolean existsByRootMessageId(UUID rootMessageId);
}
