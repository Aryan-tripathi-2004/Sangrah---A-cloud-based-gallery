package com.example.ApiGateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Externalized gateway security configuration bound from application.properties.
 * Adheres to 12-Factor App principles by eliminating hardcoded security values.
 *
 * <p>Prefix: {@code gateway.security.*}</p>
 *
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    /**
     * Origins allowed for CORS requests.
     * Example: http://localhost:4200, http://localhost:3000
     */
    private List<String> allowedOrigins;

    /**
     * API endpoints that do not require JWT authentication.
     * Requests matching any of these prefixes bypass the authentication filter.
     */
    private List<String> openEndpoints;

    /**
     * Role-based access control mapping.
     * Keys are route path prefixes; values are lists of allowed role names.
     * Example: /api/v1/gallery -> [USER, ADMIN]
     */
    private Map<String, List<String>> routeRoles;
}
