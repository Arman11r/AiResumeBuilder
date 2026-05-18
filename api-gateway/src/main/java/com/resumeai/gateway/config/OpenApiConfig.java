package com.resumeai.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ResumeAI API Gateway")
                        .description("""
                                Unified Swagger UI for all ResumeAI microservices.
                                Use the dropdown (top-right) to switch between services:
                                Auth · Resume · Section · AI · Template · Export · JobMatch · Notification
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("ResumeAI Team")));
    }
}
