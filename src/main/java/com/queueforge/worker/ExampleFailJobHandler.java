package com.queueforge.worker;

import com.queueforge.domain.Job;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExampleFailJobHandler implements JobHandler {
    private static final Logger log = LoggerFactory.getLogger(ExampleFailJobHandler.class);

    private final JobHandlerRegistry registry;

    public ExampleFailJobHandler(JobHandlerRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    void init() {
        registry.register(this);
    }

    @Override
    public String type() {
        return "example.fail";
    }

    @Override
    public void handle(Job job) {
        var payload = job.payload();
        String message = payload.has("message") ? payload.get("message").asText() : "Simulated failure";
        log.info("example.fail handler invoked — throwing: {}", message);
        throw new RuntimeException(message);
    }
}
