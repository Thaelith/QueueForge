package com.queueforge.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueforge.TestcontainersConfiguration;
import com.queueforge.domain.Job;
import com.queueforge.domain.JobPriority;
import com.queueforge.domain.JobStatus;
import com.queueforge.infrastructure.persistence.JobRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnDashboardSummary() throws Exception {
        submitJob("default", "example.log");

        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalJobs").value(1))
            .andExpect(jsonPath("$.pendingJobs").value(1));
    }

    @Test
    void shouldReturnQueueStats() throws Exception {
        submitJob("emails", "example.email");
        submitJob("emails", "example.email");

        mockMvc.perform(get("/api/v1/admin/queues/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queues").isArray())
            .andExpect(jsonPath("$.queues[?(@.queueName=='emails')].total").value(2));
    }

    @Test
    void shouldReturnWorkersAfterProcessing() throws Exception {
        UUID jobId = submitJob("default", "example.log");

        jdbcTemplate.update(
            "UPDATE jobs SET status='RUNNING', locked_by='worker-1', locked_until=now()+interval '1 hour', updated_at=now() WHERE id=?",
            jobId);

        mockMvc.perform(get("/api/v1/admin/workers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workers").isArray())
            .andExpect(jsonPath("$.workers[?(@.workerId=='worker-1')]").exists());
    }

    @Test
    void shouldRequeueDeadLetteredJob() throws Exception {
        UUID jobId = submitJob("default", "example.log");
        jdbcTemplate.update("UPDATE jobs SET status='DEAD_LETTERED', dead_lettered_at=now() WHERE id=?", jobId);

        mockMvc.perform(post("/api/v1/admin/jobs/" + jobId + "/requeue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resetAttempts\":true,\"reason\":\"test\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.newStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/admin/jobs/" + jobId + "/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[-1].eventType").value("JOB_REQUEUED"));
    }

    @Test
    void shouldRejectRequeueOnInvalidStatus() throws Exception {
        UUID jobId = submitJob("default", "example.log");

        mockMvc.perform(post("/api/v1/admin/jobs/" + jobId + "/requeue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldCancelPendingJob() throws Exception {
        UUID jobId = submitJob("default", "example.log");

        mockMvc.perform(post("/api/v1/admin/jobs/" + jobId + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.newStatus").value("CANCELLED"));

        Job job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.status()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void shouldRetryNowRetryScheduledJob() throws Exception {
        UUID jobId = submitJob("default", "example.log");
        jdbcTemplate.update("UPDATE jobs SET status='RETRY_SCHEDULED', run_at=now()+interval '1 hour' WHERE id=?", jobId);

        mockMvc.perform(post("/api/v1/admin/jobs/" + jobId + "/retry-now")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resetAttempts\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.newStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/admin/jobs/" + jobId + "/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[-1].eventType").value("JOB_RETRY_NOW"));
    }

    @Test
    void shouldReturnJobEventsAfterSubmission() throws Exception {
        UUID jobId = submitJob("default", "example.log");

        mockMvc.perform(get("/api/v1/admin/jobs/" + jobId + "/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].eventType").value("JOB_CREATED"));
    }

    @Test
    void shouldReturn404ForUnknownJobEvents() throws Exception {
        mockMvc.perform(get("/api/v1/admin/jobs/00000000-0000-0000-0000-000000000000/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    private UUID submitJob(String queue, String type) throws Exception {
        UUID id = UUID.randomUUID();
        Job job = new Job(
            id, queue, type, objectMapper.readTree("{\"msg\":\"test\"}"),
            JobStatus.PENDING, JobPriority.NORMAL, Instant.now(),
            0, 3, null, null, null, null,
            Instant.now(), Instant.now(), null, null
        );
        jobRepository.save(job);
        return id;
    }
}
