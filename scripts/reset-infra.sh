#!/bin/bash
# Reset CodeOps infrastructure — destroys all data and volumes.
#
# Usage: ./scripts/reset-infra.sh

set -e

cd "$(dirname "$0")/.."

echo "WARNING: This will destroy ALL Docker volumes (databases, Redis, Kafka)."
echo "Proceeding in 3 seconds... (Ctrl+C to cancel)"
sleep 3

echo "Removing containers and volumes..."
docker compose down -v
echo "Infrastructure reset complete. All data removed."
