package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.core.CommandService;
import com.aegisql.conveyor.service.core.ConveyorWatchService;
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
@Tag(name = "Commands", description = "Execute command carts on conveyors")
public class CommandController {

    private final CommandService commandService;
    private final ConveyorWatchService conveyorWatchService;

    public CommandController(CommandService commandService, ConveyorWatchService conveyorWatchService) {
        this.commandService = commandService;
        this.conveyorWatchService = conveyorWatchService;
    }

    @PostMapping(
            path = "/command/{conveyor}/{id}/{command}",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Execute command by correlation ID",
            description = "Returns 202 when command execution is accepted/in-progress and 200 for synchronous completion."
    )
    @Parameters({
            @Parameter(name = "ttl", description = "Command cart TTL (for example '1 MINUTES')"),
            @Parameter(name = "expirationTime", description = "Explicit cart expiration timestamp"),
            @Parameter(name = "creationTime", description = "Explicit cart creation timestamp"),
            @Parameter(name = "priority", description = "Optional cart priority"),
            @Parameter(name = "requestTTL", description = "Optional request wait timeout"),
            @Parameter(name = "watchResults", description = "Endpoint control flag: when true, registers watch updates for this request. Not forwarded as a conveyor property."),
            @Parameter(name = "watchLimit", description = "Endpoint control value for watch history limit (positive integer). Not forwarded as a conveyor property.")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Command completed synchronously",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "202", description = "Command accepted / still in progress",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Feature disabled or forbidden"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found"),
            @ApiResponse(responseCode = "415", description = "Unsupported content type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PlacementResult<Object>> commandById(
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("id") @NotBlank String id,
            @PathVariable("command") @NotBlank String command,
            @RequestBody(required = false) byte[] body,
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
            PlacementResult<Object> result = commandService.executeById(
                    conveyor,
                    id,
                    command,
                    body == null ? new byte[0] : body,
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
            path = "/command/{conveyor}/{command}",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Execute command for each current builder",
            description = "Returns 202 when execution is accepted/in-progress and 200 for synchronous completion."
    )
    @Parameters({
            @Parameter(name = "ttl", description = "Command cart TTL (for example '1 MINUTES')"),
            @Parameter(name = "expirationTime", description = "Explicit cart expiration timestamp"),
            @Parameter(name = "creationTime", description = "Explicit cart creation timestamp"),
            @Parameter(name = "priority", description = "Optional cart priority"),
            @Parameter(name = "requestTTL", description = "Optional request wait timeout"),
            @Parameter(name = "watchResults", description = "Endpoint control flag: when true, registers foreach watch updates. Not forwarded as a conveyor property."),
            @Parameter(name = "watchLimit", description = "Endpoint control value for watch history limit (positive integer). Not forwarded as a conveyor property.")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Command completed synchronously",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "202", description = "Command accepted / still in progress",
                    content = @Content(schema = @Schema(implementation = PlacementResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Feature disabled or forbidden"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found"),
            @ApiResponse(responseCode = "415", description = "Unsupported content type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PlacementResult<Object>> commandForEach(
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("command") @NotBlank String command,
            @RequestBody(required = false) byte[] body,
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
            PlacementResult<Object> result = commandService.executeForEach(
                    conveyor,
                    command,
                    body == null ? new byte[0] : body,
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
