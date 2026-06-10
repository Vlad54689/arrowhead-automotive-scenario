-- Create databases for Arrowhead Core services
CREATE DATABASE IF NOT EXISTS service_registry;
CREATE DATABASE IF NOT EXISTS authorization;
CREATE DATABASE IF NOT EXISTS gatekeeper;
CREATE DATABASE IF NOT EXISTS event_handler;
CREATE DATABASE IF NOT EXISTS gateway;
CREATE DATABASE IF NOT EXISTS orchestrator;

-- Create users for Arrowhead Core services
CREATE USER IF NOT EXISTS 'service_registry'@'%' IDENTIFIED BY 'service_registry_password';
CREATE USER IF NOT EXISTS 'authorization'@'%' IDENTIFIED BY 'authorization_password';
CREATE USER IF NOT EXISTS 'gatekeeper'@'%' IDENTIFIED BY 'gatekeeper_password';
CREATE USER IF NOT EXISTS 'event_handler'@'%' IDENTIFIED BY 'event_handler_password';
CREATE USER IF NOT EXISTS 'gateway'@'%' IDENTIFIED BY 'gateway_password';
CREATE USER IF NOT EXISTS 'orchestrator'@'%' IDENTIFIED BY 'orchestrator_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON service_registry.* TO 'service_registry'@'%';
GRANT ALL PRIVILEGES ON authorization.* TO 'authorization'@'%';
GRANT ALL PRIVILEGES ON gatekeeper.* TO 'gatekeeper'@'%';
GRANT ALL PRIVILEGES ON event_handler.* TO 'event_handler'@'%';
GRANT ALL PRIVILEGES ON gateway.* TO 'gateway'@'%';
GRANT ALL PRIVILEGES ON orchestrator.* TO 'orchestrator'@'%';

FLUSH PRIVILEGES;
