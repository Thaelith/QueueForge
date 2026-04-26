package com.queueforge.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.queueforge.domain.*;
import com.queueforge.domain.exceptions.DuplicateIdempotencyKeyException;
import com.queueforge.infrastructure.persistence.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class SubmitJobUseCase {
    private static final Logger log = LoggerFactory.getLogger(SubmitJobUseCase.class);

    private final JobRepository jobRepository;
    private final JobEventService eventService;

    public SubmitJobUseCase(JobRepository jobRepository, JobEventService eventService) {
        this.jobRepository = jobRepository;
        this.eventService = eventService;
    }

    public Job submit(String queue, String type, JsonNode payload, JobPriority priority, Instant runAt, Integer maxAttempts, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            jobRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
                log.info("Idempotency key '{}' already exists, returning existing job id={}", idempotencyKey, existing.id());
                throw new DuplicateIdempotencyKeyException("Job with idempotency key already exists: " + idempotencyKey);
            });
        }

        Job job = new Job(
            UUID.randomUUID(),
            queue != null && !queue.isBlank() ? queue : "default",
            type,
            payload,
            JobStatus.PENDING,
            priority != null ? priority : JobPriority.NORMAL,
            runAt != null ? runAt : Instant.now(),
            0,
            maxAttempts != null ? maxAttempts : 3,
            null,
            null,
            idempotencyKey,
            null,
            Instant.now(),
            Instant.now(),
            null,
            null
        );

        Job saved = jobRepository.save(job);
        eventService.record(saved.id(), "JOB_CREATED", null, saved.status().name(), null,
            "Job submitted: type=" + saved.type() + " queue=" + saved.queue());
        log.info("Job submitted: id={}, type={}, queue={}", saved.id(), saved.type(), saved.queue());
        return saved;
    }
}
