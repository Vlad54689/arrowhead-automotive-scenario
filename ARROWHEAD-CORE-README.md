# Arrowhead Core Systems Setup

This directory contains the setup for running Arrowhead core systems locally using Docker.

## What are Arrowhead Core Systems?

The Eclipse Arrowhead framework requires these core systems to function:

1. **Service Registry** (port 8080) - Discovers and registers available services
2. **Orchestrator** (port 8081) - Routes requests to appropriate service instances
3. **Authorization** (port 8082) - Validates service access permissions
4. **Event Handler** (port 8083) - Pub/sub messaging for event-driven architectures

## Quick Start

### Start All Core Systems

```bash
./start-arrowhead-core.sh
```

### Stop All Core Systems

```bash
./start-arrowhead-core.sh --stop
```

### View Logs

```bash
./start-arrowhead-core.sh --logs
```

### Check Status

```bash
./start-arrowhead-core.sh --status
```

## Service Endpoints

After starting, the services will be available at:

| Service | URL | Health Check |
|---------|-----|--------------|
| Service Registry | http://localhost:8080 | http://localhost:8080/orchestrator/health |
| Orchestrator | http://localhost:8081 | http://localhost:8081/health |
| Authorization | http://localhost:8082 | http://localhost:8082/health |
| Event Handler | http://localhost:8083 | http://localhost:8083/health |

## Configuration

The core systems are configured with the `secure` profile by default, which enables:
- Mutual TLS authentication
- Certificate-based authorization
- Secure service discovery

## Running Demo Services

Once the core systems are running, you can start the demo services:

```bash
# Navigate to the demo services directory
cd sos-examples-spring/demo-car-with-events

# Build the services
mvn clean install

# Start the services
./start_quality_inspection_demo.sh
```

## Troubleshooting

### Services not starting

Check the logs:
```bash
./start-arrowhead-core.sh --logs
```

### Port conflicts

If ports 8080-8083 are already in use, you can modify the `docker-compose-arrowhead-core.yml` file to use different ports.

### Docker not running

Start Docker before running the startup script:
```bash
# On macOS/Windows
open Docker Desktop

# On Linux
sudo systemctl start docker
```

## Advanced Usage

### Using docker-compose directly

```bash
# Start in background
docker-compose -f docker-compose-arrowhead-core.yml up -d

# Stop
docker-compose -f docker-compose-arrowhead-core.yml down

# View logs
docker-compose -f docker-compose-arrowhead-core.yml logs -f

# Restart
docker-compose -f docker-compose-arrowhead-core.yml restart
```

### Custom Java Options

Edit the `JAVA_OPTS` environment variable in the docker-compose file to adjust memory settings or add JVM options.

## References

- [Eclipse Arrowhead Official Website](https://www.arrowhead.eu/)
- [Arrowhead Framework Documentation](https://arrowhead-framework.github.io/)
- [Arrowhead GitHub Repository](https://github.com/arrowhead-framework)
