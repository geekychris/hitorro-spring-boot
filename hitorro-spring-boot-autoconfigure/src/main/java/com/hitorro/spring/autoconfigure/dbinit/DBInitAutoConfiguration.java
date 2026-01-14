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
package com.hitorro.spring.autoconfigure.dbinit;

import com.hitorro.spring.autoconfigure.HitorroProperties;
import com.hitorro.spring.autoconfigure.dms.DMSAutoConfiguration;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * Auto-configuration for Hitorro database initialization from CSV files.
 * 
 * This enables the DBInit framework which loads data from CSV files
 * using CSVHibernateLoader and consumer classes.
 */
@AutoConfiguration
@ConditionalOnClass(name = {
    "com.hitorro.basedms.db.CSVHibernateLoader",
    "com.hitorro.basedms.db.CSVHibernateLoaderConsumer"
})
@ConditionalOnProperty(prefix = "hitorro.dms.db-init", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(HitorroProperties.class)
@AutoConfigureAfter(DMSAutoConfiguration.class)
public class DBInitAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(DBInitAutoConfiguration.class);
    
    public DBInitAutoConfiguration() {
        logger.info("Initializing Hitorro DBInit auto-configuration");
    }
    
    /**
     * Create the CSV data loader bean.
     */
    @Bean
    public CSVDataLoader csvDataLoader(DMSSessionFactory dmsSessionFactory) {
        logger.info("Creating CSVDataLoader");
        return new CSVDataLoader(dmsSessionFactory);
    }
    
    /**
     * Create the DMS data initializer that runs on startup.
     */
    @Bean
    public DMSDataInitializer dmsDataInitializer(
            CSVDataLoader csvDataLoader,
            HitorroProperties properties,
            ResourceLoader resourceLoader) {
        logger.info("Creating DMSDataInitializer");
        return new DMSDataInitializer(csvDataLoader, properties, resourceLoader);
    }
}
