package com.tracenet.traceingestion.repository;

import com.tracenet.traceingestion.entity.TraceSpan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TraceSpanRepository extends JpaRepository<TraceSpan, UUID> {
}