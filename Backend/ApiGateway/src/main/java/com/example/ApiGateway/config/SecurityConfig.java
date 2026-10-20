package com.example.ApiGateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security Configuration for API Gateway.
 *
 * <p>CORS is handled natively via a {@link CorsWebFilter} bean configured from
 * {@link GatewaySecurityProperties}, replacing the previous custom
 * {@code com.example.ApiGateway.security.CorsWebFilter}.</p>
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewaySecurityProperties securityProperties;

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        // Allow ALL requests - AuthenticationFilter will validate JWT
                        .anyExchange().permitAll()
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }

    /**
     * Native Spring WebFlux CORS filter configured from externalized properties.
     * Replaces the manually coded CorsWebFilter in the security package.
     *
     * @return a {@link CorsWebFilter} registered as a Spring bean
     */
    @Bean
    CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins from externalized configuration
        config.setAllowedOrigins(securityProperties.getAllowedOrigins());

        // Allowed HTTP methods
        config.setAllowedMethods(securityProperties.getAllowedMethods());

        // Allowed request headers
        config.setAllowedHeaders(securityProperties.getAllowedHeaders());

        // Exposed response headers
        config.setExposedHeaders(securityProperties.getExposedHeaders());

        // Allow credentials (cookies, Authorization header)
        config.setAllowCredentials(securityProperties.isAllowCredentials());

        // Preflight response cache duration in seconds
        config.setMaxAge(securityProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(securityProperties.getCorsPathPattern(), config);

        return new CorsWebFilter(source);
    }
}
