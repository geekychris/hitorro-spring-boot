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
import com.hitorro.base.objects.Document;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import org.hibernate.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for document and content operations (supporting transformer UI)
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentContentController {
    
    /**
     * Get recent documents
     * 
     * GET /api/documents/recent?limit=20
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentDocuments(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        DMSSession session = null;
        try {
            session = (DMSSession) DMSSessionFactory.getFactory().getSession();
            
            Query<Document> query = session.createQuery(
                    "FROM Document ORDER BY id DESC");
            query.setMaxResults(Math.min(limit, 100)); // Cap at 100
            
            List<Document> documents = query.list();
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Document doc : documents) {
                Map<String, Object> item = new HashMap<>();
                item.put("guid", doc.getGuid());
                item.put("title", doc.getTitle());
                item.put("id", doc.getId());
                item.put("contentCount", doc.getContents() != null ? doc.getContents().size() : 0);
                result.add(item);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to load documents", "message", e.getMessage()));
        } finally {
            if (session != null) {
                DMSSessionFactory.closeSession(session);
            }
        }
    }
    
    /**
     * Search documents by title
     * 
     * GET /api/documents/search?q=test
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(@RequestParam("q") String q) {
        DMSSession session = null;
        try {
            session = (DMSSession) DMSSessionFactory.getFactory().getSession();
            
            // Try as GUID first
            if (q.startsWith("Document:")) {
                VersionableObject obj = (VersionableObject) session.getObjectFromGuid(q);
                if (obj instanceof Document) {
                    Document doc = (Document) obj;
                    List<Map<String, Object>> result = new ArrayList<>();
                    Map<String, Object> item = new HashMap<>();
                    item.put("guid", doc.getGuid());
                    item.put("title", doc.getTitle());
                    item.put("id", doc.getId());
                    item.put("contentCount", doc.getContents() != null ? doc.getContents().size() : 0);
                    result.add(item);
                    return ResponseEntity.ok(result);
                }
            }
            
            // Search by title
            Query<Document> query = session.createQuery(
                    "FROM Document WHERE LOWER(title) LIKE :query ORDER BY id DESC");
            query.setParameter("query", "%" + q.toLowerCase() + "%");
            query.setMaxResults(50);
            
            List<Document> documents = query.list();
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Document doc : documents) {
                Map<String, Object> item = new HashMap<>();
                item.put("guid", doc.getGuid());
                item.put("title", doc.getTitle());
                item.put("id", doc.getId());
                item.put("contentCount", doc.getContents() != null ? doc.getContents().size() : 0);
                result.add(item);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search documents", "message", e.getMessage()));
        } finally {
            if (session != null) {
                DMSSessionFactory.closeSession(session);
            }
        }
    }
    
    /**
     * Get content renditions for a document
     * 
     * GET /api/documents/{guid}/content
     */
    @GetMapping("/{guid}/content")
    public ResponseEntity<?> getDocumentContent(@PathVariable String guid) {
        DMSSession session = null;
        try {
            session = (DMSSession) DMSSessionFactory.getFactory().getSession();
            
            VersionableObject obj = (VersionableObject) session.getObjectFromGuid(guid);
            if (obj == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Document not found", "guid", guid));
            }
            
            if (!(obj instanceof Document)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Object is not a Document", "guid", guid));
            }
            
            Document doc = (Document) obj;
            Set<Content> contents = doc.getContents();
            
            List<Map<String, Object>> result = new ArrayList<>();
            if (contents != null) {
                for (Content content : contents) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("guid", content.getGuid());
                    item.put("fileName", content.getOriginalFileName());
                    String mimeType = content.getContentType() != null ? 
                            content.getContentType().getMimeType() : "unknown";
                    item.put("mimeType", mimeType);
                    item.put("size", content.getContentSize());
                    item.put("storeName", content.getStoreName());
                    
                    // Include info about child renditions
                    if (content.getRenditions() != null && !content.getRenditions().isEmpty()) {
                        item.put("hasRenditions", true);
                        item.put("renditionCount", content.getRenditions().size());
                    } else {
                        item.put("hasRenditions", false);
                        item.put("renditionCount", 0);
                    }
                    
                    result.add(item);
                }
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get document content", "message", e.getMessage()));
        } finally {
            if (session != null) {
                DMSSessionFactory.closeSession(session);
            }
        }
    }
    
    /**
     * Get specific content details
     * 
     * GET /api/documents/content/{contentGuid}
     */
    @GetMapping("/content/{contentGuid}")
    public ResponseEntity<?> getContentDetails(@PathVariable String contentGuid) {
        DMSSession session = null;
        try {
            session = (DMSSession) DMSSessionFactory.getFactory().getSession();
            
            Content content = (Content) session.getObjectFromGuid(contentGuid);
            if (content == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Content not found", "guid", contentGuid));
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("guid", content.getGuid());
            result.put("fileName", content.getOriginalFileName());
            String mimeType = content.getContentType() != null ? 
                    content.getContentType().getMimeType() : "unknown";
            result.put("mimeType", mimeType);
            result.put("size", content.getContentSize());
            result.put("storeName", content.getStoreName());
            
            // Parent rendition info
            if (content.getParentRendition() != null) {
                result.put("parentRendition", content.getParentRendition().getGuid());
            }
            
            // Child renditions
            if (content.getRenditions() != null && !content.getRenditions().isEmpty()) {
                List<Map<String, Object>> renditions = new ArrayList<>();
                for (Content child : content.getRenditions()) {
                    Map<String, Object> childInfo = new HashMap<>();
                    childInfo.put("guid", child.getGuid());
                    childInfo.put("fileName", child.getOriginalFileName());
                    String childMimeType = child.getContentType() != null ? 
                            child.getContentType().getMimeType() : "unknown";
                    childInfo.put("mimeType", childMimeType);
                    renditions.add(childInfo);
                }
                result.put("renditions", renditions);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get content details", "message", e.getMessage()));
        } finally {
            if (session != null) {
                DMSSessionFactory.closeSession(session);
            }
        }
    }
}
