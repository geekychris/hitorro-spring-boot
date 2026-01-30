/*
 * Copyright (c) 2026 Hitorro Project
 * Licensed under the MIT License
 */

package com.hitorro.spring.autoconfigure.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for structured logging.
 * 
 * <p>Structured logging provides a metadata-driven approach to logging
 * where log schemas are defined in JSON configuration files and
 * logs are published to Kafka for downstream processing.</p>
 * 
 * <p>Example configuration in application.yml:</p>
 * <pre>
 * hitorro:
 *   structured-logging:
 *     enabled: true
 *     kafka-bootstrap-servers: localhost:9092
 *     config-path: classpath:log-configs
 *     enable-console-fallback: true
 * </pre>
 * 
 * @see StructuredLoggerAutoConfiguration
 */
@ConfigurationProperties(prefix = "hitorro.structured-logging")
public class StructuredLoggerProperties {
    
    /**
     * Whether structured logging is enabled.
     */
    private boolean enabled = false;
    
    /**
     * Kafka bootstrap servers for log publishing.
     * Format: host1:port1,host2:port2
     */
    private String kafkaBootstrapServers = "localhost:9092";
    
    /**
     * Path to log configuration directory or classpath resource.
     * Can be a file path or classpath location.
     */
    private String configPath = "classpath:log-configs";
    
    /**
     * Enable console fallback logging when Kafka is unavailable.
     * Logs will be written to console/slf4j if Kafka publishing fails.
     */
    private boolean enableConsoleFallback = true;
    
    /**
     * Kafka producer linger time in milliseconds.
     * Controls batching behavior for better throughput.
     */
    private long lingerMs = 10;
    
    /**
     * Kafka producer batch size in bytes.
     */
    private int batchSize = 16384;
    
    /**
     * Compression type for Kafka messages.
     * Options: none, gzip, snappy, lz4, zstd
     */
    private String compressionType = "snappy";
    
    /**
     * Maximum block time in milliseconds when buffer is full.
     */
    private long maxBlockMs = 60000;
    
    /**
     * Enable async publishing. When true, logs are published
     * asynchronously without blocking the calling thread.
     */
    private boolean asyncPublishing = true;

    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public void setKafkaBootstrapServers(String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public boolean isEnableConsoleFallback() {
        return enableConsoleFallback;
    }

    public void setEnableConsoleFallback(boolean enableConsoleFallback) {
        this.enableConsoleFallback = enableConsoleFallback;
    }

    public long getLingerMs() {
        return lingerMs;
    }

    public void setLingerMs(long lingerMs) {
        this.lingerMs = lingerMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public long getMaxBlockMs() {
        return maxBlockMs;
    }

    public void setMaxBlockMs(long maxBlockMs) {
        this.maxBlockMs = maxBlockMs;
    }

    public boolean isAsyncPublishing() {
        return asyncPublishing;
    }

    public void setAsyncPublishing(boolean asyncPublishing) {
        this.asyncPublishing = asyncPublishing;
    }
}
