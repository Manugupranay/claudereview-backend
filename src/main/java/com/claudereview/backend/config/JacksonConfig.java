package com.claudereview.backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for the application.
 *
 * <p>Configures case-insensitive enum deserialization so Claude can return
 * lowercase enum values (e.g. "critical", "security", "high") as instructed
 * by our system prompt. Our Java enums use uppercase (CRITICAL, SECURITY, HIGH)
 * because that's the Java convention, but Jackson would normally reject the
 * lowercase strings as invalid enum values.
 *
 * <p>Also enables {@link DeserializationFeature#ACCEPT_SINGLE_VALUE_AS_ARRAY}
 * so a single finding (rare but possible) deserializes cleanly into a list.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .featuresToEnable(
                        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                        DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE
                )
                .postConfigurer(mapper -> {
                    mapper.configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
                });
    }
}