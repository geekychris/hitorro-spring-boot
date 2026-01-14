# Migration Guide: Standalone Hitorro to Spring Boot

This guide helps you migrate an existing Hitorro application to use Spring Boot.

## Overview

The migration maintains all Hitorro functionality while adding Spring Boot benefits:

- Dependency injection via Spring
- Configuration via application.yml
- REST endpoints via Spring MVC
- Transaction management via Spring
- Actuator for monitoring
- Spring Security integration (optional)

## Migration Steps

### 1. Update Project Dependencies

#### Before (standalone Hitorro):

```xml
<dependencies>
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-util</artifactId>
        <version>3.0.0</version>
    </dependency>
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-base</artifactId>
        <version>3.0.0</version>
    </dependency>
    <!-- Other Hitorro modules as needed -->
</dependencies>
```

#### After (with Spring Boot):

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.2</version>
</parent>

<dependencies>
    <!-- Single starter includes all core Hitorro modules -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Add specific Hitorro modules as needed -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-basedms</artifactId>
        <version>3.0.0</version>
    </dependency>
</dependencies>
```

### 2. Convert Main Class

#### Before:

```java
public class HitorroApplication {
    public static void main(String[] args) {
        // Manual ServiceContext initialization
        ServiceContext sc = ServiceContext.getSC();
        
        // Add services
        sc.addModule("com.example.MyService");
        sc.addModule("com.hitorro.base.service.BasicService");
        
        // Initialize
        String error = sc.init();
        if (error != null) {
            System.err.println("Initialization failed: " + error);
            System.exit(1);
        }
        
        // Keep application running
        ServiceContext.waitForDeInit();
    }
}
```

#### After:

```java
@SpringBootApplication
public class HitorroApplication {
    public static void main(String[] args) {
        SpringApplication.run(HitorroApplication.class, args);
    }
}
```

### 3. Service Access Pattern

#### Before (Direct ServiceContext Access):

```java
public class MyBusinessLogic {
    public void doWork() {
        ServiceContext sc = ServiceContext.getSC();
        BasicService basicService = (BasicService) sc.getInitializedModule(BasicService.class);
        basicService.doSomething();
    }
}
```

#### After (Spring Dependency Injection):

```java
@Service
public class MyBusinessLogic {
    
    private final HitorroServiceFactory serviceFactory;
    
    @Autowired
    public MyBusinessLogic(HitorroServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }
    
    public void doWork() {
        BasicService basicService = serviceFactory.getService(BasicService.class);
        basicService.doSomething();
    }
}
```

### 4. Configuration Management

#### Before (hitorro.properties or system properties):

```properties
# hitorro.properties
my.service.property=value
database.url=jdbc:mysql://localhost/hitorro
```

```java
public class MyService {
    public void configure() {
        String value = HTProperties.getProperties().get("my.service.property");
    }
}
```

#### After (application.yml):

```yaml
# application.yml
my:
  service:
    property: value

spring:
  datasource:
    url: jdbc:mysql://localhost/hitorro

hitorro:
  services:
    enabled: true
```

```java
@Service
public class MyService {
    
    @Value("${my.service.property}")
    private String property;
    
    // Or use @ConfigurationProperties for type-safe configuration
}
```

### 5. Transaction Management

#### Before (Manual Transaction Handling):

```java
public class DataService {
    public void saveData() {
        DMSSession session = getDMSSession();
        try {
            session.beginTransaction();
            // Do work
            session.commit();
        } catch (Exception e) {
            session.rollback();
            throw e;
        }
    }
}
```

#### After (Spring @Transactional):

```java
@Service
public class DataService {
    
    @Autowired
    private DMSSessionFactory dmsSessionFactory;
    
    @Transactional
    public void saveData() {
        // Transaction managed automatically by Spring
        // DMSSession lifecycle managed by Spring
        // Do work - rollback automatic on exceptions
    }
}
```

### 6. REST Endpoints

#### Before (Manual Servlet Registration):

```java
// Register servlet manually with Jetty
ServletContextHandler context = new ServletContextHandler();
context.addServlet(new ServletHolder(new DebugCommandServlet()), "/commands/*");
```

#### After (Spring MVC):

Hitorro commands are automatically exposed at `/api/commands/execute`:

```bash
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "help"}'
```

Or create custom Spring controllers:

```java
@RestController
@RequestMapping("/api/custom")
public class MyController {
    
    @Autowired
    private HitorroServiceFactory serviceFactory;
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        // Use Hitorro services
        return ResponseEntity.ok(statusData);
    }
}
```

### 7. Command Execution

#### Before:

```java
CommandSession session = new CommandSession(new ConsoleResponse(System.out));
session.executeCommand("env.services");
```

#### After (same, but also available via REST/Actuator):

```java
// Still works the same way
CommandSession session = new CommandSession(new ConsoleResponse(System.out));
session.executeCommand("env.services");

// Plus new options:
// - REST: POST /api/commands/execute
// - Actuator: GET /actuator/hitorro/command?cmd=env.services
// - Spring Shell: hitorro execute "env.services"
```

### 8. CLI Access

#### Before:

Only telnet/SSH available, configured in code.

#### After:

Multiple options configured in application.yml:

```yaml
hitorro:
  cli:
    native-enabled: true    # Keep telnet/SSH
    telnet-port: 9000
    ssh-port: 9022
    spring-shell-enabled: true    # Add Spring Shell
    actuator-enabled: true        # Add HTTP access
```

## Common Patterns

### Pattern 1: Custom Service Registration

If you have custom Hitorro services:

```java
@Configuration
public class CustomServiceConfig {
    
    @Bean
    @DependsOn("serviceContextManager")
    public MyCustomService myCustomService(ServiceContext serviceContext) {
        serviceContext.addModule("com.example.MyCustomService");
        return serviceContext.getService(MyCustomService.class);
    }
}
```

### Pattern 2: Property Bridging

Access Hitorro properties via Spring:

```java
@Component
public class PropertyBridge {
    
    @Value("${hitorro.property.key:default}")
    private String hitorroProperty;
    
    @Autowired
    private Environment environment;
    
    public void accessBothSystems() {
        // Spring way
        String springValue = environment.getProperty("some.key");
        
        // Hitorro way (still works)
        String hitorroValue = HTProperties.getProperties().get("hitorro.key");
    }
}
```

### Pattern 3: DMS with Spring Data

Combine Hitorro DMS with Spring Data repositories:

```java
// Traditional Spring Data repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByLastName(String lastName);
}

// Using Hitorro DMS unified ID system
@Service
public class DocumentService {
    
    @Autowired
    private DMSSessionFactory dmsFactory;
    
    @Transactional
    public Document getByUnifiedId(String unifiedId) {
        // Uses Hitorro's unified ID system
        return dmsFactory.getCurrentSession().get(unifiedId);
    }
}
```

## Testing

### Before (JUnit 4):

```java
public class MyServiceTest {
    @Before
    public void setup() {
        ServiceContext sc = ServiceContext.getSC();
        sc.init();
    }
    
    @Test
    public void testService() {
        // Test code
    }
}
```

### After (Spring Boot Test):

```java
@SpringBootTest
public class MyServiceTest {
    
    @Autowired
    private MyService myService;
    
    @Test
    public void testService() {
        // Service auto-injected and initialized
        assertNotNull(myService);
    }
}
```

## Troubleshooting

### Issue: Services not initializing

**Symptom**: `ServiceContext` is empty or services are null.

**Solution**: Ensure `hitorro.services.enabled=true` in application.yml and the starter is included.

### Issue: Properties not found

**Symptom**: Hitorro properties return null in Spring.

**Solution**: Check property source order. Spring properties override Hitorro properties. Use `@Value` with defaults: `@Value("${key:default}")`

### Issue: DMS transactions not working

**Symptom**: Data not persisting or transaction errors.

**Solution**: 
1. Ensure `@Transactional` is on service methods, not controller methods
2. Check `hitorro.dms.transaction-mode` setting
3. Verify Spring's transaction manager is configured

### Issue: CLI ports already in use

**Symptom**: CLI fails to start with "port already in use" error.

**Solution**: Configure different ports in application.yml:

```yaml
hitorro:
  cli:
    telnet-port: 9001
    ssh-port: 9023
```

## Rollback Strategy

If you need to rollback to standalone Hitorro:

1. Keep the original Hitorro dependencies
2. The Spring Boot integration doesn't modify Hitorro modules
3. Remove `hitorro-spring-boot-starter` dependency
4. Revert to manual `ServiceContext` initialization
5. Your Hitorro code remains unchanged

## Performance Considerations

- **Startup Time**: Spring Boot adds ~2-3 seconds to startup
- **Memory**: Additional ~50-100MB for Spring context
- **Runtime**: Negligible impact, same Hitorro performance

## Best Practices

1. **Use constructor injection** instead of field injection
2. **Keep Hitorro services stateless** where possible
3. **Configure transaction boundaries** carefully
4. **Use Spring profiles** for different environments
5. **Leverage Spring Boot Actuator** for monitoring
6. **Keep DMS session scope** to request or prototype
7. **Test both Hitorro and Spring functionality** separately

## Next Steps

After migration:

1. Add Spring Security for authentication/authorization
2. Implement Spring Boot Actuator health checks
3. Use Spring Cloud Config for distributed configuration
4. Add Micrometer for metrics
5. Implement caching with Spring Cache
6. Use Spring Batch for batch processing

## Support

For issues or questions:
- GitHub Issues: https://github.com/geekychris/hitorro-spring-boot/issues
- Hitorro Framework: https://github.com/geekychris/hitorro-all
