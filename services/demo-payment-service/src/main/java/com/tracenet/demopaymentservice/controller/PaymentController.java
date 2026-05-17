package com.tracenet.demopaymentservice.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public Map<String, Object> makePayment(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("service", "demo-payment-service");
            response.put("status", "PAYMENT_SUCCESS");
            response.put("traceId", traceId);

            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(traceId, "/payments", "POST", 200, latencyMs, null);

            return response;
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(traceId, "/payments", "POST", 500, latencyMs, e.getMessage());

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
        span.put("serviceName", "demo-payment-service");
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