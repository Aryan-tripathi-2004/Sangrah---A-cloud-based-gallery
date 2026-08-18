package com.example.Event.config.resolver;

import com.example.Event.api.annotation.CurrentUserId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * HandlerMethodArgumentResolver implementation to extract and validate the X-User-Id header
 * for controller method parameters annotated with @CurrentUserId.
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
