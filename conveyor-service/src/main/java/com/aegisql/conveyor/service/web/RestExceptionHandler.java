package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.aegisql.conveyor.service.error.UnsupportedMappingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        PartController.class,
        DashboardController.class,
        CommandController.class,
        StaticPartController.class,
        WatchController.class
})
public class RestExceptionHandler {

    @ExceptionHandler(ConveyorNotFoundException.class)
    public ResponseEntity<PlacementResult<Void>> handleNotFound(ConveyorNotFoundException ex) {
        var body = PlacementResult.<Void>builder()
                .status(PlacementStatus.REJECTED)
                .errorCode("NOT_FOUND")
                .errorMessage(ex.getMessage())
                .exceptionClass(ex.getClass().getName())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UnsupportedMappingException.class)
    public ResponseEntity<PlacementResult<Void>> handleUnsupported(UnsupportedMappingException ex) {
        var body = PlacementResult.<Void>builder()
                .status(PlacementStatus.REJECTED)
                .errorCode("UNSUPPORTED_MEDIA_TYPE")
                .errorMessage(ex.getMessage())
                .exceptionClass(ex.getClass().getName())
                .build();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PlacementResult<Void>> handleBadRequest(IllegalArgumentException ex) {
        var body = PlacementResult.<Void>builder()
                .status(PlacementStatus.REJECTED)
                .errorCode("BAD_REQUEST")
                .errorMessage(ex.getMessage())
                .exceptionClass(ex.getClass().getName())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PlacementResult<Void>> handleGeneric(Exception ex) {
        var body = PlacementResult.<Void>builder()
                .status(PlacementStatus.FAILED)
                .errorCode("INTERNAL_ERROR")
                .errorMessage(ex.getMessage())
                .exceptionClass(ex.getClass().getName())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
