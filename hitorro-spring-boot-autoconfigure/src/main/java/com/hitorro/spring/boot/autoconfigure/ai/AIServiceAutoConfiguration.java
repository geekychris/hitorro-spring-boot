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
package com.hitorro.spring.boot.autoconfigure.ai;

import com.hitorro.basedms.transformer.ai.AIService;
import com.hitorro.basedms.transformer.ai.AIServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for AI services using Spring AI with Ollama backend.
 * Enabled when hitorro.ai.enabled=true and Spring AI is on the classpath.
 */
@Configuration
@ConditionalOnClass(OllamaApi.class)
@ConditionalOnProperty(prefix = "hitorro.ai", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(AIServiceProperties.class)
public class AIServiceAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AIServiceAutoConfiguration.class);
    
    /**
     * Wrap the Spring AI ChatClient and EmbeddingModel beans that are auto-configured
     * by the spring-ai-ollama-spring-boot-starter.
     * 
     * Note: This assumes you have configured Spring AI properties in application.yml:
     * 
     * spring:
     *   ai:
     *     ollama:
     *       base-url: http://localhost:11434
     *       chat:
     *         options:
     *           model: llama3.2
     *           temperature: 0.7
     *       embedding:
     *         options:
     *           model: nomic-embed-text
     */
    @Bean
    public AIService aiService(ChatClient.Builder chatClientBuilder, 
                               EmbeddingModel embeddingModel,
                               AIServiceProperties properties) {
        logger.info("Initializing AI service with Ollama backend");
        
        // Build chat client from the autoconfigured builder
        ChatClient chatClient = chatClientBuilder.build();
        
        OllamaAIService service = new OllamaAIService(
            chatClient, 
            embeddingModel, 
            properties.getModelName()
        );
        
        // Register the service globally for transformers to use
        AIServiceRegistry.setInstance(service);
        
        logger.info("AI service registered: {}", service.getServiceName());
        
        return service;
    }
}
