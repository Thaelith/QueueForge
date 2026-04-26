package com.queueforge.domain;

public enum JobStatus {
    PENDING,
    RUNNING,
    RETRY_SCHEDULED,
    COMPLETED,
    DEAD_LETTERED,
    CANCELLED
}
