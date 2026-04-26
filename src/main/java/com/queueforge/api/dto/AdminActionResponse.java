package com.queueforge.api.dto;

import java.util.UUID;

public record AdminActionResponse(
    boolean success,
    String message,
    UUID jobId,
    String newStatus
) {}
