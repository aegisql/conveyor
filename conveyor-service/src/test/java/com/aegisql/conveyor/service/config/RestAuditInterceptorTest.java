package com.aegisql.conveyor.service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class RestAuditInterceptorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void logsAuditEventWithoutBodyContent(CapturedOutput output) throws Exception {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-24T15:00:00Z"), ZoneOffset.UTC);
        RestAuditInterceptor interceptor = new RestAuditInterceptor(true, fixedClock);

        byte[] body = "{\"secret\":\"token\"}".getBytes(StandardCharsets.UTF_8);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/part/collector/1/A");
        request.setUserPrincipal(() -> "admin");
        request.setContentType("application/json");
        request.setContent(body);
        request.addParameter("priority", "5");
        request.addParameter("requestTTL", "1 MINUTES");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of(
                "conveyor", "collector",
                "id", "1",
                "label", "A"
        ));

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(202);

        interceptor.afterCompletion(request, response, new Object(), null);

        String json = lastAuditJson(output);
        assertThat(json).doesNotContain("secret");
        assertThat(json).doesNotContain("token");

        Map<String, Object> payload = OBJECT_MAPPER.readValue(json, new TypeReference<>() {
        });
        assertThat(payload.get("timestamp")).isEqualTo("2026-02-24T15:00:00Z");
        assertThat(payload.get("userId")).isEqualTo("admin");
        assertThat(payload.get("endpoint")).isEqualTo("POST /part/collector/1/A");
        assertThat(payload.get("bodySize")).isEqualTo(body.length);
        assertThat(payload.get("status")).isEqualTo(202);

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) payload.get("parameters");
        assertThat(parameters).containsKeys("pathVariables", "requestParameters");

        @SuppressWarnings("unchecked")
        Map<String, Object> pathVariables = (Map<String, Object>) parameters.get("pathVariables");
        assertThat(pathVariables).containsEntry("conveyor", "collector");
        assertThat(pathVariables).containsEntry("id", "1");
        assertThat(pathVariables).containsEntry("label", "A");

        @SuppressWarnings("unchecked")
        Map<String, Object> requestParameters = (Map<String, Object>) parameters.get("requestParameters");
        assertThat(requestParameters).containsEntry("priority", "5");
        assertThat(requestParameters).containsEntry("requestTTL", "1 MINUTES");
    }

    @Test
    void skipsLoggingWhenDisabled(CapturedOutput output) {
        RestAuditInterceptor interceptor = new RestAuditInterceptor(false, Clock.systemUTC());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard/tree");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(lastAuditJson(output)).isNull();
    }

    @Test
    void usesAnonymousAndUnknownBodySizeWhenNotAvailable(CapturedOutput output) throws Exception {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-24T16:30:00Z"), ZoneOffset.UTC);
        RestAuditInterceptor interceptor = new RestAuditInterceptor(true, fixedClock);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard/collector");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        Map<String, Object> payload = OBJECT_MAPPER.readValue(
                lastAuditJson(output),
                new TypeReference<>() {
                }
        );
        assertThat(payload.get("userId")).isEqualTo("anonymous");
        assertThat(payload.get("bodySize")).isEqualTo(-1);
    }

    @Test
    void skipsAdminEventsPollingNoise(CapturedOutput output) {
        RestAuditInterceptor interceptor = new RestAuditInterceptor(true, Clock.systemUTC());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard/admin/events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(lastAuditJson(output)).isNull();
    }

    @Test
    void skipsWatchPollingNoise(CapturedOutput output) {
        RestAuditInterceptor interceptor = new RestAuditInterceptor(true, Clock.systemUTC());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard/watch");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(lastAuditJson(output)).isNull();
    }

    @Test
    void skipsTreePollingNoise(CapturedOutput output) {
        RestAuditInterceptor interceptor = new RestAuditInterceptor(true, Clock.systemUTC());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard/tree");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(lastAuditJson(output)).isNull();
    }

    @Test
    void rewritesDashboardPlaceEndpointAndRedactsBodyParameter(CapturedOutput output) throws Exception {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-24T16:45:00Z"), ZoneOffset.UTC);
        RestAuditInterceptor interceptor = new RestAuditInterceptor(true, fixedClock);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/dashboard/test/place");
        request.setUserPrincipal(() -> "admin");
        request.addParameter("name", "collector");
        request.addParameter("id", "1");
        request.addParameter("label", "TEST");
        request.addParameter("contentType", "application/json");
        request.addParameter("body", "{\"test\":\"me\"}");
        request.addParameter("requestTTL", "100");
        request.addParameter("tab", "tab-tester");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(302);

        interceptor.afterCompletion(request, response, new Object(), null);

        Map<String, Object> payload = OBJECT_MAPPER.readValue(
                lastAuditJson(output),
                new TypeReference<>() {
                }
        );

        assertThat(payload.get("endpoint")).isEqualTo("POST /part/collector/1/TEST");
        assertThat(payload.get("sourceEndpoint")).isEqualTo("/dashboard/test/place");
        assertThat(payload.get("requestKind")).isEqualTo("part");
        assertThat(payload.get("status")).isEqualTo(302);

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) payload.get("parameters");
        @SuppressWarnings("unchecked")
        Map<String, Object> requestParameters = (Map<String, Object>) parameters.get("requestParameters");
        assertThat(requestParameters).doesNotContainKey("body");
        assertThat(requestParameters).containsEntry("name", "collector");
        assertThat(requestParameters).containsEntry("id", "1");
        assertThat(requestParameters).containsEntry("label", "TEST");
    }

    @Test
    void rewritesDashboardStaticPartEndpointAndRedactsBodyFileParameter(CapturedOutput output) throws Exception {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-24T16:46:00Z"), ZoneOffset.UTC);
        RestAuditInterceptor interceptor = new RestAuditInterceptor(true, fixedClock);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/dashboard/test/static-part");
        request.setUserPrincipal(() -> "admin");
        request.addParameter("name", "collector");
        request.addParameter("label", "USER");
        request.addParameter("contentType", "application/json");
        request.addParameter("bodyFile", "/tmp/sample.json");
        request.addParameter("ttl", "1 MINUTES");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        Map<String, Object> payload = OBJECT_MAPPER.readValue(
                lastAuditJson(output),
                new TypeReference<>() {
                }
        );

        assertThat(payload.get("endpoint")).isEqualTo("POST /static-part/collector/USER");
        assertThat(payload.get("sourceEndpoint")).isEqualTo("/dashboard/test/static-part");
        assertThat(payload.get("requestKind")).isEqualTo("static-part");

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) payload.get("parameters");
        @SuppressWarnings("unchecked")
        Map<String, Object> requestParameters = (Map<String, Object>) parameters.get("requestParameters");
        assertThat(requestParameters).doesNotContainKey("bodyFile");
        assertThat(requestParameters).containsEntry("name", "collector");
        assertThat(requestParameters).containsEntry("label", "USER");
    }

    @Test
    void rewritesDashboardCommandEndpoint(CapturedOutput output) throws Exception {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-24T16:47:00Z"), ZoneOffset.UTC);
        RestAuditInterceptor interceptor = new RestAuditInterceptor(true, fixedClock);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/dashboard/test/command");
        request.setUserPrincipal(() -> "admin");
        request.addParameter("name", "collector");
        request.addParameter("id", "9");
        request.addParameter("operation", "cancel");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        Map<String, Object> payload = OBJECT_MAPPER.readValue(
                lastAuditJson(output),
                new TypeReference<>() {
                }
        );

        assertThat(payload.get("endpoint")).isEqualTo("POST /command/collector/9/cancel");
        assertThat(payload.get("sourceEndpoint")).isEqualTo("/dashboard/test/command");
        assertThat(payload.get("requestKind")).isEqualTo("command");
    }

    private String lastAuditJson(CapturedOutput output) {
        return Arrays.stream(output.getAll().split("\\R"))
                .map(String::trim)
                .filter(line -> line.contains("\"timestamp\"") && line.contains("\"status\""))
                .reduce((first, second) -> second)
                .map(line -> line.substring(line.indexOf('{')))
                .orElse(null);
    }
}
