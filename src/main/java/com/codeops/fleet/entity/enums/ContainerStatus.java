package com.codeops.fleet.entity.enums;

/** Lifecycle state of a Docker container managed by Fleet. */
public enum ContainerStatus {
    CREATED,
    RUNNING,
    PAUSED,
    RESTARTING,
    REMOVING,
    EXITED,
    DEAD,
    STOPPED
}
