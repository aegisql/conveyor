package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import com.aegisql.conveyor.utils.BuilderUtils;
import com.aegisql.conveyor.utils.Wrapped;
import org.junit.Test;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class ProductBuilderTest {

    @Test
    public void pocTest() {

        LastResultReference<Integer, String> res = new LastResultReference<>();
        LastScrapReference<Integer> scrap = new LastScrapReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder, String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND", (stringBuilder, o) -> stringBuilder.unwrap().append(o));
        SmartLabel<Wrapped<StringBuilder>> DONE = SmartLabel.bare("DONE");


        AssemblingConveyor<Integer, SmartLabel<Wrapped<StringBuilder>>, String> ac = new AssemblingConveyor<>();
        ac.setName("pocTest");

        BuilderUtils.wrapBuilderSupplier(ac, () -> new StringBuilder("INIT "))
                .productSupplier(StringBuilder::toString)
                .setBuilderSupplier();


        ac.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(DONE));
        ac.resultConsumer(res).set();
        ac.scrapConsumer(scrap).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place().join();
        ac.part().id(1).label(DONE).place();
        ac.completeAndStop().join();
        System.err.println(res + "\n" + scrap);
        assertEquals("INIT A B", res.getCurrent());
    }


    @Test
    public void pocTestTesting() {

        LastResultReference<Integer, String> res = new LastResultReference<>();
        LastScrapReference<Integer> scrap = new LastScrapReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder, String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND", (stringBuilder, o) -> stringBuilder.unwrap().append(o));

        AssemblingConveyor<Integer, SmartLabel<Wrapped<StringBuilder>>, String> ac = new AssemblingConveyor<>();
        ac.setName("pocTesting");

        BuilderUtils.wrapBuilderSupplier(ac, StringBuilder::new)
                .productSupplier(sb -> sb.toString())
                .tester(sb -> sb.charAt(sb.length() - 1) == '.')
                .testerOnTimeout((s, b) -> false)
                .setBuilderSupplier();


        ac.resultConsumer(res).set();
        ac.scrapConsumer(scrap).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place();
        ac.part().id(1).label(APPEND).value(".").place().join();
        ac.completeAndStop().join();
        System.err.println(res + "\n" + scrap);
        assertEquals("A B.", res.getCurrent());
    }

    @Test
    public void pocTestState() {

        LastResultReference<Integer, String> res = new LastResultReference<>();
        LastScrapReference<Integer> scrap = new LastScrapReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder, String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND", (stringBuilder, o) -> stringBuilder.unwrap().append(o));
        SmartLabel<Wrapped<StringBuilder>> DONE = SmartLabel.bare("DONE");

        AssemblingConveyor<Integer, SmartLabel<Wrapped<StringBuilder>>, String> ac = new AssemblingConveyor<>();
        ac.setName("pocTestState");

        BuilderUtils.wrapBuilderSupplier(ac, StringBuilder::new)
                .productSupplier(sb -> sb.toString())
                .tester((state, builder) -> state.eventHistory.containsKey(DONE) && builder.length() > 0)
                .setBuilderSupplier();


        ac.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(DONE));
        ac.resultConsumer(res).set();
        ac.scrapConsumer(scrap).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place().join();
        ac.part().id(1).label(DONE).place();
        ac.completeAndStop().join();
        System.err.println(res + "\n" + scrap);
        assertEquals("A B", res.getCurrent());
    }


    @Test
    public void pocTimeoutTesting() throws InterruptedException {

        LastResultReference<Integer, String> res = new LastResultReference<>();
        LastScrapReference<Integer> scrap = new LastScrapReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder, String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND", (stringBuilder, o) -> stringBuilder.unwrap().append(o));

        AssemblingConveyor<Integer, SmartLabel<Wrapped<StringBuilder>>, String> ac = new AssemblingConveyor<>();
        ac.setName("pocTimeoutTesting");
        ac.setDefaultBuilderTimeout(Duration.ofMillis(100));
        ac.setIdleHeartBeat(Duration.ofMillis(10));
        BuilderUtils.wrapBuilderSupplier(ac, StringBuilder::new)
                .productSupplier(sb -> sb.toString())
                .tester(sb -> sb.charAt(sb.length() - 1) == '.')
                .testerOnTimeout(b -> true)
                .setBuilderSupplier();


        ac.resultConsumer(res).set();
        ac.scrapConsumer(scrap).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place().join();
        ac.completeAndStop().join();
        System.err.println(res + "\n" + scrap);
        assertEquals("A B", res.getCurrent());
    }

    @Test
    public void pocTimeoutStateTesting() throws InterruptedException {

        LastResultReference<Integer, String> res = new LastResultReference<>();
        LastScrapReference<Integer> scrap = new LastScrapReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder, String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND", (stringBuilder, o) -> stringBuilder.unwrap().append(o));

        AssemblingConveyor<Integer, SmartLabel<Wrapped<StringBuilder>>, String> ac = new AssemblingConveyor<>();
        ac.setName("pocTimeoutStateTesting");
        ac.setDefaultBuilderTimeout(Duration.ofMillis(100));
        ac.setIdleHeartBeat(Duration.ofMillis(10));
        BuilderUtils.wrapBuilderSupplier(ac, StringBuilder::new)
                .productSupplier(sb -> sb.toString())
                .tester(sb -> sb.charAt(sb.length() - 1) == '.')
                .testerOnTimeout((s, b) -> true)
                .setBuilderSupplier();


        ac.resultConsumer(res).set();
        ac.scrapConsumer(scrap).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place().join();
        ac.completeAndStop().join();
        System.err.println(res + "\n" + scrap);
        assertEquals("A B", res.getCurrent());
    }

    @Test
    public void pocTimeoutTestingRev() throws InterruptedException {

        LastResultReference<Integer, String> res = new LastResultReference<>();
        LastScrapReference<Integer> scrap = new LastScrapReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder, String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND", (stringBuilder, o) -> stringBuilder.unwrap().append(o));

        AssemblingConveyor<Integer, SmartLabel<Wrapped<StringBuilder>>, String> ac = new AssemblingConveyor<>();
        ac.setName("pocTimeoutTestingRev");
        ac.setDefaultBuilderTimeout(Duration.ofMillis(100));
        ac.setIdleHeartBeat(Duration.ofMillis(10));
        BuilderUtils.wrapBuilderSupplier(ac, StringBuilder::new)
                .productSupplier(sb -> sb.toString())
                .testerOnTimeout(b -> true)
                .tester(sb -> sb.charAt(sb.length() - 1) == '.')
                .onTimeout(sb -> sb.append(" DEF"))
                .setBuilderSupplier();


        ac.resultConsumer(res).set();
        ac.scrapConsumer(scrap).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place().join();
        ac.completeAndStop().join();
        System.err.println(res + "\n" + scrap);
        assertEquals("A B DEF", res.getCurrent());
    }

    @Test
    public void pocTimeoutStateTestingRev() throws InterruptedException {

        LastResultReference<Integer, String> res = new LastResultReference<>();
        LastScrapReference<Integer> scrap = new LastScrapReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder, String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND", (stringBuilder, o) -> stringBuilder.unwrap().append(o));

        AssemblingConveyor<Integer, SmartLabel<Wrapped<StringBuilder>>, String> ac = new AssemblingConveyor<>();
        ac.setName("pocTimeoutStateTestingRev");
        ac.setDefaultBuilderTimeout(Duration.ofMillis(100));
        ac.setIdleHeartBeat(Duration.ofMillis(10));
        BuilderUtils.wrapBuilderSupplier(ac, StringBuilder::new)
                .productSupplier(sb -> sb.toString())
                .testerOnTimeout((s, b) -> true)
                .tester(sb -> sb.charAt(sb.length() - 1) == '.')
                .onTimeout(sb -> sb.append(" XYZ"))
                .setBuilderSupplier();


        ac.resultConsumer(res).set();
        ac.scrapConsumer(scrap).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place().join();
        ac.completeAndStop().join();
        System.err.println(res + "\n" + scrap);
        assertEquals("A B XYZ", res.getCurrent());
    }

    @Test
    public void pocTimeoutStateStateTestingRev() throws InterruptedException {

        LastResultReference<Integer, String> res = new LastResultReference<>();
        LastScrapReference<Integer> scrap = new LastScrapReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder, String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND", (stringBuilder, o) -> stringBuilder.unwrap().append(o));

        AssemblingConveyor<Integer, SmartLabel<Wrapped<StringBuilder>>, String> ac = new AssemblingConveyor<>();
        ac.setName("pocTimeoutStateTestingRev");
        ac.setDefaultBuilderTimeout(Duration.ofMillis(100));
        ac.setIdleHeartBeat(Duration.ofMillis(10));
        BuilderUtils.wrapBuilderSupplier(ac, StringBuilder::new)
                .productSupplier(sb -> sb.toString())
                .testerOnTimeout((s, b) -> true)
                .tester((state, sb) -> sb.charAt(sb.length() - 1) == '.')
                .onTimeout(sb -> sb.append(" XYZ"))
                .setBuilderSupplier();


        ac.resultConsumer(res).set();
        ac.scrapConsumer(scrap).set();
        ac.part().id(1).label(APPEND).value("A").place();
        ac.part().id(1).label(APPEND).value(" ").place();
        ac.part().id(1).label(APPEND).value("B").place().join();
        ac.completeAndStop().join();
        System.err.println(res + "\n" + scrap);
        assertEquals("A B XYZ", res.getCurrent());
    }

    @Test
    public void pocTimeoutSeDefValue() throws InterruptedException {

        LastResultReference<Integer, String> res = new LastResultReference<>();
        LastScrapReference<Integer> scrap = new LastScrapReference<>();
        Supplier<StringBuilder> sup = StringBuilder::new;
        Function<StringBuilder, String> prodSupplier = StringBuilder::toString;


        SmartLabel<Wrapped<StringBuilder>> APPEND = SmartLabel.of("APPEND", (stringBuilder, o) -> stringBuilder.unwrap().append(o));

        AssemblingConveyor<Integer, SmartLabel<Wrapped<StringBuilder>>, String> ac = new AssemblingConveyor<>();
        ac.setName("pocTimeoutSeDefValue");
        ac.setDefaultBuilderTimeout(Duration.ofMillis(100));
        ac.setIdleHeartBeat(Duration.ofMillis(10));
        ac.setReadinessEvaluator(sb -> {
            return ((Wrapped<StringBuilder>) sb).unwrap().length() > 0;
        });
        BuilderUtils.wrapBuilderSupplier(ac, StringBuilder::new)
                .productSupplier(sb -> sb.toString())
                .onTimeout(sb -> sb.append("DEF VAL"))
                .setBuilderSupplier();


        ac.resultConsumer(res).set();
        ac.scrapConsumer(scrap).set();
        ac.part().id(1).label(APPEND).value("").place().join();
        ac.completeAndStop().join();
        System.err.println(res + "\n" + scrap);
        assertEquals("DEF VAL", res.getCurrent());
    }


}