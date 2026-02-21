package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.error.UnsupportedMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlacementServiceTest {

    private final PlacementService service = new PlacementService(new ObjectMapper());

    @Test
    void placePartAppliesConversionsAndCompletes() {
        String conveyorName = "collector-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            Map<String, String> params = Map.of(
                    "ttl", "2 SECONDS",
                    "creationTime", "1700000000000",
                    "expirationTime", "1700000001000",
                    "priority", "7",
                    "tenant", "acme"
            );
            PlacementResult<Boolean> result = service.placePart(
                    "text/plain",
                    conveyorName,
                    "42",
                    "USER",
                    "hello".getBytes(StandardCharsets.UTF_8),
                    params
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(result.getResult()).isTrue();
            assertThat(result.getCorrelationId()).isEqualTo("42");
            assertThat(result.getProperties()).containsEntry("tenant", "acme");
            assertThat(result.getProperties()).containsEntry("foreach", false);

            PartLoader<Object, Object> placed = fixture.placedLoader().get();
            assertThat(placed).isNotNull();
            assertThat(placed.key).isEqualTo(42);
            assertThat(placed.label).isEqualTo("USER");
            assertThat(placed.partValue).isEqualTo("hello");
            assertThat(placed.creationTime).isEqualTo(1_700_000_000_000L);
            assertThat(placed.expirationTime).isEqualTo(1_700_000_001_000L);
            assertThat(placed.priority).isEqualTo(7L);
            assertThat(placed.getAllProperties())
                    .containsEntry("tenant", "acme")
                    .doesNotContainKeys("ttl", "creationTime", "expirationTime", "priority", "requestTTL");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placePartForEachReturnsInProgressWhenFutureNotDone() {
        String conveyorName = "collector-" + System.nanoTime();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            PlacementResult<Boolean> result = service.placePartForEach(
                    "text/plain",
                    conveyorName,
                    "USER",
                    "hello".getBytes(StandardCharsets.UTF_8),
                    Map.of("tenant", "acme")
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.IN_PROGRESS);
            PartLoader<Object, Object> placed = fixture.placedLoader().get();
            assertThat(placed).isNotNull();
            assertThat(placed.key).isNull();
            assertThat(placed.filter).isNotNull();
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placePartWithRequestTtlReturnsTimeoutWhenFutureStillRunning() {
        String conveyorName = "collector-" + System.nanoTime();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            PlacementResult<Boolean> result = service.placePart(
                    "text/plain",
                    conveyorName,
                    "7",
                    "USER",
                    "hello".getBytes(StandardCharsets.UTF_8),
                    Map.of("requestTTL", "1 MILLISECONDS")
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.TIMEOUT_WAITING_FOR_COMPLETION);
            assertThat(result.getErrorCode()).isEqualTo("TIMEOUT");
            assertThat(result.getErrorMessage()).contains("still running");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placePartReturnsFailedWhenFutureCompletedExceptionally() {
        String conveyorName = "collector-" + System.nanoTime();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("boom"));
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            PlacementResult<Boolean> result = service.placePart(
                    "text/plain",
                    conveyorName,
                    "8",
                    "USER",
                    "hello".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.FAILED);
            assertThat(result.getErrorCode()).isEqualTo("IllegalStateException");
            assertThat(result.getErrorMessage()).contains("boom");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placePartRejectsMissingIdWhenNotForeach() {
        String conveyorName = "collector-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            assertThatThrownBy(() -> service.placePart(
                    "text/plain",
                    conveyorName,
                    " ",
                    "USER",
                    "hello".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id is required");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placePartRejectsUnsupportedContentTypeMapping() {
        String conveyorName = "collector-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(byte[].class));
        try {
            assertThatThrownBy(() -> service.placePart(
                    "text/plain",
                    conveyorName,
                    "42",
                    "USER",
                    "hello".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            ))
                    .isInstanceOf(UnsupportedMappingException.class)
                    .hasMessageContaining("Unsupported mapping");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placePartRejectsInvalidJsonBody() {
        String conveyorName = "collector-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(JsonBody.class));
        try {
            assertThatThrownBy(() -> service.placePart(
                    "application/json",
                    conveyorName,
                    "42",
                    "USER",
                    "{invalid-json".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to convert body");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @SuppressWarnings("unchecked")
    private Fixture fixtureFor(String name, CompletableFuture<Boolean> future, Set<Class<?>> supportedTypes) {
        Conveyor<Object, Object, Object> conveyor = mock(Conveyor.class);
        AtomicReference<PartLoader<Object, Object>> placedLoader = new AtomicReference<>();

        PartLoader<Object, Object> partLoader = new PartLoader<>(loader -> {
            placedLoader.set((PartLoader<Object, Object>) loader);
            return future;
        });

        ConveyorMetaInfo<Integer, String, Object> metaInfo = new ConveyorMetaInfo<>(
                Integer.class,
                String.class,
                Object.class,
                Map.of("USER", supportedTypes),
                List.of("USER"),
                null
        );

        when(conveyor.part()).thenReturn(partLoader);
        when(conveyor.getMetaInfo()).thenReturn((ConveyorMetaInfo) metaInfo);
        when(conveyor.getName()).thenReturn(name);
        when(conveyor.mBeanInterface()).thenReturn(null);
        Conveyor.register(conveyor, conveyor);

        return new Fixture(conveyor, placedLoader);
    }

    private record Fixture(
            Conveyor<Object, Object, Object> conveyor,
            AtomicReference<PartLoader<Object, Object>> placedLoader
    ) { }

    private static class JsonBody {
        public String value;
    }
}
