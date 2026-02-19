package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ConveyorInitiatingService;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.service.config.ConveyorServiceProperties;
import com.aegisql.conveyor.service.error.ConveyorNotFoundException;
import com.aegisql.conveyor.service.error.FeatureDisabledException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class DashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardService.class);
    private final ObjectMapper objectMapper;
    private final ConveyorWatchService conveyorWatchService;
    private final Path uploadDir;
    private final String configuredUploadDir;
    private final boolean uploadEnabled;
    private final List<String> loaderErrors = new ArrayList<>();
    private final Set<String> uploadedConveyorNames = Collections.synchronizedSet(new LinkedHashSet<>());
    private URLClassLoader uploadsClassLoader;

    public DashboardService(
            ObjectMapper objectMapper,
            ConveyorServiceProperties properties,
            ConveyorWatchService conveyorWatchService
    ) {
        this.objectMapper = objectMapper;
        this.conveyorWatchService = conveyorWatchService;
        Path configuredPath = properties.getUploadDir();
        this.configuredUploadDir = configuredPath == null ? "" : configuredPath.toString();
        this.uploadEnabled = properties.isUploadEnable();
        this.uploadDir = configuredPath == null
                ? Path.of(".").toAbsolutePath().normalize()
                : configuredPath.toAbsolutePath().normalize();
        try {
            resolveUploadDir();
            refreshUploadsClassLoader();
            try {
                loadUploadedConveyors();
            } catch (RuntimeException e) {
                // loadUploadedConveyors already records detailed loader errors.
            }
            this.conveyorWatchService.ensureKnownConveyorsHooked();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize upload directory: " + this.uploadDir, e);
        }
    }

    public Map<String, Map<String, ?>> conveyorTree() {
        Set<String> names = new LinkedHashSet<>();
        try {
            names.addAll(Conveyor.getKnownConveyorNames());
        } catch (Exception e) {
            LOG.warn("Failed reading known conveyor names", e);
        }
        try {
            names.addAll(Conveyor.getRegisteredConveyorNames());
        } catch (Exception e) {
            LOG.warn("Failed reading registered conveyor names", e);
        }
        synchronized (uploadedConveyorNames) {
            names.addAll(uploadedConveyorNames);
        }
        if (names.isEmpty()) {
            return Map.of();
        }
        List<String> sorted = new ArrayList<>(names);
        Collections.sort(sorted);
        Map<String, String> parentByChild = new HashMap<>();
        for (String name : sorted) {
            try {
                Conveyor<?, ?, ?> conveyor = Conveyor.byName(name);
                Conveyor<?, ?, ?> enclosing = conveyor.getEnclosingConveyor();
                if (enclosing == null) {
                    continue;
                }
                String parentName = enclosing.getName();
                if (parentName != null && names.contains(parentName) && !name.equals(parentName)) {
                    parentByChild.put(name, parentName);
                }
            } catch (Exception ignored) { }
        }
        Map<String, List<String>> childrenByParent = new HashMap<>();
        for (Map.Entry<String, String> link : parentByChild.entrySet()) {
            childrenByParent.computeIfAbsent(link.getValue(), k -> new ArrayList<>()).add(link.getKey());
        }
        childrenByParent.values().forEach(Collections::sort);

        List<String> roots = sorted.stream().filter(name -> !parentByChild.containsKey(name)).toList();
        LinkedHashMap<String, Map<String, ?>> tree = new LinkedHashMap<>();
        HashSet<String> visited = new HashSet<>();
        for (String root : roots) {
            tree.put(root, buildTree(root, childrenByParent, visited, new HashSet<>()));
        }
        for (String name : sorted) {
            if (!visited.contains(name)) {
                tree.put(name, buildTree(name, childrenByParent, visited, new HashSet<>()));
            }
        }
        return Collections.unmodifiableMap(tree);
    }

    public Map<String, Object> inspect(String name) {
        Conveyor<?, ?, ?> conveyor = resolve(name);
        String enclosingConveyor = getEnclosingConveyorName(conveyor);
        Map<String, Object> payload = new HashMap<>(16);
        payload.put("name", conveyor.getName());
        payload.put("running", conveyor.isRunning());
        payload.put("mbeanInterface", conveyor.mBeanInterface() == null ? null : conveyor.mBeanInterface().getName());
        MetaInfoSnapshot metaInfo = readMetaInfo(conveyor);
        payload.put("metaInfoAvailable", metaInfo.available);
        payload.put("metaInfoError", metaInfo.error);
        payload.put("metaInfo", metaInfo.details);
        payload.put("enclosingConveyor", enclosingConveyor);
        payload.put("topLevel", enclosingConveyor == null);
        payload.put("generatedAt", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC)));
        payload.put("uploadDirectory", configuredUploadDir);
        payload.put("loaderErrors", getLoaderErrors());
        payload.put("attributes", List.of());
        payload.put("writableParameters", List.of());
        payload.put("operations", List.of());

        if (conveyor.mBeanInterface() == null) {
            return payload;
        }

        Object proxy = conveyor.getMBeanInstance(conveyor.getName());
        Class<?> mbeanType = conveyor.mBeanInterface();
        Map<String, Method> setters = getSetterMethods(mbeanType);
        Map<String, Object> attributeValues = new HashMap<>();
        List<Map<String, Object>> attributes = new ArrayList<>();
        List<Map<String, Object>> writable = new ArrayList<>();
        List<Map<String, Object>> operations = new ArrayList<>();

        for (Method method : mbeanType.getMethods()) {
            if (isGetter(method)) {
                String attrName = attributeName(method);
                Map<String, Object> attr = new HashMap<>(8);
                attr.put("name", attrName);
                attr.put("type", method.getReturnType().getSimpleName());
                attr.put("writable", setters.containsKey(attrName));
                Object value = tryInvoke(proxy, method);
                attr.put("value", value);
                attributeValues.put(attrName, value);
                attributes.add(attr);
            } else if (isSetter(method)) {
                String attrName = attributeNameFromSetter(method.getName());
                Map<String, Object> row = new HashMap<>(8);
                row.put("name", attrName);
                row.put("type", method.getParameterTypes()[0].getSimpleName());
                row.put("value", null);
                writable.add(row);
            } else if (isOperation(method)) {
                Map<String, Object> op = new HashMap<>(8);
                op.put("name", method.getName());
                op.put("argCount", method.getParameterCount());
                op.put("argType", method.getParameterCount() == 0 ? "" : method.getParameterTypes()[0].getSimpleName());
                op.put("returns", method.getReturnType().getSimpleName());
                op.put("stopOperation", isStopOperationName(method.getName()));
                operations.add(op);
            }
        }

        for (Map<String, Object> row : writable) {
            String attrName = String.valueOf(row.get("name"));
            row.put("value", attributeValues.get(attrName));
        }

        attributes.sort(Comparator.comparing(row -> String.valueOf(row.get("name"))));
        writable.sort(Comparator.comparing(row -> String.valueOf(row.get("name"))));
        operations.sort(
                Comparator.comparing((Map<String, Object> row) -> Boolean.TRUE.equals(row.get("stopOperation")))
                        .thenComparing(row -> toInt(row.get("argCount")) == 0)
                        .thenComparing(row -> String.valueOf(row.get("name")))
        );
        payload.put("attributes", attributes);
        payload.put("writableParameters", writable);
        payload.put("operations", operations);
        return payload;
    }

    private int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private boolean isStopOperationName(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return "stop".equals(normalized) || normalized.endsWith("stop");
    }

    public synchronized List<String> getLoaderErrors() {
        return List.copyOf(loaderErrors);
    }

    public synchronized List<String> drainLoaderErrors() {
        List<String> copy = List.copyOf(loaderErrors);
        loaderErrors.clear();
        return copy;
    }

    public boolean isUploadEnabled() {
        return uploadEnabled;
    }

    public void reload(String name) {
        Conveyor<?, ?, ?> conveyor = resolve(name);
        stopAndUnregister(conveyor);
        Conveyor.loadServices();
        try {
            refreshUploadsClassLoader();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot refresh upload directory: " + uploadDir, e);
        }
        clearLoaderErrors();
        loadUploadedConveyors();
        conveyorWatchService.ensureKnownConveyorsHooked();
    }

    public void delete(String name) {
        ensureUploadEnabled("Delete");
        Conveyor<?, ?, ?> conveyor = resolve(name);
        stopAndUnregister(conveyor);
        uploadedConveyorNames.remove(name);
    }

    public void upload(MultipartFile file) throws IOException {
        ensureUploadEnabled("Upload");
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new IllegalArgumentException("Uploaded file name is missing");
        }
        String safeName = Path.of(original).getFileName().toString();
        if (!safeName.endsWith(".jar")) {
            throw new IllegalArgumentException("Only .jar uploads are supported");
        }
        Path resolvedUploadDir = resolveUploadDir();
        Path target = resolvedUploadDir.resolve(safeName).normalize();
        if (!target.startsWith(resolvedUploadDir)) {
            throw new IllegalArgumentException("Invalid target path");
        }
        try (var input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        refreshUploadsClassLoader();
        clearLoaderErrors();
        loadUploadedConveyors();
        conveyorWatchService.ensureKnownConveyorsHooked();
    }

    private void ensureUploadEnabled(String operation) {
        if (!uploadEnabled) {
            throw new FeatureDisabledException(operation + " is disabled by the service admin");
        }
    }

    public void updateParameter(String name, String parameter, String value) {
        Conveyor<?, ?, ?> conveyor = resolve(name);
        if (conveyor.mBeanInterface() == null) {
            throw new IllegalArgumentException("No MBean interface for conveyor " + name);
        }
        Object proxy = conveyor.getMBeanInstance(conveyor.getName());
        Method setter = getSetterMethods(conveyor.mBeanInterface()).get(parameter);
        if (setter == null) {
            throw new IllegalArgumentException("Unknown writable parameter: " + parameter);
        }
        Object converted = convertArg(value, setter.getParameterTypes()[0]);
        try {
            setter.invoke(proxy, converted);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to update parameter " + parameter + " for " + name, e);
        }
    }

    public Object invokeMBean(String name, String method, Object argument) {
        Conveyor<?, ?, ?> conveyor = resolve(name);
        if (conveyor.mBeanInterface() == null) {
            throw new IllegalArgumentException("No MBean interface for conveyor " + name);
        }
        Object proxy = conveyor.getMBeanInstance(conveyor.getName());
        Method target = findOperation(conveyor.mBeanInterface(), method, argument != null);
        Object converted = target.getParameterCount() == 0 ? null : convertArg(argument, target.getParameterTypes()[0]);
        try {
            Object result = target.getParameterCount() == 0 ? target.invoke(proxy) : target.invoke(proxy, converted);
            return result == null ? "OK" : result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to invoke MBean method " + method + " on " + name, e);
        }
    }

    private Method findOperation(Class<?> type, String method, boolean withArg) {
        for (Method m : type.getMethods()) {
            if (!m.getName().equals(method)) {
                continue;
            }
            if (withArg && m.getParameterCount() == 1) {
                return m;
            }
            if (!withArg && m.getParameterCount() == 0) {
                return m;
            }
        }
        throw new IllegalArgumentException("Operation not found: " + method);
    }

    private void stopAndUnregister(Conveyor<?, ?, ?> conveyor) {
        try {
            conveyor.completeAndStop().get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) { }
        Conveyor.unRegister(conveyor.getName());
    }

    public boolean isTopLevelConveyor(String name) {
        Conveyor<?, ?, ?> conveyor = resolve(name);
        return getEnclosingConveyorName(conveyor) == null;
    }

    private Conveyor<?, ?, ?> resolve(String name) {
        try {
            return Conveyor.byName(name);
        } catch (Exception e) {
            throw new ConveyorNotFoundException(name);
        }
    }

    private MetaInfoSnapshot readMetaInfo(Conveyor<?, ?, ?> conveyor) {
        try {
            ConveyorMetaInfo<?, ?, ?> meta = conveyor.getMetaInfo();
            return new MetaInfoSnapshot(true, null, toMetaInfoDetails(meta));
        } catch (Exception e) {
            return new MetaInfoSnapshot(false, "Meta info is not available", Map.of());
        }
    }

    private Map<String, Object> toMetaInfoDetails(ConveyorMetaInfo<?, ?, ?> meta) {
        @SuppressWarnings("rawtypes")
        ConveyorMetaInfo rawMeta = (ConveyorMetaInfo) meta;
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("keyType", className(meta.getKeyType()));
        details.put("labelType", className(meta.getLabelType()));
        details.put("productType", className(meta.getProductType()));
        List<String> labels = meta.getLabels().stream().map(String::valueOf).sorted().toList();
        details.put("labels", labels);
        LinkedHashMap<String, List<String>> supportedByLabel = new LinkedHashMap<>();
        for (Object label : meta.getLabels()) {
            @SuppressWarnings("unchecked")
            Set<Class<?>> supported = rawMeta.getSupportedValueTypes(label);
            List<String> types = supported == null
                    ? List.of()
                    : supported.stream().map(this::className).sorted().toList();
            supportedByLabel.put(String.valueOf(label), types);
        }
        details.put("supportedValueTypes", supportedByLabel);
        return details;
    }

    private String className(Class<?> type) {
        return type == null ? null : type.getName();
    }

    private String getEnclosingConveyorName(Conveyor<?, ?, ?> conveyor) {
        try {
            Conveyor<?, ?, ?> enclosing = conveyor.getEnclosingConveyor();
            if (enclosing == null) {
                return null;
            }
            return enclosing.getName();
        } catch (Exception e) {
            return null;
        }
    }

    private synchronized void refreshUploadsClassLoader() throws IOException {
        if (uploadsClassLoader != null) {
            uploadsClassLoader.close();
        }
        List<URL> urls = new ArrayList<>();
        Path resolvedUploadDir = resolveUploadDir();
        try (Stream<Path> stream = Files.list(resolvedUploadDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            urls.add(path.toUri().toURL());
                        } catch (Exception e) {
                            throw new IllegalStateException("Invalid jar URL for " + path, e);
                        }
                    });
        }
        uploadsClassLoader = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }

    private Path resolveUploadDir() throws IOException {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        return uploadDir.toRealPath();
    }

    private void loadUploadedConveyors() {
        if (uploadsClassLoader == null) {
            return;
        }
        int providerCount = 0;
        List<String> candidateNames = new ArrayList<>();
        List<String> visibleNames = new ArrayList<>();
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(uploadsClassLoader);
        try {
            ServiceLoader<ConveyorInitiatingService> loader = ServiceLoader.load(ConveyorInitiatingService.class, uploadsClassLoader);
            try {
                var iterator = loader.iterator();
                while (iterator.hasNext()) {
                    providerCount++;
                    ConveyorInitiatingService service = iterator.next();
                    Conveyor<?, ?, ?> conveyor = service.getConveyor();
                    String name = conveyor == null ? null : conveyor.getName();
                    if (name == null || name.isBlank()) {
                        throw new IllegalStateException(
                                "ConveyorInitiatingService " + service.getClass().getName()
                                        + " returned conveyor with empty name");
                    }
                    candidateNames.add(name);
                    uploadedConveyorNames.add(name);
                    try {
                        Conveyor<?, ?, ?> existing = Conveyor.byName(name);
                        if (existing != conveyor) {
                            stopAndUnregister(existing);
                        }
                    } catch (Exception ignored) { }
                    ensureConveyorVisible(conveyor);
                    conveyorWatchService.ensureConveyorHooked(conveyor);
                    if (isConveyorVisible(name)) {
                        visibleNames.add(name);
                    }
                }
                if (providerCount == 0) {
                    String message = "No ConveyorInitiatingService providers discovered in uploaded jars";
                    recordLoaderError(message);
                    throw new IllegalStateException(message);
                }
                if (visibleNames.isEmpty()) {
                    String message = "Uploaded providers discovered (" + providerCount + "), but no conveyors became visible. "
                            + "Candidates: " + candidateNames;
                    recordLoaderError(message);
                    throw new IllegalStateException(message);
                }
                LOG.info("Uploaded conveyors now visible: {}", visibleNames);
            } catch (ServiceConfigurationError e) {
                String message = buildServiceLoaderErrorMessage(e);
                recordLoaderError(message);
                throw new IllegalStateException(message, e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    private void ensureConveyorVisible(Conveyor<?, ?, ?> conveyor) {
        String name = conveyor.getName();
        try {
            Conveyor<?, ?, ?> resolved = Conveyor.byName(name);
            if (resolved == conveyor) {
                LOG.info("Uploaded conveyor '{}' is available. Registered conveyors: {}",
                        name, Conveyor.getRegisteredConveyorNames());
                return;
            }
        } catch (Exception ignored) { }
        try {
            // For conveyors with snapshot-style MBeans, calling setName(name) triggers their own MBean registration flow.
            conveyor.setName(name);
            Conveyor.byName(name);
            LOG.info("Uploaded conveyor '{}' re-registered via setName. Registered conveyors: {}",
                    name, Conveyor.getRegisteredConveyorNames());
        } catch (Exception e) {
            String message = "Uploaded conveyor '" + name + "' is not visible through MBean registry. " +
                    "Details: " + rootCauseMessage(e);
            recordLoaderError(message);
            LOG.warn(message, e);
        }
    }

    private boolean isConveyorVisible(String name) {
        try {
            return Conveyor.byName(name) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, ?> buildTree(
            String node,
            Map<String, List<String>> childrenByParent,
            HashSet<String> visited,
            HashSet<String> path
    ) {
        if (!path.add(node)) {
            return Map.of();
        }
        visited.add(node);
        LinkedHashMap<String, Map<String, ?>> map = new LinkedHashMap<>();
        map.put("__meta__", treeNodeMeta(node));
        for (String child : childrenByParent.getOrDefault(node, List.of())) {
            map.put(child, buildTree(child, childrenByParent, visited, new HashSet<>(path)));
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<String, ?> treeNodeMeta(String nodeName) {
        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        boolean running = false;
        boolean suspended = false;
        String state = "STOPPED";
        try {
            Conveyor<?, ?, ?> conveyor = Conveyor.byName(nodeName);
            running = conveyor.isRunning();
            suspended = running && conveyor.isSuspended();
            if (running) {
                state = suspended ? "SUSPENDED" : "RUNNING";
            }
        } catch (Exception ignored) {
            // Keep default STOPPED state when conveyor cannot be resolved.
        }
        meta.put("running", running);
        meta.put("suspended", suspended);
        meta.put("state", state);
        return Collections.unmodifiableMap(meta);
    }

    private Object tryInvoke(Object proxy, Method getter) {
        try {
            Object value = getter.invoke(proxy);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "<error: " + e.getClass().getSimpleName() + ">";
        }
    }

    private boolean isGetter(Method method) {
        if (method.getParameterCount() != 0) return false;
        if (method.getName().equals("getClass")) return false;
        if (method.getName().equals("conveyor")) return false;
        return (method.getName().startsWith("get") && method.getName().length() > 3)
                || (method.getName().startsWith("is") && method.getName().length() > 2);
    }

    private boolean isSetter(Method method) {
        return method.getName().startsWith("set")
                && method.getName().length() > 3
                && method.getParameterCount() == 1;
    }

    private boolean isOperation(Method method) {
        if (isGetter(method) || isSetter(method)) return false;
        if (method.getName().equals("conveyor")) return false;
        if (method.getDeclaringClass().equals(Object.class)) return false;
        return method.getParameterCount() <= 1;
    }

    private String attributeName(Method method) {
        if (method.getName().startsWith("get")) {
            return decapitalize(method.getName().substring(3));
        }
        if (method.getName().startsWith("is")) {
            return decapitalize(method.getName().substring(2));
        }
        return method.getName();
    }

    private String attributeNameFromSetter(String name) {
        return decapitalize(name.substring(3));
    }

    private String decapitalize(String value) {
        if (value.isEmpty()) return value;
        if (value.length() == 1) return value.toLowerCase();
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private Map<String, Method> getSetterMethods(Class<?> mbeanType) {
        Map<String, Method> setters = new HashMap<>();
        for (Method method : mbeanType.getMethods()) {
            if (isSetter(method)) {
                setters.put(attributeNameFromSetter(method.getName()), method);
            }
        }
        return setters;
    }

    private Object convertArg(Object source, Class<?> targetType) {
        if (source == null) return null;
        if (targetType.isAssignableFrom(source.getClass())) return source;

        String textValue = String.valueOf(source);
        if (targetType.equals(String.class)) return textValue;
        if (targetType.equals(int.class) || targetType.equals(Integer.class)) return Integer.parseInt(textValue);
        if (targetType.equals(long.class) || targetType.equals(Long.class)) return Long.parseLong(textValue);
        if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) return Boolean.parseBoolean(textValue);
        if (targetType.equals(double.class) || targetType.equals(Double.class)) return Double.parseDouble(textValue);
        if (targetType.equals(float.class) || targetType.equals(Float.class)) return Float.parseFloat(textValue);
        if (targetType.equals(short.class) || targetType.equals(Short.class)) return Short.parseShort(textValue);
        if (targetType.equals(byte.class) || targetType.equals(Byte.class)) return Byte.parseByte(textValue);
        if (targetType.equals(char.class) || targetType.equals(Character.class)) {
            if (textValue.length() != 1) throw new IllegalArgumentException("Single character expected");
            return textValue.charAt(0);
        }
        if (targetType.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = Enum.valueOf((Class<Enum>) targetType.asSubclass(Enum.class), textValue);
            return enumValue;
        }
        return objectMapper.convertValue(source, targetType);
    }

    private String buildServiceLoaderErrorMessage(ServiceConfigurationError e) {
        String detail = rootCauseMessage(e);
        return "Cannot load uploaded ConveyorInitiatingService provider. " +
                "Likely causes: missing no-arg constructor, missing dependency jar, or class initialization failure. " +
                "Details: " + detail;
    }

    private String rootCauseMessage(Throwable t) {
        Throwable cursor = t;
        int depth = 0;
        while (cursor.getCause() != null && cursor.getCause() != cursor && depth < 10) {
            cursor = cursor.getCause();
            depth++;
        }
        String message = cursor.getMessage();
        if (message == null || message.isBlank()) {
            return cursor.getClass().getSimpleName();
        }
        return cursor.getClass().getSimpleName() + ": " + message;
    }

    private synchronized void recordLoaderError(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC));
        loaderErrors.add(timestamp + " - " + message);
    }

    private synchronized void clearLoaderErrors() {
        loaderErrors.clear();
    }

    private record MetaInfoSnapshot(
            boolean available,
            String error,
            Map<String, Object> details
    ) { }
}
