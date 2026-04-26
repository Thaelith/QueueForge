package com.queueforge.api.dto;

import java.time.Instant;
import java.util.List;

public record WorkerSummaryResponse(List<WorkerInfo> workers) {
    public record WorkerInfo(
        String workerId,
        String status,
        long runningJobCount,
        long completedJobCount,
        long failedJobCount,
        Instant lastSeenAt
    ) {}
}
