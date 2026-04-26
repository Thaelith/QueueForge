package com.queueforge.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueforge.TestcontainersConfiguration;
import com.queueforge.config.QueueForgeProperties;
import com.queueforge.domain.Job;
import com.queueforge.domain.JobPriority;
import com.queueforge.domain.JobStatus;
import com.queueforge.infrastructure.persistence.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class WorkerServiceIntegrationTest {

    @Autowired
    private WorkerService workerService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QueueForgeProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        properties.getWorker().setEnabled(false);
    }

    @Test
    void shouldProcessJobToCompletion() throws Exception {
        UUID jobId = submitJob("default", "example.log", 3);

        workerService.processJobs("w1", List.of("default"));

        Job job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.lockedBy()).isNull();
        assertThat(job.lockedUntil()).isNull();
    }

    @Test
    void shouldProcessMultipleJobsWithoutDuplication() throws Exception {
        properties.getWorker().setMaxJobsPerPoll(10);

        for (int i = 0; i < 5; i++) {
            submitJob("default", "example.log", 3);
        }

        workerService.processJobs("w1", List.of("default"));

        List<Integer> counts = jdbcTemplate.queryForList(
            "SELECT COUNT(*) as cnt FROM jobs WHERE status = 'COMPLETED'", Integer.class);
        assertThat(counts.get(0)).isEqualTo(5);
    }

    @Test
    void shouldRetryFailingJobAndEventuallyDeadLetter() throws Exception {
        UUID jobId = submitJob("default", "example.fail", 2);

        workerService.processJobs("w1", List.of("default"));

        Job afterFirst = jobRepository.findById(jobId).orElseThrow();
        assertThat(afterFirst.status()).isEqualTo(JobStatus.RETRY_SCHEDULED);
        assertThat(afterFirst.lastError()).contains("Simulated failure");

        jdbcTemplate.update("UPDATE jobs SET run_at = now() - interval '1 second' WHERE id = ?", jobId);

        workerService.processJobs("w1", List.of("default"));

        Job afterSecond = jobRepository.findById(jobId).orElseThrow();
        assertThat(afterSecond.status()).isEqualTo(JobStatus.DEAD_LETTERED);
        assertThat(afterSecond.deadLetteredAt()).isNotNull();
    }

    @Test
    void shouldHandleJobWithNoMatchingHandler() throws Exception {
        UUID jobId = submitJob("default", "no.such.handler", 3);

        workerService.processJobs("w1", List.of("default"));

        Job job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.status()).isEqualTo(JobStatus.RETRY_SCHEDULED);
        assertThat(job.lastError()).contains("No handler");
    }

    @Test
    void shouldNotLeaseSameJobToDifferentWorkers() throws Exception {
        int jobCount = 10;
        for (int i = 0; i < jobCount; i++) {
            submitJob("default", "example.log", 3);
        }

        workerService.processJobs("w1", List.of("default"));

        workerService.processJobs("w2", List.of("default"));

        int runningCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM jobs WHERE status = 'RUNNING'", Integer.class);
        assertThat(runningCount).isZero();

        int completedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM jobs WHERE status = 'COMPLETED'", Integer.class);
        assertThat(completedCount).isEqualTo(jobCount);
    }

    @Test
    void shouldRecoverExpiredLeases() throws Exception {
        UUID jobId = submitJob("default", "example.log", 3);

        jdbcTemplate.update(
            "UPDATE jobs SET status = 'RUNNING', locked_by = 'dead-worker', locked_until = now() - interval '1 hour', updated_at = now() WHERE id = ?",
            jobId);

        int recovered = workerService.recoverExpiredLeases();

        assertThat(recovered).isEqualTo(1);

        Job job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.lockedBy()).isNull();
        assertThat(job.lastError()).contains("expired");
    }

    @Test
    void shouldProcessJobsFromMultipleQueues() throws Exception {
        UUID q1Job = submitJob("queue-one", "example.log", 3);
        UUID q2Job = submitJob("queue-two", "example.log", 3);
        properties.getWorker().setMaxJobsPerPoll(10);

        workerService.processJobs("w1", List.of("queue-one", "queue-two"));

        assertThat(jobRepository.findById(q1Job).orElseThrow().status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(jobRepository.findById(q2Job).orElseThrow().status()).isEqualTo(JobStatus.COMPLETED);
    }

    private UUID submitJob(String queue, String type, int maxAttempts) throws Exception {
        UUID id = UUID.randomUUID();
        Job job = new Job(
            id, queue, type, objectMapper.readTree("{\"msg\":\"test\"}"),
            JobStatus.PENDING, JobPriority.NORMAL, Instant.now(),
            0, maxAttempts,
            null, null, null, null,
            Instant.now(), Instant.now(), null, null
        );
        jobRepository.save(job);
        return id;
    }
}
