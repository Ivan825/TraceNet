package com.tracenet.traceingestion.controller;

import com.tracenet.traceingestion.dto.TraceSpanRequest;
import com.tracenet.traceingestion.entity.TraceSpan;
import com.tracenet.traceingestion.repository.TraceSpanRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/traces")
public class TraceIngestionController {

    private final TraceSpanRepository traceSpanRepository;

    public TraceIngestionController(TraceSpanRepository traceSpanRepository) {
        this.traceSpanRepository = traceSpanRepository;
    }

    @PostMapping("/spans")
    public ResponseEntity<Map<String, Object>> ingestSpan(@RequestBody TraceSpanRequest request) {
        TraceSpan span = new TraceSpan(
                request.getTraceId(),
                request.getServiceName(),
                request.getEndpoint(),
                request.getHttpMethod(),
                request.getStatusCode(),
                request.getLatencyMs(),
                request.getErrorMessage(),
                Instant.now()
        );

        TraceSpan savedSpan = traceSpanRepository.save(span);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Trace span ingested successfully");
        response.put("id", savedSpan.getId());
        response.put("traceId", savedSpan.getTraceId());
        response.put("serviceName", savedSpan.getServiceName());

        return ResponseEntity.ok(response);
    }
}