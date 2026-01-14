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

import com.hitorro.spring.autoconfigure.HitorroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Hitorro command system integration.
 * Registers commands from loaded services and exposes them via REST endpoints.
 */
@AutoConfiguration
@EnableConfigurationProperties(HitorroProperties.class)
@ConditionalOnProperty(prefix = "hitorro.commands", name = "enabled", havingValue = "true", matchIfMissing = true)
@org.springframework.boot.autoconfigure.AutoConfigureAfter(
    com.hitorro.spring.autoconfigure.service.HitorroServiceAutoConfiguration.class)
public class CommandAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CommandAutoConfiguration.class);

    /**
     * Create the command registration manager that scans services for @CommandDef methods.
     */
    @Bean
    public CommandRegistrationManager commandRegistrationManager() {
        logger.info("Creating Command Registration Manager");
        return new CommandRegistrationManager();
    }

    /**
     * Create REST controller for command execution if REST is enabled.
     */
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.commands.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CommandRestController commandRestController(CommandRegistrationManager registrationManager) {
        logger.info("Creating Command REST Controller");
        return new CommandRestController(registrationManager);
    }
}
