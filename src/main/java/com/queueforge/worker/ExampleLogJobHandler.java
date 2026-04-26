package com.queueforge.worker;

import com.queueforge.domain.Job;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExampleLogJobHandler implements JobHandler {
    private static final Logger log = LoggerFactory.getLogger(ExampleLogJobHandler.class);

    private final JobHandlerRegistry registry;

    public ExampleLogJobHandler(JobHandlerRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    void init() {
        registry.register(this);
    }

    @Override
    public String type() {
        return "example.log";
    }

    @Override
    public void handle(Job job) {
        log.info("Executing example.log job id={} payload={}", job.id(), job.payload());
    }
}
