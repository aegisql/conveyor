package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.config.SecurityConfig;
import com.aegisql.conveyor.service.core.StaticPartService;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.aegisql.conveyor.service.error.UnsupportedMappingException;
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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StaticPartController.class)
@Import({SecurityConfig.class, RestExceptionHandler.class})
@ActiveProfiles("demo")
class StaticPartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaticPartService staticPartService;

    @Test
    void placeStaticPartReturnsCompletedResult() throws Exception {
        var response = PlacementResult.<Boolean>builder()
                .status(PlacementStatus.COMPLETED)
                .result(true)
                .timestamp(Instant.parse("2026-02-09T13:00:00Z"))
                .label("CONFIG")
                .properties(Map.of("conveyor", "collector", "label", "CONFIG", "delete", false))
                .build();
        when(staticPartService.placeStaticPart(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenReturn(response);

        mockMvc.perform(post("/static-part/{conveyor}/{label}", "collector", "CONFIG")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("sample-value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.label").value("CONFIG"));
    }

    @Test
    void deleteStaticPartDoesNotRequireBody() throws Exception {
        var response = PlacementResult.<Boolean>builder()
                .status(PlacementStatus.COMPLETED)
                .result(true)
                .timestamp(Instant.parse("2026-02-09T13:01:00Z"))
                .label("CONFIG")
                .properties(Map.of("delete", true))
                .build();
        when(staticPartService.placeStaticPart(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq("collector"),
                ArgumentMatchers.eq("CONFIG"),
                ArgumentMatchers.nullable(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenReturn(response);

        mockMvc.perform(post("/static-part/{conveyor}/{label}", "collector", "CONFIG")
                        .with(user("rest").roles("REST_USER"))
                        .queryParam("delete", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void unknownConveyorReturns404() throws Exception {
        when(staticPartService.placeStaticPart(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq("missing-conveyor"),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenThrow(new ConveyorNotFoundException("missing-conveyor"));

        mockMvc.perform(post("/static-part/{conveyor}/{label}", "missing-conveyor", "CONFIG")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("value"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void unsupportedMappingReturns415() throws Exception {
        when(staticPartService.placeStaticPart(
                ArgumentMatchers.any(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(byte[].class),
                ArgumentMatchers.anyMap())
        ).thenThrow(new UnsupportedMappingException("Unsupported mapping"));

        mockMvc.perform(post("/static-part/{conveyor}/{label}", "collector", "CONFIG")
                        .with(user("rest").roles("REST_USER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("value"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void staticPartEndpointRequiresRestUserRole() throws Exception {
        mockMvc.perform(post("/static-part/{conveyor}/{label}", "collector", "CONFIG")
                        .with(user("viewer").roles("DASHBOARD_VIEWER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("value"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(staticPartService);
    }
}
