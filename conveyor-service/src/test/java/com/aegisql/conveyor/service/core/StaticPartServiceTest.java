package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.loaders.StaticPartLoader;
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

class StaticPartServiceTest {

    private final StaticPartService service = new StaticPartService(new ObjectMapper());

    @Test
    void placeStaticPartAppliesPriorityAndProperties() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            PlacementResult<Boolean> result = service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of("priority", "5", "tenant", "acme")
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(result.getResult()).isTrue();
            assertThat(result.getCorrelationId()).isNull();
            assertThat(result.getProperties())
                    .containsEntry("label", "CFG")
                    .containsEntry("delete", false)
                    .containsEntry("tenant", "acme");

            StaticPartLoader<Object> placed = fixture.placedLoader().get();
            assertThat(placed).isNotNull();
            assertThat(placed.label).isEqualTo("CFG");
            assertThat(placed.staticPartValue).isEqualTo("value");
            assertThat(placed.create).isTrue();
            assertThat(placed.priority).isEqualTo(5L);
            assertThat(placed.getAllProperties())
                    .containsEntry("tenant", "acme")
                    .doesNotContainKeys("priority", "requestTTL", "delete");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placeStaticPartDeleteModeAllowsEmptyBody() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            PlacementResult<Boolean> result = service.placeStaticPart(
                    null,
                    conveyorName,
                    "CFG",
                    new byte[0],
                    Map.of("delete", "true")
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(result.getResult()).isTrue();
            assertThat(result.getProperties()).containsEntry("delete", true);

            StaticPartLoader<Object> placed = fixture.placedLoader().get();
            assertThat(placed.create).isFalse();
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placeStaticPartWithRequestTtlReturnsTimeout() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            PlacementResult<Boolean> result = service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of("requestTTL", "1 MILLISECONDS")
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.TIMEOUT_WAITING_FOR_COMPLETION);
            assertThat(result.getErrorCode()).isEqualTo("TIMEOUT");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placeStaticPartRejectsUnsupportedRequestParams() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            assertThatThrownBy(() -> service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of("ttl", "2 SECONDS")
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not supported");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placeStaticPartRejectsInvalidDeleteFlag() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            assertThatThrownBy(() -> service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of("delete", "maybe")
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid boolean for delete");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placeStaticPartRejectsMissingBodyOrContentType() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            assertThatThrownBy(() -> service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    new byte[0],
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Static part value is required");

            assertThatThrownBy(() -> service.placeStaticPart(
                    " ",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content-Type is required");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placeStaticPartRejectsUnsupportedMapping() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(byte[].class));
        try {
            assertThatThrownBy(() -> service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            ))
                    .isInstanceOf(UnsupportedMappingException.class)
                    .hasMessageContaining("Unsupported mapping");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @SuppressWarnings("unchecked")
    private Fixture fixtureFor(String name, CompletableFuture<Boolean> future, Set<Class<?>> supportedTypes) {
        Conveyor<Object, Object, Object> conveyor = mock(Conveyor.class);
        AtomicReference<StaticPartLoader<Object>> placedLoader = new AtomicReference<>();

        StaticPartLoader<Object> staticPartLoader = new StaticPartLoader<>(loader -> {
            placedLoader.set((StaticPartLoader<Object>) loader);
            return future;
        });

        ConveyorMetaInfo<Integer, String, Object> metaInfo = new ConveyorMetaInfo<>(
                Integer.class,
                String.class,
                Object.class,
                Map.of("CFG", supportedTypes),
                List.of("CFG"),
                null
        );

        when(conveyor.staticPart()).thenReturn(staticPartLoader);
        when(conveyor.getMetaInfo()).thenReturn((ConveyorMetaInfo) metaInfo);
        when(conveyor.getName()).thenReturn(name);
        when(conveyor.mBeanInterface()).thenReturn(null);
        Conveyor.register(conveyor, conveyor);

        return new Fixture(conveyor, placedLoader);
    }

    private record Fixture(
            Conveyor<Object, Object, Object> conveyor,
            AtomicReference<StaticPartLoader<Object>> placedLoader
    ) { }
}
