package com.example.Cloud.config;

import com.example.Cloud.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Configures stateless JWT-based security for the Cloud microservice.
     * All requests to {@code /api/cloud/**} require a valid JWT Bearer token
     * (issued by the Auth service). Missing or invalid tokens result in a
     * 401 Unauthorized response, NOT a 403 Forbidden – the distinction being
     * that 403 was previously returned because Spring Security's default config
     * blocked authenticated requests that lacked the required authorities.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection is disabled because this API uses stateless JWT Bearer-token
                // authentication. Tokens are sent in the Authorization header, not as cookies,
                // so CSRF attacks (which rely on automatic cookie submission by the browser)
                // do not apply.
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
