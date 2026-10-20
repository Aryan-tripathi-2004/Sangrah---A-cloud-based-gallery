package com.example.Notification.api.annotation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import org.springframework.stereotype.Component;

/**
 * Resolves the @CurrentUserId annotation by extracting the X-User-Id header.
 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Value("${notification.header.user-id}")
    private String userIdHeader;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) && 
               parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        
        String userId = webRequest.getHeader(userIdHeader);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new MissingRequestHeaderException(userIdHeader, parameter);
        }
        
        return userId;
    }
}
