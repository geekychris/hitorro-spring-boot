/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.spring.autoconfigure.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hitorro.util.json.JsonMappers;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
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
 * Boot's own endpoint mapper has no {@code @ConditionalOnMissingBean}, so
 * {@code HitorroEnvironmentPostProcessor} defaults
 * {@code management.endpoints.jackson.isolated-json-mapper=false} to switch it off. An application
 * that sets the property to {@code true} (or defines its own {@link EndpointJsonMapper}) keeps Boot's.
 */
@AutoConfiguration(after = JacksonEndpointAutoConfiguration.class)
@ConditionalOnClass(EndpointJsonMapper.class)
public class HitorroEndpointJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EndpointJsonMapper.class)
    public EndpointJsonMapper hitorroJackson2EndpointJsonMapper() {
        JsonMapper mapper = jackson2EndpointMapper();
        return () -> mapper;
    }

    /** The endpoint mapper: Boot's former isolated Jackson 2 endpoint mapper, on Jackson 3. */
    public static JsonMapper jackson2EndpointMapper() {
        return JsonMappers.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)
                        .withContentInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }
}
