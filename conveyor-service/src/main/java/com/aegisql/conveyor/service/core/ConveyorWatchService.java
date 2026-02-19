package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.BuildingSite;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ConveyorWatchService {

    private static final Logger LOG = LoggerFactory.getLogger(ConveyorWatchService.class);
    private static final long PING_INTERVAL_MILLIS = 5_000L;
    // Keep dedupe window short to suppress only duplicate callback fan-out, not new runs.
    private static final long DATA_EVENT_DEDUPE_WINDOW_MILLIS = 250L;
    private static final int DATA_EVENT_DEDUPE_CAPACITY = 64;

    private final ObjectMapper objectMapper;
    private final int defaultForeachHistoryLimit;
    private final ConcurrentMap<String, ConcurrentMap<String, WatchSubscription>> watchesByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArraySet<WatchSubscription>> watchesByConveyor = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArraySet<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final Set<String> hookedConveyorNames = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService pingScheduler;

    public ConveyorWatchService(
            ObjectMapper objectMapper,
            @Value("${conveyor.service.dashboard.default-watch-history-limit:100}") int defaultWatchHistoryLimit
    ) {
        this.objectMapper = objectMapper;
        this.defaultForeachHistoryLimit = normalizeHistoryLimitValue(defaultWatchHistoryLimit);
        this.pingScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "conveyor-watch-ping");
            thread.setDaemon(true);
            return thread;
        });
        this.pingScheduler.scheduleWithFixedDelay(this::sendPingSafely,
                PING_INTERVAL_MILLIS, PING_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    @PostConstruct
    void init() {
        ensureKnownConveyorsHooked();
    }

    @PreDestroy
    void shutdown() {
        pingScheduler.shutdownNow();
    }

    public Map<String, Object> registerWatch(
            String username,
            String conveyorName,
            String correlationId,
            boolean forEach,
            Integer maxEntries
    ) {
        String normalizedUser = normalizeUsername(username, "Authenticated username is required for watch registration");
        String normalizedConveyor = requireText(conveyorName, "Conveyor name is required for watch registration");
        String normalizedCorrelationId = forEach ? "*" : requireText(correlationId,
                "ID is required for non-foreach watch registration");

        Conveyor<?, ?, ?> conveyor = resolveConveyor(normalizedConveyor);
        ensureConveyorHooked(conveyor);

        String watchId = watchId(normalizedConveyor, normalizedCorrelationId, forEach);
        int historyLimit = forEach ? normalizeHistoryLimit(maxEntries) : 1;

        ConcurrentMap<String, WatchSubscription> userWatches = watchesByUser.computeIfAbsent(
                normalizedUser, key -> new ConcurrentHashMap<>()
        );

        synchronized (userWatches) {
            WatchSubscription existing = userWatches.get(watchId);
            if (existing != null && existing.isActive()) {
                return existing.snapshot();
            }

            WatchSubscription subscription = new WatchSubscription(
                    normalizedUser,
                    normalizedConveyor,
                    forEach ? null : normalizedCorrelationId,
                    forEach,
                    historyLimit
            );
            userWatches.put(subscription.watchId(), subscription);
            watchesByConveyor.computeIfAbsent(normalizedConveyor, key -> new CopyOnWriteArraySet<>()).add(subscription);
            return subscription.snapshot();
        }
    }

    public boolean cancelWatch(String username, String conveyorName, String correlationId, boolean forEach) {
        String normalizedUser = normalizeUsername(username, "Authenticated username is required for watch cancellation");
        String normalizedConveyor = requireText(conveyorName, "Conveyor name is required for watch cancellation");
        String normalizedCorrelationId = forEach ? "*" : requireText(correlationId,
                "ID is required for non-foreach watch cancellation");

        return removeSubscription(normalizedUser, watchId(normalizedConveyor, normalizedCorrelationId, forEach), true);
    }

    public boolean cancelWatchById(String username, String watchId) {
        String normalizedUser = normalizeUsername(username, "Authenticated username is required for watch cancellation");
        String normalizedWatchId = requireText(watchId, "watchId is required for watch cancellation");
        return removeSubscription(normalizedUser, normalizedWatchId, true);
    }

    public int updateForeachHistoryLimit(String username, Integer historyLimit) {
        String normalizedUser = normalizeUsername(username, "Authenticated username is required for watch update");
        int normalizedLimit = normalizeHistoryLimit(historyLimit);
        ConcurrentMap<String, WatchSubscription> userWatches = watchesByUser.get(normalizedUser);
        if (userWatches == null || userWatches.isEmpty()) {
            return 0;
        }
        int updated = 0;
        synchronized (userWatches) {
            for (WatchSubscription subscription : userWatches.values()) {
                if (!subscription.isActive() || !subscription.forEach()) {
                    continue;
                }
                subscription.updateHistoryLimit(normalizedLimit);
                updated++;
            }
        }
        return updated;
    }

    public List<Map<String, Object>> activeWatchesForUser(String username) {
        if (!StringUtils.hasText(username)) {
            return List.of();
        }
        String normalizedUser = normalizeUsername(username, "Authenticated username is required for watch listing");
        ConcurrentMap<String, WatchSubscription> userWatches = watchesByUser.get(normalizedUser);
        if (userWatches == null || userWatches.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (WatchSubscription subscription : userWatches.values()) {
            snapshots.add(subscription.snapshot());
        }
        snapshots.sort(Comparator.comparing(snapshot -> String.valueOf(snapshot.get("displayName"))));
        return snapshots;
    }

    public void registerSession(WebSocketSession session) {
        String username = resolveSessionUsername(session);
        if (!StringUtils.hasText(username)) {
            closeSession(session);
            return;
        }
        String normalizedUser = normalizeUsername(username, "WebSocket username is required");
        CopyOnWriteArraySet<WebSocketSession> sessions = sessionsByUser.computeIfAbsent(
                normalizedUser, key -> new CopyOnWriteArraySet<>()
        );
        sessions.add(session);
        LOG.debug("Registered watch websocket session for user '{}' activeSessions={}", normalizedUser, sessions.size());
    }

    public void unregisterSession(WebSocketSession session) {
        if (session == null) {
            return;
        }
        String username = resolveSessionUsername(session);
        if (StringUtils.hasText(username)) {
            String normalizedUser = normalizeUsername(username, "WebSocket username is required");
            CopyOnWriteArraySet<WebSocketSession> sessions = sessionsByUser.get(normalizedUser);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionsByUser.remove(normalizedUser, sessions);
                }
                LOG.debug("Unregistered watch websocket session for user '{}' activeSessions={}",
                        normalizedUser, sessions == null ? 0 : sessions.size());
            }
            return;
        }
        for (Map.Entry<String, CopyOnWriteArraySet<WebSocketSession>> entry : sessionsByUser.entrySet()) {
            CopyOnWriteArraySet<WebSocketSession> sessions = entry.getValue();
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(entry.getKey(), sessions);
            }
        }
    }

    public void ensureConveyorHooked(String conveyorName) {
        if (!StringUtils.hasText(conveyorName)) {
            return;
        }
        ensureConveyorHooked(resolveConveyor(conveyorName));
    }

    public void ensureKnownConveyorsHooked() {
        Set<String> names = ConcurrentHashMap.newKeySet();
        try {
            names.addAll(Conveyor.getKnownConveyorNames());
        } catch (Exception e) {
            LOG.debug("Failed to read known conveyor names while ensuring watch hooks", e);
        }
        try {
            names.addAll(Conveyor.getRegisteredConveyorNames());
        } catch (Exception e) {
            LOG.debug("Failed to read registered conveyor names while ensuring watch hooks", e);
        }
        for (String name : names) {
            try {
                ensureConveyorHooked(name);
            } catch (Exception e) {
                LOG.debug("Unable to attach watch bridge to conveyor '{}'", name, e);
            }
        }
    }

    public void ensureConveyorHooked(Conveyor<?, ?, ?> conveyor) {
        if (conveyor == null) {
            return;
        }
        String conveyorName = conveyor.getName();
        if (!StringUtils.hasText(conveyorName)) {
            return;
        }

        boolean shouldHook = hookedConveyorNames.add(conveyorName);
        if (!shouldHook) {
            return;
        }

        try {
            attachBridgeConsumers(conveyor, conveyorName);
            LOG.info("Watch bridge attached to conveyor '{}'", conveyor.getName());
        } catch (Exception e) {
            hookedConveyorNames.remove(conveyorName);
            LOG.warn("Failed to attach watch bridge to conveyor '{}'", conveyor.getName(), e);
        }
    }

    private <K, OUT> void attachBridgeConsumers(Conveyor<K, ?, OUT> conveyor, String conveyorName) {
        ResultConsumer<K, OUT> bridgeResultConsumer = bin -> onResult(bin, conveyorName);
        conveyor.resultConsumer().andThen(bridgeResultConsumer).set();

        ScrapConsumer<K, ?> bridgeScrapConsumer = bin -> onScrap(bin, conveyorName);
        conveyor.scrapConsumer().andThen(bridgeScrapConsumer).set();
    }

    private void onResult(ProductBin<?, ?> bin, String hookConveyorName) {
        if (bin == null || bin.conveyor == null || !StringUtils.hasText(bin.conveyor.getName())) {
            return;
        }
        if (!isHookSourceMatch(hookConveyorName, bin.conveyor.getName())) {
            return;
        }
        List<WatchSubscription> subscriptions = subscriptionsFor(bin.conveyor);
        if (subscriptions.isEmpty()) {
            return;
        }

        for (WatchSubscription subscription : subscriptions) {
            if (!subscription.matches(bin.key)) {
                continue;
            }
            boolean terminalWatchEvent = !subscription.forEach();
            PlacementResult<Object> event = buildResultEvent(subscription, bin, !terminalWatchEvent && subscription.isActive());
            boolean accepted = subscription.recordDataEvent(event);
            if (!accepted) {
                continue;
            }
            sendToUser(subscription.username(), event);
            if (terminalWatchEvent) {
                completeSubscription(subscription.username(), subscription.watchId());
            }
        }
    }

    private void onScrap(ScrapBin<?, ?> bin, String hookConveyorName) {
        if (bin == null || bin.conveyor == null || !StringUtils.hasText(bin.conveyor.getName())) {
            return;
        }
        if (!isHookSourceMatch(hookConveyorName, bin.conveyor.getName())) {
            return;
        }
        List<WatchSubscription> subscriptions = subscriptionsFor(bin.conveyor);
        if (subscriptions.isEmpty()) {
            return;
        }

        for (WatchSubscription subscription : subscriptions) {
            if (!subscription.matches(bin.key)) {
                continue;
            }
            boolean terminalWatchEvent = !subscription.forEach();
            PlacementResult<Object> event = buildScrapEvent(subscription, bin, !terminalWatchEvent && subscription.isActive());
            boolean accepted = subscription.recordDataEvent(event);
            if (!accepted) {
                continue;
            }
            sendToUser(subscription.username(), event);
            if (terminalWatchEvent) {
                completeSubscription(subscription.username(), subscription.watchId());
            }
        }
    }

    private List<WatchSubscription> subscriptionsFor(Conveyor<?, ?, ?> sourceConveyor) {
        if (sourceConveyor == null) {
            return List.of();
        }
        LinkedHashSet<WatchSubscription> subscriptions = new LinkedHashSet<>();
        Conveyor<?, ?, ?> cursor = sourceConveyor;
        int depth = 0;
        while (cursor != null && depth < 16) {
            String name = cursor.getName();
            if (StringUtils.hasText(name)) {
                CopyOnWriteArraySet<WatchSubscription> direct = watchesByConveyor.get(name);
                if (direct != null && !direct.isEmpty()) {
                    subscriptions.addAll(direct);
                }
            }
            try {
                cursor = cursor.getEnclosingConveyor();
            } catch (Exception e) {
                break;
            }
            depth++;
        }
        if (subscriptions.isEmpty()) {
            return List.of();
        }
        return List.copyOf(subscriptions);
    }

    private boolean isHookSourceMatch(String hookConveyorName, String sourceConveyorName) {
        if (!StringUtils.hasText(hookConveyorName) || !StringUtils.hasText(sourceConveyorName)) {
            return true;
        }
        return Objects.equals(hookConveyorName, sourceConveyorName);
    }

    private PlacementResult<Object> buildResultEvent(WatchSubscription subscription, ProductBin<?, ?> bin, boolean watchActive) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("eventType", "RESULT");
        props.put("watchId", subscription.watchId());
        props.put("displayName", subscription.displayName());
        props.put("conveyor", subscription.conveyor());
        props.put("sourceConveyor", bin.conveyor == null ? null : bin.conveyor.getName());
        props.put("buildCreationTime", extractResultCreationTime(bin));
        props.put("eventSignature", signatureForResult(bin));
        props.put("foreach", subscription.forEach());
        props.put("historyLimit", subscription.historyLimit());
        props.put("watchActive", watchActive);
        props.put("buildStatus", bin.status == null ? "UNKNOWN" : bin.status.name());
        addBinProperties(props, bin == null ? null : bin.properties);

        return PlacementResult.<Object>builder()
                .timestamp(Instant.now())
                .correlationId(bin.key == null ? null : String.valueOf(bin.key))
                .label(bin.status == null ? "UNKNOWN" : bin.status.name())
                .result(sanitizeForJson(bin.product))
                .properties(props)
                .status(mapStatus(bin.status))
                .build();
    }

    private PlacementResult<Object> buildScrapEvent(WatchSubscription subscription, ScrapBin<?, ?> bin, boolean watchActive) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("eventType", "SCRAP");
        props.put("watchId", subscription.watchId());
        props.put("displayName", subscription.displayName());
        props.put("conveyor", subscription.conveyor());
        props.put("sourceConveyor", bin.conveyor == null ? null : bin.conveyor.getName());
        props.put("buildCreationTime", extractScrapCreationTime(bin));
        props.put("eventSignature", signatureForScrap(bin));
        props.put("foreach", subscription.forEach());
        props.put("historyLimit", subscription.historyLimit());
        props.put("watchActive", watchActive);
        props.put("failureType", bin.failureType == null ? "UNKNOWN" : bin.failureType.name());
        props.put("comment", bin.comment);
        props.put("scrap", sanitizeForJson(bin.scrap));
        addBinProperties(props, bin == null ? null : bin.properties);

        var builder = PlacementResult.<Object>builder()
                .timestamp(Instant.now())
                .correlationId(bin.key == null ? null : String.valueOf(bin.key))
                .label("SCRAP")
                .properties(props)
                .status(PlacementStatus.FAILED)
                .errorCode(bin.failureType == null ? "SCRAP" : bin.failureType.name())
                .errorMessage(StringUtils.hasText(bin.comment) ? bin.comment : "Conveyor emitted scrap")
                .exceptionClass(bin.error == null ? null : bin.error.getClass().getName());

        if (bin.error != null && StringUtils.hasText(bin.error.getMessage())) {
            builder.errorMessage(bin.error.getMessage());
        }

        return builder.build();
    }

    private PlacementResult<Object> buildPingEvent(WatchSubscription subscription, Instant now) {
        long waitMillis = Duration.between(subscription.lastDataAt(), now).toMillis();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("waitingSince", subscription.createdAt().toString());
        result.put("waitMillis", waitMillis);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("eventType", "PING");
        props.put("watchId", subscription.watchId());
        props.put("displayName", subscription.displayName());
        props.put("conveyor", subscription.conveyor());
        props.put("foreach", subscription.forEach());
        props.put("historyLimit", subscription.historyLimit());
        props.put("watchActive", subscription.isActive());

        return PlacementResult.<Object>builder()
                .timestamp(now)
                .correlationId(subscription.correlationId())
                .label("PING")
                .result(result)
                .properties(props)
                .status(PlacementStatus.IN_PROGRESS)
                .build();
    }

    private PlacementResult<Object> buildCancelEvent(WatchSubscription subscription) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("eventType", "CANCELED");
        props.put("watchId", subscription.watchId());
        props.put("displayName", subscription.displayName());
        props.put("conveyor", subscription.conveyor());
        props.put("foreach", subscription.forEach());
        props.put("historyLimit", subscription.historyLimit());
        props.put("watchActive", false);

        return PlacementResult.<Object>builder()
                .timestamp(Instant.now())
                .correlationId(subscription.correlationId())
                .label("WATCH_CANCELED")
                .result(Boolean.TRUE)
                .properties(props)
                .status(PlacementStatus.COMPLETED)
                .build();
    }

    private boolean completeSubscription(String username, String watchId) {
        ConcurrentMap<String, WatchSubscription> userWatches = watchesByUser.get(username);
        if (userWatches == null) {
            return false;
        }

        WatchSubscription completed;
        synchronized (userWatches) {
            completed = userWatches.get(watchId);
            if (completed == null || !completed.isActive()) {
                return false;
            }
            completed.deactivate();
        }

        CopyOnWriteArraySet<WatchSubscription> conveyorWatches = watchesByConveyor.get(completed.conveyor());
        if (conveyorWatches != null) {
            conveyorWatches.remove(completed);
            if (conveyorWatches.isEmpty()) {
                watchesByConveyor.remove(completed.conveyor(), conveyorWatches);
            }
        }
        return true;
    }

    private boolean removeSubscription(String username, String watchId, boolean emitCancelEvent) {
        ConcurrentMap<String, WatchSubscription> userWatches = watchesByUser.get(username);
        if (userWatches == null) {
            return false;
        }

        WatchSubscription removed;
        synchronized (userWatches) {
            removed = userWatches.remove(watchId);
            if (removed == null) {
                return false;
            }
        }

        removed.deactivate();

        CopyOnWriteArraySet<WatchSubscription> conveyorWatches = watchesByConveyor.get(removed.conveyor());
        if (conveyorWatches != null) {
            conveyorWatches.remove(removed);
            if (conveyorWatches.isEmpty()) {
                watchesByConveyor.remove(removed.conveyor(), conveyorWatches);
            }
        }

        if (userWatches.isEmpty()) {
            watchesByUser.remove(username, userWatches);
        }

        if (emitCancelEvent) {
            sendToUser(username, buildCancelEvent(removed));
        }

        return true;
    }

    private void sendPingSafely() {
        try {
            sendPings();
        } catch (Throwable t) {
            LOG.debug("Watch ping dispatch failed", t);
        }
    }

    private void sendPings() {
        Instant now = Instant.now();
        for (Map.Entry<String, ConcurrentMap<String, WatchSubscription>> userEntry : watchesByUser.entrySet()) {
            String username = userEntry.getKey();
            for (WatchSubscription subscription : userEntry.getValue().values()) {
                if (!subscription.shouldPing(now, PING_INTERVAL_MILLIS)) {
                    continue;
                }
                subscription.markPing(now);
                sendToUser(username, buildPingEvent(subscription, now));
            }
        }
    }

    private void sendToUser(String username, PlacementResult<Object> payload) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        String normalizedUser = normalizeUsername(username, "Watch event username is required");
        CopyOnWriteArraySet<WebSocketSession> sessions = sessionsByUser.get(normalizedUser);
        if (sessions == null || sessions.isEmpty()) {
            Map<String, Object> props = payload == null || payload.getProperties() == null ? Map.of() : payload.getProperties();
            LOG.debug("No active watch websocket sessions for user '{}' eventType={} correlationId={} sourceConveyor={}",
                    normalizedUser,
                    props.get("eventType"),
                    payload == null ? null : payload.getCorrelationId(),
                    props.get("sourceConveyor"));
            return;
        }

        final String text;
        try {
            text = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            LOG.debug("Failed to serialize watch payload for user '{}'", normalizedUser, e);
            return;
        }

        TextMessage message = new TextMessage(text);
        LOG.debug("Dispatching watch websocket event to user '{}' sessions={} payloadBytes={}",
                normalizedUser, sessions.size(), text.length());
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                unregisterSession(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                LOG.debug("Failed sending watch websocket event to user '{}' sessionId={}",
                        normalizedUser, session.getId(), e);
                unregisterSession(session);
            }
        }
    }

    private void closeSession(WebSocketSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (IOException ignored) {
            // No-op
        }
    }

    private PlacementStatus mapStatus(Status status) {
        if (status == null) {
            return PlacementStatus.COMPLETED;
        }
        return switch (status) {
            case WAITING_DATA -> PlacementStatus.IN_PROGRESS;
            case TIMED_OUT -> PlacementStatus.TIMEOUT_WAITING_FOR_COMPLETION;
            case READY -> PlacementStatus.COMPLETED;
            case CANCELED, NOT_FOUND -> PlacementStatus.REJECTED;
            case INVALID -> PlacementStatus.FAILED;
        };
    }

    private Object sanitizeForJson(Object source) {
        if (source == null) {
            return null;
        }
        try {
            byte[] json = objectMapper.writeValueAsBytes(source);
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ignored) {
            return String.valueOf(source);
        }
    }

    private Conveyor<?, ?, ?> resolveConveyor(String name) {
        try {
            return Conveyor.byName(name);
        } catch (Exception e) {
            throw new ConveyorNotFoundException(name);
        }
    }

    private int normalizeHistoryLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return defaultForeachHistoryLimit;
        }
        return normalizeHistoryLimitValue(requested);
    }

    private int normalizeHistoryLimitValue(int requested) {
        if (requested <= 0) {
            return 1;
        }
        return requested;
    }

    private String watchId(String conveyor, String correlationId, boolean forEach) {
        return conveyor + "|" + (forEach ? "*" : correlationId);
    }

    private String requireText(String text, String message) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException(message);
        }
        return text.trim();
    }

    private String normalizeUsername(String text, String message) {
        return requireText(text, message).toLowerCase(Locale.ROOT);
    }

    private String resolveSessionUsername(WebSocketSession session) {
        if (session == null) {
            return null;
        }
        if (session.getPrincipal() != null && StringUtils.hasText(session.getPrincipal().getName())) {
            return session.getPrincipal().getName();
        }
        Map<String, Object> attrs = session.getAttributes();
        if (attrs == null || attrs.isEmpty()) {
            return null;
        }
        Object securityContext = attrs.get("SPRING_SECURITY_CONTEXT");
        if (!(securityContext instanceof SecurityContext context)) {
            return null;
        }
        Authentication authentication = context.getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }

    private Long extractResultCreationTime(ProductBin<?, ?> bin) {
        if (bin == null || bin.properties == null) {
            return null;
        }
        Long direct = parseLongValue(bin.properties.get("creationTime"));
        if (direct != null) {
            return direct;
        }
        Long builderCreated = parseLongValue(bin.properties.get("builderCreated"));
        if (builderCreated != null) {
            return builderCreated;
        }
        return parseLongValue(bin.properties.get("cartCreated"));
    }

    private Long extractScrapCreationTime(ScrapBin<?, ?> bin) {
        if (bin == null) {
            return null;
        }
        Object scrap = bin.scrap;
        if (scrap instanceof BuildingSite<?, ?, ?, ?> site) {
            return site.getState().builderCreated;
        }
        if (scrap instanceof Cart<?, ?, ?> cart) {
            return cart.getCreationTime();
        }
        if (bin.properties != null) {
            Long direct = parseLongValue(bin.properties.get("creationTime"));
            if (direct != null) {
                return direct;
            }
            Long builderCreated = parseLongValue(bin.properties.get("builderCreated"));
            if (builderCreated != null) {
                return builderCreated;
            }
            return parseLongValue(bin.properties.get("cartCreated"));
        }
        return null;
    }

    private String signatureForResult(ProductBin<?, ?> bin) {
        String sourceConveyor = bin == null || bin.conveyor == null ? "" : String.valueOf(bin.conveyor.getName());
        String key = bin == null || bin.key == null ? "" : String.valueOf(bin.key);
        String creation = String.valueOf(extractResultCreationTime(bin));
        return "RESULT|" + sourceConveyor + "|" + key + "|" + creation;
    }

    private String signatureForScrap(ScrapBin<?, ?> bin) {
        String sourceConveyor = bin == null || bin.conveyor == null ? "" : String.valueOf(bin.conveyor.getName());
        String key = bin == null || bin.key == null ? "" : String.valueOf(bin.key);
        String creation = String.valueOf(extractScrapCreationTime(bin));
        return "SCRAP|" + sourceConveyor + "|" + key + "|" + creation;
    }

    private Long parseLongValue(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void addBinProperties(Map<String, Object> payloadProps, Map<String, Object> binProperties) {
        if (payloadProps == null || binProperties == null || binProperties.isEmpty()) {
            return;
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : binProperties.entrySet()) {
            String key = String.valueOf(entry.getKey());
            sanitized.put(key, sanitizeForJson(entry.getValue()));
        }
        if (sanitized.isEmpty()) {
            return;
        }
        payloadProps.put("binProperties", sanitized);
        for (Map.Entry<String, Object> entry : sanitized.entrySet()) {
            payloadProps.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static final class WatchSubscription {
        private final String username;
        private final String conveyor;
        private final String correlationId;
        private final boolean forEach;
        private volatile int historyLimit;
        private final String watchId;
        private final String displayName;
        private final Instant createdAt;

        private final Deque<PlacementResult<Object>> history = new ArrayDeque<>();
        private final Deque<EventFingerprint> recentDataFingerprints = new ArrayDeque<>();

        private volatile Instant lastDataAt;
        private volatile Instant lastPingAt;
        private volatile boolean active = true;
        private WatchSubscription(String username, String conveyor, String correlationId, boolean forEach, int historyLimit) {
            this.username = username;
            this.conveyor = conveyor;
            this.correlationId = correlationId;
            this.forEach = forEach;
            this.historyLimit = historyLimit;
            this.watchId = conveyor + "|" + (forEach ? "*" : correlationId);
            this.displayName = forEach ? conveyor + " *" : conveyor + " " + correlationId;
            this.createdAt = Instant.now();
            this.lastDataAt = this.createdAt;
            this.lastPingAt = this.createdAt.minusMillis(PING_INTERVAL_MILLIS);
        }

        private String username() {
            return username;
        }

        private String conveyor() {
            return conveyor;
        }

        private String correlationId() {
            return correlationId;
        }

        private boolean forEach() {
            return forEach;
        }

        private String watchId() {
            return watchId;
        }

        private int historyLimit() {
            return historyLimit;
        }

        private String displayName() {
            return displayName;
        }

        private Instant createdAt() {
            return createdAt;
        }

        private Instant lastDataAt() {
            return lastDataAt;
        }

        private boolean isActive() {
            return active;
        }

        private boolean matches(Object key) {
            if (!active) {
                return false;
            }
            if (forEach) {
                return true;
            }
            return Objects.equals(correlationId, key == null ? null : String.valueOf(key));
        }

        private synchronized boolean recordDataEvent(PlacementResult<Object> event) {
            if (!active) {
                return false;
            }
            Instant now = Instant.now();
            String fingerprint = fingerprint(event);
            trimRecentFingerprints(now);
            for (EventFingerprint recent : recentDataFingerprints) {
                if (recent.value().equals(fingerprint)) {
                    Map<String, Object> props = event.getProperties() == null ? Map.of() : event.getProperties();
                    LOG.debug("Skipping duplicate watch event for {} eventType={} correlationId={} sourceConveyor={}",
                            watchId,
                            props.get("eventType"),
                            event.getCorrelationId(),
                            props.get("sourceConveyor"));
                    return false;
                }
            }
            recentDataFingerprints.addLast(new EventFingerprint(fingerprint, now));
            while (recentDataFingerprints.size() > DATA_EVENT_DEDUPE_CAPACITY) {
                recentDataFingerprints.removeFirst();
            }
            history.addLast(event);
            while (history.size() > historyLimit) {
                history.removeFirst();
            }
            lastDataAt = now;
            Map<String, Object> props = event.getProperties() == null ? Map.of() : event.getProperties();
            LOG.debug("Recorded watch event for {} eventType={} correlationId={} sourceConveyor={} signature={}",
                    watchId,
                    props.get("eventType"),
                    event.getCorrelationId(),
                    props.get("sourceConveyor"),
                    props.get("eventSignature"));
            return true;
        }

        private boolean shouldPing(Instant now, long intervalMillis) {
            if (!active) {
                return false;
            }
            long noDataMillis = Duration.between(lastDataAt, now).toMillis();
            long lastPingMillis = Duration.between(lastPingAt, now).toMillis();
            return noDataMillis >= intervalMillis && lastPingMillis >= intervalMillis;
        }

        private void markPing(Instant now) {
            lastPingAt = now;
        }

        private void deactivate() {
            active = false;
        }

        private synchronized void updateHistoryLimit(int newLimit) {
            historyLimit = Math.max(1, newLimit);
            while (history.size() > historyLimit) {
                history.removeFirst();
            }
        }

        private void trimRecentFingerprints(Instant now) {
            while (!recentDataFingerprints.isEmpty()) {
                EventFingerprint first = recentDataFingerprints.peekFirst();
                if (first == null) {
                    recentDataFingerprints.removeFirst();
                    continue;
                }
                long ageMillis = Duration.between(first.timestamp(), now).toMillis();
                if (ageMillis <= DATA_EVENT_DEDUPE_WINDOW_MILLIS) {
                    break;
                }
                recentDataFingerprints.removeFirst();
            }
        }

        private String fingerprint(PlacementResult<Object> event) {
            Map<String, Object> props = event.getProperties() == null ? Map.of() : event.getProperties();
            Object signature = props.get("eventSignature");
            if (signature != null) {
                return String.valueOf(signature);
            }
            Object eventType = props.get("eventType");
            Object sourceConveyor = props.get("sourceConveyor");
            Object buildCreationTime = props.get("buildCreationTime");
            return String.valueOf(eventType)
                    + "|"
                    + String.valueOf(sourceConveyor)
                    + "|"
                    + String.valueOf(event.getCorrelationId())
                    + "|"
                    + String.valueOf(buildCreationTime);
        }

        private synchronized List<PlacementResult<Object>> historySnapshot() {
            return List.copyOf(history);
        }

        private Map<String, Object> snapshot() {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("watchId", watchId);
            snapshot.put("displayName", displayName);
            snapshot.put("conveyor", conveyor);
            snapshot.put("correlationId", correlationId);
            snapshot.put("foreach", forEach);
            snapshot.put("active", active);
            snapshot.put("historyLimit", historyLimit);
            snapshot.put("createdAt", createdAt.toString());
            snapshot.put("lastDataAt", lastDataAt.toString());
            snapshot.put("events", historySnapshot());
            return snapshot;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof WatchSubscription that)) {
                return false;
            }
            return watchId.equals(that.watchId) && username.equals(that.username);
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, watchId);
        }

        @Override
        public String toString() {
            return "WatchSubscription{" +
                    "username='" + username + '\'' +
                    ", conveyor='" + conveyor + '\'' +
                    ", correlationId='" + correlationId + '\'' +
                    ", forEach=" + forEach +
                    ", active=" + active +
                    '}';
        }
    }

    private record EventFingerprint(String value, Instant timestamp) {
    }
}
