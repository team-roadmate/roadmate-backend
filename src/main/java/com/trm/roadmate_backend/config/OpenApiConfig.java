package com.trm.roadmate_backend.config; // 실제 패키지명으로 변경해주세요

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 1. 보안 스키마의 이름 정의 (Swagger UI에서 사용될 이름)
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // 2. 이 API가 보안 스키마(JWT 토큰)를 필요로 한다고 명시
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        // 3. 보안 스키마 정의 (Bearer Token 타입)
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP) // HTTP 방식
                                .scheme("bearer") // Bearer 타입
                                .bearerFormat("JWT") // 포맷은 JWT
                        )
                );
    }
}