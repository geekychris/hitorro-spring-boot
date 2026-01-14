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
import com.hitorro.spring.autoconfigure.HitorroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

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
        logger.info("Initializing JsonTypeSystem");
        
        // Configure type definition directory
        // JVS loads type definitions from: ${HT_BIN}/config/types/core/*.json
        String htBin = System.getProperty("HT_BIN");
        if (htBin == null) {
            htBin = System.getenv("HT_BIN");
        }
        
        if (htBin == null) {
            // Try to get from Spring configuration
            String typeDir = properties.getJvs().getTypeDefinitionsPath();
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
                logger.info("Set HT_BIN from configuration: {}", htBin);
            } else {
                // Default to classpath
                logger.warn("HT_BIN not configured. Type definitions may not load correctly.");
                logger.warn("Set hitorro.jvs.type-definitions-path or HT_BIN environment variable.");
                logger.warn("Type definitions should be in: ${HT_BIN}/config/types/core/*.json");
            }
        } else {
            logger.info("HT_BIN already configured: {}", htBin);
        }
        
        // Initialize the singleton
        this.jsonTypeSystem = JsonTypeSystem.getMe();
        
        logger.info("JsonTypeSystem initialized successfully");
        logger.info("Type definitions path: {}/config/types/core/", htBin != null ? htBin : "<not set>");
        
        if (properties.getJvs().isNlpEnabled()) {
            logger.info("NLP-aware features will be available once NLP integration initializes");
        }
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
}
