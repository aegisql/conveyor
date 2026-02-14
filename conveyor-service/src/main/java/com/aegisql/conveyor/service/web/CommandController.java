package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.core.CommandService;
import com.aegisql.conveyor.service.core.ConveyorWatchService;
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
    public ResponseEntity<PlacementResult<Object>> commandById(
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("id") @NotBlank String id,
            @PathVariable("command") @NotBlank String command,
            @RequestBody(required = false) byte[] body,
            @RequestParam Map<String, String> requestProperties,
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
            return ResponseEntity.status(HttpStatus.OK).body(result);
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
    public ResponseEntity<PlacementResult<Object>> commandForEach(
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("command") @NotBlank String command,
            @RequestBody(required = false) byte[] body,
            @RequestParam Map<String, String> requestProperties,
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
            return ResponseEntity.status(HttpStatus.OK).body(result);
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

    private record WatchRequest(boolean enabled, Integer limit) {
    }
}
