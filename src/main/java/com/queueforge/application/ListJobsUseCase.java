package com.queueforge.application;

import com.queueforge.domain.Job;
import com.queueforge.domain.JobStatus;
import com.queueforge.infrastructure.persistence.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListJobsUseCase {
    private final JobRepository jobRepository;

    public ListJobsUseCase(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public Page<Job> list(String queue, JobStatus status, String type, Pageable pageable) {
        return jobRepository.findAll(queue, status, type, pageable);
    }
}
