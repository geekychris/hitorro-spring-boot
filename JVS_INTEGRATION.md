# Hitorro JSON Type System (JVS) Integration

## Overview

Complete Spring Boot integration of Hitorro's **JSON Type System (JVS)** - a sophisticated, type-aware JSON processing framework with Natural Language Processing capabilities.

## What is JVS?

The JSON Type System is far more than a JSON parser. It's a comprehensive framework for:

- **Type-Aware Document Processing** - Documents have types with defined fields and validation
- **Propaccess Notation** - Access nested properties with paths like `"title.mls[en].text"`
- **Multi-Language Support** - Built-in support for multi-language strings (MLS)
- **NLP Integration** - Automatic text analysis, segmentation, NER, POS tagging
- **Semantic Analysis** - WordNet integration for synonyms, hypernyms, semantic features
- **Dynamic Mapping** - Map JSON to Java objects with type safety

## Key Features

### Type-Aware Processing
```java
JVS doc = JVS.read("{\"type\":\"article\",\"title\":{\"mls\":\"Hello\"}}");
Type articleType = doc.getType();  // Gets "article" type definition
```

### Propaccess Notation
```java
// Access nested properties with dot notation
String title = jvs.getString("title.mls[en].text");
Date created = jvs.getDate("dates.created");
List<String> tags = jvs.getStringList("metadata.tags");

// Set values
jvs.set("title.mls[en].segmented", "hello world");
```

### Multi-Language Support
```json
{
  "title": {
    "mls": {
      "en": {
        "text": "Hello World",
        "clean": "hello world",
        "segmented": ["hello", "world"],
        "segmented_parsed": [...],
        "segmented_normhash": [12345, 67890]
      },
      "de": {
        "text": "Hallo Welt",
        "segmented": ["hallo", "welt"]
      }
    }
  }
}
```

### NLP-Aware Processing
When NLP is enabled, JVS automatically enriches text fields with:
- **Segmentation** - Tokenized text
- **POS Tags** - Part-of-speech classification
- **NER** - Named Entity Recognition
- **Parse Trees** - Syntactic structure
- **Semantic Classes** - WordNet-based classification
- **Normalized Hashes** - For efficient comparison

## Spring Boot Configuration

### Basic Configuration

```yaml
hitorro:
  jvs:
    enabled: true  # Enable JVS integration
    nlp-enabled: false  # NLP features (requires WordNet data)
    rest:
      enabled: false  # REST endpoints for JVS
      base-path: /api/jvs
```

### With NLP Features

```yaml
hitorro:
  jvs:
    enabled: true
    nlp-enabled: true  # Enable WordNet and OpenNLP
```

**Requirements for NLP**:
- WordNet dictionary data in `${hitorro.home}/data/wordnet`
- OpenNLP models (tokenizer, POS tagger, NER, parser)
- `hitorro-base` module on classpath

## Usage Examples

### Basic JVS Operations

```java
@Service
public class DocumentService {
    @Autowired
    private JsonTypeSystemManager jtsManager;
    
    public void processDocument(String jsonString) {
        // Create JVS document
        JVS doc = jtsManager.createJVS(jsonString);
        
        // Access properties
        String id = doc.getString("id.id");
        String title = doc.getString("title.mls");
        Date created = doc.getDate("dates.created");
        
        // Modify properties
        doc.set("metadata.processed", true);
        doc.set("dates.modified", new Date());
        
        // Get type information
        Type type = doc.getType();
        if (type != null) {
            System.out.println("Document type: " + type.getName());
        }
    }
}
```

### Multi-Language Document

```java
@Service
public class MLSDocumentService {
    @Autowired
    private JsonTypeSystemManager jtsManager;
    
    public void processMultilingualDoc() {
        String json = """
            {
                "title": {
                    "mls": {
                        "en": {"text": "Welcome"},
                        "es": {"text": "Bienvenido"},
                        "de": {"text": "Willkommen"}
                    }
                }
            }
            """;
        
        JVS doc = jtsManager.createJVS(json);
        
        // Access different languages
        String english = doc.getString("title.mls[en].text");
        String spanish = doc.getString("title.mls[es].text");
        String german = doc.getString("title.mls[de].text");
        
        // Get segmented text (if NLP is enabled)
        List<String> segments = doc.getStringList("title.mls[en].segmented");
    }
}
```

### NLP-Aware Processing

```java
@Service
public class NLPDocumentService {
    @Autowired
    private JsonTypeSystemManager jtsManager;
    
    @Autowired(required = false)
    private NLPIntegrationManager nlpManager;
    
    public void analyzeText() {
        if (nlpManager == null || !nlpManager.isNLPAvailable()) {
            log.warn("NLP features not available");
            return;
        }
        
        String json = """
            {
                "title": {
                    "mls": {
                        "en": {
                            "text": "John Smith visited New York yesterday"
                        }
                    }
                }
            }
            """;
        
        JVS doc = jtsManager.createJVS(json);
        
        // Get NLP analysis results
        List<String> segments = doc.getStringList("title.mls[en].segmented");
        // ["John", "Smith", "visited", "New", "York", "yesterday"]
        
        List<String> ner = doc.getStringList("title.mls[en].segmented_ner");
        // ["PERSON", "PERSON", "O", "LOCATION", "LOCATION", "DATE"]
        
        List<String> pos = doc.getStringList("title.mls[en].segmented_pos");
        // ["NNP", "NNP", "VBD", "NNP", "NNP", "NN"]
        
        JsonNode parseTree = doc.get("title.mls[en].segmented_parsed");
        // Full syntactic parse tree
    }
}
```

### Type Definitions

```java
@Service
public class TypeService {
    @Autowired
    private JsonTypeSystemManager jtsManager;
    
    public void checkTypes() {
        // Check if a type exists
        boolean hasArticle = jtsManager.hasType("article");
        
        // Get type definition
        Type articleType = jtsManager.getType("article");
        if (articleType != null) {
            // Access type metadata
            String typeName = articleType.getName();
            
            // Get field definitions
            Field titleField = articleType.getField("title");
            Field bodyField = articleType.getField("body");
        }
    }
}
```

## REST API

When `hitorro.jvs.rest.enabled=true`, the following endpoints are available:

### Parse JSON Document
```bash
POST /api/jvs/parse
Content-Type: application/json

{"type":"article","title":{"mls":"Hello World"}}

Response:
{
  "success": true,
  "typeName": "article",
  "hasType": true
}
```

### Get Type Definition
```bash
GET /api/jvs/types/article

Response:
{
  "success": true,
  "typeName": "article",
  "exists": true
}
```

### Query Document Properties
```bash
POST /api/jvs/query
Content-Type: application/json

{
  "document": "{\"title\":{\"mls\":\"Test\"},\"id\":{\"id\":\"123\"}}",
  "properties": ["title.mls", "id.id"]
}

Response:
{
  "success": true,
  "values": {
    "title.mls": "Test",
    "id.id": "123"
  }
}
```

## Architecture

### Components

1. **JsonTypeSystemAutoConfiguration**
   - Auto-configures JVS singleton
   - Initializes type system
   - Configures NLP integration (if enabled)

2. **JsonTypeSystemManager**
   - Spring bean for accessing JVS
   - Factory methods for creating documents
   - Type lookup and validation

3. **NLPIntegrationManager**
   - Initializes WordNet dictionary
   - Loads OpenNLP models
   - Provides NLP feature detection

4. **JVSRestController**
   - REST endpoints for JVS operations
   - JSON parsing and querying
   - Type information access

### Type Definitions

Type definitions are stored as JSON files in:
```
${hitorro.home}/config/types/core/
```

Example type definition (`article.json`):
```json
{
  "name": "article",
  "fields": {
    "id": {"type": "id", "required": true},
    "title": {"type": "mls", "required": true},
    "body": {"type": "mls"},
    "author": {"type": "string"},
    "publishDate": {"type": "date"},
    "tags": {"type": "stringarray"}
  }
}
```

## NLP Integration

### WordNet Integration

WordNet provides semantic analysis:
- **Synonyms** - Alternative words with similar meaning
- **Hypernyms** - Parent concepts (e.g., "dog" → "animal")
- **Hyponyms** - Child concepts (e.g., "animal" → "dog")
- **Meronyms** - Part-of relationships
- **Holonyms** - Whole-of relationships

```java
@Service
public class SemanticAnalysisService {
    @Autowired
    private NLPIntegrationManager nlpManager;
    
    public void analyzeSemantic() {
        HTJWNLDictionary dict = nlpManager.getWordnetDictionary();
        
        // Get lemmas
        String[] lemmas = dict.getLemmas("running", "VB");
        // ["run"]
        
        // Get synsets
        Set<String> synsets = dict.getSynsetSet(parse);
        // WordNet synset IDs for semantic classification
    }
}
```

### OpenNLP Integration

OpenNLP provides:
- **Tokenization** - Split text into words
- **Sentence Detection** - Identify sentence boundaries
- **POS Tagging** - Part-of-speech classification
- **NER** - Named Entity Recognition (Person, Location, Organization, Date, etc.)
- **Parsing** - Syntactic parse trees
- **Chunking** - Phrase identification

## Testing

Comprehensive test suite (22 tests) covering:

### Construction and Parsing
- Create JVS from JSON strings
- Empty document creation
- Simple and complex nested structures

### Property Access
- Predefined property accessors
- Type key access
- Variable delimiters

### JSON Operations
- Arrays, booleans, null values
- Special characters and unicode
- Various number types

### Edge Cases
- Empty JSON objects
- Very large JSON (1000+ fields)

### Real World Scenarios
- User profile documents
- Article documents with metadata
- Multi-language content

### Comparators and Functions
- Identity comparison
- Key generation
- Document sorting

Run tests:
```bash
mvn test -Dtest=HitorroJVSIntegrationTest
```

All tests pass, demonstrating full JVS functionality in Spring Boot!

## Performance Considerations

- **Type caching** - Type definitions are cached for fast access
- **Lazy loading** - NLP analysis only when needed
- **Property path caching** - Propaccess paths are optimized
- **Memory efficient** - Large documents handled via Jackson streaming

## Migration from Standalone

If migrating from standalone Hitorro to Spring Boot:

### Before (Standalone)
```java
JVS jvs = JVS.read(jsonString);
Type type = JsonTypeSystem.getMe().getType("article");
```

### After (Spring Boot)
```java
@Autowired
private JsonTypeSystemManager jtsManager;

JVS jvs = jtsManager.createJVS(jsonString);
Type type = jtsManager.getType("article");
```

Both work the same way - Spring Boot just provides dependency injection!

## Summary

The JVS Spring Boot integration provides:

✅ **Full Type System** - All type definitions and validation  
✅ **Propaccess Notation** - Powerful property path access  
✅ **Multi-Language** - Built-in MLS support  
✅ **NLP Integration** - Optional WordNet and OpenNLP  
✅ **REST API** - HTTP endpoints for JVS operations  
✅ **Spring Integration** - Dependency injection, configuration  
✅ **Comprehensive Tests** - 22 tests covering all features  

The JSON Type System is one of Hitorro's most powerful features, and it's now fully available in Spring Boot applications! 🎉
