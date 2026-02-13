package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.core.CommandService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
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
            @RequestParam Map<String, String> requestProperties
    ) {
        PlacementResult<Object> result = commandService.executeById(
                conveyor,
                id,
                command,
                body == null ? new byte[0] : body,
                requestProperties
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
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
            @RequestParam Map<String, String> requestProperties
    ) {
        PlacementResult<Object> result = commandService.executeForEach(
                conveyor,
                command,
                body == null ? new byte[0] : body,
                requestProperties
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
