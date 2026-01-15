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
package com.hitorro.spring.autoconfigure.commands;

import com.hitorro.util.commandandcontrol.CommandRegistry;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Scans for @CommandDef annotated methods in Spring beans and Hitorro core classes.
 * Registers them with the CommandRegistry for use in CLI, REST, and other command interfaces.
 * 
 * Supports two types of command registration:
 * 1. @CommandDef on methods in Spring-managed beans (instance methods)
 * 2. @CommandDef on static methods in specified classes (like Env.quit)
 */
public class CommandDefScanner implements InitializingBean {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandDefScanner.class);
    
    private final ApplicationContext applicationContext;
    private final Set<Class<?>> staticCommandClasses;
    
    public CommandDefScanner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.staticCommandClasses = new HashSet<>();
        
        // Register Hitorro core classes with static @CommandDef methods
        registerCoreCommandClasses();
    }
    
    /**
     * Register Hitorro core classes that have static @CommandDef methods.
     * These are classes that aren't Spring beans but have useful commands.
     */
    private void registerCoreCommandClasses() {
        // Env class has quit command and others
        staticCommandClasses.add(com.hitorro.util.core.Env.class);
        
        // BasicCommands has various utility commands
        // (Already registered in CommandRegistry.registerBasicVerbs, but safe to re-register)
        staticCommandClasses.add(com.hitorro.util.commandandcontrol.basiccommands.BasicCommands.class);
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Scanning for @CommandDef annotated methods...");
        
        CommandRegistry registry = CommandRegistry.getRegistry();
        int commandCount = 0;
        
        // 1. Scan static methods in core Hitorro classes
        for (Class<?> clazz : staticCommandClasses) {
            try {
                logger.debug("Scanning static methods in class: {}", clazz.getName());
                registry.addAllFromClass(clazz);
                
                // Count commands found
                int count = countCommandsInClass(clazz);
                commandCount += count;
                if (count > 0) {
                    logger.info("  Registered {} static command(s) from {}", count, clazz.getSimpleName());
                }
            } catch (Exception e) {
                logger.warn("Failed to register commands from class {}: {}", clazz.getName(), e.getMessage());
                logger.debug("Command registration error details", e);
            }
        }
        
        // 2. Scan instance methods in Spring beans
        Map<String, Object> beansWithCommandDef = applicationContext.getBeansWithAnnotation(
            org.springframework.stereotype.Component.class);
        
        for (Map.Entry<String, Object> entry : beansWithCommandDef.entrySet()) {
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            
            // Skip Spring's own classes and proxies
            if (beanClass.getName().startsWith("org.springframework")) {
                continue;
            }
            
            // Check if this bean has any @CommandDef methods
            if (hasCommandDefMethods(beanClass)) {
                try {
                    logger.debug("Scanning instance methods in bean: {} ({})", 
                                entry.getKey(), beanClass.getSimpleName());
                    registry.addAllFromObject(bean);
                    
                    int count = countCommandsInClass(beanClass);
                    commandCount += count;
                    if (count > 0) {
                        logger.info("  Registered {} instance command(s) from bean '{}'", 
                                   count, entry.getKey());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to register commands from bean '{}': {}", 
                               entry.getKey(), e.getMessage());
                    logger.debug("Command registration error details", e);
                }
            }
        }
        
        logger.info("✓ Command scanning complete: {} @CommandDef command(s) registered", commandCount);
        logger.info("  Try 'help' in CLI to see all available commands");
    }
    
    /**
     * Check if a class has any @CommandDef annotated methods.
     */
    private boolean hasCommandDefMethods(Class<?> clazz) {
        // Check the actual class (handle CGLIB proxies by looking at superclass if needed)
        Class<?> targetClass = clazz;
        
        // If it's a CGLIB proxy, get the superclass (the actual bean class)
        if (clazz.getName().contains("$$EnhancerBySpringCGLIB$$")) {
            targetClass = clazz.getSuperclass();
        }
        
        for (Method method : targetClass.getDeclaredMethods()) {
            if (AnnotationUtils.findAnnotation(method, CommandDef.class) != null) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Count @CommandDef annotated methods in a class.
     */
    private int countCommandsInClass(Class<?> clazz) {
        int count = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (AnnotationUtils.findAnnotation(method, CommandDef.class) != null) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Add a class to scan for static @CommandDef methods.
     * Useful for registering custom command classes that aren't Spring beans.
     */
    public void addStaticCommandClass(Class<?> clazz) {
        staticCommandClasses.add(clazz);
    }
}
