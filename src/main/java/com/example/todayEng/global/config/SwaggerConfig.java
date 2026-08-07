package com.example.todayEng.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//http://localhost:8080/swagger-ui/index.html
@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME_NAME = "JWT";

    @Value("${swagger.server-url:https://34-50-27-108.nip.io}")
    private String serverUrl;

    @Value("${swagger.local-server-url:http://localhost:8080}")
    private String localServerUrl;

    @Bean
    public OpenAPI openAPI() {
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(JWT_SCHEME_NAME);

        SecurityScheme jwtScheme = new SecurityScheme()
                .name(JWT_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER);

        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .addSecurityItem(securityRequirement)
                .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes(JWT_SCHEME_NAME, jwtScheme));
    }

    private List<Server> apiServers() {
        return List.of(
                new Server().url(serverUrl).description("배포 서버"),
                new Server().url(localServerUrl).description("로컬 서버")
        );
    }

    private Info apiInfo() {
        return new Info()
                .title("todayEng API")
                .description("프로젝트 API 명세서")
                .version("v1.0.0");
    }
}
