package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.core.StaticPartService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
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
    public ResponseEntity<PlacementResult<Boolean>> placeStaticPart(
            @RequestHeader(name = "Content-Type", required = false) String contentType,
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("label") @NotBlank String label,
            @RequestBody(required = false) byte[] value,
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
