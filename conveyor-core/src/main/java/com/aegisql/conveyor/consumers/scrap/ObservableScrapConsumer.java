package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ObservableScrapConsumer<K> implements ScrapConsumer<K,Object>{

    private final Map<K, List<CompletableFuture<ScrapBin<K, Object>>>> futureMap = new HashMap<>();

    @Override
    public synchronized void accept(ScrapBin<K, Object> bin) {
        List<CompletableFuture<ScrapBin<K, Object>>> futures = futureMap.remove(bin.key);
        if(futures != null) {
            futures.forEach(future->future.complete(bin));
        }
    }

    public synchronized CompletableFuture<ScrapBin<K, Object>> waitFor(K key) {
        CompletableFuture<ScrapBin<K, Object>> future = new CompletableFuture<>();
        List<CompletableFuture<ScrapBin<K, Object>>> futures = futureMap.computeIfAbsent(key, k -> new ArrayList<>());
        futures.add(future);
        return future;
    }

    public static <K> ObservableScrapConsumer<K> of(Conveyor<K, ?, ?> conv) {
        return new ObservableScrapConsumer<>();
    }

}
