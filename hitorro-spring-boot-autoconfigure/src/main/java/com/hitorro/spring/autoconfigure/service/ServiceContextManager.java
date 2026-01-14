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

import com.hitorro.spring.autoconfigure.HitorroProperties;
import com.hitorro.util.core.classes.ClassUtil;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.startupframework.ServiceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.IOException;
import java.util.List;

/**
 * Manages the lifecycle of Hitorro ServiceContext within Spring's application context.
 * Bridges Hitorro's service initialization with Spring's bean lifecycle.
 */
public class ServiceContextManager implements ApplicationContextAware, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ServiceContextManager.class);

    private final ServiceContext serviceContext;
    private final HitorroProperties properties;
    private ApplicationContext applicationContext;

    public ServiceContextManager(HitorroProperties properties) {
        this.serviceContext = ServiceContext.getSC();
        this.properties = properties;
        ServiceContext.setServiceContext(this.serviceContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Initializing Hitorro ServiceContext");
        
        // Initialize Hitorro properties system before ServiceContext
        initializeHitorroProperties();
        
        // Load services from configuration
        loadServicesFromConfiguration();
        
        // Initialize service context
        serviceContext.setInitDb(properties.getServices().isDbInit());
        
        String error = serviceContext.init();
        if (error != null) {
            throw new IllegalStateException("Failed to initialize Hitorro ServiceContext: " + error);
        }
        
        logger.info("Hitorro ServiceContext initialized successfully");
        logger.info("Initialized {} services", serviceContext.getServices().size());
        
        // Log initialized services
        for (ServiceWrapper sw : serviceContext.getServices()) {
            logger.info("  ✓ {}: {}", sw.getShortName(), sw.getDescription());
        }
    }
    
    /**
     * Load services from Spring Boot configuration.
     * Supports multiple configuration patterns:
     * 1. Explicit list: hitorro.services.load=[service1, service2]
     * 2. HTServer pattern: hitorro.services.server-type=webapp (reads services.webapp)
     * 3. Auto-discovery: Scans for @ServiceDefinition annotated classes (future)
     */
    private void loadServicesFromConfiguration() {
        List<String> servicesToLoad = properties.getServices().getLoad();
        String serverType = properties.getServices().getServerType();
        
        if (serverType != null && !serverType.isEmpty()) {
            // HTServer-style loading: read services from services.{type} in JVS
            logger.info("Loading services using HTServer pattern (type: {})", serverType);
            loadServicesViaHTServer(serverType);
        } else if (servicesToLoad != null && !servicesToLoad.isEmpty()) {
            // Explicit list loading
            logger.info("Loading {} services from configuration", servicesToLoad.size());
            for (String serviceName : servicesToLoad) {
                loadService(serviceName);
            }
        } else {
            logger.info("No services configured to load. Use hitorro.services.load or hitorro.services.server-type");
        }
    }
    
    /**
     * Load services using HTServer pattern (reads from services.{type} in properties).
     */
    private void loadServicesViaHTServer(String serverType) {
        try {
            // Add HTServer which will dynamically load services based on serverType
            Class<?> htServerClass = ClassUtil.getClassForName(
                "com.hitorro.util.startupframework.HTServer", null);
            
            if (htServerClass == null) {
                logger.warn("HTServer class not found. HTServer pattern requires hitorro-util on classpath.");
                return;
            }
            
            // TODO: Set the servertype property for HTServer to read
            // The JVS needs to have servertype set before HTServer init
            // For now, users should set it in application.yml or JVS file
            logger.info("Note: Ensure 'servertype' property is set in configuration for HTServer");
            
            logger.info("Adding HTServer with servertype: {}", serverType);
            boolean added = serviceContext.addModule(htServerClass.getName());
            if (!added) {
                logger.warn("Failed to add HTServer module");
            }
        } catch (Exception e) {
            logger.error("Error loading services via HTServer pattern", e);
        }
    }
    
    /**
     * Load a single service by class name or short name.
     */
    private void loadService(String serviceName) {
        try {
            serviceName = serviceName.trim();
            
            // Try to resolve as class name first
            Class<?> serviceClass = ClassUtil.getClassForName(serviceName, null);
            
            if (serviceClass != null) {
                logger.info("  Loading service: {} ({})", serviceName, serviceClass.getSimpleName());
                boolean added = serviceContext.addModule(serviceName);
                if (!added) {
                    logger.warn("  Failed to add service: {}", serviceName);
                }
            } else {
                // Try to resolve by short name
                ServiceWrapper existing = serviceContext.getInitializedServiceByShortname(serviceName);
                if (existing == null) {
                    logger.warn("  Service not found: {} (not a valid class name or short name)", serviceName);
                } else {
                    logger.info("  Service already loaded: {}", serviceName);
                }
            }
        } catch (Exception e) {
            logger.error("  Error loading service: {}", serviceName, e);
        }
    }
    
    /**
     * Initialize Hitorro's JVSProperties system.
     * This must be done before ServiceContext.init() because services may access properties during initialization.
     */
    private void initializeHitorroProperties() {
        // Create an empty JVS object for properties if not already set
        com.hitorro.jsontypesystem.JVS properties = com.hitorro.jsontypesystem.propreaders.JVSProperties.getProperties();
        if (properties == null) {
            logger.info("Initializing Hitorro JVSProperties with empty configuration");
            properties = new com.hitorro.jsontypesystem.JVS();
            com.hitorro.jsontypesystem.propreaders.JVSProperties.setDefaultProperties(properties, false);
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Shutting down Hitorro ServiceContext");
        try {
            serviceContext.deInit();
            logger.info("Hitorro ServiceContext shutdown complete");
        } catch (IOException e) {
            logger.error("Error during Hitorro ServiceContext shutdown", e);
        }
    }

    /**
     * Get the underlying Hitorro ServiceContext.
     */
    public ServiceContext getServiceContext() {
        return serviceContext;
    }

    /**
     * Get a Hitorro service by its class.
     */
    public <T> T getService(Class<T> serviceClass) {
        Object service = serviceContext.getInitializedModule(serviceClass);
        if (service == null) {
            return null;
        }
        return serviceClass.cast(service);
    }

    /**
     * Get a Hitorro service by its short name.
     */
    public Object getService(String shortName) {
        ServiceWrapper sw = serviceContext.getInitializedServiceByShortname(shortName);
        if (sw == null) {
            return null;
        }
        return sw.getServiceObject();
    }

    /**
     * Check if a service is initialized.
     */
    public boolean isServiceInitialized(Class<?> serviceClass) {
        return serviceContext.isServiceInitialized(serviceClass);
    }

    /**
     * Get all initialized services.
     */
    public List<ServiceWrapper> getAllServices() {
        return serviceContext.getServices();
    }
}
