# Hitorro Property Chain Integration

## Overview

The Spring Boot integration now uses **Hitorro's standard property loading mechanism** from `CommandLine.reloadJVSProps()`. This ensures that properties are loaded from all the standard Hitorro configuration sources, not just system properties.

## Property Loading Chain

Properties are loaded in this order (later sources override earlier ones):

### 1. System Args
- `HT_BIN`, `HT_HOME`, `ht_data`
- Node ID, hostname, etc.
- From `Env.getSystemArgs()`

### 2. Bin Directory Config
- Location: `${HT_BIN}/config/`
- Reads: `*.properties`, `*.json`
- Server-type specific: `${HT_BIN}/config/<servertype>/`
- Debug mode: `${HT_BIN}/config/debug/`

### 3. Home Directory Config  
- Location: `${HT_HOME}/config/`
- Reads: `*.properties`, `*.json`
- Server-type specific: `${HT_HOME}/config/<servertype>/`
- Debug mode: `${HT_HOME}/config/debug/`
- Participates in file change detection

### 4. LoadProps Files
- Specified via `loadprops` property
- Can reference additional property files
- Useful for environment-specific configs

### 5. Variable Resolution
- Resolves `${variable}` references
- Example: `${ht_data}/file.csv` → `/Users/chris/hitorro/data/file.csv`

## Implementation

### JsonTypeSystemManager

The `JsonTypeSystemManager.initializeJVSProperties()` method now replicates `CommandLine.reloadJVSProps()`:

```java
private void initializeJVSProperties() {
    // Create property reader chain (same as CommandLine)
    JVSPropertiesReader[] propLoaders = {
        new JVSSystemArgsPropertyReader(),
        new JVSDirectoryReadingPropertiesReader(JVSDirectoryType.Bin, false),
        new JVSDirectoryReadingPropertiesReader(JVSDirectoryType.Home, true),
        new JVSLoadPropsPropertyReader(true)
    };
    
    // Load from each source
    JVS props = new JVS();
    for (JVSPropertiesReader reader : propLoaders) {
        reader.getProperties(props, new JVS());
    }
    
    // Resolve variables
    props.resolveVariables(props);
    
    // Set as defaults
    JVSProperties.setDefaultProperties(props, false);
}
```

## Benefits

### 1. Standard Configuration Locations

Applications can now use Hitorro's standard config directories:

```
${HT_BIN}/config/
├── app.properties          # General application properties
├── stores.properties       # Store configuration
├── nlp.properties         # NLP settings
└── server.properties      # Server-specific settings

${HT_HOME}/config/
├── local.properties       # Local overrides
└── environment.properties # Environment-specific
```

### 2. Automatic CSV Path Resolution

Store CSV files can be referenced with variables:

**Before** (hardcoded):
```yaml
dms:
  db-init:
    data-sets:
      - csv-file: "file:///Users/chris/hitorro/config/data/stores.csv"
```

**After** (variable-based):
```yaml
dms:
  db-init:
    data-sets:
      - csv-file: "${HT_BIN}/config/data/stores.csv"
```

The property chain automatically resolves `${HT_BIN}` from system properties.

### 3. Environment-Specific Configuration

Use different configs per environment:

**Development** (`${HT_BIN}/config/dev/stores.properties`):
```properties
stores.default.path=/Users/chris/hthome/stores/default
```

**Production** (`${HT_BIN}/config/prod/stores.properties`):
```properties
stores.default.path=/var/hitorro/stores/default
```

### 4. Property Cascading

Properties cascade through the chain:

1. System: `HT_BIN=/Users/chris/hitorro`
2. Bin config: `${HT_BIN}/config/app.properties` sets `stores.enabled=true`
3. Home config: `${HT_HOME}/config/local.properties` sets `stores.enabled=false`
4. **Final result**: `stores.enabled=false` (Home overrides Bin)

## Usage Examples

### Example 1: Store Configuration via Properties File

**File**: `${HT_BIN}/config/stores.properties`

```properties
# Store paths
stores.default.root=/Users/chris/hthome/stores/default
stores.archive.root=/Users/chris/hthome/stores/archive

# Store CSV location
stores.csv.path=${HT_BIN}/config/data/stores.csv
```

**Spring Config** (`application.yml`):

```yaml
hitorro:
  ht-bin: /Users/chris/hitorro
  ht-home: /Users/chris/hthome
  
dms:
  db-init:
    enabled: true
    data-sets:
      - name: "stores"
        csv-file: "${stores.csv.path}"  # Resolved from stores.properties
        consumer: "com.hitorro.basedms.csvconsumers.StoreCSVConsumer"
```

### Example 2: Environment-Specific NLP Configuration

**File**: `${HT_BIN}/config/nlp.properties`

```properties
# NLP configuration
nlp.wordnet.path=${HT_HOME}/data/wordnet
nlp.opennlp.models=${HT_HOME}/data/opennlp
nlp.enabled=true
```

Access in code:

```java
@Value("${nlp.enabled:false}")
private boolean nlpEnabled;

@Value("${nlp.wordnet.path}")
private String wordnetPath;
```

Variables are resolved automatically!

### Example 3: Dynamic Property Loading

**File**: `${HT_BIN}/config/app.properties`

```properties
# Load additional config
loadprops=${HT_HOME}/config/custom-overrides.properties

# Database settings  
db.pool.size=20
db.timeout=30000
```

The `loadprops` property causes the chain to load additional files.

## FileProperty Resolution

The property chain is critical for `FileProperty` resolution in Hitorro:

**Type Definition** (`core_mlselem.json`):

```json
{
  "name": "iso639",
  "file": "${ht_data}/iso639.psv"
}
```

**Resolution**:
1. Property chain loads: `ht_data=/Users/chris/hitorro/data`
2. FileProperty resolves: `${ht_data}/iso639.psv` → `/Users/chris/hitorro/data/iso639.psv`
3. File is loaded correctly

Without the property chain, `${ht_data}` wouldn't resolve!

## Comparison with CommandLine

### Traditional Hitorro App (CommandLine)

```java
public class MyApp {
    public static void main(String[] args) {
        CommandLine cmdLine = new CommandLine();
        cmdLine.mainAux(args, null);
        // Properties loaded via reloadJVSProps()
    }
}
```

### Spring Boot App (JsonTypeSystemManager)

```java
@SpringBootApplication
public class MySpringApp {
    public static void main(String[] args) {
        SpringApplication.run(MySpringApp.class, args);
        // Properties loaded via initializeJVSProperties()
        // Uses SAME property chain as CommandLine!
    }
}
```

**Result**: Both use identical property loading mechanism.

## Configuration Files Discovery

The property readers automatically discover configuration files:

### Bin Directory
```
${HT_BIN}/config/
├── *.properties         # All property files loaded
├── *.json              # All JSON property files loaded
├── <servertype>/       # Server-specific (if servertype set)
│   ├── *.properties
│   └── *.json
└── debug/              # Debug mode only (if debug enabled)
    ├── *.properties
    └── *.json
```

### Home Directory
```
${HT_HOME}/config/
├── *.properties         # Local overrides
├── *.json              # Local overrides
├── <servertype>/       # Server-specific overrides
└── debug/              # Debug overrides
```

## Logging

The integration logs property loading:

```
2026-01-14 14:00:00.123  INFO : Initializing JVSProperties using Hitorro property chain...
2026-01-14 14:00:00.234  INFO : ✓ JVSProperties initialized from property chain
2026-01-14 14:00:00.345  DEBUG:   HT_BIN: /Users/chris/hitorro
2026-01-14 14:00:00.456  DEBUG:   HT_HOME: /Users/chris/hthome
2026-01-14 14:00:00.567  DEBUG:   ht_data: /Users/chris/hitorro/data
```

## Error Handling

If the property chain fails, falls back to system args only:

```
2026-01-14 14:00:00.123  ERROR: Failed to initialize JVSProperties from property chain
2026-01-14 14:00:00.234  WARN : Falling back to system args only
2026-01-14 14:00:00.345  INFO : ✓ JVSProperties initialized with system args (fallback mode)
```

Application continues to run with basic configuration.

## Migration Guide

### Before (Manual Configuration)

```yaml
hitorro:
  jvs:
    type-definitions-path: /Users/chris/hitorro
  
dms:
  db-init:
    data-sets:
      - csv-file: "file:///Users/chris/hitorro/config/data/stores.csv"
```

### After (Property Chain)

```yaml
hitorro:
  ht-bin: /Users/chris/hitorro
  ht-home: /Users/chris/hthome

dms:
  db-init:
    data-sets:
      - csv-file: "${HT_BIN}/config/data/stores.csv"
```

**Benefits**:
- Portable across environments
- Inherits all properties from config directories
- Follows Hitorro conventions
- Compatible with non-Spring Hitorro apps

## Best Practices

### 1. Use Configuration Directories

Put shared config in `${HT_BIN}/config/`:
```properties
# ${HT_BIN}/config/app.properties
app.name=MyApp
app.version=1.0.0
```

Put local overrides in `${HT_HOME}/config/`:
```properties
# ${HT_HOME}/config/local.properties
app.debug=true
```

### 2. Use Variable References

Always use variables for paths:
```properties
# Good
stores.path=${HT_HOME}/stores/default
nlp.models=${ht_data}/opennlp

# Bad (hardcoded)
stores.path=/Users/chris/hthome/stores/default
```

### 3. Organize by Concern

```
${HT_BIN}/config/
├── app.properties       # Application settings
├── stores.properties    # Store configuration
├── nlp.properties      # NLP configuration
└── security.properties # Security settings
```

### 4. Use Environment Subdirectories

```
${HT_BIN}/config/
├── app.properties              # Base config
├── dev/
│   └── app.properties         # Dev overrides
└── prod/
    └── app.properties         # Prod overrides
```

Set server type: `-Dservertype=prod`

## Technical Details

### Property Resolution Order

1. **Load from chain** (system → bin → home → loadprops)
2. **Resolve variables** (`${var}` → actual value)
3. **Set as JVSProperties** (available to FileProperty)
4. **Spring Environment** (can also access via `@Value`)

### Property Sources

The integration creates these property sources:

1. **JVSProperties** - Hitorro's property system
2. **Spring Environment** - Spring's property system
3. Both reference same underlying HT_BIN/HT_HOME system properties

### Change Detection

Home directory properties support change detection:
- File modification monitoring
- Automatic reload on change
- Used by `ConfigChangeWatcher`

Not yet implemented in Spring Boot integration (future enhancement).

## Future Enhancements

Potential improvements:

1. **Live reload** - Detect config file changes and reload
2. **Property validation** - Validate required properties exist
3. **Property documentation** - Generate docs from properties
4. **Spring Cloud Config** - Integration with Spring Cloud Config Server
5. **Encrypted properties** - Support for encrypted values

## Summary

✅ **Uses Hitorro's standard property chain**  
✅ **Loads from HT_BIN/config and HT_HOME/config**  
✅ **Resolves ${variables} automatically**  
✅ **Compatible with non-Spring Hitorro apps**  
✅ **Enables Store CSV with variable paths**  
✅ **Follows Hitorro configuration conventions**  

The Spring Boot integration now properly leverages Hitorro's mature configuration system instead of reinventing property management!
