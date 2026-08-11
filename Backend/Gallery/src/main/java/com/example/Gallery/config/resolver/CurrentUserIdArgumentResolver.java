package com.example.Gallery.config.resolver;

import com.example.Gallery.api.annotation.CurrentUserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Value("${jwt.secret:sangrah_secret_key_for_jwt_token_validation_please_change_in_production}")
    private String jwtSecret;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) &&
               parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new RuntimeException("Request is null");
        }

        // Try header first
        String userId = request.getHeader("X-User-Id");
        
        // Fallback to token query param (e.g. for image/video tags in HTML)
        if (userId == null || userId.trim().isEmpty()) {
            String token = request.getParameter("token");
            if (token != null && !token.trim().isEmpty()) {
                userId = validateTokenAndGetUserId(token);
            } else {
                throw new RuntimeException("User ID not found in context (missing X-User-Id header and token parameter)");
            }
        }
        
        return userId;
    }

    private String validateTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            if (userId == null || userId.isEmpty()) {
                throw new RuntimeException("User ID not found in token");
            }
            return userId;
        } catch (Exception e) {
            log.error("❌ JWT validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid token: " + e.getMessage());
        }
    }
}
