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
     * Allowed HTTP methods for CORS requests.
     * Example: GET, POST, PUT, DELETE
     */
    private List<String> allowedMethods = List.of(
            "GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    );

    /**
     * Allowed request headers for CORS requests.
     * Example: Authorization, Content-Type, Origin
     */
    private List<String> allowedHeaders = List.of(
            "Origin",
            "X-Requested-With",
            "Content-Type",
            "Accept",
            "Authorization",
            "X-CSRF-TOKEN"
    );

    /**
     * Exposed response headers for CORS requests.
     * Example: Authorization, Content-Type
     */
    private List<String> exposedHeaders = List.of(
            "Authorization",
            "Content-Type"
    );

    /**
     * Allow credentials (cookies, Authorization header) for CORS requests.
     */
    private boolean allowCredentials = true;

    /**
     * Preflight response cache duration in seconds.
     */
    private Long maxAge = 3600L;

    /**
     * CORS path mapping pattern.
     * Example: /api/**
     */
    private String corsPathPattern = "/api/**";

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

    /**
     * Nested CORS configuration support to bind gateway.security.cors.* properties.
     */
    private Cors cors = new Cors();

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public List<String> getAllowedMethods() {
        if (cors != null && cors.getAllowedMethods() != null && !cors.getAllowedMethods().isEmpty()) {
            return cors.getAllowedMethods();
        }
        return allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        if (cors != null && cors.getAllowedHeaders() != null && !cors.getAllowedHeaders().isEmpty()) {
            return cors.getAllowedHeaders();
        }
        return allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        if (cors != null && cors.getExposedHeaders() != null && !cors.getExposedHeaders().isEmpty()) {
            return cors.getExposedHeaders();
        }
        return exposedHeaders;
    }

    public boolean isAllowCredentials() {
        if (cors != null && cors.getAllowCredentials() != null) {
            return cors.getAllowCredentials();
        }
        return allowCredentials;
    }

    public Long getMaxAge() {
        if (cors != null && cors.getMaxAge() != null) {
            return cors.getMaxAge();
        }
        return maxAge;
    }

    public String getCorsPathPattern() {
        if (cors != null && cors.getPathPattern() != null) {
            return cors.getPathPattern();
        }
        return corsPathPattern;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedMethods;
        private List<String> allowedHeaders;
        private List<String> exposedHeaders;
        private Boolean allowCredentials;
        private Long maxAge;
        private String pathPattern;
    }
}
