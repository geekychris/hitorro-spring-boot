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

import com.hitorro.basedms.db.CSVHibernateLoader;
import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import com.hitorro.util.core.classes.ClassUtil;
import com.hitorro.util.typesystem.BaseSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

/**
 * Loads data from CSV files into Hitorro DMS using CSV consumers.
 * This bridges Hitorro's CSV loading mechanism to Spring Boot's resource system.
 */
public class CSVDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(CSVDataLoader.class);
    
    private final DMSSessionFactory sessionFactory;
    
    public CSVDataLoader(DMSSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    /**
     * Load data from a CSV file using a specific consumer class.
     * 
     * @param csvResource Spring Resource pointing to CSV file
     * @param consumerClassName Fully qualified class name of CSVHibernateLoaderConsumer
     * @return true if successful
     */
    public boolean loadCSV(Resource csvResource, String consumerClassName) throws IOException {
        if (!csvResource.exists()) {
            logger.warn("CSV resource does not exist: {}", csvResource.getDescription());
            return false;
        }
        
        logger.info("Loading CSV data from: {}", csvResource.getDescription());
        logger.info("  Using consumer: {}", consumerClassName);
        
        // Get the consumer instance
        Object consumerObj = ClassUtil.getInstanceSwallowError(consumerClassName);
        if (consumerObj == null) {
            logger.error("Failed to create consumer instance: {}", consumerClassName);
            return false;
        }
        
        if (!(consumerObj instanceof CSVHibernateLoaderConsumer)) {
            logger.error("Consumer class is not a CSVHibernateLoaderConsumer: {}", consumerClassName);
            return false;
        }
        
        CSVHibernateLoaderConsumer consumer = (CSVHibernateLoaderConsumer) consumerObj;
        
        // Create a temporary file if needed (CSVHibernateLoader expects a File)
        File csvFile;
        if (csvResource.isFile()) {
            csvFile = csvResource.getFile();
        } else {
            // Create temp file for resources in JAR, classpath, etc.
            csvFile = File.createTempFile("hitorro-csv-", ".csv");
            csvFile.deleteOnExit();
            java.nio.file.Files.copy(csvResource.getInputStream(), csvFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Load using Hitorro's CSV loader
        BaseSession session = sessionFactory.createSession();
        try {
            CSVHibernateLoader loader = new CSVHibernateLoader();
            boolean success = loader.load(session, csvFile, consumer);
            
            if (success) {
                session.commit();
                logger.info("Successfully loaded CSV data from: {}", csvResource.getDescription());
            } else {
                session.rollback();
                logger.error("Failed to load CSV data from: {}", csvResource.getDescription());
            }
            
            return success;
        } catch (Exception e) {
            session.rollback();
            logger.error("Error loading CSV data from: {}", csvResource.getDescription(), e);
            return false;
        } finally {
            session.close();
        }
    }
    
    /**
     * Load stores from a CSV file.
     * Convenience method for the common case of loading stores.
     */
    public boolean loadStores(Resource csvResource) throws IOException {
        return loadCSV(csvResource, "com.hitorro.basedms.csvconsumers.StoreCSVConsumer");
    }
    
    /**
     * Load users from a CSV file.
     */
    public boolean loadUsers(Resource csvResource) throws IOException {
        return loadCSV(csvResource, "com.hitorro.basedms.csvconsumers.UserCSVConsumer");
    }
}
