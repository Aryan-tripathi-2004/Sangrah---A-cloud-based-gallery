package com.example.ApiGateway.security;

import com.example.ApiGateway.config.GatewaySecurityProperties;
import com.example.ApiGateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
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

    private static final String INTERNAL_API_PREFIX = "/internal/";
    private static final String HEADER_TOKEN_EXPIRED = "Token-Expired";

    private final JwtUtil jwtUtil;
    private final GatewaySecurityProperties securityProperties;

    /** Internal header names that only the gateway may set. */
    @Value("${gateway.security.header.user-id}")
    private String headerUserId = "X-User-Id";

    @Value("${gateway.security.header.user-email}")
    private String headerUserEmail = "X-User-Email";

    @Value("${gateway.security.auth.bearer-prefix}")
    private String bearerPrefix = "Bearer ";

    @Value("${gateway.security.auth.token-query-param}")
    private String tokenQueryParam = "token";

    @Value("${gateway.security.jwt.expected-type}")
    private String expectedTokenType = "ACCESS";

    @Value("${gateway.security.filter-order}")
    private int filterOrder = -1;

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
                    headers.remove(headerUserId);
                    headers.remove(headerUserEmail);
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

        // Refuse external ingress to internal-only endpoints (BFLA perimeter guard)
        if (path.startsWith(INTERNAL_API_PREFIX) || rawPath.startsWith(INTERNAL_API_PREFIX)) {
            log.warn("🚨 External ingress to internal endpoint refused: {}", path);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "External access to internal endpoints is strictly prohibited.");
        }

        // Allow public endpoints without authentication
        if (isOpenApi(path)) {
            log.info("✅ Public endpoint allowed: {}", path);
            return chain.filter(sanitizedExchange);
        }

        // Extract Authorization header or token query parameter
        String token = null;
        List<String> authHeaders = sanitizedRequest.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);

        if (!authHeaders.isEmpty() && authHeaders.get(0).startsWith(bearerPrefix)) {
            token = authHeaders.get(0).substring(bearerPrefix.length());
            log.info("📋 Authorization header present");
        } else {
            // Check query parameter (used by <img> and <video> tags where Authorization header cannot be set)
            token = sanitizedRequest.getQueryParams().getFirst(tokenQueryParam);
            if (token != null && !token.isBlank()) {
                log.info("📋 Token query parameter present");
            }
        }

        if (token == null || token.isBlank()) {
            log.warn("❌ No token found for protected route: {}", path);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token");
        }

        try {
            // Cryptographically verify and parse JWT token EXACTLY ONCE
            Claims claims = jwtUtil.extractAllClaims(token);

            // Strictly enforce token type isolation (API7:2023 Token Confusion Prevention)
            String tokenType = jwtUtil.extractTokenType(claims);
            if (!expectedTokenType.equals(tokenType)) {
                log.warn("🚨 Invalid token type: {}. Expected: {} for path: {}", tokenType, expectedTokenType, path);
                throw new JwtException("Invalid token type. Only " + expectedTokenType + " tokens are permitted.");
            }

            // Extract role and check authorization from pre-parsed claims
            String role = jwtUtil.extractRole(claims);
            log.info("👤 Token role: {}", role);

            if (role == null || role.isBlank()) {
                log.warn("❌ No role claim in token");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No role claim in token");
            }

            // Check if user has required role for the endpoint
            if (!isAuthorized(path, role)) {
                log.warn("❌ User with role {} not authorized for path: {}", role, path);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User with role " + role + " not authorized for path");
            }

            log.info("✅ Authorization successful for path: {} with role: {}", path, role);

            // Extract user context from pre-parsed claims and add as trusted request headers
            String userId = jwtUtil.extractUserId(claims);
            String email = jwtUtil.extractEmail(claims);

            log.info("📤 Adding trusted user context headers - userId: {}, email: {}", userId, email);

            // Mutate the sanitized request to append gateway-trusted identity headers
            ServerHttpRequest enrichedRequest = sanitizedRequest.mutate()
                    .header(headerUserId, userId != null ? userId : "")
                    .header(headerUserEmail, email != null ? email : "")
                    .build();

            ServerWebExchange enrichedExchange = sanitizedExchange.mutate()
                    .request(enrichedRequest)
                    .build();

            log.info("✅ User context headers added, forwarding to downstream service");
            return chain.filter(enrichedExchange);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (ExpiredJwtException e) {
            log.warn("⌛ Token expired for path: {}", path);
            ResponseStatusException exception = new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "The provided authentication token has expired.", e);
            exception.getResponseHeaders().add(HEADER_TOKEN_EXPIRED, "true");
            throw exception;
        } catch (JwtException e) {
            log.error("❌ Cryptographic token failure for path: {} | {}", path, e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token.", e);
        } catch (Exception e) {
            log.error("❌ Auth filter exception: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication processing failed.", e);
        }
    }

    /**
     * Check if path is a public/open API endpoint.
     * Uses externalized configuration from {@link GatewaySecurityProperties}.
     */
    private boolean isOpenApi(String path) {
        List<String> openEndpoints = securityProperties.getOpenEndpoints();
        if (openEndpoints == null || openEndpoints.isEmpty() || path == null) {
            return false;
        }

        for (String endpoint : openEndpoints) {
            if (endpoint == null || endpoint.isBlank()) {
                continue;
            }
            String clean = endpoint.trim().replaceAll("/+$", "");
            if (path.equals(clean) || path.startsWith(clean + "/")) {
                log.debug("🔍 Open API matched: {} for path: {}", clean, path);
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user role is authorized to access the endpoint.
     * Uses externalized configuration from {@link GatewaySecurityProperties}.
     */
    private boolean isAuthorized(String path, String role) {
        Map<String, List<String>> routeRoles = securityProperties.getRouteRoles();
        if (routeRoles == null || routeRoles.isEmpty() || path == null) {
            // No route-role restrictions configured — permit by default
            return true;
        }

        for (Map.Entry<String, List<String>> entry : routeRoles.entrySet()) {
            String route = entry.getKey();
            List<String> allowedRoles = entry.getValue();
            if (route == null || route.isBlank()) {
                continue;
            }
            String cleanRoute = route.trim().replaceAll("/+$", "");
            if (path.equals(cleanRoute) || path.startsWith(cleanRoute + "/")) {
                return allowedRoles != null && allowedRoles.contains(role.toUpperCase());
            }
        }
        // If route not in map, allow access (permit by default for new routes)
        return true;
    }

    @Override
    public int getOrder() {
        return filterOrder;
    }
}
