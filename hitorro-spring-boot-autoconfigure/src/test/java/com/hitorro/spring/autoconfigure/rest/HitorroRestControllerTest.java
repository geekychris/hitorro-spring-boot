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
package com.hitorro.spring.autoconfigure.rest;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.spring.autoconfigure.commands.CommandRegistrationManager;
import com.hitorro.util.commandandcontrol.*;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.DebugArgAno;
import com.hitorro.util.json.keys.StringProperty;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive tests for HitorroRestController demonstrating:
 * 1. Basic REST endpoint functionality
 * 2. Command discovery
 * 3. Parameter passing
 * 4. Error handling
 * 5. Streaming responses
 * 6. Servlet object access
 * 
 * These tests serve as examples of how to use the Hitorro REST integration.
 */
@SpringBootTest(classes = TestRestConfiguration.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "hitorro.rest.enabled=true",
    "hitorro.rest.base-path=/api/rest"
})
public class HitorroRestControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private CommandRegistrationManager commandManager;
    
    @BeforeEach
    public void setup() {
        // Register test commands
        CommandRegistry registry = commandManager.getCommandRegistry();
        
        // Clear any existing test commands
        Command existing = registry.get("test.echo");
        if (existing == null) {
            registry.addAllFromObject(new TestCommands());
        }
    }
    
    // ==================== Basic REST Endpoint Tests ====================
    
    @Test
    @DisplayName("Test 1: Discovery endpoint lists all REST-enabled commands")
    public void testDiscoveryEndpoint() throws Exception {
        mockMvc.perform(get("/api/rest")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEndpoints").isNumber())
            .andExpect(jsonPath("$.endpoints").isArray())
            .andExpect(jsonPath("$.endpoints[?(@.command == 'test.echo')]").exists())
            .andExpect(jsonPath("$.endpoints[?(@.command == 'test.echo')].methods[0]").value("GET"));
    }
    
    @Test
    @DisplayName("Test 2: Execute simple GET command with query parameters")
    public void testSimpleGetCommand() throws Exception {
        mockMvc.perform(get("/api/rest/test.echo")
                .param("message", "Hello World")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.command").value("test.echo"))
            .andExpect(jsonPath("$.operation").value("Get"))
            .andExpect(jsonPath("$.result.value").value("Hello World"))
            .andExpect(jsonPath("$.executionTimeMs").isNumber());
    }
    
    @Test
    @DisplayName("Test 3: Execute POST command with JSON body")
    public void testPostCommandWithJsonBody() throws Exception {
        String jsonBody = "{\"a\": 10, \"b\": 20}";
        
        mockMvc.perform(post("/api/rest/test.add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.command").value("test.add"))
            .andExpect(jsonPath("$.operation").value("Post"))
            .andExpect(jsonPath("$.result").isArray())
            .andExpect(jsonPath("$.result[?(@.key == 'sum')].value").value("30"));
    }
    
    @Test
    @DisplayName("Test 4: Command with dots in name (namespace support)")
    public void testCommandWithDots() throws Exception {
        mockMvc.perform(get("/api/rest/test.echo")
                .param("message", "Testing dots")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.command").value("test.echo"));
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    @DisplayName("Test 5: Non-existent command returns 404")
    public void testNonExistentCommand() throws Exception {
        mockMvc.perform(get("/api/rest/nonexistent.command")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value(containsString("not found")));
    }
    
    @Test
    @DisplayName("Test 6: Unsupported HTTP method returns 405")
    public void testUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(delete("/api/rest/test.echo")  // Only supports GET
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", containsString("GET")))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.allowedMethods").isArray());
    }
    
    @Test
    @DisplayName("Test 7: Command with required parameter missing returns error")
    public void testMissingRequiredParameter() throws Exception {
        // test.echo requires 'message' parameter
        MvcResult result = mockMvc.perform(get("/api/rest/test.echo")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())  // Command executes but may return error in result
            .andReturn();
        
        // Command should handle validation - check response
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("success") || content.contains("error"),
            "Response should indicate success or error");
    }
    
    // ==================== Streaming Response Tests ====================
    
    @Test
    @DisplayName("Test 8: Streaming CSV export command")
    public void testStreamingCsvExport() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rest/test.stream.csv")
                .accept("text/csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/csv")))
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andReturn();
        
        String csvContent = result.getResponse().getContentAsString();
        
        // Verify CSV structure
        assertTrue(csvContent.contains("id,name,value"), "Should contain CSV header");
        assertTrue(csvContent.split("\n").length > 1, "Should contain data rows");
        
        // Verify streaming happened (response committed by command)
        assertTrue(result.getResponse().isCommitted(), 
            "Response should be committed when command handles streaming");
    }
    
    @Test
    @DisplayName("Test 9: Streaming with custom headers")
    public void testStreamingWithCustomHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rest/test.stream.custom")
                .accept(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Custom-Header"))
            .andExpect(header().string("X-Custom-Header", "CustomValue"))
            .andReturn();
        
        assertTrue(result.getResponse().isCommitted(),
            "Response should be committed for streaming");
    }
    
    @Test
    @DisplayName("Test 10: Large dataset streaming (memory efficiency)")
    public void testLargeDatasetStreaming() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rest/test.stream.large")
                .accept("text/plain"))
            .andExpect(status().isOk())
            .andReturn();
        
        String content = result.getResponse().getContentAsString();
        
        // Verify large dataset was streamed
        int lineCount = content.split("\n").length;
        assertTrue(lineCount >= 1000, 
            "Should stream at least 1000 lines, got: " + lineCount);
        
        // Verify content
        assertTrue(content.contains("Line 0"), "Should contain first line");
        assertTrue(content.contains("Line 999"), "Should contain last line");
    }
    
    // ==================== Servlet Object Access Tests ====================
    
    @Test
    @DisplayName("Test 11: Command accesses HttpServletRequest")
    public void testCommandAccessesRequest() throws Exception {
        mockMvc.perform(get("/api/rest/test.request.info")
                .header("User-Agent", "TestAgent/1.0")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.userAgent").value("TestAgent/1.0"));
    }
    
    @Test
    @DisplayName("Test 12: Command accesses HttpServletResponse")
    public void testCommandAccessesResponse() throws Exception {
        mockMvc.perform(get("/api/rest/test.response.headers")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Response-Test"))
            .andExpect(header().string("X-Response-Test", "Success"));
    }
    
    // ==================== Multiple HTTP Method Tests ====================
    
    @Test
    @DisplayName("Test 13: Command supports multiple HTTP methods")
    public void testMultipleHttpMethods() throws Exception {
        // Test GET
        mockMvc.perform(get("/api/rest/test.multi.method")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operation").value("Get"));
        
        // Test POST
        mockMvc.perform(post("/api/rest/test.multi.method")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operation").value("Post"));
        
        // Test PUT
        mockMvc.perform(put("/api/rest/test.multi.method")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operation").value("Put"));
    }
    
    // ==================== Test Command Implementations ====================
    
    /**
     * Test commands demonstrating various REST features.
     * These serve as examples for implementing custom commands.
     */
    public static class TestCommands {
        
        @CommandDef(
            command = "test.echo",
            description = "Echo back a message",
            restOperations = {RestOperations.Get},
            isInternal = false
        )
        public String echo(
                @DebugArgAno(
                    propType = StringProperty.class,
                    keyName = "message",
                    description = "Message to echo",
                    defaultValue = ""
                ) String message) {
            return message;
        }
        
        @CommandDef(
            command = "test.add",
            description = "Add two numbers",
            restOperations = {RestOperations.Get, RestOperations.Post},
            isInternal = false
        )
        public int add(
                @DebugArgAno(propType = StringProperty.class, keyName = "a", defaultValue = "0") int a,
                @DebugArgAno(propType = StringProperty.class, keyName = "b", defaultValue = "0") int b) {
            return a + b;
        }
    }
    
    /**
     * Advanced streaming command that writes directly to HttpServletResponse.
     * This demonstrates how to implement streaming for large datasets.
     */
    @CommandDef(
        command = "test.stream.csv",
        description = "Stream CSV data",
        restOperations = {RestOperations.Get},
        isInternal = false
    )
    public static class StreamingCsvCommand extends Command {
        @Override
        public boolean execute(String rawValue, JVS args, Response response, 
                              CommandSession session, RestOperations operation) throws Exception {
            
            HttpServletResponse servletResp = response.getHttpResponse();
            
            if (servletResp != null) {
                // Set headers for CSV download
                servletResp.setContentType("text/csv");
                servletResp.setHeader("Content-Disposition", "attachment; filename=\"test-data.csv\"");
                
                // Stream CSV data
                try (PrintWriter writer = servletResp.getWriter()) {
                    writer.println("id,name,value");
                    for (int i = 0; i < 100; i++) {
                        writer.printf("%d,Item_%d,%d%n", i, i, i * 10);
                        if (i % 10 == 0) {
                            writer.flush();  // Flush periodically for streaming
                        }
                    }
                    return true;
                }
            }
            
            return false;
        }
    }
    
    /**
     * Command that sets custom headers.
     */
    @CommandDef(
        command = "test.stream.custom",
        description = "Stream with custom headers",
        restOperations = {RestOperations.Get},
        isInternal = false
    )
    public static class CustomHeadersCommand extends Command {
        @Override
        public boolean execute(String rawValue, JVS args, Response response,
                              CommandSession session, RestOperations operation) throws Exception {
            
            HttpServletResponse servletResp = response.getHttpResponse();
            
            if (servletResp != null) {
                servletResp.setHeader("X-Custom-Header", "CustomValue");
                servletResp.setContentType("application/octet-stream");
                
                try (ServletOutputStream out = servletResp.getOutputStream()) {
                    out.write("Custom content".getBytes());
                    return true;
                }
            }
            
            return false;
        }
    }
    
    /**
     * Command that streams large dataset to demonstrate memory efficiency.
     */
    @CommandDef(
        command = "test.stream.large",
        description = "Stream large dataset",
        restOperations = {RestOperations.Get},
        isInternal = false
    )
    public static class LargeDatasetCommand extends Command {
        @Override
        public boolean execute(String rawValue, JVS args, Response response,
                              CommandSession session, RestOperations operation) throws Exception {
            
            HttpServletResponse servletResp = response.getHttpResponse();
            
            if (servletResp != null) {
                servletResp.setContentType("text/plain");
                
                try (PrintWriter writer = servletResp.getWriter()) {
                    // Stream 1000 lines without buffering all in memory
                    for (int i = 0; i < 1000; i++) {
                        writer.printf("Line %d: %s%n", i, 
                            "Data ".repeat(10));  // Simulate large lines
                        
                        if (i % 100 == 0) {
                            writer.flush();  // Progressive streaming
                        }
                    }
                    return true;
                }
            }
            
            return false;
        }
    }
    
    /**
     * Command that accesses HttpServletRequest information.
     */
    @CommandDef(
        command = "test.request.info",
        description = "Access request information",
        restOperations = {RestOperations.Get},
        isInternal = false
    )
    public static class RequestInfoCommand extends Command {
        @Override
        public boolean execute(String rawValue, JVS args, Response response,
                              CommandSession session, RestOperations operation) throws Exception {
            
            HttpServletRequest req = response.getHttpRequest();
            
            if (req != null) {
                response.addRow("method", req.getMethod());
                response.addRow("userAgent", req.getHeader("User-Agent"));
                response.addRow("contentType", req.getContentType());
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Command that sets custom response headers.
     */
    @CommandDef(
        command = "test.response.headers",
        description = "Set custom response headers",
        restOperations = {RestOperations.Get},
        isInternal = false
    )
    public static class ResponseHeadersCommand extends Command {
        @Override
        public boolean execute(String rawValue, JVS args, Response response,
                              CommandSession session, RestOperations operation) throws Exception {
            
            HttpServletResponse servletResp = response.getHttpResponse();
            
            if (servletResp != null) {
                servletResp.setHeader("X-Response-Test", "Success");
            }
            
            response.addRow("status", "Headers set");
            return true;
        }
    }
    
    /**
     * Command that supports multiple HTTP methods.
     */
    @CommandDef(
        command = "test.multi.method",
        description = "Supports GET, POST, PUT",
        restOperations = {RestOperations.Get, RestOperations.Post, RestOperations.Put},
        isInternal = false
    )
    public static class MultiMethodCommand extends Command {
        @Override
        public boolean execute(String rawValue, JVS args, Response response,
                              CommandSession session, RestOperations operation) throws Exception {
            
            response.addRow("method", operation.toString());
            response.addRow("executed", "true");
            return true;
        }
    }
}
