package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Port mapping for a service profile defining host-to-container port forwarding.
 *
 * <p>Maps a host port to a container port with a specified protocol (tcp or udp),
 * enabling external access to services running inside Docker containers.</p>
 */
@Entity
@Table(name = "fleet_port_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortMapping extends BaseEntity {

    /** Host-side port number exposed on the Docker host. */
    @Column(name = "host_port", nullable = false)
    private Integer hostPort;

    /** Container-side port number the service listens on. */
    @Column(name = "container_port", nullable = false)
    private Integer containerPort;

    /** Network protocol for this port mapping (tcp or udp). */
    @Builder.Default
    @Column(name = "protocol", nullable = false, length = 10)
    private String protocol = "tcp";

    /** Parent service profile that owns this port mapping. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id", nullable = false)
    private ServiceProfile serviceProfile;
}
