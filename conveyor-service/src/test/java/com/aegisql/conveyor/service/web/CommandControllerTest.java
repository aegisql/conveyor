package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.config.SecurityConfig;
import com.aegisql.conveyor.service.core.CommandService;
import com.aegisql.conveyor.service.core.ConveyorWatchService;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommandController.class)
@Import({SecurityConfig.class, RestExceptionHandler.class})
@ActiveProfiles("demo")
class CommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandService commandService;

    @MockitoBean
    private ConveyorWatchService conveyorWatchService;

    @Test
    void commandByIdReturnsCompletedResult() throws Exception {
        var response = PlacementResult.<Object>builder()
                .status(PlacementStatus.COMPLETED)
                .result(Boolean.TRUE)
                .timestamp(Instant.parse("2026-02-11T10:00:00Z"))
                .correlationId("2")
                .label("cancel")
                .properties(Map.of("conveyor", "collector", "command", "cancel", "correlationId", "2", "foreach", false))
                .build();
        when(commandService.executeById(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenReturn(response);

        mockMvc.perform(post("/command/{conveyor}/{id}/{command}", "collector", "2", "cancel")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("ignored"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.correlationId").value("2"))
                .andExpect(jsonPath("$.properties.command").value("cancel"));
    }

    @Test
    void commandForeachReturnsKeyedMapResult() throws Exception {
        var response = PlacementResult.<Object>builder()
                .status(PlacementStatus.COMPLETED)
                .result(Map.of("A", "v1", "B", "v2"))
                .timestamp(Instant.parse("2026-02-11T10:01:00Z"))
                .label("peek")
                .properties(Map.of("conveyor", "collector", "command", "peek", "foreach", true))
                .build();
        when(commandService.executeForEach(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenReturn(response);

        mockMvc.perform(post("/command/{conveyor}/{command}", "collector", "peek")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.TEXT_PLAIN)
                .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.A").value("v1"))
                .andExpect(jsonPath("$.result.B").value("v2"))
                .andExpect(jsonPath("$.properties.foreach").value(true));
    }

    @Test
    void unknownConveyorReturns404() throws Exception {
        when(commandService.executeById(
                ArgumentMatchers.eq("missing-conveyor"),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenThrow(new ConveyorNotFoundException("missing-conveyor"));

        mockMvc.perform(post("/command/{conveyor}/{id}/{command}", "missing-conveyor", "2", "cancel")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void invalidCommandRequestReturns400() throws Exception {
        when(commandService.executeById(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenThrow(new IllegalArgumentException("peek requires requestTTL"));

        mockMvc.perform(post("/command/{conveyor}/{id}/{command}", "collector", "2", "peek")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void commandEndpointRequiresRestUserRole() throws Exception {
        mockMvc.perform(post("/command/{conveyor}/{id}/{command}", "collector", "2", "cancel")
                        .with(user("viewer").roles("DASHBOARD_VIEWER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(""))
                .andExpect(status().isForbidden());
        verifyNoInteractions(commandService);
    }

    @Test
    void foreachWatchRegistersAndWatchParamsAreStripped() throws Exception {
        var response = PlacementResult.<Object>builder()
                .status(PlacementStatus.IN_PROGRESS)
                .timestamp(Instant.parse("2026-02-11T10:02:00Z"))
                .label("timeout")
                .build();
        when(commandService.executeForEach(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenReturn(response);

        mockMvc.perform(post("/command/{conveyor}/{command}", "collector", "timeout")
                        .with(user("rest").roles("REST_USER"))
                        .param("watchResults", "true")
                        .param("watchLimit", "120")
                        .param("ttl", "2 SECONDS")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(conveyorWatchService).registerWatch("rest", "collector", null, true, 120);
        verify(commandService).executeForEach(
                eq("collector"),
                eq("timeout"),
                ArgumentMatchers.any(byte[].class),
                argThat(params -> !params.containsKey("watchResults")
                        && !params.containsKey("watch")
                        && !params.containsKey("watchLimit")
                        && "2 SECONDS".equals(params.get("ttl")))
        );
    }
}
