package com.codeops.relay.entity.enums;

/** Types of cross-module events posted to channels. */
public enum PlatformEventType {
    AUDIT_COMPLETED,
    ALERT_FIRED,
    SESSION_COMPLETED,
    SECRET_ROTATED,
    CONTAINER_CRASHED,
    SERVICE_REGISTERED,
    DEPLOYMENT_COMPLETED,
    BUILD_COMPLETED,
    FINDING_CRITICAL,
    MERGE_REQUEST_CREATED
}
