package com.queueforge.domain;

import java.time.Duration;

public record RetryPolicy(
    int maxAttempts,
    Duration baseDelay,
    Duration maxDelay
) {
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(10, Duration.ofSeconds(10), Duration.ofSeconds(300));
    }

    public Duration nextDelay(int attempt) {
        long seconds = baseDelay.toSeconds() * (long) Math.pow(2, Math.max(0, attempt - 1));
        return Duration.ofSeconds(Math.min(seconds, maxDelay.toSeconds()));
    }
}
