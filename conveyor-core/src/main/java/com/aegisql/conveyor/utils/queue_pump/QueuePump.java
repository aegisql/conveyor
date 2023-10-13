package com.aegisql.conveyor.utils.queue_pump;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Priority;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.loaders.BuilderLoader;
import com.aegisql.conveyor.loaders.FutureLoader;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.loaders.StaticPartLoader;

import java.util.Queue;
import java.util.function.Supplier;

import static com.aegisql.conveyor.utils.queue_pump.PumpId.PUMP_ID;
import static com.aegisql.conveyor.utils.queue_pump.PumpLabel.PUMP;

public class QueuePump <OUT> extends AssemblingConveyor<PumpId,PumpLabel,OUT> {

    public QueuePump() {
        this(Priority.DEFAULT);
    }

    public QueuePump(Supplier<Queue<? extends Cart<PumpId, ?, ?>>> cartQueueSupplier) {
        super(cartQueueSupplier);
        this.setBuilderSupplier(ScalarHolder::new);
        this.setDefaultCartConsumer((l,v,b)->{
            ScalarHolder<OUT> holder = (ScalarHolder<OUT>) b;
            holder.setValue((OUT) v);
        });
    }

    public PartLoader<PumpId,PumpLabel> part() {
        return super.part().id(PUMP_ID).label(PUMP);
    }

    @Override
    public StaticPartLoader<PumpLabel> staticPart() {
        return super.staticPart().label(PUMP);
    }

    @Override
    public BuilderLoader<PumpId, OUT> build() {
        return super.build().id(PUMP_ID);
    }

    @Override
    public FutureLoader<PumpId, OUT> future() {
        return super.future().id(PUMP_ID);
    }

}
