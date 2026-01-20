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
package com.hitorro.spring.autoconfigure.transformer;

import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.contentconstraints.MimeTypeContentConstraint;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.transformer.*;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.opers.LogicalAndOperator;
import com.hitorro.basedms.transformer.constraints.FromConstraint;
import com.hitorro.basedms.transformer.constraints.ToConstraint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for managing content rendition transformations
 */
@RestController
@RequestMapping("/api/transformer")
public class RenditionTransformationController {
    
    /**
     * Get available target MIME types for a given source MIME type
     * 
     * GET /api/transformer/available-targets?sourceMimeType=application/pdf
     */
    @GetMapping("/available-targets")
    public ResponseEntity<?> getAvailableTargets(@RequestParam("sourceMimeType") String sourceMimeType) {
        try {
            List<String> targets = RenditionTransformationHelper.getAvailableTargetMimeTypes(sourceMimeType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sourceMimeType", sourceMimeType);
            response.put("availableTargets", targets);
            response.put("count", targets.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get available targets", "message", e.getMessage()));
        }
    }
    
    /**
     * Get all available transformations for a given source MIME type
     * 
     * GET /api/transformer/transformations?sourceMimeType=application/pdf
     */
    @GetMapping("/transformations")
    public ResponseEntity<?> getAvailableTransformations(@RequestParam("sourceMimeType") String sourceMimeType) {
        try {
            Map<String, RenditionTransformationHelper.TransformationInfo> transformations = 
                    RenditionTransformationHelper.getAvailableTransformations(sourceMimeType);
            
            List<Map<String, Object>> transformationList = new ArrayList<>();
            for (Map.Entry<String, RenditionTransformationHelper.TransformationInfo> entry : transformations.entrySet()) {
                RenditionTransformationHelper.TransformationInfo info = entry.getValue();
                Map<String, Object> item = new HashMap<>();
                item.put("targetMimeType", info.getTargetMimeType());
                item.put("transformer", info.getTransformerName());
                item.put("method", info.getMethodName());
                item.put("methodArgs", info.getMethodArgs());
                item.put("available", info.isAvailable());
                transformationList.add(item);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("sourceMimeType", sourceMimeType);
            response.put("transformations", transformationList);
            response.put("count", transformationList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get transformations", "message", e.getMessage()));
        }
    }
    
    /**
     * Get available transformations for a specific content rendition
     * 
     * GET /api/transformer/content/{contentGuid}/available-transformations
     */
    @GetMapping("/content/{contentGuid}/available-transformations")
    public ResponseEntity<?> getContentAvailableTransformations(@PathVariable String contentGuid) {
        DMSSession session = null;
        try {
            session = (DMSSession) DMSSessionFactory.getFactory().getSession();
            
            Content content = (Content) session.getObjectFromGuid(contentGuid);
            if (content == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Content not found", "guid", contentGuid));
            }
            
            String sourceMimeType = content.getContentType() != null ? 
                    content.getContentType().getMimeType() : null;
            if (sourceMimeType == null || sourceMimeType.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Content has no MIME type"));
            }
            
            Map<String, RenditionTransformationHelper.TransformationInfo> transformations = 
                    RenditionTransformationHelper.getAvailableTransformations(sourceMimeType);
            
            List<Map<String, Object>> transformationList = new ArrayList<>();
            for (Map.Entry<String, RenditionTransformationHelper.TransformationInfo> entry : transformations.entrySet()) {
                RenditionTransformationHelper.TransformationInfo info = entry.getValue();
                Map<String, Object> item = new HashMap<>();
                item.put("targetMimeType", info.getTargetMimeType());
                item.put("transformer", info.getTransformerName());
                item.put("method", info.getMethodName());
                item.put("methodArgs", info.getMethodArgs());
                item.put("available", info.isAvailable());
                transformationList.add(item);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("contentGuid", contentGuid);
            response.put("sourceMimeType", sourceMimeType);
            response.put("transformations", transformationList);
            response.put("count", transformationList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get content transformations", "message", e.getMessage()));
        } finally {
            if (session != null) {
                DMSSessionFactory.closeSession(session);
            }
        }
    }
    
    /**
     * Queue a transformation job for a content rendition
     * 
     * POST /api/transformer/queue
     * Body: {
     *   "documentGuid": "Document:123",
     *   "contentGuid": "Content:456",
     *   "targetMimeType": "image/jpeg",
     *   "tagDomain": "rendition",
     *   "tagValue": "preview",
     *   "addAsChild": true
     * }
     */
    @PostMapping("/queue")
    public ResponseEntity<?> queueTransformation(@RequestBody TransformationRequest request) {
        DMSSession session = null;
        try {
            session = (DMSSession) DMSSessionFactory.getFactory().getSession();
            
            // Validate document
            VersionableObject document = (VersionableObject) session.getObjectFromGuid(request.getDocumentGuid());
            if (document == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Document not found", "guid", request.getDocumentGuid()));
            }
            
            // Get content by GUID or find by constraint
            Content sourceContent = null;
            if (request.getContentGuid() != null && !request.getContentGuid().isEmpty()) {
                sourceContent = (Content) session.getObjectFromGuid(request.getContentGuid());
            } else if (request.getSourceMimeType() != null && !request.getSourceMimeType().isEmpty()) {
                // Find content by MIME type
                MimeTypeContentConstraint constraint = new MimeTypeContentConstraint(request.getSourceMimeType());
                sourceContent = document.getContentByConstraint(constraint, false);
            }
            
            if (sourceContent == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Source content not found"));
            }
            
            String sourceMimeType = sourceContent.getContentType() != null ? 
                    sourceContent.getContentType().getMimeType() : null;
            String targetMimeType = request.getTargetMimeType();
            
            // Check if transformation is available
            if (!RenditionTransformationHelper.isTransformationAvailable(sourceMimeType, targetMimeType)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Transformation not available",
                                "sourceMimeType", sourceMimeType,
                                "targetMimeType", targetMimeType));
            }
            
            // Create constraint for the edge lookup
            HTPredicate<ConvertionEdge> constraint = new LogicalAndOperator<>(
                    new FromConstraint(sourceMimeType),
                    new ToConstraint(targetMimeType)
            );
            
            // Create job parameters
            String targetFileName = request.getTargetFileName();
            if (targetFileName == null || targetFileName.isEmpty()) {
                targetFileName = "converted_" + System.currentTimeMillis();
            }
            
            String tagDomain = request.getTagDomain() != null ? request.getTagDomain() : "rendition";
            String tagValue = request.getTagValue() != null ? request.getTagValue() : "converted";
            boolean addAsChild = request.isAddAsChild();
            
            MimeTypeContentConstraint contentConstraint = new MimeTypeContentConstraint(sourceMimeType);
            
            TransformJobParameters params = TransformerUtil.createJobParameters(
                    null, // notification guid
                    null, // notification state
                    contentConstraint,
                    request.getDocumentGuid(),
                    constraint,
                    tagDomain,
                    tagValue,
                    targetFileName,
                    addAsChild
            );
            
            if (params == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to create transformation job parameters"));
            }
            
            // Queue the job
            PersistedSerializedObject pso = TransformerUtil.queueTransformJob(params, session, true);
            
            if (pso == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to queue transformation job"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobId", params.getJobId());
            response.put("jobGuid", pso.getGuid());
            response.put("documentGuid", request.getDocumentGuid());
            response.put("sourceMimeType", sourceMimeType);
            response.put("targetMimeType", targetMimeType);
            response.put("status", "queued");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to queue transformation", "message", e.getMessage()));
        } finally {
            if (session != null) {
                DMSSessionFactory.closeSession(session);
            }
        }
    }
    
    /**
     * Request object for queuing a transformation
     */
    public static class TransformationRequest {
        private String documentGuid;
        private String contentGuid;
        private String sourceMimeType;
        private String targetMimeType;
        private String tagDomain;
        private String tagValue;
        private String targetFileName;
        private boolean addAsChild = true;
        
        public String getDocumentGuid() { return documentGuid; }
        public void setDocumentGuid(String documentGuid) { this.documentGuid = documentGuid; }
        
        public String getContentGuid() { return contentGuid; }
        public void setContentGuid(String contentGuid) { this.contentGuid = contentGuid; }
        
        public String getSourceMimeType() { return sourceMimeType; }
        public void setSourceMimeType(String sourceMimeType) { this.sourceMimeType = sourceMimeType; }
        
        public String getTargetMimeType() { return targetMimeType; }
        public void setTargetMimeType(String targetMimeType) { this.targetMimeType = targetMimeType; }
        
        public String getTagDomain() { return tagDomain; }
        public void setTagDomain(String tagDomain) { this.tagDomain = tagDomain; }
        
        public String getTagValue() { return tagValue; }
        public void setTagValue(String tagValue) { this.tagValue = tagValue; }
        
        public String getTargetFileName() { return targetFileName; }
        public void setTargetFileName(String targetFileName) { this.targetFileName = targetFileName; }
        
        public boolean isAddAsChild() { return addAsChild; }
        public void setAddAsChild(boolean addAsChild) { this.addAsChild = addAsChild; }
    }
}
