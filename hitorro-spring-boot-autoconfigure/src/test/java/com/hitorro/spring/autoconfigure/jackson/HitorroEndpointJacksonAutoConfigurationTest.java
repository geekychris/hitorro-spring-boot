package com.hitorro.spring.autoconfigure.jackson;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Constructor;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Which {@link EndpointJsonMapper} the actuator gets, in every way a context can be built: the
 * starter's Jackson 2-format one must win whether or not
 * {@code management.endpoints.jackson.isolated-json-mapper=false} reached the environment (a context
 * built without SpringApplication's environment post-processors does not get the starter's default,
 * so Boot's isolated mapper exists as well), lose only to an application's own mapper, and step aside
 * when switched off. Each case runs several times (fresh contexts).
 */
class HitorroEndpointJacksonAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonEndpointAutoConfiguration.class,
                    HitorroEndpointJacksonAutoConfiguration.class));

    /** A health component as the health endpoint writes it. */
    static Object component() throws Exception {
        Class<?> c = Class.forName(HitorroEndpointJacksonAutoConfiguration.INDICATED_HEALTH_DESCRIPTOR);
        Constructor<?> ctor = c.getDeclaredConstructor(Health.class);
        ctor.setAccessible(true);
        return ctor.newInstance(Health.up().withDetail("database", "H2").build());
    }

    private static void assertOurs(org.springframework.context.ApplicationContext context) throws Exception {
        EndpointJsonMapper actual = context.getBean(EndpointJsonMapper.class);
        assertSame(context.getBean("hitorroJackson2EndpointJsonMapper"), actual, "the starter's endpoint mapper");
        JsonMapper mapper = actual.get();
        assertEquals("{\"status\":\"UP\",\"details\":{\"database\":\"H2\"}}", mapper.writeValueAsString(component()));
        assertEquals("{\"b\":1,\"a\":2}", mapper.writeValueAsString(new java.util.LinkedHashMap<>(Map.of("b", 1)) {{ put("a", 2); }}));
    }

    @RepeatedTest(5)
    void winsWithoutTheStarterPropertyDefault() {
        // as in a context without HitorroEnvironmentPostProcessor: Boot's isolated mapper is created too
        runner.run(context -> {
            assertEquals(2, context.getBeansOfType(EndpointJsonMapper.class).size());
            assertOurs(context);
        });
    }

    @RepeatedTest(5)
    void winsWithTheStarterPropertyDefault() {
        runner.withPropertyValues("management.endpoints.jackson.isolated-json-mapper=false").run(context -> {
            assertEquals(1, context.getBeansOfType(EndpointJsonMapper.class).size());
            assertOurs(context);
        });
    }

    @Test
    void anApplicationsOwnMapperWins() {
        EndpointJsonMapper own = JsonMapper::shared;
        runner.withPropertyValues("management.endpoints.jackson.isolated-json-mapper=false")
                .withBean("appEndpointJsonMapper", EndpointJsonMapper.class, () -> own)
                .run(context -> assertSame(own, context.getBean(EndpointJsonMapper.class)));
    }

    @Test
    void canBeSwitchedOff() {
        runner.withPropertyValues("hitorro.jackson.actuator-jackson2-format=false").run(context -> {
            assertEquals(1, context.getBeansOfType(EndpointJsonMapper.class).size());
            assertNotSame(null, context.getBean(EndpointJsonMapper.class));
            assertEquals(false, context.containsBean("hitorroJackson2EndpointJsonMapper"));
        });
    }

    @Test
    void healthComponentsAlwaysWriteStatusFirst() throws Exception {
        JsonMapper mapper = HitorroEndpointJacksonAutoConfiguration.jackson2EndpointMapper();
        for (int i = 0; i < 20; i++) {
            assertEquals("{\"status\":\"UP\",\"details\":{\"database\":\"H2\"}}", mapper.writeValueAsString(component()));
        }
    }
}
