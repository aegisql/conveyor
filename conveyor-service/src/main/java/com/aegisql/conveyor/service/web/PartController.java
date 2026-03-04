package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.core.ConveyorWatchService;
import com.aegisql.conveyor.service.core.PlacementService;
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
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping
@Tag(name = "Parts", description = "Place regular parts into conveyors")
public class PartController {

    private final PlacementService placementService;
    private final ConveyorWatchService conveyorWatchService;

    public PartController(PlacementService placementService, ConveyorWatchService conveyorWatchService) {
        this.placementService = placementService;
        this.conveyorWatchService = conveyorWatchService;
    }

    @PostMapping(
            path = "/part/{conveyor}/{id}/{label}",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Place part by correlation ID",
            description = "Returns 202 when the part is accepted/in-progress and 200 when the placement completes synchronously."
    )
    @Parameters({
            @Parameter(name = "ttl", description = "Builder/cart TTL (for example '1 MINUTES')"),
            @Parameter(name = "expirationTime", description = "Explicit cart expiration timestamp"),
            @Parameter(name = "creationTime", description = "Explicit cart creation timestamp"),
            @Parameter(name = "priority", description = "Optional cart priority"),
            @Parameter(name = "requestTTL", description = "Optional request wait timeout. If omitted, async scheduling typically returns HTTP 202."),
            @Parameter(name = "watchResults", description = "When true, registers watch updates for this request"),
            @Parameter(name = "watchLimit", description = "Optional watch history limit (positive integer)")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Part processed synchronously",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "202", description = "Part accepted / still in progress",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Feature disabled or forbidden"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found"),
            @ApiResponse(responseCode = "415", description = "Unsupported content type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PlacementResult<Boolean>> placePart(
            @Parameter(hidden = true)
            @RequestHeader("Content-Type") String contentType,
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("id") @NotBlank String id,
            @PathVariable("label") @NotBlank String label,
            @RequestBody byte[] value,
            @Parameter(hidden = true)
            @RequestParam Map<String, String> requestProperties,
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        Map<String, String> forwardedParams = new LinkedHashMap<>(requestProperties);
        WatchRequest watchRequest = extractWatchRequest(forwardedParams);
        String username = null;
        if (watchRequest.enabled()) {
            username = authenticatedUsername(authentication);
            conveyorWatchService.registerWatch(username, conveyor, id, false, watchRequest.limit());
        }
        try {
            PlacementResult<Boolean> result = placementService.placePart(
                    contentType,
                    conveyor,
                    id,
                    label,
                    value,
                    forwardedParams
            );
            return ResponseEntity.status(resolveHttpStatus(result.getStatus())).body(result);
        } catch (RuntimeException ex) {
            if (watchRequest.enabled()) {
                conveyorWatchService.cancelWatch(username, conveyor, id, false);
            }
            throw ex;
        }
    }

    @PostMapping(
            path = "/part/{conveyor}/{label}",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Place part for each current builder",
            description = "Places one part to all builders matching label scope. Returns 202 for accepted/in-progress and 200 for completed."
    )
    @Parameters({
            @Parameter(name = "ttl", description = "Builder/cart TTL (for example '1 MINUTES')"),
            @Parameter(name = "expirationTime", description = "Explicit cart expiration timestamp"),
            @Parameter(name = "creationTime", description = "Explicit cart creation timestamp"),
            @Parameter(name = "priority", description = "Optional cart priority"),
            @Parameter(name = "requestTTL", description = "Optional request wait timeout"),
            @Parameter(name = "watchResults", description = "When true, registers foreach watch updates"),
            @Parameter(name = "watchLimit", description = "Optional watch history limit (positive integer)")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Part processed synchronously",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "202", description = "Part accepted / still in progress",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Feature disabled or forbidden"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found"),
            @ApiResponse(responseCode = "415", description = "Unsupported content type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PlacementResult<Boolean>> placePartForeach(
            @Parameter(hidden = true)
            @RequestHeader("Content-Type") String contentType,
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("label") @NotBlank String label,
            @RequestBody byte[] value,
            @Parameter(hidden = true)
            @RequestParam Map<String, String> requestProperties,
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        Map<String, String> forwardedParams = new LinkedHashMap<>(requestProperties);
        WatchRequest watchRequest = extractWatchRequest(forwardedParams);
        String username = null;
        if (watchRequest.enabled()) {
            username = authenticatedUsername(authentication);
            conveyorWatchService.registerWatch(username, conveyor, null, true, watchRequest.limit());
        }
        try {
            PlacementResult<Boolean> result = placementService.placePartForEach(
                    contentType,
                    conveyor,
                    label,
                    value,
                    forwardedParams
            );
            return ResponseEntity.status(resolveHttpStatus(result.getStatus())).body(result);
        } catch (RuntimeException ex) {
            if (watchRequest.enabled()) {
                conveyorWatchService.cancelWatch(username, conveyor, null, true);
            }
            throw ex;
        }
    }

    private WatchRequest extractWatchRequest(Map<String, String> requestProperties) {
        String watchResults = removeFirst(requestProperties, "watchResults", "watch");
        String watchLimitRaw = requestProperties.remove("watchLimit");
        boolean enabled = parseBoolean(watchResults);
        Integer watchLimit = null;
        if (StringUtils.hasText(watchLimitRaw)) {
            try {
                watchLimit = Integer.parseInt(watchLimitRaw.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("watchLimit must be a positive integer", ex);
            }
            if (watchLimit <= 0) {
                throw new IllegalArgumentException("watchLimit must be a positive integer");
            }
        }
        return new WatchRequest(enabled, watchLimit);
    }

    private String removeFirst(Map<String, String> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                return source.remove(key);
            }
        }
        return null;
    }

    private boolean parseBoolean(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return "true".equals(normalized) || "on".equals(normalized) || "1".equals(normalized);
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return authentication.getName();
    }

    private HttpStatus resolveHttpStatus(PlacementStatus status) {
        if (status == PlacementStatus.IN_PROGRESS || status == PlacementStatus.ACCEPTED) {
            return HttpStatus.ACCEPTED;
        }
        return HttpStatus.OK;
    }

    private record WatchRequest(boolean enabled, Integer limit) {
    }
}
