package com.queueforge.api.dto;

import java.time.Instant;

public record JobEventResponse(
    Long id,
    String eventType,
    Instant createdAt,
    String message
) {}
