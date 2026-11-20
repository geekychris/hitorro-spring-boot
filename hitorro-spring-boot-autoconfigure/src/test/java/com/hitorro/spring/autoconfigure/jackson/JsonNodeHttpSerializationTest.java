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
package com.hitorro.spring.autoconfigure.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot 4 serializes HTTP bodies with Jackson 3 by default. HiTorro controllers
 * return Jackson 2 ({@code com.fasterxml.jackson}) JsonNodes, so the starter keeps
 * HTTP JSON on Jackson 2. These tests prove a JsonNode / ObjectNode returned from a
 * controller is written as plain JSON, not as a bean dump of the node's getters.
 *
 * <p>No property is set here: the default comes from {@code HitorroEnvironmentPostProcessor}.
 */
@WebMvcTest(JsonNodeHttpSerializationTest.JsonNodeController.class)
class JsonNodeHttpSerializationTest {

    private static final String EXPECTED =
            "{\"name\":\"hitorro\",\"count\":3,\"ratio\":0.5,\"active\":true,"
            + "\"tags\":[\"a\",\"b\"],\"nested\":{\"empty\":null}}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;

    @Test
    void objectNodeIsSerializedAsPlainJson() throws Exception {
        mockMvc.perform(get("/test/object-node").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(EXPECTED, JsonCompareMode.STRICT));
    }

    @Test
    void jsonNodeIsSerializedAsPlainJson() throws Exception {
        mockMvc.perform(get("/test/json-node").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[1,\"two\",{\"three\":3}]", JsonCompareMode.STRICT));
    }

    @Test
    void preferredJsonMapperDefaultsToJackson2() {
        assertEquals("jackson2", environment.getProperty("spring.http.converters.preferred-json-mapper"));
        List<HttpMessageConverter<?>> converters = handlerAdapter.getMessageConverters();
        HttpMessageConverter<?> writer = converters.stream()
                .filter(c -> c.canWrite(ObjectNode.class, MediaType.APPLICATION_JSON))
                .findFirst()
                .orElseThrow();
        assertInstanceOf(MappingJackson2HttpMessageConverter.class, writer,
                "the converter writing ObjectNode as JSON should be Jackson 2, got " + writer.getClass().getName());
    }

    @SpringBootConfiguration
    @Import(JsonNodeController.class)
    static class TestApplication {
    }

    @RestController
    static class JsonNodeController {

        private final ObjectMapper mapper = new ObjectMapper();

        @GetMapping("/test/object-node")
        ObjectNode objectNode() {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", "hitorro");
            node.put("count", 3);
            node.put("ratio", 0.5);
            node.put("active", true);
            node.putArray("tags").add("a").add("b");
            node.putObject("nested").putNull("empty");
            return node;
        }

        @GetMapping("/test/json-node")
        JsonNode jsonNode() throws Exception {
            return mapper.readTree("[1,\"two\",{\"three\":3}]");
        }
    }
}
