package com.tracenet.demoorderservice.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public Map<String, Object> createOrder(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        long startTime = System.currentTimeMillis();

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Trace-Id", traceId);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> paymentResponse = restTemplate.exchange(
                    "http://localhost:8083/payments",
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

            return response;
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(traceId, "/orders", "POST", 500, latencyMs, e.getMessage());

            throw e;
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