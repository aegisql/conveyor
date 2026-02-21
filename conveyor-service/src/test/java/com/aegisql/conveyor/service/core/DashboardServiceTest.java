package com.aegisql.conveyor.service.core;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.service.config.ConveyorServiceProperties;
import com.aegisql.conveyor.service.error.FeatureDisabledException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
}
