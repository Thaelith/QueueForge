package com.queueforge.application;

import com.queueforge.api.dto.AdminActionResponse;
import com.queueforge.domain.Job;
import com.queueforge.domain.exceptions.JobNotFoundException;
import com.queueforge.infrastructure.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class JobAdminService {
    private final JobRepository jobRepository;
    private final JobEventService eventService;

    public JobAdminService(JobRepository jobRepository, JobEventService eventService) {
        this.jobRepository = jobRepository;
        this.eventService = eventService;
    }

    @Transactional
    public AdminActionResponse requeue(UUID jobId, boolean resetAttempts, String reason) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));

        String oldStatus = job.status().name();
        boolean ok = jobRepository.requeueJob(jobId, resetAttempts);

        if (!ok) {
            return new AdminActionResponse(false, "Cannot requeue job with status: " + oldStatus, jobId, oldStatus);
        }

        eventService.record(jobId, "JOB_REQUEUED", oldStatus, "PENDING", null,
            "Requeued by admin" + (reason != null ? ": " + reason : ""));

        return new AdminActionResponse(true, "Job requeued", jobId, "PENDING");
    }

    @Transactional
    public AdminActionResponse cancel(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));

        String oldStatus = job.status().name();
        boolean ok = jobRepository.cancelJob(jobId);

        if (!ok) {
            return new AdminActionResponse(false, "Cannot cancel job with status: " + oldStatus, jobId, oldStatus);
        }

        eventService.record(jobId, "JOB_CANCELLED", oldStatus, "CANCELLED", null,
            "Cancelled by admin");

        return new AdminActionResponse(true, "Job cancelled", jobId, "CANCELLED");
    }

    @Transactional
    public AdminActionResponse retryNow(UUID jobId, boolean resetAttempts) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));

        String oldStatus = job.status().name();
        boolean ok = jobRepository.retryNow(jobId, resetAttempts);

        if (!ok) {
            return new AdminActionResponse(false, "Cannot retry-now job with status: " + oldStatus, jobId, oldStatus);
        }

        eventService.record(jobId, "JOB_RETRY_NOW", oldStatus, "PENDING", null,
            "Retry triggered by admin" + (resetAttempts ? " (attempts reset)" : ""));

        return new AdminActionResponse(true, "Job scheduled for immediate retry", jobId, "PENDING");
    }
}
