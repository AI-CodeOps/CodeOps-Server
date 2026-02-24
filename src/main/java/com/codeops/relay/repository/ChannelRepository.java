package com.codeops.relay.repository;

import com.codeops.relay.entity.Channel;
import com.codeops.relay.entity.enums.ChannelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Channel} entities.
 *
 * <p>Provides CRUD operations plus team-scoped queries for listing,
 * filtering by type, searching by slug, and locating project/service channels.</p>
 */
@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    /**
     * Finds all channels for a team with pagination.
     *
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return a page of channels
     */
    Page<Channel> findByTeamId(UUID teamId, Pageable pageable);

    /**
     * Finds all channels for a team.
     *
     * @param teamId the team ID
     * @return list of channels
     */
    List<Channel> findByTeamId(UUID teamId);

    /**
     * Finds channels of a specific type within a team.
     *
     * @param teamId      the team ID
     * @param channelType the channel type filter
     * @return list of matching channels
     */
    List<Channel> findByTeamIdAndChannelType(UUID teamId, ChannelType channelType);

    /**
     * Finds all non-archived channels for a team.
     *
     * @param teamId the team ID
     * @return list of active channels
     */
    List<Channel> findByTeamIdAndIsArchivedFalse(UUID teamId);

    /**
     * Finds a channel by its team and slug.
     *
     * @param teamId the team ID
     * @param slug   the channel slug
     * @return the channel, if found
     */
    Optional<Channel> findByTeamIdAndSlug(UUID teamId, String slug);

    /**
     * Finds the PROJECT channel linked to a specific project.
     *
     * @param teamId    the team ID
     * @param projectId the project ID
     * @return the project channel, if it exists
     */
    Optional<Channel> findByTeamIdAndProjectId(UUID teamId, UUID projectId);

    /**
     * Finds the SERVICE channel linked to a specific service.
     *
     * @param teamId    the team ID
     * @param serviceId the service ID
     * @return the service channel, if it exists
     */
    Optional<Channel> findByTeamIdAndServiceId(UUID teamId, UUID serviceId);

    /**
     * Checks whether a channel with the given slug exists in a team.
     *
     * @param teamId the team ID
     * @param slug   the channel slug
     * @return true if the slug is taken
     */
    boolean existsByTeamIdAndSlug(UUID teamId, String slug);

    /**
     * Counts total channels in a team.
     *
     * @param teamId the team ID
     * @return channel count
     */
    long countByTeamId(UUID teamId);
}
