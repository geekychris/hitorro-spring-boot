/*
 * Copyright (c) 2026 Hitorro Project
 * Licensed under the MIT License
 */

package com.hitorro.spring.autoconfigure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Auto-configuration for Hitorro structured logging.
 * 
 * <p>This configuration is activated when:</p>
 * <ul>
 *   <li>Kafka classes are on the classpath</li>
 *   <li>Property {@code hitorro.structured-logging.enabled=true}</li>
 * </ul>
 * 
 * <p>Provides infrastructure for config-driven structured logging where
 * log schemas are defined in JSON and logs are published to Kafka topics
 * for downstream processing (e.g., Spark consumer writing to Iceberg tables).</p>
 * 
 * <p>Configuration example:</p>
 * <pre>
 * hitorro:
 *   structured-logging:
 *     enabled: true
 *     kafka-bootstrap-servers: localhost:9092
 *     config-path: classpath:log-configs
 *     enable-console-fallback: true
 *     compression-type: snappy
 *     linger-ms: 10
 * </pre>
 * 
 * @see StructuredLogger
 * @see StructuredLoggerProperties
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "hitorro.structured-logging", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(StructuredLoggerProperties.class)
public class StructuredLoggerAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(StructuredLoggerAutoConfiguration.class);
    
    public StructuredLoggerAutoConfiguration(StructuredLoggerProperties properties) {
        log.info("Initializing Hitorro Structured Logging");
        log.info("  Kafka Bootstrap Servers: {}", properties.getKafkaBootstrapServers());
        log.info("  Config Path: {}", properties.getConfigPath());
        log.info("  Console Fallback: {}", properties.isEnableConsoleFallback());
        log.info("  Compression: {}", properties.getCompressionType());
        log.info("  Async Publishing: {}", properties.isAsyncPublishing());
    }
    
    /**
     * Creates a factory bean for creating structured loggers.
     * 
     * <p>Note: StructuredLoggerProperties is automatically available as a bean
     * via @EnableConfigurationProperties, so applications can inject it directly.</p>
     * 
     * @param properties the structured logging configuration
     * @return the logger factory
     */
    @Bean
    public StructuredLoggerFactory structuredLoggerFactory(StructuredLoggerProperties properties) {
        return new StructuredLoggerFactory(properties);
    }
}
