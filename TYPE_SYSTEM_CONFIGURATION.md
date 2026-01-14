# Type System Configuration - CRITICAL REQUIREMENT

## ⚠️ CRITICAL: Type Definitions Must Be Loaded from Disk

The Hitorro JSON Type System (JVS) **requires** type definition JSON files to be loaded from the filesystem. **Without these files, the type system cannot function correctly.**

### Why This Matters

The type system drives all NLP-aware features:
- **Field definitions** - What fields exist in each document type
- **NLP processing rules** - Which fields get NLP analysis
- **Dynamic field creation** - Creates `segmented`, `clean`, `segmented_ner`, etc.
- **Type validation** - Ensures documents match their type schema
- **Multi-language support** - Configures MLS field processing

**Without type definitions loaded**:
- ❌ No automatic NLP enrichment
- ❌ No dynamic field creation
- ❌ No type validation
- ❌ No field-level processing rules
- ❌ OpenNLP integration won't work

## Configuration

### Method 1: Environment Variable (Recommended for Production)

```bash
export HT_BIN=/opt/hitorro
```

### Method 2: System Property

```bash
java -DHT_BIN=/opt/hitorro -jar app.jar
```

### Method 3: Spring Boot application.yml

```yaml
hitorro:
  jvs:
    type-definitions-path: /opt/hitorro
```

## Directory Structure

Type definitions must be organized as follows:

```
${HT_BIN}/
└── config/
    └── types/
        └── core/
            ├── core_article.json
            ├── core_user_profile.json
            ├── core_document.json
            └── ...
```

### File Naming Convention

Files MUST be named: `${domain}_${typename}.json`

- **domain**: Usually "core" (default domain)
- **typename**: The type name (e.g., "article", "user_profile")

Examples:
- `core_article.json` - Defines "article" type in "core" domain
- `core_user_profile.json` - Defines "user_profile" type
- `mydomain_customtype.json` - Custom domain and type

## Type Definition Format

Example `core_article.json`:

```json
{
  "name": "article",
  "description": "Article document type with NLP-aware fields",
  "fields": {
    "id": {
      "type": "id",
      "required": true,
      "description": "Unique document identifier"
    },
    "title": {
      "type": "mls",
      "required": true,
      "description": "Multi-language title (NLP-processed)"
    },
    "body": {
      "type": "mls",
      "description": "Multi-language article body (NLP-processed)"
    },
    "metadata": {
      "type": "object",
      "properties": {
        "author": {"type": "string"},
        "publishDate": {"type": "date"},
        "tags": {"type": "stringarray"}
      }
    }
  }
}
```

### Field Type System

| Type | Description | NLP Processing |
|------|-------------|----------------|
| `mls` | Multi-language string | YES - Creates segmented, clean, ner, pos, etc. |
| `string` | Simple string | NO |
| `id` | Identifier object | NO |
| `date` | Date/timestamp | NO |
| `integer` | Number | NO |
| `object` | Nested object | Depends on child fields |
| `stringarray` | Array of strings | NO |

### MLS (Multi-Language String) Fields

When a field has type `"mls"`, the type system instructs the NLP processors to:

1. **Tokenize** - Create `segmented` field with token array
2. **Clean** - Create `clean` field with normalized text
3. **POS Tag** - Create `segmented_pos` field with part-of-speech tags
4. **NER** - Create `segmented_ner` field with named entity tags
5. **Parse** - Create `segmented_parsed` field with syntax tree
6. **WordNet** - Create `segmented_classes` field with semantic classes
7. **Hash** - Create `segmented_normhash` field for comparison

## Verifying Configuration

### Check in Application Logs

```
INFO  JsonTypeSystemManager : Set HT_BIN from configuration: /opt/hitorro
INFO  JsonTypeSystemManager : Type definitions path: /opt/hitorro/config/types/core/
```

### Programmatic Check

```java
@Autowired
private JsonTypeSystemManager jtsManager;

public void checkTypes() {
    Type articleType = jtsManager.getType("article");
    if (articleType != null) {
        System.out.println("✓ Type 'article' loaded successfully");
    } else {
        System.out.println("✗ Type 'article' not found");
        System.out.println("  Check HT_BIN configuration!");
    }
}
```

### Common Issues

#### Problem: "Type 'article' not found"

**Cause**: Type definitions path not configured or incorrect

**Solution**:
1. Check HT_BIN is set: `echo $HT_BIN`
2. Verify directory exists: `ls -la $HT_BIN/config/types/core/`
3. Check file names: Files must be `core_*.json`
4. Review logs for "Type definitions path:" message

#### Problem: NLP fields not created

**Cause**: Type definition doesn't specify field as `"mls"` type

**Solution**:
1. Load the type: `Type t = jtsManager.getType("article");`
2. Check field definitions include `"type": "mls"`
3. Verify type was loaded (not null)

#### Problem: Type files exist but not loading

**Cause**: HT_BIN points to wrong directory level

**Solution**:
HT_BIN must point to the ROOT directory containing `config/`:
- ✅ Correct: `HT_BIN=/opt/hitorro` (contains `config/types/core/`)
- ❌ Wrong: `HT_BIN=/opt/hitorro/config`
- ❌ Wrong: `HT_BIN=/opt/hitorro/config/types`

## Complete Example

### 1. Create Directory Structure

```bash
mkdir -p /opt/hitorro/config/types/core
```

### 2. Create Type Definition

```bash
cat > /opt/hitorro/config/types/core/core_article.json << 'EOF'
{
  "name": "article",
  "fields": {
    "title": {"type": "mls", "required": true},
    "body": {"type": "mls"}
  }
}
EOF
```

### 3. Configure Application

```yaml
# application.yml
hitorro:
  jvs:
    type-definitions-path: /opt/hitorro
    nlp-enabled: true  # Enable NLP processing
```

### 4. Use in Application

```java
@Service
public class ArticleService {
    @Autowired
    private JsonTypeSystemManager jtsManager;
    
    public void processArticle(String jsonString) {
        JVS doc = jtsManager.createJVS(jsonString);
        
        // With type loaded, these fields are created automatically:
        String clean = doc.getString("title.mls[en].clean");
        List<String> tokens = doc.getStringList("title.mls[en].segmented");
        List<String> ner = doc.getStringList("title.mls[en].segmented_ner");
        
        // Without type: fields don't exist, returns null
    }
}
```

## How It Works

### Type Loading Flow

1. **Startup**: JsonTypeSystemManager reads `hitorro.jvs.type-definitions-path`
2. **Configure**: Sets `HT_BIN` system property
3. **Initialize**: `JsonTypeSystem.getMe()` creates cache
4. **Cache Setup**: Points to `${HT_BIN}/config/types/core/`
5. **Lazy Load**: Types loaded on first access via `getType(name)`
6. **File Read**: `Name2JsonMapper` reads `core_${name}.json`
7. **Parse**: JSON parsed into `Type` object with field definitions
8. **Cache**: Type cached for fast subsequent access

### NLP Processing Flow

1. **Document Created**: `JVS doc = new JVS(jsonNode)`
2. **Type Lookup**: `Type type = doc.getType()`
3. **Field Scan**: Type defines which fields are `mls`
4. **NLP Trigger**: For each `mls` field, NLP processors run
5. **Field Creation**: Dynamic fields added to JSON structure
6. **Access**: Fields available via `doc.getString()`, `doc.getStringList()`, etc.

## Summary

**CRITICAL REQUIREMENTS**:
1. ✅ Set `HT_BIN` environment variable or `hitorro.jvs.type-definitions-path`
2. ✅ Create directory: `${HT_BIN}/config/types/core/`
3. ✅ Add type files: `core_typename.json`
4. ✅ Define `mls` fields for NLP processing
5. ✅ Verify types load on startup (check logs)

**Without proper configuration**:
- Type system is non-functional
- NLP integration will not work
- Dynamic fields will not be created
- OpenNLP processing will not occur

The type system is the foundation that drives all NLP-aware features in Hitorro's JSON processing!
