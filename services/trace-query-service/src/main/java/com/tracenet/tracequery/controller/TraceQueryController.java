package com.tracenet.tracequery.controller;

import com.tracenet.tracequery.entity.TraceSpan;
import com.tracenet.tracequery.repository.TraceSpanRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/query/traces")
public class TraceQueryController {

    private final TraceSpanRepository traceSpanRepository;

    public TraceQueryController(TraceSpanRepository traceSpanRepository) {
        this.traceSpanRepository = traceSpanRepository;
    }

    @GetMapping("/{traceId}")
    public List<Map<String, Object>> getTraceById(@PathVariable String traceId) {
        return traceSpanRepository.findByTraceIdOrderByCreatedAtAsc(traceId)
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

    @GetMapping("/errors")
    public List<Map<String, Object>> getErrorTraces() {
        return traceSpanRepository.findByStatusCodeGreaterThanEqualOrderByCreatedAtDesc(500)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/slow")
    public List<Map<String, Object>> getSlowTraces(
            @RequestParam(value = "threshold", defaultValue = "1000") Long threshold
    ) {
        return traceSpanRepository.findByLatencyMsGreaterThanEqualOrderByLatencyMsDesc(threshold)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/service/{serviceName}")
    public List<Map<String, Object>> getTracesByService(@PathVariable String serviceName) {
        return traceSpanRepository.findByServiceNameOrderByCreatedAtDesc(serviceName)
                .stream()
                .map(this::toResponse)
                .toList();
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