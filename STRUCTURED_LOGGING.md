# Hitorro Structured Logging

Hitorro Spring Boot now includes support for **metadata-driven structured logging** inspired by the [geekychris/structured-logger](https://github.com/geekychris/structured-logger) framework. This feature enables config-driven type-safe logging with Kafka integration for downstream processing.

## Overview

Structured logging provides a modern approach to logging where:

- **Log schemas are defined in JSON configuration files**
- **Type-safe loggers are generated or created programmatically**
- **Events are published to Kafka topics** for downstream processing
- **Logs can be consumed by Spark and written to Iceberg tables** (following the structured-logger architecture)
- **Automatic JSON serialization** with Jackson
- **Console fallback** when Kafka is unavailable

## Architecture

```
Application Code
       ↓
StructuredLogger (Java)
       ↓
   JSON Serialization
       ↓
   Kafka Producer
       ↓
  Kafka Topic
       ↓
Spark Consumer (optional)
       ↓
Apache Iceberg Tables (optional)
       ↓
Trino/SQL Queries
```

## Key Features

- **Config-Driven**: Define log schemas once in JSON
- **Type-Safe**: Create type-safe logger classes extending `StructuredLogger`
- **Kafka Integration**: Reliable, high-throughput log streaming
- **Async Publishing**: Non-blocking log publishing with configurable batching
- **Console Fallback**: Logs to SLF4J/console when Kafka is unavailable
- **Spring Boot Auto-Configuration**: Zero-config setup with sensible defaults
- **Compression**: Snappy, gzip, lz4, zstd support
- **Automatic Enrichment**: Timestamps and metadata automatically added

## Quick Start

### 1. Enable Structured Logging

Add to your `application.yml`:

```yaml
hitorro:
  structured-logging:
    enabled: true
    kafka-bootstrap-servers: localhost:9092
    enable-console-fallback: true
```

### 2. Define a Log Configuration

Create a JSON configuration file in `src/main/resources/log-configs/`:

**user-activity-log.json**:
```json
{
  "name": "UserActivityLog",
  "version": "1.0.0",
  "description": "Logs user activity events",
  "kafka": {
    "topic": "user-events",
    "partitions": 3,
    "replication_factor": 1,
    "retention_ms": 604800000
  },
  "iceberg": {
    "table_name": "logs.user_activity",
    "partition_fields": ["event_date", "event_type"],
    "sort_fields": ["timestamp", "user_id"],
    "retention_days": 90
  },
  "fields": [
    {
      "name": "user_id",
      "type": "string",
      "required": true,
      "description": "Unique identifier for the user"
    },
    {
      "name": "event_type",
      "type": "string",
      "required": true,
      "description": "Type of event (LOGIN, LOGOUT, etc.)"
    },
    {
      "name": "timestamp",
      "type": "timestamp",
      "required": true,
      "description": "Event timestamp in ISO-8601 format"
    }
  ]
}
```

### 3. Create a Structured Logger

```java
import com.hitorro.spring.autoconfigure.logging.StructuredLogger;
import com.hitorro.spring.autoconfigure.logging.StructuredLoggerProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserActivityLogger extends StructuredLogger {
    
    public UserActivityLogger(StructuredLoggerProperties properties) {
        super(properties, "user-events");
    }
    
    public void logLogin(String userId, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("user_id", userId);
        event.put("event_type", eventType);
        event.put("timestamp", Instant.now().toString());
        publish(event);
    }
}
```

### 4. Use the Logger

```java
@RestController
public class MyController {
    
    @Autowired
    private UserActivityLogger activityLogger;
    
    @PostMapping("/login")
    public void login(@RequestParam String userId) {
        activityLogger.logLogin(userId, "LOGIN");
    }
}
```

## Configuration Reference

### Application Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `hitorro.structured-logging.enabled` | boolean | `false` | Enable/disable structured logging |
| `hitorro.structured-logging.kafka-bootstrap-servers` | string | `localhost:9092` | Kafka broker addresses |
| `hitorro.structured-logging.config-path` | string | `classpath:log-configs` | Path to log configuration files |
| `hitorro.structured-logging.enable-console-fallback` | boolean | `true` | Log to console if Kafka unavailable |
| `hitorro.structured-logging.compression-type` | string | `snappy` | Compression type (none, gzip, snappy, lz4, zstd) |
| `hitorro.structured-logging.linger-ms` | long | `10` | Producer linger time for batching |
| `hitorro.structured-logging.batch-size` | int | `16384` | Batch size in bytes |
| `hitorro.structured-logging.async-publishing` | boolean | `true` | Enable async publishing |

### Example Configuration

```yaml
hitorro:
  structured-logging:
    enabled: true
    kafka-bootstrap-servers: kafka1:9092,kafka2:9092,kafka3:9092
    config-path: classpath:log-configs
    enable-console-fallback: true
    compression-type: snappy
    linger-ms: 10
    batch-size: 32768
    async-publishing: true
```

## Log Configuration Schema

Each log configuration JSON file defines a log schema:

```json
{
  "name": "LogName",
  "version": "1.0.0",
  "description": "Description of this log type",
  "kafka": {
    "topic": "topic-name",
    "partitions": 3,
    "replication_factor": 1,
    "retention_ms": 604800000
  },
  "iceberg": {
    "table_name": "database.table_name",
    "partition_fields": ["date_field", "category_field"],
    "sort_fields": ["timestamp", "id"],
    "retention_days": 90
  },
  "fields": [
    {
      "name": "field_name",
      "type": "string|integer|long|double|boolean|timestamp|date|map<string,string>",
      "required": true|false,
      "description": "Field description"
    }
  ]
}
```

### Supported Field Types

- `string` - Text data
- `integer` - 32-bit integer
- `long` - 64-bit integer
- `double` - Floating point number
- `boolean` - True/false
- `timestamp` - ISO-8601 timestamp
- `date` - Date in YYYY-MM-DD format
- `map<string,string>` - Key-value pairs

## Best Practices

### 1. Partitioning Strategy

Choose partition keys that:
- Evenly distribute data across partitions
- Support efficient queries
- Example: Combine date + high cardinality field

```json
"partition_fields": ["event_date", "user_id"]
```

### 2. Sorting Strategy

Sort by commonly filtered fields:

```json
"sort_fields": ["timestamp", "user_id", "event_type"]
```

### 3. Field Naming

- Use **snake_case** for field names
- Mark fields as `required` only if truly essential
- Provide clear descriptions

### 4. Topic Naming

- Use **kebab-case** for topic names
- Include namespace/module prefix
- Example: `hitorro-user-activity`, `hitorro-api-metrics`

### 5. Versioning

- Update the `version` field when changing schemas
- Use semantic versioning (e.g., `1.0.0`, `1.1.0`)
- Document breaking changes

## Advanced Usage

### Using the Logger Factory

For dynamic logger creation:

```java
@Service
public class DynamicLoggingService {
    
    @Autowired
    private StructuredLoggerFactory loggerFactory;
    
    public void logEvent(String topicName, Map<String, Object> event) {
        StructuredLogger logger = loggerFactory.createLogger(topicName);
        logger.publish(event);
    }
}
```

### Custom Logger with Builder Pattern

```java
@Component
public class ApiMetricsLogger extends StructuredLogger {
    
    public ApiMetricsLogger(StructuredLoggerProperties properties) {
        super(properties, "hitorro-api-metrics");
    }
    
    public void logApiCall(String endpoint, String method, int statusCode, long responseTimeMs) {
        Map<String, Object> event = new HashMap<>();
        event.put("endpoint", endpoint);
        event.put("method", method);
        event.put("status_code", statusCode);
        event.put("response_time_ms", responseTimeMs);
        event.put("timestamp", Instant.now().toString());
        publish(event);
    }
}
```

### Logging with Metadata

```java
public void logWithMetadata(String userId, Map<String, String> metadata) {
    Map<String, Object> event = new HashMap<>();
    event.put("user_id", userId);
    event.put("event_type", "CUSTOM");
    event.put("metadata", metadata);  // Map type
    publish(event);
}
```

## Example Application

The `hitorro-example-springboot` application includes a complete structured logging demo:

### Demo Endpoints

Start Kafka locally, then enable structured logging:

```yaml
hitorro:
  structured-logging:
    enabled: true
```

Access the demo:

1. **Info endpoint**: `GET http://localhost:8080/api/demo/info`
2. **Login event**: `POST http://localhost:8080/api/demo/login`
   ```json
   {
     "userId": "user123",
     "username": "john.doe"
   }
   ```
3. **Logout event**: `POST http://localhost:8080/api/demo/logout`
   ```json
   {
     "userId": "user123",
     "username": "john.doe",
     "sessionId": "sess-abc-123"
   }
   ```
4. **API access**: `GET http://localhost:8080/api/demo/data?userId=user123`

### Demo Files

- **Logger**: `com.hitorro.example.logging.UserActivityLogger`
- **Controller**: `com.hitorro.example.logging.UserActivityDemoController`
- **Config**: `src/main/resources/log-configs/user-activity-log.json`

## Running with Kafka

### Docker Compose for Local Development

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

Start Kafka:
```bash
docker-compose up -d
```

### Verify Logs in Kafka

```bash
# List topics
docker exec -it <kafka-container> kafka-topics --list --bootstrap-server localhost:9092

# Consume logs
docker exec -it <kafka-container> kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning
```

## Integration with Iceberg (Optional)

For production deployments, consider adding a Spark consumer to write logs to Apache Iceberg tables for long-term storage and SQL analytics. This follows the architecture described in the [structured-logger](https://github.com/geekychris/structured-logger) repository.

### Benefits of Iceberg Integration

- **SQL Access**: Query logs with Trino/Presto
- **Time Travel**: Access historical snapshots
- **Schema Evolution**: Add/modify columns without downtime
- **ACID Transactions**: Atomic commits for streaming writes
- **Efficient Storage**: Parquet with compression

See the [structured-logger repository](https://github.com/geekychris/structured-logger) for complete Spark consumer and Iceberg setup instructions.

## Troubleshooting

### Logs Not Appearing in Kafka

1. **Check Kafka connection**:
   ```yaml
   hitorro:
     structured-logging:
       kafka-bootstrap-servers: localhost:9092  # Verify address
   ```

2. **Enable console fallback** to see logs locally:
   ```yaml
   hitorro:
     structured-logging:
       enable-console-fallback: true
   ```

3. **Check application logs** for connection errors:
   ```
   2026-01-28 ... ERROR StructuredLogger - Failed to create Kafka producer
   ```

### Performance Issues

1. **Increase batch size** for better throughput:
   ```yaml
   hitorro:
     structured-logging:
       batch-size: 32768
       linger-ms: 50
   ```

2. **Enable compression**:
   ```yaml
   hitorro:
     structured-logging:
       compression-type: snappy  # or lz4, zstd
   ```

3. **Use async publishing** (enabled by default):
   ```yaml
   hitorro:
     structured-logging:
       async-publishing: true
   ```

## Related Links

- **Original Framework**: [geekychris/structured-logger](https://github.com/geekychris/structured-logger)
- **Apache Kafka**: https://kafka.apache.org/
- **Apache Iceberg**: https://iceberg.apache.org/
- **Trino**: https://trino.io/

## Migration from Traditional Logging

### Before (Traditional Logging)

```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);

public void processUser(String userId) {
    log.info("User {} logged in", userId);
}
```

### After (Structured Logging)

```java
@Autowired
private UserActivityLogger activityLogger;

public void processUser(String userId) {
    activityLogger.logLogin(userId, "LOGIN");
}
```

### Benefits

- **Structured data** vs unstructured text
- **Type-safe** field definitions
- **Queryable** via SQL (with Iceberg)
- **Scalable** streaming architecture
- **Real-time** downstream processing

## License

MIT License - See LICENSE file for details.
