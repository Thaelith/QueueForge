package com.queueforge.api;

import com.queueforge.api.dto.*;
import com.queueforge.application.DashboardQueryService;
import com.queueforge.application.JobAdminService;
import com.queueforge.application.JobEventService;
import com.queueforge.domain.JobEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Queue administration and inspection")
public class AdminController {
    private final DashboardQueryService dashboardService;
    private final JobAdminService jobAdminService;
    private final JobEventService eventService;

    public AdminController(DashboardQueryService dashboardService,
                           JobAdminService jobAdminService,
                           JobEventService eventService) {
        this.dashboardService = dashboardService;
        this.jobAdminService = jobAdminService;
        this.eventService = eventService;
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Dashboard overview summary")
    public ResponseEntity<DashboardSummaryResponse> dashboardSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/queues/stats")
    @Operation(summary = "Per-queue statistics")
    public ResponseEntity<QueueStatsResponse> queueStats() {
        return ResponseEntity.ok(dashboardService.getQueueStats());
    }

    @GetMapping("/workers")
    @Operation(summary = "Worker visibility derived from job locks")
    public ResponseEntity<WorkerSummaryResponse> workers() {
        return ResponseEntity.ok(dashboardService.getWorkers());
    }

    @GetMapping("/jobs/{jobId}/events")
    @Operation(summary = "Job event timeline")
    public ResponseEntity<List<JobEventResponse>> jobEvents(@PathVariable UUID jobId) {
        List<JobEvent> events = eventService.getTimeline(jobId);
        var response = events.stream()
            .map(e -> new JobEventResponse(e.id(), e.eventType(), e.fromStatus(), e.toStatus(),
                e.workerId(), e.createdAt(), e.message()))
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jobs/{jobId}/requeue")
    @Operation(summary = "Requeue a dead-lettered or cancelled job")
    public ResponseEntity<AdminActionResponse> requeue(
        @PathVariable UUID jobId,
        @RequestBody(required = false) RequeueJobRequest request
    ) {
        boolean resetAttempts = request != null && request.resetAttempts();
        String reason = request != null ? request.reason() : null;
        AdminActionResponse result = jobAdminService.requeue(jobId, resetAttempts, reason);

        if (!result.success()) {
            return ResponseEntity.status(409).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/jobs/{jobId}/cancel")
    @Operation(summary = "Cancel a pending, retry-scheduled, or running job")
    public ResponseEntity<AdminActionResponse> cancel(@PathVariable UUID jobId) {
        AdminActionResponse result = jobAdminService.cancel(jobId);

        if (!result.success()) {
            return ResponseEntity.status(409).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/jobs/{jobId}/retry-now")
    @Operation(summary = "Immediately schedule a retry for retry-scheduled or dead-lettered job")
    public ResponseEntity<AdminActionResponse> retryNow(
        @PathVariable UUID jobId,
        @RequestBody(required = false) RetryNowRequest request
    ) {
        boolean resetAttempts = request != null && request.resetAttempts();
        AdminActionResponse result = jobAdminService.retryNow(jobId, resetAttempts);

        if (!result.success()) {
            return ResponseEntity.status(409).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
