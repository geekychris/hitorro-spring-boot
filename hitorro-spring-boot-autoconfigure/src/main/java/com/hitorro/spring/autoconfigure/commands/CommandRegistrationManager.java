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

import com.hitorro.spring.autoconfigure.service.ServiceContextManager;
import com.hitorro.util.commandandcontrol.CommandRegistry;
import com.hitorro.util.startupframework.ServiceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Automatically registers all @CommandDef methods from loaded Hitorro services
 * with the CommandRegistry, making them available via REST, telnet, SSH, etc.
 */
public class CommandRegistrationManager implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(CommandRegistrationManager.class);

    @Autowired(required = false)
    private ServiceContextManager serviceContextManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (serviceContextManager == null) {
            logger.info("No ServiceContextManager available - skipping command registration");
            return;
        }

        logger.info("Registering commands from Hitorro services...");
        
        CommandRegistry registry = CommandRegistry.getRegistry();
        List<ServiceWrapper> services = serviceContextManager.getAllServices();
        
        int totalCommands = 0;
        java.util.Collection<com.hitorro.util.commandandcontrol.Command> commandsBefore = registry.getCommands();
        int beforeCount = commandsBefore != null ? commandsBefore.size() : 0;
        
        for (ServiceWrapper sw : services) {
            Object serviceObject = sw.getServiceObject();
            if (serviceObject != null) {
                try {
                    // Register all @CommandDef methods from this service
                    registry.addAllFromObject(serviceObject);
                    logger.debug("  - Registered commands from: {}", sw.getShortName());
                } catch (Exception e) {
                    logger.warn("  - Failed to register commands from {}: {}", 
                        sw.getShortName(), e.getMessage());
                }
            }
        }
        
        java.util.Collection<com.hitorro.util.commandandcontrol.Command> commandsAfter = registry.getCommands();
        int afterCount = commandsAfter != null ? commandsAfter.size() : 0;
        totalCommands = afterCount - beforeCount;
        
        logger.info("Command registration complete:");
        logger.info("  - Scanned {} services", services.size());
        logger.info("  - Registered {} new commands", totalCommands);
        logger.info("  - Total commands available: {}", afterCount);
        
        // Log some example commands
        if (afterCount > 0 && commandsAfter != null) {
            logger.info("  Example commands:");
            int count = 0;
            for (com.hitorro.util.commandandcontrol.Command cmd : commandsAfter) {
                if (count >= 5) break;  // Show first 5
                logger.info("    - {}", cmd.toString());
                count++;
            }
            if (afterCount > 5) {
                logger.info("    ... and {} more", afterCount - 5);
            }
        }
    }

    /**
     * Get the command registry for direct access.
     */
    public CommandRegistry getCommandRegistry() {
        return CommandRegistry.getRegistry();
    }
}
