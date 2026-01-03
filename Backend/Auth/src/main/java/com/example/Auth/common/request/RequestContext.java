package com.example.Auth.common.request;

import java.time.Instant;
import java.util.UUID;

/**
 * Thread-local context for holding request-scoped information.
 * This context is populated at the beginning of each request and cleared at the
 * end.
 */
public final class RequestContext {

    private static final ThreadLocal<RequestContextData> CONTEXT = new ThreadLocal<>();

    private RequestContext() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Initialize the request context with given data.
     *
     * @param data the request context data
     */
    public static void initialize(RequestContextData data) {
        CONTEXT.set(data);
    }

    /**
     * Get the current request context data.
     *
     * @return the request context data, or null if not initialized
     */
    public static RequestContextData get() {
        return CONTEXT.get();
    }

    /**
     * Clear the request context. Should be called at the end of each request.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Get the request ID from the current context.
     *
     * @return the request ID, or null if not set
     */
    public static String getRequestId() {
        RequestContextData data = get();
        return data != null ? data.getRequestId() : null;
    }

    /**
     * Get the correlation ID from the current context.
     *
     * @return the correlation ID, or null if not set
     */
    public static String getCorrelationId() {
        RequestContextData data = get();
        return data != null ? data.getCorrelationId() : null;
    }

    /**
     * Get the trace ID from the current context.
     *
     * @return the trace ID, or null if not set
     */
    public static String getTraceId() {
        RequestContextData data = get();
        return data != null ? data.getTraceId() : null;
    }

    /**
     * Get the user ID from the current context.
     *
     * @return the user ID, or null if not set
     */
    public static String getUserId() {
        RequestContextData data = get();
        return data != null ? data.getUserId() : null;
    }

    /**
     * Get the tenant ID from the current context.
     *
     * @return the tenant ID, or null if not set
     */
    public static String getTenantId() {
        RequestContextData data = get();
        return data != null ? data.getTenantId() : null;
    }

    /**
     * Get the remote IP from the current context.
     *
     * @return the remote IP, or null if not set
     */
    public static String getRemoteIp() {
        RequestContextData data = get();
        return data != null ? data.getRemoteIp() : null;
    }

    /**
     * Get the request start time from the current context.
     *
     * @return the start time, or null if not set
     */
    public static Instant getStartTime() {
        RequestContextData data = get();
        return data != null ? data.getStartTime() : null;
    }

    /**
     * Data class holding request context information.
     */
    public static class RequestContextData {
        private final String requestId;
        private final String correlationId;
        private final String traceId;
        private String userId;
        private String tenantId;
        private final String remoteIp;
        private final String userAgent;
        private final Instant startTime;

        private RequestContextData(Builder builder) {
            this.requestId = builder.requestId;
            this.correlationId = builder.correlationId;
            this.traceId = builder.traceId;
            this.userId = builder.userId;
            this.tenantId = builder.tenantId;
            this.remoteIp = builder.remoteIp;
            this.userAgent = builder.userAgent;
            this.startTime = builder.startTime;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getRequestId() {
            return requestId;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getRemoteIp() {
            return remoteIp;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public static class Builder {
            private String requestId = UUID.randomUUID().toString();
            private String correlationId = UUID.randomUUID().toString();
            private String traceId = UUID.randomUUID().toString();
            private String userId;
            private String tenantId;
            private String remoteIp;
            private String userAgent;
            private Instant startTime = Instant.now();

            public Builder requestId(String requestId) {
                this.requestId = requestId;
                return this;
            }

            public Builder correlationId(String correlationId) {
                this.correlationId = correlationId;
                return this;
            }

            public Builder traceId(String traceId) {
                this.traceId = traceId;
                return this;
            }

            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public Builder tenantId(String tenantId) {
                this.tenantId = tenantId;
                return this;
            }

            public Builder remoteIp(String remoteIp) {
                this.remoteIp = remoteIp;
                return this;
            }

            public Builder userAgent(String userAgent) {
                this.userAgent = userAgent;
                return this;
            }

            public Builder startTime(Instant startTime) {
                this.startTime = startTime;
                return this;
            }

            public RequestContextData build() {
                return new RequestContextData(this);
            }
        }
    }
}
