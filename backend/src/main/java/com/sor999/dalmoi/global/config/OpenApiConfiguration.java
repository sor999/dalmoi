package com.sor999.dalmoi.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI dalmoiOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Dalmoi API").version("v1").description("달모이 REST API 문서"))
                .components(new Components().addSchemas("ErrorResponse", new ObjectSchema()
                        .addProperty("code", new StringSchema().example("INVALID_REQUEST"))
                        .addProperty("message", new StringSchema().example("올바르지 않은 요청입니다.")))
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인으로 발급받은 Access Token을 입력합니다.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    OperationCustomizer errorResponses() {
        return (operation, method) -> {
            List<String> statuses = method.getBeanType().getSimpleName().equals("AuthController")
                    ? switch (method.getMethod().getName()) {
                        case "signup" -> List.of("400", "409", "500");
                        case "logout" -> List.of("400", "500");
                        default -> List.of("400", "401", "500");
                    }
                    : List.of("400", "401", "403", "404", "409", "500");
            for (String status : statuses) {
                String description = switch (status) {
                    case "400" -> "요청 형식·필드 검증 실패";
                    case "401" -> "인증 누락·유효하지 않거나 만료·폐기된 토큰";
                    case "403" -> "접근 권한 부족";
                    case "404" -> "리소스 없음";
                    case "409" -> "중복 또는 데이터 충돌";
                    default -> "서버 오류";
                };
                operation.getResponses()
                        .addApiResponse(status, new ApiResponse().description(description)
                                .content(new Content().addMediaType("application/json", new MediaType()
                                        .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))));
            }
            return operation;
        };
    }
}
