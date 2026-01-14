# Hitorro CLI Integration Guide

## Overview

Complete integration of Hitorro's command-line interface (CLI) system with Spring Boot, supporting multiple access methods:

1. **Native Telnet CLI** - Original Hitorro telnet interface
2. **Native SSH CLI** - Secure shell access
3. **REST API** - HTTP endpoints for command execution
4. **Spring Boot Actuator** - Management endpoint integration

## Quick Start

### Enable CLI in Spring Boot

```yaml
hitorro:
  cli:
    native-enabled: true
    telnet-port: 5050
    ssh-port: 5022
```

### Start Application

```bash
mvn spring-boot:run
```

### Connect via Telnet

```bash
telnet localhost 5050
```

Then type commands:
```
help
status
exit
```

### Connect via SSH

```bash
ssh -p 5022 user@localhost
# Password: user
```

## Configuration

### Full Configuration Options

```yaml
hitorro:
  cli:
    # Native Hitorro CLI
    native-enabled: true      # Enable telnet/SSH listeners
    telnet-port: 5050        # Telnet port (0 to disable)
    ssh-port: 5022           # SSH port (0 to disable)
    
    # Spring Shell integration
    spring-shell-enabled: false
    
    # Actuator endpoint
    actuator-enabled: true
```

## Command Registration

Commands are automatically discovered and registered when services load.

### Using @CommandDef Annotation

```java
@ServiceDefinition(
    shortName = "myservice",
    debugCommands = {MyCommand.class}
)
public class MyService {
    // Service implementation
}

@CommandDef(
    verb = "hello",
    description = "Say hello",
    minArgs = 0,
    maxArgs = 1
)
public class MyCommand extends Command {
    @Override
    public boolean execute(String rawValue, JVS args, Response response, 
                          CommandSession session) throws Exception {
        String name = args.getString("name", "World");
        response.addRow("message", "Hello, " + name + "!");
        return true;
    }
}
```

### Registering Commands Programmatically

```java
@Component
public class CustomCommandRegistrar implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        CommandRegistry registry = CommandRegistry.getRegistry();
        registry.addCommand(new MyCustomCommand());
    }
}
```

## Access Methods

### 1. Telnet CLI

**Connect**:
```bash
telnet localhost 5050
```

**Usage**:
```
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
> help
Available commands:
  help      - Display available commands
  exit      - Exit the session
  status    - Show system status
  ...

> status
System: Running
Uptime: 00:15:32
Services: 5 active

> exit
Connection closed by foreign host.
```

**Features**:
- Command history
- Tab completion (if implemented)
- Real-time output
- Session management

### 2. SSH CLI

**Connect**:
```bash
ssh -p 5022 user@localhost
# Default credentials: user/user
```

**Usage** (same as telnet but encrypted):
```
user@localhost's password: ****
> help
> status
> exit
```

**Features**:
- Encrypted connection
- Authentication
- Same commands as telnet
- Better security for production

### 3. REST API

**Execute Commands via HTTP**:

```bash
# Execute a command
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "status"}'

# List available commands
curl http://localhost:8080/api/commands/list

# Get command help
curl http://localhost:8080/api/commands/help/status
```

**Response Format**:
```json
{
  "success": true,
  "output": "System: Running\nUptime: 00:15:32",
  "command": "status"
}
```

### 4. Actuator Endpoint

**Configure**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: hitorro
```

**Execute Commands**:
```bash
curl -X POST http://localhost:8080/actuator/hitorro/command \
  -H "Content-Type: application/json" \
  -d '{"command": "status"}'
```

## Built-in Commands

When services are loaded, these commands are automatically available:

### Core Commands

- **help** - List all available commands
- **exit** / **quit** - Close the session
- **history** - Show command history

### Service Commands (when BaseDMSService is loaded)

- **DumpVersionTree** - Show document version tree
- **TestStreams** - Test stream handling
- **ListIntegrationEvents** - Show configured integration events
- **RunIntegrationEvent** - Run a specific integration event

### Custom Commands

Your application can register custom commands that appear alongside built-in ones.

## Command Output Formats

Commands support multiple output formats:

### Console Format (CLI)
```
> status
System: Running
Uptime: 00:15:32
Services: 5 active
```

### JSON Format (REST)
```json
{
  "system": "Running",
  "uptime": "00:15:32",
  "services": "5 active"
}
```

### XML Format (REST with parameter)
```xml
<response>
  <system>Running</system>
  <uptime>00:15:32</uptime>
  <services>5 active</services>
</response>
```

## Security

### Telnet

Telnet is **unencrypted** and should only be used for:
- Development environments
- Internal networks
- Localhost connections

### SSH

SSH provides:
- Encrypted connections
- Authentication
- Suitable for production

**Configure SSH Keys** (optional):
```yaml
hitorro:
  cli:
    ssh:
      host-key: /path/to/host_key
      authorized-keys: /path/to/authorized_keys
```

### REST API

Secure REST endpoints with Spring Security:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/commands/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }
}
```

## Example: Custom Command

Create a custom command that queries the database:

```java
package com.myapp.commands;

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.jsontypesystem.JVS;
import org.springframework.stereotype.Component;

@Component
@CommandDef(
    verb = "listusers",
    description = "List all users in the system",
    minArgs = 0,
    maxArgs = 0
)
public class ListUsersCommand extends Command {
    
    private final DMSSessionFactory sessionFactory;
    
    public ListUsersCommand(DMSSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    @Override
    public boolean execute(String rawValue, JVS args, Response response,
                          CommandSession session) throws Exception {
        DMSSession dmsSession = sessionFactory.createSession();
        try {
            // Query users
            List<User> users = dmsSession.getObjects(User.class, "");
            
            response.addRow("Total Users", String.valueOf(users.size()));
            response.addRow("", "");
            
            for (User user : users) {
                response.addRow(user.getUsername(), user.getEmail());
            }
            
            return true;
        } finally {
            dmsSession.close();
        }
    }
}
```

**Register in Service**:
```java
@ServiceDefinition(
    shortName = "myapp",
    debugCommands = {ListUsersCommand.class}
)
public class MyAppService {
    // ...
}
```

**Use it**:
```bash
> listusers
Total Users: 5

admin: admin@example.com
user1: user1@example.com
user2: user2@example.com
```

## Testing

### Test Commands in Integration Tests

```java
@SpringBootTest
class CommandIntegrationTest {
    
    @Autowired
    private HitorroServiceFactory serviceFactory;
    
    @Test
    void testCommandExecution() {
        CommandRegistry registry = CommandRegistry.getRegistry();
        Command statusCmd = registry.getCommand("status");
        
        assertThat(statusCmd).isNotNull();
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleResponse response = new ConsoleResponse("status", new PrintWriter(output));
        CommandSession session = new CommandSession();
        
        boolean success = statusCmd.execute("", new JVS(), response, session);
        
        assertThat(success).isTrue();
        String result = output.toString();
        assertThat(result).contains("System");
    }
}
```

### Test Telnet Connection

```java
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class TelnetIntegrationTest {
    
    @Value("${hitorro.cli.telnet-port}")
    private int telnetPort;
    
    @Test
    void testTelnetConnection() throws Exception {
        try (Socket socket = new Socket("localhost", telnetPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println("help");
            String response = in.readLine();
            assertThat(response).contains("help");
        }
    }
}
```

## Production Considerations

### 1. Disable Telnet in Production

```yaml
# production.yml
hitorro:
  cli:
    native-enabled: false  # Disable for security
    telnet-port: 0
    ssh-port: 0
```

### 2. Use SSH Only

```yaml
# production.yml
hitorro:
  cli:
    native-enabled: true
    telnet-port: 0          # Disable telnet
    ssh-port: 5022          # SSH only
```

### 3. Secure REST Endpoints

Use Spring Security with proper authentication:
- API keys
- OAuth2
- JWT tokens

### 4. Monitoring

Monitor CLI usage:
```java
@Component
public class CommandMonitor {
    @EventListener
    public void onCommandExecuted(CommandExecutedEvent event) {
        logger.info("Command executed: {} by user: {}", 
            event.getCommand(), event.getUser());
    }
}
```

## Troubleshooting

### Port Already in Use

```
Could not listen on port 5050
```

**Solution**: Change port or set `incrementPortIfUnavailable`:
```yaml
hitorro:
  cli:
    telnet-port: 5051  # Use different port
```

### Commands Not Appearing

**Check**:
1. Service is loaded
2. Command is in `debugCommands` array
3. CommandRegistry is initialized
4. Check logs for registration errors

### SSH Connection Refused

**Possible causes**:
- SSH port not configured
- SSH dependencies missing
- Firewall blocking port

**Solution**: Check logs and ensure SSH listener started successfully.

## Advanced Features

### Command Aliases

```java
@CommandDef(
    verb = "st",
    aliases = {"status", "stat"},
    description = "System status"
)
public class StatusCommand extends Command {
    // ...
}
```

### Command Parameters

```java
@CommandDef(
    verb = "search",
    description = "Search documents",
    minArgs = 1,
    maxArgs = 3
)
public class SearchCommand extends Command {
    @CommandArgument(name = "query", description = "Search query")
    public String query;
    
    @CommandArgument(name = "limit", description = "Result limit", optional = true)
    public int limit = 10;
    
    // ...
}
```

### Async Commands

```java
@CommandDef(verb = "backup", description = "Start backup")
public class BackupCommand extends Command {
    @Override
    public boolean execute(...) {
        CompletableFuture.runAsync(() -> {
            // Long-running backup operation
        });
        response.addRow("status", "Backup started in background");
        return true;
    }
}
```

## Architecture

```
User Input (telnet/SSH/REST)
    ↓
TerminalListener / RestController
    ↓
CommandSession
    ↓
CommandRegistry.getCommand(verb)
    ↓
Command.execute()
    ↓
Response (Console/JSON/XML)
    ↓
Output to user
```

## Summary

The Hitorro CLI integration provides:

✅ **Multiple Access Methods**: Telnet, SSH, REST, Actuator
✅ **Automatic Command Discovery**: From @ServiceDefinition and @CommandDef
✅ **Spring Boot Lifecycle**: Start/stop with application
✅ **Flexible Output**: Console, JSON, XML formats
✅ **Security Ready**: SSH encryption, Spring Security integration
✅ **Production Ready**: Configurable, monitorable, testable

Commands registered in Hitorro are **immediately available** via all access methods - telnet, SSH, and REST APIs!
