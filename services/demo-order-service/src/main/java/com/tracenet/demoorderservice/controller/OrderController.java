package com.tracenet.demoorderservice.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestParam(value = "paymentFail", defaultValue = "false") boolean paymentFail,
            @RequestParam(value = "paymentDelayMs", defaultValue = "0") long paymentDelayMs
    ) {
        long startTime = System.currentTimeMillis();

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Trace-Id", traceId);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            String paymentUrl = "http://localhost:8083/payments"
                    + "?fail=" + paymentFail
                    + "&delayMs=" + paymentDelayMs;

            ResponseEntity<Map> paymentResponse = restTemplate.exchange(
                    paymentUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            ResponseEntity<Map> inventoryResponse = restTemplate.exchange(
                    "http://localhost:8084/inventory/reserve",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(traceId, "/orders", "POST", 200, latencyMs, null);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("service", "demo-order-service");
            response.put("status", "ORDER_CREATED");
            response.put("traceId", traceId);
            response.put("payment", paymentResponse.getBody());
            response.put("inventory", inventoryResponse.getBody());
            response.put("latencyMs", latencyMs);

            return ResponseEntity.ok(response);

        } catch (HttpServerErrorException e) {
            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(
                    traceId,
                    "/orders",
                    "POST",
                    500,
                    latencyMs,
                    "Order failed because downstream service failed: " + e.getResponseBodyAsString()
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("service", "demo-order-service");
            response.put("status", "ORDER_FAILED");
            response.put("traceId", traceId);
            response.put("rootCause", "demo-payment-service");
            response.put("errorMessage", "Payment service failed");
            response.put("downstreamError", e.getResponseBodyAsString());
            response.put("latencyMs", latencyMs);

            return ResponseEntity.status(500).body(response);

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(
                    traceId,
                    "/orders",
                    "POST",
                    500,
                    latencyMs,
                    e.getMessage()
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("service", "demo-order-service");
            response.put("status", "ORDER_FAILED");
            response.put("traceId", traceId);
            response.put("errorMessage", e.getMessage());
            response.put("latencyMs", latencyMs);

            return ResponseEntity.status(500).body(response);
        }
    }

    private void sendTraceSpan(
            String traceId,
            String endpoint,
            String httpMethod,
            Integer statusCode,
            Long latencyMs,
            String errorMessage
    ) {
        Map<String, Object> span = new LinkedHashMap<>();
        span.put("traceId", traceId);
        span.put("serviceName", "demo-order-service");
        span.put("endpoint", endpoint);
        span.put("httpMethod", httpMethod);
        span.put("statusCode", statusCode);
        span.put("latencyMs", latencyMs);
        span.put("errorMessage", errorMessage);

        try {
            restTemplate.postForObject(
                    "http://localhost:8085/traces/spans",
                    span,
                    Map.class
            );
        } catch (Exception ignored) {
            System.out.println("Trace ingestion service unavailable");
        }
    }
}