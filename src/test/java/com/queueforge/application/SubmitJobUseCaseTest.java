package com.queueforge.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueforge.domain.Job;
import com.queueforge.domain.JobPriority;
import com.queueforge.domain.JobStatus;
import com.queueforge.domain.exceptions.DuplicateIdempotencyKeyException;
import com.queueforge.infrastructure.persistence.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SubmitJobUseCaseTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobEventService eventService;

    private SubmitJobUseCase useCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        useCase = new SubmitJobUseCase(jobRepository, eventService);
    }

    @Test
    void shouldSubmitJobWithDefaults() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"msg\":\"hello\"}");
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = useCase.submit("default", "test", payload, null, null, null, null);

        assertThat(result.status()).isEqualTo(JobStatus.PENDING);
        assertThat(result.priority()).isEqualTo(JobPriority.NORMAL);
        assertThat(result.maxAttempts()).isEqualTo(3);
        assertThat(result.queue()).isEqualTo("default");
        assertThat(result.type()).isEqualTo("test");
    }

    @Test
    void shouldSubmitJobWithExplicitValues() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"msg\":\"hello\"}");
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant scheduled = Instant.parse("2026-01-01T00:00:00Z");
        Job result = useCase.submit("emails", "email.send", payload, JobPriority.HIGH, scheduled, 5, "idem-1");

        assertThat(result.priority()).isEqualTo(JobPriority.HIGH);
        assertThat(result.maxAttempts()).isEqualTo(5);
        assertThat(result.queue()).isEqualTo("emails");
        assertThat(result.runAt()).isEqualTo(scheduled);
        assertThat(result.idempotencyKey()).isEqualTo("idem-1");
    }

    @Test
    void shouldRejectDuplicateIdempotencyKey() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"msg\":\"hello\"}");
        Job existing = new Job(
            UUID.randomUUID(), "default", "test", payload, JobStatus.PENDING,
            JobPriority.NORMAL, Instant.now(), 0, 3, null, null,
            "dup-key", null, Instant.now(), Instant.now(), null, null
        );
        when(jobRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.submit("default", "test", payload, null, null, null, "dup-key"))
            .isInstanceOf(DuplicateIdempotencyKeyException.class)
            .hasMessageContaining("dup-key");
    }
}
