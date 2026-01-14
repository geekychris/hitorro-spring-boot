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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

import java.util.List;

/**
 * Initializes DMS data from CSV files during application startup.
 * Mimics Hitorro's IntegrationEventsContext but using Spring Boot resources.
 */
public class DMSDataInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DMSDataInitializer.class);
    
    private final CSVDataLoader csvDataLoader;
    private final HitorroProperties properties;
    private final ResourceLoader resourceLoader;
    
    public DMSDataInitializer(CSVDataLoader csvDataLoader, 
                             HitorroProperties properties,
                             ResourceLoader resourceLoader) {
        this.csvDataLoader = csvDataLoader;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * Initialize data after application is ready.
     * Runs on ApplicationReadyEvent to ensure all beans are initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        if (!properties.getDms().getDbInit().isEnabled()) {
            logger.info("DMS DB initialization disabled");
            return;
        }
        
        logger.info("=== Hitorro DMS Data Initialization ===");
        
        List<HitorroProperties.Dms.DbInit.DataSet> dataSets = 
            properties.getDms().getDbInit().getDataSets();
        
        if (dataSets == null || dataSets.isEmpty()) {
            logger.info("No datasets configured for initialization");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (HitorroProperties.Dms.DbInit.DataSet dataSet : dataSets) {
            try {
                boolean success = loadDataSet(dataSet);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("Error loading dataset: {}", dataSet.getName(), e);
                failCount++;
                if (properties.getDms().getDbInit().isFailOnError()) {
                    throw new RuntimeException("Dataset initialization failed: " + dataSet.getName(), e);
                }
            }
        }
        
        logger.info("DMS Data Initialization complete: {} success, {} failed", successCount, failCount);
        logger.info("===========================================");
    }
    
    /**
     * Load a single dataset.
     */
    private boolean loadDataSet(HitorroProperties.Dms.DbInit.DataSet dataSet) throws Exception {
        logger.info("Loading dataset: {}", dataSet.getName());
        logger.info("  CSV file: {}", dataSet.getCsvFile());
        logger.info("  Consumer: {}", dataSet.getConsumer());
        
        // Resolve the CSV file resource
        Resource csvResource;
        try {
            // Try as a pattern first (supports wildcards)
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                .getResources(dataSet.getCsvFile());
            
            if (resources.length == 0) {
                logger.warn("No CSV file found at: {}", dataSet.getCsvFile());
                return false;
            }
            
            if (resources.length > 1) {
                logger.warn("Multiple CSV files match pattern {}, using first: {}", 
                    dataSet.getCsvFile(), resources[0].getDescription());
            }
            
            csvResource = resources[0];
        } catch (Exception e) {
            logger.error("Error resolving CSV resource: {}", dataSet.getCsvFile(), e);
            return false;
        }
        
        // Load the CSV data
        return csvDataLoader.loadCSV(csvResource, dataSet.getConsumer());
    }
}
