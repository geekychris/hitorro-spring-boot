package com.hitorro.spring.autoconfigure.golden;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.spring.autoconfigure.commands.CommandRegistrationManager;
import com.hitorro.spring.autoconfigure.commands.CommandRestController;
import com.hitorro.spring.autoconfigure.jvs.JVSRestController;
import com.hitorro.spring.autoconfigure.rest.HitorroRestController;
import com.hitorro.spring.autoconfigure.service.ServiceExplorerController;
import com.hitorro.spring.autoconfigure.transformer.RenditionTransformationController;
import com.hitorro.util.commandandcontrol.RestOperations;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.DebugArgAno;
import com.hitorro.util.json.keys.StringProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The REST/JSON responses of the starter's auto-configured controllers (HitorroRestController,
 * CommandRestController, JVSRestController) and of its response types, as an application using
 * the starter gets them over HTTP. {@code jackson2-rest-3.1.0.txt} was recorded by running this
 * same test (with {@code -Dgolden.record=<file>}) against hitorro-spring-boot 3.1.0, whose HTTP
 * JSON went through Spring Boot's Jackson 2 mapper (spring-boot-jackson2,
 * preferred-json-mapper=jackson2). 4.0.0 serves them through Boot's Jackson 3 mapper with the
 * Jackson 2 defaults and must return byte-identical bodies (only execution times and error
 * timestamps are masked). The source has no Jackson types, so it compiles against both.
 * <p>
 * The {@code commands.get/help} keys record an exception on 3.1.0 too: CommandRestController's
 * {@code @PathVariable String commandName} has no name and the module is not compiled with
 * {@code -parameters}. Pinned as it is, not fixed here.
 */
@SpringBootTest(classes = Jackson2RestGoldenTest.GoldenApp.class, properties = {
        "spring.autoconfigure.exclude="
                + "com.hitorro.spring.autoconfigure.service.HitorroServiceAutoConfiguration,"
                + "com.hitorro.spring.autoconfigure.dms.DMSAutoConfiguration,"
                + "com.hitorro.spring.autoconfigure.integration.IntegrationEventsAutoConfiguration,"
                + "com.hitorro.spring.autoconfigure.cli.HitorroCliAutoConfiguration,"
                + "com.hitorro.spring.autoconfigure.dbinit.DBInitAutoConfiguration,"
                + "com.hitorro.spring.autoconfigure.transformer.TransformerAutoConfiguration,"
                + "com.hitorro.spring.boot.autoconfigure.ai.AIServiceAutoConfiguration,"
                + "com.hitorro.spring.autoconfigure.logging.StructuredLoggingConfiguration",
        "hitorro.jvs.rest.enabled=true",
        "spring.main.banner-mode=off"
})
@AutoConfigureMockMvc
class Jackson2RestGoldenTest {

    static final Date FIXED = new Date(1700000000123L);

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({GoldenController.class, GoldenCommands.class})
    static class GoldenApp {
    }

    /** Commands exposed through HitorroRestController (/api/rest) and CommandRestController (/api/commands). */
    public static class GoldenCommands {

        @CommandDef(command = "golden.echo", description = "Echo a message", restOperations = {RestOperations.Get, RestOperations.Post}, isInternal = false)
        public String echo(@DebugArgAno(propType = StringProperty.class, keyName = "message", description = "Message", defaultValue = "") String message) {
            return message;
        }

        @CommandDef(command = "golden.add", description = "Add two numbers", restOperations = {RestOperations.Get, RestOperations.Post}, isInternal = false)
        public int add(@DebugArgAno(propType = StringProperty.class, keyName = "a", defaultValue = "0") int a,
                       @DebugArgAno(propType = StringProperty.class, keyName = "b", defaultValue = "0") int b) {
            return a + b;
        }

        @CommandDef(command = "golden.jvs", description = "Return a document", restOperations = {RestOperations.Get}, isInternal = false)
        public JVS jvs() {
            return JVS.read("{\"title\":\"Caf\\u00e9 \\uD83D\\uDE00\",\"n\":1.10,\"big\":12345678901234567890,\"ok\":true,\"none\":null,"
                    + "\"tags\":[\"a\",\"b\"],\"nested\":{\"x\":{\"y\":[1,{\"z\":\"deep\"}]}}}");
        }

        @CommandDef(command = "golden.fail", description = "Always fails", restOperations = {RestOperations.Get}, isInternal = false)
        public String fail() {
            throw new IllegalStateException("golden failure");
        }
    }

    /** Returns the starter's response types and common Java values through Boot's HTTP JSON mapper. */
    @RestController
    public static class GoldenController {

        @GetMapping("/golden/commanddef")
        public Object commandDef() {
            CommandRestController.CommandDefInfo info = new CommandRestController.CommandDefInfo();
            info.setName("golden.cmd");
            info.setDescription("A command");
            info.setReturnType("java.lang.String");
            info.setInternal(false);
            info.setRestOperations(List.of("GET", "POST"));
            CommandRestController.CommandParameter p = new CommandRestController.CommandParameter();
            p.setName("message");
            p.setType("StringProperty");
            p.setRequired(true);
            p.setDefaultValue(null);
            p.setDescription("The message");
            CommandRestController.CommandParameter q = new CommandRestController.CommandParameter();
            q.setName("count");
            q.setType("IntegerProperty");
            q.setDefaultValue(5);
            info.setParameters(List.of(p, q));
            return info;
        }

        @GetMapping("/golden/execute-response")
        public Object executeResponse() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("when", FIXED);
            result.put("rows", List.of(1, 2.5, "x"));
            return new CommandRestController.CommandExecuteResponse(true, result, null, 42L);
        }

        @GetMapping("/golden/service")
        public Object service() {
            ServiceExplorerController.ServiceInfo s = new ServiceExplorerController.ServiceInfo();
            s.setShortName("svc");
            s.setDescription("A service");
            s.setClassName("com.example.Svc");
            s.setInitialized(true);
            s.setVersion("1.2");
            s.setDependentServices(List.of("a", "b"));
            s.setDependentServiceInterfaces(List.of());
            s.setDebugCommands(null);
            return s;
        }

        @GetMapping("/golden/discovery")
        public Object discovery() {
            HitorroRestController.RestDiscoveryResponse r = new HitorroRestController.RestDiscoveryResponse();
            HitorroRestController.RestEndpointInfo e = new HitorroRestController.RestEndpointInfo();
            e.setPath("/api/rest/x");
            e.setCommand("x");
            e.setDescription("desc");
            e.setMethods(List.of("GET"));
            e.setInternal(true);
            HitorroRestController.RestParameterInfo pi = new HitorroRestController.RestParameterInfo();
            pi.setName("p");
            e.setParameters(List.of(pi));
            r.setEndpoints(List.of(e));
            r.setTotalEndpoints(1);
            r.setBasePath("/api/rest");
            return r;
        }

        @GetMapping("/golden/values")
        public Object values() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", FIXED);
            m.put("sqlTimestamp", new java.sql.Timestamp(1700000000123L));
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(1709200000000L);
            m.put("calendar", cal);
            m.put("instant", Instant.ofEpochMilli(1700000000123L));
            m.put("localDate", LocalDate.of(2024, 2, 29));
            m.put("localDateTime", LocalDateTime.of(2024, 2, 29, 10, 11, 12, 345000000));
            m.put("offsetDateTime", OffsetDateTime.of(2024, 2, 29, 10, 11, 12, 0, ZoneOffset.ofHours(2)));
            m.put("duration", Duration.ofMillis(90061001));
            m.put("optional", Optional.of("x"));
            m.put("emptyOptional", Optional.empty());
            m.put("uuid", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
            m.put("enum", RestOperations.Post);
            m.put("decimal", new BigDecimal("1.10"));
            m.put("bigInteger", new BigInteger("123456789012345678901234567890"));
            m.put("double", 0.1);
            m.put("float", 1.5f);
            m.put("long", Long.MAX_VALUE);
            m.put("bytes", new byte[]{0, 1, 2, (byte) 255});
            m.put("char", 'c');
            m.put("null", null);
            m.put("emptyList", List.of());
            m.put("set", new TreeSet<>(Set.of("b", "a")));
            m.put("nested", Map.of("k", List.of(Map.of("v", 1))));
            m.put("unicode", "\u00e9 \uD83D\uDE00 \"q\" </script>");
            m.put("locale", Locale.CANADA_FRENCH);
            m.put("localeVariant", new Locale("ja", "JP", "JP"));
            m.put("localeKeys", new LinkedHashMap<>(Map.of(Locale.CANADA_FRENCH, 1)));
            m.put("uri", java.net.URI.create("http://example.com/a?b=c"));
            return m;
        }

        @GetMapping("/golden/jvs-node")
        public Object jvsNode() {
            return JVS.read("{\"b\":2,\"a\":1,\"arr\":[1.10,1e2,null,\"s\"],\"o\":{}}").getJsonNode();
        }

        @GetMapping("/golden/jvs-object")
        public Object jvsObject() {
            return JVS.read("{\"b\":2,\"a\":1}");
        }

        @GetMapping("/golden/deep")
        public Object deep() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 700; i++) sb.append("{\"d\":");
            sb.append("1");
            for (int i = 0; i < 700; i++) sb.append('}');
            return JVS.read(sb.toString()).getJsonNode();
        }

        @PostMapping("/golden/echo-map")
        public Object echoMap(@RequestBody Map<String, Object> body) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : body.entrySet()) {
                out.put(e.getKey(), e.getValue() == null ? "null" : e.getValue().getClass().getSimpleName() + ":" + e.getValue());
            }
            return out;
        }

        @PostMapping("/golden/echo-request")
        public Object echoRequest(@RequestBody RenditionTransformationController.TransformationRequest body) {
            return body;
        }

        @PostMapping("/golden/echo-query")
        public Object echoQuery(@RequestBody JVSRestController.QueryRequest body) {
            return body;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommandRegistrationManager commandManager;

    private final Map<String, String> out = new LinkedHashMap<>();

    private void call(String key, RequestBuilder request) throws Exception {
        MvcResult r;
        try {
            r = mockMvc.perform(request).andReturn();
        } catch (Exception e) {
            // the request failed inside the controller (no error response was written)
            Throwable t = e;
            while (t.getCause() != null) t = t.getCause();
            out.put(key, "!" + t.getClass().getSimpleName());
            return;
        }
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        body = body.replaceAll("\"executionTimeMs\":\\d+", "\"executionTimeMs\":<ms>")
                .replaceAll("\"(timestamp|time)\":\"[^\"]*\"", "\"$1\":<ts>")
                .replaceAll("\"cause\":\\[[^\\]]*\\]", "\"cause\":[<stack frames>]")
                .replaceAll("@[0-9a-f]{4,}", "@<hash>");
        if (UNORDERED.contains(key)) {
            body = sortFlatObject(body);
        }
        String type = r.getResponse().getContentType();
        out.put(key, r.getResponse().getStatus() + " " + type + " " + body.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r"));
    }

    /** Bodies built with {@code Map.of}, whose key order changes from one JVM run to the next (3.1.0 too). */
    private static final Set<String> UNORDERED = Set.of("rest.unknown");

    /** A flat JSON object with its members sorted (for {@link #UNORDERED} bodies). */
    static String sortFlatObject(String body) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"((?:[^\"\\\\]|\\\\.)*)\":(\"(?:[^\"\\\\]|\\\\.)*\"|[^,}]+)").matcher(body);
        TreeMap<String, String> members = new TreeMap<>();
        while (m.find()) members.put(m.group(1), m.group(2));
        StringBuilder sb = new StringBuilder("{");
        members.forEach((k, v) -> sb.append(sb.length() > 1 ? "," : "").append('"').append(k).append("\":").append(v));
        return sb.append('}').toString();
    }

    private static RequestBuilder postJson(String path, String json) {
        return post(path).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(json);
    }

    @Test
    void responsesMatchJackson2() throws Exception {
        if (commandManager.getCommandRegistry().get("golden.echo") == null) {
            commandManager.getCommandRegistry().addAllFromObject(new GoldenCommands());
        }
        MediaType json = MediaType.APPLICATION_JSON;

        // the starter's response types and Java values through Boot's mapper
        for (String p : new String[]{"commanddef", "execute-response", "service", "discovery", "values", "jvs-node", "jvs-object", "deep"}) {
            call("type." + p, get("/golden/" + p).accept(json));
        }
        // request bodies through Boot's mapper (numbers, unknown properties, trailing tokens, bad JSON)
        call("echo.map", postJson("/golden/echo-map", "{\"i\":7,\"l\":12345678901,\"big\":123456789012345678901234567890,\"d\":1.10,\"e\":1e2,\"t\":true,\"z\":null,\"s\":\"x\",\"a\":[1],\"o\":{\"k\":1}}"));
        call("echo.map.dupkey", postJson("/golden/echo-map", "{\"a\":1,\"a\":2}"));
        call("echo.map.trailing", postJson("/golden/echo-map", "{\"a\":1} trailing"));
        call("echo.map.bad", postJson("/golden/echo-map", "{bad"));
        call("echo.request", postJson("/golden/echo-request", "{\"documentGuid\":\"d1\",\"targetMimeType\":\"application/pdf\",\"addAsChild\":true,\"unknownField\":5}"));
        call("echo.request.nullprimitive", postJson("/golden/echo-request", "{\"addAsChild\":null,\"executeImmediately\":\"true\"}"));
        call("echo.query", postJson("/golden/echo-query", "{\"document\":\"{}\",\"properties\":\"single\"}"));

        // HitorroRestController
        call("rest.discovery", get("/api/rest").accept(json));
        call("rest.echo", get("/api/rest/golden.echo").param("message", "Hello \u00e9").accept(json));
        call("rest.echo.post", postJson("/api/rest/golden.echo", "{\"message\":\"posted\"}"));
        call("rest.add", get("/api/rest/golden.add").param("a", "10").param("b", "32").accept(json));
        call("rest.add.post", postJson("/api/rest/golden.add", "{\"a\":1,\"b\":2}"));
        call("rest.jvs", get("/api/rest/golden.jvs").accept(json));
        call("rest.fail", get("/api/rest/golden.fail").accept(json));
        call("rest.unknown", get("/api/rest/golden.nope").accept(json));

        // CommandRestController
        call("commands.get", get("/api/commands/golden.echo").accept(json));
        call("commands.get.unknown", get("/api/commands/golden.nope").accept(json));
        call("commands.help", get("/api/commands/help/golden.add").accept(json));
        call("commands.execute", postJson("/api/commands/execute", "{\"command\":\"golden.echo\",\"arguments\":{\"message\":\"hi\"}}"));
        call("commands.execute.add", postJson("/api/commands/execute", "{\"command\":\"golden.add\",\"arguments\":{\"a\":\"2\",\"b\":\"3\"}}"));
        call("commands.execute.jvs", postJson("/api/commands/execute", "{\"command\":\"golden.jvs\"}"));
        call("commands.execute.unknown", postJson("/api/commands/execute", "{\"command\":\"golden.nope\"}"));

        // JVSRestController
        call("jvs.parse", post("/api/jvs/parse").contentType(json).accept(json).content("{\"a\":1}"));
        call("jvs.query", postJson("/api/jvs/query", "{\"document\":\"{\\\"s\\\":\\\"x\\\",\\\"n\\\":5,\\\"o\\\":{\\\"k\\\":[1,2]},\\\"l\\\":[\\\"a\\\",\\\"b\\\"]}\",\"properties\":[\"s\",\"n\",\"o\",\"l\",\"missing\"]}"));

        String record = System.getProperty("golden.record");
        if (record != null) {
            StringBuilder sb = new StringBuilder();
            out.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
            Files.writeString(Path.of(record), sb.toString());
            return;
        }
        Map<String, String> golden = new LinkedHashMap<>();
        try (InputStream in = getClass().getResourceAsStream("jackson2-rest-3.1.0.txt")) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (line.isEmpty()) continue;
                int eq = line.indexOf('=');
                golden.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        for (Map.Entry<String, String> e : golden.entrySet()) {
            assertEquals(e.getValue(), out.get(e.getKey()), e.getKey());
        }
        assertEquals(golden.keySet(), out.keySet());
    }
}
