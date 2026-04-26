package com.queueforge.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record Job(
    UUID id,
    String queue,
    String type,
    JsonNode payload,
    JobStatus status,
    JobPriority priority,
    Instant runAt,
    int attempts,
    int maxAttempts,
    String lockedBy,
    Instant lockedUntil,
    String idempotencyKey,
    String lastError,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt,
    Instant deadLetteredAt
) {
}
