package com.queueforge.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueforge.TestcontainersConfiguration;
import com.queueforge.api.dto.SubmitJobRequest;
import com.queueforge.domain.JobPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM job_events");
        jdbcTemplate.update("DELETE FROM jobs");
    }

    @Test
    void shouldSubmitJobAndRetrieveIt() throws Exception {
        SubmitJobRequest request = new SubmitJobRequest(
            "default",
            "email.send",
            objectMapper.readTree("{\"to\":\"test@example.com\"}"),
            JobPriority.HIGH,
            5,
            null,
            null
        );

        String response = mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.queue").value("default"))
            .andExpect(jsonPath("$.type").value("email.send"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.priority").value(100))
            .andExpect(jsonPath("$.maxAttempts").value(5))
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/jobs/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.type").value("email.send"));
    }

    @Test
    void shouldReturnNotFoundForUnknownJob() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnConflictForDuplicateIdempotencyKey() throws Exception {
        SubmitJobRequest request = new SubmitJobRequest(
            "default",
            "test",
            objectMapper.readTree("{\"key\":\"value\"}"),
            null,
            null,
            null,
            "unique-key-123"
        );

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldListJobs() throws Exception {
        SubmitJobRequest request = new SubmitJobRequest(
            "list-test",
            "list.type",
            objectMapper.readTree("{\"key\":\"value\"}"),
            null,
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/jobs?queue=list-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].queue").value("list-test"));
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        String invalidJson = "{\"queue\":\"\",\"type\":\"test\",\"payload\":null}";

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }
}
