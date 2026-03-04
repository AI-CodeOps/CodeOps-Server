#!/bin/bash
# ============================================
# CodeOps Platform Launcher
# ============================================
# Starts all infrastructure, backend services, and client app
#
# Usage:
#   ./start-codeops.sh          # Start everything (infra + backends)
#   ./start-codeops.sh all      # Start everything including client
#   ./start-codeops.sh infra    # Start only infrastructure
#   ./start-codeops.sh core     # Start only Core service
#   ./start-codeops.sh analytics # Start only Analytics service
#   ./start-codeops.sh client   # Start only Client app
#   ./start-codeops.sh seed     # Load seed data
#   ./start-codeops.sh stop     # Stop everything
#   ./start-codeops.sh status   # Show service status

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration — resolve paths relative to this script's location
SCRIPT_ROOT="$(cd "$(dirname "$0")" && pwd)"
GITHUB_DIR="${GITHUB_DIR:-$(dirname "$SCRIPT_ROOT")}"
CORE_DIR="$SCRIPT_ROOT"
ANALYTICS_DIR="$GITHUB_DIR/Zevaro-Analytics"
CLIENT_DIR="$GITHUB_DIR/CodeOps-Client"
SCRIPTS_DIR="$SCRIPT_ROOT/scripts"
LOGS_DIR="$GITHUB_DIR/logs"

CORE_PORT=8090
ANALYTICS_PORT=8081
CLIENT_PORT=3000

# Log functions
info() { echo -e "${BLUE}info:${NC} $1"; }
success() { echo -e "${GREEN}done:${NC} $1"; }
warn() { echo -e "${YELLOW}warn:${NC} $1"; }
error() { echo -e "${RED}error:${NC} $1"; exit 1; }

# Check if a port is in use
port_in_use() {
    lsof -i :"$1" >/dev/null 2>&1
}

# Kill process on port
kill_port() {
    local port=$1
    local pids=$(lsof -ti :"$port" 2>/dev/null)
    if [ -n "$pids" ]; then
        echo "$pids" | xargs kill -9 2>/dev/null || true
    fi
}

# Wait for a service to be healthy
wait_for_service() {
    local name=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=1

    info "Waiting for $name to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" >/dev/null 2>&1; then
            success "$name is ready!"
            return 0
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    echo ""
    error "$name failed to start after $max_attempts attempts"
}

# Wait for PostgreSQL
wait_for_postgres() {
    local max_attempts=30
    local attempt=1

    info "Waiting for PostgreSQL..."
    while [ $attempt -le $max_attempts ]; do
        if docker exec codeops-db pg_isready -U codeops -d codeops >/dev/null 2>&1; then
            success "PostgreSQL is ready!"
            return 0
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    echo ""
    error "PostgreSQL failed to start"
}

# Wait for Kafka
wait_for_kafka() {
    local max_attempts=30
    local attempt=1

    info "Waiting for Kafka..."
    while [ $attempt -le $max_attempts ]; do
        if docker exec codeops-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; then
            success "Kafka is ready!"
            return 0
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    echo ""
    error "Kafka failed to start"
}

# Start infrastructure
start_infra() {
    info "Starting CodeOps infrastructure..."

    cd "$CORE_DIR"

    if [ ! -f "docker-compose.yml" ]; then
        error "docker-compose.yml not found in $CORE_DIR"
    fi

    # Start containers
    docker-compose up -d postgres-server postgres-vault redis zookeeper kafka

    # Wait for services
    wait_for_postgres
    sleep 3
    wait_for_kafka

    # Initialize Kafka topics
    info "Creating Kafka topics..."
    docker-compose up kafka-init

    success "Infrastructure ready!"
    echo ""
    echo "  PostgreSQL: localhost:5432"
    echo "  Redis:      localhost:6379"
    echo "  Kafka:      localhost:9092"
    echo ""
}

# Start Core service
start_core() {
    info "Starting CodeOps-Server on port $CORE_PORT..."

    if port_in_use $CORE_PORT; then
        warn "Port $CORE_PORT already in use. Core may already be running."
        return 0
    fi

    if [ ! -d "$CORE_DIR" ]; then
        error "CodeOps-Server not found at $CORE_DIR"
    fi

    # Start in background with nohup
    cd "$CORE_DIR"
    nohup mvn spring-boot:run \
        -Dspring-boot.run.arguments="--spring.jpa.hibernate.ddl-auto=update" \
        > "$LOGS_DIR/core.log" 2>&1 &

    wait_for_service "CodeOps-Server" "http://localhost:$CORE_PORT/actuator/health" 60
}

# Start Analytics service
start_analytics() {
    info "Starting CodeOps-Analytics on port $ANALYTICS_PORT..."

    if port_in_use $ANALYTICS_PORT; then
        warn "Port $ANALYTICS_PORT already in use. Analytics may already be running."
        return 0
    fi

    if [ ! -d "$ANALYTICS_DIR" ]; then
        error "CodeOps-Analytics not found at $ANALYTICS_DIR"
    fi

    # Start in background with nohup
    cd "$ANALYTICS_DIR"
    nohup mvn spring-boot:run \
        -Dspring-boot.run.arguments="--spring.jpa.hibernate.ddl-auto=update" \
        > "$LOGS_DIR/analytics.log" 2>&1 &

    wait_for_service "CodeOps-Analytics" "http://localhost:$ANALYTICS_PORT/actuator/health" 60
}

# Start Client app
start_client() {
    info "Starting CodeOps-Client on port $CLIENT_PORT..."

    if port_in_use $CLIENT_PORT; then
        warn "Port $CLIENT_PORT already in use. Client may already be running."
        return 0
    fi

    if [ ! -d "$CLIENT_DIR" ]; then
        error "CodeOps-Client not found at $CLIENT_DIR"
    fi

    cd "$CLIENT_DIR"

    # Start Flutter web server in background (headless mode)
    nohup flutter run -d web-server --web-port $CLIENT_PORT > "$LOGS_DIR/client.log" 2>&1 &

    # Wait for web server
    local max_attempts=60
    local attempt=1
    info "Waiting for CodeOps-Client to be ready (this may take a while for first build)..."
    while [ $attempt -le $max_attempts ]; do
        if port_in_use $CLIENT_PORT; then
            success "CodeOps-Client is ready!"
            echo "  Open: http://localhost:$CLIENT_PORT"
            return 0
        fi
        echo -n "."
        sleep 3
        ((attempt++))
    done
    echo ""
    warn "CodeOps-Client may still be starting. Check logs: tail -f $LOGS_DIR/client.log"
}

# Load seed data
load_seed() {
    info "Loading seed data..."

    if [ ! -f "$SCRIPTS_DIR/seed-codeops.sh" ]; then
        error "seed-codeops.sh not found at $SCRIPTS_DIR"
    fi

    "$SCRIPTS_DIR/seed-codeops.sh" "$@"
}

# Stop everything
stop_all() {
    info "Stopping CodeOps services..."

    # Stop by port (most reliable)
    if port_in_use $CORE_PORT; then
        kill_port $CORE_PORT
        success "Stopped CodeOps-Server"
    fi

    if port_in_use $ANALYTICS_PORT; then
        kill_port $ANALYTICS_PORT
        success "Stopped CodeOps-Analytics"
    fi

    if port_in_use $CLIENT_PORT; then
        kill_port $CLIENT_PORT
        success "Stopped CodeOps-Client"
    fi

    # Also kill any lingering Maven/Flutter processes
    pkill -f "spring-boot:run.*CodeOps" 2>/dev/null || true
    pkill -f "flutter.*CodeOps" 2>/dev/null || true

    # Stop infrastructure
    cd "$CORE_DIR"
    if [ -f "docker-compose.yml" ]; then
        docker-compose down
        success "Stopped infrastructure"
    fi

    success "All services stopped"
}

# Stop only backend services (keep infra running)
stop_services() {
    info "Stopping backend services..."

    if port_in_use $CORE_PORT; then
        kill_port $CORE_PORT
        success "Stopped CodeOps-Server"
    fi

    if port_in_use $ANALYTICS_PORT; then
        kill_port $ANALYTICS_PORT
        success "Stopped CodeOps-Analytics"
    fi

    if port_in_use $CLIENT_PORT; then
        kill_port $CLIENT_PORT
        success "Stopped CodeOps-Client"
    fi

    pkill -f "spring-boot:run.*CodeOps" 2>/dev/null || true
    pkill -f "flutter.*CodeOps" 2>/dev/null || true

    success "Backend services stopped (infrastructure still running)"
}

# Show status
show_status() {
    echo ""
    echo "=========================================="
    echo "        CODEOPS SERVICE STATUS"
    echo "=========================================="
    echo ""

    # Infrastructure
    echo "Infrastructure:"
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q codeops-db; then
        echo -e "  PostgreSQL      ${GREEN}RUNNING${NC}   localhost:5432"
    else
        echo -e "  PostgreSQL      ${RED}STOPPED${NC}"
    fi

    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q codeops-redis; then
        echo -e "  Redis           ${GREEN}RUNNING${NC}   localhost:6379"
    else
        echo -e "  Redis           ${RED}STOPPED${NC}"
    fi

    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q codeops-kafka; then
        echo -e "  Kafka           ${GREEN}RUNNING${NC}   localhost:9092"
    else
        echo -e "  Kafka           ${RED}STOPPED${NC}"
    fi

    echo ""
    echo "Services:"
    if port_in_use $CORE_PORT; then
        echo -e "  CodeOps-Server   ${GREEN}RUNNING${NC}   localhost:$CORE_PORT"
    else
        echo -e "  CodeOps-Server   ${RED}STOPPED${NC}"
    fi

    if port_in_use $ANALYTICS_PORT; then
        echo -e "  CodeOps-Analytics ${GREEN}RUNNING${NC}   localhost:$ANALYTICS_PORT"
    else
        echo -e "  CodeOps-Analytics ${RED}STOPPED${NC}"
    fi

    if port_in_use $CLIENT_PORT; then
        echo -e "  CodeOps-Client   ${GREEN}RUNNING${NC}   localhost:$CLIENT_PORT"
    else
        echo -e "  CodeOps-Client   ${RED}STOPPED${NC}"
    fi

    echo ""
    echo "=========================================="
    echo ""
}

# Show logs
show_logs() {
    local service=$1
    case $service in
        core)
            tail -f "$LOGS_DIR/core.log"
            ;;
        analytics)
            tail -f "$LOGS_DIR/analytics.log"
            ;;
        client)
            tail -f "$LOGS_DIR/client.log"
            ;;
        all)
            tail -f "$LOGS_DIR"/*.log
            ;;
        *)
            echo "Usage: $0 logs [core|analytics|client|all]"
            ;;
    esac
}

# Print usage
print_usage() {
    echo "CodeOps Platform Launcher"
    echo ""
    echo "Usage: $0 <command>"
    echo ""
    echo "Commands:"
    echo "  (default)   Start infrastructure + backend services"
    echo "  all         Start everything including client app"
    echo "  infra       Start only infrastructure (Postgres, Redis, Kafka)"
    echo "  core        Start only CodeOps-Server API"
    echo "  analytics   Start only CodeOps-Analytics"
    echo "  client      Start only CodeOps-Client Flutter app"
    echo "  seed [opt]  Load seed data (options: all|reset|kafka|sql|clear)"
    echo "  stop        Stop all services and infrastructure"
    echo "  restart     Restart backend services (keep infra)"
    echo "  status      Show service status"
    echo "  logs <svc>  Tail logs (core|analytics|client|all)"
    echo ""
    echo "Examples:"
    echo "  $0              # Start infra + Server + Analytics"
    echo "  $0 all          # Start everything including Client"
    echo "  $0 seed         # Load seed data"
    echo "  $0 seed reset   # Clear and reload seed data"
    echo "  $0 logs core    # Tail Server logs"
    echo ""
}

# Main
main() {
    # Create logs directory
    mkdir -p "$LOGS_DIR"

    case "${1:-}" in
        ""|backends)
            echo ""
            echo "Starting CodeOps Platform (backends only)"
            echo "=========================================="
            echo ""
            start_infra
            start_core
            start_analytics
            show_status
            success "CodeOps backends ready!"
            echo ""
            echo "To start client app: $0 client"
            echo "To load seed data: $0 seed"
            echo "To view logs: $0 logs [core|analytics]"
            echo "To stop: $0 stop"
            echo ""
            ;;
        all)
            echo ""
            echo "Starting CodeOps Platform (full stack)"
            echo "=========================================="
            echo ""
            start_infra
            start_core
            start_analytics
            start_client
            show_status
            success "CodeOps platform fully started!"
            echo ""
            echo "Client app: http://localhost:$CLIENT_PORT"
            echo "To load seed data: $0 seed"
            echo "To stop: $0 stop"
            echo ""
            ;;
        infra)
            start_infra
            ;;
        core)
            start_core
            ;;
        analytics)
            start_analytics
            ;;
        client|web)
            start_client
            ;;
        seed)
            shift
            load_seed "$@"
            ;;
        stop)
            stop_all
            ;;
        restart)
            stop_services
            echo ""
            start_core
            start_analytics
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$2"
            ;;
        help|--help|-h)
            print_usage
            ;;
        *)
            print_usage
            exit 1
            ;;
    esac
}

main "$@"
