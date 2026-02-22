package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.loaders.StaticPartLoader;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
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
    private enum LabelEnum { CFG }
    private record JsonPayload(String name, int count) { }

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
    void placeStaticPartDeleteBlankValueAndFalseFlagAreHandled() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            PlacementResult<Boolean> blankDelete = service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    new byte[0],
                    Map.of("delete", "")
            );
            assertThat(blankDelete.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(fixture.placedLoader().get().create).isFalse();

            PlacementResult<Boolean> explicitFalse = service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of("delete", "off")
            );
            assertThat(explicitFalse.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(fixture.placedLoader().get().create).isTrue();
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
    void placeStaticPartWithoutRequestTtlReturnsInProgressForPendingFuture() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Fixture fixture = fixtureFor(conveyorName, future, Set.of(String.class));
        try {
            PlacementResult<Boolean> result = service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.IN_PROGRESS);
            assertThat(result.getResult()).isNull();
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

    @Test
    void placeStaticPartSupportsBinaryAndJsonMappings() {
        String binaryConveyorName = "collector-static-binary-" + System.nanoTime();
        CompletableFuture<Boolean> binaryFuture = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture binaryFixture = fixtureFor(binaryConveyorName, binaryFuture, Set.of(byte[].class));
        try {
            byte[] input = new byte[]{1, 2, 3};
            PlacementResult<Boolean> result = service.placeStaticPart(
                    "application/octet-stream",
                    binaryConveyorName,
                    "CFG",
                    input,
                    Map.of()
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            Object value = binaryFixture.placedLoader().get().staticPartValue;
            assertThat(value).isInstanceOf(byte[].class);
            assertThat((byte[]) value).containsExactly(1, 2, 3);
        } finally {
            Conveyor.unRegister(binaryConveyorName);
        }

        String jsonConveyorName = "collector-static-json-" + System.nanoTime();
        CompletableFuture<Boolean> jsonFuture = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture jsonFixture = fixtureFor(jsonConveyorName, jsonFuture, Set.of(JsonPayload.class));
        try {
            PlacementResult<Boolean> result = service.placeStaticPart(
                    "application/json",
                    jsonConveyorName,
                    "CFG",
                    "{\"name\":\"n1\",\"count\":7}".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            );
            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(jsonFixture.placedLoader().get().staticPartValue)
                    .isEqualTo(new JsonPayload("n1", 7));
        } finally {
            Conveyor.unRegister(jsonConveyorName);
        }
    }

    @Test
    void placeStaticPartSupportsJsonFallbackToStringAndRejectsInvalidJsonConversion() {
        String fallbackConveyorName = "collector-static-json-fallback-" + System.nanoTime();
        CompletableFuture<Boolean> fallbackFuture = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fallbackFixture = fixtureFor(fallbackConveyorName, fallbackFuture, Set.of(String.class));
        try {
            PlacementResult<Boolean> fallback = service.placeStaticPart(
                    "application/json",
                    fallbackConveyorName,
                    "CFG",
                    "{\"raw\":true}".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            );

            assertThat(fallback.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(fallbackFixture.placedLoader().get().staticPartValue).isEqualTo("{\"raw\":true}");
        } finally {
            Conveyor.unRegister(fallbackConveyorName);
        }

        String invalidJsonConveyorName = "collector-static-json-invalid-" + System.nanoTime();
        CompletableFuture<Boolean> invalidJsonFuture = CompletableFuture.completedFuture(Boolean.TRUE);
        fixtureFor(invalidJsonConveyorName, invalidJsonFuture, Set.of(JsonPayload.class));
        try {
            assertThatThrownBy(() -> service.placeStaticPart(
                    "application/json",
                    invalidJsonConveyorName,
                    "CFG",
                    "{invalid".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to convert body");
        } finally {
            Conveyor.unRegister(invalidJsonConveyorName);
        }
    }

    @Test
    void placeStaticPartSupportsEnumLabels() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureForEnumLabel(conveyorName, future);
        try {
            PlacementResult<Boolean> result = service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(fixture.placedLoader().get().label).isEqualTo(LabelEnum.CFG);
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placeStaticPartRejectsInvalidPriorityAndInvalidEnumLabel() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(Boolean.TRUE);
        Fixture fixture = fixtureForEnumLabel(conveyorName, future);
        try {
            assertThatThrownBy(() -> service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of("priority", "x")
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid number for priority");

            assertThatThrownBy(() -> service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "BAD",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot convert 'BAD'");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void placeStaticPartReturnsFailedForExceptionalFuturePaths() {
        String conveyorName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> noTtlFuture = new CompletableFuture<>();
        noTtlFuture.completeExceptionally(new IllegalStateException("boom-no-ttl"));
        Fixture noTtlFixture = fixtureFor(conveyorName, noTtlFuture, Set.of(String.class));
        try {
            PlacementResult<Boolean> result = service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            );
            assertThat(result.getStatus()).isEqualTo(PlacementStatus.FAILED);
            assertThat(result.getErrorCode()).isEqualTo("IllegalStateException");
        } finally {
            Conveyor.unRegister(conveyorName);
        }

        String withTtlName = "collector-static-" + System.nanoTime();
        CompletableFuture<Boolean> withTtlFuture = new CompletableFuture<>();
        withTtlFuture.completeExceptionally(new IllegalStateException("boom-with-ttl"));
        Fixture withTtlFixture = fixtureFor(withTtlName, withTtlFuture, Set.of(String.class));
        try {
            PlacementResult<Boolean> result = service.placeStaticPart(
                    "text/plain",
                    withTtlName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of("requestTTL", "1 SECONDS")
            );
            assertThat(result.getStatus()).isEqualTo(PlacementStatus.FAILED);
            assertThat(result.getErrorCode()).isEqualTo("IllegalStateException");
            assertThat(result.getErrorMessage()).contains("boom-with-ttl");
        } finally {
            Conveyor.unRegister(withTtlName);
        }
    }

    @Test
    void placeStaticPartRejectsUnknownConveyorAndMissingMetaInfo() {
        assertThatThrownBy(() -> service.placeStaticPart(
                "text/plain",
                "missing-" + System.nanoTime(),
                "CFG",
                "value".getBytes(StandardCharsets.UTF_8),
                Map.of()
        ))
                .isInstanceOf(ConveyorNotFoundException.class);

        String conveyorName = "collector-static-no-meta-" + System.nanoTime();
        Conveyor<Object, Object, Object> conveyor = mock(Conveyor.class);
        when(conveyor.getName()).thenReturn(conveyorName);
        when(conveyor.getMetaInfo()).thenThrow(new IllegalStateException("no meta"));
        when(conveyor.mBeanInterface()).thenReturn(null);
        Conveyor.register(conveyor, conveyor);
        try {
            assertThatThrownBy(() -> service.placeStaticPart(
                    "text/plain",
                    conveyorName,
                    "CFG",
                    "value".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            ))
                    .isInstanceOf(UnsupportedMappingException.class)
                    .hasMessageContaining("does not expose meta info");
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

    @SuppressWarnings("unchecked")
    private Fixture fixtureForEnumLabel(String name, CompletableFuture<Boolean> future) {
        Conveyor<Object, Object, Object> conveyor = mock(Conveyor.class);
        AtomicReference<StaticPartLoader<Object>> placedLoader = new AtomicReference<>();

        StaticPartLoader<Object> staticPartLoader = new StaticPartLoader<>(loader -> {
            placedLoader.set((StaticPartLoader<Object>) loader);
            return future;
        });

        ConveyorMetaInfo<Integer, LabelEnum, Object> metaInfo = new ConveyorMetaInfo<>(
                Integer.class,
                LabelEnum.class,
                Object.class,
                Map.of(LabelEnum.CFG, Set.of(String.class)),
                List.of(LabelEnum.CFG),
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
