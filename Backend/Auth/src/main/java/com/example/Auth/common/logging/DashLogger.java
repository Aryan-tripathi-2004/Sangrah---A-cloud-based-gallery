package com.example.Auth.common.logging;

import java.util.Map;

/**
 * Interface for the DashLogger - a structured logging wrapper that provides
 * enhanced logging capabilities including correlation IDs, request context,
 * method entry/exit logging, and sensitive data masking.
 */
public interface DashLogger {

    /**
     * Log an informational message.
     *
     * @param message the log message
     */
    void info(String message);

    /**
     * Log an informational message with additional context.
     *
     * @param message the log message
     * @param context additional context as key-value pairs
     */
    void info(String message, Map<String, Object> context);

    /**
     * Log a debug message.
     *
     * @param message the log message
     */
    void debug(String message);

    /**
     * Log a debug message with additional context.
     *
     * @param message the log message
     * @param context additional context as key-value pairs
     */
    void debug(String message, Map<String, Object> context);

    /**
     * Log a warning message.
     *
     * @param message the log message
     */
    void warn(String message);

    /**
     * Log a warning message with additional context.
     *
     * @param message the log message
     * @param context additional context as key-value pairs
     */
    void warn(String message, Map<String, Object> context);

    /**
     * Log a warning message with an exception.
     *
     * @param message   the log message
     * @param throwable the exception
     */
    void warn(String message, Throwable throwable);

    /**
     * Log an error message.
     *
     * @param message the log message
     */
    void error(String message);

    /**
     * Log an error message with additional context.
     *
     * @param message the log message
     * @param context additional context as key-value pairs
     */
    void error(String message, Map<String, Object> context);

    /**
     * Log an error message with an exception.
     *
     * @param message   the log message
     * @param throwable the exception
     */
    void error(String message, Throwable throwable);

    /**
     * Log an error message with an exception and additional context.
     *
     * @param message   the log message
     * @param throwable the exception
     * @param context   additional context as key-value pairs
     */
    void error(String message, Throwable throwable, Map<String, Object> context);

    /**
     * Log method entry with arguments.
     *
     * @param methodName the method name
     * @param args       the method arguments
     */
    void logMethodEntry(String methodName, Object... args);

    /**
     * Log method exit with return value.
     *
     * @param methodName  the method name
     * @param returnValue the return value
     * @param durationMs  the method execution duration in milliseconds
     */
    void logMethodExit(String methodName, Object returnValue, long durationMs);

    /**
     * Log method exit with exception.
     *
     * @param methodName the method name
     * @param throwable  the exception
     * @param durationMs the method execution duration in milliseconds
     */
    void logMethodException(String methodName, Throwable throwable, long durationMs);

    /**
     * Log a step within a method execution.
     *
     * @param step    the step description
     * @param message the log message
     */
    void logStep(String step, String message);

    /**
     * Log a step within a method execution with additional context.
     *
     * @param step    the step description
     * @param message the log message
     * @param context additional context as key-value pairs
     */
    void logStep(String step, String message, Map<String, Object> context);
}
