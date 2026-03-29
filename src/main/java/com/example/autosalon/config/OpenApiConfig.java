package com.example.autosalon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI autosalonOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Autosalon API")
                        .description("REST API для управления автосалоном, автомобилями, клиентами, продажами и опциями.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Autosalon Team")
                                .email("support@autosalon.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
