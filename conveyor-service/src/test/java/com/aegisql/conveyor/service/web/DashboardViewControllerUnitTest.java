package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.core.CommandService;
import com.aegisql.conveyor.service.core.ConveyorWatchService;
import com.aegisql.conveyor.service.core.DashboardAdminOperationService;
import com.aegisql.conveyor.service.core.DashboardService;
import com.aegisql.conveyor.service.core.PlacementService;
import com.aegisql.conveyor.service.core.StaticPartService;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.aegisql.conveyor.service.error.UnsupportedMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardViewControllerUnitTest {

    @Mock
    private DashboardAdminOperationService dashboardAdminOperationService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private PlacementService placementService;
    @Mock
    private StaticPartService staticPartService;
    @Mock
    private CommandService commandService;
    @Mock
    private ConveyorWatchService conveyorWatchService;

    private DashboardViewController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardViewController(
                dashboardAdminOperationService,
                dashboardService,
                placementService,
                staticPartService,
                commandService,
                conveyorWatchService,
                new ObjectMapper(),
                5,
                9,
                " 10 SECONDS ",
                " 20 SECONDS ",
                " 1 MINUTES "
        );
    }

    @Test
    void rootRedirects() {
        assertThat(controller.root()).isEqualTo("redirect:/dashboard");
    }

    @Test
    void dashboardPopulatesModelAndNormalizesTab() throws Exception {
        when(dashboardService.conveyorTree()).thenReturn(Map.of("root", Map.of()));
        when(dashboardService.inspect("main")).thenReturn(Map.of("name", "main"));
        when(dashboardService.drainLoaderErrors()).thenReturn(List.of("load-error"));
        when(dashboardService.isUploadEnabled()).thenReturn(true);

        var model = new ExtendedModelMap();
        model.addAttribute("dashboardOutputEvent", Map.of("source", "x"));
        model.addAttribute("testRequest", Map.of("custom", true));

        String view = controller.dashboard("main", "unknown-tab", adminAuth(), model);

        assertThat(view).isEqualTo("dashboard");
        assertThat(model.getAttribute("activeTab")).isEqualTo("tab-details");
        assertThat(model.getAttribute("selectedName")).isEqualTo("main");
        assertThat(model.getAttribute("username")).isEqualTo("alice");
        assertThat(model.getAttribute("isAdmin")).isEqualTo(true);
        assertThat(model.getAttribute("uploadEnabled")).isEqualTo(true);
        assertThat(model.getAttribute("defaultTtlInputValue")).isEqualTo("10 SECONDS");
        assertThat(model.getAttribute("defaultRequestTtlInputValue")).isEqualTo("20 SECONDS");
        assertThat(String.valueOf(model.getAttribute("dashboardOutputEventJson"))).contains("\"source\":\"x\"");
        assertThat(model.getAttribute("testRequest")).isEqualTo(Map.of("custom", true));
    }

    @Test
    void dashboardUsesOauthDisplayNameAndCapturesInspectErrors() throws Exception {
        when(dashboardService.conveyorTree()).thenReturn(Map.of());
        when(dashboardService.inspect("missing")).thenThrow(new IllegalStateException("inspect failed"));
        when(dashboardService.drainLoaderErrors()).thenReturn(List.of());
        when(dashboardService.isUploadEnabled()).thenReturn(false);

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_DASHBOARD_VIEWER"));
        var oauthUser = new DefaultOAuth2User(
                authorities,
                Map.of("given_name", "Ada", "family_name", "Lovelace"),
                "given_name"
        );
        var auth = new OAuth2AuthenticationToken(oauthUser, authorities, "facebook");
        var model = new ExtendedModelMap();

        String view = controller.dashboard("missing", "tab-admin", auth, model);

        assertThat(view).isEqualTo("dashboard");
        assertThat(model.getAttribute("activeTab")).isEqualTo("tab-admin");
        assertThat(model.getAttribute("username")).isEqualTo("Ada Lovelace");
        assertThat(model.getAttribute("error")).isEqualTo("inspect failed");
        assertThat(model.getAttribute("isAdmin")).isEqualTo(false);
    }

    @Test
    void uploadHandlesSuccessAndFailure() throws Exception {
        var file = mockFile();
        var success = new RedirectAttributesModelMap();
        assertThat(controller.upload(file, "tab-admin", success)).isEqualTo("redirect:/dashboard");
        assertThat(flash(success).get("message")).isEqualTo("Upload successful");
        assertThat(success.get("tab")).isEqualTo("tab-admin");

        doThrow(new IllegalArgumentException("upload failed")).when(dashboardService).upload(file);
        var failure = new RedirectAttributesModelMap();
        assertThat(controller.upload(file, "bad-tab", failure)).isEqualTo("redirect:/dashboard");
        assertThat(flash(failure).get("error")).isEqualTo("upload failed");
        assertThat(failure.get("tab")).isEqualTo("tab-details");
    }

    @Test
    void reloadAndDeletePopulateDashboardOutputEvent() {
        Map<String, Object> scheduled = Map.of("status", "SCHEDULED");
        when(dashboardAdminOperationService.scheduleReload("alice", "main", " 2 MINUTES ")).thenReturn(scheduled);

        var reloadAttrs = new RedirectAttributesModelMap();
        assertThat(controller.reload("main", " 2 MINUTES ", "tab-admin", adminAuth(), reloadAttrs))
                .isEqualTo("redirect:/dashboard");
        assertThat(flash(reloadAttrs).get("dashboardOutputEvent")).isEqualTo(scheduled);
        assertThat(flash(reloadAttrs).get("adminStopTimeoutInputValue")).isEqualTo("2 MINUTES");
        assertThat(reloadAttrs.get("name")).isEqualTo("main");

        when(dashboardAdminOperationService.scheduleDelete("alice", "main", ""))
                .thenThrow(new ConveyorNotFoundException("main"));
        var deleteAttrs = new RedirectAttributesModelMap();
        assertThat(controller.delete("main", "", "tab-admin", adminAuth(), deleteAttrs))
                .isEqualTo("redirect:/dashboard");
        Map<String, Object> event = castMap(deleteAttrs.getFlashAttributes().get("dashboardOutputEvent"));
        Map<String, Object> status = castMap(event.get("status"));
        assertThat(status.get("httpStatus")).isEqualTo(404);
        assertThat(status.get("errorCode")).isEqualTo("NOT_FOUND");
    }

    @Test
    void invokeMbeanAndUpdateParameterHandleSuccessAndFailures() {
        when(dashboardService.invokeMBean(eq("main"), eq("reset"), any())).thenReturn(Map.of("ok", true));

        var invokeSuccess = new RedirectAttributesModelMap();
        assertThat(controller.invokeMbean("main", "reset", "{\"a\":1}", "tab-details", invokeSuccess))
                .isEqualTo("redirect:/dashboard");
        assertThat(flash(invokeSuccess).get("message")).isEqualTo("Invoked");
        Map<String, Object> invokeEvent = castMap(invokeSuccess.getFlashAttributes().get("dashboardOutputEvent"));
        Map<String, Object> payload = castMap(invokeEvent.get("payload"));
        assertThat(payload.get("operation")).isEqualTo("mbean.reset");

        when(dashboardService.invokeMBean(eq("main"), eq("bad"), any()))
                .thenThrow(new UnsupportedMappingException("unsupported"));
        var invokeFailure = new RedirectAttributesModelMap();
        assertThat(controller.invokeMbean("main", "bad", "raw", "tab-details", invokeFailure))
                .isEqualTo("redirect:/dashboard");
        Map<String, Object> failedEvent = castMap(invokeFailure.getFlashAttributes().get("dashboardOutputEvent"));
        Map<String, Object> failedStatus = castMap(failedEvent.get("status"));
        assertThat(failedStatus.get("httpStatus")).isEqualTo(415);

        var updateSuccess = new RedirectAttributesModelMap();
        assertThat(controller.updateParameter("main", "capacity", "20", "tab-admin", updateSuccess))
                .isEqualTo("redirect:/dashboard");
        assertThat(flash(updateSuccess).get("message")).isEqualTo("Parameter updated");

        doThrow(new IllegalArgumentException("bad value"))
                .when(dashboardService).updateParameter("main", "capacity", "bad");
        var updateFailure = new RedirectAttributesModelMap();
        assertThat(controller.updateParameter("main", "capacity", "bad", "tab-admin", updateFailure))
                .isEqualTo("redirect:/dashboard");
        assertThat(flash(updateFailure).get("error")).isEqualTo("bad value");
    }

    @Test
    void watchConveyorHandlesSuccessAndValidationErrors() {
        when(dashboardService.isTopLevelConveyor("main")).thenReturn(true);
        var success = new RedirectAttributesModelMap();
        assertThat(controller.watchConveyor("main", "7", "tab-operations", adminAuth(), success))
                .isEqualTo("redirect:/dashboard");
        verify(conveyorWatchService).registerWatch("alice", "main", null, true, 7);
        assertThat(flash(success).get("message")).isEqualTo("Watch started for main");

        when(dashboardService.isTopLevelConveyor("nested")).thenReturn(false);
        var failure = new RedirectAttributesModelMap();
        assertThat(controller.watchConveyor("nested", "1", "tab-operations", adminAuth(), failure))
                .isEqualTo("redirect:/dashboard");
        assertThat(String.valueOf(failure.getFlashAttributes().get("error")))
                .contains("top-level");
    }

    @Test
    void placeTestPartHandlesSuccessAndFailureWithWatchCancel() {
        when(dashboardService.isTopLevelConveyor("main")).thenReturn(true);
        PlacementResult<Boolean> placed = PlacementResult.<Boolean>builder()
                .status(PlacementStatus.COMPLETED)
                .result(Boolean.TRUE)
                .label("L1")
                .build();
        when(placementService.placePart(eq("application/json"), eq("main"), eq("42"), eq("L1"), any(), anyMap()))
                .thenReturn(placed);

        var success = new RedirectAttributesModelMap();
        assertThat(controller.placeTestPart(
                "main",
                "42",
                null,
                "true",
                "2",
                "L1",
                "application/json",
                "{\"v\":1}",
                null,
                "1 SECONDS",
                null,
                null,
                null,
                "3 SECONDS",
                List.of("x-trace"),
                List.of("abc"),
                "tab-tester",
                adminAuth(),
                success
        )).isEqualTo("redirect:/dashboard");
        verify(conveyorWatchService).registerWatch("alice", "main", "42", false, 2);
        verify(conveyorWatchService, never()).cancelWatch(anyString(), anyString(), anyString(), anyBoolean());
        assertThat(flash(success).get("message")).isEqualTo("Part test submitted");
        Map<String, Object> successEvent = castMap(flash(success).get("dashboardOutputEvent"));
        Map<String, Object> successStatus = castMap(successEvent.get("status"));
        assertThat(successStatus.get("httpStatus")).isEqualTo(200);
        assertThat(successStatus.get("status")).isEqualTo("COMPLETED");

        when(placementService.placePartForEach(eq("application/json"), eq("main"), eq("L1"), any(), anyMap()))
                .thenThrow(new IllegalStateException("placement failed"));
        var failure = new RedirectAttributesModelMap();
        assertThat(controller.placeTestPart(
                "main",
                null,
                "on",
                "true",
                "2",
                "L1",
                "application/json",
                "{\"v\":1}",
                null,
                "1 SECONDS",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                "tab-tester",
                adminAuth(),
                failure
        )).isEqualTo("redirect:/dashboard");
        verify(conveyorWatchService).cancelWatch("alice", "main", null, true);
        assertThat(flash(failure).get("error")).isEqualTo("placement failed");
    }

    @Test
    void placeTestPartUsesAcceptedHttpStatusWhenInProgress() {
        when(dashboardService.isTopLevelConveyor("main")).thenReturn(true);
        PlacementResult<Boolean> inProgress = PlacementResult.<Boolean>builder()
                .status(PlacementStatus.IN_PROGRESS)
                .label("L1")
                .build();
        when(placementService.placePart(eq("application/json"), eq("main"), eq("42"), eq("L1"), any(), anyMap()))
                .thenReturn(inProgress);

        var attrs = new RedirectAttributesModelMap();
        assertThat(controller.placeTestPart(
                "main",
                "42",
                null,
                null,
                null,
                "L1",
                "application/json",
                "{\"v\":1}",
                null,
                "1 SECONDS",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                "tab-tester",
                adminAuth(),
                attrs
        )).isEqualTo("redirect:/dashboard");

        Map<String, Object> event = castMap(attrs.getFlashAttributes().get("dashboardOutputEvent"));
        Map<String, Object> status = castMap(event.get("status"));
        assertThat(status.get("httpStatus")).isEqualTo(202);
        assertThat(status.get("status")).isEqualTo("IN_PROGRESS");
    }

    @Test
    void placeStaticPartAndCommandHandleSuccessAndFailure() {
        PlacementResult<Boolean> staticResult = PlacementResult.<Boolean>builder()
                .status(PlacementStatus.COMPLETED)
                .result(Boolean.TRUE)
                .label("S1")
                .build();
        when(staticPartService.placeStaticPart(eq("text/plain"), eq("main"), eq("S1"), any(), anyMap()))
                .thenReturn(staticResult);

        var staticSuccess = new RedirectAttributesModelMap();
        assertThat(controller.placeTestStaticPart(
                "main",
                "S1",
                "text/plain",
                "true",
                "",
                null,
                "1",
                "2 SECONDS",
                List.of("k"),
                List.of("v"),
                "tab-static",
                staticSuccess
        )).isEqualTo("redirect:/dashboard");
        assertThat(flash(staticSuccess).get("message")).isEqualTo("Static part test submitted");
        Map<String, Object> staticSuccessEvent = castMap(flash(staticSuccess).get("dashboardOutputEvent"));
        Map<String, Object> staticSuccessStatus = castMap(staticSuccessEvent.get("status"));
        assertThat(staticSuccessStatus.get("httpStatus")).isEqualTo(200);
        assertThat(staticSuccessStatus.get("status")).isEqualTo("COMPLETED");

        doThrow(new UnsupportedMappingException("bad static"))
                .when(staticPartService).placeStaticPart(eq("text/plain"), eq("main"), eq("S2"), any(), anyMap());
        var staticFailure = new RedirectAttributesModelMap();
        assertThat(controller.placeTestStaticPart(
                "main",
                "S2",
                "text/plain",
                null,
                "v",
                null,
                null,
                null,
                List.of(),
                List.of(),
                "tab-static",
                staticFailure
        )).isEqualTo("redirect:/dashboard");
        assertThat(flash(staticFailure).get("error")).isEqualTo("bad static");

        PlacementResult<Object> commandResult = PlacementResult.builder()
                .status(PlacementStatus.COMPLETED)
                .result(Boolean.TRUE)
                .label("cancel")
                .build();
        when(commandService.executeById(eq("main"), eq("42"), eq("cancel"), any(), anyMap()))
                .thenReturn(commandResult);
        var commandSuccess = new RedirectAttributesModelMap();
        assertThat(controller.executeTestCommand(
                "main",
                "42",
                null,
                null,
                null,
                "cancel",
                "payload",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                "tab-commands",
                adminAuth(),
                commandSuccess
        )).isEqualTo("redirect:/dashboard");
        assertThat(flash(commandSuccess).get("message")).isEqualTo("Command submitted");
        Map<String, Object> commandSuccessEvent = castMap(flash(commandSuccess).get("dashboardOutputEvent"));
        Map<String, Object> commandSuccessStatus = castMap(commandSuccessEvent.get("status"));
        assertThat(commandSuccessStatus.get("httpStatus")).isEqualTo(200);
        assertThat(commandSuccessStatus.get("status")).isEqualTo("COMPLETED");
        verify(commandService).executeById(eq("main"), eq("42"), eq("cancel"), any(), anyMap());

        when(commandService.executeForEach(eq("main"), eq("cancel"), any(), anyMap()))
                .thenThrow(new IllegalStateException("command failed"));
        var commandFailure = new RedirectAttributesModelMap();
        assertThat(controller.executeTestCommand(
                "main",
                null,
                "true",
                "true",
                "3",
                "cancel",
                "payload",
                "1 SECONDS",
                null,
                null,
                null,
                List.of(),
                List.of(),
                "tab-commands",
                adminAuth(),
                commandFailure
        )).isEqualTo("redirect:/dashboard");
        verify(conveyorWatchService).cancelWatch("alice", "main", null, true);
        assertThat(flash(commandFailure).get("error")).isEqualTo("command failed");
    }

    @Test
    void placeStaticPartUsesAcceptedHttpStatusWhenInProgress() {
        PlacementResult<Boolean> inProgress = PlacementResult.<Boolean>builder()
                .status(PlacementStatus.IN_PROGRESS)
                .label("S1")
                .build();
        when(staticPartService.placeStaticPart(eq("text/plain"), eq("main"), eq("S1"), any(), anyMap()))
                .thenReturn(inProgress);

        var attrs = new RedirectAttributesModelMap();
        assertThat(controller.placeTestStaticPart(
                "main",
                "S1",
                "text/plain",
                null,
                "value",
                null,
                null,
                null,
                List.of(),
                List.of(),
                "tab-static",
                attrs
        )).isEqualTo("redirect:/dashboard");

        Map<String, Object> event = castMap(attrs.getFlashAttributes().get("dashboardOutputEvent"));
        Map<String, Object> status = castMap(event.get("status"));
        assertThat(status.get("httpStatus")).isEqualTo(202);
        assertThat(status.get("status")).isEqualTo("IN_PROGRESS");
    }

    @Test
    void executeCommandUsesAcceptedHttpStatusWhenInProgress() {
        PlacementResult<Object> inProgress = PlacementResult.builder()
                .status(PlacementStatus.IN_PROGRESS)
                .label("cancel")
                .build();
        when(commandService.executeById(eq("main"), eq("42"), eq("cancel"), any(), anyMap()))
                .thenReturn(inProgress);

        var attrs = new RedirectAttributesModelMap();
        assertThat(controller.executeTestCommand(
                "main",
                "42",
                null,
                null,
                null,
                "cancel",
                "payload",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                "tab-commands",
                adminAuth(),
                attrs
        )).isEqualTo("redirect:/dashboard");

        Map<String, Object> event = castMap(attrs.getFlashAttributes().get("dashboardOutputEvent"));
        Map<String, Object> status = castMap(event.get("status"));
        assertThat(status.get("httpStatus")).isEqualTo(202);
        assertThat(status.get("status")).isEqualTo("IN_PROGRESS");
    }

    @Test
    void executeCommandRejectsInvalidWatchLimit() {
        var attrs = new RedirectAttributesModelMap();
        assertThatThrownBy(() -> controller.executeTestCommand(
                "main",
                "42",
                null,
                "true",
                "0",
                "cancel",
                "",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                "tab-commands",
                adminAuth(),
                attrs
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("watchLimit must be a positive integer");
        verify(commandService, never()).executeById(anyString(), anyString(), anyString(), any(), anyMap());
    }

    @Test
    void placeTestPartUsesUploadedBodyWhenProvided() throws Exception {
        when(dashboardService.isTopLevelConveyor("main")).thenReturn(true);
        PlacementResult<Boolean> placed = PlacementResult.<Boolean>builder()
                .status(PlacementStatus.COMPLETED)
                .result(Boolean.TRUE)
                .label("LBL")
                .build();
        when(placementService.placePart(eq("application/octet-stream"), eq("main"), eq("99"), eq("LBL"), any(), anyMap()))
                .thenReturn(placed);

        MultipartFile bodyFile = mockFile();
        when(bodyFile.isEmpty()).thenReturn(false);
        when(bodyFile.getBytes()).thenReturn("from-file".getBytes(StandardCharsets.UTF_8));

        var attrs = new RedirectAttributesModelMap();
        controller.placeTestPart(
                "main",
                "99",
                null,
                null,
                null,
                "LBL",
                "application/octet-stream",
                "inline",
                bodyFile,
                "1 SECONDS",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                "tab-tester",
                adminAuth(),
                attrs
        );

        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(placementService).placePart(eq("application/octet-stream"), eq("main"), eq("99"), eq("LBL"), bodyCaptor.capture(), anyMap());
        assertThat(new String(bodyCaptor.getValue(), StandardCharsets.UTF_8)).isEqualTo("from-file");
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(
                "alice",
                "n/a",
                List.of(
                        new SimpleGrantedAuthority("ROLE_DASHBOARD_VIEWER"),
                        new SimpleGrantedAuthority("ROLE_DASHBOARD_ADMIN")
                )
        );
    }

    private MultipartFile mockFile() {
        return org.mockito.Mockito.mock(MultipartFile.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flash(RedirectAttributesModelMap attributes) {
        return (Map<String, Object>) (Map<?, ?>) attributes.getFlashAttributes();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
