package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.loaders.CommandLoader;
import com.aegisql.conveyor.loaders.MultiKeyCommandLoader;
import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.aegisql.conveyor.service.util.RequestParsing;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class CommandService {

    private final ObjectMapper objectMapper;

    public CommandService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PlacementResult<Object> executeById(
            String conveyorName,
            String id,
            String operationName,
            byte[] body,
            Map<String, String> requestParams
    ) {
        return execute(conveyorName, id, false, operationName, body, requestParams);
    }

    public PlacementResult<Object> executeForEach(
            String conveyorName,
            String operationName,
            byte[] body,
            Map<String, String> requestParams
    ) {
        return execute(conveyorName, null, true, operationName, body, requestParams);
    }

    private PlacementResult<Object> execute(
            String conveyorName,
            String id,
            boolean forEach,
            String operationName,
            byte[] body,
            Map<String, String> requestParams
    ) {
        Conveyor<?, ?, ?> conveyor = resolveConveyor(conveyorName);
        CommandOperation operation = CommandOperation.fromString(operationName);

        if (forEach && !operation.foreachSupported) {
            throw new IllegalArgumentException("Command " + operation.apiName + " is not supported in foreach mode");
        }
        if (!forEach && !StringUtils.hasText(id)) {
            throw new IllegalArgumentException("id is required when foreach is false");
        }

        Map<String, String> remaining = new LinkedHashMap<>(requestParams);
        OptionalLong ttl = parseAndRemoveDuration("ttl", remaining);
        OptionalLong creationTime = parseAndRemoveInstant("creationTime", remaining);
        OptionalLong expirationTime = parseAndRemoveInstant("expirationTime", remaining);
        OptionalLong requestTtl = parseAndRemoveDuration("requestTTL", remaining);

        if (operation.requiresRescheduleTiming && ttl.isEmpty() && expirationTime.isEmpty()) {
            throw new IllegalArgumentException("reschedule requires ttl or expirationTime");
        }
        if (operation.requiresRequestTtl && requestTtl.isEmpty()) {
            throw new IllegalArgumentException(operation.apiName + " requires requestTTL");
        }
        if (operation == CommandOperation.ADD_PROPERTIES && remaining.isEmpty()) {
            throw new IllegalArgumentException("ADD_PROPERTIES requires at least one property");
        }
        if (operation != CommandOperation.ADD_PROPERTIES && !remaining.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unsupported parameters for command " + operation.apiName + ": " + String.join(", ", remaining.keySet())
            );
        }

        Map<String, Object> appliedProps = new LinkedHashMap<>();
        appliedProps.put("conveyor", conveyorName);
        appliedProps.put("correlationId", id);
        appliedProps.put("command", operation.apiName);
        appliedProps.put("foreach", forEach);

        CompletableFuture<?> future;
        if (forEach) {
            @SuppressWarnings("rawtypes")
            MultiKeyCommandLoader loader = conveyor.command().foreach();
            loader = applyCommonTiming(loader, ttl, creationTime, expirationTime);

            future = invokeForEach(conveyor, operation, loader, body, remaining);
        } else {
            @SuppressWarnings("rawtypes")
            CommandLoader loader = conveyor.command();
            loader = loader.id(convertSimple(id, resolveKeyType(conveyor)));
            loader = applyCommonTiming(loader, ttl, creationTime, expirationTime);

            future = invokeById(operation, loader, body, remaining);
        }

        if (operation == CommandOperation.ADD_PROPERTIES) {
            appliedProps.putAll(remaining);
        }

        return buildResult(
                castFuture(future),
                requestTtl,
                forEach ? null : id,
                operation.apiName,
                appliedProps
        );
    }

    private CompletableFuture<?> invokeById(
            CommandOperation operation,
            @SuppressWarnings("rawtypes") CommandLoader loader,
            byte[] body,
            Map<String, String> remaining
    ) {
        return switch (operation) {
            case CANCEL -> loader.cancel();
            case COMPLETE_EXCEPTIONALLY -> loader.completeExceptionally(new RuntimeException(readBodyMessage(body)));
            case ADD_PROPERTIES -> loader.addProperties(new LinkedHashMap<>(remaining));
            case TIMEOUT -> loader.timeout();
            case RESCHEDULE -> loader.reschedule();
            case CREATE -> loader.create();
            case PEEK -> loader.peek().thenApply(bin -> bin == null ? null : ((ProductBin<?, ?>) bin).product);
            case PEEK_ID -> loader.peekId(k -> {
            });
        };
    }

    private CompletableFuture<?> invokeForEach(
            Conveyor<?, ?, ?> conveyor,
            CommandOperation operation,
            @SuppressWarnings("rawtypes") MultiKeyCommandLoader loader,
            byte[] body,
            Map<String, String> remaining
    ) {
        return switch (operation) {
            case CANCEL -> loader.cancel();
            case COMPLETE_EXCEPTIONALLY -> invokeForEachCompleteExceptionally(conveyor, loader, body);
            case ADD_PROPERTIES -> loader.addProperties(toObjectMap(remaining));
            case TIMEOUT -> loader.timeout();
            case RESCHEDULE -> loader.reschedule();
            case PEEK -> loader.peek().thenApply(bins -> {
                Map<Object, Object> productsByKey = new LinkedHashMap<>();
                List<?> rawBins = (List<?>) bins;
                for (Object value : rawBins) {
                    if (value instanceof ProductBin<?, ?> bin) {
                        productsByKey.put(bin.key, bin.product);
                    }
                }
                return productsByKey;
            });
            case PEEK_ID -> {
                List<Object> ids = new ArrayList<>();
                yield loader.peekId(ids::add).thenApply(ok -> ids);
            }
            default -> throw new IllegalArgumentException("Command " + operation.apiName + " is not supported in foreach mode");
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private CompletableFuture<Boolean> invokeForEachCompleteExceptionally(
            Conveyor<?, ?, ?> conveyor,
            MultiKeyCommandLoader loader,
            byte[] body
    ) {
        String message = readBodyMessage(body);
        List<Object> ids = new ArrayList<>();
        Conveyor rawConveyor = (Conveyor) conveyor;
        return loader.peekId(ids::add).thenCompose(ignore -> {
            if (ids.isEmpty()) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }

            List<CompletableFuture<Boolean>> futures = new ArrayList<>(ids.size());
            for (Object key : ids) {
                CommandLoader perIdLoader = rawConveyor.command().id(key);
                perIdLoader = applyCommonTiming(perIdLoader, loader);
                futures.add(perIdLoader.completeExceptionally(new RuntimeException(message)));
            }

            CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            return allDone.thenApply(done ->
                    futures.stream().allMatch(future -> Boolean.TRUE.equals(future.getNow(Boolean.FALSE)))
            );
        });
    }

    @SuppressWarnings("rawtypes")
    private CommandLoader applyCommonTiming(CommandLoader loader, MultiKeyCommandLoader multiKeyLoader) {
        if (multiKeyLoader.ttlMsec > 0) {
            loader = loader.ttl(multiKeyLoader.ttlMsec, TimeUnit.MILLISECONDS);
        }
        if (multiKeyLoader.creationTime > 0) {
            loader = loader.creationTime(multiKeyLoader.creationTime);
        }
        if (multiKeyLoader.expirationTime > 0) {
            loader = loader.expirationTime(multiKeyLoader.expirationTime);
        }
        return loader;
    }

    private Map<String, Object> toObjectMap(Map<String, String> remaining) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.putAll(remaining);
        return props;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private CompletableFuture<Object> castFuture(CompletableFuture<?> future) {
        return (CompletableFuture) future;
    }

    @SuppressWarnings("rawtypes")
    private CommandLoader applyCommonTiming(
            CommandLoader loader,
            OptionalLong ttl,
            OptionalLong creationTime,
            OptionalLong expirationTime
    ) {
        if (ttl.isPresent()) {
            loader = loader.ttl(ttl.getAsLong(), TimeUnit.MILLISECONDS);
        }
        if (creationTime.isPresent()) {
            loader = loader.creationTime(creationTime.getAsLong());
        }
        if (expirationTime.isPresent()) {
            loader = loader.expirationTime(expirationTime.getAsLong());
        }
        return loader;
    }

    @SuppressWarnings("rawtypes")
    private MultiKeyCommandLoader applyCommonTiming(
            MultiKeyCommandLoader loader,
            OptionalLong ttl,
            OptionalLong creationTime,
            OptionalLong expirationTime
    ) {
        if (ttl.isPresent()) {
            loader = loader.ttl(ttl.getAsLong(), TimeUnit.MILLISECONDS);
        }
        if (creationTime.isPresent()) {
            loader = loader.creationTime(creationTime.getAsLong());
        }
        if (expirationTime.isPresent()) {
            loader = loader.expirationTime(expirationTime.getAsLong());
        }
        return loader;
    }

    private PlacementResult<Object> buildResult(
            CompletableFuture<Object> future,
            OptionalLong requestTtl,
            String correlationId,
            String label,
            Map<String, Object> appliedProps
    ) {
        var builder = PlacementResult.<Object>builder()
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .label(label)
                .properties(appliedProps);

        if (requestTtl.isPresent()) {
            long timeout = requestTtl.getAsLong();
            try {
                Object value = future.get(timeout, TimeUnit.MILLISECONDS);
                builder.status(PlacementStatus.COMPLETED).result(value);
            } catch (TimeoutException e) {
                builder.status(PlacementStatus.TIMEOUT_WAITING_FOR_COMPLETION)
                        .errorCode("TIMEOUT")
                        .errorMessage("Command is still running after " + timeout + " ms");
            } catch (ExecutionException e) {
                applyError(builder, e.getCause() == null ? e : e.getCause(), PlacementStatus.FAILED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                applyError(builder, e, PlacementStatus.FAILED);
            }
            return builder.build();
        }

        if (!future.isDone()) {
            return builder.status(PlacementStatus.IN_PROGRESS).build();
        }

        if (future.isCompletedExceptionally()) {
            Throwable cause = extractCompletedException(future);
            applyError(builder, cause, PlacementStatus.FAILED);
            return builder.build();
        }

        Object value = future.getNow(null);
        return builder.status(PlacementStatus.COMPLETED).result(value).build();
    }

    private Throwable extractCompletedException(CompletableFuture<?> future) {
        Throwable throwable = future.handle((result, error) -> error).getNow(null);
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        if (throwable instanceof ExecutionException && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable == null ? new IllegalStateException("Command failed") : throwable;
    }

    private void applyError(PlacementResult.Builder<?> builder, Throwable cause, PlacementStatus status) {
        builder.status(status)
                .errorCode(cause.getClass().getSimpleName())
                .exceptionClass(cause.getClass().getName())
                .errorMessage(Optional.ofNullable(cause.getMessage()).orElse("Unexpected error"));
    }

    private Conveyor<?, ?, ?> resolveConveyor(String name) {
        try {
            return Conveyor.byName(name);
        } catch (Exception e) {
            throw new ConveyorNotFoundException(name);
        }
    }

    private Class<?> resolveKeyType(Conveyor<?, ?, ?> conveyor) {
        try {
            var meta = conveyor.getMetaInfo();
            if (meta != null && meta.getKeyType() != null) {
                return meta.getKeyType();
            }
        } catch (Exception ignored) {
            // Fallback to String key when meta info is not available.
        }
        return String.class;
    }

    private OptionalLong parseAndRemoveDuration(String key, Map<String, String> params) {
        if (!params.containsKey(key)) {
            return OptionalLong.empty();
        }
        return RequestParsing.parseDurationMillis(params.remove(key));
    }

    private OptionalLong parseAndRemoveInstant(String key, Map<String, String> params) {
        if (!params.containsKey(key)) {
            return OptionalLong.empty();
        }
        return RequestParsing.parseEpochMillis(params.remove(key));
    }

    @SuppressWarnings("unchecked")
    private Object convertSimple(String raw, Class<?> targetType) {
        if (raw == null || targetType == null) {
            return raw;
        }
        if (targetType.equals(String.class)) {
            return raw;
        }
        try {
            if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return Long.parseLong(raw);
            }
            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return Integer.parseInt(raw);
            }
            if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return Double.parseDouble(raw);
            }
            if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return Float.parseFloat(raw);
            }
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<Enum>) targetType.asSubclass(Enum.class), raw);
            }
            return objectMapper.convertValue(raw, targetType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Cannot convert '" + raw + "' to " + targetType.getSimpleName(), ex);
        }
    }

    private String readBodyMessage(byte[] body) {
        String message = body == null ? "" : new String(body, StandardCharsets.UTF_8).trim();
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Request body message is required for completeExceptionally");
        }
        return message;
    }

    private enum CommandOperation {
        CANCEL("cancel", true, false, false),
        COMPLETE_EXCEPTIONALLY("completeExceptionally", true, false, false),
        ADD_PROPERTIES("addProperties", true, false, false),
        TIMEOUT("timeout", true, false, false),
        RESCHEDULE("reschedule", true, false, true),
        CREATE("create", false, false, false),
        PEEK("peek", true, true, false),
        PEEK_ID("peekId", true, true, false);

        private final String apiName;
        private final boolean foreachSupported;
        private final boolean requiresRequestTtl;
        private final boolean requiresRescheduleTiming;

        CommandOperation(String apiName, boolean foreachSupported, boolean requiresRequestTtl, boolean requiresRescheduleTiming) {
            this.apiName = apiName;
            this.foreachSupported = foreachSupported;
            this.requiresRequestTtl = requiresRequestTtl;
            this.requiresRescheduleTiming = requiresRescheduleTiming;
        }

        static CommandOperation fromString(String raw) {
            if (!StringUtils.hasText(raw)) {
                throw new IllegalArgumentException("command is required");
            }
            String normalized = raw.trim().replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "cancel" -> CANCEL;
                case "completeexceptionally" -> COMPLETE_EXCEPTIONALLY;
                case "addproperties" -> ADD_PROPERTIES;
                case "timeout" -> TIMEOUT;
                case "reschedule" -> RESCHEDULE;
                case "create" -> CREATE;
                case "peek" -> PEEK;
                case "peekid" -> PEEK_ID;
                default -> throw new IllegalArgumentException("Unsupported command: " + raw);
            };
        }
    }
}
