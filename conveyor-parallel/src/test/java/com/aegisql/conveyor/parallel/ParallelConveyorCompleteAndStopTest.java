package com.aegisql.conveyor.parallel;

import com.aegisql.conveyor.Conveyor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParallelConveyorCompleteAndStopTest {

    @Test
    void completeAndStopWaitsForAllEnclosedConveyorsEvenIfOneAlreadyFailed() {
        CompletableFuture<Boolean> failed = new CompletableFuture<>();
        CompletableFuture<Boolean> pending = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("boom"));

        AtomicInteger index = new AtomicInteger(0);
        Supplier<Conveyor<Integer, String, String>> supplier = () -> {
            @SuppressWarnings("unchecked")
            Conveyor<Integer, String, String> conveyor = mock(Conveyor.class);
            if (index.getAndIncrement() == 0) {
                when(conveyor.completeAndStop()).thenReturn(failed);
            } else {
                when(conveyor.completeAndStop()).thenReturn(pending);
            }
            return conveyor;
        };

        KBalancedParallelConveyor<Integer, String, String> parallel = new KBalancedParallelConveyor<>(supplier, 2);

        CompletableFuture<Boolean> aggregated = parallel.completeAndStop();

        assertFalse(aggregated.isDone(), "Aggregated future must wait for all enclosed conveyor futures");

        pending.complete(Boolean.TRUE);

        assertTrue(aggregated.isDone(), "Aggregated future should complete after all enclosed futures complete");
        assertTrue(aggregated.isCompletedExceptionally(), "Aggregated future should propagate failure");
        assertThrows(ExecutionException.class, aggregated::get);
    }

    @Test
    void completeAndStopMarksParallelConveyorAsNotRunningAfterSuccessfulCompletion() {
        AtomicInteger index = new AtomicInteger(0);
        Supplier<Conveyor<Integer, String, String>> supplier = () -> {
            @SuppressWarnings("unchecked")
            Conveyor<Integer, String, String> conveyor = mock(Conveyor.class);
            when(conveyor.completeAndStop()).thenReturn(CompletableFuture.completedFuture(Boolean.TRUE));
            when(conveyor.getName()).thenReturn("child-" + index.getAndIncrement());
            return conveyor;
        };

        KBalancedParallelConveyor<Integer, String, String> parallel = new KBalancedParallelConveyor<>(supplier, 2);
        CompletableFuture<Boolean> aggregated = parallel.completeAndStop();

        assertTrue(Boolean.TRUE.equals(aggregated.join()), "Aggregated future should complete successfully");
        assertFalse(parallel.isRunning(), "Parallel conveyor should report stopped after successful completion");
    }
}
