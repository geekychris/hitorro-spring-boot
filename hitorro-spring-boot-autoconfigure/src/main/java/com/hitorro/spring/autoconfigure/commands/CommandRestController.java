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

import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandRegistry;
import com.hitorro.util.commandandcontrol.CommandSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller that exposes Hitorro commands as HTTP endpoints.
 * Commands registered via @CommandDef annotations are available at /api/commands/*
 */
@RestController
@RequestMapping("${hitorro.commands.rest.base-path:/api/commands}")
public class CommandRestController {

    private static final Logger logger = LoggerFactory.getLogger(CommandRestController.class);

    private final CommandRegistrationManager registrationManager;

    public CommandRestController(CommandRegistrationManager registrationManager) {
        this.registrationManager = registrationManager;
        logger.info("Command REST endpoints registered at: ${hitorro.commands.rest.base-path:/api/commands}");
    }

    /**
     * Execute a command and return the result.
     * 
     * POST /api/commands/execute
     * Body: { "command": "help" }
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeCommand(@RequestBody Map<String, String> request) {
        String commandLine = request.get("command");
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Command is required"));
        }

        try {
            // Execute command using CommandSession
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            StringBuilder builder = new StringBuilder();
            CommandSession session = new CommandSession();

            logger.debug("Executing command: {}", commandLine);
            
            CommandSession.executeToLog(commandLine, "");
            
            // Capture output
            String output = sw.toString();
            if (output.isEmpty() && builder.length() > 0) {
                output = builder.toString();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("command", commandLine);
            response.put("output", output);
            response.put("success", true);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error executing command: {}", commandLine, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("command", commandLine);
            response.put("error", e.getMessage());
            response.put("success", false);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * List all available commands.
     * 
     * GET /api/commands/list
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listCommands() {
        CommandRegistry registry = registrationManager.getCommandRegistry();
        java.util.Collection<com.hitorro.util.commandandcontrol.Command> commands = registry.getCommands();

        List<Map<String, String>> commandList = commands.stream()
            .map(cmd -> {
                Map<String, String> info = new HashMap<>();
                info.put("name", cmd.toString());
                // Add more details if Command has getDescription(), etc.
                return info;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("total", commands.size());
        response.put("commands", commandList);

        return ResponseEntity.ok(response);
    }

    /**
     * Get help for a specific command.
     * 
     * GET /api/commands/help/{commandName}
     */
    @GetMapping("/help/{commandName}")
    public ResponseEntity<Map<String, Object>> getCommandHelp(@PathVariable String commandName) {
        try {
            // Execute "help {commandName}"
            String commandLine = "help " + commandName;
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            StringBuilder builder = new StringBuilder();
            
            CommandSession.executeToLog(commandLine, "");
            
            String output = sw.toString();
            if (output.isEmpty() && builder.length() > 0) {
                output = builder.toString();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("command", commandName);
            response.put("help", output);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "Command not found: " + commandName));
        }
    }
}
