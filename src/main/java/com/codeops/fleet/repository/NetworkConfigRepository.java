package com.codeops.fleet.repository;

import com.codeops.fleet.entity.NetworkConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link NetworkConfig} entities.
 *
 * <p>Provides CRUD operations plus queries to find and delete network configurations
 * by their parent service profile.</p>
 */
@Repository
public interface NetworkConfigRepository extends JpaRepository<NetworkConfig, UUID> {

    /**
     * Finds all network configurations for a service profile.
     *
     * @param serviceProfileId the service profile ID
     * @return list of network configurations
     */
    List<NetworkConfig> findByServiceProfileId(UUID serviceProfileId);

    /**
     * Deletes all network configurations for a service profile.
     *
     * @param serviceProfileId the service profile ID
     */
    void deleteByServiceProfileId(UUID serviceProfileId);
}
