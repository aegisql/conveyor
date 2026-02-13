package com.aegisql.conveyor.service.api;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PlacementResult<T> {

    private final T result;
    private final Instant timestamp;
    private final String correlationId;
    private final String label;
    private final Map<String, Object> properties;
    private final PlacementStatus status;
    private final String errorCode;
    private final String errorMessage;
    private final String exceptionClass;

    private PlacementResult(Builder<T> builder) {
        this.result = builder.result;
        this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
        this.correlationId = builder.correlationId;
        this.label = builder.label;
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
        this.status = builder.status;
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
        this.exceptionClass = builder.exceptionClass;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public T getResult() {
        return result;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public PlacementStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public boolean isSuccess() {
        return status == PlacementStatus.COMPLETED || status == PlacementStatus.ACCEPTED || status == PlacementStatus.IN_PROGRESS;
    }

    public static final class Builder<T> {
        private T result;
        private Instant timestamp;
        private String correlationId;
        private String label;
        private Map<String, Object> properties = new HashMap<>();
        private PlacementStatus status;
        private String errorCode;
        private String errorMessage;
        private String exceptionClass;

        public Builder<T> result(T result) {
            this.result = result;
            return this;
        }

        public Builder<T> timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder<T> correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder<T> label(String label) {
            this.label = label;
            return this;
        }

        public Builder<T> properties(Map<String, Object> properties) {
            if (properties != null) {
                this.properties = new HashMap<>(properties);
            }
            return this;
        }

        public Builder<T> addProperty(String key, Object value) {
            if (key != null) {
                this.properties.put(key, value);
            }
            return this;
        }

        public Builder<T> status(PlacementStatus status) {
            this.status = status;
            return this;
        }

        public Builder<T> errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder<T> errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder<T> exceptionClass(String exceptionClass) {
            this.exceptionClass = exceptionClass;
            return this;
        }

        public PlacementResult<T> build() {
            Objects.requireNonNull(status, "status is required");
            return new PlacementResult<>(this);
        }
    }
}
