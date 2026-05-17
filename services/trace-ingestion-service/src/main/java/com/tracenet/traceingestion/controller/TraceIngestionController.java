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
}