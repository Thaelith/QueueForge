package com.queueforge.infrastructure.persistence;

import com.queueforge.domain.JobEvent;

import java.util.List;
import java.util.UUID;

public interface JobEventRepository {

    void save(JobEvent event);

    List<JobEvent> findByJobId(UUID jobId);

    List<JobEvent> findRecent(int limit);
}
