package com.tracenet.alert.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "metric_value", nullable = false)
    private Double metricValue;

    @Column(name = "threshold_value", nullable = false)
    private Double thresholdValue;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    public Alert() {
    }

    public Alert(
            String orgId,
            String alertType,
            String severity,
            String serviceName,
            String message,
            String metricName,
            Double metricValue,
            Double thresholdValue,
            String status,
            Instant createdAt
    ) {
        this.orgId = orgId;
        this.alertType = alertType;
        this.severity = severity;
        this.serviceName = serviceName;
        this.message = message;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.thresholdValue = thresholdValue;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMessage() {
        return message;
    }

    public String getMetricName() {
        return metricName;
    }

    public Double getMetricValue() {
        return metricValue;
    }

    public Double getThresholdValue() {
        return thresholdValue;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void acknowledge() {
        this.status = "ACKNOWLEDGED";
        this.acknowledgedAt = Instant.now();
    }
}