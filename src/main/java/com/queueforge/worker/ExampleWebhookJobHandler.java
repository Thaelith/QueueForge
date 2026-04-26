package com.queueforge.worker;

import com.queueforge.domain.Job;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExampleWebhookJobHandler implements JobHandler {
    private static final Logger log = LoggerFactory.getLogger(ExampleWebhookJobHandler.class);

    private final JobHandlerRegistry registry;

    public ExampleWebhookJobHandler(JobHandlerRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    void init() {
        registry.register(this);
    }

    @Override
    public String type() {
        return "example.webhook";
    }

    @Override
    public void handle(Job job) {
        var payload = job.payload();
        String url = payload.has("url") ? payload.get("url").asText() : "https://example.com/webhook";
        String method = payload.has("method") ? payload.get("method").asText() : "POST";
        log.info("Delivering webhook method={} url={} payload={}", method, url, payload);
    }
}
