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
    
    private final com.hitorro.spring.autoconfigure.dms.DMSSessionFactory sessionFactory;
    
    public RenditionTransformationController(com.hitorro.spring.autoconfigure.dms.DMSSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
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
    public ResponseEntity<?> getContentAvailableTransformations(@PathVariable("contentGuid") String contentGuid) {
        DMSSession session = null;
        try {
            System.out.println("DEBUG: Looking up content with GUID: " + contentGuid);
            session = (DMSSession) sessionFactory.getSession();
            System.out.println("DEBUG: Session obtained: " + session);
            
            // Debug type system
            try {
                com.hitorro.util.typesystem.TypeManager tm = com.hitorro.util.typesystem.TypeManager.getTypeManager();
                System.out.println("DEBUG: TypeManager: " + tm);
                com.hitorro.util.typesystem.Type contentType = tm.getTypeForClass(Content.class);
                System.out.println("DEBUG: Content type: " + contentType);
                if (contentType != null) {
                    System.out.println("DEBUG: GuidAccessor for Content: " + contentType.getGuidAccessor());
                }
            } catch (Exception e) {
                System.err.println("DEBUG: Error checking type system: " + e.getMessage());
                e.printStackTrace();
            }
            
            Content content = (Content) session.getObjectFromGuid(contentGuid);
            System.out.println("DEBUG: Content lookup result: " + (content != null ? "found" : "null"));
            if (content == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Content not found", "guid", contentGuid));
            }
            
            String sourceMimeType = content.getContentType() != null ? 
                    content.getContentType().getMimeType() : null;
            System.out.println("DEBUG: Source MIME type: " + sourceMimeType);
            if (sourceMimeType == null || sourceMimeType.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Content has no MIME type"));
            }
            
            Map<String, RenditionTransformationHelper.TransformationInfo> transformations = 
                    RenditionTransformationHelper.getAvailableTransformations(sourceMimeType);
            System.out.println("DEBUG: Found " + transformations.size() + " transformations");
            for (Map.Entry<String, RenditionTransformationHelper.TransformationInfo> entry : transformations.entrySet()) {
                RenditionTransformationHelper.TransformationInfo info = entry.getValue();
                System.out.println("DEBUG:   " + info.getSourceMimeType() + " → " + info.getTargetMimeType() + 
                                 " via " + info.getMethodName() + " (available: " + info.isAvailable() + ")");
            }
            
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
            System.err.println("ERROR in getContentAvailableTransformations: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get content transformations");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("contentGuid", contentGuid);
            errorResponse.put("exceptionType", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        } finally {
            if (session != null) {
                session.close();
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
            session = (DMSSession) sessionFactory.getSession();
            
            // Validate document
            String documentGuid = request.getDocumentGuid();
            if (documentGuid == null || documentGuid.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Document GUID is required"));
            }
            
            VersionableObject document = (VersionableObject) session.getObjectFromGuid(documentGuid);
            if (document == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Document not found", "guid", documentGuid));
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
            // Note: Skip availability check for template-based transformations
            // because the source content is parameters, not the thing being transformed
            boolean isTemplateTransformation = request.getTemplateGuid() != null && !request.getTemplateGuid().isEmpty();
            
            if (!isTemplateTransformation && 
                !RenditionTransformationHelper.isTransformationAvailable(sourceMimeType, targetMimeType)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Transformation not available",
                                "sourceMimeType", sourceMimeType,
                                "targetMimeType", targetMimeType));
            }
            
            // Create job parameters
            String targetFileName = request.getTargetFileName();
            if (targetFileName == null || targetFileName.isEmpty()) {
                targetFileName = "converted_" + System.currentTimeMillis();
            }
            
            String tagDomain = request.getTagDomain() != null ? request.getTagDomain() : "rendition";
            String tagValue = request.getTagValue() != null ? request.getTagValue() : "converted";
            boolean addAsChild = request.isAddAsChild();
            
            MimeTypeContentConstraint contentConstraint = new MimeTypeContentConstraint(sourceMimeType);
            
            TransformJobParameters params;
            
            // Handle template-based transformations differently
            if (isTemplateTransformation) {
                // For template transformations, manually create job parameters
                // because there's no ConvertionEdge for this type of transformation
                params = new TransformJobParameters();
                params.provisionId();
                params.setContentConstraint(contentConstraint);
                params.setJobGuid(request.getDocumentGuid());
                
                // Get template content to build parameters with template path
                Content templateContent = (Content) session.getObjectFromGuid(request.getTemplateGuid());
                if (templateContent == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Template content not found", "guid", request.getTemplateGuid()));
                }
                
                // Get template file path
                com.hitorro.util.basefile.fs.BaseFile templateFile = templateContent.getContentFile();
                String templatePath = ((com.hitorro.util.basefile.fs.file.FileFile) templateFile).getJavaFile().getAbsolutePath();
                
                // Build parameters JSON with template path and flatten=false to keep fields visible
                String templateParams = String.format("{\"_template_path\":\"%s\",\"flatten\":false}", 
                        templatePath.replace("\\", "\\\\"));
                params.setTransformerMethodArgs(templateParams);
                
                // Set transformer and method for PDF template filling
                params.setTranformer("PDFTemplateTransformer");
                params.setTransformerMethod("pdf_template");
                
                // Create content setter for output
                com.hitorro.base.objects.ContentSetter setter = new com.hitorro.base.objects.ContentSetter();
                setter.setSysGuid(request.getDocumentGuid());
                setter.setMimeType(targetMimeType);
                setter.setFileName(targetFileName + ".pdf");
                setter.setTagDomain(tagDomain);
                setter.setTagValue(tagValue);
                
                params.setContentSetter(setter);
                params.setAddContentAsChildOfContent(addAsChild);
                
            } else {
                // For standard transformations, use edge-based lookup
                HTPredicate<ConvertionEdge> constraint = new LogicalAndOperator<>(
                        new FromConstraint(sourceMimeType),
                        new ToConstraint(targetMimeType)
                );
                
                params = TransformerUtil.createJobParameters(
                        null, // notification guid
                        null, // notification state
                        contentConstraint,
                        request.getDocumentGuid(),
                        constraint,
                        tagDomain,
                        tagValue,
                        targetFileName,
                        addAsChild,
                        null, // no template for standard transformations
                        request.getParameters()
                );
                
                if (params == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to create transformation job parameters"));
                }
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
            
            // Check if immediate execution is requested (useful for testing/synchronous workflows)
            if (request.isExecuteImmediately()) {
                try {
                    // Execute the job synchronously
                    TransformJob job = new TransformJob();
                    job.setSession(session);
                    com.hitorro.util.job.JobExecutionResult execResult = job.doAction(params);
                    
                    if (execResult != null) {
                        session.commit();
                        response.put("status", "completed");
                        response.put("executed", true);
                    } else {
                        response.put("status", "failed");
                        response.put("error", "Job execution returned null");
                    }
                } catch (Exception e) {
                    response.put("status", "failed");
                    response.put("error", "Job execution failed: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                response.put("status", "queued");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to queue transformation", "message", message));
        } finally {
            if (session != null) {
                session.close();
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
        private String templateGuid;
        private String parameters;
        private boolean executeImmediately = false;
        
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
        
        public String getTemplateGuid() { return templateGuid; }
        public void setTemplateGuid(String templateGuid) { this.templateGuid = templateGuid; }

        public String getParameters() { return parameters; }
        public void setParameters(String parameters) { this.parameters = parameters; }
        
        public boolean isExecuteImmediately() { return executeImmediately; }
        public void setExecuteImmediately(boolean executeImmediately) { this.executeImmediately = executeImmediately; }
    }
}
