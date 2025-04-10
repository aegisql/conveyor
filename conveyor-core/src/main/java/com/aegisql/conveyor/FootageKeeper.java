package com.aegisql.conveyor;

public interface FootageKeeper<K> {
    void setMaxFootage(long maxFootage);
    void incFootage(BuildingSite<K,?,?,?> buildingSite);
    void decFootage(BuildingSite<K,?,?,?> buildingSite);
    K getOldestInactiveKey();
    void removeKey(K key);
}
