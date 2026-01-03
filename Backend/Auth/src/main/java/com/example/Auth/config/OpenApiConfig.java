package com.example.Auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

import static com.example.Auth.util.ConfigConstants.*;

/**
 * OpenAPI/Swagger configuration for the Auth Service.
 * Provides API documentation at /swagger-ui.html
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = API_TITLE, version = API_VERSION, description = API_DESCRIPTION, contact = @Contact(name = CONTACT_NAME, email = CONTACT_EMAIL, url = CONTACT_URL), license = @License(name = LICENSE_NAME, url = LICENSE_URL)), servers = {
                @Server(url = SERVER_LOCAL_URL, description = SERVER_LOCAL_DESC),
                @Server(url = SERVER_PROD_URL, description = SERVER_PROD_DESC)
})
@SecurityScheme(name = SECURITY_SCHEME_NAME, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = SECURITY_SCHEME_BEARER_FORMAT, description = SECURITY_SCHEME_DESCRIPTION)
public class OpenApiConfig {
        // Configuration is done via annotations
        // Spring Boot auto-configuration will handle the rest with
        // springdoc-openapi-starter-webmvc-ui
}
