package com.aegisql.conveyor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConveyorTreeTest {

    @Test
    public void knownConveyorNameTreeReflectsEnclosingHierarchy() {
        var suffix = String.valueOf(System.nanoTime());
        var rootName = "tree-root-" + suffix;
        var childName = "tree-child-" + suffix;
        var grandChildName = "tree-grand-child-" + suffix;
        var secondRootName = "tree-second-root-" + suffix;

        AssemblingConveyor<Integer, String, String> root = new AssemblingConveyor<>();
        AssemblingConveyor<Integer, String, String> child = new AssemblingConveyor<>();
        AssemblingConveyor<Integer, String, String> grandChild = new AssemblingConveyor<>();
        AssemblingConveyor<Integer, String, String> secondRoot = new AssemblingConveyor<>();

        root.setName(rootName);
        child.setName(childName);
        grandChild.setName(grandChildName);
        secondRoot.setName(secondRootName);

        child.setEnclosingConveyor(root);
        grandChild.setEnclosingConveyor(child);

        try {
            var tree = Conveyor.getKnownConveyorNameTree();
            assertTrue(tree.containsKey(rootName));
            assertTrue(tree.containsKey(secondRootName));
            assertFalse(tree.containsKey(childName));
            assertFalse(tree.containsKey(grandChildName));

            Map<String, ?> rootSubTree = tree.get(rootName);
            assertNotNull(rootSubTree);
            assertTrue(rootSubTree.containsKey(childName));

            Map<String, ?> childSubTree = (Map<String, ?>) rootSubTree.get(childName);
            assertNotNull(childSubTree);
            assertTrue(childSubTree.containsKey(grandChildName));

            Map<String, ?> grandChildSubTree = (Map<String, ?>) childSubTree.get(grandChildName);
            assertNotNull(grandChildSubTree);
            assertTrue(grandChildSubTree.isEmpty());

            System.out.println(tree);
        } finally {
            Conveyor.unRegister(grandChildName);
            Conveyor.unRegister(childName);
            Conveyor.unRegister(rootName);
            Conveyor.unRegister(secondRootName);
        }
    }
}
