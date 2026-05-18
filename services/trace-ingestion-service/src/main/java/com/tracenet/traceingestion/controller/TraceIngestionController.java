package com.tracenet.traceingestion.controller;

import com.tracenet.traceingestion.dto.TraceSpanRequest;
import com.tracenet.traceingestion.entity.TraceSpan;
import com.tracenet.traceingestion.repository.TraceSpanRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/traces")
public class TraceIngestionController {

    private final TraceSpanRepository traceSpanRepository;

    public TraceIngestionController(TraceSpanRepository traceSpanRepository) {
        this.traceSpanRepository = traceSpanRepository;
    }

    @PostMapping("/spans")
    public Map<String, Object> ingestSpan(@RequestBody TraceSpanRequest request) {
        TraceSpan span = new TraceSpan(
                request.getTraceId(),
                request.getServiceName(),
                request.getEndpoint(),
                request.getHttpMethod(),
                request.getStatusCode(),
                request.getLatencyMs(),
                request.getErrorMessage(),
                Instant.now()
        );

        TraceSpan savedSpan = traceSpanRepository.save(span);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Trace span ingested successfully");
        response.put("id", savedSpan.getId());
        response.put("traceId", savedSpan.getTraceId());
        response.put("serviceName", savedSpan.getServiceName());

        return response;
    }

    @GetMapping("/spans")
    public List<Map<String, Object>> getAllSpans() {
        return traceSpanRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{traceId}")
    public List<Map<String, Object>> getSpansByTraceId(@PathVariable String traceId) {
        return traceSpanRepository.findByTraceIdOrderByCreatedAtAsc(traceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/errors")
    public List<Map<String, Object>> getErrorSpans() {
        return traceSpanRepository.findByStatusCodeGreaterThanEqualOrderByCreatedAtDesc(500)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{traceId}/summary")
    public Map<String, Object> getTraceSummary(@PathVariable String traceId) {
        List<TraceSpan> traceSpans = traceSpanRepository.findByTraceIdOrderByCreatedAtAsc(traceId);

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

        for (TraceSpan span : traceSpans) {
            int statusCode = span.getStatusCode();
            long latencyMs = span.getLatencyMs();

            totalLatencyMs += latencyMs;

            if (latencyMs > maxLatencyMs) {
                maxLatencyMs = latencyMs;
                slowestService = span.getServiceName();
                slowestEndpoint = span.getEndpoint();
            }

            if (statusCode >= 500) {
                failureCount++;

                if (!hasFailure) {
                    hasFailure = true;
                    rootCauseService = span.getServiceName();
                    rootCauseError = span.getErrorMessage() == null
                            ? "No error message available"
                            : span.getErrorMessage();
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

    private Map<String, Object> toResponse(TraceSpan span) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", span.getId());
        response.put("traceId", span.getTraceId());
        response.put("serviceName", span.getServiceName());
        response.put("endpoint", span.getEndpoint());
        response.put("httpMethod", span.getHttpMethod());
        response.put("statusCode", span.getStatusCode());
        response.put("latencyMs", span.getLatencyMs());
        response.put("errorMessage", span.getErrorMessage());
        response.put("createdAt", span.getCreatedAt());
        return response;
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
}