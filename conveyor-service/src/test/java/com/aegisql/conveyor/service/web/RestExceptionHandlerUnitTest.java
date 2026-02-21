package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.aegisql.conveyor.service.error.FeatureDisabledException;
import com.aegisql.conveyor.service.error.UnsupportedMappingException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerUnitTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void handleNotFoundProducesNotFoundEnvelope() {
        var ex = new ConveyorNotFoundException("missing");
        var response = handler.handleNotFound(ex);

        assertEnvelope(response.getBody(), PlacementStatus.REJECTED, "NOT_FOUND", ex.getMessage(), ConveyorNotFoundException.class.getName());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleUnsupportedProducesUnsupportedMediaTypeEnvelope() {
        var response = handler.handleUnsupported(new UnsupportedMappingException("unsupported"));

        assertEnvelope(
                response.getBody(),
                PlacementStatus.REJECTED,
                "UNSUPPORTED_MEDIA_TYPE",
                "unsupported",
                UnsupportedMappingException.class.getName()
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void handleFeatureDisabledProducesForbiddenEnvelope() {
        var response = handler.handleFeatureDisabled(new FeatureDisabledException("disabled"));

        assertEnvelope(
                response.getBody(),
                PlacementStatus.REJECTED,
                "FORBIDDEN",
                "disabled",
                FeatureDisabledException.class.getName()
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleBadRequestProducesBadRequestEnvelope() {
        var response = handler.handleBadRequest(new IllegalArgumentException("bad input"));

        assertEnvelope(
                response.getBody(),
                PlacementStatus.REJECTED,
                "BAD_REQUEST",
                "bad input",
                IllegalArgumentException.class.getName()
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleGenericProducesInternalErrorEnvelope() {
        var response = handler.handleGeneric(new IllegalStateException("boom"));

        assertEnvelope(
                response.getBody(),
                PlacementStatus.FAILED,
                "INTERNAL_ERROR",
                "boom",
                IllegalStateException.class.getName()
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void assertEnvelope(
            PlacementResult<Void> body,
            PlacementStatus status,
            String errorCode,
            String message,
            String exceptionClass
    ) {
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(status);
        assertThat(body.getErrorCode()).isEqualTo(errorCode);
        assertThat(body.getErrorMessage()).isEqualTo(message);
        assertThat(body.getExceptionClass()).isEqualTo(exceptionClass);
    }
}
