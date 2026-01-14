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

import com.hitorro.spring.autoconfigure.HitorroProperties;
import com.hitorro.spring.autoconfigure.service.HitorroServiceAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Hitorro REST endpoint integration.
 * Discovers Commands that support REST operations and exposes them via Spring MVC.
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.hitorro.util.commandandcontrol.Command")
@ConditionalOnProperty(name = "hitorro.rest.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(HitorroProperties.class)
@AutoConfigureAfter(HitorroServiceAutoConfiguration.class)
public class HitorroRestAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(HitorroRestAutoConfiguration.class);
    
    public HitorroRestAutoConfiguration() {
        logger.info("Initializing Hitorro REST auto-configuration");
    }
    
    /**
     * Create the REST mapping manager that discovers REST-enabled commands.
     */
    @Bean
    public HitorroRestMappingManager hitorroRestMappingManager() {
        logger.info("Creating HitorroRestMappingManager");
        return new HitorroRestMappingManager();
    }
    
    /**
     * Create the REST controller that handles HTTP requests to REST commands.
     */
    @Bean
    public HitorroRestController hitorroRestController(HitorroRestMappingManager mappingManager) {
        logger.info("Creating HitorroRestController");
        return new HitorroRestController(mappingManager);
    }
}
