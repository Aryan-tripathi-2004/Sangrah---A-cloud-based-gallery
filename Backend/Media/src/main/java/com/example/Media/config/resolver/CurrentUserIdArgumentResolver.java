package com.example.Media.config.resolver;

import com.example.Media.api.annotation.CurrentUserId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} implementation that extracts and validates
 * the {@code X-User-Id} request header for controller method parameters annotated
 * with {@link CurrentUserId}.
 *
 * <p>This resolver centralises the authentication header extraction logic that was
 * previously duplicated across every controller method via manual
 * {@code HttpServletRequest.getHeader("X-User-Id")} calls.</p>
 *
 * <p>Behaviour:
 * <ul>
 *   <li>If {@code @CurrentUserId(required = true)} (default) and the header is absent
 *       or blank, a {@link MissingRequestHeaderException} is thrown. The
 *       {@code GlobalExceptionHandler} converts this to a 401 {@code ProblemDetail}.</li>
 *   <li>If {@code @CurrentUserId(required = false)} and the header is absent or blank,
 *       {@code null} is returned — allowing optional authentication flows.</li>
 * </ul>
 * </p>
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String X_USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) &&
               parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
        boolean required = annotation == null || annotation.required();

        String userId = webRequest.getHeader(X_USER_ID_HEADER);
        if (required && (userId == null || userId.trim().isEmpty())) {
            throw new MissingRequestHeaderException(X_USER_ID_HEADER, parameter);
        }
        if (userId != null && userId.trim().isEmpty()) {
            return null;
        }
        return userId;
    }
}
