package com.workouthub.common.config;

import com.workouthub.common.web.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String FORWARD_AUTH = "forwardAuth";

    private final String version;

    public OpenApiConfig(@Value("${app.version:0.0.0}") String version) {
        this.version = version;
    }

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WorkoutHub API")
                        .version(version)
                        .description(
                                "Self-hosted multi-user fitness tracker. "
                                        + "Authentication: built-in JWT (default) or "
                                        + "reverse-proxy forward-auth. See docs/API.md for "
                                        + "navigational reference and "
                                        + "docs/SELF_HOSTED_CONTRACT.md section 7 for the "
                                        + "auth contract."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes(FORWARD_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Forwarded-Email")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    // SpringDoc 3.0.x under Spring Boot 4 stops surfacing @ControllerAdvice
    // ExceptionHandler return-body schemas in components.schemas, even with
    // springdoc.override-with-generic-response=false. The previous attempt to
    // add the schema directly inside the @Bean OpenAPI was overwritten by
    // SpringDoc's later schema scan, so we use OpenApiCustomizer (which fires
    // after that scan) to re-register ApiError + its nested FieldError. This
    // mirrors the pre-Phase-45 workaround that proved durable under SpringDoc
    // 2.6 / Spring Boot 3.5.
    @Bean
    OpenApiCustomizer apiErrorSchemaCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            ResolvedSchema resolved =
                    ModelConverters.getInstance().readAllAsResolvedSchema(ApiError.class);
            if (resolved == null) return;
            if (resolved.schema != null) {
                components.addSchemas(resolved.schema.getName(), resolved.schema);
            }
            if (resolved.referencedSchemas != null) {
                for (Map.Entry<String, Schema> entry : resolved.referencedSchemas.entrySet()) {
                    components.addSchemas(entry.getKey(), entry.getValue());
                }
            }
        };
    }
}
