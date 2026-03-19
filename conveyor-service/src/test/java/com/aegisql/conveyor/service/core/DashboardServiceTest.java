package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.service.config.ConveyorServiceProperties;
import com.aegisql.conveyor.service.error.FeatureDisabledException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void constructorRejectsInvalidConfiguredStopTimeout() {
        ConveyorServiceProperties properties = new ConveyorServiceProperties();
        properties.setUploadDir(tempDir.resolve("upload-invalid-timeout"));
        properties.setUploadEnable(false);

        assertThatThrownBy(() -> new DashboardService(
                new ObjectMapper(),
                properties,
                mock(ConveyorWatchService.class),
                "bad-timeout"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default-admin-stop-timeout");
    }

    @Test
    void constructorRecordsLoaderErrorAndContinuesWhenUploadedProviderHasMissingDependency() throws Exception {
        Path uploadDir = tempDir.resolve("upload-broken-provider");
        Files.createDirectories(uploadDir);
        createBrokenInitiatingServiceJar(uploadDir.resolve("broken-provider.jar"));

        DashboardService service = newService(true, uploadDir, "1 MINUTES");

        assertThat(service.getLoaderErrors())
                .anyMatch(message ->
                        message.contains("Cannot load uploaded ConveyorInitiatingService provider")
                                && message.contains("ClassNotFoundException")
                                && message.contains("broken.MissingBase")
                );
        assertThat(service.conveyorTree()).isNotNull();
    }

    @Test
    void deleteAndUploadAreBlockedWhenUploadFeatureDisabled() {
        DashboardService service = newService(false, tempDir.resolve("upload-disabled"), "1 MINUTES");

        assertThatThrownBy(() -> service.delete("any", null))
                .isInstanceOf(FeatureDisabledException.class)
                .hasMessageContaining("Delete is disabled");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.jar",
                "application/java-archive",
                new byte[]{1, 2, 3}
        );
        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(FeatureDisabledException.class)
                .hasMessageContaining("Upload is disabled");
    }

    @Test
    void conveyorTreeShowsHierarchyAndStateMeta() {
        DashboardService service = newService(false, tempDir.resolve("upload-tree"), "1 MINUTES");
        String parentName = "tree-parent-" + System.nanoTime();
        String childName = "tree-child-" + System.nanoTime();

        Conveyor<Object, Object, Object> parent = registerConveyor(parentName, null, true, false);
        Conveyor<Object, Object, Object> child = registerConveyor(childName, parent, false, false);
        try {
            Map<String, Map<String, ?>> tree = service.conveyorTree();

            assertThat(tree).containsKey(parentName);
            @SuppressWarnings("unchecked")
            Map<String, Object> parentNode = (Map<String, Object>) tree.get(parentName);
            assertThat(parentNode).containsKey("__meta__");
            assertThat(parentNode).containsKey(childName);

            @SuppressWarnings("unchecked")
            Map<String, Object> parentMeta = (Map<String, Object>) parentNode.get("__meta__");
            assertThat(parentMeta).containsEntry("running", true);
            assertThat(parentMeta).containsEntry("state", "RUNNING");

            @SuppressWarnings("unchecked")
            Map<String, Object> childNode = (Map<String, Object>) parentNode.get(childName);
            @SuppressWarnings("unchecked")
            Map<String, Object> childMeta = (Map<String, Object>) childNode.get("__meta__");
            assertThat(childMeta).containsEntry("state", "STOPPED");
        } finally {
            safeUnregister(childName);
            safeUnregister(parentName);
        }
    }

    @Test
    void inspectWithMBeanBuildsAttributeAndOperationLists() {
        DashboardService service = newService(false, tempDir.resolve("upload-inspect"), "1 MINUTES");
        String conveyorName = "inspect-conveyor-" + System.nanoTime();

        TestMBeanImpl proxy = new TestMBeanImpl();
        proxy.setLimit(7);

        Conveyor<Object, Object, Object> conveyor = registerConveyor(conveyorName, null, true, false);
        try {
            when(conveyor.mBeanInterface()).thenReturn((Class) TestMBean.class);
            when(conveyor.getMBeanInstance(conveyorName)).thenReturn(proxy);

            Map<String, Object> inspected = service.inspect(conveyorName);

            assertThat(inspected).containsEntry("name", conveyorName);
            assertThat(inspected).containsEntry("topLevel", true);
            assertThat(inspected).containsEntry("metaInfoAvailable", true);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attributes = (List<Map<String, Object>>) inspected.get("attributes");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> writable = (List<Map<String, Object>>) inspected.get("writableParameters");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> operations = (List<Map<String, Object>>) inspected.get("operations");

            assertThat(attributes).anyMatch(row ->
                    "limit".equals(row.get("name")) && Integer.valueOf(7).equals(row.get("value"))
            );
            assertThat(writable).anyMatch(row ->
                    "limit".equals(row.get("name")) && Integer.valueOf(7).equals(row.get("value"))
            );
            assertThat(operations).anyMatch(row -> "ping".equals(row.get("name")));
            assertThat(operations).anyMatch(row -> "echo".equals(row.get("name")));
        } finally {
            safeUnregister(conveyorName);
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void inspectWithoutMBeanAndWithMetaInfoErrorReturnsSafeDefaults() {
        DashboardService service = newService(false, tempDir.resolve("upload-inspect-no-mbean"), "1 MINUTES");
        String conveyorName = "inspect-no-mbean-" + System.nanoTime();

        Conveyor<Object, Object, Object> conveyor = mock(Conveyor.class);
        when(conveyor.getName()).thenReturn(conveyorName);
        when(conveyor.getEnclosingConveyor()).thenReturn(null);
        when(conveyor.isRunning()).thenReturn(true);
        when(conveyor.mBeanInterface()).thenReturn(null);
        when(conveyor.getMetaInfo()).thenThrow(new IllegalStateException("meta unavailable"));
        Conveyor.register(conveyor, conveyor);
        try {
            Map<String, Object> inspected = service.inspect(conveyorName);

            assertThat(inspected).containsEntry("name", conveyorName);
            assertThat(inspected).containsEntry("metaInfoAvailable", false);
            assertThat(inspected).containsEntry("metaInfoError", "Meta info is not available");
            assertThat((List<?>) inspected.get("attributes")).isEmpty();
            assertThat((List<?>) inspected.get("operations")).isEmpty();
            assertThat((List<?>) inspected.get("writableParameters")).isEmpty();
        } finally {
            safeUnregister(conveyorName);
        }
    }

    @Test
    void inspectHandlesGetterInvocationErrorsAndMarksStopOperations() {
        DashboardService service = newService(false, tempDir.resolve("upload-inspect-throwing"), "1 MINUTES");
        String conveyorName = "inspect-throwing-" + System.nanoTime();

        ThrowingMBeanImpl proxy = new ThrowingMBeanImpl();
        Conveyor<Object, Object, Object> conveyor = registerConveyor(conveyorName, null, true, false);
        try {
            when(conveyor.mBeanInterface()).thenReturn((Class) ThrowingMBean.class);
            when(conveyor.getMBeanInstance(conveyorName)).thenReturn(proxy);

            Map<String, Object> inspected = service.inspect(conveyorName);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attributes = (List<Map<String, Object>>) inspected.get("attributes");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> operations = (List<Map<String, Object>>) inspected.get("operations");

            assertThat(attributes).anyMatch(row ->
                    "broken".equals(row.get("name"))
                            && String.valueOf(row.get("value")).startsWith("<error:")
            );
            assertThat(operations).anyMatch(row ->
                    "hardStop".equals(row.get("name")) && Boolean.TRUE.equals(row.get("stopOperation"))
            );
        } finally {
            safeUnregister(conveyorName);
        }
    }

    @Test
    void updateParameterAndInvokeMBeanWorkForRegisteredConveyor() {
        DashboardService service = newService(false, tempDir.resolve("upload-mbean"), "1 MINUTES");
        String conveyorName = "mbean-conveyor-" + System.nanoTime();

        TestMBeanImpl proxy = new TestMBeanImpl();
        Conveyor<Object, Object, Object> conveyor = registerConveyor(conveyorName, null, true, false);
        try {
            when(conveyor.mBeanInterface()).thenReturn((Class) TestMBean.class);
            when(conveyor.getMBeanInstance(conveyorName)).thenReturn(proxy);

            service.updateParameter(conveyorName, "limit", "15");
            assertThat(proxy.getLimit()).isEqualTo(15);

            Object ping = service.invokeMBean(conveyorName, "ping", null);
            Object echo = service.invokeMBean(conveyorName, "echo", "abc");
            assertThat(ping).isEqualTo("pong");
            assertThat(echo).isEqualTo("echo:abc");
        } finally {
            safeUnregister(conveyorName);
        }
    }

    @Test
    void reloadUsesConfiguredDefaultTimeoutWhenNoInputProvided() {
        DashboardService service = newService(false, tempDir.resolve("upload-reload-default"), "2 SECONDS");
        String conveyorName = "reload-default-" + System.nanoTime();

        Conveyor<Object, Object, Object> conveyor = registerConveyor(conveyorName, null, false, false);
        doNothing().when(conveyor).completeThenForceStop(anyLong(), eq(TimeUnit.MILLISECONDS));
        try {
            service.reload(conveyorName, null);
            verify(conveyor).completeThenForceStop(2000L, TimeUnit.MILLISECONDS);
        } finally {
            safeUnregister(conveyorName);
        }
    }

    @Test
    void reloadForcesStopWhenConveyorStillRunningAfterTimeout() {
        DashboardService service = newService(false, tempDir.resolve("upload-reload-force"), "1 SECONDS");
        String conveyorName = "reload-force-" + System.nanoTime();

        Conveyor<Object, Object, Object> conveyor = registerConveyor(conveyorName, null, true, false);
        doNothing().when(conveyor).completeThenForceStop(anyLong(), eq(TimeUnit.MILLISECONDS));
        try {
            service.reload(conveyorName, "1 MILLISECONDS");
            verify(conveyor).stop();
        } finally {
            safeUnregister(conveyorName);
        }
    }

    @Test
    void reloadRejectsInvalidStopTimeoutAndDeleteRejectsChildConveyor() {
        DashboardService service = newService(true, tempDir.resolve("upload-invalid-stop"), "1 SECONDS");
        String parentName = "reload-parent-" + System.nanoTime();
        String childName = "reload-child-" + System.nanoTime();

        Conveyor<Object, Object, Object> parent = registerConveyor(parentName, null, false, false);
        Conveyor<Object, Object, Object> child = registerConveyor(childName, parent, false, false);
        try {
            assertThatThrownBy(() -> service.reload(parentName, "bad-timeout"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid stopTimeout");

            assertThatThrownBy(() -> service.delete(childName, "1 SECONDS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("top-level conveyors");
        } finally {
            safeUnregister(childName);
            safeUnregister(parentName);
        }
    }

    @Test
    void uploadValidationRejectsInvalidInput() {
        DashboardService service = newService(true, tempDir.resolve("upload-validation"), "1 SECONDS");

        MockMultipartFile empty = new MockMultipartFile(
                "file",
                "demo.jar",
                "application/java-archive",
                new byte[0]
        );
        assertThatThrownBy(() -> service.upload(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");

        MockMultipartFile missingName = new MockMultipartFile(
                "file",
                "",
                "application/java-archive",
                new byte[]{1}
        );
        assertThatThrownBy(() -> service.upload(missingName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is missing");

        MockMultipartFile badExtension = new MockMultipartFile(
                "file",
                "demo.txt",
                "text/plain",
                new byte[]{1}
        );
        assertThatThrownBy(() -> service.upload(badExtension))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".jar");
    }

    @Test
    void updateParameterAndInvokeMBeanValidateInputs() {
        DashboardService service = newService(false, tempDir.resolve("upload-mbean-fail"), "1 SECONDS");
        String conveyorName = "mbean-invalid-" + System.nanoTime();

        TestMBeanImpl proxy = new TestMBeanImpl();
        Conveyor<Object, Object, Object> conveyor = registerConveyor(conveyorName, null, true, false);
        try {
            when(conveyor.mBeanInterface()).thenReturn((Class) TestMBean.class);
            when(conveyor.getMBeanInstance(conveyorName)).thenReturn(proxy);

            assertThatThrownBy(() -> service.updateParameter(conveyorName, "missing", "1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown writable parameter");

            assertThatThrownBy(() -> service.invokeMBean(conveyorName, "missingOperation", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Operation not found");

            when(conveyor.mBeanInterface()).thenReturn(null);
            assertThatThrownBy(() -> service.updateParameter(conveyorName, "limit", "1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No MBean interface");
        } finally {
            safeUnregister(conveyorName);
        }
    }

    @Test
    void updateParameterAndInvokeMBeanCoverPrimitiveEnumAndPojoConversions() {
        DashboardService service = newService(false, tempDir.resolve("upload-mbean-convert"), "1 SECONDS");
        String conveyorName = "mbean-convert-" + System.nanoTime();

        ConversionMBeanImpl proxy = new ConversionMBeanImpl();
        Conveyor<Object, Object, Object> conveyor = registerConveyor(conveyorName, null, true, false);
        try {
            when(conveyor.mBeanInterface()).thenReturn((Class) ConversionMBean.class);
            when(conveyor.getMBeanInstance(conveyorName)).thenReturn(proxy);

            service.updateParameter(conveyorName, "longValue", "12");
            service.updateParameter(conveyorName, "enabled", "true");
            service.updateParameter(conveyorName, "marker", "Z");
            service.updateParameter(conveyorName, "level", "HIGH");

            assertThat(proxy.getLongValue()).isEqualTo(12L);
            assertThat(proxy.isEnabled()).isTrue();
            assertThat(proxy.getMarker()).isEqualTo('Z');
            assertThat(proxy.getLevel()).isEqualTo(Level.HIGH);

            Object voidResult = service.invokeMBean(conveyorName, "reset", null);
            assertThat(voidResult).isEqualTo("OK");

            Object pojoResult = service.invokeMBean(
                    conveyorName,
                    "acceptPojo",
                    Map.of("name", "n1", "count", 5)
            );
            assertThat(pojoResult).isEqualTo("accepted:n1:5");
            assertThat(proxy.getPojo()).isEqualTo(new ConversionPojo("n1", 5));

            assertThatThrownBy(() -> service.updateParameter(conveyorName, "marker", "ZZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Single character expected");
        } finally {
            safeUnregister(conveyorName);
        }
    }

    @Test
    void invokeMBeanRejectsOperationWhenArgumentPresenceDoesNotMatchSignature() {
        DashboardService service = newService(false, tempDir.resolve("upload-mbean-arg-mismatch"), "1 SECONDS");
        String conveyorName = "mbean-arg-mismatch-" + System.nanoTime();

        ConversionMBeanImpl proxy = new ConversionMBeanImpl();
        Conveyor<Object, Object, Object> conveyor = registerConveyor(conveyorName, null, true, false);
        try {
            when(conveyor.mBeanInterface()).thenReturn((Class) ConversionMBean.class);
            when(conveyor.getMBeanInstance(conveyorName)).thenReturn(proxy);

            assertThatThrownBy(() -> service.invokeMBean(conveyorName, "acceptPojo", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Operation not found");
        } finally {
            safeUnregister(conveyorName);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Conveyor<Object, Object, Object> registerConveyor(
            String name,
            Conveyor<?, ?, ?> enclosing,
            boolean running,
            boolean suspended
    ) {
        Conveyor<Object, Object, Object> conveyor = mock(Conveyor.class);
        ConveyorMetaInfo<String, String, Object> metaInfo = new ConveyorMetaInfo<>(
                String.class,
                String.class,
                Object.class,
                Map.of("X", Set.of(String.class)),
                List.of("X"),
                null
        );

        when(conveyor.getName()).thenReturn(name);
        when(conveyor.getEnclosingConveyor()).thenReturn((Conveyor) enclosing);
        when(conveyor.isRunning()).thenReturn(running);
        when(conveyor.isSuspended()).thenReturn(suspended);
        when(conveyor.getMetaInfo()).thenReturn((ConveyorMetaInfo) metaInfo);
        when(conveyor.mBeanInterface()).thenReturn((Class) TestMBean.class);
        doNothing().when(conveyor).completeThenForceStop(anyLong(), eq(TimeUnit.MILLISECONDS));
        doNothing().when(conveyor).stop();
        Conveyor.register(conveyor, new TestMBeanImpl());
        return conveyor;
    }

    private DashboardService newService(boolean uploadEnabled, Path uploadDir, String defaultStopTimeout) {
        ConveyorServiceProperties properties = new ConveyorServiceProperties();
        properties.setUploadDir(uploadDir);
        properties.setUploadEnable(uploadEnabled);
        return new DashboardService(
                new ObjectMapper(),
                properties,
                mock(ConveyorWatchService.class),
                defaultStopTimeout
        );
    }

    private void createBrokenInitiatingServiceJar(Path jarPath) throws Exception {
        Path sourceDir = tempDir.resolve("broken-service-src");
        Path classesDir = tempDir.resolve("broken-service-classes");
        Files.createDirectories(sourceDir.resolve("broken"));
        Files.createDirectories(classesDir);

        Path missingBaseSource = sourceDir.resolve("broken/MissingBase.java");
        Path providerSource = sourceDir.resolve("broken/BrokenInitiatingService.java");

        Files.writeString(
                missingBaseSource,
                """
                        package broken;

                        public class MissingBase {
                        }
                        """,
                StandardCharsets.UTF_8
        );
        Files.writeString(
                providerSource,
                """
                        package broken;

                        import com.aegisql.conveyor.Conveyor;
                        import com.aegisql.conveyor.ConveyorInitiatingService;

                        public class BrokenInitiatingService extends MissingBase implements ConveyorInitiatingService {
                            @Override
                            public Conveyor<?, ?, ?> getConveyor() {
                                return null;
                            }
                        }
                        """,
                StandardCharsets.UTF_8
        );

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("system java compiler").isNotNull();
        int exitCode = compiler.run(
                null,
                null,
                null,
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                classesDir.toString(),
                missingBaseSource.toString(),
                providerSource.toString()
        );
        assertThat(exitCode).isZero();

        Path providerClass = classesDir.resolve("broken/BrokenInitiatingService.class");
        assertThat(Files.exists(providerClass)).isTrue();

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jarOutputStream.putNextEntry(new JarEntry("broken/BrokenInitiatingService.class"));
            jarOutputStream.write(Files.readAllBytes(providerClass));
            jarOutputStream.closeEntry();

            jarOutputStream.putNextEntry(new JarEntry("META-INF/services/com.aegisql.conveyor.ConveyorInitiatingService"));
            jarOutputStream.write("broken.BrokenInitiatingService\n".getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }
    }

    private void safeUnregister(String name) {
        try {
            Conveyor.unRegister(name);
        } catch (Exception ignored) {
            // Ignore already-unregistered conveyors in cleanup.
        }
    }

    public interface TestMBean {
        int getLimit();
        void setLimit(int limit);
        String ping();
        String echo(String value);
    }

    public static class TestMBeanImpl implements TestMBean {
        private int limit;

        @Override
        public int getLimit() {
            return limit;
        }

        @Override
        public void setLimit(int limit) {
            this.limit = limit;
        }

        @Override
        public String ping() {
            return "pong";
        }

        @Override
        public String echo(String value) {
            return "echo:" + value;
        }
    }

    public interface ThrowingMBean {
        int getStableValue();
        int getBroken();
        void hardStop();
        String alpha();
    }

    public static class ThrowingMBeanImpl implements ThrowingMBean {
        @Override
        public int getStableValue() {
            return 11;
        }

        @Override
        public int getBroken() {
            throw new IllegalStateException("boom");
        }

        @Override
        public void hardStop() {
            // no-op
        }

        @Override
        public String alpha() {
            return "a";
        }
    }

    enum Level {
        LOW, HIGH
    }

    record ConversionPojo(String name, int count) {
    }

    public interface ConversionMBean {
        long getLongValue();
        void setLongValue(long value);
        boolean isEnabled();
        void setEnabled(boolean enabled);
        char getMarker();
        void setMarker(char marker);
        Level getLevel();
        void setLevel(Level level);
        ConversionPojo getPojo();
        void setPojo(ConversionPojo pojo);
        void reset();
        String acceptPojo(ConversionPojo pojo);
    }

    public static class ConversionMBeanImpl implements ConversionMBean {
        private long longValue;
        private boolean enabled;
        private char marker;
        private Level level = Level.LOW;
        private ConversionPojo pojo;

        @Override
        public long getLongValue() {
            return longValue;
        }

        @Override
        public void setLongValue(long value) {
            this.longValue = value;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public char getMarker() {
            return marker;
        }

        @Override
        public void setMarker(char marker) {
            this.marker = marker;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public void setLevel(Level level) {
            this.level = level;
        }

        @Override
        public ConversionPojo getPojo() {
            return pojo;
        }

        @Override
        public void setPojo(ConversionPojo pojo) {
            this.pojo = pojo;
        }

        @Override
        public void reset() {
            this.longValue = 0L;
        }

        @Override
        public String acceptPojo(ConversionPojo pojo) {
            this.pojo = pojo;
            return "accepted:" + pojo.name() + ":" + pojo.count();
        }
    }
}
