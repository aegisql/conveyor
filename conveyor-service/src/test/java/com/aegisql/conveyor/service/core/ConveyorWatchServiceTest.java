package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.loaders.ResultConsumerLoader;
import com.aegisql.conveyor.loaders.ScrapConsumerLoader;
import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConveyorWatchServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> registeredConveyors = new ArrayList<>();
    private ConveyorWatchService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
        for (String name : registeredConveyors) {
            try {
                Conveyor.unRegister(name);
            } catch (Exception ignored) {
                // Ignore cleanup failures for already removed test conveyors.
            }
        }
    }

    @Test
    void registerSessionWithoutUsernameClosesSocket() throws Exception {
        service = new ConveyorWatchService(mapper, 3);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn(null);
        when(session.getAttributes()).thenReturn(Map.of());

        service.registerSession(session);

        verify(session).close();
    }

    @Test
    void registerSessionUsesSecurityContextAttributeWhenPrincipalMissing() throws Exception {
        service = new ConveyorWatchService(mapper, 3);
        WebSocketSession session = mock(WebSocketSession.class);
        var auth = new UsernamePasswordAuthenticationToken("alice", "n/a");
        var attrs = new HashMap<String, Object>();
        attrs.put("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(auth));
        when(session.getPrincipal()).thenReturn(null);
        when(session.getAttributes()).thenReturn(attrs);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("secctx-session");

        service.registerSession(session);
        service.unregisterSession(session);

        verify(session, times(2)).getAttributes();
    }

    @Test
    void byIdWatchEmitsResultEventAndRemainsActiveUntilCanceled() throws Exception {
        service = new ConveyorWatchService(mapper, 3);
        HookedConveyor hooked = registerHookedConveyor("watch-by-id-" + System.nanoTime(), null);

        Map<String, Object> snapshot = service.registerWatch("Alice", hooked.name(), "42", false, null);
        assertThat(snapshot).containsEntry("foreach", false).containsEntry("active", true);

        hooked.resultConsumer().get().accept(new ProductBin<>(
                hooked.conveyor(),
                42,
                Map.of("ok", true),
                0,
                Status.READY,
                Map.of("creationTime", 100L, "tenant", "acme"),
                null
        ));
        hooked.resultConsumer().get().accept(new ProductBin<>(
                hooked.conveyor(),
                42,
                Map.of("ok", true, "seq", 2),
                0,
                Status.READY,
                Map.of("creationTime", 101L, "tenant", "acme"),
                null
        ));

        List<Map<String, Object>> active = service.activeWatchesForUser("alice");
        assertThat(active).hasSize(1);
        assertThat(active.get(0))
                .containsEntry("foreach", false)
                .containsEntry("active", true)
                .containsEntry("correlationId", "42");
        @SuppressWarnings("unchecked")
        List<PlacementResult<Object>> events = (List<PlacementResult<Object>>) active.get(0).get("events");
        assertThat(events).hasSize(1);
        PlacementResult<Object> event = events.get(0);
        assertThat(event.getStatus()).isEqualTo(PlacementStatus.COMPLETED);
        assertThat(event.getCorrelationId()).isEqualTo("42");
        assertThat(event.getResult()).isEqualTo(Map.of("ok", true, "seq", 2));
        assertThat(event.getProperties())
                .containsEntry("eventType", "RESULT")
                .containsEntry("watchActive", true)
                .containsEntry("tenant", "acme");

        String watchId = String.valueOf(active.get(0).get("watchId"));
        assertThat(service.cancelWatchById("alice", watchId)).isTrue();
        assertThat(service.activeWatchesForUser("alice")).isEmpty();
    }

    @Test
    void foreachWatchDeduplicatesHistoryAndCancelByIdEmitsCanceledEvent() throws Exception {
        service = new ConveyorWatchService(mapper, 3);
        HookedConveyor hooked = registerHookedConveyor("watch-foreach-" + System.nanoTime(), null);

        Map<String, Object> watch = service.registerWatch("alice", hooked.name(), null, true, 2);
        String watchId = String.valueOf(watch.get("watchId"));

        hooked.resultConsumer().get().accept(new ProductBin<>(
                hooked.conveyor(),
                "A",
                "alpha",
                0,
                Status.WAITING_DATA,
                Map.of("creationTime", 1L),
                null
        ));
        // Duplicate signature should be skipped.
        hooked.resultConsumer().get().accept(new ProductBin<>(
                hooked.conveyor(),
                "A",
                "alpha",
                0,
                Status.WAITING_DATA,
                Map.of("creationTime", 1L),
                null
        ));
        hooked.resultConsumer().get().accept(new ProductBin<>(
                hooked.conveyor(),
                "B",
                "beta",
                0,
                Status.READY,
                Map.of("creationTime", 2L),
                null
        ));
        hooked.resultConsumer().get().accept(new ProductBin<>(
                hooked.conveyor(),
                "C",
                "gamma",
                0,
                Status.READY,
                Map.of("creationTime", 3L),
                null
        ));

        List<Map<String, Object>> active = service.activeWatchesForUser("alice");
        assertThat(active).hasSize(1);
        @SuppressWarnings("unchecked")
        List<Object> events = (List<Object>) active.get(0).get("events");
        assertThat(events).hasSize(2);

        assertThat(service.updateForeachHistoryLimit("alice", 1)).isEqualTo(1);
        active = service.activeWatchesForUser("alice");
        @SuppressWarnings("unchecked")
        List<Object> trimmedEvents = (List<Object>) active.get(0).get("events");
        assertThat(trimmedEvents).hasSize(1);

        assertThat(service.cancelWatchById("alice", watchId)).isTrue();
        assertThat(service.activeWatchesForUser("alice")).isEmpty();
    }

    @Test
    void scrapEventsUseErrorMessageAndCancelWatchByCompositeKey() throws Exception {
        service = new ConveyorWatchService(mapper, 3);
        HookedConveyor hooked = registerHookedConveyor("watch-scrap-" + System.nanoTime(), null);

        service.registerWatch("alice", hooked.name(), null, true, 3);

        @SuppressWarnings("unchecked")
        ScrapConsumer<Object, Object> scrapBridge = (ScrapConsumer<Object, Object>) hooked.scrapConsumer().get();
        scrapBridge.accept(new ScrapBin<>(
                hooked.conveyor(),
                "K1",
                (Object) "scrap-payload",
                "comment",
                new IllegalStateException("boom"),
                ScrapBin.FailureType.BUILD_FAILED,
                Map.of("creationTime", 77L),
                null
        ));

        List<Map<String, Object>> active = service.activeWatchesForUser("alice");
        assertThat(active).hasSize(1);
        @SuppressWarnings("unchecked")
        List<PlacementResult<Object>> events = (List<PlacementResult<Object>>) active.get(0).get("events");
        assertThat(events).hasSize(1);
        PlacementResult<Object> event = events.get(0);
        assertThat(event.getStatus()).isEqualTo(PlacementStatus.FAILED);
        assertThat(event.getErrorMessage()).isEqualTo("boom");
        assertThat(event.getProperties()).containsEntry("eventType", "SCRAP");

        assertThat(service.cancelWatch("alice", hooked.name(), null, true)).isTrue();
        assertThat(service.cancelWatch("alice", hooked.name(), null, true)).isFalse();
    }

    @Test
    void watchValidationAndHookingScenarios() {
        service = new ConveyorWatchService(mapper, 3);
        HookedConveyor hooked = registerHookedConveyor("watch-validate-" + System.nanoTime(), null);

        assertThatThrownBy(() -> service.registerWatch("alice", hooked.name(), " ", false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID is required");
        assertThatThrownBy(() -> service.cancelWatchById(" ", "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated username is required");
        assertThatThrownBy(() -> service.ensureConveyorHooked("missing-" + System.nanoTime()))
                .isInstanceOf(ConveyorNotFoundException.class);

        service.ensureConveyorHooked(hooked.name());
        service.ensureConveyorHooked(hooked.name());
        service.ensureConveyorHooked((String) null);
        service.ensureKnownConveyorsHooked();

        assertThat(hooked.resultSetCount().get()).isEqualTo(1);
        assertThat(hooked.scrapSetCount().get()).isEqualTo(1);
    }

    @Test
    void privateHelpersMapStatusesSanitizePayloadsAndParseLongs() throws Exception {
        service = new ConveyorWatchService(mapper, 3);

        assertThat(invokePrivate(service, "mapStatus", new Class<?>[]{Status.class}, (Object) null))
                .isEqualTo(PlacementStatus.COMPLETED);
        assertThat(invokePrivate(service, "mapStatus", new Class<?>[]{Status.class}, Status.WAITING_DATA))
                .isEqualTo(PlacementStatus.IN_PROGRESS);
        assertThat(invokePrivate(service, "mapStatus", new Class<?>[]{Status.class}, Status.TIMED_OUT))
                .isEqualTo(PlacementStatus.TIMEOUT_WAITING_FOR_COMPLETION);
        assertThat(invokePrivate(service, "mapStatus", new Class<?>[]{Status.class}, Status.READY))
                .isEqualTo(PlacementStatus.COMPLETED);
        assertThat(invokePrivate(service, "mapStatus", new Class<?>[]{Status.class}, Status.CANCELED))
                .isEqualTo(PlacementStatus.REJECTED);
        assertThat(invokePrivate(service, "mapStatus", new Class<?>[]{Status.class}, Status.NOT_FOUND))
                .isEqualTo(PlacementStatus.REJECTED);
        assertThat(invokePrivate(service, "mapStatus", new Class<?>[]{Status.class}, Status.INVALID))
                .isEqualTo(PlacementStatus.FAILED);

        Object sanitizedMap = invokePrivate(service, "sanitizeForJson", new Class<?>[]{Object.class}, Map.of("k", 1));
        assertThat(sanitizedMap).isInstanceOf(Map.class);
        assertThat((Map<Object, Object>) sanitizedMap).containsEntry("k", 1);

        class SelfRef {
            Object self = this;
        }
        Object fallback = invokePrivate(service, "sanitizeForJson", new Class<?>[]{Object.class}, new SelfRef());
        assertThat(fallback).isInstanceOf(String.class);

        assertThat(invokePrivate(service, "parseLongValue", new Class<?>[]{Object.class}, 7)).isEqualTo(7L);
        assertThat(invokePrivate(service, "parseLongValue", new Class<?>[]{Object.class}, " 9 ")).isEqualTo(9L);
        assertThat(invokePrivate(service, "parseLongValue", new Class<?>[]{Object.class}, "not-a-number")).isNull();
        assertThat(invokePrivate(service, "parseLongValue", new Class<?>[]{Object.class}, new Object())).isNull();
    }

    private static Object invokePrivate(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private WebSocketSession openSession(String username, String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        Principal principal = () -> username;
        when(session.getPrincipal()).thenReturn(principal);
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn(id);
        return session;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private HookedConveyor registerHookedConveyor(String name, Conveyor<?, ?, ?> parent) {
        AtomicReference<ResultConsumer<Object, Object>> resultConsumer = new AtomicReference<>();
        AtomicReference<ScrapConsumer<Object, ?>> scrapConsumer = new AtomicReference<>();
        AtomicInteger resultSetCount = new AtomicInteger();
        AtomicInteger scrapSetCount = new AtomicInteger();

        ResultConsumerLoader<Object, Object> resultLoader = new ResultConsumerLoader<>(
                loader -> CompletableFuture.completedFuture(Boolean.TRUE),
                consumer -> {
                    resultSetCount.incrementAndGet();
                    resultConsumer.set((ResultConsumer<Object, Object>) consumer);
                },
                bin -> {
                    // No-op initial consumer for tests.
                }
        );
        ScrapConsumerLoader<Object> scrapLoader = new ScrapConsumerLoader<>(
                consumer -> {
                    scrapSetCount.incrementAndGet();
                    scrapConsumer.set((ScrapConsumer<Object, ?>) consumer);
                },
                null
        );

        Conveyor<Object, Object, Object> conveyor = mock(Conveyor.class);
        when(conveyor.getName()).thenReturn(name);
        when(conveyor.getEnclosingConveyor()).thenReturn((Conveyor) parent);
        when(conveyor.resultConsumer()).thenReturn((ResultConsumerLoader) resultLoader);
        when(conveyor.scrapConsumer()).thenReturn((ScrapConsumerLoader) scrapLoader);
        when(conveyor.mBeanInterface()).thenReturn(null);
        Conveyor.register(conveyor, conveyor);
        registeredConveyors.add(name);
        return new HookedConveyor(name, conveyor, resultConsumer, scrapConsumer, resultSetCount, scrapSetCount);
    }

    private record HookedConveyor(
            String name,
            Conveyor<Object, Object, Object> conveyor,
            AtomicReference<ResultConsumer<Object, Object>> resultConsumer,
            AtomicReference<ScrapConsumer<Object, ?>> scrapConsumer,
            AtomicInteger resultSetCount,
            AtomicInteger scrapSetCount
    ) {
    }
}
