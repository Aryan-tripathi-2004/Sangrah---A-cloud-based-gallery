package com.example.Auth.common.constants;

/**
 * Constants for HTTP headers, status codes, and related HTTP operations.
 */
public final class HttpConstants {

    private HttpConstants() {
        throw new UnsupportedOperationException("Cannot instantiate constants class");
    }

    // HTTP Headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_BEARER_PREFIX = "Bearer ";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";

    // Content Types
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";

    // HTTP Status Messages
    public static final String STATUS_SUCCESS = "Success";
    public static final String STATUS_CREATED = "Created";
    public static final String STATUS_BAD_REQUEST = "Bad Request";
    public static final String STATUS_UNAUTHORIZED = "Unauthorized";
    public static final String STATUS_FORBIDDEN = "Forbidden";
    public static final String STATUS_NOT_FOUND = "Not Found";
    public static final String STATUS_CONFLICT = "Conflict";
    public static final String STATUS_TOO_MANY_REQUESTS = "Too Many Requests";
    public static final String STATUS_INTERNAL_SERVER_ERROR = "Internal Server Error";
    public static final String STATUS_SERVICE_UNAVAILABLE = "Service Unavailable";
}
