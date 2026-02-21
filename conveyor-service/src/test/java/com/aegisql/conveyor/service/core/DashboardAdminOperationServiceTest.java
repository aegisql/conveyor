package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardAdminOperationServiceTest {

    @Mock
    private DashboardService dashboardService;

    private DashboardAdminOperationService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    void scheduleReloadReturnsScheduledEventAndEmitsCompletedEvent() {
        service = new DashboardAdminOperationService(dashboardService);

        Map<String, Object> scheduled = service.scheduleReload("  admin  ", "  collector  ", " 1 MINUTES ");
        assertThat(scheduled).containsEntry("sourceType", "admin")
                .containsEntry("sourceKey", "admin")
                .containsEntry("title", "Admin");

        Map<String, Object> scheduledStatus = castMap(scheduled.get("status"));
        assertThat(scheduledStatus.get("httpStatus")).isEqualTo(202);
        assertThat(scheduledStatus.get("status")).isEqualTo("SCHEDULED");

        Map<String, Object> completed = awaitSingleEvent("admin");
        Map<String, Object> completedStatus = castMap(completed.get("status"));
        Map<String, Object> completedPayload = castMap(completed.get("payload"));

        assertThat(completedStatus.get("httpStatus")).isEqualTo(200);
        assertThat(completedStatus.get("status")).isEqualTo("COMPLETED");
        assertThat(completedPayload.get("phase")).isEqualTo("completed");
        assertThat(completedPayload.get("treeRefresh")).isEqualTo(Boolean.TRUE);

        verify(dashboardService).reload("collector", "1 MINUTES");
        assertThat(service.drainEvents("admin")).isEmpty();
    }

    @Test
    void scheduleDeleteMapsRootCauseToBadRequestFailure() {
        service = new DashboardAdminOperationService(dashboardService);
        doThrow(new RuntimeException(new IllegalArgumentException("bad timeout")))
                .when(dashboardService)
                .delete("collector", "5 SECONDS");

        service.scheduleDelete("admin", "collector", "5 SECONDS");
        Map<String, Object> failed = awaitSingleEvent("admin");

        Map<String, Object> status = castMap(failed.get("status"));
        Map<String, Object> payload = castMap(failed.get("payload"));

        assertThat(status.get("httpStatus")).isEqualTo(400);
        assertThat(status.get("status")).isEqualTo("FAILED");
        assertThat(status.get("errorCode")).isEqualTo("BAD_REQUEST");
        assertThat(String.valueOf(status.get("errorMessage"))).contains("bad timeout");
        assertThat(payload.get("phase")).isEqualTo("failed");
    }

    @Test
    void scheduleReloadMapsNotFoundFailure() {
        service = new DashboardAdminOperationService(dashboardService);
        doThrow(new ConveyorNotFoundException("missing"))
                .when(dashboardService)
                .reload("missing", null);

        service.scheduleReload("admin", "missing", null);
        Map<String, Object> failed = awaitSingleEvent("admin");

        Map<String, Object> status = castMap(failed.get("status"));
        assertThat(status.get("httpStatus")).isEqualTo(404);
        assertThat(status.get("errorCode")).isEqualTo("NOT_FOUND");
        assertThat(status.get("status")).isEqualTo("FAILED");
    }

    @Test
    void scheduleRejectsMissingUserOrConveyor() {
        service = new DashboardAdminOperationService(dashboardService);

        assertThatThrownBy(() -> service.scheduleReload(" ", "collector", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user is required");

        assertThatThrownBy(() -> service.scheduleDelete("admin", " ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conveyor name is required");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private Map<String, Object> awaitSingleEvent(String username) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(3));
        while (Instant.now().isBefore(deadline)) {
            List<Map<String, Object>> events = service.drainEvents(username);
            if (!events.isEmpty()) {
                return events.get(0);
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("Timed out waiting for admin event for user " + username);
    }
}
