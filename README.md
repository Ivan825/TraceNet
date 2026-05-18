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




