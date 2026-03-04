#!/bin/bash
# Start CodeOps infrastructure and wait for all services to be healthy.
#
# Usage: ./scripts/start-infra.sh

set -e

TIMEOUT=60
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

echo "Starting CodeOps infrastructure..."
docker compose up -d

# wait_healthy <container_name> <label>
wait_healthy() {
    local container="$1"
    local label="$2"
    local elapsed=0

    printf "  Waiting for %-20s " "$label..."
    while [ $elapsed -lt $TIMEOUT ]; do
        status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "missing")
        if [ "$status" = "healthy" ]; then
            echo "healthy"
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done

    echo "TIMEOUT (${TIMEOUT}s)"
    echo ""
    echo "Last 20 log lines for $container:"
    docker logs --tail 20 "$container" 2>&1
    return 1
}

echo ""
echo "Waiting for services (timeout: ${TIMEOUT}s)..."

FAILED=0
wait_healthy codeops-db         "PostgreSQL (Server)" || FAILED=1
wait_healthy codeops-vault-db   "PostgreSQL (Vault)"  || FAILED=1
wait_healthy codeops-redis      "Redis"               || FAILED=1
wait_healthy codeops-zookeeper  "Zookeeper"           || FAILED=1
wait_healthy codeops-kafka      "Kafka"               || FAILED=1

if [ $FAILED -ne 0 ]; then
    echo ""
    echo "One or more services failed to start."
    exit 1
fi

# Initialize Kafka topics
echo ""
echo "Initializing Kafka topics..."
docker compose up kafka-init --no-log-prefix 2>/dev/null

echo ""
echo "=========================================="
echo "  CodeOps infrastructure is ready"
echo "=========================================="
echo ""
echo "  PostgreSQL (Server): localhost:5432  (user: codeops, pass: codeops, db: codeops)"
echo "  PostgreSQL (Vault):  localhost:5433  (user: codeops_vault, pass: codeops_vault, db: codeops_vault)"
echo "  Redis:               localhost:6379"
echo "  Zookeeper:           localhost:2181"
echo "  Kafka:               localhost:9092"
echo ""
echo "  View logs:  docker compose logs -f"
echo "  Stop:       ./scripts/stop-infra.sh"
echo ""
