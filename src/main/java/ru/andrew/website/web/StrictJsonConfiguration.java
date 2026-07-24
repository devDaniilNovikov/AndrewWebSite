package ru.andrew.website.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration(proxyBeanMethods = false)
class StrictJsonConfiguration {
    @Bean
    JsonMapperBuilderCustomizer strictTextualJsonTypes() {
        return builder -> builder.withCoercionConfig(LogicalType.Textual, coercion -> {
            coercion.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
            coercion.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            coercion.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        });
    }
}
