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
package com.hitorro.spring.autoconfigure.cli;

import com.hitorro.util.commandandcontrol.CommandRegistry;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.ConsoleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Spring Boot Actuator endpoint for executing Hitorro commands via HTTP.
 * Accessible at /actuator/hitorro/command when enabled.
 * 
 * Note: This is a placeholder. The actual @Endpoint annotation will be added
 * when we confirm Spring Boot Actuator is available.
 */
public class CommandActuatorEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(CommandActuatorEndpoint.class);

    /**
     * Execute a command via the actuator endpoint.
     * 
     * @param command The command to execute
     * @return Map containing execution results
     */
    public Map<String, Object> executeCommand(String command) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConsoleResponse response = new ConsoleResponse(new PrintStream(outputStream));
            
            CommandSession session = new CommandSession(response);
            boolean success = session.executeCommand(command);
            
            result.put("success", success);
            result.put("command", command);
            result.put("output", outputStream.toString());
            
        } catch (Exception e) {
            logger.error("Error executing command via actuator: {}", command, e);
            result.put("success", false);
            result.put("command", command);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * List all available commands.
     * 
     * @return Map containing list of commands
     */
    public Map<String, Object> listCommands() {
        Map<String, Object> result = new HashMap<>();
        Set<String> commands = CommandRegistry.getRegistry().getCommandNames();
        result.put("commands", commands);
        result.put("count", commands.size());
        return result;
    }
}
