package com.queueforge.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record JobEvent(
    Long id,
    UUID jobId,
    String eventType,
    String fromStatus,
    String toStatus,
    String workerId,
    String message,
    JsonNode metadata,
    Instant createdAt
) {
}
