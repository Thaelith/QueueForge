package com.queueforge.application;

import com.queueforge.config.QueueForgeProperties;
import com.queueforge.domain.Job;
import com.queueforge.domain.JobPriority;
import com.queueforge.domain.JobStatus;
import com.queueforge.domain.RetryPolicy;
import com.queueforge.infrastructure.persistence.JobRepository;
import com.queueforge.worker.JobHandlerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class WorkerService {
    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);

    private final JobRepository jobRepository;
    private final JobHandlerRegistry handlerRegistry;
    private final JobEventService eventService;
    private final QueueForgeProperties properties;
    private final RetryPolicy retryPolicy;

    private final Counter jobsLeased;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Counter jobsDeadLettered;
    private final Counter leasesRecovered;
    private final Counter workerPolls;
    private final Counter workerPollErrors;
    private final Timer executionDuration;

    public WorkerService(JobRepository jobRepository,
                         JobHandlerRegistry handlerRegistry,
                         JobEventService eventService,
                         QueueForgeProperties properties,
                         MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.handlerRegistry = handlerRegistry;
        this.eventService = eventService;
        this.properties = properties;
        this.retryPolicy = new RetryPolicy(
            properties.getRetry().getMaxAttempts(),
            Duration.ofSeconds(properties.getRetry().getBaseDelaySeconds()),
            Duration.ofSeconds(properties.getRetry().getMaxDelaySeconds()));

        this.jobsLeased = Counter.builder("queueforge.jobs.leased")
            .description("Total jobs leased")
            .register(meterRegistry);
        this.jobsCompleted = Counter.builder("queueforge.jobs.completed")
            .description("Total jobs completed")
            .register(meterRegistry);
        this.jobsFailed = Counter.builder("queueforge.jobs.failed")
            .description("Total job failures (retried)")
            .register(meterRegistry);
        this.jobsDeadLettered = Counter.builder("queueforge.jobs.dead_lettered")
            .description("Total jobs moved to dead letter")
            .register(meterRegistry);
        this.leasesRecovered = Counter.builder("queueforge.jobs.recovered")
            .description("Total expired leases recovered")
            .register(meterRegistry);
        this.workerPolls = Counter.builder("queueforge.worker.polls")
            .description("Total worker poll cycles")
            .register(meterRegistry);
        this.workerPollErrors = Counter.builder("queueforge.worker.poll_errors")
            .description("Poll cycles that errored")
            .register(meterRegistry);
        this.executionDuration = Timer.builder("queueforge.job.execution.duration")
            .description("Job handler execution time")
            .register(meterRegistry);
    }

    public void processJobs(String workerId, Iterable<String> queues) {
        workerPolls.increment();
        int processed = 0;
        int maxJobs = properties.getWorker().getMaxJobsPerPoll();

        for (String queue : queues) {
            if (processed >= maxJobs) break;
            while (processed < maxJobs) {
                var result = leaseAndExecute(workerId, queue, properties.getWorker().getLeaseDuration());
                if (result.processed()) {
                    processed++;
                } else {
                    break;
                }
            }
        }
    }

    public JobResult leaseAndExecute(String workerId, String queueName) {
        return leaseAndExecute(workerId, queueName, properties.getWorker().getLeaseDuration());
    }

    public JobResult leaseAndExecute(String workerId, String queueName, Duration leaseDuration) {
        var leased = jobRepository.leaseNextAvailable(workerId, queueName, leaseDuration);

        if (leased.isEmpty()) {
            return JobResult.notFound(queueName);
        }

        Job job = leased.get();
        jobsLeased.increment();
        MDC.put("jobId", job.id().toString());
        MDC.put("workerId", workerId);
        MDC.put("queueName", queueName);
        MDC.put("jobType", job.type());
        MDC.put("attempt", String.valueOf(job.attempts()));
        eventService.record(job.id(), "JOB_LEASED", job.status().name(), "RUNNING", workerId,
            "Leased by " + workerId + " (attempt " + job.attempts() + ")");
        log.info("status=LEASED");

        try {
            var handler = handlerRegistry.getHandler(job.type());
            if (handler.isEmpty()) {
                log.warn("status=NO_HANDLER");
                failJob(job, workerId, "No handler for type: " + job.type());
                return JobResult.failed(job, "No handler for type: " + job.type());
            }

            long start = System.nanoTime();
            try {
                handler.get().handle(job);
                long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                executionDuration.record(ms, TimeUnit.MILLISECONDS);
                completeJob(job, workerId, ms);
                return JobResult.completed(job);
            } catch (Exception e) {
                long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                executionDuration.record(ms, TimeUnit.MILLISECONDS);
                log.error("status=FAILED error={}", e.getMessage());
                failJob(job, workerId, e.getMessage());
                return JobResult.failed(job, e.getMessage());
            }
        } finally {
            MDC.clear();
        }
    }

    public int recoverExpiredLeases() {
        int count = jobRepository.releaseExpiredLeases();
        if (count > 0) {
            leasesRecovered.increment(count);
            eventService.record(null, "LEASE_EXPIRED_BATCH", "RUNNING", "PENDING", null,
                "Recovered " + count + " expired leases");
            log.info("action=recover_expired_leases recovered={}", count);
        }
        return count;
    }

    private void completeJob(Job job, String workerId, long durationMs) {
        jobRepository.markCompleted(job.id(), workerId);
        jobsCompleted.increment();
        eventService.record(job.id(), "JOB_COMPLETED", "RUNNING", "COMPLETED", job.lockedBy(),
            "Completed in " + durationMs + "ms");
        log.info("status=COMPLETED durationMs={}", durationMs);
    }

    private void failJob(Job job, String workerId, String errorMessage) {
        int attempts = job.attempts();
        int maxAttempts = job.maxAttempts();

        if (attempts >= maxAttempts) {
            jobRepository.moveToDeadLetter(job.id(), workerId, errorMessage);
            jobsDeadLettered.increment();
            eventService.record(job.id(), "JOB_DEAD_LETTERED", job.status().name(), "DEAD_LETTERED", workerId,
                "Failed after " + attempts + " attempts: " + errorMessage);
            log.warn("status=DEAD_LETTERED attempts={}/{}", attempts, maxAttempts);
            return;
        }

        Instant nextRetry = Instant.now().plus(retryPolicy.nextDelay(attempts));
        jobRepository.markFailed(job.id(), workerId, errorMessage, nextRetry);
        jobsFailed.increment();
        eventService.record(job.id(), "JOB_FAILED", job.status().name(), "RETRY_SCHEDULED", workerId,
            "Attempt " + attempts + "/" + maxAttempts + ": " + errorMessage);
        log.info("status=RETRY_SCHEDULED attempts={}/{} nextRetryAt={}", attempts, maxAttempts, nextRetry);
    }

    public record JobResult(UUID jobId, String jobType, String queueName, boolean processed,
                            boolean success, String error) {
        public static JobResult completed(Job job) {
            return new JobResult(job.id(), job.type(), job.queue(), true, true, null);
        }
        public static JobResult failed(Job job, String error) {
            return new JobResult(job.id(), job.type(), job.queue(), true, false, error);
        }
        public static JobResult notFound(String queueName) {
            return new JobResult(null, null, queueName, false, false, null);
        }
    }
}
