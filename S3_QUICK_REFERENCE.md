# S3 Filesystem - Quick Reference

## Enable S3 Storage

**File:** `application.yml`

```yaml
hitorro:
  filesystem:
    s3:
      enabled: true
      endpoint: http://localhost:9000
      bucket: hitorro-data
      access-key: minioadmin
      secret-key: minioadmin
      ssl-enabled: false
```

## Start MinIO (Docker)

```bash
docker run -d \
  --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

**Console:** `http://localhost:9001` (minioadmin/minioadmin)

## Create Bucket

Via MinIO Console:
1. Open `http://localhost:9001`
2. Buckets → Create Bucket
3. Name: `hitorro-data`

## Spring Injection

```java
@Autowired(required = false)
private S3CompatibleFileSystem s3FileSystem;
```

## Basic Operations

### Write File
```java
BaseFile file = s3FileSystem.getFileEnsuringDir("docs/report.txt");
try (OutputStream os = file.getOutputStream()) {
    os.write("Content".getBytes(StandardCharsets.UTF_8));
}
```

### Read File
```java
BaseFile file = s3FileSystem.getFile("docs/report.txt");
if (file.exists()) {
    try (InputStream is = file.getInputStream()) {
        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
```

### List Files
```java
BaseFile dir = s3FileSystem.getFile("docs/");
BaseFile[] files = dir.listFiles();
for (BaseFile f : files) {
    System.out.println(f.getName() + " - " + f.length());
}
```

## REST API Endpoints

### Write
```bash
curl -X POST http://localhost:8080/api/filesystem/s3/write \
  -H "Content-Type: application/json" \
  -d '{"path": "test/file.txt", "content": "Hello!"}'
```

### Read
```bash
curl http://localhost:8080/api/filesystem/s3/read/test/file.txt
```

### List
```bash
curl http://localhost:8080/api/filesystem/s3/list?path=/test
```

### Status
```bash
curl http://localhost:8080/api/filesystem/status
```

## Production Configs

### AWS S3
```yaml
endpoint: https://s3.amazonaws.com
bucket: my-bucket
access-key: ${AWS_ACCESS_KEY_ID}
secret-key: ${AWS_SECRET_ACCESS_KEY}
ssl-enabled: true
```

### Wasabi
```yaml
endpoint: https://s3.us-east-2.wasabisys.com
bucket: my-bucket
access-key: ${WASABI_ACCESS_KEY}
secret-key: ${WASABI_SECRET_KEY}
ssl-enabled: true
```

### DigitalOcean Spaces
```yaml
endpoint: https://nyc3.digitaloceanspaces.com
bucket: my-space
access-key: ${DO_SPACES_KEY}
secret-key: ${DO_SPACES_SECRET}
ssl-enabled: true
```

## Common Issues

### Connection Refused
- Check MinIO is running: `docker ps`
- Verify endpoint URL: `http://localhost:9000`

### Access Denied
- Verify credentials in MinIO console
- Check bucket exists and is accessible

### Bean is Null
- Set `enabled: true` in application.yml
- Check all required properties are configured
- Look for startup errors in logs

## Performance Tuning

Override default settings:

```java
@Configuration
public class S3Config {
    @Bean
    public S3CompatibleFileSystem s3FileSystem(FileSystemProperties props) {
        return new S3CompatibleFileSystem(...) {
            @Override
            protected void configureFileSystem(Configuration conf) {
                super.configureFileSystem(conf);
                conf.set("fs.s3a.connection.maximum", "100");
                conf.set("fs.s3a.threads.max", "64");
                conf.set("fs.s3a.multipart.size", "104857600"); // 100MB
            }
        };
    }
}
```

## Path Format

✅ **Correct:**
- `documents/report.txt`
- `path/to/file.txt`
- `data/2024/file.txt`

❌ **Incorrect:**
- `/documents/report.txt` (no leading slash)
- `documents\report.txt` (use forward slashes)

## File System Status Check

```java
@GetMapping("/status")
public Map<String, String> getStatus() {
    Map<String, String> status = new HashMap<>();
    status.put("s3", s3FileSystem != null ? "available" : "not configured");
    return status;
}
```

## Complete Example

```java
@RestController
@RequestMapping("/api/files")
public class FileController {
    
    @Autowired(required = false)
    private S3CompatibleFileSystem s3;
    
    @PostMapping("/upload")
    public String upload(@RequestParam String path, 
                        @RequestParam String content) {
        if (s3 == null) {
            return "S3 not configured";
        }
        
        try {
            BaseFile file = s3.getFileEnsuringDir(path);
            try (OutputStream os = file.getOutputStream()) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return "Uploaded: " + file.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/download/{*path}")
    public ResponseEntity<String> download(@PathVariable String path) {
        if (s3 == null) {
            return ResponseEntity.status(503)
                .body("S3 not configured");
        }
        
        try {
            BaseFile file = s3.getFile(path);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            try (InputStream is = file.getInputStream()) {
                String content = new String(
                    is.readAllBytes(), 
                    StandardCharsets.UTF_8
                );
                return ResponseEntity.ok(content);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Error: " + e.getMessage());
        }
    }
}
```

## Testing Checklist

- [ ] MinIO running on port 9000
- [ ] Bucket created in MinIO console
- [ ] `enabled: true` in application.yml
- [ ] All config properties set (endpoint, bucket, keys)
- [ ] Application started without errors
- [ ] Status endpoint shows S3 as "available"
- [ ] Can write files via POST endpoint
- [ ] Can read files via GET endpoint
- [ ] Can list files via GET endpoint
- [ ] Files visible in MinIO console

## Documentation

- **Full Guide:** `hitorro-spring-boot/S3_FILESYSTEM_GUIDE.md`
- **Implementation Summary:** `S3_IMPLEMENTATION_SUMMARY.md`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

**Need Help?** Check the full S3_FILESYSTEM_GUIDE.md for detailed troubleshooting and examples.
