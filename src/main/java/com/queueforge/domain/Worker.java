package com.queueforge.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record Worker(
    String id,
    String hostname,
    WorkerStatus status,
    Instant lastHeartbeatAt,
    Instant startedAt,
    JsonNode metadata
) {
}
