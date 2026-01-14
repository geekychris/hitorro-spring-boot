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
package com.hitorro.spring.autoconfigure.jvs;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.Type;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for JSON Type System operations.
 * 
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/jvs/parse - Parse JSON and return type info</li>
 *   <li>GET /api/jvs/types - List available types</li>
 *   <li>GET /api/jvs/types/{name} - Get type definition</li>
 *   <li>POST /api/jvs/query - Query JVS document properties</li>
 * </ul>
 */
@RestController
@RequestMapping("${hitorro.jvs.rest.base-path:/api/jvs}")
public class JVSRestController {
    
    private final JsonTypeSystemManager jtsManager;
    
    public JVSRestController(JsonTypeSystemManager jtsManager) {
        this.jtsManager = jtsManager;
    }
    
    /**
     * Parse JSON and get type information.
     * 
     * Example:
     * POST /api/jvs/parse
     * {"type":"article","title":{"mls":"Hello World"}}
     */
    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parseJSON(@RequestBody String jsonString) {
        try {
            JVS jvs = jtsManager.createJVS(jsonString);
            Type type = jvs.getType();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("typeName", type != null ? type.getName() : "unknown");
            result.put("hasType", type != null);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get type definition by name.
     * 
     * Example:
     * GET /api/jvs/types/article
     */
    @GetMapping("/types/{typeName}")
    public ResponseEntity<Map<String, Object>> getType(@PathVariable String typeName) {
        Type type = jtsManager.getType(typeName);
        
        if (type == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Type not found: " + typeName);
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("typeName", type.getName());
        result.put("exists", true);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Query properties from a JVS document.
     * 
     * Example:
     * POST /api/jvs/query
     * {
     *   "document": "{\"title\":{\"mls\":\"Test\"}}",
     *   "properties": ["title.mls", "id.id"]
     * }
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> queryProperties(@RequestBody QueryRequest request) {
        try {
            JVS jvs = jtsManager.createJVS(request.getDocument());
            Map<String, Object> values = new HashMap<>();
            
            for (String propPath : request.getProperties()) {
                try {
                    // Try different accessor types
                    String stringVal = jvs.getString(propPath);
                    if (stringVal != null) {
                        values.put(propPath, stringVal);
                        continue;
                    }
                    
                    Date dateVal = jvs.getDate(propPath);
                    if (dateVal != null) {
                        values.put(propPath, dateVal);
                        continue;
                    }
                    
                    List<String> listVal = jvs.getStringList(propPath);
                    if (listVal != null) {
                        values.put(propPath, listVal);
                        continue;
                    }
                    
                    JsonNode nodeVal = jvs.get(propPath);
                    if (nodeVal != null) {
                        values.put(propPath, nodeVal.toString());
                    }
                } catch (Exception e) {
                    values.put(propPath, "ERROR: " + e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("values", values);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Request model for property queries.
     */
    public static class QueryRequest {
        private String document;
        private List<String> properties;
        
        public String getDocument() {
            return document;
        }
        
        public void setDocument(String document) {
            this.document = document;
        }
        
        public List<String> getProperties() {
            return properties;
        }
        
        public void setProperties(List<String> properties) {
            this.properties = properties;
        }
    }
}
