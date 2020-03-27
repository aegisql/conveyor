package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ObservableResultConsumer<K,V> implements ResultConsumer<K,V>{

    private final Map<K, List<CompletableFuture<V>>> futureMap = new HashMap<>();

    @Override
    public synchronized void accept(ProductBin<K, V> bin) {
        List<CompletableFuture<V>> futures = futureMap.remove(bin.key);
        if(futures != null) {
            futures.forEach(future->future.complete(bin.product));
        }
    }

    public synchronized CompletableFuture<V> waitFor(K key) {
        CompletableFuture<V> future = new CompletableFuture<>();
        List<CompletableFuture<V>> futures = futureMap.computeIfAbsent(key, k -> new ArrayList<>());
        futures.add(future);
        return future;
    }

    public static <K,V> ObservableResultConsumer<K, V> of(Conveyor<K, ?, V> conv) {
        return new ObservableResultConsumer<>();
    }

}
