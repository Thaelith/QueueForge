package com.queueforge.api.dto;

import java.time.Instant;
import java.util.List;

public record QueueStatsResponse(List<QueueStat> queues) {
    public record QueueStat(
        String queueName,
        long pending,
        long running,
        long retryScheduled,
        long completed,
        long deadLettered,
        long cancelled,
        long total,
        Instant oldestPendingAt,
        Instant newestCreatedAt
    ) {}
}
