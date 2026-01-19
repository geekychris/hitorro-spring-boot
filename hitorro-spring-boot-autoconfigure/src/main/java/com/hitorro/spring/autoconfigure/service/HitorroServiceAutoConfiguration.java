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
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.startupframework.ServiceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

/**
 * Auto-configuration for Hitorro services integration with Spring Boot.
 * This configuration initializes the Hitorro ServiceContext and makes
 * Hitorro services available as Spring beans.
 */
@AutoConfiguration
@EnableConfigurationProperties(HitorroProperties.class)
@ConditionalOnProperty(prefix = "hitorro.services", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HitorroServiceAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(HitorroServiceAutoConfiguration.class);

    /**
     * Create the Spring database config provider.
     * This makes Spring's DataSource the single source of truth for database configuration.
     */
    @Bean
    public SpringDatabaseConfigProvider springDatabaseConfigProvider(
            javax.sql.DataSource dataSource,
            org.springframework.boot.autoconfigure.jdbc.DataSourceProperties dataSourceProperties) {
        logger.info("Creating Spring Database Config Provider");
        return new SpringDatabaseConfigProvider(dataSource, dataSourceProperties);
    }

    /**
     * Create the ServiceContextManager that manages Hitorro's ServiceContext lifecycle.
     * This will load and initialize services based on configuration.
     */
    @Bean
    public ServiceContextManager serviceContextManager(HitorroProperties properties, 
                                                        SpringDatabaseConfigProvider databaseConfigProvider) {
        logger.info("Creating Hitorro ServiceContextManager");
        return new ServiceContextManager(properties, databaseConfigProvider);
    }

    /**
     * Expose the Hitorro ServiceContext as a Spring bean.
     */
    @Bean
    @DependsOn("serviceContextManager")
    public ServiceContext hitorroServiceContext(ServiceContextManager manager) {
        return manager.getServiceContext();
    }

    /**
     * Create a factory bean that can provide access to individual Hitorro services.
     * This allows other Spring beans to inject Hitorro services by class type.
     */
    @Bean
    public HitorroServiceFactory hitorroServiceFactory(ServiceContextManager manager) {
        return new HitorroServiceFactory(manager);
    }

    /**
     * REST controller for exploring Hitorro services.
     */
    @Bean
    public ServiceExplorerController serviceExplorerController(ServiceContextManager manager) {
        logger.info("Creating Hitorro Service Explorer REST controller");
        return new ServiceExplorerController(manager);
    }
}
