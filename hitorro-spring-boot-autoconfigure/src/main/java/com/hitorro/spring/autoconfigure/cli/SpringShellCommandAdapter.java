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

import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandRegistry;
import com.hitorro.util.commandandcontrol.CommandSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Adapter that exposes Hitorro commands via Spring Shell.
 * This provides a Spring-native CLI experience while delegating to Hitorro's command system.
 * 
 * Note: Actual Spring Shell commands will be registered dynamically based on
 * CommandRegistry contents. This adapter provides the bridge between the two systems.
 */
public class SpringShellCommandAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SpringShellCommandAdapter.class);

    public SpringShellCommandAdapter() {
        logger.info("Spring Shell adapter initialized for Hitorro commands");
    }

    /**
     * Execute a Hitorro command from Spring Shell.
     * 
     * @param commandLine The full command line to execute
     * @return Command output as string
     */
    public String executeCommand(String commandLine) {
        try {
            CommandSession.executeToLog(commandLine, "");
            return "Command executed - check logs for output";
        } catch (Exception e) {
            logger.error("Error executing command via Spring Shell: {}", commandLine, e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get all available Hitorro commands.
     * This can be used to dynamically register Spring Shell commands.
     * 
     * @return Array of command names
     */
    public String[] getAvailableCommands() {
        // Note: This is a placeholder. In practice, you would iterate through
        // CommandRegistry to get all commands. For now, return empty array.
        return new String[0];
    }
}
