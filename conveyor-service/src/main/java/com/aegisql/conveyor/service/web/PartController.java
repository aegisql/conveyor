package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.core.PlacementService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
public class PartController {

    private final PlacementService placementService;

    public PartController(PlacementService placementService) {
        this.placementService = placementService;
    }

    @PostMapping(
            path = "/part/{conveyor}/{id}/{label}",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PlacementResult<Boolean>> placePart(
            @RequestHeader("Content-Type") String contentType,
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("id") @NotBlank String id,
            @PathVariable("label") @NotBlank String label,
            @RequestBody byte[] value,
            @RequestParam Map<String, String> requestProperties
    ) {
        PlacementResult<Boolean> result = placementService.placePart(
                contentType,
                conveyor,
                id,
                label,
                value,
                requestProperties
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping(
            path = "/part/{conveyor}/{label}",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PlacementResult<Boolean>> placePartForeach(
            @RequestHeader("Content-Type") String contentType,
            @PathVariable("conveyor") @NotBlank String conveyor,
            @PathVariable("label") @NotBlank String label,
            @RequestBody byte[] value,
            @RequestParam Map<String, String> requestProperties
    ) {
        PlacementResult<Boolean> result = placementService.placePartForEach(
                contentType,
                conveyor,
                label,
                value,
                requestProperties
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
