package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.core.DashboardAdminOperationService;
import com.aegisql.conveyor.service.core.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerUnitTest {

    @Mock
    private DashboardService dashboardService;
    @Mock
    private DashboardAdminOperationService dashboardAdminOperationService;

    private DashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardController(dashboardService, dashboardAdminOperationService);
    }

    @Test
    void treeAndInspectDelegateToDashboardService() {
        Map<String, Map<String, ?>> tree = Map.of("root", Map.of());
        Map<String, Object> inspect = Map.of("name", "collector");
        when(dashboardService.conveyorTree()).thenReturn(tree);
        when(dashboardService.inspect("collector")).thenReturn(inspect);

        assertThat(controller.tree()).isEqualTo(tree);
        assertThat(controller.inspect("collector")).isEqualTo(inspect);

        verify(dashboardService).conveyorTree();
        verify(dashboardService).inspect("collector");
    }

    @Test
    void uploadInvokeAndUpdateParameterDelegate() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(dashboardService.invokeMBean("collector", "reset", Map.of("force", true)))
                .thenReturn(Map.of("ok", true));
        var invokeResponse = controller.invoke("collector", "reset", Map.of("force", true));

        assertThat(controller.upload(file).getStatusCode().value()).isEqualTo(200);
        assertThat(invokeResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(invokeResponse.getBody()).isEqualTo(Map.of("ok", true));
        assertThat(controller.updateParameter("collector", "capacity", "4").getStatusCode().value()).isEqualTo(200);

        verify(dashboardService).upload(file);
        verify(dashboardService).invokeMBean("collector", "reset", Map.of("force", true));
        verify(dashboardService).updateParameter("collector", "capacity", "4");
    }

    @Test
    void reloadDeleteAndEventsUseAuthenticatedUsername() {
        var adminAuth = new UsernamePasswordAuthenticationToken("admin", "n/a");
        when(dashboardAdminOperationService.drainEvents("admin"))
                .thenReturn(List.of(Map.of("title", "Admin")));

        assertThat(controller.reload("collector", "1 MINUTES", adminAuth).getStatusCode().value()).isEqualTo(202);
        assertThat(controller.delete("collector", "2 MINUTES", adminAuth).getStatusCode().value()).isEqualTo(202);
        assertThat(controller.adminEvents(adminAuth)).hasSize(1);

        verify(dashboardAdminOperationService).scheduleReload("admin", "collector", "1 MINUTES");
        verify(dashboardAdminOperationService).scheduleDelete("admin", "collector", "2 MINUTES");
        verify(dashboardAdminOperationService).drainEvents("admin");
        verifyNoInteractions(dashboardService);
    }

    @Test
    void adminMethodsRequireAuthenticatedUser() {
        assertThatThrownBy(() -> controller.reload("collector", "1 MINUTES", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user is required");
        assertThatThrownBy(() -> controller.delete("collector", "1 MINUTES", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user is required");
        assertThatThrownBy(() -> controller.adminEvents(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user is required");

        var blankUser = new UsernamePasswordAuthenticationToken("", "n/a");
        assertThatThrownBy(() -> controller.reload("collector", "1 MINUTES", blankUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user is required");
    }
}
