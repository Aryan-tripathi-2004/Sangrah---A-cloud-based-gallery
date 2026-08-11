package com.example.Notification.api.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves the @CurrentUserId annotation by extracting the X-User-Id header.
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String X_USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) && 
               parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        
        String userId = webRequest.getHeader(X_USER_ID_HEADER);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new MissingRequestHeaderException(X_USER_ID_HEADER, parameter);
        }
        
        return userId;
    }
}
