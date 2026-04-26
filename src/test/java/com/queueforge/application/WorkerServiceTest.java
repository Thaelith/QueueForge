package com.queueforge.application;

import com.queueforge.config.QueueForgeProperties;
import com.queueforge.domain.Job;
import com.queueforge.domain.JobPriority;
import com.queueforge.domain.JobStatus;
import com.queueforge.infrastructure.persistence.JobRepository;
import com.queueforge.worker.JobHandler;
import com.queueforge.worker.JobHandlerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobHandlerRegistry handlerRegistry;

    @Mock
    private JobEventService eventService;

    private QueueForgeProperties properties;
    private WorkerService workerService;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new QueueForgeProperties();
        properties.getWorker().setEnabled(true);
        properties.getWorker().setWorkerId("test-worker");
        properties.getWorker().setQueues(List.of("default"));
        properties.getWorker().setMaxJobsPerPoll(2);
        properties.getWorker().setLeaseDuration(Duration.ofSeconds(30));

        workerService = new WorkerService(jobRepository, handlerRegistry, eventService, properties, meterRegistry);
    }

    @Test
    void shouldProcessSuccessfulJob() throws Exception {
        Job job = createJob(UUID.randomUUID(), "example.log", JobStatus.PENDING, Instant.now(), 0, 3);
        when(jobRepository.leaseNextAvailable(eq("test-worker"), eq("default"), any()))
            .thenReturn(Optional.of(job))
            .thenReturn(Optional.empty());

        var handler = mock(JobHandler.class);
        when(handlerRegistry.getHandler("example.log")).thenReturn(Optional.of(handler));

        workerService.processJobs("test-worker", List.of("default"));

        verify(handler).handle(job);
        verify(jobRepository).markCompleted(job.id(), "test-worker");
        assertThat(meterRegistry.counter("queueforge.jobs.leased").count()).isEqualTo(1);
        assertThat(meterRegistry.counter("queueforge.jobs.completed").count()).isEqualTo(1);
        assertThat(meterRegistry.counter("queueforge.worker.polls").count()).isEqualTo(1);
    }

    @Test
    void shouldHandleHandlerExceptionAndScheduleRetry() throws Exception {
        Job job = createJob(UUID.randomUUID(), "flaky.task", JobStatus.PENDING, Instant.now(), 0, 3);
        when(jobRepository.leaseNextAvailable(eq("test-worker"), eq("default"), any()))
            .thenReturn(Optional.of(job))
            .thenReturn(Optional.empty());

        var handler = mock(JobHandler.class);
        doThrow(new RuntimeException("Boom!")).when(handler).handle(job);
        when(handlerRegistry.getHandler("flaky.task")).thenReturn(Optional.of(handler));

        workerService.processJobs("test-worker", List.of("default"));

        verify(jobRepository).markFailed(eq(job.id()), eq("test-worker"), eq("Boom!"), any());
        assertThat(meterRegistry.counter("queueforge.jobs.failed").count()).isEqualTo(1);
    }

    @Test
    void shouldMoveToDeadLetterWhenMaxAttemptsExceeded() throws Exception {
        Job job = createJob(UUID.randomUUID(), "broken.task", JobStatus.PENDING, Instant.now(), 2, 2);
        when(jobRepository.leaseNextAvailable(eq("test-worker"), eq("default"), any()))
            .thenReturn(Optional.of(job))
            .thenReturn(Optional.empty());

        var handler = mock(JobHandler.class);
        doThrow(new RuntimeException("Fatal!")).when(handler).handle(job);
        when(handlerRegistry.getHandler("broken.task")).thenReturn(Optional.of(handler));

        workerService.processJobs("test-worker", List.of("default"));

        verify(jobRepository).moveToDeadLetter(job.id(), "test-worker", "Fatal!");
        assertThat(meterRegistry.counter("queueforge.jobs.dead_lettered").count()).isEqualTo(1);
    }

    @Test
    void shouldFailWhenNoHandlerRegistered() throws Exception {
        Job job = createJob(UUID.randomUUID(), "unknown.type", JobStatus.PENDING, Instant.now(), 0, 3);
        when(jobRepository.leaseNextAvailable(eq("test-worker"), eq("default"), any()))
            .thenReturn(Optional.of(job))
            .thenReturn(Optional.empty());
        when(handlerRegistry.getHandler("unknown.type")).thenReturn(Optional.empty());

        workerService.processJobs("test-worker", List.of("default"));

        verify(jobRepository).markFailed(eq(job.id()), eq("test-worker"), contains("No handler"), any());
    }

    @Test
    void shouldRespectMaxJobsPerPoll() throws Exception {
        Job job1 = createJob(UUID.randomUUID(), "example.log", JobStatus.PENDING, Instant.now(), 0, 3);
        properties.getWorker().setMaxJobsPerPoll(1);

        when(jobRepository.leaseNextAvailable(eq("test-worker"), eq("default"), any()))
            .thenReturn(Optional.of(job1));
        var handler = mock(JobHandler.class);
        when(handlerRegistry.getHandler("example.log")).thenReturn(Optional.of(handler));

        workerService.processJobs("test-worker", List.of("default"));

        verify(jobRepository, times(1)).leaseNextAvailable(any(), any(), any());
        verify(jobRepository).markCompleted(job1.id(), "test-worker");
    }

    @Test
    void shouldDoNothingWhenNoJobsAvailable() throws Exception {
        when(jobRepository.leaseNextAvailable(eq("test-worker"), eq("default"), any()))
            .thenReturn(Optional.empty());

        workerService.processJobs("test-worker", List.of("default"));

        assertThat(meterRegistry.counter("queueforge.jobs.leased").count()).isEqualTo(0);
    }

    @Test
    void shouldRecoverExpiredLeases() {
        when(jobRepository.releaseExpiredLeases()).thenReturn(3);

        int recovered = workerService.recoverExpiredLeases();

        assertThat(recovered).isEqualTo(3);
        assertThat(meterRegistry.counter("queueforge.jobs.recovered").count()).isEqualTo(3);
    }

    private Job createJob(UUID id, String type, JobStatus status, Instant runAt, int attempts, int maxAttempts) {
        return new Job(id, "default", type, null, status, JobPriority.NORMAL, runAt,
            attempts, maxAttempts, null, null, null, null, Instant.now(), Instant.now(), null, null);
    }
}
