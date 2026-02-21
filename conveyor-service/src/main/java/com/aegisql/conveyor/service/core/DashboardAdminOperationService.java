package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DashboardAdminOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardAdminOperationService.class);
    private static final String ADMIN_OUTPUT_SOURCE_KEY = "admin";
    private static final String ADMIN_OUTPUT_TITLE = "Admin";

    private final DashboardService dashboardService;
    private final ConcurrentMap<String, ConcurrentLinkedQueue<Map<String, Object>>> eventsByUser = new ConcurrentHashMap<>();
    private final ExecutorService operationExecutor;

    public DashboardAdminOperationService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        AtomicInteger threadCounter = new AtomicInteger(0);
        this.operationExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "dashboard-admin-op-" + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @PreDestroy
    void shutdown() {
        operationExecutor.shutdownNow();
    }

    public Map<String, Object> scheduleReload(String username, String conveyorName, String stopTimeout) {
        return schedule(username, conveyorName, stopTimeout, AdminOperation.RELOAD);
    }

    public Map<String, Object> scheduleDelete(String username, String conveyorName, String stopTimeout) {
        return schedule(username, conveyorName, stopTimeout, AdminOperation.DELETE);
    }

    public List<Map<String, Object>> drainEvents(String username) {
        String normalizedUser = normalizeUsername(username);
        ConcurrentLinkedQueue<Map<String, Object>> queue = eventsByUser.get(normalizedUser);
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> event;
        while ((event = queue.poll()) != null) {
            events.add(event);
        }
        if (queue.isEmpty()) {
            eventsByUser.remove(normalizedUser, queue);
        }
        return List.copyOf(events);
    }

    private Map<String, Object> schedule(
            String username,
            String conveyorName,
            String stopTimeout,
            AdminOperation operation
    ) {
        String normalizedUser = normalizeUsername(username);
        String normalizedConveyorName = requireText(conveyorName, "Conveyor name is required");
        String normalizedStopTimeout = normalizeOptionalInput(stopTimeout);
        Map<String, Object> requestPayload = buildRequestPayload(normalizedConveyorName, normalizedStopTimeout, operation);

        Map<String, Object> scheduledEvent = buildScheduledEvent(normalizedConveyorName, operation, requestPayload);
        operationExecutor.execute(() -> runOperation(
                normalizedUser,
                normalizedConveyorName,
                normalizedStopTimeout,
                operation,
                requestPayload
        ));
        return scheduledEvent;
    }

    private void runOperation(
            String username,
            String conveyorName,
            String stopTimeout,
            AdminOperation operation,
            Map<String, Object> requestPayload
    ) {
        long startedNanos = System.nanoTime();
        try {
            if (operation == AdminOperation.RELOAD) {
                dashboardService.reload(conveyorName, stopTimeout);
            } else {
                dashboardService.delete(conveyorName, stopTimeout);
            }
            long elapsedMillis = elapsedMillis(startedNanos);
            enqueueEvent(username, buildCompletedEvent(conveyorName, operation, requestPayload, elapsedMillis));
            LOG.info("Admin {} completed for conveyor='{}' in {}", operation.operationId, conveyorName, formatDuration(elapsedMillis));
        } catch (Throwable throwable) {
            Throwable cause = rootCause(throwable);
            long elapsedMillis = elapsedMillis(startedNanos);
            LOG.error("Admin {} failed for conveyor='{}': {}", operation.operationId, conveyorName, safeError(cause), cause);
            enqueueEvent(username, buildFailedEvent(conveyorName, operation, requestPayload, cause, elapsedMillis));
        }
    }

    private void enqueueEvent(String username, Map<String, Object> event) {
        eventsByUser.computeIfAbsent(username, ignored -> new ConcurrentLinkedQueue<>()).add(event);
    }

    private Map<String, Object> buildScheduledEvent(
            String conveyorName,
            AdminOperation operation,
            Map<String, Object> requestPayload
    ) {
        String summaryLine = operation.scheduledMessage(conveyorName);
        Map<String, Object> responsePayload = Map.of(
                "phase", "scheduled",
                "status", "SCHEDULED",
                "completedAt", Instant.now().toString()
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", operation.operationId);
        payload.put("phase", "scheduled");
        payload.put("treeRefresh", Boolean.FALSE);
        payload.put("message", summaryLine);
        payload.put("request", requestPayload);
        payload.put("response", responsePayload);
        payload.put("result", Map.of(
                "message", summaryLine,
                "request", requestPayload,
                "response", responsePayload
        ));
        return buildOutputEvent(
                buildStatusPayload(HttpStatus.ACCEPTED.value(), Boolean.TRUE, "SCHEDULED", null, null, "0 ms", summaryLine),
                payload
        );
    }

    private Map<String, Object> buildCompletedEvent(
            String conveyorName,
            AdminOperation operation,
            Map<String, Object> requestPayload,
            long responseMillis
    ) {
        String summaryLine = operation.completedMessage(conveyorName);
        Map<String, Object> responsePayload = Map.of(
                "phase", "completed",
                "status", "COMPLETED",
                "result", Boolean.TRUE,
                "responseMillis", responseMillis,
                "completedAt", Instant.now().toString()
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", operation.operationId);
        payload.put("phase", "completed");
        payload.put("treeRefresh", Boolean.TRUE);
        payload.put("message", summaryLine);
        payload.put("request", requestPayload);
        payload.put("response", responsePayload);
        payload.put("result", Map.of(
                "message", summaryLine,
                "request", requestPayload,
                "response", responsePayload
        ));
        return buildOutputEvent(
                buildStatusPayload(
                        HttpStatus.OK.value(),
                        Boolean.TRUE,
                        "COMPLETED",
                        null,
                        null,
                        formatDuration(responseMillis),
                        summaryLine
                ),
                payload
        );
    }

    private Map<String, Object> buildFailedEvent(
            String conveyorName,
            AdminOperation operation,
            Map<String, Object> requestPayload,
            Throwable throwable,
            long responseMillis
    ) {
        HttpStatus httpStatus = mapHttpStatus(throwable);
        String errorCode = mapErrorCode(throwable);
        String errorMessage = safeError(throwable);
        String summaryLine = operation.failedMessage(conveyorName, errorMessage);

        Map<String, Object> responsePayload = Map.of(
                "phase", "failed",
                "status", "FAILED",
                "result", Boolean.FALSE,
                "errorCode", errorCode == null ? "" : errorCode,
                "errorMessage", errorMessage,
                "responseMillis", responseMillis,
                "completedAt", Instant.now().toString()
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", operation.operationId);
        payload.put("phase", "failed");
        payload.put("treeRefresh", Boolean.TRUE);
        payload.put("message", summaryLine);
        payload.put("exceptionClass", throwable.getClass().getName());
        payload.put("errorMessage", errorMessage);
        payload.put("request", requestPayload);
        payload.put("response", responsePayload);
        payload.put("result", Map.of(
                "message", summaryLine,
                "request", requestPayload,
                "response", responsePayload
        ));

        return buildOutputEvent(
                buildStatusPayload(
                        httpStatus.value(),
                        Boolean.FALSE,
                        "FAILED",
                        errorCode,
                        errorMessage,
                        formatDuration(responseMillis),
                        summaryLine
                ),
                payload
        );
    }

    private Map<String, Object> buildRequestPayload(String conveyorName, String stopTimeout, AdminOperation operation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", operation.operationId);
        payload.put("conveyor", conveyorName);
        payload.put("stopTimeout", StringUtils.hasText(stopTimeout) ? stopTimeout : "");
        payload.put("submittedAt", Instant.now().toString());
        return payload;
    }

    private Map<String, Object> buildOutputEvent(Map<String, Object> status, Map<String, Object> payload) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("sourceType", "admin");
        event.put("sourceKey", ADMIN_OUTPUT_SOURCE_KEY);
        event.put("title", ADMIN_OUTPUT_TITLE);
        event.put("status", status);
        event.put("payload", payload);
        return event;
    }

    private Map<String, Object> buildStatusPayload(
            int httpStatus,
            Object result,
            String status,
            String errorCode,
            String errorMessage,
            String responseTime,
            String summaryLine
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("httpStatus", httpStatus);
        payload.put("result", result);
        payload.put("status", status);
        payload.put("errorCode", errorCode);
        payload.put("errorMessage", errorMessage);
        payload.put("responseTime", responseTime);
        payload.put("summaryLine", summaryLine);
        return payload;
    }

    private HttpStatus mapHttpStatus(Throwable throwable) {
        if (throwable instanceof ConveyorNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (throwable instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String mapErrorCode(Throwable throwable) {
        if (throwable instanceof ConveyorNotFoundException) {
            return "NOT_FOUND";
        }
        if (throwable instanceof IllegalArgumentException) {
            return "BAD_REQUEST";
        }
        return "INTERNAL_ERROR";
    }

    private String normalizeUsername(String username) {
        return requireText(username, "Authenticated user is required");
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptionalInput(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String safeError(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }
        if (StringUtils.hasText(throwable.getMessage())) {
            return throwable.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    private String formatDuration(long millis) {
        if (millis < 1_000) {
            return millis + " ms";
        }
        long seconds = millis / 1_000;
        long remainderMillis = millis % 1_000;
        if (seconds < 60) {
            return seconds + "." + String.format("%03d", remainderMillis) + " s";
        }
        long minutes = seconds / 60;
        long remainderSeconds = seconds % 60;
        return minutes + "m " + remainderSeconds + "." + String.format("%03d", remainderMillis) + "s";
    }

    private enum AdminOperation {
        RELOAD("reload", "reload", "reload is complete"),
        DELETE("delete", "deletion", "deleted");

        private final String operationId;
        private final String scheduledSuffix;
        private final String completedSuffix;

        AdminOperation(String operationId, String scheduledSuffix, String completedSuffix) {
            this.operationId = operationId;
            this.scheduledSuffix = scheduledSuffix;
            this.completedSuffix = completedSuffix;
        }

        private String scheduledMessage(String conveyorName) {
            return "Conveyor " + conveyorName + " scheduled for " + scheduledSuffix;
        }

        private String completedMessage(String conveyorName) {
            return "Conveyor " + conveyorName + " " + completedSuffix;
        }

        private String failedMessage(String conveyorName, String errorMessage) {
            return "Conveyor " + conveyorName + " " + operationId + " failed: " + errorMessage;
        }
    }
}
