package com.workouthub.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class OpenApiSurfaceIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private static final List<String> EXPECTED_TAGS =
            List.of(
                    "Achievements",
                    "Admin: Audit",
                    "Admin: Challenges",
                    "Admin: Exercises",
                    "Admin: Users",
                    "Analytics",
                    "Analytics: Exercise",
                    "Auth",
                    "Auth: Password Reset",
                    "Body Metrics",
                    "Challenges",
                    "Exercises",
                    "Export",
                    "Health Import",
                    "Nutrition",
                    "Push Notifications",
                    "Supplements",
                    "2FA",
                    "User Sessions (refresh tokens)",
                    "Users",
                    "Users: Password",
                    "Water",
                    "Webhook Tokens",
                    "Workout Days",
                    "Workout Plans",
                    "Workout Plans: Days",
                    "Workout Sessions",
                    "Workout Sessions: Sets");

    private static final Set<String> EXPECTED_API_ERROR_CODES =
            Set.of(
                    "SESSION_ALREADY_ACTIVE",
                    "SESSION_ALREADY_FINISHED",
                    "SESSION_FINISHED",
                    "SET_NUMBER_DUPLICATE");

    private JsonNode fetchOpenApiDocument() throws Exception {
        MvcResult result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void openApiJsonReturns200WithoutAuth() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void swaggerUiReturns200WithoutAuth() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }

    @Test
    void openApiJsonDeclaresBothSecuritySchemes() throws Exception {
        JsonNode doc = fetchOpenApiDocument();
        JsonNode schemes = doc.path("components").path("securitySchemes");
        assertThat(schemes.isObject()).isTrue();
        assertThat(schemes.has("bearerAuth")).isTrue();
        assertThat(schemes.has("forwardAuth")).isTrue();
        JsonNode bearer = schemes.get("bearerAuth");
        assertThat(bearer.path("type").asText()).isEqualTo("http");
        assertThat(bearer.path("scheme").asText()).isEqualTo("bearer");
        assertThat(bearer.path("bearerFormat").asText()).isEqualTo("JWT");
        JsonNode forward = schemes.get("forwardAuth");
        assertThat(forward.path("type").asText()).isEqualTo("apiKey");
        assertThat(forward.path("in").asText()).isEqualTo("header");
        assertThat(forward.path("name").asText()).isEqualTo("X-Forwarded-Email");
    }

    @Test
    void openApiJsonDeclaresAllHandCuratedTags() throws Exception {
        JsonNode doc = fetchOpenApiDocument();
        JsonNode tagsNode = doc.path("tags");
        assertThat(tagsNode.isArray()).isTrue();
        Set<String> actualTagNames = new HashSet<>();
        tagsNode.forEach(t -> actualTagNames.add(t.path("name").asText()));
        assertThat(actualTagNames).containsAll(EXPECTED_TAGS);
    }

    @Test
    void openApiJsonDeclaresApiErrorSchemaWithCodeEnum() throws Exception {
        JsonNode doc = fetchOpenApiDocument();
        JsonNode schemas = doc.path("components").path("schemas");
        assertThat(schemas.isObject()).isTrue();
        assertThat(schemas.has("ApiError")).isTrue();
        boolean hasFieldError =
                schemas.has("ApiError.FieldError")
                        || schemas.has("ApiError$FieldError")
                        || schemas.has("FieldError");
        assertThat(hasFieldError)
                .as(
                        "Expected one of: ApiError.FieldError, ApiError$FieldError, FieldError under components.schemas")
                .isTrue();
        JsonNode codeProp = schemas.path("ApiError").path("properties").path("code");
        assertThat(codeProp.isObject()).isTrue();
        Set<String> actualEnum = new HashSet<>();
        codeProp.path("enum").forEach(n -> actualEnum.add(n.asText()));
        assertThat(actualEnum).isEqualTo(EXPECTED_API_ERROR_CODES);
    }
}
