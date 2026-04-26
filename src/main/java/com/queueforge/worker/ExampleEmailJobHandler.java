package com.queueforge.worker;

import com.queueforge.domain.Job;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExampleEmailJobHandler implements JobHandler {
    private static final Logger log = LoggerFactory.getLogger(ExampleEmailJobHandler.class);

    private final JobHandlerRegistry registry;

    public ExampleEmailJobHandler(JobHandlerRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    void init() {
        registry.register(this);
    }

    @Override
    public String type() {
        return "example.email";
    }

    @Override
    public void handle(Job job) {
        var payload = job.payload();
        String to = payload.has("to") ? payload.get("to").asText() : "unknown";
        String subject = payload.has("subject") ? payload.get("subject").asText() : "no subject";
        log.info("Sending email to={} subject={}", to, subject);
    }
}
