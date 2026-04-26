package com.queueforge.domain;

import java.time.Instant;
import java.util.UUID;

public record DeadLetterJob(
    UUID jobId,
    String reason,
    Instant deadLetteredAt,
    int totalAttempts
) {
}
