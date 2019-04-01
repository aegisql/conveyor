package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.LastResultReference;
import org.junit.Test;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class ProductBuilderTest {

    @Test
    public void pocTest() {

        LastResultReference<Integer,String> res = new LastResultReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder,String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND",(stringBuilder, o) -> stringBuilder.unwrap().append(o));
        SmartLabel<Wrapped<StringBuilder>> DONE = SmartLabel.bare("DONE");



        AssemblingConveyor<Integer,SmartLabel<Wrapped<StringBuilder>>,String> ac = new AssemblingConveyor<>();
        ac.setName("pocTest");

        BuilderUtils.forConveyor(ac)
                .builderSupplier(StringBuilder::new)
                .productSupplier(sb->sb.toString())
                .setBuilderSupplier();


        ac.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(DONE));
        ac.resultConsumer(res).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place();
        ac.part().id(1).label(DONE).place();
        ac.completeAndStop().join();
        System.err.println(res);
        assertEquals("A B",res.getCurrent());
    }


    @Test
    public void pocTestTesting() {

        LastResultReference<Integer,String> res = new LastResultReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder,String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND",(stringBuilder, o) -> stringBuilder.unwrap().append(o));

        AssemblingConveyor<Integer,SmartLabel<Wrapped<StringBuilder>>,String> ac = new AssemblingConveyor<>();
        ac.setName("pocTesting");

        BuilderUtils.forConveyor(ac)
                .builderSupplier(StringBuilder::new)
                .productSupplier(sb->sb.toString())
                .tester(sb->sb.charAt(sb.length()-1)=='.')
                .setBuilderSupplier();


        ac.resultConsumer(res).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place();
        ac.part().id(1).label(APPEND).value(".").place();
        ac.completeAndStop().join();
        System.err.println(res);
        assertEquals("A B.",res.getCurrent());
    }

    @Test
    public void pocTestState() {

        LastResultReference<Integer,String> res = new LastResultReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder,String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND",(stringBuilder, o) -> stringBuilder.unwrap().append(o));
        SmartLabel<Wrapped<StringBuilder>> DONE = SmartLabel.bare("DONE");

        AssemblingConveyor<Integer,SmartLabel<Wrapped<StringBuilder>>,String> ac = new AssemblingConveyor<>();
        ac.setName("pocTestState");

        BuilderUtils.forConveyor(ac)
                .builderSupplier(StringBuilder::new)
                .productSupplier(sb->sb.toString())
                .tester((state,builder)->state.eventHistory.containsKey(DONE) && builder.length() > 0)
                .setBuilderSupplier();


        ac.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(DONE));
        ac.resultConsumer(res).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place();
        ac.part().id(1).label(DONE).place();
        ac.completeAndStop().join();
        System.err.println(res);
        assertEquals("A B",res.getCurrent());
    }

}