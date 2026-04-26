package com.queueforge.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueforge.domain.Job;
import com.queueforge.domain.JobPriority;
import com.queueforge.domain.JobStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcJobRepository implements JobRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final JobRowMapper rowMapper;

    public JdbcJobRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.rowMapper = new JobRowMapper(objectMapper);
    }

    @Override
    public Job save(Job job) {
        String sql = """
            INSERT INTO jobs (id, queue_name, type, payload, status, priority, run_at, attempts, max_attempts,
                              locked_by, locked_until, idempotency_key, last_error, created_at, updated_at,
                              completed_at, dead_lettered_at)
            VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                queue_name = EXCLUDED.queue_name,
                type = EXCLUDED.type,
                payload = EXCLUDED.payload,
                status = EXCLUDED.status,
                priority = EXCLUDED.priority,
                run_at = EXCLUDED.run_at,
                attempts = EXCLUDED.attempts,
                max_attempts = EXCLUDED.max_attempts,
                locked_by = EXCLUDED.locked_by,
                locked_until = EXCLUDED.locked_until,
                idempotency_key = EXCLUDED.idempotency_key,
                last_error = EXCLUDED.last_error,
                updated_at = EXCLUDED.updated_at,
                completed_at = EXCLUDED.completed_at,
                dead_lettered_at = EXCLUDED.dead_lettered_at
            """;

        jdbcTemplate.update(sql,
            job.id(),
            job.queue(),
            job.type(),
            toJsonString(job.payload()),
            job.status().name(),
            job.priority().getValue(),
            Timestamp.from(job.runAt()),
            job.attempts(),
            job.maxAttempts(),
            job.lockedBy(),
            job.lockedUntil() != null ? Timestamp.from(job.lockedUntil()) : null,
            job.idempotencyKey(),
            job.lastError(),
            Timestamp.from(job.createdAt()),
            Timestamp.from(job.updatedAt()),
            job.completedAt() != null ? Timestamp.from(job.completedAt()) : null,
            job.deadLetteredAt() != null ? Timestamp.from(job.deadLetteredAt()) : null
        );

        return job;
    }

    @Override
    public Optional<Job> findById(UUID id) {
        String sql = "SELECT * FROM jobs WHERE id = ?";
        List<Job> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<Job> findByIdempotencyKey(String idempotencyKey) {
        String sql = "SELECT * FROM jobs WHERE idempotency_key = ?";
        List<Job> results = jdbcTemplate.query(sql, rowMapper, idempotencyKey);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Page<Job> findAll(String queue, JobStatus status, String type, Pageable pageable) {
        StringBuilder sql = new StringBuilder("SELECT * FROM jobs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (queue != null && !queue.isBlank()) {
            sql.append(" AND queue_name = ?");
            params.add(queue);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            params.add(type);
        }

        String countSql = sql.toString().replace("SELECT *", "SELECT COUNT(*)");
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        if (pageable.getSort().isSorted()) {
            sql.append(" ORDER BY ");
            List<String> sorts = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                String property = switch (order.getProperty()) {
                    case "createdAt" -> "created_at";
                    case "updatedAt" -> "updated_at";
                    case "runAt", "scheduledAt" -> "run_at";
                    case "priority" -> "priority";
                    case "status" -> "status";
                    case "type" -> "type";
                    case "queue" -> "queue_name";
                    default -> "created_at";
                };
                sorts.add(property + " " + order.getDirection().name());
            });
            sql.append(String.join(", ", sorts));
        } else {
            sql.append(" ORDER BY created_at DESC");
        }

        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add((int) pageable.getOffset());

        List<Job> content = jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    @Transactional
    public Optional<Job> leaseNextAvailable(String workerId, String queueName, Duration leaseDuration) {
        String sql = """
            WITH selected AS (
                SELECT id
                FROM jobs
                WHERE status IN ('PENDING', 'RETRY_SCHEDULED')
                  AND run_at <= now()
                  AND queue_name = ?
                  AND (locked_until IS NULL OR locked_until < now())
                ORDER BY priority DESC, created_at ASC
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            UPDATE jobs SET
                status = 'RUNNING',
                locked_by = ?,
                locked_until = now() + ?::interval,
                attempts = attempts + 1,
                updated_at = now()
            FROM selected
            WHERE jobs.id = selected.id
            RETURNING jobs.*
            """;

        try {
            Job job = jdbcTemplate.queryForObject(sql, rowMapper, queueName, workerId, leaseDuration.getSeconds() + " seconds");
            if (job != null && job.status() == JobStatus.RUNNING) {
                return Optional.of(job);
            }
            return Optional.empty();
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void markCompleted(UUID jobId, String workerId) {
        String sql = """
            UPDATE jobs
            SET status = 'COMPLETED',
                locked_by = NULL,
                locked_until = NULL,
                completed_at = now(),
                updated_at = now()
            WHERE id = ?
              AND locked_by = ?
              AND status = 'RUNNING'
            """;
        jdbcTemplate.update(sql, jobId, workerId);
    }

    @Override
    public void markFailed(UUID jobId, String workerId, String errorMessage, Instant nextRetryAt) {
        String sql = """
            UPDATE jobs
            SET status = 'RETRY_SCHEDULED',
                locked_by = NULL,
                locked_until = NULL,
                last_error = ?,
                run_at = ?,
                updated_at = now()
            WHERE id = ?
              AND locked_by = ?
              AND status = 'RUNNING'
            """;
        jdbcTemplate.update(sql, errorMessage, Timestamp.from(nextRetryAt), jobId, workerId);
    }

    @Override
    public void moveToDeadLetter(UUID jobId, String workerId, String errorMessage) {
        String sql = """
            UPDATE jobs
            SET status = 'DEAD_LETTERED',
                locked_by = NULL,
                locked_until = NULL,
                last_error = ?,
                dead_lettered_at = now(),
                updated_at = now()
            WHERE id = ?
              AND locked_by = ?
              AND status = 'RUNNING'
            """;
        jdbcTemplate.update(sql, errorMessage, jobId, workerId);
    }

    @Override
    public int releaseExpiredLeases() {
        String sql = """
            UPDATE jobs
            SET status = 'PENDING',
                locked_by = NULL,
                locked_until = NULL,
                last_error = 'Lease expired before completion',
                updated_at = now()
            WHERE status = 'RUNNING'
              AND locked_until < now()
            """;
        return jdbcTemplate.update(sql);
    }

    @Override
    public Map<String, Long> countByStatus() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT status, COUNT(*) as cnt FROM jobs GROUP BY status");
        Map<String, Long> result = new LinkedHashMap<>();
        for (var row : rows) {
            result.put((String) row.get("status"), ((Number) row.get("cnt")).longValue());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> queueStats() {
        return jdbcTemplate.queryForList("""
            SELECT
                queue_name,
                COUNT(*) FILTER (WHERE status = 'PENDING') AS pending,
                COUNT(*) FILTER (WHERE status = 'RUNNING') AS running,
                COUNT(*) FILTER (WHERE status = 'RETRY_SCHEDULED') AS retry_scheduled,
                COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed,
                COUNT(*) FILTER (WHERE status = 'DEAD_LETTERED') AS dead_lettered,
                COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled,
                COUNT(*) AS total,
                MIN(run_at) FILTER (WHERE status = 'PENDING') AS oldest_pending_at,
                MAX(created_at) AS newest_created_at
            FROM jobs
            GROUP BY queue_name
            ORDER BY queue_name
            """);
    }

    @Override
    public List<Map<String, Object>> workerStats() {
        return jdbcTemplate.queryForList("""
            SELECT
                COALESCE(locked_by, 'unknown') AS worker_id,
                COUNT(*) FILTER (WHERE status = 'RUNNING') AS running_count,
                COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_count,
                COUNT(*) FILTER (WHERE status IN ('RETRY_SCHEDULED', 'DEAD_LETTERED')) AS failed_count,
                MAX(updated_at) AS last_seen_at
            FROM jobs
            WHERE locked_by IS NOT NULL
               OR status = 'RUNNING'
            GROUP BY locked_by
            ORDER BY last_seen_at DESC
            """);
    }

    @Override
    @Transactional
    public boolean requeueJob(UUID jobId, boolean resetAttempts) {
        String sql = "UPDATE jobs SET status = 'PENDING', locked_by = NULL, locked_until = NULL, " +
            "last_error = NULL, run_at = now(), updated_at = now()" +
            (resetAttempts ? ", attempts = 0" : "") +
            " WHERE id = ? AND status IN ('DEAD_LETTERED', 'CANCELLED', 'RETRY_SCHEDULED')";
        return jdbcTemplate.update(sql, jobId) > 0;
    }

    @Override
    @Transactional
    public boolean cancelJob(UUID jobId) {
        String sql = """
            UPDATE jobs SET status = 'CANCELLED', locked_by = NULL, locked_until = NULL,
            updated_at = now()
            WHERE id = ? AND status IN ('PENDING', 'RETRY_SCHEDULED', 'RUNNING')
            """;
        return jdbcTemplate.update(sql, jobId) > 0;
    }

    @Override
    @Transactional
    public boolean retryNow(UUID jobId, boolean resetAttempts) {
        String sql = "UPDATE jobs SET status = 'PENDING', run_at = now(), updated_at = now()" +
            (resetAttempts ? ", attempts = 0" : "") +
            " WHERE id = ? AND status IN ('RETRY_SCHEDULED', 'DEAD_LETTERED')";
        return jdbcTemplate.update(sql, jobId) > 0;
    }

    @Override
    @Transactional
    public int cleanupTerminalJobs(int completedRetentionDays, int cancelledRetentionDays, int batchSize) {
        String sql = """
            DELETE FROM jobs
            WHERE id IN (
                SELECT id FROM jobs
                WHERE (status = 'COMPLETED' AND updated_at < now() - make_interval(days => ?))
                   OR (status = 'CANCELLED' AND updated_at < now() - make_interval(days => ?))
                LIMIT ?
            )
            """;
        return jdbcTemplate.update(sql, completedRetentionDays, cancelledRetentionDays, batchSize);
    }

    @Override
    public Map<String, Long> countLastHour() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT
                COUNT(*) FILTER (WHERE status = 'COMPLETED' AND updated_at > now() - interval '1 hour') AS completed_last_hour,
                COUNT(*) FILTER (WHERE (status = 'RETRY_SCHEDULED' OR status = 'DEAD_LETTERED')
                    AND updated_at > now() - interval '1 hour') AS failed_last_hour
            FROM jobs
            """);
        Map<String, Long> result = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            var row = rows.get(0);
            result.put("completedLastHour", toLongVal(row.get("completed_last_hour")));
            result.put("failedLastHour", toLongVal(row.get("failed_last_hour")));
        }
        return result;
    }

    private long toLongVal(Object val) {
        return val != null ? ((Number) val).longValue() : 0L;
    }

    private String toJsonString(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON payload", e);
        }
    }

    private static class JobRowMapper implements RowMapper<Job> {
        private final ObjectMapper objectMapper;

        JobRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Job mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Job(
                UUID.fromString(rs.getString("id")),
                rs.getString("queue_name"),
                rs.getString("type"),
                readJson(rs.getString("payload")),
                JobStatus.valueOf(rs.getString("status")),
                JobPriority.fromValue(rs.getInt("priority")),
                toInstant(rs.getTimestamp("run_at")),
                rs.getInt("attempts"),
                rs.getInt("max_attempts"),
                rs.getString("locked_by"),
                toInstant(rs.getTimestamp("locked_until")),
                rs.getString("idempotency_key"),
                rs.getString("last_error"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")),
                toInstant(rs.getTimestamp("completed_at")),
                toInstant(rs.getTimestamp("dead_lettered_at"))
            );
        }

        private JsonNode readJson(String json) {
            if (json == null) return null;
            try {
                return objectMapper.readTree(json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize JSON payload", e);
            }
        }

        private static Instant toInstant(Timestamp ts) {
            return ts != null ? ts.toInstant() : null;
        }
    }
}
