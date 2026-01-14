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
package com.hitorro.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Hitorro Spring Boot integration.
 */
@ConfigurationProperties(prefix = "hitorro")
public class HitorroProperties {

    /**
     * Whether Hitorro services should be enabled.
     */
    private boolean enabled = true;

    /**
     * Service-related configuration.
     */
    private Services services = new Services();

    /**
     * DMS-related configuration.
     */
    private Dms dms = new Dms();

    /**
     * Command framework configuration.
     */
    private Commands commands = new Commands();

    /**
     * CLI configuration.
     */
    private Cli cli = new Cli();
    
    /**
     * REST endpoint configuration.
     */
    private Rest rest = new Rest();
    
    /**
     * JSON Type System configuration.
     */
    private Jvs jvs = new Jvs();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public Dms getDms() {
        return dms;
    }

    public void setDms(Dms dms) {
        this.dms = dms;
    }

    public Commands getCommands() {
        return commands;
    }

    public void setCommands(Commands commands) {
        this.commands = commands;
    }

    public Cli getCli() {
        return cli;
    }

    public void setCli(Cli cli) {
        this.cli = cli;
    }
    
    public Rest getRest() {
        return rest;
    }
    
    public void setRest(Rest rest) {
        this.rest = rest;
    }
    
    public Jvs getJvs() {
        return jvs;
    }
    
    public void setJvs(Jvs jvs) {
        this.jvs = jvs;
    }

    public static class Services {
        /**
         * Whether to auto-configure Hitorro services.
         */
        private boolean enabled = true;

        /**
         * Path to startup scripts to execute.
         */
        private String startupScripts;

        /**
         * Whether to perform database initialization.
         */
        private boolean dbInit = false;

        /**
         * List of service class names or short names to load.
         * Example: ["com.hitorro.base.service.BasicService", "hibernate", "typesystem"]
         */
        private List<String> load = new ArrayList<>();

        /**
         * Server type for HTServer-style loading (reads services.{type} from properties).
         * If set, services will be loaded from services.{serverType} configuration.
         */
        private String serverType = null;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getStartupScripts() {
            return startupScripts;
        }

        public void setStartupScripts(String startupScripts) {
            this.startupScripts = startupScripts;
        }

        public boolean isDbInit() {
            return dbInit;
        }

        public void setDbInit(boolean dbInit) {
            this.dbInit = dbInit;
        }

        public List<String> getLoad() {
            return load;
        }

        public void setLoad(List<String> load) {
            this.load = load;
        }

        public String getServerType() {
            return serverType;
        }

        public void setServerType(String serverType) {
            this.serverType = serverType;
        }
    }

    public static class Dms {
        /**
         * Whether to enable DMS integration.
         */
        private boolean enabled = true;

        /**
         * Session scope: request, session, or prototype.
         */
        private String sessionScope = "request";

        /**
         * Transaction mode: spring-managed or hitorro-native.
         */
        private String transactionMode = "spring-managed";
        
        /**
         * Database initialization configuration.
         */
        private DbInit dbInit = new DbInit();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSessionScope() {
            return sessionScope;
        }

        public void setSessionScope(String sessionScope) {
            this.sessionScope = sessionScope;
        }

        public String getTransactionMode() {
            return transactionMode;
        }

        public void setTransactionMode(String transactionMode) {
            this.transactionMode = transactionMode;
        }
        
        public DbInit getDbInit() {
            return dbInit;
        }
        
        public void setDbInit(DbInit dbInit) {
            this.dbInit = dbInit;
        }
        
        /**
         * Database initialization configuration for loading data from CSV files.
         */
        public static class DbInit {
            /**
             * Whether to run database initialization on startup.
             */
            private boolean enabled = false;
            
            /**
             * Whether to fail application startup if data loading fails.
             */
            private boolean failOnError = false;
            
            /**
             * List of datasets to load on startup.
             */
            private List<DataSet> dataSets = new ArrayList<>();
            
            public boolean isEnabled() {
                return enabled;
            }
            
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
            
            public boolean isFailOnError() {
                return failOnError;
            }
            
            public void setFailOnError(boolean failOnError) {
                this.failOnError = failOnError;
            }
            
            public List<DataSet> getDataSets() {
                return dataSets;
            }
            
            public void setDataSets(List<DataSet> dataSets) {
                this.dataSets = dataSets;
            }
            
            /**
             * Configuration for a single dataset to load.
             */
            public static class DataSet {
                /**
                 * Name of this dataset (for logging).
                 */
                private String name;
                
                /**
                 * Path to CSV file (supports Spring Resource patterns like classpath:, file:, etc).
                 */
                private String csvFile;
                
                /**
                 * Fully qualified class name of CSVHibernateLoaderConsumer to use.
                 */
                private String consumer;
                
                public String getName() {
                    return name;
                }
                
                public void setName(String name) {
                    this.name = name;
                }
                
                public String getCsvFile() {
                    return csvFile;
                }
                
                public void setCsvFile(String csvFile) {
                    this.csvFile = csvFile;
                }
                
                public String getConsumer() {
                    return consumer;
                }
                
                public void setConsumer(String consumer) {
                    this.consumer = consumer;
                }
            }
        }
    }
    
    /**
     * REST endpoint configuration.
     */
    public static class Rest {
        /**
         * Whether to enable REST endpoint discovery and mapping.
         */
        private boolean enabled = true;
        
        /**
         * Base path for REST endpoints.
         */
        private String basePath = "/api/rest";
        
        /**
         * Whether to expose internal commands via REST.
         */
        private boolean exposeInternal = false;
        
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
        
        public boolean isExposeInternal() {
            return exposeInternal;
        }
        
        public void setExposeInternal(boolean exposeInternal) {
            this.exposeInternal = exposeInternal;
        }
    }

    public static class Commands {
        /**
         * REST endpoint configuration.
         */
        private Rest rest = new Rest();

        public Rest getRest() {
            return rest;
        }

        public void setRest(Rest rest) {
            this.rest = rest;
        }

        public static class Rest {
            /**
             * Whether to enable REST endpoints for commands.
             */
            private boolean enabled = true;

            /**
             * Base path for command REST endpoints.
             */
            private String basePath = "/api/commands";

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
    }

    public static class Cli {
        /**
         * Whether to enable Hitorro's native CLI (telnet/SSH).
         */
        private boolean nativeEnabled = true;

        /**
         * Telnet port for CLI access.
         */
        private int telnetPort = 9000;

        /**
         * SSH port for CLI access.
         */
        private int sshPort = 9022;

        /**
         * Whether to enable Spring Shell integration.
         */
        private boolean springShellEnabled = true;

        /**
         * Whether to enable Actuator endpoint for commands.
         */
        private boolean actuatorEnabled = true;

        public boolean isNativeEnabled() {
            return nativeEnabled;
        }

        public void setNativeEnabled(boolean nativeEnabled) {
            this.nativeEnabled = nativeEnabled;
        }

        public int getTelnetPort() {
            return telnetPort;
        }

        public void setTelnetPort(int telnetPort) {
            this.telnetPort = telnetPort;
        }

        public int getSshPort() {
            return sshPort;
        }

        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }

        public boolean isSpringShellEnabled() {
            return springShellEnabled;
        }

        public void setSpringShellEnabled(boolean springShellEnabled) {
            this.springShellEnabled = springShellEnabled;
        }

        public boolean isActuatorEnabled() {
            return actuatorEnabled;
        }

        public void setActuatorEnabled(boolean actuatorEnabled) {
            this.actuatorEnabled = actuatorEnabled;
        }
    }
    
    /**
     * JSON Type System configuration.
     */
    public static class Jvs {
        /**
         * Whether to enable JVS integration.
         */
        private boolean enabled = true;
        
        /**
         * Whether to enable NLP features (WordNet, OpenNLP).
         */
        private boolean nlpEnabled = false;
        
        /**
         * REST endpoint configuration for JVS.
         */
        private JvsRest rest = new JvsRest();
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isNlpEnabled() {
            return nlpEnabled;
        }
        
        public void setNlpEnabled(boolean nlpEnabled) {
            this.nlpEnabled = nlpEnabled;
        }
        
        public JvsRest getRest() {
            return rest;
        }
        
        public void setRest(JvsRest rest) {
            this.rest = rest;
        }
        
        /**
         * JVS REST endpoint configuration.
         */
        public static class JvsRest {
            /**
             * Whether to enable REST endpoints for JVS.
             */
            private boolean enabled = false;
            
            /**
             * Base path for JVS REST endpoints.
             */
            private String basePath = "/api/jvs";
            
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
    }
}
