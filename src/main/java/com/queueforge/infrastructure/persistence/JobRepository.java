package com.queueforge.infrastructure.persistence;

import com.queueforge.domain.Job;
import com.queueforge.domain.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository {
    Job save(Job job);
    Optional<Job> findById(UUID id);
    Optional<Job> findByIdempotencyKey(String idempotencyKey);
    Page<Job> findAll(String queue, JobStatus status, String type, Pageable pageable);

    Optional<Job> leaseNextAvailable(String workerId, String queueName, Duration leaseDuration);

    void markCompleted(UUID jobId, String workerId);

    void markFailed(UUID jobId, String workerId, String errorMessage, Instant nextRetryAt);

    void moveToDeadLetter(UUID jobId, String workerId, String errorMessage);

    int releaseExpiredLeases();

    // Admin operations
    Map<String, Long> countByStatus();

    List<Map<String, Object>> queueStats();

    List<Map<String, Object>> workerStats();

    boolean requeueJob(UUID jobId, boolean resetAttempts);

    boolean cancelJob(UUID jobId);

    boolean retryNow(UUID jobId, boolean resetAttempts);

    int cleanupTerminalJobs(int completedRetentionDays, int cancelledRetentionDays, int batchSize);

    Map<String, Long> countLastHour();
}
