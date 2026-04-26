package com.queueforge.application;

import com.queueforge.domain.JobEvent;
import com.queueforge.infrastructure.persistence.JobEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class JobEventService {
    private final JobEventRepository eventRepository;

    public JobEventService(JobEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void record(UUID jobId, String eventType, String fromStatus, String toStatus,
                       String workerId, String message) {
        if (jobId == null) {
            return;
        }
        var event = new JobEvent(
            null, jobId, eventType, fromStatus, toStatus,
            workerId, message, null,
            java.time.Instant.now()
        );
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<JobEvent> getTimeline(UUID jobId) {
        return eventRepository.findByJobId(jobId);
    }
}
