package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.loaders.CommandLoader;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandServiceTest {

    private final CommandService service = new CommandService(new ObjectMapper());

    @Test
    void executeByIdCancelConvertsKeyAndAppliesTiming() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                Integer.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            PlacementResult<Object> result = service.executeById(
                    conveyorName,
                    "42",
                    "cancel",
                    null,
                    Map.of(
                            "ttl", "2 SECONDS",
                            "creationTime", "1700000000000",
                            "expirationTime", "1700000005000"
                    )
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(result.getResult()).isEqualTo(Boolean.TRUE);
            assertThat(result.getCorrelationId()).isEqualTo("42");
            assertThat(result.getProperties())
                    .containsEntry("conveyor", conveyorName)
                    .containsEntry("command", "cancel")
                    .containsEntry("foreach", false);

            GeneralCommand<Object, ?> command = fixture.commands().get(0);
            assertThat(command.getLabel()).isEqualTo(CommandLabel.CANCEL_BUILD);
            assertThat(command.getKey()).isEqualTo(42);
            assertThat(command.getCreationTime()).isEqualTo(1_700_000_000_000L);
            assertThat(command.getExpirationTime()).isEqualTo(1_700_000_005_000L);
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdReturnsInProgressWhenFutureNotDone() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> new CompletableFuture<>()
        );
        try {
            PlacementResult<Object> result = service.executeById(
                    conveyorName,
                    "id-1",
                    "timeout",
                    null,
                    Map.of()
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.IN_PROGRESS);
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdReturnsTimeoutWhenRequestTtlExpires() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> new CompletableFuture<>()
        );
        try {
            PlacementResult<Object> result = service.executeById(
                    conveyorName,
                    "id-1",
                    "cancel",
                    null,
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
    void executeByIdReturnsFailedWhenFutureCompletesExceptionally() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.failedFuture(new IllegalStateException("boom"))
        );
        try {
            PlacementResult<Object> result = service.executeById(
                    conveyorName,
                    "id-1",
                    "cancel",
                    null,
                    Map.of("requestTTL", "5 SECONDS")
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.FAILED);
            assertThat(result.getErrorCode()).isEqualTo("IllegalStateException");
            assertThat(result.getErrorMessage()).contains("boom");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdReturnsFailedWhenFutureHasWrappedCompletionException() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.failedFuture(
                        new CompletionException(new IllegalArgumentException("bad"))
                )
        );
        try {
            PlacementResult<Object> result = service.executeById(
                    conveyorName,
                    "id-1",
                    "cancel",
                    null,
                    Map.of()
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.FAILED);
            assertThat(result.getErrorCode()).isEqualTo("IllegalArgumentException");
            assertThat(result.getErrorMessage()).contains("bad");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdRejectsMissingId() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    " ",
                    "cancel",
                    null,
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id is required");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdRejectsUnsupportedParamsForCommand() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    "id-1",
                    "cancel",
                    null,
                    Map.of("unexpected", "x")
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported parameters");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdRescheduleRequiresTiming() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    "id-1",
                    "reschedule",
                    null,
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reschedule requires ttl or expirationTime");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdPeekAndPeekIdRequireRequestTtl() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    "id-1",
                    "peek",
                    null,
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("peek requires requestTTL");

            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    "id-1",
                    "peekId",
                    null,
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("peekId requires requestTTL");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdSupportsPeekAndPeekId() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                Integer.class,
                (command, conveyor) -> {
                    if (command.getLabel() == CommandLabel.PEEK_BUILD) {
                        @SuppressWarnings("unchecked")
                        Consumer<ProductBin<Object, Object>> consumer =
                                (Consumer<ProductBin<Object, Object>>) command.getValue();
                        consumer.accept(new ProductBin<>(conveyor,
                                command.getKey(),
                                "product-" + command.getKey(),
                                0,
                                Status.READY,
                                Map.of(),
                                null));
                    } else if (command.getLabel() == CommandLabel.PEEK_KEY) {
                        @SuppressWarnings("unchecked")
                        Consumer<Object> consumer = (Consumer<Object>) command.getValue();
                        consumer.accept(command.getKey());
                    }
                    return CompletableFuture.completedFuture(Boolean.TRUE);
                }
        );
        try {
            PlacementResult<Object> peek = service.executeById(
                    conveyorName,
                    "7",
                    "peek",
                    null,
                    Map.of("requestTTL", "1 SECONDS")
            );
            assertThat(peek.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(peek.getResult()).isEqualTo("product-7");

            PlacementResult<Object> peekId = service.executeById(
                    conveyorName,
                    "8",
                    "peek-id",
                    null,
                    Map.of("requestTTL", "1 SECONDS")
            );
            assertThat(peekId.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(peekId.getResult()).isEqualTo(Boolean.TRUE);
            assertThat(fixture.commands().get(1).getKey()).isEqualTo(8);
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdAddPropertiesRequiresAtLeastOneProperty() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    "id-1",
                    "addProperties",
                    null,
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ADD_PROPERTIES requires at least one property");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdAddPropertiesAppliesValues() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            PlacementResult<Object> result = service.executeById(
                    conveyorName,
                    "id-1",
                    "addProperties",
                    null,
                    Map.of("tenant", "acme", "mode", "fast")
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(result.getProperties())
                    .containsEntry("tenant", "acme")
                    .containsEntry("mode", "fast");

            GeneralCommand<Object, ?> command = fixture.commands().get(0);
            assertThat(command.getLabel()).isEqualTo(CommandLabel.PROPERTIES);
            assertThat(command.getAllProperties())
                    .containsEntry("tenant", "acme")
                    .containsEntry("mode", "fast");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdCompleteExceptionallyRequiresMessage() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    "id-1",
                    "completeExceptionally",
                    "   ".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Request body message is required");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdCompleteExceptionallyUsesMessageBody() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            PlacementResult<Object> result = service.executeById(
                    conveyorName,
                    "id-1",
                    "completeExceptionally",
                    "boom".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);

            GeneralCommand<Object, ?> command = fixture.commands().get(0);
            assertThat(command.getLabel()).isEqualTo(CommandLabel.COMPLETE_BUILD_EXEPTIONALLY);
            assertThat(command.getValue()).isInstanceOf(RuntimeException.class);
            assertThat(((RuntimeException) command.getValue()).getMessage()).isEqualTo("boom");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeForEachRejectsUnsupportedCreate() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            assertThatThrownBy(() -> service.executeForEach(
                    conveyorName,
                    "create",
                    null,
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not supported in foreach mode");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeForEachSupportsPeekAndPeekId() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> {
                    if (command.getLabel() == CommandLabel.PEEK_BUILD) {
                        @SuppressWarnings("unchecked")
                        Consumer<ProductBin<Object, Object>> consumer =
                                (Consumer<ProductBin<Object, Object>>) command.getValue();
                        consumer.accept(new ProductBin<>(conveyor, "A", "alpha", 0, Status.READY, Map.of(), null));
                        consumer.accept(new ProductBin<>(conveyor, "B", "beta", 0, Status.READY, Map.of(), null));
                    } else if (command.getLabel() == CommandLabel.PEEK_KEY) {
                        @SuppressWarnings("unchecked")
                        Consumer<Object> consumer = (Consumer<Object>) command.getValue();
                        consumer.accept("A");
                        consumer.accept("B");
                    }
                    return CompletableFuture.completedFuture(Boolean.TRUE);
                }
        );
        try {
            PlacementResult<Object> peek = service.executeForEach(
                    conveyorName,
                    "peek",
                    null,
                    Map.of("requestTTL", "1 SECONDS")
            );
            assertThat(peek.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(peek.getResult()).isEqualTo(Map.of("A", "alpha", "B", "beta"));

            PlacementResult<Object> peekId = service.executeForEach(
                    conveyorName,
                    "peekId",
                    null,
                    Map.of("requestTTL", "1 SECONDS")
            );
            assertThat(peekId.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(peekId.getResult()).isEqualTo(List.of("A", "B"));
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeForEachCompleteExceptionallyReturnsFalseWhenNoMatchingIds() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            PlacementResult<Object> result = service.executeForEach(
                    conveyorName,
                    "completeExceptionally",
                    "boom".getBytes(StandardCharsets.UTF_8),
                    Map.of()
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(result.getResult()).isEqualTo(Boolean.FALSE);
            assertThat(fixture.commands()).hasSize(1);
            assertThat(fixture.commands().get(0).getLabel()).isEqualTo(CommandLabel.PEEK_KEY);
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeForEachCompleteExceptionallyTargetsEachId() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> {
                    if (command.getLabel() == CommandLabel.PEEK_KEY && command.getKey() == null) {
                        @SuppressWarnings("unchecked")
                        Consumer<Object> consumer = (Consumer<Object>) command.getValue();
                        consumer.accept("a");
                        consumer.accept("b");
                    }
                    return CompletableFuture.completedFuture(Boolean.TRUE);
                }
        );
        try {
            PlacementResult<Object> result = service.executeForEach(
                    conveyorName,
                    "completeExceptionally",
                    "fail all".getBytes(StandardCharsets.UTF_8),
                    Map.of("ttl", "3 SECONDS")
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(result.getResult()).isEqualTo(Boolean.TRUE);

            assertThat(fixture.commands()).hasSize(3);
            assertThat(fixture.commands().get(0).getLabel()).isEqualTo(CommandLabel.PEEK_KEY);

            GeneralCommand<Object, ?> first = fixture.commands().get(1);
            GeneralCommand<Object, ?> second = fixture.commands().get(2);
            assertThat(first.getLabel()).isEqualTo(CommandLabel.COMPLETE_BUILD_EXEPTIONALLY);
            assertThat(second.getLabel()).isEqualTo(CommandLabel.COMPLETE_BUILD_EXEPTIONALLY);
            assertThat(first.getValue()).isInstanceOf(RuntimeException.class);
            assertThat(((RuntimeException) first.getValue()).getMessage()).isEqualTo("fail all");
            assertThat(first.getCreationTime()).isPositive();
            assertThat(first.getExpirationTime()).isGreaterThan(first.getCreationTime());
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdSupportsEnumAndRejectsInvalidConversion() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                KeyMode.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            PlacementResult<Object> ok = service.executeById(
                    conveyorName,
                    "ALPHA",
                    "cancel",
                    null,
                    Map.of()
            );
            assertThat(ok.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(fixture.commands().get(0).getKey()).isEqualTo(KeyMode.ALPHA);

            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    "UNKNOWN",
                    "cancel",
                    null,
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot convert 'UNKNOWN'");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdFallsBackToStringWhenMetaInfoUnavailable() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureWithMetaFailure(
                conveyorName,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            PlacementResult<Object> result = service.executeById(
                    conveyorName,
                    "raw-id",
                    "cancel",
                    null,
                    Map.of()
            );

            assertThat(result.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
            assertThat(fixture.commands().get(0).getKey()).isEqualTo("raw-id");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @Test
    void executeByIdThrowsWhenConveyorIsUnknown() {
        assertThatThrownBy(() -> service.executeById(
                "missing-conveyor-" + System.nanoTime(),
                "1",
                "cancel",
                null,
                Map.of()
        ))
                .isInstanceOf(ConveyorNotFoundException.class);
    }

    @Test
    void executeRejectsMissingAndUnsupportedCommandNames() {
        String conveyorName = "command-" + System.nanoTime();
        Fixture fixture = fixtureFor(
                conveyorName,
                String.class,
                (command, conveyor) -> CompletableFuture.completedFuture(Boolean.TRUE)
        );
        try {
            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    "1",
                    " ",
                    null,
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("command is required");

            assertThatThrownBy(() -> service.executeById(
                    conveyorName,
                    "1",
                    "unknownCommand",
                    null,
                    Map.of()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported command");
        } finally {
            Conveyor.unRegister(conveyorName);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Fixture fixtureFor(String name, Class<?> keyType, CommandHandler handler) {
        Conveyor<Object, Object, Object> conveyor = mock(Conveyor.class);
        List<GeneralCommand<Object, ?>> commands = Collections.synchronizedList(new ArrayList<>());

        CommandLoader<Object, Object> commandLoader = new CommandLoader<>(command -> {
            GeneralCommand<Object, ?> typedCommand = (GeneralCommand<Object, ?>) command;
            commands.add(typedCommand);
            return handler.handle(typedCommand, conveyor);
        });

        ConveyorMetaInfo<Object, String, Object> metaInfo = new ConveyorMetaInfo<>(
                (Class<Object>) keyType,
                String.class,
                Object.class,
                Map.of(),
                List.of(),
                null
        );

        when(conveyor.command()).thenReturn(commandLoader);
        when(conveyor.getMetaInfo()).thenReturn((ConveyorMetaInfo) metaInfo);
        when(conveyor.getName()).thenReturn(name);
        when(conveyor.mBeanInterface()).thenReturn(null);
        Conveyor.register(conveyor, conveyor);
        return new Fixture(commands);
    }

    @SuppressWarnings("unchecked")
    private Fixture fixtureWithMetaFailure(String name, CommandHandler handler) {
        Conveyor<Object, Object, Object> conveyor = mock(Conveyor.class);
        List<GeneralCommand<Object, ?>> commands = Collections.synchronizedList(new ArrayList<>());

        CommandLoader<Object, Object> commandLoader = new CommandLoader<>(command -> {
            GeneralCommand<Object, ?> typedCommand = (GeneralCommand<Object, ?>) command;
            commands.add(typedCommand);
            return handler.handle(typedCommand, conveyor);
        });

        when(conveyor.command()).thenReturn(commandLoader);
        when(conveyor.getMetaInfo()).thenThrow(new IllegalStateException("meta unavailable"));
        when(conveyor.getName()).thenReturn(name);
        when(conveyor.mBeanInterface()).thenReturn(null);
        Conveyor.register(conveyor, conveyor);
        return new Fixture(commands);
    }

    private interface CommandHandler {
        CompletableFuture<Boolean> handle(GeneralCommand<Object, ?> command, Conveyor<Object, Object, Object> conveyor);
    }

    private record Fixture(List<GeneralCommand<Object, ?>> commands) {
    }

    private enum KeyMode {
        ALPHA
    }
}
