package com.aegisql.conveyor.loaders;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MultiKeyCommandLoaderTest {

    @Test
    public void multiKeyCommandLoader() {
        MultiKeyCommandLoader cl = new MultiKeyCommandLoader(cmd->new CompletableFuture<Boolean>())
                .foreach()
                .foreach(k->true)
                .creationTime(1)
                .creationTime(Instant.now());

    }

    @Test
    public void peekTest() {
        CompletableFuture<Boolean> canceled = new CompletableFuture<>();
        canceled.cancel(true);
        MultiKeyCommandLoader cl1 = new MultiKeyCommandLoader(cmd->canceled);
        CompletableFuture peek1 = cl1.peek();

        try {
            peek1.get(1, TimeUnit.MILLISECONDS);
        } catch (Exception e) {

        }
        CompletableFuture<Boolean> exceptional = new CompletableFuture<>();
        canceled.completeExceptionally(new Exception("test"));
        MultiKeyCommandLoader cl2 = new MultiKeyCommandLoader(cmd->exceptional);
        CompletableFuture peek2 = cl2.peek();
        try {
            peek2.get(1, TimeUnit.MILLISECONDS);
        } catch (Exception e) {

        }

        CompletableFuture<Boolean> falseRes = new CompletableFuture<>();
        falseRes.complete(false);
        MultiKeyCommandLoader cl3 = new MultiKeyCommandLoader(cmd->falseRes);
        CompletableFuture peek3 = cl3.peek();
        try {
            peek3.get(1, TimeUnit.MILLISECONDS);
        } catch (Exception e) {

        }

    }

    @Test
    public void mementoTest() {
        CompletableFuture<Boolean> canceled = new CompletableFuture<>();
        canceled.cancel(true);
        MultiKeyCommandLoader cl1 = new MultiKeyCommandLoader(cmd->canceled);
        CompletableFuture memento1 = cl1.memento();

        try {
            memento1.get(1, TimeUnit.MILLISECONDS);
        } catch (Exception e) {

        }
        CompletableFuture<Boolean> exceptional = new CompletableFuture<>();
        canceled.completeExceptionally(new Exception("test"));
        MultiKeyCommandLoader cl2 = new MultiKeyCommandLoader(cmd->exceptional);
        CompletableFuture memento2 = cl2.memento();
        try {
            memento2.get(1, TimeUnit.MILLISECONDS);
        } catch (Exception e) {

        }

        CompletableFuture<Boolean> falseRes = new CompletableFuture<>();
        falseRes.complete(false);
        MultiKeyCommandLoader cl3 = new MultiKeyCommandLoader(cmd->falseRes);
        CompletableFuture memento3 = cl3.memento();
        try {
            memento3.get(1, TimeUnit.MILLISECONDS);
        } catch (Exception e) {

        }

    }
}