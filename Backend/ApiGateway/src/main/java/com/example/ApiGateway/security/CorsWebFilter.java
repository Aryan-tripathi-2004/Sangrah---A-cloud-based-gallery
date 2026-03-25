package com.example.ApiGateway.security;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * CORS WebFilter - Handles CORS preflight requests BEFORE authentication
 * Order = -2 ensures this runs BEFORE AuthenticationFilter (order = -1)
 * This filter adds CORS headers to all responses and responds to preflight OPTIONS requests
 */
@Component
@Order(-2)
public class CorsWebFilter implements WebFilter {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:4200",
            "http://localhost:3000",
            "http://127.0.0.1:4200",
            "http://127.0.0.1:3000"
    );

    private static final String ALLOWED_HEADERS = "Origin, X-Requested-With, Content-Type, Accept, Authorization, X-CSRF-TOKEN";
    private static final String ALLOWED_METHODS = "GET, HEAD, POST, PUT, DELETE, OPTIONS, PATCH";
    private static final String EXPOSED_HEADERS = "Authorization, Content-Type";
    private static final String MAX_AGE = "3600";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String originHeader = exchange.getRequest().getHeaders().getOrigin();

        // Use origin if provided, otherwise use localhost for development
        String origin = (originHeader != null && isOriginAllowed(originHeader))
            ? originHeader
            : "http://localhost:4200";  // Default for development

        ServerWebExchange corsExchange = addCorsHeaders(exchange, origin);

        // Handle CORS preflight request immediately
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            corsExchange.getResponse().setStatusCode(HttpStatus.OK);
            return corsExchange.getResponse().setComplete();
        }

        // For non-preflight requests, continue the filter chain
        return chain.filter(corsExchange);
    }

    /**
     * Check if the origin is in the allowed list
     */
    private boolean isOriginAllowed(String origin) {
        return ALLOWED_ORIGINS.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(origin));
    }

    /**
     * Add CORS headers to the response
     * Echo back the requesting origin (not all origins)
     */
    private ServerWebExchange addCorsHeaders(ServerWebExchange exchange, String origin) {
        exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", origin);
        exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", ALLOWED_METHODS);
        exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        exchange.getResponse().getHeaders().add("Access-Control-Expose-Headers", EXPOSED_HEADERS);
        exchange.getResponse().getHeaders().add("Access-Control-Allow-Credentials", "true");
        exchange.getResponse().getHeaders().add("Access-Control-Max-Age", MAX_AGE);
        return exchange;
    }
}

