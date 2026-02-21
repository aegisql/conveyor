package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.config.SecurityConfig;
import com.aegisql.conveyor.service.core.DashboardAdminOperationService;
import com.aegisql.conveyor.service.core.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, RestExceptionHandler.class})
@ActiveProfiles("demo")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private DashboardAdminOperationService dashboardAdminOperationService;

    @Test
    void reloadSchedulesAsyncAdminOperation() throws Exception {
        when(dashboardAdminOperationService.scheduleReload("admin", "collector", "1 MINUTES"))
                .thenReturn(Map.of());

        mockMvc.perform(post("/api/dashboard/admin/reload/{name}", "collector")
                        .with(user("admin").roles("DASHBOARD_ADMIN"))
                        .param("stopTimeout", "1 MINUTES"))
                .andExpect(status().isAccepted());

        verify(dashboardAdminOperationService).scheduleReload("admin", "collector", "1 MINUTES");
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deleteSchedulesAsyncAdminOperation() throws Exception {
        when(dashboardAdminOperationService.scheduleDelete("admin", "collector", "2 MINUTES"))
                .thenReturn(Map.of());

        mockMvc.perform(delete("/api/dashboard/admin/{name}", "collector")
                        .with(user("admin").roles("DASHBOARD_ADMIN"))
                        .param("stopTimeout", "2 MINUTES"))
                .andExpect(status().isAccepted());

        verify(dashboardAdminOperationService).scheduleDelete("admin", "collector", "2 MINUTES");
        verifyNoInteractions(dashboardService);
    }

    @Test
    void adminEventsReturnsQueuedEvents() throws Exception {
        when(dashboardAdminOperationService.drainEvents("admin"))
                .thenReturn(List.of(Map.of(
                        "sourceKey", "admin",
                        "title", "Admin",
                        "status", Map.of(
                                "status", "COMPLETED",
                                "summaryLine", "Conveyor collector reload is complete"
                        ),
                        "payload", Map.of(
                                "operation", "reload",
                                "treeRefresh", true
                        )
                )));

        mockMvc.perform(get("/api/dashboard/admin/events")
                        .with(user("admin").roles("DASHBOARD_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceKey").value("admin"))
                .andExpect(jsonPath("$[0].status.status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].status.summaryLine").value("Conveyor collector reload is complete"))
                .andExpect(jsonPath("$[0].payload.treeRefresh").value(true));

        verify(dashboardAdminOperationService).drainEvents("admin");
        verifyNoInteractions(dashboardService);
    }

    @Test
    void adminEndpointRequiresDashboardAdminRole() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin/events")
                        .with(user("viewer").roles("DASHBOARD_VIEWER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardAdminOperationService);
        verifyNoInteractions(dashboardService);
    }
}
