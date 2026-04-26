package com.queueforge.domain;

import java.time.Instant;
import java.util.UUID;

public record JobAttempt(
    long id,
    UUID jobId,
    int attemptNumber,
    String workerId,
    Instant startedAt,
    Instant endedAt,
    boolean success,
    String errorMessage
) {
}
