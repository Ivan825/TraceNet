# TraceNet

TraceNet is a backend-focused distributed tracing and API failure diagnosis platform built using Spring Boot microservices. It helps developers track how a request moves across multiple services, collect service-level latency and error information, and identify which service caused a failure or slowdown.

In a real microservice system, one request may pass through several services such as an API Gateway, Order Service, Payment Service, Inventory Service, and Notification Service. When the request fails, developers often need to manually check logs across many services. TraceNet solves this by assigning every request a unique `traceId`, propagating it across services, collecting trace spans, and exposing APIs to view the full request journey.

## Current Status

The project currently implements the first working backend milestone:

- Multiple Spring Boot services running independently
- Demo order, payment, and inventory services
- Request flow from Order Service to Payment Service and Inventory Service
- Shared `traceId` propagation across services
- Central trace ingestion service
- In-memory trace span storage
- API to query all spans for a specific trace ID

The current working flow is:

```text
Client
  ↓
demo-order-service
  ↓
demo-payment-service
  ↓
demo-inventory-service
  ↓
trace-ingestion-service