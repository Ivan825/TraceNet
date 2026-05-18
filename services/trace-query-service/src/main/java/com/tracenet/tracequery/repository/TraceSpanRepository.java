package com.tracenet.tracequery.repository;

import com.tracenet.tracequery.entity.TraceSpan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TraceSpanRepository extends JpaRepository<TraceSpan, UUID> {

    List<TraceSpan> findByTraceIdAndOrgIdOrderByCreatedAtAsc(String traceId, String orgId);

    List<TraceSpan> findByOrgIdAndStatusCodeGreaterThanEqualOrderByCreatedAtDesc(String orgId, Integer statusCode);

    List<TraceSpan> findByOrgIdAndLatencyMsGreaterThanEqualOrderByLatencyMsDesc(String orgId, Long latencyMs);

    List<TraceSpan> findByOrgIdAndServiceNameOrderByCreatedAtDesc(String orgId, String serviceName);
}