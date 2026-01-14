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

import com.hitorro.language.HTJWNLDictionary;
import com.hitorro.spring.autoconfigure.HitorroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Manager for Hitorro's NLP integration components.
 * 
 * <p>Provides access to:
 * <ul>
 *   <li>WordNet dictionary (via JWNL) - Lexical database for semantic analysis</li>
 *   <li>OpenNLP models - Tokenization, POS tagging, NER, parsing</li>
 *   <li>Language processing - Multi-language text analysis</li>
 * </ul>
 * 
 * <p>NLP-aware features in JVS documents:
 * <ul>
 *   <li>Automatic text segmentation</li>
 *   <li>Named Entity Recognition (NER)</li>
 *   <li>Part-of-Speech (POS) tagging</li>
 *   <li>Syntactic parsing</li>
 *   <li>Semantic analysis via WordNet</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> Requires WordNet dictionary data to be available
 * in the configured directory (default: ${hitorro.home}/data/wordnet).
 */
public class NLPIntegrationManager implements InitializingBean {
    
    private static final Logger logger = LoggerFactory.getLogger(NLPIntegrationManager.class);
    
    private final HitorroProperties properties;
    private HTJWNLDictionary wordnetDictionary;
    private boolean nlpAvailable = false;
    
    public NLPIntegrationManager(HitorroProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Initializing NLP Integration");
        
        try {
            // Initialize WordNet dictionary
            this.wordnetDictionary = HTJWNLDictionary.me.get();
            this.nlpAvailable = true;
            
            logger.info("NLP Integration initialized successfully");
            logger.info("WordNet dictionary loaded - NLP-aware JVS features available");
            
        } catch (Exception e) {
            logger.warn("Failed to initialize NLP components: {}", e.getMessage());
            logger.warn("JVS will work without NLP features. To enable NLP:");
            logger.warn("  1. Install WordNet data in: ${hitorro.home}/data/wordnet");
            logger.warn("  2. Configure OpenNLP models directory");
            logger.info("Continuing without NLP support...");
            this.nlpAvailable = false;
        }
    }
    
    /**
     * Check if NLP features are available.
     * 
     * @return true if WordNet and OpenNLP are initialized
     */
    public boolean isNLPAvailable() {
        return nlpAvailable;
    }
    
    /**
     * Get the WordNet dictionary instance.
     * 
     * @return WordNet dictionary, or null if not available
     */
    public HTJWNLDictionary getWordnetDictionary() {
        return wordnetDictionary;
    }
}
