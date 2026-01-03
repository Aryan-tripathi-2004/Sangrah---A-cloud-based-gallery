package com.example.Auth.common.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Auth.common.request.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * Default implementation of DashLogger that wraps SLF4J and provides
 * structured JSON logging with request context, correlation IDs, and
 * sensitive data masking.
 */
public class DashLoggerImpl implements DashLogger {

    private final Logger logger;
    private final ObjectMapper objectMapper;
    private final String component;

    public DashLoggerImpl(Class<?> clazz, ObjectMapper objectMapper) {
        this.logger = LoggerFactory.getLogger(clazz);
        this.objectMapper = objectMapper;
        this.component = clazz.getSimpleName();
    }

    @Override
    public void info(String message) {
        info(message, null);
    }

    @Override
    public void info(String message, Map<String, Object> context) {
        if (logger.isInfoEnabled()) {
            enrichMDC();
            if (context != null && !context.isEmpty()) {
                logger.info("{} | {}", message, serializeContext(context));
            } else {
                logger.info(message);
            }
            clearMDC();
        }
    }

    @Override
    public void debug(String message) {
        debug(message, null);
    }

    @Override
    public void debug(String message, Map<String, Object> context) {
        if (logger.isDebugEnabled()) {
            enrichMDC();
            if (context != null && !context.isEmpty()) {
                logger.debug("{} | {}", message, serializeContext(context));
            } else {
                logger.debug(message);
            }
            clearMDC();
        }
    }

    @Override
    public void warn(String message) {
        warn(message, (Map<String, Object>) null);
    }

    @Override
    public void warn(String message, Map<String, Object> context) {
        enrichMDC();
        if (context != null && !context.isEmpty()) {
            logger.warn("{} | {}", message, serializeContext(context));
        } else {
            logger.warn(message);
        }
        clearMDC();
    }

    @Override
    public void warn(String message, Throwable throwable) {
        enrichMDC();
        logger.warn(message, throwable);
        clearMDC();
    }

    @Override
    public void error(String message) {
        error(message, (Map<String, Object>) null);
    }

    @Override
    public void error(String message, Map<String, Object> context) {
        enrichMDC();
        if (context != null && !context.isEmpty()) {
            logger.error("{} | {}", message, serializeContext(context));
        } else {
            logger.error(message);
        }
        clearMDC();
    }

    @Override
    public void error(String message, Throwable throwable) {
        error(message, throwable, null);
    }

    @Override
    public void error(String message, Throwable throwable, Map<String, Object> context) {
        enrichMDC();
        if (context != null && !context.isEmpty()) {
            logger.error("{} | {}", message, serializeContext(context), throwable);
        } else {
            logger.error(message, throwable);
        }
        clearMDC();
    }

    @Override
    public void logMethodEntry(String methodName, Object... args) {
        if (logger.isDebugEnabled()) {
            enrichMDC();
            Map<String, Object> context = new HashMap<>();
            context.put("method", methodName);
            context.put("event", "METHOD_ENTRY");

            if (args != null && args.length > 0) {
                Object[] maskedArgs = maskSensitiveData(args);
                context.put("arguments", maskedArgs);
            }

            logger.debug("Entering method {} | {}", methodName, serializeContext(context));
            clearMDC();
        }
    }

    @Override
    public void logMethodExit(String methodName, Object returnValue, long durationMs) {
        if (logger.isDebugEnabled()) {
            enrichMDC();
            Map<String, Object> context = new HashMap<>();
            context.put("method", methodName);
            context.put("event", "METHOD_EXIT");
            context.put("durationMs", durationMs);

            if (returnValue != null) {
                Object maskedReturn = maskSensitiveData(returnValue);
                context.put("returnValue", maskedReturn);
            }

            logger.debug("Exiting method {} after {}ms | {}", methodName, durationMs, serializeContext(context));
            clearMDC();
        }
    }

    @Override
    public void logMethodException(String methodName, Throwable throwable, long durationMs) {
        enrichMDC();
        Map<String, Object> context = new HashMap<>();
        context.put("method", methodName);
        context.put("event", "METHOD_EXCEPTION");
        context.put("durationMs", durationMs);
        context.put("exceptionType", throwable.getClass().getName());

        logger.error("Exception in method {} after {}ms | {}", methodName, durationMs, serializeContext(context),
                throwable);
        clearMDC();
    }

    @Override
    public void logStep(String step, String message) {
        logStep(step, message, null);
    }

    @Override
    public void logStep(String step, String message, Map<String, Object> context) {
        if (logger.isDebugEnabled()) {
            enrichMDC();
            Map<String, Object> enrichedContext = new HashMap<>();
            enrichedContext.put("step", step);
            if (context != null) {
                enrichedContext.putAll(context);
            }

            logger.debug("[STEP: {}] {} | {}", step, message, serializeContext(enrichedContext));
            clearMDC();
        }
    }

    /**
     * Enrich MDC with request context information.
     */
    private void enrichMDC() {
        RequestContext.RequestContextData contextData = RequestContext.get();
        if (contextData != null) {
            putIfNotNull("requestId", contextData.getRequestId());
            putIfNotNull("correlationId", contextData.getCorrelationId());
            putIfNotNull("traceId", contextData.getTraceId());
            putIfNotNull("userId", contextData.getUserId());
            putIfNotNull("tenantId", contextData.getTenantId());
            putIfNotNull("remoteIp", contextData.getRemoteIp());
        }
        MDC.put("component", component);
        MDC.put("timestamp", Instant.now().toString());
    }

    /**
     * Clear MDC.
     */
    private void clearMDC() {
        MDC.clear();
    }

    /**
     * Put value in MDC if not null.
     */
    private void putIfNotNull(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }

    /**
     * Serialize context map to JSON string.
     */
    private String serializeContext(Map<String, Object> context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return context.toString();
        }
    }

    /**
     * Mask sensitive data in objects based on @Sensitive annotations.
     */
    private Object maskSensitiveData(Object obj) {
        if (obj == null) {
            return null;
        }

        // Handle arrays
        if (obj.getClass().isArray()) {
            return Arrays.stream((Object[]) obj)
                    .map(this::maskSensitiveData)
                    .toArray();
        }

        // Handle primitives and common types
        if (isPrimitiveOrWrapper(obj.getClass()) || obj instanceof String) {
            return obj;
        }

        // Handle objects with @Sensitive fields
        try {
            Map<String, Object> maskedObject = new HashMap<>();
            Field[] fields = obj.getClass().getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);

                if (field.isAnnotationPresent(Sensitive.class)) {
                    Sensitive sensitive = field.getAnnotation(Sensitive.class);
                    maskedObject.put(field.getName(), maskValue(value, sensitive));
                } else {
                    maskedObject.put(field.getName(), value);
                }
            }

            return maskedObject;
        } catch (Exception e) {
            // If we can't introspect, return string representation
            return obj.toString();
        }
    }

    /**
     * Mask sensitive data in an array of objects.
     */
    private Object[] maskSensitiveData(Object[] objects) {
        Object[] masked = new Object[objects.length];
        for (int i = 0; i < objects.length; i++) {
            masked[i] = maskSensitiveData(objects[i]);
        }
        return masked;
    }

    /**
     * Mask a value based on Sensitive annotation configuration.
     */
    private String maskValue(Object value, Sensitive sensitive) {
        if (value == null) {
            return null;
        }

        String strValue = value.toString();
        int length = strValue.length();
        int showFirst = sensitive.showFirst();
        int showLast = sensitive.showLast();
        char maskChar = sensitive.maskChar();

        if (showFirst + showLast >= length) {
            return strValue;
        }

        StringBuilder masked = new StringBuilder();

        // Add first characters
        if (showFirst > 0) {
            masked.append(strValue, 0, showFirst);
        }

        // Add mask characters
        int maskLength = length - showFirst - showLast;
        for (int i = 0; i < maskLength; i++) {
            masked.append(maskChar);
        }

        // Add last characters
        if (showLast > 0) {
            masked.append(strValue.substring(length - showLast));
        }

        return masked.toString();
    }

    /**
     * Check if a class is a primitive or wrapper type.
     */
    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == Boolean.class ||
                type == Integer.class ||
                type == Long.class ||
                type == Float.class ||
                type == Double.class ||
                type == Byte.class ||
                type == Short.class ||
                type == Character.class;
    }
}
