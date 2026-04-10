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

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.util.cmdline.BaseCommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Boot-aware implementation of BaseCommandLine.
 * This provides the minimal CommandLine interface needed for CLI sessions
 * to work properly in a Spring Boot environment.
 */
public class SpringBootCommandLine extends BaseCommandLine<SpringBootCommandLine> {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringBootCommandLine.class);
    
    private final String applicationName;
    
    public SpringBootCommandLine(String applicationName) {
        this.applicationName = applicationName;
        this.commandLine = applicationName;
        this.command = applicationName;
        this.processStartTime = System.currentTimeMillis();
        
        // Register this as the global CommandLine instance
        BaseCommandLine.setCommandLine(this);
        
        logger.debug("SpringBootCommandLine registered for application: {}", applicationName);
    }
    
    @Override
    protected void setupPrimordialLogging() {
        // Spring Boot handles logging setup
        logger.debug("Primordial logging delegated to Spring Boot");
    }
    
    @Override
    protected void setupLogging() {
        // Spring Boot handles logging setup
        logger.debug("Logging setup delegated to Spring Boot");
    }
    
    @Override
    public boolean haveJVSConfigsChanged() {
        // In Spring Boot, config changes are handled by Spring's property system
        return false;
    }
    
    @Override
    public JsonNode reloadJVSProps(boolean runDiff) {
        // Properties already loaded by JsonTypeSystemManager
        logger.debug("JVS properties already loaded by Spring Boot autoconfiguration");
        return commandLineArguments;
    }
    
    @Override
    protected void initConfigChangeWatching() {
        // In Spring Boot, config changes are handled by Spring Cloud Config or similar
        logger.debug("Config change watching delegated to Spring Boot");
    }
    
    @Override
    protected boolean setupLogWatching() {
        // Spring Boot handles log rotation through logback/log4j2 configuration
        logger.debug("Log watching delegated to Spring Boot");
        return false;
    }
    
    @Override
    protected com.hitorro.util.startupframework.ServiceContext getServiceContext() {
        // Spring Boot doesn't use Hitorro's ServiceContext - return null
        // CLI commands that need services should use Spring's ApplicationContext instead
        return null;
    }
    
    @Override
    public String toString() {
        return "SpringBootCommandLine[" + applicationName + ", uptime=" + 
               (System.currentTimeMillis() - processStartTime) + "ms]";
    }
}
