/*
 * Copyright (c) 2026 Hitorro Project
 * Licensed under the MIT License
 */

package com.hitorro.spring.autoconfigure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating structured logger instances.
 * 
 * <p>This factory manages the lifecycle of structured loggers,
 * ensuring only one logger instance exists per topic name.
 * Loggers are cached and reused for efficiency.</p>
 * 
 * <p>Usage:</p>
 * <pre>
 * {@literal @}Autowired
 * private StructuredLoggerFactory loggerFactory;
 * 
 * // Create a logger for a specific topic
 * StructuredLogger logger = loggerFactory.createLogger("user-events");
 * </pre>
 */
public class StructuredLoggerFactory {
    
    private static final Logger log = LoggerFactory.getLogger(StructuredLoggerFactory.class);
    
    private final StructuredLoggerProperties properties;
    private final Map<String, StructuredLogger> loggers = new ConcurrentHashMap<>();
    
    /**
     * Creates a new logger factory.
     * 
     * @param properties the structured logging configuration
     */
    public StructuredLoggerFactory(StructuredLoggerProperties properties) {
        this.properties = properties;
    }
    
    /**
     * Creates or retrieves a structured logger for the specified topic.
     * Loggers are cached and reused.
     * 
     * @param topicName the Kafka topic name
     * @return a structured logger for the topic
     */
    public StructuredLogger createLogger(String topicName) {
        return loggers.computeIfAbsent(topicName, topic -> {
            log.info("Creating structured logger for topic: {}", topic);
            return new SimpleStructuredLogger(properties, topic);
        });
    }
    
    /**
     * Closes all managed loggers and releases resources.
     * Should be called during application shutdown.
     */
    public void closeAll() {
        log.info("Closing all structured loggers");
        loggers.values().forEach(logger -> {
            try {
                logger.close();
            } catch (Exception e) {
                log.error("Error closing logger for topic: {}", logger.getTopicName(), e);
            }
        });
        loggers.clear();
    }
    
    /**
     * Simple implementation of StructuredLogger that provides
     * a generic logging interface.
     */
    private static class SimpleStructuredLogger extends StructuredLogger {
        
        public SimpleStructuredLogger(StructuredLoggerProperties properties, String topicName) {
            super(properties, topicName);
        }
        
        /**
         * Logs an event with the specified fields.
         * 
         * @param event the log event as a map
         */
        public void log(Map<String, Object> event) {
            publish(event);
        }
    }
}
