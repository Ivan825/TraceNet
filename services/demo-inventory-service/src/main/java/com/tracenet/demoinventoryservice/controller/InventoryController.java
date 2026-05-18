package com.tracenet.demoinventoryservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${trace.ingestion.url}")
    private String traceIngestionUrl;

    @PostMapping("/reserve")
    public Map<String, Object> reserveInventory(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("service", "demo-inventory-service");
            response.put("status", "INVENTORY_RESERVED");
            response.put("traceId", traceId);

            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(traceId, "/inventory/reserve", "POST", 200, latencyMs, null);

            return response;
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(traceId, "/inventory/reserve", "POST", 500, latencyMs, e.getMessage());

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
        span.put("orgId", "org-demo");
        span.put("serviceName", "demo-inventory-service");
        span.put("endpoint", endpoint);
        span.put("httpMethod", httpMethod);
        span.put("statusCode", statusCode);
        span.put("latencyMs", latencyMs);
        span.put("errorMessage", errorMessage);

        try {
            restTemplate.postForObject(
                    traceIngestionUrl + "/traces/spans",
                    span,
                    Map.class
            );
        } catch (Exception ignored) {
            System.out.println("Trace ingestion service unavailable");
        }
    }
}