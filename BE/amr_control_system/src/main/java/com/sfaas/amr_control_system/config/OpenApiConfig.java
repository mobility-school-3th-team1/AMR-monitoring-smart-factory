package com.sfaas.amr_control_system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI amrMonitoringOpenApi() {
        String bearerScheme = OpenApiDocumentationConstants.BEARER_SECURITY_SCHEME;

        return new OpenAPI()
                .info(new Info()
                        .title("AMR Smart Factory Monitoring API")
                        .description("REST API for the AMR integrated monitoring backend. WebSocket `/stream` is documented in `docs/API 정의.md` §8.")
                        .version("v1"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api/v1")
                                .description("Local / Docker (context-path /api/v1)")))
                .components(new Components()
                        .addSecuritySchemes(bearerScheme, new SecurityScheme()
                                .name(bearerScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme));
    }
}
