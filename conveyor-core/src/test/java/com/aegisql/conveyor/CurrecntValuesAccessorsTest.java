package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.LogResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Supplier;

public class CurrecntValuesAccessorsTest {

    class Tester implements Supplier<String> {

        private final AssemblingConveyor<Integer, String, String> conveyor;

        public Tester(AssemblingConveyor<Integer,String,String> conveyor) {
            this.conveyor = conveyor;
        }

        public void processValue(Object value) {
            System.out.println("Current value: "+value);
            System.out.println("Current ID: "+conveyor.current_id.get());
            System.out.println("Current label: "+conveyor.current_label.get());
            System.out.println("Current properties: "+conveyor.current_properties.get());
            System.out.println("Current property: "+conveyor.current_property.apply("SOME_PROPERTY"));
            System.out.println("Current creation time: "+conveyor.current_creation_time.get());
            System.out.println("Current expiration time: "+conveyor.current_expiration_time.get());
            System.out.println("Current load type: "+conveyor.current_load_type.get());
            System.out.println("Current status: "+conveyor.current_status.get());
            conveyor.current_properties.get().put("ENRICHED",value);
        }

        @Override
        public String get() {
            return "DONE";
        }
    }

    @Test
    public void testAccessors() {
        AssemblingConveyor<Integer, String, String> conveyor = new AssemblingConveyor<>();
        conveyor.setName("currentAccessors");
        conveyor.setBuilderSupplier(()->new Tester(conveyor));
        conveyor.setDefaultCartConsumer((l,v,b)->{
            Tester tester = (Tester) b;
            tester.processValue(v);
        });
        conveyor.resultConsumer(LogResult.debug(conveyor)).set();
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted("SOME_LABEL"));

        conveyor.part().id(100).label("SOME_LABEL").addProperty("SOME_PROPERTY","VAL").value("VALUE1").place();
        conveyor.part().ttl(Duration.ofSeconds(10)).id(200).label("SOME_LABEL").addProperty("SOME_OTHER_PROPERTY","OTHER_VAL").value("VALUE2").place().join();


    }

}
