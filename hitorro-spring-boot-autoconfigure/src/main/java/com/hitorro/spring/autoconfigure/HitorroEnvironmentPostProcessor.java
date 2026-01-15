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
package com.hitorro.spring.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Environment post-processor that ensures HT_BIN and HT_HOME system properties
 * are configured early in the Spring Boot startup process.
 * 
 * <p>This runs BEFORE application context initialization, ensuring Hitorro
 * framework components can access these properties during bean creation.
 * 
 * <p>Configuration priority:
 * <ol>
 *   <li>JVM system properties (-DHT_BIN=...)</li>
 *   <li>Environment variables (export HT_BIN=...)</li>
 *   <li>Spring configuration (hitorro.jvs.type-definitions-path)</li>
 * </ol>
 */
public class HitorroEnvironmentPostProcessor implements EnvironmentPostProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(HitorroEnvironmentPostProcessor.class);
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Configure HT_BIN (must be first)
        configureHtBin(environment);
        
        // Configure HT_HOME
        configureHtHome(environment);
        
        // Configure ht_data (derived from HT_BIN)
        configureHtData();
    }
    
    private void configureHtBin(Environment environment) {
        String htBin = System.getProperty("HT_BIN");
        
        if (htBin != null) {
            logger.debug("HT_BIN already set via system property: {}", htBin);
            return;
        }
        
        // Try environment variable
        htBin = System.getenv("HT_BIN");
        if (htBin != null) {
            System.setProperty("HT_BIN", htBin);
            logger.info("HT_BIN set from environment variable: {}", htBin);
            return;
        }
        
        // Try Spring configuration - NEW: hitorro.ht-bin (preferred)
        htBin = environment.getProperty("hitorro.ht-bin");
        if (htBin != null && !htBin.isEmpty()) {
            System.setProperty("HT_BIN", htBin);
            logger.info("HT_BIN set from Spring configuration (hitorro.ht-bin): {}", htBin);
            return;
        }
        
        // Try legacy JVS type-definitions-path (for backward compatibility)
        String typePath = environment.getProperty("hitorro.jvs.type-definitions-path");
        if (typePath != null && !typePath.isEmpty()) {
            // Extract parent directory (strip /config/types if present)
            if (typePath.endsWith("/config/types") || typePath.endsWith("/config/types/")) {
                htBin = typePath.substring(0, typePath.indexOf("/config/types"));
            } else if (typePath.endsWith("/config")) {
                htBin = typePath.substring(0, typePath.indexOf("/config"));
            } else {
                htBin = typePath;
            }
            System.setProperty("HT_BIN", htBin);
            logger.info("HT_BIN set from Spring configuration (hitorro.jvs.type-definitions-path): {}", htBin);
            return;
        }
        
        logger.warn("╔════════════════════════════════════════════════════════════╗");
        logger.warn("║ WARNING: HT_BIN not configured                             ║");
        logger.warn("╠════════════════════════════════════════════════════════════╣");
        logger.warn("║ Configure using one of these methods:                     ║");
        logger.warn("║ 1. application.yml: hitorro.ht-bin: /path/to/hitorro     ║");
        logger.warn("║ 2. System property: -DHT_BIN=/path/to/hitorro            ║");
        logger.warn("║ 3. Environment var: export HT_BIN=/path/to/hitorro       ║");
        logger.warn("╚════════════════════════════════════════════════════════════╝");
    }
    
    private void configureHtHome(Environment environment) {
        String htHome = System.getProperty("HT_HOME");
        
        if (htHome != null) {
            logger.debug("HT_HOME already set via system property: {}", htHome);
            return;
        }
        
        // Try environment variable
        htHome = System.getenv("HT_HOME");
        if (htHome != null) {
            System.setProperty("HT_HOME", htHome);
            logger.info("HT_HOME set from environment variable: {}", htHome);
            return;
        }
        
        // Try Spring configuration - NEW: hitorro.ht-home (preferred)
        htHome = environment.getProperty("hitorro.ht-home");
        if (htHome != null && !htHome.isEmpty()) {
            System.setProperty("HT_HOME", htHome);
            logger.info("HT_HOME set from Spring configuration (hitorro.ht-home): {}", htHome);
            return;
        }
        
        logger.warn("HT_HOME not configured (optional but recommended)");
        logger.warn("  Configure via: hitorro.ht-home in application.yml");
        logger.warn("  Or: -DHT_HOME=/path or export HT_HOME=/path");
    }
    
    private void configureHtData() {
        String htData = System.getProperty("ht_data");
        
        if (htData != null) {
            logger.debug("ht_data already set via system property: {}", htData);
            return;
        }
        
        // Try environment variable
        htData = System.getenv("HT_DATA");
        if (htData != null) {
            System.setProperty("ht_data", htData);
            logger.info("ht_data set from environment variable: {}", htData);
            return;
        }
        
        // Derive from HT_BIN (standard Hitorro convention: ${HT_BIN}/data)
        String htBin = System.getProperty("HT_BIN");
        if (htBin != null) {
            htData = htBin + "/data";
            System.setProperty("ht_data", htData);
            logger.info("ht_data derived from HT_BIN: {}", htData);
            return;
        }
        
        logger.warn("ht_data not configured (derived value unavailable, HT_BIN not set)");
    }
}
