package com.tracenet.alert.controller;

import com.tracenet.alert.entity.Alert;
import com.tracenet.alert.repository.AlertRepository;
import com.tracenet.alert.repository.TraceSpanRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private static final double ERROR_RATE_THRESHOLD_PERCENT = 10.0;
    private static final double SERVICE_P95_THRESHOLD_MS = 1000.0;
    private static final long SLOW_TRACE_THRESHOLD_MS = 1000L;

    private final TraceSpanRepository traceSpanRepository;
    private final AlertRepository alertRepository;

    public AlertController(
            TraceSpanRepository traceSpanRepository,
            AlertRepository alertRepository
    ) {
        this.traceSpanRepository = traceSpanRepository;
        this.alertRepository = alertRepository;
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluateAlerts(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId
    ) {
        String resolvedOrgId = resolveOrgId(orgId);

        List<Alert> generatedAlerts = new ArrayList<>();

        evaluateErrorRate(resolvedOrgId, generatedAlerts);
        evaluateServiceP95Latency(resolvedOrgId, generatedAlerts);
        evaluateSlowTraces(resolvedOrgId, generatedAlerts);
        evaluateServiceFailures(resolvedOrgId, generatedAlerts);

        List<Alert> savedAlerts = alertRepository.saveAll(generatedAlerts);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orgId", resolvedOrgId);
        response.put("generatedAlertCount", savedAlerts.size());
        response.put("alerts", savedAlerts.stream().map(this::toResponse).toList());

        return response;
    }

    @GetMapping
    public List<Map<String, Object>> getAlerts(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestParam(value = "status", required = false) String status
    ) {
        String resolvedOrgId = resolveOrgId(orgId);

        List<Alert> alerts;

        if (status == null || status.isBlank()) {
            alerts = alertRepository.findByOrgIdOrderByCreatedAtDesc(resolvedOrgId);
        } else {
            alerts = alertRepository.findByOrgIdAndStatusOrderByCreatedAtDesc(
                    resolvedOrgId,
                    status.toUpperCase(Locale.ROOT)
            );
        }

        return alerts.stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @PathVariable UUID id
    ) {
        String resolvedOrgId = resolveOrgId(orgId);

        Optional<Alert> optionalAlert = alertRepository.findById(id);

        if (optionalAlert.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "message", "Alert not found"
            ));
        }

        Alert alert = optionalAlert.get();

        if (!resolvedOrgId.equals(alert.getOrgId())) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Alert does not belong to this organization"
            ));
        }

        alert.acknowledge();
        Alert savedAlert = alertRepository.save(alert);

        return ResponseEntity.ok(toResponse(savedAlert));
    }

    private void evaluateErrorRate(String orgId, List<Alert> generatedAlerts) {
        long totalSpans = traceSpanRepository.countByOrgId(orgId);

        if (totalSpans == 0) {
            return;
        }

        long failedSpans = traceSpanRepository.countByOrgIdAndStatusCodeGreaterThanEqual(orgId, 500);
        double errorRate = (failedSpans * 100.0) / totalSpans;

        if (errorRate > ERROR_RATE_THRESHOLD_PERCENT) {
            generatedAlerts.add(new Alert(
                    orgId,
                    "ERROR_RATE_THRESHOLD",
                    "HIGH",
                    null,
                    "Error rate crossed threshold. Current error rate: " + round(errorRate) + "%",
                    "errorRatePercent",
                    round(errorRate),
                    ERROR_RATE_THRESHOLD_PERCENT,
                    "OPEN",
                    Instant.now()
            ));
        }
    }

    private void evaluateServiceP95Latency(String orgId, List<Alert> generatedAlerts) {
        List<String> services = traceSpanRepository.findDistinctServices(orgId);

        for (String serviceName : services) {
            Double p95Latency = traceSpanRepository.getP95LatencyForService(orgId, serviceName);

            if (p95Latency != null && p95Latency > SERVICE_P95_THRESHOLD_MS) {
                generatedAlerts.add(new Alert(
                        orgId,
                        "SERVICE_P95_LATENCY_THRESHOLD",
                        "MEDIUM",
                        serviceName,
                        "Service p95 latency crossed threshold for " + serviceName
                                + ". Current p95: " + round(p95Latency) + " ms",
                        "p95LatencyMs",
                        round(p95Latency),
                        SERVICE_P95_THRESHOLD_MS,
                        "OPEN",
                        Instant.now()
                ));
            }
        }
    }

    private void evaluateSlowTraces(String orgId, List<Alert> generatedAlerts) {
        long slowTraceSpanCount = traceSpanRepository.countByOrgIdAndLatencyMsGreaterThanEqual(
                orgId,
                SLOW_TRACE_THRESHOLD_MS
        );

        if (slowTraceSpanCount > 0) {
            generatedAlerts.add(new Alert(
                    orgId,
                    "SLOW_TRACE_DETECTED",
                    "MEDIUM",
                    null,
                    "Detected " + slowTraceSpanCount + " spans with latency >= "
                            + SLOW_TRACE_THRESHOLD_MS + " ms",
                    "slowSpanCount",
                    (double) slowTraceSpanCount,
                    (double) 0,
                    "OPEN",
                    Instant.now()
            ));
        }
    }

    private void evaluateServiceFailures(String orgId, List<Alert> generatedAlerts) {
        List<String> services = traceSpanRepository.findDistinctServices(orgId);

        for (String serviceName : services) {
            long failureCount =
                    traceSpanRepository.countByOrgIdAndServiceNameAndStatusCodeGreaterThanEqual(
                            orgId,
                            serviceName,
                            500
                    );

            if (failureCount > 0) {
                generatedAlerts.add(new Alert(
                        orgId,
                        "SERVICE_FAILURES_DETECTED",
                        "HIGH",
                        serviceName,
                        "Detected " + failureCount + " failed spans for service " + serviceName,
                        "failureCount",
                        (double) failureCount,
                        0.0,
                        "OPEN",
                        Instant.now()
                ));
            }
        }
    }

    private Map<String, Object> toResponse(Alert alert) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", alert.getId());
        response.put("orgId", alert.getOrgId());
        response.put("alertType", alert.getAlertType());
        response.put("severity", alert.getSeverity());
        response.put("serviceName", alert.getServiceName());
        response.put("message", alert.getMessage());
        response.put("metricName", alert.getMetricName());
        response.put("metricValue", alert.getMetricValue());
        response.put("thresholdValue", alert.getThresholdValue());
        response.put("status", alert.getStatus());
        response.put("createdAt", alert.getCreatedAt());
        response.put("acknowledgedAt", alert.getAcknowledgedAt());
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