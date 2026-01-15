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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Hitorro file systems.
 * 
 * <p>Example configuration in {@code application.yml}:
 * <pre>
 * hitorro:
 *   filesystem:
 *     # Local file system
 *     local:
 *       enabled: true
 *       base-path: /data
 *     
 *     # JAR file system
 *     jar:
 *       enabled: true
 *       jar-path: /path/to/resources.jar
 *     
 *     # S3-compatible file system (MinIO, Wasabi, etc.)
 *     s3:
 *       enabled: true
 *       endpoint: http://localhost:9000
 *       bucket: my-bucket
 *       access-key: minioadmin
 *       secret-key: minioadmin
 *       ssl-enabled: false
 * </pre>
 */
@ConfigurationProperties(prefix = "hitorro.filesystem")
public class FileSystemProperties {
    
    private LocalFileSystemConfig local = new LocalFileSystemConfig();
    private JarFileSystemConfig jar = new JarFileSystemConfig();
    private S3FileSystemConfig s3 = new S3FileSystemConfig();
    
    public LocalFileSystemConfig getLocal() {
        return local;
    }
    
    public void setLocal(LocalFileSystemConfig local) {
        this.local = local;
    }
    
    public JarFileSystemConfig getJar() {
        return jar;
    }
    
    public void setJar(JarFileSystemConfig jar) {
        this.jar = jar;
    }
    
    public S3FileSystemConfig getS3() {
        return s3;
    }
    
    public void setS3(S3FileSystemConfig s3) {
        this.s3 = s3;
    }
    
    /**
     * Configuration for local file system.
     */
    public static class LocalFileSystemConfig {
        private boolean enabled = false;
        private String basePath;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getBasePath() {
            return basePath;
        }
        
        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }
    
    /**
     * Configuration for JAR file system.
     */
    public static class JarFileSystemConfig {
        private boolean enabled = false;
        private String jarPath;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getJarPath() {
            return jarPath;
        }
        
        public void setJarPath(String jarPath) {
            this.jarPath = jarPath;
        }
    }
    
    /**
     * Configuration for S3-compatible file system.
     */
    public static class S3FileSystemConfig {
        private boolean enabled = false;
        private String endpoint;
        private String bucket;
        private String accessKey;
        private String secretKey;
        private boolean sslEnabled = true;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public String getBucket() {
            return bucket;
        }
        
        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
        
        public String getAccessKey() {
            return accessKey;
        }
        
        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }
        
        public String getSecretKey() {
            return secretKey;
        }
        
        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
        
        public boolean isSslEnabled() {
            return sslEnabled;
        }
        
        public void setSslEnabled(boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
        }
    }
}
