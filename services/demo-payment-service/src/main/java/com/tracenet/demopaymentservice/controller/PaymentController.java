package com.tracenet.demopaymentservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public ResponseEntity<Map<String, Object>> makePayment(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestParam(value = "fail", defaultValue = "false") boolean fail,
            @RequestParam(value = "delayMs", defaultValue = "0") long delayMs
    ) {
        long startTime = System.currentTimeMillis();

        try {
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }

            if (fail) {
                long latencyMs = System.currentTimeMillis() - startTime;

                sendTraceSpan(
                        traceId,
                        "/payments",
                        "POST",
                        500,
                        latencyMs,
                        "Simulated payment failure"
                );

                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("service", "demo-payment-service");
                errorResponse.put("status", "PAYMENT_FAILED");
                errorResponse.put("traceId", traceId);
                errorResponse.put("errorMessage", "Simulated payment failure");

                return ResponseEntity.status(500).body(errorResponse);
            }

            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(
                    traceId,
                    "/payments",
                    "POST",
                    200,
                    latencyMs,
                    null
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("service", "demo-payment-service");
            response.put("status", "PAYMENT_SUCCESS");
            response.put("traceId", traceId);
            response.put("latencyMs", latencyMs);

            return ResponseEntity.ok(response);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            long latencyMs = System.currentTimeMillis() - startTime;

            sendTraceSpan(
                    traceId,
                    "/payments",
                    "POST",
                    500,
                    latencyMs,
                    "Payment delay interrupted"
            );

            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("service", "demo-payment-service");
            errorResponse.put("status", "PAYMENT_INTERRUPTED");
            errorResponse.put("traceId", traceId);
            errorResponse.put("errorMessage", "Payment delay interrupted");

            return ResponseEntity.status(500).body(errorResponse);
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