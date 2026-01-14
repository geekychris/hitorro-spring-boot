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
package com.hitorro.spring.autoconfigure.dms;

import com.hitorro.spring.autoconfigure.HitorroProperties;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * Auto-configuration for Hitorro DMS (Document Management System) integration.
 * This configuration preserves Hitorro's DMSSession concept and unified ID system
 * while making it available to Spring Boot applications.
 * 
 * Key features preserved:
 * - DMSSession lifecycle management
 * - Unified ID system for object retrieval
 * - Transaction semantics
 * - Object versioning and persistence
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.hitorro.basedms.session.DMSSession")
@EnableConfigurationProperties(HitorroProperties.class)
@ConditionalOnProperty(prefix = "hitorro.dms", name = "enabled", havingValue = "true", matchIfMissing = true)
@org.springframework.boot.autoconfigure.AutoConfigureAfter(
    com.hitorro.spring.autoconfigure.service.HitorroServiceAutoConfiguration.class)
public class DMSAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DMSAutoConfiguration.class);

    /**
     * Create the DMSSessionFactory that will manage DMSSession instances.
     * Initializes the Hitorro DMS system including the Type system and services.
     * 
     * Note: If HibernateService is loaded via services configuration, it will handle
     * initialization. This auto-config only runs when services are NOT configured.
     */
    @Bean
    public DMSSessionFactory dmsSessionFactory(
            HitorroProperties properties,
            @Autowired(required = false) EntityManagerFactory entityManagerFactory,
            @Autowired(required = false) com.hitorro.spring.autoconfigure.service.ServiceContextManager serviceContextManager) {
        
        logger.info("Configuring Hitorro DMS integration");
        logger.info("  Session scope: {}", properties.getDms().getSessionScope());
        logger.info("  Transaction mode: {}", properties.getDms().getTransactionMode());
        
        try {
            // Check if HibernateService is already loaded via services
            boolean hibernateServiceLoaded = false;
            if (serviceContextManager != null) {
                try {
                    Class<?> hibernateServiceClass = Class.forName("com.hitorro.basedms.db.HibernateService");
                    hibernateServiceLoaded = serviceContextManager.isServiceInitialized(hibernateServiceClass);
                } catch (ClassNotFoundException e) {
                    // HibernateService not available
                }
            }
            
            if (hibernateServiceLoaded) {
                logger.info("HibernateService already loaded via services - skipping DMS auto-configuration");
                logger.info("Type system and Hibernate are already initialized by HibernateService");
            } else {
                // HibernateService not loaded, we need to initialize DMS ourselves
                logger.info("HibernateService not loaded - performing DMS initialization");
                
                // Step 1: Initialize Hitorro's Type system (required for soft references and GUID lookups)
                logger.info("Initializing Hitorro Type system...");
                initializeTypeSystem(entityManagerFactory);
                
                // Step 2: Initialize Hibernate SessionFactory bridge
                if (entityManagerFactory != null) {
                    bridgeHibernateSessionFactory(entityManagerFactory);
                } else {
                    logger.warn("No EntityManagerFactory available - DMS will not function properly");
                }
                
                // Step 3: Initialize the native Hitorro DMSSessionFactory
                initializeDMSSessionFactory();
                
                logger.info("Hitorro DMS integration fully initialized");
            }
            
        } catch (Exception e) {
            logger.error("Error during DMS initialization", e);
        }
        
        return new DMSSessionFactory();
    }
    
    /**
     * Initialize Hitorro's Type system which is required for soft references and GUID lookups.
     */
    private void initializeTypeSystem(EntityManagerFactory entityManagerFactory) throws Exception {
        logger.info("  - Initializing TypeManager...");
        
        // Get all entity classes from Spring's EntityManagerFactory
        if (entityManagerFactory != null) {
            SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            
            // Get TypeManager instance
            com.hitorro.util.typesystem.TypeManager tm = 
                com.hitorro.util.typesystem.TypeManager.getTypeManager();
            
            // Register all entity classes with TypeManager
            // This enables the Type system to understand entity metadata and support soft references
            sessionFactory.getMetamodel().getEntities().forEach(entityType -> {
                Class<?> javaType = entityType.getJavaType();
                try {
                    tm.addType(javaType);
                    logger.debug("  - Registered type: {}", javaType.getName());
                } catch (Exception e) {
                    logger.warn("Could not register type {}: {}", javaType.getName(), e.getMessage());
                }
            });
            
            // Initialize type objects (builds internal metadata structures)
            tm.initTypeObjects();
            logger.info("  - TypeManager initialized with {} entity types", 
                tm.getListOfPersistedTypes().size());
        }
    }
    
    /**
     * Bridge Spring's Hibernate SessionFactory to Hitorro's HibernateUtil.
     */
    private void bridgeHibernateSessionFactory(EntityManagerFactory entityManagerFactory) throws Exception {
        logger.info("  - Bridging Hibernate SessionFactory...");
        
        // Get Hibernate SessionFactory from JPA EntityManagerFactory
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        
        // Set it in Hitorro's HibernateUtil using reflection
        java.lang.reflect.Field sessionField = 
            com.hitorro.basedms.db.HibernateUtil.class.getDeclaredField("s_session");
        sessionField.setAccessible(true);
        sessionField.set(null, sessionFactory);
        
        // Also put it in the map
        java.lang.reflect.Field mapField = 
            com.hitorro.basedms.db.HibernateUtil.class.getDeclaredField("s_map");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, SessionFactory> sessionMap = 
            (java.util.Map<String, SessionFactory>) mapField.get(null);
        sessionMap.put("default", sessionFactory);
        
        logger.info("  - Hibernate SessionFactory bridge established");
    }
    
    /**
     * Initialize the native Hitorro DMSSessionFactory.
     */
    private void initializeDMSSessionFactory() {
        logger.info("  - Initializing DMSSessionFactory...");
        
        com.hitorro.util.typesystem.BaseSessionFactory nativeFactory =  
            com.hitorro.basedms.session.DMSSessionFactory.getFactory();
        
        if (nativeFactory == null) {
            // Create and set the native factory
            com.hitorro.basedms.session.DMSSessionFactory dmsFactory = 
                new com.hitorro.basedms.session.DMSSessionFactory();
            com.hitorro.util.typesystem.BaseSessionFactory.setFactory(dmsFactory);
            logger.info("  - DMSSessionFactory created and registered");
        } else {
            logger.info("  - Using existing DMSSessionFactory");
        }
    }

    /**
     * Note: The actual DMSSession bean configuration will be added once we have
     * access to the hitorro-basedms module to understand the exact API.
     * 
     * The session scope will be configurable:
     * - "request" scope for web applications (one session per HTTP request)
     * - "session" scope for stateful web applications
     * - "prototype" for manual session management
     * 
     * Example (to be implemented):
     * 
     * @Bean
     * @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
     * @ConditionalOnProperty(prefix = "hitorro.dms", name = "session-scope", havingValue = "request")
     * public DMSSession dmsSession(DMSSessionFactory factory) {
     *     return factory.createSession();
     * }
     */
}
