# Adding Structured Loggers to Hitorro Spring Boot

This guide explains how to add new structured loggers to the Hitorro Spring Boot example application using the `structured-logging` project's generator tool.

## Prerequisites

- **Structured Logging Project**: `/Users/chris/code/warp_experiments/done/structured-logging`
- **Hitorro Spring Boot Example**: `/Users/chris/hitorro/hitorro-spring-boot`
- **Kafka**: Running and accessible at `localhost:9092`
- **Spark Consumer**: Running and processing logs to Iceberg tables

## Quick Start

### 1. Create a Log Configuration

Create a JSON configuration file in the structured logging project's `log-configs/` directory:

```bash
cd /Users/chris/code/warp_experiments/done/structured-logging
```

Create a new config file, e.g., `order_events.json`:

```json
{
  "name": "OrderEvents",
  "log_type": "order_events",
  "version": "1.0.0",
  "description": "Logs order placement, fulfillment, and delivery events",
  "kafka": {
    "topic": "order-events",
    "partitions": 3,
    "replication_factor": 1,
    "retention_ms": 604800000
  },
  "warehouse": {
    "table_name": "analytics_logs.order_events",
    "partition_by": ["event_date", "event_type"],
    "sort_by": ["timestamp", "order_id"],
    "retention_days": 90
  },
  "iceberg": {
    "enabled": true,
    "partition_fields": ["event_date", "event_type"],
    "sort_fields": ["timestamp", "order_id"]
  },
  "fields": [
    {
      "name": "order_id",
      "type": "string",
      "required": true,
      "description": "Unique order identifier"
    },
    {
      "name": "customer_id",
      "type": "string",
      "required": true,
      "description": "Customer account ID"
    },
    {
      "name": "event_type",
      "type": "string",
      "required": true,
      "description": "Type of order event (PLACED, FULFILLED, DELIVERED, etc.)"
    },
    {
      "name": "event_date",
      "type": "date",
      "required": true,
      "description": "Event date for partitioning (YYYY-MM-DD)"
    },
    {
      "name": "timestamp",
      "type": "timestamp",
      "required": true,
      "description": "Timestamp when the event occurred"
    },
    {
      "name": "amount_cents",
      "type": "long",
      "required": true,
      "description": "Order amount in cents"
    },
    {
      "name": "currency",
      "type": "string",
      "required": false,
      "description": "Currency code (e.g., USD, EUR)"
    },
    {
      "name": "status",
      "type": "string",
      "required": false,
      "description": "Current order status"
    }
  ]
}
```

### 2. Generate the Logger

Run the Python generator tool:

```bash
cd /Users/chris/code/warp_experiments/done/structured-logging
python3 generators/generate_loggers.py log-configs/your_config.json
```

This creates:
- `java-logger/src/main/java/com/logging/generated/YourLogger.java` - Type-safe Java logger
- `scripts/create-topic-your-topic.sh` - Kafka topic creation script

### 3. Install the Updated Library

```bash
cd java-logger
mvn install -DskipTests
```

### 4. Copy the Generated Logger to Hitorro

Copy the generated logger from the structured-logging project to your Hitorro example:

```bash
cp /Users/chris/code/warp_experiments/done/structured-logging/java-logger/src/main/java/com/logging/generated/YourLogger.java \
   /Users/chris/hitorro/hitorro-example-springboot/src/main/java/com/hitorro/example/logging/YourLogger.java
```

Update the package declaration in the copied file:
```java
package com.hitorro.example.logging;
```

Add Spring annotations:
```java
@Component
@ConditionalOnProperty(prefix = "hitorro.structured-logging", name = "enabled", havingValue = "true")
```

### 5. Create a Demo Controller

Create a controller to demo your logger (`/Users/chris/hitorro/hitorro-example-springboot/src/main/java/com/hitorro/example/logging/YourDemoController.java`):

```java
package com.hitorro.example.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo/your-domain")
@ConditionalOnProperty(prefix = "hitorro.structured-logging", name = "enabled", havingValue = "true")
public class YourDemoController {
    
    private final YourLogger logger;
    
    @Autowired
    public YourDemoController(YourLogger logger) {
        this.logger = logger;
    }
    
    @PostMapping("/log-event")
    public ResponseEntity<Map<String, String>> logEvent(@RequestBody Map<String, String> data) {
        // Use the logger methods with appropriate parameters
        logger.log(...);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Event logged successfully");
        
        return ResponseEntity.ok(response);
    }
}
```

### 6. Create Kafka Topic

Run the generated topic creation script:

```bash
bash /Users/chris/code/warp_experiments/done/structured-logging/scripts/create-topic-your-topic.sh
```

### 7. Add Config to Spark Consumer Directory

The Spark consumer reads configs from `/Users/chris/code/warp_experiments/done/structured-logging/spark-consumer/log-configs/`. Copy your config there:

```bash
cp /Users/chris/code/warp_experiments/done/structured-logging/log-configs/your_config.json \
   /Users/chris/code/warp_experiments/done/structured-logging/spark-consumer/log-configs/
```

Note: Spark consumers are mounted via Docker volumes from this directory.

### 8. Restart Required Services

1. **Rebuild and restart HitorroExampleApplication** for the new logger
2. **Restart Spark consumer** to pick up the new config:

```bash
# Stop consumer
docker exec spark-master pkill -f StructuredLogConsumer

# Restart consumer
cd /Users/chris/code/warp_experiments/done/structured-logging
./start-consumer.sh
```

### 9. Verify Everything Works

1. **Check Kafka topic was created:**
   ```bash
   docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
   ```

2. **Test the endpoint:**
   ```bash
   curl -X POST http://localhost:8080/api/demo/your-domain/log-event \
     -H "Content-Type: application/json" \
     -d '{"key":"value"}'
   ```

3. **Verify in Kafka:**
   ```bash
   docker exec kafka kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic your-topic \
     --from-beginning --max-messages 1
   ```

4. **Check Spark consumer logs:**
   ```bash
   docker exec spark-master tail -100 /opt/spark-data/consumer.log | grep -i your_config_name
   ```

5. **Query Iceberg table:**
   ```sql
   SELECT * FROM local.analytics_logs.your_table ORDER BY timestamp DESC LIMIT 10;
   ```

## Configuration Reference

### JSON Schema

Required fields:
- `name`: Logger class name (PascalCase, e.g., `UserActivityLog`)
- `log_type`: Routing identifier (snake_case, e.g., `user_activity`)
- `version`: Schema version (e.g., `1.0.0`)
- `kafka.topic`: Kafka topic name (kebab-case, e.g., `user-events`)
- `kafka.partitions`: Number of Kafka partitions (usually 3-6)
- `kafka.replication_factor`: Kafka replication (use 1 for local dev)
- `warehouse.table_name`: Iceberg table name (schema.table format)
- `warehouse.partition_by`: Partition fields (date + categorical)
- `warehouse.sort_by`: Sort fields (commonly filtered)
- `fields`: Array of log field definitions

### Supported Field Types

| Type | Java | Description |
|------|------|-------------|
| `string` | `String` | Text |
| `int` | `Integer` | 32-bit integer |
| `long` | `Long` | 64-bit integer |
| `float` | `Float` | 32-bit decimal |
| `double` | `Double` | 64-bit decimal |
| `timestamp` | `Instant` | ISO-8601 timestamp |
| `date` | `LocalDate` | Date only (YYYY-MM-DD) |
| `array<string>` | `List<String>` | List of strings |
| `map<string,string>` | `Map<String,String>` | Key-value pairs |

## Message Format

All messages are automatically wrapped in an envelope:

```json
{
  "_log_type": "your_log_type",
  "_log_class": "YourClassName",
  "_version": "1.0.0",
  "data": {
    "your_field1": "value1",
    "your_field2": "value2"
  }
}
```

The `_log_type` field is used by the Spark consumer to route messages to the correct Iceberg table.

## Troubleshooting

### No messages appearing in Kafka

1. Check application logs for connection errors
2. Verify Kafka is running: `docker ps | grep kafka`
3. Test connectivity: `nc -zv localhost 9092`
4. Check if producer is being created properly

### Spark consumer not processing messages

1. Restart consumer: `./start-consumer.sh`
2. Check consumer logs: `docker exec spark-master tail -50 /opt/spark-data/consumer.log`
3. Verify config is in the mount directory: `docker exec spark-master ls -la /opt/spark-apps/log-configs/`

### Rows are null in Iceberg

1. Verify envelope format is being applied (messages have `_log_type`, `data`, etc.)
2. Check schema matches the envelope
3. Look for parsing errors in consumer logs

### Bean creation errors

1. Ensure `@Component` and `@ConditionalOnProperty` are on the logger
2. Verify `structured-logging.enabled=true` in `application.yml`
3. Check that the logger class is being scanned (correct package)

## Best Practices

1. **Naming:**
   - Config file: `your_events.json` or `your_log.json`
   - Logger class: `YourEventsLogger` or `YourLogLogger`
   - Kafka topic: `your-topic` or `your-events`
   - Table: `analytics_logs.your_table` or `analytics_logs.your_events_log`

2. **Partitioning:**
   - Always include a date field for partitioning
   - Choose fields with low-to-medium cardinality
   - Avoid high-cardinality fields like IDs

3. **Field definitions:**
   - Use snake_case for field names
   - Mark essential fields as `required: true`
   - Add helpful `description` for each field

4. **Testing:**
   - Generate and test loggers locally first
   - Use the JSON schema to validate configs
   - Check consumer logs for parsing errors

5. **Versioning:**
   - Start with `1.0.0`
   - Only bump version when breaking schema changes
   - Add changelog notes for each version

## Automation Scripts

### Copy Generated Logger Script

Create a helper script to simplify copying loggers:

```bash
#!/bin/bash
# scripts/copy-logger.sh
set -e

STRUCTURED_LOGGING_HOME=/Users/chris/code/warp_experiments/done/structured-logging
HITORRO_EXAMPLE=/Users/chris/hitorro/hitorro-example-springboot
LOGGER_NAME=$1

if [ -z "$LOGGER_NAME" ]; then
    echo "Usage: $0 <Logger Name (e.g., UserActivityLogLogger)>"
    exit 1
fi

SOURCE="$STRUCTURED_LOGGING_HOME/java-logger/src/main/java/com/logging/generated/${LOGGER_NAME}.java"
TARGET="$HITORRO_EXAMPLE/src/main/java/com/hitorro/example/logging/${LOGGER_NAME}.java"

if [ ! -f "$SOURCE" ]; then
    echo "Error: Logger not found: $SOURCE"
    exit 1
fi

cp "$SOURCE" "$TARGET"
echo "✅ Copied $LOGGER_NAME to Hitorro example"

# Update package declaration
sed -i.bak 's/package com.logging.generated;/package com.hitorro.example.logging;/g' "$TARGET"
rm "${TARGET}.bak"

# Add Spring annotations (if not present)
if ! grep -q "@Component" "$TARGET"; then
    sed -i.bak "1a\\
import org.springframework.stereotype.Component;\\
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;\\
\\
@Component\\
@ConditionalOnProperty(prefix = \"hitorro.structured-logging\", name = \"enabled\", havingValue = \"true\")\\
" "$TARGET"
    rm "${TARGET}.bak"
fi

echo "✅ Updated package and added Spring annotations"
```

Usage:
```bash
chmod +x scripts/copy-logger.sh
./scripts/copy-logger.sh OrderEventsLogger
```

## Additional Resources

- **Structured Logging README**: `/Users/chris/code/warp_experiments/done/structured-logging/README.md`
- **Adding New Loggers Guide**: `/Users/chris/code/warp_experiments/done/structured-logging/ADDING_NEW_LOGGERS.md`
- **Generator README**: `/Users/chris/code/warp_experiments/done/structured-logging/generators/README.md`
- **Envelope Format Spec**: `/Users/chris/code/warp_experiments/done/structured-logging/ENVELOPE_FORMAT.md`