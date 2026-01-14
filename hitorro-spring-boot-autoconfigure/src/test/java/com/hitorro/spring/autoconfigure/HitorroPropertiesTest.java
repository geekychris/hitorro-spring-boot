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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HitorroProperties configuration class.
 */
public class HitorroPropertiesTest {

    @Test
    public void testDefaultValues() {
        HitorroProperties properties = new HitorroProperties();
        
        assertTrue(properties.isEnabled(), "Hitorro should be enabled by default");
        assertTrue(properties.getServices().isEnabled(), "Services should be enabled by default");
        assertFalse(properties.getServices().isDbInit(), "DB init should be disabled by default");
        assertTrue(properties.getDms().isEnabled(), "DMS should be enabled by default");
        assertEquals("request", properties.getDms().getSessionScope(), "DMS session scope should default to request");
        assertEquals("spring-managed", properties.getDms().getTransactionMode(), "Transaction mode should default to spring-managed");
    }

    @Test
    public void testCommandsConfiguration() {
        HitorroProperties properties = new HitorroProperties();
        
        assertTrue(properties.getCommands().getRest().isEnabled(), "REST commands should be enabled by default");
        assertEquals("/api/commands", properties.getCommands().getRest().getBasePath(), "Base path should default to /api/commands");
    }

    @Test
    public void testCliConfiguration() {
        HitorroProperties properties = new HitorroProperties();
        
        assertTrue(properties.getCli().isNativeEnabled(), "Native CLI should be enabled by default");
        assertEquals(9000, properties.getCli().getTelnetPort(), "Telnet port should default to 9000");
        assertEquals(9022, properties.getCli().getSshPort(), "SSH port should default to 9022");
        assertTrue(properties.getCli().isSpringShellEnabled(), "Spring Shell should be enabled by default");
        assertTrue(properties.getCli().isActuatorEnabled(), "Actuator should be enabled by default");
    }

    @Test
    public void testSetters() {
        HitorroProperties properties = new HitorroProperties();
        
        properties.setEnabled(false);
        assertFalse(properties.isEnabled());
        
        properties.getServices().setDbInit(true);
        assertTrue(properties.getServices().isDbInit());
        
        properties.getDms().setSessionScope("session");
        assertEquals("session", properties.getDms().getSessionScope());
        
        properties.getCli().setTelnetPort(9001);
        assertEquals(9001, properties.getCli().getTelnetPort());
    }
}
