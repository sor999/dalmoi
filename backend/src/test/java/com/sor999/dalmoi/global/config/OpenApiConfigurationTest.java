package com.sor999.dalmoi.global.config;

import com.sor999.dalmoi.global.response.ApiResponse;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PostgresIntegrationTest
@AutoConfigureMockMvc
@Import(OpenApiConfigurationTest.TestApi.class)
class OpenApiConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger 진입 경로는 UI로 이동한다")
    void swaggerUi_defaultConfiguration_redirectsToUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @ParameterizedTest(name = "문서 경로: {0}")
    @ValueSource(strings = {"/swagger-ui/index.html", "/swagger-ui/swagger-ui.css", "/v3/api-docs/swagger-config",
            "/v3/api-docs.yaml"})
    @DisplayName("Swagger 리소스와 문서 설정을 조회한다")
    void swaggerResources_defaultConfiguration_returnsOk(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OpenAPI는 JWT 인증 방식과 API 경로를 문서화한다")
    void apiDocs_apiController_exposesApiAndBearerScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Dalmoi API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/swagger-probe'].get").exists());
    }

    @Test
    @DisplayName("명시적으로 공개한 API는 전역 Bearer 인증 요구를 해제한다")
    void apiDocs_publicOperation_overridesGlobalSecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/swagger-public-probe'].get.security").isEmpty());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class TestApi {

        @GetMapping("/api/v1/swagger-probe")
        ApiResponse<String> probe() {
            return ApiResponse.of("ok");
        }

        @GetMapping("/api/v1/swagger-public-probe")
        @SecurityRequirements
        ApiResponse<String> publicProbe() {
            return ApiResponse.of("ok");
        }
    }
}
