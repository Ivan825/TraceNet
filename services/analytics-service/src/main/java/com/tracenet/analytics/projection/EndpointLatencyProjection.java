package com.tracenet.analytics.projection;

public interface EndpointLatencyProjection {

    String getServiceName();

    String getEndpoint();

    Double getAverageLatencyMs();

    Long getMaxLatencyMs();

    Long getRequestCount();
}