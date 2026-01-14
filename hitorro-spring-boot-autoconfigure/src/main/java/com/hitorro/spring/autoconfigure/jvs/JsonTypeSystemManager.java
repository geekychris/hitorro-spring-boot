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
package com.hitorro.spring.autoconfigure.jvs;

import com.hitorro.jsontypesystem.JsonTypeSystem;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.Type;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.util.core.Env;
import com.hitorro.spring.autoconfigure.HitorroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

/**
 * Manager for Hitorro's JSON Type System integration with Spring Boot.
 * 
 * <p>Provides access to:
 * <ul>
 *   <li>JsonTypeSystem singleton - Type definitions and caching</li>
 *   <li>JVS factory methods - Create type-aware JSON documents</li>
 *   <li>Type lookup - Retrieve type definitions by name</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * @Autowired
 * private JsonTypeSystemManager jtsManager;
 * 
 * // Create a JVS document
 * JVS doc = jtsManager.createJVS("{\"type\":\"article\"}");
 * 
 * // Get a type definition
 * Type articleType = jtsManager.getType("article");
 * }
 * </pre>
 */
public class JsonTypeSystemManager implements InitializingBean {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonTypeSystemManager.class);
    
    private final HitorroProperties properties;
    private JsonTypeSystem jsonTypeSystem;
    
    public JsonTypeSystemManager(HitorroProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("=== Initializing JsonTypeSystem ===");
        
        // CRITICAL: Initialize JVSProperties with system args BEFORE anything else
        // This must happen before any FileProperty or type loading attempts
        initializeJVSProperties();
        
        // Configure type definition directory
        // JVS loads type definitions from: ${HT_BIN}/config/types/core/*.json
        String htBin = System.getProperty("HT_BIN");
        logger.debug("System property HT_BIN: {}", htBin);
        
        if (htBin == null) {
            htBin = System.getenv("HT_BIN");
            logger.debug("Environment variable HT_BIN: {}", htBin);
        }
        
        if (htBin == null) {
            // Try to get from Spring configuration
            String typeDir = properties.getJvs().getTypeDefinitionsPath();
            logger.debug("Spring configuration type-definitions-path: {}", typeDir);
            
            if (typeDir != null && !typeDir.isEmpty()) {
                // Extract parent directory (strip /config/types if present)
                if (typeDir.endsWith("/config/types") || typeDir.endsWith("/config/types/")) {
                    htBin = typeDir.substring(0, typeDir.indexOf("/config/types"));
                } else if (typeDir.endsWith("/config")) {
                    htBin = typeDir.substring(0, typeDir.indexOf("/config"));
                } else {
                    htBin = typeDir;
                }
                System.setProperty("HT_BIN", htBin);
                logger.info("✓ Set HT_BIN from Spring configuration: {}", htBin);
            } else {
                // No configuration found at all
                logger.error("╔════════════════════════════════════════════════════════════╗");
                logger.error("║ CRITICAL: HT_BIN not configured!                          ║");
                logger.error("╠════════════════════════════════════════════════════════════╣");
                logger.error("║ Type definitions may not load correctly.                  ║");
                logger.error("║                                                            ║");
                logger.error("║ Configure using one of these methods:                     ║");
                logger.error("║ 1. System property: -DHT_BIN=/path/to/hitorro            ║");
                logger.error("║ 2. Environment var: export HT_BIN=/path/to/hitorro       ║");
                logger.error("║ 3. Spring config: hitorro.jvs.type-definitions-path      ║");
                logger.error("║                                                            ║");
                logger.error("║ Type definitions expected at:                             ║");
                logger.error("║   ${HT_BIN}/config/types/core/*.json                     ║");
                logger.error("╚════════════════════════════════════════════════════════════╝");
            }
        } else {
            logger.info("✓ HT_BIN already configured: {}", htBin);
        }
        
        // Also check HT_HOME
        String htHome = System.getProperty("HT_HOME");
        if (htHome == null) {
            htHome = System.getenv("HT_HOME");
        }
        
        if (htHome != null) {
            logger.info("✓ HT_HOME configured: {}", htHome);
        } else {
            logger.warn("⚠ HT_HOME not configured (optional but recommended)");
            logger.warn("  Set via: -DHT_HOME=/path or export HT_HOME=/path");
        }
        
        // Initialize the singleton
        logger.info("Initializing JsonTypeSystem singleton...");
        this.jsonTypeSystem = JsonTypeSystem.getMe();
        
        logger.info("✓ JsonTypeSystem initialized successfully");
        logger.info("  Type definitions path: {}/config/types/core/", htBin != null ? htBin : "<not set>");
        
        if (properties.getJvs().isNlpEnabled()) {
            logger.info("  NLP-aware features enabled (requires language module)");
        }
        
        logger.info("=== JsonTypeSystem initialization complete ===");
    }
    
    /**
     * Get the JsonTypeSystem singleton.
     */
    public JsonTypeSystem getJsonTypeSystem() {
        return jsonTypeSystem;
    }
    
    /**
     * Get a type definition by name.
     * 
     * @param typeName The name of the type (e.g., "article", "user_profile")
     * @return The type definition, or null if not found
     */
    public Type getType(String typeName) {
        return jsonTypeSystem.getType(typeName);
    }
    
    /**
     * Create a new JVS document from JSON string.
     * 
     * @param jsonString JSON string to parse
     * @return JVS document instance
     */
    public JVS createJVS(String jsonString) {
        return JVS.read(jsonString);
    }
    
    /**
     * Create an empty JVS document.
     * 
     * @return Empty JVS document instance
     */
    public JVS createJVS() {
        return new JVS();
    }
    
    /**
     * Check if a type exists.
     * 
     * @param typeName The name of the type
     * @return true if the type is defined
     */
    public boolean hasType(String typeName) {
        return getType(typeName) != null;
    }
    
    /**
     * Initialize JVS properties using Hitorro's standard property loading chain.
     * 
     * This uses the same mechanism as CommandLine.reloadJVSProps() to load properties from:
     * 1. System args (HT_BIN, HT_HOME, ht_data, etc.)
     * 2. ${HT_BIN}/config/ directory (*.properties, *.json)
     * 3. ${HT_HOME}/config/ directory (*.properties, *.json)
     * 4. LoadProps files
     * 5. Saved JVS properties
     * 
     * This ensures FileProperty resolution works correctly and all Hitorro
     * configuration is available (including stores.csv paths, etc.)
     */
    private void initializeJVSProperties() {
        logger.info("Initializing JVSProperties using Hitorro property chain...");
        
        try {
            // Use Hitorro's standard property loading chain (same as CommandLine)
            JVS props = loadPropertiesFromChain();
            
            // Resolve variables (${ht_data}, ${HT_BIN}, etc.)
            props.resolveVariables(props);
            
            // Set as default properties (used by FileProperty for variable resolution)
            com.hitorro.jsontypesystem.propreaders.JVSProperties.setDefaultProperties(props, false);
            
            logger.info("✓ JVSProperties initialized from property chain");
            
            // Log key properties for diagnostics
            if (logger.isDebugEnabled()) {
                logger.debug("  HT_BIN: {}", props.get("HT_BIN"));
                logger.debug("  HT_HOME: {}", props.get("HT_HOME"));
                logger.debug("  ht_data: {}", props.get("ht_data"));
            }
        } catch (Exception e) {
            logger.error("Failed to initialize JVSProperties from property chain", e);
            // Fallback to basic system args
            logger.warn("Falling back to system args only");
            fallbackToSystemArgs();
        }
    }
    
    /**
     * Load properties using the same chain as CommandLine.jvsPropLoaders.
     * This replicates the property loading mechanism from CommandLine.reloadJVSProps().
     */
    private JVS loadPropertiesFromChain() throws Exception {
        JVS props = new JVS();
        
        // Create the same property reader chain as CommandLine
        com.hitorro.jsontypesystem.propreaders.JVSPropertiesReader[] propLoaders = {
            // 1. System args (HT_BIN, HT_HOME, ht_data, etc.)
            new com.hitorro.jsontypesystem.propreaders.JVSSystemArgsPropertyReader(),
            
            // 2. ${HT_BIN}/config/ directory
            new com.hitorro.jsontypesystem.propreaders.JVSDirectoryReadingPropertiesReader(
                com.hitorro.jsontypesystem.propreaders.JVSDirectoryType.Bin, false),
            
            // 3. ${HT_HOME}/config/ directory
            new com.hitorro.jsontypesystem.propreaders.JVSDirectoryReadingPropertiesReader(
                com.hitorro.jsontypesystem.propreaders.JVSDirectoryType.Home, true),
            
            // 4. LoadProps files (if specified)
            new com.hitorro.jsontypesystem.propreaders.JVSLoadPropsPropertyReader(true)
        };
        
        // Load properties from each reader in order (later readers override earlier ones)
        for (com.hitorro.jsontypesystem.propreaders.JVSPropertiesReader reader : propLoaders) {
            reader.getProperties(props, new JVS()); // empty cmdLineArgs for Spring Boot context
        }
        
        return props;
    }
    
    /**
     * Fallback: Just load system args if the full chain fails.
     */
    private void fallbackToSystemArgs() {
        try {
            JVS props = new JVS();
            Map<String, String> systemArgs = Env.getSystemArgs();
            
            for (Map.Entry<String, String> entry : systemArgs.entrySet()) {
                props.set(entry.getKey(), entry.getValue());
            }
            
            com.hitorro.jsontypesystem.propreaders.JVSProperties.setDefaultProperties(props, false);
            logger.info("✓ JVSProperties initialized with system args (fallback mode)");
        } catch (Exception e) {
            logger.error("Failed even fallback initialization", e);
        }
    }
}
