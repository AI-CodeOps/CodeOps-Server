package com.codeops.config;

import java.util.UUID;

/**
 * Application-wide constants for the CodeOps platform.
 *
 * <p>Defines limits for team membership, file sizes, authentication token lifetimes,
 * S3 storage prefixes, QA agent configuration, pagination defaults, notification
 * scheduling parameters, and Registry module constants (port ranges, slug rules,
 * health checks, topology layers). These values are used across services and controllers
 * to enforce consistent business rules.</p>
 *
 * <p>This class cannot be instantiated.</p>
 */
public final class AppConstants {
    private AppConstants() {}

    // Team limits
    public static final int MAX_TEAM_MEMBERS = 50;
    public static final int MAX_PROJECTS_PER_TEAM = 100;
    public static final int MAX_PERSONAS_PER_TEAM = 50;
    public static final int MAX_DIRECTIVES_PER_PROJECT = 20;

    // File size limits
    public static final int MAX_REPORT_SIZE_MB = 25;
    public static final int MAX_PERSONA_SIZE_KB = 100;
    public static final int MAX_DIRECTIVE_SIZE_KB = 200;
    public static final int MAX_SPEC_FILE_SIZE_MB = 50;

    // Auth
    public static final int JWT_EXPIRY_HOURS = 24;
    public static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
    public static final int INVITATION_EXPIRY_DAYS = 7;
    public static final int MIN_PASSWORD_LENGTH = 1;

    // Notifications
    public static final int HEALTH_DIGEST_DAY = 1;  // Monday
    public static final int HEALTH_DIGEST_HOUR = 8;  // 8 AM

    // S3 prefixes
    public static final String S3_REPORTS = "reports/";
    public static final String S3_SPECS = "specs/";
    public static final String S3_PERSONAS = "personas/";
    public static final String S3_RELEASES = "releases/";

    // QA
    public static final int MAX_CONCURRENT_AGENTS = 5;
    public static final int AGENT_TIMEOUT_MINUTES = 15;
    public static final int DEFAULT_HEALTH_SCORE = 100;

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // ── Registry Constants ──

    // Registry seed data identifiers
    public static final UUID SEED_TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID SEED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // Port ranges (defaults)
    public static final int HTTP_API_RANGE_START = 8080;
    public static final int HTTP_API_RANGE_END = 8199;
    public static final int FRONTEND_DEV_RANGE_START = 3000;
    public static final int FRONTEND_DEV_RANGE_END = 3199;
    public static final int DATABASE_RANGE_START = 5432;
    public static final int DATABASE_RANGE_END = 5499;
    public static final int REDIS_RANGE_START = 6379;
    public static final int REDIS_RANGE_END = 6399;
    public static final int KAFKA_RANGE_START = 9092;
    public static final int KAFKA_RANGE_END = 9099;
    public static final int KAFKA_INTERNAL_RANGE_START = 29092;
    public static final int KAFKA_INTERNAL_RANGE_END = 29099;
    public static final int ZOOKEEPER_RANGE_START = 2181;
    public static final int ZOOKEEPER_RANGE_END = 2199;
    public static final int GRPC_RANGE_START = 50051;
    public static final int GRPC_RANGE_END = 50099;
    public static final int WEBSOCKET_RANGE_START = 8200;
    public static final int WEBSOCKET_RANGE_END = 8249;
    public static final int DEBUG_RANGE_START = 5005;
    public static final int DEBUG_RANGE_END = 5049;
    public static final int ACTUATOR_RANGE_START = 8300;
    public static final int ACTUATOR_RANGE_END = 8349;

    // Slug validation
    public static final String SLUG_PATTERN = "^[a-z0-9][a-z0-9-]*[a-z0-9]$";
    public static final int SLUG_MIN_LENGTH = 2;
    public static final int SLUG_MAX_LENGTH = 63;

    // Health check
    public static final int HEALTH_CHECK_TIMEOUT_SECONDS = 5;
    public static final int DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS = 30;

    // Registry per-team limits
    public static final int MAX_SERVICES_PER_TEAM = 200;
    public static final int MAX_SOLUTIONS_PER_TEAM = 50;
    public static final int MAX_PORTS_PER_SERVICE = 20;
    public static final int MAX_DEPENDENCIES_PER_SERVICE = 50;
    public static final int MAX_WORKSTATION_PROFILES_PER_TEAM = 20;

    // Config engine
    public static final int CONFIG_TEMPLATE_MAX_CONTENT_SIZE = 1_000_000;
    public static final String DOCKER_COMPOSE_VERSION = "3.8";
    public static final String DEFAULT_DOCKER_NETWORK = "codeops-network";

    // Topology
    public static final int TOPOLOGY_MAX_NEIGHBORHOOD_DEPTH = 3;
    public static final String TOPOLOGY_LAYER_INFRASTRUCTURE = "infrastructure";
    public static final String TOPOLOGY_LAYER_BACKEND = "backend";
    public static final String TOPOLOGY_LAYER_FRONTEND = "frontend";
    public static final String TOPOLOGY_LAYER_GATEWAY = "gateway";
    public static final String TOPOLOGY_LAYER_STANDALONE = "standalone";

    // ── Logger Constants ──

    // Logger API prefix
    public static final String LOGGER_API_PREFIX = "/api/v1/logger";

    // Log ingestion
    public static final int MAX_BATCH_SIZE = 1000;
    public static final int MAX_LOG_MESSAGE_LENGTH = 65_536;
    public static final int MAX_CUSTOM_FIELDS = 50;

    // Kafka topics
    public static final String KAFKA_LOG_TOPIC = "codeops-logs";
    public static final String KAFKA_METRICS_TOPIC = "codeops-metrics";
    public static final String KAFKA_CONSUMER_GROUP = "codeops-logger";

    // Retention
    public static final int DEFAULT_RETENTION_DAYS = 30;
    public static final int MAX_RETENTION_DAYS = 365;
    public static final int MIN_RETENTION_DAYS = 1;

    // Query
    public static final int MAX_QUERY_RESULTS = 10_000;
    public static final int DEFAULT_QUERY_LIMIT = 100;
    public static final int MAX_QUERY_TIME_RANGE_DAYS = 90;

    // Metrics resolution
    public static final int DEFAULT_METRIC_RESOLUTION_SECONDS = 60;
    public static final int MIN_METRIC_RESOLUTION_SECONDS = 10;
    public static final int MAX_METRIC_RESOLUTION_SECONDS = 3600;

    // Dashboards
    public static final int MAX_WIDGETS_PER_DASHBOARD = 20;
    public static final int MAX_DASHBOARDS_PER_TEAM = 50;
    public static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 30;

    // Traps
    public static final int MAX_TRAPS_PER_TEAM = 100;
    public static final int MAX_TRAP_CONDITIONS = 10;

    // Alerts
    public static final int DEFAULT_THROTTLE_MINUTES = 5;
    public static final int MAX_ALERT_CHANNELS = 20;

    // Anomaly detection
    public static final int DEFAULT_BASELINE_WINDOW_HOURS = 168;
    public static final double DEFAULT_DEVIATION_THRESHOLD = 2.0;

    // Timeouts
    public static final int REQUEST_TIMEOUT_SECONDS = 30;

    // ── Courier Constants ──

    /** Base path prefix for all Courier API endpoints. */
    public static final String COURIER_API_PREFIX = "/api/v1/courier";

    /** Service name used in health checks and structured logging. */
    public static final String COURIER_SERVICE_NAME = "codeops-courier";

    // Courier rate limiting
    public static final int COURIER_RATE_LIMIT_REQUESTS = 100;
    public static final int COURIER_RATE_LIMIT_WINDOW_SECONDS = 60;

    // Courier HTTP proxy
    public static final int COURIER_DEFAULT_TIMEOUT_MS = 30000;
    public static final int COURIER_MAX_TIMEOUT_MS = 300000;
    public static final int COURIER_MIN_TIMEOUT_MS = 1000;
    public static final int COURIER_MAX_REDIRECT_COUNT = 10;
    public static final int COURIER_MAX_RESPONSE_BODY_SIZE = 10 * 1024 * 1024;
    public static final int COURIER_HISTORY_BODY_TRUNCATE_SIZE = 1024 * 1024;
    public static final String COURIER_USER_AGENT = "CodeOps-Courier/1.0";

    // Courier script engine
    public static final int COURIER_SCRIPT_TIMEOUT_SECONDS = 5;
    public static final int COURIER_SCRIPT_MAX_STATEMENTS = 100000;
    public static final int COURIER_SCRIPT_MAX_CONSOLE_LINES = 1000;
    public static final int COURIER_SCRIPT_MAX_OUTPUT_SIZE = 1024 * 1024;

    // ── Relay Constants ──

    /** Base path prefix for all Relay API endpoints. */
    public static final String RELAY_API_PREFIX = "/api/v1/relay";

    /** Maximum character length for a channel or DM message. */
    public static final int RELAY_MAX_MESSAGE_LENGTH = 10_000;

    /** Maximum character length for a channel name. */
    public static final int RELAY_MAX_CHANNEL_NAME_LENGTH = 100;

    /** Maximum character length for a channel topic. */
    public static final int RELAY_MAX_TOPIC_LENGTH = 500;

    /** Maximum character length for a user status message. */
    public static final int RELAY_MAX_STATUS_MESSAGE_LENGTH = 200;

    /** Maximum file upload size in bytes (25 MB). */
    public static final int RELAY_MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024;

    /** Maximum number of pinned messages per channel. */
    public static final int RELAY_MAX_PINS_PER_CHANNEL = 50;

    /** Maximum number of reactions per message. */
    public static final int RELAY_MAX_REACTIONS_PER_MESSAGE = 50;

    /** Maximum number of participants in a group DM. */
    public static final int RELAY_MAX_GROUP_DM_PARTICIPANTS = 10;

    /** Heartbeat timeout in seconds before a user is considered offline. */
    public static final int RELAY_PRESENCE_HEARTBEAT_TIMEOUT_SECONDS = 120;

    /** Default page size for Relay paginated queries. */
    public static final int RELAY_DEFAULT_PAGE_SIZE = 50;

    /** Maximum page size for Relay paginated queries. */
    public static final int RELAY_MAX_PAGE_SIZE = 100;

    /** Slug of the default general channel auto-created for each team. */
    public static final String RELAY_GENERAL_CHANNEL_SLUG = "general";

    /** MIME types allowed for file uploads in Relay. */
    public static final String[] RELAY_ALLOWED_FILE_TYPES = {
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "application/pdf", "text/plain", "text/markdown",
            "application/json", "application/zip"
    };
}
