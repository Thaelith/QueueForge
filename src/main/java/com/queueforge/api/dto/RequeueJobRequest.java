package com.queueforge.api.dto;

public record RequeueJobRequest(
    boolean resetAttempts,
    String reason
) {}
