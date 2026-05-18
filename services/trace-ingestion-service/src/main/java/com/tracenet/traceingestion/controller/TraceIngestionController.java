package com.tracenet.traceingestion.controller;

import com.tracenet.traceingestion.dto.TraceSpanRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/traces")
public class TraceIngestionController {

    private final List<Map<String, Object>> spans = new CopyOnWriteArrayList<>();

    @PostMapping("/spans")
    public Map<String, Object> ingestSpan(@RequestBody TraceSpanRequest request) {
        Map<String, Object> span = new LinkedHashMap<>();

        span.put("id", UUID.randomUUID().toString());
        span.put("traceId", request.getTraceId());
        span.put("serviceName", request.getServiceName());
        span.put("endpoint", request.getEndpoint());
        span.put("httpMethod", request.getHttpMethod());
        span.put("statusCode", request.getStatusCode());
        span.put("latencyMs", request.getLatencyMs());
        span.put("errorMessage", request.getErrorMessage());
        span.put("createdAt", Instant.now().toString());

        spans.add(span);

        return Map.of(
                "message", "Trace span ingested successfully",
                "traceId", request.getTraceId(),
                "serviceName", request.getServiceName()
        );
    }

    @GetMapping("/spans")
    public List<Map<String, Object>> getAllSpans() {
        return spans;
    }

    @GetMapping("/{traceId}")
    public List<Map<String, Object>> getSpansByTraceId(@PathVariable String traceId) {
        return spans.stream()
                .filter(span -> traceId.equals(span.get("traceId")))
                .toList();
    }

    @GetMapping("/{traceId}/summary")
    public Map<String, Object> getTraceSummary(@PathVariable String traceId) {
        List<Map<String, Object>> traceSpans = spans.stream()
                .filter(span -> traceId.equals(span.get("traceId")))
                .toList();

        if (traceSpans.isEmpty()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("traceId", traceId);
            response.put("status", "NOT_FOUND");
            response.put("message", "No spans found for this traceId");
            return response;
        }

        boolean hasFailure = false;
        String rootCauseService = null;
        String rootCauseError = null;

        String slowestService = null;
        String slowestEndpoint = null;
        long maxLatencyMs = -1L;

        long totalLatencyMs = 0L;
        int successCount = 0;
        int failureCount = 0;

        for (Map<String, Object> span : traceSpans) {
            int statusCode = toInt(span.get("statusCode"));
            long latencyMs = toLong(span.get("latencyMs"));

            totalLatencyMs += latencyMs;

            if (latencyMs > maxLatencyMs) {
                maxLatencyMs = latencyMs;
                slowestService = String.valueOf(span.get("serviceName"));
                slowestEndpoint = String.valueOf(span.get("endpoint"));
            }

            if (statusCode >= 500) {
                failureCount++;

                if (!hasFailure) {
                    hasFailure = true;
                    rootCauseService = String.valueOf(span.get("serviceName"));
                    rootCauseError = span.get("errorMessage") == null
                            ? "No error message available"
                            : String.valueOf(span.get("errorMessage"));
                }
            } else {
                successCount++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("traceId", traceId);
        summary.put("status", hasFailure ? "FAILED" : "SUCCESS");
        summary.put("totalSpans", traceSpans.size());
        summary.put("successCount", successCount);
        summary.put("failureCount", failureCount);
        summary.put("rootCauseService", hasFailure ? rootCauseService : null);
        summary.put("rootCauseError", hasFailure ? rootCauseError : null);
        summary.put("slowestService", slowestService);
        summary.put("slowestEndpoint", slowestEndpoint);
        summary.put("maxLatencyMs", maxLatencyMs);
        summary.put("totalLatencyMs", totalLatencyMs);
        summary.put("diagnosis", buildDiagnosis(
                hasFailure,
                rootCauseService,
                rootCauseError,
                slowestService,
                maxLatencyMs
        ));

        return summary;
    }

    private String buildDiagnosis(
            boolean hasFailure,
            String rootCauseService,
            String rootCauseError,
            String slowestService,
            long maxLatencyMs
    ) {
        if (hasFailure) {
            return "Request failed because " + rootCauseService + " returned an error: " + rootCauseError;
        }

        if (maxLatencyMs >= 1000) {
            return "Request succeeded but was slow. Slowest service was "
                    + slowestService
                    + " with latency "
                    + maxLatencyMs
                    + " ms.";
        }

        return "Request completed successfully with no detected service failure.";
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Integer integerValue) {
            return integerValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }

        return Integer.parseInt(String.valueOf(value));
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }

        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        return Long.parseLong(String.valueOf(value));
    }
}