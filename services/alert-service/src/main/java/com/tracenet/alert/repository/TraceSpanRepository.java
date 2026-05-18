package com.tracenet.alert.repository;

import com.tracenet.alert.entity.TraceSpan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TraceSpanRepository extends JpaRepository<TraceSpan, UUID> {

    Long countByOrgId(String orgId);

    Long countByOrgIdAndStatusCodeGreaterThanEqual(String orgId, Integer statusCode);

    @Query(value = """
            SELECT DISTINCT service_name
            FROM trace_spans
            WHERE org_id = :orgId
            ORDER BY service_name
            """, nativeQuery = true)
    List<String> findDistinctServices(String orgId);

    Long countByOrgIdAndServiceNameAndStatusCodeGreaterThanEqual(
            String orgId,
            String serviceName,
            Integer statusCode
    );

    @Query(value = """
            SELECT 
                percentile_cont(0.95) WITHIN GROUP (ORDER BY latency_ms)
            FROM trace_spans
            WHERE org_id = :orgId
              AND service_name = :serviceName
            """, nativeQuery = true)
    Double getP95LatencyForService(String orgId, String serviceName);

    Long countByOrgIdAndLatencyMsGreaterThanEqual(String orgId, Long latencyMs);
}