package com.queueforge.application;

import com.queueforge.domain.Job;
import com.queueforge.domain.exceptions.JobNotFoundException;
import com.queueforge.infrastructure.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetJobUseCase {
    private final JobRepository jobRepository;

    public GetJobUseCase(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public Job getById(UUID id) {
        return jobRepository.findById(id)
            .orElseThrow(() -> new JobNotFoundException("Job not found: " + id));
    }
}
