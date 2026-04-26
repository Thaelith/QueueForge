package com.queueforge.api.dto;

import java.util.Map;

public record DashboardSummaryResponse(
    long totalJobs,
    long pendingJobs,
    long runningJobs,
    long completedJobs,
    long retryScheduledJobs,
    long deadLetteredJobs,
    long cancelledJobs,
    int activeWorkers,
    long completedLastHour,
    long failedLastHour,
    Map<String, Long> jobsByQueue
) {}
