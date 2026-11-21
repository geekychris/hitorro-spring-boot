/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.spring.autoconfigure.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hitorro.util.json.JsonMappers;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The actuator's JSON in the Jackson 2 format. Spring Boot 4 writes endpoint JSON (health, info,
 * metrics, ...) with an isolated plain Jackson 3 mapper ({@code JacksonEndpointAutoConfiguration}),
 * which sorts properties alphabetically ({@code {"components":..,"groups":..,"status":"UP"}}) and
 * which {@code spring.jackson.use-jackson2-defaults} does not reach. HiTorro 3.x apps had Boot's
 * isolated Jackson 2 endpoint mapper: Jackson 2 defaults (declaration order), ISO dates and
 * durations, unknown properties and default views off, NON_NULL. This endpoint mapper does the same,
 * on {@link JsonMappers#builder()} (Jackson 2 defaults, 2.18 stream limits, Jackson 2's JDK types).
 * <p>
 * The bean is deterministic in every context: this configuration runs <em>before</em> Boot's
 * {@code JacksonEndpointAutoConfiguration}, so {@code @ConditionalOnMissingBean} only sees an
 * application's own {@link EndpointJsonMapper} (which then wins), and the bean is {@code @Primary}, so
 * it also wins where Boot's isolated mapper is created as well (Boot's has no
 * {@code @ConditionalOnMissingBean}; it is only skipped when
 * {@code management.endpoints.jackson.isolated-json-mapper=false}, which
 * {@code HitorroEnvironmentPostProcessor} defaults, but which a context built without
 * {@code SpringApplication}'s environment post-processors does not get). Set
 * {@code hitorro.jackson.actuator-jackson2-format=false} to keep Boot's mapper.
 * <p>
 * Health components ({@code IndicatedHealthDescriptor}) have no fields, only {@code getStatus()} and
 * {@code getDetails()}, so Jackson (2 and 3) orders the two by {@code Class.getDeclaredMethods()},
 * which differs between JVM runs: on 3.1.0 a component came out as {@code {"status":..,"details":..}}
 * in most runs and {@code {"details":..,"status":..}} in others. The mapper pins
 * {@code status, details}, the usual order, with a mix-in.
 */
@AutoConfiguration(before = JacksonEndpointAutoConfiguration.class)
@ConditionalOnClass(EndpointJsonMapper.class)
@ConditionalOnBooleanProperty(name = "hitorro.jackson.actuator-jackson2-format", matchIfMissing = true)
public class HitorroEndpointJacksonAutoConfiguration {

    static final String INDICATED_HEALTH_DESCRIPTOR = "org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor";

    @JsonPropertyOrder({"status", "details"})
    interface StatusFirst {
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(EndpointJsonMapper.class)
    public EndpointJsonMapper hitorroJackson2EndpointJsonMapper() {
        JsonMapper mapper = jackson2EndpointMapper();
        return () -> mapper;
    }

    /** The endpoint mapper: Boot's former isolated Jackson 2 endpoint mapper, on Jackson 3. */
    public static JsonMapper jackson2EndpointMapper() {
        JsonMapper.Builder builder = JsonMappers.builder();
        try {
            builder.addMixIn(Class.forName(INDICATED_HEALTH_DESCRIPTOR, false,
                    HitorroEndpointJacksonAutoConfiguration.class.getClassLoader()), StatusFirst.class);
        } catch (ClassNotFoundException e) {
            // no health endpoint on the classpath
        }
        return builder
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)
                        .withContentInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }
}
