package com.queueforge.application;

import com.queueforge.api.dto.DashboardSummaryResponse;
import com.queueforge.api.dto.QueueStatsResponse;
import com.queueforge.api.dto.WorkerSummaryResponse;
import com.queueforge.infrastructure.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardQueryService {
    private final JobRepository jobRepository;

    public DashboardQueryService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public DashboardSummaryResponse getSummary() {
        Map<String, Long> counts = jobRepository.countByStatus();

        long pending = counts.getOrDefault("PENDING", 0L);
        long running = counts.getOrDefault("RUNNING", 0L);
        long completed = counts.getOrDefault("COMPLETED", 0L);
        long retryScheduled = counts.getOrDefault("RETRY_SCHEDULED", 0L);
        long deadLettered = counts.getOrDefault("DEAD_LETTERED", 0L);
        long cancelled = counts.getOrDefault("CANCELLED", 0L);
        long total = pending + running + completed + retryScheduled + deadLettered + cancelled;

        var stats = jobRepository.queueStats();
        Map<String, Long> jobsByQueue = new LinkedHashMap<>();
        for (var row : stats) {
            jobsByQueue.put((String) row.get("queue_name"), ((Number) row.get("total")).longValue());
        }

        int activeWorkers = jobRepository.workerStats().size();
        var lastHour = jobRepository.countLastHour();
        long completedLastHour = lastHour.getOrDefault("completedLastHour", 0L);
        long failedLastHour = lastHour.getOrDefault("failedLastHour", 0L);

        return new DashboardSummaryResponse(
            total, pending, running, completed, retryScheduled, deadLettered, cancelled,
            activeWorkers, completedLastHour, failedLastHour, jobsByQueue
        );
    }

    public QueueStatsResponse getQueueStats() {
        var rows = jobRepository.queueStats();
        var list = new ArrayList<QueueStatsResponse.QueueStat>();

        for (var row : rows) {
            list.add(new QueueStatsResponse.QueueStat(
                (String) row.get("queue_name"),
                toLong(row, "pending"),
                toLong(row, "running"),
                toLong(row, "retry_scheduled"),
                toLong(row, "completed"),
                toLong(row, "dead_lettered"),
                toLong(row, "cancelled"),
                toLong(row, "total"),
                toInstant(row, "oldest_pending_at"),
                toInstant(row, "newest_created_at")
            ));
        }
        return new QueueStatsResponse(list);
    }

    public WorkerSummaryResponse getWorkers() {
        var rows = jobRepository.workerStats();
        var list = new ArrayList<WorkerSummaryResponse.WorkerInfo>();

        for (var row : rows) {
            long running = toLong(row, "running_count");
            String status;
            Instant lastSeen = toInstant(row, "last_seen_at");
            if (running > 0) status = "ACTIVE";
            else if (lastSeen != null && lastSeen.isBefore(Instant.now().minusSeconds(120))) status = "STALE";
            else if (lastSeen != null) status = "IDLE";
            else status = "UNKNOWN";

            list.add(new WorkerSummaryResponse.WorkerInfo(
                (String) row.get("worker_id"),
                status,
                running,
                toLong(row, "completed_count"),
                toLong(row, "failed_count"),
                lastSeen
            ));
        }
        return new WorkerSummaryResponse(list);
    }

    private long toLong(Map<String, Object> row, String key) {
        Object val = row.get(key);
        return val != null ? ((Number) val).longValue() : 0L;
    }

    private Instant toInstant(Map<String, Object> row, String key) {
        Object val = row.get(key);
        if (val instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (val instanceof Instant i) return i;
        return null;
    }
}
