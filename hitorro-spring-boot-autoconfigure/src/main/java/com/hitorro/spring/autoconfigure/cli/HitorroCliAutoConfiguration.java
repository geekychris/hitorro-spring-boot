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

import com.hitorro.spring.autoconfigure.HitorroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Hitorro CLI integration.
 * Supports three CLI access modes:
 * 1. Native Hitorro CLI (telnet/SSH) - preserved from original framework
 * 2. Spring Shell integration - Spring-native CLI experience
 * 3. Spring Boot Actuator endpoints - CLI via HTTP
 */
@AutoConfiguration
@EnableConfigurationProperties(HitorroProperties.class)
public class HitorroCliAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(HitorroCliAutoConfiguration.class);

    /**
     * Configure native Hitorro CLI (telnet/SSH).
     * This preserves the original Hitorro CLI functionality.
     */
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.cli", name = "native-enabled", havingValue = "true", matchIfMissing = true)
    public NativeCliManager nativeCliManager(HitorroProperties properties) {
        int telnetPort = properties.getCli().getTelnetPort();
        int sshPort = properties.getCli().getSshPort();
        
        logger.info("Configuring Hitorro native CLI");
        logger.info("  Telnet port: {}", telnetPort);
        logger.info("  SSH port: {}", sshPort);
        
        return new NativeCliManager(telnetPort, sshPort);
    }

    /**
     * Configure Spring Shell integration for Hitorro commands.
     * This provides a Spring-native CLI experience.
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.shell.standard.ShellComponent")
    @ConditionalOnProperty(prefix = "hitorro.cli", name = "spring-shell-enabled", havingValue = "true", matchIfMissing = true)
    public SpringShellCommandAdapter springShellCommandAdapter() {
        logger.info("Configuring Spring Shell integration for Hitorro commands");
        return new SpringShellCommandAdapter();
    }

    /**
     * Configure Actuator endpoint for command execution.
     * This allows CLI access via HTTP when Spring Boot Actuator is available.
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnProperty(prefix = "hitorro.cli", name = "actuator-enabled", havingValue = "true", matchIfMissing = true)
    public CommandActuatorEndpoint commandActuatorEndpoint() {
        logger.info("Configuring Actuator endpoint for Hitorro commands at /actuator/hitorro/command");
        return new CommandActuatorEndpoint();
    }
}
