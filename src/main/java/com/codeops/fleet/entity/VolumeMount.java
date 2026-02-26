package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Volume mount configuration for a service profile.
 *
 * <p>Defines how host directories or named Docker volumes are mounted into a container.
 * Either {@code hostPath} (bind mount) or {@code volumeName} (named volume) should be set,
 * but not both.</p>
 */
@Entity
@Table(name = "fleet_volume_mounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolumeMount extends BaseEntity {

    /** Host filesystem path for bind mounts (null for named volumes). */
    @Column(name = "host_path", length = 500)
    private String hostPath;

    /** Mount path inside the container. */
    @Column(name = "container_path", nullable = false, length = 500)
    private String containerPath;

    /** Named Docker volume (null for bind mounts). */
    @Column(name = "volume_name", length = 200)
    private String volumeName;

    /** Whether this volume is mounted as read-only. */
    @Builder.Default
    @Column(name = "is_read_only", nullable = false)
    private boolean isReadOnly = false;

    /** Parent service profile that owns this volume mount. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id", nullable = false)
    private ServiceProfile serviceProfile;
}
