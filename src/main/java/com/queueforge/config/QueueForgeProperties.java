package com.queueforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "queueforge")
public class QueueForgeProperties {
    private Worker worker = new Worker();
    private Recovery recovery = new Recovery();
    private Retry retry = new Retry();
    private Cleanup cleanup = new Cleanup();

    public Worker getWorker() { return worker; }
    public void setWorker(Worker worker) { this.worker = worker; }
    public Recovery getRecovery() { return recovery; }
    public void setRecovery(Recovery recovery) { this.recovery = recovery; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
    public Cleanup getCleanup() { return cleanup; }
    public void setCleanup(Cleanup cleanup) { this.cleanup = cleanup; }

    public static class Worker {
        private boolean enabled = false;
        private String workerId = "local-worker-1";
        private List<String> queues = List.of("default");
        private long pollIntervalMs = 1000;
        private Duration leaseDuration = Duration.ofSeconds(30);
        private int maxJobsPerPoll = 5;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public List<String> getQueues() { return queues; }
        public void setQueues(List<String> queues) { this.queues = queues; }
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
        public Duration getLeaseDuration() { return leaseDuration; }
        public void setLeaseDuration(Duration leaseDuration) { this.leaseDuration = leaseDuration; }
        public int getMaxJobsPerPoll() { return maxJobsPerPoll; }
        public void setMaxJobsPerPoll(int maxJobsPerPoll) { this.maxJobsPerPoll = maxJobsPerPoll; }
    }

    public static class Recovery {
        private boolean enabled = true;
        private long intervalMs = 30000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    }

    public static class Retry {
        private long baseDelaySeconds = 10;
        private long maxDelaySeconds = 300;
        private int maxAttempts = 10;

        public long getBaseDelaySeconds() { return baseDelaySeconds; }
        public void setBaseDelaySeconds(long baseDelaySeconds) { this.baseDelaySeconds = baseDelaySeconds; }
        public long getMaxDelaySeconds() { return maxDelaySeconds; }
        public void setMaxDelaySeconds(long maxDelaySeconds) { this.maxDelaySeconds = maxDelaySeconds; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    }

    public static class Cleanup {
        private boolean enabled = false;
        private long intervalMs = 3600000;
        private int completedRetentionDays = 7;
        private int cancelledRetentionDays = 7;
        private int batchSize = 500;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
        public int getCompletedRetentionDays() { return completedRetentionDays; }
        public void setCompletedRetentionDays(int completedRetentionDays) { this.completedRetentionDays = completedRetentionDays; }
        public int getCancelledRetentionDays() { return cancelledRetentionDays; }
        public void setCancelledRetentionDays(int cancelledRetentionDays) { this.cancelledRetentionDays = cancelledRetentionDays; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }
}
