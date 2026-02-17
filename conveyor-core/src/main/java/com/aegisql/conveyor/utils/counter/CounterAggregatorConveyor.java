package com.aegisql.conveyor.utils.counter;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.meta.ConveyorMetaInfoBuilder;

import java.util.Collection;
import java.util.List;
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

    protected ConveyorMetaInfo<K,String, Map<String, Map<String, Integer>>> metaInfo = null;

    @SuppressWarnings("unchecked")
    private static Class<List<String>> toListOfStringType() {
        return (Class<List<String>>) (Class<?>) List.class;
    }
    @SuppressWarnings("unchecked")
    protected static Class<Map<String, Map<String, Integer>>> productMapType() {
        return (Class<Map<String, Map<String, Integer>>>) (Class<?>) Map.class;
    }
    protected ConveyorMetaInfoBuilder<K,String, Map<String, Map<String, Integer>>> getMetaInfoBuilder() {
        return new ConveyorMetaInfoBuilder<K,String, Map<String, Map<String, Integer>>>()
                .labelType(String.class)
                .productType(productMapType())
                .labels(names_label,".*"+expected_label_suffix,".*")
                .supportedTypes(names_label,toListOfStringType())
                .supportedTypes(".*"+expected_label_suffix,Integer.class)
                .supportedTypes(".*",Integer.class)
                ;
    }

}
