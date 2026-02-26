package com.codeops.fleet.repository;

import com.codeops.fleet.entity.EnvironmentVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link EnvironmentVariable} entities.
 *
 * <p>Provides CRUD operations plus queries to find and delete environment variables
 * by their parent service profile.</p>
 */
@Repository("fleetEnvironmentVariableRepository")
public interface EnvironmentVariableRepository extends JpaRepository<EnvironmentVariable, UUID> {

    /**
     * Finds all environment variables for a service profile.
     *
     * @param serviceProfileId the service profile ID
     * @return list of environment variables
     */
    List<EnvironmentVariable> findByServiceProfileId(UUID serviceProfileId);

    /**
     * Deletes all environment variables for a service profile.
     *
     * @param serviceProfileId the service profile ID
     */
    void deleteByServiceProfileId(UUID serviceProfileId);
}
