package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.core.DashboardAdminOperationService;
import com.aegisql.conveyor.service.core.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard API", description = "Runtime inspection and admin operations for dashboard integration")
public class DashboardController {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardService dashboardService;
    private final DashboardAdminOperationService dashboardAdminOperationService;

    public DashboardController(
            DashboardService dashboardService,
            DashboardAdminOperationService dashboardAdminOperationService
    ) {
        this.dashboardService = dashboardService;
        this.dashboardAdminOperationService = dashboardAdminOperationService;
    }

    @GetMapping("/tree")
    @Operation(summary = "Get conveyor tree")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tree returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Map<String, Map<String, ?>> tree() {
        return dashboardService.conveyorTree();
    }

    @GetMapping("/{name}")
    @Operation(summary = "Inspect conveyor details by name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conveyor details returned"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Map<String, Object> inspect(@PathVariable("name") String name) {
        return dashboardService.inspect(name);
    }

    @PostMapping("/admin/upload")
    @Operation(summary = "Upload or replace conveyor extension jar")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload processed"),
            @ApiResponse(responseCode = "400", description = "Invalid upload"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> upload(@RequestParam("file") MultipartFile file) throws IOException {
        dashboardService.upload(file);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/reload/{name}")
    @Operation(summary = "Schedule controlled reload for top-level conveyor")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Reload scheduled"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found")
    })
    public ResponseEntity<Void> reload(
            @PathVariable("name") String name,
            @RequestParam(name = "stopTimeout", required = false) String stopTimeout,
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        LOG.info("API admin reload requested: name='{}', stopTimeout='{}'", name, stopTimeout);
        dashboardAdminOperationService.scheduleReload(authenticatedUsername(authentication), name, stopTimeout);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/admin/{name}")
    @Operation(summary = "Schedule controlled delete for top-level conveyor tree")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Delete scheduled"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found")
    })
    public ResponseEntity<Void> delete(
            @PathVariable("name") String name,
            @RequestParam(name = "stopTimeout", required = false) String stopTimeout,
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        LOG.info("API admin delete requested: name='{}', stopTimeout='{}'", name, stopTimeout);
        dashboardAdminOperationService.scheduleDelete(authenticatedUsername(authentication), name, stopTimeout);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/admin/events")
    @Operation(summary = "Drain admin output events for current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public List<Map<String, Object>> adminEvents(Authentication authentication) {
        return dashboardAdminOperationService.drainEvents(authenticatedUsername(authentication));
    }

    @PostMapping("/admin/{name}/mbean/{method}")
    @Operation(summary = "Invoke writable MBean operation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invocation result returned"),
            @ApiResponse(responseCode = "400", description = "Invalid operation payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found")
    })
    public ResponseEntity<Object> invoke(
            @PathVariable("name") String name,
            @PathVariable("method") String method,
            @RequestBody(required = false) Object payload
    ) {
        var result = dashboardService.invokeMBean(name, method, payload);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/{name}/parameter/{parameter}")
    @Operation(summary = "Set writable MBean parameter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parameter updated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter value"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Conveyor not found")
    })
    public ResponseEntity<Void> updateParameter(
            @PathVariable("name") String name,
            @PathVariable("parameter") String parameter,
            @RequestParam("value") String value
    ) {
        dashboardService.updateParameter(name, parameter, value);
        return ResponseEntity.ok().build();
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return authentication.getName();
    }
}
