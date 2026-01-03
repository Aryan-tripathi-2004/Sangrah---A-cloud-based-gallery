package com.example.Auth.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Factory for creating DashLogger instances.
 */
public class DashLoggerFactory {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    private DashLoggerFactory() {
        throw new UnsupportedOperationException("Cannot instantiate factory class");
    }

    /**
     * Create a DashLogger for the given class.
     *
     * @param clazz the class to create logger for
     * @return a new DashLogger instance
     */
    public static DashLogger getLogger(Class<?> clazz) {
        return new DashLoggerImpl(clazz, objectMapper);
    }

    /**
     * Set a custom ObjectMapper for JSON serialization.
     *
     * @param mapper the ObjectMapper to use
     */
    public static void setObjectMapper(ObjectMapper mapper) {
        objectMapper = mapper;
    }
}
