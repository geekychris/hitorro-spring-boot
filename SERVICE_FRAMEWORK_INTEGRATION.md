# Hitorro Service Framework Integration Plan

## Overview

This document outlines the complete integration of Hitorro's service framework into Spring Boot, preserving all Hitorro functionality while enabling Spring Boot compatibility.

## Current State ✅

### What's Working
1. **ServiceContext** - Basic initialization via `ServiceContextManager`
2. **TypeManager** - Entity metadata registration for DMS
3. **DMSSession** - Full DMS CRUD operations
4. **Properties Bridge** - JVSProperties initialized (empty)

### What's Missing
1. **Service Loading** - Dynamic service registration from configuration
2. **Command Registry** - Commands not registered for REST/CLI
3. **Properties Loading** - JVS properties from files/Spring config
4. **HTServer Pattern** - Service dependency resolution
5. **REST Command Endpoints** - Commands not exposed as REST APIs

## Architecture

### Hitorro's Service Initialization Flow

```
CommandLine.main()
  ├─> Load JVS Properties (from files, args, etc.)
  ├─> ServiceContext.getSC()
  ├─> addModule(HTServer.class)
  │     └─> HTServer.getDependentService()
  │           └─> Reads services.{servertype} from JVS
  │                 └─> Returns [ServiceA, ServiceB, ServiceC]
  ├─> ServiceContext.init()
  │     ├─> Resolve dependencies
  │     ├─> Init each service (init -> start -> run)
  │     └─> Register commands from @CommandDef
  └─> CommandRegistry available for telnet/SSH/REST
```

### Proposed Spring Boot Integration

```
Spring Boot Application Start
  ├─> HitorroAutoConfiguration
  │     ├─> Load hitorro.* properties from application.yml
  │     ├─> Build JVS from Spring properties
  │     └─> Create beans:
  │           ├─> ServiceContextManager
  │           ├─> HitorroServiceFactory  
  │           └─> CommandRestController
  │
  ├─> ServiceContextManager.afterPropertiesSet()
  │     ├─> Initialize JVSProperties from Spring config
  │     ├─> Load services (configurable approach):
  │     │     Option A: From hitorro.services.enabled list
  │     │     Option B: From HTServer pattern (services.{type})
  │     │     Option C: Explicit bean registration
  │     ├─> ServiceContext.init()
  │     └─> Register all @CommandDef methods
  │
  ├─> CommandRestController
  │     ├─> GET /api/commands/list
  │     ├─> POST /api/commands/execute
  │     └─> Uses CommandSession/CommandRegistry
  │
  └─> Application Ready
        └─> Commands available via REST, telnet (if enabled), SSH (if enabled)
```

## Implementation Plan

### Phase 1: Enhanced Properties Integration ✅ (Partially Done)

**Goal**: Load Hitorro properties from Spring configuration

**Tasks**:
1. Create `SpringJVSPropertiesReader` to convert Spring properties → JVS
2. Support both approaches:
   - `hitorro.properties.*` → JVS structure
   - Load traditional JVS files if specified
3. Make JVSProperties available to all Hitorro services

**Files**:
- `SpringJVSPropertiesReader.java` (new)
- `HitorroPropertiesConfiguration.java` (update)

### Phase 2: Service Registration & Loading

**Goal**: Allow Spring Boot apps to specify which Hitorro services to load

**Approach A: Spring-Native (Recommended)**
```yaml
hitorro:
  services:
    enabled:
      - com.hitorro.base.service.BasicService
      - com.hitorro.util.typesystem.TypeManagerService
      - com.hitorro.basedms.db.HibernateService
      # Or by short name:
      - hibernate
      - typesystem
```

**Approach B: HTServer Pattern**
```yaml
hitorro:
  server:
    type: myapp  # Looks up services.myapp in JVS
    
services:
  myapp:
    - com.hitorro.base.service.BasicService
    - com.hitorro.basedms.db.HibernateService
```

**Approach C: Programmatic**
```java
@Configuration
public class HitorroServicesConfig {
    @Bean
    public ServiceConfigurator serviceConfigurator() {
        return sc -> {
            sc.addModule(BasicService.class);
            sc.addModule(HibernateService.class);
        };
    }
}
```

**Implementation**:
1. Update `ServiceContextManager` to accept service list
2. Add `ServiceConfigurator` interface
3. Auto-register services based on config

**Files**:
- `ServiceContextManager.java` (update)
- `ServiceConfigurator.java` (new)
- `HitorroServiceAutoConfiguration.java` (update)

### Phase 3: Command Registry Integration ✅ (Partially Done)

**Goal**: Expose Hitorro commands as REST endpoints automatically

**Current**: `CommandRestController` exists but commands not registered

**Needed**:
1. Scan all initialized services for `@CommandDef` methods
2. Register them with `CommandRegistry`
3. Make available via REST

**Implementation**:
```java
@PostConstruct
public void registerServiceCommands() {
    ServiceContext sc = ServiceContext.getSC();
    CommandRegistry registry = CommandRegistry.getRegistry();
    
    // Register commands from all services
    for (ServiceWrapper sw : sc.getServices()) {
        Object service = sw.getServiceObject();
        registry.addAllFromObject(service);
    }
    
    logger.info("Registered {} commands", registry.getCommands().size());
}
```

**Files**:
- `CommandRestController.java` (update)
- `CommandRegistrationPostProcessor.java` (new)

### Phase 4: CLI Integration (Optional)

**Goal**: Support telnet/SSH command interfaces if desired

**Options**:
1. **Native Hitorro CLI** - Start TerminalListener/SshListener services
2. **Spring Shell** - Bridge commands to Spring Shell (already done)
3. **Actuator** - Expose via actuator endpoints (already done)

**Configuration**:
```yaml
hitorro:
  cli:
    native:
      enabled: true
      telnet:
        port: 9000
      ssh:
        enabled: true
        port: 9022
```

### Phase 5: REST Servlet Integration

**Goal**: Support Hitorro's servlet-based REST framework alongside Spring MVC

**Hitorro Pattern**: `RestServletHandler` and servlet registration

**Spring Integration**: Register as ServletRegistrationBean

**Implementation**:
```java
@Bean
public ServletRegistrationBean<RestServletHandler> hitorroRestServlet() {
    ServletRegistrationBean<RestServletHandler> bean = 
        new ServletRegistrationBean<>(new RestServletHandler(), "/hitorro/*");
    bean.setLoadOnStartup(1);
    return bean;
}
```

## Configuration Examples

### Minimal Configuration (DMS Only)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl

hitorro:
  enabled: true
  dms:
    enabled: true
  services:
    enabled:
      - hibernate
      - typesystem
```

### Full Configuration (All Features)
```yaml
hitorro:
  enabled: true
  services:
    db-init: false
    enabled:
      - basicservice
      - typesystem
      - hibernate
      # Add more services as needed
      
  dms:
    enabled: true
    session-scope: prototype
    transaction-mode: spring-managed
    
  commands:
    rest:
      enabled: true
      base-path: /api/commands
      
  cli:
    native-enabled: true
    native-telnet-port: 9000
    native-ssh-enabled: true
    native-ssh-port: 9022
    spring-shell-enabled: false
    actuator-enabled: true
    
  properties:
    # Can override JVS properties here
    custom-prop: value
```

### HTServer-Style Configuration
```yaml
hitorro:
  server:
    enabled: true
    type: webapp  # Uses services.webapp from properties
    
services:
  webapp:
    - com.hitorro.base.service.BasicService
    - com.hitorro.util.typesystem.TypeManagerService
    - com.hitorro.basedms.db.HibernateService
    - com.myapp.MyCustomService
```

## Benefits

### For Hitorro Users Migrating to Spring Boot
- **Minimal Changes**: Existing service definitions work
- **Familiar Patterns**: HTServer, properties, commands all preserved
- **Gradual Migration**: Can use Hitorro and Spring features side-by-side

### For Spring Boot Users
- **Spring-Native**: Configure via application.yml
- **Dependency Injection**: Access Hitorro services as beans
- **Standard Patterns**: REST controllers, actuators, etc.

### For Both
- **Zero Changes to Hitorro**: All integration in adapter layer
- **Full Feature Support**: DMS, commands, services all work
- **Production Ready**: Proper lifecycle management

## Implementation Priority

1. **Phase 2** (Service Loading) - **HIGH PRIORITY**
   - Enables full Hitorro service framework
   - Required for complex applications
   
2. **Phase 3** (Command Registration) - **HIGH PRIORITY**  
   - Makes commands accessible
   - Critical for CLI/REST functionality
   
3. **Phase 1** (Enhanced Properties) - **MEDIUM PRIORITY**
   - Nice-to-have for complex configs
   - Current empty JVS works for most cases
   
4. **Phase 4** (CLI Integration) - **LOW PRIORITY**
   - Optional feature
   - Spring Shell already provides CLI
   
5. **Phase 5** (REST Servlets) - **LOW PRIORITY**
   - Can use Spring MVC controllers instead
   - Only needed for legacy servlet compatibility

## Next Steps

1. Implement **Phase 2** (Service Loading) to enable service framework
2. Enhance **Phase 3** (Command Registration) to auto-register commands
3. Test with a real application using multiple services
4. Document patterns for common use cases
5. Provide migration guides for existing Hitorro applications

## Questions to Resolve

1. **Service Loading**: Which approach (A, B, or C)? Or support all three?
2. **Properties**: Load from files, or rely on Spring properties?
3. **CLI**: Support native telnet/SSH, or just Spring Shell?
4. **Servlets**: Support Hitorro's servlet pattern, or Spring MVC only?
5. **Commands**: Auto-register all @CommandDef, or require explicit registration?

## Success Criteria

- ✅ All Hitorro services can be loaded
- ✅ Service dependencies resolved correctly
- ✅ Commands registered and accessible via REST
- ✅ Properties available to all services
- ✅ Lifecycle (init/start/deInit) managed properly
- ✅ Zero changes to Hitorro core code
- ✅ Comprehensive documentation and examples
