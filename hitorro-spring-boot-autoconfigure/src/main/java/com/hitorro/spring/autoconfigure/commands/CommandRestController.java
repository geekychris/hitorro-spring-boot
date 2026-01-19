/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.spring.autoconfigure.commands;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandRegistry;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.RestOperations;
import com.hitorro.util.commandandcontrol.json.JSONResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller that exposes Hitorro commands as HTTP endpoints.
 * 
 * This controller properly integrates with the Hitorro CommandRegistry:
 * - Uses CommandRegistry.getRegistry() to list all registered commands
 * - Uses CommandRegistry.execute() to run commands through the Hitorro infrastructure
 * - Uses JSONResponse to capture command output
 * - All commands registered via @CommandDef annotations are automatically available
 */
@RestController
@RequestMapping("${hitorro.commands.rest.base-path:/api/commands}")
public class CommandRestController {

    private static final Logger logger = LoggerFactory.getLogger(CommandRestController.class);

    private final CommandRegistrationManager registrationManager;

    public CommandRestController(CommandRegistrationManager registrationManager) {
        this.registrationManager = registrationManager;
        logger.info("Command REST endpoints registered - using Hitorro CommandRegistry");
    }

    /**
     * List all available commands with full metadata from the Hitorro CommandRegistry.
     * 
     * GET /api/commands/list?includeInternal=true
     * 
     * @param includeInternal if true, includes internal commands; if false (default), only public commands
     */
    @GetMapping("/list")
    public ResponseEntity<List<CommandDefInfo>> listCommands(
            @RequestParam(name = "includeInternal", required = false, defaultValue = "false") 
            boolean includeInternal) {
        
        CommandRegistry registry = registrationManager.getCommandRegistry();
        java.util.Collection<Command> commands = registry.getCommands();

        List<CommandDefInfo> commandList = commands.stream()
            .filter(cmd -> includeInternal || !cmd.isInternal())  // Filter based on parameter
            .map(this::buildCommandDefInfo)
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .collect(Collectors.toList());

        logger.debug("Listing {} commands from CommandRegistry (includeInternal={}, total={})", 
                    commandList.size(), includeInternal, commands.size());
        return ResponseEntity.ok(commandList);
    }
    
    /**
     * Get metadata for a specific command from the CommandRegistry.
     * 
     * GET /api/commands/{commandName}
     */
    @GetMapping("/{commandName}")
    public ResponseEntity<CommandDefInfo> getCommand(@PathVariable String commandName) {
        CommandRegistry registry = registrationManager.getCommandRegistry();
        Command cmd = registry.get(commandName);
        
        if (cmd == null) {
            return ResponseEntity.notFound().build();
        }
        
        CommandDefInfo info = buildCommandDefInfo(cmd);
        return ResponseEntity.ok(info);
    }
    
    /**
     * Execute a command using the Hitorro CommandRegistry infrastructure.
     * 
     * POST /api/commands/execute
     * Body: { "commandName": "demo.echo", "parameters": { "message": "hello" } }
     * 
     * This properly uses:
     * - CommandRegistry.execute() to run the command
     * - JSONResponse to capture structured output
     * - JVS (JSON Value Store) for parameters
     */
    @PostMapping("/execute")
    public ResponseEntity<CommandExecuteResponse> executeCommand(@RequestBody CommandExecuteRequest request) {
        long startTime = System.currentTimeMillis();
        String commandName = request.getCommandName();
        
        if (commandName == null || commandName.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new CommandExecuteResponse(false, null, "Command name is required", 0));
        }

        logger.info("Executing command via CommandRegistry: {} with parameters: {}", 
                   commandName, request.getParameters());

        try {
            // Get the command from the registry
            CommandRegistry registry = registrationManager.getCommandRegistry();
            Command command = registry.get(commandName);
            
            if (command == null) {
                return ResponseEntity.status(404)
                    .body(new CommandExecuteResponse(false, null, 
                        "Command not found: " + commandName, 
                        System.currentTimeMillis() - startTime));
            }
            
            // Convert parameters to JVS (Hitorro's JSON Value Store)
            JVS jvs = new JVS();
            if (request.getParameters() != null) {
                for (Map.Entry<String, Object> entry : request.getParameters().entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        jvs.set(entry.getKey(), ((Number) value).intValue());
                    } else if (value instanceof Boolean) {
                        jvs.set(entry.getKey(), (Boolean) value);
                    } else {
                        jvs.set(entry.getKey(), String.valueOf(value));
                    }
                }
            }
            
            // Create a JSONResponse to capture the command output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JSONResponse response = new JSONResponse(commandName, baos);
            
            // Create a CommandSession for execution context
            CommandSession session = new CommandSession();
            
            // Execute the command using the Hitorro CommandRegistry infrastructure
            boolean success = registry.execute("", commandName, jvs, response, session);
            
            // End the response to flush output
            response.end();
            
            // Parse the NDJSON output (newline-delimited JSON)
            String jsonOutput = baos.toString("UTF-8");
            Object result = parseNDJsonOutput(jsonOutput);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.info("Command {} executed via CommandRegistry: success={}, time={}ms, result={}", 
                       commandName, success, executionTime, result);
            
            return ResponseEntity.ok(new CommandExecuteResponse(success, result, null, executionTime));
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Error executing command via CommandRegistry: {}", commandName, e);
            
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.status(500)
                .body(new CommandExecuteResponse(false, null, errorMessage, executionTime));
        }
    }
    
    /**
     * Parse NDJSON (newline-delimited JSON) output from JSONResponse.
     * JSONResponse writes each row as a separate JSON object on its own line.
     */
    private Object parseNDJsonOutput(String ndjsonOutput) {
        if (ndjsonOutput == null || ndjsonOutput.trim().isEmpty()) {
            return null;
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            
            // Split by newlines and parse each JSON object
            String[] lines = ndjsonOutput.trim().split("\n");
            List<Object> results = new ArrayList<>();
            
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        Object obj = mapper.readValue(line, Object.class);
                        results.add(obj);
                    } catch (Exception e) {
                        // If line is not JSON, add as string
                        results.add(line);
                    }
                }
            }
            
            // If only one result, return it directly; otherwise return the list
            if (results.isEmpty()) {
                return null;
            } else if (results.size() == 1) {
                return results.get(0);
            } else {
                return results;
            }
            
        } catch (Exception e) {
            logger.warn("Error parsing NDJSON output: {}", e.getMessage());
            // Return as raw string if parsing fails
            return ndjsonOutput;
        }
    }
    
    /**
     * Build CommandDefInfo from a Command object from the CommandRegistry.
     */
    private CommandDefInfo buildCommandDefInfo(Command cmd) {
        CommandDefInfo info = new CommandDefInfo();
        info.setName(cmd.getCommand());
        info.setDescription(cmd.getDescription());
        info.setInternal(cmd.isInternal());
        info.setReturnType("Object");  // Commands don't expose return type metadata
        
        // Add REST operations metadata
        RestOperations[] restOps = cmd.getRestOperations();
        if (restOps != null && restOps.length > 0) {
            List<String> httpMethods = new ArrayList<>();
            for (RestOperations op : restOps) {
                httpMethods.add(toHttpMethod(op));
            }
            info.setRestOperations(httpMethods);
        }
        
        List<CommandParameter> params = new ArrayList<>();
        List<com.hitorro.util.commandandcontrol.DebugCommandArg> args = cmd.getArguments();
        if (args != null) {
            for (com.hitorro.util.commandandcontrol.DebugCommandArg arg : args) {
                CommandParameter param = new CommandParameter();
                param.setName(arg.getName());
                param.setType(arg.getJsonPropertyKey().getPropertyType());
                param.setRequired(arg.getRequired());
                param.setDescription(arg.getDescription());
                params.add(param);
            }
        }
        info.setParameters(params);
        
        return info;
    }
    
    /**
     * Convert Hitorro RestOperations to HTTP method string.
     */
    private String toHttpMethod(RestOperations operation) {
        switch (operation) {
            case Get: return "GET";
            case Post: return "POST";
            case Put: return "PUT";
            case Delete: return "DELETE";
            case Head: return "HEAD";
            case LastModified: return "GET";  // LastModified is a GET variant
            default: return operation.toString().toUpperCase();
        }
    }
    
    /**
     * Get help for a specific command.
     * 
     * GET /api/commands/help/{commandName}
     */
    @GetMapping("/help/{commandName}")
    public ResponseEntity<Map<String, Object>> getCommandHelp(@PathVariable String commandName) {
        try {
            // Execute "help {commandName}" using CommandRegistry
            CommandSession.executeToLog("help " + commandName, "");
            
            Map<String, Object> response = new HashMap<>();
            response.put("command", commandName);
            response.put("message", "Help information logged - check server logs");

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "Command not found: " + commandName));
        }
    }
    
    // DTO Classes for command metadata and execution
    
    public static class CommandDefInfo {
        private String name;
        private String description;
        private List<CommandParameter> parameters;
        private String returnType;
        private boolean internal;
        private List<String> restOperations;  // HTTP methods this command supports (GET, POST, etc.)
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<CommandParameter> getParameters() { return parameters; }
        public void setParameters(List<CommandParameter> parameters) { this.parameters = parameters; }
        
        public String getReturnType() { return returnType; }
        public void setReturnType(String returnType) { this.returnType = returnType; }
        
        public boolean isInternal() { return internal; }
        public void setInternal(boolean internal) { this.internal = internal; }
        
        public List<String> getRestOperations() { return restOperations; }
        public void setRestOperations(List<String> restOperations) { this.restOperations = restOperations; }
    }
    
    public static class CommandParameter {
        private String name;
        private String type;
        private boolean required;
        private Object defaultValue;
        private String description;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class CommandExecuteRequest {
        private String commandName;
        private Map<String, Object> parameters;
        
        public String getCommandName() { return commandName; }
        public void setCommandName(String commandName) { this.commandName = commandName; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
    
    public static class CommandExecuteResponse {
        private boolean success;
        private Object result;
        private String error;
        private long executionTimeMs;
        
        public CommandExecuteResponse() {}
        
        public CommandExecuteResponse(boolean success, Object result, String error, long executionTimeMs) {
            this.success = success;
            this.result = result;
            this.error = error;
            this.executionTimeMs = executionTimeMs;
        }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    }
}
