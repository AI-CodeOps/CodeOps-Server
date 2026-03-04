#!/bin/bash
# Stop CodeOps infrastructure.
#
# Usage: ./scripts/stop-infra.sh

set -e

cd "$(dirname "$0")/.."

echo "Stopping CodeOps infrastructure..."
docker compose down
echo "Infrastructure stopped."
