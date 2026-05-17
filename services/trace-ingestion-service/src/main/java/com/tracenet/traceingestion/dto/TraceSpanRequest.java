package com.tracenet.traceingestion.dto;

public class TraceSpanRequest {

    private String traceId;
    private String serviceName;
    private String endpoint;
    private String httpMethod;
    private Integer statusCode;
    private Long latencyMs;
    private String errorMessage;

    public TraceSpanRequest() {
    }

    public TraceSpanRequest(
            String traceId,
            String serviceName,
            String endpoint,
            String httpMethod,
            Integer statusCode,
            Long latencyMs,
            String errorMessage
    ) {
        this.traceId = traceId;
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}