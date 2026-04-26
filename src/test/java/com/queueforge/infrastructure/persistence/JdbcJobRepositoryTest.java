package com.queueforge.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class JdbcJobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSaveAndFindJob() throws Exception {
        UUID id = UUID.randomUUID();
        JsonNode payload = objectMapper.readTree("{\"key\":\"value\"}");
        Job job = new Job(
            id, "test-queue", "test.type", payload, JobStatus.PENDING,
            JobPriority.HIGH, Instant.now(), 0, 5, null, null,
            null, null, Instant.now(), Instant.now(), null, null
        );

        jobRepository.save(job);

        Job found = jobRepository.findById(id).orElseThrow();
        assertThat(found.queue()).isEqualTo("test-queue");
        assertThat(found.type()).isEqualTo("test.type");
        assertThat(found.status()).isEqualTo(JobStatus.PENDING);
        assertThat(found.priority()).isEqualTo(JobPriority.HIGH);
        assertThat(found.payload().get("key").asText()).isEqualTo("value");
    }

    @Test
    void shouldFindByIdempotencyKey() throws Exception {
        UUID id = UUID.randomUUID();
        JsonNode payload = objectMapper.readTree("{\"key\":\"value\"}");
        Job job = new Job(
            id, "default", "type", payload, JobStatus.PENDING,
            JobPriority.NORMAL, Instant.now(), 0, 3, null, null,
            "idem-123", null, Instant.now(), Instant.now(), null, null
        );

        jobRepository.save(job);

        assertThat(jobRepository.findByIdempotencyKey("idem-123")).isPresent();
        assertThat(jobRepository.findByIdempotencyKey("nonexistent")).isEmpty();
    }

    @Test
    void shouldListJobsWithPaginationAndFiltering() throws Exception {
        for (int i = 0; i < 5; i++) {
            UUID id = UUID.randomUUID();
            JsonNode payload = objectMapper.readTree("{\"i\":" + i + "}");
            Job job = new Job(
                id, "list-queue", "list.type", payload, JobStatus.PENDING,
                JobPriority.NORMAL, Instant.now(), 0, 3, null, null,
                null, null, Instant.now(), Instant.now(), null, null
            );
            jobRepository.save(job);
        }

        Page<Job> page = jobRepository.findAll(
            "list-queue", JobStatus.PENDING, null,
            PageRequest.of(0, 2, Sort.by("createdAt").descending())
        );
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(5);
    }
}
