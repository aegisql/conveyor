package com.aegisql.conveyor;

abstract class AbstractBin <K,L,OUT> {

    public final Conveyor<K,L,OUT> conveyor;
    public final K key;

    AbstractBin(Conveyor<K,L,OUT> conveyor, K key) {
        this.key      = key;
        this.conveyor = conveyor;
    }

}
