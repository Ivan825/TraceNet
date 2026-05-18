package com.tracenet.analytics.projection;

public interface ServiceLatencyProjection {

    String getServiceName();

    Double getAverageLatencyMs();

    Long getMaxLatencyMs();

    Long getRequestCount();
}