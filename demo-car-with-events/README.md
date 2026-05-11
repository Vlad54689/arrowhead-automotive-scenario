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

## General Scenario Description

The *Automotive Quality Maintenance Traceability Scenario* demonstrates an event-driven distributed architecture implemented using the Eclipse Arrowhead Framework. The scenario models a collaborative ecosystem of automotive microservices that exchange information through publish-subscribe mechanisms coordinated by the Arrowhead core systems. The main objective is to ensure complete traceability of vehicle inspections, maintenance recommendations, and maintenance history updates across the entire automotive service lifecycle.

The architecture integrates four application services: the *Quality Inspection Service*, the *Maintenance Recommendation Service*, the *Traceability Log Service*, and the *Car Service*. These services interact through the Arrowhead Event Handler, while orchestration, authorization, and service discovery are managed by the Arrowhead core systems. The scenario highlights how decoupled microservices can cooperate asynchronously to support auditability, predictive maintenance, operational transparency, and scalable industrial automation.

The workflow begins when a quality inspection event is generated for a vehicle. The Maintenance Recommendation Service consumes the inspection event and automatically produces maintenance recommendations. These recommendations are then consumed by the Car Service to update the maintenance history of the corresponding vehicle. Simultaneously, the Traceability Log Service records all generated events, creating a complete audit trail that can later be queried for compliance, diagnostics, or analytics purposes.

---

<img width="1491" height="1055" alt="image" src="https://github.com/user-attachments/assets/760cbbc1-586c-40d1-8571-9960d9d11dd4" />

# Architecture Diagram Description

The architecture diagram presents a high-level overview of the Automotive Quality Maintenance Traceability ecosystem deployed inside an Arrowhead Local Cloud. The upper section illustrates the Arrowhead core systems, namely the Service Registry, Authorization, Orchestrator, and Event Handler, which collectively provide service discovery, access control, orchestration, and event management functionalities.

The lower section contains the business services participating in the scenario. The *Quality Inspection Service* acts as an event publisher by generating quality inspection events. The *Maintenance Recommendation Service* consumes these events and publishes maintenance recommendations. The *Traceability Log Service* subscribes to all important events to maintain a complete audit history, while the *Car Service* consumes maintenance recommendations to update vehicle maintenance records.

The diagram emphasizes the event-driven communication model, where services interact asynchronously through event publication and subscription rather than through tightly coupled direct communication. This architectural approach increases scalability, modularity, and fault tolerance.

---

<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/f408d8be-62e1-48a7-a049-9b6d026d1afc" />


# Message Sequence Chart (MSC) Description

The Message Sequence Chart illustrates the chronological interaction between the participating services and the Arrowhead infrastructure during the execution of the traceability workflow. Each vertical lifeline represents a service or core system, while horizontal arrows represent requests, event publications, and event deliveries.

The sequence starts when an external actor submits a quality inspection request to the Quality Inspection Service. After processing the inspection, the service publishes a `quality.inspection.created` event through the Arrowhead Event Handler. The Maintenance Recommendation Service receives the event, generates a recommendation, and subsequently publishes a `maintenance.recommendation.created` event.

The Traceability Log Service receives and stores both event types, ensuring complete auditability. The Car Service consumes the maintenance recommendation event and updates the vehicle maintenance history accordingly. The MSC clearly highlights the asynchronous publish-subscribe interactions enabled by the Event Handler and demonstrates the distributed orchestration of automotive microservices.

---

<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/40b45641-af82-46ba-abe6-ac1ee991367a" />


# UML Component Diagram Description

The UML Component Diagram presents the internal software organization of the system by illustrating the main application components, their interfaces, dependencies, and exchanged data structures. Each application service is represented as an independent component containing REST controllers, business logic modules, event consumers, and event publishers.

The diagram shows how the *Quality Inspection Service* exposes inspection APIs and publishes inspection events. The *Maintenance Recommendation Service* contains both consumer and publisher modules, enabling it to process incoming events and generate new ones. The *Traceability Log Service* focuses on persistent event logging, while the *Car Service* updates vehicle maintenance records based on received recommendations.

Additionally, the diagram highlights shared DTOs and event models used across the system, ensuring interoperability between services. The relationships between components emphasize loose coupling, event-driven communication, and modular software design principles consistent with microservice architectures.

---

<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/6ade5357-44e5-4c57-9915-df0135dcd551" />


# UML Deployment Diagram Description

The UML Deployment Diagram illustrates the physical deployment structure of the automotive traceability system. It describes how the Arrowhead core systems and application services are distributed across network nodes and interconnected through secure communication channels.

The upper section represents the Arrowhead core systems deployed as independent infrastructure nodes responsible for orchestration, authorization, service registration, and event distribution. The lower section contains the application services, each deployed as standalone Spring Boot applications packaged as executable JAR artifacts.

The deployment view also illustrates the communication ports used by each service and highlights the use of secure HTTPS/TLS communication within the Arrowhead Local Cloud. Event-based interactions between services are represented separately from standard REST communication, emphasizing the hybrid communication architecture that combines synchronous APIs with asynchronous event propagation.

---

<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/d8a5b806-8648-48a3-9b03-76d6b48ad964" />


# UML Activity Diagram with Swimlanes Description

The UML Activity Diagram with Swimlanes models the operational workflow of the traceability scenario by separating responsibilities among participating services. Each swimlane corresponds to a specific service or actor involved in the process execution.

The workflow begins with the external actor initiating a quality inspection request. The Quality Inspection Service validates and stores the inspection data before publishing a corresponding event. The Maintenance Recommendation Service then receives the event, generates maintenance recommendations, and publishes a new maintenance recommendation event.

Next, the Traceability Log Service records all generated events for audit and compliance purposes. The Car Service consumes the maintenance recommendation event and updates the vehicle maintenance history accordingly. Finally, the traceability logs can be queried by external actors for monitoring and analysis.

This activity diagram clearly illustrates the control flow, event flow, and distribution of responsibilities within the event-driven architecture, making the operational behavior of the system easier to understand.

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

<img width="1491" height="1055" alt="image" src="https://github.com/user-attachments/assets/760cbbc1-586c-40d1-8571-9960d9d11dd4" />


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

<img width="528" height="87" alt="Screenshot_20260508_125954" src="https://github.com/user-attachments/assets/31f9d279-9091-46f1-b184-09b4f0853b69" />

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
