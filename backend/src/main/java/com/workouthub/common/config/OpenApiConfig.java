package com.workouthub.common.config;

import com.workouthub.common.web.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
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
        Components components = new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes(FORWARD_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Forwarded-Email"));

        // SpringDoc 3.0.x under Spring Boot 4 stops surfacing @ControllerAdvice
        // ExceptionHandler return-body schemas in components.schemas, even with
        // springdoc.override-with-generic-response=false. Register ApiError and
        // its FieldError nested type explicitly so the OpenApi surface stays
        // stable for tooling that branches on ApiError.code.
        Map<String, Schema> apiErrorSchemas =
                ModelConverters.getInstance().readAll(ApiError.class);
        apiErrorSchemas.forEach(components::addSchemas);

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
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
