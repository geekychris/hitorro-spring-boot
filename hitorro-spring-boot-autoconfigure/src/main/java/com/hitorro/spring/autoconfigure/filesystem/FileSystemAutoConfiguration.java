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
package com.hitorro.spring.autoconfigure.filesystem;

import com.hitorro.spring.autoconfigure.HitorroProperties;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import com.hitorro.util.basefile.fs.jarfile.JarFileSystem;
import com.hitorro.util.basefile.fs.s3.S3CompatibleFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Auto-configuration for Hitorro file system abstractions.
 * 
 * <p>Provides Spring beans for various file system implementations:
 * <ul>
 *   <li><b>Local File System</b> - Access to local files and directories</li>
 *   <li><b>JAR File System</b> - Access to files inside JAR archives</li>
 *   <li><b>S3-Compatible</b> - Access to S3-compatible object storage (MinIO, Wasabi, etc.)</li>
 * </ul>
 * 
 * <p>Enable file system support in {@code application.yml}:
 * <pre>
 * hitorro:
 *   filesystem:
 *     local:
 *       enabled: true
 *       base-path: /data
 *     jar:
 *       enabled: true
 *       jar-path: /path/to/archive.jar
 *     s3:
 *       enabled: true
 *       endpoint: http://localhost:9000
 *       bucket: my-bucket
 *       access-key: minioadmin
 *       secret-key: minioadmin
 *       ssl-enabled: false
 * </pre>
 * 
 * @see FileSystemProperties
 * @see FileFileSystem
 * @see JarFileSystem
 * @see S3CompatibleFileSystem
 */
@Configuration
@EnableConfigurationProperties({HitorroProperties.class, FileSystemProperties.class})
public class FileSystemAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(FileSystemAutoConfiguration.class);
    
    /**
     * Create local file system bean if enabled.
     * 
     * @param properties File system configuration properties
     * @return FileFileSystem for accessing local files
     */
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.filesystem.local", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "localFileSystem")
    public FileFileSystem localFileSystem(FileSystemProperties properties) {
        FileSystemProperties.LocalFileSystemConfig config = properties.getLocal();
        
        String basePath = config.getBasePath();
        if (basePath == null || basePath.isEmpty()) {
            basePath = System.getProperty("user.dir");
            logger.info("Local file system base-path not configured, using current directory: {}", basePath);
        }
        
        File baseDir = new File(basePath);
        logger.info("Creating local file system with base path: {}", baseDir.getAbsolutePath());
        
        return new FileFileSystem(baseDir);
    }
    
    /**
     * Create JAR file system bean if enabled.
     * 
     * @param properties File system configuration properties
     * @return JarFileSystem for accessing files inside JAR archives
     */
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.filesystem.jar", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "jarFileSystem")
    @ConditionalOnClass(name = "com.hitorro.util.basefile.fs.jarfile.JarFileSystem")
    public JarFileSystem jarFileSystem(FileSystemProperties properties) {
        FileSystemProperties.JarFileSystemConfig config = properties.getJar();
        
        String jarPath = config.getJarPath();
        if (jarPath == null || jarPath.isEmpty()) {
            throw new IllegalStateException("JAR file system enabled but jar-path not configured");
        }
        
        logger.info("Creating JAR file system for: {}", jarPath);
        
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            logger.warn("JAR file does not exist: {}", jarFile.getAbsolutePath());
        }
        
        return new JarFileSystem(jarFile);
    }
    
    /**
     * Create S3-compatible file system bean if enabled.
     * 
     * <p>Supports MinIO, Wasabi, DigitalOcean Spaces, and other S3-compatible services.
     * 
     * @param properties File system configuration properties
     * @return S3CompatibleFileSystem for accessing S3-compatible object storage
     * @throws IllegalStateException if required S3 configuration is missing
     */
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.filesystem.s3", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "s3FileSystem")
    @ConditionalOnClass(name = "com.hitorro.util.basefile.fs.s3.S3CompatibleFileSystem")
    public S3CompatibleFileSystem s3FileSystem(FileSystemProperties properties) {
        FileSystemProperties.S3FileSystemConfig config = properties.getS3();
        
        // Validate required S3 configuration
        if (config.getEndpoint() == null || config.getEndpoint().isEmpty()) {
            throw new IllegalStateException("S3 file system enabled but endpoint not configured");
        }
        if (config.getBucket() == null || config.getBucket().isEmpty()) {
            throw new IllegalStateException("S3 file system enabled but bucket not configured");
        }
        if (config.getAccessKey() == null || config.getAccessKey().isEmpty()) {
            throw new IllegalStateException("S3 file system enabled but access-key not configured");
        }
        if (config.getSecretKey() == null || config.getSecretKey().isEmpty()) {
            throw new IllegalStateException("S3 file system enabled but secret-key not configured");
        }
        
        logger.info("Creating S3-compatible file system:");
        logger.info("  Endpoint: {}", config.getEndpoint());
        logger.info("  Bucket: {}", config.getBucket());
        logger.info("  SSL: {}", config.isSslEnabled());
        
        return new S3CompatibleFileSystem(
            config.getEndpoint(),
            config.getBucket(),
            config.getAccessKey(),
            config.getSecretKey(),
            config.isSslEnabled()
        );
    }
}
