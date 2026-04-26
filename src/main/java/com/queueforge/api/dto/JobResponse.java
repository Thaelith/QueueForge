package com.queueforge.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
    UUID id,
    String queue,
    String type,
    JsonNode payload,
    String status,
    int priority,
    int attempts,
    int maxAttempts,
    Instant scheduledAt,
    Instant createdAt,
    Instant updatedAt
) {
}
