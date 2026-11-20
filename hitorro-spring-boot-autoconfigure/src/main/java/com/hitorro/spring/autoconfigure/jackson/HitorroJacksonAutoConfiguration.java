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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonFactoryBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.util.Locale;

/**
 * Jackson 2's factory-level defaults for Spring Boot's auto-configured Jackson 3 {@code JsonMapper}
 * ({@code TSFBuilder.configureForJackson2()}: the fast double/big-number parsers off, property-name
 * interning on), as {@code com.hitorro.util.json.JsonMappers} uses. The mapper-level Jackson 2
 * defaults and Jackson 2.18's stream limits are property defaults set by
 * {@code HitorroEnvironmentPostProcessor} ({@code spring.jackson.use-jackson2-defaults},
 * {@code spring.jackson.factory.constraints.*}); {@code configureForJackson2()} changes only
 * features, not the constraints.
 * <p>
 * It also keeps Jackson 2's {@link Locale} values: Jackson 3 always writes a Locale as its
 * language tag ({@code "fr-CA"}), Jackson 2 wrote {@code Locale.toString()} ({@code "fr_CA"}),
 * and no feature switches it back. Reading accepts both forms either way; map keys were
 * already {@code toString()}.
 */
@AutoConfiguration(before = JacksonAutoConfiguration.class)
@ConditionalOnClass(JsonFactoryBuilderCustomizer.class)
public class HitorroJacksonAutoConfiguration {

    @Bean
    public JsonFactoryBuilderCustomizer hitorroJackson2FactoryDefaults() {
        return builder -> builder.configureForJackson2();
    }

    @Bean
    public JsonMapperBuilderCustomizer hitorroJackson2LocaleFormat() {
        return builder -> builder.addModule(new SimpleModule("hitorro-jackson2-locale")
                .addSerializer(Locale.class, ToStringSerializer.instance));
    }
}
