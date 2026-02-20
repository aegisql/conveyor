package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.core.DashboardAdminOperationService;
import com.aegisql.conveyor.service.core.DashboardService;
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
    public Map<String, Map<String, ?>> tree() {
        return dashboardService.conveyorTree();
    }

    @GetMapping("/{name}")
    public Map<String, Object> inspect(@PathVariable("name") String name) {
        return dashboardService.inspect(name);
    }

    @PostMapping("/admin/upload")
    public ResponseEntity<Void> upload(@RequestParam("file") MultipartFile file) throws IOException {
        dashboardService.upload(file);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/reload/{name}")
    public ResponseEntity<Void> reload(
            @PathVariable("name") String name,
            @RequestParam(name = "stopTimeout", required = false) String stopTimeout,
            Authentication authentication
    ) {
        LOG.info("API admin reload requested: name='{}', stopTimeout='{}'", name, stopTimeout);
        dashboardAdminOperationService.scheduleReload(authenticatedUsername(authentication), name, stopTimeout);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/admin/{name}")
    public ResponseEntity<Void> delete(
            @PathVariable("name") String name,
            @RequestParam(name = "stopTimeout", required = false) String stopTimeout,
            Authentication authentication
    ) {
        LOG.info("API admin delete requested: name='{}', stopTimeout='{}'", name, stopTimeout);
        dashboardAdminOperationService.scheduleDelete(authenticatedUsername(authentication), name, stopTimeout);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/admin/events")
    public List<Map<String, Object>> adminEvents(Authentication authentication) {
        return dashboardAdminOperationService.drainEvents(authenticatedUsername(authentication));
    }

    @PostMapping("/admin/{name}/mbean/{method}")
    public ResponseEntity<Object> invoke(
            @PathVariable("name") String name,
            @PathVariable("method") String method,
            @RequestBody(required = false) Object payload
    ) {
        var result = dashboardService.invokeMBean(name, method, payload);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/{name}/parameter/{parameter}")
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
