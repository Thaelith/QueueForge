package com.queueforge.worker;

import com.queueforge.application.WorkerService;
import com.queueforge.config.QueueForgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackgroundWorkerRunner {
    private static final Logger log = LoggerFactory.getLogger(BackgroundWorkerRunner.class);

    private final WorkerService workerService;
    private final QueueForgeProperties properties;

    public BackgroundWorkerRunner(WorkerService workerService, QueueForgeProperties properties) {
        this.workerService = workerService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (properties.getWorker().isEnabled()) {
            log.info("Background worker enabled. workerId={} queues={} pollIntervalMs={}",
                properties.getWorker().getWorkerId(),
                properties.getWorker().getQueues(),
                properties.getWorker().getPollIntervalMs());
        } else {
            log.info("Background worker disabled. Use API endpoints to trigger manually.");
        }
    }

    @Scheduled(fixedDelayString = "${queueforge.worker.poll-interval-ms:1000}")
    public void pollAndProcess() {
        if (!properties.getWorker().isEnabled()) {
            return;
        }

        try {
            workerService.processJobs(
                properties.getWorker().getWorkerId(),
                properties.getWorker().getQueues());
        } catch (Exception e) {
            log.error("Worker poll cycle failed: error={}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelayString = "${queueforge.recovery.interval-ms:30000}")
    public void recoverExpiredLeases() {
        if (!properties.getRecovery().isEnabled()) {
            return;
        }

        try {
            int count = workerService.recoverExpiredLeases();
            if (count > 0) {
                log.info("Recovery cycle: {} expired leases recovered", count);
            }
        } catch (Exception e) {
            log.error("Recovery cycle failed: error={}", e.getMessage(), e);
        }
    }
}
