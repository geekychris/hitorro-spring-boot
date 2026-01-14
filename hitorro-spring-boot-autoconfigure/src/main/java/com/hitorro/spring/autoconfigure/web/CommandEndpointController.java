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
package com.hitorro.spring.autoconfigure.web;

import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.json.JSONResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller that exposes Hitorro command execution via HTTP endpoints.
 * Delegates to Hitorro's CommandSession for actual command execution.
 */
@RestController
public class CommandEndpointController {

    private static final Logger logger = LoggerFactory.getLogger(CommandEndpointController.class);

    private final String basePath;

    public CommandEndpointController(String basePath) {
        this.basePath = basePath;
    }

    /**
     * Execute a Hitorro command via POST request.
     * 
     * @param request Command execution request containing command and arguments
     * @return ResponseEntity with command output
     */
    @PostMapping(value = "${hitorro.commands.rest.base-path:/api/commands}/execute", 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> executeCommand(@RequestBody CommandRequest request) {
        logger.debug("Executing command: {}", request.getCommand());
        
        try {
            // Create a response handler that captures output
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JSONResponse response = new JSONResponse(new PrintStream(outputStream));
            
            // Execute the command
            CommandSession session = new CommandSession(response);
            boolean success = session.executeCommand(request.getCommand());
            
            // Build response
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("command", request.getCommand());
            
            // Try to parse JSON output if available
            String output = outputStream.toString();
            if (!output.isEmpty()) {
                result.put("output", output);
            }
            
            HttpStatus status = success ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(result);
            
        } catch (Exception e) {
            logger.error("Error executing command: {}", request.getCommand(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("command", request.getCommand());
            errorResult.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Get list of available commands.
     * 
     * @return List of registered commands
     */
    @GetMapping(value = "${hitorro.commands.rest.base-path:/api/commands}/list",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listCommands() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JSONResponse response = new JSONResponse(new PrintStream(outputStream));
            
            CommandSession session = new CommandSession(response);
            session.executeCommand("help");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("commands", outputStream.toString());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error listing commands", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Request body for command execution.
     */
    public static class CommandRequest {
        private String command;
        private Map<String, String> arguments;

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public Map<String, String> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, String> arguments) {
            this.arguments = arguments;
        }
    }
}
