package com.queueforge.worker;

import com.queueforge.domain.Job;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExampleReportJobHandler implements JobHandler {
    private static final Logger log = LoggerFactory.getLogger(ExampleReportJobHandler.class);

    private final JobHandlerRegistry registry;

    public ExampleReportJobHandler(JobHandlerRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    void init() {
        registry.register(this);
    }

    @Override
    public String type() {
        return "example.report";
    }

    @Override
    public void handle(Job job) {
        var payload = job.payload();
        String reportName = payload.has("reportName") ? payload.get("reportName").asText() : "unknown";
        String format = payload.has("format") ? payload.get("format").asText() : "pdf";
        log.info("Generating report name={} format={}", reportName, format);
    }
}
