package com.workouthub.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
}
