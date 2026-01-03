package com.example.Auth.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard API response wrapper for all REST endpoints.
 *
 * @param <T> The type of data in the response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseWrapper<T> {

    private boolean success;
    private String message;
    private T data;
    private Integer errorCode;
    private Instant timestamp;

    public ResponseWrapper() {
        this.timestamp = Instant.now();
    }

    public ResponseWrapper(boolean success, String message, T data) {
        this();
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public ResponseWrapper(boolean success, String message, T data, Integer errorCode) {
        this(success, message, data);
        this.errorCode = errorCode;
    }

    /**
     * Creates a successful response with data.
     *
     * @param data    The response data
     * @param message The success message
     * @param <T>     The type of data
     * @return ApiResponse with success status
     */
    public static <T> ResponseWrapper<T> success(T data, String message) {
        return new ResponseWrapper<>(true, message, data);
    }

    /**
     * Creates a successful response with data and default message.
     *
     * @param data The response data
     * @param <T>  The type of data
     * @return ApiResponse with success status
     */
    public static <T> ResponseWrapper<T> success(T data) {
        return new ResponseWrapper<>(true, "Success", data);
    }

    /**
     * Creates a successful response without data.
     *
     * @param message The success message
     * @param <T>     The type of data
     * @return ApiResponse with success status
     */
    public static <T> ResponseWrapper<T> success(String message) {
        return new ResponseWrapper<>(true, message, null);
    }

    /**
     * Creates an error response.
     *
     * @param message   The error message
     * @param errorCode The HTTP status code
     * @param <T>       The type of data
     * @return ApiResponse with error status
     */
    public static <T> ResponseWrapper<T> error(String message, Integer errorCode) {
        return new ResponseWrapper<>(false, message, null, errorCode);
    }

    /**
     * Creates an error response with data.
     *
     * @param message   The error message
     * @param data      The error data (e.g., validation errors)
     * @param errorCode The HTTP status code
     * @param <T>       The type of data
     * @return ApiResponse with error status
     */
    public static <T> ResponseWrapper<T> error(String message, T data, Integer errorCode) {
        return new ResponseWrapper<>(false, message, data, errorCode);
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", errorCode=" + errorCode +
                ", timestamp=" + timestamp +
                '}';
    }
}
