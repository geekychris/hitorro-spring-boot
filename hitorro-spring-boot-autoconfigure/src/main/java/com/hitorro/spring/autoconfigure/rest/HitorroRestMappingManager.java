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

import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandRegistry;
import com.hitorro.util.commandandcontrol.RestOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the mapping of Hitorro Commands to Spring REST endpoints.
 * Discovers commands that support REST operations and provides metadata
 * for dynamic endpoint registration.
 */
@Component
public class HitorroRestMappingManager {
    private static final Logger logger = LoggerFactory.getLogger(HitorroRestMappingManager.class);
    
    private final Map<String, RestCommandMapping> restMappings = new HashMap<>();
    
    /**
     * REST command mapping metadata.
     */
    public static class RestCommandMapping {
        private final Command command;
        private final String path;
        private final Set<RestOperations> supportedOperations;
        private final boolean internal;
        
        public RestCommandMapping(Command command, String path, Set<RestOperations> operations, boolean internal) {
            this.command = command;
            this.path = path;
            this.supportedOperations = operations;
            this.internal = internal;
        }
        
        public Command getCommand() { return command; }
        public String getPath() { return path; }
        public Set<RestOperations> getSupportedOperations() { return supportedOperations; }
        public boolean isInternal() { return internal; }
        
        public boolean supportsOperation(RestOperations op) {
            return supportedOperations.contains(op);
        }
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void discoverRestEndpoints() {
        logger.info("Discovering Hitorro REST endpoints from loaded commands...");
        
        CommandRegistry registry = CommandRegistry.getRegistry();
        Collection<Command> commands = registry.getCommands();
        
        int externalCount = 0;
        int internalCount = 0;
        
        for (Command command : commands) {
            RestOperations[] operations = command.getRestOperations();
            if (operations != null && operations.length > 0) {
                String path = command.getCommand();
                boolean internal = command.isInternal();
                
                Set<RestOperations> opSet = new HashSet<>(Arrays.asList(operations));
                RestCommandMapping mapping = new RestCommandMapping(command, path, opSet, internal);
                
                restMappings.put(path, mapping);
                
                if (internal) {
                    internalCount++;
                } else {
                    externalCount++;
                }
                
                logger.debug("Mapped REST endpoint: {} -> {} [{}] (internal={})", 
                    path, command.getClass().getSimpleName(), 
                    String.join(", ", opSet.stream().map(Enum::name).collect(Collectors.toList())),
                    internal);
            }
        }
        
        logger.info("Discovered {} REST endpoints: {} external, {} internal", 
            externalCount + internalCount, externalCount, internalCount);
    }
    
    /**
     * Get all REST command mappings.
     */
    public Map<String, RestCommandMapping> getAllMappings() {
        return Collections.unmodifiableMap(restMappings);
    }
    
    /**
     * Get a specific REST command mapping by path.
     */
    public RestCommandMapping getMapping(String path) {
        return restMappings.get(path);
    }
    
    /**
     * Get all external (publicly accessible) REST endpoints.
     */
    public Map<String, RestCommandMapping> getExternalMappings() {
        return restMappings.entrySet().stream()
            .filter(e -> !e.getValue().isInternal())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Get all internal (admin-only) REST endpoints.
     */
    public Map<String, RestCommandMapping> getInternalMappings() {
        return restMappings.entrySet().stream()
            .filter(e -> e.getValue().isInternal())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
