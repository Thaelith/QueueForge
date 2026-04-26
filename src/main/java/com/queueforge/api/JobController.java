package com.queueforge.api;

import com.queueforge.api.dto.*;
import com.queueforge.application.GetJobUseCase;
import com.queueforge.application.ListJobsUseCase;
import com.queueforge.application.SubmitJobUseCase;
import com.queueforge.domain.Job;
import com.queueforge.domain.JobStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs", description = "Job submission and management")
public class JobController {
    private final SubmitJobUseCase submitJobUseCase;
    private final GetJobUseCase getJobUseCase;
    private final ListJobsUseCase listJobsUseCase;

    public JobController(SubmitJobUseCase submitJobUseCase, GetJobUseCase getJobUseCase, ListJobsUseCase listJobsUseCase) {
        this.submitJobUseCase = submitJobUseCase;
        this.getJobUseCase = getJobUseCase;
        this.listJobsUseCase = listJobsUseCase;
    }

    @PostMapping
    @Operation(summary = "Submit a new job")
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody SubmitJobRequest request) {
        Job job = submitJobUseCase.submit(
            request.queue(),
            request.type(),
            request.payload(),
            request.priority(),
            request.scheduledAt(),
            request.maxAttempts(),
            request.idempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(job));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        Job job = getJobUseCase.getById(id);
        return ResponseEntity.ok(toResponse(job));
    }

    @GetMapping
    @Operation(summary = "List jobs with optional filtering")
    public ResponseEntity<Page<JobResponse>> listJobs(
        @RequestParam(required = false) String queue,
        @RequestParam(required = false) JobStatus status,
        @RequestParam(required = false) String type,
        @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable
    ) {
        Page<Job> jobs = listJobsUseCase.list(queue, status, type, pageable);
        return ResponseEntity.ok(jobs.map(this::toResponse));
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
            job.id(),
            job.queue(),
            job.type(),
            job.payload(),
            job.status().name(),
            job.priority().getValue(),
            job.attempts(),
            job.maxAttempts(),
            job.runAt(),
            job.createdAt(),
            job.updatedAt()
        );
    }
}
