package com.queueforge.worker;

import com.queueforge.domain.Job;

public interface JobHandler {
    String type();
    void handle(Job job) throws Exception;
}
