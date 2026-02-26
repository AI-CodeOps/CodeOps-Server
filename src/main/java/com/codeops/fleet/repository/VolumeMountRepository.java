package com.codeops.fleet.repository;

import com.codeops.fleet.entity.VolumeMount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link VolumeMount} entities.
 *
 * <p>Provides CRUD operations plus queries to find and delete volume mounts
 * by their parent service profile.</p>
 */
@Repository
public interface VolumeMountRepository extends JpaRepository<VolumeMount, UUID> {

    /**
     * Finds all volume mounts for a service profile.
     *
     * @param serviceProfileId the service profile ID
     * @return list of volume mounts
     */
    List<VolumeMount> findByServiceProfileId(UUID serviceProfileId);

    /**
     * Deletes all volume mounts for a service profile.
     *
     * @param serviceProfileId the service profile ID
     */
    void deleteByServiceProfileId(UUID serviceProfileId);
}
