package com.aegisql.conveyor;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ConveyorTest {

    @Test
    public void getConsumerFor() {
        LabeledValueConsumer lc1 = Conveyor.getConsumerFor(null);
        LabeledValueConsumer lc2 = Conveyor.getConsumerFor(null,null);
        assertNotNull(lc1);
        assertNotNull(lc2);
        try{
            lc1.accept("","",null);
            fail("Must fail");
        } catch (Exception e) { }
        try{
            lc2.accept("","",null);
            fail("Must fail");
        } catch (Exception e) { }
    }

    @Test
    public void unRegister() {
        assertThrows(RuntimeException.class,()->Conveyor.unRegister("something wrong :"));
    }

    @Test
    public void defaultHelpersShouldCoverRegisterUnregisterAndKill() {
        AssemblingConveyor<Integer, String, String> c = new AssemblingConveyor<>();
        c.setName("ConveyorTestDefaultHelpers");
        try {
            c.setBuilderSupplier(() -> () -> "v");
            c.setDefaultCartConsumer((l, v, b) -> {});
            c.setReadinessEvaluator((state, builder) -> true);

            c.register(c.getMBeanInstance(c.getName()));
            assertTrue(Conveyor.getRegisteredConveyorNames().contains("ConveyorTestDefaultHelpers"));

            assertDoesNotThrow(() -> c.kill(1).join());

            c.unRegister();
            assertFalse(Conveyor.getKnownConveyorNames().contains("ConveyorTestDefaultHelpers"));
        } finally {
            Conveyor.unRegister("ConveyorTestDefaultHelpers");
            c.stop();
        }
    }

    @Test
    public void knownConveyorTreeShouldHandleCyclesAndDisconnectedNodes() {
        AssemblingConveyor<Integer, String, String> a = new AssemblingConveyor<>();
        AssemblingConveyor<Integer, String, String> b = new AssemblingConveyor<>();
        AssemblingConveyor<Integer, String, String> orphan = new AssemblingConveyor<>();
        a.setName("ConveyorTreeCycleA");
        b.setName("ConveyorTreeCycleB");
        orphan.setName("ConveyorTreeCycleOrphan");
        try {
            // force cyclic parent relation so there are no roots in this subgraph
            a.setEnclosingConveyor(b);
            b.setEnclosingConveyor(a);

            Map<String, Map<String, ?>> tree = Conveyor.getKnownConveyorNameTree();
            assertTrue(tree.containsKey("ConveyorTreeCycleOrphan"));

            String cycleRoot = tree.containsKey("ConveyorTreeCycleA") ? "ConveyorTreeCycleA" : "ConveyorTreeCycleB";
            String cycleChild = cycleRoot.equals("ConveyorTreeCycleA") ? "ConveyorTreeCycleB" : "ConveyorTreeCycleA";

            Map<String, ?> rootChildren = tree.get(cycleRoot);
            assertNotNull(rootChildren);
            assertTrue(rootChildren.containsKey(cycleChild));

            // recursion guard path should produce an empty subtree for the cycle closure.
            assertEquals(Map.of(), ((Map<?, ?>) rootChildren.get(cycleChild)).get(cycleRoot));

            Set<String> registered = Conveyor.getRegisteredConveyorNames();
            assertTrue(registered.contains("ConveyorTreeCycleA"));
            assertTrue(registered.contains("ConveyorTreeCycleB"));
            assertTrue(registered.contains("ConveyorTreeCycleOrphan"));
        } finally {
            Conveyor.unRegister("ConveyorTreeCycleA");
            Conveyor.unRegister("ConveyorTreeCycleB");
            Conveyor.unRegister("ConveyorTreeCycleOrphan");
            a.stop();
            b.stop();
            orphan.stop();
        }
    }
}
