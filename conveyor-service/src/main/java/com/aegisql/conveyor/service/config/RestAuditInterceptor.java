package com.aegisql.conveyor.service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RestAuditInterceptor implements HandlerInterceptor {

    static final String AUDIT_LOGGER_NAME = "conveyor.audit.rest";
    private static final String ADMIN_EVENTS_POLL_PATH = "/api/dashboard/admin/events";
    private static final String WATCH_POLL_PATH = "/api/dashboard/watch";
    private static final String TREE_POLL_PATH = "/api/dashboard/tree";
    private static final String DASHBOARD_TEST_PLACE_PATH = "/dashboard/test/place";
    private static final String DASHBOARD_TEST_STATIC_PART_PATH = "/dashboard/test/static-part";
    private static final String DASHBOARD_TEST_COMMAND_PATH = "/dashboard/test/command";
    private static final String DASHBOARD_WATCH_PATH = "/dashboard/watch";
    private static final String DASHBOARD_ADMIN_RELOAD_PATH = "/dashboard/admin/reload";
    private static final String DASHBOARD_ADMIN_DELETE_PATH = "/dashboard/admin/delete";
    private static final String DASHBOARD_ADMIN_MBEAN_PATH = "/dashboard/admin/mbean";
    private static final String DASHBOARD_ADMIN_PARAMETER_PATH = "/dashboard/admin/parameter";

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger(AUDIT_LOGGER_NAME);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final boolean enabled;
    private final Clock clock;

    @Autowired
    public RestAuditInterceptor(@Value("${conveyor.service.audit.enabled:false}") boolean enabled) {
        this(enabled, Clock.systemUTC());
    }

    RestAuditInterceptor(boolean enabled, Clock clock) {
        this.enabled = enabled;
        this.clock = clock;
    }

    boolean isEnabled() {
        return enabled;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        Map<String, Object> payload = buildAuditPayload(request, response, ex);
        if (payload == null) {
            return;
        }
        writeAuditPayload(payload);
    }

    Map<String, Object> buildAuditPayload(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception ex
    ) {
        if (!enabled || shouldSkip(request)) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now(clock).toString());
        payload.put("userId", resolveUserId(request));
        EndpointDetails endpointDetails = resolveEndpointDetails(request);
        payload.put("endpoint", endpointDetails.endpoint());
        if (endpointDetails.sourceEndpoint() != null) {
            payload.put("sourceEndpoint", endpointDetails.sourceEndpoint());
        }
        if (endpointDetails.requestKind() != null) {
            payload.put("requestKind", endpointDetails.requestKind());
        }
        payload.put("parameters", collectParameters(request));
        payload.put("bodySize", resolveBodySize(request));
        payload.put("status", response.getStatus());
        if (ex != null) {
            payload.put("exception", ex.getClass().getName());
        }
        return payload;
    }

    void writeAuditPayload(Map<String, Object> payload) {
        AUDIT_LOG.info(formatAuditPayload(payload));
    }

    String formatAuditPayload(Map<String, Object> payload) {
        return toJson(payload);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        return "GET".equalsIgnoreCase(method)
                && uri != null
                && (
                uri.endsWith(ADMIN_EVENTS_POLL_PATH)
                        || uri.endsWith(WATCH_POLL_PATH)
                        || uri.endsWith(TREE_POLL_PATH)
        );
    }

    private EndpointDetails resolveEndpointDetails(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if (!StringUtils.hasText(method) || !StringUtils.hasText(uri)) {
            return new EndpointDetails(String.valueOf(method) + " " + String.valueOf(uri), null, null);
        }

        if ("POST".equalsIgnoreCase(method)) {
            if (DASHBOARD_TEST_PLACE_PATH.equals(uri)) {
                return resolveDashboardPartEndpoint(request, uri);
            }
            if (DASHBOARD_TEST_STATIC_PART_PATH.equals(uri)) {
                return resolveDashboardStaticPartEndpoint(request, uri);
            }
            if (DASHBOARD_TEST_COMMAND_PATH.equals(uri)) {
                return resolveDashboardCommandEndpoint(request, uri);
            }
            if (DASHBOARD_WATCH_PATH.equals(uri)) {
                return new EndpointDetails("POST /api/dashboard/watch", uri, "watch");
            }
            if (DASHBOARD_ADMIN_RELOAD_PATH.equals(uri)) {
                String name = firstRequestParameter(request, "name");
                if (StringUtils.hasText(name)) {
                    return new EndpointDetails("POST /api/dashboard/admin/reload/" + name, uri, "admin-reload");
                }
                return new EndpointDetails("POST /api/dashboard/admin/reload", uri, "admin-reload");
            }
            if (DASHBOARD_ADMIN_DELETE_PATH.equals(uri)) {
                String name = firstRequestParameter(request, "name");
                if (StringUtils.hasText(name)) {
                    return new EndpointDetails("DELETE /api/dashboard/admin/" + name, uri, "admin-delete");
                }
                return new EndpointDetails("DELETE /api/dashboard/admin", uri, "admin-delete");
            }
            if (DASHBOARD_ADMIN_MBEAN_PATH.equals(uri)) {
                String name = firstRequestParameter(request, "name");
                String methodName = firstRequestParameter(request, "method");
                if (StringUtils.hasText(name) && StringUtils.hasText(methodName)) {
                    return new EndpointDetails(
                            "POST /api/dashboard/admin/" + name + "/mbean/" + methodName,
                            uri,
                            "admin-mbean"
                    );
                }
                return new EndpointDetails("POST /api/dashboard/admin/mbean", uri, "admin-mbean");
            }
            if (DASHBOARD_ADMIN_PARAMETER_PATH.equals(uri)) {
                String name = firstRequestParameter(request, "name");
                String parameter = firstRequestParameter(request, "parameter");
                if (StringUtils.hasText(name) && StringUtils.hasText(parameter)) {
                    return new EndpointDetails(
                            "POST /api/dashboard/admin/" + name + "/parameter/" + parameter,
                            uri,
                            "admin-parameter"
                    );
                }
                return new EndpointDetails("POST /api/dashboard/admin/parameter", uri, "admin-parameter");
            }
        }

        String requestKind = requestKindFromPath(uri);
        return new EndpointDetails(method + " " + uri, null, requestKind);
    }

    private EndpointDetails resolveDashboardPartEndpoint(HttpServletRequest request, String sourceUri) {
        String conveyor = firstRequestParameter(request, "name");
        String id = firstRequestParameter(request, "id");
        String label = firstRequestParameter(request, "label");
        boolean forEach = parseBoolean(firstRequestParameter(request, "foreach"));
        String endpoint = "POST /part";
        if (StringUtils.hasText(conveyor) && StringUtils.hasText(label)) {
            if (forEach || !StringUtils.hasText(id)) {
                endpoint = "POST /part/" + conveyor + "/" + label;
            } else {
                endpoint = "POST /part/" + conveyor + "/" + id + "/" + label;
            }
        }
        return new EndpointDetails(endpoint, sourceUri, "part");
    }

    private EndpointDetails resolveDashboardStaticPartEndpoint(HttpServletRequest request, String sourceUri) {
        String conveyor = firstRequestParameter(request, "name");
        String label = firstRequestParameter(request, "label");
        String endpoint = "POST /static-part";
        if (StringUtils.hasText(conveyor) && StringUtils.hasText(label)) {
            endpoint = "POST /static-part/" + conveyor + "/" + label;
        }
        return new EndpointDetails(endpoint, sourceUri, "static-part");
    }

    private EndpointDetails resolveDashboardCommandEndpoint(HttpServletRequest request, String sourceUri) {
        String conveyor = firstRequestParameter(request, "name");
        String id = firstRequestParameter(request, "id");
        String operation = firstRequestParameter(request, "operation");
        boolean forEach = parseBoolean(firstRequestParameter(request, "foreach"));
        String endpoint = "POST /command";
        if (StringUtils.hasText(conveyor) && StringUtils.hasText(operation)) {
            if (forEach || !StringUtils.hasText(id)) {
                endpoint = "POST /command/" + conveyor + "/" + operation;
            } else {
                endpoint = "POST /command/" + conveyor + "/" + id + "/" + operation;
            }
        }
        return new EndpointDetails(endpoint, sourceUri, "command");
    }

    private String requestKindFromPath(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.startsWith("/part/")) {
            return "part";
        }
        if (uri.startsWith("/static-part/")) {
            return "static-part";
        }
        if (uri.startsWith("/command/")) {
            return "command";
        }
        if (uri.startsWith("/api/dashboard/watch")) {
            return "watch";
        }
        if (uri.startsWith("/api/dashboard/admin")) {
            return "admin";
        }
        if (uri.startsWith("/api/dashboard/")) {
            return "dashboard";
        }
        return null;
    }

    private String firstRequestParameter(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean parseBoolean(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "on".equals(normalized) || "1".equals(normalized);
    }

    private String resolveUserId(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal != null && StringUtils.hasText(principal.getName())) {
            return principal.getName();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && StringUtils.hasText(authentication.getName())
                && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private Map<String, Object> collectParameters(HttpServletRequest request) {
        Map<String, Object> all = new LinkedHashMap<>();
        Map<String, String> pathVariables = extractPathVariables(request);
        if (!pathVariables.isEmpty()) {
            all.put("pathVariables", pathVariables);
        }

        Map<String, Object> requestParameters = extractRequestParameters(request);
        if (!requestParameters.isEmpty()) {
            all.put("requestParameters", requestParameters);
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractPathVariables(HttpServletRequest request) {
        Object pathVariablesAttribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(pathVariablesAttribute instanceof Map<?, ?> variables)) {
            return Map.of();
        }
        Map<String, String> pathVariables = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : variables.entrySet()) {
            Object key = entry.getKey();
            if (key == null) {
                continue;
            }
            pathVariables.put(String.valueOf(key), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
        }
        return pathVariables;
    }

    private Map<String, Object> extractRequestParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> requestParameters = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (isRedactedParameter(entry.getKey())) {
                continue;
            }
            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                requestParameters.put(entry.getKey(), null);
            } else if (values.length == 1) {
                requestParameters.put(entry.getKey(), values[0]);
            } else {
                List<String> list = Arrays.asList(values);
                requestParameters.put(entry.getKey(), list);
            }
        }
        return requestParameters;
    }

    private boolean isRedactedParameter(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return "body".equals(normalized) || "bodyfile".equals(normalized);
    }

    private long resolveBodySize(HttpServletRequest request) {
        long contentLength = request.getContentLengthLong();
        if (contentLength < 0) {
            return -1L;
        }
        return contentLength;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return payload.toString();
        }
    }

    private record EndpointDetails(String endpoint, String sourceEndpoint, String requestKind) {
    }
}
