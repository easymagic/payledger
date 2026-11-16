package com.payledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI payLedgerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayLedger API")
                        .description("Milestone 1 wallet and transfer API. Built to demonstrate Spring Boot 3 "
                                + "and Spring Data JPA patterns used in payments systems.")
                        .version("0.1.0")
                        .contact(new Contact().name("PayLedger").url("https://github.com/payledger/payledger"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
