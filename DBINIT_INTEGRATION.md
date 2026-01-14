# Hitorro DBInit Framework Integration

## Overview

Complete Spring Boot integration of Hitorro's DBInit framework for loading initial data from CSV files using `CSVHibernateLoader` and consumer classes.

## Features

### CSV Data Loading
- **CSVDataLoader**: Spring component that wraps Hitorro's CSVHibernateLoader
- **DMSDataInitializer**: Runs on ApplicationReadyEvent to load configured datasets
- **Spring Resource Support**: Use classpath:, file:, http:, etc. for CSV locations
- **Multiple Consumers**: Load stores, users, or custom entities from CSV

### Integration Events Pattern
Mimics Hitorro's native `IntegrationEventsContext` but using Spring Boot configuration:
- Configure datasets in `application.yml`
- Automatic loading on startup
- Fail-fast or continue-on-error modes
- Full logging and error reporting

## Configuration

```yaml
hitorro:
  dms:
    db-init:
      enabled: true  # Enable CSV data loading
      fail-on-error: false  # Continue if dataset fails to load
      
      # List of datasets to load
      data-sets:
        # Load stores from CSV
        - name: "stores"
          csv-file: "classpath:data/stores.csv"
          consumer: "com.hitorro.basedms.csvconsumers.StoreCSVConsumer"
        
        # Load users from CSV  
        - name: "users"
          csv-file: "file:/opt/hitorro/data/users.csv"
          consumer: "com.hitorro.basedms.csvconsumers.UserCSVConsumer"
        
        # Load custom entities
        - name: "products"
          csv-file: "classpath:data/products.csv"
          consumer: "com.myapp.csv.ProductCSVConsumer"
```

## CSV File Format

### Stores CSV
```csv
name,storetype,rootpath,docroot,default,public,allowslinks
default,file,${java.io.tmpdir}/hitorro-store/default,/content/default,true,true,true
blob,blob,,,false,false,false
link,link,,,false,false,false
```

**Columns**:
- **name**: Unique store name
- **storetype**: file, blob, link, or unmanaged
- **rootpath**: File system path for file stores (supports property placeholders)
- **docroot**: Web-accessible path prefix
- **default**: true if this is the default store
- **public**: true if publicly accessible
- **allowslinks**: true if symlinks allowed

### Users CSV
```csv
username,password,email,firstname,lastname,enabled
admin,{bcrypt}$2a$10$...,admin@example.com,Admin,User,true
user1,{bcrypt}$2a$10$...,user1@example.com,John,Doe,true
```

## Available Consumers

### Built-in Consumers (hitorro-basedms)

1. **StoreCSVConsumer** - `com.hitorro.basedms.csvconsumers.StoreCSVConsumer`
   - Loads Store entities
   - Creates file system directories
   - Validates store configuration

2. **UserCSVConsumer** - `com.hitorro.basedms.csvconsumers.UserCSVConsumer`  
   - Loads User entities
   - Handles password encoding
   - Sets up roles and permissions

3. **Custom Consumers** - Extend `CSVHibernateLoaderConsumer<T>`

## Creating Custom Consumers

```java
package com.myapp.csv;

import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.util.core.map.MapUtil;
import com.myapp.entities.Product;

public class ProductCSVConsumer extends CSVHibernateLoaderConsumer<Product> {
    
    private static final String[][] Keys = {{"sku", "sku"}};
    
    @Override
    public Class getPersistingClass() {
        return Product.class;
    }
    
    @Override
    public String[][] getKeyNameMap() {
        return Keys;  // Used to check if entity already exists
    }
    
    @Override
    public boolean add(String[] row, Product product, boolean existsAlready) {
        // Extract values from CSV row
        String sku = MapUtil.getColumnFromColumMap("sku", m_headerMap, row);
        String name = MapUtil.getColumnFromColumMap("name", m_headerMap, row);
        double price = MapUtil.getDoubleColumnFromColumMap("price", m_headerMap, row);
        
        // Set entity properties
        product.setSku(sku);
        product.setName(name);
        product.setPrice(price);
        
        // Save or update
        this.saveOrUpdate(existsAlready, product);
        
        return true;
    }
    
    @Override
    public void start() {
        // Called before processing rows
    }
    
    @Override
    public void done() {
        // Called after all rows processed
    }
}
```

## Property Placeholders

CSV files support property placeholders:
- `${java.io.tmpdir}` - System temp directory
- `${user.home}` - User home directory
- `${HT_STORE_ROOT}` - Custom property (define in application.yml)
- Any Spring property can be referenced

Define custom properties:

```yaml
# application.yml
hitorro:
  store:
    root: /var/lib/hitorro/content
```

Then use in CSV:
```csv
name,storetype,rootpath,docroot,default,public,allowslinks
default,file,${hitorro.store.root}/default,/content/default,true,true,true
```

## Programmatic API

You can also load CSVs programmatically:

```java
@Component
public class DataLoader implements ApplicationRunner {
    @Autowired
    private CSVDataLoader csvDataLoader;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Load stores
        Resource storesCSV = resourceLoader.getResource("classpath:data/stores.csv");
        csvDataLoader.loadStores(storesCSV);
        
        // Load custom data
        Resource productsCSV = resourceLoader.getResource("file:/data/products.csv");
        csvDataLoader.loadCSV(productsCSV, "com.myapp.csv.ProductCSVConsumer");
    }
}
```

## Load Order

Datasets are loaded in the order defined in configuration:

```yaml
data-sets:
  - name: "stores"      # Loaded first
    csv-file: "..."
  - name: "users"       # Loaded second
    csv-file: "..."
  - name: "documents"   # Loaded third
    csv-file: "..."
```

This allows dependencies between datasets (e.g., documents reference stores).

## Error Handling

### Continue on Error (default)
```yaml
hitorro:
  dms:
    db-init:
      fail-on-error: false
```
- Logs errors
- Continues with remaining datasets
- Application starts even if data load fails

### Fail Fast
```yaml
hitorro:
  dms:
    db-init:
      fail-on-error: true
```
- Throws RuntimeException on first error
- Application startup fails
- Good for production where data must be loaded

## Logging

DBInit provides detailed logging:

```
=== Hitorro DMS Data Initialization ===
Loading dataset: stores
  CSV file: classpath:data/stores.csv
  Consumer: com.hitorro.basedms.csvconsumers.StoreCSVConsumer
Loading CSV data from: class path resource [data/stores.csv]
  Using consumer: com.hitorro.basedms.csvconsumers.StoreCSVConsumer
Successfully loaded CSV data from: class path resource [data/stores.csv]
DMS Data Initialization complete: 1 success, 0 failed
===========================================
```

Set log level for more details:

```yaml
logging:
  level:
    com.hitorro.spring.autoconfigure.dbinit: DEBUG
    com.hitorro.basedms.db: DEBUG
```

## Integration with Hitorro Services

When using full Hitorro services (not just DMS auto-config):

```yaml
hitorro:
  services:
    load:
      - com.hitorro.basedms.db.HibernateService
      - com.hitorro.base.objects.BaseDMSService  # Has built-in integration events
  
  dms:
    db-init:
      enabled: false  # BaseDMSService handles this via IntegrationEventsContext
```

BaseDMSService automatically runs these integration events:
- `stores` - Loads stores.csv
- `users` - Loads users.csv  
- `domaininfo` - Loads domain configuration

## Testing

For tests, disable DBInit or use test-specific data:

```yaml
# application-test.yml
spring:
  profiles: test
  
hitorro:
  dms:
    db-init:
      enabled: false  # Or load test data
```

Or load minimal test data:

```yaml
# application-test.yml
hitorro:
  dms:
    db-init:
      enabled: true
      data-sets:
        - name: "test-stores"
          csv-file: "classpath:test-data/stores.csv"
          consumer: "com.hitorro.basedms.csvconsumers.StoreCSVConsumer"
```

## Best Practices

1. **Use classpath for bundled data**: `classpath:data/stores.csv`
2. **Use file: for external config**: `file:/etc/hitorro/data/users.csv`
3. **Load order matters**: Define dependent datasets in order
4. **Idempotent consumers**: Handle both new and existing entities
5. **Validate in consumer**: Check required fields, constraints
6. **Log appropriately**: Info for success, error for failures
7. **Property placeholders**: Use for environment-specific paths
8. **Test with minimal data**: Don't load production data in tests

## Comparison with Native Hitorro

### Native Integration Events
```java
// In BaseDMSService.init()
IntegrationEventsContext.getContext().runEvent("stores");
IntegrationEventsContext.getContext().runEvent("users");
```

### Spring Boot Integration
```yaml
# In application.yml
hitorro:
  dms:
    db-init:
      data-sets:
        - name: "stores"
          csv-file: "classpath:data/stores.csv"
          consumer: "com.hitorro.basedms.csvconsumers.StoreCSVConsumer"
```

Both use the same underlying mechanism (`CSVHibernateLoader`) but Spring Boot version:
- Uses Spring configuration
- Supports Spring Resource patterns
- Integrates with Spring lifecycle
- Better logging and error handling
- No need to configure integration events separately

## Example Application

See `hitorro-example-springboot` for a complete working example with:
- Store initialization from CSV
- Custom entity loading
- Test configuration
- Error handling

## Architecture

```
ApplicationReadyEvent
    ↓
DMSDataInitializer.initializeData()
    ↓
For each dataset:
    ↓
CSVDataLoader.loadCSV(resource, consumer)
    ↓
CSVHibernateLoader.load(session, file, consumer)
    ↓
Consumer.add(row, entity, existsAlready)
    ↓
session.persist(entity) / session.saveOrUpdate(entity)
    ↓
session.commit()
```

## References

- **CSVHibernateLoader**: `/hitorro-basedms/src/main/java/com/hitorro/basedms/db/CSVHibernateLoader.java`
- **Store Consumer**: `/hitorro-basedms/src/main/java/com/hitorro/basedms/csvconsumers/StoreCSVConsumer.java`
- **Integration Events**: `/hitorro-basedms/src/main/java/com/hitorro/base/objects/BaseDMSService.java`
- **Example CSV**: `/data/initdb/stores.csv`
