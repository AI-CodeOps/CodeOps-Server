package com.codeops.fleet.entity.enums;

/** Docker restart policy for a container. */
public enum RestartPolicy {
    NO,
    ALWAYS,
    ON_FAILURE,
    UNLESS_STOPPED
}
