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
package com.hitorro.spring.autoconfigure.service;

import com.hitorro.util.startupframework.ServiceWrapper;

import java.util.List;

/**
 * Factory for accessing Hitorro services from Spring context.
 * Provides type-safe access to Hitorro services.
 */
public class HitorroServiceFactory {

    private final ServiceContextManager contextManager;

    public HitorroServiceFactory(ServiceContextManager contextManager) {
        this.contextManager = contextManager;
    }

    /**
     * Get a Hitorro service by its class type.
     * 
     * @param serviceClass The service class
     * @return The service instance, or null if not found
     */
    public <T> T getService(Class<T> serviceClass) {
        return contextManager.getService(serviceClass);
    }

    /**
     * Get a Hitorro service by its short name.
     * 
     * @param shortName The service short name
     * @return The service instance, or null if not found
     */
    public Object getService(String shortName) {
        return contextManager.getService(shortName);
    }

    /**
     * Check if a service is initialized and available.
     * 
     * @param serviceClass The service class
     * @return true if the service is initialized
     */
    public boolean isServiceAvailable(Class<?> serviceClass) {
        return contextManager.isServiceInitialized(serviceClass);
    }

    /**
     * Get all initialized Hitorro services.
     * 
     * @return List of all service wrappers
     */
    public List<ServiceWrapper> getAllServices() {
        return contextManager.getAllServices();
    }
}
