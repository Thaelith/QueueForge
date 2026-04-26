package com.queueforge.api;

import com.queueforge.application.WorkerService;
import com.queueforge.config.QueueForgeProperties;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {
    private final WorkerService workerService;
    private final QueueForgeProperties properties;

    public WorkerController(WorkerService workerService, QueueForgeProperties properties) {
        this.workerService = workerService;
        this.properties = properties;
    }

    @GetMapping("/config")
    @Operation(summary = "Get current worker configuration")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
            "workerEnabled", properties.getWorker().isEnabled(),
            "workerId", properties.getWorker().getWorkerId(),
            "queues", properties.getWorker().getQueues(),
            "pollIntervalMs", properties.getWorker().getPollIntervalMs(),
            "leaseDurationSeconds", properties.getWorker().getLeaseDuration().toSeconds(),
            "maxJobsPerPoll", properties.getWorker().getMaxJobsPerPoll(),
            "recoveryEnabled", properties.getRecovery().isEnabled(),
            "recoveryIntervalMs", properties.getRecovery().getIntervalMs()
        ));
    }

    @PostMapping("/{workerId}/run-once")
    @Operation(summary = "Trigger one full poll cycle for a worker")
    public ResponseEntity<Map<String, Object>> runOnce(@PathVariable String workerId) {
        List<String> queues = properties.getWorker().getQueues();
        long start = System.currentTimeMillis();
        workerService.processJobs(workerId, queues);
        long durationMs = System.currentTimeMillis() - start;

        return ResponseEntity.ok(Map.of(
            "workerId", workerId,
            "queues", queues,
            "durationMs", durationMs
        ));
    }

    @PostMapping("/{workerId}/lease")
    @Operation(summary = "Manually trigger single lease-and-execute cycle")
    public ResponseEntity<Map<String, Object>> leaseAndExecute(
        @PathVariable String workerId,
        @RequestParam(defaultValue = "default") String queue
    ) {
        var result = workerService.leaseAndExecute(workerId, queue);

        if (!result.processed()) {
            return ResponseEntity.ok(Map.of("leased", false));
        }

        return ResponseEntity.ok(Map.of(
            "leased", true,
            "jobId", result.jobId().toString(),
            "jobType", result.jobType(),
            "success", result.success(),
            "error", result.error() != null ? result.error() : "none"
        ));
    }

    @PostMapping("/recover-leases")
    @Operation(summary = "Recover expired job leases")
    public ResponseEntity<Map<String, Object>> recoverExpiredLeases() {
        int count = workerService.recoverExpiredLeases();
        return ResponseEntity.ok(Map.of("recovered", count));
    }
}
