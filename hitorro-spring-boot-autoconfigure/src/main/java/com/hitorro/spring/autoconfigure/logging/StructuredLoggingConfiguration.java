/*
 * Copyright (c) 2026 Hitorro Project
 * Licensed under the MIT License
 */

package com.hitorro.spring.autoconfigure.logging;

import com.logging.BaseStructuredLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to enable structured logging support from the structured-logging-java library.
 * 
 * <p>This configuration is activated when:</p>
 * <ul>
 *   <li>Structured logging library is on classpath</li>
 *   <li>Property {@code hitorro.structured-logging.enabled=true}</li>
 * </ul>
 * 
 * <p>The actual structured logging implementation (BaseStructuredLogger) comes from
 * the com.logging:structured-logging-java library, which handles Kafka publishing,
 * JSON serialization, and envelope management.</p>
 */
@AutoConfiguration
@ConditionalOnClass(BaseStructuredLogger.class)
@ConditionalOnProperty(prefix = "hitorro.structured-logging", name = "enabled", havingValue = "true")
@Configuration
public class StructuredLoggingConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingConfiguration.class);
    
    public StructuredLoggingConfiguration() {
        log.info("Structured logging configuration enabled. Using com.logging.BaseStructuredLogger library.");
        log.info("Applications should extend BaseStructuredLogger to create custom loggers.");
    }
}