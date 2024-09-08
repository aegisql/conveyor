package com.aegisql.conveyor.poc.thread_pool;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Testing;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class TaskHolder <T> implements Supplier<T>, Testing {


    enum Label implements SmartLabel<TaskHolder> {
        EXECUTE(TaskHolder::execute),
        MY_ID((th,supplier)->th.setMyId((Supplier<Integer>) supplier))
        ;

        private final BiConsumer<TaskHolder, Object> consumer;

        Label(BiConsumer<TaskHolder, Object> consumer) {
            this.consumer = consumer;
        }

        @Override
        public BiConsumer<TaskHolder, Object> get() {
            return consumer;
        }
    }

    Supplier<T> supplier;
    private Supplier<Integer> myId;

    public void setMyId(Supplier<Integer> myId) {
        this.myId = myId;
    }

    @Override
    public T get() {
        return supplier.get();
    }


    private static void execute(TaskHolder b, Object supplier) {
        b.supplier = (Supplier) supplier;
    }

    @Override
    public boolean test() {
        return true;
    }

}
