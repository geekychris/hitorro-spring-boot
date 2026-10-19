<p align="center">
  <img src="assets/logo.png" alt="Hitorro Spring Boot logo" width="180">
</p>

# Hitorro Spring Boot Integration

Spring Boot integration for the Hitorro framework, enabling seamless use of Hitorro's services, properties, command framework, and DMS in Spring Boot applications.

## Features

- **Service Auto-Configuration**: Automatic initialization of Hitorro services with Spring lifecycle management
- **Property Integration**: Use Hitorro properties via Spring's `@Value` and `Environment`
- **REST Endpoints**: Expose Hitorro commands as REST APIs
- **Multiple CLI Options**: Telnet/SSH, Spring Shell, and Actuator endpoints
- **DMS Integration**: Preserve Hitorro's DMSSession and unified ID system
- **Transaction Management**: Bridge Hitorro and Spring transactions
- **Structured Logging**: Config-driven structured logging with Kafka integration (see [STRUCTURED_LOGGING.md](STRUCTURED_LOGGING.md))

## Quick Start

### Maven Dependency

Add the starter to your `pom.xml`:

```xml
<dependency>
    <groupId>com.hitorro</groupId>
    <artifactId>hitorro-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Enable Hitorro

Add `@EnableHitorro` to your Spring Boot application (coming soon, or just include the starter):

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### Configuration

Configure Hitorro in `application.yml`:

```yaml
hitorro:
  enabled: true
  
  # Service configuration
  services:
    enabled: true
    startup-scripts: ${HT_BIN}/scripts/startup.scr
    db-init: false
  
  # DMS configuration
  dms:
    enabled: true
    session-scope: request  # request, session, or prototype
    transaction-mode: spring-managed  # or hitorro-native
  
  # Command REST endpoints
  commands:
    rest:
      enabled: true
      base-path: /api/commands
  
  # CLI configuration
  cli:
    native-enabled: true
    telnet-port: 9000
    ssh-port: 9022
    spring-shell-enabled: true
    actuator-enabled: true
```

## Usage Examples

### Accessing Hitorro Services

Inject Hitorro services into your Spring beans:

```java
@Service
public class MyService {
    
    @Autowired
    private HitorroServiceFactory serviceFactory;
    
    public void useHitorroService() {
        // Get service by class
        BasicService basicService = serviceFactory.getService(BasicService.class);
        
        // Get service by name
        Object service = serviceFactory.getService("basic");
        
        // Check if service is available
        boolean available = serviceFactory.isServiceAvailable(BasicService.class);
    }
}
```

### Using Hitorro Properties

Access Hitorro properties via Spring:

```java
@Component
public class MyComponent {
    
    @Value("${my.hitorro.property}")
    private String hitorroProperty;
    
    @Autowired
    private Environment environment;
    
    public void accessProperty() {
        String value = environment.getProperty("my.hitorro.property");
    }
}
```

### Executing Commands via REST

```bash
# Execute a command
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "help"}'

# List available commands
curl http://localhost:8080/api/commands/list
```

### Using DMS (Document Management System)

```java
@Service
public class DocumentService {
    
    @Autowired
    private DMSSessionFactory dmsSessionFactory;
    
    @Transactional
    public void workWithDMS() {
        // DMS session is automatically managed
        // Unified ID system is preserved
        // Transactions are integrated with Spring
        
        // TODO: Actual DMS usage will be documented once
        // hitorro-basedms API is integrated
    }
}
```

### CLI Access Methods

#### 1. Native Hitorro CLI (Telnet/SSH)

```bash
# Via telnet
telnet localhost 9000

# Via SSH
ssh -p 9022 localhost
```

#### 2. Spring Shell (when enabled)

Start your application and use the interactive shell:

```bash
java -jar myapp.jar
shell:> hitorro help
shell:> hitorro execute "env.services"
```

#### 3. Actuator Endpoint

```bash
# Execute command via HTTP
curl http://localhost:8080/actuator/hitorro/command?cmd=help

# List commands
curl http://localhost:8080/actuator/hitorro/commands
```

## Architecture

### Integration Approach

This integration uses a **Bridge Module Pattern**:

1. **Core Modules Unchanged**: All Hitorro modules remain Spring-agnostic
2. **Thin Adapter Layer**: `hitorro-spring-boot-autoconfigure` provides Spring integration
3. **Opt-in Usage**: Applications choose to use Spring Boot or standalone Hitorro

### Component Structure

```
hitorro-spring-boot/
├── hitorro-spring-boot-autoconfigure/
│   ├── service/          # Service lifecycle integration
│   ├── properties/       # Property source bridging
│   ├── web/             # REST endpoint adapters
│   ├── cli/             # CLI integration (3 modes)
│   └── dms/             # DMS and transaction management
└── hitorro-spring-boot-starter/
    └── Dependency aggregator
```

## Advanced Configuration

### Custom Service Initialization

```java
@Configuration
public class CustomHitorroConfig {
    
    @Bean
    public ServiceContextManager customServiceContext() {
        ServiceContextManager manager = new ServiceContextManager(false);
        // Add custom service initialization logic
        return manager;
    }
}
```

### Transaction Management

Choose between Spring-managed and Hitorro-native transactions:

```yaml
hitorro:
  dms:
    transaction-mode: spring-managed  # Use Spring's @Transactional
    # or
    transaction-mode: hitorro-native  # Use Hitorro's transaction semantics
```

### Selective Feature Enablement

Disable features you don't need:

```yaml
hitorro:
  services:
    enabled: true
  dms:
    enabled: false  # Disable DMS integration
  commands:
    rest:
      enabled: false  # Disable REST endpoints
  cli:
    native-enabled: false  # Disable native CLI
    spring-shell-enabled: true  # Keep Spring Shell
```

## Adding Structured Loggers

For detailed instructions on adding new structured loggers to this project:

- 📘 [STRUCTURED_LOGGING_INTEGRATION.md](STRUCTURED_LOGGING_INTEGRATION.md) - Complete integration guide
- 🛠️ [scripts/README.md](scripts/README.md) - Automation scripts
- 📖 [STRUCTURED_LOGGING.md](STRUCTURED_LOGGING.md) - Comprehensive documentation

### Quick Start

```bash
# Generate and configure a new logger
./scripts/add-logger.sh your_config_name

# Copy and configure generated logger
./scripts/copy-logger.sh YourLoggerName
```

## Migration Guide

### From Standalone Hitorro to Spring Boot

1. **Add the starter dependency** to your `pom.xml`
2. **Convert your main class** to use `@SpringBootApplication`
3. **Configure Hitorro** in `application.yml`
4. **Inject services** instead of accessing via `ServiceContext.getSC()`
5. **Use Spring transactions** instead of manual transaction management

Example before:

```java
public class MyApp {
    public static void main(String[] args) {
        ServiceContext sc = ServiceContext.getSC();
        sc.addModule("com.example.MyService");
        sc.init();
        
        BasicService service = (BasicService) sc.getInitializedModule(BasicService.class);
    }
}
```

Example after:

```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}

@Service
public class MyBusinessLogic {
    @Autowired
    private HitorroServiceFactory serviceFactory;
    
    public void doWork() {
        BasicService service = serviceFactory.getService(BasicService.class);
    }
}
```

## Requirements

- Java 21+
- Spring Boot 3.2+
- Hitorro 3.0.0+
- Maven 3.8+

## Building from Source

```bash
git clone https://github.com/geekychris/hitorro-spring-boot.git
cd hitorro-spring-boot
mvn clean install
```

## Compatibility

| Hitorro Version | Spring Boot Version | Java Version |
|----------------|---------------------|--------------|
| 3.0.0          | 3.2+               | 21+          |

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- GitHub Issues: https://github.com/geekychris/hitorro-spring-boot/issues
- Documentation: https://github.com/geekychris/hitorro-spring-boot/wiki
- Hitorro Framework: https://github.com/geekychris/hitorro-all

## Roadmap

- [ ] Complete DMS integration with full DMSSession API
- [ ] Spring Data repository support for Hitorro entities
- [ ] Spring Security integration for CLI access control
- [ ] Metrics and monitoring via Micrometer
- [ ] Spring Cloud Config integration
- [ ] Kubernetes health checks and readiness probes
- [ ] GraphQL endpoint support for commands

## Acknowledgments

Built on top of the Hitorro framework by Chris Collins.
