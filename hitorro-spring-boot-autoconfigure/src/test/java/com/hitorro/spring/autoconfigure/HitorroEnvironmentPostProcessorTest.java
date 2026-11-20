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
package com.hitorro.spring.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HitorroEnvironmentPostProcessorTest {

    private static final String KEY = "spring.jackson.use-jackson2-defaults";

    @Test
    void defaultsBootsJacksonMapperToJackson2Format() {
        StandardEnvironment environment = new StandardEnvironment();
        new HitorroEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        assertEquals("true", environment.getProperty(KEY));
        assertEquals("1000", environment.getProperty("spring.jackson.factory.constraints.read.max-nesting-depth"));
        assertEquals("1000", environment.getProperty("spring.jackson.factory.constraints.write.max-nesting-depth"));
        assertEquals("20000000", environment.getProperty("spring.jackson.factory.constraints.read.max-string-length"));
        assertNull(environment.getProperty("spring.http.converters.preferred-json-mapper"),
                "the Jackson 2 bridge setting is gone");
    }

    @Test
    void applicationSettingWins() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("app", Map.of(KEY, "false")));
        new HitorroEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        assertEquals("false", environment.getProperty(KEY));
    }
}
