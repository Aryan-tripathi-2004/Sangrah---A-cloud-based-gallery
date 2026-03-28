package com.example.ApiGateway.security;

import com.example.ApiGateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Global authentication and authorization filter for API Gateway
 * Validates JWT tokens and enforces role-based access control
 * Note: CORS preflight (OPTIONS) requests are handled by CorsWebFilter before reaching this
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Public endpoints that don't require authentication
    private final List<String> openApiEndpoints = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/token/refresh",  // ⭐ FIXED: Correct path for token refresh
            "/swagger-ui/",
            "/v3/api-docs/",
            "/health",
            "/actuator/"
    );

    // Role-based access control mapping for protected endpoints
    private final Map<String, List<String>> routeRoleMap = Map.of(
            "/api/v1/gallery", List.of("USER", "ADMIN"),
            "/api/v1/events", List.of("USER", "ADMIN"),
            "/api/v1/billing", List.of("USER", "ADMIN"),
            "/api/v1/notifications", List.of("USER", "ADMIN"),
            "/api/v1/admin/", List.of("ADMIN")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        String rawPath = exchange.getRequest().getURI().getRawPath();
        String fullPath = exchange.getRequest().getURI().toString();

        log.info("🔐 AuthenticationFilter - Normalized Path: {} | Raw Path: {}", path, rawPath);
        log.debug("   Full URI: {}", fullPath);

        // Allow public endpoints without authentication
        if (isOpenApi(path)) {
            log.info("✅ Public endpoint allowed: {}", path);
            return chain.filter(exchange);
        }

        // Allow file serving endpoint without Authorization header (JWT can be in query param)
        log.debug("🔍 Checking bypass - path contains /gallery/media/: {} | contains /file: {}",
            path.contains("/gallery/media/"), path.contains("/file"));
        if (path.contains("/gallery/media/") && path.contains("/file")) {
            log.info("✅📁 FILE ENDPOINT BYPASSED - forwarding to Gallery Service (JWT in query param)");
            log.info("   Path: {} | Auth header presence: {}", path, !exchange.getRequest().getHeaders().getOrEmpty("Authorization").isEmpty());
            return chain.filter(exchange);
        }

        // Extract Authorization header
        List<String> authHeaders = exchange.getRequest().getHeaders().getOrEmpty("Authorization");
        log.info("📋 Authorization headers present: {}", !authHeaders.isEmpty());

        if (authHeaders.isEmpty()) {
            log.warn("❌ No Authorization header for protected route: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String authHeader = authHeaders.get(0);

        // Validate Bearer token format
        if (!authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        try {
            // Validate JWT token
            if (!jwtUtil.validateToken(token)) {
                log.warn("❌ Invalid token for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Extract role and check authorization
            String role = jwtUtil.extractRole(token);
            log.info("👤 Token role: {}", role);

            if (role == null || role.isBlank()) {
                log.warn("❌ No role claim in token");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Check if user has required role for the endpoint
            if (!isAuthorized(path, role)) {
                log.warn("❌ User with role {} not authorized for path: {}", role, path);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            log.info("✅ Authorization successful for path: {} with role: {}", path, role);

            // Extract user context from token and add as request headers for downstream services
            String userId = jwtUtil.extractUserId(token);
            String email = jwtUtil.extractEmail(token);

            log.info("📤 Adding user context headers - userId: {}, email: {}", userId, email);

            // Mutate the exchange to add user context headers for downstream services
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-User-Id", userId != null ? userId : "")
                            .header("X-User-Email", email != null ? email : "")
                            .build())
                    .build();

            log.info("✅ User context headers added, forwarding to downstream service");
            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            log.error("❌ Auth filter exception: {}", e.getMessage(), e);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * Check if path is a public/open API endpoint
     */
    private boolean isOpenApi(String path) {
        // Use startsWith for exact path matching
        boolean isOpen = openApiEndpoints.stream().anyMatch(endpoint -> path.startsWith(endpoint));
        log.debug("🔍 Checking if open API - Path: {} | Is Open: {}", path, isOpen);
        return isOpen;
    }

    /**
     * Check if user role is authorized to access the endpoint
     */
    private boolean isAuthorized(String path, String role) {
        for (Map.Entry<String, List<String>> entry : routeRoleMap.entrySet()) {
            String route = entry.getKey();
            List<String> allowedRoles = entry.getValue();

            if (path.startsWith(route)) {
                return allowedRoles.contains(role.toUpperCase());
            }
        }
        // If route not in map, allow access (permit by default for new routes)
        return true;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
