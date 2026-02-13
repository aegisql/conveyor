package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.config.SecurityConfig;
import com.aegisql.conveyor.service.core.PlacementService;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.aegisql.conveyor.service.error.UnsupportedMappingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PartController.class)
@Import({SecurityConfig.class, RestExceptionHandler.class})
@ActiveProfiles("demo")
class PartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlacementService placementService;

    @Test
    void placePartWithJsonReturnsCompletedResult() throws Exception {
        var response = PlacementResult.<Boolean>builder()
                .status(PlacementStatus.COMPLETED)
                .result(true)
                .timestamp(Instant.parse("2026-02-08T20:00:00Z"))
                .correlationId("2")
                .label("USER")
                .properties(new HashMap<>(Map.of(
                        "conveyor", "collector",
                        "correlationId", "2",
                        "label", "USER"
                )))
                .build();
        when(placementService.placePart(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenReturn(response);

        mockMvc.perform(post("/part/{conveyor}/{id}/{label}", "collector", "2", "USER")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"age\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.correlationId").value("2"))
                .andExpect(jsonPath("$.label").value("USER"))
                .andExpect(jsonPath("$.properties.conveyor").value("collector"));
    }

    @Test
    void placePartForeachWithLabelOnlyReturnsCompletedResult() throws Exception {
        var response = PlacementResult.<Boolean>builder()
                .status(PlacementStatus.COMPLETED)
                .result(true)
                .timestamp(Instant.parse("2026-02-10T00:00:00Z"))
                .label("USER")
                .properties(new HashMap<>(Map.of(
                        "conveyor", "collector",
                        "label", "USER",
                        "foreach", true
                )))
                .build();
        when(placementService.placePartForEach(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenReturn(response);

        mockMvc.perform(post("/part/{conveyor}/{label}", "collector", "USER")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"age\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.label").value("USER"))
                .andExpect(jsonPath("$.properties.foreach").value(true));
    }

    @Test
    void unknownConveyorReturns404() throws Exception {
        when(placementService.placePart(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq("missing-conveyor"),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenThrow(new ConveyorNotFoundException("missing-conveyor"));

        mockMvc.perform(post("/part/{conveyor}/{id}/{label}", "missing-conveyor", "1", "USER")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"age\":42}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void unsupportedMappingReturns415() throws Exception {
        when(placementService.placePart(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenThrow(new UnsupportedMappingException("Unsupported mapping"));

        mockMvc.perform(post("/part/{conveyor}/{id}/{label}", "collector", "3", "USER")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain-text-value"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void invalidRequestDurationReturns400() throws Exception {
        when(placementService.placePart(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenThrow(new IllegalArgumentException("Invalid duration format: not-a-duration"));

        mockMvc.perform(post("/part/{conveyor}/{id}/{label}", "collector", "4", "USER")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("ttl", "not-a-duration")
                        .content("{\"name\":\"Ann\",\"age\":42}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void restEndpointRequiresRestUserRole() throws Exception {
        mockMvc.perform(post("/part/{conveyor}/{id}/{label}", "collector", "5", "USER")
                        .with(user("viewer").roles("DASHBOARD_VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"age\":42}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(placementService);
    }
}
