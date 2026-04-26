package com.queueforge.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobHandlerRegistry {
    private static final Logger log = LoggerFactory.getLogger(JobHandlerRegistry.class);

    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();

    public void register(JobHandler handler) {
        handlers.put(handler.type(), handler);
        log.info("Registered handler for type: {}", handler.type());
    }

    public Optional<JobHandler> getHandler(String type) {
        return Optional.ofNullable(handlers.get(type));
    }

    public boolean hasHandler(String type) {
        return handlers.containsKey(type);
    }
}
