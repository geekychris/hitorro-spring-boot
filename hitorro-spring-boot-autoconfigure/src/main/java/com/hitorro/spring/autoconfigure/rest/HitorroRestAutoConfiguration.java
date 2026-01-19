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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Hitorro REST endpoints.
 * 
 * This configuration automatically enables REST API endpoints for Hitorro commands
 * that declare restOperations in their @CommandDef annotation.
 * 
 * Commands become accessible at: /api/rest/{commandName}
 * Discovery endpoint available at: /api/rest
 */
@Configuration
@ConditionalOnProperty(name = "hitorro.rest.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(HitorroRestProperties.class)
public class HitorroRestAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(HitorroRestAutoConfiguration.class);
    
    @Bean
    public HitorroRestController hitorroRestController() {
        logger.info("✓ Hitorro REST endpoints enabled at ${hitorro.rest.base-path:/api/rest}");
        logger.info("  REST-enabled commands will be automatically exposed as HTTP endpoints");
        logger.info("  Discovery endpoint: GET ${hitorro.rest.base-path:/api/rest}");
        return new HitorroRestController();
    }
}
