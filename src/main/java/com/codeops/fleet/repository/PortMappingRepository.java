package com.codeops.fleet.repository;

import com.codeops.fleet.entity.PortMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PortMapping} entities.
 *
 * <p>Provides CRUD operations plus queries to find and delete port mappings
 * by their parent service profile.</p>
 */
@Repository
public interface PortMappingRepository extends JpaRepository<PortMapping, UUID> {

    /**
     * Finds all port mappings for a service profile.
     *
     * @param serviceProfileId the service profile ID
     * @return list of port mappings
     */
    List<PortMapping> findByServiceProfileId(UUID serviceProfileId);

    /**
     * Deletes all port mappings for a service profile.
     *
     * @param serviceProfileId the service profile ID
     */
    void deleteByServiceProfileId(UUID serviceProfileId);
}
