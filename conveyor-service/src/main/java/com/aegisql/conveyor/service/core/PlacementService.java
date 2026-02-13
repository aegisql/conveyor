package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.service.api.PlacementResult;
import com.aegisql.conveyor.service.api.PlacementStatus;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.aegisql.conveyor.service.error.UnsupportedMappingException;
import com.aegisql.conveyor.service.util.RequestParsing;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class PlacementService {

    private final ObjectMapper objectMapper;

    public PlacementService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PlacementResult<Boolean> placePart(
            String contentType,
            String conveyorName,
            String id,
            String label,
            byte[] body,
            Map<String, String> requestParams
    ) {
        return placePart(contentType, conveyorName, id, label, false, body, requestParams);
    }

    public PlacementResult<Boolean> placePartForEach(
            String contentType,
            String conveyorName,
            String label,
            byte[] body,
            Map<String, String> requestParams
    ) {
        return placePart(contentType, conveyorName, null, label, true, body, requestParams);
    }

    private PlacementResult<Boolean> placePart(
            String contentType,
            String conveyorName,
            String id,
            String label,
            boolean forEach,
            byte[] body,
            Map<String, String> requestParams
    ) {
        Conveyor<?, ?, ?> conveyor = resolveConveyor(conveyorName);
        var metaInfo = resolveMetaInfo(conveyor);

        Object typedLabel = convertSimple(label, metaInfo.getLabelType());

        @SuppressWarnings("rawtypes")
        PartLoader loader = conveyor.part();
        if (forEach) {
            loader = loader.foreach();
        } else {
            if (!StringUtils.hasText(id)) {
                throw new IllegalArgumentException("id is required when foreach is false");
            }
            Object typedKey = convertSimple(id, metaInfo.getKeyType());
            loader = loader.id(typedKey);
        }
        loader = loader.label(typedLabel);

        Map<String, String> remaining = new HashMap<>(requestParams);

        OptionalLong ttl = parseAndRemoveDuration("ttl", remaining);
        OptionalLong creationTime = parseAndRemoveInstant("creationTime", remaining);
        OptionalLong expirationTime = parseAndRemoveInstant("expirationTime", remaining);
        OptionalLong priority = parseAndRemoveLong("priority", remaining);
        OptionalLong requestTtl = parseAndRemoveDuration("requestTTL", remaining);

        if (ttl.isPresent()) {
            loader = loader.ttl(ttl.getAsLong(), TimeUnit.MILLISECONDS);
        }
        if (creationTime.isPresent()) {
            loader = loader.creationTime(creationTime.getAsLong());
        }
        if (expirationTime.isPresent()) {
            loader = loader.expirationTime(expirationTime.getAsLong());
        }
        if (priority.isPresent()) {
            loader = loader.priority(priority.getAsLong());
        }

        Map<String, Object> appliedProps = new HashMap<>();
        appliedProps.put("conveyor", conveyorName);
        appliedProps.put("correlationId", id);
        appliedProps.put("label", label);
        appliedProps.put("foreach", forEach);
        loader = loader.addProperties(new HashMap<>(remaining));
        appliedProps.putAll(remaining);

        Class<?> targetType = resolveTargetType(metaInfo, contentType, typedLabel);
        Object convertedValue = convertBody(body, targetType, contentType);
        loader = loader.value(convertedValue);

        CompletableFuture<Boolean> future = loader.place();

        return buildResult(future, requestTtl, id, label, appliedProps);
    }

    private Conveyor<?, ?, ?> resolveConveyor(String name) {
        try {
            return Conveyor.byName(name);
        } catch (Exception e) {
            throw new ConveyorNotFoundException(name);
        }
    }

    private ConveyorMetaInfo<?, ?, ?> resolveMetaInfo(Conveyor<?, ?, ?> conveyor) {
        try {
            return conveyor.getMetaInfo();
        } catch (Exception e) {
            throw new UnsupportedMappingException("Conveyor does not expose meta info: " + conveyor.getName());
        }
    }

    private OptionalLong parseAndRemoveDuration(String key, Map<String, String> params) {
        if (!params.containsKey(key)) return OptionalLong.empty();
        var value = params.remove(key);
        return RequestParsing.parseDurationMillis(value);
    }

    private OptionalLong parseAndRemoveInstant(String key, Map<String, String> params) {
        if (!params.containsKey(key)) return OptionalLong.empty();
        var value = params.remove(key);
        return RequestParsing.parseEpochMillis(value);
    }

    private OptionalLong parseAndRemoveLong(String key, Map<String, String> params) {
        if (!params.containsKey(key)) return OptionalLong.empty();
        var value = params.remove(key);
        try {
            return OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number for " + key + ": " + value, ex);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Class<?> resolveTargetType(ConveyorMetaInfo<?, ?, ?> metaInfo, String contentType, Object typedLabel) {
        ConveyorMetaInfo rawMeta = (ConveyorMetaInfo) metaInfo;
        Set<Class<?>> supported = rawMeta.getSupportedValueTypes(typedLabel);
        if (supported == null || supported.isEmpty()) {
            throw new UnsupportedMappingException("No value types registered for label " + typedLabel);
        }
        MediaType mt = MediaType.parseMediaType(contentType);
        if (mt.isCompatibleWith(MediaType.APPLICATION_OCTET_STREAM)) {
            if (supported.contains(byte[].class)) return byte[].class;
        } else if (mt.isCompatibleWith(MediaType.TEXT_PLAIN) || MimeTypeUtils.TEXT_PLAIN_VALUE.equals(mt.getType())) {
            if (supported.contains(String.class)) return String.class;
        } else if (mt.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            Optional<Class<?>> jsonType = supported.stream()
                    .filter(type -> type != byte[].class && type != String.class)
                    .findFirst();
            if (jsonType.isPresent()) return jsonType.get();
            if (supported.contains(String.class)) return String.class;
        }
        throw new UnsupportedMappingException("Unsupported mapping for content type " + contentType + " and label " + typedLabel);
    }

    private Object convertBody(byte[] body, Class<?> targetType, String contentType) {
        if (targetType == byte[].class) {
            return body;
        }
        if (targetType == String.class) {
            return new String(body, StandardCharsets.UTF_8);
        }
        try {
            return objectMapper.readValue(body, targetType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert body for content type " + contentType + " to " + targetType.getSimpleName(), e);
        }
    }

    private PlacementResult<Boolean> buildResult(
            CompletableFuture<Boolean> future,
            OptionalLong requestTtl,
            String correlationId,
            String label,
            Map<String, Object> appliedProps
    ) {
        var builder = PlacementResult.<Boolean>builder()
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .label(label)
                .properties(appliedProps);

        if (requestTtl.isPresent()) {
            long timeout = requestTtl.getAsLong();
            try {
                Boolean value = future.get(timeout, TimeUnit.MILLISECONDS);
                builder.status(PlacementStatus.COMPLETED).result(value);
            } catch (TimeoutException e) {
                builder.status(PlacementStatus.TIMEOUT_WAITING_FOR_COMPLETION)
                        .errorCode("TIMEOUT")
                        .errorMessage("Placement is still running after " + timeout + " ms");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                applyError(builder, cause, PlacementStatus.FAILED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                applyError(builder, e, PlacementStatus.FAILED);
            }
            return builder.build();
        }

        if (!future.isDone()) {
            return builder.status(PlacementStatus.IN_PROGRESS).build();
        }

        try {
            Boolean value = future.getNow(Boolean.FALSE);
            return builder.status(PlacementStatus.COMPLETED).result(value).build();
        } catch (CompletionException e) {
            applyError(builder, e.getCause() == null ? e : e.getCause(), PlacementStatus.FAILED);
            return builder.build();
        }
    }

    private void applyError(PlacementResult.Builder<?> builder, Throwable cause, PlacementStatus status) {
        builder.status(status)
                .errorCode(cause.getClass().getSimpleName())
                .exceptionClass(cause.getClass().getName())
                .errorMessage(Optional.ofNullable(cause.getMessage()).orElse("Unexpected error"));
    }

    private Object convertSimple(String raw, Class<?> targetType) {
        if (raw == null || targetType == null) return raw;
        if (targetType.equals(String.class)) return raw;
        try {
            if (targetType.equals(Long.class) || targetType.equals(long.class)) return Long.parseLong(raw);
            if (targetType.equals(Integer.class) || targetType.equals(int.class)) return Integer.parseInt(raw);
            if (targetType.equals(Double.class) || targetType.equals(double.class)) return Double.parseDouble(raw);
            if (targetType.equals(Float.class) || targetType.equals(float.class)) return Float.parseFloat(raw);
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<Enum>) targetType.asSubclass(Enum.class), raw);
            }
            return objectMapper.convertValue(raw, targetType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Cannot convert '" + raw + "' to " + targetType.getSimpleName(), ex);
        }
    }
}
