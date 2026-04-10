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
package com.hitorro.spring.autoconfigure.dms;

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.typesystem.BaseSession;

/**
 * Spring-managed factory for creating and managing DMSSession instances.
 * Delegates to Hitorro's native DMSSessionFactory while providing Spring integration.
 * 
 * Preserves Hitorro's DMS session semantics and unified ID system.
 */
public class DMSSessionFactory {

    /**
     * Creates a new DMSSession using Hitorro's native factory.
     * 
     * @return A new DMSSession instance
     */
    public DMSSession createSession() {
        com.hitorro.util.typesystem.BaseSessionFactory factory = com.hitorro.basedms.session.DMSSessionFactory.getFactory();
        if (factory == null) {
            throw new IllegalStateException(
                "DMSSessionFactory not initialized. This typically happens when ClusterService " +
                "fails on localhost (127.0.0.1), preventing HibernateService from initializing. " +
                "Set a real hostname or configure network.disablelocalhostcheck=true in GlobalProperties.");
        }
        BaseSession session = factory.getSession();
        return (DMSSession) session;
    }
    
    /**
     * Gets a DMSSession using Hitorro's native factory (alias for createSession).
     * 
     * @return A new DMSSession instance
     */
    public DMSSession getSession() {
        System.out.println("DEBUG: DMSSessionFactory.getSession() called");
        System.out.println("DEBUG: Native factory: " + com.hitorro.basedms.session.DMSSessionFactory.getFactory());
        DMSSession session = createSession();
        System.out.println("DEBUG: Session created: " + session);
        System.out.println("DEBUG: Session class: " + (session != null ? session.getClass().getName() : "null"));
        return session;
    }
    
    /**
     * Gets the native Hitorro DMSSessionFactory (returns BaseSessionFactory).
     * 
     * @return The Hitorro DMSSessionFactory instance as BaseSessionFactory
     */
    public com.hitorro.util.typesystem.BaseSessionFactory getNativeFactory() {
        return com.hitorro.basedms.session.DMSSessionFactory.getFactory();
    }
}
