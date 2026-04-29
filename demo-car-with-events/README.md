# Car Demo With Events (Java Spring-Boot)
##### The project provides Arrowhead Application demo implementation developed from [application-skeleton project](https://github.com/arrowhead-f/client-skeleton-java-spring)

## Overview
The goal of the original Car Demo project is to simply demonstrate how a consumer could orchestrate for service and consume it afterward.

The goal of the Events addition is to simply demonstrate how would a consumer subscribe to events and receive events, and how would a producer publish events.


##### The Local Cloud Architecture 
🟦 `AH Service Registry`
🟥 `AH Authorization` 
🟩 `AH Orchestrator`
🟨 `AH Event Handler`
![Alt text](https://github.com/arrowhead-f/sos-examples-spring/blob/master/demo-car-with-events/doc/overview.png)

## Service Descriptions
**create-car:**

Creates a new car instance.
* ***input:*** CarRequestDTO.json
```
{
   "brand":"string",
   "color":"string"
}
```
* ***output:*** CarResponseDTO.json
```
{
   "id":"integer",
   "brand":"string",
   "color":"string"
}
```

**get-car:**

Returns a car list based on the given parameters.
* ***input:*** Query parameters: 

  `brand`={brand} [*not mandatory*]
  
  `color`={color} [*not mandatory*]

* ***output:*** List of CarResponseDTO.json
```
[{
   "id":"integer",
   "brand":"string",
   "color":"string"
}]
```

## How to run?
1. Clone this repo to your local machine.

2. Go to the root directory and execute `mvn install` command, then wait until the build succeeds.

3. Start the [Arrowhead Framework](https://github.com/eclipse-arrowhead/core-java-spring), before you would start the demo. Required core systems:
   * Service Registry
   * Authorization
   * Orchestration
   * Event Handler

4. ( Optional ) Set service_limit property at the provider's application.properties.
   * The provider will terminate after it served the number of requests given in service_limit property.
   
5. Start the provider (it will do the registration automatically to the Service Registry Core System).

6. ( Optional ) Set max_retry property at the consumer's application.properties.
   * The consumer will terminate after it performed the number of consecutive unsuccessful orchestration, given in max_retry.
   
7. ( Optional ) Set reorchestration property at the consumer's application.properties.
   * The consumer will terminate after it received a `PUBLISHER_DESTROYED` event if reorchestration is set to false.
   
8. For the very first time, register the consumer manually and create the `intracloud`  authorization rules.

9. Start the Consumer.

## Configuration
  - Find the `application.properties` confirguration file under the `<project>/src/main/resources` folder before the build or under the `<project>/target` after the build.
  - Default configuration is provided out of the box which works when the Arrowhead Local Cloud is running on your localhost and has the common [testclou2 certificates](https://github.com/eclipse-arrowhead/core-java-spring/tree/master/certificates/testcloud2). 

---

## Automotive Quality Maintenance Traceability Scenario

### Overview

This scenario implements a complete **automotive quality maintenance traceability system** using Arrowhead's event-driven architecture. It demonstrates how multiple microservices can collaborate through publish-subscribe patterns to maintain a complete audit trail of quality inspections and maintenance recommendations.

### Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Arrowhead Local Cloud                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────────────┐  │
│  │   Registry  │  │ Authorization│  │ Orchestrator│  │   Event Handler   │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
         │                │                  │                    │
         │                │                  │                    │
         ▼                ▼                  ▼                    ▼
┌──────────────┐  ┌────────────────┐  ┌──────────────┐  ┌────────────────┐
│Quality       │  │Maintenance     │  │Traceability  │  │Car             │
│Inspection    │  │Recommendation  │  │Log           │  │Service         │
│Service       │  │Service         │  │Service       │  │Service         │
│(Publisher)   │  │(Consumer+      │  │(Consumer)    │  │(Consumer)      │
│             │  │ Publisher)     │  │              │  │                │
└──────┬───────┘  └───────┬────────┘  └──────┬───────┘  └───────┬────────┘
       │                 │                   │                  │
       │ publishes       │                   │                  │
       │ quality         │ consumes          │                  │
       │ inspection      │──────────────────▶│                  │
       │ events          │                   │                  │
       │                 │                   │                  │
       │                 │ publishes         │                  │
       │                 │ maintenance       │                  │
       │                 │ recommendation    │                  │
       │                 │ events            │                  │
       │                 │──────────────────▶│                  │
       │                 │                   │                  │
       │                 │                   │ consumes         │
       │                 │                   │ traceability     │
       │                 │                   │ logs             │
       │                 │                   │─────────────────▶│
       │                 │                   │                  │
       │                 │                   │                  │ consumes
       │                 │                   │                  │ maintenance
       │                 │                   │                  │ recommendations
       │                 │                   │                  │────────┐
       └────────────────▶│                   │                          │
                         │                   │                          │
                         └───────────────────┴──────────────────────────┘
```

### Services

#### 1. Quality Inspection Service
- **Role:** Event Publisher
- **Function:** Publishes quality inspection events to the Arrowhead event system
- **REST API:** `POST /quality-inspections` - Create a new quality inspection
- **Events Published:** `quality.inspection.created`

#### 2. Maintenance Recommendation Service
- **Role:** Event Consumer & Publisher
- **Function:** Consumes quality inspection events, generates maintenance recommendations, and publishes them
- **REST API:** `POST /maintenance-recommendations` - Create a new maintenance recommendation
- **Events Consumed:** `quality.inspection.created`
- **Events Published:** `maintenance.recommendation.created`

#### 3. Traceability Log Service
- **Role:** Event Consumer
- **Function:** Consumes all events and maintains a complete traceability log for audit purposes
- **REST API:** `GET /traceability-logs` - Retrieve traceability logs
- **Events Consumed:** `quality.inspection.created`, `maintenance.recommendation.created`

#### 4. Car Service
- **Role:** Event Consumer
- **Function:** Consumes maintenance recommendation events to update car maintenance records
- **REST API:** `POST /cars` - Create a new car with maintenance history
- **Events Consumed:** `maintenance.recommendation.created`

### Traceability Chain

The complete traceability chain ensures full auditability:

```
Quality Inspection Event
        │
        ▼
Maintenance Recommendation Event
        │
        ▼
Car Service Update
        │
        └──> All events logged by Traceability Log Service
```

### How to Run

#### Prerequisites
1. Arrowhead Local Cloud with Service Registry, Authorization, Orchestrator, and Event Handler running
2. Authorization configured for all four services

#### Build and Deploy
```bash
# Build all modules
mvn clean install

# Start services in order:
# 1. Start Quality Inspection Service (publisher)
cd demo-quality-inspection-service
java -jar target/demo-quality-inspection-service-1.0.0-SNAPSHOT.jar

# 2. Start Maintenance Recommendation Service
cd ../demo-maintenance-recommendation-service
java -jar target/demo-maintenance-recommendation-service-1.0.0-SNAPSHOT.jar

# 3. Start Traceability Log Service
cd ../demo-traceability-log-service
java -jar target/demo-traceability-log-service-1.0.0-SNAPSHOT.jar

# 4. Start Car Service
cd ../demo-car-service
java -jar target/demo-car-service-1.0.0-SNAPSHOT.jar
```

#### Testing the Scenario

1. **Create a Quality Inspection:**
```bash
curl -X POST http://localhost:8081/quality-inspections \
  -H "Content-Type: application/json" \
  -d '{
    "carId": "CAR-001",
    "inspectionType": "BRAKE_CHECK",
    "result": "PASS",
    "notes": "Brake pads within acceptable limits",
    "inspectorId": "INS-123"
  }'
```

2. **Check Traceability Logs:**
```bash
curl http://localhost:8083/traceability-logs
```

3. **Create a Maintenance Recommendation:**
```bash
curl -X POST http://localhost:8082/maintenance-recommendations \
  -H "Content-Type: application/json" \
  -d '{
    "carId": "CAR-001",
    "recommendationType": "ROUTINE_MAINTENANCE",
    "priority": "MEDIUM",
    "description": "Schedule next service interval",
    "validUntil": "2024-12-31"
  }'
```

4. **Create a Car:**
```bash
curl -X POST http://localhost:8084/cars \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CAR-002",
    "brand": "Toyota",
    "model": "Camry",
    "year": 2024
  }'
```

### Configuration

Configuration files are located at:
- `<service>/src/main/resources/application.properties`

Key configuration properties:
- `arrowhead.address` - Arrowhead framework address
- `arrowhead.port` - Arrowhead framework port
- `server.port` - Service port
- `service.name` - Service name registered with Arrowhead
- `service.version` - Service version

### Test Coverage

The scenario includes comprehensive integration tests:
- `QualityInspectionServiceIntegrationTest` - Tests quality inspection publishing
- `MaintenanceRecommendationServiceIntegrationTest` - Tests recommendation publishing
- `TraceabilityLogServiceIntegrationTest` - Tests log retrieval
- `CarServiceIntegrationTest` - Tests car service functionality
- `AutomotiveScenarioIntegrationTest` - Tests complete traceability chain

Run all tests:
```bash
mvn test
```

### Use Cases

This scenario demonstrates:
- **Event-driven architecture** with Arrowhead Event Handler
- **Publish-subscribe pattern** for decoupled service communication
- **Complete traceability** for automotive quality management
- **Microservices collaboration** through service orchestration
- **REST API integration** alongside event-based communication

### Related Documentation

- [Arrowhead Framework Wiki](https://github.com/eclipse-arrowhead/core-java-spring/wiki)
- [Automotive Quality Maintenance Traceability](#automotive-quality-maintenance-traceability-scenario)
