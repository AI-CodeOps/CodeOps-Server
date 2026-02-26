package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Network configuration for a service profile.
 *
 * <p>Defines which Docker network a container should join, along with optional
 * network aliases and a fixed IP address. Aliases are stored as a JSON array string.</p>
 */
@Entity
@Table(name = "fleet_network_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkConfig extends BaseEntity {

    /** Docker network name this container should be attached to. */
    @Column(name = "network_name", nullable = false, length = 200)
    private String networkName;

    /** JSON array of network aliases for service discovery (e.g., {@code ["db","postgres"]}). */
    @Column(name = "aliases", columnDefinition = "TEXT")
    private String aliases;

    /** Fixed IP address within the Docker network (null for DHCP assignment). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Parent service profile that owns this network configuration. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id", nullable = false)
    private ServiceProfile serviceProfile;
}
