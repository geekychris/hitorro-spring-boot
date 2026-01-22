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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AI service
 */
@ConfigurationProperties(prefix = "hitorro.ai")
public class AIServiceProperties {
    
    /**
     * Enable AI service integration
     */
    private boolean enabled = false;
    
    /**
     * Ollama server base URL
     */
    private String ollamaUrl = "http://localhost:11434";
    
    /**
     * Model name for chat/completion tasks (e.g., "llama3.2", "mistral", "phi3")
     */
    private String modelName = "llama3.2";
    
    /**
     * Model name for embedding tasks (e.g., "nomic-embed-text", "mxbai-embed-large")
     */
    private String embeddingModel = "nomic-embed-text";
    
    /**
     * Temperature for generation (0.0-1.0, higher = more creative)
     */
    private double temperature = 0.7;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getOllamaUrl() {
        return ollamaUrl;
    }
    
    public void setOllamaUrl(String ollamaUrl) {
        this.ollamaUrl = ollamaUrl;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getEmbeddingModel() {
        return embeddingModel;
    }
    
    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
