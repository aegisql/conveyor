package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.core.ConveyorWatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/watch")
@Tag(name = "Watch API", description = "Manage user watch registrations and limits")
public class WatchController {

    private final ConveyorWatchService conveyorWatchService;

    public WatchController(ConveyorWatchService conveyorWatchService) {
        this.conveyorWatchService = conveyorWatchService;
    }

    @GetMapping
    @Operation(summary = "List active watch registrations for current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active watches returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public List<Map<String, Object>> activeWatches(Authentication authentication) {
        return conveyorWatchService.activeWatchesForUser(authenticatedUsername(authentication));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel watch by watchId or by conveyor/correlationId/foreach tuple")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancel processed"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Map<String, Object> cancelWatch(
            @RequestBody(required = false) Map<String, Object> request,
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        String username = authenticatedUsername(authentication);
        String watchId = request == null ? null : toStringValue(request.get("watchId"));

        boolean canceled;
        if (StringUtils.hasText(watchId)) {
            canceled = conveyorWatchService.cancelWatchById(username, watchId);
        } else {
            String conveyor = request == null ? null : toStringValue(request.get("conveyor"));
            boolean forEach = parseBoolean(request == null ? null : request.get("foreach"));
            String correlationId = request == null ? null : toStringValue(request.get("correlationId"));
            if (!forEach && !StringUtils.hasText(correlationId)) {
                throw new IllegalArgumentException("correlationId is required when foreach is false");
            }
            canceled = conveyorWatchService.cancelWatch(username, conveyor, correlationId, forEach);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("canceled", canceled);
        return payload;
    }

    @PostMapping("/history-limit")
    @Operation(summary = "Update foreach watch history limit for current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History limit updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Map<String, Object> updateHistoryLimit(
            @RequestBody(required = false) Map<String, Object> request,
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        Integer historyLimit = parsePositiveInt(request == null ? null : request.get("historyLimit"), "historyLimit");
        int updated = conveyorWatchService.updateForeachHistoryLimit(authenticatedUsername(authentication), historyLimit);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("historyLimit", historyLimit);
        payload.put("updated", updated);
        return payload;
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return authentication.getName();
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }
        String normalized = String.valueOf(value).trim().toLowerCase();
        return "true".equals(normalized) || "on".equals(normalized) || "1".equals(normalized);
    }

    private Integer parsePositiveInt(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            int parsed = Integer.parseInt(String.valueOf(value).trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldName + " must be a positive integer");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a positive integer", ex);
        }
    }
}
