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
package com.hitorro.spring.autoconfigure.integration;

import com.hitorro.spring.autoconfigure.HitorroProperties;
import com.hitorro.util.integrationevents.IntegrationEventsContext;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Hitorro Integration Events.
 * 
 * IMPORTANT: This provides an ADDITIONAL trigger point for integration events beyond
 * the normal service lifecycle. The ServiceContextManager already runs InitDBEvents
 * phase during serviceContext.init() IF dbInit=true.
 * 
 * This auto-configuration allows:
 * 1. Running integration events AFTER Spring is fully ready (ApplicationReadyEvent)
 * 2. Running events even when dbInit=false (typical in Spring Boot)
 * 3. Manual triggering via IntegrationEventsManager bean
 * 
 * Integration events are part of the InitDBEvents phase in the service lifecycle:
 *   1. RegisterHooks
 *   2. RegisterInterfaces  
 *   3. InitWithUpgrade
 *   4. InitUIDirs
 *   5. InitDBEvents ← Integration events run here
 *   6. Start
 * 
 * Configuration:
 * - hitorro.integration.enabled: Enable/disable integration events (default: true)
 * - hitorro.integration.run-on-startup: Run CSV imports on application startup (default: false)
 * 
 * The events to run are defined in JVSProperties:
 * - integration.initdblist: Comma-separated list of event names
 * - integration.events.{eventName}: Configuration for each event
 */
@AutoConfiguration
@ConditionalOnClass(IntegrationEventsContext.class)
@EnableConfigurationProperties(HitorroProperties.class)
@ConditionalOnProperty(prefix = "hitorro.integration", name = "enabled", havingValue = "true", matchIfMissing = true)
@org.springframework.boot.autoconfigure.AutoConfigureAfter({
    com.hitorro.spring.autoconfigure.service.HitorroServiceAutoConfiguration.class,
    com.hitorro.spring.autoconfigure.dms.DMSAutoConfiguration.class
})
public class IntegrationEventsAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(IntegrationEventsAutoConfiguration.class);
    
    /**
     * Create a listener that runs integration events after the application is ready.
     * This ensures all services, Hibernate session factory, and database schema are initialized.
     */
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.integration", name = "run-on-startup", havingValue = "true", matchIfMissing = false)
    public ApplicationListener<ApplicationReadyEvent> integrationEventsRunner(HitorroProperties properties) {
        return event -> {
            logger.info("=== Running Hitorro Integration Events ===");
            logger.info("Application is ready - triggering CSV imports and integration events");
            
            try {
                IntegrationEventsContext context = IntegrationEventsContext.getContext();
                
                // Get the list of events that should be run
                java.util.List<String> eventNames = context.getEventNames();
                if (eventNames == null || eventNames.isEmpty()) {
                    logger.warn("No integration events configured in JVSProperties");
                    logger.warn("Expected property: integration.initdblist");
                    logger.warn("Example: integration.initdblist=users,permissions,roles,htcategory");
                    return;
                }
                
                logger.info("Found {} configured integration event(s):", eventNames.size());
                for (String eventName : eventNames) {
                    logger.info("  - {}", eventName);
                }
                
                // Run the events
                logger.info("Executing integration events...");
                context.runInitDBEvents();
                
                logger.info("=== Integration Events Complete ===");
                
            } catch (PropaccessError e) {
                logger.error("Property access error during integration events", e);
            } catch (Exception e) {
                logger.error("Error running integration events", e);
            }
        };
    }
    
    /**
     * Create a manager bean that can be used to manually trigger integration events.
     */
    @Bean
    public IntegrationEventsManager integrationEventsManager() {
        return new IntegrationEventsManager();
    }
    
    /**
     * Manager for manually triggering integration events.
     * Can be used by application code or REST endpoints to trigger CSV imports on demand.
     */
    public static class IntegrationEventsManager {
        
        private static final Logger logger = LoggerFactory.getLogger(IntegrationEventsManager.class);
        
        /**
         * Run all configured integration events.
         */
        public void runAllEvents() throws PropaccessError {
            logger.info("Running all configured integration events");
            IntegrationEventsContext.getContext().runInitDBEvents();
        }
        
        /**
         * Run a specific integration event by name.
         * 
         * @param eventName The event name (e.g., "htcategory", "users", "permissions")
         * @return true if the event ran successfully
         */
        public boolean runEvent(String eventName) throws PropaccessError {
            logger.info("Running integration event: {}", eventName);
            return IntegrationEventsContext.getContext().runEvent(eventName);
        }
        
        /**
         * Get the list of configured integration event names.
         */
        public java.util.List<String> getConfiguredEvents() {
            return IntegrationEventsContext.getContext().getEventNames();
        }
        
        /**
         * Check if a specific event is configured.
         */
        public boolean hasEvent(String eventName) {
            return IntegrationEventsContext.getContext().hasEvent(eventName);
        }
    }
}
