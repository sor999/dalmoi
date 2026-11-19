package com.sor999.dalmoi.auth;

import com.sor999.dalmoi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PostgresIntegrationTest
@AutoConfigureMockMvc
class AuthOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("인증 응답 계약과 CurrentMember 파라미터 숨김을 OpenAPI에 반영한다")
    void apiDocs_authContracts_hidesPrincipalAndRefreshExpiry() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/members/me'].get.parameters").doesNotExist())
                .andExpect(
                        jsonPath("$.components.schemas.LoginResponse.properties.accessTokenExpiresInSeconds").exists())
                .andExpect(jsonPath("$.components.schemas.LoginResponse.properties.refreshTokenExpiresInSeconds")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.RefreshResponse.properties.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/auth/signup'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post.responses['204']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/auth/login'].post.responses['401'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"));
    }

    @ParameterizedTest(name = "공개 인증 API: {0}")
    @ValueSource(strings = {"signup", "login", "refresh", "logout"})
    @DisplayName("공개 인증 API는 Swagger에서도 Access Token을 요구하지 않는다")
    void apiDocs_publicAuthEndpoint_overridesBearerRequirement(String endpoint) throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/" + endpoint + "'].post.security").isEmpty());
    }
}
