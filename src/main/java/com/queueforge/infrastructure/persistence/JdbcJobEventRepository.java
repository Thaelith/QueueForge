package com.queueforge.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueforge.domain.JobEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcJobEventRepository implements JobEventRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EventRowMapper rowMapper;

    public JdbcJobEventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.rowMapper = new EventRowMapper(objectMapper);
    }

    @Override
    public void save(JobEvent event) {
        String sql = """
            INSERT INTO job_events (job_id, event_type, message, metadata, created_at)
            VALUES (?, ?, ?, ?::jsonb, ?)
            """;
        jdbcTemplate.update(sql,
            event.jobId(),
            event.eventType(),
            event.message(),
            toJsonString(event.metadata()),
            Timestamp.from(event.createdAt())
        );
    }

    @Override
    public List<JobEvent> findByJobId(UUID jobId) {
        String sql = "SELECT * FROM job_events WHERE job_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, rowMapper, jobId);
    }

    @Override
    public List<JobEvent> findRecent(int limit) {
        String sql = "SELECT * FROM job_events ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, rowMapper, limit);
    }

    private String toJsonString(JsonNode node) {
        if (node == null) return null;
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static class EventRowMapper implements RowMapper<JobEvent> {
        private final ObjectMapper objectMapper;

        EventRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public JobEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new JobEvent(
                rs.getLong("id"),
                UUID.fromString(rs.getString("job_id")),
                rs.getString("event_type"),
                null,
                null,
                null,
                rs.getString("message"),
                readJson(rs.getString("metadata")),
                toInstant(rs.getTimestamp("created_at"))
            );
        }

        private JsonNode readJson(String json) {
            if (json == null) return null;
            try {
                return objectMapper.readTree(json);
            } catch (JsonProcessingException e) {
                return null;
            }
        }

        private static Instant toInstant(Timestamp ts) {
            return ts != null ? ts.toInstant() : null;
        }
    }
}
