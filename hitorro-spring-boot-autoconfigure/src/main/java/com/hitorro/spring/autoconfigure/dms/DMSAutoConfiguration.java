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
public class DMSAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DMSAutoConfiguration.class);

    /**
     * Create the DMSSessionFactory that will manage DMSSession instances.
     * Initializes the Hitorro DMS system if not already initialized.
     */
    @Bean
    public DMSSessionFactory dmsSessionFactory(
            HitorroProperties properties,
            @Autowired(required = false) EntityManagerFactory entityManagerFactory) {
        
        logger.info("Configuring Hitorro DMS integration");
        logger.info("  Session scope: {}", properties.getDms().getSessionScope());
        logger.info("  Transaction mode: {}", properties.getDms().getTransactionMode());
        
        // Initialize Hibernate SessionFactory from Spring's EntityManagerFactory
        if (entityManagerFactory != null) {
            try {
                // Get Hibernate SessionFactory from JPA EntityManagerFactory
                SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
                
                // Set it in Hitorro's HibernateUtil using reflection
                // HibernateUtil has a static field s_session that holds the default SessionFactory
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
                
                logger.info("Successfully configured Hibernate SessionFactory for Hitorro DMS");
            } catch (Exception e) {
                logger.error("Error configuring Hibernate SessionFactory", e);
            }
        } else {
            logger.warn("No EntityManagerFactory available - DMS will not function properly");
        }
        
        // Initialize the native Hitorro DMSSessionFactory if needed
        try {
            com.hitorro.util.typesystem.BaseSessionFactory nativeFactory =  
                com.hitorro.basedms.session.DMSSessionFactory.getFactory();
            
            if (nativeFactory == null) {
                logger.info("Initializing Hitorro DMS native factory");
                // Create and set the native factory
                com.hitorro.basedms.session.DMSSessionFactory dmsFactory = 
                    new com.hitorro.basedms.session.DMSSessionFactory();
                com.hitorro.util.typesystem.BaseSessionFactory.setFactory(dmsFactory);
                logger.info("Successfully initialized Hitorro DMS native factory");
            } else {
                logger.info("Using existing Hitorro DMS native factory");
            }
        } catch (Exception e) {
            logger.error("Error initializing DMS factory", e);
        }
        
        return new DMSSessionFactory();
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
