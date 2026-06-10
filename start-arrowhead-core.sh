#!/bin/bash

# Arrowhead Core Systems Startup Script
# This script starts all Arrowhead core systems using Docker Compose

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose-arrowhead-core.yml"

echo "=========================================="
echo "Arrowhead Core Systems Startup Script"
echo "=========================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker daemon is not running. Please start Docker first."
    exit 1
fi

# Check if compose file exists
if [ ! -f "${COMPOSE_FILE}" ]; then
    echo "ERROR: Docker Compose file not found: ${COMPOSE_FILE}"
    exit 1
fi

# Parse arguments
case "${1:-}" in
    --stop|-s)
        echo "Stopping Arrowhead Core Systems..."
        docker-compose -f "${COMPOSE_FILE}" down
        echo "All Arrowhead core systems have been stopped."
        exit 0
        ;;
    --restart|-r)
        echo "Restarting Arrowhead Core Systems..."
        docker-compose -f "${COMPOSE_FILE}" restart
        echo "All Arrowhead core systems have been restarted."
        exit 0
        ;;
    --status)
        echo "Arrowhead Core Systems Status:"
        docker-compose -f "${COMPOSE_FILE}" ps
        exit 0
        ;;
    --logs)
        echo "Arrowhead Core Systems Logs (Ctrl+C to exit):"
        docker-compose -f "${COMPOSE_FILE}" logs -f
        exit 0
        ;;
    --help|-h)
        echo "Usage: $0 [OPTIONS]"
        echo ""
        echo "Options:"
        echo "  (none)    Start all Arrowhead core systems"
        echo "  --stop    Stop all Arrowhead core systems"
        echo "  --restart Restart all Arrowhead core systems"
        echo "  --status  Show status of all Arrowhead core systems"
        echo "  --logs    View logs from all Arrowhead core systems"
        echo "  --help    Show this help message"
        exit 0
        ;;
esac

# Start the core systems
echo "Starting Arrowhead Core Systems..."
echo ""
echo "This will start the following services:"
echo "  - Service Registry (https://localhost:8443)"
echo "  - Orchestrator (https://localhost:8441)"
echo "  - Authorization (https://localhost:8445)"
echo "  - Event Handler (https://localhost:8455)"
echo "  - Gatekeeper (https://localhost:8449)"
echo "  - Gateway (https://localhost:8453)"
echo "  - Certificate Authority (https://localhost:8448)"
echo ""
echo "Waiting for services to become healthy..."
echo ""

# Start services and wait for them to be healthy
docker-compose -f "${COMPOSE_FILE}" up -d --wait

echo ""
echo "=========================================="
echo "Arrowhead Core Systems Started!"
echo "=========================================="
echo ""
echo "Service Endpoints:"
echo "  Service Registry:      https://localhost:8443"
echo "  Orchestrator:          https://localhost:8441"
echo "  Authorization:         https://localhost:8445"
echo "  Event Handler:         https://localhost:8455"
echo "  Gatekeeper:            https://localhost:8449"
echo "  Gateway:               https://localhost:8453"
echo "  Certificate Authority: https://localhost:8448"
echo ""
echo "To view logs:    $0 --logs"
echo "To stop:         $0 --stop"
echo "To check status: $0 --status"
echo ""
echo "To view logs:    $0 --logs"
echo "To stop:         $0 --stop"
echo "To check status: $0 --status"
echo ""
