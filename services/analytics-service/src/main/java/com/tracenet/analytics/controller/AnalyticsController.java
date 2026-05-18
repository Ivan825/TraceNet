package com.tracenet.analytics.controller;

import com.tracenet.analytics.projection.EndpointLatencyProjection;
import com.tracenet.analytics.projection.ServiceErrorProjection;
import com.tracenet.analytics.projection.ServiceLatencyProjection;
import com.tracenet.analytics.repository.TraceSpanRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final TraceSpanRepository traceSpanRepository;

    public AnalyticsController(TraceSpanRepository traceSpanRepository) {
        this.traceSpanRepository = traceSpanRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId
    ) {
        String resolvedOrgId = resolveOrgId(orgId);

        long totalSpans = traceSpanRepository.countByOrgId(resolvedOrgId);
        long failedSpans = traceSpanRepository.countByOrgIdAndStatusCodeGreaterThanEqual(resolvedOrgId, 500);
        long successSpans = totalSpans - failedSpans;

        double errorRate = totalSpans == 0
                ? 0.0
                : (failedSpans * 100.0) / totalSpans;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orgId", resolvedOrgId);
        response.put("totalSpans", totalSpans);
        response.put("successSpans", successSpans);
        response.put("failedSpans", failedSpans);
        response.put("errorRatePercent", round(errorRate));

        return response;
    }

    @GetMapping("/services/latency")
    public List<Map<String, Object>> getServiceLatencyStats(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId
    ) {
        return traceSpanRepository.getServiceLatencyStats(resolveOrgId(orgId))
                .stream()
                .map(this::serviceLatencyToMap)
                .toList();
    }

    @GetMapping("/services/errors")
    public List<Map<String, Object>> getServiceErrorStats(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId
    ) {
        return traceSpanRepository.getServiceErrorStats(resolveOrgId(orgId))
                .stream()
                .map(this::serviceErrorToMap)
                .toList();
    }

    @GetMapping("/endpoints/latency")
    public List<Map<String, Object>> getEndpointLatencyStats(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId
    ) {
        return traceSpanRepository.getEndpointLatencyStats(resolveOrgId(orgId))
                .stream()
                .map(this::endpointLatencyToMap)
                .toList();
    }

    @GetMapping("/slow-traces")
    public Map<String, Object> getSlowTraceIds(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestParam(value = "threshold", defaultValue = "1000") Long threshold
    ) {
        String resolvedOrgId = resolveOrgId(orgId);
        List<String> traceIds = traceSpanRepository.findSlowTraceIds(resolvedOrgId, threshold);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orgId", resolvedOrgId);
        response.put("thresholdMs", threshold);
        response.put("count", traceIds.size());
        response.put("traceIds", traceIds);

        return response;
    }

    @GetMapping("/services/p95")
    public List<Map<String, Object>> getServiceP95Stats(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId
    ) {
        String resolvedOrgId = resolveOrgId(orgId);

        return traceSpanRepository.findDistinctServices(resolvedOrgId)
                .stream()
                .map(serviceName -> {
                    Double p95 = traceSpanRepository.getP95LatencyForService(resolvedOrgId, serviceName);

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("serviceName", serviceName);
                    response.put("p95LatencyMs", p95 == null ? 0.0 : round(p95));
                    return response;
                })
                .toList();
    }

    private Map<String, Object> serviceLatencyToMap(ServiceLatencyProjection projection) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", projection.getServiceName());
        response.put("averageLatencyMs", round(projection.getAverageLatencyMs()));
        response.put("maxLatencyMs", projection.getMaxLatencyMs());
        response.put("requestCount", projection.getRequestCount());
        return response;
    }

    private Map<String, Object> serviceErrorToMap(ServiceErrorProjection projection) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", projection.getServiceName());
        response.put("errorCount", projection.getErrorCount());
        return response;
    }

    private Map<String, Object> endpointLatencyToMap(EndpointLatencyProjection projection) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", projection.getServiceName());
        response.put("endpoint", projection.getEndpoint());
        response.put("averageLatencyMs", round(projection.getAverageLatencyMs()));
        response.put("maxLatencyMs", projection.getMaxLatencyMs());
        response.put("requestCount", projection.getRequestCount());
        return response;
    }

    private String resolveOrgId(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            return "unknown-org";
        }
        return orgId;
    }

    private double round(Double value) {
        if (value == null) {
            return 0.0;
        }

        return Math.round(value * 100.0) / 100.0;
    }
}