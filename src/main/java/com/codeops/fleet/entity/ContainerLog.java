package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Captured log line from a Docker container.
 *
 * <p>Stores individual log entries from container stdout or stderr streams,
 * along with the Docker-reported timestamp. Used for container log viewing
 * and debugging within Fleet.</p>
 */
@Entity
@Table(name = "fleet_container_logs",
        indexes = {
                @Index(name = "idx_fcl_container_id", columnList = "container_id"),
                @Index(name = "idx_fcl_timestamp", columnList = "timestamp")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerLog extends BaseEntity {

    /** Output stream this log line came from ({@code "stdout"} or {@code "stderr"}). */
    @Column(name = "stream", nullable = false, length = 10)
    private String stream;

    /** Log content text. */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Docker-reported timestamp for this log line. */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /** Container instance that produced this log entry. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private ContainerInstance container;
}
