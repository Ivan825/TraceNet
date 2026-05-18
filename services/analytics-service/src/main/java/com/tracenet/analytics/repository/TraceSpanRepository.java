package com.tracenet.analytics.repository;

import com.tracenet.analytics.entity.TraceSpan;
import com.tracenet.analytics.projection.EndpointLatencyProjection;
import com.tracenet.analytics.projection.ServiceErrorProjection;
import com.tracenet.analytics.projection.ServiceLatencyProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TraceSpanRepository extends JpaRepository<TraceSpan, UUID> {

    Long countByOrgId(String orgId);

    Long countByOrgIdAndStatusCodeGreaterThanEqual(String orgId, Integer statusCode);

    @Query(value = """
            SELECT 
                service_name AS serviceName,
                AVG(latency_ms) AS averageLatencyMs,
                MAX(latency_ms) AS maxLatencyMs,
                COUNT(*) AS requestCount
            FROM trace_spans
            WHERE org_id = :orgId
            GROUP BY service_name
            ORDER BY AVG(latency_ms) DESC
            """, nativeQuery = true)
    List<ServiceLatencyProjection> getServiceLatencyStats(String orgId);

    @Query(value = """
            SELECT 
                service_name AS serviceName,
                COUNT(*) AS errorCount
            FROM trace_spans
            WHERE org_id = :orgId
              AND status_code >= 500
            GROUP BY service_name
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<ServiceErrorProjection> getServiceErrorStats(String orgId);

    @Query(value = """
            SELECT 
                service_name AS serviceName,
                endpoint AS endpoint,
                AVG(latency_ms) AS averageLatencyMs,
                MAX(latency_ms) AS maxLatencyMs,
                COUNT(*) AS requestCount
            FROM trace_spans
            WHERE org_id = :orgId
            GROUP BY service_name, endpoint
            ORDER BY AVG(latency_ms) DESC
            """, nativeQuery = true)
    List<EndpointLatencyProjection> getEndpointLatencyStats(String orgId);

    @Query(value = """
            SELECT DISTINCT trace_id
            FROM trace_spans
            WHERE org_id = :orgId
              AND latency_ms >= :threshold
            ORDER BY trace_id
            """, nativeQuery = true)
    List<String> findSlowTraceIds(String orgId, Long threshold);

    @Query(value = """
            SELECT 
                percentile_cont(0.95) WITHIN GROUP (ORDER BY latency_ms)
            FROM trace_spans
            WHERE org_id = :orgId
              AND service_name = :serviceName
            """, nativeQuery = true)
    Double getP95LatencyForService(String orgId, String serviceName);

    @Query(value = """
            SELECT DISTINCT service_name
            FROM trace_spans
            WHERE org_id = :orgId
            ORDER BY service_name
            """, nativeQuery = true)
    List<String> findDistinctServices(String orgId);
}