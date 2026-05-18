#!/bin/bash

set -e

echo "Starting TraceNet local CI build..."

SERVICES=(
  "api-gateway"
  "auth-service"
  "trace-ingestion-service"
  "trace-query-service"
  "analytics-service"
  "alert-service"
  "demo-order-service"
  "demo-payment-service"
  "demo-inventory-service"
)

for SERVICE in "${SERVICES[@]}"
do
  echo "Building $SERVICE..."
  cd "services/$SERVICE"
  mvn clean package -DskipTests
  cd ../..
done

echo "Building Docker images..."
docker compose build

echo "TraceNet local CI build completed successfully."