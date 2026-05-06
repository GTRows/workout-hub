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

    /**
     * Registers ApiError (and its nested FieldError) under components.schemas.
     *
     * <p>Normally SpringDoc 2.6.0 surfaces these via its
     * {@code GenericResponseService} scan of {@code @RestControllerAdvice}
     * exception handlers, but that path calls
     * {@code new ControllerAdviceBean(Object)}, a constructor Spring 6.2 removed.
     * We disable that scan with {@code springdoc.override-with-generic-response=false}
     * to keep /v3/api-docs from 500-ing, and manually re-register the error
     * schemas here so the OpenAPI consumers (and our integration test) still
     * see ApiError + ApiError.FieldError under components.schemas.
     */
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
