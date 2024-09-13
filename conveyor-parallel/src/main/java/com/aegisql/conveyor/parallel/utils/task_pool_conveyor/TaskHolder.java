package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Testing;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class TaskHolder<K,T> implements Supplier<T>, Testing {

    private final TaskPoolConveyor<K, T> taskPoolConveyor;
    private final AssemblingConveyor<K, Label, T> myConveyor;

    public TaskHolder(TaskPoolConveyor<K, T> taskPoolConveyor, AssemblingConveyor<K, Label,T> assemblingConveyor) {
        this.taskPoolConveyor = taskPoolConveyor;
        this.myConveyor = assemblingConveyor;
    }


    enum Label implements SmartLabel<TaskHolder<?,?>> {
        EXECUTE((b,v)->b.execute((Supplier) v))
        ;

        private final BiConsumer<TaskHolder<?,?>, Object> consumer;

        Label(BiConsumer<TaskHolder<?,?>, Object> consumer) {
            this.consumer = consumer;
        }

        @Override
        public BiConsumer<TaskHolder<?,?>, Object> get() {
            return consumer;
        }
    }

    Supplier<T> supplier;
    private boolean ready = true;

    public void execute(Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        this.supplier = supplier;
    }

    @Override
    public T get() {
        taskPoolConveyor.part().id(myConveyor.current_id.get()).label(TaskManager.ProtectedLabel.STARTED).value(System.currentTimeMillis()).place();
        return supplier.get();
    }

    @Override
    public boolean test() {
        return ready;
    }

}
