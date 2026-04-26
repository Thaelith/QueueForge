package com.queueforge.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueforge.TestcontainersConfiguration;
import com.queueforge.domain.Job;
import com.queueforge.domain.JobPriority;
import com.queueforge.domain.JobStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class JobLeasingIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLeaseJobAndSetRunning() throws Exception {
        UUID id = submitPendingJob("queue-a", "email.send");

        Optional<Job> leased = jobRepository.leaseNextAvailable("worker-1", "queue-a", Duration.ofSeconds(60));
        assertThat(leased).isPresent();
        assertThat(leased.get().status()).isEqualTo(JobStatus.RUNNING);
        assertThat(leased.get().lockedBy()).isEqualTo("worker-1");
        assertThat(leased.get().lockedUntil()).isAfter(Instant.now());
        assertThat(leased.get().attempts()).isEqualTo(1);
    }

    @Test
    void shouldIncrementAttemptsOnLease() throws Exception {
        submitPendingJob("queue-b", "report.generate");

        jobRepository.leaseNextAvailable("w1", "queue-b", Duration.ofSeconds(60));
        Optional<Job> leased = jobRepository.leaseNextAvailable("w1", "queue-b", Duration.ofSeconds(60));

        assertThat(leased).isEmpty();
    }

    @Test
    void shouldLeaseRetryScheduledJobAfterRunAt() throws Exception {
        UUID id = submitJob("queue-c", "webhook", JobStatus.RETRY_SCHEDULED, Instant.now().minus(1, ChronoUnit.MINUTES));

        Optional<Job> leased = jobRepository.leaseNextAvailable("w1", "queue-c", Duration.ofSeconds(60));
        assertThat(leased).isPresent();
        assertThat(leased.get().status()).isEqualTo(JobStatus.RUNNING);
    }

    @Test
    void shouldNotLeaseFutureJob() throws Exception {
        submitJob("queue-d", "future.job", JobStatus.PENDING, Instant.now().plus(1, ChronoUnit.HOURS));

        Optional<Job> leased = jobRepository.leaseNextAvailable("w1", "queue-d", Duration.ofSeconds(60));
        assertThat(leased).isEmpty();
    }

    @Test
    void shouldNotLeaseRunningJob() throws Exception {
        UUID id = submitPendingJob("queue-e", "test.type");
        jobRepository.leaseNextAvailable("w1", "queue-e", Duration.ofSeconds(60));

        Optional<Job> secondLease = jobRepository.leaseNextAvailable("w2", "queue-e", Duration.ofSeconds(60));
        assertThat(secondLease).isEmpty();
    }

    @Test
    void shouldNotLeaseSameJobToDifferentWorkers() throws Exception {
        int jobCount = 20;
        for (int i = 0; i < jobCount; i++) {
            submitPendingJob("concurrent-queue", "parallel.task");
        }

        AtomicInteger w1Count = new AtomicInteger(0);
        AtomicInteger w2Count = new AtomicInteger(0);
        Set<UUID> leasedIds = Collections.synchronizedSet(new HashSet<>());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        Runnable worker1 = () -> {
            for (int i = 0; i < 15; i++) {
                Optional<Job> job = jobRepository.leaseNextAvailable("worker-A", "concurrent-queue", Duration.ofSeconds(60));
                if (job.isPresent()) {
                    UUID jobId = job.get().id();
                    boolean added = leasedIds.add(jobId);
                    if (added) w1Count.incrementAndGet();
                }
            }
            latch.countDown();
        };

        Runnable worker2 = () -> {
            for (int i = 0; i < 15; i++) {
                Optional<Job> job = jobRepository.leaseNextAvailable("worker-B", "concurrent-queue", Duration.ofSeconds(60));
                if (job.isPresent()) {
                    UUID jobId = job.get().id();
                    boolean added = leasedIds.add(jobId);
                    if (added) w2Count.incrementAndGet();
                }
            }
            latch.countDown();
        };

        executor.submit(worker1);
        executor.submit(worker2);
        latch.await();
        executor.shutdown();

        int totalLeased = w1Count.get() + w2Count.get();
        assertThat(totalLeased).isEqualTo(jobCount);
        assertThat(leasedIds).hasSize(jobCount);
    }

    @Test
    void shouldMarkCompletedClearingLock() throws Exception {
        UUID id = submitPendingJob("queue-f", "cleanup.task");
        jobRepository.leaseNextAvailable("w1", "queue-f", Duration.ofSeconds(60));

        jobRepository.markCompleted(id, "w1");

        Job job = jobRepository.findById(id).orElseThrow();
        assertThat(job.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.lockedBy()).isNull();
        assertThat(job.lockedUntil()).isNull();
    }

    @Test
    void shouldMarkFailedAndScheduleRetry() throws Exception {
        UUID id = submitJob("queue-g", "flaky.task", JobStatus.PENDING, Instant.now());
        jobRepository.leaseNextAvailable("w1", "queue-g", Duration.ofSeconds(60));

        Instant nextRetry = Instant.now().plusSeconds(30);
        jobRepository.markFailed(id, "w1", "Connection timeout", nextRetry);

        Job job = jobRepository.findById(id).orElseThrow();
        assertThat(job.status()).isEqualTo(JobStatus.RETRY_SCHEDULED);
        assertThat(job.lastError()).isEqualTo("Connection timeout");
        assertThat(job.lockedBy()).isNull();
        assertThat(job.lockedUntil()).isNull();
    }

    @Test
    void shouldMoveToDeadLetter() throws Exception {
        UUID id = submitJob("queue-h", "broken.task", JobStatus.PENDING, Instant.now(), 1);
        jobRepository.leaseNextAvailable("w1", "queue-h", Duration.ofSeconds(60));

        jobRepository.moveToDeadLetter(id, "w1", "Fatal error");

        Job job = jobRepository.findById(id).orElseThrow();
        assertThat(job.status()).isEqualTo(JobStatus.DEAD_LETTERED);
        assertThat(job.lastError()).isEqualTo("Fatal error");
        assertThat(job.deadLetteredAt()).isNotNull();
    }

    @Test
    void shouldReleaseExpiredLeases() throws Exception {
        UUID id = submitPendingJob("queue-i", "stale.task");
        String sql = "UPDATE jobs SET status = 'RUNNING', locked_by = 'dead-worker', locked_until = now() - interval '1 minute', updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, id);

        int recovered = jobRepository.releaseExpiredLeases();
        assertThat(recovered).isEqualTo(1);

        Job job = jobRepository.findById(id).orElseThrow();
        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.lockedBy()).isNull();
        assertThat(job.lastError()).contains("expired");
    }

    @Test
    void shouldRespectPriorityOrderOnLease() throws Exception {
        UUID lowId = submitJob("priority-queue", "task", JobStatus.PENDING, Instant.now(), 3, JobPriority.LOW);
        UUID normalId = submitJob("priority-queue", "task", JobStatus.PENDING, Instant.now(), 3, JobPriority.NORMAL);
        UUID highId = submitJob("priority-queue", "task", JobStatus.PENDING, Instant.now(), 3, JobPriority.HIGH);
        UUID criticalId = submitJob("priority-queue", "task", JobStatus.PENDING, Instant.now(), 3, JobPriority.CRITICAL);

        Optional<Job> first = jobRepository.leaseNextAvailable("w1", "priority-queue", Duration.ofSeconds(60));
        assertThat(first).isPresent();
        assertThat(first.get().id()).isEqualTo(criticalId);
    }

    private UUID submitPendingJob(String queue, String type) throws Exception {
        return submitJob(queue, type, JobStatus.PENDING, Instant.now());
    }

    private UUID submitJob(String queue, String type, JobStatus status, Instant runAt) throws Exception {
        return submitJob(queue, type, status, runAt, 3, JobPriority.NORMAL);
    }

    private UUID submitJob(String queue, String type, JobStatus status, Instant runAt, int maxAttempts) throws Exception {
        return submitJob(queue, type, status, runAt, maxAttempts, JobPriority.NORMAL);
    }

    private UUID submitJob(String queue, String type, JobStatus status, Instant runAt, int maxAttempts, JobPriority priority) throws Exception {
        UUID id = UUID.randomUUID();
        Job job = new Job(
            id, queue, type, objectMapper.readTree("{\"msg\":\"test\"}"),
            status, priority, runAt, 0, maxAttempts,
            null, null, null, null,
            Instant.now(), Instant.now(), null, null
        );
        jobRepository.save(job);
        return id;
    }
}
