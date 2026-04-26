package com.queueforge.api.dto;

import java.time.Instant;

public record JobEventResponse(
    Long id,
    String eventType,
    String fromStatus,
    String toStatus,
    String workerId,
    Instant createdAt,
    String message
) {}
