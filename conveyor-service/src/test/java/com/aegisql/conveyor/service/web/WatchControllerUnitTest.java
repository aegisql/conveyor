package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.core.ConveyorWatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchControllerUnitTest {

    @Mock
    private ConveyorWatchService conveyorWatchService;

    private WatchController controller;

    @BeforeEach
    void setUp() {
        controller = new WatchController(conveyorWatchService);
    }

    @Test
    void activeWatchesDelegateToServiceForAuthenticatedUser() {
        when(conveyorWatchService.activeWatchesForUser("viewer")).thenReturn(List.of(Map.of("watchId", "w-1")));

        var payload = controller.activeWatches(auth("viewer"));

        assertThat(payload).hasSize(1);
        verify(conveyorWatchService).activeWatchesForUser("viewer");
    }

    @Test
    void activeWatchesRequireAuthenticatedUser() {
        assertThatThrownBy(() -> controller.activeWatches(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user is required");
        assertThatThrownBy(() -> controller.activeWatches(auth(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user is required");
    }

    @Test
    void cancelWatchByIdUsesStringifiedWatchId() {
        when(conveyorWatchService.cancelWatchById("viewer", "123")).thenReturn(true);

        var payload = controller.cancelWatch(Map.of("watchId", 123), auth("viewer"));

        assertThat(payload).containsEntry("canceled", true);
        verify(conveyorWatchService).cancelWatchById("viewer", "123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "on", "1", " TRUE "})
    void cancelWatchAcceptsTruthyForeachWithoutCorrelationId(String foreachValue) {
        when(conveyorWatchService.cancelWatch("viewer", "collector", null, true)).thenReturn(true);

        var payload = controller.cancelWatch(
                Map.of("conveyor", "collector", "foreach", foreachValue),
                auth("viewer")
        );

        assertThat(payload).containsEntry("canceled", true);
        verify(conveyorWatchService).cancelWatch("viewer", "collector", null, true);
    }

    @Test
    void cancelWatchRequiresCorrelationIdWhenForeachIsFalse() {
        assertThatThrownBy(() -> controller.cancelWatch(Map.of("conveyor", "collector"), auth("viewer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correlationId is required");
    }

    @Test
    void cancelWatchUsesCorrelationIdAndConveyorStringValueWhenForeachIsFalse() {
        when(conveyorWatchService.cancelWatch("viewer", "42", "abc", false)).thenReturn(false);

        var payload = controller.cancelWatch(
                Map.of("conveyor", 42, "correlationId", "abc", "foreach", "false"),
                auth("viewer")
        );

        assertThat(payload).containsEntry("canceled", false);
        verify(conveyorWatchService).cancelWatch("viewer", "42", "abc", false);
    }

    @Test
    void updateHistoryLimitParsesPositiveIntegerAndDelegates() {
        when(conveyorWatchService.updateForeachHistoryLimit("viewer", 7)).thenReturn(3);

        var payload = controller.updateHistoryLimit(Map.of("historyLimit", " 7 "), auth("viewer"));

        assertThat(payload).containsEntry("historyLimit", 7);
        assertThat(payload).containsEntry("updated", 3);
        verify(conveyorWatchService).updateForeachHistoryLimit("viewer", 7);
    }

    @Test
    void updateHistoryLimitRequiresPositiveNumericValue() {
        assertThatThrownBy(() -> controller.updateHistoryLimit(null, auth("viewer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("historyLimit is required");
        assertThatThrownBy(() -> controller.updateHistoryLimit(Map.of("historyLimit", 0), auth("viewer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive integer");
        assertThatThrownBy(() -> controller.updateHistoryLimit(Map.of("historyLimit", "abc"), auth("viewer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive integer");
    }

    private static UsernamePasswordAuthenticationToken auth(String username) {
        return new UsernamePasswordAuthenticationToken(username, "n/a");
    }
}
