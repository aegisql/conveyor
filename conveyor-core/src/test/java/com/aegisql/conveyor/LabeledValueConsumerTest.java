package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.LogResult;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

public class LabeledValueConsumerTest {

    class ListBuilder implements Supplier<Collection<String>>, Testing {
        Map<Integer,String> data;
        boolean ready = false;
        public ListBuilder() {
            data = new TreeMap<>();
        }
        @Override
        public Collection<String> get() {
            return data.values();
        }
        @Override
        public boolean test() {
            return ready;
        }
    }

    @Test
    public void resequencerTest() {

        AssemblingConveyor<Integer,Integer,Collection<String>> sortingConveyor = new AssemblingConveyor<>();
        LabeledValueConsumer<Integer,String,ListBuilder> labeledValueConsumer = (l,s,b)->{
            b.data.put(l,s);
        };
        Integer READY = -1;
        labeledValueConsumer = labeledValueConsumer.when(READY,(listBuilder,s)->{listBuilder.ready = true;});
        sortingConveyor.setDefaultCartConsumer(labeledValueConsumer);
        sortingConveyor.setName("resequencer");
        sortingConveyor.setBuilderSupplier(ListBuilder::new);
        sortingConveyor.resultConsumer(LogResult.debug(sortingConveyor)).set();

        sortingConveyor.part().id(1).label(2).value("two").place();
        sortingConveyor.part().id(1).label(1).value("one").place();
        sortingConveyor.part().id(1).label(3).value("three").place();
        sortingConveyor.part().id(1).label(-1).place().join();

        sortingConveyor.completeAndStop();

    }

}