package com.hitorro.spring.autoconfigure.transformer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify transformer configuration is correct
 */
public class TransformerConfigurationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(TransformerConfigurationTest.class);
    
    @Test
    public void testSpringFactoriesContainsTransformerAutoConfiguration() {
        logger.info("Testing spring.factories contains TransformerAutoConfiguration...");
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/spring.factories")) {
            assertNotNull(is, "spring.factories file should exist");
            
            String content = new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n"));
            
            logger.info("spring.factories content:\n{}", content);
            
            assertTrue(content.contains("TransformerAutoConfiguration"),
                "spring.factories should contain TransformerAutoConfiguration");
            
            assertTrue(content.contains("com.hitorro.spring.autoconfigure.transformer.TransformerAutoConfiguration"),
                "spring.factories should contain full TransformerAutoConfiguration class name");
            
            logger.info("✓ spring.factories correctly configured");
        } catch (Exception e) {
            fail("Failed to read spring.factories: " + e.getMessage());
        }
    }
    
    @Test
    public void testTransformerAutoConfigurationClassExists() {
        logger.info("Testing TransformerAutoConfiguration class exists...");
        
        try {
            Class<?> clazz = Class.forName("com.hitorro.spring.autoconfigure.transformer.TransformerAutoConfiguration");
            assertNotNull(clazz, "TransformerAutoConfiguration class should exist");
            
            logger.info("✓ TransformerAutoConfiguration class found: {}", clazz.getName());
        } catch (ClassNotFoundException e) {
            fail("TransformerAutoConfiguration class not found: " + e.getMessage());
        }
    }
    
    @Test
    public void testRenditionTransformationControllerClassExists() {
        logger.info("Testing RenditionTransformationController class exists...");
        
        try {
            Class<?> clazz = Class.forName("com.hitorro.spring.autoconfigure.transformer.RenditionTransformationController");
            assertNotNull(clazz, "RenditionTransformationController class should exist");
            
            logger.info("✓ RenditionTransformationController class found: {}", clazz.getName());
        } catch (ClassNotFoundException e) {
            fail("RenditionTransformationController class not found: " + e.getMessage());
        }
    }
    
    @Test
    public void testDocumentContentControllerClassExists() {
        logger.info("Testing DocumentContentController class exists...");
        
        try {
            Class<?> clazz = Class.forName("com.hitorro.spring.autoconfigure.transformer.DocumentContentController");
            assertNotNull(clazz, "DocumentContentController class should exist");
            
            logger.info("✓ DocumentContentController class found: {}", clazz.getName());
        } catch (ClassNotFoundException e) {
            fail("DocumentContentController class not found: " + e.getMessage());
        }
    }
    
    @Test
    public void testTransformerToolsAvailable() {
        logger.info("Testing transformer tools availability...");
        
        boolean hasPdftoppm = checkCommand("pdftoppm", "--version");
        boolean hasSoffice = checkCommand("soffice", "--version");
        boolean hasConvert = checkCommand("convert", "--version");
        
        logger.info("pdftoppm: {}", hasPdftoppm ? "✓ available" : "✗ not available");
        logger.info("soffice: {}", hasSoffice ? "✓ available" : "✗ not available");
        logger.info("convert: {}", hasConvert ? "✓ available" : "✗ not available");
        
        int availableCount = (hasPdftoppm ? 1 : 0) + (hasSoffice ? 1 : 0) + (hasConvert ? 1 : 0);
        logger.info("Total tools available: {}/3", availableCount);
        
        if (availableCount == 0) {
            logger.warn("⚠ No transformer tools available - transformations will not work!");
        }
    }
    
    private boolean checkCommand(String command, String... args) {
        try {
            String[] cmd = new String[args.length + 1];
            cmd[0] = command;
            System.arraycopy(args, 0, cmd, 1, args.length);
            
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
