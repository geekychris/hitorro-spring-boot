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
package com.hitorro.spring.autoconfigure.cli;

import com.hitorro.util.commandandcontrol.SshListener;
import com.hitorro.util.commandandcontrol.TerminalListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Manages the lifecycle of Hitorro's native CLI (telnet/SSH).
 * This preserves the original Hitorro CLI functionality while running in Spring Boot.
 * 
 * Commands registered in the CommandRegistry are automatically available via:
 * - Telnet on configured port (default: 5050)
 * - SSH on configured port (default: 5022)
 */
public class NativeCliManager implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(NativeCliManager.class);

    private final int telnetPort;
    private final int sshPort;
    
    private Thread telnetThread;
    private Thread sshThread;
    private TerminalListener telnetListener;
    private SshListener sshListener;

    public NativeCliManager(int telnetPort, int sshPort) {
        this.telnetPort = telnetPort;
        this.sshPort = sshPort;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Starting Hitorro native CLI");
        
        // Start Telnet listener
        if (telnetPort > 0) {
            try {
                telnetListener = new TerminalListener();
                // TerminalListener will read port from properties or use default
                
                telnetThread = new Thread(telnetListener, "Hitorro-Telnet-Listener");
                telnetThread.setDaemon(true);
                telnetThread.start();
                
                // Give it a moment to start
                Thread.sleep(200);
                int actualPort = telnetListener.getPort();
                logger.info("✓ Telnet CLI started on port {}", actualPort);
                logger.info("  Connect: telnet localhost {}", actualPort);
                logger.info("  Try: telnet localhost {} (then type 'help')", actualPort);
            } catch (Exception e) {
                logger.warn("Failed to start Telnet listener: {}", e.getMessage());
            }
        }
        
        // Start SSH listener  
        if (sshPort > 0) {
            try {
                sshListener = new SshListener();
                // SshListener will read port from properties or use default
                
                sshThread = new Thread(sshListener, "Hitorro-SSH-Listener");
                sshThread.setDaemon(true);
                sshThread.start();
                
                Thread.sleep(200);
                int actualPort = sshListener.getPort();
                logger.info("✓ SSH CLI started on port {}", actualPort);
                logger.info("  Connect: ssh -p {} user@localhost (password: user)", actualPort);
            } catch (Exception e) {
                logger.warn("SSH listener not available (may need additional dependencies): {}", e.getMessage());
                logger.debug("SSH error details", e);
            }
        }
        
        logger.info("Hitorro native CLI initialization complete");
        logger.info("Commands are available - type 'help' in CLI session to see all registered commands");
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Stopping Hitorro native CLI");
        
        // Telnet and SSH listeners run in daemon threads
        // They will be stopped when application shuts down
        // No explicit cleanup needed for daemon threads
        
        if (telnetThread != null && telnetThread.isAlive()) {
            logger.info("Telnet CLI thread will stop with application shutdown");
        }
        
        if (sshThread != null && sshThread.isAlive()) {
            logger.info("SSH CLI thread will stop with application shutdown");
        }
        
        logger.info("Hitorro native CLI shutdown complete");
    }

    public int getTelnetPort() {
        return telnetPort;
    }

    public int getSshPort() {
        return sshPort;
    }
    
    public TerminalListener getTelnetListener() {
        return telnetListener;
    }
    
    public SshListener getSshListener() {
        return sshListener;
    }
}
