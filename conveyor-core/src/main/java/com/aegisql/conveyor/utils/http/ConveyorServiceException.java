package com.aegisql.conveyor.utils.http;

public class ConveyorServiceException extends RuntimeException {

    private final int httpStatus;
    private final String placementStatus;
    private final String rawBody;

    public ConveyorServiceException(String message, int httpStatus, String placementStatus, String rawBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.placementStatus = placementStatus;
        this.rawBody = rawBody;
    }

    public ConveyorServiceException(String message, Throwable cause, int httpStatus, String placementStatus, String rawBody) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.placementStatus = placementStatus;
        this.rawBody = rawBody;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getPlacementStatus() {
        return placementStatus;
    }

    public String getRawBody() {
        return rawBody;
    }
}
