package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.core.CommandService;
import com.aegisql.conveyor.service.core.ConveyorWatchService;
import com.aegisql.conveyor.service.core.DashboardService;
import com.aegisql.conveyor.service.core.PlacementService;
import com.aegisql.conveyor.service.core.StaticPartService;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.aegisql.conveyor.service.error.UnsupportedMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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
    private final ConveyorWatchService conveyorWatchService;
    private final ObjectMapper objectMapper;
    private final int watchHistoryLimitDefault;
    private final int conveyorHistoryLimitDefault;

    public DashboardViewController(
            DashboardService dashboardService,
            PlacementService placementService,
            StaticPartService staticPartService,
            CommandService commandService,
            ConveyorWatchService conveyorWatchService,
            ObjectMapper objectMapper,
            @Value("${conveyor.service.dashboard.default-watch-history-limit:100}") int watchHistoryLimitDefault,
            @Value("${conveyor.service.dashboard.default-conveyor-history-limit:100}") int conveyorHistoryLimitDefault
    ) {
        this.dashboardService = dashboardService;
        this.placementService = placementService;
        this.staticPartService = staticPartService;
        this.commandService = commandService;
        this.conveyorWatchService = conveyorWatchService;
        this.objectMapper = objectMapper;
        this.watchHistoryLimitDefault = Math.max(1, Math.min(500, watchHistoryLimitDefault));
        this.conveyorHistoryLimitDefault = Math.max(1, Math.min(500, conveyorHistoryLimitDefault));
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
        model.addAttribute("watchHistoryLimitDefault", watchHistoryLimitDefault);
        model.addAttribute("conveyorHistoryLimitDefault", conveyorHistoryLimitDefault);
        if (model.containsAttribute("dashboardOutputEvent")) {
            model.addAttribute("dashboardOutputEventJson",
                    objectMapper.writeValueAsString(model.asMap().get("dashboardOutputEvent")));
        }
        if (!model.containsAttribute("testRequest")) {
            model.addAttribute("testRequest", Map.of(
                    "foreach", false,
                    "watchResults", false,
                    "watchLimit", String.valueOf(watchHistoryLimitDefault),
                    "extraProperties", List.of()
            ));
        }
        if (!model.containsAttribute("staticTestRequest")) {
            model.addAttribute("staticTestRequest", Map.of("delete", false, "extraProperties", List.of()));
        }
        if (!model.containsAttribute("commandTestRequest")) {
            model.addAttribute("commandTestRequest", Map.of(
                    "foreach", false,
                    "operation", "cancel",
                    "watchResults", false,
                    "watchLimit", String.valueOf(watchHistoryLimitDefault),
                    "extraProperties", List.of()
            ));
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

    @PostMapping("/dashboard/watch")
    public String watchConveyor(
            @RequestParam("name") String name,
            @RequestParam(name = "watchLimit", required = false) String watchLimit,
            @RequestParam(name = "tab", required = false) String tab,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (!dashboardService.isTopLevelConveyor(name)) {
                throw new IllegalArgumentException("Watch is available only for top-level conveyors");
            }
            Integer watchLimitValue = parseOptionalPositiveInt("watchLimit", watchLimit);
            String username = authenticatedUsername(authentication);
            conveyorWatchService.registerWatch(username, name, null, true, watchLimitValue);
            redirectAttributes.addFlashAttribute("message", "Watch started for " + name);
        } catch (Throwable t) {
            redirectAttributes.addFlashAttribute("error", safeError(t));
        }
        redirectAttributes.addAttribute("name", name);
        redirectAttributes.addAttribute("tab", normalizeTab(tab));
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/test/place")
    public String placeTestPart(
            @RequestParam("name") String name,
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "foreach", required = false) String foreach,
            @RequestParam(name = "watchResults", required = false) String watchResults,
            @RequestParam(name = "watchLimit", required = false) String watchLimit,
            @RequestParam("label") String label,
            @RequestParam("contentType") String contentType,
            @RequestParam("body") String body,
            @RequestParam(name = "bodyFile", required = false) MultipartFile bodyFile,
            @RequestParam(name = "ttl", required = false) String ttl,
            @RequestParam(name = "expirationTime", required = false) String expirationTime,
            @RequestParam(name = "creationTime", required = false) String creationTime,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "requestTTL", required = false) String requestTtl,
            @RequestParam(name = "extraParamKey", required = false) List<String> extraParamKeys,
            @RequestParam(name = "extraParamValue", required = false) List<String> extraParamValues,
            @RequestParam(name = "tab", required = false) String tab,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        List<ExtraPropertyRow> extraRows = collectExtraPropertyRows(extraParamKeys, extraParamValues);
        boolean forEach = parseDeleteFlag(foreach);
        boolean watchEnabled = parseDeleteFlag(watchResults);
        Integer watchLimitValue = parseOptionalPositiveInt("watchLimit", watchLimit);
        String username = null;
        boolean watchRegistered = false;
        long startedNanos = System.nanoTime();
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

            if (watchEnabled) {
                username = authenticatedUsername(authentication);
                conveyorWatchService.registerWatch(username, name, id, forEach, watchLimitValue);
                watchRegistered = true;
            }

            PlacementResult<Boolean> result;
            if (forEach) {
                result = placementService.placePartForEach(
                        contentType,
                        name,
                        label,
                        resolveBodyBytes(body, bodyFile),
                        requestParams
                );
            } else {
                result = placementService.placePart(
                        contentType,
                        name,
                        id,
                        label,
                        resolveBodyBytes(body, bodyFile),
                        requestParams
                );
            }
            redirectAttributes.addFlashAttribute("dashboardOutputEvent",
                    buildConveyorOutputEvent(name, HttpStatus.OK.value(), result, elapsedMillis(startedNanos)));
            redirectAttributes.addFlashAttribute("message", "Part test submitted");
            redirectAttributes.addFlashAttribute("testRequest",
                    buildTestRequest(
                            id,
                            forEach,
                            watchEnabled,
                            watchLimit,
                            label,
                            contentType,
                            body,
                            ttl,
                            expirationTime,
                            creationTime,
                            priority,
                            requestTtl,
                            extraRows
                    ));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            if (watchRegistered) {
                conveyorWatchService.cancelWatch(username, name, id, forEach);
            }
            LOG.error("Part loader dashboard test failed for conveyor={} label={} foreach={}", name, label, forEach, t);
            redirectAttributes.addFlashAttribute("dashboardOutputEvent",
                    buildConveyorErrorOutputEvent(name, t, elapsedMillis(startedNanos)));
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addFlashAttribute("testRequest",
                    buildTestRequest(
                            id,
                            forEach,
                            watchEnabled,
                            watchLimit,
                            label,
                            contentType,
                            body,
                            ttl,
                            expirationTime,
                            creationTime,
                            priority,
                            requestTtl,
                            extraRows
                    ));
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
            @RequestParam(name = "bodyFile", required = false) MultipartFile bodyFile,
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
        long startedNanos = System.nanoTime();
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
                    resolveBodyBytes(requestBody, bodyFile),
                    requestParams
            );
            redirectAttributes.addFlashAttribute("dashboardOutputEvent",
                    buildConveyorOutputEvent(name, HttpStatus.OK.value(), result, elapsedMillis(startedNanos)));
            redirectAttributes.addFlashAttribute("message", "Static part test submitted");
            redirectAttributes.addFlashAttribute("staticTestRequest",
                    buildStaticTestRequest(label, contentType, deleteMode, requestBody, priority, requestTtl, extraRows));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            LOG.error("Static part dashboard test failed for conveyor={} label={} delete={}", name, label, deleteMode, t);
            redirectAttributes.addFlashAttribute("dashboardOutputEvent",
                    buildConveyorErrorOutputEvent(name, t, elapsedMillis(startedNanos)));
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
            @RequestParam(name = "watchResults", required = false) String watchResults,
            @RequestParam(name = "watchLimit", required = false) String watchLimit,
            @RequestParam("operation") String operation,
            @RequestParam(name = "body", required = false) String body,
            @RequestParam(name = "ttl", required = false) String ttl,
            @RequestParam(name = "expirationTime", required = false) String expirationTime,
            @RequestParam(name = "creationTime", required = false) String creationTime,
            @RequestParam(name = "requestTTL", required = false) String requestTtl,
            @RequestParam(name = "commandExtraParamKey", required = false) List<String> extraParamKeys,
            @RequestParam(name = "commandExtraParamValue", required = false) List<String> extraParamValues,
            @RequestParam(name = "tab", required = false) String tab,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        List<ExtraPropertyRow> extraRows = collectExtraPropertyRows(extraParamKeys, extraParamValues);
        boolean forEach = parseDeleteFlag(foreach);
        boolean watchEnabled = parseDeleteFlag(watchResults);
        Integer watchLimitValue = parseOptionalPositiveInt("watchLimit", watchLimit);
        String requestBody = safeString(body);
        String username = null;
        boolean watchRegistered = false;
        long startedNanos = System.nanoTime();
        try {
            Map<String, String> requestParams = new HashMap<>();
            putIfText(requestParams, "ttl", ttl);
            putIfText(requestParams, "expirationTime", expirationTime);
            putIfText(requestParams, "creationTime", creationTime);
            putIfText(requestParams, "requestTTL", requestTtl);
            requestParams.putAll(parseExtraParams(extraRows));

            if (watchEnabled) {
                username = authenticatedUsername(authentication);
                conveyorWatchService.registerWatch(username, name, id, forEach, watchLimitValue);
                watchRegistered = true;
            }

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

            redirectAttributes.addFlashAttribute("dashboardOutputEvent",
                    buildConveyorOutputEvent(name, HttpStatus.OK.value(), result, elapsedMillis(startedNanos)));
            redirectAttributes.addFlashAttribute("message", "Command submitted");
            redirectAttributes.addFlashAttribute("commandTestRequest",
                    buildCommandTestRequest(
                            id,
                            forEach,
                            watchEnabled,
                            watchLimit,
                            operation,
                            requestBody,
                            ttl,
                            expirationTime,
                            creationTime,
                            requestTtl,
                            extraRows
                    ));
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addAttribute("tab", normalizeTab(tab));
            return "redirect:/dashboard";
        } catch (Throwable t) {
            if (watchRegistered) {
                conveyorWatchService.cancelWatch(username, name, id, forEach);
            }
            LOG.error("Command dashboard test failed for conveyor={} id={} command={} foreach={}", name, id, operation, forEach, t);
            redirectAttributes.addFlashAttribute("dashboardOutputEvent",
                    buildConveyorErrorOutputEvent(name, t, elapsedMillis(startedNanos)));
            redirectAttributes.addFlashAttribute("error", safeError(t));
            redirectAttributes.addFlashAttribute("commandTestRequest",
                    buildCommandTestRequest(
                            id,
                            forEach,
                            watchEnabled,
                            watchLimit,
                            operation,
                            requestBody,
                            ttl,
                            expirationTime,
                            creationTime,
                            requestTtl,
                            extraRows
                    ));
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

    private Integer parseOptionalPositiveInt(String key, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(key + " must be a positive integer");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a positive integer", ex);
        }
    }

    private Map<String, Object> buildTestRequest(
            String id,
            boolean forEach,
            boolean watchResults,
            String watchLimit,
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
        request.put("watchResults", watchResults);
        request.put("watchLimit", StringUtils.hasText(watchLimit) ? watchLimit.trim() : String.valueOf(watchHistoryLimitDefault));
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
            boolean watchResults,
            String watchLimit,
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
        request.put("watchResults", watchResults);
        request.put("watchLimit", StringUtils.hasText(watchLimit) ? watchLimit.trim() : String.valueOf(watchHistoryLimitDefault));
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

    private byte[] resolveBodyBytes(String inlineBody, MultipartFile bodyFile) {
        if (bodyFile != null && !bodyFile.isEmpty()) {
            try {
                return bodyFile.getBytes();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read body file", e);
            }
        }
        return safeString(inlineBody).getBytes(StandardCharsets.UTF_8);
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private Map<String, Object> buildConveyorOutputEvent(
            String conveyorName,
            int httpStatus,
            PlacementResult<?> result,
            long responseMillis
    ) {
        Object resultValue = result == null ? null : result.getResult();
        String placementStatus = result == null || result.getStatus() == null ? "UNKNOWN" : result.getStatus().name();
        String errorCode = result == null ? null : result.getErrorCode();
        String errorMessage = result == null ? null : result.getErrorMessage();

        Map<String, Object> status = buildStatusPayload(
                httpStatus,
                resultValue,
                placementStatus,
                errorCode,
                errorMessage,
                responseMillis
        );
        return buildOutputEvent(
                "conveyor",
                conveyorName,
                conveyorName,
                status,
                toJsonCompatible(result)
        );
    }

    private Map<String, Object> buildConveyorErrorOutputEvent(
            String conveyorName,
            Throwable throwable,
            long responseMillis
    ) {
        Throwable cause = rootCause(throwable);
        HttpStatus httpStatus = mapHttpStatus(cause);
        String placementStatus = httpStatus.is4xxClientError() ? "REJECTED" : "FAILED";
        String errorCode = mapErrorCode(cause);
        String errorMessage = safeError(cause);

        Map<String, Object> status = buildStatusPayload(
                httpStatus.value(),
                Boolean.FALSE,
                placementStatus,
                errorCode,
                errorMessage,
                responseMillis
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exceptionClass", cause.getClass().getName());
        payload.put("message", errorMessage);

        return buildOutputEvent("conveyor", conveyorName, conveyorName, status, payload);
    }

    private Map<String, Object> buildStatusPayload(
            int httpStatus,
            Object result,
            String status,
            String errorCode,
            String errorMessage,
            long responseMillis
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("httpStatus", httpStatus);
        payload.put("result", toJsonCompatible(result));
        payload.put("status", StringUtils.hasText(status) ? status : "UNKNOWN");
        payload.put("errorCode", errorCode);
        payload.put("errorMessage", errorMessage);
        payload.put("responseTime", formatDuration(responseMillis));
        payload.put("summaryLine", buildSummaryLine(httpStatus, result, status, errorCode, errorMessage, responseMillis));
        return payload;
    }

    private Map<String, Object> buildOutputEvent(
            String sourceType,
            String sourceKey,
            String title,
            Map<String, Object> status,
            Object payload
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("sourceType", sourceType);
        event.put("sourceKey", sourceKey);
        event.put("title", title);
        event.put("status", status);
        event.put("payload", payload);
        return event;
    }

    private Object toJsonCompatible(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, Object.class);
        } catch (IllegalArgumentException ex) {
            return String.valueOf(value);
        }
    }

    private String buildSummaryLine(
            int httpStatus,
            Object result,
            String status,
            String errorCode,
            String errorMessage,
            long responseMillis
    ) {
        StringBuilder line = new StringBuilder();
        line.append("HTTP ").append(httpStatus);
        line.append(" | result=").append(result == null ? "null" : result);
        line.append(" | status=").append(StringUtils.hasText(status) ? status : "UNKNOWN");
        if (StringUtils.hasText(errorCode)) {
            line.append(" | errorCode=").append(errorCode);
        }
        if (StringUtils.hasText(errorMessage)) {
            line.append(" | errorMessage=").append(errorMessage);
        }
        line.append(" | response=").append(formatDuration(responseMillis));
        return line.toString();
    }

    private String formatDuration(long millis) {
        if (millis < 1_000) {
            return millis + " ms";
        }
        long seconds = millis / 1_000;
        long remainderMillis = millis % 1_000;
        if (seconds < 60) {
            return seconds + "." + String.format("%03d", remainderMillis) + " s";
        }
        long minutes = seconds / 60;
        long remainderSeconds = seconds % 60;
        return minutes + "m " + remainderSeconds + "." + String.format("%03d", remainderMillis) + "s";
    }

    private HttpStatus mapHttpStatus(Throwable throwable) {
        if (throwable instanceof ConveyorNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (throwable instanceof UnsupportedMappingException) {
            return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        }
        if (throwable instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String mapErrorCode(Throwable throwable) {
        if (throwable instanceof ConveyorNotFoundException) {
            return "NOT_FOUND";
        }
        if (throwable instanceof UnsupportedMappingException) {
            return "UNSUPPORTED_MEDIA_TYPE";
        }
        if (throwable instanceof IllegalArgumentException) {
            return "BAD_REQUEST";
        }
        return "INTERNAL_ERROR";
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return authentication.getName();
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    private String safeError(Throwable t) {
        Throwable cursor = rootCause(t);
        String message = cursor.getMessage();
        if (!StringUtils.hasText(message)) {
            message = cursor.getClass().getSimpleName();
        }
        return message;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable cursor = throwable;
        int depth = 0;
        while (cursor.getCause() != null && cursor.getCause() != cursor && depth < 8) {
            cursor = cursor.getCause();
            depth++;
        }
        return cursor;
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
