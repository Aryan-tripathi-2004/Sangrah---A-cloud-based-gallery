package com.example.Auth.security;

import com.example.Auth.common.constants.HttpConstants;
import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import com.example.Auth.common.request.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter that intercepts requests and validates JWT tokens.
 * Extracts user information from valid tokens and sets up Spring Security
 * context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final DashLogger logger = DashLoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Initialize request context for logging
            initializeRequestContext(request);

            // Extract JWT token from request
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                // Extract user information from token
                UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                List<String> roles = jwtTokenProvider.getRolesFromToken(jwt);
                UUID sessionId = jwtTokenProvider.getSessionIdFromToken(jwt);

                // Convert roles to Spring Security authorities
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId.toString(), // Principal is user ID
                        null, // No credentials needed after authentication
                        authorities);

                // Set additional details
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Update request context with user information
                RequestContext.get().setUserId(userId.toString());

                logger.debug("JWT authentication successful",
                        java.util.Map.of("userId", userId, "username", username, "sessionId", sessionId));

            } else if (jwt != null) {
                logger.warn("Invalid JWT token");
            }

        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
            // Continue with unauthenticated request
        }

        // Continue filter chain
        filterChain.doFilter(request, response);

        // Clear request context after request processing
        RequestContext.clear();
    }

    /**
     * Extract JWT token from Authorization header.
     *
     * @param request the HTTP request
     * @return the JWT token or null if not present
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpConstants.HEADER_AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Initialize request context with request information.
     *
     * @param request the HTTP request
     */
    private void initializeRequestContext(HttpServletRequest request) {
        String requestId = request.getHeader(HttpConstants.HEADER_REQUEST_ID);
        String correlationId = request.getHeader(HttpConstants.HEADER_CORRELATION_ID);
        String traceId = request.getHeader(HttpConstants.HEADER_TRACE_ID);

        RequestContext.RequestContextData contextData = RequestContext.RequestContextData.builder()
                .requestId(requestId)
                .correlationId(correlationId)
                .traceId(traceId)
                .remoteIp(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .build();

        RequestContext.initialize(contextData);
    }

    /**
     * Get client IP address from request, considering X-Forwarded-For header.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
