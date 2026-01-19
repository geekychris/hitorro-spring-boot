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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Hitorro REST endpoints.
 */
@ConfigurationProperties(prefix = "hitorro.rest")
public class HitorroRestProperties {
    
    /**
     * Enable or disable Hitorro REST endpoints.
     * When disabled, commands will not be exposed as REST endpoints.
     */
    private boolean enabled = true;
    
    /**
     * Base path for REST endpoints.
     * Commands will be accessible at: {basePath}/{commandName}
     */
    private String basePath = "/api/rest";
    
    /**
     * Whether to expose internal commands via REST endpoints.
     * Internal commands are typically for debugging and system administration.
     */
    private boolean exposeInternal = false;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getBasePath() {
        return basePath;
    }
    
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
    public boolean isExposeInternal() {
        return exposeInternal;
    }
    
    public void setExposeInternal(boolean exposeInternal) {
        this.exposeInternal = exposeInternal;
    }
}
