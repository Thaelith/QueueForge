package com.queueforge.application;

import com.queueforge.config.QueueForgeProperties;
import com.queueforge.infrastructure.persistence.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CleanupService {
    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    private final JobRepository jobRepository;
    private final QueueForgeProperties properties;

    public CleanupService(JobRepository jobRepository, QueueForgeProperties properties) {
        this.jobRepository = jobRepository;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${queueforge.cleanup.interval-ms:3600000}")
    public void cleanupTerminalJobs() {
        if (!properties.getCleanup().isEnabled()) {
            return;
        }

        try {
            int count = jobRepository.cleanupTerminalJobs(
                properties.getCleanup().getCompletedRetentionDays(),
                properties.getCleanup().getCancelledRetentionDays(),
                properties.getCleanup().getBatchSize()
            );
            if (count > 0) {
                log.info("Cleaned up {} terminal jobs (completed > {}d, cancelled > {}d)",
                    count,
                    properties.getCleanup().getCompletedRetentionDays(),
                    properties.getCleanup().getCancelledRetentionDays());
            }
        } catch (Exception e) {
            log.error("Job cleanup failed: {}", e.getMessage(), e);
        }
    }
}
