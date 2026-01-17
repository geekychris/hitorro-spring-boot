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
    private final SpringDatabaseConfigProvider databaseConfigProvider;
    private ApplicationContext applicationContext;

    public ServiceContextManager(HitorroProperties properties, SpringDatabaseConfigProvider databaseConfigProvider) {
        this.serviceContext = ServiceContext.getSC();
        this.properties = properties;
        this.databaseConfigProvider = databaseConfigProvider;
        ServiceContext.setServiceContext(this.serviceContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("=== Hitorro Service Lifecycle: Starting ===");
        
        // PHASE 0: Initialize Hitorro properties system before ServiceContext
        logger.info("Phase 0: Initializing JVSProperties");
        initializeHitorroProperties();
        
        // PHASE 0.5: Install Spring database config provider
        logger.info("Phase 0.5: Installing Spring database config provider");
        com.hitorro.basedms.db.DatabaseConfigProvider.setProvider(databaseConfigProvider);
        logger.info("  ✓ Spring DataSource will be used as single source of truth for database config");
        
        // PHASE 1: Load services from configuration (equivalent to sc.addModule)
        logger.info("Phase 1: Loading services (addModule)");
        loadServicesFromConfiguration();
        
        // PHASE 2: Validate configuration keys (IMPORTANT - missing in original!)
        logger.info("Phase 2: Validating configuration keys");
        String validationError = ServiceContext.validateConfigKeys();
        if (!com.hitorro.util.core.string.StringUtil.nullOrEmptyOrBlankString(validationError)) {
            logger.error("Configuration validation failed:");
            logger.error(validationError);
            throw new IllegalStateException("Unable to validate service configuration: " + validationError);
        }
        logger.info("  ✓ All configuration keys validated successfully");
        
        // PHASE 3: Initialize service context (runs all startup phases)
        logger.info("Phase 3: Initializing services (runs startup phases)");
        serviceContext.setInitDb(properties.getServices().isDbInit());
        logger.info("  - dbInit flag: {}", properties.getServices().isDbInit());
        
        String initError = serviceContext.init();
        if (initError != null) {
            logger.error("Service initialization failed: {}", initError);
            throw new IllegalStateException("Failed to initialize Hitorro ServiceContext: " + initError);
        }
        
        logger.info("=== Hitorro Service Lifecycle: Complete ===");
        logger.info("Initialized {} services", serviceContext.getServices().size());
        
        // Log initialized services
        for (ServiceWrapper sw : serviceContext.getServices()) {
            logger.info("  ✓ {}: {}", sw.getShortName(), sw.getDescription());
        }
        
        // Note: runIfRunnable() and deInit() are handled separately
        // - runIfRunnable() would be called by application code if needed
        // - deInit() is called in destroy() method during Spring shutdown
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
     * Initialize Hitorro's JVSProperties system using the same approach as CommandLine.
     * This loads properties from multiple sources in order:
     * 1. System arguments
     * 2. HT_BIN/config directory
     * 3. HT_HOME/config directory (where hibernate.json, types, collections, etc. live)
     * 4. Spring Boot custom properties overlay
     */
    private void initializeHitorroProperties() {
        com.hitorro.jsontypesystem.JVS existingProperties = com.hitorro.jsontypesystem.propreaders.JVSProperties.getProperties();
        if (existingProperties == null) {
            logger.info("Initializing Hitorro JVSProperties from config directories");
            
            try {
                // Create empty JVS and set it temporarily so property readers can reference it
                com.hitorro.jsontypesystem.JVS props = new com.hitorro.jsontypesystem.JVS();
                com.hitorro.jsontypesystem.propreaders.JVSProperties.setDefaultProperties(props, false);
                
                // Create property loaders just like CommandLine does
                com.hitorro.jsontypesystem.propreaders.JVSPropertiesReader[] propLoaders = {
                    new com.hitorro.jsontypesystem.propreaders.JVSSystemArgsPropertyReader(),
                    new com.hitorro.jsontypesystem.propreaders.JVSDirectoryReadingPropertiesReader(
                        com.hitorro.jsontypesystem.propreaders.JVSDirectoryType.Bin, false),
                    new com.hitorro.jsontypesystem.propreaders.JVSDirectoryReadingPropertiesReader(
                        com.hitorro.jsontypesystem.propreaders.JVSDirectoryType.Home, true)
                };
                
                // Load properties from all sources
                // Pass props as both the accumulator AND as cmdLineArgs so property readers can reference it
                for (com.hitorro.jsontypesystem.propreaders.JVSPropertiesReader reader : propLoaders) {
                    reader.getProperties(props, props);
                }
                
                // Overlay Spring Boot custom properties if provided
                if (properties.getHitorroProperties() != null && !properties.getHitorroProperties().isEmpty()) {
                    logger.info("Overlaying {} custom properties from Spring configuration", 
                               properties.getHitorroProperties().size());
                    
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.node.ObjectNode customNode = mapper.createObjectNode();
                    addMapToNode(customNode, properties.getHitorroProperties(), mapper, "");
                    
                    // Merge custom properties into loaded props
                    com.hitorro.jsontypesystem.JVS customProps = new com.hitorro.jsontypesystem.JVS(customNode);
                    props.merge(customProps);
                }
                
                // Resolve variables in properties (e.g., ${HT_HOME})
                props.resolveVariables(props);
                
                // Set as default properties
                com.hitorro.jsontypesystem.propreaders.JVSProperties.setDefaultProperties(props, false);
                
                logger.info("✓ JVSProperties initialized from:");
                logger.info("  - System arguments");
                logger.info("  - {}/config", properties.getHtBin() != null ? properties.getHtBin() : "HT_BIN");
                logger.info("  - {}/config", properties.getHtHome() != null ? properties.getHtHome() : "HT_HOME");
                if (properties.getHitorroProperties() != null && !properties.getHitorroProperties().isEmpty()) {
                    logger.info("  - Spring Boot custom properties");
                }
                
            } catch (Exception e) {
                logger.error("Failed to load JVSProperties from config directories", e);
                // Fall back to empty properties
                com.hitorro.jsontypesystem.JVS fallbackProps = new com.hitorro.jsontypesystem.JVS();
                com.hitorro.jsontypesystem.propreaders.JVSProperties.setDefaultProperties(fallbackProps, false);
            }
        } else {
            logger.info("JVSProperties already initialized");
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
    
    /**
     * Run a service if it implements Runnable.
     * This is equivalent to calling s.runIfRunnable() in BaseCommandLine.
     * 
     * @param serviceClass The service class to run
     * @return true if the service was runnable and executed
     */
    public boolean runServiceIfRunnable(Class<?> serviceClass) {
        ServiceWrapper sw = serviceContext.getInitializedServiceWrapper(serviceClass);
        if (sw == null) {
            logger.warn("Service {} is not initialized", serviceClass.getName());
            return false;
        }
        
        logger.info("Running service if runnable: {}", sw.getShortName());
        sw.runIfRunnable();
        return true;
    }
    
    /**
     * Run a service by short name if it implements Runnable.
     * 
     * @param shortName The service short name to run
     * @return true if the service was found, runnable, and executed
     */
    public boolean runServiceIfRunnable(String shortName) {
        ServiceWrapper sw = serviceContext.getInitializedServiceByShortname(shortName);
        if (sw == null) {
            logger.warn("Service '{}' is not initialized", shortName);
            return false;
        }
        
        logger.info("Running service if runnable: {}", sw.getShortName());
        sw.runIfRunnable();
        return true;
    }
    
    /**
     * Validate configuration keys for all services.
     * This is equivalent to ServiceContext.validateConfigKeys() in BaseCommandLine.
     * 
     * @return Empty string if valid, error message if validation failed
     */
    public String validateConfiguration() {
        return ServiceContext.validateConfigKeys();
    }
    
    /**
     * Check if the service context has been initialized.
     */
    public boolean isInitialized() {
        return serviceContext.isInitialized();
    }
    
    /**
     * Recursively add map entries to ObjectNode.
     */
    @SuppressWarnings("unchecked")
    private void addMapToNode(com.fasterxml.jackson.databind.node.ObjectNode node, 
                               java.util.Map<String, Object> map,
                               com.fasterxml.jackson.databind.ObjectMapper mapper,
                               String prefix) {
        for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            
            if (value instanceof java.util.Map) {
                // Nested map - recurse
                com.fasterxml.jackson.databind.node.ObjectNode nestedNode = mapper.createObjectNode();
                node.set(key, nestedNode);
                addMapToNode(nestedNode, (java.util.Map<String, Object>) value, mapper, fullPath);
            } else if (value instanceof Boolean) {
                node.put(key, (Boolean) value);
                logger.info("  - {}: {}", fullPath, value);
            } else if (value instanceof Number) {
                node.put(key, ((Number) value).intValue());
                logger.info("  - {}: {}", fullPath, value);
            } else if (value instanceof String) {
                node.put(key, (String) value);
                logger.info("  - {}: {}", fullPath, value);
            }
        }
    }
}
