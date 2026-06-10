# Arrowhead Core Systems Setup

This directory contains the setup for running Arrowhead core systems locally using Docker.

## What are Arrowhead Core Systems?

The Eclipse Arrowhead framework requires these core systems to function:

1. **Service Registry** (port 8443) - Discovers and registers available services
2. **Orchestrator** (port 8441) - Routes requests to appropriate service instances
3. **Authorization** (port 8445) - Validates service access permissions
4. **Event Handler** (port 8455) - Pub/sub messaging for event-driven architectures
5. **Gatekeeper** (port 8449) - Manages access control policies
6. **Gateway** (port 8453) - Entry point for external requests
7. **Certificate Authority** (port 8448) - Issues and manages certificates

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
| Service Registry | https://localhost:8443 | https://localhost:8443/orchestrator/health |
| Orchestrator | https://localhost:8441 | https://localhost:8441/health |
| Authorization | https://localhost:8445 | https://localhost:8445/health |
| Event Handler | https://localhost:8455 | https://localhost:8455/health |
| Gatekeeper | https://localhost:8449 | https://localhost:8449/health |
| Gateway | https://localhost:8453 | https://localhost:8453/health |
| Certificate Authority | https://localhost:8448 | https://localhost:8448/health |

## Configuration

The core systems are configured with the `secure` profile by default, which enables:
- Mutual TLS authentication
- Certificate-based authorization
- Secure service discovery

## Firewall Configuration

**Note:** If you have UFW firewall enabled, you need to allow the following ports:

```bash
sudo ufw allow 8441/tcp
sudo ufw allow 8443/tcp
sudo ufw allow 8445/tcp
sudo ufw allow 8448/tcp
sudo ufw allow 8449/tcp
sudo ufw allow 8453/tcp
sudo ufw allow 8455/tcp
sudo ufw allow 3306/tcp
```

Or allow all ports at once:
```bash
sudo ufw allow 8441:8455/tcp
sudo ufw allow 3306/tcp
```

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

If ports 8441-8455 are already in use, you can modify the `docker-compose-arrowhead-core.yml` file to use different ports.

### Docker not running

Start Docker before running the startup script:
```bash
# On macOS/Windows
open Docker Desktop

# On Linux
sudo systemctl start docker
```

### Database connection issues

The Arrowhead core systems use MySQL 5.7 for persistence. Check that the database container is running:
```bash
docker ps | grep arrowhead_core_mysql
```

### Firewall blocking connections

If services are running but not accessible, check your firewall settings:
```bash
sudo ufw status
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

Edit the `JVM_FLAGS` environment variable in the docker-compose file to adjust memory settings or add JVM options.

### Database Initialization

The SQL initialization scripts in `sql/` are automatically executed when the MySQL container starts for the first time. To reset the database:

```bash
docker-compose -f docker-compose-arrowhead-core.yml down
docker volume rm sos-arrowhead-automotive_arrowhead_core_mysql
docker-compose -f docker-compose-arrowhead-core.yml up -d
```

## References

- [Eclipse Arrowhead Official Website](https://www.arrowhead.eu/)
- [Arrowhead Framework Documentation](https://arrowhead-framework.github.io/)
- [Arrowhead GitHub Repository](https://github.com/arrowhead-framework)
