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
package com.hitorro.spring.autoconfigure.rest;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.spring.autoconfigure.commands.CommandRegistrationManager;
import com.hitorro.util.commandandcontrol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller that dynamically routes HTTP requests to Hitorro commands
 * based on their declared restOperations.
 * 
 * This controller leverages Hitorro's built-in REST support where commands
 * declare which HTTP methods they support via @CommandDef(restOperations = {...}).
 * 
 * Unlike the generic /api/commands/execute endpoint, these endpoints map directly
 * to command names and HTTP methods, providing a RESTful API that mirrors the
 * command structure.
 */
@RestController
@RequestMapping("${hitorro.rest.base-path:/api/rest}")
public class HitorroRestController {
    
    private static final Logger logger = LoggerFactory.getLogger(HitorroRestController.class);
    
    @Autowired
    private CommandRegistrationManager commandManager;
    
    /**
     * Discovery endpoint that lists all REST-enabled commands.
     * This provides API documentation and discovery for client applications.
     * 
     * GET /api/rest
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestDiscoveryResponse> discoverEndpoints(
            @RequestParam(name = "includeInternal", required = false, defaultValue = "false") 
            boolean includeInternal) {
        
        CommandRegistry registry = commandManager.getCommandRegistry();
        Collection<Command> commands = registry.getCommands();
        
        List<RestEndpointInfo> endpoints = new ArrayList<>();
        
        for (Command command : commands) {
            // Filter internal commands unless requested
            if (!includeInternal && command.isInternal()) {
                continue;
            }
            
            // Only include commands that support REST operations
            RestOperations[] operations = command.getRestOperations();
            if (operations == null || operations.length == 0) {
                continue;
            }
            
            // Convert to endpoint info
            RestEndpointInfo info = new RestEndpointInfo();
            info.setPath("/" + command.getCommand());
            info.setCommand(command.getCommand());
            info.setDescription(command.getDescription());
            info.setInternal(command.isInternal());
            
            // Convert RestOperations to HTTP methods
            List<String> methods = Arrays.stream(operations)
                .map(this::toHttpMethod)
                .collect(Collectors.toList());
            info.setMethods(methods);
            
            // Extract parameter info
            List<com.hitorro.util.commandandcontrol.DebugCommandArg> args = command.getArguments();
            if (args != null) {
                List<RestParameterInfo> params = new ArrayList<>();
                for (com.hitorro.util.commandandcontrol.DebugCommandArg arg : args) {
                    RestParameterInfo param = new RestParameterInfo();
                    param.setName(arg.getName());
                    param.setDescription(arg.getDescription());
                    param.setRequired(arg.getRequired());
                    param.setType(arg.getJsonPropertyKey().getPropertyType());
                    param.setDefaultValue(null); // Default value not directly accessible
                    params.add(param);
                }
                info.setParameters(params);
            }
            
            endpoints.add(info);
        }
        
        RestDiscoveryResponse response = new RestDiscoveryResponse();
        response.setEndpoints(endpoints);
        response.setTotalEndpoints(endpoints.size());
        response.setBasePath("${hitorro.rest.base-path:/api/rest}");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Handle GET requests to REST-enabled commands.
     * 
     * GET /api/rest/{commandName}?param1=value1&param2=value2
     */
    @GetMapping("/{commandName:.+}")
    public ResponseEntity<?> handleGet(
            @PathVariable("commandName") String commandName,
            @RequestParam(required = false) Map<String, String> params,
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        
        return executeCommand(commandName, RestOperations.Get, params, null, request, response);
    }
    
    /**
     * Handle POST requests to REST-enabled commands.
     * Supports both query parameters and JSON body.
     * 
     * POST /api/rest/{commandName}
     * Body: {"param1": "value1", "param2": "value2"}
     */
    @PostMapping("/{commandName:.+}")
    public ResponseEntity<?> handlePost(
            @PathVariable("commandName") String commandName,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Map<String, String> params,
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        
        return executeCommand(commandName, RestOperations.Post, params, body, request, response);
    }
    
    /**
     * Handle PUT requests to REST-enabled commands.
     * 
     * PUT /api/rest/{commandName}
     * Body: {"param1": "value1", "param2": "value2"}
     */
    @PutMapping("/{commandName:.+}")
    public ResponseEntity<?> handlePut(
            @PathVariable("commandName") String commandName,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Map<String, String> params,
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        
        return executeCommand(commandName, RestOperations.Put, params, body, request, response);
    }
    
    /**
     * Handle DELETE requests to REST-enabled commands.
     * 
     * DELETE /api/rest/{commandName}?id=123
     */
    @DeleteMapping("/{commandName:.+}")
    public ResponseEntity<?> handleDelete(
            @PathVariable("commandName") String commandName,
            @RequestParam(required = false) Map<String, String> params,
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        
        return executeCommand(commandName, RestOperations.Delete, params, null, request, response);
    }
    
    /**
     * Handle HEAD requests to REST-enabled commands.
     * 
     * HEAD /api/rest/{commandName}
     */
    @RequestMapping(value = "/{commandName:.+}", method = RequestMethod.HEAD)
    public ResponseEntity<?> handleHead(
            @PathVariable("commandName") String commandName,
            @RequestParam(required = false) Map<String, String> params,
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        
        return executeCommand(commandName, RestOperations.Head, params, null, request, response);
    }
    
    /**
     * Core execution method that routes HTTP requests to Hitorro commands.
     * This method:
     * 1. Validates the command exists and supports the requested operation
     * 2. Converts HTTP parameters to Hitorro JVS format
     * 3. Executes the command through CommandRegistry with RestOperations context
     * 4. Supports streaming responses and direct servlet access
     * 5. Parses and returns the response (if not already committed by command)
     */
    private ResponseEntity<?> executeCommand(
            String commandName,
            RestOperations operation,
            Map<String, String> queryParams,
            Map<String, Object> body,
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse servletResponse) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Get command from registry
            CommandRegistry registry = commandManager.getCommandRegistry();
            Command command = registry.get(commandName);
            
            if (command == null) {
                logger.warn("Command not found: {}", commandName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "error", "Command not found: " + commandName,
                        "success", false
                    ));
            }
            
            // Check if command supports the requested REST operation
            if (!command.getSupportsOperation(operation)) {
                logger.warn("Command {} does not support operation {}", commandName, operation);
                
                // Return allowed methods
                List<String> allowedMethods = Arrays.stream(command.getRestOperations())
                    .map(this::toHttpMethod)
                    .collect(Collectors.toList());
                
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .header("Allow", String.join(", ", allowedMethods))
                    .body(Map.of(
                        "error", "Method not allowed. Command supports: " + String.join(", ", allowedMethods),
                        "success", false,
                        "allowedMethods", allowedMethods
                    ));
            }
            
            // Merge query parameters and body into JVS
            JVS jvs = new JVS();
            
            // Add query parameters
            if (queryParams != null) {
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    jvs.set(entry.getKey(), entry.getValue());
                }
            }
            
            // Add body parameters (override query params if same key)
            if (body != null) {
                for (Map.Entry<String, Object> entry : body.entrySet()) {
                    jvs.set(entry.getKey(), entry.getValue());
                }
            }
            
            // Create response handler
            // Use ByteArrayOutputStream to capture output, but also provide servlet objects
            // for commands that need direct access (e.g., streaming, file downloads)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            com.hitorro.util.commandandcontrol.json.JSONResponse hitorroResponse = 
                new com.hitorro.util.commandandcontrol.json.JSONResponse(commandName, outputStream);
            
            // CRITICAL: Set HttpServletRequest and HttpServletResponse on the Response object
            // This allows commands to access the raw servlet objects for streaming,
            // custom headers, file downloads, etc.
            hitorroResponse.setHttpRequest(request);
            hitorroResponse.setHttpResponse(servletResponse);
            
            // Execute command through Hitorro's CommandRegistry
            // This is the key: we're using Hitorro's infrastructure, not reimplementing
            CommandSession session = new CommandSession(false);
            boolean success = registry.execute("", commandName, jvs, hitorroResponse, session);
            
            hitorroResponse.end();
            
            // IMPORTANT: Check if the response was already committed
            // If a command wrote directly to the HttpServletResponse (for streaming, etc.),
            // we should NOT try to return a ResponseEntity as the response is already sent
            if (servletResponse.isCommitted()) {
                logger.debug("Response already committed by command {} - command handled response directly", 
                    commandName);
                // Return null - Spring knows not to write anything else
                return null;
            }
            
            // Parse NDJSON output
            Object result = parseNDJSON(outputStream.toString("UTF-8"));
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Build response
            Map<String, Object> responseMap = new LinkedHashMap<>();
            responseMap.put("success", success);
            responseMap.put("command", commandName);
            responseMap.put("operation", operation.toString());
            responseMap.put("result", result);
            responseMap.put("executionTimeMs", executionTime);
            
            return ResponseEntity.ok(responseMap);
            
        } catch (Exception e) {
            logger.error("Error executing command: " + commandName, e);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "command", commandName,
                    "operation", operation.toString(),
                    "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    "executionTimeMs", executionTime
                ));
        }
    }
    
    /**
     * Parse newline-delimited JSON (NDJSON) output from JSONResponse.
     * JSONResponse writes each row as a separate JSON object on its own line.
     */
    private Object parseNDJSON(String ndjson) {
        if (ndjson == null || ndjson.trim().isEmpty()) {
            return null;
        }
        
        List<Object> rows = new ArrayList<>();
        String[] lines = ndjson.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            try {
                // Parse JSON line using Jackson
                com.fasterxml.jackson.databind.ObjectMapper mapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
                Object obj = mapper.readValue(line, Object.class);
                rows.add(obj);
            } catch (Exception e) {
                logger.warn("Failed to parse JSON line: {}", line, e);
                rows.add(Map.of("raw", line));
            }
        }
        
        // Return single object if only one row, otherwise array
        if (rows.size() == 1) {
            return rows.get(0);
        }
        return rows;
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
            case LastModified: return "GET"; // LastModified is a GET variant
            default: return operation.toString().toUpperCase();
        }
    }
    
    // ============= DTOs for API responses =============
    
    public static class RestDiscoveryResponse {
        private List<RestEndpointInfo> endpoints;
        private int totalEndpoints;
        private String basePath;
        
        public List<RestEndpointInfo> getEndpoints() { return endpoints; }
        public void setEndpoints(List<RestEndpointInfo> endpoints) { this.endpoints = endpoints; }
        
        public int getTotalEndpoints() { return totalEndpoints; }
        public void setTotalEndpoints(int totalEndpoints) { this.totalEndpoints = totalEndpoints; }
        
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
    }
    
    public static class RestEndpointInfo {
        private String path;
        private String command;
        private String description;
        private List<String> methods;
        private boolean internal;
        private List<RestParameterInfo> parameters;
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<String> getMethods() { return methods; }
        public void setMethods(List<String> methods) { this.methods = methods; }
        
        public boolean isInternal() { return internal; }
        public void setInternal(boolean internal) { this.internal = internal; }
        
        public List<RestParameterInfo> getParameters() { return parameters; }
        public void setParameters(List<RestParameterInfo> parameters) { this.parameters = parameters; }
    }
    
    public static class RestParameterInfo {
        private String name;
        private String description;
        private boolean required;
        private String type;
        private String defaultValue;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    }
}
