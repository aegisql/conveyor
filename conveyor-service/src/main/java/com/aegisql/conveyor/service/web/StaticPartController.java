package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.core.StaticPartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
@Tag(name = "Static Parts", description = "Place or delete static parts")
public class StaticPartController {

    private final StaticPartService staticPartService;

    public StaticPartController(StaticPartService staticPartService) {
        this.staticPartService = staticPartService;
    }

    @PostMapping(
            path = "/static-part/{conveyor}/{label}",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Place or delete static part",
            description = "Returns 202 for accepted/in-progress updates and 200 for synchronous completion."
    )
    @Parameters({
            @Parameter(name = "delete", description = "When true, deletes the static part"),
            @Parameter(name = "priority", description = "Optional cart priority"),
            @Parameter(name = "requestTTL", description = "Optional request wait timeout"),
            @Parameter(name = "ttl", description = "Optional static-part TTL"),
            @Parameter(name = "expirationTime", description = "Explicit cart expiration timestamp"),
            @Parameter(name = "creationTime", description = "Explicit cart creation timestamp")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Static part processed synchronously",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "202", description = "Static part accepted / still in progress",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Feature disabled or forbidden"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found"),
            @ApiResponse(responseCode = "415", description = "Unsupported content type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PlacementResult<Boolean>> placeStaticPart(
            @Parameter(hidden = true)
            @RequestHeader(name = "Content-Type", required = false) String contentType,
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("label") @NotBlank String label,
            @RequestBody(required = false) byte[] value,
            @Parameter(hidden = true)
            @RequestParam Map<String, String> requestProperties
    ) {
        PlacementResult<Boolean> result = staticPartService.placeStaticPart(
                contentType,
                conveyor,
                label,
                value,
                requestProperties
        );
        return ResponseEntity.status(resolveHttpStatus(result.getStatus())).body(result);
    }

    private HttpStatus resolveHttpStatus(PlacementStatus status) {
        if (status == PlacementStatus.IN_PROGRESS || status == PlacementStatus.ACCEPTED) {
            return HttpStatus.ACCEPTED;
        }
        return HttpStatus.OK;
    }
}
