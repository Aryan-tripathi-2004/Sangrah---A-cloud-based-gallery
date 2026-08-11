package com.example.Media.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to resolve the current user's ID from the {@code X-User-Id}
 * request header, populated by the API Gateway after JWT validation.
 *
 * <p>When placed on a controller method parameter of type {@code String}, the
 * {@link com.example.Media.config.resolver.CurrentUserIdArgumentResolver} extracts
 * and validates the header value before the controller body executes.</p>
 *
 * <p>If {@link #required()} is {@code true} (default) and the header is missing or
 * blank, a {@link org.springframework.web.bind.MissingRequestHeaderException} is
 * thrown — which the {@code GlobalExceptionHandler} converts to a strict RFC-9457
 * {@code ProblemDetail} response.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
    boolean required() default true;
}
