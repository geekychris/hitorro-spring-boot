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

import com.hitorro.util.startupframework.ServiceWrapper;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for exploring Hitorro services.
 * Provides information about loaded services, their metadata, and dependencies.
 */
@RestController
@RequestMapping("/api/services")
public class ServiceExplorerController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceExplorerController.class);
    private final ServiceContextManager serviceContextManager;

    public ServiceExplorerController(ServiceContextManager serviceContextManager) {
        this.serviceContextManager = serviceContextManager;
    }

    /**
     * List all initialized Hitorro services with their metadata.
     * 
     * GET /api/services/list
     * 
     * @return List of service information
     */
    @GetMapping("/list")
    public ResponseEntity<List<ServiceInfo>> listServices() {
        try {
            List<ServiceWrapper> services = serviceContextManager.getAllServices();
            List<ServiceInfo> serviceInfos = new ArrayList<>();

            for (ServiceWrapper sw : services) {
                ServiceInfo info = buildServiceInfo(sw);
                serviceInfos.add(info);
            }

            return ResponseEntity.ok(serviceInfos);
        } catch (Exception e) {
            logger.error("Error listing services", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get detailed information about a specific service.
     * 
     * GET /api/services/{shortName}
     * 
     * @param shortName The service short name
     * @return Service information
     */
    @GetMapping("/{shortName}")
    public ResponseEntity<ServiceInfo> getService(@PathVariable String shortName) {
        try {
            ServiceWrapper sw = serviceContextManager.getServiceContext()
                .getInitializedServiceByShortname(shortName);
            
            if (sw == null) {
                return ResponseEntity.notFound().build();
            }

            ServiceInfo info = buildServiceInfo(sw);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            logger.error("Error getting service: {}", shortName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Build ServiceInfo DTO from ServiceWrapper.
     */
    private ServiceInfo buildServiceInfo(ServiceWrapper sw) {
        ServiceInfo info = new ServiceInfo();
        info.setShortName(sw.getShortName());
        info.setDescription(sw.getDescription());
        info.setClassName(sw.getClazz().getName());
        info.setInitialized(sw.isInitialized());
        
        // Version is not directly available from ServiceWrapper
        // Could be extracted from ServiceDefinition if needed in future
        info.setVersion("");

        // Use ServiceWrapper's convenience methods to get metadata
        // These already handle the @ServiceDefinition annotation
        
        // Dependent services
        Class<?>[] depServices = sw.getDependentService();
        if (depServices != null && depServices.length > 0) {
            info.setDependentServices(Arrays.stream(depServices)
                .map(Class::getName)
                .collect(Collectors.toList()));
        }

        // Dependent service interfaces
        Class<?>[] depInterfaces = sw.getDependentServiceInterfaces();
        if (depInterfaces != null && depInterfaces.length > 0) {
            info.setDependentServiceInterfaces(Arrays.stream(depInterfaces)
                .map(Class::getName)
                .collect(Collectors.toList()));
        }

        // Debug commands
        Class<?>[] debugCommands = sw.getDebugCommands();
        if (debugCommands != null && debugCommands.length > 0) {
            info.setDebugCommands(Arrays.stream(debugCommands)
                .map(Class::getSimpleName)
                .collect(Collectors.toList()));
        }

        // Type-managed classes
        Class<?>[] typeManagedClasses = sw.getTypeManagedClasses();
        if (typeManagedClasses != null && typeManagedClasses.length > 0) {
            info.setTypeManagedClasses(Arrays.stream(typeManagedClasses)
                .map(Class::getName)
                .collect(Collectors.toList()));
        }

        // UI directories
        String[] uiDirs = sw.getUIDirectories();
        if (uiDirs != null && uiDirs.length > 0) {
            info.setUiDirectories(Arrays.asList(uiDirs));
        }

        return info;
    }

    /**
     * DTO for service information.
     */
    public static class ServiceInfo {
        private String shortName;
        private String description;
        private String className;
        private boolean initialized;
        private String version;
        private List<String> dependentServices = new ArrayList<>();
        private List<String> dependentServiceInterfaces = new ArrayList<>();
        private List<String> debugCommands = new ArrayList<>();
        private List<String> typeManagedClasses = new ArrayList<>();
        private List<String> uiDirectories = new ArrayList<>();

        public String getShortName() { return shortName; }
        public void setShortName(String shortName) { this.shortName = shortName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public boolean isInitialized() { return initialized; }
        public void setInitialized(boolean initialized) { this.initialized = initialized; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public List<String> getDependentServices() { return dependentServices; }
        public void setDependentServices(List<String> dependentServices) { 
            this.dependentServices = dependentServices; 
        }

        public List<String> getDependentServiceInterfaces() { return dependentServiceInterfaces; }
        public void setDependentServiceInterfaces(List<String> dependentServiceInterfaces) { 
            this.dependentServiceInterfaces = dependentServiceInterfaces; 
        }

        public List<String> getDebugCommands() { return debugCommands; }
        public void setDebugCommands(List<String> debugCommands) { 
            this.debugCommands = debugCommands; 
        }

        public List<String> getTypeManagedClasses() { return typeManagedClasses; }
        public void setTypeManagedClasses(List<String> typeManagedClasses) { 
            this.typeManagedClasses = typeManagedClasses; 
        }

        public List<String> getUiDirectories() { return uiDirectories; }
        public void setUiDirectories(List<String> uiDirectories) { 
            this.uiDirectories = uiDirectories; 
        }
    }
}
