# TraceNet

TraceNet is a backend-focused distributed tracing, API failure diagnosis, and observability platform built using Spring Boot microservices. It helps developers track how a request moves across multiple services, collect service-level latency and error data, identify slow or failing services, generate analytics, and trigger alerts based on unhealthy system behavior.

In a real microservice architecture, one request may pass through many services such as an API Gateway, Auth Service, Order Service, Payment Service, Inventory Service, Query Service, Analytics Service, and Alert Service. When a request fails or becomes slow, developers usually need to manually inspect logs across different services. TraceNet solves this by assigning every request a unique `traceId`, collecting spans from services, storing those spans in PostgreSQL, and exposing secure APIs to query, analyze, and diagnose failures.

---

## Project Status

TraceNet currently implements a strong backend MVP with authentication, authorization, tracing, persistence, analytics, and alerting.

Implemented features:

```text
Spring Boot microservices
API Gateway routing
JWT authentication
Permission-based RBAC
Organization-level tenant isolation
Trace ID propagation
Distributed span ingestion
PostgreSQL persistence
Trace query APIs
Failure diagnosis summaries
Latency and failure simulation
Analytics APIs
Alert evaluation and acknowledgement
Gateway-secured platform APIs
External demo services for trace generation
```

---

## High-Level Architecture

```text
                            ┌────────────────────┐
                            │      Client        │
                            └─────────┬──────────┘
                                      │
                                      ▼
                            ┌────────────────────┐
                            │    API Gateway     │
                            │      :8080         │
                            └─────────┬──────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌─────────────────┐         ┌────────────────────┐        ┌────────────────────┐
│  Auth Service   │         │ Trace Ingestion    │        │ Trace Query Service │
│     :8081       │         │ Service :8085      │        │       :8086         │
└─────────────────┘         └─────────┬──────────┘        └─────────┬──────────┘
                                      │                             │
                                      ▼                             ▼
                              ┌────────────────────────────────────────┐
                              │              PostgreSQL                │
                              │      tracenet_auth_db                  │
                              │      tracenet_trace_db                 │
                              └────────────────────────────────────────┘
                                      ▲                             ▲
                                      │                             │
        ┌─────────────────────────────┘                             └─────────────────────────────┐
        │                                                                                         │
        ▼                                                                                         ▼
┌────────────────────┐                                                                ┌────────────────────┐
│ Analytics Service  │                                                                │  Alert Service     │
│      :8087         │                                                                │      :8088         │
└────────────────────┘                                                                └────────────────────┘
```

Demo services run separately as an external monitored system:

```text
Client → demo-order-service :8082
       → demo-payment-service :8083
       → demo-inventory-service :8084
       → trace-ingestion-service :8085
```

The demo services are not the TraceNet product itself. They simulate an external microservice application that sends trace spans into TraceNet.

---

## Services

### 1. API Gateway

The API Gateway is the main product entry point.

Port:

```text
8080
```

Responsibilities:

```text
Routes platform APIs
Validates JWT tokens
Enforces permission-based RBAC
Extracts user/org/role/permissions from JWT
Propagates identity context to downstream services
Protects TraceNet APIs from unauthorized access
```

Gateway routes:

```text
/api/auth/**        → auth-service
/api/traces/**      → trace-ingestion-service
/api/query/**       → trace-query-service
/api/analytics/**   → analytics-service
/api/alerts/**      → alert-service
```

---

### 2. Auth Service

Port:

```text
8081
```

Responsibilities:

```text
User registration
User login
Password hashing using BCrypt
JWT generation
Role and permission management
RBAC seeding
```

Auth service maintains:

```text
user_credentials
roles
permissions
role_permissions
```

JWT contains:

```text
userId
email
orgId
role
permissions
iat
exp
```

---

### 3. Trace Ingestion Service

Port:

```text
8085
```

Responsibilities:

```text
Accept trace spans
Store spans in PostgreSQL
Attach organization context to spans
Act as the write-side service for tracing data
```

Main endpoint:

```http
POST /traces/spans
```

Through API Gateway:

```http
POST /api/traces/spans
```

---

### 4. Trace Query Service

Port:

```text
8086
```

Responsibilities:

```text
Read trace spans from PostgreSQL
Query traces by traceId
Query failed traces
Query slow traces
Query traces by service
Generate trace-level diagnostic summaries
Enforce organization-scoped reads
```

Main APIs:

```http
GET /query/traces/{traceId}
GET /query/traces/{traceId}/summary
GET /query/traces/errors
GET /query/traces/slow?threshold=1000
GET /query/traces/service/{serviceName}
```

Through API Gateway:

```http
GET /api/query/traces/{traceId}
GET /api/query/traces/{traceId}/summary
GET /api/query/traces/errors
GET /api/query/traces/slow?threshold=1000
GET /api/query/traces/service/{serviceName}
```

---

### 5. Analytics Service

Port:

```text
8087
```

Responsibilities:

```text
Generate organization-scoped observability analytics
Calculate total spans, failed spans, and error rate
Calculate average and max latency per service
Calculate error count per service
Calculate p95 latency per service
Detect slow traces
```

Main APIs:

```http
GET /analytics/summary
GET /analytics/services/latency
GET /analytics/services/errors
GET /analytics/services/p95
GET /analytics/slow-traces?threshold=1000
```

Through API Gateway:

```http
GET /api/analytics/summary
GET /api/analytics/services/latency
GET /api/analytics/services/errors
GET /api/analytics/services/p95
GET /api/analytics/slow-traces?threshold=1000
```

---

### 6. Alert Service

Port:

```text
8088
```

Responsibilities:

```text
Evaluate trace data against alert thresholds
Generate alerts for unhealthy system behavior
Persist alerts in PostgreSQL
List alerts
Filter alerts by status
Acknowledge alerts
Protect alert APIs using permissions
```

Alert types currently supported:

```text
ERROR_RATE_THRESHOLD
SERVICE_P95_LATENCY_THRESHOLD
SLOW_TRACE_DETECTED
SERVICE_FAILURES_DETECTED
```

Main APIs:

```http
POST /alerts/evaluate
GET /alerts
GET /alerts?status=OPEN
PUT /alerts/{id}/acknowledge
```

Through API Gateway:

```http
POST /api/alerts/evaluate
GET /api/alerts
GET /api/alerts?status=OPEN
PUT /api/alerts/{id}/acknowledge
```

---

### 7. Demo Services

The demo services simulate an external distributed application.

They are used to generate realistic trace data.

Services:

```text
demo-order-service       :8082
demo-payment-service     :8083
demo-inventory-service   :8084
```

Flow:

```text
POST /orders
  ↓
demo-order-service
  ↓
demo-payment-service
  ↓
demo-inventory-service
  ↓
trace-ingestion-service
```

The payment service supports failure and latency simulation:

```http
POST /orders?paymentDelayMs=1500
POST /orders?paymentFail=true
```

---

## Tech Stack

```text
Java 21
Spring Boot
Spring Web MVC
Spring Security
Spring Cloud Gateway MVC
Spring Data JPA
PostgreSQL
JWT using JJWT
BCrypt password hashing
Maven
REST APIs
Docker
Docker Compose
Jenkins
GitHub
```

---

## Current Folder Structure

```text
TraceNet/
  services/
    api-gateway/
    auth-service/
    trace-ingestion-service/
    trace-query-service/
    analytics-service/
    alert-service/
    demo-order-service/
    demo-payment-service/
    demo-inventory-service/
  docker-compose.yml
  Jenkinsfile
  README.md
  .gitignore
```

---

## Databases

TraceNet currently uses PostgreSQL.

### Auth database

```text
tracenet_auth_db
```

Tables:

```text
user_credentials
roles
permissions
role_permissions
```

### Trace database

```text
tracenet_trace_db
```

Tables:

```text
trace_spans
alerts
```

---

## RBAC Model

TraceNet uses database-backed, permission-based RBAC.

Roles:

```text
ADMIN
SRE
DEVELOPER
VIEWER
```

Permissions:

```text
VIEW_TRACES
VIEW_ERRORS
VIEW_SLOW_TRACES
INGEST_TRACES
MANAGE_USERS
MANAGE_ALERTS
VIEW_AUDIT_LOGS
```

Current permission mapping:

```text
ADMIN
  - VIEW_TRACES
  - VIEW_ERRORS
  - VIEW_SLOW_TRACES
  - INGEST_TRACES
  - MANAGE_USERS
  - MANAGE_ALERTS
  - VIEW_AUDIT_LOGS

SRE
  - VIEW_TRACES
  - VIEW_ERRORS
  - VIEW_SLOW_TRACES
  - INGEST_TRACES
  - MANAGE_ALERTS

DEVELOPER
  - VIEW_TRACES
  - VIEW_ERRORS
  - VIEW_SLOW_TRACES

VIEWER
  - VIEW_TRACES
```

Gateway permission enforcement:

```text
/api/query/**       requires VIEW_TRACES
/api/analytics/**   requires VIEW_TRACES
/api/traces/**      requires INGEST_TRACES
/api/alerts/**      requires MANAGE_ALERTS
/api/auth/**        public
```

---

## Organization-Level Tenant Isolation

TraceNet supports organization-scoped trace access.

Each JWT includes:

```text
orgId
```

The API Gateway extracts this value and forwards it to downstream services using:

```text
X-Org-Id
```

Trace spans and alerts are stored with `orgId`.

Query, analytics, and alert APIs only return data for the authenticated user's organization.

This prevents one organization from accessing another organization's traces or alerts.

---

## Trace Span Model

A trace span represents one service operation inside a distributed request.

Example span fields:

```text
id
traceId
orgId
serviceName
endpoint
httpMethod
statusCode
latencyMs
errorMessage
createdAt
```

Example:

```json
{
  "traceId": "4f2e4659-bbf4-44e9-9618-f79ad9fc53af",
  "orgId": "org-demo",
  "serviceName": "demo-payment-service",
  "endpoint": "/payments",
  "httpMethod": "POST",
  "statusCode": 200,
  "latencyMs": 1506,
  "errorMessage": null
}
```

---

## Running the Project Locally

Before Docker Compose, each service can be run manually in separate terminals.

### Start PostgreSQL

```bash
brew services start postgresql@14
```

Create databases if needed:

```bash
psql postgres
```

```sql
CREATE DATABASE tracenet_auth_db;
CREATE DATABASE tracenet_trace_db;
\q
```

---

### Start Auth Service

```bash
cd services/auth-service
mvn spring-boot:run
```

Runs on:

```text
8081
```

---

### Start API Gateway

```bash
cd services/api-gateway
mvn spring-boot:run
```

Runs on:

```text
8080
```

---

### Start Trace Ingestion Service

```bash
cd services/trace-ingestion-service
mvn spring-boot:run
```

Runs on:

```text
8085
```

---

### Start Trace Query Service

```bash
cd services/trace-query-service
mvn spring-boot:run
```

Runs on:

```text
8086
```

---

### Start Analytics Service

```bash
cd services/analytics-service
mvn spring-boot:run
```

Runs on:

```text
8087
```

---

### Start Alert Service

```bash
cd services/alert-service
mvn spring-boot:run
```

Runs on:

```text
8088
```

---

### Start Demo Services

```bash
cd services/demo-payment-service
mvn spring-boot:run
```

```bash
cd services/demo-inventory-service
mvn spring-boot:run
```

```bash
cd services/demo-order-service
mvn spring-boot:run
```

Ports:

```text
demo-order-service       8082
demo-payment-service     8083
demo-inventory-service   8084
```

---

## API Usage

### Register a user

```bash
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
  "email": "sre@tracenet.com",
  "password": "password123",
  "orgId": "org-demo",
  "role": "SRE"
}'
```

---

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
  "email": "sre@tracenet.com",
  "password": "password123"
}'
```

---

### Store token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
  "email": "sre@tracenet.com",
  "password": "password123"
}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
```

Check token:

```bash
echo $TOKEN
```

---

## Generate Trace Data

### Normal request

```bash
curl -X POST http://localhost:8082/orders
```

Expected result:

```json
{
  "service": "demo-order-service",
  "status": "ORDER_CREATED",
  "traceId": "some-trace-id",
  "payment": {
    "service": "demo-payment-service",
    "status": "PAYMENT_SUCCESS"
  },
  "inventory": {
    "service": "demo-inventory-service",
    "status": "INVENTORY_RESERVED"
  }
}
```

---

### Slow payment request

```bash
curl -X POST "http://localhost:8082/orders?paymentDelayMs=1500"
```

This creates a trace where payment latency is high.

---

### Failed payment request

```bash
curl -X POST "http://localhost:8082/orders?paymentFail=true"
```

This creates a failed trace where payment returns a simulated error.

---

## Query Traces

### Get trace by ID

```bash
curl http://localhost:8080/api/query/traces/YOUR_TRACE_ID \
-H "Authorization: Bearer $TOKEN"
```

---

### Get trace summary

```bash
curl http://localhost:8080/api/query/traces/YOUR_TRACE_ID/summary \
-H "Authorization: Bearer $TOKEN"
```

Example response:

```json
{
  "traceId": "YOUR_TRACE_ID",
  "orgId": "org-demo",
  "status": "FAILED",
  "totalSpans": 2,
  "successCount": 0,
  "failureCount": 2,
  "rootCauseService": "demo-payment-service",
  "rootCauseError": "Simulated payment failure",
  "slowestService": "demo-order-service",
  "maxLatencyMs": 56,
  "diagnosis": "Request failed because demo-payment-service returned an error: Simulated payment failure"
}
```

---

### Get failed spans

```bash
curl http://localhost:8080/api/query/traces/errors \
-H "Authorization: Bearer $TOKEN"
```

---

### Get slow spans

```bash
curl "http://localhost:8080/api/query/traces/slow?threshold=1000" \
-H "Authorization: Bearer $TOKEN"
```

---

### Get traces by service

```bash
curl http://localhost:8080/api/query/traces/service/demo-payment-service \
-H "Authorization: Bearer $TOKEN"
```

---

## Analytics APIs

### Summary

```bash
curl http://localhost:8080/api/analytics/summary \
-H "Authorization: Bearer $TOKEN"
```

Example:

```json
{
  "orgId": "org-demo",
  "totalSpans": 14,
  "successSpans": 12,
  "failedSpans": 2,
  "errorRatePercent": 14.29
}
```

---

### Service latency

```bash
curl http://localhost:8080/api/analytics/services/latency \
-H "Authorization: Bearer $TOKEN"
```

---

### Service errors

```bash
curl http://localhost:8080/api/analytics/services/errors \
-H "Authorization: Bearer $TOKEN"
```

---

### Service p95 latency

```bash
curl http://localhost:8080/api/analytics/services/p95 \
-H "Authorization: Bearer $TOKEN"
```

---

### Slow traces

```bash
curl "http://localhost:8080/api/analytics/slow-traces?threshold=1000" \
-H "Authorization: Bearer $TOKEN"
```

---

## Alert APIs

### Evaluate alerts

```bash
curl -X POST http://localhost:8080/api/alerts/evaluate \
-H "Authorization: Bearer $TOKEN"
```

Example alert types:

```text
ERROR_RATE_THRESHOLD
SERVICE_P95_LATENCY_THRESHOLD
SLOW_TRACE_DETECTED
SERVICE_FAILURES_DETECTED
```

---

### List alerts

```bash
curl http://localhost:8080/api/alerts \
-H "Authorization: Bearer $TOKEN"
```

---

### List open alerts

```bash
curl "http://localhost:8080/api/alerts?status=OPEN" \
-H "Authorization: Bearer $TOKEN"
```

---

### Acknowledge alert

```bash
curl -X PUT http://localhost:8080/api/alerts/ALERT_ID/acknowledge \
-H "Authorization: Bearer $TOKEN"
```

Replace `ALERT_ID` with an actual alert ID.

---

## Security Testing

### Request without token

```bash
curl http://localhost:8080/api/query/traces/errors
```

Expected:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid Authorization header"
}
```

---

### Viewer blocked from alerts

A `VIEWER` user can query traces but cannot manage alerts.

```bash
curl http://localhost:8080/api/alerts \
-H "Authorization: Bearer $VIEWER_TOKEN"
```

Expected:

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "User does not have required permission for this resource"
}
```

---




# TraceNet Docker and Jenkins Work Summary

## Docker Compose Setup

TraceNet was containerized using Docker and Docker Compose so that the complete platform can be started from the project root using one command instead of manually running every Spring Boot service in separate terminals.

The Docker setup includes the following containers:

```text
postgres
api-gateway
auth-service
trace-ingestion-service
trace-query-service
analytics-service
alert-service
demo-order-service
demo-payment-service
demo-inventory-service
```

The Dockerized architecture allows the TraceNet product services and demo services to run together on a shared Docker network. Service-to-service communication no longer depends on `localhost`; instead, each service talks to other services using Docker service names such as `postgres`, `auth-service`, `trace-ingestion-service`, `trace-query-service`, `analytics-service`, and `alert-service`.

---

## Dockerfiles

A Dockerfile was added to every Spring Boot service folder.

Services with Dockerfiles:

```text
services/api-gateway/Dockerfile
services/auth-service/Dockerfile
services/trace-ingestion-service/Dockerfile
services/trace-query-service/Dockerfile
services/analytics-service/Dockerfile
services/alert-service/Dockerfile
services/demo-order-service/Dockerfile
services/demo-payment-service/Dockerfile
services/demo-inventory-service/Dockerfile
```

Each Dockerfile follows a multi-stage build pattern:

```text
Stage 1: Maven build using Java 21
Stage 2: Lightweight Java runtime image
```

The Maven stage builds the Spring Boot JAR using:

```bash
mvn clean package -DskipTests
```

The runtime stage copies the generated JAR and runs it using:

```bash
java -jar app.jar
```

This keeps the runtime container cleaner than running the full Maven build environment in production mode.

---

## Docker Compose File

A root-level `docker-compose.yml` file was created to orchestrate the complete TraceNet platform.

The Compose file defines:

```text
PostgreSQL database container
Auth service container
API Gateway container
Trace ingestion service container
Trace query service container
Analytics service container
Alert service container
Demo order service container
Demo payment service container
Demo inventory service container
Shared PostgreSQL volume
Database initialization script
Service dependencies
Container networking
Environment variables
Port mappings
```

The main command to build and start everything is:

```bash
docker compose up --build
```

For background mode:

```bash
docker compose up --build -d
```

To stop the containers but keep database data:

```bash
docker compose down
```

To stop containers and remove the PostgreSQL volume:

```bash
docker compose down -v
```

---

## PostgreSQL in Docker

A PostgreSQL container was added using the official `postgres:16` image.

The container uses:

```text
POSTGRES_USER=tracenet
POSTGRES_PASSWORD=tracenet
POSTGRES_DB=postgres
```

A database initialization script was created at:

```text
docker/postgres/init.sql
```

The script creates the required TraceNet databases:

```sql
CREATE DATABASE tracenet_auth_db;
CREATE DATABASE tracenet_trace_db;
```

The PostgreSQL data is persisted using a Docker volume:

```text
tracenet_postgres_data
```

This ensures data survives normal container restarts. When a clean reset is needed, the volume can be removed using:

```bash
docker compose down -v
```

---

## Docker-Friendly Application Properties

The Spring Boot `application.properties` files were updated so services can work both locally and inside Docker.

Instead of hardcoding database URLs such as:

```text
jdbc:postgresql://localhost:5432/...
```

services now use environment-variable based configuration:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/tracenet_trace_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:ivanbhargava}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
```

This means:

```text
When running locally, the service uses localhost defaults.
When running in Docker, Docker Compose injects the correct container URLs.
```

For example, inside Docker:

```text
jdbc:postgresql://postgres:5432/tracenet_trace_db
```

is used instead of:

```text
jdbc:postgresql://localhost:5432/tracenet_trace_db
```

---

## Docker Service Networking

The API Gateway was updated to use environment-based service URLs.

Example:

```properties
AUTH_SERVICE_URL=http://auth-service:8081
TRACE_INGESTION_SERVICE_URL=http://trace-ingestion-service:8085
TRACE_QUERY_SERVICE_URL=http://trace-query-service:8086
ANALYTICS_SERVICE_URL=http://analytics-service:8087
ALERT_SERVICE_URL=http://alert-service:8088
```

This allows the gateway container to route traffic to other containers using Docker service names.

The demo services were also updated to use environment-based URLs:

```properties
PAYMENT_SERVICE_URL=http://demo-payment-service:8083
INVENTORY_SERVICE_URL=http://demo-inventory-service:8084
TRACE_INGESTION_URL=http://trace-ingestion-service:8085
```

This fixed the common Docker issue where `localhost` inside a container refers to the same container, not the host machine or another service.

---

## Demo Services Docker Update

The demo services were made Docker-compatible.

### demo-order-service

The order service now reads downstream service URLs from configuration:

```properties
payment.service.url=${PAYMENT_SERVICE_URL:http://localhost:8083}
inventory.service.url=${INVENTORY_SERVICE_URL:http://localhost:8084}
trace.ingestion.url=${TRACE_INGESTION_URL:http://localhost:8085}
```

This allows it to call:

```text
demo-payment-service
demo-inventory-service
trace-ingestion-service
```

inside Docker.

### demo-payment-service

The payment service now reads the trace ingestion URL from:

```properties
trace.ingestion.url=${TRACE_INGESTION_URL:http://localhost:8085}
```

### demo-inventory-service

The inventory service also reads the trace ingestion URL from:

```properties
trace.ingestion.url=${TRACE_INGESTION_URL:http://localhost:8085}
```

The demo services continue to simulate an external monitored system. They generate trace spans and send them to the TraceNet ingestion service.

---

## Docker Testing Completed

The Dockerized stack was tested end-to-end.

The following flow worked successfully:

```text
Register SRE user through API Gateway
Login through API Gateway
Generate normal order trace
Generate slow payment trace
Generate failed payment trace
Query analytics through API Gateway
Evaluate alerts through API Gateway
```

Example successful auth flow:

```bash
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
  "email": "sre@tracenet.com",
  "password": "password123",
  "orgId": "org-demo",
  "role": "SRE"
}'
```

The response included a JWT token with the expected role and permissions:

```text
role=SRE
permissions=INGEST_TRACES, MANAGE_ALERTS, VIEW_ERRORS, VIEW_SLOW_TRACES, VIEW_TRACES
```

Example trace generation:

```bash
curl -X POST http://localhost:8082/orders
curl -X POST "http://localhost:8082/orders?paymentDelayMs=1500"
curl -X POST "http://localhost:8082/orders?paymentFail=true"
```

Example analytics check:

```bash
curl http://localhost:8080/api/analytics/summary \
-H "Authorization: Bearer $TOKEN"
```

The analytics service returned org-scoped metrics such as:

```text
totalSpans
successSpans
failedSpans
errorRatePercent
```

Example alert evaluation:

```bash
curl -X POST http://localhost:8080/api/alerts/evaluate \
-H "Authorization: Bearer $TOKEN"
```

The alert service generated alerts for:

```text
Error rate threshold
Service p95 latency threshold
Slow trace detection
Service failure detection
```

This confirms that the full Docker Compose environment works across authentication, tracing, analytics, and alerting.

---

## Useful Docker Commands

Start full platform with live logs:

```bash
docker compose up --build
```

Start full platform in background:

```bash
docker compose up --build -d
```

Check running containers:

```bash
docker compose ps
```

View logs for all services:

```bash
docker compose logs -f
```

View logs for one service:

```bash
docker compose logs -f api-gateway
```

Stop all containers:

```bash
docker compose down
```

Stop all containers and remove database volume:

```bash
docker compose down -v
```

Rebuild images:

```bash
docker compose build
```

Restart one service:

```bash
docker compose restart api-gateway
```

---

# Jenkins CI/CD Setup

A root-level `Jenkinsfile` was added to define the CI/CD pipeline for TraceNet.

The Jenkins pipeline is designed to validate the complete backend system automatically.

The pipeline performs:

```text
Checkout source code
Verify project folder structure
Build all Maven services
Build Docker images using Docker Compose
Start the TraceNet stack
Wait for services to initialize
Check API Gateway health
Run auth smoke test
Run trace generation smoke test
Run analytics smoke test
Collect Docker container status
Show logs on failure
```

---

## Jenkins Pipeline Stages

### 1. Checkout

Jenkins checks out the GitHub repository.

```text
checkout scm
```

### 2. Verify Project Structure

The pipeline validates that all required service folders and `docker-compose.yml` exist.

Checked folders:

```text
services/api-gateway
services/auth-service
services/trace-ingestion-service
services/trace-query-service
services/analytics-service
services/alert-service
services/demo-order-service
services/demo-payment-service
services/demo-inventory-service
```

### 3. Build Maven Services

Each Spring Boot service is built using Maven.

The build command used for each service is:

```bash
mvn clean package -DskipTests
```

The Jenkinsfile builds services in parallel to reduce pipeline time.

Services built:

```text
api-gateway
auth-service
trace-ingestion-service
trace-query-service
analytics-service
alert-service
demo-order-service
demo-payment-service
demo-inventory-service
```

### 4. Docker Compose Build

Jenkins builds all Docker images using:

```bash
docker compose build
```

### 5. Start Stack

Jenkins starts the full TraceNet stack using:

```bash
docker compose up -d
```

### 6. Wait for Services

The pipeline waits for services to initialize:

```bash
sleep 45
```

### 7. Gateway Health Check

The pipeline checks if API Gateway is healthy:

```bash
curl --fail --silent http://localhost:8080/actuator/health
```

### 8. Auth Smoke Test

The pipeline registers or logs in a test SRE user through the API Gateway.

It verifies that a JWT token is generated successfully.

### 9. Trace Flow Smoke Test

The pipeline generates a demo trace by calling:

```bash
curl --fail --silent -X POST http://localhost:8082/orders
```

Then it checks analytics through the gateway:

```bash
curl --fail --silent http://localhost:8080/api/analytics/summary \
-H "Authorization: Bearer $TOKEN"
```

This confirms that Docker, routing, authentication, trace generation, and analytics are working inside the CI/CD environment.

---

## Local CI Script

A local build script was added at:

```text
scripts/ci-build.sh
```

This script is useful for checking the project locally before pushing to GitHub.

It performs:

```text
Build all Maven services
Build Docker images using Docker Compose
```

It does not start the full stack by default.

Run it using:

```bash
./scripts/ci-build.sh
```

The local script and Jenkinsfile are different.

```text
scripts/ci-build.sh
  - Runs manually on local machine
  - Builds services
  - Builds Docker images
  - Quick local validation before pushing

Jenkinsfile
  - Runs inside Jenkins
  - Triggered by Jenkins pipeline
  - Checks out GitHub code
  - Builds services
  - Builds Docker images
  - Starts stack
  - Runs health checks
  - Runs smoke tests
```

The local script does not call the Jenkinsfile. Jenkins reads the Jenkinsfile directly from the GitHub repository.

---

## Jenkins Local Setup Notes

Jenkins can be run locally using Docker:

```bash
docker run -d \
  --name jenkins-tracenet \
  -p 8089:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts
```

Jenkins UI can then be opened at:

```text
http://localhost:8089
```

The initial admin password can be retrieved using:

```bash
docker exec jenkins-tracenet cat /var/jenkins_home/secrets/initialAdminPassword
```

To run the TraceNet pipeline in Jenkins:

```text
Create a new Pipeline job
Choose Pipeline script from SCM
Select Git
Paste the GitHub repository URL
Set branch to main
Set script path to Jenkinsfile
Run Build Now
```

Important note:

```text
For the Jenkinsfile to run Docker commands, the Jenkins agent must have Docker and Docker Compose available.
```

If Jenkins is running inside Docker, Docker socket access or a properly configured Docker-enabled Jenkins agent may be needed.

---

## Final DevOps Status

The TraceNet DevOps setup now includes:

```text
Dockerfiles for all services
Root Docker Compose orchestration
Dockerized PostgreSQL
Database initialization script
Environment-variable based service configuration
Docker networking between services
End-to-end Docker testing
Jenkins CI/CD pipeline definition
Local CI build script
Gateway health checks
Auth smoke test
Trace generation smoke test
Analytics smoke test
```

This makes TraceNet a deployable, CI/CD-ready microservices project instead of only a locally-run Spring Boot application.

---

## Resume Description for Docker and Jenkins Work

```text
Containerized all TraceNet microservices using Docker and orchestrated the full platform with Docker Compose, including PostgreSQL, API Gateway, auth, trace ingestion, query, analytics, alerting, and demo services.

Implemented Docker-friendly environment configuration for service discovery, database connectivity, and inter-service communication across containers.

Created a Jenkins CI/CD pipeline to build all Maven services, build Docker images, start the Compose stack, and run health and smoke tests for authentication, trace generation, and analytics.

Added a local CI build script to validate all services and Docker images before pushing changes to GitHub.
```
