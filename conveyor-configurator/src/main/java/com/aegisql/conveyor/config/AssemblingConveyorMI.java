package com.aegisql.conveyor.config;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;

import java.util.Queue;
import java.util.function.Supplier;

public class AssemblingConveyorMI<K,L,OUT> extends AssemblingConveyor<K,L,OUT> {

    private final ConveyorMetaInfo<K,L,OUT> metaInfo;

    public AssemblingConveyorMI(ConveyorMetaInfo<K, L, OUT> metaInfo) {
        super();
        this.metaInfo = metaInfo;
    }

    public AssemblingConveyorMI(Supplier<Queue<? extends Cart<K, ?, ?>>> cartQueueSupplier, ConveyorMetaInfo<K, L, OUT> metaInfo) {
        super(cartQueueSupplier);
        this.metaInfo = metaInfo;
    }

    @Override
    public ConveyorMetaInfo<K, L, OUT> getMetaInfo() {
        return metaInfo;
    }

}
