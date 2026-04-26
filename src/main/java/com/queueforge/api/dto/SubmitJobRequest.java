package com.queueforge.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.queueforge.domain.JobPriority;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record SubmitJobRequest(
    @NotBlank @Size(max = 100) String queue,
    @NotBlank @Size(max = 100) String type,
    @NotNull JsonNode payload,
    JobPriority priority,
    @Min(1) @Max(20) Integer maxAttempts,
    Instant scheduledAt,
    @Size(max = 200) String idempotencyKey
) {
}
