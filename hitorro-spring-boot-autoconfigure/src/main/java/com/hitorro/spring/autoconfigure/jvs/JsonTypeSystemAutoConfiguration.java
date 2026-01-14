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
import com.hitorro.language.HTJWNLDictionary;
import com.hitorro.spring.autoconfigure.HitorroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Hitorro's JSON Type System (JVS) with NLP support.
 * 
 * <p>The JSON Type System is a sophisticated type-aware JSON processing framework
 * with Natural Language Processing capabilities via WordNet and OpenNLP integration.
 * 
 * <p>Features:
 * <ul>
 *   <li>Type-safe JSON document manipulation</li>
 *   <li>Multi-language support with segmentation</li>
 *   <li>NLP analysis (NER, POS tagging, parsing)</li>
 *   <li>WordNet integration for semantic analysis</li>
 *   <li>Dynamic property access via propaccess notation</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(name = {
    "com.hitorro.jsontypesystem.JsonTypeSystem",
    "com.hitorro.jsontypesystem.JVS"
})
@ConditionalOnProperty(prefix = "hitorro.jvs", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(HitorroProperties.class)
public class JsonTypeSystemAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonTypeSystemAutoConfiguration.class);
    
    /**
     * Creates a bean for the JsonTypeSystem singleton.
     * This provides access to type definitions and configurations.
     */
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.jvs", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JsonTypeSystemManager jsonTypeSystemManager(HitorroProperties properties) {
        logger.info("Initializing Hitorro JSON Type System (JVS)");
        return new JsonTypeSystemManager(properties);
    }
    
    /**
     * Creates a bean for WordNet dictionary integration.
     * This enables NLP-aware features in JVS documents.
     */
    @Bean
    @ConditionalOnClass(name = "com.hitorro.language.HTJWNLDictionary")
    @ConditionalOnProperty(prefix = "hitorro.jvs.nlp", name = "enabled", havingValue = "true", matchIfMissing = false)
    public NLPIntegrationManager nlpIntegrationManager(HitorroProperties properties) {
        logger.info("Initializing Hitorro NLP Integration (WordNet, OpenNLP)");
        return new NLPIntegrationManager(properties);
    }
    
    /**
     * Creates REST endpoints for JVS operations.
     */
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.jvs.rest", name = "enabled", havingValue = "true", matchIfMissing = false)
    public JVSRestController jvsRestController(JsonTypeSystemManager jtsManager) {
        logger.info("Enabling JVS REST endpoints");
        return new JVSRestController(jtsManager);
    }
}
