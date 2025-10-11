package com.aegisql.conveyor.utils.counter;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.function.Supplier;

public class CounterAggregatorConveyor<K> extends AssemblingConveyor<K,String, Map<String, Map<String, Integer>>> {

    private String names_label = "_NAMES_";
    private String expected_label_suffix = "_EXPECTED";

    public CounterAggregatorConveyor() {
        super();
        init();
    }

    public CounterAggregatorConveyor(Supplier<Queue<? extends Cart<K, ?, ?>>> cartQueueSupplier) {
        super(cartQueueSupplier);
        init();
    }

    private void init() {
        this.setName("CounterAggregatorConveyor " + innerThread.threadId());
        this.setBuilderSupplier(CountersAggregator::new);
        this.setDefaultCartConsumer(names_label, expected_label_suffix);

    }

    private void setDefaultCartConsumer(String names_label, String expected_label_suffix) {
        this.setDefaultCartConsumer(Conveyor.getConsumerFor(this, CountersAggregator.class)
                .match(".*", (l, v, b) -> b.addCounter(l, (int) v))
                .match(".*" + expected_label_suffix + "$", (l, v, b) -> b.addExpected(l.substring(0, l.length() - expected_label_suffix.length()), (int) v))
                .when(names_label, (b,v) -> b.addNames((Collection<String>) v))
        );
    }

    public void setNamesLabel(String names_label) {
        this.names_label = names_label;
        setDefaultCartConsumer(names_label, expected_label_suffix);
    }

    public void setExpectedLabelSuffix(String expected_label_suffix) {
        this.expected_label_suffix = expected_label_suffix;
        setDefaultCartConsumer(names_label, expected_label_suffix);
    }

}
