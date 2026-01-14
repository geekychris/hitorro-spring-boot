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
import com.hitorro.jsontypesystem.JVSUtils;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.RestOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC controller that exposes Hitorro Commands as REST endpoints.
 * Mimics the behavior of Hitorro's ActionServlet but integrates with Spring MVC.
 */
@RestController
@RequestMapping("${hitorro.rest.base-path:/api/rest}")
public class HitorroRestController {
    private static final Logger logger = LoggerFactory.getLogger(HitorroRestController.class);
    
    private final HitorroRestMappingManager mappingManager;
    
    public HitorroRestController(HitorroRestMappingManager mappingManager) {
        this.mappingManager = mappingManager;
    }
    
    /**
     * List all available REST endpoints.
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> listEndpoints() {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, HitorroRestMappingManager.RestCommandMapping> external = 
            mappingManager.getExternalMappings();
        Map<String, HitorroRestMappingManager.RestCommandMapping> internal = 
            mappingManager.getInternalMappings();
        
        result.put("external_endpoints", external.keySet());
        result.put("internal_endpoints", internal.keySet());
        result.put("total_count", external.size() + internal.size());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Handle GET requests to Hitorro REST endpoints.
     */
    @GetMapping("/{commandPath}/**")
    public ResponseEntity<byte[]> handleGet(
            @PathVariable String commandPath,
            @RequestParam Map<String, String> params,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return handleRequest(commandPath, params, RestOperations.Get, request, response, null);
    }
    
    /**
     * Handle POST requests to Hitorro REST endpoints.
     */
    @PostMapping("/{commandPath}/**")
    public ResponseEntity<byte[]> handlePost(
            @PathVariable String commandPath,
            @RequestParam Map<String, String> params,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return handleRequest(commandPath, params, RestOperations.Post, request, response, request.getInputStream());
    }
    
    /**
     * Handle PUT requests to Hitorro REST endpoints.
     */
    @PutMapping("/{commandPath}/**")
    public ResponseEntity<byte[]> handlePut(
            @PathVariable String commandPath,
            @RequestParam Map<String, String> params,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return handleRequest(commandPath, params, RestOperations.Put, request, response, request.getInputStream());
    }
    
    /**
     * Handle DELETE requests to Hitorro REST endpoints.
     */
    @DeleteMapping("/{commandPath}/**")
    public ResponseEntity<byte[]> handleDelete(
            @PathVariable String commandPath,
            @RequestParam Map<String, String> params,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return handleRequest(commandPath, params, RestOperations.Delete, request, response, null);
    }
    
    /**
     * Core request handling logic that bridges to Hitorro's Command system.
     */
    private ResponseEntity<byte[]> handleRequest(
            String commandPath,
            Map<String, String> params,
            RestOperations operation,
            HttpServletRequest request,
            HttpServletResponse response,
            InputStream inputStream) throws IOException {
        
        // Get the command mapping
        HitorroRestMappingManager.RestCommandMapping mapping = mappingManager.getMapping(commandPath);
        
        if (mapping == null) {
            logger.warn("No REST endpoint found for path: {}", commandPath);
            return ResponseEntity.notFound().build();
        }
        
        Command command = mapping.getCommand();
        
        // Check if operation is supported
        if (!mapping.supportsOperation(operation)) {
            logger.warn("Operation {} not supported for command: {}", operation, commandPath);
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }
        
        // Check internal vs external access
        if (mapping.isInternal()) {
            // TODO: Add authorization check for internal commands
            // For now, log a warning
            logger.warn("Internal command {} accessed via REST - consider adding authorization", commandPath);
        }
        
        try {
            // Convert params to JVS
            JVS jvs = JVSUtils.convertMapToJVS(params, new JVS());
            
            // Create output stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Create Response - using JSON format by default
            Response resp = new com.hitorro.util.commandandcontrol.json.JSONResponse(command.getCommand(), outputStream);
            resp.setHttpRequest(request);
            resp.setHttpResponse(response);
            
            // Create CommandSession
            CommandSession session = new CommandSession();
            
            // Execute the command
            long executionTime = command.executeRestfull("", jvs, resp, session, operation, inputStream);
            
            resp.end();
            outputStream.flush();
            
            byte[] responseBytes = outputStream.toByteArray();
            
            logger.debug("Executed REST command {} in {}ms", commandPath, executionTime);
            
            // Return response
            return ResponseEntity.ok()
                .header("X-Execution-Time-Ms", String.valueOf(executionTime))
                .body(responseBytes);
                
        } catch (Exception e) {
            logger.error("Error executing REST command: {}", commandPath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Error: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * Get information about a specific endpoint.
     */
    @GetMapping("/_info/{commandPath}")
    public ResponseEntity<Map<String, Object>> getEndpointInfo(@PathVariable String commandPath) {
        HitorroRestMappingManager.RestCommandMapping mapping = mappingManager.getMapping(commandPath);
        
        if (mapping == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("path", mapping.getPath());
        info.put("command_class", mapping.getCommand().getClass().getName());
        info.put("supported_operations", mapping.getSupportedOperations());
        info.put("internal", mapping.isInternal());
        info.put("description", mapping.getCommand().getDescription());
        
        return ResponseEntity.ok(info);
    }
}
