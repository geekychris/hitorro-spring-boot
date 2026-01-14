# Hitorro REST Endpoint Integration

## Overview

Automatic discovery and exposure of Hitorro Commands as RESTful endpoints via Spring MVC. This integration bridges Hitorro's native REST support (via `ActionServlet` and `RestOperations`) to Spring Boot's web framework.

## Architecture

### How Hitorro REST Works

In native Hitorro:
1. Commands can declare support for REST operations (GET, POST, PUT, DELETE, etc.)
2. `ServletService` exposes an `ActionServlet` that routes HTTP requests to commands
3. Commands implement `executeRestfull()` method to handle REST requests
4. Requests/responses use Hitorro's Response types (JSON, XML, Console, etc.)

### Spring Boot Integration

The integration provides:
1. **Automatic Discovery** - Scans loaded commands for REST operation support
2. **Spring MVC Mapping** - Maps REST commands to Spring `@RestController` endpoints
3. **Dual Operation** - Works alongside native Hitorro `ActionServlet` or replaces it
4. **Spring Features** - Integrates with Spring Security, metrics, logging, etc.

## Components

### 1. HitorroRestMappingManager
**Purpose**: Discovers and tracks commands that support REST operations

- Scans `CommandRegistry` on application startup
- Creates `RestCommandMapping` for each REST-enabled command
- Tracks supported operations (GET, POST, PUT, DELETE, etc.)
- Distinguishes internal (admin) vs external (public) endpoints

### 2. HitorroRestController  
**Purpose**: Spring MVC controller that handles HTTP requests

- Exposes all REST commands under configurable base path
- Routes requests to appropriate Command via `executeRestfull()`
- Handles all HTTP methods (GET, POST, PUT, DELETE)
- Returns responses in Hitorro's native format (JSON by default)

### 3. HitorroRestAutoConfiguration
**Purpose**: Spring Boot auto-configuration

- Conditionally enables based on Command class presence
- Registers beans automatically
- Orders after service loading
- Configurable via properties

## Configuration

```yaml
hitorro:
  rest:
    # Enable REST endpoint discovery and mapping
    enabled: true
    
    # Base path for all REST endpoints
    base-path: /api/rest
    
    # Whether to expose internal (admin) commands
    expose-internal: false
```

## Usage Examples

### Discovering Available Endpoints

```bash
# List all available REST endpoints
curl http://localhost:8080/api/rest

# Get info about specific endpoint
curl http://localhost:8080/api/rest/_info/mycommand
```

### Calling REST Endpoints

If a Hitorro command named "status" supports GET:

```bash
# HTTP GET
curl http://localhost:8080/api/rest/status

# With query parameters
curl http://localhost:8080/api/rest/status?format=json&details=true
```

If a command supports POST:

```bash
# HTTP POST with form data
curl -X POST http://localhost:8080/api/rest/upload \
  -d "filename=test.txt" \
  -d "content=Hello World"

# HTTP POST with body
curl -X POST http://localhost:8080/api/rest/process \
  -H "Content-Type: application/json" \
  -d '{"data": "value"}'
```

### From Java/Spring

```java
@RestController
public class MyController {
    @Autowired
    private HitorroRestMappingManager mappingManager;
    
    @GetMapping("/endpoints")
    public Map<String, Object> listEndpoints() {
        return Map.of(
            "external", mappingManager.getExternalMappings().keySet(),
            "internal", mappingManager.getInternalMappings().keySet()
        );
    }
}
```

## Command Implementation

Hitorro commands declare REST support via `getRestOperations()`:

```java
@CommandDef(
    verb = "mycommand",
    description = "Example REST command"
)
public class MyCommand extends Command {
    
    @Override
    public RestOperations[] getRestOperations() {
        // Support GET and POST
        return new RestOperations[] {
            RestOperations.Get,
            RestOperations.Post
        };
    }
    
    @Override
    public boolean executeRestfull(
            String rawValue,
            JVS args,
            Response response,
            CommandSession session,
            RestOperations operation,
            InputStream inputStream) throws Exception {
        
        // Handle based on operation
        switch (operation) {
            case Get:
                // Handle GET request
                response.addRow("status", "ok");
                break;
            case Post:
                // Handle POST request
                // Read from inputStream if needed
                response.addRow("created", "success");
                break;
        }
        
        return true;
    }
}
```

## Request Flow

1. **HTTP Request** → Spring DispatcherServlet
2. **Route Matching** → `HitorroRestController` method
3. **Command Lookup** → `HitorroRestMappingManager.getMapping()`
4. **Authorization Check** → Internal vs External validation
5. **Parameter Conversion** → `Map<String, String>` → `JVS`
6. **Response Creation** → `JSONResponse` (or other format)
7. **Command Execution** → `command.executeRestfull()`
8. **HTTP Response** → Bytes returned to client

## Response Formats

By default, responses use Hitorro's `JSONResponse`, but commands can use:

- **JSONResponse** - JSON format (default)
- **XMLResponse** - XML format  
- **ConsoleResponse** - Plain text
- **HTMLResponse** - HTML format

The format can be selected via:
- Query parameter: `?otype=json|xml|html|console`
- Command implementation logic
- Custom Response implementations

## Security Considerations

### Internal Commands

Commands marked as internal (`command.isInternal() == true`) should be protected:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                // Allow external endpoints
                .requestMatchers("/api/rest/**").permitAll()
                // Protect internal endpoints (would need separate path)
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

**Recommendation**: Set `hitorro.rest.expose-internal=false` in production and expose internal commands via a separate, secured path or admin interface.

### Input Validation

Commands should validate input parameters:

```java
@Override
public boolean executeRestfull(...) {
    String value = args.getString("required_param");
    if (value == null || value.isEmpty()) {
        response.addError("required_param is missing");
        return false;
    }
    // ... proceed with validated input
}
```

## Integration with Native Hitorro

This Spring integration can:

1. **Replace ActionServlet** - Use Spring MVC exclusively
2. **Coexist with ActionServlet** - Run both (different base paths)
3. **Supplement ActionServlet** - Add Spring features (security, metrics) to some endpoints

### Configuration for Coexistence

```yaml
# Spring Boot REST endpoints
hitorro:
  rest:
    base-path: /api/rest
    
# Native Hitorro (if running ServletService)
defaulthttp:
  port: 8080
  # ActionServlet would be at /internal/* and /external/*
```

## Monitoring and Metrics

Integrate with Spring Boot Actuator:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  metrics:
    tags:
      application: hitorro-app
```

Commands automatically get:
- Request counting (via Spring MVC metrics)
- Response time tracking (X-Execution-Time-Ms header)
- Error rate monitoring
- Standard Spring Boot actuator endpoints

## Best Practices

1. **Use External Commands** - For public APIs, ensure commands aren't marked internal
2. **Document Endpoints** - Use `@CommandDef(description = "...")` 
3. **Version APIs** - Include version in base path: `/api/v1/rest`
4. **Validate Input** - Always validate parameters in commands
5. **Handle Errors** - Return meaningful error messages via Response
6. **Test Thoroughly** - Write integration tests for REST endpoints
7. **Secure Production** - Use Spring Security for authentication/authorization

## Troubleshooting

### Endpoint Not Found (404)

**Check**:
1. Command implements `getRestOperations()` and returns non-empty array
2. Command is loaded via ServiceContext/CommandRegistry
3. Base path matches configuration (`hitorro.rest.base-path`)

### Method Not Allowed (405)

**Check**:
- Command's `getRestOperations()` includes the HTTP method you're using
- Example: If using POST, array must contain `RestOperations.Post`

### Internal Server Error (500)

**Check**:
- Command logs for exception details
- Ensure `executeRestfull()` doesn't throw unhandled exceptions
- Validate that Response is properly created and ended

## Future Enhancements

Potential improvements:
- OpenAPI/Swagger documentation generation
- Async/reactive endpoint support  
- WebSocket integration for streaming
- Content negotiation (Accept header)
- Rate limiting/throttling
- API versioning framework

## Complete Example

See `hitorro-example-springboot` for a full working example demonstrating:
- Service loading with REST-enabled commands
- REST endpoint discovery
- Calling endpoints via HTTP
- Integration with Spring Security
- Monitoring with Actuator
