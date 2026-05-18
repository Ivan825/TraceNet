package com.tracenet.tracequery.repository;

import com.tracenet.tracequery.entity.TraceSpan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TraceSpanRepository extends JpaRepository<TraceSpan, UUID> {

    List<TraceSpan> findByTraceIdOrderByCreatedAtAsc(String traceId);

    List<TraceSpan> findByStatusCodeGreaterThanEqualOrderByCreatedAtDesc(Integer statusCode);

    List<TraceSpan> findByLatencyMsGreaterThanEqualOrderByLatencyMsDesc(Long latencyMs);

    List<TraceSpan> findByServiceNameOrderByCreatedAtDesc(String serviceName);
}