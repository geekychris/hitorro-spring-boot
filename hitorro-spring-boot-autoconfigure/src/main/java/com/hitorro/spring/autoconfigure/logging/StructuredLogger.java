/*
 * Copyright (c) 2026 Hitorro Project
 * Licensed under the MIT License
 */

package com.hitorro.spring.autoconfigure.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Base class for structured logging with Kafka integration.
 * 
 * <p>This class provides the foundation for type-safe structured loggers
 * that publish log events to Kafka topics. Each log event is serialized
 * as JSON and published asynchronously to a configured Kafka topic.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Automatic JSON serialization with Jackson</li>
 *   <li>Kafka publishing with configurable batching and compression</li>
 *   <li>Console fallback when Kafka is unavailable</li>
 *   <li>Async publishing for non-blocking operation</li>
 *   <li>Automatic timestamp injection</li>
 * </ul>
 * 
 * <p>Usage Example:</p>
 * <pre>
 * public class UserActionLogger extends StructuredLogger {
 *     public UserActionLogger(StructuredLoggerProperties properties) {
 *         super(properties, "user-actions");
 *     }
 *     
 *     public void logUserAction(String userId, String action) {
 *         Map&lt;String, Object&gt; event = new HashMap&lt;&gt;();
 *         event.put("user_id", userId);
 *         event.put("action", action);
 *         publish(event);
 *     }
 * }
 * </pre>
 */
public abstract class StructuredLogger {
    
    private static final Logger log = LoggerFactory.getLogger(StructuredLogger.class);
    
    protected final String topicName;
    protected final StructuredLoggerProperties properties;
    protected final ObjectMapper objectMapper;
    protected final KafkaProducer<String, String> producer;
    protected final boolean consoleFallback;
    
    /**
     * Creates a new structured logger for the specified topic.
     * 
     * @param properties the structured logging configuration properties
     * @param topicName the Kafka topic name for publishing logs
     */
    protected StructuredLogger(StructuredLoggerProperties properties, String topicName) {
        this.properties = properties;
        this.topicName = topicName;
        this.objectMapper = new ObjectMapper();
        this.consoleFallback = properties.isEnableConsoleFallback();
        this.producer = createKafkaProducer();
        
        log.info("Initialized structured logger for topic: {}", topicName);
    }
    
    /**
     * Creates and configures the Kafka producer.
     * 
     * @return configured KafkaProducer instance
     */
    private KafkaProducer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getKafkaBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, properties.getCompressionType());
        props.put(ProducerConfig.LINGER_MS_CONFIG, properties.getLingerMs());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, properties.getBatchSize());
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, properties.getMaxBlockMs());
        props.put(ProducerConfig.ACKS_CONFIG, "1"); // Leader acknowledgment
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        try {
            return new KafkaProducer<>(props);
        } catch (Exception e) {
            log.error("Failed to create Kafka producer. Console fallback will be used.", e);
            return null;
        }
    }
    
    /**
     * Publishes a log event to Kafka.
     * 
     * <p>The event map will be automatically enriched with:
     * <ul>
     *   <li>timestamp: ISO-8601 timestamp</li>
     *   <li>topic: the configured topic name</li>
     * </ul>
     * 
     * @param event the log event as a map of field names to values
     */
    protected void publish(Map<String, Object> event) {
        // Enrich event with metadata
        enrichEvent(event);
        
        try {
            // Serialize to JSON
            String jsonEvent = objectMapper.writeValueAsString(event);
            
            // Publish to Kafka if producer is available
            if (producer != null) {
                publishToKafka(jsonEvent);
            } else if (consoleFallback) {
                logToConsole(jsonEvent);
            }
        } catch (Exception e) {
            log.error("Failed to publish log event", e);
            if (consoleFallback) {
                logToConsole(event.toString());
            }
        }
    }
    
    /**
     * Enriches the event with standard metadata fields.
     * 
     * @param event the event map to enrich
     */
    private void enrichEvent(Map<String, Object> event) {
        if (!event.containsKey("timestamp")) {
            event.put("timestamp", Instant.now().toString());
        }
        if (!event.containsKey("topic")) {
            event.put("topic", topicName);
        }
    }
    
    /**
     * Publishes the serialized event to Kafka.
     * 
     * @param jsonEvent the JSON-serialized event
     */
    private void publishToKafka(String jsonEvent) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topicName, jsonEvent);
        
        if (properties.isAsyncPublishing()) {
            // Async publishing with callback
            producer.send(record, (RecordMetadata metadata, Exception exception) -> {
                if (exception != null) {
                    log.error("Error publishing to Kafka topic {}", topicName, exception);
                    if (consoleFallback) {
                        logToConsole(jsonEvent);
                    }
                } else {
                    log.debug("Published to topic {} partition {} offset {}", 
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        } else {
            // Sync publishing
            try {
                Future<RecordMetadata> future = producer.send(record);
                RecordMetadata metadata = future.get();
                log.debug("Published to topic {} partition {} offset {}", 
                        metadata.topic(), metadata.partition(), metadata.offset());
            } catch (Exception e) {
                log.error("Error publishing to Kafka topic {}", topicName, e);
                if (consoleFallback) {
                    logToConsole(jsonEvent);
                }
            }
        }
    }
    
    /**
     * Logs the event to console/SLF4J as fallback.
     * 
     * @param message the message to log
     */
    private void logToConsole(String message) {
        log.info("[STRUCTURED_LOG:{}] {}", topicName, message);
    }
    
    /**
     * Flushes any pending log events and closes the producer.
     * Should be called during application shutdown.
     */
    public void close() {
        if (producer != null) {
            try {
                producer.flush();
                producer.close();
                log.info("Closed structured logger for topic: {}", topicName);
            } catch (Exception e) {
                log.error("Error closing Kafka producer", e);
            }
        }
    }
    
    /**
     * Gets the topic name this logger publishes to.
     * 
     * @return the Kafka topic name
     */
    public String getTopicName() {
        return topicName;
    }
}
