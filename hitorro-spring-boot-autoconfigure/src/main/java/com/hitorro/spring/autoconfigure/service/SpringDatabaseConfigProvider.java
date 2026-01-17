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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hitorro.basedms.db.DatabaseConfigProvider;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Spring Boot implementation of DatabaseConfigProvider.
 * 
 * Provides database configuration from Spring's DataSource, making
 * application.yml the single source of truth for database configuration.
 * 
 * Falls back to JVSProperties for any keys not mapped to Spring's DataSource
 * (e.g., "testdb" for tests).
 */
public class SpringDatabaseConfigProvider extends DatabaseConfigProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringDatabaseConfigProvider.class);
    
    private final DataSource dataSource;
    private final DataSourceProperties dataSourceProperties;
    private final ObjectMapper mapper = new ObjectMapper();
    
    // Cache the Spring DB config
    private ObjectNode springDbConfig;
    private ObjectNode hibernateConfig;
    
    public SpringDatabaseConfigProvider(DataSource dataSource, DataSourceProperties dataSourceProperties) {
        this.dataSource = dataSource;
        this.dataSourceProperties = dataSourceProperties;
        initializeSpringConfig();
    }
    
    private void initializeSpringConfig() {
        logger.info("=== Initializing Spring Database Config Provider ===");
        
        // Get connection info from Spring's DataSource
        String url = dataSourceProperties.getUrl();
        String username = dataSourceProperties.getUsername();
        String password = dataSourceProperties.getPassword();
        
        if (url == null) {
            // Try to get URL from actual connection
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                url = metaData.getURL();
                logger.info("  Detected database URL from connection: {}", url);
            } catch (Exception e) {
                logger.warn("  Could not detect database URL from connection", e);
            }
        }
        
        logger.info("  Spring DataSource URL: {}", url);
        logger.info("  Spring DataSource username: {}", username);
        
        // Create database connection config
        springDbConfig = mapper.createObjectNode();
        springDbConfig.put("url", url);
        springDbConfig.put("username", username != null ? username : "");
        springDbConfig.put("password", password != null ? password : "");
        
        // Create Hibernate configuration (minimal - will use defaults from JVSProperties)
        hibernateConfig = mapper.createObjectNode();
        hibernateConfig.put("hibernate.dialect", detectDialect(url));
        hibernateConfig.put("hibernate.connection.driver_class", detectDriver(url));
        hibernateConfig.put("hibernate.hbm2ddl.auto", "update");
        hibernateConfig.put("hibernate.show_sql", "false");
        
        logger.info("  ✓ Spring database configuration initialized");
        logger.info("=== Spring Database Config Provider ready ===");
    }
    
    @Override
    public JsonNode getDatabaseConfig(String connectionKey) {
        // For "defaultdb", return Spring's DataSource config
        if ("defaultdb".equals(connectionKey)) {
            logger.debug("Providing Spring DataSource config for key: {}", connectionKey);
            return springDbConfig;
        }
        
        // For other keys (e.g., "testdb"), fall back to JVSProperties
        logger.debug("Falling back to JVSProperties for key: {}", connectionKey);
        return JVSProperties.getProperties().get(connectionKey);
    }
    
    @Override
    public JsonNode getHibernateConfig(String databaseConfigKey) {
        // For "dbconfig", return our Spring-based Hibernate config
        if ("dbconfig".equals(databaseConfigKey)) {
            logger.debug("Providing Spring Hibernate config for key: {}", databaseConfigKey);
            return hibernateConfig;
        }
        
        // For other keys (e.g., "dbconfigderby"), fall back to JVSProperties
        logger.debug("Falling back to JVSProperties for key: {}", databaseConfigKey);
        JsonNode fromProps = JVSProperties.getProperties().get(databaseConfigKey);
        
        // If not found in properties, return our default config
        if (fromProps == null || fromProps.isNull()) {
            logger.debug("No config found in JVSProperties, using Spring config");
            return hibernateConfig;
        }
        
        return fromProps;
    }
    
    private String detectDialect(String url) {
        if (url == null) return "org.hibernate.dialect.H2Dialect";
        
        if (url.contains(":h2:")) {
            return "org.hibernate.dialect.H2Dialect";
        } else if (url.contains(":mysql:")) {
            return "org.hibernate.dialect.MySQLDialect";
        } else if (url.contains(":postgresql:")) {
            return "org.hibernate.dialect.PostgreSQLDialect";
        } else if (url.contains(":oracle:")) {
            return "org.hibernate.dialect.OracleDialect";
        }
        
        return "org.hibernate.dialect.H2Dialect";
    }
    
    private String detectDriver(String url) {
        if (url == null) return "org.h2.Driver";
        
        if (url.contains(":h2:")) {
            return "org.h2.Driver";
        } else if (url.contains(":mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        } else if (url.contains(":postgresql:")) {
            return "org.postgresql.Driver";
        } else if (url.contains(":oracle:")) {
            return "oracle.jdbc.OracleDriver";
        }
        
        return "org.h2.Driver";
    }
}
