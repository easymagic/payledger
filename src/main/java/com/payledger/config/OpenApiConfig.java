package com.payledger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI payLedgerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayLedger API")
                        .description("Payments and wallet API (milestones 1–5). Spring Boot 3 / Spring Data JPA "
                                + "portfolio project covering ledger design, refunds, webhook outbox, JWT security, "
                                + "caching and metrics.")
                        .version("0.1.0")
                        .contact(new Contact().name("PayLedger").url("https://github.com/easymagic/payledger"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
