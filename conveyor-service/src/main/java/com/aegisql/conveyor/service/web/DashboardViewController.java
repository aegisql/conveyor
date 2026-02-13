package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.core.CommandService;
import com.aegisql.conveyor.service.core.DashboardService;
import com.aegisql.conveyor.service.core.PlacementService;
import com.aegisql.conveyor.service.core.StaticPartService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class DashboardViewController {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardViewController.class);
    private static final String TAB_DETAILS = "tab-details";
    private static final Set<String> VALID_TABS = new HashSet<>(Set.of(
            TAB_DETAILS, "tab-operations", "tab-tester", "tab-static", "tab-commands", "tab-admin"
    ));

    private final DashboardService dashboardService;
    private final PlacementService placementService;
    private final StaticPartService staticPartService;
    private final CommandService commandService;
    private final ObjectMapper objectMapper;

    public DashboardViewController(
            DashboardService dashboardService,
            PlacementService placementService,
            StaticPartService staticPartService,
            CommandService commandService,
            ObjectMapper objectMapper
    ) {
        this.dashboardService = dashboardService;
        this.placementService = placementService;
        this.staticPartService = staticPartService;
        this.commandService = commandService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping(value = {"/dashboard", "/dashboard/"}, produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard(@RequestParam(name = "name", required = false) String name,
                            @RequestParam(name = "tab", required = false) String tab,
                            Authentication authentication,
                            Model model) throws JsonProcessingException {
        Map<String, Map<String, ?>> tree = dashboardService.conveyorTree();
        Map<String, Object> selected = Map.of(
                "attributes", List.of(),
                "writableParameters", List.of(),
                "operations", List.of()
        );
        model.addAttribute("treeJson", objectMapper.writeValueAsString(tree));
        if (StringUtils.hasText(name)) {
            try {
                selected = dashboardService.inspect(name);
            } catch (Throwable t) {
                model.addAttribute("error", safeError(t));
            }
        }
        model.addAttribute("selected", selected);
        model.addAttribute("selectedName", name);
        model.addAttribute("activeTab", normalizeTab(tab));
        model.addAttribute("systemErrors", dashboardService.drainLoaderErrors());
        model.addAttribute("username", authentication == null ? "" : authentication.getName());
        model.addAttribute("isAdmin", hasRole(authentication, "ROLE_DASHBOARD_ADMIN"));
        if (!model.containsAttribute("testRequest")) {
            model.addAttribute("testRequest", Map.of("foreach", false, "extraProperties", List.of()));
        }
        if (!model.containsAttribute("staticTestRequest")) {
            model.addAttribute("staticTestRequest", Map.of("delete", false, "extraProperties", List.of()));
        }
        if (!model.containsAttribute("commandTestRequest")) {
            model.addAttribute("commandTestRequest", Map.of("foreach", false, "operation", "cancel", "extraProperties", List.of()));
        }
        return "dashboard";
    }

    @PostMapping("/dashboard/admin/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(name = "tab", required = false) String tab,
                         RedirectAttributes redirectAttributes) {
        try {
            dashboardService.upload(file);
            redirectAttributes.addFlashAttribute("message", "Upload successful");
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/dashboard/admin/reload")
    public String reload(@RequestParam("name") String name,
                         @RequestParam(name = "tab", required = false) String tab,
                         RedirectAttributes redirectAttributes) {
        try {
            dashboardService.reload(name);
            redirectAttributes.addFlashAttribute("message", "Reloaded");
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/dashboard/admin/delete")
    public String delete(@RequestParam("name") String name,
                         @RequestParam(name = "tab", required = false) String tab,
                         RedirectAttributes redirectAttributes) {
        try {
            dashboardService.delete(name);
            redirectAttributes.addFlashAttribute("message", "Deleted");
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/dashboard/admin/mbean")
    public String invokeMbean(@RequestParam("name") String name,
                              @RequestParam("method") String method,
                              @RequestParam(name = "arg", required = false) String arg,
                              @RequestParam(name = "tab", required = false) String tab,
                              RedirectAttributes redirectAttributes) {
        try {
            Object payload = null;
            if (StringUtils.hasText(arg)) {
                try {
                    payload = objectMapper.readTree(arg);
                } catch (Exception ignored) {
                    payload = arg;
                }
            }
            dashboardService.invokeMBean(name, method, payload);
            redirectAttributes.addFlashAttribute("message", "Invoked");
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/dashboard/admin/parameter")
    public String updateParameter(@RequestParam("name") String name,
                                  @RequestParam("parameter") String parameter,
                                  @RequestParam("value") String value,
                                  @RequestParam(name = "tab", required = false) String tab,
                                  RedirectAttributes redirectAttributes) {
        try {
            dashboardService.updateParameter(name, parameter, value);
            redirectAttributes.addFlashAttribute("message", "Parameter updated");
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/dashboard/test/place")
    public String placeTestPart(
            @RequestParam("name") String name,
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "foreach", required = false) String foreach,
            @RequestParam("label") String label,
            @RequestParam("contentType") String contentType,
            @RequestParam("body") String body,
            @RequestParam(name = "ttl", required = false) String ttl,
            @RequestParam(name = "expirationTime", required = false) String expirationTime,
            @RequestParam(name = "creationTime", required = false) String creationTime,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "requestTTL", required = false) String requestTtl,
            @RequestParam(name = "extraParamKey", required = false) List<String> extraParamKeys,
            @RequestParam(name = "extraParamValue", required = false) List<String> extraParamValues,
            @RequestParam(name = "tab", required = false) String tab,
            RedirectAttributes redirectAttributes
    ) {
        List<ExtraPropertyRow> extraRows = collectExtraPropertyRows(extraParamKeys, extraParamValues);
        boolean forEach = parseDeleteFlag(foreach);
        try {
            if (!dashboardService.isTopLevelConveyor(name)) {
                throw new IllegalArgumentException("Part loader tester is only available for top-level conveyors");
            }
            if (!forEach && !StringUtils.hasText(id)) {
                throw new IllegalArgumentException("ID is required unless foreach is enabled");
            }
            if (!StringUtils.hasText(ttl) && !StringUtils.hasText(expirationTime)) {
                throw new IllegalArgumentException("Either ttl or expirationTime is required for dashboard test");
            }
            Map<String, String> requestParams = new HashMap<>();
            putIfText(requestParams, "ttl", ttl);
            putIfText(requestParams, "expirationTime", expirationTime);
            putIfText(requestParams, "creationTime", creationTime);
            putIfText(requestParams, "priority", priority);
            putIfText(requestParams, "requestTTL", requestTtl);
            requestParams.putAll(parseExtraParams(extraRows));

            PlacementResult<Boolean> result;
            if (forEach) {
                result = placementService.placePartForEach(
                        contentType,
                        name,
                        label,
                        body.getBytes(StandardCharsets.UTF_8),
                        requestParams
                );
            } else {
                result = placementService.placePart(
                        contentType,
                        name,
                        id,
                        label,
                        body.getBytes(StandardCharsets.UTF_8),
                        requestParams
                );
            }
            redirectAttributes.addFlashAttribute("testResultJson",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
            redirectAttributes.addFlashAttribute("message", "Part test submitted");
            redirectAttributes.addFlashAttribute("testRequest",
                    buildTestRequest(id, forEach, label, contentType, body, ttl, expirationTime, creationTime, priority, requestTtl, extraRows));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            LOG.error("Part loader dashboard test failed for conveyor={} label={} foreach={}", name, label, forEach, t);
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addFlashAttribute("testRequest",
                    buildTestRequest(id, forEach, label, contentType, body, ttl, expirationTime, creationTime, priority, requestTtl, extraRows));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/dashboard/test/static-part")
    public String placeTestStaticPart(
            @RequestParam("name") String name,
            @RequestParam("label") String label,
            @RequestParam("contentType") String contentType,
            @RequestParam(name = "delete", required = false) String delete,
            @RequestParam(name = "body", required = false) String body,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "requestTTL", required = false) String requestTtl,
            @RequestParam(name = "staticExtraParamKey", required = false) List<String> extraParamKeys,
            @RequestParam(name = "staticExtraParamValue", required = false) List<String> extraParamValues,
            @RequestParam(name = "tab", required = false) String tab,
            RedirectAttributes redirectAttributes
    ) {
        List<ExtraPropertyRow> extraRows = collectExtraPropertyRows(extraParamKeys, extraParamValues);
        boolean deleteMode = parseDeleteFlag(delete);
        String requestBody = safeString(body);
        try {
            Map<String, String> requestParams = new HashMap<>();
            putIfText(requestParams, "priority", priority);
            putIfText(requestParams, "requestTTL", requestTtl);
            if (deleteMode) {
                requestParams.put("delete", "true");
            }
            requestParams.putAll(parseExtraParams(extraRows));

            PlacementResult<Boolean> result = staticPartService.placeStaticPart(
                    contentType,
                    name,
                    label,
                    requestBody.getBytes(StandardCharsets.UTF_8),
                    requestParams
            );
            redirectAttributes.addFlashAttribute("staticTestResultJson",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
            redirectAttributes.addFlashAttribute("message", "Static part test submitted");
            redirectAttributes.addFlashAttribute("staticTestRequest",
                    buildStaticTestRequest(label, contentType, deleteMode, requestBody, priority, requestTtl, extraRows));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            LOG.error("Static part dashboard test failed for conveyor={} label={} delete={}", name, label, deleteMode, t);
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addFlashAttribute("staticTestRequest",
                    buildStaticTestRequest(label, contentType, deleteMode, requestBody, priority, requestTtl, extraRows));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/dashboard/test/command")
    public String executeTestCommand(
            @RequestParam("name") String name,
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "foreach", required = false) String foreach,
            @RequestParam("operation") String operation,
            @RequestParam(name = "body", required = false) String body,
            @RequestParam(name = "ttl", required = false) String ttl,
            @RequestParam(name = "expirationTime", required = false) String expirationTime,
            @RequestParam(name = "creationTime", required = false) String creationTime,
            @RequestParam(name = "requestTTL", required = false) String requestTtl,
            @RequestParam(name = "commandExtraParamKey", required = false) List<String> extraParamKeys,
            @RequestParam(name = "commandExtraParamValue", required = false) List<String> extraParamValues,
            @RequestParam(name = "tab", required = false) String tab,
            RedirectAttributes redirectAttributes
    ) {
        List<ExtraPropertyRow> extraRows = collectExtraPropertyRows(extraParamKeys, extraParamValues);
        boolean forEach = parseDeleteFlag(foreach);
        String requestBody = safeString(body);
        try {
            Map<String, String> requestParams = new HashMap<>();
            putIfText(requestParams, "ttl", ttl);
            putIfText(requestParams, "expirationTime", expirationTime);
            putIfText(requestParams, "creationTime", creationTime);
            putIfText(requestParams, "requestTTL", requestTtl);
            requestParams.putAll(parseExtraParams(extraRows));

            PlacementResult<Object> result;
            if (forEach) {
                result = commandService.executeForEach(
                        name,
                        operation,
                        requestBody.getBytes(StandardCharsets.UTF_8),
                        requestParams
                );
            } else {
                result = commandService.executeById(
                        name,
                        id,
                        operation,
                        requestBody.getBytes(StandardCharsets.UTF_8),
                        requestParams
                );
            }

            redirectAttributes.addFlashAttribute("commandTestResultJson",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
            redirectAttributes.addFlashAttribute("message", "Command submitted");
            redirectAttributes.addFlashAttribute("commandTestRequest",
                    buildCommandTestRequest(id, forEach, operation, requestBody, ttl, expirationTime, creationTime, requestTtl, extraRows));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            LOG.error("Command dashboard test failed for conveyor={} id={} command={} foreach={}", name, id, operation, forEach, t);
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addFlashAttribute("commandTestRequest",
                    buildCommandTestRequest(id, forEach, operation, requestBody, ttl, expirationTime, creationTime, requestTtl, extraRows));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        }
    }

    private void putIfText(Map<String, String> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value.trim());
        }
    }

    private Map<String, Object> buildTestRequest(
            String id,
            boolean forEach,
            String label,
            String contentType,
            String body,
            String ttl,
            String expirationTime,
            String creationTime,
            String priority,
            String requestTtl,
            List<ExtraPropertyRow> extraRows
    ) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", safeString(id));
        request.put("foreach", forEach);
        request.put("label", safeString(label));
        request.put("contentType", safeString(contentType));
        request.put("body", safeString(body));
        request.put("ttl", safeString(ttl));
        request.put("expirationTime", safeString(expirationTime));
        request.put("creationTime", safeString(creationTime));
        request.put("priority", safeString(priority));
        request.put("requestTTL", safeString(requestTtl));
        request.put("extraProperties", toExtraPropertyMaps(extraRows));
        return request;
    }

    private Map<String, Object> buildStaticTestRequest(
            String label,
            String contentType,
            boolean delete,
            String body,
            String priority,
            String requestTtl,
            List<ExtraPropertyRow> extraRows
    ) {
        Map<String, Object> request = new HashMap<>();
        request.put("label", safeString(label));
        request.put("contentType", safeString(contentType));
        request.put("delete", delete);
        request.put("body", safeString(body));
        request.put("priority", safeString(priority));
        request.put("requestTTL", safeString(requestTtl));
        request.put("extraProperties", toExtraPropertyMaps(extraRows));
        return request;
    }

    private Map<String, Object> buildCommandTestRequest(
            String id,
            boolean forEach,
            String operation,
            String body,
            String ttl,
            String expirationTime,
            String creationTime,
            String requestTtl,
            List<ExtraPropertyRow> extraRows
    ) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", safeString(id));
        request.put("foreach", forEach);
        request.put("operation", safeString(operation));
        request.put("body", safeString(body));
        request.put("ttl", safeString(ttl));
        request.put("expirationTime", safeString(expirationTime));
        request.put("creationTime", safeString(creationTime));
        request.put("requestTTL", safeString(requestTtl));
        request.put("extraProperties", toExtraPropertyMaps(extraRows));
        return request;
    }

    private boolean parseDeleteFlag(String deleteValue) {
        if (!StringUtils.hasText(deleteValue)) {
            return false;
        }
        String normalized = deleteValue.trim().toLowerCase();
        return "true".equals(normalized) || "on".equals(normalized) || "1".equals(normalized);
    }

    private List<ExtraPropertyRow> collectExtraPropertyRows(List<String> keys, List<String> values) {
        int keySize = keys == null ? 0 : keys.size();
        int valueSize = values == null ? 0 : values.size();
        int rowCount = Math.max(keySize, valueSize);
        if (rowCount == 0) {
            return List.of();
        }
        List<ExtraPropertyRow> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            String key = keySize > i && keys.get(i) != null ? keys.get(i).trim() : "";
            String value = valueSize > i && values.get(i) != null ? values.get(i).trim() : "";
            if (key.isEmpty() && value.isEmpty()) {
                continue;
            }
            rows.add(new ExtraPropertyRow(key, value));
        }
        return rows;
    }

    private Map<String, String> parseExtraParams(List<ExtraPropertyRow> rows) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            ExtraPropertyRow row = rows.get(i);
            if (!StringUtils.hasText(row.key())) {
                throw new IllegalArgumentException("Additional property key is required for row " + (i + 1));
            }
            params.put(row.key(), row.value());
        }
        return params;
    }

    private List<Map<String, String>> toExtraPropertyMaps(List<ExtraPropertyRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> props = new ArrayList<>(rows.size());
        for (ExtraPropertyRow row : rows) {
            props.add(Map.of("key", row.key(), "value", row.value()));
        }
        return props;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    private String safeError(Throwable t) {
        Throwable cursor = t;
        int depth = 0;
        while (cursor.getCause() != null && cursor.getCause() != cursor && depth < 8) {
            cursor = cursor.getCause();
            depth++;
        }
        String message = cursor.getMessage();
        if (!StringUtils.hasText(message)) {
            message = cursor.getClass().getSimpleName();
        }
        return message;
    }

    private String normalizeTab(String tab) {
        if (tab == null) {
            return TAB_DETAILS;
        }
        String candidate = tab.trim();
        if (!VALID_TABS.contains(candidate)) {
            return TAB_DETAILS;
        }
        return candidate;
    }

    private record ExtraPropertyRow(String key, String value) { }
}
