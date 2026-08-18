package com.example.ApiGateway.security;

import com.example.ApiGateway.config.GatewaySecurityProperties;
import com.example.ApiGateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Global authentication and authorization filter for API Gateway.
 *
 * <p>Validates JWT tokens, enforces role-based access control, and propagates
 * trusted user-context headers ({@code X-User-Id}, {@code X-User-Email}) to
 * downstream services.</p>
 *
 * <h3>Security: Header Spoofing Prevention</h3>
 * <p>The <strong>very first action</strong> in this filter is to strip any
 * client-supplied {@code X-User-Id} and {@code X-User-Email} headers.
 * Only the Gateway is authorised to set these after JWT validation.</p>
 *
 * <p>Open endpoints and route-role mappings are sourced from
 * {@link GatewaySecurityProperties} (12-Factor externalized config).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final GatewaySecurityProperties securityProperties;

    /** Internal header names that only the gateway may set. */
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // ====================================================================
        // CRITICAL SECURITY PATCH: Strip any client-supplied identity headers.
        // These headers must ONLY be set by the gateway after JWT validation.
        // This prevents header-spoofing attacks where a malicious client injects
        // X-User-Id / X-User-Email to impersonate another user.
        // ====================================================================
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_EMAIL);
                })
                .build();

        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(sanitizedRequest)
                .build();

        // All subsequent logic operates on the sanitized exchange/request
        String path = sanitizedRequest.getPath().toString();
        String rawPath = sanitizedRequest.getURI().getRawPath();
        String fullPath = sanitizedRequest.getURI().toString();

        log.info("🔐 AuthenticationFilter - Normalized Path: {} | Raw Path: {}", path, rawPath);
        log.debug("   Full URI: {}", fullPath);

        // Allow public endpoints without authentication
        if (isOpenApi(path)) {
            log.info("✅ Public endpoint allowed: {}", path);
            return chain.filter(sanitizedExchange);
        }

        // Extract Authorization header or token query parameter
        String token = null;
        List<String> authHeaders = sanitizedRequest.getHeaders().getOrEmpty("Authorization");

        if (!authHeaders.isEmpty() && authHeaders.get(0).startsWith("Bearer ")) {
            token = authHeaders.get(0).substring(7);
            log.info("📋 Authorization header present");
        } else {
            // Check query parameter (used by <img> and <video> tags where Authorization header cannot be set)
            token = sanitizedRequest.getQueryParams().getFirst("token");
            if (token != null && !token.isBlank()) {
                log.info("📋 Token query parameter present");
            }
        }

        if (token == null || token.isBlank()) {
            log.warn("❌ No token found for protected route: {}", path);
            sanitizedExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return sanitizedExchange.getResponse().setComplete();
        }

        try {
            // Validate JWT token
            if (!jwtUtil.validateToken(token)) {
                log.warn("❌ Invalid token for path: {}", path);
                sanitizedExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return sanitizedExchange.getResponse().setComplete();
            }

            // Extract role and check authorization
            String role = jwtUtil.extractRole(token);
            log.info("👤 Token role: {}", role);

            if (role == null || role.isBlank()) {
                log.warn("❌ No role claim in token");
                sanitizedExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return sanitizedExchange.getResponse().setComplete();
            }

            // Check if user has required role for the endpoint
            if (!isAuthorized(path, role)) {
                log.warn("❌ User with role {} not authorized for path: {}", role, path);
                sanitizedExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return sanitizedExchange.getResponse().setComplete();
            }

            log.info("✅ Authorization successful for path: {} with role: {}", path, role);

            // Extract user context from token and add as trusted request headers
            String userId = jwtUtil.extractUserId(token);
            String email = jwtUtil.extractEmail(token);

            log.info("📤 Adding trusted user context headers - userId: {}, email: {}", userId, email);

            // Mutate the sanitized request to append gateway-trusted identity headers
            ServerHttpRequest enrichedRequest = sanitizedRequest.mutate()
                    .header(HEADER_USER_ID, userId != null ? userId : "")
                    .header(HEADER_USER_EMAIL, email != null ? email : "")
                    .build();

            ServerWebExchange enrichedExchange = sanitizedExchange.mutate()
                    .request(enrichedRequest)
                    .build();

            log.info("✅ User context headers added, forwarding to downstream service");
            return chain.filter(enrichedExchange);

        } catch (Exception e) {
            log.error("❌ Auth filter exception: {}", e.getMessage(), e);
            sanitizedExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return sanitizedExchange.getResponse().setComplete();
        }
    }

    /**
     * Check if path is a public/open API endpoint.
     * Uses externalized configuration from {@link GatewaySecurityProperties}.
     */
    private boolean isOpenApi(String path) {
        List<String> openEndpoints = securityProperties.getOpenEndpoints();
        if (openEndpoints == null) {
            return false;
        }
        boolean isOpen = openEndpoints.stream().anyMatch(path::startsWith);
        log.debug("🔍 Checking if open API - Path: {} | Is Open: {}", path, isOpen);
        return isOpen;
    }

    /**
     * Check if user role is authorized to access the endpoint.
     * Uses externalized configuration from {@link GatewaySecurityProperties}.
     */
    private boolean isAuthorized(String path, String role) {
        Map<String, List<String>> routeRoles = securityProperties.getRouteRoles();
        if (routeRoles == null) {
            // No route-role restrictions configured — permit by default
            return true;
        }

        for (Map.Entry<String, List<String>> entry : routeRoles.entrySet()) {
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
