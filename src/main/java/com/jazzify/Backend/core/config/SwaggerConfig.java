package com.jazzify.backend.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@NullMarked
@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(BEARER_SCHEME);

        return new OpenAPI()
                .info(new Info()
                        .title("Jazzify API")
                        .description("Jazzify 백엔드 API 문서")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, bearerScheme))
                .addSecurityItem(securityRequirement);
    }
}

