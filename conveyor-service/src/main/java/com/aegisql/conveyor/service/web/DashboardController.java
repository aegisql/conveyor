package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.core.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
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
            @RequestParam(name = "stopTimeout", required = false) String stopTimeout
    ) {
        LOG.info("API admin reload requested: name='{}', stopTimeout='{}'", name, stopTimeout);
        dashboardService.reload(name, stopTimeout);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/{name}")
    public ResponseEntity<Void> delete(
            @PathVariable("name") String name,
            @RequestParam(name = "stopTimeout", required = false) String stopTimeout
    ) {
        LOG.info("API admin delete requested: name='{}', stopTimeout='{}'", name, stopTimeout);
        dashboardService.delete(name, stopTimeout);
        return ResponseEntity.ok().build();
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
}
