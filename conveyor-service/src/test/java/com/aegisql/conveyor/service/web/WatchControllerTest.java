package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.config.SecurityConfig;
import com.aegisql.conveyor.service.core.ConveyorWatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WatchController.class)
@Import({SecurityConfig.class, RestExceptionHandler.class})
@ActiveProfiles("demo")
class WatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConveyorWatchService conveyorWatchService;

    @Test
    void activeWatchesAreReturnedForDashboardUser() throws Exception {
        when(conveyorWatchService.activeWatchesForUser("viewer")).thenReturn(List.of(
                Map.of(
                        "watchId", "collector|1",
                        "displayName", "collector 1",
                        "foreach", false,
                        "active", true
                )
        ));

        mockMvc.perform(get("/api/dashboard/watch")
                        .with(user("viewer").roles("DASHBOARD_VIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].watchId").value("collector|1"))
                .andExpect(jsonPath("$[0].displayName").value("collector 1"));
    }

    @Test
    void cancelByWatchIdReturnsResponsePayload() throws Exception {
        when(conveyorWatchService.cancelWatchById("viewer", "collector|*")).thenReturn(true);

        mockMvc.perform(post("/api/dashboard/watch/cancel")
                        .with(user("viewer").roles("DASHBOARD_VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"watchId\":\"collector|*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceled").value(true));
    }

    @Test
    void watchEndpointsRequireDashboardRole() throws Exception {
        mockMvc.perform(get("/api/dashboard/watch")
                        .with(user("rest").roles("REST_USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conveyorWatchService);
    }

    @Test
    void cancelByIdAndConveyorWithoutIdReturnsBadRequest() throws Exception {
        when(conveyorWatchService.cancelWatch(anyString(), eq("collector"), eq(null), eq(true))).thenReturn(true);

        mockMvc.perform(post("/api/dashboard/watch/cancel")
                        .with(user("viewer").roles("DASHBOARD_VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conveyor\":\"collector\",\"foreach\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceled").value(true));

        mockMvc.perform(post("/api/dashboard/watch/cancel")
                        .with(user("viewer").roles("DASHBOARD_VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conveyor\":\"collector\",\"foreach\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }
}
